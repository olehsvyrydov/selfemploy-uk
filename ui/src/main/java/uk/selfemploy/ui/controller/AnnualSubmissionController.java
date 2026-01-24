package uk.selfemploy.ui.controller;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import uk.selfemploy.common.domain.AnnualSubmissionState;
import uk.selfemploy.common.domain.TaxCalculationResult;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.legal.Disclaimers;
import uk.selfemploy.ui.viewmodel.AnnualSubmissionViewModel;
import uk.selfemploy.ui.viewmodel.SubmissionDeclarationViewModel;

import javafx.stage.Stage;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Controller for the Annual Self Assessment Submission view.
 *
 * <p>Manages the multi-step submission process:
 * <ol>
 *   <li>Review Summary - Display financial data</li>
 *   <li>Calculate Tax - Trigger calculation and show loading</li>
 *   <li>Review Calculation - Display tax breakdown</li>
 *   <li>Submit Declaration - Final submission to HMRC</li>
 * </ol>
 *
 * <p>This controller binds UI elements to the AnnualSubmissionViewModel
 * and handles user actions.
 */
public class AnnualSubmissionController {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);

    // === FXML Injected Elements ===

    @FXML private ScrollPane rootScroll;
    @FXML private Label taxYearLabel;

    // Progress Indicator
    @FXML private HBox progressIndicator;
    @FXML private VBox step1Container;
    @FXML private VBox step2Container;
    @FXML private VBox step3Container;
    @FXML private VBox step4Container;
    @FXML private Label step1Number;
    @FXML private Label step2Number;
    @FXML private Label step3Number;
    @FXML private Label step4Number;

    // Summary Panel
    @FXML private VBox summaryPanel;
    @FXML private Label turnoverValue;
    @FXML private Label expensesValue;
    @FXML private Label netProfitValue;

    // Calculation Panel
    @FXML private VBox calculationPanel;
    @FXML private VBox loadingIndicator;
    @FXML private VBox calculationResults;
    @FXML private Label incomeTaxValue;
    @FXML private Label niValue;
    @FXML private Label totalTaxValue;

    // Error Panel
    @FXML private VBox errorPanel;
    @FXML private Label errorMessage;
    @FXML private Button retryButton;

    // Success Panel
    @FXML private VBox successPanel;
    @FXML private Label submissionReference;

    // Submission Disclaimer Banner (SE-509)
    @FXML private HBox submissionDisclaimerBanner;
    @FXML private Label submissionDisclaimerText;

    // 6-Checkbox Declaration Card (SE-512)
    @FXML private VBox declarationCard;
    @FXML private VBox declarationRows;
    @FXML private HBox decl1Row;
    @FXML private HBox decl2Row;
    @FXML private HBox decl3Row;
    @FXML private HBox decl4Row;
    @FXML private HBox decl5Row;
    @FXML private HBox decl6Row;
    @FXML private CheckBox decl1Checkbox;
    @FXML private CheckBox decl2Checkbox;
    @FXML private CheckBox decl3Checkbox;
    @FXML private CheckBox decl4Checkbox;
    @FXML private CheckBox decl5Checkbox;
    @FXML private CheckBox decl6Checkbox;
    @FXML private HBox progressSection;
    @FXML private Label progressLabel;
    @FXML private VBox timestampSection;
    @FXML private Label timestampLabel;
    @FXML private Label declarationIdLabel;

    // Action Bar
    @FXML private HBox actionBar;
    @FXML private Button cancelButton;
    @FXML private Button calculateButton;
    @FXML private Button reviewButton;
    @FXML private VBox submitContainer;
    @FXML private Button submitButton;
    @FXML private Label submitHelperText;

    // Date/Time formatters for timestamp display (SE-506)
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy");

    // Dynamic width per step (user request)
    private static final int STEP_1_WIDTH = 700;
    private static final int STEP_2_WIDTH = 840;
    private static final int STEP_3_WIDTH = 1220;

    // Dialog stage reference for dynamic resizing
    private Stage dialogStage;

    // === ViewModels ===

    private AnnualSubmissionViewModel viewModel;
    private SubmissionDeclarationViewModel declarationViewModel;

    /**
     * Initializes the controller after FXML injection.
     * Sets up data bindings and listeners.
     */
    @FXML
    public void initialize() {
        viewModel = new AnnualSubmissionViewModel();
        setupBindings();
        setupListeners();
        initializeDisclaimers();

        // Initialize Step 1 as active and show Calculate button
        javafx.application.Platform.runLater(() -> {
            updateStepIndicator(1);
            updateActionButtons(1);
        });
    }

    /**
     * Initializes the 6-checkbox declaration ViewModel for a specific tax year (SE-512).
     *
     * @param taxYear the tax year label (e.g., "2025-26")
     */
    private void initializeDeclarationViewModel(String taxYear) {
        declarationViewModel = new SubmissionDeclarationViewModel(Clock.systemUTC(), taxYear);
        setupDeclarationBindings();
    }

    /**
     * Sets up bindings between the 6 checkboxes and the SubmissionDeclarationViewModel.
     */
    private void setupDeclarationBindings() {
        if (declarationViewModel == null) {
            return;
        }

        // Bind checkboxes to ViewModel properties (bidirectional)
        decl1Checkbox.selectedProperty().bindBidirectional(declarationViewModel.accuracyStatementProperty());
        decl2Checkbox.selectedProperty().bindBidirectional(declarationViewModel.penaltiesWarningProperty());
        decl3Checkbox.selectedProperty().bindBidirectional(declarationViewModel.recordKeepingProperty());
        decl4Checkbox.selectedProperty().bindBidirectional(declarationViewModel.calculationVerificationProperty());
        decl5Checkbox.selectedProperty().bindBidirectional(declarationViewModel.legalEffectProperty());
        decl6Checkbox.selectedProperty().bindBidirectional(declarationViewModel.identityConfirmationProperty());

        // Bind progress label
        progressLabel.textProperty().bind(declarationViewModel.progressTextProperty());

        // Bind timestamp section visibility
        timestampSection.visibleProperty().bind(declarationViewModel.showTimestampSectionProperty());
        timestampSection.managedProperty().bind(declarationViewModel.showTimestampSectionProperty());

        // Bind submit button to completion state
        submitButton.disableProperty().bind(declarationViewModel.isCompleteProperty().not());

        // Update submit helper text based on completion
        declarationViewModel.isCompleteProperty().addListener((obs, wasComplete, isComplete) -> {
            if (isComplete) {
                submitHelperText.setText("Ready to submit");
                submitHelperText.getStyleClass().add("ready");
                submitButton.getStyleClass().add("ready");

                // Update timestamp display
                timestampLabel.setText("Timestamp: " + declarationViewModel.getTimestampDisplay());
                declarationIdLabel.setText("Declaration ID: " + declarationViewModel.getDeclarationIdDisplay());
            } else {
                submitHelperText.setText("Please confirm all declarations above");
                submitHelperText.getStyleClass().remove("ready");
                submitButton.getStyleClass().remove("ready");
            }
        });

        // Add checked style to rows when checkbox is selected
        setupRowStyleBindings();
    }

    /**
     * Sets up style bindings for declaration rows (checked state).
     */
    private void setupRowStyleBindings() {
        bindRowCheckedStyle(decl1Row, decl1Checkbox);
        bindRowCheckedStyle(decl2Row, decl2Checkbox);
        bindRowCheckedStyle(decl3Row, decl3Checkbox);
        bindRowCheckedStyle(decl4Row, decl4Checkbox);
        bindRowCheckedStyle(decl5Row, decl5Checkbox);
        bindRowCheckedStyle(decl6Row, decl6Checkbox);
    }

    /**
     * Binds the "checked" style class to a row based on checkbox selection.
     */
    private void bindRowCheckedStyle(HBox row, CheckBox checkbox) {
        checkbox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                if (!row.getStyleClass().contains("checked")) {
                    row.getStyleClass().add("checked");
                }
            } else {
                row.getStyleClass().remove("checked");
            }
        });
    }

    /**
     * Initializes the submission disclaimer text from centralized legal constants (SE-509).
     * AC-4: Disclaimer cannot be dismissed permanently.
     */
    private void initializeDisclaimers() {
        if (submissionDisclaimerText != null) {
            submissionDisclaimerText.setText(Disclaimers.HMRC_SUBMISSION_DISCLAIMER);
        }
    }

    /**
     * Initializes the view with a specific tax year and financial data.
     *
     * @param taxYear The tax year for submission
     * @param totalIncome Total income for the year
     * @param totalExpenses Total expenses for the year
     * @param netProfit Net profit for the year
     */
    public void initializeSubmission(TaxYear taxYear, BigDecimal totalIncome,
                                     BigDecimal totalExpenses, BigDecimal netProfit) {
        viewModel.setTotalIncome(totalIncome);
        viewModel.setTotalExpenses(totalExpenses);
        viewModel.setNetProfit(netProfit);
        viewModel.startSubmission(taxYear);

        // Initialize the 6-checkbox declaration ViewModel (SE-512)
        initializeDeclarationViewModel(taxYear.label());
    }

    /**
     * Gets the ViewModel for testing purposes.
     */
    public AnnualSubmissionViewModel getViewModel() {
        return viewModel;
    }

    /**
     * Gets the declaration ViewModel for testing purposes (SE-512).
     */
    public SubmissionDeclarationViewModel getDeclarationViewModel() {
        return declarationViewModel;
    }

    /**
     * Sets the dialog stage for dynamic resizing.
     * Call this after loading the FXML to enable step-based width changes.
     *
     * @param stage the Stage containing this view
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
        System.out.println("[DEBUG] setDialogStage called, stage = " + stage);
    }

    /**
     * Updates the dialog width based on the current step.
     * Step 1: 700px, Step 2: 900px, Step 3+: 1200px
     */
    private void updateDialogWidth(int currentStep) {
        if (dialogStage == null) {
            System.out.println("[DEBUG] dialogStage is null, cannot resize");
            return;
        }

        int targetWidth = switch (currentStep) {
            case 1 -> STEP_1_WIDTH;
            case 2 -> STEP_2_WIDTH;
            default -> STEP_3_WIDTH;
        };

        double currentWidth = dialogStage.getWidth();
        System.out.println("[DEBUG] updateDialogWidth called for step " + currentStep +
            ", current=" + currentWidth + ", target=" + targetWidth);

        if (Math.abs(currentWidth - targetWidth) < 1) {
            System.out.println("[DEBUG] Already at target width, skipping");
            return;
        }

        // Calculate new X position to keep dialog centered
        double currentX = dialogStage.getX();
        double currentCenterX = currentX + currentWidth / 2;
        double newX = currentCenterX - targetWidth / 2.0;

        // Force stage to resize by setting min and max width temporarily
        double originalMinWidth = dialogStage.getMinWidth();
        double originalMaxWidth = dialogStage.getMaxWidth();

        dialogStage.setMinWidth(targetWidth);
        dialogStage.setMaxWidth(targetWidth);
        dialogStage.setWidth(targetWidth);
        dialogStage.setX(newX);

        // Restore flexible sizing after a brief delay
        javafx.application.Platform.runLater(() -> {
            dialogStage.setMinWidth(originalMinWidth);
            dialogStage.setMaxWidth(Double.MAX_VALUE);
        });

        System.out.println("[DEBUG] After resize: stage.getWidth() = " + dialogStage.getWidth());
    }

    // === Event Handlers ===

    @FXML
    private void handleCalculate() {
        System.out.println("[DEBUG] handleCalculate() called, current step = " + viewModel.getCurrentStep());
        viewModel.executeNextStep();
        // TODO: Call backend service to calculate tax
        // For now, simulate with mock data
        simulateCalculation();
    }

    @FXML
    private void handleReview() {
        viewModel.executeNextStep();
    }

    @FXML
    private void handleSubmit() {
        // Show confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Submission");
        confirmDialog.setHeaderText("Submit Annual Self Assessment");
        confirmDialog.setContentText(
            "Are you sure you want to submit your Annual Self Assessment to HMRC?\n\n" +
            "This action cannot be undone."
        );

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                viewModel.confirmAndSubmit();
                // TODO: Call backend service to submit
                simulateSubmission();
            }
        });
    }

    @FXML
    private void handleRetry() {
        viewModel.clearError();
        viewModel.executeNextStep();
    }

    @FXML
    private void handleCancel() {
        // Show confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Cancel Submission");
        confirmDialog.setHeaderText("Cancel Annual Submission");
        confirmDialog.setContentText(
            "Are you sure you want to cancel this submission?\n\n" +
            "All progress will be lost."
        );

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                viewModel.cancel();
                // TODO: Navigate back to previous screen
            }
        });
    }

    /**
     * Handles declaration checkbox state change (SE-512).
     * This method is called from FXML when any of the 6 checkboxes change.
     * The actual state updates are handled by the ViewModel bindings.
     */
    @FXML
    private void handleDeclarationChange() {
        // The checkbox bindings automatically update the ViewModel
        // This method exists for any additional UI updates needed on checkbox change

        // Note: Row style updates, progress updates, and timestamp section visibility
        // are all handled by the setupDeclarationBindings() method
    }

    // === Private Methods ===

    private void setupBindings() {
        // Bind financial summary values (with null-safe formatting)
        turnoverValue.textProperty().bind(
            Bindings.createStringBinding(
                () -> formatCurrency(viewModel.totalIncomeProperty().get()),
                viewModel.totalIncomeProperty()
            )
        );
        expensesValue.textProperty().bind(
            Bindings.createStringBinding(
                () -> formatCurrency(viewModel.totalExpensesProperty().get()),
                viewModel.totalExpensesProperty()
            )
        );
        netProfitValue.textProperty().bind(
            Bindings.createStringBinding(
                () -> formatCurrency(viewModel.netProfitProperty().get()),
                viewModel.netProfitProperty()
            )
        );

        // Bind tax calculation values (with null-safe formatting)
        incomeTaxValue.textProperty().bind(
            Bindings.createStringBinding(
                () -> formatCurrency(viewModel.taxDueProperty().get()),
                viewModel.taxDueProperty()
            )
        );
        niValue.textProperty().bind(
            Bindings.createStringBinding(
                () -> formatCurrency(viewModel.nationalInsuranceProperty().get()),
                viewModel.nationalInsuranceProperty()
            )
        );

        // Bind error message
        errorMessage.textProperty().bind(viewModel.errorMessageProperty());

        // Bind loading indicator visibility
        loadingIndicator.visibleProperty().bind(viewModel.isLoadingProperty());
        loadingIndicator.managedProperty().bind(viewModel.isLoadingProperty());

        // Bind error panel visibility
        errorPanel.visibleProperty().bind(viewModel.errorMessageProperty().isNotNull());
        errorPanel.managedProperty().bind(viewModel.errorMessageProperty().isNotNull());
    }

    private void setupListeners() {
        // Listen for tax year changes
        viewModel.taxYearProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                taxYearLabel.setText("Tax Year " + newVal.label());
            }
        });

        // Listen for current step changes
        viewModel.currentStepProperty().addListener((obs, oldVal, newVal) -> {
            int step = newVal.intValue();
            System.out.println("[DEBUG] Step changed: " + oldVal + " -> " + newVal + " (dialogStage=" + dialogStage + ")");
            updateStepIndicator(step);
            updateActionButtons(step);
            updateDialogWidth(step);
        });

        // Listen for state changes
        viewModel.currentStateProperty().addListener((obs, oldVal, newVal) -> {
            updateViewForState(newVal);
        });

        // Listen for calculation result
        viewModel.calculationResultProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Use totalTaxLiability from common module's TaxCalculationResult
                totalTaxValue.setText(formatCurrency(newVal.totalTaxLiability()));
            }
        });
    }

    private void updateStepIndicator(int currentStep) {
        // Reset all steps
        step1Container.getStyleClass().removeAll("step-active", "step-completed");
        step2Container.getStyleClass().removeAll("step-active", "step-completed");
        step3Container.getStyleClass().removeAll("step-active", "step-completed");
        step4Container.getStyleClass().removeAll("step-active", "step-completed");

        // Mark completed steps
        for (int i = 1; i < currentStep; i++) {
            getStepContainer(i).getStyleClass().add("step-completed");
        }

        // Mark current step as active
        if (currentStep > 0 && currentStep <= 4) {
            getStepContainer(currentStep).getStyleClass().add("step-active");
        }
    }

    private void updateActionButtons(int currentStep) {
        // Hide all buttons, disclaimer banner, and declaration card first
        calculateButton.setVisible(false);
        calculateButton.setManaged(false);
        reviewButton.setVisible(false);
        reviewButton.setManaged(false);
        submitContainer.setVisible(false);
        submitContainer.setManaged(false);
        declarationCard.setVisible(false);
        declarationCard.setManaged(false);
        // SE-509: Hide submission disclaimer when not in submission step
        if (submissionDisclaimerBanner != null) {
            submissionDisclaimerBanner.setVisible(false);
            submissionDisclaimerBanner.setManaged(false);
        }

        // Show appropriate button based on step
        switch (currentStep) {
            case 1 -> {
                calculateButton.setVisible(true);
                calculateButton.setManaged(true);
            }
            case 2 -> {
                reviewButton.setVisible(true);
                reviewButton.setManaged(true);
                reviewButton.setDisable(true); // Disable until calculation completes
            }
            case 3 -> {
                // SE-509: Show submission disclaimer banner (AC-2)
                // AC-4: Disclaimer cannot be dismissed
                if (submissionDisclaimerBanner != null) {
                    submissionDisclaimerBanner.setVisible(true);
                    submissionDisclaimerBanner.setManaged(true);
                }
                // Show declaration card and submit container (SE-512)
                declarationCard.setVisible(true);
                declarationCard.setManaged(true);
                submitContainer.setVisible(true);
                submitContainer.setManaged(true);
                // Reset all 6 declaration checkboxes (SE-512)
                if (declarationViewModel != null) {
                    declarationViewModel.reset();
                }
            }
        }
    }

    private void updateViewForState(AnnualSubmissionState state) {
        if (state == null) {
            return;
        }

        switch (state) {
            case INITIATED -> {
                calculationPanel.setVisible(false);
                calculationPanel.setManaged(false);
                successPanel.setVisible(false);
                successPanel.setManaged(false);
            }
            case CALCULATING -> {
                calculationPanel.setVisible(true);
                calculationPanel.setManaged(true);
                calculationResults.setVisible(false);
                calculationResults.setManaged(false);
            }
            case CALCULATED -> {
                calculationResults.setVisible(true);
                calculationResults.setManaged(true);
                reviewButton.setDisable(false);
            }
            case DECLARING -> {
                // Keep calculation visible, disable all buttons
                calculateButton.setDisable(true);
                reviewButton.setDisable(true);
                submitButton.setDisable(true);
                // Disable all 6 declaration checkboxes during submission (SE-512)
                if (declarationViewModel != null) {
                    declarationViewModel.disabledProperty().set(true);
                }
                decl1Checkbox.setDisable(true);
                decl2Checkbox.setDisable(true);
                decl3Checkbox.setDisable(true);
                decl4Checkbox.setDisable(true);
                decl5Checkbox.setDisable(true);
                decl6Checkbox.setDisable(true);
            }
            case COMPLETED -> {
                successPanel.setVisible(true);
                successPanel.setManaged(true);
                actionBar.setVisible(false);
                actionBar.setManaged(false);
                // Hide declaration card on completion (SE-506)
                declarationCard.setVisible(false);
                declarationCard.setManaged(false);
                // TODO: Set submission reference
                submissionReference.setText("SA-" + viewModel.getSagaId().toString().substring(0, 8).toUpperCase());
            }
            case FAILED -> {
                // Error panel is shown via binding
                calculateButton.setDisable(false);
                reviewButton.setDisable(false);
                submitButton.setDisable(false);
            }
        }
    }

    private VBox getStepContainer(int step) {
        return switch (step) {
            case 1 -> step1Container;
            case 2 -> step2Container;
            case 3 -> step3Container;
            case 4 -> step4Container;
            default -> step1Container;
        };
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return CURRENCY_FORMAT.format(amount);
    }

    // === Temporary Simulation Methods (will be replaced by backend service) ===

    private void simulateCalculation() {
        // Simulate async calculation
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simulate network delay

                // Create mock result using common module's TaxCalculationResult
                javafx.application.Platform.runLater(() -> {
                    TaxCalculationResult result = TaxCalculationResult.create(
                        "calc-sim-" + System.currentTimeMillis(),  // calculationId
                        viewModel.getTotalIncome(),                 // totalIncome
                        viewModel.getTotalExpenses(),               // totalExpenses
                        viewModel.getNetProfit(),                   // netProfit
                        new BigDecimal("5430.00"),                  // incomeTax
                        new BigDecimal("165.00"),                   // nationalInsuranceClass2
                        new BigDecimal("1980.00")                   // nationalInsuranceClass4
                    );

                    viewModel.setCalculationResult(result);
                    viewModel.setCurrentState(AnnualSubmissionState.CALCULATED);
                    viewModel.setLoading(false);
                });

            } catch (InterruptedException e) {
                javafx.application.Platform.runLater(() -> {
                    viewModel.setErrorMessage("Calculation failed: " + e.getMessage());
                    viewModel.setCurrentState(AnnualSubmissionState.FAILED);
                    viewModel.setLoading(false);
                });
            }
        }).start();
    }

    private void simulateSubmission() {
        // Simulate async submission
        new Thread(() -> {
            try {
                Thread.sleep(3000); // Simulate network delay

                javafx.application.Platform.runLater(() -> {
                    viewModel.setCurrentState(AnnualSubmissionState.COMPLETED);
                    viewModel.setLoading(false);
                });

            } catch (InterruptedException e) {
                javafx.application.Platform.runLater(() -> {
                    viewModel.setErrorMessage("Submission failed: " + e.getMessage());
                    viewModel.setCurrentState(AnnualSubmissionState.FAILED);
                    viewModel.setLoading(false);
                });
            }
        }).start();
    }
}
