package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeStatus;
import uk.selfemploy.core.service.IncomeService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ViewModel for the Income List View.
 * Manages income data, filtering, sorting, and pagination.
 */
public class IncomeListViewModel {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final IncomeService incomeService;
    private final UUID businessId;

    // Income data
    private final ObservableList<IncomeTableRow> incomeItems = FXCollections.observableArrayList();
    private final ObservableList<IncomeTableRow> filteredItems = FXCollections.observableArrayList();

    // Summary values
    private final ObjectProperty<BigDecimal> totalIncome = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> paidIncome = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> unpaidIncome = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final IntegerProperty totalCount = new SimpleIntegerProperty(0);
    private final IntegerProperty paidCount = new SimpleIntegerProperty(0);
    private final IntegerProperty unpaidCount = new SimpleIntegerProperty(0);

    // Filters
    private final StringProperty searchText = new SimpleStringProperty("");
    private final ObjectProperty<IncomeStatus> statusFilter = new SimpleObjectProperty<>(null);

    // Pagination
    private final IntegerProperty currentPage = new SimpleIntegerProperty(0);
    private final IntegerProperty totalPages = new SimpleIntegerProperty(0);
    private final IntegerProperty pageSize = new SimpleIntegerProperty(DEFAULT_PAGE_SIZE);

    // Sorting
    private Comparator<IncomeTableRow> currentComparator = Comparator.comparing(IncomeTableRow::date).reversed();

    // Current tax year for refresh
    private TaxYear currentTaxYear;

    /**
     * Creates a new IncomeListViewModel.
     *
     * @param incomeService The income service for data access
     * @param businessId The business ID
     */
    public IncomeListViewModel(IncomeService incomeService, UUID businessId) {
        this.incomeService = incomeService;
        this.businessId = businessId;

        // Listen for filter changes
        searchText.addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilter.addListener((obs, oldVal, newVal) -> applyFilters());
    }

    /**
     * Loads income for the given tax year.
     *
     * @param taxYear The tax year to load income for
     */
    public void loadIncome(TaxYear taxYear) {
        this.currentTaxYear = taxYear;
        List<Income> incomes = incomeService.findByTaxYear(businessId, taxYear);

        incomeItems.clear();
        for (Income income : incomes) {
            incomeItems.add(IncomeTableRow.fromIncome(income));
        }

        updateSummaries();
        applyFilters();
    }

    /**
     * Refreshes the income data from the service.
     */
    public void refresh() {
        if (currentTaxYear != null) {
            loadIncome(currentTaxYear);
        }
    }

    /**
     * Adds an income row to the list.
     * Used for testing and manual additions.
     *
     * @param row The income row to add
     */
    public void addIncomeRow(IncomeTableRow row) {
        incomeItems.add(row);
        updateSummaries();
        applyFilters();
    }

    /**
     * Clears all income items.
     */
    public void clearAll() {
        incomeItems.clear();
        updateSummaries();
        applyFilters();
    }

    /**
     * Applies current filters and updates the filtered list.
     */
    public void applyFilters() {
        List<IncomeTableRow> filtered = incomeItems.stream()
            .filter(item -> item.matchesSearch(searchText.get()))
            .filter(item -> item.matchesStatus(statusFilter.get()))
            .sorted(currentComparator)
            .collect(Collectors.toList());

        filteredItems.setAll(filtered);
        updatePagination();
        currentPage.set(0); // Reset to first page when filters change
    }

    /**
     * Sorts by date.
     *
     * @param ascending True for ascending, false for descending
     */
    public void sortByDate(boolean ascending) {
        currentComparator = ascending
            ? Comparator.comparing(IncomeTableRow::date)
            : Comparator.comparing(IncomeTableRow::date).reversed();
        applyFilters();
    }

    /**
     * Sorts by amount.
     *
     * @param ascending True for ascending, false for descending
     */
    public void sortByAmount(boolean ascending) {
        currentComparator = ascending
            ? Comparator.comparing(IncomeTableRow::amount)
            : Comparator.comparing(IncomeTableRow::amount).reversed();
        applyFilters();
    }

    /**
     * Sorts by client name.
     *
     * @param ascending True for ascending, false for descending
     */
    public void sortByClientName(boolean ascending) {
        currentComparator = ascending
            ? Comparator.comparing(IncomeTableRow::clientName, String.CASE_INSENSITIVE_ORDER)
            : Comparator.comparing(IncomeTableRow::clientName, String.CASE_INSENSITIVE_ORDER).reversed();
        applyFilters();
    }

    /**
     * Navigates to the next page.
     */
    public void nextPage() {
        if (canGoNext()) {
            currentPage.set(currentPage.get() + 1);
        }
    }

    /**
     * Navigates to the previous page.
     */
    public void previousPage() {
        if (canGoPrevious()) {
            currentPage.set(currentPage.get() - 1);
        }
    }

    /**
     * Checks if navigation to next page is possible.
     */
    public boolean canGoNext() {
        return currentPage.get() < totalPages.get() - 1;
    }

    /**
     * Checks if navigation to previous page is possible.
     */
    public boolean canGoPrevious() {
        return currentPage.get() > 0;
    }

