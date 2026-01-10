package uk.selfemploy.ui.e2e.page;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDate;
import java.util.Set;

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
            return robot.lookup("#dialogTitle").tryQuery().isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public String getDialogTitle() {
        return robot.lookup("#dialogTitle").queryAs(Label.class).getText();
    }

    public boolean isAddMode() {
        return "Add Expense".equals(getDialogTitle());
    }

    public boolean isEditMode() {
        return "Edit Expense".equals(getDialogTitle());
    }

    // === Form Fields ===

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
        DatePicker datePicker = robot.lookup("#dateField").queryAs(DatePicker.class);
        robot.interact(() -> datePicker.setValue(date));
        WaitForAsyncUtils.waitForFxEvents();
    }

    public LocalDate getDate() {
        return robot.lookup("#dateField").queryAs(DatePicker.class).getValue();
    }

    public void selectCategory(String category) {
        robot.clickOn("#categoryField");
        WaitForAsyncUtils.waitForFxEvents();
        // Use keyboard navigation
        robot.type(javafx.scene.input.KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();
    }

    public String getCategory() {
        ComboBox<?> categoryCombo = robot.lookup("#categoryField").queryAs(ComboBox.class);
        Object value = categoryCombo.getValue();
        return value != null ? value.toString() : "";
    }

    public void setDeductible(boolean deductible) {
        CheckBox deductibleCheck = robot.lookup("#deductibleCheckbox").queryAs(CheckBox.class);
        if (deductibleCheck.isSelected() != deductible) {
            robot.clickOn("#deductibleCheckbox");
            WaitForAsyncUtils.waitForFxEvents();
        }
    }

    public boolean isDeductible() {
        return robot.lookup("#deductibleCheckbox").queryAs(CheckBox.class).isSelected();
    }

    // === Validation ===

    public boolean hasDescriptionError() {
        try {
            Label errorLabel = robot.lookup("#descriptionError").queryAs(Label.class);
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

    // === Receipt Section Methods (SE-308) ===

    /**
     * Checks if the receipt section is visible.
     */
    public boolean isReceiptSectionVisible() {
        try {
            VBox receiptSection = robot.lookup("#receiptSection").queryAs(VBox.class);
            return receiptSection != null && receiptSection.isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the receipt count text (e.g., "0 of 5").
     */
    public String getReceiptCountText() {
        try {
            return robot.lookup("#receiptCount").queryAs(Label.class).getText();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Gets the number of attached receipts.
     */
    public int getReceiptCount() {
        String text = getReceiptCountText();
        if (text.isEmpty()) return 0;
        try {
            return Integer.parseInt(text.split(" ")[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Checks if the receipt dropzone is visible (empty state).
     */
    public boolean isReceiptDropzoneVisible() {
        try {
            VBox dropzone = robot.lookup("#receiptDropzone").queryAs(VBox.class);
            return dropzone != null && dropzone.isVisible() && dropzone.isManaged();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the receipt grid is visible (has receipts).
     */
    public boolean isReceiptGridVisible() {
        try {
            FlowPane grid = robot.lookup("#receiptGrid").queryAs(FlowPane.class);
            return grid != null && grid.isVisible() && grid.isManaged();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the attach button exists and is visible.
     */
    public boolean isAttachButtonVisible() {
        try {
            Button attachBtn = robot.lookup("#attachBtn").queryAs(Button.class);
            return attachBtn != null && attachBtn.isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clicks the attach receipt button.
     * Note: This opens a native file chooser which cannot be automated.
     * For E2E tests, inject files directly via ViewModel.
     */
    public void clickAttachReceipt() {
        robot.clickOn("#attachBtn");
        WaitForAsyncUtils.waitForFxEvents();
    }

    /**
     * Gets all receipt thumbnail nodes.
     */
    public Set<Node> getReceiptThumbnails() {
        return robot.lookup(".receipt-thumbnail").queryAll();
    }

    /**
     * Gets the number of receipt thumbnails displayed.
     */
    public int getReceiptThumbnailCount() {
        return getReceiptThumbnails().size();
    }

    /**
     * Clicks the view button on a receipt thumbnail by index.
     */
    public void clickViewReceipt(int index) {
        Set<Node> thumbnails = getReceiptThumbnails();
        if (thumbnails.isEmpty() || index >= thumbnails.size()) {
            throw new IllegalArgumentException("Receipt thumbnail at index " + index + " not found");
        }
        Node thumbnail = thumbnails.toArray(new Node[0])[index];
        robot.clickOn(thumbnail.lookup(".btn-thumbnail-action"));
        WaitForAsyncUtils.waitForFxEvents();
    }

    /**
     * Clicks the remove button on a receipt thumbnail by index.
     */
    public void clickRemoveReceipt(int index) {
        Set<Node> thumbnails = getReceiptThumbnails();
        if (thumbnails.isEmpty() || index >= thumbnails.size()) {
            throw new IllegalArgumentException("Receipt thumbnail at index " + index + " not found");
        }
        Node thumbnail = thumbnails.toArray(new Node[0])[index];
        robot.clickOn(thumbnail.lookup(".btn-thumbnail-remove"));
        WaitForAsyncUtils.waitForFxEvents();
    }

    /**
     * Checks if the add receipt button (in grid mode) is visible.
     */
    public boolean isAddReceiptButtonVisible() {
        try {
            return !robot.lookup(".receipt-add-button").queryAll().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clicks the add receipt button in grid mode.
     */
    public void clickAddReceiptInGrid() {
        robot.clickOn(".receipt-add-button");
        WaitForAsyncUtils.waitForFxEvents();
    }

    // === Receipt Error Handling ===

    /**
     * Checks if the receipt error message is visible.
     */
    public boolean isReceiptErrorVisible() {
        try {
            HBox errorBox = robot.lookup("#receiptError").queryAs(HBox.class);
            return errorBox != null && errorBox.isVisible() && errorBox.isManaged();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the receipt error message text.
     */
    public String getReceiptErrorText() {
        try {
            return robot.lookup("#receiptErrorText").queryAs(Label.class).getText();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Gets the receipt error helper text.
     */
    public String getReceiptErrorHelper() {
        try {
            return robot.lookup("#receiptErrorHelper").queryAs(Label.class).getText();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Clicks the dismiss error button.
     */
    public void dismissReceiptError() {
        robot.clickOn("#dismissErrorBtn");
        WaitForAsyncUtils.waitForFxEvents();
    }

    // === Confirmation Dialog Handling ===

    /**
     * Checks if a confirmation dialog is open.
     */
    public boolean isConfirmDialogOpen() {
        try {
            return robot.lookup(".dialog-pane").tryQuery().isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clicks OK in a confirmation dialog.
     */
    public void confirmDialog() {
        robot.clickOn("OK");
        WaitForAsyncUtils.waitForFxEvents();
    }

    /**
     * Clicks Cancel in a confirmation dialog.
     */
    public void cancelDialog() {
        robot.clickOn("Cancel");
        WaitForAsyncUtils.waitForFxEvents();
    }

    /**
     * Checks if a thumbnail shows a PDF icon (vs image preview).
     */
    public boolean hasPdfIcon(int index) {
        Set<Node> thumbnails = getReceiptThumbnails();
        if (thumbnails.isEmpty() || index >= thumbnails.size()) {
            return false;
        }
        Node thumbnail = thumbnails.toArray(new Node[0])[index];
        return thumbnail.lookup(".pdf-icon") != null;
    }

    /**
     * Checks if the dropzone has the drag-over style applied.
     */
    public boolean hasDropzoneDragOverStyle() {
        try {
            VBox dropzone = robot.lookup("#receiptDropzone").queryAs(VBox.class);
            return dropzone != null && dropzone.getStyleClass().contains("drag-over");
        } catch (Exception e) {
            return false;
        }
    }
}
