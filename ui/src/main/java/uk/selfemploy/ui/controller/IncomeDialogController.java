package uk.selfemploy.ui.controller;
import uk.selfemploy.ui.component.AppDialog;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.common.enums.IncomeStatus;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.util.Stylesheets;
import uk.selfemploy.ui.viewmodel.IncomeDialogViewModel;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Controller for the Add/Edit Income Dialog.
 * Manages form bindings, validation display, and dialog actions.
 */
public class IncomeDialogController implements Initializable {

    // Header
    @FXML private Label dialogTitle;
    @FXML private Button closeBtn;

    // Form fields
    @FXML private DatePicker dateField;
    @FXML private TextField clientNameField;
    @FXML private TextField descriptionField;
    @FXML private TextField amountField;
    @FXML private ComboBox<IncomeCategory> categoryField;
    @FXML private ComboBox<IncomeStatus> statusField;
    @FXML private TextField referenceField;

    // Error labels
    @FXML private Label dateError;
    @FXML private Label clientNameError;
    @FXML private Label descriptionError;
    @FXML private Label amountError;
    @FXML private Label referenceError;

    // Buttons
    @FXML private Button deleteBtn;
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;

    private IncomeDialogViewModel viewModel;
    private Stage dialogStage;

    // Dependencies
    private IncomeService incomeService;
    private UUID businessId;
    private TaxYear taxYear;

