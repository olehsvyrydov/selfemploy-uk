package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.service.ExpenseService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ViewModel for the Expense List view.
 * Manages expense data, filtering, sorting, and pagination.
 */
public class ExpenseListViewModel {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ExpenseService expenseService;

    // Business context
    private UUID businessId;
    private TaxYear taxYear;
    private boolean cisBusiness = false;

    // All expense items (unfiltered)
    private final ObservableList<ExpenseTableRow> expenseItems = FXCollections.observableArrayList();

    // Summary totals
    private final ObjectProperty<BigDecimal> totalExpenses = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> deductibleTotal = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> nonDeductibleTotal = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final IntegerProperty totalCount = new SimpleIntegerProperty(0);
    private final IntegerProperty deductibleCount = new SimpleIntegerProperty(0);
    private final IntegerProperty nonDeductibleCount = new SimpleIntegerProperty(0);

    // Filters
    private final StringProperty searchText = new SimpleStringProperty("");
    private final ObjectProperty<ExpenseCategory> selectedCategory = new SimpleObjectProperty<>(null);

    // Pagination
    private final IntegerProperty currentPage = new SimpleIntegerProperty(0);
    private final IntegerProperty pageSize = new SimpleIntegerProperty(DEFAULT_PAGE_SIZE);
    private final IntegerProperty totalPages = new SimpleIntegerProperty(0);

    // State
    private final BooleanProperty emptyState = new SimpleBooleanProperty(true);
    private final BooleanProperty loading = new SimpleBooleanProperty(false);

    public ExpenseListViewModel(ExpenseService expenseService) {
        this.expenseService = expenseService;

        // Reset page when filters change
        searchText.addListener((obs, oldVal, newVal) -> resetPage());
        selectedCategory.addListener((obs, oldVal, newVal) -> resetPage());
    }

    // === Data Loading ===

    /**
     * Loads expenses from the service for the current tax year.
     */
    public void loadExpenses() {
        if (businessId == null || taxYear == null) {
            return;
        }

        loading.set(true);
        try {
            // Load expenses from service
            List<Expense> expenses = expenseService.findByTaxYear(businessId, taxYear);

            // Convert to table rows and sort by date descending
            List<ExpenseTableRow> rows = expenses.stream()
                .map(ExpenseTableRow::fromExpense)
                .sorted(Comparator.comparing(ExpenseTableRow::date).reversed())
                .toList();

            expenseItems.setAll(rows);

            // Load totals from service
            BigDecimal total = expenseService.getTotalByTaxYear(businessId, taxYear);
            BigDecimal allowable = expenseService.getDeductibleTotal(businessId, taxYear);
            BigDecimal nonAllowable = total.subtract(allowable);

            totalExpenses.set(total);
            deductibleTotal.set(allowable);
            nonDeductibleTotal.set(nonAllowable);

            // Calculate counts
            totalCount.set(rows.size());
            long deductible = rows.stream().filter(ExpenseTableRow::deductible).count();
            deductibleCount.set((int) deductible);
            nonDeductibleCount.set(rows.size() - (int) deductible);

            // Update pagination
            updatePagination();

            // Update empty state
            emptyState.set(rows.isEmpty());

        } finally {
            loading.set(false);
        }
    }

    /**
     * Refreshes the expense list from the database.
     */
    public void refresh() {
        loadExpenses();
    }

    // === Filtering ===

    /**
     * Returns filtered items based on current search text and category filter.
     */
    public List<ExpenseTableRow> getFilteredItems() {
        return expenseItems.stream()
            .filter(this::matchesSearchFilter)
            .filter(this::matchesCategoryFilter)
            .toList();
    }

    private boolean matchesSearchFilter(ExpenseTableRow row) {
        String search = searchText.get();
        if (search == null || search.isBlank()) {
            return true;
        }
        return row.description().toLowerCase().contains(search.toLowerCase());
    }

    private boolean matchesCategoryFilter(ExpenseTableRow row) {
        ExpenseCategory category = selectedCategory.get();
        if (category == null) {
            return true;
        }
        return row.category() == category;
    }

    // === Pagination ===

    /**
     * Returns the items for the current page.
     */
    public List<ExpenseTableRow> getCurrentPageItems() {
        List<ExpenseTableRow> filtered = getFilteredItems();
        int start = currentPage.get() * pageSize.get();
        int end = Math.min(start + pageSize.get(), filtered.size());

        if (start >= filtered.size()) {
            return Collections.emptyList();
        }

        return filtered.subList(start, end);
    }

