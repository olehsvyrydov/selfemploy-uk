package uk.selfemploy.ui.e2e.page;

import javafx.scene.control.*;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDate;

/**
 * Page Object for Add/Edit Expense Dialog (SE-205).
 * Provides abstraction for E2E test interactions.
 */
public class ExpenseDialogPage {

    private final FxRobot robot;

    public ExpenseDialogPage(FxRobot robot) {
        this.robot = robot;
    }

    // === Dialog State ===

    public boolean isDialogOpen() {
        try {
            return robot.lookup("#expenseDialogTitle").tryQuery().isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public String getDialogTitle() {
        return robot.lookup("#expenseDialogTitle").queryAs(Label.class).getText();
    }

    public boolean isAddMode() {
        return "Add Expense".equals(getDialogTitle());
    }

    public boolean isEditMode() {
        return "Edit Expense".equals(getDialogTitle());
    }

    // === Form Fields ===

    public void enterDescription(String description) {
        TextField descField = robot.lookup("#expenseDescField").queryAs(TextField.class);
        descField.clear();
        robot.clickOn("#expenseDescField");
        robot.write(description);
        WaitForAsyncUtils.waitForFxEvents();
    }

    public String getDescription() {
        return robot.lookup("#expenseDescField").queryAs(TextField.class).getText();
    }

    public void enterAmount(String amount) {
        TextField amountField = robot.lookup("#expenseAmountField").queryAs(TextField.class);
        amountField.clear();
        robot.clickOn("#expenseAmountField");
        robot.write(amount);
        WaitForAsyncUtils.waitForFxEvents();
    }

    public String getAmount() {
        return robot.lookup("#expenseAmountField").queryAs(TextField.class).getText();
    }

    public void selectDate(LocalDate date) {
        DatePicker datePicker = robot.lookup("#expenseDatePicker").queryAs(DatePicker.class);
        robot.interact(() -> datePicker.setValue(date));
        WaitForAsyncUtils.waitForFxEvents();
    }

    public LocalDate getDate() {
        return robot.lookup("#expenseDatePicker").queryAs(DatePicker.class).getValue();
    }

    public void selectCategory(String category) {
        robot.clickOn("#categoryCombo");
        WaitForAsyncUtils.waitForFxEvents();
        // Use keyboard navigation
        robot.type(javafx.scene.input.KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();
    }

    public String getCategory() {
        ComboBox<?> categoryCombo = robot.lookup("#categoryCombo").queryAs(ComboBox.class);
        Object value = categoryCombo.getValue();
        return value != null ? value.toString() : "";
    }

    public void setDeductible(boolean deductible) {
        CheckBox deductibleCheck = robot.lookup("#deductibleCheck").queryAs(CheckBox.class);
        if (deductibleCheck.isSelected() != deductible) {
            robot.clickOn("#deductibleCheck");
            WaitForAsyncUtils.waitForFxEvents();
        }
    }

    public boolean isDeductible() {
        return robot.lookup("#deductibleCheck").queryAs(CheckBox.class).isSelected();
    }

    // === Validation ===

    public boolean hasDescriptionError() {
        try {
            Label errorLabel = robot.lookup("#expenseDescError").queryAs(Label.class);
            return errorLabel != null && errorLabel.isVisible() && !errorLabel.getText().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasAmountError() {
        try {
            Label errorLabel = robot.lookup("#expenseAmountError").queryAs(Label.class);
            return errorLabel != null && errorLabel.isVisible() && !errorLabel.getText().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasCategoryError() {
        try {
            Label errorLabel = robot.lookup("#categoryError").queryAs(Label.class);
            return errorLabel != null && errorLabel.isVisible() && !errorLabel.getText().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // === Actions ===

    public void clickSave() {
        robot.clickOn("#expenseSaveBtn");
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void clickCancel() {
        robot.clickOn("#expenseCancelBtn");
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void clickDelete() {
        robot.clickOn("#expenseDeleteBtn");
        WaitForAsyncUtils.waitForFxEvents();
    }

    public boolean isSaveButtonEnabled() {
        return !robot.lookup("#expenseSaveBtn").queryAs(Button.class).isDisabled();
    }

    public boolean isDeleteButtonVisible() {
        try {
            Button deleteBtn = robot.lookup("#expenseDeleteBtn").queryAs(Button.class);
            return deleteBtn != null && deleteBtn.isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    // === Convenience Methods ===

    public void fillForm(String description, String amount, LocalDate date, String category, boolean deductible) {
        enterDescription(description);
        enterAmount(amount);
        selectDate(date);
        selectCategory(category);
        setDeductible(deductible);
    }

    public void saveAndClose() {
        clickSave();
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void cancelAndClose() {
        clickCancel();
        WaitForAsyncUtils.waitForFxEvents();
    }
}