    // Callbacks
    private Consumer<Income> onSaveCallback;
    private Runnable onDeleteCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCategoryField();
        setupStatusField();
        setupAmountField();
    }

    /**
     * Initializes the controller with required dependencies.
     */
    public void initializeWithDependencies(IncomeService incomeService, UUID businessId, TaxYear taxYear) {
        this.incomeService = incomeService;
        this.businessId = businessId;
        this.taxYear = taxYear;

        // Create ViewModel
        viewModel = new IncomeDialogViewModel(incomeService, businessId, taxYear);

        setupBindings();
        configureDatePicker();
    }

    private void setupCategoryField() {
        categoryField.setItems(FXCollections.observableArrayList(IncomeCategory.values()));
        categoryField.setValue(IncomeCategory.SALES);

        categoryField.setConverter(new StringConverter<>() {
            @Override
            public String toString(IncomeCategory category) {
                if (category == null) return "";
                return category.getDisplayName() + " (Box " + category.getSa103Box() + ")";
            }

            @Override
            public IncomeCategory fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });
    }

    private void setupStatusField() {
        statusField.setItems(FXCollections.observableArrayList(IncomeStatus.values()));
        statusField.setValue(IncomeStatus.PAID);

        statusField.setConverter(new StringConverter<>() {
            @Override
            public String toString(IncomeStatus status) {
                if (status == null) return "";
                return status.getDisplayName();
            }

            @Override
            public IncomeStatus fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });
    }

    private void setupAmountField() {
        // Only allow numeric input with decimal point and comma
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            // Allow digits, decimal point, and comma
            if (!newVal.matches("[0-9,\\.]*")) {
                amountField.setText(oldVal);
            }
        });
    }

    private void configureDatePicker() {
        if (taxYear == null) return;

        Stylesheets.attachComponents(dateField);

        // Set default date to today
        dateField.setValue(LocalDate.now());

        // Make date picker editable and add style class
        dateField.setEditable(true);
        dateField.getStyleClass().add("date-picker");

        // Restrict to tax year bounds
        dateField.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date == null || empty) return;

                getStyleClass().removeAll("income-date-outside", "income-date-future");

                // Disable dates outside tax year
                if (!taxYear.contains(date)) {
                    setDisable(true);
                    getStyleClass().add("income-date-outside");
                }

                // Disable future dates
                if (date.isAfter(LocalDate.now())) {
                    setDisable(true);
                    getStyleClass().add("income-date-future");
                }
            }
        });
    }

    private void setupBindings() {
        if (viewModel == null) return;

        // Bind form fields to ViewModel
        dateField.valueProperty().bindBidirectional(viewModel.dateProperty());
        clientNameField.textProperty().bindBidirectional(viewModel.clientNameProperty());
        descriptionField.textProperty().bindBidirectional(viewModel.descriptionProperty());
        amountField.textProperty().bindBidirectional(viewModel.amountProperty());
        categoryField.valueProperty().bindBidirectional(viewModel.categoryProperty());
        statusField.valueProperty().bindBidirectional(viewModel.statusProperty());
        referenceField.textProperty().bindBidirectional(viewModel.referenceProperty());

        // Bind error labels
        viewModel.dateErrorProperty().addListener((obs, oldVal, newVal) ->
            showFieldError(dateError, newVal));
        viewModel.clientNameErrorProperty().addListener((obs, oldVal, newVal) ->
            showFieldError(clientNameError, newVal));
        viewModel.descriptionErrorProperty().addListener((obs, oldVal, newVal) ->
            showFieldError(descriptionError, newVal));
        viewModel.amountErrorProperty().addListener((obs, oldVal, newVal) ->
            showFieldError(amountError, newVal));
        viewModel.referenceErrorProperty().addListener((obs, oldVal, newVal) ->
            showFieldError(referenceError, newVal));

        // Bind save button state
        saveBtn.disableProperty().bind(viewModel.formValidProperty().not());

        // Update dialog title and save button text
        viewModel.editModeProperty().addListener((obs, oldVal, newVal) -> {
            dialogTitle.setText(viewModel.getDialogTitle());
            saveBtn.setText(viewModel.getSaveButtonText());
            deleteBtn.setVisible(newVal);
            deleteBtn.setManaged(newVal);
        });

        // Set initial states
        dialogTitle.setText(viewModel.getDialogTitle());
        saveBtn.setText(viewModel.getSaveButtonText());
    }

    private void showFieldError(Label errorLabel, String errorMessage) {
        if (errorMessage != null && !errorMessage.isEmpty()) {
            errorLabel.setText(errorMessage);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        } else {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    // === Public Methods ===

    /**
     * Sets the dialog to edit mode with the given income.
     */
    public void setEditMode(Income income, String clientName, IncomeStatus status) {
        if (viewModel != null) {
            viewModel.loadIncome(income, clientName, status);
        }
    }

    /**
     * Sets the dialog stage reference.
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * Sets the callback for successful save.
     */
    public void setOnSaveCallback(Consumer<Income> callback) {
        this.onSaveCallback = callback;
        if (viewModel != null) {
            viewModel.setOnSaveCallback(callback);
        }
    }

    /**
     * Sets the callback for successful delete.
     */
    public void setOnDeleteCallback(Runnable callback) {
        this.onDeleteCallback = callback;
        if (viewModel != null) {
            viewModel.setOnDeleteCallback(callback);
        }
    }

    // === Action Handlers ===

    @FXML
    void handleSave(ActionEvent event) {
        if (viewModel == null) {
            return;
        }
        try {
            if (viewModel.save()) {
                closeDialog();
            }
        } catch (RuntimeException e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            AppDialog.error("Save Failed",
                "This income entry could not be saved and your changes were not stored:\n\n" + reason);
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        if (viewModel != null && viewModel.isDirty()) {
            if (showUnsavedChangesDialog()) {
                closeDialog();
            }
        } else {
            closeDialog();
        }
    }

    @FXML
    void handleClose(ActionEvent event) {
        handleCancel(event);
    }

    @FXML
    void handleDelete(ActionEvent event) {
        if (viewModel == null || !viewModel.isEditMode()) return;

        if (showDeleteConfirmation()) {
            if (viewModel.delete()) {
                closeDialog();
            }
        }
    }

    private boolean showUnsavedChangesDialog() {
        return AppDialog.confirm("Unsaved Changes",
            "You have unsaved changes\n\nAre you sure you want to close without saving?",
            "Discard", "Keep Editing");
    }

    private boolean showDeleteConfirmation() {
        return AppDialog.confirm("Delete Income?",
            "Are you sure you want to delete this income entry?\n\n" + String.format(
                "Client: %s%nAmount: £%s%nDate: %s%n%nThis action cannot be undone.",
                viewModel.getClientName(),
                viewModel.getAmount(),
                viewModel.getDate()),
            "Delete", "Cancel");
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Returns the ViewModel for testing.
     */
    public IncomeDialogViewModel getViewModel() {
        return viewModel;
    }
}
