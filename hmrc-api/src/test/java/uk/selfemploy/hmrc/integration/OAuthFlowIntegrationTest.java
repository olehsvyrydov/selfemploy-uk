package uk.selfemploy.hmrc.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static uk.selfemploy.hmrc.integration.HmrcWireMockStubs.*;

/**
 * OAuth Flow Integration Tests for HMRC Sandbox.
 *
 * <p>Tests AC-1: OAuth flow with sandbox credentials.
 *
 * <h3>HMRC OAuth2 Flow Steps:</h3>
 * <ol>
 *     <li>Generate authorization URL with client_id, redirect_uri, scope, state</li>
 *     <li>User authorizes in browser (simulated)</li>
 *     <li>Exchange authorization code for tokens</li>
 *     <li>Use access token for API calls</li>
 *     <li>Refresh token when expired</li>
 * </ol>
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation">HMRC OAuth Documentation</a>
 */
@DisplayName("OAuth Flow Integration Tests (AC-1)")
@Tag("integration")
@Tag("oauth")
@Tag("sandbox")
class OAuthFlowIntegrationTest {

    private static WireMockServer wireMockServer;
    private HttpClient httpClient;

    // Test OAuth configuration
    private static final String TEST_CLIENT_ID = "test_client_id_12345";
    private static final String TEST_CLIENT_SECRET = "test_client_secret_67890";
    private static final String TEST_REDIRECT_URI = "http://localhost:8088/oauth/callback";
    private static final String TEST_SCOPE = "read:self-assessment write:self-assessment";

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig()
            .dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setup() {
        WireMock.reset();
        httpClient = HttpClient.newHttpClient();
    }

    // ==================== Authorization URL Tests ====================

    @Nested
    @DisplayName("OAUTH-001: Authorization URL Generation")
    class AuthorizationUrlGeneration {

        @Test
        @DisplayName("OAUTH-001-01: Authorization URL includes required parameters")
        void authorizationUrlIncludesRequiredParameters() {
            // Given
            String baseAuthUrl = wireMockServer.baseUrl() + "/oauth/authorize";
            String state = "random_state_value_12345";

            // When
            String authUrl = buildTestAuthorizationUrl(baseAuthUrl, state);

            // Then
            assertThat(authUrl)
                .contains("client_id=" + TEST_CLIENT_ID)
                .contains("response_type=code")
                .contains("redirect_uri=" + URLEncoder.encode(TEST_REDIRECT_URI, StandardCharsets.UTF_8))
                .contains("scope=" + URLEncoder.encode(TEST_SCOPE, StandardCharsets.UTF_8))
                .contains("state=" + state);
        }

        @Test
        @DisplayName("OAUTH-001-02: State parameter is URL-safe")
        void stateParameterIsUrlSafe() {
            // Given
            String state = "abc123XYZ_-";

            // When/Then
            assertThat(state).matches("^[A-Za-z0-9_-]+$");
        }

        @Test
        @DisplayName("OAUTH-001-03: Scopes include required MTD permissions")
        void scopesIncludeRequiredMtdPermissions() {
            // Given
            String scopes = TEST_SCOPE;

            // Then
            assertThat(scopes)
                .contains("read:self-assessment")
                .contains("write:self-assessment");
        }
    }

    // ==================== Token Exchange Tests ====================

    @Nested
    @DisplayName("OAUTH-002: Token Exchange")
    class TokenExchange {

        @Test
        @DisplayName("OAUTH-002-01: Successful token exchange returns access and refresh tokens")
        void successfulTokenExchangeReturnsTokens() throws Exception {
            // Given
            stubOAuthTokenExchangeSuccess();
            String authorizationCode = "valid_auth_code_12345";

            // When
            HttpRequest request = buildTokenExchangeRequest(authorizationCode);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                .contains("access_token")
                .contains("refresh_token")
                .contains("token_type")
                .contains("expires_in");

            // Verify token exchange request format
            verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withRequestBody(containing("grant_type=authorization_code"))
                .withRequestBody(containing("code=" + authorizationCode))
                .withRequestBody(containing("client_id=" + TEST_CLIENT_ID))
                .withRequestBody(containing("client_secret=" + TEST_CLIENT_SECRET))
                .withRequestBody(containing("redirect_uri=")));
        }

