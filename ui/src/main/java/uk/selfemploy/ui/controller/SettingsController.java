package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.service.HmrcConnectionService;
import uk.selfemploy.ui.service.OAuthServiceFactory;
import uk.selfemploy.ui.service.SqliteDataStore;
import uk.selfemploy.ui.service.UiDuplicateDetectionService;
import uk.selfemploy.ui.viewmodel.ImportAction;
import uk.selfemploy.ui.viewmodel.ImportCandidateViewModel;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

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
     * Sandbox test business ID used when HMRC sandbox API returns 404.
     * This is the standard test business ID provided by HMRC for sandbox testing.
     */
    static final String SANDBOX_FALLBACK_BUSINESS_ID = "XAIS12345678901";

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

    // HMRC Connection Setup FXML fields
    @FXML private FontIcon hmrcStatusIcon;
    @FXML private Label hmrcConnectionStatusLabel;
    @FXML private Label hmrcSetupInstructions;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadDisplayNameFromStore();
        updateDisplayNameDisplay();
        loadUtrFromStore();
        updateUtrDisplay();
        loadNinoFromStore();
        updateNinoDisplay();
        updateHmrcConnectionStatus();
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
        String connectedNino = SqliteDataStore.getInstance().loadConnectedNino();
        String currentNino = SqliteDataStore.getInstance().loadNino();

        // If no connected NINO exists, this is the first connection - no change
        if (connectedNino == null || connectedNino.isBlank()) {
            return false;
        }

        // If current NINO is not set, can't compare
        if (currentNino == null || currentNino.isBlank()) {
            return false;
        }

        // Compare case-insensitively
        return !connectedNino.equalsIgnoreCase(currentNino);
    }

    // === Sandbox Mode Detection ===

    /**
     * Checks if the given HMRC API base URL is for the sandbox environment.
     * Sandbox URLs contain "test-api" in the hostname.
     *
     * @param apiBaseUrl the HMRC API base URL
     * @return true if sandbox mode, false for production
     */
    public boolean isSandboxMode(String apiBaseUrl) {
        if (apiBaseUrl == null || apiBaseUrl.isEmpty()) {
            return false; // Default to production mode (safer)
        }
        return apiBaseUrl.toLowerCase().contains("test-api");
    }

    /**
     * Checks if the given HTTP status code indicates a server error (5xx).
     * Server errors indicate temporary issues on HMRC's side that may resolve
     * and should be treated differently from client errors (4xx).
     *
     * @param statusCode the HTTP status code to check
     * @return true if the status code is in the 5xx range (500-599)
     */
    public boolean isServerError(int statusCode) {
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * Returns the fallback business ID for sandbox mode.
     * This is the standard HMRC test business ID used when the sandbox API returns 404.
     *
     * @return the sandbox fallback business ID
     */
    public String getSandboxFallbackBusinessId() {
        return SANDBOX_FALLBACK_BUSINESS_ID;
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

    // === HMRC Connection Setup ===

    @FXML
    void handleHmrcSetup(ActionEvent event) {
        LOG.info("Starting HMRC Connection Setup");

        // Step 1: Check if NINO is saved
        String savedNino = SqliteDataStore.getInstance().loadNino();
        if (savedNino == null || savedNino.isBlank()) {
            showError("NINO Required",
                    "Please save your National Insurance Number (NINO) first before connecting to HMRC.");
            return;
        }

        // Step 2: Disable button and show progress
        if (hmrcSetupButton != null) {
            hmrcSetupButton.setDisable(true);
            hmrcSetupButton.setText("Connecting...");
        }
        if (hmrcConnectionStatusLabel != null) {
            hmrcConnectionStatusLabel.setText("Opening browser for HMRC login...");
        }

        // Step 3: Trigger OAuth
        HmrcOAuthService oAuthService = OAuthServiceFactory.getOAuthService();
        oAuthService.authenticate()
                .thenAccept(tokens -> Platform.runLater(() -> handleOAuthSuccess(tokens)))
                .exceptionally(error -> {
                    Platform.runLater(() -> handleOAuthError(error));
                    return null;
                });
    }

    private void handleOAuthSuccess(OAuthTokens tokens) {
        LOG.info("OAuth authentication successful");

        // Save tokens
        SqliteDataStore.getInstance().saveOAuthTokens(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.expiresIn(),
                tokens.tokenType(),
                tokens.scope(),
                tokens.issuedAt()
        );

        // Mark session as verified
        HmrcConnectionService.getInstance().markSessionVerified();

        // Step 4: Fetch business profile
        if (hmrcConnectionStatusLabel != null) {
            hmrcConnectionStatusLabel.setText("Fetching business profile...");
        }

        fetchBusinessProfile(tokens.accessToken());
    }

    /**
     * Fetches business profile from HMRC to get the business ID.
     */
    private void fetchBusinessProfile(String accessToken) {
        String nino = SqliteDataStore.getInstance().loadNino();
        String apiBaseUrl = System.getProperty("HMRC_API_BASE_URL", "https://test-api.service.hmrc.gov.uk");
        String url = apiBaseUrl + "/individuals/business/self-employment/" + nino;

        LOG.info("Fetching business details from: " + url);

        Thread.startVirtualThread(() -> {
            try {
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(30))
                        .build();

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(30))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Accept", "application/vnd.hmrc.2.0+json")
                        .GET()
                        .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request,
                        java.net.http.HttpResponse.BodyHandlers.ofString());

                LOG.info("Business details response: " + response.statusCode());

                if (response.statusCode() == 200) {
                    String body = response.body();
                    LOG.info("Business details: " + body);

                    // Parse business ID from response
                    // Format: {"selfEmployments":[{"businessId":"XAIS12345678901",...}]}
                    String businessId = parseBusinessIdFromResponse(body);
                    boolean isSandbox = isSandboxMode(apiBaseUrl);
                    if (businessId != null) {
                        SqliteDataStore.getInstance().saveHmrcBusinessId(businessId);
                        SqliteDataStore.getInstance().saveNinoVerified(true); // NINO verified by HMRC
                        LOG.info("Stored business ID: " + businessId);
                        Platform.runLater(() -> {
                            setNinoVerificationStatus(NinoVerificationStatus.VERIFIED);
                            String modeLabel = isSandbox ? "Sandbox" : "Production";
                            completeSetup(true, "Connected and verified (" + modeLabel + ")");
                            showInfo("HMRC Connected (" + modeLabel + ")",
                                    "Successfully connected to HMRC " + modeLabel + "!\n\n" +
                                    "Your NINO and business profile have been verified.\n" +
                                    "Business ID: " + businessId);
                        });
                    } else {
                        // Response OK but no business found - NINO may not have self-employment registered
                        LOG.warning("No self-employment business found for NINO: " + nino);
                        Platform.runLater(() -> {
                            setNinoVerificationStatus(NinoVerificationStatus.FAILED);
                            SqliteDataStore.getInstance().saveNinoVerified(false);
                            completeSetup(false, "No business found");
                            showError("NINO Verification Failed",
                                    "No self-employment business is registered with this NINO.\n\n" +
                                    "Please check:\n" +
                                    "1. Your NINO is entered correctly\n" +
                                    "2. You have registered for Self Assessment with HMRC\n" +
                                    "3. Your self-employment is registered in your Government Gateway account");
                        });
                    }
                } else if (response.statusCode() == 403 || response.statusCode() == 401) {
                    // NINO doesn't match the authenticated user
                    LOG.warning("NINO mismatch - HTTP " + response.statusCode() + ": " + response.body());
                    Platform.runLater(() -> {
                        setNinoVerificationStatus(NinoVerificationStatus.FAILED);
                        SqliteDataStore.getInstance().saveNinoVerified(false);
                        completeSetup(false, "NINO mismatch");
                        showError("NINO Verification Failed",
                                "The NINO you entered doesn't match your HMRC account.\n\n" +
                                "Please check that you entered the correct National Insurance Number.");
                    });
                } else if (response.statusCode() == 404) {
                    // NINO not found - check if sandbox mode
                    LOG.warning("NINO not found - HTTP 404: " + response.body());

                    if (isSandboxMode(apiBaseUrl)) {
                        // Sandbox mode: use fallback test business ID
                        // This is EXPECTED behavior - sandbox doesn't have real NINOs
                        String fallbackBusinessId = getSandboxFallbackBusinessId();
                        LOG.info("Sandbox mode detected - saving fallback business ID: " + fallbackBusinessId);
                        LOG.info("Note: Real NINOs cannot be verified in sandbox mode (expected behavior)");
                        SqliteDataStore.getInstance().saveHmrcBusinessId(fallbackBusinessId);

                        // Check if NINO has changed since last connection
                        String connectedNino = SqliteDataStore.getInstance().loadConnectedNino();
                        String currentNino = nino; // Current NINO being used
                        boolean isFirstConnection = (connectedNino == null || connectedNino.isBlank());
                        boolean ninoChanged = !isFirstConnection &&
                                !connectedNino.equalsIgnoreCase(currentNino);

                        if (ninoChanged) {
                            // NINO changed - warn user that sandbox cannot verify
                            LOG.warning("NINO changed from " + connectedNino + " to " + currentNino +
                                    " - sandbox cannot verify correctness");
                            SqliteDataStore.getInstance().saveNinoVerified(false);

                            // Update connected NINO to the new value so subsequent reconnects
                            // with the same NINO won't trigger the warning again
                            SqliteDataStore.getInstance().saveConnectedNino(currentNino);
                            LOG.info("Updated connected NINO to: " + currentNino);

                            // Verify the save worked
                            String savedId = SqliteDataStore.getInstance().loadHmrcBusinessId();
                            LOG.info("Verified saved HMRC business ID: " + savedId);

                            Platform.runLater(() -> {
                                setNinoVerificationStatus(NinoVerificationStatus.NINO_CHANGED);
                                completeSetup(true, "Connected (Sandbox) - NINO changed");
                                showWarning("NINO Changed",
                                        "Your NINO has changed since your last connection.\n\n" +
                                        "Previous NINO: " + formatNinoForDisplay(connectedNino) + "\n" +
                                        "Current NINO: " + formatNinoForDisplay(currentNino) + "\n\n" +
                                        "WARNING: Sandbox mode cannot verify if your new NINO is correct.\n" +
                                        "In production, your NINO will be validated against HMRC records.\n\n" +
                                        "If this change was intentional, please verify your NINO is correct.");
                            });
                        } else {
                            // First connection or same NINO - proceed normally
                            SqliteDataStore.getInstance().saveNinoVerified(true);
                            // Save the connected NINO for future change detection
                            SqliteDataStore.getInstance().saveConnectedNino(currentNino);
                            LOG.info("Saved connected NINO for change detection: " + currentNino);

                            // Verify the save worked
                            String savedId = SqliteDataStore.getInstance().loadHmrcBusinessId();
                            LOG.info("Verified saved HMRC business ID: " + savedId);

                            Platform.runLater(() -> {
                                setNinoVerificationStatus(NinoVerificationStatus.VERIFIED);
                                completeSetup(true, "Connected (Sandbox)");
                                showInfo("HMRC Sandbox Connected",
                                        "Connected to HMRC Sandbox!\n\n" +
                                        "Your OAuth authentication was successful.\n" +
                                        "Business ID: " + fallbackBusinessId + "\n\n" +
                                        "Note: Sandbox mode uses test data. Your real NINO will be " +
                                        "verified when you switch to production mode.");
                            });
                        }
                    } else {
                        // Production mode: real error - NINO not found is a problem
                        Platform.runLater(() -> {
                            setNinoVerificationStatus(NinoVerificationStatus.FAILED);
                            SqliteDataStore.getInstance().saveNinoVerified(false);
                            completeSetup(false, "NINO not found");
                            showError("NINO Verification Failed",
                                    "No self-employment record found for this NINO.\n\n" +
                                    "Make sure you have registered for Self Assessment with HMRC.");
                        });
                    }
                } else {
                    LOG.warning("Failed to fetch business details: " + response.statusCode() + " - " + response.body());
                    int statusCode = response.statusCode();
                    Platform.runLater(() -> {
                        if (isServerError(statusCode)) {
                            // 5xx server error: OAuth worked but HMRC had a temporary issue
                            setNinoVerificationStatus(NinoVerificationStatus.PROFILE_SYNC_PENDING);
                            completeSetup(true, "Connected (profile sync pending)");
                            showWarning("HMRC Partially Connected",
                                    "Connected to HMRC but business profile sync failed.\n\n" +
                                    "Your OAuth authentication was successful, but HMRC returned a " +
                                    "server error (" + statusCode + ") when fetching your profile.\n\n" +
                                    "Your profile will sync automatically on your first submission, " +
                                    "or you can try reconnecting later.");
                        } else {
                            // 4xx client error: different handling
                            setNinoVerificationStatus(NinoVerificationStatus.NOT_VERIFIED);
                            completeSetup(true, "Connected (profile sync pending)");
                            showInfo("HMRC Connected",
                                    "Connected to HMRC. Business profile will sync on first submission.\n\n" +
                                    "Note: If you're using sandbox mode, ensure test data is set up.");
                        }
                    });
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to fetch business profile", e);
                Platform.runLater(() -> {
                    setNinoVerificationStatus(NinoVerificationStatus.PROFILE_SYNC_PENDING);
                    completeSetup(true, "Connected (profile sync pending)");
                    showWarning("HMRC Partially Connected",
                            "Connected to HMRC but business profile sync failed.\n\n" +
                            "Your OAuth authentication was successful, but there was an error " +
                            "fetching your profile.\n\n" +
                            "Your profile will sync automatically on your first submission, " +
                            "or you can try reconnecting later.");
                });
            }
        });
    }

    /**
     * Parses business ID from HMRC response.
     */
    private String parseBusinessIdFromResponse(String jsonResponse) {
        try {
            // Simple JSON parsing - look for "businessId":"XAIS..."
            int idx = jsonResponse.indexOf("\"businessId\"");
            if (idx >= 0) {
                int colonIdx = jsonResponse.indexOf(":", idx);
                int quoteStart = jsonResponse.indexOf("\"", colonIdx + 1);
                int quoteEnd = jsonResponse.indexOf("\"", quoteStart + 1);
                if (quoteStart >= 0 && quoteEnd > quoteStart) {
                    String businessId = jsonResponse.substring(quoteStart + 1, quoteEnd);
                    // Validate format: X[A-Z0-9]{1}IS[0-9]{11}
                    if (businessId.matches("^X[A-Z0-9]{1}IS[0-9]{11}$")) {
                        return businessId;
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse business ID from response", e);
        }
        return null;
    }

    private void handleOAuthError(Throwable error) {
        LOG.log(Level.WARNING, "OAuth authentication failed", error);
        completeSetup(false, "Connection failed");

        String message = error.getMessage();
        if (message != null && message.contains("USER_CANCELLED")) {
            showInfo("Connection Cancelled", "HMRC connection was cancelled. You can try again when ready.");
        } else {
            showError("Connection Failed",
                    "Failed to connect to HMRC: " + (message != null ? message : "Unknown error"));
        }
    }

    private void completeSetup(boolean success, String statusText) {
        if (hmrcSetupButton != null) {
            hmrcSetupButton.setDisable(false);
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

        // Update status text and icon - differentiate between verified, unverified, and changed
        if (hasNino && hasOAuth && hasProfile && ninoChanged) {
            // NINO changed since connection - AMBER WARNING
            hmrcConnectionStatusLabel.setText("Connected (Sandbox) - NINO changed");
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
            hmrcConnectionStatusLabel.setText("Connected (Sandbox) - NINO not verified");
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

        // Re-enable button if NINO is set
        if (hmrcSetupButton != null && hasNino) {
            hmrcSetupButton.setDisable(false);
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
            // stage.initStyle(StageStyle.UNDECORATED); // No system title bar - dialog has its own header with close button

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

            // Set minimum size to prevent dialog from becoming too small
            stage.setMinWidth(width * 0.8);
            stage.setMinHeight(height * 0.8);

            // Allow resizing
            stage.setResizable(true);

            stage.showAndWait();

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to load legal document: " + fxmlPath, e);
            showError("Error", "Failed to open " + title + ": " + e.getMessage());
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
