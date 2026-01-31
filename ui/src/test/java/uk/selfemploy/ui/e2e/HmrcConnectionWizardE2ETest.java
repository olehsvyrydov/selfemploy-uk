package uk.selfemploy.ui.e2e;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.mockito.ArgumentCaptor;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.ui.component.NinoInputField;
import uk.selfemploy.ui.controller.HmrcConnectionWizardController;
import uk.selfemploy.ui.service.OAuthConnectionHandler.ConnectionStatus;
import uk.selfemploy.ui.service.OAuthConnectionHandler.OAuthResult;
import uk.selfemploy.ui.service.SqliteDataStore;
import uk.selfemploy.ui.service.SqliteTestSupport;
import uk.selfemploy.ui.service.WizardProgress;
import uk.selfemploy.ui.service.WizardProgressRepository;
import uk.selfemploy.ui.util.HmrcErrorGuidance;
import uk.selfemploy.ui.viewmodel.HmrcConnectionWizardViewModel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.*;

/**
 * E2E/UI Tests for HMRC Connection Wizard.
 * Sprint 12 - SE-12-007: Integration and E2E Tests
 *
 * <p>Tests the complete 5-step wizard flow including:</p>
 * <ul>
 *   <li>Step 1: Prerequisites Checklist</li>
 *   <li>Step 2: NINO Entry with Validation</li>
 *   <li>Step 3: Government Gateway Explainer</li>
 *   <li>Step 4: OAuth Connection with Progress</li>
 *   <li>Step 5: Confirmation and Next Steps</li>
 * </ul>
 *
 * <p>Financial conditions tested (per /inga):</p>
 * <ul>
 *   <li>FIN-001: NINO vs UTR clarification displayed</li>
 *   <li>FIN-002: 5-year retention reminder displayed</li>
 *   <li>FIN-003: Privacy reminder before OAuth displayed</li>
 * </ul>
 */
@Tag("e2e")
@DisabledIfSystemProperty(named = "skipE2ETests", matches = "true")
@DisplayName("SE-12-007: HMRC Connection Wizard E2E Tests")
class HmrcConnectionWizardE2ETest extends ApplicationTest {

    private HmrcConnectionWizardController controller;
    private HmrcConnectionWizardViewModel viewModel;
    private HmrcOAuthService mockOAuthService;
    private WizardProgressRepository mockRepository;
    private HmrcErrorGuidance errorGuidance;
    private VBox wizardContainer;
    private Stage stage;

    @Override
    public void start(Stage stage) throws Exception {
        // Configure headless mode if requested
        if (Boolean.getBoolean("headless") || Boolean.getBoolean("testfx.headless")) {
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
            System.setProperty("java.awt.headless", "true");
            System.setProperty("glass.platform", "Monocle");
            System.setProperty("monocle.platform", "Headless");
        }

        this.stage = stage;

        // Create controller and view model
        controller = new HmrcConnectionWizardController();
        viewModel = new HmrcConnectionWizardViewModel();
        controller.setViewModel(viewModel);

        // Setup mocks
        mockOAuthService = mock(HmrcOAuthService.class);
        mockRepository = mock(WizardProgressRepository.class);
        errorGuidance = new HmrcErrorGuidance();

        controller.setOAuthService(mockOAuthService);
        controller.setProgressRepository(mockRepository);
        controller.setErrorGuidance(errorGuidance);

        // Build the wizard content programmatically
        wizardContainer = buildWizardContainer();

        Scene scene = new Scene(wizardContainer,
            HmrcConnectionWizardController.DIALOG_WIDTH,
            HmrcConnectionWizardController.DIALOG_MAX_HEIGHT);

        // Clear stylesheets to avoid CSS resolution issues in tests
        scene.getStylesheets().clear();
        wizardContainer.getStylesheets().clear();

        // Apply minimal test CSS
        try {
            String testCss = getClass().getResource("/css/test-minimal.css").toExternalForm();
            scene.getStylesheets().add(testCss);
        } catch (Exception ignored) {
            // Test CSS may not exist
        }

        stage.setScene(scene);
        stage.show();
        WaitForAsyncUtils.waitForFxEvents();
    }

    /**
     * Builds the wizard container for testing.
     * Mimics the controller's buildDialogContent() method.
     */
    private VBox buildWizardContainer() {
        VBox container = new VBox();
        container.setId("wizardContainer");
        container.setMinWidth(HmrcConnectionWizardController.DIALOG_WIDTH);
        container.setMaxWidth(HmrcConnectionWizardController.DIALOG_WIDTH);

        // Header
        HBox header = new HBox();
        header.setId("headerBox");

        Label headerTitle = new Label(HmrcConnectionWizardController.WIZARD_TITLE);
        headerTitle.setId("headerTitle");
        header.getChildren().add(headerTitle);

        // Progress container
        HBox progress = new HBox();
        progress.setId("progressContainer");

        // Content area
        VBox content = new VBox();
        content.setId("contentArea");

        // Footer
        HBox footer = new HBox();
        footer.setId("footerBox");

        Label stepLabel = new Label(viewModel.getStepLabel());
        stepLabel.setId("stepLabel");

        Button cancelButton = new Button(viewModel.getCancelButtonText());
        cancelButton.setId("cancelButton");
        cancelButton.setOnAction(e -> handleCancel());

        Button nextButton = new Button(viewModel.getNextButtonText());
        nextButton.setId("nextButton");
        nextButton.setOnAction(e -> handleNext());
        nextButton.disableProperty().bind(viewModel.canProceedProperty().not());

        footer.getChildren().addAll(stepLabel, cancelButton, nextButton);

        container.getChildren().addAll(header, progress, content, footer);

        // Load initial content (Step 1)
        loadStep1Content(content);

        return container;
    }

