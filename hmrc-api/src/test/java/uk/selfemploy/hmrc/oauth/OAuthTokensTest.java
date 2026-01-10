package uk.selfemploy.hmrc.oauth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OAuthTokens DTO.
 */
@DisplayName("OAuthTokens")
class OAuthTokensTest {

    @Nested
    @DisplayName("Token Creation")
    class TokenCreation {

        @Test
        @DisplayName("should create tokens with all fields")
        void shouldCreateTokensWithAllFields() {
            OAuthTokens tokens = OAuthTokens.create(
                "access123",
                "refresh456",
                14400,
                "Bearer",
                "read:self-assessment"
            );

            assertThat(tokens.accessToken()).isEqualTo("access123");
            assertThat(tokens.refreshToken()).isEqualTo("refresh456");
            assertThat(tokens.expiresIn()).isEqualTo(14400);
            assertThat(tokens.tokenType()).isEqualTo("Bearer");
            assertThat(tokens.scope()).isEqualTo("read:self-assessment");
            assertThat(tokens.issuedAt()).isNotNull();
        }

        @Test
        @DisplayName("should set issuedAt to current time on creation")
        void shouldSetIssuedAtToCurrentTime() {
            Instant before = Instant.now();
            OAuthTokens tokens = OAuthTokens.create("a", "r", 3600, "Bearer", "scope");
            Instant after = Instant.now();

            assertThat(tokens.issuedAt())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
        }
    }

    @Nested
    @DisplayName("Token Expiry")
    class TokenExpiry {

        @Test
        @DisplayName("should report as not expired when within validity period")
        void shouldNotBeExpiredWhenWithinValidityPeriod() {
            // Token valid for 1 hour, just created
            OAuthTokens tokens = OAuthTokens.create("a", "r", 3600, "Bearer", "scope");

            assertThat(tokens.isExpired()).isFalse();
        }

        @Test
        @DisplayName("should report as expired when past expiry time")
        void shouldBeExpiredWhenPastExpiryTime() {
            // Token with issuedAt in the past (2 hours ago), expires in 1 hour
            Instant twoHoursAgo = Instant.now().minusSeconds(7200);
            OAuthTokens tokens = new OAuthTokens("a", "r", 3600, "Bearer", "scope", twoHoursAgo);

            assertThat(tokens.isExpired()).isTrue();
        }

        @Test
        @DisplayName("should report as expired when within 5-minute buffer")
        void shouldBeExpiredWhenWithin5MinuteBuffer() {
            // Token expires in 4 minutes (within 5-minute buffer)
            Instant fourMinutesAgo = Instant.now().minusSeconds(3600 - 240);
            OAuthTokens tokens = new OAuthTokens("a", "r", 3600, "Bearer", "scope", fourMinutesAgo);

            assertThat(tokens.isExpired()).isTrue();
        }

        @Test
        @DisplayName("should report as not expired when just outside 5-minute buffer")
        void shouldNotBeExpiredWhenJustOutside5MinuteBuffer() {
            // Token expires in 6 minutes (outside 5-minute buffer)
            Instant sixMinutesTilExpiry = Instant.now().minusSeconds(3600 - 360);
            OAuthTokens tokens = new OAuthTokens("a", "r", 3600, "Bearer", "scope", sixMinutesTilExpiry);

            assertThat(tokens.isExpired()).isFalse();
        }

        @Test
        @DisplayName("should be expired when expiresIn is zero")
        void shouldBeExpiredWhenExpiresInIsZero() {
            OAuthTokens tokens = OAuthTokens.create("a", "r", 0, "Bearer", "scope");

            assertThat(tokens.isExpired()).isTrue();
        }

