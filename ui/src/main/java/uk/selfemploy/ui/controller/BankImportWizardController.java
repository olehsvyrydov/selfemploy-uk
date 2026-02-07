package uk.selfemploy.ui.controller;

import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.viewmodel.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Consumer;

import uk.selfemploy.ui.service.CsvTransactionParser;

/**
 * Controller for the CSV Bank Import Wizard.
 * Handles the 4-step wizard flow for importing bank transactions from CSV files.
 *
 * SE-601: CSV Bank Import Wizard
 */
public class BankImportWizardController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(BankImportWizardController.class);
    private static final int MAX_IMPORT_DESCRIPTION_LENGTH = 100;
    private static final long MAX_CSV_FILE_SIZE = 50 * 1024 * 1024; // 50 MB

    // === Wizard Progress ===
    @FXML private HBox progressIndicator;
    @FXML private VBox step1Circle;
    @FXML private VBox step2Circle;
    @FXML private VBox step3Circle;
    @FXML private VBox step4Circle;

    // === Step 1: File Selection ===
    @FXML private VBox step1Content;
    @FXML private VBox dropZone;
    @FXML private VBox fileSelectedBox;
    @FXML private Label fileNameLabel;
    @FXML private Label fileSizeLabel;
    @FXML private HBox bankDetectedBox;
    @FXML private Label bankNameLabel;
    @FXML private HBox unknownFormatBox;

    // === Step 2: Column Mapping ===
    @FXML private VBox step2Content;
    @FXML private HBox csvHeadersPreview;
    @FXML private ComboBox<String> dateColumnCombo;
    @FXML private ComboBox<String> dateFormatCombo;
    @FXML private VBox dateFormatBox;
    @FXML private ComboBox<String> descriptionColumnCombo;
    @FXML private ComboBox<String> amountColumnCombo;
    @FXML private CheckBox separateColumnsCheckbox;
    @FXML private VBox separateColumnsBox;
    @FXML private ComboBox<String> incomeColumnCombo;
    @FXML private ComboBox<String> expenseColumnCombo;
    @FXML private ComboBox<String> categoryColumnCombo;
    @FXML private TableView<List<String>> samplePreviewTable;

    // === Step 3: Preview & Categorize ===
    @FXML private VBox step3Content;
    @FXML private Label incomeCountLabel;
    @FXML private Label incomeTotalLabel;
    @FXML private Label expenseCountLabel;
    @FXML private Label expenseTotalLabel;
    @FXML private Label duplicateCountLabel;
    @FXML private Label uncategorizedCountLabel;
    @FXML private HBox duplicateWarning;
    @FXML private Label duplicateWarningTitle;
    @FXML private ComboBox<TransactionFilter> filterCombo;
    @FXML private TextField searchField;
    @FXML private Button bulkCategoryBtn;
    @FXML private TableView<ImportedTransactionRow> transactionsTable;
    @FXML private TableColumn<ImportedTransactionRow, Boolean> selectColumn;
    @FXML private TableColumn<ImportedTransactionRow, String> statusColumn;
    @FXML private TableColumn<ImportedTransactionRow, String> dateColumn;
    @FXML private TableColumn<ImportedTransactionRow, String> descriptionColumn;
    @FXML private TableColumn<ImportedTransactionRow, String> amountColumn;
    @FXML private TableColumn<ImportedTransactionRow, String> typeColumn;
    @FXML private TableColumn<ImportedTransactionRow, ExpenseCategory> categoryColumn;
    @FXML private TableColumn<ImportedTransactionRow, String> confidenceColumn;
    @FXML private HBox suggestionPanel;
    @FXML private Label suggestionText;
    @FXML private Label suggestionReason;

    // === Step 4: Confirm & Import ===
    @FXML private VBox step4Content;
    @FXML private Label confirmIncomeCount;
    @FXML private Label confirmIncomeTotal;
    @FXML private Label confirmExpenseCount;
    @FXML private Label confirmExpenseTotal;
    @FXML private VBox categoryBreakdown;
    @FXML private Label skippedLabel;
    @FXML private Label sourceFileLabel;
    @FXML private Button importBtn;
    @FXML private VBox importProgress;
    @FXML private ProgressIndicator progressIndicatorSpinner;
    @FXML private Label progressLabel;
    @FXML private ProgressBar progressBar;

    // === Footer ===
    @FXML private Button cancelBtn;
    @FXML private Button backBtn;
    @FXML private Button nextBtn;

    private BankImportWizardViewModel viewModel;
    private Stage dialogStage;
    private Consumer<List<ImportedTransactionRow>> onImportCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        viewModel = new BankImportWizardViewModel();

        setupStepBindings();
        setupStep1Bindings();
        setupStep2Bindings();
        setupStep3Bindings();
        setupStep4Bindings();
        setupNavigationBindings();
    }

    /**
     * Sets the dialog stage for closing.
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * Sets the callback to be invoked when import completes.
     */
    public void setOnImportCallback(Consumer<List<ImportedTransactionRow>> callback) {
        this.onImportCallback = callback;
    }

    /**
     * Returns the ViewModel for testing.
     */
    public BankImportWizardViewModel getViewModel() {
        return viewModel;
    }

    // =====================================================
    // SETUP BINDINGS
    // =====================================================

    private void setupStepBindings() {
        // Update step circles based on current step
        viewModel.currentStepProperty().addListener((obs, oldVal, newVal) -> {
            updateStepIndicators(newVal.intValue());
            updateContentVisibility(newVal.intValue());
            updateNavigationButtons();
        });

        // Initial state
        updateStepIndicators(1);
        updateContentVisibility(1);
    }

    private void updateStepIndicators(int currentStep) {
        VBox[] steps = {step1Circle, step2Circle, step3Circle, step4Circle};

        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == null) continue;

            int stepNumber = i + 1;
            steps[i].getStyleClass().removeAll("step-active", "step-completed");

            if (stepNumber < currentStep) {
                steps[i].getStyleClass().add("step-completed");
            } else if (stepNumber == currentStep) {
                steps[i].getStyleClass().add("step-active");
            }
        }
    }

    private void updateContentVisibility(int currentStep) {
        if (step1Content != null) {
            step1Content.setVisible(currentStep == 1);
            step1Content.setManaged(currentStep == 1);
        }
        if (step2Content != null) {
            step2Content.setVisible(currentStep == 2);
            step2Content.setManaged(currentStep == 2);
        }
        if (step3Content != null) {
            step3Content.setVisible(currentStep == 3);
            step3Content.setManaged(currentStep == 3);
        }
        if (step4Content != null) {
            step4Content.setVisible(currentStep == 4);
            step4Content.setManaged(currentStep == 4);
        }
    }

    private void setupStep1Bindings() {
        // File selection state
        viewModel.selectedFileProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasFile = newVal != null;

            if (dropZone != null) {
                dropZone.setVisible(!hasFile);
                dropZone.setManaged(!hasFile);
            }
            if (fileSelectedBox != null) {
                fileSelectedBox.setVisible(hasFile);
                fileSelectedBox.setManaged(hasFile);
            }

            if (hasFile && fileNameLabel != null) {
                fileNameLabel.setText(newVal.getName());
                fileSizeLabel.setText(formatFileSize(newVal.length()) + " - " +
                        viewModel.getRowCount() + " rows detected");
            }

            updateNavigationButtons();
        });

        // Bank format detection
        viewModel.detectedBankFormatProperty().addListener((obs, oldVal, newVal) -> {
            boolean isKnown = newVal != BankFormat.UNKNOWN;

            if (bankDetectedBox != null) {
                bankDetectedBox.setVisible(isKnown);
                bankDetectedBox.setManaged(isKnown);
            }
            if (unknownFormatBox != null) {
                unknownFormatBox.setVisible(!isKnown && viewModel.isFileSelected());
                unknownFormatBox.setManaged(!isKnown && viewModel.isFileSelected());
            }
            if (bankNameLabel != null && isKnown) {
                bankNameLabel.setText(newVal.getDetectionMessage());
            }
        });
    }

    private void setupStep2Bindings() {
        if (dateFormatCombo != null) {
            dateFormatCombo.setItems(FXCollections.observableArrayList(
                    viewModel.getAvailableDateFormats()));
            dateFormatCombo.valueProperty().bindBidirectional(
                    viewModel.getColumnMapping().dateFormatProperty());
        }

        if (dateColumnCombo != null) {
            dateColumnCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                viewModel.getColumnMapping().setDateColumn(newVal);
                if (dateFormatBox != null) {
                    dateFormatBox.setVisible(newVal != null);
                    dateFormatBox.setManaged(newVal != null);
                }
                updateNavigationButtons();
            });
        }

        if (descriptionColumnCombo != null) {
            descriptionColumnCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                viewModel.getColumnMapping().setDescriptionColumn(newVal);
                updateNavigationButtons();
            });
        }

        if (amountColumnCombo != null) {
            amountColumnCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                viewModel.getColumnMapping().setAmountColumn(newVal);
                updateNavigationButtons();
            });
        }

        if (separateColumnsCheckbox != null) {
            separateColumnsCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                viewModel.getColumnMapping().setSeparateAmountColumns(newVal);
                if (separateColumnsBox != null) {
                    separateColumnsBox.setVisible(newVal);
                    separateColumnsBox.setManaged(newVal);
                }
                if (amountColumnCombo != null) {
                    amountColumnCombo.setDisable(newVal);
                }
                updateNavigationButtons();
            });
        }

        if (incomeColumnCombo != null) {
            incomeColumnCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                viewModel.getColumnMapping().setIncomeColumn(newVal);
                updateNavigationButtons();
            });
        }

        if (expenseColumnCombo != null) {
            expenseColumnCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                viewModel.getColumnMapping().setExpenseColumn(newVal);
                updateNavigationButtons();
            });
        }

        if (categoryColumnCombo != null) {
            categoryColumnCombo.valueProperty().addListener((obs, oldVal, newVal) ->
                    viewModel.getColumnMapping().setCategoryColumn(newVal));
        }
    }

    private void setupStep3Bindings() {
        // Filter combo
        if (filterCombo != null) {
            filterCombo.setItems(FXCollections.observableArrayList(TransactionFilter.values()));
            filterCombo.setValue(TransactionFilter.ALL);
            filterCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                viewModel.setTransactionFilter(newVal);
                refreshTransactionsTable();
            });
        }

        // Search field
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                viewModel.setSearchText(newVal);
                refreshTransactionsTable();
            });
        }

        // Bulk category button
        if (bulkCategoryBtn != null) {
            bulkCategoryBtn.disableProperty().bind(
                    Bindings.createBooleanBinding(
                            () -> viewModel.getSelectedCount() == 0,
                            viewModel.currentStepProperty()
                    )
            );
        }

        setupTransactionsTable();
    }

    private void setupTransactionsTable() {
        if (transactionsTable == null) return;

        // Select column with checkboxes
        if (selectColumn != null) {
            selectColumn.setCellFactory(col -> new CheckBoxTableCell<>() {
                @Override
                public void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        ImportedTransactionRow row = getTableView().getItems().get(getIndex());
                        CheckBox checkBox = new CheckBox();
                        checkBox.setSelected(viewModel.isSelected(row.id()));
                        checkBox.setOnAction(e -> viewModel.toggleSelection(row.id()));
                        setGraphic(checkBox);
                    }
                }
            });
        }

        // Status column
        if (statusColumn != null) {
            statusColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().status().getIcon()));
            statusColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Label badge = new Label(item);
                        ImportedTransactionRow row = getTableView().getItems().get(getIndex());
                        badge.getStyleClass().add(row.status().getCssClass());
                        setGraphic(badge);
                    }
                }
            });
        }

        // Date column
        if (dateColumn != null) {
            dateColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getFormattedDate()));
        }

        // Description column
        if (descriptionColumn != null) {
            descriptionColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().description()));
        }

        // Amount column
        if (amountColumn != null) {
            amountColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getFormattedAmountWithSign()));
            amountColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        ImportedTransactionRow row = getTableView().getItems().get(getIndex());
                        if (row.type() == TransactionType.INCOME) {
                            setStyle("-fx-text-fill: -fx-success; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #fd7e14; -fx-font-weight: bold;");
                        }
                    }
                }
            });
        }

        // Type column
        if (typeColumn != null) {
            typeColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().type().getDisplayName()));
            typeColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Label badge = new Label(item);
                        ImportedTransactionRow row = getTableView().getItems().get(getIndex());
                        badge.getStyleClass().add(row.type().getBadgeCssClass());
                        setGraphic(badge);
                    }
                }
            });
        }

        // Category column with ComboBox
        if (categoryColumn != null) {
            categoryColumn.setCellFactory(col -> new TableCell<>() {
                private final ComboBox<ExpenseCategory> combo = new ComboBox<>();

                {
                    combo.getItems().addAll(ExpenseCategory.values());
                    combo.setOnAction(e -> {
                        ImportedTransactionRow row = getTableView().getItems().get(getIndex());
                        viewModel.updateTransactionCategory(row.id(), combo.getValue());
                        refreshSummaryStats();
                    });
                }

                @Override
                protected void updateItem(ExpenseCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        combo.setValue(item);
                        setGraphic(combo);
                    }
                }
            });
            categoryColumn.setCellValueFactory(data ->
                    new javafx.beans.property.SimpleObjectProperty<>(data.getValue().category()));
        }

        // Confidence column
        if (confidenceColumn != null) {
            confidenceColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getConfidenceDisplay()));
            confidenceColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        ImportedTransactionRow row = getTableView().getItems().get(getIndex());
                        getStyleClass().add(row.getConfidenceCssClass());
                    }
                }
            });
        }

        // Row selection
        transactionsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> updateSuggestionPanel(newVal));
    }

    private void setupStep4Bindings() {
        // Import button binding
        if (importBtn != null) {
            viewModel.importingProperty().addListener((obs, oldVal, newVal) -> {
                importBtn.setDisable(newVal);
            });
        }

        // Progress binding
        if (progressBar != null) {
            progressBar.progressProperty().bind(viewModel.importProgressProperty());
        }

        // Import progress visibility
        if (importProgress != null) {
            importProgress.visibleProperty().bind(viewModel.importingProperty());
            importProgress.managedProperty().bind(viewModel.importingProperty());
        }
    }

    private void setupNavigationBindings() {
        if (backBtn != null) {
            backBtn.setOnAction(this::handleBack);
        }
        if (nextBtn != null) {
            nextBtn.setOnAction(this::handleNext);
        }
        if (cancelBtn != null) {
            cancelBtn.setOnAction(this::handleCancel);
        }

        updateNavigationButtons();
    }

    private void updateNavigationButtons() {
        if (backBtn != null) {
            backBtn.setDisable(!viewModel.canGoPrevious());
            backBtn.setVisible(viewModel.getCurrentStep() > 1);
            backBtn.setManaged(viewModel.getCurrentStep() > 1);
        }

        if (nextBtn != null) {
            nextBtn.setDisable(!viewModel.canGoNext());

            String buttonText = switch (viewModel.getCurrentStep()) {
                case 1 -> "Next: Map Columns";
                case 2 -> "Next: Preview";
                case 3 -> "Next: Confirm";
                case 4 -> viewModel.getImportButtonText();
                default -> "Next";
            };
            nextBtn.setText(buttonText);

            // Change to primary/success for final step
            nextBtn.getStyleClass().remove("button-success");
            if (viewModel.getCurrentStep() == 4) {
                nextBtn.getStyleClass().add("button-success");
            }
        }
    }

    // =====================================================
    // DRAG & DROP HANDLERS
    // =====================================================

    @FXML
    void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            File file = event.getDragboard().getFiles().get(0);
            if (isValidCsvFile(file)) {
                event.acceptTransferModes(TransferMode.COPY);
                dropZone.getStyleClass().add("drag-over");
            }
        }
        event.consume();
    }

    @FXML
    void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles()) {
            File file = db.getFiles().get(0);
            if (isValidCsvFile(file)) {
                loadCsvFile(file);
                success = true;
            }
        }

        event.setDropCompleted(success);
        event.consume();
        dropZone.getStyleClass().remove("drag-over");
    }

    @FXML
    void handleDragExited(DragEvent event) {
        dropZone.getStyleClass().remove("drag-over");
        event.consume();
    }

    @FXML
    void handleBrowseFiles(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Bank Statement CSV");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(dialogStage);
        if (file != null) {
            loadCsvFile(file);
        }
    }

    @FXML
    void handleChangeFile(ActionEvent event) {
        viewModel.clearFile();
    }

    private boolean isValidCsvFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".csv") || name.endsWith(".txt");
    }

    private void loadCsvFile(File file) {
        // Reject files larger than 50 MB to prevent memory issues
        if (file.length() > MAX_CSV_FILE_SIZE) {
            showError("File too large",
                new IllegalArgumentException("Maximum file size is 50 MB. Selected file is "
                    + formatFileSize(file.length()) + "."));
            return;
        }

        try (var reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            // Read first line as headers using proper CSV parsing (handles quoted headers)
            String headerLine = reader.readLine();
            if (headerLine != null) {
                String[] parsed = CsvTransactionParser.parseCsvLine(headerLine);
                List<String> headers = Arrays.stream(parsed).map(String::trim).toList();
                viewModel.setCsvHeaders(headers);

                // Count rows
                int rowCount = 0;
                while (reader.readLine() != null) {
                    rowCount++;
                }
                viewModel.setRowCount(rowCount);

                // Update combos in step 2
                updateColumnCombos(headers);
            }

            viewModel.setSelectedFile(file);
            LOG.info("Loaded CSV file: {} with {} rows", file.getName(), viewModel.getRowCount());

        } catch (IOException e) {
            LOG.error("Failed to read CSV file", e);
            showError("Failed to read file", e);
        }
    }

    private void updateColumnCombos(List<String> headers) {
        if (dateColumnCombo != null) {
            dateColumnCombo.setItems(FXCollections.observableArrayList(headers));
        }
        if (descriptionColumnCombo != null) {
            descriptionColumnCombo.setItems(FXCollections.observableArrayList(headers));
        }
        if (amountColumnCombo != null) {
            amountColumnCombo.setItems(FXCollections.observableArrayList(headers));
        }
        if (incomeColumnCombo != null) {
            incomeColumnCombo.setItems(FXCollections.observableArrayList(headers));
        }
        if (expenseColumnCombo != null) {
            expenseColumnCombo.setItems(FXCollections.observableArrayList(headers));
        }
        if (categoryColumnCombo != null) {
            List<String> optionalHeaders = new java.util.ArrayList<>(headers);
            optionalHeaders.add(0, "None - Auto-categorize");
            categoryColumnCombo.setItems(FXCollections.observableArrayList(optionalHeaders));
        }

        // Apply auto-detected mapping if known format
        if (viewModel.getDetectedBankFormat() != BankFormat.UNKNOWN) {
            applyAutoMapping();
        }

        updateCsvHeaderChips(headers);
    }

    private void applyAutoMapping() {
        ColumnMapping mapping = viewModel.getColumnMapping();

        if (dateColumnCombo != null && mapping.getDateColumn() != null) {
            dateColumnCombo.setValue(mapping.getDateColumn());
        }
        if (descriptionColumnCombo != null && mapping.getDescriptionColumn() != null) {
            descriptionColumnCombo.setValue(mapping.getDescriptionColumn());
        }
        if (dateFormatCombo != null && mapping.getDateFormat() != null) {
            dateFormatCombo.setValue(mapping.getDateFormat());
        }
        if (separateColumnsCheckbox != null) {
            separateColumnsCheckbox.setSelected(mapping.hasSeparateAmountColumns());
        }
        if (mapping.hasSeparateAmountColumns()) {
            if (incomeColumnCombo != null) {
                incomeColumnCombo.setValue(mapping.getIncomeColumn());
            }
            if (expenseColumnCombo != null) {
                expenseColumnCombo.setValue(mapping.getExpenseColumn());
            }
        } else {
            if (amountColumnCombo != null && mapping.getAmountColumn() != null) {
                amountColumnCombo.setValue(mapping.getAmountColumn());
            }
        }
    }

    private void updateCsvHeaderChips(List<String> headers) {
        if (csvHeadersPreview == null) return;

        csvHeadersPreview.getChildren().clear();
        for (String header : headers) {
            Label chip = new Label(header);
            chip.getStyleClass().add("header-chip");
            csvHeadersPreview.getChildren().add(chip);
        }
    }

    // =====================================================
    // STEP 3 ACTIONS
    // =====================================================

    @FXML
    void handleReviewDuplicates(ActionEvent event) {
        viewModel.setTransactionFilter(TransactionFilter.DUPLICATES);
        if (filterCombo != null) {
            filterCombo.setValue(TransactionFilter.DUPLICATES);
        }
    }

    @FXML
    void handleBulkCategory(ActionEvent event) {
        // Show category selection dialog
        ChoiceDialog<ExpenseCategory> dialog = new ChoiceDialog<>(
                ExpenseCategory.OTHER_EXPENSES,
                ExpenseCategory.values()
        );
        dialog.setTitle("Set Category");
        dialog.setHeaderText("Assign category to " + viewModel.getSelectedCount() + " selected transactions");
        dialog.setContentText("Category:");

        dialog.showAndWait().ifPresent(category -> {
            viewModel.applyBulkCategory(category);
            refreshTransactionsTable();
            refreshSummaryStats();
        });
    }

    @FXML
    void handleAcceptSuggestion(ActionEvent event) {
        ImportedTransactionRow selected = transactionsTable.getSelectionModel().getSelectedItem();
        if (selected != null && selected.category() != null) {
            // Already has category from suggestion - nothing to do
            LOG.info("Accepted category suggestion for transaction: {}", selected.id());
        }
    }

    @FXML
    void handleRejectSuggestion(ActionEvent event) {
        // Open category picker
        handleBulkCategory(event);
    }

    private void refreshTransactionsTable() {
        if (transactionsTable == null) return;

        List<ImportedTransactionRow> filtered = viewModel.getFilteredTransactions();
        transactionsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void refreshSummaryStats() {
        if (incomeCountLabel != null) {
            incomeCountLabel.setText(String.valueOf(viewModel.getIncomeCount()));
        }
        if (incomeTotalLabel != null) {
            incomeTotalLabel.setText(viewModel.getFormattedIncomeTotal());
        }
        if (expenseCountLabel != null) {
            expenseCountLabel.setText(String.valueOf(viewModel.getExpenseCount()));
        }
        if (expenseTotalLabel != null) {
            expenseTotalLabel.setText(viewModel.getFormattedExpenseTotal());
        }
        if (duplicateCountLabel != null) {
            duplicateCountLabel.setText(String.valueOf(viewModel.getDuplicateCount()));
        }
        if (uncategorizedCountLabel != null) {
            uncategorizedCountLabel.setText(String.valueOf(viewModel.getUncategorizedCount()));
        }

        // Duplicate warning
        boolean hasDuplicates = viewModel.getDuplicateCount() > 0;
        if (duplicateWarning != null) {
            duplicateWarning.setVisible(hasDuplicates);
            duplicateWarning.setManaged(hasDuplicates);
        }
        if (duplicateWarningTitle != null && hasDuplicates) {
            duplicateWarningTitle.setText(viewModel.getDuplicateCount() + " potential duplicates found");
        }
    }

    private void updateSuggestionPanel(ImportedTransactionRow row) {
        if (suggestionPanel == null) return;

        boolean show = row != null && row.category() != null && row.confidence() > 0;
        suggestionPanel.setVisible(show);
        suggestionPanel.setManaged(show);

        if (show) {
            suggestionText.setText("Suggested: " + row.category().getDisplayName() +
                    " (" + row.confidence() + "% confidence)");
            suggestionReason.setText("Based on description");
        }
    }

    // =====================================================
    // NAVIGATION HANDLERS
    // =====================================================

    @FXML
    void handleBack(ActionEvent event) {
        viewModel.goToPreviousStep();
        updateNavigationButtons();
    }

    @FXML
    void handleNext(ActionEvent event) {
        int currentStep = viewModel.getCurrentStep();

        if (currentStep == 4) {
            // Import action
            handleImport(event);
        } else {
            viewModel.goToNextStep();

            // Special handling when entering step 3 - parse and populate transactions
            if (viewModel.getCurrentStep() == 3) {
                parseTransactions();
                refreshTransactionsTable();
                refreshSummaryStats();
            }

            // Special handling when entering step 4 - update confirm summary
            if (viewModel.getCurrentStep() == 4) {
                refreshConfirmSummary();
            }

            updateNavigationButtons();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    @FXML
    void handleImport(ActionEvent event) {
        viewModel.setImporting(true);
        performImport();
    }

    private void performImport() {
        List<ImportedTransactionRow> toImport = viewModel.getTransactionsToImport();
        if (toImport.isEmpty()) {
            viewModel.setImporting(false);
            showError("Nothing to import", new IllegalStateException("No transactions to import"));
            return;
        }

        UUID businessId = CoreServiceFactory.getDefaultBusinessId();
        IncomeService incomeService = CoreServiceFactory.getIncomeService();
        ExpenseService expenseService = CoreServiceFactory.getExpenseService();

        // Run import on a background thread to keep the UI responsive
        Thread importThread = new Thread(() -> {
            try {
                int imported = 0;
                int errors = 0;

                for (int i = 0; i < toImport.size(); i++) {
                    ImportedTransactionRow row = toImport.get(i);
                    try {
                        // Truncate description to service limit (100 chars) to avoid validation errors
                        String description = row.description();
                        if (description.length() > MAX_IMPORT_DESCRIPTION_LENGTH) {
                            description = description.substring(0, MAX_IMPORT_DESCRIPTION_LENGTH);
                        }

                        if (row.type() == TransactionType.INCOME) {
                            IncomeCategory incomeCategory = row.incomeCategory() != null
                                ? row.incomeCategory()
                                : IncomeCategory.SALES;
                            incomeService.create(
                                businessId,
                                row.date(),
                                row.amount(),
                                description,
                                incomeCategory,
                                null
                            );
                        } else {
                            ExpenseCategory category = row.category() != null
                                ? row.category()
                                : ExpenseCategory.OTHER_EXPENSES;
                            expenseService.create(
                                businessId,
                                row.date(),
                                row.amount(),
                                description,
                                category,
                                null,
                                null
                            );
                        }
                        imported++;
                    } catch (Exception e) {
                        errors++;
                        LOG.error("Failed to import transaction: {}", row.description(), e);
                    }

                    // Update progress on the FX thread
                    final double progress = (i + 1.0) / toImport.size();
                    javafx.application.Platform.runLater(() -> viewModel.setImportProgress(progress));
                }

                final int finalImported = imported;
                final int finalErrors = errors;

                javafx.application.Platform.runLater(() -> {
                    viewModel.setImporting(false);

                    if (finalErrors > 0) {
                        showSuccessToast(String.format(
                            "Imported %d transaction(s). %d failed.", finalImported, finalErrors));
                    } else {
                        showSuccessToast(String.format(
                            "Successfully imported %d transaction(s).", finalImported));
                    }

                    if (onImportCallback != null) {
                        onImportCallback.accept(toImport);
                    }

                    if (dialogStage != null) {
                        PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                        delay.setOnFinished(evt -> dialogStage.close());
                        delay.play();
                    }
                });
            } catch (Exception e) {
                LOG.error("Import failed unexpectedly", e);
                javafx.application.Platform.runLater(() -> {
                    viewModel.setImporting(false);
                    showError("Import failed", e);
                });
            }
        }, "bank-import-thread");

        importThread.setDaemon(true);
        importThread.start();
    }

    private void parseTransactions() {
        File file = viewModel.getSelectedFile();
        if (file == null) {
            LOG.warn("No file selected for parsing");
            return;
        }

        LOG.info("Parsing CSV file: {} with mapping: date={}, desc={}, amount={}",
                file.getName(),
                viewModel.getColumnMapping().getDateColumn(),
                viewModel.getColumnMapping().getDescriptionColumn(),
                viewModel.getColumnMapping().getAmountColumn());

        CsvTransactionParser csvParser = new CsvTransactionParser();
        CsvTransactionParser.ParseResult result = csvParser.parse(
            file.toPath(), viewModel.getColumnMapping());

        viewModel.setTransactions(result.transactions());

        if (!result.warnings().isEmpty()) {
            LOG.warn("CSV parse warnings: {}", result.warnings());
            String warningMessage = result.warnings().size() + " row(s) could not be parsed.";
            showParseWarning(warningMessage, result.warnings());
        }

        LOG.info("Parsed {} transactions ({} warnings)",
                result.transactions().size(), result.warnings().size());
    }

    private void showParseWarning(String summary, List<String> details) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Parse Warnings");
        alert.setHeaderText(summary);
        alert.setContentText(String.join("\n", details.subList(0, Math.min(details.size(), 10))));
        if (details.size() > 10) {
            alert.setContentText(alert.getContentText() + "\n... and " + (details.size() - 10) + " more");
        }
        alert.showAndWait();
    }

    private void refreshConfirmSummary() {
        if (confirmIncomeCount != null) {
            confirmIncomeCount.setText(String.valueOf(viewModel.getConfirmIncomeCount()));
        }
        if (confirmIncomeTotal != null) {
            confirmIncomeTotal.setText("£" + viewModel.getConfirmIncomeTotal().toPlainString());
        }
        if (confirmExpenseCount != null) {
            confirmExpenseCount.setText(String.valueOf(viewModel.getConfirmExpenseCount()));
        }
        if (confirmExpenseTotal != null) {
            confirmExpenseTotal.setText("£" + viewModel.getConfirmExpenseTotal().toPlainString());
        }
        if (skippedLabel != null) {
            int skipped = viewModel.getSkippedCount();
            skippedLabel.setText(skipped + " duplicate transaction" +
                    (skipped == 1 ? "" : "s") + " will be skipped");
        }
        if (sourceFileLabel != null) {
            sourceFileLabel.setText(viewModel.getFileName());
        }
        if (importBtn != null) {
            importBtn.setText(viewModel.getImportButtonText());
        }

        refreshCategoryBreakdown();
    }

    private void refreshCategoryBreakdown() {
        if (categoryBreakdown == null) return;

        categoryBreakdown.getChildren().clear();

        var breakdown = viewModel.getCategoryBreakdown();
        for (var entry : breakdown.entrySet()) {
            HBox row = new HBox(8);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Category dot
            Label dot = new Label();
            dot.getStyleClass().addAll("category-dot",
                    "category-" + entry.getKey().name().toLowerCase().replace("_", "-"));

            // Category name
            Label name = new Label(entry.getKey().getDisplayName());
            name.getStyleClass().add("category-name");

            // Spacer
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            // Count
            Label count = new Label(entry.getValue().count() + " items");
            count.getStyleClass().add("category-count");

            // Amount
            Label amount = new Label(entry.getValue().getFormattedTotal());
            amount.getStyleClass().add("category-amount");

            row.getChildren().addAll(dot, name, spacer, count, amount);
            categoryBreakdown.getChildren().add(row);
        }
    }

    // =====================================================
    // UTILITY METHODS
    // =====================================================

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private void showSuccessToast(String message) {
        LOG.info("Import success: {}", message);

        Alert toast = new Alert(Alert.AlertType.INFORMATION);
        toast.setTitle(null);
        toast.setHeaderText(null);
        toast.setContentText(message);
        toast.show();

        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(event -> toast.close());
        delay.play();
    }

    private void showError(String message, Exception e) {
        LOG.error(message, e);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(message);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}
