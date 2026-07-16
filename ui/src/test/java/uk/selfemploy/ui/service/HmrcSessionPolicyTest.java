package uk.selfemploy.ui.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.exception.HmrcOAuthException;
import uk.selfemploy.hmrc.exception.HmrcOAuthException.OAuthError;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * A stored HMRC session must only be destroyed when HMRC has actually refused it.
 *
 * <p>The refresh token lives for eighteen months and can only be replaced by sending the user back
 * through a browser sign-in, so deleting one is expensive and irreversible. Every refresh call site
 * used to delete the whole session on <em>any</em> exception — a timeout, an outage, or simply having
 * no refresh token to send — which is how a momentary blip logged the user out of HMRC.
 */
@DisplayName("HMRC session policy")
class HmrcSessionPolicyTest {

    private HmrcOAuthService oauthService;

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetTestData();
        oauthService = mock(HmrcOAuthService.class);
        storeSession();
    }

    private static void storeSession() {
        SqliteDataStore.getInstance().saveOAuthTokens("access-token", "refresh-token", 4 * 3600,
            "bearer", "read:self-assessment", Instant.now());
    }

    private static SqliteDataStore store() {
        return SqliteDataStore.getInstance();
    }

    @Nested
    @DisplayName("When HMRC rejects the refresh token")
    class Rejection {

        @Test
        @DisplayName("the session is cleared, because HMRC has said the grant is dead")
        void rejectionClearsTheSession() {
            boolean cleared = HmrcSessionPolicy.onRefreshFailure(
                new CompletionException(new HmrcOAuthException(OAuthError.INVALID_GRANT)), oauthService);

            assertThat(cleared).isTrue();
            assertThat(store().hasOAuthTokens()).isFalse();
        }

        @Test
        @DisplayName("the in-memory session is cleared too, so the dead token is not presented again")
        void rejectionClearsTheInMemorySession() {
            HmrcSessionPolicy.onRefreshFailure(
                new CompletionException(new HmrcOAuthException(OAuthError.INVALID_GRANT)), oauthService);

            verify(oauthService).setTokens(null);
        }
    }

    @Nested
    @DisplayName("When the refresh fails without a rejection")
    class NoRejection {

        @Test
        @DisplayName("a network timeout destroys nothing")
        void timeoutKeepsTheSession() {
            boolean cleared = HmrcSessionPolicy.onRefreshFailure(
                new CompletionException(new SocketTimeoutException("read timed out")), oauthService);

            assertThat(cleared).isFalse();
            assertThat(store().hasOAuthTokens()).isTrue();
            verify(oauthService, never()).setTokens(null);
        }

        @Test
        @DisplayName("an HMRC outage destroys nothing")
        void serverErrorKeepsTheSession() {
            boolean cleared = HmrcSessionPolicy.onRefreshFailure(
                new CompletionException(new HmrcOAuthException(OAuthError.SERVER_ERROR)), oauthService);

            assertThat(cleared).isFalse();
            assertThat(store().hasOAuthTokens()).isTrue();
        }

        /**
         * This is the bug that logged users out. The stored session can fail to decrypt (the
         * master key is momentarily unreadable), leaving nothing in memory to refresh with. That
         * used to be reported as {@code invalid_grant} and mistaken for an HMRC rejection, which
         * wiped a session HMRC had never refused - including an access token with hours of life
         * left.
         */
        @Test
        @DisplayName("having no refresh token to send is a local failure, not HMRC's word: nothing is destroyed")
        void missingRefreshTokenKeepsTheSession() {
            boolean cleared = HmrcSessionPolicy.onRefreshFailure(
                new CompletionException(new HmrcOAuthException(OAuthError.NO_REFRESH_TOKEN)), oauthService);

            assertThat(cleared).isFalse();
            assertThat(store().hasOAuthTokens()).isTrue();
            verify(oauthService, never()).setTokens(null);
        }

        @Test
        @DisplayName("an unreadable master key destroys nothing")
        void unreadableMasterKeyKeepsTheSession() {
            boolean cleared = HmrcSessionPolicy.onRefreshFailure(
                new MasterKeyUnavailableException("master key unreadable"), oauthService);

            assertThat(cleared).isFalse();
            assertThat(store().hasOAuthTokens()).isTrue();
        }
    }
}
