package uk.selfemploy.hmrc.oauth;

import uk.selfemploy.hmrc.config.HmrcConfig;
import uk.selfemploy.hmrc.exception.HmrcOAuthException;
import uk.selfemploy.hmrc.exception.HmrcOAuthException.OAuthError;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Service for managing OAuth2 authentication with HMRC.
 * Implements the Authorization Code flow with PKCE using a system browser
 * and localhost callback server.
 *
 * <p>Note: This class is NOT a CDI bean. It requires manual instantiation
 * through a CDI producer because of its complex constructor dependencies.
 * See OAuthProducer for CDI integration.</p>
 */
public class HmrcOAuthService {

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(HmrcOAuthService.class.getName());
    private static final int STATE_BYTES = 24; // 24 bytes = 32 chars in Base64

    private final HmrcConfig config;
    private final OAuthCallbackServer callbackServer;
    private final TokenExchangeClient tokenExchangeClient;
    private final BrowserLauncher browserLauncher;
    private final SecureRandom secureRandom;

    private final AtomicReference<OAuthTokens> currentTokens = new AtomicReference<>();
    private final AtomicReference<String> currentAuthUrl = new AtomicReference<>();
    private final AtomicBoolean authenticationInProgress = new AtomicBoolean(false);

    /** The refresh currently in flight, if any, shared by every caller that asks while it runs. */
    private final AtomicReference<CompletableFuture<OAuthTokens>> refreshInFlight = new AtomicReference<>();

    /** Notified when a refresh rotates the session, so the rotation is recorded even if callers give up. */
    private volatile Consumer<OAuthTokens> refreshListener = tokens -> { };

    public HmrcOAuthService(HmrcConfig config,
                           OAuthCallbackServer callbackServer,
                           TokenExchangeClient tokenExchangeClient,
                           BrowserLauncher browserLauncher) {
        this.config = config;
        this.callbackServer = callbackServer;
        this.tokenExchangeClient = tokenExchangeClient;
        this.browserLauncher = browserLauncher;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Initiates the OAuth2 authentication flow.
     * Opens a browser to HMRC's authorization page, waits for the callback,
     * and exchanges the authorization code for tokens.
     *
     * @return Future containing the OAuth tokens
     * @throws HmrcOAuthException if authentication fails
     */
    public CompletableFuture<OAuthTokens> authenticate() {
        validateConfiguration();

        // A single service instance owns one flow at a time. Without this guard a second call
        // (e.g. a double-clicked "Connect") would open a second browser and then tear down the
        // first, still-listening callback server, aborting the login already in progress.
        if (!authenticationInProgress.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new HmrcOAuthException(
                OAuthError.SERVER_ERROR, "An authentication flow is already in progress"));
        }

        try {
            String state = generateSecureState();
            PkceChallenge pkce = PkceChallenge.generate(secureRandom);

            String authUrl = buildAuthorizationUrl(state, pkce.challenge());
            currentAuthUrl.set(authUrl);

            LOG.info("Starting OAuth2 authentication flow");

            CompletableFuture<String> callbackFuture = callbackServer.startAndAwaitCallback(state);

            return callbackServer.listening()
                // listening() is completed on the callback server's Vert.x event loop; launching the
                // browser there could block the single loop that must serve the OAuth callback, so
                // the launch is offloaded off that thread.
                .thenComposeAsync(listening -> {
                    browserLauncher.openUrl(authUrl);
                    return callbackFuture;
                })
                .thenCompose(authCode -> tokenExchangeClient.exchangeCodeForTokens(authCode, pkce.verifier()))
                .thenApply(tokens -> {
                    LOG.info("OAuth2 authentication completed successfully");
                    setTokens(tokens);
                    return tokens;
                })
                .whenComplete((result, error) -> {
                    // Release the in-progress guard even if stop() throws, so a teardown failure
                    // cannot permanently wedge the flow into "already in progress".
                    try {
                        callbackServer.stop();
                    } finally {
                        authenticationInProgress.set(false);
                    }
                    if (error != null) {
                        LOG.severe("OAuth2 authentication failed: " + error.getMessage());
                    }
                });
        } catch (RuntimeException e) {
            authenticationInProgress.set(false);
            throw e;
        }
    }

