package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.geometry.Rectangle2D;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import uk.selfemploy.ui.util.DialogBounds;
import uk.selfemploy.ui.util.StatusGlyph;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.export.DataExportService;
import uk.selfemploy.core.export.DataImportService;
import uk.selfemploy.core.export.ExportResult;
import uk.selfemploy.core.export.ImportException;
import uk.selfemploy.core.export.ImportOptions;
import uk.selfemploy.core.export.ImportPreview;
import uk.selfemploy.core.export.ImportResult;
import uk.selfemploy.core.export.ImportType;
import uk.selfemploy.core.service.PrivacyAcknowledgmentService;
import uk.selfemploy.core.service.TermsAcceptanceService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.ui.viewmodel.HmrcConnectionWizardViewModel;

import uk.selfemploy.common.legal.Disclaimers;
import uk.selfemploy.common.util.VersionInfo;
import uk.selfemploy.ui.component.AppDialog;
import uk.selfemploy.ui.component.HmrcRegistrationGuideDialog;
import uk.selfemploy.ui.help.HelpContent;
import uk.selfemploy.hmrc.logging.HmrcPiiRedactor;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.service.CredentialEncryptionException;
import uk.selfemploy.ui.service.HmrcBusinessProfileService;
import uk.selfemploy.ui.service.HmrcConnectionSelfTest;
import uk.selfemploy.ui.service.HmrcRegistrationGuide;
import uk.selfemploy.ui.service.HmrcCredentialValidator;
import uk.selfemploy.ui.service.HmrcConnectionService;
import uk.selfemploy.ui.service.OAuthServiceFactory;
import uk.selfemploy.ui.service.SqliteDataStore;
import uk.selfemploy.ui.service.UiDuplicateDetectionService;
import uk.selfemploy.ui.viewmodel.ImportAction;
import uk.selfemploy.ui.viewmodel.ImportCandidateViewModel;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.net.URI;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Settings page.
 * Provides access to user profile, legal documents, and data management.
 */
public class SettingsController implements Initializable, MainController.TaxYearAware {

    private static final Logger LOG = Logger.getLogger(SettingsController.class.getName());
    private static final List<String> SETTINGS_CATEGORIES = Arrays.asList("Profile", "Legal", "Data");

    /**
     * Verification status for the user's NINO against HMRC.
     * Tracks whether the NINO has been confirmed to exist in HMRC's system.
     */
    public enum NinoVerificationStatus {
        /**
         * NINO has not yet been verified with HMRC.
         * This is the initial state before any verification attempt.
         */
        NOT_VERIFIED,

        /**
         * NINO was verified successfully by HMRC (200 response with business ID).
         * The user's self-employment is registered and confirmed.
         */
        VERIFIED,

        /**
         * NINO verification failed - HMRC returned 401/403 (mismatch).
         * The NINO doesn't match the authenticated Government Gateway account.
         */
        FAILED,

        /**
         * Using sandbox fallback business ID because HMRC returned 404.
         * This is NOT a true verification - the NINO was not found.
         * Used only in sandbox mode for testing purposes.
         */
        SANDBOX_FALLBACK,

        /**
         * NINO was changed since the last successful connection.
         * In sandbox mode, we cannot verify if the new NINO is valid.
         * User should be warned that their NINO change cannot be verified.
         */
        NINO_CHANGED,

        /**
         * OAuth succeeded but business profile sync failed due to server error (5xx).
         * The connection is partially established - user can proceed but should be aware
         * that profile data will sync on first submission.
         */
        PROFILE_SYNC_PENDING
    }

    // === FXML Injected Fields ===

    @FXML private Label displayNameLabel;
    @FXML private TextField displayNameField;
    @FXML private Button saveDisplayNameButton;
    @FXML private Label utrLabel;
    @FXML private TextField utrField;
    @FXML private Button saveUtrButton;
    @FXML private Label ninoLabel;
    @FXML private TextField ninoField;
    @FXML private Button saveNinoButton;
    @FXML private Button exportButton;
    @FXML private Button importButton;
    @FXML private Button termsButton;
    @FXML private Button privacyButton;
    @FXML private Button disclaimerButton;

    // About section FXML fields
    @FXML private Label versionLabel;
    @FXML private Label buildDateLabel;
    @FXML private Label licenseLabel;
    @FXML private Label githubLabel;

    // HMRC API Credentials FXML fields
    @FXML private Label hmrcCredentialsStatusLabel;
    @FXML private TextField hmrcClientIdField;
    @FXML private PasswordField hmrcClientSecretField;
    @FXML private TextField hmrcRedirectUriField;
    @FXML private Button saveCredentialsButton;
    @FXML private Button clearCredentialsButton;
    @FXML private Button copyRedirectUriButton;
    @FXML private Button hmrcRegistrationHelpButton;

    // HMRC Connection Setup FXML fields
    @FXML private FontIcon hmrcStatusIcon;
    @FXML private Label hmrcConnectionStatusLabel;
    @FXML private Label hmrcSetupInstructions;
    @FXML private ComboBox<String> hmrcEnvironmentCombo;
    @FXML private Label hmrcEnvironmentHint;
    @FXML private Button hmrcSetupButton;
    @FXML private Button hmrcDisconnectButton;
    @FXML private VBox hmrcSetupChecklist;
    @FXML private FontIcon checkNino;
    @FXML private FontIcon checkOAuth;
    @FXML private FontIcon checkProfile;

    // === State ===

    private TaxYear taxYear;
    private String displayName = "";
    private String utr = "";
    private String nino = "";
    private NinoVerificationStatus ninoVerificationStatus = NinoVerificationStatus.NOT_VERIFIED;

