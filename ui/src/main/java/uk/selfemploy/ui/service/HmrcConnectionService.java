package uk.selfemploy.ui.service;

import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Single Source of Truth for HMRC connection status.
 * Provides a unified way to check connection state across all controllers.
 *
 * <p>Connection State Machine:</p>
 * <pre>
 * NOT_CONNECTED → CONNECTED → PROFILE_SYNCED → READY_TO_SUBMIT
 *       ↑              ↓            ↓               ↓
 *       └──────────────────────────────────────────┘
 *                    (Disconnect)
 * </pre>
 *
 * <h2>State Definitions</h2>
 * <ul>
 *   <li>NOT_CONNECTED - No OAuth tokens or business ID stored</li>
 *   <li>CONNECTED - OAuth tokens valid (future: token validation)</li>
 *   <li>PROFILE_SYNCED - OAuth tokens valid AND HMRC business ID stored</li>
 *   <li>READY_TO_SUBMIT - All prerequisites: tokens, business ID, NINO</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Get singleton instance
 * HmrcConnectionService service = HmrcConnectionService.getInstance();
 *
 * // Check connection state
 * if (service.isConnected()) {
 *     // Show connected UI
 * }
 *
 * // Check if ready to submit
 * if (service.isReadyToSubmit()) {
 *     // Enable submit buttons
 * }
 *
 * // Get unified status text
 * label.setText(service.getStatusText());
 * }</pre>
 *
 * @since Sprint 12
 */
public class HmrcConnectionService {

    private static final Logger LOG = Logger.getLogger(HmrcConnectionService.class.getName());

    private static HmrcConnectionService instance;

    /**
     * Connection state enum for the HMRC connection state machine.
     */
    public enum ConnectionState {
        /**
         * No OAuth tokens or business ID stored.
         * User has never connected or has disconnected.
         */
        NOT_CONNECTED,

        /**
         * Business ID exists but OAuth session has expired (token time exceeded).
         * User needs to reconnect to HMRC.
         */
        SESSION_EXPIRED,

        /**
         * Tokens exist and aren't expired by time, but haven't been verified this session.
         * After app restart, we cannot assume tokens actually work until verified.
         * User should verify or reconnect.
         */
        NEEDS_VERIFICATION,

        /**
         * OAuth tokens are valid but business profile not yet fetched.
         * This is a transient state during the connection wizard.
         */
        CONNECTED,

        /**
         * OAuth tokens valid AND HMRC business ID stored.
         * The HMRC profile has been successfully fetched and stored.
         */
        PROFILE_SYNCED,

        /**
         * All prerequisites met: tokens, business ID, AND NINO.
         * User is ready to make quarterly/annual submissions.
         */
        READY_TO_SUBMIT
    }

    /**
     * Tracks whether the session has been verified in this app run.
     * After restart, tokens may exist but we haven't confirmed they work.
     * This flag is set to true after successful OAuth or API call.
     */
    private static boolean sessionVerifiedThisRun = false;

    // Status text constants for unified terminology
    private static final String STATUS_NOT_CONNECTED = "Not connected to HMRC";
    private static final String STATUS_SESSION_EXPIRED = "Session expired";
    private static final String STATUS_NEEDS_VERIFICATION = "Connection saved";
    private static final String STATUS_CONNECTED = "Connected to HMRC";

    // Status message constants
    private static final String MESSAGE_NOT_CONNECTED =
            "Click 'Connect' to authorize with HMRC via Government Gateway";
    private static final String MESSAGE_SESSION_EXPIRED =
            "Your HMRC session has expired. Click 'Reconnect' to continue.";
    private static final String MESSAGE_NEEDS_VERIFICATION =
            "Previous session found. Click 'Verify' to check connection.";
    private static final String MESSAGE_PROFILE_SYNCED_NO_NINO =
            "Connected but NINO not set. Go to Settings to add your NINO.";
    private static final String MESSAGE_READY =
            "Your account is linked and ready to submit";

    /**
     * Creates a new HmrcConnectionService instance.
     * Use {@link #getInstance()} for singleton access.
     */
    public HmrcConnectionService() {
        // Public constructor for testing
    }

    /**
     * Gets the singleton instance of HmrcConnectionService.
     *
     * @return the singleton instance
     */
    public static synchronized HmrcConnectionService getInstance() {
        if (instance == null) {
            instance = new HmrcConnectionService();
        }
        return instance;
    }

    // === State Determination ===