        @Test
        @DisplayName("should be expired when expiresIn is negative")
        void shouldBeExpiredWhenExpiresInIsNegative() {
            OAuthTokens tokens = OAuthTokens.create("a", "r", -100, "Bearer", "scope");

            assertThat(tokens.isExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("Expiry Calculations")
    class ExpiryCalculations {

        @Test
        @DisplayName("should calculate expiry time correctly")
        void shouldCalculateExpiryTimeCorrectly() {
            Instant issuedAt = Instant.parse("2026-01-10T10:00:00Z");
            OAuthTokens tokens = new OAuthTokens("a", "r", 3600, "Bearer", "scope", issuedAt);

            Instant expectedExpiry = Instant.parse("2026-01-10T11:00:00Z");
            assertThat(tokens.getExpiryTime()).isEqualTo(expectedExpiry);
        }

        @Test
        @DisplayName("should calculate seconds until expiry")
        void shouldCalculateSecondsUntilExpiry() {
            // Token issued now, valid for 1 hour
            OAuthTokens tokens = OAuthTokens.create("a", "r", 3600, "Bearer", "scope");

            // Should be close to 3600 seconds (within a few seconds tolerance)
            assertThat(tokens.getSecondsUntilExpiry())
                .isGreaterThan(3595)
                .isLessThanOrEqualTo(3600);
        }

        @Test
        @DisplayName("should return zero seconds when expired")
        void shouldReturnZeroSecondsWhenExpired() {
            Instant twoHoursAgo = Instant.now().minusSeconds(7200);
            OAuthTokens tokens = new OAuthTokens("a", "r", 3600, "Bearer", "scope", twoHoursAgo);

            assertThat(tokens.getSecondsUntilExpiry()).isZero();
        }
    }

    @Nested
    @DisplayName("Token Refresh")
    class TokenRefresh {

        @Test
        @DisplayName("should create new tokens with refreshed values")
        void shouldCreateNewTokensWithRefreshedValues() {
            OAuthTokens original = OAuthTokens.create("old_access", "old_refresh", 3600, "Bearer", "scope");

            OAuthTokens refreshed = original.withRefreshedTokens("new_access", "new_refresh", 7200);

            assertThat(refreshed.accessToken()).isEqualTo("new_access");
            assertThat(refreshed.refreshToken()).isEqualTo("new_refresh");
            assertThat(refreshed.expiresIn()).isEqualTo(7200);
            assertThat(refreshed.tokenType()).isEqualTo("Bearer");
            assertThat(refreshed.scope()).isEqualTo("scope");
            assertThat(refreshed.issuedAt()).isAfterOrEqualTo(original.issuedAt());
        }

        @Test
        @DisplayName("should keep original refresh token if new one is null")
        void shouldKeepOriginalRefreshTokenIfNewIsNull() {
            OAuthTokens original = OAuthTokens.create("old_access", "keep_this_refresh", 3600, "Bearer", "scope");

            OAuthTokens refreshed = original.withRefreshedTokens("new_access", null, 7200);

            assertThat(refreshed.refreshToken()).isEqualTo("keep_this_refresh");
        }
    }

    @Nested
    @DisplayName("toString Security")
    class ToStringSecurity {

        @Test
        @DisplayName("should not expose access token in toString")
        void shouldNotExposeAccessTokenInToString() {
            OAuthTokens tokens = OAuthTokens.create("secret_access_token_123", "r", 3600, "Bearer", "scope");

            String str = tokens.toString();

            assertThat(str).doesNotContain("secret_access_token_123");
        }

        @Test
        @DisplayName("should not expose refresh token in toString")
        void shouldNotExposeRefreshTokenInToString() {
            OAuthTokens tokens = OAuthTokens.create("a", "secret_refresh_token_456", 3600, "Bearer", "scope");

            String str = tokens.toString();

            assertThat(str).doesNotContain("secret_refresh_token_456");
        }

        @Test
        @DisplayName("should include token type and scope in toString")
        void shouldIncludeTokenTypeAndScopeInToString() {
            OAuthTokens tokens = OAuthTokens.create("a", "r", 3600, "Bearer", "read:self-assessment");

            String str = tokens.toString();

            assertThat(str).contains("Bearer");
            assertThat(str).contains("read:self-assessment");
        }
    }
}