    /**
     * Validates that required configuration is present.
     *
     * @throws HmrcOAuthException if configuration is missing
     */
    private void validateConfiguration() {
        if (config.clientId().isEmpty()) {
            throw new HmrcOAuthException(OAuthError.CONFIGURATION_ERROR, "Client ID not configured");
        }
        if (config.clientSecret().isEmpty()) {
            throw new HmrcOAuthException(OAuthError.CONFIGURATION_ERROR, "Client Secret not configured");
        }
    }

    /**
     * Builds the HMRC authorization URL with required parameters.
     *
     * @param state         The state parameter for CSRF protection
     * @param codeChallenge The PKCE S256 challenge (RFC 7636)
     * @return The complete authorization URL
     */
    public String buildAuthorizationUrl(String state, String codeChallenge) {
        StringBuilder url = new StringBuilder(config.authorizeUrl());
        url.append("?");
        url.append("client_id=").append(encode(config.clientId().orElse("")));
        url.append("&response_type=code");
        url.append("&redirect_uri=").append(encode(config.getRedirectUri()));
        url.append("&scope=").append(encode(String.join(" ", config.scopes())));
        url.append("&state=").append(encode(state));
        url.append("&code_challenge=").append(encode(codeChallenge));
        url.append("&code_challenge_method=").append(PkceChallenge.METHOD);

        return url.toString();
    }

