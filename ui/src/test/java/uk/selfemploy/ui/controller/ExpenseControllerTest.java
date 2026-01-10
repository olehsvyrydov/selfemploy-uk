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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ExpenseController.
 * Tests the controller logic for the expense list view.
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
    @DisplayName("Data Display")
    class DataDisplay {

        @Test
        @DisplayName("should display formatted summary values")
        void shouldDisplayFormattedSummaryValues() {
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of());
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(new BigDecimal("8230.50"));
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(new BigDecimal("7980.00"));

            viewModel.loadExpenses();

            assertThat(viewModel.getFormattedTotalExpenses()).isEqualTo("£8,230.50");
            assertThat(viewModel.getFormattedDeductibleTotal()).isEqualTo("£7,980.00");
            assertThat(viewModel.getFormattedNonDeductibleTotal()).isEqualTo("£250.50");
        }

        @Test
        @DisplayName("should format entry counts")
        void shouldFormatEntryCounts() {
            List<Expense> expenses = List.of(
                createExpense(ExpenseCategory.OFFICE_COSTS),
                createExpense(ExpenseCategory.TRAVEL),
                createExpense(ExpenseCategory.DEPRECIATION)
            );

            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            viewModel.loadExpenses();

            assertThat(viewModel.getTotalCount()).isEqualTo(3);
            assertThat(viewModel.getDeductibleCount()).isEqualTo(2); // Office and Travel
            assertThat(viewModel.getNonDeductibleCount()).isEqualTo(1); // Depreciation
        }
    }

    @Nested
    @DisplayName("Table Data")
    class TableData {

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

        @Test
        @DisplayName("should format table row date correctly")
        void shouldFormatTableRowDate() {
            Expense expense = createExpense(ExpenseCategory.OFFICE_COSTS, LocalDate.of(2025, 6, 10));
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of(expense));
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            viewModel.loadExpenses();

            ExpenseTableRow row = viewModel.getExpenseItems().get(0);
            assertThat(row.getFormattedDate()).isEqualTo("10 Jun '25");
        }

        @Test
        @DisplayName("should format table row amount correctly")
        void shouldFormatTableRowAmount() {
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

            viewModel.loadExpenses();

            ExpenseTableRow row = viewModel.getExpenseItems().get(0);
            assertThat(row.getFormattedAmount()).isEqualTo("£54.99");
        }

        @Test
        @DisplayName("should show deductible indicator based on category")
        void shouldShowDeductibleIndicator() {
            List<Expense> expenses = List.of(
                createExpense(ExpenseCategory.OFFICE_COSTS),
                createExpense(ExpenseCategory.DEPRECIATION)
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            viewModel.loadExpenses();

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
    }

    @Nested
    @DisplayName("Category Styling")
    class CategoryStyling {

        @Test
        @DisplayName("should return correct category display name")
        void shouldReturnCorrectCategoryDisplayName() {
            Expense expense = createExpense(ExpenseCategory.OFFICE_COSTS);
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of(expense));
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            viewModel.loadExpenses();

            ExpenseTableRow row = viewModel.getExpenseItems().get(0);
            assertThat(row.getCategoryDisplayName()).contains("Box 23");
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
    @DisplayName("Filtering")
    class Filtering {

        @Test
        @DisplayName("should update table when category filter changes")
        void shouldUpdateTableWhenCategoryFilterChanges() {
            List<Expense> expenses = List.of(
                createExpense(ExpenseCategory.OFFICE_COSTS),
                createExpense(ExpenseCategory.TRAVEL)
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            viewModel.loadExpenses();
            viewModel.setSelectedCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.getFilteredItems()).hasSize(1);
            assertThat(viewModel.getFilteredItems().get(0).category()).isEqualTo(ExpenseCategory.OFFICE_COSTS);
        }

        @Test
        @DisplayName("should update table when search text changes")
        void shouldUpdateTableWhenSearchTextChanges() {
            List<Expense> expenses = List.of(
                new Expense(UUID.randomUUID(), businessId, LocalDate.of(2025, 6, 10),
                    new BigDecimal("50.00"), "Adobe subscription", ExpenseCategory.OFFICE_COSTS, null, null),
                new Expense(UUID.randomUUID(), businessId, LocalDate.of(2025, 6, 10),
                    new BigDecimal("50.00"), "Train ticket", ExpenseCategory.TRAVEL, null, null)
            );
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            viewModel.loadExpenses();
            viewModel.setSearchText("Adobe");

            assertThat(viewModel.getFilteredItems()).hasSize(1);
            assertThat(viewModel.getFilteredItems().get(0).description()).contains("Adobe");
        }
    }

    @Nested
    @DisplayName("Empty State")
    class EmptyState {

        @Test
        @DisplayName("should show empty state when no expenses")
        void shouldShowEmptyStateWhenNoExpenses() {
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of());
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            viewModel.loadExpenses();

            assertThat(viewModel.isEmptyState()).isTrue();
        }

        @Test
        @DisplayName("should hide empty state when expenses exist")
        void shouldHideEmptyStateWhenExpensesExist() {
            when(expenseService.findByTaxYear(any(), any())).thenReturn(List.of(createExpense(ExpenseCategory.OFFICE_COSTS)));
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            viewModel.loadExpenses();

            assertThat(viewModel.isEmptyState()).isFalse();
        }
    }

    @Nested
    @DisplayName("Pagination Controls")
    class PaginationControls {

        @Test
        @DisplayName("should enable prev button when not on first page")
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
        @DisplayName("should enable next button when not on last page")
        void shouldEnableNextButton() {
            List<Expense> expenses = createManyExpenses(25);
            when(expenseService.findByTaxYear(any(), any())).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            viewModel.loadExpenses();

            assertThat(viewModel.hasNextPage()).isTrue();
            viewModel.nextPage();
            assertThat(viewModel.hasNextPage()).isFalse();
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

    private List<Expense> createManyExpenses(int count) {
        return java.util.stream.IntStream.range(0, count)
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
