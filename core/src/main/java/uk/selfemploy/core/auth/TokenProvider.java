package uk.selfemploy.core.auth;

/**
 * Abstraction for centralized OAuth token management.
 *
 * <p>Provides a simple interface for obtaining valid access tokens,
 * handling expiration and refresh automatically. Implementations
 * should never expose the raw token in logs.</p>
 *
 * <p>Architecture Decision Record: ADR-014</p>
 *
 * @see TokenProviderImpl
 */
public interface TokenProvider {

    /**
     * Returns a valid OAuth access token as a Bearer token string.
     *
     * <p>This method will:
     * <ul>
     *   <li>Return the current token if still valid</li>
     *   <li>Automatically refresh the token if expired</li>
     *   <li>Throw TokenException if no token is available or refresh fails</li>
     * </ul>
     *
     * <p>The returned string is in the format "Bearer {token}" ready for use
     * in HTTP Authorization headers.</p>
     *
     * @return Bearer token string (e.g., "Bearer abc123...")
     * @throws TokenException if token is unavailable or refresh fails
     */
    String getValidToken() throws TokenException;

    /**
     * Invalidates the current token, forcing a refresh on next access.
     *
     * <p>This should be called when receiving a 401 Unauthorized response
     * from the HMRC API, indicating the token may have been revoked.</p>
     */
    void invalidateToken();

    /**
     * Checks if a token is currently available (may be expired).
     *
     * @return true if any token is stored
     */
    boolean hasToken();

    /**
     * Checks if the current token is valid and not expired.
     *
     * @return true if token exists and is not expired
     */
    boolean isTokenValid();
}
