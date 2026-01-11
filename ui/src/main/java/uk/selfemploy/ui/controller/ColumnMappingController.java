package uk.selfemploy.ui.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.ui.viewmodel.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller for the Column Mapping Wizard.
 * Handles the 3-step wizard flow for mapping CSV columns and confirming amount interpretation.
 *
 * SE-802: Bank Import Column Mapping Wizard
 */
public class ColumnMappingController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(ColumnMappingController.class);

    private static final List<String> DATE_FORMATS = List.of(
            "dd/MM/yyyy",
            "MM/dd/yyyy",
            "yyyy-MM-dd",
            "d MMM yyyy",
            "dd-MM-yyyy"
    );

    // === Wizard Header ===
    @FXML private Label stepTitle;
    @FXML private Label stepSubtitle;
    @FXML private VBox step1Indicator;
    @FXML private VBox step2Indicator;
    @FXML private VBox step3Indicator;

    // === Step Content ===
    @FXML private VBox step1Content;
    @FXML private VBox step2Content;
    @FXML private VBox step3Content;

    // === Step 1: Column Selection ===
    @FXML private TableView<PreviewRow> previewTable;
    @FXML private ComboBox<String> dateColumnCombo;
    @FXML private ComboBox<String> dateFormatCombo;
    @FXML private ComboBox<String> descriptionColumnCombo;
    @FXML private ComboBox<String> amountColumnCombo;
    @FXML private ComboBox<String> categoryColumnCombo;

    // === Step 2: Amount Interpretation ===
    @FXML private RadioButton standardRadio;
    @FXML private RadioButton invertedRadio;
    @FXML private RadioButton separateRadio;
    @FXML private ToggleGroup interpretationToggle;
    @FXML private Label positiveExampleStandard;
    @FXML private Label negativeExampleStandard;
    @FXML private HBox separateColumnsInputs;
    @FXML private ComboBox<String> incomeColumnCombo;
    @FXML private ComboBox<String> expenseColumnCombo;

    // === Step 3: Summary & Confirmation ===
    @FXML private Label incomeTransactionCount;
    @FXML private Label incomeTotalAmount;
    @FXML private Label expenseTransactionCount;
    @FXML private Label expenseTotalAmount;
    @FXML private CheckBox saveMappingCheckbox;
    @FXML private Button confirmMappingBtn;

    // === Footer Navigation ===
    @FXML private Button cancelBtn;
    @FXML private Button backBtn;
    @FXML private Button nextBtn;

    // === ViewModel and Callbacks ===
    private ColumnMappingViewModel viewModel;
    private Consumer<ColumnMapping> onConfirmCallback;
    private Runnable onCancelCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        viewModel = new ColumnMappingViewModel();

        setupStepBindings();
        setupStep1Bindings();
        setupStep2Bindings();
        setupStep3Bindings();
        setupNavigationBindings();

        LOG.info("ColumnMappingController initialized");
    }

    // =====================================================
    // PUBLIC API
    // =====================================================

    /**
     * Sets the CSV headers for column mapping.
     */
    public void setCsvHeaders(List<String> headers) {
        viewModel.setCsvHeaders(headers);
        populateColumnCombos(headers);
        viewModel.autoDetectColumns();
        applyAutoDetectedColumns();
    }

    /**
     * Sets the preview rows from the CSV file.
     */
    public void setPreviewRows(List<PreviewRow> rows) {
        viewModel.setPreviewRows(rows);
        populatePreviewTable();
    }

    /**
     * Sets the callback to be invoked when mapping is confirmed.
     */
    public void setOnConfirmCallback(Consumer<ColumnMapping> callback) {
        this.onConfirmCallback = callback;
    }

    /**
     * Sets the callback to be invoked when wizard is cancelled.
     */
    public void setOnCancelCallback(Runnable callback) {
        this.onCancelCallback = callback;
    }

    /**
     * Returns the ViewModel for testing.
     */
    public ColumnMappingViewModel getViewModel() {
        return viewModel;
    }

    // =====================================================
    // SETUP BINDINGS
    // =====================================================

    private void setupStepBindings() {
        viewModel.currentStepProperty().addListener((obs, oldVal, newVal) -> {
            updateStepIndicators(newVal.intValue());
            updateStepContent(newVal.intValue());
            updateStepHeader(newVal.intValue());
            updateNavigationButtons();
        });

        // Initial state
        updateStepIndicators(1);
        updateStepContent(1);
        updateStepHeader(1);
    }

    private void updateStepIndicators(int currentStep) {
        VBox[] indicators = {step1Indicator, step2Indicator, step3Indicator};

        for (int i = 0; i < indicators.length; i++) {
            if (indicators[i] == null) continue;

            int stepNum = i + 1;
            indicators[i].getStyleClass().removeAll("step-active", "step-completed");

            if (stepNum < currentStep) {
                indicators[i].getStyleClass().add("step-completed");
            } else if (stepNum == currentStep) {
                indicators[i].getStyleClass().add("step-active");
            }
        }
    }

    private void updateStepContent(int currentStep) {
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
    }

    private void updateStepHeader(int currentStep) {
        if (stepTitle != null) {
            stepTitle.setText(String.format("Step %d: %s", currentStep, viewModel.getCurrentStepName()));
        }
        if (stepSubtitle != null) {
            stepSubtitle.setText(getStepSubtitle(currentStep));
        }
    }

    private String getStepSubtitle(int step) {
        return switch (step) {
            case 1 -> "Map your CSV columns to transaction fields";
            case 2 -> "Choose how to interpret positive and negative values";
            case 3 -> "Review the mapping summary and confirm";
            default -> "";
        };
    }

    private void setupStep1Bindings() {
        if (dateFormatCombo != null) {
            dateFormatCombo.setItems(FXCollections.observableArrayList(DATE_FORMATS));
        }
    }

    private void setupStep2Bindings() {
        // Radio button user data for identification
        if (standardRadio != null) standardRadio.setUserData(AmountInterpretation.STANDARD);
        if (invertedRadio != null) invertedRadio.setUserData(AmountInterpretation.INVERTED);
        if (separateRadio != null) separateRadio.setUserData(AmountInterpretation.SEPARATE_COLUMNS);
    }

    private void setupStep3Bindings() {
        // Summary updates when entering step 3
        viewModel.currentStepProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.intValue() == 3) {
                updateSummary();
            }
        });
    }

    private void setupNavigationBindings() {
        updateNavigationButtons();
    }

    private void updateNavigationButtons() {
        int currentStep = viewModel.getCurrentStep();

        if (backBtn != null) {
            backBtn.setVisible(currentStep > 1);
            backBtn.setManaged(currentStep > 1);
        }

        if (nextBtn != null) {
            nextBtn.setDisable(!viewModel.canGoNext());

            String buttonText = switch (currentStep) {
                case 1 -> "Next: Amount Interpretation";
                case 2 -> "Next: Review Summary";
                case 3 -> "Confirm Mapping";
                default -> "Next";
            };
            nextBtn.setText(buttonText);

            // On step 3, hide next button (use Confirm Mapping instead)
            nextBtn.setVisible(currentStep < 3);
            nextBtn.setManaged(currentStep < 3);
        }
    }

    // =====================================================
    // STEP 1: COLUMN SELECTION HANDLERS
    // =====================================================

    @FXML
    void handleDateColumnChange(ActionEvent event) {
        if (dateColumnCombo != null && dateColumnCombo.getValue() != null) {
            viewModel.setSelectedDateColumn(dateColumnCombo.getValue());
            updateNavigationButtons();
        }
    }

    @FXML
    void handleDateFormatChange(ActionEvent event) {
        if (dateFormatCombo != null && dateFormatCombo.getValue() != null) {
            viewModel.setSelectedDateFormat(dateFormatCombo.getValue());
            updateNavigationButtons();
        }
    }

    @FXML
    void handleDescriptionColumnChange(ActionEvent event) {
        if (descriptionColumnCombo != null && descriptionColumnCombo.getValue() != null) {
            viewModel.setSelectedDescriptionColumn(descriptionColumnCombo.getValue());
            updateNavigationButtons();
        }
    }

    @FXML
    void handleAmountColumnChange(ActionEvent event) {
        if (amountColumnCombo != null && amountColumnCombo.getValue() != null) {
            viewModel.setSelectedAmountColumn(amountColumnCombo.getValue());
            updateNavigationButtons();
        }
    }

    @FXML
    void handleCategoryColumnChange(ActionEvent event) {
        if (categoryColumnCombo != null) {
            viewModel.setSelectedCategoryColumn(categoryColumnCombo.getValue());
        }
    }

    private void populateColumnCombos(List<String> headers) {
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

        // Category combo includes "None" option
        if (categoryColumnCombo != null) {
            List<String> categoryOptions = new java.util.ArrayList<>();
            categoryOptions.add("None - Auto-categorize");
            categoryOptions.addAll(headers);
            categoryColumnCombo.setItems(FXCollections.observableArrayList(categoryOptions));
        }
    }

    private void applyAutoDetectedColumns() {
        if (viewModel.getSelectedDateColumn() != null && dateColumnCombo != null) {
            dateColumnCombo.setValue(viewModel.getSelectedDateColumn());
        }
        if (viewModel.getSelectedDescriptionColumn() != null && descriptionColumnCombo != null) {
            descriptionColumnCombo.setValue(viewModel.getSelectedDescriptionColumn());
        }
        if (viewModel.getSelectedAmountColumn() != null && amountColumnCombo != null) {
            amountColumnCombo.setValue(viewModel.getSelectedAmountColumn());
        }

        // Default date format
        if (dateFormatCombo != null && viewModel.getSelectedDateFormat() == null) {
            dateFormatCombo.setValue("dd/MM/yyyy");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");
        }

        updateNavigationButtons();
    }

    private void populatePreviewTable() {
        if (previewTable == null) return;

        previewTable.getColumns().clear();
        previewTable.getItems().clear();

        List<String> headers = viewModel.getCsvHeaders();
        List<PreviewRow> rows = viewModel.getPreviewRows();

        // Create columns
        for (int i = 0; i < headers.size(); i++) {
            final int colIndex = i;
            TableColumn<PreviewRow, String> column = new TableColumn<>(headers.get(i));
            column.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getValue(colIndex)));
            column.setPrefWidth(120);
            previewTable.getColumns().add(column);
        }

        // Add rows
        previewTable.setItems(FXCollections.observableArrayList(rows));
    }

    // =====================================================
    // STEP 2: AMOUNT INTERPRETATION HANDLERS
    // =====================================================

    @FXML
    void handleInterpretationChange(ActionEvent event) {
        Toggle selected = interpretationToggle.getSelectedToggle();
        if (selected != null && selected.getUserData() instanceof AmountInterpretation interpretation) {
            viewModel.setAmountInterpretation(interpretation);

            // Show/hide separate columns inputs
            boolean showSeparate = interpretation == AmountInterpretation.SEPARATE_COLUMNS;
            if (separateColumnsInputs != null) {
                separateColumnsInputs.setVisible(showSeparate);
                separateColumnsInputs.setManaged(showSeparate);
            }

            updateAmountExamples();
            updateNavigationButtons();
        }
    }

    @FXML
    void handleIncomeColumnChange(ActionEvent event) {
        if (incomeColumnCombo != null && incomeColumnCombo.getValue() != null) {
            viewModel.setSelectedIncomeColumn(incomeColumnCombo.getValue());
            updateNavigationButtons();
        }
    }

    @FXML
    void handleExpenseColumnChange(ActionEvent event) {
        if (expenseColumnCombo != null && expenseColumnCombo.getValue() != null) {
            viewModel.setSelectedExpenseColumn(expenseColumnCombo.getValue());
            updateNavigationButtons();
        }
    }

    private void updateAmountExamples() {
        if (positiveExampleStandard != null) {
            positiveExampleStandard.setText(viewModel.getFormattedPositiveExample());
        }
        if (negativeExampleStandard != null) {
            negativeExampleStandard.setText(viewModel.getFormattedNegativeExample());
        }
    }

    // =====================================================
    // STEP 3: SUMMARY & CONFIRMATION HANDLERS
    // =====================================================

    @FXML
    void handleSavePreferenceChange(ActionEvent event) {
        if (saveMappingCheckbox != null) {
            viewModel.setSavePreferenceSelected(saveMappingCheckbox.isSelected());
        }
    }

    @FXML
    void handleConfirmMapping(ActionEvent event) {
        viewModel.confirmMapping();

        ColumnMapping mapping = viewModel.buildColumnMapping();
        LOG.info("Mapping confirmed: date={}, desc={}, amount={}, interpretation={}",
                mapping.getDateColumn(), mapping.getDescriptionColumn(),
                mapping.getAmountColumn(), mapping.getAmountInterpretation());

        if (onConfirmCallback != null) {
            onConfirmCallback.accept(mapping);
        }
    }

    private void updateSummary() {
        if (incomeTransactionCount != null) {
            incomeTransactionCount.setText(viewModel.getIncomeSummaryText());
        }
        if (incomeTotalAmount != null) {
            incomeTotalAmount.setText(viewModel.getFormattedIncomeTotal());
        }
        if (expenseTransactionCount != null) {
            expenseTransactionCount.setText(viewModel.getExpenseSummaryText());
        }
        if (expenseTotalAmount != null) {
            expenseTotalAmount.setText(viewModel.getFormattedExpenseTotal());
        }
    }

    // =====================================================
    // NAVIGATION HANDLERS
    // =====================================================

    @FXML
    void handleBack(ActionEvent event) {
        viewModel.goToPreviousStep();
    }

    @FXML
    void handleNext(ActionEvent event) {
        int currentStep = viewModel.getCurrentStep();

        if (currentStep == 2) {
            // Before going to step 3, update the amount examples
            updateAmountExamples();
        }

        viewModel.goToNextStep();
    }

    @FXML
    void handleCancel(ActionEvent event) {
        LOG.info("Column mapping wizard cancelled");

        if (onCancelCallback != null) {
            onCancelCallback.run();
        }
    }
}
