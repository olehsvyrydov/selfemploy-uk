package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.ui.viewmodel.ExpenseListViewModel;
import uk.selfemploy.ui.viewmodel.ExpenseTableRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ExpenseController.
 * Tests the controller logic for the expense list view.
 *
 * Test Coverage (as per Sprint 8 Test Design):
 * - TC-EXP-001 to TC-EXP-007: Bento Grid Summary Cards
 * - TC-EXP-010 to TC-EXP-014: Help Icon Click Handlers
 * - TC-EXP-020 to TC-EXP-025: Category Filter Functionality
 * - TC-EXP-030 to TC-EXP-035: Search Field with Debounce
 * - TC-EXP-040 to TC-EXP-052: Table Interactions
 * - TC-EXP-060 to TC-EXP-066: Pagination Controls
 * - TC-EXP-070 to TC-EXP-074: Empty State Display
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseController")
class ExpenseControllerTest {

    @Mock
    private ExpenseService expenseService;

    private ExpenseListViewModel viewModel;
    private TaxYear taxYear;
    private UUID businessId;

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        taxYear = TaxYear.of(2025);
        viewModel = new ExpenseListViewModel(expenseService);
        viewModel.setBusinessId(businessId);
        viewModel.setTaxYear(taxYear);
    }

    @Nested
    @DisplayName("Tax Year Awareness")
    class TaxYearAwareness {

        @Test
        @DisplayName("should update view model when tax year changes")
        void shouldUpdateViewModelWhenTaxYearChanges() {
            TaxYear newTaxYear = TaxYear.of(2024);
            viewModel.setTaxYear(newTaxYear);

            assertThat(viewModel.getTaxYear()).isEqualTo(newTaxYear);
        }

        @Test
        @DisplayName("should refresh data when tax year changes")
        void shouldRefreshDataWhenTaxYearChanges() {
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of());
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            TaxYear newTaxYear = TaxYear.of(2024);
            viewModel.setTaxYear(newTaxYear);
            viewModel.loadExpenses();

            verify(expenseService).findByTaxYear(businessId, newTaxYear);
        }
    }

    @Nested
    @DisplayName("Bento Grid Summary Cards - TC-EXP-001 to TC-EXP-007")
    class BentoGridSummaryCards {

        @Test
        @DisplayName("TC-EXP-001: should display total expenses formatted as currency")
        void shouldDisplayFormattedTotalExpenses() {
            // Given - expenses exist for the current tax year
            List<Expense> expenses = List.of(
                createExpenseWithAmount(ExpenseCategory.OFFICE_COSTS, "50.00"),
                createExpenseWithAmount(ExpenseCategory.TRAVEL, "100.00"),
                createExpenseWithAmount(ExpenseCategory.DEPRECIATION, "200.00")
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(new BigDecimal("350.00"));
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(new BigDecimal("150.00"));

            // When - expense page loads
            viewModel.loadExpenses();

            // Then - total card displays sum formatted as currency with pound sign
            assertThat(viewModel.getFormattedTotalExpenses()).isEqualTo("£350.00");
        }

        @Test
        @DisplayName("TC-EXP-002: should display deductible total correctly")
        void shouldDisplayDeductibleTotal() {
            // Given - expenses with mixed deductibility
            List<Expense> expenses = List.of(
                createExpenseWithAmount(ExpenseCategory.OFFICE_COSTS, "50.00"),  // deductible
                createExpenseWithAmount(ExpenseCategory.TRAVEL, "100.00"),       // deductible
                createExpenseWithAmount(ExpenseCategory.DEPRECIATION, "200.00")  // non-deductible
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(new BigDecimal("350.00"));
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(new BigDecimal("150.00"));

            // When
            viewModel.loadExpenses();

            // Then - deductible card shows only tax-deductible expenses
            assertThat(viewModel.getFormattedDeductibleTotal()).isEqualTo("£150.00");
        }

        @Test
        @DisplayName("TC-EXP-003: should display non-deductible total as calculated difference")
        void shouldDisplayNonDeductibleTotal() {
            // Given
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of());
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(new BigDecimal("8230.50"));
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(new BigDecimal("7980.00"));

            // When
            viewModel.loadExpenses();

            // Then - non-deductible = total - deductible
            assertThat(viewModel.getFormattedNonDeductibleTotal()).isEqualTo("£250.50");
        }

        @Test
        @DisplayName("TC-EXP-004: should display entry counts correctly")
        void shouldFormatEntryCounts() {
            // Given - 3 expenses with 2 deductible
            List<Expense> expenses = List.of(
                createExpense(ExpenseCategory.OFFICE_COSTS),  // deductible
                createExpense(ExpenseCategory.TRAVEL),        // deductible
                createExpense(ExpenseCategory.DEPRECIATION)   // non-deductible
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            viewModel.loadExpenses();

            // Then - each card shows correct entry count
            assertThat(viewModel.getTotalCount()).isEqualTo(3);
            assertThat(viewModel.getDeductibleCount()).isEqualTo(2);
            assertThat(viewModel.getNonDeductibleCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-EXP-005: should update summary cards on data refresh")
        void shouldUpdateSummaryOnRefresh() {
            // Given - initial load
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of());
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When - data changes and refresh called
            List<Expense> newExpenses = List.of(createExpenseWithAmount(ExpenseCategory.OFFICE_COSTS, "500.00"));
            when(expenseService.findByTaxYear(any(), any())).thenReturn(newExpenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(new BigDecimal("500.00"));
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(new BigDecimal("500.00"));
            viewModel.refresh();

            // Then - all summary values update
            assertThat(viewModel.getFormattedTotalExpenses()).isEqualTo("£500.00");
            assertThat(viewModel.getTotalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-EXP-006: should handle zero values correctly")
        void shouldHandleZeroValues() {
            // Given - no expenses
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of());
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            viewModel.loadExpenses();

            // Then - cards display "£0.00" and count = 0
            assertThat(viewModel.getFormattedTotalExpenses()).isEqualTo("£0.00");
            assertThat(viewModel.getFormattedDeductibleTotal()).isEqualTo("£0.00");
            assertThat(viewModel.getFormattedNonDeductibleTotal()).isEqualTo("£0.00");
            assertThat(viewModel.getTotalCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Category Filter - TC-EXP-020 to TC-EXP-025")
    class CategoryFilterFunctionality {

        @Test
        @DisplayName("TC-EXP-020: should default to all categories (null filter)")
        void shouldDefaultToAllCategories() {
            // Given
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of());
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // Then - default is "All Categories" (null)
            assertThat(viewModel.getSelectedCategory()).isNull();
        }

        @Test
        @DisplayName("TC-EXP-021: should exclude CIS categories for non-CIS business")
        void shouldExcludeCisCategoriesForNonCisBusiness() {
            // Given - not a CIS business
            viewModel.setCisBusiness(false);

            // When
            List<ExpenseCategory> categories = viewModel.getAvailableCategories();

            // Then - CIS-only categories not visible
            assertThat(categories).doesNotContain(ExpenseCategory.SUBCONTRACTOR_COSTS);
        }

        @Test
        @DisplayName("TC-EXP-021b: should include CIS categories for CIS business")
        void shouldIncludeCisCategoriesForCisBusiness() {
            // Given - is a CIS business
            viewModel.setCisBusiness(true);

            // When
            List<ExpenseCategory> categories = viewModel.getAvailableCategories();

            // Then - CIS categories are included
            assertThat(categories).contains(ExpenseCategory.SUBCONTRACTOR_COSTS);
        }

        @Test
        @DisplayName("TC-EXP-022: should filter table rows by selected category")
        void shouldFilterByCategory() {
            // Given - multiple expenses with different categories
            List<Expense> expenses = List.of(
                createExpense(ExpenseCategory.OFFICE_COSTS),
                createExpense(ExpenseCategory.OFFICE_COSTS),
                createExpense(ExpenseCategory.TRAVEL)
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When - select a specific category
            viewModel.setSelectedCategory(ExpenseCategory.OFFICE_COSTS);

            // Then - only matching expenses displayed
            assertThat(viewModel.getFilteredItems()).hasSize(2);
            assertThat(viewModel.getFilteredItems())
                .allMatch(row -> row.category() == ExpenseCategory.OFFICE_COSTS);
        }

        @Test
        @DisplayName("TC-EXP-023: should show all expenses when filter cleared")
        void shouldShowAllWhenFilterCleared() {
            // Given - filter is set
            List<Expense> expenses = List.of(
                createExpense(ExpenseCategory.OFFICE_COSTS),
                createExpense(ExpenseCategory.TRAVEL)
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();
            viewModel.setSelectedCategory(ExpenseCategory.OFFICE_COSTS);

            // When - select "All Categories" (null)
            viewModel.setSelectedCategory(null);

            // Then - full list restored
            assertThat(viewModel.getFilteredItems()).hasSize(2);
        }

        @Test
        @DisplayName("TC-EXP-025: should update filtered count on category change")
        void shouldUpdateFilteredCountOnCategoryChange() {
            // Given
            List<Expense> expenses = List.of(
                createExpense(ExpenseCategory.OFFICE_COSTS),
                createExpense(ExpenseCategory.OFFICE_COSTS),
                createExpense(ExpenseCategory.TRAVEL),
                createExpense(ExpenseCategory.PREMISES)
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When
            viewModel.setSelectedCategory(ExpenseCategory.OFFICE_COSTS);

            // Then - result count updates to match filtered items
            assertThat(viewModel.getFilteredItems()).hasSize(2);
            assertThat(viewModel.getResultCountText()).contains("2");
        }
    }

    @Nested
    @DisplayName("Search Field - TC-EXP-030 to TC-EXP-035")
    class SearchFieldFunctionality {

        @Test
        @DisplayName("TC-EXP-030: should filter by description")
        void shouldFilterByDescription() {
            // Given - expenses with varying descriptions
            List<Expense> expenses = List.of(
                createExpenseWithDescription("Adobe subscription"),
                createExpenseWithDescription("Microsoft 365"),
                createExpenseWithDescription("Adobe Creative Cloud")
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When - search for "Adobe"
            viewModel.setSearchText("Adobe");

            // Then - only Adobe-related expenses shown
            assertThat(viewModel.getFilteredItems()).hasSize(2);
            assertThat(viewModel.getFilteredItems())
                .allMatch(row -> row.description().contains("Adobe"));
        }

        @Test
        @DisplayName("TC-EXP-031: should search case-insensitively")
        void shouldSearchCaseInsensitively() {
            // Given
            List<Expense> expenses = List.of(
                createExpenseWithDescription("Adobe subscription"),
                createExpenseWithDescription("Train ticket")
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When - search lowercase
            viewModel.setSearchText("adobe");
            int lowercaseResults = viewModel.getFilteredItems().size();

            // And - search uppercase
            viewModel.setSearchText("ADOBE");
            int uppercaseResults = viewModel.getFilteredItems().size();

            // Then - same results
            assertThat(lowercaseResults).isEqualTo(uppercaseResults).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-EXP-032: should update result count on search")
        void shouldUpdateResultCountOnSearch() {
            // Given
            List<Expense> expenses = List.of(
                createExpenseWithDescription("Adobe"),
                createExpenseWithDescription("Microsoft"),
                createExpenseWithDescription("Adobe Cloud")
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When
            viewModel.setSearchText("Adobe");

            // Then - result count reflects search results
            assertThat(viewModel.getResultCountText()).contains("2");
        }

        @Test
        @DisplayName("TC-EXP-033: should show all expenses when search cleared")
        void shouldShowAllWhenSearchCleared() {
            // Given
            List<Expense> expenses = List.of(
                createExpenseWithDescription("Adobe subscription"),
                createExpenseWithDescription("Train ticket")
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();
            viewModel.setSearchText("Adobe");

            // When - clear search
            viewModel.setSearchText("");

            // Then - full list restored
            assertThat(viewModel.getFilteredItems()).hasSize(2);
        }

        @Test
        @DisplayName("TC-EXP-034: should combine search and category filter")
        void shouldCombineSearchAndCategoryFilter() {
            // Given
            List<Expense> expenses = List.of(
                new Expense(UUID.randomUUID(), businessId, LocalDate.of(2025, 6, 10),
                    new BigDecimal("50.00"), "Adobe subscription", ExpenseCategory.OFFICE_COSTS, null, null),
                new Expense(UUID.randomUUID(), businessId, LocalDate.of(2025, 6, 10),
                    new BigDecimal("100.00"), "Adobe training travel", ExpenseCategory.TRAVEL, null, null),
                new Expense(UUID.randomUUID(), businessId, LocalDate.of(2025, 6, 10),
                    new BigDecimal("50.00"), "Microsoft Office", ExpenseCategory.OFFICE_COSTS, null, null)
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When - apply both filters
            viewModel.setSelectedCategory(ExpenseCategory.OFFICE_COSTS);
            viewModel.setSearchText("Adobe");

            // Then - both filters apply simultaneously
            assertThat(viewModel.getFilteredItems()).hasSize(1);
            assertThat(viewModel.getFilteredItems().get(0).description()).isEqualTo("Adobe subscription");
        }

        @Test
        @DisplayName("TC-EXP-035: should handle special characters in search")
        void shouldHandleSpecialCharacters() {
            // Given
            List<Expense> expenses = List.of(
                createExpenseWithDescription("Smith & Sons consultation"),
                createExpenseWithDescription("O'Brien's services"),
                createExpenseWithDescription("Regular service")
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When - search with special characters
            viewModel.setSearchText("&");

            // Then - doesn't break, finds matching expense
            assertThat(viewModel.getFilteredItems()).hasSize(1);

            viewModel.setSearchText("'");
            assertThat(viewModel.getFilteredItems()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Table Data Display - TC-EXP-040 to TC-EXP-052")
    class TableDataDisplay {

        @Test
        @DisplayName("TC-EXP-047: should format date column correctly")
        void shouldFormatTableRowDate() {
            // Given - expense with specific date
            Expense expense = createExpense(ExpenseCategory.OFFICE_COSTS, LocalDate.of(2025, 6, 10));
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of(expense));
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            viewModel.loadExpenses();

            // Then - format: "10 Jun '25"
            ExpenseTableRow row = viewModel.getExpenseItems().get(0);
            assertThat(row.getFormattedDate()).isEqualTo("10 Jun '25");
        }

        @Test
        @DisplayName("TC-EXP-048: should format amount column correctly")
        void shouldFormatTableRowAmount() {
            // Given
            Expense expense = new Expense(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 6, 10),
                new BigDecimal("54.99"),
                "Test expense",
                ExpenseCategory.OFFICE_COSTS,
                null,
                null
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of(expense));
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            viewModel.loadExpenses();

            // Then - formatted with pound sign
            ExpenseTableRow row = viewModel.getExpenseItems().get(0);
            assertThat(row.getFormattedAmount()).isEqualTo("£54.99");
        }

        @Test
        @DisplayName("TC-EXP-050: should show deductible indicator based on category")
        void shouldShowDeductibleIndicator() {
            // Given - deductible and non-deductible expenses
            List<Expense> expenses = List.of(
                createExpense(ExpenseCategory.OFFICE_COSTS),   // deductible
                createExpense(ExpenseCategory.DEPRECIATION)    // non-deductible
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            viewModel.loadExpenses();

            // Then - checkmark for deductible, X for non-deductible
            List<ExpenseTableRow> rows = viewModel.getExpenseItems();
            ExpenseTableRow officeRow = rows.stream()
                .filter(r -> r.category() == ExpenseCategory.OFFICE_COSTS)
                .findFirst()
                .orElseThrow();
            ExpenseTableRow deprecRow = rows.stream()
                .filter(r -> r.category() == ExpenseCategory.DEPRECIATION)
                .findFirst()
                .orElseThrow();

            assertThat(officeRow.deductible()).isTrue();
            assertThat(deprecRow.deductible()).isFalse();
        }

        @Test
        @DisplayName("should convert expenses to table rows")
        void shouldConvertExpensesToTableRows() {
            Expense expense = createExpense(ExpenseCategory.OFFICE_COSTS);
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of(expense));
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            viewModel.loadExpenses();

            assertThat(viewModel.getExpenseItems()).hasSize(1);
            ExpenseTableRow row = viewModel.getExpenseItems().get(0);
            assertThat(row.id()).isEqualTo(expense.id());
            assertThat(row.description()).isEqualTo(expense.description());
        }
    }

    @Nested
    @DisplayName("Category Styling")
    class CategoryStyling {

        @Test
        @DisplayName("TC-EXP-024: should return category display name with SA103 box")
        void shouldReturnCorrectCategoryDisplayName() {
            Expense expense = createExpense(ExpenseCategory.OFFICE_COSTS);
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of(expense));
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            viewModel.loadExpenses();

            ExpenseTableRow row = viewModel.getExpenseItems().get(0);
            assertThat(row.getCategoryDisplayName()).contains("Box");
        }

        @Test
        @DisplayName("should return short category name for table")
        void shouldReturnShortCategoryName() {
            Expense expense = createExpense(ExpenseCategory.OFFICE_COSTS);
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of(expense));
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            viewModel.loadExpenses();

            ExpenseTableRow row = viewModel.getExpenseItems().get(0);
            assertThat(row.getCategoryShortName()).isEqualTo("Office");
        }

        @Test
        @DisplayName("should return category style class for color")
        void shouldReturnCategoryStyleClass() {
            Expense expense = createExpense(ExpenseCategory.OFFICE_COSTS);
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of(expense));
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            viewModel.loadExpenses();

            ExpenseTableRow row = viewModel.getExpenseItems().get(0);
            assertThat(row.getCategoryStyleClass()).isEqualTo("category-office");
        }
    }

    @Nested
    @DisplayName("Pagination Controls - TC-EXP-060 to TC-EXP-066")
    class PaginationControls {

        @Test
        @DisplayName("TC-EXP-060: should paginate with correct page size")
        void shouldPaginateWithCorrectPageSize() {
            // Given - more than default page size
            List<Expense> expenses = createManyExpenses(25);
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.setPageSize(20);

            // When
            viewModel.loadExpenses();

            // Then - first page shows max page size items
            assertThat(viewModel.getCurrentPageItems()).hasSize(20);
        }

        @Test
        @DisplayName("TC-EXP-061: should disable previous button on first page")
        void shouldDisablePrevButtonOnFirstPage() {
            // Given
            List<Expense> expenses = createManyExpenses(25);
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            // When - on first page
            viewModel.loadExpenses();

            // Then - previous not available
            assertThat(viewModel.hasPreviousPage()).isFalse();
        }

        @Test
        @DisplayName("TC-EXP-062: should disable next button on last page")
        void shouldDisableNextButtonOnLastPage() {
            // Given
            List<Expense> expenses = createManyExpenses(25);
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When - navigate to last page
            while (viewModel.hasNextPage()) {
                viewModel.nextPage();
            }

            // Then - next not available
            assertThat(viewModel.hasNextPage()).isFalse();
        }

        @Test
        @DisplayName("TC-EXP-063: should show accurate result count")
        void shouldShowAccurateResultCount() {
            // Given - 45 entries
            List<Expense> expenses = createManyExpenses(45);
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.setPageSize(20);

            // When
            viewModel.loadExpenses();

            // Then - format: "Showing 1-20 of 45 entries"
            assertThat(viewModel.getResultCountText()).contains("1-");
            assertThat(viewModel.getResultCountText()).contains("45");
        }

        @Test
        @DisplayName("TC-EXP-064: should enable prev button when not on first page")
        void shouldEnablePrevButton() {
            List<Expense> expenses = createManyExpenses(25);
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            viewModel.loadExpenses();

            assertThat(viewModel.hasPreviousPage()).isFalse();
            viewModel.nextPage();
            assertThat(viewModel.hasPreviousPage()).isTrue();
        }

        @Test
        @DisplayName("TC-EXP-065: should enable next button when not on last page")
        void shouldEnableNextButton() {
            // Given - 25 items with page size 20 means 2 pages
            List<Expense> expenses = createManyExpenses(25);
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.setPageSize(20);

            viewModel.loadExpenses();

            // On first page with 25 items and page size 20, we have 2 pages
            assertThat(viewModel.hasNextPage()).isTrue();
            viewModel.nextPage();
            // Now on last page (page 2 with 5 items)
            assertThat(viewModel.hasNextPage()).isFalse();
        }
    }

    @Nested
    @DisplayName("Empty State - TC-EXP-070 to TC-EXP-074")
    class EmptyState {

        @Test
        @DisplayName("TC-EXP-070: should show empty state when no expenses")
        void shouldShowEmptyStateWhenNoExpenses() {
            // Given - no expenses
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of());
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            viewModel.loadExpenses();

            // Then - empty state visible
            assertThat(viewModel.isEmptyState()).isTrue();
        }

        @Test
        @DisplayName("TC-EXP-074: should hide empty state when data exists")
        void shouldHideEmptyStateWhenExpensesExist() {
            // Given - expenses exist
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of(createExpense(ExpenseCategory.OFFICE_COSTS)));
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            viewModel.loadExpenses();

            // Then - empty state hidden
            assertThat(viewModel.isEmptyState()).isFalse();
        }

        @Test
        @DisplayName("should show zero entries text when empty")
        void shouldShowZeroEntriesWhenEmpty() {
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of());
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            viewModel.loadExpenses();

            assertThat(viewModel.getResultCountText()).contains("0");
        }
    }

    // === Helper Methods ===

    private Expense createExpense(ExpenseCategory category) {
        return createExpense(category, LocalDate.of(2025, 6, 10));
    }

    private Expense createExpense(ExpenseCategory category, LocalDate date) {
        return new Expense(
            UUID.randomUUID(),
            businessId,
            date,
            new BigDecimal("50.00"),
            "Test expense for " + category.name(),
            category,
            null,
            null
        );
    }

    private Expense createExpenseWithAmount(ExpenseCategory category, String amount) {
        return new Expense(
            UUID.randomUUID(),
            businessId,
            LocalDate.of(2025, 6, 10),
            new BigDecimal(amount),
            "Test expense for " + category.name(),
            category,
            null,
            null
        );
    }

    private Expense createExpenseWithDescription(String description) {
        return new Expense(
            UUID.randomUUID(),
            businessId,
            LocalDate.of(2025, 6, 10),
            new BigDecimal("50.00"),
            description,
            ExpenseCategory.OFFICE_COSTS,
            null,
            null
        );
    }

    private List<Expense> createManyExpenses(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> new Expense(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 6, 1).plusDays(i % 30),
                new BigDecimal("10.00"),
                "Test expense " + i,
                ExpenseCategory.OFFICE_COSTS,
                null,
                null
            ))
            .toList();
    }
}
