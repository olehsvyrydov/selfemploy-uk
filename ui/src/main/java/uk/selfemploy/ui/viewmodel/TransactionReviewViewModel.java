package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.enums.ReviewStatus;
import uk.selfemploy.ui.service.SqliteBankTransactionService;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Central ViewModel for the Transaction Review Dashboard.
 * Manages data loading, filtering, sorting, batch operations, undo, and pagination.
 */
public class TransactionReviewViewModel {

    private static final Logger LOG = Logger.getLogger(TransactionReviewViewModel.class.getName());
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final SqliteBankTransactionService service;

    // All items and filtered subset
    private final ObservableList<TransactionReviewTableRow> allItems = FXCollections.observableArrayList();
    private final ObservableList<TransactionReviewTableRow> filteredItems = FXCollections.observableArrayList();

    // Summary counts
    private final IntegerProperty totalCount = new SimpleIntegerProperty(0);
    private final IntegerProperty pendingCount = new SimpleIntegerProperty(0);
    private final IntegerProperty categorizedCount = new SimpleIntegerProperty(0);
    private final IntegerProperty excludedCount = new SimpleIntegerProperty(0);

    // Progress: reviewed / total
    private final DoubleProperty reviewProgress = new SimpleDoubleProperty(0.0);
    private final IntegerProperty reviewedCount = new SimpleIntegerProperty(0);

    // Filters
    private final StringProperty searchText = new SimpleStringProperty("");
    private final ObjectProperty<ReviewStatus> statusFilter = new SimpleObjectProperty<>(null);
    private final ObjectProperty<LocalDate> dateFrom = new SimpleObjectProperty<>(null);
    private final ObjectProperty<LocalDate> dateTo = new SimpleObjectProperty<>(null);
    private final ObjectProperty<BigDecimal> amountMin = new SimpleObjectProperty<>(null);
    private final ObjectProperty<BigDecimal> amountMax = new SimpleObjectProperty<>(null);

    // Sorting
    private Comparator<TransactionReviewTableRow> currentComparator =
            Comparator.comparing(TransactionReviewTableRow::date).reversed();

    // Selection
    private final Set<UUID> selectedIds = new HashSet<>();
    private final IntegerProperty selectedCount = new SimpleIntegerProperty(0);

    // Pagination
    private final IntegerProperty currentPage = new SimpleIntegerProperty(0);
    private final IntegerProperty totalPages = new SimpleIntegerProperty(1);
    private final IntegerProperty pageSize = new SimpleIntegerProperty(DEFAULT_PAGE_SIZE);

    // Undo (single level)
    private List<BankTransaction> undoSnapshot;
    private final BooleanProperty canUndo = new SimpleBooleanProperty(false);

    public TransactionReviewViewModel(SqliteBankTransactionService service) {
        this.service = service;

        // Auto-apply filters when filter properties change
        searchText.addListener((obs, o, n) -> applyFilters());
        statusFilter.addListener((obs, o, n) -> applyFilters());
        dateFrom.addListener((obs, o, n) -> applyFilters());
        dateTo.addListener((obs, o, n) -> applyFilters());
        amountMin.addListener((obs, o, n) -> applyFilters());
        amountMax.addListener((obs, o, n) -> applyFilters());
    }

    // === Data Loading ===

    /**
     * Loads all bank transactions from the service.
     */
    public void loadTransactions() {
        List<BankTransaction> transactions = service.findAll();

        allItems.clear();
        for (BankTransaction tx : transactions) {
            allItems.add(TransactionReviewTableRow.fromDomain(tx));
        }

        updateSummaries();
        applyFilters();
        LOG.info("Loaded " + transactions.size() + " bank transactions for review");
    }

    // === Filtering ===

