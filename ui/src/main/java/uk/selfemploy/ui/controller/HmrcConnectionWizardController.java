package uk.selfemploy.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.ui.component.InfoCard;
import uk.selfemploy.ui.component.NinoInputField;
import uk.selfemploy.ui.component.PrerequisiteItem;
import uk.selfemploy.ui.component.WizardProgressIndicator;
import uk.selfemploy.ui.service.OAuthConnectionHandler;
import uk.selfemploy.ui.service.OAuthConnectionHandler.ConnectionStatus;
import uk.selfemploy.ui.service.SqliteDataStore;
import uk.selfemploy.ui.service.WizardProgressRepository;
import uk.selfemploy.ui.util.DialogStyler;
import uk.selfemploy.ui.util.HmrcErrorGuidance;
import uk.selfemploy.ui.viewmodel.HmrcConnectionWizardViewModel;

import java.time.Clock;
import java.time.Duration;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the HMRC Connection Wizard.
 * Sprint 12 - SE-12-001: Prerequisites Checklist Screen
 *
 * <p>A 5-step wizard for connecting to HMRC services. Step 1 displays
 * the prerequisites checklist that users must complete before proceeding.</p>
 *
 * <h2>Wizard Steps</h2>
 * <ol>
 *   <li>Prerequisites Checklist</li>
 *   <li>Enter Credentials</li>
 *   <li>OAuth Authorization</li>
 *   <li>Confirmation</li>
 *   <li>Complete</li>
 * </ol>
 *
 * <h2>Accessibility</h2>
 * <ul>
 *   <li>Full keyboard navigation with Tab</li>
 *   <li>Escape key to cancel</li>
 *   <li>Enter/Space to activate links and buttons</li>
 *   <li>ARIA labels for screen readers</li>
 * </ul>
 */
public class HmrcConnectionWizardController implements Initializable {

    private static final Logger LOG = Logger.getLogger(HmrcConnectionWizardController.class.getName());

    // === Dialog Constants ===
    public static final int DIALOG_WIDTH = 520;
    public static final int DIALOG_MIN_HEIGHT = 400;
    public static final int DIALOG_MAX_HEIGHT = 600;
    public static final String WIZARD_TITLE = "Connect to HMRC";

    // === Prerequisite Constants ===
    public static final int PREREQUISITE_COUNT = 3;

    public static final String SELF_ASSESSMENT_URL = "https://www.gov.uk/register-for-self-assessment";
    public static final String GOV_GATEWAY_URL = "https://www.gov.uk/log-in-register-hmrc-online-services";
    public static final String FIND_NINO_URL = "https://www.gov.uk/lost-national-insurance-number";

    public static final String PREREQ_TITLE_SELF_ASSESSMENT = "Registered for Self Assessment";
    public static final String PREREQ_TITLE_GOV_GATEWAY = "Government Gateway account";
    public static final String PREREQ_TITLE_NINO = "Your National Insurance Number (NINO)";

    public static final String PREREQ_DESC_SELF_ASSESSMENT =
            "You must be registered with HMRC for Self Assessment tax returns.";
    public static final String PREREQ_DESC_GOV_GATEWAY =
            "You need a Government Gateway user ID and password.";
    public static final String PREREQ_DESC_NINO =
            "A 9-character code like 'QQ 12 34 56 A'. Find it on payslips, P60, or tax letters.";

    // === Step 2 Constants (SE-12-002) ===
    public static final String STEP2_TITLE = "Enter your National Insurance Number";
    public static final String NINO_PLACEHOLDER = "QQ 12 34 56 A";
    public static final String NINO_VALIDATION_ERROR =
            "Invalid format. NINO should be like \"QQ 12 34 56 A\"";

    // FIN-001: NINO vs UTR clarification (per /inga)
    public static final String NINO_VS_UTR_TITLE = "NINO vs UTR";
    public static final String NINO_VS_UTR_DESCRIPTION =
            "Your National Insurance Number (NINO) is different from your Unique Taxpayer Reference (UTR). " +
            "Your NINO is a 9-character code like 'QQ 12 34 56 A' found on payslips, P60, or tax letters. " +
            "Your UTR is a 10-digit number used for Self Assessment - you'll need this later for submissions.";

    // === Step 3 Constants (SE-12-003) ===
    public static final String STEP3_TITLE = "About Government Gateway";

    public static final String GOV_GATEWAY_INTRO =
            "Government Gateway is HMRC's secure login service. " +
            "It's the same system you use for:";

    public static final String[] GOV_GATEWAY_USES = {
            "Filing your Self Assessment",
            "Viewing your tax account",
            "Managing your tax credits"
    };

    public static final String SECURITY_TITLE = "Your Security";
    public static final String SECURITY_MESSAGE =
            "When you click \"Next\", your browser will open the Government Gateway login page. " +
            "You'll sign in directly with HMRC - this app NEVER sees your password. " +
            "After you authorize, you'll return here automatically.";

    public static final String WHAT_HAPPENS_TITLE = "What happens next";
    public static final String[] WHAT_HAPPENS_STEPS = {
            "Your browser opens to Government Gateway",
            "Sign in with your Government Gateway credentials",
            "Click \"Grant authority\" to authorize this app",
            "You'll be redirected back here automatically"
    };

    // === Step 4 Constants (SE-12-004) ===
    public static final String STEP4_TITLE = "Connect to HMRC";

    // FIN-003: Privacy reminder before OAuth
    public static final String PRIVACY_REMINDER =
            "Your data stays on your device. We only connect to HMRC to submit your tax information.";

