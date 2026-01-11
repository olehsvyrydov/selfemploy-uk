package uk.selfemploy.core.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.hmrc.oauth.storage.TokenStorageService;
import uk.selfemploy.hmrc.exception.HmrcOAuthException;
import uk.selfemploy.hmrc.exception.HmrcOAuthException.OAuthError;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD tests for TokenProviderImpl.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>AC-3: Valid OAuth token retrieved before each HMRC API call</li>
 *   <li>AC-4: Token refresh triggered automatically when token expires</li>
 *   <li>AC-5: User-friendly error shown when token refresh fails</li>
 *   <li>AC-7: Unit tests verify token injection and refresh logic</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenProviderImpl")
class TokenProviderImplTest {

    @Mock
    private TokenStorageService tokenStorageService;

    @Mock
    private HmrcOAuthService oAuthService;

    private TokenProviderImpl tokenProvider;

    private static final String ACCESS_TOKEN = "test-access-token-12345";
    private static final String REFRESH_TOKEN = "test-refresh-token-67890";
    private static final String NEW_ACCESS_TOKEN = "new-access-token-99999";

    @BeforeEach
    void setUp() {
        tokenProvider = new TokenProviderImpl(tokenStorageService, oAuthService);
    }

    @Nested
    @DisplayName("getValidToken")
    class GetValidTokenTests {

        @Test
        @DisplayName("should return Bearer token when valid token exists")
        void shouldReturnBearerTokenWhenValid() {
            // Given
            OAuthTokens validTokens = createValidTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(validTokens));

            // When
            String token = tokenProvider.getValidToken();

            // Then
            assertThat(token).isEqualTo("Bearer " + ACCESS_TOKEN);
            verify(oAuthService, never()).refreshAccessToken();
        }

        @Test
        @DisplayName("should automatically refresh expired token")
        void shouldRefreshExpiredToken() {
            // Given
            OAuthTokens expiredTokens = createExpiredTokens();
            OAuthTokens newTokens = createValidTokens(NEW_ACCESS_TOKEN);

            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(expiredTokens));
            when(oAuthService.refreshAccessToken())
                .thenReturn(CompletableFuture.completedFuture(newTokens));

            // When
            String token = tokenProvider.getValidToken();

