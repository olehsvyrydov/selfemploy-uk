package uk.selfemploy.ui.e2e.page;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

/**
 * Page Object for Expense List View (SE-204).
 * Provides abstraction for E2E test interactions.
 */
public class ExpensePage {

    private final FxRobot robot;

    public ExpensePage(FxRobot robot) {
        this.robot = robot;
    }

    // === Navigation ===

    public void navigateTo() {
        robot.clickOn("#navExpenses");
        WaitForAsyncUtils.waitForFxEvents();
    }

    // === Summary Cards ===

    public String getTotalExpenses() {
        return robot.lookup("#totalValue").queryAs(Label.class).getText();
    }

    public String getTotalCount() {
        return robot.lookup("#totalCount").queryAs(Label.class).getText();
    }

    public String getDeductibleExpenses() {
        return robot.lookup("#deductibleValue").queryAs(Label.class).getText();
    }

    public String getDeductibleCount() {
        return robot.lookup("#deductibleCount").queryAs(Label.class).getText();
    }

    public String getNonDeductibleExpenses() {
        return robot.lookup("#nonDeductibleValue").queryAs(Label.class).getText();
    }

    public String getNonDeductibleCount() {
        return robot.lookup("#nonDeductibleCount").queryAs(Label.class).getText();
    }

    // === Filters ===

    public void selectCategoryFilter(String category) {
        robot.clickOn("#categoryFilter");
        WaitForAsyncUtils.waitForFxEvents();
        // Use keyboard navigation for dropdown
        robot.type(javafx.scene.input.KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void searchFor(String text) {
        TextField searchField = robot.lookup("#searchField").queryAs(TextField.class);
        searchField.clear();
        robot.clickOn("#searchField");
        robot.write(text);
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void clearSearch() {
        TextField searchField = robot.lookup("#searchField").queryAs(TextField.class);
        searchField.clear();
        WaitForAsyncUtils.waitForFxEvents();
    }

    // === Table ===

    @SuppressWarnings("unchecked")
    public TableView<Object> getTable() {
        return robot.lookup("#expenseTable").queryAs(TableView.class);
    }

    public int getTableRowCount() {
        return getTable().getItems().size();
    }

    public boolean isTableEmpty() {
        return getTableRowCount() == 0;
    }

    public void selectRow(int index) {
        getTable().getSelectionModel().select(index);
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void doubleClickRow(int index) {
        selectRow(index);
        robot.doubleClickOn("#expenseTable");
        WaitForAsyncUtils.waitForFxEvents();
    }

    // === Actions ===

    public void clickAddExpense() {
        robot.clickOn("#addExpenseBtn");
        WaitForAsyncUtils.waitForFxEvents();
    }

    // === Pagination ===

    public String getResultCount() {
        return robot.lookup("#resultCount").queryAs(Label.class).getText();
    }

    public void clickPrevPage() {
        robot.clickOn("#prevBtn");
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void clickNextPage() {
        robot.clickOn("#nextBtn");
        WaitForAsyncUtils.waitForFxEvents();
    }

    public boolean isPrevButtonDisabled() {
        return robot.lookup("#prevBtn").queryAs(Button.class).isDisabled();
    }

    public boolean isNextButtonDisabled() {
        return robot.lookup("#nextBtn").queryAs(Button.class).isDisabled();
    }

    // === Empty State ===

    public boolean isEmptyStateVisible() {
        VBox emptyState = robot.lookup("#emptyState").queryAs(VBox.class);
        return emptyState != null && emptyState.isVisible();
    }

    // === Context Menu ===

    public void rightClickRow(int index) {
        selectRow(index);
        robot.rightClickOn("#expenseTable");
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void clickContextMenuEdit() {
        robot.clickOn("Edit Expense");
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void clickContextMenuDelete() {
        robot.clickOn("Delete Expense");
        WaitForAsyncUtils.waitForFxEvents();
    }

    // === Verification Helpers ===

    public boolean isOnExpensePage() {
        Label pageTitle = robot.lookup(".page-title").queryAs(Label.class);
        return pageTitle != null && "Expenses".equals(pageTitle.getText());
    }

    public boolean hasPageTitle(String title) {
        Label pageTitle = robot.lookup(".page-title").queryAs(Label.class);
        return pageTitle != null && title.equals(pageTitle.getText());
    }
}
