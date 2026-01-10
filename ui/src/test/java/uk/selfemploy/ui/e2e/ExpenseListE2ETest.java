package uk.selfemploy.ui.e2e;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import uk.selfemploy.ui.e2e.page.ExpensePage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Tests for SE-204: Expense List View.
 * Based on QA test specification from /rob (rob-qa-sprint-2.md).
 *
 * @see docs/sprints/sprint-2/testing/rob-qa-sprint-2.md
 */
@Tag("e2e")
@DisplayName("SE-204: Expense List View E2E")
class ExpenseListE2ETest extends BaseE2ETest {

    private ExpensePage expensePage;

    @BeforeEach
    void setupExpensePage() {
        expensePage = new ExpensePage(this);
        expensePage.navigateTo();
    }

    // === TC-204-01: Expense List Navigation (P0) ===

    @Nested
    @DisplayName("TC-204-01: Expense List Navigation")
    class NavigationTests {

        @Test
        @DisplayName("TC-204-01a: Navigate to Expense List from sidebar")
        void navigateToExpenseListFromSidebar() {
            // Step 1: Verify page title
            assertThat(expensePage.hasPageTitle("Expenses")).isTrue();

            // Step 2: Verify Expenses nav button is selected
            ToggleButton expensesBtn = lookup("#navExpenses").queryAs(ToggleButton.class);
            assertThat(expensesBtn.isSelected()).isTrue();
        }

        @Test
        @DisplayName("TC-204-01b: Expense page has correct layout")
        void expensePageHasCorrectLayout() {
            // Verify key elements exist
            assertThat(lookup("#expenseTable").tryQuery()).isPresent();
            assertThat(lookup("#addExpenseBtn").tryQuery()).isPresent();
            assertThat(lookup("#categoryFilter").tryQuery()).isPresent();
            assertThat(lookup("#searchField").tryQuery()).isPresent();
        }
    }

    // === TC-204-02: Summary Cards Display (P0) ===

    @Nested
    @DisplayName("TC-204-02: Summary Cards Display")
    class SummaryCardsTests {

        @Test
        @DisplayName("TC-204-02a: Summary cards show total expenses")
        void summaryCardsShowTotalExpenses() {
            String totalValue = expensePage.getTotalExpenses();
            assertThat(totalValue).matches("£[\\d,]+\\.\\d{2}");
        }

        @Test
        @DisplayName("TC-204-02b: Summary cards show deductible expenses")
        void summaryCardsShowDeductibleExpenses() {
            String deductibleValue = expensePage.getDeductibleExpenses();
            assertThat(deductibleValue).matches("£[\\d,]+\\.\\d{2}");
        }

        @Test
        @DisplayName("TC-204-02c: Summary cards show non-deductible expenses")
        void summaryCardsShowNonDeductibleExpenses() {
            String nonDeductibleValue = expensePage.getNonDeductibleExpenses();
            assertThat(nonDeductibleValue).matches("£[\\d,]+\\.\\d{2}");
        }

        @Test
        @DisplayName("TC-204-02d: Summary cards show entry counts")
        void summaryCardsShowEntryCounts() {
            String totalCount = expensePage.getTotalCount();
            String deductibleCount = expensePage.getDeductibleCount();
            String nonDeductibleCount = expensePage.getNonDeductibleCount();

            assertThat(totalCount).containsPattern("\\d+ entr(y|ies)");
            assertThat(deductibleCount).containsPattern("\\d+ entr(y|ies)");
            assertThat(nonDeductibleCount).containsPattern("\\d+ entr(y|ies)");
        }
    }

    // === TC-204-03: Expense Table Display (P0) ===

    @Nested
    @DisplayName("TC-204-03: Expense Table Display")
    class TableDisplayTests {

        @Test
        @DisplayName("TC-204-03a: Table has required columns")
        void tableHasRequiredColumns() {
            @SuppressWarnings("unchecked")
            TableView<Object> table = lookup("#expenseTable").queryAs(TableView.class);

            // Verify columns exist (5 columns)
            assertThat(table.getColumns()).hasSize(5);

            // Verify column headers
            assertThat(table.getColumns().get(0).getText()).isEqualTo("DATE");
            assertThat(table.getColumns().get(1).getText()).isEqualTo("DESCRIPTION");
            assertThat(table.getColumns().get(2).getText()).isEqualTo("CATEGORY");
            assertThat(table.getColumns().get(3).getText()).isEqualTo("AMOUNT");
            assertThat(table.getColumns().get(4).getText()).isEqualTo("D"); // Deductible column
        }

        @Test
        @DisplayName("TC-204-03b: Empty state displayed when no data")
        void emptyStateDisplayedWhenNoData() {
            if (expensePage.isTableEmpty()) {
                VBox emptyState = lookup("#emptyState").queryAs(VBox.class);
                assertThat(emptyState).isNotNull();
            }
        }
    }

    // === TC-204-04: Category Filter (P1) ===

    @Nested
    @DisplayName("TC-204-04: Category Filter")
    class CategoryFilterTests {

