package uk.selfemploy.ui.viewmodel;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import uk.selfemploy.core.service.SubmissionDeclaration;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * ViewModel for the 6-checkbox submission declaration UI component.
 *
 * <p>SE-512: Provides JavaFX property bindings for all 6 HMRC-required
 * declaration confirmations. Tracks checkbox state, progress, and generates
 * the final SubmissionDeclaration record when complete.</p>
 *
 * <h3>The 6 Declaration Properties:</h3>
 * <ol>
 *   <li>{@link #accuracyStatementProperty()} - Information is correct and complete</li>
 *   <li>{@link #penaltiesWarningProperty()} - Understands penalties for false info</li>
 *   <li>{@link #recordKeepingProperty()} - Records kept for 5 years</li>
 *   <li>{@link #calculationVerificationProperty()} - Tax calculation reviewed</li>
 *   <li>{@link #legalEffectProperty()} - Submission is a legal act</li>
 *   <li>{@link #identityConfirmationProperty()} - User is taxpayer or authorized</li>
 * </ol>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * SubmissionDeclarationViewModel viewModel = new SubmissionDeclarationViewModel(clock, "2025-26");
 *
 * // Bind checkboxes
 * checkbox1.selectedProperty().bindBidirectional(viewModel.accuracyStatementProperty());
 *
 * // Bind submit button
 * submitButton.disableProperty().bind(viewModel.isCompleteProperty().not());
 *
 * // Build final declaration when complete
 * if (viewModel.isCompleteProperty().get()) {
 *     SubmissionDeclaration declaration = viewModel.buildDeclaration().orElseThrow();
 * }
 * }</pre>
 */
public class SubmissionDeclarationViewModel {

    private static final int DECLARATION_COUNT = 6;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss 'on' d MMMM yyyy");

    // === Clock and Tax Year ===

    private final Clock clock;
    private final String taxYear;

    // === Checkbox Properties ===

    private final BooleanProperty accuracyStatement = new SimpleBooleanProperty(false);
    private final BooleanProperty penaltiesWarning = new SimpleBooleanProperty(false);
    private final BooleanProperty recordKeeping = new SimpleBooleanProperty(false);
    private final BooleanProperty calculationVerification = new SimpleBooleanProperty(false);
    private final BooleanProperty legalEffect = new SimpleBooleanProperty(false);
    private final BooleanProperty identityConfirmation = new SimpleBooleanProperty(false);

    // === Computed Properties ===

    private final IntegerProperty confirmedCount = new SimpleIntegerProperty(0);
    private final BooleanProperty isComplete = new SimpleBooleanProperty(false);
    private final StringProperty progressText = new SimpleStringProperty("0 of 6 confirmations completed");
    private final BooleanProperty showTimestampSection = new SimpleBooleanProperty(false);
    private final BooleanProperty disabled = new SimpleBooleanProperty(false);

    // === Cached Declaration (for ID preservation) ===

    private SubmissionDeclaration cachedDeclaration;

    /**
     * Creates a new SubmissionDeclarationViewModel.
     *
     * @param clock   clock for timestamps (allows testing with fixed clock)
     * @param taxYear tax year for the submission (e.g., "2025-26")
     */
    public SubmissionDeclarationViewModel(Clock clock, String taxYear) {
        this.clock = clock;
        this.taxYear = taxYear;
        setupBindings();
        setupListeners();
    }

    /**
     * Creates a new SubmissionDeclarationViewModel using the system clock.
     *
     * @param taxYear tax year for the submission (e.g., "2025-26")
     */
    public SubmissionDeclarationViewModel(String taxYear) {
        this(Clock.systemUTC(), taxYear);
    }

    // === Checkbox Properties ===

    /**
     * Property for Declaration 1: Accuracy Statement.
     * HMRC official declaration that information is correct and complete.
     */
    public BooleanProperty accuracyStatementProperty() {
        return accuracyStatement;
    }

    /**
     * Property for Declaration 2: Penalties Warning.
     * HMRC official acknowledgment of penalties for false information.
     */
    public BooleanProperty penaltiesWarningProperty() {
        return penaltiesWarning;
    }

    /**
     * Property for Declaration 3: Record Keeping.
     * Confirmation that records are kept for 5 years.
     */
    public BooleanProperty recordKeepingProperty() {
        return recordKeeping;
    }

    /**
     * Property for Declaration 4: Calculation Verification.
     * Confirmation that tax calculation has been reviewed.
     */
    public BooleanProperty calculationVerificationProperty() {
        return calculationVerification;
    }

    /**
     * Property for Declaration 5: Legal Effect.
     * Acknowledgment that submission is a legal act.
     */
    public BooleanProperty legalEffectProperty() {
        return legalEffect;
    }

    /**
     * Property for Declaration 6: Identity Confirmation.
     * Confirmation of identity or authorization.
     */
    public BooleanProperty identityConfirmationProperty() {
        return identityConfirmation;
    }

    // === Computed Properties ===

    /**
     * The number of checkboxes currently checked (0-6).
     */
    public ReadOnlyIntegerProperty confirmedCountProperty() {
        return confirmedCount;
    }

    /**
     * True when all 6 checkboxes are checked.
     */
    public ReadOnlyBooleanProperty isCompleteProperty() {
        return isComplete;
    }

    /**
     * Progress text like "3 of 6 confirmations completed".
     */
    public ReadOnlyStringProperty progressTextProperty() {
        return progressText;
    }

    /**
     * True when timestamp section should be visible (all checkboxes checked).
     */
    public ReadOnlyBooleanProperty showTimestampSectionProperty() {
        return showTimestampSection;
    }

    /**
     * Property to disable all checkboxes (e.g., during submission).
     */
    public BooleanProperty disabledProperty() {
        return disabled;
    }

    // === Declaration Text Getters ===

    /**
     * Returns the declaration text for accuracy statement.
     */
    public String getAccuracyStatementText() {
        return SubmissionDeclaration.getDeclarationText("accuracy_statement");
    }

    /**
     * Returns the declaration text for penalties warning.
     */
    public String getPenaltiesWarningText() {
        return SubmissionDeclaration.getDeclarationText("penalties_warning");
    }

    /**
     * Returns the declaration text for record keeping.
     */
    public String getRecordKeepingText() {
        return SubmissionDeclaration.getDeclarationText("record_keeping");
    }

    /**
     * Returns the declaration text for calculation verification.
     */
    public String getCalculationVerificationText() {
        return SubmissionDeclaration.getDeclarationText("calculation_verification");
    }

    /**
     * Returns the declaration text for legal effect.
     */
    public String getLegalEffectText() {
        return SubmissionDeclaration.getDeclarationText("legal_effect");
    }

    /**
     * Returns the declaration text for identity confirmation.
     */
    public String getIdentityConfirmationText() {
        return SubmissionDeclaration.getDeclarationText("identity_confirmation");
    }

    // === Timestamp and ID Display ===

    /**
     * Returns formatted timestamp display or empty string if not complete.
     * Format: "14:30:00 on 12 January 2026"
     */
    public String getTimestampDisplay() {
        if (!isComplete.get()) {
            return "";
        }
        ensureDeclarationBuilt();
        if (cachedDeclaration == null) {
            return "";
        }
        return cachedDeclaration.completedAt()
                .atZone(ZoneId.systemDefault())
                .format(TIMESTAMP_FORMATTER);
    }

    /**
     * Returns declaration ID or empty string if not complete.
     * Format: "DECL-YYYYMMDD-HHMMSS-XXXXX"
     */
    public String getDeclarationIdDisplay() {
        if (!isComplete.get()) {
            return "";
        }
        ensureDeclarationBuilt();
        if (cachedDeclaration == null) {
            return "";
        }
        return cachedDeclaration.declarationId();
    }

    // === Build Declaration ===

    /**
     * Builds the final SubmissionDeclaration if complete.
     *
     * @return Optional containing the declaration if complete, empty otherwise
     */
    public Optional<SubmissionDeclaration> buildDeclaration() {
        if (!isComplete.get()) {
            return Optional.empty();
        }
        ensureDeclarationBuilt();
        return Optional.ofNullable(cachedDeclaration);
    }

    // === Reset ===

    /**
     * Resets all checkboxes and clears cached declaration.
     */
    public void reset() {
        accuracyStatement.set(false);
        penaltiesWarning.set(false);
        recordKeeping.set(false);
        calculationVerification.set(false);
        legalEffect.set(false);
        identityConfirmation.set(false);
        cachedDeclaration = null;
    }

    // === Private Methods ===

    private void setupBindings() {
        // Bind confirmed count
        confirmedCount.bind(Bindings.createIntegerBinding(
                this::countConfirmed,
                accuracyStatement, penaltiesWarning, recordKeeping,
                calculationVerification, legalEffect, identityConfirmation
        ));

        // Bind isComplete
        isComplete.bind(confirmedCount.isEqualTo(DECLARATION_COUNT));

        // Bind progress text
        progressText.bind(Bindings.createStringBinding(
                () -> confirmedCount.get() + " of " + DECLARATION_COUNT + " confirmations completed",
                confirmedCount
        ));

        // Bind showTimestampSection
        showTimestampSection.bind(isComplete);
    }

    private void setupListeners() {
        // Clear cached declaration when any checkbox changes
        accuracyStatement.addListener((obs, old, val) -> onCheckboxChange());
        penaltiesWarning.addListener((obs, old, val) -> onCheckboxChange());
        recordKeeping.addListener((obs, old, val) -> onCheckboxChange());
        calculationVerification.addListener((obs, old, val) -> onCheckboxChange());
        legalEffect.addListener((obs, old, val) -> onCheckboxChange());
        identityConfirmation.addListener((obs, old, val) -> onCheckboxChange());
    }

    private void onCheckboxChange() {
        // Only clear cached declaration if we become incomplete
        if (!isComplete.get()) {
            cachedDeclaration = null;
        }
    }

    private int countConfirmed() {
        int count = 0;
        if (accuracyStatement.get()) count++;
        if (penaltiesWarning.get()) count++;
        if (recordKeeping.get()) count++;
        if (calculationVerification.get()) count++;
        if (legalEffect.get()) count++;
        if (identityConfirmation.get()) count++;
        return count;
    }

    private void ensureDeclarationBuilt() {
        if (cachedDeclaration != null) {
            return;
        }
        if (!isComplete.get()) {
            return;
        }

        SubmissionDeclaration.Builder builder = SubmissionDeclaration.builder(clock)
                .forTaxYear(taxYear);

        if (accuracyStatement.get()) {
            builder.confirm("accuracy_statement");
        }
        if (penaltiesWarning.get()) {
            builder.confirm("penalties_warning");
        }
        if (recordKeeping.get()) {
            builder.confirm("record_keeping");
        }
        if (calculationVerification.get()) {
            builder.confirm("calculation_verification");
        }
        if (legalEffect.get()) {
            builder.confirm("legal_effect");
        }
        if (identityConfirmation.get()) {
            builder.confirm("identity_confirmation");
        }

        cachedDeclaration = builder.build();
    }
}
