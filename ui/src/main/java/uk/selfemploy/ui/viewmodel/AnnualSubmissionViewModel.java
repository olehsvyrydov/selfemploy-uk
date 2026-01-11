package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import uk.selfemploy.common.domain.AnnualSubmissionState;
import uk.selfemploy.common.domain.TaxCalculationResult;
import uk.selfemploy.common.domain.TaxYear;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ViewModel for the Annual Self Assessment Submission view.
 *
 * <p>Manages the multi-step submission process with saga state tracking:
 * <ul>
 *   <li>Step 1: Review Summary - Display income, expenses, net profit</li>
 *   <li>Step 2: Calculate Tax - Trigger tax calculation with HMRC</li>
 *   <li>Step 3: Review Calculation - Display tax breakdown and confirm</li>
 *   <li>Step 4: Submit Declaration - Final submission to HMRC</li>
 * </ul>
 *
 * <p>This ViewModel follows the MVVM pattern and is designed for JavaFX property binding.
 *
 * @see uk.selfemploy.ui.controller.AnnualSubmissionController
 */
public class AnnualSubmissionViewModel {

    // === HMRC Official Declaration Text (SE-506, AC-5) ===

    /**
     * Official HMRC declaration text that must be confirmed before submission.
     * This text is mandated by HMRC and must match their official wording exactly.
     */
    public static final String DECLARATION_TEXT =
        "I declare that the information I have given on this tax return " +
        "and any supplementary pages is correct and complete to the best of my knowledge and belief. " +
        "I understand that I may have to pay financial penalties and face prosecution if I give false information.";

    // === Saga Management ===

    private final ObjectProperty<UUID> sagaId = new SimpleObjectProperty<>();
    private final ObjectProperty<AnnualSubmissionState> currentState = new SimpleObjectProperty<>();

    // === Calculation Result ===

    private final ObjectProperty<TaxCalculationResult> calculationResult = new SimpleObjectProperty<>();

