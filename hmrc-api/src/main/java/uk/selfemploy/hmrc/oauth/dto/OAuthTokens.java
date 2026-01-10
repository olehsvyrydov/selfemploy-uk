package uk.selfemploy.hmrc.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Container for OAuth2 tokens from HMRC.
 * Access token expires after 4 hours, refresh token after 18 months.
 */
public record OAuthTokens(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("expires_in") long expiresIn,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("scope") String scope,
    @JsonIgnore Instant issuedAt
) {
    /**
     * Creates tokens with current timestamp as issued time.
     */
    public static OAuthTokens create(String accessToken, String refreshToken, long expiresIn,
                                      String tokenType, String scope) {
        return new OAuthTokens(accessToken, refreshToken, expiresIn, tokenType, scope, Instant.now());
    }

    /**
     * Creates tokens from JSON response with current timestamp.
     */
    public OAuthTokens {
        if (issuedAt == null) {
            issuedAt = Instant.now();
        }
    }

    /**
     * Checks if access token has expired.
     * Returns true if token expires within 5 minutes (buffer for safety).
     */
    @JsonIgnore
    public boolean isExpired() {
        if (issuedAt == null || expiresIn <= 0) {
            return true;
        }
        Instant expiryTime = issuedAt.plusSeconds(expiresIn);
        // Add 5-minute buffer
        return Instant.now().isAfter(expiryTime.minusSeconds(300));
    }

    /**
     * Returns the expiry instant of the access token.
     */
    @JsonIgnore
    public Instant getExpiryTime() {
        if (issuedAt == null) {
            return Instant.EPOCH;
        }
        return issuedAt.plusSeconds(expiresIn);
    }

    /**
     * Returns seconds remaining until token expires.
     */
    @JsonIgnore
    public long getSecondsUntilExpiry() {
        if (issuedAt == null) {
            return 0;
        }
        Instant expiryTime = issuedAt.plusSeconds(expiresIn);
        long remaining = expiryTime.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    /**
     * Creates a new OAuthTokens instance with updated tokens after refresh.
     */
    public OAuthTokens withRefreshedTokens(String newAccessToken, String newRefreshToken, long newExpiresIn) {
        return new OAuthTokens(
            newAccessToken,
            newRefreshToken != null ? newRefreshToken : this.refreshToken,
            newExpiresIn,
            this.tokenType,
            this.scope,
            Instant.now()
        );
    }

    @Override
    public String toString() {
        // Never log actual tokens - security requirement
        return "OAuthTokens[tokenType=" + tokenType + ", scope=" + scope +
               ", expiresIn=" + expiresIn + ", expired=" + isExpired() + "]";
    }
}
