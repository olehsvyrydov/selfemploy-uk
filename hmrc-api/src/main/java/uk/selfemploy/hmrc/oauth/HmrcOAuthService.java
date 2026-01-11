package uk.selfemploy.hmrc.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.config.HmrcConfig;
import uk.selfemploy.hmrc.exception.HmrcOAuthException;
import uk.selfemploy.hmrc.exception.HmrcOAuthException.OAuthError;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

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

    private static final Logger log = LoggerFactory.getLogger(HmrcOAuthService.class);
    private static final int STATE_BYTES = 24; // 24 bytes = 32 chars in Base64

    private final HmrcConfig config;
    private final OAuthCallbackServer callbackServer;
    private final TokenExchangeClient tokenExchangeClient;
    private final BrowserLauncher browserLauncher;
    private final SecureRandom secureRandom;

    private final AtomicReference<OAuthTokens> currentTokens = new AtomicReference<>();

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
        // Validate configuration
        validateConfiguration();

        // Generate secure state for CSRF protection
        String state = generateSecureState();

        // Build authorization URL
        String authUrl = buildAuthorizationUrl(state);

        log.info("Starting OAuth2 authentication flow");

        // Start callback server and await callback
        CompletableFuture<String> callbackFuture = callbackServer.startAndAwaitCallback(state);

        // Open browser with authorization URL
        try {
            browserLauncher.openUrl(authUrl);
        } catch (HmrcOAuthException e) {
            callbackServer.stop();
            return CompletableFuture.failedFuture(e);
        }

        // Process the callback and exchange code for tokens
        return callbackFuture
            .thenCompose(authCode -> {
                log.debug("Received authorization code, exchanging for tokens");
                return tokenExchangeClient.exchangeCodeForTokens(authCode);
            })
            .thenApply(tokens -> {
                log.info("OAuth2 authentication completed successfully");
                currentTokens.set(tokens);
                return tokens;
            })
            .whenComplete((result, error) -> {
                // Always stop the callback server
                callbackServer.stop();
                if (error != null) {
                    log.error("OAuth2 authentication failed", error);
                }
            });
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
     * @param state The state parameter for CSRF protection
     * @return The complete authorization URL
     */
    public String buildAuthorizationUrl(String state) {
        StringBuilder url = new StringBuilder(config.authorizeUrl());
        url.append("?");
        url.append("client_id=").append(encode(config.clientId().orElse("")));
        url.append("&response_type=code");
        url.append("&redirect_uri=").append(encode(config.getRedirectUri()));
        url.append("&scope=").append(encode(String.join(" ", config.scopes())));
        url.append("&state=").append(encode(state));

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
     * @return Future containing new tokens
     * @throws HmrcOAuthException if no refresh token is available
     */
    public CompletableFuture<OAuthTokens> refreshAccessToken() {
        OAuthTokens current = currentTokens.get();
        if (current == null || current.refreshToken() == null) {
            return CompletableFuture.failedFuture(
                new HmrcOAuthException(OAuthError.INVALID_GRANT, "No refresh token available")
            );
        }

        log.info("Refreshing access token");
        return tokenExchangeClient.refreshTokens(current.refreshToken())
            .thenApply(tokens -> {
                log.info("Access token refreshed successfully");
                currentTokens.set(tokens);
                return tokens;
            });
    }

    /**
     * Disconnects from HMRC by clearing stored tokens.
     */
    public void disconnect() {
        log.info("Disconnecting from HMRC");
        currentTokens.set(null);
    }

    /**
     * Sets tokens (e.g., when restoring from secure storage).
     *
     * @param tokens The tokens to set
     */
    public void setTokens(OAuthTokens tokens) {
        currentTokens.set(tokens);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