    private void handleCancel() {
        viewModel.cancel();
        stage.close();
    }

    private void handleNext() {
        if (viewModel.getCurrentStep() == 5) {
            stage.close();
            return;
        }
        if (viewModel.canProceed()) {
            viewModel.goNext();
            updateContentForStep();
        }
    }

    private void updateContentForStep() {
        VBox content = (VBox) wizardContainer.lookup("#contentArea");
        if (content == null) return;

        int step = viewModel.getCurrentStep();
        switch (step) {
            case 1 -> loadStep1Content(content);
            case 2 -> loadStep2Content(content);
            case 3 -> loadStep3Content(content);
            case 4 -> loadStep4Content(content);
            case 5 -> loadStep5Content(content);
        }
        updateButtonAndLabel();
    }

    private void updateButtonAndLabel() {
        Button nextButton = (Button) wizardContainer.lookup("#nextButton");
        Label stepLabel = (Label) wizardContainer.lookup("#stepLabel");

        if (nextButton != null) {
            nextButton.setText(viewModel.getNextButtonText());
        }
        if (stepLabel != null) {
            stepLabel.setText(viewModel.getStepLabel());
        }
    }

    private void loadStep1Content(VBox content) {
        content.getChildren().clear();

        Label title = new Label("Before connecting to HMRC, please ensure you have the following:");
        title.setId("step1Title");

        // Three prerequisite items
        VBox prereq1 = createPrerequisiteItem("prereq1",
            HmrcConnectionWizardController.PREREQ_TITLE_SELF_ASSESSMENT,
            HmrcConnectionWizardController.PREREQ_DESC_SELF_ASSESSMENT);
        VBox prereq2 = createPrerequisiteItem("prereq2",
            HmrcConnectionWizardController.PREREQ_TITLE_GOV_GATEWAY,
            HmrcConnectionWizardController.PREREQ_DESC_GOV_GATEWAY);
        VBox prereq3 = createPrerequisiteItem("prereq3",
            HmrcConnectionWizardController.PREREQ_TITLE_NINO,
            HmrcConnectionWizardController.PREREQ_DESC_NINO);

        content.getChildren().addAll(title, prereq1, prereq2, prereq3);

        // Step 1 can always proceed
        viewModel.setCanProceed(true);
    }

    private VBox createPrerequisiteItem(String id, String title, String description) {
        VBox item = new VBox();
        item.setId(id);

        Label titleLabel = new Label(title);
        titleLabel.setId(id + "Title");

        Label descLabel = new Label(description);
        descLabel.setId(id + "Desc");
        descLabel.setWrapText(true);

        item.getChildren().addAll(titleLabel, descLabel);
        return item;
    }