    /**
     * Navigates to the next page if available.
     */
    public void nextPage() {
        if (hasNextPage()) {
            currentPage.set(currentPage.get() + 1);
        }
    }

    /**
     * Navigates to the previous page if available.
     */
    public void previousPage() {
        if (hasPreviousPage()) {
            currentPage.set(currentPage.get() - 1);
        }
    }

    /**
     * Returns true if there is a next page available.
     */
    public boolean hasNextPage() {
        return currentPage.get() < totalPages.get() - 1;
    }

    /**
     * Returns true if there is a previous page available.
     */
    public boolean hasPreviousPage() {
        return currentPage.get() > 0;
    }

    /**
     * Returns the result count text (e.g., "Showing 1-20 of 89 entries").
     */
    public String getResultCountText() {
        List<ExpenseTableRow> filtered = getFilteredItems();
        int total = filtered.size();

        if (total == 0) {
            return "Showing 0 entries";
        }

        int start = currentPage.get() * pageSize.get() + 1;
        int end = Math.min(start + pageSize.get() - 1, total);

        return String.format("Showing %d-%d of %d entries", start, end, total);
    }

    private void resetPage() {
        currentPage.set(0);
        updatePagination();
    }

    private void updatePagination() {
        int filtered = getFilteredItems().size();
        int pages = (filtered + pageSize.get() - 1) / pageSize.get();
        totalPages.set(Math.max(1, pages));

        // Ensure current page is valid
        if (currentPage.get() >= totalPages.get()) {
            currentPage.set(Math.max(0, totalPages.get() - 1));
        }
    }

    // === Available Categories ===

    /**
     * Returns the list of available categories based on whether this is a CIS business.
     */
    public List<ExpenseCategory> getAvailableCategories() {
        return Arrays.stream(ExpenseCategory.values())
            .filter(category -> cisBusiness || !category.isCisOnly())
            .toList();
    }

    // === Formatted Values ===

    public String getFormattedTotalExpenses() {
        return formatCurrency(totalExpenses.get());
    }

    public String getFormattedDeductibleTotal() {
        return formatCurrency(deductibleTotal.get());
    }

    public String getFormattedNonDeductibleTotal() {
        return formatCurrency(nonDeductibleTotal.get());
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return CURRENCY_FORMAT.format(amount);
    }

    // === Getters and Setters ===

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public TaxYear getTaxYear() {
        return taxYear;
    }

    public void setTaxYear(TaxYear taxYear) {
        this.taxYear = taxYear;
    }

    public boolean isCisBusiness() {
        return cisBusiness;
    }

    public void setCisBusiness(boolean cisBusiness) {
        this.cisBusiness = cisBusiness;
    }

    public ObservableList<ExpenseTableRow> getExpenseItems() {
        return expenseItems;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses.get();
    }

    public ObjectProperty<BigDecimal> totalExpensesProperty() {
        return totalExpenses;
    }

    public BigDecimal getDeductibleTotal() {
        return deductibleTotal.get();
    }

    public ObjectProperty<BigDecimal> deductibleTotalProperty() {
        return deductibleTotal;
    }

    public BigDecimal getNonDeductibleTotal() {
        return nonDeductibleTotal.get();
    }

    public ObjectProperty<BigDecimal> nonDeductibleTotalProperty() {
        return nonDeductibleTotal;
    }

    public int getTotalCount() {
        return totalCount.get();
    }

    public IntegerProperty totalCountProperty() {
        return totalCount;
    }

    public int getDeductibleCount() {
        return deductibleCount.get();
    }

    public IntegerProperty deductibleCountProperty() {
        return deductibleCount;
    }

    public int getNonDeductibleCount() {
        return nonDeductibleCount.get();
    }

    public IntegerProperty nonDeductibleCountProperty() {
        return nonDeductibleCount;
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

    public ExpenseCategory getSelectedCategory() {
        return selectedCategory.get();
    }

    public void setSelectedCategory(ExpenseCategory category) {
        selectedCategory.set(category);
    }

    public ObjectProperty<ExpenseCategory> selectedCategoryProperty() {
        return selectedCategory;
    }

    public int getCurrentPage() {
        return currentPage.get();
    }

    public IntegerProperty currentPageProperty() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize.get();
    }

    public void setPageSize(int size) {
        pageSize.set(size);
    }

    public IntegerProperty pageSizeProperty() {
        return pageSize;
    }

    public int getTotalPages() {
        return totalPages.get();
    }

    public IntegerProperty totalPagesProperty() {
        return totalPages;
    }

    public boolean isEmptyState() {
        return emptyState.get();
    }

    public BooleanProperty emptyStateProperty() {
        return emptyState;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }
}
