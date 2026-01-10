package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.ReceiptMetadata;
import uk.selfemploy.core.service.ReceiptStorageService;
import uk.selfemploy.ui.viewmodel.ExpenseDialogViewModel;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Controller for the Add/Edit Expense dialog.
 * Manages form interaction, validation display, and dialog actions.
 */
public class ExpenseDialogController implements Initializable {

    // Header
    @FXML private Label dialogTitle;
    @FXML private Button closeBtn;

    // Form fields
    @FXML private DatePicker dateField;
    @FXML private Label dateError;
    @FXML private TextField descriptionField;
    @FXML private Label descriptionError;
    @FXML private TextField amountField;
    @FXML private Label amountError;
    @FXML private ComboBox<ExpenseCategory> categoryField;
    @FXML private Label categoryError;

    // Category help
    @FXML private VBox categoryHelpBox;
    @FXML private Label categoryHelpIcon;
    @FXML private Label categoryHelpTitle;
    @FXML private Label categoryHelpText;
    @FXML private VBox categoryHelpExamples;

    // Deductible
    @FXML private HBox deductibleRow;
    @FXML private CheckBox deductibleCheckbox;
    @FXML private Label deductibleLabel;
    @FXML private Label deductibleHelper;

    // Notes
    @FXML private TextArea notesField;

    // Receipt section (SE-308)
    @FXML private VBox receiptSection;
    @FXML private Label receiptCount;
    @FXML private StackPane receiptContainer;
    @FXML private VBox receiptDropzone;
    @FXML private Button attachBtn;
    @FXML private FlowPane receiptGrid;
    @FXML private HBox receiptError;
    @FXML private Label receiptErrorText;
    @FXML private Label receiptErrorHelper;
    @FXML private Button dismissErrorBtn;

    // Footer buttons
    @FXML private Button deleteBtn;
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;

    // ViewModel
    private ExpenseDialogViewModel viewModel;

    // Service and context
    private ExpenseService expenseService;
    private ReceiptStorageService receiptStorageService;
    private UUID businessId;
    private TaxYear taxYear;
    private boolean cisBusiness = false;

    // Stage reference
    private Stage dialogStage;