    /**
     * Returns the items for the current page.
     */
    public List<IncomeTableRow> getCurrentPageItems() {
        int start = currentPage.get() * pageSize.get();
        int end = Math.min(start + pageSize.get(), filteredItems.size());
        if (start >= filteredItems.size()) {
            return List.of();
        }
        return filteredItems.subList(start, end);
    }

    /**
     * Returns the result count text (e.g., "Showing 1-20 of 47 entries").
     */
    public String getResultCountText() {
        int total = filteredItems.size();
        if (total == 0) {
            return "Showing 0 entries";
        }
        int start = currentPage.get() * pageSize.get() + 1;
        int end = Math.min(start + pageSize.get() - 1, total);
        return String.format("Showing %d-%d of %d entries", start, end, total);
    }

    // === Summary Updates ===

    private void updateSummaries() {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal unpaid = BigDecimal.ZERO;
        int paidCnt = 0;
        int unpaidCnt = 0;

        for (IncomeTableRow item : incomeItems) {
            total = total.add(item.amount());
            if (item.status() == IncomeStatus.PAID) {
                paid = paid.add(item.amount());
                paidCnt++;
            } else {
                unpaid = unpaid.add(item.amount());
                unpaidCnt++;
            }
        }

        totalIncome.set(total);
        paidIncome.set(paid);
        unpaidIncome.set(unpaid);
        totalCount.set(incomeItems.size());
        paidCount.set(paidCnt);
        unpaidCount.set(unpaidCnt);
    }

    private void updatePagination() {
        int total = filteredItems.size();
        int pages = total == 0 ? 1 : (int) Math.ceil((double) total / pageSize.get());
        totalPages.set(pages);
    }

    // === Getters ===

    public ObservableList<IncomeTableRow> getIncomeItems() {
        return incomeItems;
    }

    public List<IncomeTableRow> getFilteredItems() {
        return filteredItems;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome.get();
    }

    public ObjectProperty<BigDecimal> totalIncomeProperty() {
        return totalIncome;
    }

    public String getFormattedTotalIncome() {
        return CURRENCY_FORMAT.format(totalIncome.get());
    }

    public BigDecimal getPaidIncome() {
        return paidIncome.get();
    }

    public ObjectProperty<BigDecimal> paidIncomeProperty() {
        return paidIncome;
    }

    public String getFormattedPaidIncome() {
        return CURRENCY_FORMAT.format(paidIncome.get());
    }

    public BigDecimal getUnpaidIncome() {
        return unpaidIncome.get();
    }

    public ObjectProperty<BigDecimal> unpaidIncomeProperty() {
        return unpaidIncome;
    }

    public String getFormattedUnpaidIncome() {
        return CURRENCY_FORMAT.format(unpaidIncome.get());
    }

    public int getTotalCount() {
        return totalCount.get();
    }

    public IntegerProperty totalCountProperty() {
        return totalCount;
    }

    public String getTotalCountText() {
        int count = totalCount.get();
        return count == 1 ? "1 entry" : count + " entries";
    }

    public String getPaidCountText() {
        int count = paidCount.get();
        return count == 1 ? "1 entry" : count + " entries";
    }

    public String getUnpaidCountText() {
        int count = unpaidCount.get();
        return count == 1 ? "1 entry" : count + " entries";
    }

    public int getPaidCount() {
        return paidCount.get();
    }

    public IntegerProperty paidCountProperty() {
        return paidCount;
    }

    public int getUnpaidCount() {
        return unpaidCount.get();
    }

    public IntegerProperty unpaidCountProperty() {
        return unpaidCount;
    }

    public String getSearchText() {
        return searchText.get();
    }

    public void setSearchText(String text) {
        searchText.set(text);
    }

    public StringProperty searchTextProperty() {
        return searchText;
    }

    public IncomeStatus getStatusFilter() {
        return statusFilter.get();
    }

    public void setStatusFilter(IncomeStatus status) {
        statusFilter.set(status);
    }

    public ObjectProperty<IncomeStatus> statusFilterProperty() {
        return statusFilter;
    }

    public int getCurrentPage() {
        return currentPage.get();
    }

    public IntegerProperty currentPageProperty() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages.get();
    }

    public IntegerProperty totalPagesProperty() {
        return totalPages;
    }

    public int getPageSize() {
        return pageSize.get();
    }

    public void setPageSize(int size) {
        pageSize.set(size);
        updatePagination();
    }

    public IntegerProperty pageSizeProperty() {
        return pageSize;
    }

    /**
     * Returns true if there are no income items at all (true empty state).
     */
    public boolean isEmptyState() {
        return incomeItems.isEmpty();
    }

    /**
     * Returns true if filters returned no results but there are items.
     */
    public boolean isNoResults() {
        return !incomeItems.isEmpty() && filteredItems.isEmpty();
    }

    /**
     * Returns the business ID.
     */
    public UUID getBusinessId() {
        return businessId;
    }

    /**
     * Returns the current tax year.
     */
    public TaxYear getCurrentTaxYear() {
        return currentTaxYear;
    }
}
