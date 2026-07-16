package uk.selfemploy.hmrc.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.selfemploy.hmrc.config.HmrcConfig;
import uk.selfemploy.hmrc.exception.HmrcOAuthException;
import uk.selfemploy.hmrc.exception.HmrcOAuthException.OAuthError;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for HmrcOAuthService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("HmrcOAuthService")
class HmrcOAuthServiceTest {

    @Mock
    private HmrcConfig config;

    @Mock
    private OAuthCallbackServer callbackServer;

    @Mock
    private TokenExchangeClient tokenExchangeClient;

    @Mock
    private BrowserLauncher browserLauncher;

    private HmrcOAuthService oAuthService;

    @BeforeEach
    void setup() {
        oAuthService = new HmrcOAuthService(config, callbackServer, tokenExchangeClient, browserLauncher);
    }

    @Nested
    @DisplayName("Configuration Validation")
    class ConfigurationValidation {

        @Test
        @DisplayName("should throw exception if client ID not configured")
        void shouldThrowIfClientIdNotConfigured() {
            when(config.clientId()).thenReturn(Optional.empty());
            when(config.clientSecret()).thenReturn(Optional.of("secret"));

            assertThatThrownBy(() -> oAuthService.authenticate())
                .isInstanceOf(HmrcOAuthException.class)
                .hasMessageContaining("not configured");
        }

        @Test
        @DisplayName("should throw exception if client secret not configured")
        void shouldThrowIfClientSecretNotConfigured() {
            when(config.clientId()).thenReturn(Optional.of("client-id"));
            when(config.clientSecret()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oAuthService.authenticate())
                .isInstanceOf(HmrcOAuthException.class)
                .hasMessageContaining("not configured");
        }
    }

    @Nested
    @DisplayName("Authorization URL Generation")
    class AuthorizationUrlGeneration {

        @BeforeEach
        void setupConfig() {
            when(config.clientId()).thenReturn(Optional.of("test-client-id"));
            when(config.clientSecret()).thenReturn(Optional.of("test-secret"));
            when(config.authorizeUrl()).thenReturn("https://test-www.tax.service.gov.uk/oauth/authorize");
            when(config.scopes()).thenReturn(List.of("read:self-assessment", "write:self-assessment"));
            when(config.getRedirectUri()).thenReturn("http://localhost:8088/oauth/callback");
        }

        @Test
        @DisplayName("should include client_id in authorization URL")
        void shouldIncludeClientId() {
            String url = oAuthService.buildAuthorizationUrl("state123", "test-challenge");

            assertThat(url).contains("client_id=test-client-id");
        }

        @Test
        @DisplayName("should include response_type=code")
        void shouldIncludeResponseTypeCode() {
            String url = oAuthService.buildAuthorizationUrl("state123", "test-challenge");

            assertThat(url).contains("response_type=code");
        }

        @Test
        @DisplayName("should include state parameter")
        void shouldIncludeStateParameter() {
            String url = oAuthService.buildAuthorizationUrl("my-state-param", "test-challenge");

            assertThat(url).contains("state=my-state-param");
        }

        @Test
        @DisplayName("should include redirect_uri")
        void shouldIncludeRedirectUri() {
            String url = oAuthService.buildAuthorizationUrl("state", "test-challenge");

            assertThat(url).contains("redirect_uri=");
            assertThat(url).contains("localhost");
        }

        @Test
        @DisplayName("should include scopes")
        void shouldIncludeScopes() {
            String url = oAuthService.buildAuthorizationUrl("state", "test-challenge");

            assertThat(url).contains("scope=");
            assertThat(url).contains("self-assessment");
        }

        @Test
        @DisplayName("should use configured authorize URL")
        void shouldUseConfiguredAuthorizeUrl() {
            String url = oAuthService.buildAuthorizationUrl("state", "test-challenge");

            assertThat(url).startsWith("https://test-www.tax.service.gov.uk/oauth/authorize");
        }
    }

    @Nested
    @DisplayName("State Generation")
    class StateGeneration {

        @Test
        @DisplayName("should generate cryptographically secure state")
        void shouldGenerateCryptographicallySecureState() {
            String state1 = oAuthService.generateSecureState();
            String state2 = oAuthService.generateSecureState();

            assertThat(state1).isNotEqualTo(state2);
            assertThat(state1).hasSize(32); // Base64 encoded 24 bytes
        }

