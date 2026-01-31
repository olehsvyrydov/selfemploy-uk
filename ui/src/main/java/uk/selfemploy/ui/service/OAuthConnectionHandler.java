package uk.selfemploy.ui.service;

import uk.selfemploy.hmrc.exception.HmrcOAuthException;
import uk.selfemploy.hmrc.exception.HmrcOAuthException.OAuthError;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles OAuth connection flow with progress reporting.
 * Sprint 12 - SE-12-004: OAuth Connection with Progress
 *
 * <p>This class orchestrates the OAuth authentication flow, providing
 * real-time status updates via callbacks. It handles:</p>
 * <ul>
 *   <li>Starting the OAuth flow and opening the browser</li>
 *   <li>Reporting progress states (opening browser, waiting, completing)</li>
 *   <li>Handling success, error, timeout, and cancellation</li>
 *   <li>Persisting progress to the WizardProgressRepository</li>
 * </ul>
 *
 * <h2>FIN-003 Compliance</h2>
 * <p>The UI layer displays a privacy reminder before OAuth:
 * "Your data stays on your device. We only connect to HMRC to submit your tax information."</p>
 */
public class OAuthConnectionHandler {

    private static final Logger LOG = Logger.getLogger(OAuthConnectionHandler.class.getName());
    private static final int OAUTH_STEP = 4;
    private static final long STATUS_TRANSITION_DELAY_MS = 100;

    private final HmrcOAuthService oAuthService;
    private final WizardProgressRepository progressRepository;
    private final Consumer<ConnectionStatus> statusCallback;
    private final Consumer<OAuthResult> resultCallback;
    private final Clock clock;
    private final Duration timeout;