    public static final String CONNECTING_TO_HMRC = "Connecting to HMRC...";
    public static final String OAUTH_SUCCESS_MESSAGE = "Successfully connected to HMRC";
    public static final String OAUTH_TIMEOUT_MESSAGE = "Connection timed out. HMRC may be busy.";
    public static final String OAUTH_CANCELLED_MESSAGE = "Connection cancelled.";

    // Browser waiting messages
    public static final String BROWSER_WAITING_TITLE = "Complete sign-in in your browser";
    public static final String BROWSER_WAITING_DESC =
            "A browser window has opened. Please sign in with your Government Gateway credentials and grant access.";
    public static final String BROWSER_WAITING_HINT =
            "This window will update automatically once you complete the sign-in.";

    public static final Duration OAUTH_TIMEOUT = Duration.ofSeconds(120);

    // === Step 5 Constants (SE-12-005) ===
    public static final String STEP5_TITLE = "Connected to HMRC";
    public static final String SUCCESS_MESSAGE = "Successfully Connected to HMRC";
    public static final String SUCCESS_DESCRIPTION =
            "Your account is now linked. You can submit quarterly updates and annual returns directly to HMRC.";

    public static final String WHAT_YOU_CAN_DO = "What you can do now:";
    public static final String[] NEXT_STEPS = {
            "Submit quarterly updates to HMRC",
            "File your annual Self Assessment",
            "View your submission history"
    };

    // FIN-002: 5-year retention reminder (per /inga)
    public static final String RETENTION_TITLE = "Important Reminder";
    public static final String RETENTION_MESSAGE =
            "HMRC requires you to keep records for at least 5 years after the submission deadline.";

    // === FXML Injected Fields ===
    @FXML private VBox wizardContainer;
    @FXML private HBox headerBox;
    @FXML private HBox progressContainer;
    @FXML private VBox contentArea;
    @FXML private HBox footerBox;
    @FXML private Button cancelButton;
    @FXML private Button backButton;
    @FXML private Button nextButton;
    @FXML private Label stepLabel;

    // === State ===
    private HmrcConnectionWizardViewModel viewModel;
    private Stage dialogStage;
    private WizardProgressIndicator progressIndicator;

    // Step 2 components (SE-12-002)
    private NinoInputField ninoInput;
    private Label validationErrorLabel;

    // Step 4 components (SE-12-004)
    private OAuthConnectionHandler oAuthHandler;
    private HmrcOAuthService oAuthService;
    private WizardProgressRepository progressRepository;
    private HmrcErrorGuidance errorGuidance;
    private Label statusLabel;
    private ProgressIndicator spinner;
    private VBox connectContainer;
    private VBox progressContainer4;
    private VBox successContainer;
    private VBox errorContainer;

    // Header components (for Step 5 green theme)
    private HBox headerPane;
    private FontIcon headerIcon;
    private Label headerTitle;

    /**
     * Creates a new wizard controller with default view model.
     */
    public HmrcConnectionWizardController() {
        this.viewModel = new HmrcConnectionWizardViewModel();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupProgressIndicator();
        setupKeyboardHandlers();
        bindToViewModel();
        loadStep1Content();
    }

    /**
     * Sets the view model (for testing).
     *
     * @param viewModel the view model to use
     */
    public void setViewModel(HmrcConnectionWizardViewModel viewModel) {
        this.viewModel = viewModel;
    }

    /**
     * Sets the dialog stage for closing.
     *
     * @param stage the dialog stage
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * Sets the OAuth service for Step 4.
     * Required for OAuth connection functionality.
     *
     * @param service the HMRC OAuth service
     */
    public void setOAuthService(HmrcOAuthService service) {
        this.oAuthService = service;
    }

    /**
     * Sets the wizard progress repository for Step 4.
     * Required for persisting wizard state.
     *
     * @param repository the progress repository
     */
    public void setProgressRepository(WizardProgressRepository repository) {
        this.progressRepository = repository;
    }

    /**
     * Sets the error guidance service for Step 4.
     * Required for user-friendly error messages.
     *
     * @param guidance the error guidance service
     */
    public void setErrorGuidance(HmrcErrorGuidance guidance) {
        this.errorGuidance = guidance;
    }

    // === Setup Methods ===

    /**
     * Sets up the wizard progress indicator.
     */
    private void setupProgressIndicator() {
        progressIndicator = new WizardProgressIndicator(
                HmrcConnectionWizardViewModel.TOTAL_STEPS,
                viewModel.getCurrentStep()
        );

        if (progressContainer != null) {
            progressContainer.getChildren().clear();
            progressContainer.getChildren().add(progressIndicator);
            progressContainer.setAlignment(Pos.CENTER);
        }
    }

    /**
     * Sets up keyboard event handlers.
     */
    private void setupKeyboardHandlers() {
        if (wizardContainer != null) {
            wizardContainer.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
        }
    }

    /**
     * Binds UI elements to the view model.
     */
    private void bindToViewModel() {
        if (nextButton != null) {
            nextButton.textProperty().bind(viewModel.currentStepProperty().asString().map(
                    s -> viewModel.getNextButtonText()
            ));
            nextButton.disableProperty().bind(viewModel.canProceedProperty().not());
        }

        if (cancelButton != null) {
            cancelButton.setText(viewModel.getCancelButtonText());
        }

        if (stepLabel != null) {
            viewModel.currentStepProperty().addListener((obs, old, newVal) -> {
                stepLabel.setText(viewModel.getStepLabel());
                progressIndicator.setCurrentStep(newVal.intValue());
            });
            stepLabel.setText(viewModel.getStepLabel());
        }
    }

    // === Content Loading ===

