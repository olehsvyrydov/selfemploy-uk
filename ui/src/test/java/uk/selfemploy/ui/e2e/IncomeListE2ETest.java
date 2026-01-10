package uk.selfemploy.ui.e2e;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import uk.selfemploy.ui.e2e.page.IncomePage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Tests for SE-201: Income List View.
 * Based on QA test specification from /rob (rob-qa-sprint-2.md).
 *
 * @see docs/sprints/sprint-2/testing/rob-qa-sprint-2.md
 */
@Tag("e2e")
@DisplayName("SE-201: Income List View E2E")
class IncomeListE2ETest extends BaseE2ETest {

    private IncomePage incomePage;

    @BeforeEach
    void setupIncomePage() {
        incomePage = new IncomePage(this);
        incomePage.navigateTo();
    }

    // === TC-201-01: Income List Navigation (P0) ===

    @Nested
    @DisplayName("TC-201-01: Income List Navigation")
    class NavigationTests {

        @Test
        @DisplayName("TC-201-01a: Navigate to Income List from sidebar")
        void navigateToIncomeListFromSidebar() {
            // Step 1: Verify page title
            assertThat(incomePage.hasPageTitle("Income")).isTrue();

            // Step 2: Verify Income nav button is selected
            ToggleButton incomeBtn = lookup("#navIncome").queryAs(ToggleButton.class);
            assertThat(incomeBtn.isSelected()).isTrue();
        }

        @Test
        @DisplayName("TC-201-01b: Income page has correct layout")
        void incomePageHasCorrectLayout() {
            // Verify key elements exist
            assertThat(lookup("#incomeTable").tryQuery()).isPresent();
            assertThat(lookup("#addIncomeBtn").tryQuery()).isPresent();
            assertThat(lookup("#statusFilter").tryQuery()).isPresent();
            assertThat(lookup("#searchField").tryQuery()).isPresent();
        }
    }

    // === TC-201-02: Summary Cards Display (P0) ===

    @Nested
    @DisplayName("TC-201-02: Summary Cards Display")
    class SummaryCardsTests {

        @Test
        @DisplayName("TC-201-02a: Summary cards show total income")
        void summaryCardsShowTotalIncome() {
            // Verify total value exists and has correct format
            String totalValue = incomePage.getTotalIncome();
            assertThat(totalValue).matches("£[\\d,]+\\.\\d{2}");
        }

        @Test
        @DisplayName("TC-201-02b: Summary cards show paid income")
        void summaryCardsShowPaidIncome() {
            String paidValue = incomePage.getPaidIncome();
            assertThat(paidValue).matches("£[\\d,]+\\.\\d{2}");
        }

        @Test
        @DisplayName("TC-201-02c: Summary cards show unpaid income")
        void summaryCardsShowUnpaidIncome() {
            String unpaidValue = incomePage.getUnpaidIncome();
            assertThat(unpaidValue).matches("£[\\d,]+\\.\\d{2}");
        }

        @Test
        @DisplayName("TC-201-02d: Summary cards show entry counts")
        void summaryCardsShowEntryCounts() {
            String totalCount = incomePage.getTotalCount();
            String paidCount = incomePage.getPaidCount();
            String unpaidCount = incomePage.getUnpaidCount();

            assertThat(totalCount).containsPattern("\\d+ entr(y|ies)");
            assertThat(paidCount).containsPattern("\\d+ entr(y|ies)");
            assertThat(unpaidCount).containsPattern("\\d+ entr(y|ies)");
        }
    }

    // === TC-201-03: Income Table Display (P0) ===

    @Nested
    @DisplayName("TC-201-03: Income Table Display")
    class TableDisplayTests {

        @Test
        @DisplayName("TC-201-03a: Table has required columns")
        void tableHasRequiredColumns() {
            @SuppressWarnings("unchecked")
            TableView<Object> table = lookup("#incomeTable").queryAs(TableView.class);

            // Verify columns
            assertThat(table.getColumns()).hasSize(5);

            // Verify column headers
            assertThat(table.getColumns().get(0).getText()).isEqualTo("DATE");
            assertThat(table.getColumns().get(1).getText()).isEqualTo("CLIENT NAME");
            assertThat(table.getColumns().get(2).getText()).isEqualTo("DESCRIPTION");
            assertThat(table.getColumns().get(3).getText()).isEqualTo("AMOUNT");
            assertThat(table.getColumns().get(4).getText()).isEqualTo("STATUS");
        }

        @Test
        @DisplayName("TC-201-03b: Empty state displayed when no data")
        void emptyStateDisplayedWhenNoData() {
            // With no test data, table should be empty
            // Verify either table is empty or empty state is shown
            if (incomePage.isTableEmpty()) {
                VBox emptyState = lookup("#emptyState").queryAs(VBox.class);
                assertThat(emptyState).isNotNull();
            }
        }
    }

    // === TC-201-04: Status Filter (P1) ===

    @Nested
    @DisplayName("TC-201-04: Status Filter")
    class StatusFilterTests {

        @Test
        @DisplayName("TC-201-04a: Status filter has correct options")
        void statusFilterHasCorrectOptions() {
            @SuppressWarnings("unchecked")
            ComboBox<String> statusFilter = lookup("#statusFilter").queryAs(ComboBox.class);

            assertThat(statusFilter.getItems()).contains("All Status", "Paid", "Unpaid");
        }

