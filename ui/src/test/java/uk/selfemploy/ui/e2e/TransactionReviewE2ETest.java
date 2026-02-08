package uk.selfemploy.ui.e2e;

import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Tests for the Transaction Review Dashboard.
 *
 * <p>Tests the full UI rendering of the Bank Review page including navigation,
 * summary cards, filters, batch actions, table structure, export, pagination,
 * undo, and keyboard shortcuts.</p>
 *
 * <p>These tests verify empty-state behavior (no bank transactions loaded).
 * They require a display. Run with {@code -Dgroups=e2e} to include,
 * or {@code -DexcludedGroups=e2e} to exclude.</p>
 */
@Tag("e2e")
@DisplayName("Transaction Review Dashboard E2E")
class TransactionReviewE2ETest extends BaseE2ETest {

    @BeforeEach
    void navigateToTransactionReview() {
        clickOn("#navTransactionReview");
        waitForFxEvents();
    }

    // === Navigation ===

    @Nested
    @DisplayName("Navigation Tests")
    class NavigationTests {

        @Test
        @DisplayName("TC-01: Bank Review nav button exists and is clickable")
        void bankReviewNavButtonExistsAndIsClickable() {
            ToggleButton navBtn = lookup("#navTransactionReview").queryAs(ToggleButton.class);
            assertThat(navBtn).isNotNull();
            assertThat(navBtn.getText()).isEqualTo("Bank Review");
            assertThat(navBtn.isSelected()).isTrue();
        }

        @Test
        @DisplayName("TC-02: Page title shows Transaction Review")
        void pageTitleShowsTransactionReview() {
            Label pageTitle = lookup(".page-title").queryAs(Label.class);
            assertThat(pageTitle).isNotNull();
            assertThat(pageTitle.getText()).isEqualTo("Transaction Review");
        }

        @Test
        @DisplayName("TC-03: Can navigate away and back")
        void canNavigateAwayAndBack() {
            // Navigate to Dashboard
            clickOn("#navDashboard");
            waitForFxEvents();

            ToggleButton dashBtn = lookup("#navDashboard").queryAs(ToggleButton.class);
            assertThat(dashBtn.isSelected()).isTrue();

            // Navigate back to Bank Review
            clickOn("#navTransactionReview");
            waitForFxEvents();

            ToggleButton navBtn = lookup("#navTransactionReview").queryAs(ToggleButton.class);
            assertThat(navBtn.isSelected()).isTrue();

            Label pageTitle = lookup(".page-title").queryAs(Label.class);
            assertThat(pageTitle.getText()).isEqualTo("Transaction Review");
        }
    }

    // === Empty State ===

    @Nested
    @DisplayName("Empty State Tests")
    class EmptyStateTests {

        @Test
        @DisplayName("TC-04: Empty state container exists")
        void emptyStateContainerExists() {
            assertThat(lookup("#emptyState").tryQuery()).isPresent();
        }

        @Test
        @DisplayName("TC-05: Summary cards show zero values")
        void summaryCardsShowZeroValues() {
            Label totalValue = lookup("#totalValue").queryAs(Label.class);
            Label pendingValue = lookup("#pendingValue").queryAs(Label.class);
            Label categorizedValue = lookup("#categorizedValue").queryAs(Label.class);
            Label excludedValue = lookup("#excludedValue").queryAs(Label.class);

            assertThat(totalValue.getText()).isEqualTo("0");
            assertThat(pendingValue.getText()).isEqualTo("0");
            assertThat(categorizedValue.getText()).isEqualTo("0");
            assertThat(excludedValue.getText()).isEqualTo("0");
        }

        @Test
        @DisplayName("TC-06: Table has no items when empty")
        void tableHasNoItemsWhenEmpty() {
            TableView<?> table = lookup("#transactionTable").queryAs(TableView.class);
            assertThat(table.getItems()).isEmpty();
        }
    }

    // === Summary Cards ===

    @Nested
    @DisplayName("Summary Cards Tests")
    class SummaryCardsTests {

        @Test
        @DisplayName("TC-07: Total card exists")
        void totalCardExists() {
            Label totalValue = lookup("#totalValue").queryAs(Label.class);
            assertThat(totalValue).isNotNull();
        }

        @Test
        @DisplayName("TC-08: Pending card exists")
        void pendingCardExists() {
            Label pendingValue = lookup("#pendingValue").queryAs(Label.class);
            assertThat(pendingValue).isNotNull();
        }

        @Test
        @DisplayName("TC-09: Categorized card exists")
        void categorizedCardExists() {
            Label categorizedValue = lookup("#categorizedValue").queryAs(Label.class);
            assertThat(categorizedValue).isNotNull();
        }

        @Test
        @DisplayName("TC-10: Excluded card exists")
        void excludedCardExists() {
            Label excludedValue = lookup("#excludedValue").queryAs(Label.class);
            assertThat(excludedValue).isNotNull();
        }

