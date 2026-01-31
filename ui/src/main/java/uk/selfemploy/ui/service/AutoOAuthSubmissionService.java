package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.core.exception.SubmissionException;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service that wraps HMRC submission with automatic OAuth handling.
 *
 * <p>This service automatically triggers OAuth authentication when:</p>
 * <ul>
 *   <li>Not connected to HMRC (no tokens)</li>
 *   <li>Session has expired</li>
 *   <li>Session needs verification</li>
 *   <li>Submission fails with 401 (automatic retry after re-auth)</li>
 * </ul>
 *
 * <p>Pre-flight validation ensures NINO is set BEFORE triggering OAuth,
 * providing a better user experience by failing fast.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * AutoOAuthSubmissionService service = new AutoOAuthSubmissionService();
 * service.setProgressCallback(progress -> {
 *     // Update UI with progress.stage() and progress.message()
 * });
 *
 * try {
 *     Submission result = service.submit(reviewData, declarationTime, hash);
 *     // Handle success
 * } catch (SubmissionException e) {
 *     // Handle error
 * }
 * }</pre>
 *
 * @since Sprint 13
 */
public class AutoOAuthSubmissionService {

    private static final Logger LOG = Logger.getLogger(AutoOAuthSubmissionService.class.getName());

    /**
     * Submission progress stages.
     */
    public enum SubmissionStage {
        /** Validating prerequisites (NINO, business ID) */
        VALIDATING("Validating submission details..."),
        /** Authenticating with HMRC via OAuth */
        AUTHENTICATING("Authenticating with HMRC..."),
        /** Submitting data to HMRC */
        SUBMITTING("Submitting to HMRC..."),
        /** Submission complete */
        COMPLETE("Submission complete"),
        /** Submission failed */
        FAILED("Submission failed");

        private final String defaultMessage;

        SubmissionStage(String defaultMessage) {
            this.defaultMessage = defaultMessage;
        }

        /**
         * Returns the default message for this stage.
         *
         * @return the default message
         */
        public String getDefaultMessage() {
            return defaultMessage;
        }
    }

    /**
     * Progress update record.
     *
     * @param stage   the current submission stage
     * @param message the progress message
     */
    public record SubmissionProgress(SubmissionStage stage, String message) {
        /**
         * Creates a progress update with the default message for the stage.
         *
         * @param stage the stage
         * @return the progress update
         */
        public static SubmissionProgress of(SubmissionStage stage) {
            return new SubmissionProgress(stage, stage.getDefaultMessage());
        }

        /**
         * Creates a progress update with a custom message.
         *
         * @param stage   the stage
         * @param message the custom message
         * @return the progress update
         */
        public static SubmissionProgress of(SubmissionStage stage, String message) {
            return new SubmissionProgress(stage, message);
        }
    }

    private final UiQuarterlySubmissionService submissionService;
    private final HmrcOAuthService oAuthService;
    private final SqliteDataStore dataStore;
    private final HmrcConnectionService connectionService;

    private Consumer<SubmissionProgress> progressCallback;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * Creates a new AutoOAuthSubmissionService with default dependencies.
     */
    public AutoOAuthSubmissionService() {
        this(
                CoreServiceFactory.getQuarterlySubmissionService(),
                OAuthServiceFactory.getOAuthService(),
                SqliteDataStore.getInstance(),
                HmrcConnectionService.getInstance()
        );
    }

    /**
     * Creates a new AutoOAuthSubmissionService with custom dependencies.
     * Visible for testing.
     *
     * @param submissionService  the underlying submission service
     * @param oAuthService       the OAuth service
     * @param dataStore          the SQLite data store
     * @param connectionService  the HMRC connection service
     */
    AutoOAuthSubmissionService(
            UiQuarterlySubmissionService submissionService,
            HmrcOAuthService oAuthService,
            SqliteDataStore dataStore,
            HmrcConnectionService connectionService) {
        this.submissionService = submissionService;
        this.oAuthService = oAuthService;
        this.dataStore = dataStore;
        this.connectionService = connectionService;
    }

    /**
     * Sets the progress callback for UI updates.
     *
     * @param callback the callback to receive progress updates
     */
    public void setProgressCallback(Consumer<SubmissionProgress> callback) {
        this.progressCallback = callback;
    }

    /**
     * Submits quarterly data to HMRC with automatic OAuth handling.
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Pre-flight validation (NINO required)</li>
     *   <li>Check connection status</li>
     *   <li>If not connected, trigger OAuth flow</li>
     *   <li>Submit to HMRC</li>
     *   <li>On 401, re-authenticate and retry once</li>
     * </ol>
     *
     * @param reviewData            the reviewed quarterly data
     * @param declarationAcceptedAt when the user accepted the declaration
     * @param declarationTextHash   SHA-256 hash of the declaration text
     * @return the Submission record on success
     * @throws SubmissionException if submission fails
     */
    public Submission submit(QuarterlyReviewData reviewData, Instant declarationAcceptedAt,
                              String declarationTextHash) {
        cancelled.set(false);

        // Phase 1: Pre-flight validation (before OAuth)
        reportProgress(SubmissionStage.VALIDATING);
        validatePrerequisites();

        // Phase 2: Ensure connected (trigger OAuth if needed)
        if (!connectionService.isConnected()) {
            ensureAuthenticated();
        }

        // Phase 3: Submit with retry on 401
        return submitWithRetry(reviewData, declarationAcceptedAt, declarationTextHash);
    }

