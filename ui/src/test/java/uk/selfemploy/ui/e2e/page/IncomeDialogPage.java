package uk.selfemploy.ui.e2e.page;

import javafx.scene.control.*;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDate;

/**
 * Page Object for Add/Edit Income Dialog (SE-202).
 * Provides abstraction for E2E test interactions.
 */
public class IncomeDialogPage {

    private final FxRobot robot;

    public IncomeDialogPage(FxRobot robot) {
        this.robot = robot;
    }

    // === Dialog State ===

    public boolean isDialogOpen() {
        try {
            return robot.lookup("#incomeDialogTitle").tryQuery().isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public String getDialogTitle() {
        return robot.lookup("#incomeDialogTitle").queryAs(Label.class).getText();
    }

    public boolean isAddMode() {
        return "Add Income".equals(getDialogTitle());
    }

    public boolean isEditMode() {
        return "Edit Income".equals(getDialogTitle());
    }

    // === Form Fields ===

    public void enterClient(String client) {
        TextField clientField = robot.lookup("#clientField").queryAs(TextField.class);
        clientField.clear();
        robot.clickOn("#clientField");
        robot.write(client);
        WaitForAsyncUtils.waitForFxEvents();
    }

    public String getClient() {
        return robot.lookup("#clientField").queryAs(TextField.class).getText();
    }

    public void enterDescription(String description) {
        TextField descField = robot.lookup("#descriptionField").queryAs(TextField.class);
        descField.clear();
        robot.clickOn("#descriptionField");
        robot.write(description);
        WaitForAsyncUtils.waitForFxEvents();
    }

    public String getDescription() {
        return robot.lookup("#descriptionField").queryAs(TextField.class).getText();
    }

    public void enterAmount(String amount) {
        TextField amountField = robot.lookup("#amountField").queryAs(TextField.class);
        amountField.clear();
        robot.clickOn("#amountField");
        robot.write(amount);
        WaitForAsyncUtils.waitForFxEvents();
    }

    public String getAmount() {
        return robot.lookup("#amountField").queryAs(TextField.class).getText();
    }

    public void selectDate(LocalDate date) {
        DatePicker datePicker = robot.lookup("#datePicker").queryAs(DatePicker.class);
        robot.interact(() -> datePicker.setValue(date));
        WaitForAsyncUtils.waitForFxEvents();
    }

    public LocalDate getDate() {
        return robot.lookup("#datePicker").queryAs(DatePicker.class).getValue();
    }

    public void selectStatus(String status) {
        robot.clickOn("#statusCombo");
        WaitForAsyncUtils.waitForFxEvents();
        // Navigate to the appropriate status
        if ("Unpaid".equals(status)) {
            robot.type(javafx.scene.input.KeyCode.DOWN);
        }
        robot.type(javafx.scene.input.KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();
    }

    public String getStatus() {
        ComboBox<?> statusCombo = robot.lookup("#statusCombo").queryAs(ComboBox.class);
        Object value = statusCombo.getValue();
        return value != null ? value.toString() : "";
    }

    // === Validation ===

    public boolean hasClientError() {
        try {
            Label errorLabel = robot.lookup("#clientError").queryAs(Label.class);
            return errorLabel != null && errorLabel.isVisible() && !errorLabel.getText().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasAmountError() {
        try {
            Label errorLabel = robot.lookup("#amountError").queryAs(Label.class);
            return errorLabel != null && errorLabel.isVisible() && !errorLabel.getText().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasDateError() {
        try {
            Label errorLabel = robot.lookup("#dateError").queryAs(Label.class);
            return errorLabel != null && errorLabel.isVisible() && !errorLabel.getText().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // === Actions ===

    public void clickSave() {
        robot.clickOn("#saveBtn");
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void clickCancel() {
        robot.clickOn("#cancelBtn");
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void clickDelete() {
        robot.clickOn("#deleteBtn");
        WaitForAsyncUtils.waitForFxEvents();
    }

    public boolean isSaveButtonEnabled() {
        return !robot.lookup("#saveBtn").queryAs(Button.class).isDisabled();
    }

    public boolean isDeleteButtonVisible() {
        try {
            Button deleteBtn = robot.lookup("#deleteBtn").queryAs(Button.class);
            return deleteBtn != null && deleteBtn.isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    // === Convenience Methods ===

    public void fillForm(String client, String description, String amount, LocalDate date, String status) {
        enterClient(client);
        enterDescription(description);
        enterAmount(amount);
        selectDate(date);
        selectStatus(status);
    }

    public void saveAndClose() {
        clickSave();
        // Wait for dialog to close
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void cancelAndClose() {
        clickCancel();
        WaitForAsyncUtils.waitForFxEvents();
    }
}