    // === UI State ===

    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);
    private final StringProperty errorMessage = new SimpleStringProperty();
    private final BooleanProperty canConfirm = new SimpleBooleanProperty(false);

    // === Declaration State (SE-506) ===

    private final BooleanProperty declarationConfirmed = new SimpleBooleanProperty(false);
    private final ObjectProperty<Instant> declarationTimestamp = new SimpleObjectProperty<>();
    private final BooleanProperty canSubmit = new SimpleBooleanProperty(false);

    // === Step Progress (1-4) ===

    private final IntegerProperty currentStep = new SimpleIntegerProperty(0);
    private final StringProperty stepDescription = new SimpleStringProperty("");

    // === Financial Summary Data ===

    private final ObjectProperty<BigDecimal> totalIncome = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> totalExpenses = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> netProfit = new SimpleObjectProperty<>();

    // === Tax Calculation Data ===

    private final ObjectProperty<BigDecimal> taxDue = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> nationalInsurance = new SimpleObjectProperty<>();

    // === Tax Year ===

    private final ObjectProperty<TaxYear> taxYear = new SimpleObjectProperty<>();

    // === Step Descriptions ===

    private static final String[] STEP_DESCRIPTIONS = {
        "",
        "Review Summary",
        "Calculate Tax",
        "Review Calculation",
        "Submit Declaration"
    };

    public AnnualSubmissionViewModel() {
        // Listen for step changes to update description
        currentStep.addListener((obs, oldVal, newVal) -> {
            int step = newVal.intValue();
            if (step >= 0 && step < STEP_DESCRIPTIONS.length) {
                stepDescription.set(STEP_DESCRIPTIONS[step]);
            }
        });

        // Listen for calculation result changes to update tax properties
        // Uses common module's TaxCalculationResult with incomeTax and NI class 2+4
        calculationResult.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                taxDue.set(newVal.incomeTax());
                // Combine NI Class 2 and Class 4 for total National Insurance
                nationalInsurance.set(
                    newVal.nationalInsuranceClass2().add(newVal.nationalInsuranceClass4())
                );
            }
        });

        // Listen for state changes to update canConfirm
        currentState.addListener((obs, oldVal, newVal) -> {
            canConfirm.set(newVal == AnnualSubmissionState.CALCULATED);
            updateCanSubmit();
        });

        // Listen for declaration changes to update timestamp and canSubmit (SE-506)
        declarationConfirmed.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                // Record timestamp in UTC when declaration is confirmed (AC-4)
                declarationTimestamp.set(Instant.now());
            } else {
                // Clear timestamp when declaration is unchecked
                declarationTimestamp.set(null);
            }
            updateCanSubmit();
        });
    }

    /**
     * Updates the canSubmit property based on current state and declaration.
     * Submit is only allowed when in CALCULATED state AND declaration is confirmed (SE-506, AC-3).
     */
    private void updateCanSubmit() {
        boolean calculatedState = currentState.get() == AnnualSubmissionState.CALCULATED;
        boolean declared = declarationConfirmed.get();
        canSubmit.set(calculatedState && declared);
    }

    // === Public Actions ===

    /**
     * Starts a new annual submission for the given tax year.
     *
     * @param taxYear The tax year to submit for
     */
    public void startSubmission(TaxYear taxYear) {
        this.taxYear.set(taxYear);
        this.sagaId.set(UUID.randomUUID());
        this.currentState.set(AnnualSubmissionState.INITIATED);
        this.currentStep.set(1);
        this.errorMessage.set(null);
        this.isLoading.set(false);
        // Reset declaration state (SE-506)
        this.declarationConfirmed.set(false);
        this.declarationTimestamp.set(null);
    }

    /**
     * Resumes an existing submission using the saga ID.
     *
     * @param sagaId The saga ID to resume
     */
    public void resumeSubmission(UUID sagaId) {
        this.sagaId.set(sagaId);
        this.errorMessage.set(null);
        this.currentState.set(AnnualSubmissionState.INITIATED);
        this.currentStep.set(1);
    }

    /**
     * Executes the next step in the submission saga.
     * Validates that required data is present before proceeding.
     */
    public void executeNextStep() {
        AnnualSubmissionState state = currentState.get();

        if (state == null) {
            throw new IllegalStateException("Cannot execute step: submission not started");
        }

        switch (state) {
            case INITIATED -> {
                // Transition to CALCULATING
                if (!hasRequiredSummaryData()) {
                    throw new IllegalStateException("Cannot calculate: missing summary data");
                }
                currentState.set(AnnualSubmissionState.CALCULATING);
                currentStep.set(2);
                isLoading.set(true);
            }
            case CALCULATED -> {
                // Transition to review
                currentStep.set(3);
                // State remains CALCULATED
            }
            default -> {
                // No action for other states
            }
        }
    }

    /**
     * Confirms the declaration and submits to HMRC.
     * Can only be called when in CALCULATED state at step 3.
     */
    public void confirmAndSubmit() {
        if (currentState.get() != AnnualSubmissionState.CALCULATED) {
            throw new IllegalStateException("Cannot submit: calculation not complete");
        }

        currentState.set(AnnualSubmissionState.DECLARING);
        currentStep.set(4);
        isLoading.set(true);
    }

    /**
     * Cancels the current submission and resets state.
     */
    public void cancel() {
        currentState.set(null);
        currentStep.set(0);
        stepDescription.set("");
        errorMessage.set(null);
        isLoading.set(false);
        canConfirm.set(false);
        sagaId.set(null);
        // Reset declaration state (SE-506)
        declarationConfirmed.set(false);
        declarationTimestamp.set(null);
    }

    /**
     * Clears the current error message.
     */
    public void clearError() {
        errorMessage.set(null);
    }

    // === Saga ID ===

    public UUID getSagaId() {
        return sagaId.get();
    }

    public void setSagaId(UUID value) {
        sagaId.set(value);
    }

    public ObjectProperty<UUID> sagaIdProperty() {
        return sagaId;
    }

    // === Current State ===

    public AnnualSubmissionState getCurrentState() {
        return currentState.get();
    }

    public void setCurrentState(AnnualSubmissionState value) {
        currentState.set(value);
    }

    public ObjectProperty<AnnualSubmissionState> currentStateProperty() {
        return currentState;
    }

    // === Calculation Result ===

    public TaxCalculationResult getCalculationResult() {
        return calculationResult.get();
    }

    public void setCalculationResult(TaxCalculationResult value) {
        calculationResult.set(value);
    }

    public ObjectProperty<TaxCalculationResult> calculationResultProperty() {
        return calculationResult;
    }

    // === Loading ===

    public boolean isLoading() {
        return isLoading.get();
    }

    public void setLoading(boolean value) {
        isLoading.set(value);
    }

    public BooleanProperty isLoadingProperty() {
        return isLoading;
    }

    // === Error Message ===

    public String getErrorMessage() {
        return errorMessage.get();
    }

    public void setErrorMessage(String value) {
        errorMessage.set(value);
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    // === Can Confirm ===

    public boolean canConfirm() {
        return canConfirm.get();
    }

    public BooleanProperty canConfirmProperty() {
        return canConfirm;
    }

    // === Current Step ===

    public int getCurrentStep() {
        return currentStep.get();
    }

    public void setCurrentStep(int value) {
        currentStep.set(value);
    }

    public IntegerProperty currentStepProperty() {
        return currentStep;
    }

    // === Step Description ===

    public String getStepDescription() {
        return stepDescription.get();
    }

    public StringProperty stepDescriptionProperty() {
        return stepDescription;
    }

    // === Financial Summary Properties ===

    public BigDecimal getTotalIncome() {
        return totalIncome.get();
    }

    public void setTotalIncome(BigDecimal value) {
        totalIncome.set(value);
    }

    public ObjectProperty<BigDecimal> totalIncomeProperty() {
        return totalIncome;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses.get();
    }

    public void setTotalExpenses(BigDecimal value) {
        totalExpenses.set(value);
    }

    public ObjectProperty<BigDecimal> totalExpensesProperty() {
        return totalExpenses;
    }

    public BigDecimal getNetProfit() {
        return netProfit.get();
    }

    public void setNetProfit(BigDecimal value) {
        netProfit.set(value);
    }

    public ObjectProperty<BigDecimal> netProfitProperty() {
        return netProfit;
    }

    // === Tax Calculation Properties ===

    public BigDecimal getTaxDue() {
        return taxDue.get();
    }

    public ObjectProperty<BigDecimal> taxDueProperty() {
        return taxDue;
    }

    public BigDecimal getNationalInsurance() {
        return nationalInsurance.get();
    }

    public ObjectProperty<BigDecimal> nationalInsuranceProperty() {
        return nationalInsurance;
    }

    // === Tax Year ===

    public TaxYear getTaxYear() {
        return taxYear.get();
    }

    public void setTaxYear(TaxYear value) {
        taxYear.set(value);
    }

    public ObjectProperty<TaxYear> taxYearProperty() {
        return taxYear;
    }

    // === Declaration Properties (SE-506) ===

    /**
     * Returns whether the user has confirmed the declaration.
     *
     * @return true if declaration is confirmed
     */
    public boolean isDeclarationConfirmed() {
        return declarationConfirmed.get();
    }

    /**
     * Sets the declaration confirmation state.
     * When set to true, automatically records the timestamp in UTC.
     * When set to false, clears the timestamp.
     *
     * @param value true to confirm, false to unconfirm
     */
    public void setDeclarationConfirmed(boolean value) {
        declarationConfirmed.set(value);
    }

    /**
     * Returns the declaration confirmed property for binding.
     *
     * @return the declaration confirmed property
     */
    public BooleanProperty declarationConfirmedProperty() {
        return declarationConfirmed;
    }

    /**
     * Returns the timestamp when the declaration was confirmed.
     * The timestamp is in UTC (ISO 8601 format when serialized).
     *
     * @return the declaration timestamp, or null if not confirmed
     */
    public Instant getDeclarationTimestamp() {
        return declarationTimestamp.get();
    }

    /**
     * Returns the declaration timestamp property for binding.
     *
     * @return the declaration timestamp property
     */
    public ObjectProperty<Instant> declarationTimestampProperty() {
        return declarationTimestamp;
    }

    /**
     * Returns whether the user can submit to HMRC.
     * Submit is only allowed when in CALCULATED state AND declaration is confirmed.
     *
     * @return true if submission is allowed
     */
    public boolean canSubmit() {
        return canSubmit.get();
    }

    /**
     * Returns the canSubmit property for binding to submit button disable state.
     *
     * @return the canSubmit property
     */
    public BooleanProperty canSubmitProperty() {
        return canSubmit;
    }

    // === Private Helper Methods ===

    private boolean hasRequiredSummaryData() {
        return totalIncome.get() != null
            && totalExpenses.get() != null
            && netProfit.get() != null;
    }
}