    /**
     * Cancels the current submission/OAuth flow.
     */
    public void cancel() {
        LOG.info("Cancelling submission flow");
        cancelled.set(true);
        oAuthService.cancelAuthentication();
    }

    /**
     * Validates prerequisites before triggering OAuth.
     * Fails fast with helpful error messages.
     */
    private void validatePrerequisites() {
        // Check NINO first (required for all submissions)
        String nino = dataStore.loadNino();
        if (nino == null || nino.isBlank()) {
            throw new SubmissionException(
                    "NINO_REQUIRED: National Insurance Number (NINO) is required. " +
                    "Please set your NINO in Settings before submitting.");
        }

        // Note: We don't check business ID here because:
        // 1. It might be fetched during OAuth flow
        // 2. The underlying submission service will validate it
    }

    /**
     * Ensures the user is authenticated with HMRC.
     * Triggers OAuth flow if not connected.
     */
    private void ensureAuthenticated() {
        HmrcConnectionService.ConnectionState state = connectionService.getConnectionState();
        LOG.info("Connection state: " + state);

        if (state == HmrcConnectionService.ConnectionState.READY_TO_SUBMIT ||
            state == HmrcConnectionService.ConnectionState.PROFILE_SYNCED ||
            state == HmrcConnectionService.ConnectionState.CONNECTED) {
            LOG.info("Already connected, skipping OAuth");
            return;
        }

        // Need to authenticate
        reportProgress(SubmissionStage.AUTHENTICATING);
        performOAuth();
    }

    /**
     * Performs OAuth authentication synchronously.
     */
    private void performOAuth() {
        LOG.info("Starting OAuth authentication flow");

        try {
            CompletableFuture<OAuthTokens> future = oAuthService.authenticate();

            // Wait for OAuth to complete (blocking)
            OAuthTokens tokens = future.get(120, java.util.concurrent.TimeUnit.SECONDS);

            if (cancelled.get()) {
                throw new SubmissionException("CANCELLED: Submission cancelled by user");
            }

            // Persist tokens
            dataStore.saveOAuthTokens(
                    tokens.accessToken(),
                    tokens.refreshToken(),
                    tokens.expiresIn(),
                    tokens.tokenType(),
                    tokens.scope(),
                    tokens.issuedAt()
            );

            // Mark session as verified
            connectionService.markSessionVerified();

            LOG.info("OAuth authentication successful");

        } catch (java.util.concurrent.TimeoutException e) {
            throw new SubmissionException("TIMEOUT: OAuth authentication timed out. Please try again.");
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new SubmissionException("OAuth authentication failed: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SubmissionException("CANCELLED: OAuth was interrupted");
        } catch (SubmissionException e) {
            throw e;
        } catch (Exception e) {
            throw new SubmissionException("OAuth authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Submits to HMRC with automatic retry on 401.
     */
    private Submission submitWithRetry(QuarterlyReviewData reviewData, Instant declarationAcceptedAt,
                                        String declarationTextHash) {
        reportProgress(SubmissionStage.SUBMITTING);

        try {
            // First attempt
            Submission result = submissionService.submit(reviewData, declarationAcceptedAt, declarationTextHash);
            reportProgress(SubmissionStage.COMPLETE);
            return result;

        } catch (SubmissionException e) {
            // Check if it's a 401/session expired error
            if (isSessionExpiredError(e)) {
                LOG.info("Session expired during submission - attempting re-authentication");
                return retryAfterReauth(reviewData, declarationAcceptedAt, declarationTextHash);
            }
            throw e;
        }
    }

    /**
     * Retries submission after re-authentication.
     */
    private Submission retryAfterReauth(QuarterlyReviewData reviewData, Instant declarationAcceptedAt,
                                         String declarationTextHash) {
        // Re-authenticate
        reportProgress(SubmissionStage.AUTHENTICATING, "Re-authenticating with HMRC...");
        performOAuth();

        // Retry submission (only once)
        reportProgress(SubmissionStage.SUBMITTING, "Retrying submission...");
        try {
            Submission result = submissionService.submit(reviewData, declarationAcceptedAt, declarationTextHash);
            reportProgress(SubmissionStage.COMPLETE);
            return result;

        } catch (SubmissionException e) {
            reportProgress(SubmissionStage.FAILED, e.getMessage());
            throw e;
        }
    }

    /**
     * Checks if the exception indicates a session expired error (401).
     */
    private boolean isSessionExpiredError(SubmissionException e) {
        String message = e.getMessage();
        return message != null && (
                message.contains("SESSION_EXPIRED") ||
                message.contains("401") ||
                message.contains("TOKEN_EXPIRED") ||
                message.contains("NOT_CONNECTED")
        );
    }

    /**
     * Reports progress to the callback if set.
     */
    private void reportProgress(SubmissionStage stage) {
        reportProgress(stage, stage.getDefaultMessage());
    }

    /**
     * Reports progress to the callback if set.
     */
    private void reportProgress(SubmissionStage stage, String message) {
        if (progressCallback != null) {
            progressCallback.accept(SubmissionProgress.of(stage, message));
        }
    }
}