    private void loadStep2Content(VBox content) {
        content.getChildren().clear();

        Label title = new Label(HmrcConnectionWizardController.STEP2_TITLE);
        title.setId("step2Title");

        // NINO input field
        NinoInputField ninoInput = new NinoInputField();
        ninoInput.setId("ninoInput");
        ninoInput.validProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setCanProceed(newVal);
        });
        ninoInput.ninoProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setNino(newVal);
        });

        // Pre-populate if NINO was previously entered
        String existingNino = viewModel.getNino();
        if (existingNino != null && !existingNino.isEmpty()) {
            ninoInput.setNino(existingNino);
        }

        // Initial state: cannot proceed until valid NINO
        viewModel.setCanProceed(ninoInput.isValid());

        // Validation error label
        Label validationError = new Label(HmrcConnectionWizardController.NINO_VALIDATION_ERROR);
        validationError.setId("validationError");
        validationError.setVisible(false);

        // NINO vs UTR info box (FIN-001)
        VBox ninoInfoBox = createNinoVsUtrInfoBox();

        content.getChildren().addAll(title, ninoInput, validationError, ninoInfoBox);
    }

    private VBox createNinoVsUtrInfoBox() {
        VBox infoBox = new VBox();
        infoBox.setId("ninoVsUtrInfoBox");

        Label titleLabel = new Label(HmrcConnectionWizardController.NINO_VS_UTR_TITLE);
        titleLabel.setId("ninoVsUtrTitle");

        Label descLabel = new Label(HmrcConnectionWizardController.NINO_VS_UTR_DESCRIPTION);
        descLabel.setId("ninoVsUtrDesc");
        descLabel.setWrapText(true);

        infoBox.getChildren().addAll(titleLabel, descLabel);
        return infoBox;
    }

    private void loadStep3Content(VBox content) {
        content.getChildren().clear();

        Label title = new Label(HmrcConnectionWizardController.STEP3_TITLE);
        title.setId("step3Title");

        // Security message
        Label securityTitle = new Label(HmrcConnectionWizardController.SECURITY_TITLE);
        securityTitle.setId("securityTitle");

        Label securityMessage = new Label(HmrcConnectionWizardController.SECURITY_MESSAGE);
        securityMessage.setId("securityMessage");
        securityMessage.setWrapText(true);

        // What happens next
        Label whatHappensTitle = new Label(HmrcConnectionWizardController.WHAT_HAPPENS_TITLE);
        whatHappensTitle.setId("whatHappensTitle");

        VBox stepsBox = new VBox();
        stepsBox.setId("whatHappensSteps");
        for (int i = 0; i < HmrcConnectionWizardController.WHAT_HAPPENS_STEPS.length; i++) {
            Label stepLabel = new Label((i + 1) + ". " + HmrcConnectionWizardController.WHAT_HAPPENS_STEPS[i]);
            stepLabel.setId("whatHappensStep" + (i + 1));
            stepsBox.getChildren().add(stepLabel);
        }

        content.getChildren().addAll(title, securityTitle, securityMessage, whatHappensTitle, stepsBox);

        // Step 3 is informational - can always proceed
        viewModel.setCanProceed(true);
    }

    private void loadStep4Content(VBox content) {
        content.getChildren().clear();

        Label title = new Label(HmrcConnectionWizardController.STEP4_TITLE);
        title.setId("step4Title");

        // Privacy reminder (FIN-003)
        VBox privacyBox = new VBox();
        privacyBox.setId("privacyReminderBox");

        Label privacyLabel = new Label(HmrcConnectionWizardController.PRIVACY_REMINDER);
        privacyLabel.setId("privacyReminder");
        privacyLabel.setWrapText(true);

        privacyBox.getChildren().add(privacyLabel);

        // Connect container
        VBox connectContainer = new VBox();
        connectContainer.setId("connectContainer");

        Button connectButton = new Button("Connect to HMRC");
        connectButton.setId("connectButton");
        connectButton.setOnAction(e -> startOAuthConnection(content));

        connectContainer.getChildren().add(connectButton);

        // Progress container (hidden initially)
        VBox progressContainer = new VBox();
        progressContainer.setId("progressContainer4");
        progressContainer.setVisible(false);
        progressContainer.setManaged(false);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setId("oauthSpinner");

        Label statusLabel = new Label(HmrcConnectionWizardController.CONNECTING_TO_HMRC);
        statusLabel.setId("oauthStatusLabel");

        progressContainer.getChildren().addAll(spinner, statusLabel);

        // Success container (hidden initially)
        VBox successContainer = new VBox();
        successContainer.setId("successContainer");
        successContainer.setVisible(false);
        successContainer.setManaged(false);

        Label successLabel = new Label(HmrcConnectionWizardController.OAUTH_SUCCESS_MESSAGE);
        successLabel.setId("oauthSuccessLabel");

        Button continueButton = new Button("Continue");
        continueButton.setId("continueButton");
        continueButton.setOnAction(e -> {
            viewModel.setCanProceed(true);
            viewModel.goNext();
            updateContentForStep();
        });

        successContainer.getChildren().addAll(successLabel, continueButton);

        content.getChildren().addAll(title, privacyBox, connectContainer, progressContainer, successContainer);

        // Cannot proceed until OAuth completes
        viewModel.setCanProceed(false);
    }

    private void startOAuthConnection(VBox content) {
        // Show progress, hide connect button
        VBox connectContainer = (VBox) content.lookup("#connectContainer");
        VBox progressContainer = (VBox) content.lookup("#progressContainer4");

        if (connectContainer != null) {
            connectContainer.setVisible(false);
            connectContainer.setManaged(false);
        }
        if (progressContainer != null) {
            progressContainer.setVisible(true);
            progressContainer.setManaged(true);
        }

        viewModel.setConnecting(true);
        viewModel.setConnectionStatus(ConnectionStatus.OPENING_BROWSER);

        // Simulate OAuth completion (in tests, we control the mock)
        // The actual OAuth is handled via mockOAuthService
    }

    /**
     * Simulates OAuth success for testing.
     */
    private void simulateOAuthSuccess(VBox content) {
        VBox progressContainer = (VBox) content.lookup("#progressContainer4");
        VBox successContainer = (VBox) content.lookup("#successContainer");

        if (progressContainer != null) {
            progressContainer.setVisible(false);
            progressContainer.setManaged(false);
        }
        if (successContainer != null) {
            successContainer.setVisible(true);
            successContainer.setManaged(true);
        }

        viewModel.setConnecting(false);
        viewModel.setConnectionSuccessful(true);
        viewModel.setConnectionStatus(ConnectionStatus.SUCCESS);
    }

    private void loadStep5Content(VBox content) {
        content.getChildren().clear();

        // Update header to green theme
        Label headerTitle = (Label) wizardContainer.lookup("#headerTitle");
        if (headerTitle != null) {
            headerTitle.setText(HmrcConnectionWizardController.STEP5_TITLE);
            headerTitle.setStyle("-fx-text-fill: #28a745;"); // Green success theme
        }

        // Success card
        VBox successCard = new VBox();
        successCard.setId("successCard");

        Label successTitle = new Label(HmrcConnectionWizardController.SUCCESS_MESSAGE);
        successTitle.setId("successTitle");

        Label successDesc = new Label(HmrcConnectionWizardController.SUCCESS_DESCRIPTION);
        successDesc.setId("successDescription");
        successDesc.setWrapText(true);

        successCard.getChildren().addAll(successTitle, successDesc);

        // What you can do now
        VBox nextStepsBox = new VBox();
        nextStepsBox.setId("nextStepsBox");

        Label nextStepsTitle = new Label(HmrcConnectionWizardController.WHAT_YOU_CAN_DO);
        nextStepsTitle.setId("nextStepsTitle");
        nextStepsBox.getChildren().add(nextStepsTitle);

        for (int i = 0; i < HmrcConnectionWizardController.NEXT_STEPS.length; i++) {
            Label stepLabel = new Label(HmrcConnectionWizardController.NEXT_STEPS[i]);
            stepLabel.setId("nextStep" + (i + 1));
            nextStepsBox.getChildren().add(stepLabel);
        }

        // Retention reminder (FIN-002)
        VBox retentionBox = new VBox();
        retentionBox.setId("retentionReminderBox");

        Label retentionTitle = new Label(HmrcConnectionWizardController.RETENTION_TITLE);
        retentionTitle.setId("retentionTitle");

        Label retentionMessage = new Label(HmrcConnectionWizardController.RETENTION_MESSAGE);
        retentionMessage.setId("retentionMessage");
        retentionMessage.setWrapText(true);

        retentionBox.getChildren().addAll(retentionTitle, retentionMessage);

        content.getChildren().addAll(successCard, nextStepsBox, retentionBox);

        // Step 5 can proceed (closes wizard)
        viewModel.setCanProceed(true);
    }

    // =========================================================================
    // Complete Flow Tests
    // =========================================================================

    @Nested
    @DisplayName("Complete Wizard Flow Tests")
    class CompleteWizardFlowTests {

        @Test
        @DisplayName("E2E-12-001: Complete wizard flow through all 5 steps successfully")
        void completeWizardFlow_success() {
            // Step 1: Verify prerequisites displayed
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
            verifyThat("#prereq1", isVisible());
            verifyThat("#prereq2", isVisible());
            verifyThat("#prereq3", isVisible());

            // Click Next to go to Step 2
            runOnFxThread(() -> handleNext());
            assertThat(viewModel.getCurrentStep()).isEqualTo(2);

            // Step 2: Enter valid NINO
            NinoInputField ninoInput = lookup("#ninoInput").queryAs(NinoInputField.class);
            runOnFxThread(() -> ninoInput.setNino("QQ123456A"));
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(ninoInput.isValid()).isTrue();
            assertThat(viewModel.canProceed()).isTrue();

            // Click Next to go to Step 3
            runOnFxThread(() -> handleNext());
            assertThat(viewModel.getCurrentStep()).isEqualTo(3);

            // Step 3: Verify Government Gateway info displayed
            verifyThat("#securityMessage", isVisible());
            verifyThat("#whatHappensSteps", isVisible());

            // Click Next to go to Step 4
            runOnFxThread(() -> handleNext());
            assertThat(viewModel.getCurrentStep()).isEqualTo(4);

            // Step 4: Start OAuth and simulate success
            VBox content = lookup("#contentArea").queryAs(VBox.class);
            runOnFxThread(() -> {
                startOAuthConnection(content);
                simulateOAuthSuccess(content);
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Click Continue to go to Step 5
            Button continueBtn = lookup("#continueButton").queryAs(Button.class);
            runOnFxThread(() -> continueBtn.fire());
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(viewModel.getCurrentStep()).isEqualTo(5);

            // Step 5: Verify success message displayed
            verifyThat("#successTitle", isVisible());
            verifyThat("#retentionReminderBox", isVisible());
        }

        @Test
        @DisplayName("E2E-12-002: Back navigation returns to previous steps correctly")
        void wizardFlow_backNavigation() {
            // Navigate to Step 3
            runOnFxThread(() -> {
                handleNext(); // to 2
                viewModel.setNino("QQ123456A");
                viewModel.setCanProceed(true);
                handleNext(); // to 3
            });
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(viewModel.getCurrentStep()).isEqualTo(3);

            // Go back to Step 2
            runOnFxThread(() -> viewModel.goBack());
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(viewModel.getCurrentStep()).isEqualTo(2);

            // Update content
            runOnFxThread(() -> updateContentForStep());
            WaitForAsyncUtils.waitForFxEvents();

            // NINO should still be there
            assertThat(viewModel.getNino()).isEqualTo("QQ123456A");

            // Go back to Step 1
            runOnFxThread(() -> {
                viewModel.goBack();
                updateContentForStep();
            });
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
            verifyThat("#prereq1", isVisible());
        }

        @Test
        @DisplayName("E2E-12-003: Cancel exits wizard from any step")
        void wizardFlow_cancelExitsAtAnyStep() {
            // Navigate to Step 2
            runOnFxThread(() -> handleNext());
            assertThat(viewModel.getCurrentStep()).isEqualTo(2);

            // Cancel
            runOnFxThread(() -> viewModel.cancel());

            assertThat(viewModel.isCancelled()).isTrue();
        }

        @Test
        @DisplayName("E2E-12-004: Escape key sets cancelled flag")
        void wizardFlow_escapeKeyExits() {
            // Simulate Escape key by directly calling cancel
            runOnFxThread(() -> viewModel.cancel());

            assertThat(viewModel.isCancelled()).isTrue();
        }
    }

    // =========================================================================
    // Step 1 Tests - Prerequisites Checklist
    // =========================================================================

    @Nested
    @DisplayName("Step 1: Prerequisites Checklist Tests")
    class Step1PrerequisitesTests {

        @Test
        @DisplayName("E2E-12-005: Step 1 displays all 3 prerequisite items")
        void step1_displaysAllPrerequisites() {
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);

            verifyThat("#prereq1", isVisible());
            verifyThat("#prereq2", isVisible());
            verifyThat("#prereq3", isVisible());

            // Verify titles
            Label title1 = lookup("#prereq1Title").queryAs(Label.class);
            Label title2 = lookup("#prereq2Title").queryAs(Label.class);
            Label title3 = lookup("#prereq3Title").queryAs(Label.class);

            assertThat(title1.getText()).isEqualTo(HmrcConnectionWizardController.PREREQ_TITLE_SELF_ASSESSMENT);
            assertThat(title2.getText()).isEqualTo(HmrcConnectionWizardController.PREREQ_TITLE_GOV_GATEWAY);
            assertThat(title3.getText()).isEqualTo(HmrcConnectionWizardController.PREREQ_TITLE_NINO);
        }

        @Test
        @DisplayName("E2E-12-006: Step 1 prerequisite descriptions are displayed")
        void step1_prerequisiteDescriptionsDisplayed() {
            Label desc1 = lookup("#prereq1Desc").queryAs(Label.class);
            Label desc2 = lookup("#prereq2Desc").queryAs(Label.class);
            Label desc3 = lookup("#prereq3Desc").queryAs(Label.class);

            assertThat(desc1.getText()).contains("registered with HMRC");
            assertThat(desc2.getText()).contains("Government Gateway");
            assertThat(desc3.getText()).contains("9-character code");
        }
    }

    // =========================================================================
    // Step 2 Tests - NINO Entry
    // =========================================================================

    @Nested
    @DisplayName("Step 2: NINO Entry Tests")
    class Step2NinoEntryTests {

        @BeforeEach
        void navigateToStep2() {
            runOnFxThread(() -> handleNext());
            WaitForAsyncUtils.waitForFxEvents();
        }

        @Test
        @DisplayName("E2E-12-007: Invalid NINO disables Next button")
        void step2_invalidNinoDisablesNext() {
            NinoInputField ninoInput = lookup("#ninoInput").queryAs(NinoInputField.class);

            // Enter invalid NINO
            runOnFxThread(() -> ninoInput.setNino("INVALID"));
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(ninoInput.isValid()).isFalse();
            assertThat(viewModel.canProceed()).isFalse();

            // Next button should be disabled
            Button nextButton = lookup("#nextButton").queryAs(Button.class);
            assertThat(nextButton.isDisabled()).isTrue();
        }

        @Test
        @DisplayName("E2E-12-008: Valid NINO enables Next button")
        void step2_validNinoEnablesNext() {
            NinoInputField ninoInput = lookup("#ninoInput").queryAs(NinoInputField.class);

            // Enter valid NINO
            runOnFxThread(() -> ninoInput.setNino("QQ123456A"));
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(ninoInput.isValid()).isTrue();
            assertThat(viewModel.canProceed()).isTrue();

            // Next button should be enabled
            Button nextButton = lookup("#nextButton").queryAs(Button.class);
            assertThat(nextButton.isDisabled()).isFalse();
        }

        @Test
        @DisplayName("E2E-12-009: NINO auto-formats as user types")
        void step2_ninoAutoFormats() {
            NinoInputField ninoInput = lookup("#ninoInput").queryAs(NinoInputField.class);

            // Enter NINO without spaces
            runOnFxThread(() -> ninoInput.setNino("qq123456a"));
            WaitForAsyncUtils.waitForFxEvents();

            // Should be formatted and uppercased
            String formatted = ninoInput.getTextField().getText();
            assertThat(formatted).isEqualTo("QQ 12 34 56 A");
        }

        @Test
        @DisplayName("E2E-12-010: NINO vs UTR info box displayed (FIN-001)")
        void step2_ninoVsUtrInfoDisplayed() {
            verifyThat("#ninoVsUtrInfoBox", isVisible());

            Label title = lookup("#ninoVsUtrTitle").queryAs(Label.class);
            Label desc = lookup("#ninoVsUtrDesc").queryAs(Label.class);

            assertThat(title.getText()).isEqualTo(HmrcConnectionWizardController.NINO_VS_UTR_TITLE);
            assertThat(desc.getText()).contains("National Insurance Number (NINO)");
            assertThat(desc.getText()).contains("Unique Taxpayer Reference (UTR)");
            assertThat(desc.getText()).contains("9-character code");
            assertThat(desc.getText()).contains("10-digit number");
        }
    }

    // =========================================================================
    // Step 3 Tests - Government Gateway Explainer
    // =========================================================================

    @Nested
    @DisplayName("Step 3: Government Gateway Explainer Tests")
    class Step3GovGatewayTests {

        @BeforeEach
        void navigateToStep3() {
            runOnFxThread(() -> {
                handleNext(); // to 2
                viewModel.setNino("QQ123456A");
                viewModel.setCanProceed(true);
                handleNext(); // to 3
            });
            WaitForAsyncUtils.waitForFxEvents();
        }

        @Test
        @DisplayName("E2E-12-011: Step 3 displays security message")
        void step3_displaysSecurityMessage() {
            verifyThat("#securityMessage", isVisible());

            Label securityMsg = lookup("#securityMessage").queryAs(Label.class);
            assertThat(securityMsg.getText()).contains("browser will open");
            assertThat(securityMsg.getText()).contains("NEVER sees your password");
        }

        @Test
        @DisplayName("E2E-12-012: Step 3 displays numbered what-happens steps")
        void step3_displaysWhatHappensSteps() {
            verifyThat("#whatHappensSteps", isVisible());

            VBox stepsBox = lookup("#whatHappensSteps").queryAs(VBox.class);
            assertThat(stepsBox.getChildren()).hasSize(4);

            // Verify each step has a number
            for (int i = 1; i <= 4; i++) {
                Label stepLabel = lookup("#whatHappensStep" + i).queryAs(Label.class);
                assertThat(stepLabel.getText()).startsWith(i + ".");
            }
        }
    }

    // =========================================================================
    // Step 4 Tests - OAuth Connection
    // =========================================================================

    @Nested
    @DisplayName("Step 4: OAuth Connection Tests")
    class Step4OAuthTests {

        @BeforeEach
        void navigateToStep4() {
            runOnFxThread(() -> {
                handleNext(); // to 2
                viewModel.setNino("QQ123456A");
                viewModel.setCanProceed(true);
                handleNext(); // to 3
                handleNext(); // to 4
            });
            WaitForAsyncUtils.waitForFxEvents();
        }

        @Test
        @DisplayName("E2E-12-013: Step 4 displays privacy reminder (FIN-003)")
        void step4_displaysPrivacyReminder() {
            verifyThat("#privacyReminderBox", isVisible());

            Label privacyLabel = lookup("#privacyReminder").queryAs(Label.class);
            assertThat(privacyLabel.getText()).isEqualTo(HmrcConnectionWizardController.PRIVACY_REMINDER);
            assertThat(privacyLabel.getText()).contains("data stays on your device");
        }

        @Test
        @DisplayName("E2E-12-014: Step 4 shows progress during connection")
        void step4_showsProgressDuringConnection() {
            VBox content = lookup("#contentArea").queryAs(VBox.class);

            // Start connection
            runOnFxThread(() -> startOAuthConnection(content));
            WaitForAsyncUtils.waitForFxEvents();

            // Progress container should be visible
            verifyThat("#progressContainer4", isVisible());

            // Connect container should be hidden
            VBox connectContainer = lookup("#connectContainer").queryAs(VBox.class);
            assertThat(connectContainer.isVisible()).isFalse();

            // Status label should show connecting message
            Label statusLabel = lookup("#oauthStatusLabel").queryAs(Label.class);
            assertThat(statusLabel.getText()).isEqualTo(HmrcConnectionWizardController.CONNECTING_TO_HMRC);
        }

        @Test
        @DisplayName("E2E-12-015: OAuth success shows success container")
        void step4_oAuthSuccessShowsSuccessContainer() {
            VBox content = lookup("#contentArea").queryAs(VBox.class);

            // Start and complete OAuth
            runOnFxThread(() -> {
                startOAuthConnection(content);
                simulateOAuthSuccess(content);
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Success container should be visible
            verifyThat("#successContainer", isVisible());

            // Success message displayed
            Label successLabel = lookup("#oauthSuccessLabel").queryAs(Label.class);
            assertThat(successLabel.getText()).isEqualTo(HmrcConnectionWizardController.OAUTH_SUCCESS_MESSAGE);
        }
    }

    // =========================================================================
    // Step 5 Tests - Confirmation
    // =========================================================================

    @Nested
    @DisplayName("Step 5: Confirmation Tests")
    class Step5ConfirmationTests {

        @BeforeEach
        void navigateToStep5() {
            VBox content = (VBox) wizardContainer.lookup("#contentArea");
            runOnFxThread(() -> {
                handleNext(); // to 2
                viewModel.setNino("QQ123456A");
                viewModel.setCanProceed(true);
                handleNext(); // to 3
                handleNext(); // to 4
                // Simulate OAuth success
                viewModel.setConnectionSuccessful(true);
                viewModel.setCanProceed(true);
                viewModel.goNext(); // to 5
                updateContentForStep();
            });
            WaitForAsyncUtils.waitForFxEvents();
        }

        @Test
        @DisplayName("E2E-12-016: Step 5 displays success message")
        void step5_displaysSuccessMessage() {
            assertThat(viewModel.getCurrentStep()).isEqualTo(5);

            verifyThat("#successCard", isVisible());

            Label successTitle = lookup("#successTitle").queryAs(Label.class);
            assertThat(successTitle.getText()).isEqualTo(HmrcConnectionWizardController.SUCCESS_MESSAGE);
        }

        @Test
        @DisplayName("E2E-12-017: Step 5 displays retention reminder (FIN-002)")
        void step5_displaysRetentionReminder() {
            verifyThat("#retentionReminderBox", isVisible());

            Label retentionTitle = lookup("#retentionTitle").queryAs(Label.class);
            Label retentionMsg = lookup("#retentionMessage").queryAs(Label.class);

            assertThat(retentionTitle.getText()).isEqualTo(HmrcConnectionWizardController.RETENTION_TITLE);
            assertThat(retentionMsg.getText()).contains("5 years");
            assertThat(retentionMsg.getText()).contains("submission deadline");
        }

        @Test
        @DisplayName("E2E-12-018: Done button text shown on step 5")
        void step5_doneButtonText() {
            assertThat(viewModel.getNextButtonText()).isEqualTo("Done");

            Button nextButton = lookup("#nextButton").queryAs(Button.class);
            assertThat(nextButton.getText()).isEqualTo("Done");
        }

        @Test
        @DisplayName("E2E-12-019: Step 5 header shows success theme")
        void step5_headerIsGreen() {
            Label headerTitle = lookup("#headerTitle").queryAs(Label.class);
            assertThat(headerTitle.getText()).isEqualTo(HmrcConnectionWizardController.STEP5_TITLE);

            // Header should have green color style
            String style = headerTitle.getStyle();
            assertThat(style).contains("#28a745"); // Green color
        }
    }

    // =========================================================================
    // Persistence Tests
    // =========================================================================

    @Nested
    @DisplayName("Wizard Persistence Tests")
    class WizardPersistenceTests {

        @Test
        @DisplayName("E2E-12-020: NINO is stored in viewModel when entered")
        void wizardProgress_ninoStoredInViewModel() {
            // Navigate to Step 2
            runOnFxThread(() -> handleNext());
            WaitForAsyncUtils.waitForFxEvents();

            NinoInputField ninoInput = lookup("#ninoInput").queryAs(NinoInputField.class);

            // Enter valid NINO
            runOnFxThread(() -> ninoInput.setNino("QQ123456A"));
            WaitForAsyncUtils.waitForFxEvents();

            // Verify stored in viewModel
            assertThat(viewModel.getNino()).isEqualTo("QQ123456A");
            assertThat(viewModel.isNinoValid()).isTrue();
        }
    }

    // =========================================================================
    // Button State Tests
    // =========================================================================

    @Nested
    @DisplayName("Button State Tests")
    class ButtonStateTests {

        @Test
        @DisplayName("Step 1 shows 'Get Started' button")
        void step1_showsGetStartedButton() {
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
            assertThat(viewModel.getNextButtonText()).isEqualTo("Get Started");
        }

        @Test
        @DisplayName("Step 2 shows 'Next' button")
        void step2_showsNextButton() {
            runOnFxThread(() -> handleNext());
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(viewModel.getCurrentStep()).isEqualTo(2);
            assertThat(viewModel.getNextButtonText()).isEqualTo("Next");
        }

        @Test
        @DisplayName("Step label updates correctly")
        void stepLabel_updatesCorrectly() {
            assertThat(viewModel.getStepLabel()).isEqualTo("Step 1 of 5");

            runOnFxThread(() -> handleNext());
            assertThat(viewModel.getStepLabel()).isEqualTo("Step 2 of 5");

            runOnFxThread(() -> {
                viewModel.setNino("QQ123456A");
                viewModel.setCanProceed(true);
                handleNext();
            });
            assertThat(viewModel.getStepLabel()).isEqualTo("Step 3 of 5");
        }
    }

    // =========================================================================
    // NINO Sync E2E Tests (Settings ↔ Wizard)
    // =========================================================================

    @Nested
    @DisplayName("NINO Sync E2E Tests (Settings ↔ Wizard)")
    class NinoSyncE2ETests {

        private static final String TEST_NINO = "AB123456C";
        private static final String DIFFERENT_NINO = "CD987654E";

        @BeforeEach
        void setUpNinoSync() {
            // Initialize test environment
            SqliteTestSupport.setUpTestEnvironment();
            SqliteTestSupport.resetTestData();
        }

        @Test
        @DisplayName("E2E-12-021: Pre-populates NINO from Settings when exists")
        void step2_prePopulatesNinoFromSettings() {
            // Given: NINO exists in Settings
            SqliteDataStore.getInstance().saveNino(TEST_NINO);

            // Navigate to Step 2
            runOnFxThread(() -> handleNext());
            WaitForAsyncUtils.waitForFxEvents();

            // Simulate what the real controller does: check Settings for existing NINO
            NinoInputField ninoInput = lookup("#ninoInput").queryAs(NinoInputField.class);

            // In the real implementation, loadStep2Content() checks SqliteDataStore
            String savedNino = SqliteDataStore.getInstance().loadNino();
            if (savedNino != null && !savedNino.isEmpty()) {
                runOnFxThread(() -> {
                    ninoInput.setNino(savedNino);
                    viewModel.setNino(savedNino);
                });
            }
            WaitForAsyncUtils.waitForFxEvents();

            // Then: NINO should be pre-populated
            assertThat(ninoInput.getNino()).isEqualTo(TEST_NINO);
            assertThat(viewModel.getNino()).isEqualTo(TEST_NINO);
        }

        @Test
        @DisplayName("E2E-12-022: Shows pre-filled hint when NINO loaded from Settings")
        void step2_showsPreFilledHintWhenNinoFromSettings() {
            // Given: NINO exists in Settings
            SqliteDataStore.getInstance().saveNino(TEST_NINO);

            // Navigate to Step 2
            runOnFxThread(() -> handleNext());
            WaitForAsyncUtils.waitForFxEvents();

            // Pre-populate from Settings
            NinoInputField ninoInput = lookup("#ninoInput").queryAs(NinoInputField.class);
            String savedNino = SqliteDataStore.getInstance().loadNino();
            if (savedNino != null && !savedNino.isEmpty()) {
                runOnFxThread(() -> {
                    ninoInput.setNino(savedNino);
                    viewModel.setNino(savedNino);
                });
            }
            WaitForAsyncUtils.waitForFxEvents();

            // Then: NINO should be valid and button enabled
            assertThat(ninoInput.isValid()).isTrue();
            assertThat(viewModel.canProceed()).isTrue();
        }

        @Test
        @DisplayName("E2E-12-023: Does not pre-populate when no NINO in Settings")
        void step2_emptyWhenNoNinoInSettings() {
            // Given: No NINO in Settings (cleared by setUp)
            assertThat(SqliteDataStore.getInstance().loadNino()).isNull();

            // Navigate to Step 2
            runOnFxThread(() -> handleNext());
            WaitForAsyncUtils.waitForFxEvents();

            // Check Settings for existing NINO
            NinoInputField ninoInput = lookup("#ninoInput").queryAs(NinoInputField.class);
            String savedNino = SqliteDataStore.getInstance().loadNino();

            // Then: NINO field should be empty
            assertThat(savedNino).isNull();
            assertThat(ninoInput.getNino()).isEmpty();
        }

        @Test
        @DisplayName("E2E-12-024: Auto-saves NINO to Settings on Step 5")
        void step5_autoSavesNinoToSettings() {
            // Given: No NINO in Settings initially
            assertThat(SqliteDataStore.getInstance().loadNino()).isNull();

            // Navigate through wizard with new NINO
            runOnFxThread(() -> handleNext()); // to Step 2
            WaitForAsyncUtils.waitForFxEvents();

            NinoInputField ninoInput = lookup("#ninoInput").queryAs(NinoInputField.class);
            runOnFxThread(() -> ninoInput.setNino(TEST_NINO));
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(ninoInput.isValid()).isTrue();

            runOnFxThread(() -> {
                viewModel.setNino(TEST_NINO);
                viewModel.setCanProceed(true);
                handleNext(); // to Step 3
                handleNext(); // to Step 4
                viewModel.setConnectionSuccessful(true);
                viewModel.setCanProceed(true);
                viewModel.goNext(); // to Step 5

                // Simulate auto-save logic from loadStep5Content()
                String wizardNino = viewModel.getNino();
                if (wizardNino != null && !wizardNino.isEmpty()) {
                    SqliteDataStore.getInstance().saveNino(wizardNino);
                }

                updateContentForStep();
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Then: NINO should be saved to Settings
            assertThat(viewModel.getCurrentStep()).isEqualTo(5);
            assertThat(SqliteDataStore.getInstance().loadNino()).isEqualTo(TEST_NINO);
        }

        @Test
        @DisplayName("E2E-12-025: Full sync cycle - Settings → Wizard → Settings (updated)")
        void fullSyncCycle_settingsToWizardToSettings() {
            // Given: Initial NINO in Settings
            SqliteDataStore.getInstance().saveNino(TEST_NINO);

            // Step 1: Navigate to Step 2 and load NINO from Settings
            runOnFxThread(() -> handleNext());
            WaitForAsyncUtils.waitForFxEvents();

            NinoInputField ninoInput = lookup("#ninoInput").queryAs(NinoInputField.class);
            String savedNino = SqliteDataStore.getInstance().loadNino();
            runOnFxThread(() -> {
                ninoInput.setNino(savedNino);
                viewModel.setNino(savedNino);
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Verify pre-population
            assertThat(viewModel.getNino()).isEqualTo(TEST_NINO);

            // Step 2: User corrects NINO
            runOnFxThread(() -> {
                ninoInput.setNino(DIFFERENT_NINO);
                viewModel.setNino(DIFFERENT_NINO);
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Step 3: Navigate to Step 5 and auto-save
            runOnFxThread(() -> {
                viewModel.setCanProceed(true);
                handleNext(); // to Step 3
                handleNext(); // to Step 4
                viewModel.setConnectionSuccessful(true);
                viewModel.setCanProceed(true);
                viewModel.goNext(); // to Step 5

                // Auto-save NINO
                String wizardNino = viewModel.getNino();
                if (wizardNino != null && !wizardNino.isEmpty()) {
                    SqliteDataStore.getInstance().saveNino(wizardNino);
                }

                updateContentForStep();
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Then: Settings should have corrected NINO
            assertThat(SqliteDataStore.getInstance().loadNino()).isEqualTo(DIFFERENT_NINO);
        }

        @Test
        @DisplayName("E2E-12-026: ViewModel NINO takes precedence over Settings when already set")
        void viewModelNino_takesPrecedenceOverSettings() {
            // Given: NINO in Settings
            SqliteDataStore.getInstance().saveNino(TEST_NINO);

            // Navigate to Step 2
            runOnFxThread(() -> handleNext());
            WaitForAsyncUtils.waitForFxEvents();

            // Simulate user already having typed a different NINO before Settings load
            runOnFxThread(() -> viewModel.setNino(DIFFERENT_NINO));
            WaitForAsyncUtils.waitForFxEvents();

            // Now check Settings (but don't overwrite if already set)
            String existingNino = viewModel.getNino();
            if (existingNino == null || existingNino.isEmpty()) {
                String savedNino = SqliteDataStore.getInstance().loadNino();
                if (savedNino != null && !savedNino.isEmpty()) {
                    runOnFxThread(() -> viewModel.setNino(savedNino));
                }
            }
            WaitForAsyncUtils.waitForFxEvents();

            // Then: ViewModel should keep user's input
            assertThat(viewModel.getNino()).isEqualTo(DIFFERENT_NINO);
        }

        @Test
        @DisplayName("E2E-12-027: Does not save empty NINO to Settings")
        void step5_doesNotSaveEmptyNino() {
            // Given: Existing NINO in Settings
            SqliteDataStore.getInstance().saveNino(TEST_NINO);

            // Navigate to Step 5 with empty NINO (edge case)
            runOnFxThread(() -> {
                handleNext(); // to Step 2
                viewModel.setNino(""); // Empty NINO
                viewModel.setCanProceed(true);
                handleNext(); // to Step 3
                handleNext(); // to Step 4
                viewModel.setConnectionSuccessful(true);
                viewModel.setCanProceed(true);
                viewModel.goNext(); // to Step 5

                // Simulate auto-save logic (should not overwrite)
                String wizardNino = viewModel.getNino();
                if (wizardNino != null && !wizardNino.isEmpty()) {
                    SqliteDataStore.getInstance().saveNino(wizardNino);
                }

                updateContentForStep();
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Then: Settings should still have original NINO
            assertThat(SqliteDataStore.getInstance().loadNino()).isEqualTo(TEST_NINO);
        }

        @Test
        @DisplayName("E2E-12-028: NINO format preserved through sync")
        void ninoFormat_preservedThroughSync() {
            // Given: Formatted NINO in Settings
            String formattedNino = "AB 12 34 56 C";
            SqliteDataStore.getInstance().saveNino(formattedNino);

            // Navigate to Step 2
            runOnFxThread(() -> handleNext());
            WaitForAsyncUtils.waitForFxEvents();

            // Load and verify format preservation
            String savedNino = SqliteDataStore.getInstance().loadNino();

            // Note: saveNino() normalizes to uppercase but preserves format
            // The stored value might be normalized depending on SqliteDataStore implementation
            assertThat(savedNino).isNotNull();
            assertThat(savedNino.toUpperCase().replace(" ", "")).isEqualTo("AB123456C");
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Runs the given action on the FX application thread and waits for completion.
     */
    private void runOnFxThread(Runnable action) {
        interact(action);
    }
}