        @Test
        @DisplayName("should generate URL-safe state")
        void shouldGenerateUrlSafeState() {
            String state = oAuthService.generateSecureState();

            // URL-safe Base64 should not contain + or /
            assertThat(state).doesNotContain("+");
            assertThat(state).doesNotContain("/");
        }
    }

    @Nested
    @DisplayName("Authentication Flow")
    class AuthenticationFlow {

        @BeforeEach
        void setupConfig() {
            when(config.clientId()).thenReturn(Optional.of("test-client-id"));
            when(config.clientSecret()).thenReturn(Optional.of("test-secret"));
            when(config.authorizeUrl()).thenReturn("https://test.gov.uk/oauth/authorize");
            when(config.tokenUrl()).thenReturn("https://test.gov.uk/oauth/token");
            when(config.scopes()).thenReturn(List.of("read:self-assessment"));
            when(config.getRedirectUri()).thenReturn("http://localhost:8088/oauth/callback");
            when(callbackServer.listening()).thenReturn(CompletableFuture.completedFuture(null));
        }

        @Test
        @DisplayName("should not open the browser until the callback listener is bound")
        void shouldNotOpenBrowserBeforeListenerIsBound() {
            when(callbackServer.listening()).thenReturn(CompletableFuture.failedFuture(
                new HmrcOAuthException(OAuthError.PORT_IN_USE)));
            when(callbackServer.startAndAwaitCallback(anyString()))
                .thenReturn(new CompletableFuture<>());

            assertThatThrownBy(() -> oAuthService.authenticate().get())
                .hasCauseInstanceOf(HmrcOAuthException.class);

            verify(browserLauncher, never()).openUrl(anyString());
            verify(callbackServer).stop();
        }

