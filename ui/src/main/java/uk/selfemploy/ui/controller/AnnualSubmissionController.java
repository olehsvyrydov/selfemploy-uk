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

import java.math.BigDecimal;
import java.text.NumberFormat;
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

    // Declaration Card (SE-506)
    @FXML private VBox declarationCard;
    @FXML private CheckBox declarationCheckbox;
    @FXML private HBox timestampBox;
    @FXML private Label timestampLabel;

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

    // === ViewModel ===

    private AnnualSubmissionViewModel viewModel;

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
    }

    /**
     * Gets the ViewModel for testing purposes.
     */
    public AnnualSubmissionViewModel getViewModel() {
        return viewModel;
    }

    // === Event Handlers ===

    @FXML
    private void handleCalculate() {
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
     * Handles declaration checkbox state change (SE-506).
     * Updates the ViewModel and UI elements based on checkbox state.
     */
    @FXML
    private void handleDeclarationChange() {
        boolean isChecked = declarationCheckbox.isSelected();

        // Update ViewModel - this will automatically record timestamp (AC-4)
        viewModel.setDeclarationConfirmed(isChecked);

        // Update UI elements
        updateDeclarationUI(isChecked);
    }

    /**
     * Updates the declaration UI based on checkbox state.
     *
     * @param isChecked whether the declaration checkbox is checked
     */
    private void updateDeclarationUI(boolean isChecked) {
        if (isChecked) {
            // Show timestamp (AC-4)
            Instant timestamp = viewModel.getDeclarationTimestamp();
            if (timestamp != null) {
                String timeStr = timestamp.atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
                String dateStr = timestamp.atZone(ZoneId.systemDefault()).format(DATE_FORMATTER);
                timestampLabel.setText(String.format("Declaration confirmed at %s on %s", timeStr, dateStr));
            }
            timestampBox.setVisible(true);
            timestampBox.setManaged(true);

            // Update helper text (AC-3)
            submitHelperText.setText("Ready to submit");
            submitHelperText.getStyleClass().add("ready");

            // Enable submit button and change to green style
            submitButton.setDisable(false);
            submitButton.getStyleClass().add("ready");
        } else {
            // Hide timestamp
            timestampBox.setVisible(false);
            timestampBox.setManaged(false);

            // Update helper text (AC-3)
            submitHelperText.setText("Please confirm the declaration above");
            submitHelperText.getStyleClass().remove("ready");

            // Disable submit button and remove green style
            submitButton.setDisable(true);
            submitButton.getStyleClass().remove("ready");
        }
    }

    // === Private Methods ===

    private void setupBindings() {
        // Bind financial summary values
        turnoverValue.textProperty().bind(
            Bindings.createStringBinding(
                () -> CURRENCY_FORMAT.format(viewModel.totalIncomeProperty().get()),
                viewModel.totalIncomeProperty()
            )
        );
        expensesValue.textProperty().bind(
            Bindings.createStringBinding(
                () -> CURRENCY_FORMAT.format(viewModel.totalExpensesProperty().get()),
                viewModel.totalExpensesProperty()
            )
        );
        netProfitValue.textProperty().bind(
            Bindings.createStringBinding(
                () -> CURRENCY_FORMAT.format(viewModel.netProfitProperty().get()),
                viewModel.netProfitProperty()
            )
        );

        // Bind tax calculation values
        incomeTaxValue.textProperty().bind(
            Bindings.createStringBinding(
                () -> CURRENCY_FORMAT.format(viewModel.taxDueProperty().get()),
                viewModel.taxDueProperty()
            )
        );
        niValue.textProperty().bind(
            Bindings.createStringBinding(
                () -> CURRENCY_FORMAT.format(viewModel.nationalInsuranceProperty().get()),
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
            updateStepIndicator(newVal.intValue());
            updateActionButtons(newVal.intValue());
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
                // Show declaration card and submit container (SE-506, AC-1)
                declarationCard.setVisible(true);
                declarationCard.setManaged(true);
                submitContainer.setVisible(true);
                submitContainer.setManaged(true);
                // Reset declaration checkbox state
                declarationCheckbox.setSelected(false);
                updateDeclarationUI(false);
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
                // Disable declaration checkbox during submission (SE-506)
                declarationCheckbox.setDisable(true);
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
