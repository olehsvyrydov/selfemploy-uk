package uk.selfemploy.ui.controller;
import uk.selfemploy.ui.component.AppDialog;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import uk.selfemploy.common.domain.AnnualSubmissionState;
import uk.selfemploy.common.domain.TaxCalculationResult;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.legal.Disclaimers;
import uk.selfemploy.core.calculator.TaxLiabilityCalculator;
import uk.selfemploy.core.calculator.TaxLiabilityResult;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.service.HmrcCalculationComparison;
import uk.selfemploy.ui.service.HmrcCalculationService;
import uk.selfemploy.ui.service.HmrcCalculationService.CalculationOutcome;
import uk.selfemploy.ui.service.HmrcFinalDeclarationService;
import uk.selfemploy.ui.service.HmrcFinalDeclarationService.DeclarationConfirmation;
import uk.selfemploy.ui.service.HmrcFinalDeclarationService.DeclarationOutcome;
import uk.selfemploy.ui.service.OAuthServiceFactory;
import uk.selfemploy.ui.service.SqliteDataStore;
import uk.selfemploy.ui.service.SqliteSubmissionRepository;
import uk.selfemploy.ui.service.SubmissionCredentialGate;
import uk.selfemploy.ui.service.SubmissionEnvironment;
import uk.selfemploy.ui.service.SubmissionRecord;
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
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final Logger LOG = Logger.getLogger(AnnualSubmissionController.class.getName());
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);

    // === FXML Injected Elements ===

    @FXML private BorderPane rootPane;
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
    @FXML private Button doneButton;

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

    // HMRC clients, constructed lazily so unit tests that never submit don't touch
    // the OAuth service or database.
    private HmrcCalculationService calculationService;
    private HmrcFinalDeclarationService declarationService;

    // The real HMRC reference from the last successful declaration, shown on the
    // success panel. Never fabricated.
    private String lastHmrcReference = "";

    HmrcCalculationService calculationService() {
        if (calculationService == null) {
            calculationService = new HmrcCalculationService();
        }
        return calculationService;
    }

    HmrcFinalDeclarationService declarationService() {
        if (declarationService == null) {
            declarationService = new HmrcFinalDeclarationService();
        }
        return declarationService;
    }

    /** Visible for testing: inject stubbed HMRC clients. */
    void setHmrcServices(HmrcCalculationService calculationService,
                         HmrcFinalDeclarationService declarationService) {
        this.calculationService = calculationService;
        this.declarationService = declarationService;
    }

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
            // Name the environment so the user always knows whether this is the HMRC
            // sandbox or production.
            submissionDisclaimerText.setText(
                SubmissionEnvironment.current().badgeLabel() + " — "
                + Disclaimers.HMRC_SUBMISSION_DISCLAIMER);
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
        if (stage != null && stage.getScene() != null) {
            // Escape closes the dialog on every non-destructive step. A submission in
            // progress is not interrupted, and steps with confirmed input still ask
            // before discarding via handleCancel.
            stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    handleEscape();
                    event.consume();
                }
            });
        }
    }

    private void handleEscape() {
        AnnualSubmissionState state = viewModel != null ? viewModel.getCurrentState() : null;
        if (state == AnnualSubmissionState.DECLARING) {
            // A live HMRC submission is running; do not let Escape interrupt it.
            return;
        }
        if (state == AnnualSubmissionState.COMPLETED) {
            closeDialog();
            return;
        }
        handleCancel();
    }

    @FXML
    private void handleDone() {
        closeDialog();
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Updates the dialog width based on the current step.
     * Step 1: 700px, Step 2: 900px, Step 3+: 1200px
     */
    private void updateDialogWidth(int currentStep) {
        if (dialogStage == null) {
            return;
        }

        int targetWidth = switch (currentStep) {
            case 1 -> STEP_1_WIDTH;
            case 2 -> STEP_2_WIDTH;
            default -> STEP_3_WIDTH;
        };

        double currentWidth = dialogStage.getWidth();
        if (Math.abs(currentWidth - targetWidth) < 1) {
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
    }

    // === Event Handlers ===

    @FXML
    private void handleCalculate() {
        viewModel.executeNextStep();
        calculateEstimate();
    }

    @FXML
    private void handleReview() {
        viewModel.executeNextStep();
    }

    @FXML
    private void handleSubmit() {
        // Block BEFORE any HMRC call if the taxpayer cannot actually submit, with an
        // actionable message rather than a failed request.
        boolean connected = isConnectedToHmrc();
        String nino = SqliteDataStore.getInstance().loadNino();
        SubmissionCredentialGate.Decision gate = SubmissionCredentialGate.evaluate(connected, nino);
        if (!gate.allowed()) {
            AppDialog.info("Can't submit yet", gate.message());
            return;
        }

        SubmissionEnvironment environment = SubmissionEnvironment.current();
        boolean proceed = AppDialog.confirm("Submit to " + environment.badgeLabel() + "?",
            "This will trigger a real HMRC tax calculation for " + viewModel.getTaxYear().label()
            + " and submit your final declaration to " + environment.badgeLabel() + ".\n\n"
            + "The figures you have confirmed will be declared final. Continue?",
            "Submit", "Cancel");
        if (!proceed) {
            return;
        }

        submitToHmrc(nino);
    }

    private boolean isConnectedToHmrc() {
        try {
            return OAuthServiceFactory.getOAuthService().isConnected();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Could not determine HMRC connection state", e);
            return false;
        }
    }

    /**
     * Starts the real annual submission against HMRC. First triggers a crystallisation
     * calculation off the FX thread; the final declaration is only sent once the
     * taxpayer has seen HMRC's own figures and explicitly confirmed them
     * (see {@link #onCalculationReady}). The taxpayer never declares figures they have
     * not been shown.
     *
     * <p>The wizard stays on the review step throughout the calculation; the Submit step
     * is not marked active until {@link #declareToHmrc} actually sends the declaration.
     */
    private void submitToHmrc(String nino) {
        TaxYear taxYear = viewModel.getTaxYear();

        viewModel.setCurrentState(AnnualSubmissionState.DECLARING);
        viewModel.setLoading(true);

        Thread.startVirtualThread(() -> {
            CalculationOutcome calcOutcome;
            try {
                calcOutcome = calculationService().calculate(nino, taxYear, true);
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE, "Unexpected error requesting HMRC calculation", e);
                failOnFxThread("Unexpected error requesting your HMRC calculation: " + e.getMessage());
                return;
            }
            CalculationOutcome outcome = calcOutcome;
            javafx.application.Platform.runLater(() -> onCalculationReady(nino, taxYear, outcome));
        });
    }

    /**
     * On the FX thread: show HMRC's returned figures next to the app's own estimate and
     * require the taxpayer to confirm HMRC's figures before the final declaration is
     * sent. When the two calculations diverge the taxpayer is warned explicitly.
     * Declining leaves the return undeclared and returns to the review step.
     */
    private void onCalculationReady(String nino, TaxYear taxYear, CalculationOutcome calcOutcome) {
        if (calcOutcome instanceof CalculationOutcome.Failure failure) {
            failOnFxThread("HMRC could not calculate your return: " + failure.message());
            return;
        }
        CalculationOutcome.Success success = (CalculationOutcome.Success) calcOutcome;

        TaxCalculationResult local = viewModel.getCalculationResult();
        HmrcCalculationComparison comparison = new HmrcCalculationComparison(
            local != null ? local.incomeTax() : null,
            local != null ? local.nationalInsuranceClass2() : null,
            local != null ? local.nationalInsuranceClass4() : null,
            success.calculation());

        viewModel.setLoading(false);
        boolean confirmed = AppDialog.confirm(
            comparison.hasMismatch() ? "HMRC's figures differ — review before declaring"
                                     : "Confirm HMRC's figures",
            buildHmrcFiguresMessage(comparison),
            "Declare these figures to HMRC", "Cancel");
        if (!confirmed) {
            viewModel.setCurrentState(AnnualSubmissionState.CALCULATED);
            unlockDeclarationControls();
            updateStepIndicator(3);
            return;
        }

        declareToHmrc(nino, taxYear, success.calculationId());
    }

    /**
     * Sends the final declaration for the confirmed calculation off the FX thread and
     * persists the result there too, so no SQLite I/O runs on the FX thread.
     */
    private void declareToHmrc(String nino, TaxYear taxYear, String calculationId) {
        DeclarationConfirmation confirmation = new DeclarationConfirmation(true, Instant.now());
        viewModel.setCurrentState(AnnualSubmissionState.DECLARING);
        viewModel.setLoading(true);
        updateStepIndicator(4);

        Thread.startVirtualThread(() -> {
            DeclarationOutcome declOutcome;
            try {
                declOutcome = declarationService()
                    .submitFinalDeclaration(nino, taxYear, calculationId, confirmation);
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE, "Unexpected error submitting to HMRC", e);
                failOnFxThread("Unexpected error submitting to HMRC: " + e.getMessage());
                return;
            }
            if (declOutcome instanceof DeclarationOutcome.Failure failure) {
                failOnFxThread("HMRC did not accept the declaration: " + failure.message());
                return;
            }
            String reference = ((DeclarationOutcome.Success) declOutcome).hmrcReference();
            boolean persisted = persistSubmission(taxYear, reference);
            javafx.application.Platform.runLater(() -> onDeclarationComplete(reference, persisted));
        });
    }

    private void onDeclarationComplete(String reference, boolean persisted) {
        lastHmrcReference = reference;
        viewModel.setLoading(false);
        viewModel.setCurrentState(AnnualSubmissionState.COMPLETED);

        if (!persisted) {
            AppDialog.warning("Declared to HMRC, but not saved locally",
                "HMRC accepted your declaration (reference " + reference + "), but it could not "
                + "be saved to your local submission history. Please keep a note of this reference.");
        }
    }

    /**
     * Builds the side-by-side message shown before declaring: the app's estimate and
     * HMRC's figure for each line, flagging any that differ.
     */
    private String buildHmrcFiguresMessage(HmrcCalculationComparison comparison) {
        StringBuilder sb = new StringBuilder();
        if (comparison.hasMismatch()) {
            sb.append("HMRC's calculation does not match this app's estimate. Review both "
                + "figures below before you declare — HMRC's figures are what will be declared "
                + "as final.\n\n");
        } else {
            sb.append("HMRC has calculated your return. These are the figures that will be "
                + "declared as final:\n\n");
        }
        sb.append(String.format("%-14s %12s %12s%n", "", "This app", "HMRC"));
        for (HmrcCalculationComparison.Line line : comparison.lines()) {
            sb.append(String.format("%-14s %12s %12s  %s%n",
                line.label(),
                formatFigure(line.appValue()),
                formatFigure(line.hmrcValue()),
                line.matches() ? "" : "differs"));
        }
        return sb.toString();
    }

    private String formatFigure(BigDecimal amount) {
        return amount == null ? "—" : formatCurrency(amount);
    }

    private void failOnFxThread(String message) {
        javafx.application.Platform.runLater(() -> {
            viewModel.setLoading(false);
            viewModel.setErrorMessage(message);
            viewModel.setCurrentState(AnnualSubmissionState.FAILED);
            updateStepIndicator(3);
        });
    }

    /**
     * Records the completed annual submission with the real HMRC reference so the
     * submission history is truthful.
     *
     * @return true if the record was saved, false if saving failed
     */
    boolean persistSubmission(TaxYear taxYear, String hmrcReference) {
        try {
            String id = UUID.randomUUID().toString();
            SubmissionRecord record = new SubmissionRecord(
                id,
                CoreServiceFactory.getDefaultBusinessId().toString(),
                "ANNUAL",
                taxYear.startYear(),
                taxYear.startDate(),
                taxYear.endDate(),
                viewModel.getTotalIncome(),
                viewModel.getTotalExpenses(),
                viewModel.getNetProfit(),
                "SUBMITTED",
                hmrcReference,
                null,
                Instant.now());
            SqliteSubmissionRepository repository =
                new SqliteSubmissionRepository(CoreServiceFactory.getDefaultBusinessId());
            repository.save(record);
            // save logs and swallows SQL errors, so confirm the row is actually there
            // before telling the caller the save succeeded.
            return repository.findById(id).isPresent();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Failed to persist submission record", e);
            return false;
        }
    }

    @FXML
    private void handleRetry() {
        viewModel.clearError();
        viewModel.executeNextStep();
    }

    @FXML
    private void handleCancel() {
        boolean confirmed = AppDialog.confirm("Cancel Submission",
            "Cancel Annual Submission\n\n"
            + "Are you sure you want to cancel this submission?\n\n"
            + "All progress will be lost.",
            "Yes, cancel", "Keep editing");
        if (confirmed) {
            viewModel.cancel();
            closeDialog();
        }
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
                calculateButton.setDisable(true);
                reviewButton.setDisable(true);
                lockDeclarationControls();
            }
            case COMPLETED -> {
                successPanel.setVisible(true);
                successPanel.setManaged(true);
                actionBar.setVisible(false);
                actionBar.setManaged(false);
                // Hide declaration card on completion (SE-506)
                declarationCard.setVisible(false);
                declarationCard.setManaged(false);
                // Show the real HMRC reference from the declaration (never fabricated).
                submissionReference.setText(lastHmrcReference != null ? lastHmrcReference : "");
            }
            case FAILED -> {
                // Error panel is shown via binding
                calculateButton.setDisable(false);
                reviewButton.setDisable(false);
                unlockDeclarationControls();
            }
        }
    }

    /**
     * Disables the Submit button and the six declaration checkboxes while a live HMRC
     * round-trip is in flight, so nothing can be re-submitted or re-ticked mid-call.
     */
    private void lockDeclarationControls() {
        // Unbind before setting - submitButton is bound to declarationViewModel.isCompleteProperty()
        submitButton.disableProperty().unbind();
        submitButton.setDisable(true);
        if (declarationViewModel != null) {
            declarationViewModel.disabledProperty().set(true);
        }
        setDeclarationCheckboxesDisabled(true);
    }

    /**
     * Re-enables the declaration controls and rebinds the Submit button to the
     * declaration-complete state, so the taxpayer can retry after a failure or after
     * cancelling the HMRC-figures confirmation.
     */
    private void unlockDeclarationControls() {
        if (declarationViewModel != null) {
            submitButton.disableProperty().bind(declarationViewModel.isCompleteProperty().not());
            declarationViewModel.disabledProperty().set(false);
        } else {
            submitButton.disableProperty().unbind();
            submitButton.setDisable(false);
        }
        setDeclarationCheckboxesDisabled(false);
    }

    private void setDeclarationCheckboxesDisabled(boolean disabled) {
        decl1Checkbox.setDisable(disabled);
        decl2Checkbox.setDisable(disabled);
        decl3Checkbox.setDisable(disabled);
        decl4Checkbox.setDisable(disabled);
        decl5Checkbox.setDisable(disabled);
        decl6Checkbox.setDisable(disabled);
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

    // === Local tax estimate (no HMRC call) ===

    /**
     * Calculates a local tax estimate for the reviewed figures using the application's own
     * {@link TaxLiabilityCalculator} — the same engine as the Dashboard and Tax Summary.
     *
     * <p>These figures are an estimate only and are never sent to HMRC; the previous
     * implementation displayed hard-coded tax amounts, which misrepresented the user's
     * position. Package-private for testing.</p>
     */
    void calculateEstimate() {
        TaxYear taxYear = viewModel.getTaxYear();
        BigDecimal netProfit = viewModel.getNetProfit() != null ? viewModel.getNetProfit() : BigDecimal.ZERO;
        try {
            TaxLiabilityCalculator calculator = new TaxLiabilityCalculator(taxYear.startYear());
            TaxLiabilityResult liability = calculator.calculate(netProfit);

            TaxCalculationResult result = TaxCalculationResult.create(
                "local-estimate-" + taxYear.label(),
                viewModel.getTotalIncome(),
                viewModel.getTotalExpenses(),
                netProfit,
                liability.incomeTax(),
                liability.niClass2(),
                liability.niClass4()
            );

            viewModel.setCalculationResult(result);
            viewModel.setCurrentState(AnnualSubmissionState.CALCULATED);
            viewModel.setLoading(false);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Failed to calculate tax estimate", e);
            viewModel.setErrorMessage("Could not calculate your estimate: " + e.getMessage());
            viewModel.setCurrentState(AnnualSubmissionState.FAILED);
            viewModel.setLoading(false);
        }
    }
}