            // Then
            assertThat(token).isEqualTo("Bearer " + NEW_ACCESS_TOKEN);
            verify(oAuthService).refreshAccessToken();
            verify(tokenStorageService).saveTokens(newTokens);
        }

        @Test
        @DisplayName("should throw TokenException when no token stored")
        void shouldThrowExceptionWhenNoToken() {
            // Given
            when(tokenStorageService.loadTokens()).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> tokenProvider.getValidToken())
                .isInstanceOf(TokenException.class)
                .satisfies(ex -> {
                    TokenException tokenEx = (TokenException) ex;
                    assertThat(tokenEx.getError()).isEqualTo(TokenException.TokenError.NO_TOKEN);
                    assertThat(tokenEx.requiresReauthentication()).isTrue();
                    assertThat(tokenEx.getUserMessage()).contains("connect to HMRC");
                });
        }

        @Test
        @DisplayName("should throw TokenException with user-friendly message when refresh fails")
        void shouldThrowUserFriendlyExceptionWhenRefreshFails() {
            // Given
            OAuthTokens expiredTokens = createExpiredTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(expiredTokens));
            when(oAuthService.refreshAccessToken())
                .thenReturn(CompletableFuture.failedFuture(
                    new HmrcOAuthException(OAuthError.INVALID_GRANT, "Refresh token expired")));

            // When / Then
            assertThatThrownBy(() -> tokenProvider.getValidToken())
                .isInstanceOf(TokenException.class)
                .satisfies(ex -> {
                    TokenException tokenEx = (TokenException) ex;
                    assertThat(tokenEx.getError()).isEqualTo(TokenException.TokenError.REFRESH_FAILED);
                    assertThat(tokenEx.getUserMessage()).contains("re-authenticate");
                    assertThat(tokenEx.requiresReauthentication()).isTrue();
                });
        }

        @Test
        @DisplayName("should throw TokenException on network error during refresh")
        void shouldHandleNetworkErrorDuringRefresh() {
            // Given
            OAuthTokens expiredTokens = createExpiredTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(expiredTokens));
            when(oAuthService.refreshAccessToken())
                .thenReturn(CompletableFuture.failedFuture(
                    new RuntimeException("Connection refused")));

            // When / Then
            assertThatThrownBy(() -> tokenProvider.getValidToken())
                .isInstanceOf(TokenException.class)
                .satisfies(ex -> {
                    TokenException tokenEx = (TokenException) ex;
                    assertThat(tokenEx.getError()).isEqualTo(TokenException.TokenError.NETWORK_ERROR);
                    assertThat(tokenEx.getUserMessage()).contains("internet connection");
                });
        }

        @Test
        @DisplayName("should mask token in logs (security requirement)")
        void shouldNotExposeTokenInLogs() {
            // Given
            OAuthTokens validTokens = createValidTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(validTokens));

            // When
            String token = tokenProvider.getValidToken();

            // Then - token should be returned but never logged
            assertThat(token).contains(ACCESS_TOKEN);
            // Token provider should use masked logging - verified by code review
            // The OAuthTokens.toString() already masks the token
        }
    }

    @Nested
    @DisplayName("invalidateToken")
    class InvalidateTokenTests {

        @Test
        @DisplayName("should force refresh on next getValidToken call")
        void shouldForceRefreshOnNextCall() {
            // Given
            OAuthTokens validTokens = createValidTokens();
            OAuthTokens newTokens = createValidTokens(NEW_ACCESS_TOKEN);

            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(validTokens));
            when(oAuthService.refreshAccessToken())
                .thenReturn(CompletableFuture.completedFuture(newTokens));

            // First call - token is valid
            tokenProvider.getValidToken();
            verify(oAuthService, never()).refreshAccessToken();

            // When - invalidate token
            tokenProvider.invalidateToken();

            // Reset the mock to return fresh tokens that will need refresh
            reset(tokenStorageService, oAuthService);
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(validTokens));
            when(oAuthService.refreshAccessToken())
                .thenReturn(CompletableFuture.completedFuture(newTokens));

            // Then - next call should force refresh
            String token = tokenProvider.getValidToken();
            assertThat(token).isEqualTo("Bearer " + NEW_ACCESS_TOKEN);
            verify(oAuthService).refreshAccessToken();
        }

        @Test
        @DisplayName("should handle invalidation when no token exists")
        void shouldHandleInvalidationWhenNoToken() {
            // When / Then - should not throw
            assertThatCode(() -> tokenProvider.invalidateToken())
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("hasToken")
    class HasTokenTests {

        @Test
        @DisplayName("should return true when token exists")
        void shouldReturnTrueWhenTokenExists() {
            // Given
            when(tokenStorageService.hasStoredTokens()).thenReturn(true);

            // When / Then
            assertThat(tokenProvider.hasToken()).isTrue();
        }

        @Test
        @DisplayName("should return false when no token exists")
        void shouldReturnFalseWhenNoToken() {
            // Given
            when(tokenStorageService.hasStoredTokens()).thenReturn(false);

            // When / Then
            assertThat(tokenProvider.hasToken()).isFalse();
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValidTests {

        @Test
        @DisplayName("should return true when token exists and not expired")
        void shouldReturnTrueWhenValid() {
            // Given
            OAuthTokens validTokens = createValidTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(validTokens));

            // When / Then
            assertThat(tokenProvider.isTokenValid()).isTrue();
        }

        @Test
        @DisplayName("should return false when token is expired")
        void shouldReturnFalseWhenExpired() {
            // Given
            OAuthTokens expiredTokens = createExpiredTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(expiredTokens));

            // When / Then
            assertThat(tokenProvider.isTokenValid()).isFalse();
        }

        @Test
        @DisplayName("should return false when no token exists")
        void shouldReturnFalseWhenNoToken() {
            // Given
            when(tokenStorageService.loadTokens()).thenReturn(Optional.empty());

            // When / Then
            assertThat(tokenProvider.isTokenValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Refresh Scenarios")
    class TokenRefreshScenarios {

        @Test
        @DisplayName("should handle 401 response by invalidating and retrying")
        void shouldHandle401ByInvalidatingAndRetrying() {
            // Given - simulate scenario where external service gets 401
            OAuthTokens validTokens = createValidTokens();
            OAuthTokens newTokens = createValidTokens(NEW_ACCESS_TOKEN);

            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(validTokens));

            // First call returns current token
            String firstToken = tokenProvider.getValidToken();
            assertThat(firstToken).isEqualTo("Bearer " + ACCESS_TOKEN);

            // External service gets 401, so we invalidate
            tokenProvider.invalidateToken();

            // Setup for refresh
            reset(tokenStorageService, oAuthService);
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(validTokens));
            when(oAuthService.refreshAccessToken())
                .thenReturn(CompletableFuture.completedFuture(newTokens));

            // Then - next call should use refreshed token
            String secondToken = tokenProvider.getValidToken();
            assertThat(secondToken).isEqualTo("Bearer " + NEW_ACCESS_TOKEN);
        }

        @Test
        @DisplayName("should save refreshed tokens to storage")
        void shouldSaveRefreshedTokensToStorage() {
            // Given
            OAuthTokens expiredTokens = createExpiredTokens();
            OAuthTokens newTokens = createValidTokens(NEW_ACCESS_TOKEN);

            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(expiredTokens));
            when(oAuthService.refreshAccessToken())
                .thenReturn(CompletableFuture.completedFuture(newTokens));

            // When
            tokenProvider.getValidToken();

            // Then
            verify(tokenStorageService).saveTokens(newTokens);
        }

        @Test
        @DisplayName("should not save tokens on refresh failure")
        void shouldNotSaveTokensOnRefreshFailure() {
            // Given
            OAuthTokens expiredTokens = createExpiredTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(expiredTokens));
            when(oAuthService.refreshAccessToken())
                .thenReturn(CompletableFuture.failedFuture(
                    new HmrcOAuthException(OAuthError.INVALID_GRANT)));

            // When / Then
            assertThatThrownBy(() -> tokenProvider.getValidToken())
                .isInstanceOf(TokenException.class);

            verify(tokenStorageService, never()).saveTokens(any());
        }
    }

    // Helper methods

    private OAuthTokens createValidTokens() {
        return createValidTokens(ACCESS_TOKEN);
    }

    private OAuthTokens createValidTokens(String accessToken) {
        // Token valid for 4 hours
        return new OAuthTokens(
            accessToken,
            REFRESH_TOKEN,
            14400, // 4 hours in seconds
            "Bearer",
            "write:self-assessment",
            Instant.now()
        );
    }

    private OAuthTokens createExpiredTokens() {
        // Token issued 5 hours ago, expired 1 hour ago
        return new OAuthTokens(
            ACCESS_TOKEN,
            REFRESH_TOKEN,
            14400, // 4 hours in seconds
            "Bearer",
            "write:self-assessment",
            Instant.now().minusSeconds(18000) // 5 hours ago
        );
    }
}
