package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.common.enums.IncomeStatus;
import uk.selfemploy.core.service.IncomeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD tests for IncomeListViewModel.
 * Tests the ViewModel logic without requiring JavaFX initialization.
 */
@DisplayName("IncomeListViewModel")
class IncomeListViewModelTest {

    @Mock
    private IncomeService incomeService;

    private IncomeListViewModel viewModel;
    private UUID businessId;
    private TaxYear taxYear;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        businessId = UUID.randomUUID();
        taxYear = TaxYear.of(2025);
        viewModel = new IncomeListViewModel(incomeService, businessId);
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("should initialize with empty income list")
        void shouldInitializeWithEmptyIncomeList() {
            assertThat(viewModel.getIncomeItems()).isEmpty();
        }

        @Test
        @DisplayName("should initialize with zero totals")
        void shouldInitializeWithZeroTotals() {
            assertThat(viewModel.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(viewModel.getPaidIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(viewModel.getUnpaidIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should initialize with zero counts")
        void shouldInitializeWithZeroCounts() {
            assertThat(viewModel.getTotalCount()).isZero();
            assertThat(viewModel.getPaidCount()).isZero();
            assertThat(viewModel.getUnpaidCount()).isZero();
        }

        @Test
        @DisplayName("should initialize with no status filter")
        void shouldInitializeWithNoStatusFilter() {
            assertThat(viewModel.getStatusFilter()).isNull();
        }

        @Test
        @DisplayName("should initialize with empty search text")
        void shouldInitializeWithEmptySearchText() {
            assertThat(viewModel.getSearchText()).isEmpty();
        }

        @Test
        @DisplayName("should initialize on first page")
        void shouldInitializeOnFirstPage() {
            assertThat(viewModel.getCurrentPage()).isZero();
        }

        @Test
        @DisplayName("should initialize with default page size of 20")
        void shouldInitializeWithDefaultPageSize() {
            assertThat(viewModel.getPageSize()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("Loading Income")
    class LoadingIncome {

        @Test
        @DisplayName("should load income for tax year")
        void shouldLoadIncomeForTaxYear() {
            // Given
            List<Income> incomes = createSampleIncomes();
            when(incomeService.findByTaxYear(businessId, taxYear)).thenReturn(incomes);

            // When
            viewModel.loadIncome(taxYear);

            // Then
            verify(incomeService).findByTaxYear(businessId, taxYear);
            assertThat(viewModel.getIncomeItems()).hasSize(3);
        }

        @Test
        @DisplayName("should calculate total income correctly")
        void shouldCalculateTotalIncome() {
            // Given
            List<Income> incomes = createSampleIncomes();
            when(incomeService.findByTaxYear(businessId, taxYear)).thenReturn(incomes);

            // When
            viewModel.loadIncome(taxYear);

            // Then
            // Total: 1000 + 2500 + 750 = 4250
            assertThat(viewModel.getTotalIncome()).isEqualByComparingTo(new BigDecimal("4250.00"));
        }

        @Test
        @DisplayName("should calculate paid income correctly")
        void shouldCalculatePaidIncome() {
            // Given - incomes with mixed status
            viewModel.addIncomeRow(createIncomeRow("Client A", new BigDecimal("1000"), IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRow("Client B", new BigDecimal("2500"), IncomeStatus.UNPAID));
            viewModel.addIncomeRow(createIncomeRow("Client C", new BigDecimal("750"), IncomeStatus.PAID));

            // Then
            assertThat(viewModel.getPaidIncome()).isEqualByComparingTo(new BigDecimal("1750.00"));
        }

        @Test
        @DisplayName("should calculate unpaid income correctly")
        void shouldCalculateUnpaidIncome() {
            // Given - incomes with mixed status
            viewModel.addIncomeRow(createIncomeRow("Client A", new BigDecimal("1000"), IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRow("Client B", new BigDecimal("2500"), IncomeStatus.UNPAID));
            viewModel.addIncomeRow(createIncomeRow("Client C", new BigDecimal("750"), IncomeStatus.PAID));

            // Then
            assertThat(viewModel.getUnpaidIncome()).isEqualByComparingTo(new BigDecimal("2500.00"));
        }

        @Test
        @DisplayName("should update counts correctly")
        void shouldUpdateCountsCorrectly() {
            // Given - incomes with mixed status
            viewModel.addIncomeRow(createIncomeRow("Client A", new BigDecimal("1000"), IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRow("Client B", new BigDecimal("2500"), IncomeStatus.UNPAID));
            viewModel.addIncomeRow(createIncomeRow("Client C", new BigDecimal("750"), IncomeStatus.PAID));

            // Then
            assertThat(viewModel.getTotalCount()).isEqualTo(3);
            assertThat(viewModel.getPaidCount()).isEqualTo(2);
            assertThat(viewModel.getUnpaidCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should sort by date descending by default")
        void shouldSortByDateDescending() {
            // Given
            LocalDate date1 = LocalDate.of(2025, 4, 10);
            LocalDate date2 = LocalDate.of(2025, 4, 15);
            LocalDate date3 = LocalDate.of(2025, 4, 12);

            viewModel.addIncomeRow(createIncomeRowWithDate("Client A", new BigDecimal("1000"), date1));
            viewModel.addIncomeRow(createIncomeRowWithDate("Client B", new BigDecimal("2000"), date2));
            viewModel.addIncomeRow(createIncomeRowWithDate("Client C", new BigDecimal("3000"), date3));

            // When
            viewModel.sortByDate(false); // descending

            // Then
            List<IncomeTableRow> items = viewModel.getFilteredItems();
            assertThat(items.get(0).date()).isEqualTo(date2);
            assertThat(items.get(1).date()).isEqualTo(date3);
            assertThat(items.get(2).date()).isEqualTo(date1);
        }

        @Test
        @DisplayName("should handle empty income list")
        void shouldHandleEmptyIncomeList() {
            // Given
            when(incomeService.findByTaxYear(businessId, taxYear)).thenReturn(Collections.emptyList());

            // When
            viewModel.loadIncome(taxYear);

            // Then
            assertThat(viewModel.getIncomeItems()).isEmpty();
            assertThat(viewModel.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Filtering by Status")
    class FilteringByStatus {

        @BeforeEach
        void setUpIncomes() {
            viewModel.addIncomeRow(createIncomeRow("Client A", new BigDecimal("1000"), IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRow("Client B", new BigDecimal("2500"), IncomeStatus.UNPAID));
            viewModel.addIncomeRow(createIncomeRow("Client C", new BigDecimal("750"), IncomeStatus.PAID));
        }

        @Test
        @DisplayName("should filter by paid status")
        void shouldFilterByPaidStatus() {
            // When
            viewModel.setStatusFilter(IncomeStatus.PAID);

            // Then
            List<IncomeTableRow> filtered = viewModel.getFilteredItems();
            assertThat(filtered).hasSize(2);
            assertThat(filtered).allMatch(item -> item.status() == IncomeStatus.PAID);
        }

        @Test
        @DisplayName("should filter by unpaid status")
        void shouldFilterByUnpaidStatus() {
            // When
            viewModel.setStatusFilter(IncomeStatus.UNPAID);

            // Then
            List<IncomeTableRow> filtered = viewModel.getFilteredItems();
            assertThat(filtered).hasSize(1);
            assertThat(filtered).allMatch(item -> item.status() == IncomeStatus.UNPAID);
        }

        @Test
        @DisplayName("should show all when status filter is null")
        void shouldShowAllWhenStatusFilterIsNull() {
            // Given
            viewModel.setStatusFilter(IncomeStatus.PAID);

            // When
            viewModel.setStatusFilter(null);

            // Then
            assertThat(viewModel.getFilteredItems()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Search Functionality")
    class SearchFunctionality {

        @BeforeEach
        void setUpIncomes() {
            viewModel.addIncomeRow(createIncomeRowWithDescription("Acme Corp", "Website design", new BigDecimal("1000")));
            viewModel.addIncomeRow(createIncomeRowWithDescription("TechStart Ltd", "Mobile app development", new BigDecimal("2500")));
            viewModel.addIncomeRow(createIncomeRowWithDescription("Local Bakery", "Logo design", new BigDecimal("750")));
        }

        @Test
        @DisplayName("should search by client name")
        void shouldSearchByClientName() {
            // When
            viewModel.setSearchText("Acme");

            // Then
            List<IncomeTableRow> filtered = viewModel.getFilteredItems();
            assertThat(filtered).hasSize(1);
            assertThat(filtered.get(0).clientName()).isEqualTo("Acme Corp");
        }

        @Test
        @DisplayName("should search by description")
        void shouldSearchByDescription() {
            // When
            viewModel.setSearchText("design");

            // Then
            List<IncomeTableRow> filtered = viewModel.getFilteredItems();
            assertThat(filtered).hasSize(2);
        }

        @Test
        @DisplayName("should be case insensitive")
        void shouldBeCaseInsensitive() {
            // When
            viewModel.setSearchText("ACME");

            // Then
            assertThat(viewModel.getFilteredItems()).hasSize(1);
        }

        @Test
        @DisplayName("should show all when search text is empty")
        void shouldShowAllWhenSearchTextIsEmpty() {
            // Given
            viewModel.setSearchText("Acme");

            // When
            viewModel.setSearchText("");

            // Then
            assertThat(viewModel.getFilteredItems()).hasSize(3);
        }

        @Test
        @DisplayName("should combine search with status filter")
        void shouldCombineSearchWithStatusFilter() {
            // Given
            viewModel.clearAll();
            viewModel.addIncomeRow(createIncomeRowWithDescriptionAndStatus("Acme Corp", "Website design", new BigDecimal("1000"), IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithDescriptionAndStatus("Acme Ltd", "Mobile app", new BigDecimal("2500"), IncomeStatus.UNPAID));
            viewModel.addIncomeRow(createIncomeRowWithDescriptionAndStatus("Local Bakery", "Logo design", new BigDecimal("750"), IncomeStatus.PAID));

            // When
            viewModel.setSearchText("Acme");
            viewModel.setStatusFilter(IncomeStatus.PAID);

            // Then
            List<IncomeTableRow> filtered = viewModel.getFilteredItems();
            assertThat(filtered).hasSize(1);
            assertThat(filtered.get(0).clientName()).isEqualTo("Acme Corp");
        }
    }

    @Nested
    @DisplayName("Pagination")
    class Pagination {

        @BeforeEach
        void setUpManyIncomes() {
            // Add 25 income items
            for (int i = 0; i < 25; i++) {
                viewModel.addIncomeRow(createIncomeRow("Client " + i, new BigDecimal(100 * (i + 1)), IncomeStatus.PAID));
            }
        }

        @Test
        @DisplayName("should return first page of items")
        void shouldReturnFirstPageOfItems() {
            // Given
            viewModel.setPageSize(10);

            // Then
            assertThat(viewModel.getCurrentPageItems()).hasSize(10);
        }

        @Test
        @DisplayName("should navigate to next page")
        void shouldNavigateToNextPage() {
            // Given
            viewModel.setPageSize(10);

            // When
            viewModel.nextPage();

            // Then
            assertThat(viewModel.getCurrentPage()).isEqualTo(1);
            assertThat(viewModel.getCurrentPageItems()).hasSize(10);
        }

        @Test
        @DisplayName("should navigate to previous page")
        void shouldNavigateToPreviousPage() {
            // Given
            viewModel.setPageSize(10);
            viewModel.nextPage();
            viewModel.nextPage();

            // When
            viewModel.previousPage();

            // Then
            assertThat(viewModel.getCurrentPage()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not go below first page")
        void shouldNotGoBelowFirstPage() {
            // When
            viewModel.previousPage();
            viewModel.previousPage();

            // Then
            assertThat(viewModel.getCurrentPage()).isZero();
        }

        @Test
        @DisplayName("should not go beyond last page")
        void shouldNotGoBeyondLastPage() {
            // Given
            viewModel.setPageSize(10);

            // When - try to navigate beyond available pages
            for (int i = 0; i < 10; i++) {
                viewModel.nextPage();
            }

            // Then - should be on the last page (index 2 for 25 items with pageSize 10)
            assertThat(viewModel.getCurrentPage()).isEqualTo(2);
        }

        @Test
        @DisplayName("should calculate total pages correctly")
        void shouldCalculateTotalPagesCorrectly() {
            // Given
            viewModel.setPageSize(10);

            // Then - 25 items / 10 per page = 3 pages
            assertThat(viewModel.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("should reset to first page when filter changes")
        void shouldResetToFirstPageWhenFilterChanges() {
            // Given
            viewModel.setPageSize(10);
            viewModel.nextPage();

            // When
            viewModel.setSearchText("Client 1");

            // Then
            assertThat(viewModel.getCurrentPage()).isZero();
        }

        @Test
        @DisplayName("should handle last page with fewer items")
        void shouldHandleLastPageWithFewerItems() {
            // Given
            viewModel.setPageSize(10);
            viewModel.nextPage();
            viewModel.nextPage();

            // Then - last page should have 5 items (25 - 20)
            assertThat(viewModel.getCurrentPageItems()).hasSize(5);
        }

        @Test
        @DisplayName("should check if can go to previous page")
        void shouldCheckIfCanGoToPreviousPage() {
            // Given - on first page
            assertThat(viewModel.canGoPrevious()).isFalse();

            // When
            viewModel.nextPage();

            // Then
            assertThat(viewModel.canGoPrevious()).isTrue();
        }

        @Test
        @DisplayName("should check if can go to next page")
        void shouldCheckIfCanGoToNextPage() {
            // Given
            viewModel.setPageSize(10);

            // Then - can go next when not on last page
            assertThat(viewModel.canGoNext()).isTrue();

            // When - go to last page
            viewModel.nextPage();
            viewModel.nextPage();

            // Then
            assertThat(viewModel.canGoNext()).isFalse();
        }

        @Test
        @DisplayName("should format result count text correctly")
        void shouldFormatResultCountTextCorrectly() {
            // Given
            viewModel.setPageSize(10);

            // Then - first page
            assertThat(viewModel.getResultCountText()).isEqualTo("Showing 1-10 of 25 entries");

            // When - go to last page
            viewModel.nextPage();
            viewModel.nextPage();

            // Then
            assertThat(viewModel.getResultCountText()).isEqualTo("Showing 21-25 of 25 entries");
        }
    }

    @Nested
    @DisplayName("Currency Formatting")
    class CurrencyFormatting {

        @Test
        @DisplayName("should format total income with GBP symbol")
        void shouldFormatTotalIncomeWithGbpSymbol() {
            // Given
            viewModel.addIncomeRow(createIncomeRow("Client A", new BigDecimal("24500.00"), IncomeStatus.PAID));

            // Then
            assertThat(viewModel.getFormattedTotalIncome()).isEqualTo("£24,500.00");
        }

        @Test
        @DisplayName("should format paid income with GBP symbol")
        void shouldFormatPaidIncomeWithGbpSymbol() {
            // Given
            viewModel.addIncomeRow(createIncomeRow("Client A", new BigDecimal("21300.50"), IncomeStatus.PAID));

            // Then
            assertThat(viewModel.getFormattedPaidIncome()).isEqualTo("£21,300.50");
        }

        @Test
        @DisplayName("should format unpaid income with GBP symbol")
        void shouldFormatUnpaidIncomeWithGbpSymbol() {
            // Given
            viewModel.addIncomeRow(createIncomeRow("Client A", new BigDecimal("3200.00"), IncomeStatus.UNPAID));

            // Then
            assertThat(viewModel.getFormattedUnpaidIncome()).isEqualTo("£3,200.00");
        }

        @Test
        @DisplayName("should format count text correctly")
        void shouldFormatCountTextCorrectly() {
            // Given
            viewModel.addIncomeRow(createIncomeRow("Client A", new BigDecimal("1000"), IncomeStatus.PAID));

            // Then
            assertThat(viewModel.getTotalCountText()).isEqualTo("1 entry");

            // When - add more
            viewModel.addIncomeRow(createIncomeRow("Client B", new BigDecimal("2000"), IncomeStatus.PAID));

            // Then
            assertThat(viewModel.getTotalCountText()).isEqualTo("2 entries");
        }
    }

    @Nested
    @DisplayName("Empty State")
    class EmptyState {

        @Test
        @DisplayName("should show empty state when no income")
        void shouldShowEmptyStateWhenNoIncome() {
            assertThat(viewModel.isEmptyState()).isTrue();
        }

        @Test
        @DisplayName("should hide empty state when has income")
        void shouldHideEmptyStateWhenHasIncome() {
            // Given
            viewModel.addIncomeRow(createIncomeRow("Client A", new BigDecimal("1000"), IncomeStatus.PAID));

            // Then
            assertThat(viewModel.isEmptyState()).isFalse();
        }

        @Test
        @DisplayName("should show no results message when filter returns empty")
        void shouldShowNoResultsMessageWhenFilterReturnsEmpty() {
            // Given
            viewModel.addIncomeRow(createIncomeRow("Client A", new BigDecimal("1000"), IncomeStatus.PAID));

            // When
            viewModel.setSearchText("NonExistentClient");

            // Then
            assertThat(viewModel.getFilteredItems()).isEmpty();
            assertThat(viewModel.isNoResults()).isTrue();
            assertThat(viewModel.isEmptyState()).isFalse();
        }
    }

    @Nested
    @DisplayName("Refresh Functionality")
    class RefreshFunctionality {

        @Test
        @DisplayName("should reload income on refresh")
        void shouldReloadIncomeOnRefresh() {
            // Given
            when(incomeService.findByTaxYear(businessId, taxYear)).thenReturn(Collections.emptyList());
            viewModel.loadIncome(taxYear);

            // When
            List<Income> updatedIncomes = createSampleIncomes();
            when(incomeService.findByTaxYear(businessId, taxYear)).thenReturn(updatedIncomes);
            viewModel.refresh();

            // Then
            verify(incomeService, times(2)).findByTaxYear(businessId, taxYear);
        }

        @Test
        @DisplayName("should preserve filter on refresh")
        void shouldPreserveFilterOnRefresh() {
            // Given
            List<Income> incomes = createSampleIncomes();
            when(incomeService.findByTaxYear(businessId, taxYear)).thenReturn(incomes);
            viewModel.loadIncome(taxYear);
            viewModel.setStatusFilter(IncomeStatus.PAID);
            viewModel.setSearchText("Test");

            // When
            viewModel.refresh();

            // Then
            assertThat(viewModel.getStatusFilter()).isEqualTo(IncomeStatus.PAID);
            assertThat(viewModel.getSearchText()).isEqualTo("Test");
        }
    }

    // === Helper Methods ===

    private List<Income> createSampleIncomes() {
        return Arrays.asList(
            createIncome("Description 1", new BigDecimal("1000.00")),
            createIncome("Description 2", new BigDecimal("2500.00")),
            createIncome("Description 3", new BigDecimal("750.00"))
        );
    }

    private Income createIncome(String description, BigDecimal amount) {
        return new Income(
            UUID.randomUUID(),
            businessId,
            LocalDate.now(),
            amount,
            description,
            IncomeCategory.SALES,
            null
        );
    }

    private IncomeTableRow createIncomeRow(String clientName, BigDecimal amount, IncomeStatus status) {
        return new IncomeTableRow(
            UUID.randomUUID(),
            LocalDate.now(),
            clientName,
            "Test description",
            amount,
            status,
            IncomeCategory.SALES,
            null
        );
    }

    private IncomeTableRow createIncomeRowWithDate(String clientName, BigDecimal amount, LocalDate date) {
        return new IncomeTableRow(
            UUID.randomUUID(),
            date,
            clientName,
            "Test description",
            amount,
            IncomeStatus.PAID,
            IncomeCategory.SALES,
            null
        );
    }

    private IncomeTableRow createIncomeRowWithDescription(String clientName, String description, BigDecimal amount) {
        return new IncomeTableRow(
            UUID.randomUUID(),
            LocalDate.now(),
            clientName,
            description,
            amount,
            IncomeStatus.PAID,
            IncomeCategory.SALES,
            null
        );
    }

    private IncomeTableRow createIncomeRowWithDescriptionAndStatus(String clientName, String description, BigDecimal amount, IncomeStatus status) {
        return new IncomeTableRow(
            UUID.randomUUID(),
            LocalDate.now(),
            clientName,
            description,
            amount,
            status,
            IncomeCategory.SALES,
            null
        );
    }
}