    /**
     * Loads Step 1: Prerequisites Checklist content.
     */
    private void loadStep1Content() {
        if (contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();
        contentArea.setSpacing(12);
        contentArea.setPadding(new Insets(20));

        // Step description
        Label descLabel = new Label("Before connecting to HMRC, please ensure you have the following:");
        descLabel.getStyleClass().add("hmrc-wizard-step-description");
        descLabel.setWrapText(true);
        contentArea.getChildren().add(descLabel);

        // Prerequisite items
        PrerequisiteItem item1 = new PrerequisiteItem(
                "FILE_ALT", "teal",
                PREREQ_TITLE_SELF_ASSESSMENT,
                PREREQ_DESC_SELF_ASSESSMENT,
                "How to register",
                SELF_ASSESSMENT_URL
        );
        item1.setOnLinkClick(this::openExternalLink);

        PrerequisiteItem item2 = new PrerequisiteItem(
                "USER_SHIELD", "blue",
                PREREQ_TITLE_GOV_GATEWAY,
                PREREQ_DESC_GOV_GATEWAY,
                "Create an account",
                GOV_GATEWAY_URL
        );
        item2.setOnLinkClick(this::openExternalLink);

        PrerequisiteItem item3 = new PrerequisiteItem(
                "ID_CARD", "violet",
                PREREQ_TITLE_NINO,
                PREREQ_DESC_NINO,
                "Find your NINO",
                FIND_NINO_URL
        );
        item3.setOnLinkClick(this::openExternalLink);

        contentArea.getChildren().addAll(item1, item2, item3);

        // Bind checkbox states to enable/disable "Get Started" button
        // All checkboxes must be checked to proceed
        // Note: nextButton.disableProperty() is bound to viewModel.canProceedProperty().not()
        // in bindToViewModel(), so we only need to update the viewModel
        Runnable updateCanProceed = () -> {
            boolean allChecked = item1.isChecked() && item2.isChecked() && item3.isChecked();
            viewModel.setCanProceed(allChecked);
        };

        // Initial state: disabled until all checked
        viewModel.setCanProceed(false);

        // Listen for checkbox changes
        item1.checkedProperty().addListener((obs, oldVal, newVal) -> updateCanProceed.run());
        item2.checkedProperty().addListener((obs, oldVal, newVal) -> updateCanProceed.run());
        item3.checkedProperty().addListener((obs, oldVal, newVal) -> updateCanProceed.run());
    }

    // === Event Handlers ===

    /**
     * Handles global key press events.
     */
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            handleEscapeKey(event);
        }
    }

    /**
     * Handles the Escape key to cancel the wizard.
     */
    void handleEscapeKey(KeyEvent event) {
        handleCancel();
        event.consume();
    }

    /**
     * Handles the Cancel button click.
     */
    @FXML
    void handleCancel() {
        viewModel.cancel();
        closeDialog();
    }

    /**
     * Handles the Next/Get Started button click.
     * On Step 5, clicking "Done" closes the wizard.
     */
    @FXML
    void handleNext() {
        if (viewModel.getCurrentStep() == 5) {
            // Step 5: "Done" button closes the wizard
            closeDialog();
            return;
        }

        if (viewModel.canProceed()) {
            viewModel.goNext();
            updateContentForStep();
            updateBackButtonVisibility();
        }
    }

    /**
     * Handles the Back button click.
     * Goes to the previous step.
     */
    @FXML
    void handleBack() {
        if (viewModel.getCurrentStep() > 1) {
            viewModel.goBack();
            updateContentForStep();
            updateBackButtonVisibility();
            // Re-enable proceed since we're going back to a completed step
            viewModel.setCanProceed(true);
        }
    }

    /**
     * Updates the Back button visibility based on current step.
     * Hidden on Step 1 and Step 5.
     */
    private void updateBackButtonVisibility() {
        if (backButton != null) {
            int step = viewModel.getCurrentStep();
            boolean showBack = step > 1 && step < 5;
            backButton.setVisible(showBack);
            backButton.setManaged(showBack);
        }
    }

    /**
     * Updates the content area for the current step.
     */
    private void updateContentForStep() {
        int step = viewModel.getCurrentStep();
        switch (step) {
            case 1 -> loadStep1Content();
            case 2 -> loadStep2Content();
            case 3 -> loadStep3Content();
            case 4 -> loadStep4Content();
            case 5 -> loadStep5Content();
        }
    }

    /**
     * Loads Step 2: NINO Entry content.
     * SE-12-002: NINO Entry with Validation Screen
     */
    private void loadStep2Content() {
        if (contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();
        contentArea.setSpacing(16);
        contentArea.setPadding(new Insets(20));

        // Title
        Label title = new Label(STEP2_TITLE);
        title.getStyleClass().add("hmrc-wizard-section-title");
        title.setWrapText(true);

        // Validation error label - create BEFORE setting up listeners to avoid NPE
        validationErrorLabel = new Label(NINO_VALIDATION_ERROR);
        validationErrorLabel.getStyleClass().add("validation-error");
        validationErrorLabel.setVisible(false);
        validationErrorLabel.setManaged(false);

        // NINO input field
        ninoInput = new NinoInputField();
        ninoInput.validProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setCanProceed(newVal);
            boolean showError = !newVal && !ninoInput.getNino().isEmpty();
            validationErrorLabel.setVisible(showError);
            validationErrorLabel.setManaged(showError);
        });

        // Bind NINO to ViewModel
        ninoInput.ninoProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setNino(newVal);
        });

        // Pre-populate NINO: first from ViewModel (if returning to step), then from Settings
        String existingNino = viewModel.getNino();
        boolean preFilledFromSettings = false;

        if (existingNino != null && !existingNino.isEmpty()) {
            ninoInput.setNino(existingNino);
        } else {
            // Try to load from Settings (SqliteDataStore)
            String savedNino = SqliteDataStore.getInstance().loadNino();
            if (savedNino != null && !savedNino.isEmpty()) {
                ninoInput.setNino(savedNino);
                viewModel.setNino(savedNino);
                preFilledFromSettings = true;
                LOG.info("NINO pre-filled from Settings");
            }
        }

        // Initial state: cannot proceed until valid NINO
        viewModel.setCanProceed(ninoInput.isValid());

        // Info label for pre-filled NINO (only show if loaded from Settings)
        Label preFilledLabel = new Label("Pre-filled from your profile");
        preFilledLabel.getStyleClass().add("nino-prefilled-hint");
        preFilledLabel.setVisible(preFilledFromSettings);
        preFilledLabel.setManaged(preFilledFromSettings);

        // Separator before info box
        Separator separator = new Separator();
        separator.getStyleClass().add("nino-separator");

        // NINO vs UTR info box (FIN-001)
        VBox infoBox = createNinoVsUtrInfoBox();

        // Find NINO link
        Hyperlink findNinoLink = new Hyperlink("Find your NINO on GOV.UK >");
        findNinoLink.getStyleClass().add("nino-gov-link");
        findNinoLink.setOnAction(e -> openExternalLink(FIND_NINO_URL));

        contentArea.getChildren().addAll(
            title,
            ninoInput,
            preFilledLabel,
            validationErrorLabel,
            separator,
            infoBox,
            findNinoLink
        );
    }

    /**
     * Creates the NINO vs UTR info box per FIN-001 requirement.
     *
     * @return the info box VBox
     */
    private VBox createNinoVsUtrInfoBox() {
        VBox infoBox = new VBox(8);
        infoBox.getStyleClass().add("nino-info-box");
        infoBox.setPadding(new Insets(12));

        // Info icon and title row
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon infoIcon = FontIcon.of(FontAwesomeSolid.INFO_CIRCLE, 16);
        infoIcon.getStyleClass().add("nino-info-icon");

        Label titleLabel = new Label(NINO_VS_UTR_TITLE);
        titleLabel.getStyleClass().add("nino-info-title");

        titleRow.getChildren().addAll(infoIcon, titleLabel);

        // Description text
        Label descLabel = new Label(NINO_VS_UTR_DESCRIPTION);
        descLabel.getStyleClass().add("nino-info-description");
        descLabel.setWrapText(true);

        // NINO bullet point
        HBox ninoBullet = createBulletPoint(
            "NINO: 9-character code like 'QQ 12 34 56 A'",
            "Found on payslips, P60, or tax letters"
        );

        // UTR bullet point
        HBox utrBullet = createBulletPoint(
            "UTR: 10-digit number for Self Assessment",
            "You'll need this later for submissions"
        );

        VBox bulletPoints = new VBox(8);
        bulletPoints.getChildren().addAll(ninoBullet, utrBullet);

        infoBox.getChildren().addAll(titleRow, descLabel, bulletPoints);
        return infoBox;
    }

    /**
     * Creates a bullet point with main text and subtext.
     */
    private HBox createBulletPoint(String mainText, String subText) {
        HBox bulletRow = new HBox(8);
        bulletRow.setAlignment(Pos.TOP_LEFT);

        Label bullet = new Label("\u2022"); // Bullet character
        bullet.getStyleClass().add("nino-bullet");

        VBox textBox = new VBox(2);
        Label mainLabel = new Label(mainText);
        mainLabel.getStyleClass().add("nino-bullet-main");
        mainLabel.setWrapText(true);

        Label subLabel = new Label(subText);
        subLabel.getStyleClass().add("nino-bullet-sub");
        subLabel.setWrapText(true);

        textBox.getChildren().addAll(mainLabel, subLabel);
        bulletRow.getChildren().addAll(bullet, textBox);

        return bulletRow;
    }

    /**
     * Loads Step 3: Government Gateway Explainer content.
     * SE-12-003: Government Gateway Explainer Screen
     *
     * <p>Displays informational content about Government Gateway:</p>
     * <ul>
     *   <li>What Government Gateway is</li>
     *   <li>Security reassurance (app never sees password)</li>
     *   <li>What happens when user clicks Next</li>
     * </ul>
     */
    private void loadStep3Content() {
        if (contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();
        contentArea.setSpacing(16);
        contentArea.setPadding(new Insets(20));

        // Title
        Label title = new Label(STEP3_TITLE);
        title.getStyleClass().add("hmrc-wizard-section-title");
        title.setWrapText(true);

        // Info card 1: What is Government Gateway (bullet points)
        InfoCard gatewayCard = new InfoCard(
            "SHIELD_ALT",
            null,
            GOV_GATEWAY_INTRO,
            java.util.Arrays.asList(GOV_GATEWAY_USES)
        );

        // Info card 2: Security reassurance (description)
        InfoCard securityCard = new InfoCard(
            "LOCK",
            SECURITY_TITLE,
            SECURITY_MESSAGE
        );

        // Info card 3: What happens next (numbered steps)
        InfoCard stepsCard = new InfoCard(
            "INFO_CIRCLE",
            WHAT_HAPPENS_TITLE,
            java.util.Arrays.asList(WHAT_HAPPENS_STEPS),
            true
        );

        contentArea.getChildren().addAll(title, gatewayCard, securityCard, stepsCard);

        // Step 3 is informational - user can always proceed
        viewModel.setCanProceed(true);
    }

    /**
     * Loads Step 4: OAuth Connection content.
     * SE-12-004: OAuth Connection with Progress
     */
    private void loadStep4Content() {
        if (contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();
        contentArea.setSpacing(20);
        contentArea.setPadding(new Insets(20));

        // Create all containers
        connectContainer = createConnectContainer();
        progressContainer4 = createProgressContainer();
        successContainer = createSuccessContainer();
        errorContainer = createErrorContainer();

        // Initially show connect container
        progressContainer4.setVisible(false);
        progressContainer4.setManaged(false);
        successContainer.setVisible(false);
        successContainer.setManaged(false);
        errorContainer.setVisible(false);
        errorContainer.setManaged(false);

        contentArea.getChildren().addAll(
            connectContainer,
            progressContainer4,
            successContainer,
            errorContainer
        );

        // Update canProceed - user must complete OAuth before proceeding
        viewModel.setCanProceed(false);
    }

    /**
     * Creates the initial connect container with privacy reminder.
     * State 1: READY
     */
    private VBox createConnectContainer() {
        VBox container = new VBox(16);
        container.setAlignment(Pos.CENTER);
        container.getStyleClass().add("oauth-connect-container");

        // Title
        Label title = new Label(STEP4_TITLE);
        title.getStyleClass().add("hmrc-wizard-section-title");

        // Privacy reminder (FIN-003)
        HBox privacyBox = new HBox(12);
        privacyBox.setAlignment(Pos.CENTER_LEFT);
        privacyBox.getStyleClass().add("oauth-privacy-reminder");
        privacyBox.setPadding(new Insets(12));

        FontIcon lockIcon = FontIcon.of(FontAwesomeSolid.LOCK, 16);
        lockIcon.getStyleClass().add("oauth-privacy-icon");

        Label privacyLabel = new Label(PRIVACY_REMINDER);
        privacyLabel.getStyleClass().add("oauth-privacy-text");
        privacyLabel.setWrapText(true);
        HBox.setHgrow(privacyLabel, Priority.ALWAYS);

        privacyBox.getChildren().addAll(lockIcon, privacyLabel);

        // Connect button
        Button connectBtn = new Button("Connect to HMRC");
        connectBtn.getStyleClass().add("hmrc-wizard-btn-primary");
        connectBtn.setOnAction(e -> startOAuthConnection());

        // Description
        Label descLabel = new Label(
            "Click the button above to open your browser and sign in to HMRC " +
            "with your Government Gateway credentials."
        );
        descLabel.getStyleClass().add("oauth-description");
        descLabel.setWrapText(true);

        container.getChildren().addAll(title, privacyBox, connectBtn, descLabel);
        return container;
    }

    /**
     * Creates the progress container shown during connection.
     * State 2: CONNECTING - Shows clear instructions to complete in browser
     */
    private VBox createProgressContainer() {
        VBox container = new VBox(16);
        container.setAlignment(Pos.CENTER);
        container.getStyleClass().add("oauth-progress-container");
        container.setPadding(new Insets(20));

        // Browser icon with spinner overlay
        StackPane iconStack = new StackPane();
        iconStack.setAlignment(Pos.CENTER);

        FontIcon browserIcon = FontIcon.of(FontAwesomeSolid.EXTERNAL_LINK_ALT, 48);
        browserIcon.getStyleClass().add("oauth-browser-icon");

        spinner = new ProgressIndicator();
        spinner.getStyleClass().add("oauth-spinner-small");
        spinner.setMaxSize(24, 24);
        StackPane.setAlignment(spinner, Pos.BOTTOM_RIGHT);

        iconStack.getChildren().addAll(browserIcon, spinner);

        // Title - clear call to action
        Label titleLabel = new Label(BROWSER_WAITING_TITLE);
        titleLabel.getStyleClass().add("oauth-browser-title");
        titleLabel.setWrapText(true);

        // Description
        Label descLabel = new Label(BROWSER_WAITING_DESC);
        descLabel.getStyleClass().add("oauth-browser-desc");
        descLabel.setWrapText(true);
        descLabel.setTextAlignment(TextAlignment.CENTER);

        // Status label (updates during connection)
        statusLabel = new Label(CONNECTING_TO_HMRC);
        statusLabel.getStyleClass().add("oauth-status-text");

        // Hint about auto-update
        Label hintLabel = new Label(BROWSER_WAITING_HINT);
        hintLabel.getStyleClass().add("oauth-browser-hint");
        hintLabel.setWrapText(true);
        hintLabel.setTextAlignment(TextAlignment.CENTER);

        // Button container
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);

        // Re-open browser button (in case user closed it)
        Button reopenBtn = new Button("Open Browser Again");
        reopenBtn.getStyleClass().add("hmrc-wizard-btn-secondary");
        reopenBtn.setOnAction(e -> reopenBrowser());

        // Cancel button
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("hmrc-wizard-btn-secondary");
        cancelBtn.setOnAction(e -> cancelOAuthConnection());

        buttonBox.getChildren().addAll(reopenBtn, cancelBtn);

        container.getChildren().addAll(iconStack, titleLabel, descLabel, statusLabel, hintLabel, buttonBox);
        return container;
    }

    /**
     * Re-opens the browser for OAuth if user closed it.
     */
    private void reopenBrowser() {
        if (oAuthHandler != null) {
            boolean opened = oAuthHandler.reopenBrowser();
            if (opened) {
                LOG.info("Browser re-opened for OAuth");
            } else {
                LOG.warning("Could not re-open browser - no authorization URL available");
            }
        }
    }

    /**
     * Creates the success container shown after successful connection.
     * State 3: SUCCESS
     */
    private VBox createSuccessContainer() {
        VBox container = new VBox(16);
        container.setAlignment(Pos.CENTER);
        container.getStyleClass().add("oauth-success-container");

        // Success icon
        StackPane iconWrapper = new StackPane();
        iconWrapper.getStyleClass().add("oauth-success-icon-wrapper");
        iconWrapper.setMinSize(64, 64);
        iconWrapper.setMaxSize(64, 64);

        FontIcon checkIcon = FontIcon.of(FontAwesomeSolid.CHECK, 32);
        checkIcon.getStyleClass().add("oauth-success-icon");
        iconWrapper.getChildren().add(checkIcon);

        // Success message
        Label successLabel = new Label(OAUTH_SUCCESS_MESSAGE);
        successLabel.getStyleClass().add("oauth-success-text");

        // Continue button
        Button continueBtn = new Button("Continue");
        continueBtn.getStyleClass().add("hmrc-wizard-btn-primary");
        continueBtn.setOnAction(e -> {
            viewModel.setCanProceed(true);
            viewModel.goNext();
            updateContentForStep();
        });

        container.getChildren().addAll(iconWrapper, successLabel, continueBtn);
        return container;
    }

    /**
     * Creates the error container shown after connection failure.
     * State 4: ERROR or State 5: TIMEOUT
     */
    private VBox createErrorContainer() {
        VBox container = new VBox(16);
        container.setAlignment(Pos.CENTER);
        container.getStyleClass().add("oauth-error-container");

        // Error icon
        StackPane iconWrapper = new StackPane();
        iconWrapper.getStyleClass().add("oauth-error-icon-wrapper");
        iconWrapper.setMinSize(64, 64);
        iconWrapper.setMaxSize(64, 64);

        FontIcon errorIcon = FontIcon.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE, 32);
        errorIcon.getStyleClass().add("oauth-error-icon");
        iconWrapper.getChildren().add(errorIcon);

        // Error message (will be updated dynamically)
        Label errorLabel = new Label();
        errorLabel.setId("oauth-error-label");
        errorLabel.getStyleClass().add("oauth-error-text");
        errorLabel.setWrapText(true);

        // Error guidance (will be updated dynamically)
        Label guidanceLabel = new Label();
        guidanceLabel.setId("oauth-guidance-label");
        guidanceLabel.getStyleClass().add("oauth-guidance-text");
        guidanceLabel.setWrapText(true);

        // Button container
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);

        Button tryAgainBtn = new Button("Try Again");
        tryAgainBtn.getStyleClass().add("hmrc-wizard-btn-primary");
        tryAgainBtn.setOnAction(e -> resetToConnectState());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("hmrc-wizard-btn-secondary");
        cancelBtn.setOnAction(e -> handleCancel());

        buttonBox.getChildren().addAll(tryAgainBtn, cancelBtn);

        container.getChildren().addAll(iconWrapper, errorLabel, guidanceLabel, buttonBox);
        return container;
    }

    /**
     * Starts the OAuth connection flow.
     */
    private void startOAuthConnection() {
        if (oAuthService == null) {
            LOG.warning("OAuth service not configured - cannot start connection");
            showError("CONFIGURATION_ERROR", "OAuth service is not configured. Please restart the application.");
            return;
        }

        LOG.info("Starting OAuth connection flow");
        viewModel.setConnecting(true);
        viewModel.resetConnectionState();

        // Show progress container
        showContainer(progressContainer4);

        // Create handler
        oAuthHandler = new OAuthConnectionHandler(
            oAuthService,
            progressRepository,
            this::handleOAuthStatus,
            this::handleOAuthResult,
            Clock.systemDefaultZone(),
            OAUTH_TIMEOUT
        );

        // Start connection
        oAuthHandler.startConnection();
    }

    /**
     * Cancels the OAuth connection flow.
     */
    private void cancelOAuthConnection() {
        LOG.info("Cancelling OAuth connection");
        if (oAuthHandler != null) {
            oAuthHandler.cancel();
        }
    }

    /**
     * Handles OAuth status updates from the handler.
     */
    private void handleOAuthStatus(ConnectionStatus status) {
        Platform.runLater(() -> {
            viewModel.setConnectionStatus(status);

            if (statusLabel != null) {
                statusLabel.setText(status.getDisplayMessage());
            }

            LOG.fine("OAuth status: " + status);
        });
    }

    /**
     * Handles the final OAuth result.
     */
    private void handleOAuthResult(OAuthConnectionHandler.OAuthResult result) {
        Platform.runLater(() -> {
            viewModel.handleOAuthResult(result);

            if (result.success()) {
                showSuccess();
            } else {
                showError(result.errorCode(), result.errorMessage());
            }
        });
    }

    /**
     * Shows the success state.
     */
    private void showSuccess() {
        LOG.info("OAuth connection successful");
        showContainer(successContainer);
    }

    /**
     * Shows the error state with the given error details.
     */
    private void showError(String errorCode, String errorMessage) {
        LOG.warning("OAuth connection failed: " + errorCode + " - " + errorMessage);

        // Update error labels
        if (errorContainer != null) {
            Label errorLabel = (Label) errorContainer.lookup("#oauth-error-label");
            Label guidanceLabel = (Label) errorContainer.lookup("#oauth-guidance-label");

            if (errorLabel != null) {
                errorLabel.setText(errorMessage != null ? errorMessage : "Connection failed");
            }

            if (guidanceLabel != null && errorGuidance != null) {
                String guidance = errorGuidance.getGuidanceForErrorCode(errorCode);
                guidanceLabel.setText(guidance);
                guidanceLabel.setVisible(!guidance.isEmpty());
                guidanceLabel.setManaged(!guidance.isEmpty());
            }
        }

        showContainer(errorContainer);
    }

    /**
     * Resets to the initial connect state for retry.
     */
    private void resetToConnectState() {
        viewModel.resetConnectionState();
        showContainer(connectContainer);
    }

    /**
     * Shows only the specified container, hiding all others.
     */
    private void showContainer(VBox toShow) {
        // Hide all
        if (connectContainer != null) {
            connectContainer.setVisible(false);
            connectContainer.setManaged(false);
        }
        if (progressContainer4 != null) {
            progressContainer4.setVisible(false);
            progressContainer4.setManaged(false);
        }
        if (successContainer != null) {
            successContainer.setVisible(false);
            successContainer.setManaged(false);
        }
        if (errorContainer != null) {
            errorContainer.setVisible(false);
            errorContainer.setManaged(false);
        }

        // Show specified
        if (toShow != null) {
            toShow.setVisible(true);
            toShow.setManaged(true);
        }
    }

    /**
     * Loads Step 5: Confirmation and Next Steps content.
     * SE-12-005: Confirmation and Next Steps Screen
     */
    private void loadStep5Content() {
        if (contentArea == null) return;

        contentArea.getChildren().clear();
        contentArea.setSpacing(20);
        contentArea.setPadding(new Insets(20));
        contentArea.setAlignment(Pos.TOP_CENTER);

        // Auto-save NINO to Settings (bidirectional sync)
        String wizardNino = viewModel.getNino();
        if (wizardNino != null && !wizardNino.isEmpty()) {
            SqliteDataStore.getInstance().saveNino(wizardNino);
            LOG.info("NINO saved to Settings from wizard");
        }

        // Success card with large checkmark
        VBox successCard = createStep5SuccessCard();

        // What you can do now
        VBox nextStepsBox = createStep5NextStepsBox();

        // Retention reminder (FIN-002)
        InfoCard retentionCard = new InfoCard(
            "INFO_CIRCLE",
            RETENTION_TITLE,
            RETENTION_MESSAGE
        );
        retentionCard.getStyleClass().add("retention-reminder");

        contentArea.getChildren().addAll(successCard, nextStepsBox, retentionCard);

        // Update header to green theme
        updateHeaderForSuccess();

        // Note: Button text is handled by binding to viewModel.getNextButtonText()
        // which returns "Done" for step 5

        // User can proceed (close dialog)
        viewModel.setCanProceed(true);
    }

    /**
     * Creates the success card for Step 5.
     *
     * @return the success card VBox
     */
    private VBox createStep5SuccessCard() {
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("success-card");
        card.setPadding(new Insets(24));

        // Large checkmark icon
        FontIcon checkIcon = FontIcon.of(FontAwesomeSolid.CHECK_CIRCLE, 64);
        checkIcon.getStyleClass().add("success-icon-large");

        // Success title
        Label title = new Label(SUCCESS_MESSAGE);
        title.getStyleClass().add("success-title");

        // Success description
        Label desc = new Label(SUCCESS_DESCRIPTION);
        desc.getStyleClass().add("success-description");
        desc.setWrapText(true);
        desc.setTextAlignment(TextAlignment.CENTER);

        card.getChildren().addAll(checkIcon, title, desc);
        return card;
    }

    /**
     * Creates the next steps box for Step 5.
     *
     * @return the next steps VBox
     */
    private VBox createStep5NextStepsBox() {
        VBox box = new VBox(12);

        Label title = new Label(WHAT_YOU_CAN_DO);
        title.getStyleClass().add("next-steps-title");
        box.getChildren().add(title);

        for (String step : NEXT_STEPS) {
            HBox stepRow = new HBox(8);
            stepRow.setAlignment(Pos.CENTER_LEFT);

            FontIcon checkIcon = FontIcon.of(FontAwesomeSolid.CHECK, 14);
            checkIcon.getStyleClass().add("next-step-check");

            Label stepLabel = new Label(step);
            stepLabel.getStyleClass().add("next-step-text");

            stepRow.getChildren().addAll(checkIcon, stepLabel);
            box.getChildren().add(stepRow);
        }

        return box;
    }

    /**
     * Updates the header to green success theme for Step 5.
     */
    private void updateHeaderForSuccess() {
        // Change header gradient to green
        if (headerPane != null) {
            headerPane.getStyleClass().remove("hmrc-wizard-header");
            headerPane.getStyleClass().add("hmrc-wizard-header-success");
        }

        // Update header icon to checkmark
        if (headerIcon != null) {
            headerIcon.setIconCode(FontAwesomeSolid.CHECK_CIRCLE);
        }

        // Update title
        if (headerTitle != null) {
            headerTitle.setText(STEP5_TITLE);
        }
    }

    /**
     * Loads placeholder content for unimplemented steps.
     */
    private void loadPlaceholderContent(int step) {
        if (contentArea == null) return;

        contentArea.getChildren().clear();
        Label placeholder = new Label("Step " + step + " - Coming soon");
        placeholder.getStyleClass().add("hmrc-wizard-placeholder");
        contentArea.getChildren().add(placeholder);
    }

    /**
     * Opens an external link in the system browser.
     * Runs on a background thread to avoid blocking the JavaFX Application Thread.
     *
     * @param url the URL to open
     */
    void openExternalLink(String url) {
        // Run on background thread to avoid blocking the UI
        Thread browserThread = new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                    LOG.fine("Opened external link: " + url);
                } else {
                    LOG.warning("Desktop browse not supported, cannot open: " + url);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to open external link: " + url, e);
            }
        }, "browser-launcher");
        browserThread.setDaemon(true);
        browserThread.start();
    }

    /**
     * Closes the dialog.
     */
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    // === Factory Method ===

    /**
     * Creates and shows the wizard dialog.
     *
     * @param ownerStage the owner stage for the dialog
     * @return the view model for checking result
     */
    public static HmrcConnectionWizardViewModel showWizard(Stage ownerStage) {
        try {
            HmrcConnectionWizardController controller = new HmrcConnectionWizardController();

            // Initialize OAuth service from factory
            controller.setOAuthService(uk.selfemploy.ui.service.OAuthServiceFactory.getOAuthService());

            // Initialize progress repository for state persistence
            controller.setProgressRepository(new uk.selfemploy.ui.service.SqliteWizardProgressRepository());

            // Build the dialog content programmatically
            VBox container = controller.buildDialogContent();

            // Create stage
            Stage stage = new Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            if (ownerStage != null) {
                stage.initOwner(ownerStage);
            }
            stage.setTitle(WIZARD_TITLE);
            stage.setResizable(false);

            controller.setDialogStage(stage);

            // Apply styling
            DialogStyler.applyRoundedClip(container, DialogStyler.CORNER_RADIUS);
            StackPane wrapper = DialogStyler.createShadowWrapper(container);
            DialogStyler.setupStyledDialog(stage, wrapper, "/css/hmrc-wizard.css");
            DialogStyler.centerOnOwner(stage);

            // Show dialog
            stage.showAndWait();

            return controller.viewModel;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to show HMRC connection wizard", e);
            return null;
        }
    }

    /**
     * Builds the dialog content programmatically.
     */
    private VBox buildDialogContent() {
        VBox container = new VBox();
        container.getStyleClass().add("hmrc-wizard-container");
        container.setMinWidth(DIALOG_WIDTH);
        container.setMaxWidth(DIALOG_WIDTH);
        container.setMinHeight(DIALOG_MIN_HEIGHT);
        container.setMaxHeight(DIALOG_MAX_HEIGHT);

        // Store reference
        this.wizardContainer = container;

        // Header
        HBox header = buildHeader();
        this.headerBox = header;

        // Progress indicator
        HBox progress = new HBox();
        progress.getStyleClass().add("wizard-progress-container");
        progress.setAlignment(Pos.CENTER);
        progress.setPadding(new Insets(16, 20, 16, 20));
        this.progressContainer = progress;

        // Content area (inside ScrollPane for overflow handling)
        VBox content = new VBox();
        content.getStyleClass().add("hmrc-wizard-content");
        this.contentArea = content;

        // Wrap content in ScrollPane to handle overflow
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("hmrc-wizard-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Footer
        HBox footer = buildFooter();
        this.footerBox = footer;

        container.getChildren().addAll(header, progress, scrollPane, footer);

        // Initialize components
        setupProgressIndicator();
        setupKeyboardHandlers();
        bindToViewModel();
        loadStep1Content();

        return container;
    }

    /**
     * Builds the dialog header.
     */
    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("hmrc-wizard-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));

        // Store reference for Step 5 header update
        this.headerPane = header;

        // Icon in white circle
        StackPane iconWrapper = new StackPane();
        iconWrapper.getStyleClass().add("hmrc-wizard-header-icon-wrapper");
        iconWrapper.setMinSize(40, 40);
        iconWrapper.setMaxSize(40, 40);

        FontIcon icon = FontIcon.of(FontAwesomeSolid.CLIPBOARD_CHECK, 20);
        icon.getStyleClass().add("hmrc-wizard-header-icon");
        iconWrapper.getChildren().add(icon);

        // Store reference for Step 5 header update
        this.headerIcon = icon;

        // Title
        Label title = new Label(WIZARD_TITLE);
        title.getStyleClass().add("hmrc-wizard-header-title");

        // Store reference for Step 5 header update
        this.headerTitle = title;

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Close button
        Button closeBtn = new Button("\u2715");
        closeBtn.getStyleClass().add("hmrc-wizard-close-btn");
        closeBtn.setOnAction(e -> handleCancel());

        header.getChildren().addAll(iconWrapper, title, spacer, closeBtn);
        return header;
    }

    /**
     * Builds the dialog footer with buttons.
     */
    private HBox buildFooter() {
        HBox footer = new HBox(12);
        footer.getStyleClass().add("hmrc-wizard-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 20, 20, 20));

        // Step label
        Label stepLbl = new Label(viewModel.getStepLabel());
        stepLbl.getStyleClass().add("hmrc-wizard-step-label");
        this.stepLabel = stepLbl;

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Back button (hidden on Step 1 and Step 5)
        Button back = new Button("Back");
        back.getStyleClass().add("hmrc-wizard-btn-secondary");
        back.setOnAction(e -> handleBack());
        back.setVisible(false);  // Hidden initially on Step 1
        back.setManaged(false);
        this.backButton = back;

        // Cancel button
        Button cancel = new Button(viewModel.getCancelButtonText());
        cancel.getStyleClass().add("hmrc-wizard-btn-secondary");
        cancel.setOnAction(e -> handleCancel());
        this.cancelButton = cancel;

        // Next button
        Button next = new Button(viewModel.getNextButtonText());
        next.getStyleClass().add("hmrc-wizard-btn-primary");
        next.setOnAction(e -> handleNext());
        this.nextButton = next;

        footer.getChildren().addAll(stepLbl, spacer, back, cancel, next);
        return footer;
    }
}