    /**
     * Determines the current HMRC connection state based on stored data.
     * Sprint 12: Now checks business ID, OAuth token validity, AND session verification.
     *
     * <p>After app restart, even if tokens exist and aren't expired by time,
     * we return NEEDS_VERIFICATION until the session is explicitly verified.
     * This prevents showing "Connected" when the tokens might be invalid.
     *
     * @return the current connection state
     */
    public ConnectionState getConnectionState() {
        String businessId = SqliteDataStore.getInstance().loadHmrcBusinessId();
        boolean hasBusinessId = businessId != null && !businessId.isBlank();

        // Check if OAuth tokens exist and are valid by time
        if (!hasValidOAuthTokens()) {
            // No valid tokens or tokens expired
            if (hasBusinessId) {
                // Had connection before but tokens expired
                return ConnectionState.SESSION_EXPIRED;
            }
            return ConnectionState.NOT_CONNECTED;
        }

        // Tokens exist and aren't expired by time, but have we verified they work?
        if (!sessionVerifiedThisRun) {
            // After restart, we can't assume tokens work - need verification
            return ConnectionState.NEEDS_VERIFICATION;
        }

        // Session verified this run - check for business ID and NINO
        String nino = SqliteDataStore.getInstance().loadNino();
        boolean hasNino = nino != null && !nino.isBlank();

        if (hasBusinessId && hasNino) {
            return ConnectionState.READY_TO_SUBMIT;
        } else if (hasBusinessId) {
            return ConnectionState.PROFILE_SYNCED;
        } else {
            // OAuth verified but no business ID yet
            return ConnectionState.CONNECTED;
        }
    }

    /**
     * Checks if valid (non-expired) OAuth tokens exist.
     *
     * @return true if OAuth tokens exist and are not expired
     */
    private boolean hasValidOAuthTokens() {
        String[] tokenData = SqliteDataStore.getInstance().loadOAuthTokens();
        if (tokenData == null) {
            return false;
        }

        try {
            // Parse expiry information
            long expiresIn = Long.parseLong(tokenData[2]);
            String issuedAtStr = tokenData[5];
            if (issuedAtStr == null) {
                return false;
            }

            java.time.Instant issuedAt = java.time.Instant.parse(issuedAtStr);
            java.time.Instant expiryTime = issuedAt.plusSeconds(expiresIn);

            // Add 5-minute buffer for safety
            java.time.Instant now = java.time.Instant.now();
            boolean isValid = now.isBefore(expiryTime.minusSeconds(300));

            if (!isValid) {
                LOG.fine("OAuth tokens expired at " + expiryTime);
            }

            return isValid;
        } catch (Exception e) {
            LOG.warning("Failed to check OAuth token validity: " + e.getMessage());
            return false;
        }
    }

    // === Session Verification ===

    /**
     * Result of session verification attempt.
     */
    public enum VerificationResult {
        /** Session verified successfully - tokens are valid */
        VERIFIED,
        /** Session verification failed - tokens expired or revoked */
        EXPIRED,
        /** No tokens to verify - user needs to connect */
        NOT_CONNECTED
    }

    /**
     * Verifies the current session by attempting to refresh the OAuth tokens.
     * This is the proper way to verify that stored tokens are still valid with HMRC.
     *
     * <p>Use this method when:
     * <ul>
     *   <li>App starts and finds stored tokens (lazy verification)</li>
     *   <li>User clicks "Verify" button</li>
     *   <li>Before making an HMRC API call after restart</li>
     * </ul>
     *
     * @return CompletableFuture with verification result
     */
    public CompletableFuture<VerificationResult> verifySession() {
        // Check if we have tokens to verify
        if (!SqliteDataStore.getInstance().hasOAuthTokens()) {
            LOG.info("No OAuth tokens to verify");
            return CompletableFuture.completedFuture(VerificationResult.NOT_CONNECTED);
        }

        String businessId = SqliteDataStore.getInstance().loadHmrcBusinessId();
        if (businessId == null || businessId.isBlank()) {
            LOG.info("No business ID - not connected");
            return CompletableFuture.completedFuture(VerificationResult.NOT_CONNECTED);
        }

        LOG.info("Verifying session by refreshing OAuth tokens...");

        // Get the OAuth service and attempt refresh
        HmrcOAuthService oauthService = OAuthServiceFactory.getOAuthService();

        return oauthService.refreshAccessToken()
            .thenApply(newTokens -> {
                // Refresh succeeded - persist new tokens and mark verified
                persistTokens(newTokens);
                markSessionVerified();
                LOG.info("Session verified successfully (tokens refreshed)");
                return VerificationResult.VERIFIED;
            })
            .exceptionally(ex -> {
                // Refresh failed - session is expired/revoked
                LOG.log(Level.WARNING, "Session verification failed: " + ex.getMessage(), ex);
                // Clear invalid tokens
                SqliteDataStore.getInstance().clearOAuthTokens();
                resetSessionVerification();
                return VerificationResult.EXPIRED;
            });
    }

    /**
     * Persists OAuth tokens to storage.
     */
    private void persistTokens(OAuthTokens tokens) {
        SqliteDataStore.getInstance().saveOAuthTokens(
            tokens.accessToken(),
            tokens.refreshToken(),
            tokens.expiresIn(),
            tokens.tokenType(),
            tokens.scope(),
            tokens.issuedAt()
        );
    }

    /**
     * Marks the session as verified for this app run.
     * Call this after successful OAuth completion or successful HMRC API call.
     */
    public void markSessionVerified() {
        LOG.info("Session marked as verified for this app run");
        sessionVerifiedThisRun = true;
    }

    /**
     * Checks if the session has been verified this app run.
     *
     * @return true if verified, false if needs verification
     */
    public boolean isSessionVerified() {
        return sessionVerifiedThisRun;
    }