        @Test
        @DisplayName("TC-201-04b: Default filter is 'All Status'")
        void defaultFilterIsAllStatus() {
            @SuppressWarnings("unchecked")
            ComboBox<String> statusFilter = lookup("#statusFilter").queryAs(ComboBox.class);

            assertThat(statusFilter.getValue()).isEqualTo("All Status");
        }
    }

    // === TC-201-05: Search Functionality (P1) ===

    @Nested
    @DisplayName("TC-201-05: Search Functionality")
    class SearchTests {

        @Test
        @DisplayName("TC-201-05a: Search field has placeholder text")
        void searchFieldHasPlaceholderText() {
            TextField searchField = lookup("#searchField").queryAs(TextField.class);

            assertThat(searchField.getPromptText()).contains("Search");
        }

        @Test
        @DisplayName("TC-201-05b: Search field accepts text input")
        void searchFieldAcceptsTextInput() {
            // Enter some text
            incomePage.searchFor("test");

            TextField searchField = lookup("#searchField").queryAs(TextField.class);
            assertThat(searchField.getText()).isEqualTo("test");

            // Clear programmatically (escape clear tested via unit tests)
            incomePage.clearSearch();
            assertThat(searchField.getText()).isEmpty();
        }
    }

    // === TC-201-06: Add Income Button (P0) ===

    @Nested
    @DisplayName("TC-201-06: Add Income Button")
    class AddIncomeButtonTests {

        @Test
        @DisplayName("TC-201-06a: Add Income button exists and is styled correctly")
        void addIncomeButtonExistsAndIsStyled() {
            Button addBtn = lookup("#addIncomeBtn").queryAs(Button.class);

            assertThat(addBtn).isNotNull();
            assertThat(addBtn.getText()).isEqualTo("+ Add Income");
            assertThat(addBtn.getStyleClass()).contains("button-success");
        }

        @Test
        @DisplayName("TC-201-06b: Add Income button is clickable")
        void addIncomeButtonIsClickable() {
            Button addBtn = lookup("#addIncomeBtn").queryAs(Button.class);

            // Click should not crash and button should be enabled
            assertThat(addBtn.isDisabled()).isFalse();

            // Note: Dialog opening tested in SE-202 E2E tests
        }
    }

    // === TC-201-07: Pagination (P1) ===

    @Nested
    @DisplayName("TC-201-07: Pagination")
    class PaginationTests {

        @Test
        @DisplayName("TC-201-07a: Pagination controls exist")
        void paginationControlsExist() {
            assertThat(lookup("#paginationBar").tryQuery()).isPresent();
            assertThat(lookup("#resultCount").tryQuery()).isPresent();
            assertThat(lookup("#prevBtn").tryQuery()).isPresent();
            assertThat(lookup("#nextBtn").tryQuery()).isPresent();
        }

        @Test
        @DisplayName("TC-201-07b: Result count shows entry count")
        void resultCountShowsEntryCount() {
            String resultCount = incomePage.getResultCount();

            assertThat(resultCount).containsPattern("(Showing \\d+|No) entr");
        }

        @Test
        @DisplayName("TC-201-07c: Prev button disabled on first page")
        void prevButtonDisabledOnFirstPage() {
            // On first page, prev should be disabled
            assertThat(incomePage.isPrevButtonDisabled()).isTrue();
        }
    }

    // === TC-201-08: Table Row Interactions (P1) ===

    @Nested
    @DisplayName("TC-201-08: Table Row Interactions")
    class TableInteractionTests {

        @Test
        @DisplayName("TC-201-08a: Row selection works")
        void rowSelectionWorks() {
            // If table has items, selection should work
            if (!incomePage.isTableEmpty()) {
                incomePage.selectRow(0);

                @SuppressWarnings("unchecked")
                TableView<Object> table = lookup("#incomeTable").queryAs(TableView.class);
                assertThat(table.getSelectionModel().getSelectedItem()).isNotNull();
            }
        }

        @Test
        @DisplayName("TC-201-08b: Table has context menu")
        void tableHasContextMenu() {
            @SuppressWarnings("unchecked")
            TableView<Object> table = lookup("#incomeTable").queryAs(TableView.class);

            assertThat(table.getContextMenu()).isNotNull();
        }
    }

    // === TC-201-09: Currency Formatting (P0) ===

    @Nested
    @DisplayName("TC-201-09: Currency Formatting")
    class CurrencyFormattingTests {

        @Test
        @DisplayName("TC-201-09a: All amounts use GBP format")
        void allAmountsUseGbpFormat() {
            // Verify summary amounts use £ symbol
            String total = incomePage.getTotalIncome();
            String paid = incomePage.getPaidIncome();
            String unpaid = incomePage.getUnpaidIncome();

            assertThat(total).startsWith("£");
            assertThat(paid).startsWith("£");
            assertThat(unpaid).startsWith("£");
        }

        @Test
        @DisplayName("TC-201-09b: Amounts have 2 decimal places")
        void amountsHaveTwoDecimalPlaces() {
            String total = incomePage.getTotalIncome();

            assertThat(total).matches("£[\\d,]+\\.\\d{2}");
        }
    }
}
