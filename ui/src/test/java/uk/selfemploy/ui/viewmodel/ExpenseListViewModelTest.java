package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.service.ExpenseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TDD tests for ExpenseListViewModel.
 * Tests the ViewModel logic for the expense list view.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseListViewModel")
@Tag("e2e") // Uses JavaFX properties - exclude from headless CI
class ExpenseListViewModelTest {

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
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("should initialize with zero totals")
        void shouldInitializeWithZeroTotals() {
            assertThat(viewModel.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(viewModel.getDeductibleTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(viewModel.getNonDeductibleTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should initialize with zero counts")
        void shouldInitializeWithZeroCounts() {
            assertThat(viewModel.getTotalCount()).isZero();
            assertThat(viewModel.getDeductibleCount()).isZero();
            assertThat(viewModel.getNonDeductibleCount()).isZero();
        }

        @Test
        @DisplayName("should initialize with empty expense list")
        void shouldInitializeWithEmptyList() {
            assertThat(viewModel.getExpenseItems()).isEmpty();
        }

        @Test
        @DisplayName("should initialize with no category filter")
        void shouldInitializeWithNoCategoryFilter() {
            assertThat(viewModel.getSelectedCategory()).isNull();
        }

        @Test
        @DisplayName("should initialize with empty search text")
        void shouldInitializeWithEmptySearchText() {
            assertThat(viewModel.getSearchText()).isEmpty();
        }

        @Test
        @DisplayName("should initialize at first page")
        void shouldInitializeAtFirstPage() {
            assertThat(viewModel.getCurrentPage()).isZero();
        }

        @Test
        @DisplayName("should initialize with default page size of 20")
        void shouldInitializeWithDefaultPageSize() {
            assertThat(viewModel.getPageSize()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("Loading Expenses")
    class LoadingExpenses {

        @Test
        @DisplayName("should load expenses from service for tax year")
        void shouldLoadExpensesFromService() {
            // Given
            List<Expense> expenses = createSampleExpenses();
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(eq(businessId), eq(taxYear)))
                .thenReturn(new BigDecimal("350.00"));
            when(expenseService.getDeductibleTotal(eq(businessId), eq(taxYear)))
                .thenReturn(new BigDecimal("300.00"));

            // When
            viewModel.loadExpenses();

            // Then
            verify(expenseService).findByTaxYear(businessId, taxYear);
            assertThat(viewModel.getExpenseItems()).hasSize(3);
        }

        @Test
        @DisplayName("should calculate totals correctly")
        void shouldCalculateTotalsCorrectly() {
            // Given
            List<Expense> expenses = createSampleExpenses();
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(eq(businessId), eq(taxYear)))
                .thenReturn(new BigDecimal("350.00"));
            when(expenseService.getDeductibleTotal(eq(businessId), eq(taxYear)))
                .thenReturn(new BigDecimal("300.00"));

            // When
            viewModel.loadExpenses();

            // Then
            assertThat(viewModel.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("350.00"));
            assertThat(viewModel.getDeductibleTotal()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(viewModel.getNonDeductibleTotal()).isEqualByComparingTo(new BigDecimal("50.00"));
        }

        @Test
        @DisplayName("should count entries correctly")
        void shouldCountEntriesCorrectly() {
            // Given
            List<Expense> expenses = createSampleExpenses();
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(eq(businessId), eq(taxYear)))
                .thenReturn(new BigDecimal("350.00"));
            when(expenseService.getDeductibleTotal(eq(businessId), eq(taxYear)))
                .thenReturn(new BigDecimal("300.00"));

            // When
            viewModel.loadExpenses();

            // Then
            assertThat(viewModel.getTotalCount()).isEqualTo(3);
            assertThat(viewModel.getDeductibleCount()).isEqualTo(2);
            assertThat(viewModel.getNonDeductibleCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should sort expenses by date descending by default")
        void shouldSortByDateDescending() {
            // Given
            List<Expense> expenses = createSampleExpenses();
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            viewModel.loadExpenses();

            // Then
            List<ExpenseTableRow> items = viewModel.getExpenseItems();
            assertThat(items.get(0).date()).isAfterOrEqualTo(items.get(1).date());
            assertThat(items.get(1).date()).isAfterOrEqualTo(items.get(2).date());
        }

        @Test
        @DisplayName("should handle empty expense list")
        void shouldHandleEmptyList() {
            // Given
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(Collections.emptyList());
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            viewModel.loadExpenses();

            // Then
            assertThat(viewModel.getExpenseItems()).isEmpty();
            assertThat(viewModel.isEmptyState()).isTrue();
        }
    }

    @Nested
    @DisplayName("Category Filtering")
    class CategoryFiltering {

        @Test
        @DisplayName("should filter expenses by selected category")
        void shouldFilterByCategory() {
            // Given
            List<Expense> expenses = createSampleExpenses();
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When
            viewModel.setSelectedCategory(ExpenseCategory.OFFICE_COSTS);

            // Then
            assertThat(viewModel.getFilteredItems()).hasSize(1);
            assertThat(viewModel.getFilteredItems().get(0).category()).isEqualTo(ExpenseCategory.OFFICE_COSTS);
        }

        @Test
        @DisplayName("should show all expenses when category is null")
        void shouldShowAllWhenNoCategoryFilter() {
            // Given
            List<Expense> expenses = createSampleExpenses();
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();
            viewModel.setSelectedCategory(ExpenseCategory.OFFICE_COSTS);

            // When
            viewModel.setSelectedCategory(null);

            // Then
            assertThat(viewModel.getFilteredItems()).hasSize(3);
        }

        @Test
        @DisplayName("should reset to first page when category changes")
        void shouldResetPageOnCategoryChange() {
            // Given
            List<Expense> expenses = createManyExpenses(30);
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();
            viewModel.nextPage();
            assertThat(viewModel.getCurrentPage()).isEqualTo(1);

            // When
            viewModel.setSelectedCategory(ExpenseCategory.OFFICE_COSTS);

            // Then
            assertThat(viewModel.getCurrentPage()).isZero();
        }
    }

    @Nested
    @DisplayName("Search Filtering")
    class SearchFiltering {

        @Test
        @DisplayName("should filter expenses by description search")
        void shouldFilterByDescription() {
            // Given
            List<Expense> expenses = createSampleExpenses();
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When
            viewModel.setSearchText("Adobe");

            // Then
            assertThat(viewModel.getFilteredItems()).hasSize(1);
            assertThat(viewModel.getFilteredItems().get(0).description()).contains("Adobe");
        }

        @Test
        @DisplayName("should be case-insensitive search")
        void shouldBeCaseInsensitive() {
            // Given
            List<Expense> expenses = createSampleExpenses();
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When
            viewModel.setSearchText("adobe");

            // Then
            assertThat(viewModel.getFilteredItems()).hasSize(1);
        }

        @Test
        @DisplayName("should combine search with category filter")
        void shouldCombineSearchAndCategoryFilter() {
            // Given
            List<Expense> expenses = createSampleExpenses();
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When
            viewModel.setSelectedCategory(ExpenseCategory.OFFICE_COSTS);
            viewModel.setSearchText("Adobe");

            // Then
            assertThat(viewModel.getFilteredItems()).hasSize(1);
        }

        @Test
        @DisplayName("should return empty when no matches")
        void shouldReturnEmptyOnNoMatch() {
            // Given
            List<Expense> expenses = createSampleExpenses();
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When
            viewModel.setSearchText("NonExistent");

            // Then
            assertThat(viewModel.getFilteredItems()).isEmpty();
        }

        @Test
        @DisplayName("should reset to first page when search changes")
        void shouldResetPageOnSearchChange() {
            // Given
            List<Expense> expenses = createManyExpenses(30);
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();
            viewModel.nextPage();

            // When
            viewModel.setSearchText("test");

            // Then
            assertThat(viewModel.getCurrentPage()).isZero();
        }
    }

    @Nested
    @DisplayName("Pagination")
    class Pagination {

        @Test
        @DisplayName("should paginate results correctly")
        void shouldPaginateResults() {
            // Given
            List<Expense> expenses = createManyExpenses(25);
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // Then - first page
            assertThat(viewModel.getCurrentPageItems()).hasSize(20);
            assertThat(viewModel.getCurrentPage()).isZero();
            assertThat(viewModel.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("should navigate to next page")
        void shouldNavigateToNextPage() {
            // Given
            List<Expense> expenses = createManyExpenses(25);
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When
            viewModel.nextPage();

            // Then
            assertThat(viewModel.getCurrentPage()).isEqualTo(1);
            assertThat(viewModel.getCurrentPageItems()).hasSize(5);
        }

        @Test
        @DisplayName("should navigate to previous page")
        void shouldNavigateToPreviousPage() {
            // Given
            List<Expense> expenses = createManyExpenses(25);
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();
            viewModel.nextPage();

            // When
            viewModel.previousPage();

            // Then
            assertThat(viewModel.getCurrentPage()).isZero();
        }

        @Test
        @DisplayName("should not go before first page")
        void shouldNotGoBelowZero() {
            // Given
            List<Expense> expenses = createManyExpenses(25);
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // When
            viewModel.previousPage();

            // Then
            assertThat(viewModel.getCurrentPage()).isZero();
        }

        @Test
        @DisplayName("should not go past last page")
        void shouldNotGoAboveLast() {
            // Given
            List<Expense> expenses = createManyExpenses(25);
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();
            viewModel.nextPage();

            // When
            viewModel.nextPage();

            // Then
            assertThat(viewModel.getCurrentPage()).isEqualTo(1);
        }

        @Test
        @DisplayName("should indicate if has next page")
        void shouldIndicateHasNextPage() {
            // Given
            List<Expense> expenses = createManyExpenses(25);
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // Then
            assertThat(viewModel.hasNextPage()).isTrue();
            viewModel.nextPage();
            assertThat(viewModel.hasNextPage()).isFalse();
        }

        @Test
        @DisplayName("should indicate if has previous page")
        void shouldIndicateHasPreviousPage() {
            // Given
            List<Expense> expenses = createManyExpenses(25);
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // Then
            assertThat(viewModel.hasPreviousPage()).isFalse();
            viewModel.nextPage();
            assertThat(viewModel.hasPreviousPage()).isTrue();
        }

        @Test
        @DisplayName("should format result count text correctly")
        void shouldFormatResultCountText() {
            // Given
            List<Expense> expenses = createManyExpenses(25);
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(expenses);
            when(expenseService.getTotalByTaxYear(any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // Then
            assertThat(viewModel.getResultCountText()).isEqualTo("Showing 1-20 of 25 entries");
            viewModel.nextPage();
            assertThat(viewModel.getResultCountText()).isEqualTo("Showing 21-25 of 25 entries");
        }
    }

    @Nested
    @DisplayName("Currency Formatting")
    class CurrencyFormatting {

        @Test
        @DisplayName("should format total expenses with GBP symbol")
        void shouldFormatTotalExpenses() {
            // Given
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(Collections.emptyList());
            when(expenseService.getTotalByTaxYear(eq(businessId), eq(taxYear)))
                .thenReturn(new BigDecimal("8230.50"));
            when(expenseService.getDeductibleTotal(any(), any())).thenReturn(BigDecimal.ZERO);
            viewModel.loadExpenses();

            // Then
            assertThat(viewModel.getFormattedTotalExpenses()).isEqualTo("£8,230.50");
        }

        @Test
        @DisplayName("should format deductible total with GBP symbol")
        void shouldFormatDeductibleTotal() {
            // Given
            when(expenseService.findByTaxYear(eq(businessId), eq(taxYear))).thenReturn(Collections.emptyList());
            when(expenseService.getTotalByTaxYear(eq(businessId), eq(taxYear)))
                .thenReturn(new BigDecimal("8000.00"));
            when(expenseService.getDeductibleTotal(eq(businessId), eq(taxYear)))
                .thenReturn(new BigDecimal("7500.00"));
            viewModel.loadExpenses();

            // Then
            assertThat(viewModel.getFormattedDeductibleTotal()).isEqualTo("£7,500.00");
        }

        @Test
        @DisplayName("should format zero as £0.00")
        void shouldFormatZeroCorrectly() {
            assertThat(viewModel.getFormattedTotalExpenses()).isEqualTo("£0.00");
        }
    }

    @Nested
    @DisplayName("Available Categories")
    class AvailableCategories {

        @Test
        @DisplayName("should return all non-CIS categories when not CIS business")
        void shouldReturnNonCisCategories() {
            // Given
            viewModel.setCisBusiness(false);

            // When
            List<ExpenseCategory> categories = viewModel.getAvailableCategories();

            // Then
            assertThat(categories).doesNotContain(ExpenseCategory.SUBCONTRACTOR_COSTS);
            assertThat(categories).contains(ExpenseCategory.OFFICE_COSTS, ExpenseCategory.TRAVEL);
        }

        @Test
        @DisplayName("should include CIS categories when CIS business")
        void shouldIncludeCisCategoriesForCisBusiness() {
            // Given
            viewModel.setCisBusiness(true);

            // When
            List<ExpenseCategory> categories = viewModel.getAvailableCategories();

            // Then
            assertThat(categories).contains(ExpenseCategory.SUBCONTRACTOR_COSTS);
        }
    }

    // === Helper Methods ===

    private List<Expense> createSampleExpenses() {
        LocalDate date1 = LocalDate.of(2025, 6, 10);
        LocalDate date2 = LocalDate.of(2025, 6, 8);
        LocalDate date3 = LocalDate.of(2025, 6, 5);

        return Arrays.asList(
            new Expense(UUID.randomUUID(), businessId, date1, new BigDecimal("54.99"),
                "Adobe Creative Cloud", ExpenseCategory.OFFICE_COSTS, null, null, null, null, null),
            new Expense(UUID.randomUUID(), businessId, date2, new BigDecimal("45.00"),
                "Train to client meeting", ExpenseCategory.TRAVEL, null, null, null, null, null),
            new Expense(UUID.randomUUID(), businessId, date3, new BigDecimal("250.00"),
                "Equipment depreciation", ExpenseCategory.DEPRECIATION, null, null, null, null, null)
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
                null,
                null,
                null,
                null
            ))
            .toList();
    }
}
