package uk.selfemploy.ui.e2e.page;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;

/**
 * Page Object for Income List View (SE-201).
 * Provides abstraction for E2E test interactions.
 */
public class IncomePage {

    private final FxRobot robot;

    public IncomePage(FxRobot robot) {
        this.robot = robot;
    }

    // === Navigation ===

    public void navigateTo() {
        robot.clickOn("#navIncome");
        WaitForAsyncUtils.waitForFxEvents();
    }

    // === Summary Cards ===

    public String getTotalIncome() {
        return robot.lookup("#totalValue").queryAs(Label.class).getText();
    }

    public String getTotalCount() {
        return robot.lookup("#totalCount").queryAs(Label.class).getText();
    }

    public String getPaidIncome() {
        return robot.lookup("#paidValue").queryAs(Label.class).getText();
    }

    public String getPaidCount() {
        return robot.lookup("#paidCount").queryAs(Label.class).getText();
    }

    public String getUnpaidIncome() {
        return robot.lookup("#unpaidValue").queryAs(Label.class).getText();
    }

    public String getUnpaidCount() {
        return robot.lookup("#unpaidCount").queryAs(Label.class).getText();
    }

    // === Filters ===

    public void selectStatusFilter(String status) {
        robot.clickOn("#statusFilter");
        WaitForAsyncUtils.waitForFxEvents();
        robot.type(javafx.scene.input.KeyCode.DOWN);
        // Navigate to the appropriate option based on status
        switch (status) {
            case "All Status" -> {} // First item
            case "Paid" -> robot.type(javafx.scene.input.KeyCode.DOWN);
            case "Unpaid" -> {
                robot.type(javafx.scene.input.KeyCode.DOWN);
                robot.type(javafx.scene.input.KeyCode.DOWN);
            }
        }
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
        return robot.lookup("#incomeTable").queryAs(TableView.class);
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
        robot.doubleClickOn("#incomeTable");
        WaitForAsyncUtils.waitForFxEvents();
    }

    // === Actions ===

    public void clickAddIncome() {
        robot.clickOn("#addIncomeBtn");
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
        robot.rightClickOn("#incomeTable");
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void clickContextMenuEdit() {
        robot.clickOn("Edit Income");
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void clickContextMenuDelete() {
        robot.clickOn("Delete Income");
        WaitForAsyncUtils.waitForFxEvents();
    }

    // === Verification Helpers ===

    public boolean isOnIncomePage() {
        Label pageTitle = robot.lookup(".page-title").queryAs(Label.class);
        return pageTitle != null && "Income".equals(pageTitle.getText());
    }

    public boolean hasPageTitle(String title) {
        Label pageTitle = robot.lookup(".page-title").queryAs(Label.class);
        return pageTitle != null && title.equals(pageTitle.getText());
    }
}