        @Test
        @DisplayName("should send a PKCE S256 challenge on the authorization request")
        void shouldSendPkceChallengeOnAuthorize() throws Exception {
            when(callbackServer.startAndAwaitCallback(anyString()))
                .thenReturn(CompletableFuture.completedFuture("auth_code"));
            when(tokenExchangeClient.exchangeCodeForTokens(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(createTestTokens()));

            oAuthService.authenticate().get();

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(browserLauncher).openUrl(urlCaptor.capture());

            assertThat(urlCaptor.getValue()).contains("code_challenge_method=S256");
            assertThat(urlCaptor.getValue()).contains("code_challenge=");
        }

        @Test
        @DisplayName("should redeem the code with the verifier matching the challenge that was sent")
        void shouldRedeemCodeWithVerifierMatchingChallenge() throws Exception {
            when(callbackServer.startAndAwaitCallback(anyString()))
                .thenReturn(CompletableFuture.completedFuture("auth_code"));
            when(tokenExchangeClient.exchangeCodeForTokens(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(createTestTokens()));

            oAuthService.authenticate().get();

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(browserLauncher).openUrl(urlCaptor.capture());
            ArgumentCaptor<String> verifierCaptor = ArgumentCaptor.forClass(String.class);
            verify(tokenExchangeClient).exchangeCodeForTokens(anyString(), verifierCaptor.capture());

            String sentChallenge = extractQueryParam(urlCaptor.getValue(), "code_challenge");
            assertThat(sentChallenge).isEqualTo(PkceChallenge.challengeFor(verifierCaptor.getValue()));
        }

        private String extractQueryParam(String url, String name) {
            for (String pair : url.substring(url.indexOf('?') + 1).split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0 && pair.substring(0, eq).equals(name)) {
                    return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                }
            }
            return null;
        }

        @Test
        @DisplayName("should start callback server before opening browser")
        void shouldStartCallbackServerFirst() throws Exception {
            when(callbackServer.startAndAwaitCallback(anyString()))
                .thenReturn(CompletableFuture.completedFuture("auth_code"));
            when(tokenExchangeClient.exchangeCodeForTokens(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(createTestTokens()));

            oAuthService.authenticate().get();

            // Verify order: callback server started before browser opened
            var inOrder = inOrder(callbackServer, browserLauncher);
            inOrder.verify(callbackServer).startAndAwaitCallback(anyString());
            inOrder.verify(browserLauncher).openUrl(anyString());
        }

        @Test
        @DisplayName("should open system browser with authorization URL")
        void shouldOpenBrowserWithAuthUrl() throws Exception {
            when(callbackServer.startAndAwaitCallback(anyString()))
                .thenReturn(CompletableFuture.completedFuture("auth_code"));
            when(tokenExchangeClient.exchangeCodeForTokens(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(createTestTokens()));

            oAuthService.authenticate().get();

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(browserLauncher).openUrl(urlCaptor.capture());

            String url = urlCaptor.getValue();
            assertThat(url).contains("oauth/authorize");
            assertThat(url).contains("client_id=");
        }

        @Test
        @DisplayName("should exchange authorization code for tokens")
        void shouldExchangeCodeForTokens() throws Exception {
            when(callbackServer.startAndAwaitCallback(anyString()))
                .thenReturn(CompletableFuture.completedFuture("test_auth_code"));
            when(tokenExchangeClient.exchangeCodeForTokens(eq("test_auth_code"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(createTestTokens()));

            OAuthTokens tokens = oAuthService.authenticate().get();

            verify(tokenExchangeClient).exchangeCodeForTokens(eq("test_auth_code"), anyString());
            assertThat(tokens.accessToken()).isEqualTo("test_access_token");
        }

        @Test
        @DisplayName("should stop callback server after successful authentication")
        void shouldStopCallbackServerAfterSuccess() throws Exception {
            when(callbackServer.startAndAwaitCallback(anyString()))
                .thenReturn(CompletableFuture.completedFuture("auth_code"));
            when(tokenExchangeClient.exchangeCodeForTokens(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(createTestTokens()));

            oAuthService.authenticate().get();

            verify(callbackServer).stop();
        }

        @Test
        @DisplayName("should stop callback server on error")
        void shouldStopCallbackServerOnError() throws Exception {
            when(callbackServer.startAndAwaitCallback(anyString()))
                .thenReturn(CompletableFuture.failedFuture(new HmrcOAuthException(OAuthError.USER_CANCELLED)));

            try {
                oAuthService.authenticate().get();
            } catch (Exception e) {
                // Expected
            }

            verify(callbackServer).stop();
        }

        @Test
        @DisplayName("rejects a second concurrent authentication without disturbing the first")
        void rejectsConcurrentAuthentication() {
            when(callbackServer.startAndAwaitCallback(anyString()))
                .thenReturn(new CompletableFuture<>()); // first flow never completes

            CompletableFuture<OAuthTokens> first = oAuthService.authenticate();
            CompletableFuture<OAuthTokens> second = oAuthService.authenticate();

            assertThat(first).isNotCompleted();
            assertThat(second).isCompletedExceptionally();
            assertThatThrownBy(second::get).hasCauseInstanceOf(HmrcOAuthException.class);

            verify(browserLauncher, times(1)).openUrl(anyString());
            verify(callbackServer, never()).stop();
        }

        @Test
        @DisplayName("should propagate token exchange errors")
        void shouldPropagateTokenExchangeErrors() throws Exception {
            when(callbackServer.startAndAwaitCallback(anyString()))
                .thenReturn(CompletableFuture.completedFuture("auth_code"));
            when(tokenExchangeClient.exchangeCodeForTokens(anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(
                    new HmrcOAuthException(OAuthError.INVALID_GRANT)));

            assertThatThrownBy(() -> oAuthService.authenticate().get())
                .hasCauseInstanceOf(HmrcOAuthException.class);
        }
    }

    @Nested
    @DisplayName("Connection Status")
    class ConnectionStatus {

        @Test
        @DisplayName("should report not connected when no tokens stored")
        void shouldReportNotConnectedWhenNoTokens() {
            assertThat(oAuthService.isConnected()).isFalse();
        }
    }

    @Nested
    @DisplayName("Cancel Authentication")
    class CancelAuthentication {

        @Test
        @DisplayName("should stop callback server when cancelAuthentication is called")
        void shouldStopCallbackServerOnCancel() {
            oAuthService.cancelAuthentication();

            verify(callbackServer).stop();
        }

        @Test
        @DisplayName("should be safe to call cancelAuthentication when not authenticating")
        void shouldBeSafeToCallCancelWhenNotAuthenticating() {
            // Should not throw even when no authentication is in progress
            oAuthService.cancelAuthentication();

            verify(callbackServer).stop();
        }
    }

    @Nested
    @DisplayName("Token Refresh")
    class TokenRefresh {

        @Test
        @DisplayName("reports a missing refresh token as NO_REFRESH_TOKEN, not as an HMRC rejection")
        void missingRefreshTokenIsNotAnHmrcRejection() {
            // Nothing is sent to HMRC, so nothing has been rejected. Reporting this as invalid_grant
            // is what led callers to delete a session HMRC had never refused.
            oAuthService.setTokens(OAuthTokens.create("access", null, 14400, "Bearer", "scope"));

            assertThatThrownBy(() -> oAuthService.refreshAccessToken().get())
                .cause()
                .extracting(e -> ((HmrcOAuthException) e).getError())
                .isEqualTo(OAuthError.NO_REFRESH_TOKEN);

            verify(tokenExchangeClient, never()).refreshTokens(anyString());
        }

        @Test
        @DisplayName("reports an absent session as NO_REFRESH_TOKEN")
        void absentSessionIsNotAnHmrcRejection() {
            assertThatThrownBy(() -> oAuthService.refreshAccessToken().get())
                .cause()
                .extracting(e -> ((HmrcOAuthException) e).getError())
                .isEqualTo(OAuthError.NO_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("keeps the existing refresh token when HMRC's response omits a new one")
        void carriesForwardRefreshTokenWhenResponseOmitsIt() throws Exception {
            oAuthService.setTokens(createTestTokens());
            when(tokenExchangeClient.refreshTokens("test_refresh_token")).thenReturn(
                CompletableFuture.completedFuture(
                    OAuthTokens.create("new_access_token", null, 14400, "Bearer", "scope")));

            OAuthTokens refreshed = oAuthService.refreshAccessToken().get();

            assertThat(refreshed.accessToken()).isEqualTo("new_access_token");
            assertThat(refreshed.refreshToken()).isEqualTo("test_refresh_token");
            assertThat(oAuthService.getCurrentTokens().refreshToken()).isEqualTo("test_refresh_token");
        }

        @Test
        @DisplayName("adopts the rotated refresh token when HMRC issues one")
        void adoptsRotatedRefreshToken() throws Exception {
            oAuthService.setTokens(createTestTokens());
            when(tokenExchangeClient.refreshTokens("test_refresh_token")).thenReturn(
                CompletableFuture.completedFuture(OAuthTokens.create(
                    "new_access_token", "rotated_refresh_token", 14400, "Bearer", "scope")));

            assertThat(oAuthService.refreshAccessToken().get().refreshToken())
                .isEqualTo("rotated_refresh_token");
        }

        @Test
        @DisplayName("shares one refresh between concurrent callers, so a rotation cannot reject itself")
        void concurrentRefreshesShareOneRequest() {
            // HMRC invalidates a refresh token when it is redeemed. Two refreshes in flight together
            // would present the same token, and the loser would be told invalid_grant - a rejection
            // the app inflicted on itself, indistinguishable from a real revocation.
            oAuthService.setTokens(createTestTokens());
            CompletableFuture<OAuthTokens> pending = new CompletableFuture<>();
            when(tokenExchangeClient.refreshTokens("test_refresh_token")).thenReturn(pending);

            CompletableFuture<OAuthTokens> first = oAuthService.refreshAccessToken();
            CompletableFuture<OAuthTokens> second = oAuthService.refreshAccessToken();

            verify(tokenExchangeClient, times(1)).refreshTokens("test_refresh_token");

            pending.complete(OAuthTokens.create("new_access", "rotated_refresh", 14400, "Bearer", "scope"));
            assertThat(first).isCompleted();
            assertThat(second).isCompleted();
        }

        @Test
        @DisplayName("treats a blank refresh token as none, rather than presenting it to HMRC")
        void blankRefreshTokenIsNotPresented() {
            // An empty string is not a credential. Sending one earns an invalid_grant that looks
            // exactly like a real revocation, and would cost the user their stored session.
            oAuthService.setTokens(OAuthTokens.create("access", "  ", 14400, "Bearer", "scope"));

            assertThatThrownBy(() -> oAuthService.refreshAccessToken().get())
                .cause()
                .extracting(e -> ((HmrcOAuthException) e).getError())
                .isEqualTo(OAuthError.NO_REFRESH_TOKEN);

            verify(tokenExchangeClient, never()).refreshTokens(anyString());
        }

        @Test
        @DisplayName("disowns an in-flight refresh when the session it belongs to is replaced")
        void replacingTheSessionDisownsAnInFlightRefresh() {
            oAuthService.setTokens(createTestTokens());
            when(tokenExchangeClient.refreshTokens("test_refresh_token"))
                .thenReturn(new CompletableFuture<>());
            when(tokenExchangeClient.refreshTokens("second_refresh_token"))
                .thenReturn(CompletableFuture.completedFuture(
                    OAuthTokens.create("second_access", "rotated", 14400, "Bearer", "scope")));

            oAuthService.refreshAccessToken();  // never completes

            oAuthService.setTokens(OAuthTokens.create("second_access", "second_refresh_token",
                14400, "Bearer", "scope"));
            oAuthService.refreshAccessToken();

            // The second call must refresh the new session, not join the abandoned one.
            verify(tokenExchangeClient).refreshTokens("second_refresh_token");
        }

        @Test
        @DisplayName("starts a fresh request once the previous refresh has finished")
        void laterRefreshIsNotJoinedToACompletedOne() throws Exception {
            oAuthService.setTokens(createTestTokens());
            when(tokenExchangeClient.refreshTokens(anyString())).thenReturn(
                CompletableFuture.completedFuture(
                    OAuthTokens.create("access_1", "refresh_1", 14400, "Bearer", "scope")),
                CompletableFuture.completedFuture(
                    OAuthTokens.create("access_2", "refresh_2", 14400, "Bearer", "scope")));

            assertThat(oAuthService.refreshAccessToken().get().accessToken()).isEqualTo("access_1");
            assertThat(oAuthService.refreshAccessToken().get().accessToken()).isEqualTo("access_2");

            verify(tokenExchangeClient, times(1)).refreshTokens("test_refresh_token");
            verify(tokenExchangeClient, times(1)).refreshTokens("refresh_1");
        }

        @Test
        @DisplayName("notifies the refresh listener with the rotated tokens")
        void notifiesTheListenerOnRotation() throws Exception {
            // HMRC has already invalidated the old refresh token by this point, so the new one must
            // be recorded by the refresh itself - a caller that gave up waiting cannot be relied on.
            List<OAuthTokens> recorded = new ArrayList<>();
            oAuthService.setRefreshListener(recorded::add);
            oAuthService.setTokens(createTestTokens());
            when(tokenExchangeClient.refreshTokens("test_refresh_token")).thenReturn(
                CompletableFuture.completedFuture(
                    OAuthTokens.create("new_access", "rotated_refresh", 14400, "Bearer", "scope")));

            oAuthService.refreshAccessToken().get();

            assertThat(recorded).singleElement()
                .satisfies(tokens -> assertThat(tokens.refreshToken()).isEqualTo("rotated_refresh"));
        }

        @Test
        @DisplayName("a listener that throws does not fail the refresh, and the rotation still installs")
        void aThrowingListenerDoesNotFailTheRefresh() throws Exception {
            // The rotation is already installed by the time the listener runs. A listener failure is a
            // persistence side effect, not a refresh failure: turning it into a failed future would make
            // callers treat a renewed session as expired and discard it.
            oAuthService.setRefreshListener(tokens -> {
                throw new RuntimeException("persistence unavailable");
            });
            oAuthService.setTokens(createTestTokens());
            when(tokenExchangeClient.refreshTokens("test_refresh_token")).thenReturn(
                CompletableFuture.completedFuture(
                    OAuthTokens.create("new_access", "rotated_refresh", 14400, "Bearer", "scope")));

            OAuthTokens refreshed = oAuthService.refreshAccessToken().get();

            assertThat(refreshed.accessToken()).isEqualTo("new_access");
            assertThat(oAuthService.getCurrentTokens().refreshToken()).isEqualTo("rotated_refresh");
        }

        @Test
        @DisplayName("neither installs nor records a refresh that lands after the session was replaced")
        void discardsARefreshThatLandsAfterTheSessionChanged() throws Exception {
            List<OAuthTokens> recorded = new ArrayList<>();
            oAuthService.setRefreshListener(recorded::add);
            oAuthService.setTokens(createTestTokens());
            CompletableFuture<OAuthTokens> pending = new CompletableFuture<>();
            when(tokenExchangeClient.refreshTokens("test_refresh_token")).thenReturn(pending);

            CompletableFuture<OAuthTokens> refresh = oAuthService.refreshAccessToken();
            oAuthService.setTokens(null);
            pending.complete(OAuthTokens.create("late_access", "late_refresh", 14400, "Bearer", "scope"));
            refresh.get();

            assertThat(oAuthService.getCurrentTokens()).isNull();
            assertThat(recorded).isEmpty();
        }
    }

    private OAuthTokens createTestTokens() {
        return OAuthTokens.create(
            "test_access_token",
            "test_refresh_token",
            14400,
            "Bearer",
            "read:self-assessment"
        );
    }
}