        @Test
        @DisplayName("TC-204-04a: Category filter exists")
        void categoryFilterExists() {
            @SuppressWarnings("unchecked")
            ComboBox<Object> categoryFilter = lookup("#categoryFilter").queryAs(ComboBox.class);
            assertThat(categoryFilter).isNotNull();
        }

        @Test
        @DisplayName("TC-204-04b: Category filter has SA103 categories")
        void categoryFilterHasSa103Categories() {
            @SuppressWarnings("unchecked")
            ComboBox<Object> categoryFilter = lookup("#categoryFilter").queryAs(ComboBox.class);

            // Should have categories plus "All Categories"
            assertThat(categoryFilter.getItems()).isNotEmpty();
        }
    }

    // === TC-204-05: Search Functionality (P1) ===

    @Nested
    @DisplayName("TC-204-05: Search Functionality")
    class SearchTests {

        @Test
        @DisplayName("TC-204-05a: Search field has placeholder text")
        void searchFieldHasPlaceholderText() {
            TextField searchField = lookup("#searchField").queryAs(TextField.class);
            assertThat(searchField.getPromptText()).contains("Search");
        }

        @Test
        @DisplayName("TC-204-05b: Search field accepts text input")
        void searchFieldAcceptsTextInput() {
            expensePage.searchFor("test");

            TextField searchField = lookup("#searchField").queryAs(TextField.class);
            assertThat(searchField.getText()).isEqualTo("test");

            expensePage.clearSearch();
            assertThat(searchField.getText()).isEmpty();
        }
    }

    // === TC-204-06: Add Expense Button (P0) ===

    @Nested
    @DisplayName("TC-204-06: Add Expense Button")
    class AddExpenseButtonTests {

        @Test
        @DisplayName("TC-204-06a: Add Expense button exists and is styled correctly")
        void addExpenseButtonExistsAndIsStyled() {
            Button addBtn = lookup("#addExpenseBtn").queryAs(Button.class);

            assertThat(addBtn).isNotNull();
            assertThat(addBtn.getText()).isEqualTo("+ Add Expense");
            assertThat(addBtn.getStyleClass()).contains("button-warning");
        }

        @Test
        @DisplayName("TC-204-06b: Add Expense button is clickable")
        void addExpenseButtonIsClickable() {
            Button addBtn = lookup("#addExpenseBtn").queryAs(Button.class);
            assertThat(addBtn.isDisabled()).isFalse();
        }
    }

    // === TC-204-07: Pagination (P1) ===

    @Nested
    @DisplayName("TC-204-07: Pagination")
    class PaginationTests {

        @Test
        @DisplayName("TC-204-07a: Pagination controls exist")
        void paginationControlsExist() {
            assertThat(lookup("#paginationBar").tryQuery()).isPresent();
            assertThat(lookup("#resultCount").tryQuery()).isPresent();
            assertThat(lookup("#prevBtn").tryQuery()).isPresent();
            assertThat(lookup("#nextBtn").tryQuery()).isPresent();
        }

        @Test
        @DisplayName("TC-204-07b: Result count shows entry count")
        void resultCountShowsEntryCount() {
            String resultCount = expensePage.getResultCount();
            assertThat(resultCount).containsPattern("(Showing \\d+|No) entr");
        }

        @Test
        @DisplayName("TC-204-07c: Prev button disabled on first page")
        void prevButtonDisabledOnFirstPage() {
            assertThat(expensePage.isPrevButtonDisabled()).isTrue();
        }
    }

    // === TC-204-08: Table Row Interactions (P1) ===

    @Nested
    @DisplayName("TC-204-08: Table Row Interactions")
    class TableInteractionTests {

        @Test
        @DisplayName("TC-204-08a: Row selection works")
        void rowSelectionWorks() {
            if (!expensePage.isTableEmpty()) {
                expensePage.selectRow(0);

                @SuppressWarnings("unchecked")
                TableView<Object> table = lookup("#expenseTable").queryAs(TableView.class);
                assertThat(table.getSelectionModel().getSelectedItem()).isNotNull();
            }
        }

        @Test
        @DisplayName("TC-204-08b: Table has context menu")
        void tableHasContextMenu() {
            @SuppressWarnings("unchecked")
            TableView<Object> table = lookup("#expenseTable").queryAs(TableView.class);
            assertThat(table.getContextMenu()).isNotNull();
        }
    }

    // === TC-204-09: Currency Formatting (P0) ===

    @Nested
    @DisplayName("TC-204-09: Currency Formatting")
    class CurrencyFormattingTests {

        @Test
        @DisplayName("TC-204-09a: All amounts use GBP format")
        void allAmountsUseGbpFormat() {
            String total = expensePage.getTotalExpenses();
            String deductible = expensePage.getDeductibleExpenses();
            String nonDeductible = expensePage.getNonDeductibleExpenses();

            assertThat(total).startsWith("£");
            assertThat(deductible).startsWith("£");
            assertThat(nonDeductible).startsWith("£");
        }

        @Test
        @DisplayName("TC-204-09b: Amounts have 2 decimal places")
        void amountsHaveTwoDecimalPlaces() {
            String total = expensePage.getTotalExpenses();
            assertThat(total).matches("£[\\d,]+\\.\\d{2}");
        }
    }
}