        @Test
        @DisplayName("TC-11: Progress bar exists")
        void progressBarExists() {
            ProgressBar progressBar = lookup("#reviewProgressBar").queryAs(ProgressBar.class);
            assertThat(progressBar).isNotNull();
            assertThat(progressBar.getProgress()).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("TC-12: Progress label shows reviewed count")
        void progressLabelShowsReviewedCount() {
            Label progressLabel = lookup("#progressLabel").queryAs(Label.class);
            assertThat(progressLabel).isNotNull();
            assertThat(progressLabel.getText()).matches("\\d+ of \\d+ reviewed");
        }
    }

    // === Filter Controls ===

    @Nested
    @DisplayName("Filter Controls Tests")
    class FilterControlsTests {

        @Test
        @DisplayName("TC-13: Status filter combo exists with default value")
        void statusFilterComboExists() {
            ComboBox<?> statusFilter = lookup("#statusFilterCombo").queryAs(ComboBox.class);
            assertThat(statusFilter).isNotNull();
            assertThat(statusFilter.getValue()).isEqualTo("All Status");
        }

        @Test
        @DisplayName("TC-14: Date pickers exist")
        void datePickersExist() {
            DatePicker dateFrom = lookup("#dateFromPicker").queryAs(DatePicker.class);
            DatePicker dateTo = lookup("#dateToPicker").queryAs(DatePicker.class);
            assertThat(dateFrom).isNotNull();
            assertThat(dateTo).isNotNull();
            assertThat(dateFrom.getPromptText()).isEqualTo("From");
            assertThat(dateTo.getPromptText()).isEqualTo("To");
        }

        @Test
        @DisplayName("TC-15: Amount fields exist")
        void amountFieldsExist() {
            TextField amountMin = lookup("#amountMinField").queryAs(TextField.class);
            TextField amountMax = lookup("#amountMaxField").queryAs(TextField.class);
            assertThat(amountMin).isNotNull();
            assertThat(amountMax).isNotNull();
            assertThat(amountMin.getPromptText()).isEqualTo("Min £");
            assertThat(amountMax.getPromptText()).isEqualTo("Max £");
        }

        @Test
        @DisplayName("TC-16: Search field exists")
        void searchFieldExists() {
            TextField searchField = lookup("#searchField").queryAs(TextField.class);
            assertThat(searchField).isNotNull();
        }

        @Test
        @DisplayName("TC-17: Search field has placeholder text")
        void searchFieldHasPlaceholderText() {
            TextField searchField = lookup("#searchField").queryAs(TextField.class);
            assertThat(searchField.getPromptText()).isEqualTo("Search description...");
        }
    }

    // === Batch Action Bar ===

    @Nested
    @DisplayName("Batch Action Bar Tests")
    class BatchActionBarTests {

        @Test
        @DisplayName("TC-18: Batch bar exists")
        void batchBarExists() {
            HBox batchBar = lookup("#batchBar").queryAs(HBox.class);
            assertThat(batchBar).isNotNull();
        }

        @Test
        @DisplayName("TC-19: Business, Personal, Exclude buttons exist")
        void batchButtonsExist() {
            Button businessBtn = lookup("#batchBusinessBtn").queryAs(Button.class);
            Button personalBtn = lookup("#batchPersonalBtn").queryAs(Button.class);
            Button excludeBtn = lookup("#batchExcludeBtn").queryAs(Button.class);

            assertThat(businessBtn).isNotNull();
            assertThat(businessBtn.getText()).isEqualTo("Business");
            assertThat(personalBtn).isNotNull();
            assertThat(personalBtn.getText()).isEqualTo("Personal");
            assertThat(excludeBtn).isNotNull();
            assertThat(excludeBtn.getText()).isEqualTo("Exclude");
        }

        @Test
        @DisplayName("TC-20: Selected count label shows 0 selected")
        void selectedCountLabelShowsZero() {
            Label selectedCount = lookup("#selectedCountLabel").queryAs(Label.class);
            assertThat(selectedCount).isNotNull();
            assertThat(selectedCount.getText()).isEqualTo("0 selected");
        }

        @Test
        @DisplayName("TC-21: Batch buttons are disabled when nothing selected")
        void batchButtonsDisabledWhenNothingSelected() {
            Button businessBtn = lookup("#batchBusinessBtn").queryAs(Button.class);
            Button personalBtn = lookup("#batchPersonalBtn").queryAs(Button.class);
            Button excludeBtn = lookup("#batchExcludeBtn").queryAs(Button.class);

            assertThat(businessBtn.isDisabled()).isTrue();
            assertThat(personalBtn.isDisabled()).isTrue();
            assertThat(excludeBtn.isDisabled()).isTrue();
        }
    }

    // === Table Structure ===

    @Nested
    @DisplayName("Table Structure Tests")
    class TableStructureTests {

        @Test
        @DisplayName("TC-22: Table exists")
        void tableExists() {
            TableView<?> table = lookup("#transactionTable").queryAs(TableView.class);
            assertThat(table).isNotNull();
        }