    private final AtomicBoolean connectionInProgress = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicReference<CompletableFuture<OAuthTokens>> currentFuture = new AtomicReference<>();
    private final AtomicReference<ScheduledFuture<?>> timeoutFuture = new AtomicReference<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "oauth-timeout-scheduler");
        t.setDaemon(true);
        return t;
    });

    /**
     * Connection status enum representing the OAuth flow states.
     */
    public enum ConnectionStatus {
        OPENING_BROWSER("Opening your browser..."),
        WAITING_FOR_AUTH("Waiting for HMRC authorization..."),
        COMPLETING("Completing connection..."),
        SUCCESS("Successfully connected to HMRC"),
        ERROR("Connection failed"),
        TIMEOUT("Connection timed out"),
        CANCELLED("Connection cancelled");

        private final String displayMessage;

        ConnectionStatus(String displayMessage) {
            this.displayMessage = displayMessage;
        }

        /**
         * Returns the user-friendly display message for this status.
         *
         * @return the display message
         */
        public String getDisplayMessage() {
            return displayMessage;
        }
    }

    /**
     * Result of the OAuth connection attempt.
     *
     * @param success      true if connection succeeded
     * @param errorCode    error code if failed, null on success
     * @param errorMessage human-readable error message if failed, null on success
     */
    public record OAuthResult(
        boolean success,
        String errorCode,
        String errorMessage
    ) {
        /**
         * Creates a success result.
         *
         * @return success result
         */
        public static OAuthResult ofSuccess() {
            return new OAuthResult(true, null, null);
        }

        /**
         * Creates an error result.
         *
         * @param errorCode    the error code
         * @param errorMessage the error message
         * @return error result
         */
        public static OAuthResult ofError(String errorCode, String errorMessage) {
            return new OAuthResult(false, errorCode, errorMessage);
        }

        /**
         * Creates a timeout result.
         *
         * @return timeout result
         */
        public static OAuthResult ofTimeout() {
            return new OAuthResult(false, "TIMEOUT", "Connection timed out. HMRC may be busy.");
        }

        /**
         * Creates a cancelled result.
         *
         * @return cancelled result
         */
        public static OAuthResult ofCancelled() {
            return new OAuthResult(false, "USER_CANCELLED", "Connection was cancelled by user.");
        }
    }

    /**
     * Creates a new OAuthConnectionHandler.
     *
     * @param oAuthService       the HMRC OAuth service
     * @param progressRepository repository for persisting wizard progress
     * @param statusCallback     callback for status updates (called on background thread)
     * @param resultCallback     callback for final result (called on background thread)
     * @param clock              clock for timestamps
     * @param timeout            timeout duration for the OAuth flow
     */
    public OAuthConnectionHandler(
            HmrcOAuthService oAuthService,
            WizardProgressRepository progressRepository,
            Consumer<ConnectionStatus> statusCallback,
            Consumer<OAuthResult> resultCallback,
            Clock clock,
            Duration timeout) {
        this.oAuthService = oAuthService;
        this.progressRepository = progressRepository;
        this.statusCallback = statusCallback;
        this.resultCallback = resultCallback;
        this.clock = clock;
        this.timeout = timeout;
    }

    /**
     * Starts the OAuth connection flow.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Reports OPENING_BROWSER status</li>
     *   <li>Calls the OAuth service to start authentication</li>
     *   <li>Reports WAITING_FOR_AUTH after browser opens</li>
     *   <li>Reports COMPLETING when callback is received</li>
     *   <li>Reports SUCCESS or ERROR based on result</li>
     * </ol>
     *
     * <p>If the connection times out, TIMEOUT is reported.</p>
     * <p>If cancelled, CANCELLED is reported.</p>
     */
    public void startConnection() {
        // Prevent multiple concurrent connections
        if (!connectionInProgress.compareAndSet(false, true)) {
            LOG.warning("Connection already in progress, ignoring duplicate startConnection call");
            return;
        }

        cancelled.set(false);
        LOG.info("Starting OAuth connection flow");

        // Report opening browser status
        reportStatus(ConnectionStatus.OPENING_BROWSER);

        // Start the OAuth authentication
        CompletableFuture<OAuthTokens> future = oAuthService.authenticate();
        currentFuture.set(future);

        // Schedule timeout
        scheduleTimeout();

        // Transition to waiting status after a brief delay
        scheduler.schedule(() -> {
            if (!cancelled.get() && connectionInProgress.get()) {
                reportStatus(ConnectionStatus.WAITING_FOR_AUTH);
            }
        }, STATUS_TRANSITION_DELAY_MS, TimeUnit.MILLISECONDS);

        // Handle the result
        future.whenComplete((tokens, error) -> {
            cancelTimeout();

            if (cancelled.get()) {
                handleCancellation();
                return;
            }

            if (error != null) {
                handleError(error);
            } else {
                handleSuccess(tokens);
            }
        });
    }

    /**
     * Cancels the OAuth connection flow.
     *
     * <p>This method is safe to call at any time, including when no connection
     * is in progress.</p>
     */
    public void cancel() {
        LOG.info("Cancelling OAuth connection flow");
        cancelled.set(true);
        oAuthService.cancelAuthentication();
        cancelTimeout();
    }

    /**
     * Re-opens the browser with the current authorization URL.
     * Use this if the user accidentally closed the browser during OAuth flow.
     *
     * @return true if the browser was opened, false if no auth URL is available
     */
    public boolean reopenBrowser() {
        return oAuthService.reopenBrowser();
    }

    /**
     * Gets the current authorization URL if an OAuth flow is in progress.
     *
     * @return the authorization URL, or null if no flow is in progress
     */
    public String getAuthorizationUrl() {
        return oAuthService.getCurrentAuthUrl();
    }

    private void scheduleTimeout() {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            if (connectionInProgress.get() && !cancelled.get()) {
                LOG.warning("OAuth connection timed out after " + timeout.toSeconds() + " seconds");
                handleTimeout();
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        timeoutFuture.set(future);
    }

    private void cancelTimeout() {
        ScheduledFuture<?> future = timeoutFuture.getAndSet(null);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void handleSuccess(OAuthTokens tokens) {
        LOG.info("OAuth connection successful");
        reportStatus(ConnectionStatus.COMPLETING);

        // Persist OAuth tokens for session survival (Sprint 12)
        persistOAuthTokens(tokens);

        // Mark session as verified since we just authenticated
        HmrcConnectionService.getInstance().markSessionVerified();

        // Small delay before showing success for UX
        scheduler.schedule(() -> {
            reportStatus(ConnectionStatus.SUCCESS);
            persistProgress();
            reportResult(OAuthResult.ofSuccess());
            connectionInProgress.set(false);
        }, STATUS_TRANSITION_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Persists OAuth tokens to SqliteDataStore for session survival.
     * Sprint 12: Tokens are now persisted so they survive app restart.
     */
    private void persistOAuthTokens(OAuthTokens tokens) {
        try {
            SqliteDataStore.getInstance().saveOAuthTokens(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.expiresIn(),
                tokens.tokenType(),
                tokens.scope(),
                tokens.issuedAt()
            );
            LOG.info("OAuth tokens persisted to storage");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to persist OAuth tokens", e);
        }
    }

    private void handleError(Throwable error) {
        LOG.log(Level.WARNING, "OAuth connection failed", error);
        reportStatus(ConnectionStatus.ERROR);

        String errorCode;
        String errorMessage;

        Throwable cause = error.getCause() != null ? error.getCause() : error;

        if (cause instanceof HmrcOAuthException oauthError) {
            OAuthError oauthErrorType = oauthError.getError();
            errorCode = oauthErrorType.name();
            errorMessage = oauthError.getMessage();

            // Check if this was actually a cancellation
            if (oauthErrorType == OAuthError.USER_CANCELLED) {
                handleCancellation();
                return;
            }
        } else {
            errorCode = "UNKNOWN_ERROR";
            errorMessage = cause.getMessage();
        }

        reportResult(OAuthResult.ofError(errorCode, errorMessage));
        connectionInProgress.set(false);
    }

    private void handleTimeout() {
        reportStatus(ConnectionStatus.TIMEOUT);
        oAuthService.cancelAuthentication();
        reportResult(OAuthResult.ofTimeout());
        connectionInProgress.set(false);
    }

    private void handleCancellation() {
        LOG.info("OAuth connection was cancelled");
        reportStatus(ConnectionStatus.CANCELLED);
        reportResult(OAuthResult.ofCancelled());
        connectionInProgress.set(false);
    }

    private void persistProgress() {
        try {
            Instant now = clock.instant();
            WizardProgress progress = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                OAUTH_STEP,
                null,
                null,
                now,
                now
            );
            progressRepository.save(progress);
            LOG.info("Persisted wizard progress at step " + OAUTH_STEP);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to persist wizard progress", e);
        }
    }

    private void reportStatus(ConnectionStatus status) {
        try {
            statusCallback.accept(status);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Status callback threw exception", e);
        }
    }

    private void reportResult(OAuthResult result) {
        try {
            resultCallback.accept(result);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Result callback threw exception", e);
        }
    }
}