    /**
     * Applies all current filters and sorts the result.
     */
    public void applyFilters() {
        List<TransactionReviewTableRow> filtered = allItems.stream()
            .filter(row -> row.matchesSearch(searchText.get()))
            .filter(row -> row.matchesStatus(statusFilter.get()))
            .filter(row -> row.matchesDateRange(dateFrom.get(), dateTo.get()))
            .filter(row -> row.matchesAmountRange(amountMin.get(), amountMax.get()))
            .sorted(currentComparator)
            .collect(Collectors.toList());

        filteredItems.setAll(filtered);
        updatePagination();
        currentPage.set(0);
    }

    // === Sorting ===

    public void sortByDate(boolean ascending) {
        currentComparator = ascending
            ? Comparator.comparing(TransactionReviewTableRow::date)
            : Comparator.comparing(TransactionReviewTableRow::date).reversed();
        applyFilters();
    }

    public void sortByAmount(boolean ascending) {
        currentComparator = ascending
            ? Comparator.comparing(r -> r.amount().abs())
            : Comparator.<TransactionReviewTableRow, BigDecimal>comparing(r -> r.amount().abs()).reversed();
        applyFilters();
    }

    public void sortByDescription(boolean ascending) {
        currentComparator = ascending
            ? Comparator.comparing(TransactionReviewTableRow::description, String.CASE_INSENSITIVE_ORDER)
            : Comparator.comparing(TransactionReviewTableRow::description, String.CASE_INSENSITIVE_ORDER).reversed();
        applyFilters();
    }

    public void sortByConfidence(boolean ascending) {
        Comparator<TransactionReviewTableRow> cmp = Comparator.comparing(
            r -> r.confidenceScore() != null ? r.confidenceScore() : BigDecimal.ZERO
        );
        currentComparator = ascending ? cmp : cmp.reversed();
        applyFilters();
    }

    // === Selection ===