        @Test
        @DisplayName("TC-23: All 9 columns present")
        void allNineColumnsPresent() {
            TableView<?> table = lookup("#transactionTable").queryAs(TableView.class);
            assertThat(table.getColumns()).hasSize(9);

            // Verify column headers
            assertThat(table.getColumns().get(0).getText()).isEmpty(); // select checkbox
            assertThat(table.getColumns().get(1).getText()).isEqualTo("DATE");
            assertThat(table.getColumns().get(2).getText()).isEqualTo("DESCRIPTION");
            assertThat(table.getColumns().get(3).getText()).isEqualTo("AMOUNT");
            assertThat(table.getColumns().get(4).getText()).isEqualTo("CATEGORY");
            assertThat(table.getColumns().get(5).getText()).isEqualTo("CONFIDENCE");
            assertThat(table.getColumns().get(6).getText()).isEqualTo("BUS/PERS");
            assertThat(table.getColumns().get(7).getText()).isEqualTo("STATUS");
            assertThat(table.getColumns().get(8).getText()).isEqualTo("ACTIONS");
        }

        @Test
        @DisplayName("TC-24: Date and Amount columns are sortable")
        void dateAndAmountColumnsAreSortable() {
            TableView<?> table = lookup("#transactionTable").queryAs(TableView.class);

            // Date column (index 1) should be sortable
            assertThat(table.getColumns().get(1).isSortable()).isTrue();
            // Amount column (index 3) should be sortable
            assertThat(table.getColumns().get(3).isSortable()).isTrue();
            // Select column (index 0) should NOT be sortable
            assertThat(table.getColumns().get(0).isSortable()).isFalse();
        }
    }

    // === Export ===

    @Nested
    @DisplayName("Export Tests")
    class ExportTests {

        @Test
        @DisplayName("TC-25: Export menu button exists")
        void exportMenuButtonExists() {
            MenuButton exportBtn = lookup("#exportBtn").queryAs(MenuButton.class);
            assertThat(exportBtn).isNotNull();
            assertThat(exportBtn.getText()).isEqualTo("Export");
        }

        @Test
        @DisplayName("TC-26: Export menu has CSV and JSON options")
        void exportMenuHasCsvAndJsonOptions() {
            MenuButton exportBtn = lookup("#exportBtn").queryAs(MenuButton.class);
            assertThat(exportBtn.getItems()).hasSize(2);
            assertThat(exportBtn.getItems().get(0).getText()).isEqualTo("Export as CSV");
            assertThat(exportBtn.getItems().get(1).getText()).isEqualTo("Export as JSON");
        }
    }

    // === Pagination ===

    @Nested
    @DisplayName("Pagination Tests")
    class PaginationTests {

        @Test
        @DisplayName("TC-27: Pagination bar exists")
        void paginationBarExists() {
            HBox paginationBar = lookup("#paginationBar").queryAs(HBox.class);
            assertThat(paginationBar).isNotNull();
        }

        @Test
        @DisplayName("TC-28: Previous and Next buttons exist")
        void previousAndNextButtonsExist() {
            Button prevBtn = lookup("#prevBtn").queryAs(Button.class);
            Button nextBtn = lookup("#nextBtn").queryAs(Button.class);

            assertThat(prevBtn).isNotNull();
            assertThat(prevBtn.getText()).isEqualTo("< Previous");
            assertThat(nextBtn).isNotNull();
            assertThat(nextBtn.getText()).isEqualTo("Next >");
        }

        @Test
        @DisplayName("TC-29: Result count label exists")
        void resultCountLabelExists() {
            Label resultCount = lookup("#resultCount").queryAs(Label.class);
            assertThat(resultCount).isNotNull();
        }

        @Test
        @DisplayName("TC-30: Prev button disabled on first page")
        void prevButtonDisabledOnFirstPage() {
            Button prevBtn = lookup("#prevBtn").queryAs(Button.class);
            assertThat(prevBtn.isDisabled()).isTrue();
        }
    }

    // === Undo ===

    @Nested
    @DisplayName("Undo Tests")
    class UndoTests {

        @Test
        @DisplayName("TC-31: Undo button exists")
        void undoButtonExists() {
            Button undoBtn = lookup("#undoBtn").queryAs(Button.class);
            assertThat(undoBtn).isNotNull();
            assertThat(undoBtn.getText()).isEqualTo("Undo");
        }

        @Test
        @DisplayName("TC-32: Undo button disabled when no undo available")
        void undoButtonDisabledWhenNoUndoAvailable() {
            Button undoBtn = lookup("#undoBtn").queryAs(Button.class);
            assertThat(undoBtn.isDisabled()).isTrue();
        }
    }

    // === Keyboard Shortcuts ===

    @Nested
    @DisplayName("Keyboard Shortcuts Tests")
    class KeyboardShortcutsTests {

        @Test
        @DisplayName("TC-33: Ctrl+Z does not crash when no undo available")
        void ctrlZDoesNotCrashWhenNoUndo() {
            // Press Ctrl+Z - should do nothing, not crash
            push(KeyCode.CONTROL, KeyCode.Z);
            waitForFxEvents();

            // Verify page is still showing correctly
            Label pageTitle = lookup(".page-title").queryAs(Label.class);
            assertThat(pageTitle.getText()).isEqualTo("Transaction Review");
        }
    }
}
