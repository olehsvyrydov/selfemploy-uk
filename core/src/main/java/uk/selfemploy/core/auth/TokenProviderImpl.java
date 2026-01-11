package uk.selfemploy.core.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.exception.HmrcOAuthException;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.hmrc.oauth.storage.TokenStorageService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of TokenProvider that manages OAuth tokens with automatic refresh.
 *
 * <p>This implementation:
 * <ul>
 *   <li>Retrieves tokens from secure storage</li>
 *   <li>Automatically refreshes expired tokens</li>
 *   <li>Provides user-friendly error messages</li>
 *   <li>Never logs raw token values (security requirement)</li>
 * </ul>
 *
 * <p>Architecture Decision Record: ADR-014</p>
 */
@ApplicationScoped
public class TokenProviderImpl implements TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(TokenProviderImpl.class);

    private final TokenStorageService tokenStorageService;
    private final HmrcOAuthService oAuthService;

    /**
     * Flag indicating the current token has been invalidated and needs refresh.
     * This is set when a 401 response is received from the API.
     */
    private final AtomicBoolean tokenInvalidated = new AtomicBoolean(false);

    @Inject
    public TokenProviderImpl(TokenStorageService tokenStorageService,
                             HmrcOAuthService oAuthService) {
        this.tokenStorageService = tokenStorageService;
        this.oAuthService = oAuthService;
    }

    @Override
    public String getValidToken() throws TokenException {
        log.debug("Requesting valid OAuth token");

        // Load tokens from storage
        Optional<OAuthTokens> storedTokens = tokenStorageService.loadTokens();

        if (storedTokens.isEmpty()) {
            log.warn("No OAuth tokens found in storage");
            throw new TokenException(TokenException.TokenError.NO_TOKEN);
        }

        OAuthTokens tokens = storedTokens.get();

        // Check if token is expired or has been invalidated (e.g., after 401 response)
        if (tokens.isExpired() || tokenInvalidated.get()) {
            log.info("Token is {} - attempting refresh",
                    tokenInvalidated.get() ? "invalidated" : "expired");
            tokens = refreshToken(tokens);
            tokenInvalidated.set(false);
        }

        // Return as Bearer token format
        log.debug("Returning valid token (expires in {} seconds)",
                tokens.getSecondsUntilExpiry());
        return "Bearer " + tokens.accessToken();
    }

    @Override
    public void invalidateToken() {
        log.info("Token invalidated - will refresh on next access");
        tokenInvalidated.set(true);
    }

    @Override
    public boolean hasToken() {
        return tokenStorageService.hasStoredTokens();
    }

    @Override
    public boolean isTokenValid() {
        Optional<OAuthTokens> tokens = tokenStorageService.loadTokens();
        return tokens.isPresent() && !tokens.get().isExpired();
    }

    /**
     * Refreshes the access token using the refresh token.
     *
     * @param currentTokens The current (expired) tokens
     * @return New valid tokens
     * @throws TokenException if refresh fails
     */
    private OAuthTokens refreshToken(OAuthTokens currentTokens) {
        log.info("Refreshing OAuth access token");

        // Set tokens in OAuth service for refresh
        oAuthService.setTokens(currentTokens);

        try {
            CompletableFuture<OAuthTokens> refreshFuture = oAuthService.refreshAccessToken();
            OAuthTokens newTokens = refreshFuture.join();

            // Save refreshed tokens to storage
            tokenStorageService.saveTokens(newTokens);
            log.info("Token refreshed successfully, new expiry in {} seconds",
                    newTokens.getSecondsUntilExpiry());

            return newTokens;

        } catch (Exception e) {
            log.error("Token refresh failed: {}", maskSensitiveInfo(e.getMessage()));
            throw mapToTokenException(e);
        }
    }

    /**
     * Maps various exceptions to appropriate TokenException types.
     */
    private TokenException mapToTokenException(Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;

        if (cause instanceof HmrcOAuthException oauthEx) {
            return switch (oauthEx.getError()) {
                case INVALID_GRANT, INVALID_STATE -> new TokenException(
                        TokenException.TokenError.REFRESH_FAILED,
                        "Refresh token is invalid or expired",
                        oauthEx);
                case INVALID_CLIENT -> new TokenException(
                        TokenException.TokenError.REFRESH_FAILED,
                        "OAuth client credentials are invalid",
                        oauthEx);
                default -> new TokenException(
                        TokenException.TokenError.REFRESH_FAILED,
                        oauthEx.getMessage(),
                        oauthEx);
            };
        }

        // Network-related errors
        if (isNetworkError(cause)) {
            return new TokenException(
                    TokenException.TokenError.NETWORK_ERROR,
                    "Network error during token refresh",
                    cause);
        }

        // Default to refresh failed
        return new TokenException(
                TokenException.TokenError.REFRESH_FAILED,
                cause.getMessage(),
                cause);
    }

    /**
     * Checks if the exception indicates a network error.
     */
    private boolean isNetworkError(Throwable e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return message.contains("connection") ||
               message.contains("timeout") ||
               message.contains("network") ||
               message.contains("unreachable") ||
               e instanceof java.net.ConnectException ||
               e instanceof java.net.SocketTimeoutException;
    }

    /**
     * Masks sensitive information in error messages.
     * Never log actual tokens or refresh tokens.
     */
    private String maskSensitiveInfo(String message) {
        if (message == null) {
            return "Unknown error";
        }
        // Mask anything that looks like a token (long alphanumeric strings)
        return message.replaceAll("[a-zA-Z0-9]{20,}", "***MASKED***");
    }
}