    public void toggleSelection(UUID id) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id);
        } else {
            selectedIds.add(id);
        }
        selectedCount.set(selectedIds.size());
    }

    public boolean isSelected(UUID id) {
        return selectedIds.contains(id);
    }

    public void selectAll() {
        selectedIds.clear();
        for (TransactionReviewTableRow row : filteredItems) {
            selectedIds.add(row.id());
        }
        selectedCount.set(selectedIds.size());
    }

    public void selectAllPending() {
        selectedIds.clear();
        for (TransactionReviewTableRow row : filteredItems) {
            if (row.reviewStatus() == ReviewStatus.PENDING) {
                selectedIds.add(row.id());
            }
        }
        selectedCount.set(selectedIds.size());
    }

    public void clearSelection() {
        selectedIds.clear();
        selectedCount.set(0);
    }

    public Set<UUID> getSelectedIds() {
        return Set.copyOf(selectedIds);
    }

    // === Batch Operations ===

    /**
     * Marks all selected transactions as business.
     */
    public void batchMarkBusiness() {
        if (selectedIds.isEmpty()) return;
        saveUndoSnapshot();
        Instant now = Instant.now();
        for (UUID id : selectedIds) {
            service.setBusinessFlag(id, true, now);
        }
        clearSelection();
        loadTransactions();
    }

    /**
     * Marks all selected transactions as personal.
     */
    public void batchMarkPersonal() {
        if (selectedIds.isEmpty()) return;
        saveUndoSnapshot();
        Instant now = Instant.now();
        for (UUID id : selectedIds) {
            service.setBusinessFlag(id, false, now);
        }
        clearSelection();
        loadTransactions();
    }

    /**
     * Excludes all selected transactions with the given reason.
     */
    public void batchExclude(String reason) {
        if (selectedIds.isEmpty()) return;
        saveUndoSnapshot();
        Instant now = Instant.now();
        for (UUID id : selectedIds) {
            service.exclude(id, reason, now);
        }
        clearSelection();
        loadTransactions();
    }

    // === Individual Operations ===

    /**
     * Categorizes a single transaction as income.
     */
    public void categorizeAsIncome(UUID txId, UUID incomeId) {
        saveUndoSnapshot();
        service.categorizeAsIncome(txId, incomeId, Instant.now());
        loadTransactions();
    }

    /**
     * Categorizes a single transaction as expense.
     */
    public void categorizeAsExpense(UUID txId, UUID expenseId) {
        saveUndoSnapshot();
        service.categorizeAsExpense(txId, expenseId, Instant.now());
        loadTransactions();
    }

    /**
     * Excludes a single transaction.
     */
    public void excludeTransaction(UUID txId, String reason) {
        saveUndoSnapshot();
        service.exclude(txId, reason, Instant.now());
        loadTransactions();
    }

    /**
     * Skips a single transaction.
     */
    public void skipTransaction(UUID txId) {
        saveUndoSnapshot();
        service.skip(txId, Instant.now());
        loadTransactions();
    }

    /**
     * Toggles the business/personal flag.
     */
    public void toggleBusinessFlag(UUID txId, Boolean isBusiness) {
        saveUndoSnapshot();
        service.setBusinessFlag(txId, isBusiness, Instant.now());
        loadTransactions();
    }

    // === Undo ===

    private void saveUndoSnapshot() {
        undoSnapshot = new ArrayList<>(service.findAll());
        canUndo.set(true);
    }

    /**
     * Undoes the last operation by restoring the snapshot.
     */
    public void undo() {
        if (undoSnapshot == null) return;
        // Restore all transactions from snapshot
        for (BankTransaction tx : undoSnapshot) {
            service.save(tx);
        }
        undoSnapshot = null;
        canUndo.set(false);
        loadTransactions();
        LOG.info("Undo completed - restored previous transaction state");
    }

    // === Export ===

    /**
     * Exports filtered transactions to CSV.
     */
    public void exportCsv(Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("Date,Description,Amount,Status,Business/Personal,Category,Exclusion Reason");
            writer.newLine();
            for (TransactionReviewTableRow row : filteredItems) {
                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                    row.date(),
                    row.description().replace("\"", "\"\""),
                    row.amount().toPlainString(),
                    row.reviewStatus().name(),
                    row.getBusinessLabel(),
                    row.getSuggestedCategoryDisplay(),
                    row.exclusionReason() != null ? row.exclusionReason() : ""
                ));
                writer.newLine();
            }
        }
        LOG.info("Exported " + filteredItems.size() + " transactions to CSV: " + path);
    }

    /**
     * Exports filtered transactions to JSON.
     */
    public void exportJson(Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("[\n");
            for (int i = 0; i < filteredItems.size(); i++) {
                TransactionReviewTableRow row = filteredItems.get(i);
                writer.write(String.format(
                    "  {\"date\":\"%s\",\"description\":\"%s\",\"amount\":\"%s\",\"status\":\"%s\"," +
                    "\"isBusiness\":%s,\"category\":\"%s\",\"exclusionReason\":%s}",
                    row.date(),
                    row.description().replace("\"", "\\\""),
                    row.amount().toPlainString(),
                    row.reviewStatus().name(),
                    row.isBusiness() != null ? row.isBusiness().toString() : "null",
                    row.getSuggestedCategoryDisplay(),
                    row.exclusionReason() != null ? "\"" + row.exclusionReason().replace("\"", "\\\"") + "\"" : "null"
                ));
                if (i < filteredItems.size() - 1) {
                    writer.write(",");
                }
                writer.newLine();
            }
            writer.write("]");
        }
        LOG.info("Exported " + filteredItems.size() + " transactions to JSON: " + path);
    }

    // === Pagination ===

    public List<TransactionReviewTableRow> getCurrentPageItems() {
        int start = currentPage.get() * pageSize.get();
        int end = Math.min(start + pageSize.get(), filteredItems.size());
        if (start >= filteredItems.size()) {
            return List.of();
        }
        return filteredItems.subList(start, end);
    }

    public String getResultCountText() {
        int total = filteredItems.size();
        if (total == 0) {
            return "Showing 0 entries";
        }
        int start = currentPage.get() * pageSize.get() + 1;
        int end = Math.min(start + pageSize.get() - 1, total);
        return String.format("Showing %d\u2013%d of %d entries", start, end, total);
    }

    public void nextPage() {
        if (canGoNext()) {
            currentPage.set(currentPage.get() + 1);
        }
    }

    public void previousPage() {
        if (canGoPrevious()) {
            currentPage.set(currentPage.get() - 1);
        }
    }

    public boolean canGoNext() {
        return currentPage.get() < totalPages.get() - 1;
    }

    public boolean canGoPrevious() {
        return currentPage.get() > 0;
    }

    // === Summary Calculations ===

    private void updateSummaries() {
        int total = allItems.size();
        int pending = 0;
        int categorized = 0;
        int excluded = 0;
        int reviewed = 0;

        for (TransactionReviewTableRow row : allItems) {
            switch (row.reviewStatus()) {
                case PENDING -> pending++;
                case CATEGORIZED -> { categorized++; reviewed++; }
                case EXCLUDED -> { excluded++; reviewed++; }
                case SKIPPED -> reviewed++;
            }
        }

        totalCount.set(total);
        pendingCount.set(pending);
        categorizedCount.set(categorized);
        excludedCount.set(excluded);
        reviewedCount.set(reviewed);
        reviewProgress.set(total > 0 ? (double) reviewed / total : 0.0);
    }

    private void updatePagination() {
        int total = filteredItems.size();
        int pages = total == 0 ? 1 : (int) Math.ceil((double) total / pageSize.get());
        totalPages.set(pages);
    }

    // === Property Getters ===

    public ObservableList<TransactionReviewTableRow> getAllItems() { return allItems; }
    public ObservableList<TransactionReviewTableRow> getFilteredItems() { return filteredItems; }

    public int getTotalCount() { return totalCount.get(); }
    public IntegerProperty totalCountProperty() { return totalCount; }

    public int getPendingCount() { return pendingCount.get(); }
    public IntegerProperty pendingCountProperty() { return pendingCount; }

    public int getCategorizedCount() { return categorizedCount.get(); }
    public IntegerProperty categorizedCountProperty() { return categorizedCount; }

    public int getExcludedCount() { return excludedCount.get(); }
    public IntegerProperty excludedCountProperty() { return excludedCount; }

    public double getReviewProgress() { return reviewProgress.get(); }
    public DoubleProperty reviewProgressProperty() { return reviewProgress; }

    public int getReviewedCount() { return reviewedCount.get(); }
    public IntegerProperty reviewedCountProperty() { return reviewedCount; }

    public StringProperty searchTextProperty() { return searchText; }
    public ObjectProperty<ReviewStatus> statusFilterProperty() { return statusFilter; }
    public ObjectProperty<LocalDate> dateFromProperty() { return dateFrom; }
    public ObjectProperty<LocalDate> dateToProperty() { return dateTo; }
    public ObjectProperty<BigDecimal> amountMinProperty() { return amountMin; }
    public ObjectProperty<BigDecimal> amountMaxProperty() { return amountMax; }

    public int getSelectedCount() { return selectedCount.get(); }
    public IntegerProperty selectedCountProperty() { return selectedCount; }

    public IntegerProperty currentPageProperty() { return currentPage; }
    public IntegerProperty totalPagesProperty() { return totalPages; }
    public IntegerProperty pageSizeProperty() { return pageSize; }

    public boolean getCanUndo() { return canUndo.get(); }
    public BooleanProperty canUndoProperty() { return canUndo; }

    public boolean isEmptyState() { return allItems.isEmpty(); }
    public boolean isNoResults() { return !allItems.isEmpty() && filteredItems.isEmpty(); }
}
