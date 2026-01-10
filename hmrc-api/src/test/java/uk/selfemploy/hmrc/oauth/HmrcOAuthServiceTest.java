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
            String url = oAuthService.buildAuthorizationUrl("state123");

            assertThat(url).contains("client_id=test-client-id");
        }

        @Test
        @DisplayName("should include response_type=code")
        void shouldIncludeResponseTypeCode() {
            String url = oAuthService.buildAuthorizationUrl("state123");

            assertThat(url).contains("response_type=code");
        }

        @Test
        @DisplayName("should include state parameter")
        void shouldIncludeStateParameter() {
            String url = oAuthService.buildAuthorizationUrl("my-state-param");

            assertThat(url).contains("state=my-state-param");
        }

        @Test
        @DisplayName("should include redirect_uri")
        void shouldIncludeRedirectUri() {
            String url = oAuthService.buildAuthorizationUrl("state");

            assertThat(url).contains("redirect_uri=");
            assertThat(url).contains("localhost");
        }

        @Test
        @DisplayName("should include scopes")
        void shouldIncludeScopes() {
            String url = oAuthService.buildAuthorizationUrl("state");

            assertThat(url).contains("scope=");
            assertThat(url).contains("self-assessment");
        }

        @Test
        @DisplayName("should use configured authorize URL")
        void shouldUseConfiguredAuthorizeUrl() {
            String url = oAuthService.buildAuthorizationUrl("state");

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
        }

        @Test
        @DisplayName("should start callback server before opening browser")
        void shouldStartCallbackServerFirst() throws Exception {
            when(callbackServer.startAndAwaitCallback(anyString()))
                .thenReturn(CompletableFuture.completedFuture("auth_code"));
            when(tokenExchangeClient.exchangeCodeForTokens(anyString()))
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
            when(tokenExchangeClient.exchangeCodeForTokens(anyString()))
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
            when(tokenExchangeClient.exchangeCodeForTokens("test_auth_code"))
                .thenReturn(CompletableFuture.completedFuture(createTestTokens()));

            OAuthTokens tokens = oAuthService.authenticate().get();

            verify(tokenExchangeClient).exchangeCodeForTokens("test_auth_code");
            assertThat(tokens.accessToken()).isEqualTo("test_access_token");
        }

        @Test
        @DisplayName("should stop callback server after successful authentication")
        void shouldStopCallbackServerAfterSuccess() throws Exception {
            when(callbackServer.startAndAwaitCallback(anyString()))
                .thenReturn(CompletableFuture.completedFuture("auth_code"));
            when(tokenExchangeClient.exchangeCodeForTokens(anyString()))
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
        @DisplayName("should propagate token exchange errors")
        void shouldPropagateTokenExchangeErrors() throws Exception {
            when(callbackServer.startAndAwaitCallback(anyString()))
                .thenReturn(CompletableFuture.completedFuture("auth_code"));
            when(tokenExchangeClient.exchangeCodeForTokens(anyString()))
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