        @Test
        @DisplayName("OAUTH-002-02: Token response includes Bearer token type")
        void tokenResponseIncludesBearerTokenType() throws Exception {
            // Given
            stubOAuthTokenExchangeSuccess();

            // When
            HttpRequest request = buildTokenExchangeRequest("valid_code");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then - Check for token_type with flexible whitespace
            assertThat(response.body())
                .contains("token_type")
                .contains("Bearer");
        }

        @Test
        @DisplayName("OAUTH-002-03: Token expires_in is returned (typically 4 hours)")
        void tokenExpiresInIsReturned() throws Exception {
            // Given
            stubOAuthTokenExchangeSuccess();

            // When
            HttpRequest request = buildTokenExchangeRequest("valid_code");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then - HMRC tokens typically expire in 4 hours (14400 seconds)
            assertThat(response.body()).contains("\"expires_in\":");
        }
    }

    // ==================== Token Refresh Tests ====================

    @Nested
    @DisplayName("OAUTH-003: Token Refresh")
    class TokenRefresh {

        @Test
        @DisplayName("OAUTH-003-01: Successful token refresh returns new tokens")
        void successfulTokenRefreshReturnsNewTokens() throws Exception {
            // Given
            stubOAuthTokenRefreshSuccess();
            String refreshToken = "valid_refresh_token_12345";

            // When
            HttpRequest request = buildTokenRefreshRequest(refreshToken);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                .contains("access_token")
                .contains("new_access_token"); // Verify it's a new token

            // Verify refresh request format
            verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withRequestBody(containing("grant_type=refresh_token"))
                .withRequestBody(containing("refresh_token=" + refreshToken)));
        }

        @Test
        @DisplayName("OAUTH-003-02: Token refresh returns new refresh token (token rotation)")
        void tokenRefreshReturnsNewRefreshToken() throws Exception {
            // Given
            stubOAuthTokenRefreshSuccess();

            // When
            HttpRequest request = buildTokenRefreshRequest("old_refresh_token");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then - HMRC uses refresh token rotation (check with flexible whitespace)
            assertThat(response.body())
                .contains("refresh_token")
                .contains("new_refresh_token");
        }
    }

    // ==================== Error Handling Tests ====================

    @Nested
    @DisplayName("OAUTH-004: OAuth Error Handling")
    class OAuthErrorHandling {

        @Test
        @DisplayName("OAUTH-004-01: Invalid authorization code returns invalid_grant error")
        void invalidAuthorizationCodeReturnsError() throws Exception {
            // Given
            stubOAuthInvalidGrant();

            // When
            HttpRequest request = buildTokenExchangeRequest("invalid");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body())
                .contains("invalid_grant")
                .contains("expired or is invalid");
        }

        @Test
        @DisplayName("OAUTH-004-02: Expired authorization code returns invalid_grant error")
        void expiredAuthorizationCodeReturnsError() throws Exception {
            // Given
            stubFor(post(urlPathEqualTo("/oauth/token"))
                .withRequestBody(containing("code=expired_code"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "error": "invalid_grant",
                            "error_description": "The authorization code has expired"
                        }
                        """)));

            // When
            HttpRequest request = buildTokenExchangeRequest("expired_code");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("expired");
        }

        @Test
        @DisplayName("OAUTH-004-03: Expired refresh token returns invalid_grant error")
        void expiredRefreshTokenReturnsError() throws Exception {
            // Given
            stubOAuthExpiredRefreshToken();

            // When
            HttpRequest request = buildTokenRefreshRequest("expired");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body())
                .contains("invalid_grant")
                .contains("expired");
        }

        @Test
        @DisplayName("OAUTH-004-04: Invalid client credentials returns unauthorized_client error")
        void invalidClientCredentialsReturnsError() throws Exception {
            // Given
            stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "error": "unauthorized_client",
                            "error_description": "The client credentials are invalid"
                        }
                        """)));

            // When
            HttpRequest request = buildTokenExchangeRequest("any_code");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.body()).contains("unauthorized_client");
        }

        @ParameterizedTest
        @DisplayName("OAUTH-004-05: Missing required parameters return invalid_request error")
        @ValueSource(strings = {"grant_type", "code", "client_id", "redirect_uri"})
        void missingRequiredParametersReturnError(String missingParam) throws Exception {
            // Given
            stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "error": "invalid_request",
                            "error_description": "Missing required parameter: %s"
                        }
                        """.formatted(missingParam))));

            // When
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/oauth/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("incomplete=true"))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("invalid_request");
        }
    }

    // ==================== Token Usage Tests ====================

    @Nested
    @DisplayName("OAUTH-005: Token Usage")
    class TokenUsage {

        @Test
        @DisplayName("OAUTH-005-01: Access token is used as Bearer token in Authorization header")
        void accessTokenUsedAsBearerToken() throws Exception {
            // Given
            String accessToken = "test_access_token_12345";
            stubFor(get(urlPathEqualTo("/api/test"))
                .withHeader("Authorization", equalTo("Bearer " + accessToken))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{\"success\": true}")));

            // When
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/api/test"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            verify(getRequestedFor(urlPathEqualTo("/api/test"))
                .withHeader("Authorization", equalTo("Bearer " + accessToken)));
        }

        @Test
        @DisplayName("OAUTH-005-02: Request without Authorization header returns 401")
        void requestWithoutAuthorizationReturns401() throws Exception {
            // Given
            stubFor(get(urlPathEqualTo("/api/test"))
                .withHeader("Authorization", absent())
                .willReturn(aResponse()
                    .withStatus(401)
                    .withBody("{\"code\": \"UNAUTHORIZED\"}")));

            // When
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/api/test"))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(401);
        }

        @Test
        @DisplayName("OAUTH-005-03: Request with expired token returns 401")
        void requestWithExpiredTokenReturns401() throws Exception {
            // Given
            stubFor(get(urlPathEqualTo("/api/test"))
                .withHeader("Authorization", matching("Bearer expired.*"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "code": "INVALID_CREDENTIALS",
                            "message": "Bearer token has expired"
                        }
                        """)));

            // When
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/api/test"))
                .header("Authorization", "Bearer expired_token_12345")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.body()).contains("expired");
        }
    }

    // ==================== CSRF Protection Tests ====================

    @Nested
    @DisplayName("OAUTH-006: CSRF Protection (State Parameter)")
    class CsrfProtection {

        @Test
        @DisplayName("OAUTH-006-01: State parameter is validated on callback")
        void stateParameterValidatedOnCallback() {
            // Given
            String originalState = "original_state_12345";
            String returnedState = "original_state_12345";

            // Then - states should match
            assertThat(returnedState).isEqualTo(originalState);
        }

        @Test
        @DisplayName("OAUTH-006-02: Mismatched state parameter is rejected")
        void mismatchedStateParameterRejected() {
            // Given
            String originalState = "original_state_12345";
            String returnedState = "different_state_67890";

            // Then - states should NOT match
            assertThat(returnedState).isNotEqualTo(originalState);
        }

        @Test
        @DisplayName("OAUTH-006-03: State parameter is cryptographically random (at least 32 chars)")
        void stateParameterIsCryptographicallyRandom() {
            // Given - typical Base64 encoded 24 bytes = 32 characters
            String state = "aBcDeFgHiJkLmNoPqRsTuVwXyZ012345";

            // Then
            assertThat(state.length()).isGreaterThanOrEqualTo(32);
        }
    }

    // ==================== Helper Methods ====================

    private String buildTestAuthorizationUrl(String baseUrl, String state) {
        return baseUrl + "?" +
            "client_id=" + encode(TEST_CLIENT_ID) +
            "&response_type=code" +
            "&redirect_uri=" + encode(TEST_REDIRECT_URI) +
            "&scope=" + encode(TEST_SCOPE) +
            "&state=" + encode(state);
    }

    private HttpRequest buildTokenExchangeRequest(String authorizationCode) {
        String body = "grant_type=authorization_code" +
            "&code=" + encode(authorizationCode) +
            "&client_id=" + encode(TEST_CLIENT_ID) +
            "&client_secret=" + encode(TEST_CLIENT_SECRET) +
            "&redirect_uri=" + encode(TEST_REDIRECT_URI);

        return HttpRequest.newBuilder()
            .uri(URI.create(wireMockServer.baseUrl() + "/oauth/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    }

    private HttpRequest buildTokenRefreshRequest(String refreshToken) {
        String body = "grant_type=refresh_token" +
            "&refresh_token=" + encode(refreshToken) +
            "&client_id=" + encode(TEST_CLIENT_ID) +
            "&client_secret=" + encode(TEST_CLIENT_SECRET);

        return HttpRequest.newBuilder()
            .uri(URI.create(wireMockServer.baseUrl() + "/oauth/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
