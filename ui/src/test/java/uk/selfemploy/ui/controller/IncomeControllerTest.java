package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.common.enums.IncomeStatus;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.viewmodel.IncomeListViewModel;
import uk.selfemploy.ui.viewmodel.IncomeTableRow;

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
 * Unit tests for IncomeController.
 * Tests the controller logic for the income list view.
 *
 * Test Coverage (as per Sprint 8 Test Design):
 * - TC-INC-001 to TC-INC-009: Bento Grid Summary Cards
 * - TC-INC-010 to TC-INC-014: Help Icon Click Handlers
 * - TC-INC-020 to TC-INC-025: Status Filter Functionality
 * - TC-INC-030 to TC-INC-034: Search Field
 * - TC-INC-040 to TC-INC-051: Table Interactions
 * - TC-INC-060 to TC-INC-064: Pagination Controls
 * - TC-INC-070 to TC-INC-073: Empty State Display
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IncomeController")
class IncomeControllerTest {

    @Mock
    private IncomeService incomeService;

    private IncomeListViewModel viewModel;
    private TaxYear taxYear;
    private UUID businessId;

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        taxYear = TaxYear.of(2025);
        viewModel = new IncomeListViewModel(incomeService, businessId);
    }

    @Nested
    @DisplayName("Bento Grid Summary Cards - TC-INC-001 to TC-INC-009")
    class BentoGridSummaryCards {

        @Test
        @DisplayName("TC-INC-001: should display total income formatted as currency")
        void shouldDisplayFormattedTotalIncome() {
            // Given - income entries exist via IncomeTableRow (to control status)
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("5000.00", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("3500.00", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("2250.00", IncomeStatus.UNPAID));

            // Then - total card displays sum formatted as currency
            assertThat(viewModel.getFormattedTotalIncome()).isEqualTo("£10,750.00");
        }

        @Test
        @DisplayName("TC-INC-002: should display paid income total")
        void shouldDisplayPaidIncomeTotal() {
            // Given - income entries with mixed status
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("5000.00", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("3500.00", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("2250.00", IncomeStatus.UNPAID));

            // Then - paid card shows sum of paid entries
            assertThat(viewModel.getFormattedPaidIncome()).isEqualTo("£8,500.00");
        }

        @Test
        @DisplayName("TC-INC-003: should display unpaid income total")
        void shouldDisplayUnpaidIncomeTotal() {
            // Given
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("5000.00", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("3500.00", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("2250.00", IncomeStatus.UNPAID));

            // Then - unpaid card shows sum of unpaid entries
            assertThat(viewModel.getFormattedUnpaidIncome()).isEqualTo("£2,250.00");
        }

        @Test
        @DisplayName("TC-INC-004: should display entry counts correctly")
        void shouldDisplayEntryCounts() {
            // Given - 3 entries: 2 paid, 1 unpaid
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("5000.00", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("3500.00", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("2250.00", IncomeStatus.UNPAID));

            // Then - each card shows correct entry count
            assertThat(viewModel.getTotalCount()).isEqualTo(3);
            assertThat(viewModel.getPaidCount()).isEqualTo(2);
            assertThat(viewModel.getUnpaidCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-INC-005: should update summary cards on data refresh")
        void shouldUpdateSummaryOnRefresh() {
            // Given - initial load
            when(incomeService.findByTaxYear(any(), any())).thenReturn(List.of());
            viewModel.loadIncome(taxYear);

            // When - data changes and refresh called
            List<Income> newIncomes = List.of(createIncome());
            when(incomeService.findByTaxYear(any(), any())).thenReturn(newIncomes);
            viewModel.refresh();

            // Then - all summary values update
            assertThat(viewModel.getFormattedTotalIncome()).isEqualTo("£1,000.00");
            assertThat(viewModel.getTotalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-INC-006: should handle zero values correctly")
        void shouldHandleZeroValues() {
            // Given - no income
            when(incomeService.findByTaxYear(any(), any())).thenReturn(List.of());

            // When
            viewModel.loadIncome(taxYear);

            // Then - cards display "£0.00" and count = 0
            assertThat(viewModel.getFormattedTotalIncome()).isEqualTo("£0.00");
            assertThat(viewModel.getFormattedPaidIncome()).isEqualTo("£0.00");
            assertThat(viewModel.getFormattedUnpaidIncome()).isEqualTo("£0.00");
            assertThat(viewModel.getTotalCount()).isZero();
        }

        @Test
        @DisplayName("TC-INC-014: total card should not have help-related assertions")
        void totalCardShouldNotRequireHelpIcon() {
            // This tests that total displays correctly (no special help needed)
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("100.00", IncomeStatus.PAID));

            assertThat(viewModel.getTotalIncome()).isEqualTo(new BigDecimal("100.00"));
        }
    }

    @Nested
    @DisplayName("Status Filter - TC-INC-020 to TC-INC-025")
    class StatusFilterFunctionality {

        @Test
        @DisplayName("TC-INC-020: should default to all status (null filter)")
        void shouldDefaultToAllStatus() {
            // Given
            when(incomeService.findByTaxYear(any(), any())).thenReturn(List.of());
            viewModel.loadIncome(taxYear);

            // Then - default is "All Status" (null)
            assertThat(viewModel.getStatusFilter()).isNull();
        }

        @Test
        @DisplayName("TC-INC-022: should filter to paid entries only")
        void shouldFilterToPaidEntries() {
            // Given - mixed status entries (using addIncomeRow to control status)
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("1000.00", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("2000.00", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("3000.00", IncomeStatus.UNPAID));

            // When - select Paid filter
            viewModel.setStatusFilter(IncomeStatus.PAID);

            // Then - only paid entries displayed
            assertThat(viewModel.getFilteredItems()).hasSize(2);
            assertThat(viewModel.getFilteredItems())
                .allMatch(row -> row.status() == IncomeStatus.PAID);
        }

        @Test
        @DisplayName("TC-INC-023: should filter to unpaid entries only")
        void shouldFilterToUnpaidEntries() {
            // Given (using addIncomeRow to control status)
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("1000.00", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("2000.00", IncomeStatus.UNPAID));
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("3000.00", IncomeStatus.UNPAID));

            // When - select Unpaid filter
            viewModel.setStatusFilter(IncomeStatus.UNPAID);

            // Then - only unpaid entries displayed
            assertThat(viewModel.getFilteredItems()).hasSize(2);
            assertThat(viewModel.getFilteredItems())
                .allMatch(row -> row.status() == IncomeStatus.UNPAID);
        }

        @Test
        @DisplayName("TC-INC-024: should show all entries when filter cleared")
        void shouldShowAllWhenFilterCleared() {
            // Given - filter is set (using addIncomeRow to control status)
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("1000.00", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("2000.00", IncomeStatus.UNPAID));
            viewModel.setStatusFilter(IncomeStatus.PAID);

            // When - select "All Status" (null)
            viewModel.setStatusFilter(null);

            // Then - full list restored
            assertThat(viewModel.getFilteredItems()).hasSize(2);
        }

        @Test
        @DisplayName("TC-INC-025: should update filtered result count")
        void shouldUpdateFilteredResultCount() {
            // Given (using addIncomeRow to control status)
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("1000.00", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("2000.00", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithAmountAndStatus("3000.00", IncomeStatus.UNPAID));

            // When
            viewModel.setStatusFilter(IncomeStatus.PAID);

            // Then - result count reflects filtered items
            assertThat(viewModel.getFilteredItems()).hasSize(2);
            assertThat(viewModel.getResultCountText()).contains("2");
        }
    }

    @Nested
    @DisplayName("Search Field - TC-INC-030 to TC-INC-034")
    class SearchFieldFunctionality {

        @Test
        @DisplayName("TC-INC-030: should filter by client name")
        void shouldFilterByClientName() {
            // Given - incomes with different client names
            viewModel.addIncomeRow(createIncomeRowWithClient("Acme Corp", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithClient("Beta Inc", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithClient("Acme Ltd", IncomeStatus.PAID));

            // When - search for "Acme"
            viewModel.setSearchText("Acme");

            // Then - only Acme clients shown
            assertThat(viewModel.getFilteredItems()).hasSize(2);
            assertThat(viewModel.getFilteredItems())
                .allMatch(row -> row.clientName().contains("Acme"));
        }

        @Test
        @DisplayName("TC-INC-031: should filter by description")
        void shouldFilterByDescription() {
            // Given
            viewModel.addIncomeRow(createIncomeRowWithDescription("Consulting services"));
            viewModel.addIncomeRow(createIncomeRowWithDescription("Software development"));
            viewModel.addIncomeRow(createIncomeRowWithDescription("IT consulting"));

            // When - search for "consulting"
            viewModel.setSearchText("consulting");

            // Then - matches in description
            assertThat(viewModel.getFilteredItems()).hasSize(2);
        }

        @Test
        @DisplayName("TC-INC-032: should search case-insensitively")
        void shouldSearchCaseInsensitively() {
            // Given
            viewModel.addIncomeRow(createIncomeRowWithClient("ACME Corp", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithClient("Beta Inc", IncomeStatus.PAID));

            // When - search lowercase
            viewModel.setSearchText("acme");
            int lowercaseResults = viewModel.getFilteredItems().size();

            // And - search uppercase
            viewModel.setSearchText("ACME");
            int uppercaseResults = viewModel.getFilteredItems().size();

            // Then - same results
            assertThat(lowercaseResults).isEqualTo(uppercaseResults).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-INC-034: should combine search with status filter")
        void shouldCombineSearchWithStatusFilter() {
            // Given
            viewModel.addIncomeRow(createIncomeRowWithClientAndStatus("Acme Corp", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithClientAndStatus("Acme Ltd", IncomeStatus.UNPAID));
            viewModel.addIncomeRow(createIncomeRowWithClientAndStatus("Beta Inc", IncomeStatus.PAID));

            // When - apply both filters
            viewModel.setStatusFilter(IncomeStatus.PAID);
            viewModel.setSearchText("Acme");

            // Then - both filters apply simultaneously
            assertThat(viewModel.getFilteredItems()).hasSize(1);
            assertThat(viewModel.getFilteredItems().get(0).clientName()).isEqualTo("Acme Corp");
        }

        @Test
        @DisplayName("should show all entries when search cleared")
        void shouldShowAllWhenSearchCleared() {
            // Given
            viewModel.addIncomeRow(createIncomeRowWithClient("Acme Corp", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithClient("Beta Inc", IncomeStatus.PAID));
            viewModel.setSearchText("Acme");

            // When - clear search
            viewModel.setSearchText("");

            // Then - full list restored
            assertThat(viewModel.getFilteredItems()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Table Data Display - TC-INC-040 to TC-INC-051")
    class TableDataDisplay {

        @Test
        @DisplayName("TC-INC-048: should format date column correctly")
        void shouldFormatTableRowDate() {
            // Given - income with specific date
            IncomeTableRow row = new IncomeTableRow(
                UUID.randomUUID(),
                LocalDate.of(2025, 6, 10),
                "Test Client",
                "Test description",
                new BigDecimal("1000.00"),
                IncomeStatus.PAID,
                IncomeCategory.SALES,
                null
            );
            viewModel.addIncomeRow(row);

            // Then - format matches expense page
            assertThat(row.getFormattedDate()).isEqualTo("10 Jun '25");
        }

        @Test
        @DisplayName("TC-INC-050: should format amount column correctly")
        void shouldFormatTableRowAmount() {
            // Given
            IncomeTableRow row = new IncomeTableRow(
                UUID.randomUUID(),
                LocalDate.of(2025, 6, 10),
                "Test Client",
                "Test description",
                new BigDecimal("2500.50"),
                IncomeStatus.PAID,
                IncomeCategory.SALES,
                null
            );

            // Then - formatted with pound sign
            assertThat(row.getFormattedAmount()).isEqualTo("£2,500.50");
        }

        @Test
        @DisplayName("TC-INC-051: should show status display")
        void shouldShowStatusDisplay() {
            // Given
            IncomeTableRow paidRow = new IncomeTableRow(
                UUID.randomUUID(),
                LocalDate.of(2025, 6, 10),
                "Client A",
                "Test",
                new BigDecimal("1000.00"),
                IncomeStatus.PAID,
                IncomeCategory.SALES,
                null
            );
            IncomeTableRow unpaidRow = new IncomeTableRow(
                UUID.randomUUID(),
                LocalDate.of(2025, 6, 10),
                "Client B",
                "Test",
                new BigDecimal("1000.00"),
                IncomeStatus.UNPAID,
                IncomeCategory.SALES,
                null
            );

            // Then - status display shows correct text
            assertThat(paidRow.getStatusDisplay()).isEqualTo("Paid");
            assertThat(unpaidRow.getStatusDisplay()).isEqualTo("Unpaid");
        }

        @Test
        @DisplayName("should convert incomes to table rows")
        void shouldConvertIncomesToTableRows() {
            Income income = createIncome();
            when(incomeService.findByTaxYear(any(), any())).thenReturn(List.of(income));

            viewModel.loadIncome(taxYear);

            assertThat(viewModel.getIncomeItems()).hasSize(1);
            IncomeTableRow row = viewModel.getIncomeItems().get(0);
            assertThat(row.id()).isEqualTo(income.id());
        }
    }

    @Nested
    @DisplayName("Sorting")
    class Sorting {

        @Test
        @DisplayName("should sort by date descending by default")
        void shouldSortByDateDescendingByDefault() {
            // Given
            viewModel.addIncomeRow(createIncomeRowWithDate(LocalDate.of(2025, 1, 15)));
            viewModel.addIncomeRow(createIncomeRowWithDate(LocalDate.of(2025, 6, 10)));
            viewModel.addIncomeRow(createIncomeRowWithDate(LocalDate.of(2025, 3, 20)));

            // When - apply filters (triggers sort)
            viewModel.applyFilters();

            // Then - most recent first
            List<IncomeTableRow> items = viewModel.getFilteredItems();
            assertThat(items.get(0).date()).isEqualTo(LocalDate.of(2025, 6, 10));
            assertThat(items.get(2).date()).isEqualTo(LocalDate.of(2025, 1, 15));
        }

        @Test
        @DisplayName("should sort by amount")
        void shouldSortByAmount() {
            // Given
            viewModel.addIncomeRow(createIncomeRowWithAmount("500.00"));
            viewModel.addIncomeRow(createIncomeRowWithAmount("1500.00"));
            viewModel.addIncomeRow(createIncomeRowWithAmount("1000.00"));

            // When
            viewModel.sortByAmount(false); // Descending

            // Then
            List<IncomeTableRow> items = viewModel.getFilteredItems();
            assertThat(items.get(0).amount()).isEqualTo(new BigDecimal("1500.00"));
            assertThat(items.get(2).amount()).isEqualTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("should sort by client name")
        void shouldSortByClientName() {
            // Given
            viewModel.addIncomeRow(createIncomeRowWithClient("Zebra Corp", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithClient("Alpha Inc", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithClient("Beta Ltd", IncomeStatus.PAID));

            // When
            viewModel.sortByClientName(true); // Ascending

            // Then
            List<IncomeTableRow> items = viewModel.getFilteredItems();
            assertThat(items.get(0).clientName()).isEqualTo("Alpha Inc");
            assertThat(items.get(2).clientName()).isEqualTo("Zebra Corp");
        }
    }

    @Nested
    @DisplayName("Pagination Controls - TC-INC-060 to TC-INC-064")
    class PaginationControls {

        @Test
        @DisplayName("TC-INC-060: should paginate with correct page size")
        void shouldPaginateWithCorrectPageSize() {
            // Given - more than default page size
            for (int i = 0; i < 25; i++) {
                viewModel.addIncomeRow(createIncomeRowWithClient("Client " + i, IncomeStatus.PAID));
            }

            // When
            viewModel.applyFilters();

            // Then - first page shows max page size items
            assertThat(viewModel.getCurrentPageItems()).hasSize(10); // Default page size
        }

        @Test
        @DisplayName("TC-INC-061: should disable previous button on first page")
        void shouldDisablePrevButtonOnFirstPage() {
            // Given
            for (int i = 0; i < 25; i++) {
                viewModel.addIncomeRow(createIncomeRowWithClient("Client " + i, IncomeStatus.PAID));
            }
            viewModel.applyFilters();

            // When - on first page
            // Then - previous not available
            assertThat(viewModel.canGoPrevious()).isFalse();
        }

        @Test
        @DisplayName("TC-INC-062: should disable next button on last page")
        void shouldDisableNextButtonOnLastPage() {
            // Given
            for (int i = 0; i < 25; i++) {
                viewModel.addIncomeRow(createIncomeRowWithClient("Client " + i, IncomeStatus.PAID));
            }
            viewModel.applyFilters();

            // When - navigate to last page
            while (viewModel.canGoNext()) {
                viewModel.nextPage();
            }

            // Then - next not available
            assertThat(viewModel.canGoNext()).isFalse();
        }

        @Test
        @DisplayName("TC-INC-063: should show accurate result count")
        void shouldShowAccurateResultCount() {
            // Given - 47 entries
            for (int i = 0; i < 47; i++) {
                viewModel.addIncomeRow(createIncomeRowWithClient("Client " + i, IncomeStatus.PAID));
            }
            viewModel.applyFilters();

            // Then - result count text accurate
            assertThat(viewModel.getResultCountText()).contains("47");
        }

        @Test
        @DisplayName("should enable prev button when not on first page")
        void shouldEnablePrevButton() {
            for (int i = 0; i < 25; i++) {
                viewModel.addIncomeRow(createIncomeRowWithClient("Client " + i, IncomeStatus.PAID));
            }
            viewModel.applyFilters();

            assertThat(viewModel.canGoPrevious()).isFalse();
            viewModel.nextPage();
            assertThat(viewModel.canGoPrevious()).isTrue();
        }

        @Test
        @DisplayName("should navigate pages correctly")
        void shouldNavigatePagesCorrectly() {
            for (int i = 0; i < 25; i++) {
                viewModel.addIncomeRow(createIncomeRowWithClient("Client " + i, IncomeStatus.PAID));
            }
            viewModel.applyFilters();

            assertThat(viewModel.getCurrentPage()).isZero();
            viewModel.nextPage();
            assertThat(viewModel.getCurrentPage()).isEqualTo(1);
            viewModel.previousPage();
            assertThat(viewModel.getCurrentPage()).isZero();
        }
    }

    @Nested
    @DisplayName("Empty State - TC-INC-070 to TC-INC-073")
    class EmptyState {

        @Test
        @DisplayName("TC-INC-070: should show empty state when no income")
        void shouldShowEmptyStateWhenNoIncome() {
            // Given - no income
            when(incomeService.findByTaxYear(any(), any())).thenReturn(List.of());

            // When
            viewModel.loadIncome(taxYear);

            // Then - empty state visible
            assertThat(viewModel.isEmptyState()).isTrue();
        }

        @Test
        @DisplayName("TC-INC-073: should hide empty state when data exists")
        void shouldHideEmptyStateWhenIncomeExists() {
            // Given - income exists
            when(incomeService.findByTaxYear(any(), any())).thenReturn(List.of(createIncome()));

            // When
            viewModel.loadIncome(taxYear);

            // Then - empty state hidden
            assertThat(viewModel.isEmptyState()).isFalse();
        }

        @Test
        @DisplayName("should show no results state when filter returns empty")
        void shouldShowNoResultsWhenFilterReturnsEmpty() {
            // Given - income exists but filtered out
            viewModel.addIncomeRow(createIncomeRowWithClientAndStatus("Client", IncomeStatus.PAID));
            viewModel.setStatusFilter(IncomeStatus.UNPAID);

            // Then - not empty state, but no results
            assertThat(viewModel.isEmptyState()).isFalse();
            assertThat(viewModel.isNoResults()).isTrue();
        }

        @Test
        @DisplayName("should show zero entries text when empty")
        void shouldShowZeroEntriesWhenEmpty() {
            when(incomeService.findByTaxYear(any(), any())).thenReturn(List.of());
            viewModel.loadIncome(taxYear);

            assertThat(viewModel.getResultCountText()).contains("0");
        }
    }

    @Nested
    @DisplayName("Entry Count Text Formatting")
    class EntryCountTextFormatting {

        @Test
        @DisplayName("should format single entry correctly")
        void shouldFormatSingleEntry() {
            viewModel.addIncomeRow(createIncomeRowWithClient("Client", IncomeStatus.PAID));

            assertThat(viewModel.getTotalCountText()).isEqualTo("1 entry");
        }

        @Test
        @DisplayName("should format multiple entries correctly")
        void shouldFormatMultipleEntries() {
            viewModel.addIncomeRow(createIncomeRowWithClient("Client 1", IncomeStatus.PAID));
            viewModel.addIncomeRow(createIncomeRowWithClient("Client 2", IncomeStatus.PAID));

            assertThat(viewModel.getTotalCountText()).isEqualTo("2 entries");
        }

        @Test
        @DisplayName("should format paid count text correctly")
        void shouldFormatPaidCountText() {
            viewModel.addIncomeRow(createIncomeRowWithClientAndStatus("Client", IncomeStatus.PAID));

            assertThat(viewModel.getPaidCountText()).isEqualTo("1 entry");
        }

        @Test
        @DisplayName("should format unpaid count text correctly")
        void shouldFormatUnpaidCountText() {
            viewModel.addIncomeRow(createIncomeRowWithClientAndStatus("Client 1", IncomeStatus.UNPAID));
            viewModel.addIncomeRow(createIncomeRowWithClientAndStatus("Client 2", IncomeStatus.UNPAID));

            assertThat(viewModel.getUnpaidCountText()).isEqualTo("2 entries");
        }
    }

    @Nested
    @DisplayName("Data Refresh")
    class DataRefresh {

        @Test
        @DisplayName("should call service on refresh")
        void shouldCallServiceOnRefresh() {
            when(incomeService.findByTaxYear(any(), any())).thenReturn(List.of());
            viewModel.loadIncome(taxYear);

            viewModel.refresh();

            // loadIncome calls findByTaxYear, refresh calls loadIncome again = 2 total calls
            verify(incomeService, org.mockito.Mockito.times(2)).findByTaxYear(businessId, taxYear);
        }

        @Test
        @DisplayName("should clear and reload on refresh")
        void shouldClearAndReloadOnRefresh() {
            // Given - initial data
            List<Income> initialIncomes = List.of(createIncome());
            when(incomeService.findByTaxYear(any(), any())).thenReturn(initialIncomes);
            viewModel.loadIncome(taxYear);
            assertThat(viewModel.getIncomeItems()).hasSize(1);

            // When - refresh with new data
            List<Income> newIncomes = List.of(createIncome(), createIncome());
            when(incomeService.findByTaxYear(any(), any())).thenReturn(newIncomes);
            viewModel.refresh();

            // Then - data updated
            assertThat(viewModel.getIncomeItems()).hasSize(2);
        }
    }

    // === Helper Methods ===

    private Income createIncome() {
        return new Income(
            UUID.randomUUID(),
            businessId,
            LocalDate.of(2025, 6, 10),
            new BigDecimal("1000.00"),
            "Test income",
            IncomeCategory.SALES,
            null,
            null,
            null,
            null,
            null
        );
    }

    private Income createIncomeWithAmountAndStatus(String amount, IncomeStatus status) {
        // Note: Income domain doesn't have status field, so we create via IncomeTableRow
        // For this test, we simulate by having the ViewModel handle status
        return new Income(
            UUID.randomUUID(),
            businessId,
            LocalDate.of(2025, 6, 10),
            new BigDecimal(amount),
            "Client payment - " + status.name(),
            IncomeCategory.SALES,
            null,
            null,
            null,
            null,
            null
        );
    }

    private IncomeTableRow createIncomeRowWithClient(String clientName, IncomeStatus status) {
        return new IncomeTableRow(
            UUID.randomUUID(),
            LocalDate.of(2025, 6, 10),
            clientName,
            "Test description",
            new BigDecimal("1000.00"),
            status,
            IncomeCategory.SALES,
            null
        );
    }

    private IncomeTableRow createIncomeRowWithClientAndStatus(String clientName, IncomeStatus status) {
        return createIncomeRowWithClient(clientName, status);
    }

    private IncomeTableRow createIncomeRowWithDescription(String description) {
        return new IncomeTableRow(
            UUID.randomUUID(),
            LocalDate.of(2025, 6, 10),
            "Test Client",
            description,
            new BigDecimal("1000.00"),
            IncomeStatus.PAID,
            IncomeCategory.SALES,
            null
        );
    }

    private IncomeTableRow createIncomeRowWithDate(LocalDate date) {
        return new IncomeTableRow(
            UUID.randomUUID(),
            date,
            "Test Client",
            "Test description",
            new BigDecimal("1000.00"),
            IncomeStatus.PAID,
            IncomeCategory.SALES,
            null
        );
    }

    private IncomeTableRow createIncomeRowWithAmount(String amount) {
        return new IncomeTableRow(
            UUID.randomUUID(),
            LocalDate.of(2025, 6, 10),
            "Test Client",
            "Test description",
            new BigDecimal(amount),
            IncomeStatus.PAID,
            IncomeCategory.SALES,
            null
        );
    }

    private IncomeTableRow createIncomeRowWithAmountAndStatus(String amount, IncomeStatus status) {
        return new IncomeTableRow(
            UUID.randomUUID(),
            LocalDate.of(2025, 6, 10),
            "Test Client",
            "Test description",
            new BigDecimal(amount),
            status,
            IncomeCategory.SALES,
            null
        );
    }
}