    private final HmrcBusinessProfileService profileService = new HmrcBusinessProfileService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadDisplayNameFromStore();
        updateDisplayNameDisplay();
        loadUtrFromStore();
        updateUtrDisplay();
        loadNinoFromStore();
        updateNinoDisplay();
        loadHmrcCredentialsStatus();
        initHmrcEnvironmentCombo();
        updateHmrcConnectionStatus();
        initAboutSection();
    }

    @Override
    public void setTaxYear(TaxYear taxYear) {
        this.taxYear = taxYear;
    }

    /**
     * Returns the current tax year.
     */
    public TaxYear getTaxYear() {
        return taxYear;
    }

    // === Settings Categories ===

    /**
     * Returns the list of settings categories.
     */
    public List<String> getSettingsCategories() {
        return SETTINGS_CATEGORIES;
    }

    // === Legal Documents ===

    /**
     * Returns true if Terms of Service can be shown.
     */
    public boolean canShowTermsOfService() {
        return true;
    }

    /**
     * Returns true if Privacy Notice can be shown.
     */
    public boolean canShowPrivacyNotice() {
        return true;
    }

    // === Data Management ===

    /**
     * Returns true if data export is supported.
     */
    public boolean canExportData() {
        return true;
    }

    /**
     * Returns true if data import is supported.
     */
    public boolean canImportData() {
        return true;
    }

    // === Display Name Management ===

    /**
     * Returns the stored display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name.
     * Trims whitespace and handles null by setting to empty string.
     */
    public void setDisplayName(String displayName) {
        if (displayName == null) {
            this.displayName = "";
        } else {
            this.displayName = displayName.trim();
        }
    }

    /**
     * Returns true if a display name is set.
     */
    public boolean hasDisplayName() {
        return displayName != null && !displayName.isBlank();
    }

    /**
     * Returns the display name formatted for display.
     * Returns "Not set" if empty.
     */
    public String getFormattedDisplayName() {
        if (displayName == null || displayName.isBlank()) {
            return "Not set";
        }
        return displayName;
    }

    // === UTR Management ===

    /**
     * Returns the stored UTR.
     */
    public String getUtr() {
        return utr;
    }

    /**
     * Sets the UTR.
     */
    public void setUtr(String utr) {
        this.utr = utr != null ? utr : "";
    }

    /**
     * Validates if the given UTR format is valid.
     * UTR must be exactly 10 digits.
     */
    public boolean isValidUtr(String utr) {
        if (utr == null || utr.isEmpty()) {
            return false;
        }
        return utr.matches("\\d{10}");
    }

    /**
     * Returns the UTR formatted for display.
     * Formats as "12345 67890" or "Not set" if empty.
     */
    public String getFormattedUtr() {
        if (utr == null || utr.isEmpty()) {
            return "Not set";
        }
        if (utr.length() == 10) {
            return utr.substring(0, 5) + " " + utr.substring(5);
        }
        return utr;
    }

    // === NINO Management ===

    /**
     * Returns the stored NINO.
     */
    public String getNino() {
        return nino;
    }

    /**
     * Sets the NINO (normalised to uppercase, spaces stripped).
     */
    public void setNino(String nino) {
        this.nino = nino != null ? nino.replaceAll("\\s", "").toUpperCase() : "";
    }

    /**
     * Validates if the given NINO format is valid.
     * UK NINO format: 2 letters, 6 digits, 1 letter (A/B/C/D).
     * Excludes prefixes: BG, GB, NK, KN, TN, NT, ZZ and first letter D, F, I, Q, U, V.
     */
    public boolean isValidNino(String nino) {
        if (nino == null || nino.isEmpty()) {
            return false;
        }
        String cleaned = nino.replaceAll("\\s", "").toUpperCase();
        return cleaned.matches("[A-CEGHJ-PR-TW-Z][A-CEGHJ-NPR-TW-Z]\\d{6}[A-D]");
    }

    /**
     * Returns the NINO formatted for display.
     * Formats as "QQ 12 34 56 A" or "Not set" if empty.
     */
    public String getFormattedNino() {
        if (nino == null || nino.isEmpty()) {
            return "Not set";
        }
        return formatNinoForDisplay(nino);
    }

    /**
     * Formats any NINO string for display.
     * Formats as "QQ 12 34 56 A" or returns as-is if not 9 characters.
     *
     * @param ninoValue the NINO to format
     * @return formatted NINO string
     */
    private String formatNinoForDisplay(String ninoValue) {
        if (ninoValue == null || ninoValue.isEmpty()) {
            return "Not set";
        }
        if (ninoValue.length() == 9) {
            return ninoValue.substring(0, 2) + " " + ninoValue.substring(2, 4) + " "
                    + ninoValue.substring(4, 6) + " " + ninoValue.substring(6, 8) + " " + ninoValue.substring(8);
        }
        return ninoValue;
    }

    // === NINO Verification Status ===

    /**
     * Returns the current NINO verification status.
     *
     * @return the verification status
     */
    public NinoVerificationStatus getNinoVerificationStatus() {
        return ninoVerificationStatus;
    }

    /**
     * Sets the NINO verification status.
     *
     * @param status the verification status
     */
    public void setNinoVerificationStatus(NinoVerificationStatus status) {
        this.ninoVerificationStatus = status != null ? status : NinoVerificationStatus.NOT_VERIFIED;
    }

    /**
     * Returns true only if the NINO has been truly verified by HMRC.
     * Returns false for sandbox fallback, not verified, or failed states.
     *
     * @return true if NINO is verified, false otherwise
     */
    public boolean isNinoVerified() {
        return ninoVerificationStatus == NinoVerificationStatus.VERIFIED;
    }

    /**
     * Returns a human-readable message describing the NINO verification status.
     *
     * @return status message
     */
    public String getNinoVerificationMessage() {
        return switch (ninoVerificationStatus) {
            case NOT_VERIFIED -> "NINO has not been verified with HMRC";
            case VERIFIED -> "NINO verified - Self-employment registered with HMRC";
            case FAILED -> "NINO verification failed - NINO doesn't match HMRC account";
            case SANDBOX_FALLBACK -> "NINO not verified - Using sandbox test data";
            case NINO_CHANGED -> "NINO changed since last connection - Sandbox cannot verify new NINO";
            case PROFILE_SYNC_PENDING -> "Connected - Business profile sync pending (will retry on first submission)";
        };
    }

    /**
     * Checks if the current NINO differs from the NINO used when
     * the HMRC connection was first established.
     * <p>
     * In sandbox mode, HMRC returns 404 for all NINOs (real NINOs aren't in sandbox),
     * so we cannot verify if a changed NINO is valid. This method helps detect
     * when a user has changed their NINO so we can warn them.
     *
     * @return true if the current NINO differs from the connected NINO,
     *         false if they match or if no connected NINO exists (first connection)
     */
    public boolean hasNinoChangedSinceConnection() {
        return HmrcConnectionService.getInstance().isNinoChangedSinceConnection();
    }

    // === Sandbox Mode Detection ===

    /**
     * Checks if the given HMRC API base URL is for the sandbox environment.
     *
     * @param apiBaseUrl the HMRC API base URL
     * @return true if sandbox mode, false for production
     */
    public boolean isSandboxMode(String apiBaseUrl) {
        return HmrcBusinessProfileService.isSandbox(apiBaseUrl);
    }

    /**
     * Checks if the given HTTP status code indicates a server error (5xx).
     *
     * @param statusCode the HTTP status code to check
     * @return true if the status code is in the 5xx range (500-599)
     */
    public boolean isServerError(int statusCode) {
        return HmrcBusinessProfileService.isServerError(statusCode);
    }

    /**
     * Returns the fallback business ID for sandbox mode.
     *
     * @return the sandbox fallback business ID
     */
    public String getSandboxFallbackBusinessId() {
        return HmrcBusinessProfileService.sandboxFallbackBusinessId();
    }

    // === Private Helper Methods ===

    private void loadDisplayNameFromStore() {
        try {
            String stored = SqliteDataStore.getInstance().loadDisplayName();
            if (stored != null && !stored.isBlank()) {
                this.displayName = stored;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load display name from store", e);
        }
    }

    private void updateDisplayNameDisplay() {
        if (displayNameLabel != null) {
            displayNameLabel.setText(getFormattedDisplayName());
        }
    }

    private void loadUtrFromStore() {
        try {
            String stored = SqliteDataStore.getInstance().loadUtr();
            if (stored != null && !stored.isBlank()) {
                this.utr = stored;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load UTR from store", e);
        }
    }

    private void loadNinoFromStore() {
        try {
            String stored = SqliteDataStore.getInstance().loadNino();
            if (stored != null && !stored.isBlank()) {
                this.nino = stored;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load NINO from store", e);
        }
    }

    private void updateNinoDisplay() {
        if (ninoLabel != null) {
            ninoLabel.setText(getFormattedNino());
        }
    }

    private void updateUtrDisplay() {
        if (utrLabel != null) {
            utrLabel.setText(getFormattedUtr());
        }
    }

    // === FXML Event Handlers ===

    @FXML
    void handleSaveDisplayName(ActionEvent event) {
        if (displayNameField != null) {
            String newName = displayNameField.getText();
            setDisplayName(newName);
            SqliteDataStore.getInstance().saveDisplayName(displayName.isEmpty() ? null : displayName);
            updateDisplayNameDisplay();
            displayNameField.clear();
            if (displayName.isEmpty()) {
                showInfo("Display Name Cleared", "Your display name has been removed.");
            } else {
                showInfo("Display Name Saved", "Your display name has been saved as: " + displayName);
            }
        }
    }

    @FXML
    void handleSaveUtr(ActionEvent event) {
        if (utrField != null) {
            String newUtr = utrField.getText().replaceAll("\\s", "");
            if (isValidUtr(newUtr)) {
                setUtr(newUtr);
                SqliteDataStore.getInstance().saveUtr(newUtr);
                updateUtrDisplay();
                utrField.clear();
                showInfo("UTR Saved", "Your Unique Taxpayer Reference has been saved.");
            } else {
                showError("Invalid UTR", "Please enter a valid 10-digit UTR number.");
            }
        }
    }

    @FXML
    void handleSaveNino(ActionEvent event) {
        if (ninoField != null) {
            String newNino = ninoField.getText().replaceAll("\\s", "").toUpperCase();
            if (isValidNino(newNino)) {
                setNino(newNino);
                SqliteDataStore.getInstance().saveNino(newNino);
                updateNinoDisplay();
                updateHmrcConnectionStatus(); // Update HMRC status after NINO saved
                ninoField.clear();
                showInfo("NINO Saved", "Your National Insurance Number has been saved.");
            } else {
                showError("Invalid NINO",
                        "Please enter a valid National Insurance Number (e.g. QQ 12 34 56 A).");
            }
        }
    }

    // === HMRC API Credentials ===

    private void loadHmrcCredentialsStatus() {
        try {
            boolean hasCredentials = SqliteDataStore.getInstance().hasHmrcCredentials();
            if (hmrcCredentialsStatusLabel != null) {
                if (hasCredentials) {
                    hmrcCredentialsStatusLabel.setText("Configured");
                    hmrcCredentialsStatusLabel.setStyle("-fx-text-fill: #27ae60;");
                } else {
                    hmrcCredentialsStatusLabel.setText("Not configured — enter your Client ID and Secret above");
                    hmrcCredentialsStatusLabel.setStyle("");
                }
            }
            if (clearCredentialsButton != null) {
                clearCredentialsButton.setVisible(hasCredentials);
                clearCredentialsButton.setManaged(hasCredentials);
            }
            // The Connect & Verify button's enabled state is owned solely by
            // updateHmrcConnectionStatus, which gates on both credentials and NINO.
            updateHmrcConnectionStatus();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load HMRC credentials status", e);
        }
    }

    private boolean hmrcOperationInProgress;

    @FXML
    void handleSaveHmrcCredentials(ActionEvent event) {
        if (hmrcOperationInProgress) {
            return;
        }

        // Trim first: a leading/trailing space is a routine paste artifact, and the stored value is
        // trimmed anyway. Validate the trimmed value, so only genuinely malformed input is rejected.
        String clientId = hmrcClientIdField != null ? hmrcClientIdField.getText() : null;
        String clientSecret = hmrcClientSecretField != null ? hmrcClientSecretField.getText() : null;
        String trimmedId = clientId == null ? "" : clientId.trim();
        String trimmedSecret = clientSecret == null ? "" : clientSecret.trim();

        HmrcCredentialValidator.Result idCheck = HmrcCredentialValidator.validateClientId(trimmedId);
        if (!idCheck.valid()) {
            showError("Check your Client ID", idCheck.message());
            return;
        }
        HmrcCredentialValidator.Result secretCheck = HmrcCredentialValidator.validateClientSecret(trimmedSecret);
        if (!secretCheck.valid()) {
            showError("Check your Client Secret", secretCheck.message());
            return;
        }

        SqliteDataStore store = SqliteDataStore.getInstance();
        try {
            store.saveHmrcClientId(trimmedId);
            store.saveHmrcClientSecret(trimmedSecret);
        } catch (CredentialEncryptionException e) {
            LOG.log(Level.SEVERE, "Failed to encrypt and save HMRC credentials", e);
            showError("Could Not Save Credentials",
                "Your HMRC credentials could not be secured on this device. Check that the "
                    + "application data folder is writable, then try again.");
            return;
        }

        // Apply credentials as system properties for HmrcConfig
        applyHmrcCredentialsToSystemProperties(trimmedId, trimmedSecret);

        String environment = SqliteDataStore.getInstance().loadHmrcEnvironment();

        loadHmrcCredentialsStatus(); // also refreshes the connection status and button state
        // Fields are kept until the self-test passes, so a failed test leaves them editable to fix.
        runCredentialSelfTest(environment, trimmedId, trimmedSecret);
    }

    /**
     * Runs the HMRC connection self-test off the UI thread after a save and reports the three checks.
     * The credential values are passed straight to the test and never re-read into a control; the
     * self-test itself keeps them out of logs and messages. Saving is debounced
     * while a test is in flight, and any unexpected failure of the test still returns the UI to a
     * usable state rather than leaving it stuck on "Testing…".
     */
    private void runCredentialSelfTest(String environment, String clientId, String clientSecret) {
        hmrcOperationInProgress = true;
        setCredentialButtonsDisabled(true);
        // Disable Connect & Verify for the duration too, so the OAuth flow can't interleave with the
        // self-test. The operation flag keeps it disabled through any status refresh.
        if (hmrcSetupButton != null) {
            hmrcSetupButton.setDisable(true);
        }
        if (hmrcCredentialsStatusLabel != null) {
            hmrcCredentialsStatusLabel.setText("Testing…");
        }
        Thread.startVirtualThread(() -> {
            HmrcConnectionSelfTest.SelfTestReport report = null;
            try {
                report = new HmrcConnectionSelfTest().run(environment, clientId, clientSecret);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "HMRC connection self-test failed unexpectedly", e);
            } finally {
                // A finally block guarantees the UI is restored even if run() throws an Error
                // (e.g. a linkage error or OOM) rather than a RuntimeException.
                HmrcConnectionSelfTest.SelfTestReport finalReport = report;
                Platform.runLater(() -> completeSelfTest(finalReport));
            }
        });
    }

    /**
     * Restores the UI after a self-test and shows the outcome. If the credentials were cleared while
     * the test ran, the "passed" report would be about credentials that no longer exist, so the
     * result is discarded when nothing is stored.
     */
    private void completeSelfTest(HmrcConnectionSelfTest.SelfTestReport report) {
        hmrcOperationInProgress = false;
        setCredentialButtonsDisabled(false);
        loadHmrcCredentialsStatus();

        if (!SqliteDataStore.getInstance().hasHmrcCredentials()) {
            return;
        }
        if (report == null) {
            showWarning("Connection Test",
                "Your credentials were saved, but the connection test couldn't be completed. "
                    + "Try 'Connect & Verify', or save again to retest.");
            return;
        }
        showSelfTestReport(report);
    }

    private void setCredentialButtonsDisabled(boolean disabled) {
        if (saveCredentialsButton != null) {
            saveCredentialsButton.setDisable(disabled);
        }
        if (clearCredentialsButton != null) {
            clearCredentialsButton.setDisable(disabled);
        }
    }

    private void showSelfTestReport(HmrcConnectionSelfTest.SelfTestReport report) {
        StringBuilder body = new StringBuilder("Your HMRC credentials are saved and encrypted.\n\n");
        for (HmrcConnectionSelfTest.Check check : report.checks()) {
            String mark = switch (check.status()) {
                case PASS -> StatusGlyph.PASS;
                case FAIL -> StatusGlyph.FAIL;
                case SKIPPED -> StatusGlyph.NEUTRAL;
            };
            body.append(mark).append("  ").append(check.name()).append(" — ")
                .append(check.message()).append('\n');
        }
        if (report.allPassed()) {
            if (hmrcClientIdField != null) {
                hmrcClientIdField.clear();
            }
            if (hmrcClientSecretField != null) {
                hmrcClientSecretField.clear();
            }
            body.append("\nYou're ready to connect with 'Connect & Verify'.");
            showInfo("Connection Test Passed", body.toString());
        } else {
            body.append("\nCheck the values above and save again to retest.");
            showWarning("Connection Test", body.toString());
        }
    }

    @FXML
    void handleShowRegistrationHelp(ActionEvent event) {
        String redirectUri = hmrcRedirectUriField != null && !hmrcRedirectUriField.getText().isBlank()
            ? hmrcRedirectUriField.getText()
            : OAuthServiceFactory.getRedirectUri();
        HmrcRegistrationGuideDialog.show(getOwnerWindow(), new HmrcRegistrationGuide(redirectUri));
    }

    @FXML
    void handleCopyRedirectUri(ActionEvent event) {
        String uri = hmrcRedirectUriField != null ? hmrcRedirectUriField.getText() : "http://localhost:8088/oauth/callback";
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(uri);
        clipboard.setContent(content);
        if (copyRedirectUriButton != null) {
            String originalText = copyRedirectUriButton.getText();
            copyRedirectUriButton.setText("Copied!");
            copyRedirectUriButton.setDisable(true);
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1500));
            pause.setOnFinished(ev -> {
                copyRedirectUriButton.setText(originalText);
                copyRedirectUriButton.setDisable(false);
            });
            pause.play();
        }
    }

    @FXML
    void handleClearHmrcCredentials(ActionEvent event) {
        SqliteDataStore.getInstance().clearHmrcCredentials();
        System.clearProperty("HMRC_CLIENT_ID");
        System.clearProperty("HMRC_CLIENT_SECRET");
        loadHmrcCredentialsStatus(); // also refreshes the connection status and button state
        showInfo("Credentials Cleared", "Your HMRC API credentials have been removed.");
    }

    /**
     * Applies stored HMRC credentials as system properties so that
     * HmrcConfig and the OAuth flow can read them.
     */
    static void applyHmrcCredentialsToSystemProperties(String clientId, String clientSecret) {
        if (clientId != null && !clientId.isBlank()) {
            System.setProperty("HMRC_CLIENT_ID", clientId);
        }
        if (clientSecret != null && !clientSecret.isBlank()) {
            System.setProperty("HMRC_CLIENT_SECRET", clientSecret);
        }
    }

    /**
     * Loads stored HMRC credentials from SQLite and applies them as system properties.
     * Called during app startup to make credentials available to HmrcConfig.
     */
    public static void loadAndApplyStoredCredentials() {
        try {
            SqliteDataStore store = SqliteDataStore.getInstance();
            String clientId = store.loadHmrcClientId();
            String clientSecret = store.loadHmrcClientSecret();
            applyHmrcCredentialsToSystemProperties(clientId, clientSecret);
        } catch (Exception e) {
            Logger.getLogger(SettingsController.class.getName())
                    .log(Level.WARNING, "Failed to load stored HMRC credentials", e);
        }
    }

    // === HMRC Environment ===

    private static final String SANDBOX_API_BASE = "https://test-api.service.hmrc.gov.uk";
    private static final String SANDBOX_AUTHORIZE = "https://test-www.tax.service.gov.uk/oauth/authorize";
    private static final String SANDBOX_TOKEN = "https://test-api.service.hmrc.gov.uk/oauth/token";
    private static final String PRODUCTION_API_BASE = "https://api.service.hmrc.gov.uk";
    private static final String PRODUCTION_AUTHORIZE = "https://www.tax.service.gov.uk/oauth/authorize";
    private static final String PRODUCTION_TOKEN = "https://api.service.hmrc.gov.uk/oauth/token";

    private void initHmrcEnvironmentCombo() {
        if (hmrcEnvironmentCombo == null) return;

        hmrcEnvironmentCombo.getItems().addAll("Sandbox (Testing)", "Production (Live)");

        String stored = SqliteDataStore.getInstance().loadHmrcEnvironment();
        hmrcEnvironmentCombo.setValue("production".equals(stored)
                ? "Production (Live)" : "Sandbox (Testing)");

        hmrcEnvironmentCombo.setOnAction(e -> handleEnvironmentChange());
    }

    private boolean environmentChangeInProgress;

    private void handleEnvironmentChange() {
        if (hmrcEnvironmentCombo == null || environmentChangeInProgress) return;

        String selected = hmrcEnvironmentCombo.getValue();
        boolean isProduction = "Production (Live)".equals(selected);

        if (isProduction) {
            boolean switchToLive = AppDialog.confirm("Switch to Live Mode?",
                    "The app will connect to HMRC's live service. Your saved data stays the same.",
                    "Switch to Live", "Stay in Sandbox");
            if (!switchToLive) {
                // Revert silently without triggering the handler again
                environmentChangeInProgress = true;
                hmrcEnvironmentCombo.setValue("Sandbox (Testing)");
                environmentChangeInProgress = false;
                return;
            }
        }

        String envValue = isProduction ? "production" : "sandbox";
        SqliteDataStore.getInstance().saveHmrcEnvironment(envValue);
        applyHmrcEnvironmentToSystemProperties(envValue);

        // Reset OAuth connection since tokens are environment-specific
        SqliteDataStore.getInstance().clearOAuthTokens();
        SqliteDataStore.getInstance().saveHmrcBusinessId(null);
        SqliteDataStore.getInstance().saveHmrcTradingName(null);
        SqliteDataStore.getInstance().saveNinoVerified(false);
        OAuthServiceFactory.shutdown();

        updateHmrcConnectionStatus();

        String envLabel = isProduction ? "Production" : "Sandbox";
        showInfo("Environment Changed",
                "Switched to " + envLabel + " environment.\n\n" +
                "Please reconnect to HMRC using the 'Connect & Verify' button.");
    }

    /**
     * Applies HMRC environment URLs as system properties based on the stored environment setting.
     */
    static void applyHmrcEnvironmentToSystemProperties(String environment) {
        // null/blank defaults to sandbox (safe default)
        boolean isProduction = "production".equals(environment != null ? environment.strip() : "");
        System.setProperty("HMRC_API_BASE_URL", isProduction ? PRODUCTION_API_BASE : SANDBOX_API_BASE);
        System.setProperty("HMRC_AUTHORIZE_URL", isProduction ? PRODUCTION_AUTHORIZE : SANDBOX_AUTHORIZE);
        System.setProperty("HMRC_TOKEN_URL", isProduction ? PRODUCTION_TOKEN : SANDBOX_TOKEN);
    }

    /**
     * Loads stored HMRC environment from SQLite and applies URLs as system properties.
     * Called during app startup.
     */
    public static void loadAndApplyStoredEnvironment() {
        try {
            String env = SqliteDataStore.getInstance().loadHmrcEnvironment();
            applyHmrcEnvironmentToSystemProperties(env);
        } catch (Exception e) {
            Logger.getLogger(SettingsController.class.getName())
                    .log(Level.WARNING, "Failed to load stored HMRC environment", e);
        }
    }

    // === HMRC Connection Setup ===

    @FXML
    void handleHmrcSetup(ActionEvent event) {
        LOG.info("Starting HMRC connection");

        HmrcConnectionService connectionService = HmrcConnectionService.getInstance();
        if (connectionService.canQuickReconnect()) {
            quickReconnect(connectionService);
        } else {
            launchConnectionWizard();
        }
    }

    /**
     * Refreshes an existing session without the full wizard, then re-verifies the business profile
     * with the refreshed token so a changed NINO or a stale business ID is re-verified (the old
     * connect always did this).
     *
     * <p>Surviving, loadable tokens after a failed refresh mean the failure was transient rather
     * than a rejection, so the user is offered a retry. A genuine rejection or an unloadable session
     * has no session left to refresh and falls back to the full wizard.
     */
    private void quickReconnect(HmrcConnectionService connectionService) {
        hmrcOperationInProgress = true;
        if (hmrcSetupButton != null) {
            hmrcSetupButton.setDisable(true);
            hmrcSetupButton.setText("Reconnecting...");
        }
        if (hmrcConnectionStatusLabel != null) {
            hmrcConnectionStatusLabel.setText("Refreshing your HMRC session...");
        }

        connectionService.verifySession().whenComplete((result, error) -> Platform.runLater(() -> {
            OAuthTokens tokens = OAuthServiceFactory.getOAuthService().getCurrentTokens();

            if (error == null && result == HmrcConnectionService.VerificationResult.VERIFIED
                    && tokens != null) {
                String nino = SqliteDataStore.getInstance().loadNino();
                if (nino != null && !nino.isBlank()) {
                    reverifyBusinessProfile(nino, tokens.accessToken());
                    return;
                }
                hmrcOperationInProgress = false;
                launchConnectionWizard();
            } else if (tokens != null && connectionService.canQuickReconnect()) {
                hmrcOperationInProgress = false;
                updateHmrcConnectionStatus();
                showWarning("Couldn't Reconnect",
                    "We couldn't reach HMRC just now. Please check your connection and try again.");
            } else {
                hmrcOperationInProgress = false;
                LOG.info("Session refresh was not possible; launching the full connection wizard");
                launchConnectionWizard();
            }
        }));
    }

    /**
     * Re-resolves and persists the business profile for a silent reconnect (no wizard). Runs the
     * fetch on a background thread via {@link HmrcBusinessProfileService} and, unlike the wizard
     * flow, surfaces the outcome as a dialog because the user triggered the reconnect directly.
     */
    private void reverifyBusinessProfile(String nino, String accessToken) {
        if (hmrcConnectionStatusLabel != null) {
            hmrcConnectionStatusLabel.setText("Fetching business profile...");
        }
        Thread.startVirtualThread(() -> {
            HmrcBusinessProfileService.Result result = null;
            try {
                result = profileService.fetchAndPersist(nino, accessToken);
            } finally {
                // A finally guarantees the operation is cleared and the UI restored even if the fetch
                // throws, so the button is never left permanently disabled on "Fetching…".
                HmrcBusinessProfileService.Result finalResult = result;
                Platform.runLater(() -> {
                    hmrcOperationInProgress = false;
                    if (finalResult != null) {
                        applyProfileStatus(finalResult);
                        showProfileOutcomeDialog(finalResult);
                    } else {
                        updateHmrcConnectionStatus();
                        showWarning("Couldn't Finish Connecting",
                            "We couldn't fetch your HMRC business profile just now. "
                                + "Please check your connection and try again.");
                    }
                });
            }
        });
    }

    /**
     * Launches the guided connection wizard, which owns the whole flow: OAuth, resolving and
     * persisting the business profile, and showing the outcome. Once it closes, this reflects the
     * final persisted state (and re-enables the button, since quick-reconnect may have disabled it).
     */
    private void launchConnectionWizard() {
        Stage owner = getOwnerWindow() instanceof Stage stage ? stage : null;

        HmrcConnectionWizardViewModel result = HmrcConnectionWizardController.showWizard(owner);

        if (result == null) {
            showError("Connection Failed",
                "The HMRC connection screen could not be opened. Please try again.");
        }
        updateHmrcConnectionStatus();
    }

    /**
     * Applies a resolved business-profile outcome to the Settings verification badge and button.
     *
     * @param result the resolved business-profile outcome
     */
    private void applyProfileStatus(HmrcBusinessProfileService.Result result) {
        switch (result.outcome()) {
            case VERIFIED -> {
                setNinoVerificationStatus(NinoVerificationStatus.VERIFIED);
                completeSetup(true, "Connected and verified");
            }
            case NINO_CHANGED_SANDBOX -> {
                setNinoVerificationStatus(NinoVerificationStatus.NINO_CHANGED);
                completeSetup(true, "Connected (Sandbox) - NINO changed");
            }
            case PROFILE_SYNC_PENDING -> {
                setNinoVerificationStatus(NinoVerificationStatus.PROFILE_SYNC_PENDING);
                completeSetup(true, "Connected (profile sync pending)");
            }
            case NINO_MISMATCH, NO_BUSINESS_FOUND, NINO_NOT_FOUND -> {
                setNinoVerificationStatus(NinoVerificationStatus.FAILED);
                completeSetup(false, "NINO verification failed");
            }
        }
    }

    /**
     * Shows a dialog describing a resolved business-profile outcome, for the direct reconnect flow
     * (the wizard shows its own outcome screen instead).
     *
     * @param result the resolved business-profile outcome
     */
    private void showProfileOutcomeDialog(HmrcBusinessProfileService.Result result) {
        String environment = result.sandbox() ? "Sandbox" : "Production";
        switch (result.outcome()) {
            case VERIFIED -> showInfo("HMRC Connected (" + environment + ")",
                "Your National Insurance number and business profile have been verified.\n\n"
                    + "Business ID: " + result.businessId());
            case NINO_CHANGED_SANDBOX -> showWarning("NINO Changed",
                "Your National Insurance number has changed since your last connection.\n\n"
                    + "Sandbox mode cannot verify whether the new number is correct; in production it "
                    + "will be validated against HMRC records.");
            case PROFILE_SYNC_PENDING -> showWarning("HMRC Partially Connected",
                "Connected to HMRC, but your business profile could not be fetched right now.\n\n"
                    + "It will sync automatically on your first submission, or you can try "
                    + "reconnecting later.");
            case NINO_MISMATCH -> showError("NINO Verification Failed",
                "The National Insurance number you entered does not match your HMRC account.\n\n"
                    + "Please check that you entered it correctly.");
            case NO_BUSINESS_FOUND -> showError("NINO Verification Failed",
                "No self-employment business is registered with this National Insurance number.\n\n"
                    + "Make sure you have registered for Self Assessment with HMRC.");
            case NINO_NOT_FOUND -> showError("NINO Verification Failed",
                "No self-employment record was found for this National Insurance number.\n\n"
                    + "Make sure you have registered for Self Assessment with HMRC.");
        }
    }

    private void completeSetup(boolean success, String statusText) {
        if (hmrcSetupButton != null) {
            hmrcSetupButton.setText(success ? "Reconnect" : "Connect & Verify");
        }
        updateHmrcConnectionStatus();
    }

    @FXML
    void handleHmrcDisconnect(ActionEvent event) {
        LOG.info("Disconnecting from HMRC");

        // Clear OAuth tokens
        SqliteDataStore.getInstance().clearOAuthTokens();

        // Clear business ID
        SqliteDataStore.getInstance().saveHmrcBusinessId(null);

        // Clear NINO verification status and connected NINO
        SqliteDataStore.getInstance().saveNinoVerified(false);
        SqliteDataStore.getInstance().saveConnectedNino(null);
        setNinoVerificationStatus(NinoVerificationStatus.NOT_VERIFIED);

        // Update UI
        updateHmrcConnectionStatus();
        showInfo("Disconnected", "HMRC connection has been removed. You can reconnect at any time.");
    }

    private void updateHmrcConnectionStatus() {
        if (hmrcConnectionStatusLabel == null) {
            return;
        }

        HmrcConnectionService.ConnectionState state = HmrcConnectionService.getInstance().getConnectionState();
        String nino = SqliteDataStore.getInstance().loadNino();
        String businessId = SqliteDataStore.getInstance().loadHmrcBusinessId();
        boolean ninoVerified = SqliteDataStore.getInstance().isNinoVerified();

        boolean hasNino = nino != null && !nino.isBlank();
        boolean hasCredentials = SqliteDataStore.getInstance().hasHmrcCredentials();
        boolean hasOAuth = state == HmrcConnectionService.ConnectionState.CONNECTED ||
                           state == HmrcConnectionService.ConnectionState.PROFILE_SYNCED ||
                           state == HmrcConnectionService.ConnectionState.READY_TO_SUBMIT ||
                           state == HmrcConnectionService.ConnectionState.NEEDS_VERIFICATION;
        boolean hasProfile = businessId != null && !businessId.isBlank();

        // Check if NINO has changed since connection
        boolean ninoChanged = hasNinoChangedSinceConnection();

        // Update in-memory verification status from persisted state
        if (hasProfile && ninoChanged) {
            this.ninoVerificationStatus = NinoVerificationStatus.NINO_CHANGED;
        } else if (hasProfile && ninoVerified) {
            this.ninoVerificationStatus = NinoVerificationStatus.VERIFIED;
        } else if (hasProfile && !ninoVerified) {
            this.ninoVerificationStatus = NinoVerificationStatus.SANDBOX_FALLBACK;
        }

        // Determine environment label for status display
        String envLabel = SqliteDataStore.getInstance().isSandboxEnvironment() ? "Sandbox" : "Production";

        // Update status text and icon - differentiate between verified, unverified, and changed
        if (hasNino && hasOAuth && hasProfile && ninoChanged) {
            // NINO changed since connection - AMBER WARNING
            hmrcConnectionStatusLabel.setText("Connected (" + envLabel + ") - NINO changed");
            if (hmrcStatusIcon != null) {
                hmrcStatusIcon.setIconLiteral("fas-exclamation-triangle");
                hmrcStatusIcon.setIconColor(Color.web("#f59e0b")); // Amber
            }
            if (hmrcSetupButton != null) {
                hmrcSetupButton.setText("Reconnect");
            }
        } else if (hasNino && hasOAuth && hasProfile && ninoVerified) {
            // Fully verified - GREEN
            hmrcConnectionStatusLabel.setText("Setup complete - Ready for submissions");
            if (hmrcStatusIcon != null) {
                hmrcStatusIcon.setIconLiteral("fas-check-circle");
                hmrcStatusIcon.setIconColor(Color.web("#22c55e")); // Green
            }
            if (hmrcSetupButton != null) {
                hmrcSetupButton.setText("Reconnect");
            }
        } else if (hasNino && hasOAuth && hasProfile && !ninoVerified) {
            // Connected but NINO NOT verified (sandbox fallback) - AMBER
            hmrcConnectionStatusLabel.setText("Connected (" + envLabel + ") - NINO not verified");
            if (hmrcStatusIcon != null) {
                hmrcStatusIcon.setIconLiteral("fas-exclamation-triangle");
                hmrcStatusIcon.setIconColor(Color.web("#f59e0b")); // Amber
            }
            if (hmrcSetupButton != null) {
                hmrcSetupButton.setText("Reconnect");
            }
        } else if (hasNino && hasOAuth) {
            hmrcConnectionStatusLabel.setText("Connected (profile sync pending)");
            if (hmrcStatusIcon != null) {
                hmrcStatusIcon.setIconLiteral("fas-exclamation-circle");
                hmrcStatusIcon.setIconColor(Color.web("#f59e0b")); // Amber
            }
            if (hmrcSetupButton != null) {
                hmrcSetupButton.setText("Sync Profile");
            }
        } else if (hasNino && !hasCredentials) {
            hmrcConnectionStatusLabel.setText("Enter your HMRC Client ID and Secret above to connect");
            if (hmrcStatusIcon != null) {
                hmrcStatusIcon.setIconLiteral("fas-key");
                hmrcStatusIcon.setIconColor(Color.web("#3b82f6")); // Blue
            }
            if (hmrcSetupButton != null) {
                hmrcSetupButton.setText("Connect & Verify");
            }
        } else if (hasNino) {
            hmrcConnectionStatusLabel.setText("NINO saved - Click to connect to HMRC");
            if (hmrcStatusIcon != null) {
                hmrcStatusIcon.setIconLiteral("fas-link");
                hmrcStatusIcon.setIconColor(Color.web("#3b82f6")); // Blue
            }
            if (hmrcSetupButton != null) {
                hmrcSetupButton.setText("Connect & Verify");
            }
        } else {
            hmrcConnectionStatusLabel.setText("Not set up - Save NINO first");
            if (hmrcStatusIcon != null) {
                hmrcStatusIcon.setIconLiteral("fas-times-circle");
                hmrcStatusIcon.setIconColor(Color.web("#ef4444")); // Red
            }
            if (hmrcSetupButton != null) {
                hmrcSetupButton.setText("Connect & Verify");
                hmrcSetupButton.setDisable(true);
            }
        }

        // Update checklist visibility
        if (hmrcSetupChecklist != null) {
            hmrcSetupChecklist.setVisible(true);
            hmrcSetupChecklist.setManaged(true);
        }

        // Update checklist icons - profile shows amber if unverified or NINO changed
        updateChecklistIcon(checkNino, hasNino);
        updateChecklistIcon(checkOAuth, hasOAuth);
        updateChecklistIconWithWarning(checkProfile, hasProfile, ninoVerified && !ninoChanged);

        // Show/hide disconnect button
        if (hmrcDisconnectButton != null) {
            boolean showDisconnect = hasOAuth;
            hmrcDisconnectButton.setVisible(showDisconnect);
            hmrcDisconnectButton.setManaged(showDisconnect);
        }

        // Update setup instructions
        if (hmrcSetupInstructions != null) {
            if (hasNino && hasOAuth && hasProfile && ninoChanged) {
                hmrcSetupInstructions.setText("⚠ Your NINO has changed since last connection. " +
                        "Sandbox mode cannot verify if your new NINO is correct. Click Reconnect to confirm the change.");
            } else if (hasNino && hasOAuth && hasProfile && ninoVerified) {
                hmrcSetupInstructions.setText("Your HMRC connection is set up and ready for MTD submissions.");
            } else if (hasNino && hasOAuth && hasProfile && !ninoVerified) {
                hmrcSetupInstructions.setText("⚠ Connected to sandbox but your NINO was not verified. " +
                        "Ensure you use your real NINO in production.");
            } else if (!hasNino) {
                hmrcSetupInstructions.setText("Save your National Insurance Number (NINO) above, then connect to HMRC.");
            } else if (!hasOAuth) {
                hmrcSetupInstructions.setText("Click 'Connect & Verify' to authenticate with HMRC via Government Gateway.");
            } else {
                hmrcSetupInstructions.setText("Connection established. Your business profile will sync on next submission.");
            }
        }

        // Credentials cleared while a session is still active: the tokens work until they need a
        // refresh, which the missing credentials cannot do. Say so plainly instead of leaving a green
        // "ready" status above a disabled button.
        if (hasOAuth && !hasCredentials) {
            hmrcConnectionStatusLabel.setText("Re-enter your HMRC Client ID and Secret to keep this connection working");
            if (hmrcStatusIcon != null) {
                hmrcStatusIcon.setIconLiteral("fas-exclamation-circle");
                hmrcStatusIcon.setIconColor(Color.web("#f59e0b")); // Amber
            }
            if (hmrcSetupInstructions != null) {
                hmrcSetupInstructions.setText("Your HMRC credentials were removed. Re-enter your Client ID "
                    + "and Secret above so your session can be renewed when it expires.");
            }
        }

        // Single authority for the button's enabled state. It stays disabled while an HMRC operation
        // (a reconnect or a credential self-test) is in flight, so a status refresh triggered mid-flight
        // cannot re-enable it and let the user start a second, concurrent flow. Otherwise it is enabled
        // only when both a NINO and saved credentials are present, which the OAuth flow requires.
        if (hmrcSetupButton != null) {
            hmrcSetupButton.setDisable(hmrcOperationInProgress || !(hasNino && hasCredentials));
        }
    }

    private void updateChecklistIcon(FontIcon icon, boolean complete) {
        if (icon != null) {
            if (complete) {
                icon.setIconLiteral("fas-check-circle");
                icon.setIconColor(Color.web("#22c55e")); // Green
            } else {
                icon.setIconLiteral("fas-circle");
                icon.setIconColor(Color.web("#d1d5db")); // Gray
            }
        }
    }

    /**
     * Updates a checklist icon with optional warning state.
     * Used for the profile checklist item to show amber when NINO is unverified.
     *
     * @param icon the FontIcon to update
     * @param complete true if the item is complete
     * @param verified true if the item is verified (green), false shows warning (amber)
     */
    private void updateChecklistIconWithWarning(FontIcon icon, boolean complete, boolean verified) {
        if (icon != null) {
            if (complete && verified) {
                // Complete and verified - GREEN check
                icon.setIconLiteral("fas-check-circle");
                icon.setIconColor(Color.web("#22c55e")); // Green
            } else if (complete && !verified) {
                // Complete but NOT verified (sandbox fallback) - AMBER warning
                icon.setIconLiteral("fas-exclamation-triangle");
                icon.setIconColor(Color.web("#f59e0b")); // Amber
            } else {
                // Not complete - GRAY circle
                icon.setIconLiteral("fas-circle");
                icon.setIconColor(Color.web("#d1d5db")); // Gray
            }
        }
    }

    @FXML
    void handleShowTerms(ActionEvent event) {
        showLegalDocument("/fxml/terms-of-service.fxml", "Terms of Service", true);
    }

    @FXML
    void handleShowPrivacy(ActionEvent event) {
        showLegalDocument("/fxml/privacy-notice.fxml", "Privacy Notice", true);
    }

    @FXML
    void handleShowDisclaimer(ActionEvent event) {
        AppDialog.info("Disclaimer - UK Self-Employment Manager",
                Disclaimers.CONSUMER_RIGHTS_TITLE + "\n\n" +
                Disclaimers.CONSUMER_RIGHTS_PARAGRAPH_1 + "\n\n" +
                Disclaimers.CONSUMER_RIGHTS_PARAGRAPH_2 + "\n\n" +
                Disclaimers.CONSUMER_RIGHTS_RECOMMENDATIONS_HEADER + "\n" +
                "  - " + Disclaimers.CONSUMER_RIGHTS_RECOMMENDATION_1 + "\n" +
                "  - " + Disclaimers.CONSUMER_RIGHTS_RECOMMENDATION_2 + "\n" +
                "  - " + Disclaimers.CONSUMER_RIGHTS_RECOMMENDATION_3 + "\n\n" +
                Disclaimers.PDF_CONFIRMATION_DISCLAIMER + "\n\n" +
                Disclaimers.CONSUMER_RIGHTS_ACKNOWLEDGMENT);
    }

    // === About Section ===

    private void initAboutSection() {
        if (versionLabel != null) {
            versionLabel.setText("Version " + VersionInfo.getVersion());
        }
        if (buildDateLabel != null) {
            buildDateLabel.setText("Built: " + VersionInfo.getBuildTimestamp());
        }
        if (licenseLabel != null) {
            licenseLabel.setText("License: " + VersionInfo.getLicense());
        }
        if (githubLabel != null) {
            String url = VersionInfo.getGitHubUrl();
            githubLabel.setText(url);
            githubLabel.setOnMouseClicked(e -> {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Failed to open GitHub URL", ex);
                }
            });
        }
    }

    @FXML
    void handleExportData(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Data");
        fileChooser.setInitialFileName("self-employment-data.json");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("JSON Files", "*.json"),
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = null;
        if (exportButton != null && exportButton.getScene() != null) {
            file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        }

        if (file != null) {
            try {
                DataExportService exportService = CoreServiceFactory.getDataExportService();
                UUID businessId = CoreServiceFactory.getDefaultBusinessId();
                TaxYear[] taxYears = taxYear != null
                    ? new TaxYear[]{taxYear}
                    : new TaxYear[]{TaxYear.current()};

                ExportResult result;
                String fileName = file.getName().toLowerCase();

                if (fileName.endsWith(".json")) {
                    result = exportService.exportToJson(businessId, taxYears, file.toPath());
                } else if (fileName.endsWith(".csv")) {
                    // For CSV, export income by default
                    result = exportService.exportIncomeToCsv(businessId, taxYears, file.toPath());
                } else {
                    showError("Export Error", "Unsupported file format. Please use .json or .csv");
                    return;
                }

                if (result.success()) {
                    showInfo("Export Successful",
                        String.format("Exported %d income and %d expense records to:\n%s",
                            result.incomeCount(), result.expenseCount(), file.getAbsolutePath()));
                } else {
                    showError("Export Failed", result.errorMessage());
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Export failed", e);
                showError("Export Error", "Failed to export data: " + e.getMessage());
            }
        }
    }

    @FXML
    void handleImportData(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Data");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("JSON Files", "*.json"),
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = null;
        if (importButton != null && importButton.getScene() != null) {
            file = fileChooser.showOpenDialog(importButton.getScene().getWindow());
        }

        if (file != null) {
            try {
                DataImportService importService = CoreServiceFactory.getDataImportService();
                UUID businessId = CoreServiceFactory.getDefaultBusinessId();
                Path filePath = file.toPath();
                String fileName = file.getName().toLowerCase();

                // Step 1: Parse and validate the file
                List<Income> importedIncomes;
                List<Expense> importedExpenses;

                if (fileName.endsWith(".json")) {
                    // Preview first for validation
                    ImportPreview preview = importService.previewJsonImport(filePath);
                    if (!preview.isValid() && preview.errors() != null && !preview.errors().isEmpty()) {
                        int maxErrors = Math.min(5, preview.errors().size());
                        showError("Import Validation Failed",
                            "Found validation errors:\n" + String.join("\n", preview.errors().subList(0, maxErrors)));
                        return;
                    }
                    // Parse the JSON to get income and expense records
                    var parsedData = importService.parseJsonFile(filePath);
                    importedIncomes = parsedData.incomes();
                    importedExpenses = parsedData.expenses();
                } else if (fileName.endsWith(".csv")) {
                    // For CSV, default to income (could add dialog to choose)
                    ImportPreview preview = importService.previewCsvImport(filePath, ImportType.INCOME);
                    if (!preview.isValid() && preview.errors() != null && !preview.errors().isEmpty()) {
                        int maxErrors = Math.min(5, preview.errors().size());
                        showError("Import Validation Failed",
                            "Found validation errors:\n" + String.join("\n", preview.errors().subList(0, maxErrors)));
                        return;
                    }
                    // For CSV, we only import incomes
                    importedIncomes = importService.parseCsvFile(filePath, ImportType.INCOME);
                    importedExpenses = List.of();
                } else {
                    showError("Import Error", "Unsupported file format. Please use .json or .csv");
                    return;
                }

                // Step 2: Run duplicate detection
                TaxYear currentTaxYear = taxYear != null ? taxYear : TaxYear.current();
                UiDuplicateDetectionService dupService = CoreServiceFactory.getDuplicateDetectionService();

                List<ImportCandidateViewModel> candidates = new java.util.ArrayList<>();
                candidates.addAll(dupService.analyzeIncomes(importedIncomes, currentTaxYear));
                candidates.addAll(dupService.analyzeExpenses(importedExpenses, currentTaxYear));

                if (candidates.isEmpty()) {
                    showInfo("Import Empty", "No records found in the file to import.");
                    return;
                }

                // Step 3: Show Import Review dialog
                showImportReviewDialog(candidates, importedIncomes, importedExpenses, businessId, filePath);

            } catch (ImportException e) {
                LOG.log(Level.WARNING, "Import validation failed", e);
                showError("Import Error", e.getMessage());
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Import failed", e);
                showError("Import Error", "Failed to import data: " + e.getMessage());
            }
        }
    }

    /**
     * Shows the Import Review dialog for user to review duplicate detection results.
     * BUG-10B-002: Integration fix - Settings import now uses Import Review UI.
     */
    private void showImportReviewDialog(List<ImportCandidateViewModel> candidates,
                                         List<Income> importedIncomes,
                                         List<Expense> importedExpenses,
                                         UUID businessId,
                                         Path filePath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/import-review.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Import Review - UK Self-Employment Manager");
            stage.initModality(Modality.APPLICATION_MODAL);

            ImportReviewController controller = loader.getController();
            controller.setCandidates(candidates);

            // Hide the "Back to Mapping" button since there's no mapping step in Settings import
            controller.hideBackButton();

            // Set callbacks for import completion
            controller.setOnImportComplete(approvedCandidates -> {
                executeApprovedImport(approvedCandidates, importedIncomes, importedExpenses, businessId);
                stage.close();
            });

            controller.setOnCancel(() -> {
                LOG.info("Import cancelled by user");
                stage.close();
            });

            // Set dialog size
            double width = 1000;
            double height = 700;

            Scene scene = new Scene(root, width, height);
            var mainCss = getClass().getResource("/css/main.css");
            var importReviewCss = getClass().getResource("/css/import-review.css");
            var bankImportCss = getClass().getResource("/css/bank-import.css");
            if (mainCss != null) {
                scene.getStylesheets().add(mainCss.toExternalForm());
            }
            if (importReviewCss != null) {
                scene.getStylesheets().add(importReviewCss.toExternalForm());
            }
            if (bankImportCss != null) {
                scene.getStylesheets().add(bankImportCss.toExternalForm());
            }

            stage.setScene(scene);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.setResizable(true);
            stage.showAndWait();

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to load Import Review dialog", e);
            showError("Error", "Failed to open Import Review: " + e.getMessage());
        }
    }

    /**
     * Executes the import for approved candidates only.
     * BUG-10B-002: Only imports records that user approved in the review.
     */
    private void executeApprovedImport(List<ImportCandidateViewModel> approvedCandidates,
                                        List<Income> importedIncomes,
                                        List<Expense> importedExpenses,
                                        UUID businessId) {
        try {
            var incomeService = CoreServiceFactory.getIncomeService();
            var expenseService = CoreServiceFactory.getExpenseService();

            // Build maps for quick lookup
            Map<UUID, Income> incomeMap = new java.util.HashMap<>();
            for (Income income : importedIncomes) {
                incomeMap.put(income.id(), income);
            }
            Map<UUID, Expense> expenseMap = new java.util.HashMap<>();
            for (Expense expense : importedExpenses) {
                expenseMap.put(expense.id(), expense);
            }

            int imported = 0;
            int skipped = 0;
            int updated = 0;

            for (ImportCandidateViewModel candidate : approvedCandidates) {
                ImportAction action = candidate.getAction();

                if (action == ImportAction.SKIP) {
                    skipped++;
                    continue;
                }

                if (candidate.isIncome()) {
                    Income income = incomeMap.get(candidate.getId());
                    if (income != null) {
                        if (action == ImportAction.IMPORT) {
                            incomeService.create(businessId, income.date(), income.amount(),
                                income.description(), income.category(), income.reference());
                            imported++;
                        } else if (action == ImportAction.UPDATE && candidate.getMatchedRecordId() != null) {
                            // Update existing record
                            incomeService.update(candidate.getMatchedRecordId(), income.date(), income.amount(),
                                income.description(), income.category(), income.reference());
                            updated++;
                        }
                    }
                } else {
                    Expense expense = expenseMap.get(candidate.getId());
                    if (expense != null) {
                        if (action == ImportAction.IMPORT) {
                            expenseService.create(businessId, expense.date(), expense.amount(),
                                expense.description(), expense.category(), expense.receiptPath(), expense.notes());
                            imported++;
                        } else if (action == ImportAction.UPDATE && candidate.getMatchedRecordId() != null) {
                            // Update existing record
                            expenseService.update(candidate.getMatchedRecordId(), expense.date(), expense.amount(),
                                expense.description(), expense.category(), expense.receiptPath(), expense.notes());
                            updated++;
                        }
                    }
                }
            }

            LOG.info("Import completed: " + imported + " imported, " + updated + " updated, " + skipped + " skipped");
            showInfo("Import Successful",
                String.format("Import completed successfully.\nImported: %d\nUpdated: %d\nSkipped: %d",
                    imported, updated, skipped));

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to execute approved import", e);
            showError("Import Error", "Failed to import approved records: " + e.getMessage());
        }
    }

    private void showLegalDocument(String fxmlPath, String title, boolean settingsMode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title + " - UK Self-Employment Manager");
            stage.initModality(Modality.APPLICATION_MODAL);
            Window owner = getOwnerWindow();
            if (owner != null) stage.initOwner(owner);

            // Initialize the controller with required dependencies and set settings mode
            Object controller = loader.getController();

            // Determine dialog size based on document type
            double width;
            double height;

            if (controller instanceof TermsOfServiceController tos) {
                // Initialize with service and set settings mode
                TermsAcceptanceService termsService = CoreServiceFactory.getTermsAcceptanceService();
                tos.initializeWithDependencies(termsService);
                tos.setDialogStage(stage);
                tos.setSettingsMode(settingsMode);
                // Terms of Service needs wide view for table of contents + content (nearly full screen)
                width = 1200;
                height = 1200;
            } else if (controller instanceof PrivacyNoticeController privacy) {
                // Initialize with service and set settings mode
                PrivacyAcknowledgmentService privacyService = CoreServiceFactory.getPrivacyAcknowledgmentService();
                privacy.initializeWithDependencies(privacyService);
                privacy.setDialogStage(stage);
                privacy.setSettingsMode(settingsMode);
                // Privacy Notice modal dialog - matching app height
                width = 550;
                height = 1200;
            } else {
                // Default fallback
                width = 650;
                height = 750;
            }

            Scene scene = new Scene(root, width, height);
            // Load stylesheets
            var mainCss = getClass().getResource("/css/main.css");
            var legalCss = getClass().getResource("/css/legal.css");
            if (mainCss != null) {
                scene.getStylesheets().add(mainCss.toExternalForm());
            }
            if (legalCss != null) {
                scene.getStylesheets().add(legalCss.toExternalForm());
            }

            stage.setScene(scene);

            // Bound the window to the visible screen so a tall document never pushes its footer
            // (and the Close button) off the bottom edge on smaller displays.
            Rectangle2D visual = DialogBounds.visualBoundsForOwner(owner);
            double boundedWidth = Math.min(width, visual.getWidth() - 40);
            double boundedHeight = Math.min(height, visual.getHeight() - 60);
            stage.setWidth(boundedWidth);
            stage.setHeight(boundedHeight);
            stage.setMinWidth(Math.min(width * 0.8, boundedWidth));
            stage.setMinHeight(Math.min(height * 0.8, boundedHeight));
            stage.setResizable(true);
            stage.centerOnScreen();

            stage.showAndWait();

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to load legal document: " + fxmlPath, e);
            showError("Error", "Failed to open " + title + ": " + e.getMessage());
        }
    }

    private Window getOwnerWindow() {
        if (displayNameLabel != null && displayNameLabel.getScene() != null) {
            return displayNameLabel.getScene().getWindow();
        }
        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .findFirst()
                .orElse(null);
    }

    private void showInfo(String title, String content) {
        AppDialog.info(title, content);
    }

    private void showWarning(String title, String content) {
        AppDialog.warning(title, content);
    }

    private void showError(String title, String content) {
        AppDialog.error(title, content);
    }
}