    // Callbacks
    private Consumer<Expense> onSave;
    private Runnable onDelete;
    private Runnable onClose;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDatePicker();
        setupAmountFormatter();
    }

    private void initializeViewModel() {
        if (expenseService == null) return;

        viewModel = new ExpenseDialogViewModel(expenseService);
        viewModel.setBusinessId(businessId);
        viewModel.setTaxYear(taxYear);
        viewModel.setCisBusiness(cisBusiness);

        // Set receipt storage service if available
        if (receiptStorageService != null) {
            viewModel.setReceiptStorageService(receiptStorageService);
        }

        setupCategoryDropdown();
        setupBindings();
        setupReceiptBindings();

        // Set callbacks
        viewModel.setOnSave(expense -> {
            if (onSave != null) {
                onSave.accept(expense);
            }
        });

        viewModel.setOnDelete(() -> {
            if (onDelete != null) {
                onDelete.run();
            }
        });
    }

    private void setupDatePicker() {
        dateField.setValue(LocalDate.now());

        // Configure date picker to restrict to tax year
        dateField.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                if (taxYear != null) {
                    boolean outsideTaxYear = date.isBefore(taxYear.startDate()) ||
                                            date.isAfter(taxYear.endDate());
                    boolean inFuture = date.isAfter(LocalDate.now());

                    setDisable(outsideTaxYear || inFuture);

                    if (outsideTaxYear) {
                        setStyle("-fx-background-color: #ffc0c0;");
                    } else if (inFuture) {
                        setStyle("-fx-background-color: #e0e0e0;");
                    }
                }
            }
        });
    }

    private void setupAmountFormatter() {
        // Only allow numeric input with decimal point
        amountField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) return;

            // Allow empty, or valid decimal format
            if (!newValue.matches("\\d*\\.?\\d{0,2}")) {
                amountField.setText(oldValue);
            }
        });
    }

    private void setupCategoryDropdown() {
        if (viewModel == null) return;

        categoryField.getItems().clear();
        categoryField.getItems().addAll(viewModel.getAvailableCategories());

        // Custom cell factory for display
        categoryField.setConverter(new StringConverter<>() {
            @Override
            public String toString(ExpenseCategory category) {
                if (category == null) return "";
                return category.getDisplayName() + " (Box " + category.getSa103Box() + ")";
            }

            @Override
            public ExpenseCategory fromString(String string) {
                return null;
            }
        });

        categoryField.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ExpenseCategory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getDisplayName() + " (Box " + item.getSa103Box() + ")");

                    // Add warning indicator for non-allowable categories
                    if (!item.isAllowable()) {
                        setText(getText() + " [!]");
                        setStyle("-fx-text-fill: #856404;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    private void setupBindings() {
        if (viewModel == null) return;

        // Two-way bindings for form fields
        dateField.valueProperty().bindBidirectional(viewModel.dateProperty());
        descriptionField.textProperty().bindBidirectional(viewModel.descriptionProperty());
        amountField.textProperty().bindBidirectional(viewModel.amountProperty());
        categoryField.valueProperty().bindBidirectional(viewModel.categoryProperty());
        deductibleCheckbox.selectedProperty().bindBidirectional(viewModel.deductibleProperty());
        notesField.textProperty().bindBidirectional(viewModel.notesProperty());

        // Error label visibility
        viewModel.dateErrorProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasError = newVal != null && !newVal.isEmpty();
            dateError.setText(newVal);
            dateError.setVisible(hasError);
            dateError.setManaged(hasError);
        });

        viewModel.descriptionErrorProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasError = newVal != null && !newVal.isEmpty();
            descriptionError.setText(newVal);
            descriptionError.setVisible(hasError);
            descriptionError.setManaged(hasError);
        });

        viewModel.amountErrorProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasError = newVal != null && !newVal.isEmpty();
            amountError.setText(newVal);
            amountError.setVisible(hasError);
            amountError.setManaged(hasError);
        });

        viewModel.categoryErrorProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasError = newVal != null && !newVal.isEmpty();
            categoryError.setText(newVal);
            categoryError.setVisible(hasError);
            categoryError.setManaged(hasError);
        });

        // Save button enable state
        saveBtn.disableProperty().bind(viewModel.formValidProperty().not());

        // Deductible checkbox enable state
        deductibleCheckbox.disableProperty().bind(viewModel.deductibleEnabledProperty().not());

        // Deductible helper text
        viewModel.deductibleEnabledProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                deductibleHelper.setText("This expense will reduce your taxable profit");
                deductibleHelper.getStyleClass().remove("warning");
            } else {
                deductibleHelper.setText("This category is not allowable for tax relief");
                if (!deductibleHelper.getStyleClass().contains("warning")) {
                    deductibleHelper.getStyleClass().add("warning");
                }
            }
        });

        // Category help box
        viewModel.categoryProperty().addListener((obs, oldVal, newVal) -> {
            updateCategoryHelp();
        });

        // Edit mode bindings
        viewModel.editModeProperty().addListener((obs, oldVal, newVal) -> {
            deleteBtn.setVisible(newVal);
            deleteBtn.setManaged(newVal);
            dialogTitle.setText(viewModel.getDialogTitle());
            saveBtn.setText(viewModel.getSaveButtonText());
        });
    }

    private void updateCategoryHelp() {
        if (viewModel == null) return;

        ExpenseCategory category = viewModel.getCategory();
        boolean hasCategory = category != null;

        categoryHelpBox.setVisible(hasCategory);
        categoryHelpBox.setManaged(hasCategory);

        if (hasCategory) {
            categoryHelpTitle.setText(viewModel.getCategoryHelpTitle());
            categoryHelpText.setText(viewModel.getCategoryHelpText());

            // Update examples
            categoryHelpExamples.getChildren().clear();
            for (String example : viewModel.getCategoryHelpExamples()) {
                Label exampleLabel = new Label("• " + example);
                exampleLabel.getStyleClass().add("help-box-example");
                categoryHelpExamples.getChildren().add(exampleLabel);
            }

            // Warning mode for non-allowable
            if (viewModel.isCategoryWarning()) {
                categoryHelpBox.getStyleClass().add("warning");
                categoryHelpIcon.setText("[!]");
            } else {
                categoryHelpBox.getStyleClass().remove("warning");
                categoryHelpIcon.setText("[i]");
            }
        }
    }

    private void setupReceiptBindings() {
        if (viewModel == null) return;

        // Receipt count text
        viewModel.receiptCountTextProperty().addListener((obs, oldVal, newVal) -> {
            if (receiptCount != null) {
                receiptCount.setText(newVal);
            }
        });

        // Dropzone visibility
        viewModel.dropzoneVisibleProperty().addListener((obs, oldVal, newVal) -> {
            if (receiptDropzone != null) {
                receiptDropzone.setVisible(newVal);
                receiptDropzone.setManaged(newVal);
            }
        });

        // Receipt grid visibility
        viewModel.receiptGridVisibleProperty().addListener((obs, oldVal, newVal) -> {
            if (receiptGrid != null) {
                receiptGrid.setVisible(newVal);
                receiptGrid.setManaged(newVal);
            }
        });

        // Attach button disable state
        viewModel.canAddMoreReceiptsProperty().addListener((obs, oldVal, newVal) -> {
            if (attachBtn != null) {
                attachBtn.setDisable(!newVal);
            }
        });

        // Error visibility
        viewModel.hasReceiptErrorProperty().addListener((obs, oldVal, newVal) -> {
            if (receiptError != null) {
                receiptError.setVisible(newVal);
                receiptError.setManaged(newVal);
            }
        });

        // Error message text
        viewModel.receiptErrorMessageProperty().addListener((obs, oldVal, newVal) -> {
            if (receiptErrorText != null) {
                receiptErrorText.setText(newVal);
            }
        });

        // Error helper text
        viewModel.receiptErrorHelperProperty().addListener((obs, oldVal, newVal) -> {
            if (receiptErrorHelper != null) {
                receiptErrorHelper.setText(newVal);
            }
        });

        // Listen for changes to the receipts list to update the UI
        viewModel.getReceipts().addListener((javafx.collections.ListChangeListener<ReceiptMetadata>) change -> {
            updateReceiptGrid();
        });

        // Set initial values
        receiptCount.setText(viewModel.getReceiptCountText());
        receiptDropzone.setVisible(viewModel.isDropzoneVisible());
        receiptDropzone.setManaged(viewModel.isDropzoneVisible());
        receiptGrid.setVisible(viewModel.isReceiptGridVisible());
        receiptGrid.setManaged(viewModel.isReceiptGridVisible());
        receiptError.setVisible(false);
        receiptError.setManaged(false);
    }

    private void updateReceiptGrid() {
        if (receiptGrid == null || viewModel == null) return;

        receiptGrid.getChildren().clear();

        // Add thumbnail for each receipt
        for (ReceiptMetadata receipt : viewModel.getReceipts()) {
            VBox thumbnail = createReceiptThumbnail(receipt);
            receiptGrid.getChildren().add(thumbnail);
        }

        // Add "Add More" button if under limit
        if (viewModel.canAddMoreReceipts()) {
            VBox addButton = createAddReceiptButton();
            receiptGrid.getChildren().add(addButton);
        }
    }

    private VBox createReceiptThumbnail(ReceiptMetadata receipt) {
        VBox thumbnail = new VBox(4);
        thumbnail.getStyleClass().add("receipt-thumbnail");
        thumbnail.setAlignment(Pos.CENTER);

        // Image/PDF icon
        if (receipt.isPdf()) {
            // PDF icon
            VBox pdfIcon = new VBox();
            pdfIcon.getStyleClass().add("receipt-thumbnail-pdf");
            pdfIcon.setAlignment(Pos.CENTER);
            Label pdfLabel = new Label("PDF");
            pdfLabel.getStyleClass().add("receipt-thumbnail-pdf-text");
            pdfIcon.getChildren().add(pdfLabel);
            thumbnail.getChildren().add(pdfIcon);
        } else {
            // Image thumbnail
            try {
                Image image = new Image(receipt.storagePath().toUri().toString(), 60, 60, true, true);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(60);
                imageView.setFitHeight(60);
                imageView.setPreserveRatio(true);
                imageView.getStyleClass().add("receipt-thumbnail-image");
                thumbnail.getChildren().add(imageView);
            } catch (Exception e) {
                // Fallback to generic icon
                Label imgIcon = new Label("[IMG]");
                imgIcon.getStyleClass().add("receipt-thumbnail-image");
                thumbnail.getChildren().add(imgIcon);
            }
        }

        // Filename
        String displayName = receipt.originalFilename();
        if (displayName.length() > 12) {
            displayName = displayName.substring(0, 9) + "...";
        }
        Label filename = new Label(displayName);
        filename.getStyleClass().add("receipt-thumbnail-filename");
        thumbnail.getChildren().add(filename);

        // Action buttons
        HBox actions = new HBox(4);
        actions.setAlignment(Pos.CENTER);

        Button viewBtn = new Button("View");
        viewBtn.getStyleClass().add("btn-thumbnail-action");
        viewBtn.setOnAction(e -> handleViewReceipt(receipt));

        Button removeBtn = new Button("X");
        removeBtn.getStyleClass().add("btn-thumbnail-remove");
        removeBtn.setOnAction(e -> handleRemoveReceipt(receipt));

        actions.getChildren().addAll(viewBtn, removeBtn);
        thumbnail.getChildren().add(actions);

        return thumbnail;
    }

    private VBox createAddReceiptButton() {
        VBox addButton = new VBox(4);
        addButton.getStyleClass().add("receipt-add-button");
        addButton.setAlignment(Pos.CENTER);
        addButton.setOnMouseClicked(e -> openFileChooser());

        Label addIcon = new Label("+");
        addIcon.getStyleClass().add("receipt-add-icon");

        Label addLabel = new Label("Add");
        addLabel.getStyleClass().add("receipt-add-label");

        addButton.getChildren().addAll(addIcon, addLabel);

        return addButton;
    }

    private void handleViewReceipt(ReceiptMetadata receipt) {
        if (receipt == null || receipt.storagePath() == null) return;

        try {
            File file = receipt.storagePath().toFile();
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {
            showReceiptErrorAlert("Could not open receipt", e.getMessage());
        }
    }

    private void handleRemoveReceipt(ReceiptMetadata receipt) {
        if (receipt == null || viewModel == null) return;

        // Show confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Receipt?");
        confirm.setHeaderText("Remove this receipt?");
        confirm.setContentText("File: " + receipt.originalFilename());

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                viewModel.removeReceipt(receipt.receiptId());
            }
        });
    }

    private void showReceiptErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void openFileChooser() {
        if (viewModel == null || !viewModel.canAddMoreReceipts()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Receipt");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif"),
            new FileChooser.ExtensionFilter("PDF Documents", "*.pdf"),
            new FileChooser.ExtensionFilter("All Supported", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.pdf")
        );

        // Allow multiple file selection
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(dialogStage);
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            viewModel.attachReceipts(selectedFiles);
        }
    }

    // === Receipt Event Handlers ===

    @FXML
    void handleAttachReceipt(ActionEvent event) {
        openFileChooser();
    }

    @FXML
    void handleDragOver(DragEvent event) {
        if (viewModel != null && viewModel.canAddMoreReceipts()) {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                if (receiptDropzone != null && !receiptDropzone.getStyleClass().contains("drag-over")) {
                    receiptDropzone.getStyleClass().add("drag-over");
                }
            }
        }
        event.consume();
    }

    @FXML
    void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles() && viewModel != null) {
            viewModel.attachReceipts(db.getFiles());
            success = true;
        }

        event.setDropCompleted(success);
        event.consume();

        // Remove drag-over style
        if (receiptDropzone != null) {
            receiptDropzone.getStyleClass().remove("drag-over");
        }
    }

    @FXML
    void handleDragExited(DragEvent event) {
        if (receiptDropzone != null) {
            receiptDropzone.getStyleClass().remove("drag-over");
        }
        event.consume();
    }

    @FXML
    void handleDismissReceiptError(ActionEvent event) {
        if (viewModel != null) {
            viewModel.clearReceiptError();
        }
    }

    // === Event Handlers ===

    @FXML
    void handleSave(ActionEvent event) {
        if (viewModel != null) {
            viewModel.save();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        if (viewModel != null && viewModel.isDirty()) {
            if (!showUnsavedChangesDialog()) {
                return;
            }
        }
        closeDialog();
    }

    @FXML
    void handleClose(ActionEvent event) {
        if (viewModel != null && viewModel.isDirty()) {
            if (!showUnsavedChangesDialog()) {
                return;
            }
        }
        closeDialog();
    }

    @FXML
    void handleDelete(ActionEvent event) {
        if (viewModel != null && showDeleteConfirmation()) {
            viewModel.delete();
        }
    }

    private boolean showUnsavedChangesDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You have unsaved changes");
        alert.setContentText("Do you want to discard your changes?");

        ButtonType discardBtn = new ButtonType("Discard", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(discardBtn, cancelBtn);

        return alert.showAndWait()
            .map(result -> result == discardBtn)
            .orElse(false);
    }

    private boolean showDeleteConfirmation() {
        if (viewModel == null) return false;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Expense?");
        alert.setHeaderText("Are you sure you want to delete this expense entry?");
        alert.setContentText(
            "Description: " + viewModel.getDescription() + "\n" +
            "Amount: £" + viewModel.getAmount() + "\n" +
            (viewModel.getCategory() != null ?
                "Category: " + viewModel.getCategory().getDisplayName() : "") + "\n\n" +
            "This action cannot be undone."
        );

        return alert.showAndWait()
            .map(result -> result == ButtonType.OK)
            .orElse(false);
    }

    private void closeDialog() {
        if (onClose != null) {
            onClose.run();
        }
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    // === Public API ===

    /**
     * Sets the dialog to add mode with default values.
     */
    public void setAddMode() {
        initializeViewModel();
        if (viewModel != null) {
            viewModel.resetForm();
            dialogTitle.setText(viewModel.getDialogTitle());
            saveBtn.setText(viewModel.getSaveButtonText());
            deleteBtn.setVisible(false);
            deleteBtn.setManaged(false);
        }
    }

    /**
     * Sets the dialog to edit mode with the given expense.
     */
    public void setEditMode(Expense expense) {
        initializeViewModel();
        if (viewModel != null) {
            viewModel.loadExpense(expense);
            dialogTitle.setText(viewModel.getDialogTitle());
            saveBtn.setText(viewModel.getSaveButtonText());
            deleteBtn.setVisible(true);
            deleteBtn.setManaged(true);
            updateCategoryHelp();
        }
    }

    /**
     * Sets the expense service.
     */
    public void setExpenseService(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /**
     * Sets the receipt storage service.
     */
    public void setReceiptStorageService(ReceiptStorageService receiptStorageService) {
        this.receiptStorageService = receiptStorageService;
        if (viewModel != null) {
            viewModel.setReceiptStorageService(receiptStorageService);
        }
    }

    /**
     * Sets the business ID.
     */
    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    /**
     * Sets the tax year.
     */
    public void setTaxYear(TaxYear taxYear) {
        this.taxYear = taxYear;
        setupDatePicker(); // Reconfigure date picker with new tax year
    }

    /**
     * Sets whether this is a CIS business.
     */
    public void setCisBusiness(boolean cisBusiness) {
        this.cisBusiness = cisBusiness;
    }

    /**
     * Sets the dialog stage reference.
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Sets the onSave callback.
     */
    public void setOnSave(Consumer<Expense> onSave) {
        this.onSave = onSave;
        if (viewModel != null) {
            viewModel.setOnSave(expense -> {
                if (this.onSave != null) {
                    this.onSave.accept(expense);
                }
            });
        }
    }

    /**
     * Sets the onDelete callback.
     */
    public void setOnDelete(Runnable onDelete) {
        this.onDelete = onDelete;
        if (viewModel != null) {
            viewModel.setOnDelete(() -> {
                if (this.onDelete != null) {
                    this.onDelete.run();
                }
            });
        }
    }

    /**
     * Sets the onClose callback.
     */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /**
     * Returns the underlying ViewModel for testing purposes.
     */
    public ExpenseDialogViewModel getViewModel() {
        return viewModel;
    }
}