    /**
     * Generates a cryptographically secure state parameter.
     * Uses URL-safe Base64 encoding without padding.
     *
     * @return A random URL-safe state string
     */
    public String generateSecureState() {
        byte[] bytes = new byte[STATE_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Checks if the user is currently connected (has valid tokens).
     *
     * @return true if connected and tokens are not expired
     */
    public boolean isConnected() {
        OAuthTokens tokens = currentTokens.get();
        return tokens != null && !tokens.isExpired();
    }

    /**
     * Gets the current access token if available and not expired.
     *
     * @return The current tokens, or null if not authenticated
     */
    public OAuthTokens getCurrentTokens() {
        return currentTokens.get();
    }

    /**
     * Refreshes the current access token using the refresh token.
     *
     * <p>Absence of a refresh token fails with {@link OAuthError#NO_REFRESH_TOKEN} rather than
     * {@link OAuthError#INVALID_GRANT}: no request reaches HMRC, so nothing has been rejected, and a
     * caller must not read this as a dead credential and discard the session over it.
     *
     * <p>HMRC usually rotates the refresh token on every refresh, but is not obliged to return a new
     * one. When the response omits it, the existing refresh token remains valid and is carried
     * forward, because storing the {@code null} would silently destroy the ability to refresh again.
     *
     * <p>Concurrent callers share a single refresh rather than racing. HMRC invalidates a refresh
     * token the moment it is redeemed, so two refreshes in flight together would present the same
     * token and the loser would be told {@code invalid_grant} — a rejection the app inflicted on
     * itself, and one it cannot tell apart from a real revocation.
     *
     * @return Future containing new tokens
     * @throws HmrcOAuthException with {@link OAuthError#NO_REFRESH_TOKEN} if there is nothing to refresh
     */
    public synchronized CompletableFuture<OAuthTokens> refreshAccessToken() {
        CompletableFuture<OAuthTokens> inFlight = refreshInFlight.get();
        if (inFlight != null && !inFlight.isDone()) {
            LOG.info("A token refresh is already in flight; joining it");
            return inFlight;
        }

        OAuthTokens current = currentTokens.get();
        if (current == null || isBlank(current.refreshToken())) {
            return CompletableFuture.failedFuture(new HmrcOAuthException(OAuthError.NO_REFRESH_TOKEN));
        }

        LOG.info("Refreshing access token");
        CompletableFuture<OAuthTokens> refresh = tokenExchangeClient.refreshTokens(current.refreshToken())
            .thenApply(tokens -> {
                OAuthTokens refreshed = isBlank(tokens.refreshToken())
                    ? new OAuthTokens(tokens.accessToken(), current.refreshToken(), tokens.expiresIn(),
                        tokens.tokenType(), tokens.scope(), tokens.issuedAt())
                    : tokens;

                // Install and record the rotation only if the session we refreshed is still the
                // current one. If it was replaced or disconnected while this was in flight, the
                // result belongs to a session that no longer exists: adopting it would overwrite a
                // newer credential, and recording it would resurrect a deleted one.
                if (!currentTokens.compareAndSet(current, refreshed)) {
                    LOG.warning("The HMRC session was replaced while a refresh was in flight; "
                        + "the refreshed tokens are discarded rather than overwriting it");
                    return refreshed;
                }
                LOG.info("Access token refreshed successfully");
                // The rotation has already been installed; notifying the listener is a side effect of
                // a refresh that has succeeded. A listener that throws must not turn that success into a
                // failed future, or callers would treat a renewed session as expired and discard it.
                try {
                    refreshListener.accept(refreshed);
                } catch (RuntimeException e) {
                    LOG.log(Level.WARNING, "The refresh listener failed to record a rotated session", e);
                }
                return refreshed;
            });
        refreshInFlight.set(refresh);
        return refresh;
    }

    /**
     * Registers the listener notified when a refresh rotates the session.
     *
     * <p>The rotation is recorded from inside the refresh rather than left to whoever asked for it.
     * HMRC invalidates the old refresh token the moment it issues a new one, so a caller that gave up
     * waiting — a response arriving just past its timeout is routine — would otherwise leave the spent
     * token as the only copy on record, and the next start would be refused.
     *
     * @param listener receives the rotated session, or null to remove the current listener
     */
    public void setRefreshListener(Consumer<OAuthTokens> listener) {
        this.refreshListener = listener != null ? listener : tokens -> { };
    }

    /**
     * Whether a refresh token is effectively absent. An empty string is not a credential: presenting
     * one would earn an {@code invalid_grant} that looks exactly like a real revocation.
     */
    private static boolean isBlank(String refreshToken) {
        return refreshToken == null || refreshToken.isBlank();
    }

    /**
     * Cancels an in-progress authentication flow.
     * Stops the callback server, which completes the pending future with USER_CANCELLED,
     * causing the CompletableFuture chain to resolve with an error.
     *
     * <p>Safe to call even when no authentication is in progress.</p>
     */
    public void cancelAuthentication() {
        LOG.info("Cancelling OAuth authentication flow");
        callbackServer.stop();
    }

    /**
     * Re-opens the browser with the current authorization URL.
     * Use this if the user accidentally closed the browser during OAuth flow.
     *
     * <p>This only works if an authentication flow is currently in progress
     * (i.e., after authenticate() was called but before completion).</p>
     *
     * @return true if the browser was opened, false if no auth URL is available
     */
    public boolean reopenBrowser() {
        String authUrl = currentAuthUrl.get();
        if (authUrl == null || authUrl.isEmpty()) {
            LOG.warning("Cannot reopen browser: no authorization URL available");
            return false;
        }

        LOG.info("Re-opening browser for OAuth");
        try {
            browserLauncher.openUrl(authUrl);
            return true;
        } catch (HmrcOAuthException e) {
            LOG.warning("Failed to reopen browser: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the current authorization URL if an OAuth flow is in progress.
     *
     * @return the authorization URL, or null if no flow is in progress
     */
    public String getCurrentAuthUrl() {
        return currentAuthUrl.get();
    }

    /**
     * Disconnects from HMRC by clearing stored tokens.
     */
    public void disconnect() {
        LOG.info("Disconnecting from HMRC");
        setTokens(null);
    }

    /**
     * Sets tokens (e.g., when restoring from secure storage).
     *
     * <p>Any refresh still in flight belongs to the session being replaced, so it is disowned: a
     * later caller must not join it and adopt tokens derived from a session that no longer exists.
     *
     * @param tokens The tokens to set
     */
    public synchronized void setTokens(OAuthTokens tokens) {
        currentTokens.set(tokens);
        refreshInFlight.set(null);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