    /**
     * Resets the session verification flag.
     * Used for testing and when disconnecting.
     */
    public void resetSessionVerification() {
        sessionVerifiedThisRun = false;
    }

    // === Convenience Methods ===

    /**
     * Checks if the user is connected to HMRC with a VERIFIED session.
     * Returns true only for CONNECTED, PROFILE_SYNCED, and READY_TO_SUBMIT states.
     * Returns false for NOT_CONNECTED, SESSION_EXPIRED, and NEEDS_VERIFICATION.
     *
     * <p>Important: After app restart, this returns false until the session
     * is explicitly verified, even if tokens exist.
     *
     * @return true if connected with verified session, false otherwise
     */
    public boolean isConnected() {
        ConnectionState state = getConnectionState();
        return state == ConnectionState.CONNECTED
                || state == ConnectionState.PROFILE_SYNCED
                || state == ConnectionState.READY_TO_SUBMIT;
    }

    /**
     * Checks if the session has expired (business ID exists but OAuth tokens invalid by time).
     *
     * @return true if session expired by time
     */
    public boolean isSessionExpired() {
        return getConnectionState() == ConnectionState.SESSION_EXPIRED;
    }

    /**
     * Checks if session needs verification (tokens exist but not verified this run).
     *
     * @return true if needs verification
     */
    public boolean needsVerification() {
        return getConnectionState() == ConnectionState.NEEDS_VERIFICATION;
    }

    /**
     * Checks if all prerequisites are met for HMRC submissions.
     * Requires: verified OAuth tokens, business ID, and NINO.
     *
     * @return true if ready to submit, false otherwise
     */
    public boolean isReadyToSubmit() {
        return getConnectionState() == ConnectionState.READY_TO_SUBMIT;
    }

    // === Status Text Generation (Unified Terminology) ===

    /**
     * Gets the status text for display (unified across all pages).
     *
     * @return status text based on connection state
     */
    public String getStatusText() {
        return switch (getConnectionState()) {
            case NOT_CONNECTED -> STATUS_NOT_CONNECTED;
            case SESSION_EXPIRED -> STATUS_SESSION_EXPIRED;
            case NEEDS_VERIFICATION -> STATUS_NEEDS_VERIFICATION;
            case CONNECTED, PROFILE_SYNCED, READY_TO_SUBMIT -> STATUS_CONNECTED;
        };
    }

    /**
     * Gets the status message explaining the current state.
     *
     * @return a descriptive message about the connection state
     */
    public String getStatusMessage() {
        return switch (getConnectionState()) {
            case NOT_CONNECTED -> MESSAGE_NOT_CONNECTED;
            case SESSION_EXPIRED -> MESSAGE_SESSION_EXPIRED;
            case NEEDS_VERIFICATION -> MESSAGE_NEEDS_VERIFICATION;
            case CONNECTED, PROFILE_SYNCED -> MESSAGE_PROFILE_SYNCED_NO_NINO;
            case READY_TO_SUBMIT -> MESSAGE_READY;
        };
    }

    // === Business Details ===

    /**
     * Gets the HMRC-assigned business ID.
     *
     * @return the business ID (e.g., "XAIS12345678901"), or null if not connected
     */
    public String getHmrcBusinessId() {
        String businessId = SqliteDataStore.getInstance().loadHmrcBusinessId();
        return (businessId != null && !businessId.isBlank()) ? businessId : null;
    }

    /**
     * Gets the trading name associated with the HMRC business.
     *
     * @return the trading name, or null if not set
     */
    public String getTradingName() {
        return SqliteDataStore.getInstance().loadHmrcTradingName();
    }

    // === NINO Access ===

    /**
     * Gets the stored NINO.
     *
     * @return the NINO, or null if not set
     */
    public String getNino() {
        return SqliteDataStore.getInstance().loadNino();
    }

    /**
     * Gets the NINO masked for display (security).
     * Shows only the last 4 characters.
     *
     * @return masked NINO (e.g., "*****56C"), or empty string if not set
     */
    public String getMaskedNino() {
        String nino = getNino();
        if (nino == null || nino.length() < 4) {
            return "";
        }
        // Mask all but last 3 characters (e.g., AB123456C → *****56C)
        int visibleChars = 3;
        int maskLength = nino.length() - visibleChars;
        return "*".repeat(maskLength) + nino.substring(maskLength);
    }

    // === Connection Management ===

    /**
     * Disconnects from HMRC by clearing connection-related data.
     * Note: NINO is NOT cleared as it's profile data, not connection data.
     */
    public void disconnect() {
        LOG.info("Disconnecting from HMRC");

        // Clear OAuth tokens
        SqliteDataStore.getInstance().clearOAuthTokens();

        // Clear connection data
        SqliteDataStore.getInstance().saveHmrcBusinessId(null);
        SqliteDataStore.getInstance().saveHmrcTradingName(null);

        // Reset session verification
        sessionVerifiedThisRun = false;

        // Note: NINO is NOT cleared - it's profile data that persists
        LOG.info("HMRC connection data cleared (including OAuth tokens)");
    }
}
