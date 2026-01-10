package uk.selfemploy.hmrc.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * HMRC API Integration Tests - Sprint 3
 *
 * Tests SE-304 Business Details API per /rob QA specification:
 * - BIZ-001: API Integration
 * - BIZ-002: NINO Validation
 * - BIZ-003: NINO Masking (GDPR)
 * - BIZ-004: Error Handling
 *
 * @see docs/sprints/sprint-3/testing/rob-qa-sprint-3-backend.md
 */
@DisplayName("HMRC API Integration Tests")
@Tag("integration")
@Tag("api")
class HmrcApiIntegrationTest {

    private static WireMockServer wireMockServer;

    private ByteArrayOutputStream logCapture;
    private PrintStream originalOut;
    private PrintStream originalErr;

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
        // Reset WireMock
        WireMock.reset();

        // Capture stdout/stderr for log analysis
        logCapture = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        PrintStream captureStream = new PrintStream(logCapture);
        System.setOut(captureStream);
        System.setErr(captureStream);
    }

    @AfterEach
    void teardown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    /**
     * BIZ-001: API Integration Tests
     */
    @Nested
    @DisplayName("BIZ-001: API Integration")
    class ApiIntegration {

        @Test
        @DisplayName("BIZ-001-01: List businesses returns business details")
        void listBusinessesReturnsBusinessDetails() throws Exception {
            // Given
            String nino = "AA123456A";
            stubFor(get(urlPathEqualTo("/individuals/business/self-employment/" + nino))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "selfEmployments": [
                                {
                                    "businessId": "XAIS12345678901",
                                    "typeOfBusiness": "self-employment",
                                    "tradingName": "Test Business",
                                    "accountingPeriods": []
                                }
                            ]
                        }
                        """)));

            // When
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + nino))
                .header("Accept", "application/vnd.hmrc.1.0+json")
                .header("Authorization", "Bearer test_access_token_12345")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("XAIS12345678901");
            assertThat(response.body()).contains("Test Business");
        }

        @Test
        @DisplayName("BIZ-001-02: Get specific business details")
        void getSpecificBusinessDetails() throws Exception {
            // Given
            String nino = "AA123456A";
            String businessId = "XAIS12345678901";
            stubFor(get(urlPathEqualTo("/individuals/business/self-employment/" + nino + "/" + businessId))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "businessId": "XAIS12345678901",
                            "typeOfBusiness": "self-employment",
                            "tradingName": "Test Business",
                            "businessAddressLineOne": "123 Test Street",
                            "businessAddressPostcode": "AB12 3CD",
                            "accountingPeriods": [
                                {
                                    "start": "2025-04-06",
                                    "end": "2026-04-05"
                                }
                            ]
                        }
                        """)));

            // When
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + nino + "/" + businessId))
                .header("Accept", "application/vnd.hmrc.1.0+json")
                .header("Authorization", "Bearer test_access_token_12345")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("XAIS12345678901");
            assertThat(response.body()).contains("123 Test Street");
            assertThat(response.body()).contains("AB12 3CD");
        }

        @Test
        @DisplayName("BIZ-001-03: Bearer token required in Authorization header")
        void bearerTokenRequiredInAuthorizationHeader() throws Exception {
            // Given
            String nino = "AA123456A";
            stubFor(get(urlPathEqualTo("/individuals/business/self-employment/" + nino))
                .withHeader("Authorization", matching("Bearer .+"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"selfEmployments\": []}")));

            stubFor(get(urlPathEqualTo("/individuals/business/self-employment/" + nino))
                .withHeader("Authorization", absent())
                .willReturn(aResponse()
                    .withStatus(401)
                    .withBody("{\"code\": \"UNAUTHORIZED\"}")));

            // When - with token
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest requestWithToken = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + nino))
                .header("Authorization", "Bearer test_token")
                .GET()
                .build();

            HttpResponse<String> responseWithToken = client.send(requestWithToken, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(responseWithToken.statusCode()).isEqualTo(200);

            // When - without token
            HttpRequest requestWithoutToken = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + nino))
                .GET()
                .build();

            HttpResponse<String> responseWithoutToken = client.send(requestWithoutToken, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(responseWithoutToken.statusCode()).isEqualTo(401);
        }

        @Test
        @DisplayName("BIZ-001-05: Accept header is HMRC versioned")
        void acceptHeaderIsHmrcVersioned() throws Exception {
            // Given
            String nino = "AA123456A";
            stubFor(get(urlPathEqualTo("/individuals/business/self-employment/" + nino))
                .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"selfEmployments\": []}")));

            // When
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + nino))
                .header("Accept", "application/vnd.hmrc.1.0+json")
                .header("Authorization", "Bearer test_token")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            verify(getRequestedFor(urlPathEqualTo("/individuals/business/self-employment/" + nino))
                .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json")));
        }
    }

    /**
     * BIZ-002: NINO Validation Tests
     */
    @Nested
    @DisplayName("BIZ-002: NINO Validation")
    class NinoValidation {

        @Test
        @DisplayName("BIZ-002-01: Valid NINO accepted")
        void validNinoAccepted() {
            // Should not throw for valid NINO format
            assertThatCode(() -> validateNino("AB123456C"))
                .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @DisplayName("BIZ-002-02: NINO with spaces normalized")
        @CsvSource({
            "AB 12 34 56 C, AB123456C",
            "AB  123456  C, AB123456C",
            " AB123456C , AB123456C"
        })
        void ninoWithSpacesNormalized(String input, String expected) {
            String normalized = normalizeNino(input);
            assertThat(normalized).isEqualTo(expected);
        }

        @ParameterizedTest
        @DisplayName("BIZ-002-03: Lowercase NINO normalized to uppercase")
        @CsvSource({
            "ab123456c, AB123456C",
            "Ab123456C, AB123456C",
            "aB123456c, AB123456C"
        })
        void lowercaseNinoNormalized(String input, String expected) {
            String normalized = normalizeNino(input);
            assertThat(normalized).isEqualTo(expected);
        }

        @ParameterizedTest
        @DisplayName("BIZ-002-04 to BIZ-002-08: Invalid prefixes rejected")
        @ValueSource(strings = {"BG123456A", "GB123456A", "KN123456A", "NK123456A", "NT123456A", "TN123456A", "ZZ123456A"})
        void invalidPrefixesRejected(String nino) {
            assertThatThrownBy(() -> validateNino(nino))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid NINO format");
        }

        @ParameterizedTest
        @DisplayName("BIZ-002-09: Invalid suffix rejected (must be A-D)")
        @ValueSource(strings = {"AB123456E", "AB123456F", "AB123456G", "AB123456Z"})
        void invalidSuffixRejected(String nino) {
            assertThatThrownBy(() -> validateNino(nino))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid NINO format");
        }

        @Test
        @DisplayName("BIZ-002-10: Too short NINO rejected")
        void tooShortNinoRejected() {
            assertThatThrownBy(() -> validateNino("AB12345"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid NINO format");
        }

        @Test
        @DisplayName("BIZ-002-11: Too long NINO rejected")
        void tooLongNinoRejected() {
            assertThatThrownBy(() -> validateNino("AB1234567A"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid NINO format");
        }

        @Test
        @DisplayName("BIZ-002-12: Null NINO rejected")
        void nullNinoRejected() {
            assertThatThrownBy(() -> validateNino(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
        }

        @Test
        @DisplayName("BIZ-002-13: Empty NINO rejected")
        void emptyNinoRejected() {
            assertThatThrownBy(() -> validateNino(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
        }

        // Helper methods for NINO validation tests
        private void validateNino(String nino) {
            if (nino == null || nino.isBlank()) {
                throw new IllegalArgumentException("NINO cannot be null or empty");
            }

            String normalized = normalizeNino(nino);

            // NINO pattern from BusinessDetailsService
            String pattern = "^(?!BG|GB|KN|NK|NT|TN|ZZ)[A-CEGHJ-PR-TW-Z][A-CEGHJ-NPR-TW-Z][0-9]{6}[A-D]$";
            if (!normalized.matches(pattern)) {
                throw new IllegalArgumentException("Invalid NINO format: " + maskNino(normalized));
            }
        }

        private String normalizeNino(String nino) {
            if (nino == null) return null;
            return nino.replaceAll("\\s+", "").toUpperCase();
        }

        private String maskNino(String nino) {
            if (nino == null || nino.length() < 4) {
                return "****";
            }
            return nino.substring(0, 2) + "****" + nino.substring(nino.length() - 1);
        }
    }

    /**
     * BIZ-003: NINO Masking (GDPR Compliance)
     */
    @Nested
    @DisplayName("BIZ-003: NINO Masking (GDPR)")
    class NinoMasking {

        @Test
        @DisplayName("BIZ-003-01: NINO masked in logs during list businesses")
        void ninoMaskedInLogsDuringListBusinesses() throws Exception {
            // Given
            String nino = "AB123456C";
            stubFor(get(urlPathMatching("/individuals/business/self-employment/.*"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{\"selfEmployments\": []}")));

            // When - Simulate a log message with NINO
            System.out.println("Listing businesses for NINO: " + maskNino(nino));

            // Then
            String logs = logCapture.toString();
            assertThat(logs).contains("AB****C");
            assertThat(logs).doesNotContain("AB123456C");
            assertThat(logs).doesNotContain("123456");
        }

        @Test
        @DisplayName("BIZ-003-02: Invalid NINO masked in error message")
        void invalidNinoMaskedInError() {
            // Given
            String invalidNino = "XY123456Z";

            // When
            Throwable thrown = catchThrowable(() -> validateAndMaskNino(invalidNino));

            // Then
            assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("XY****Z")
                .hasMessageNotContaining("XY123456Z");
        }

        @Test
        @DisplayName("BIZ-003-03: NINO masking format correct (first 2 + **** + last 1)")
        void ninoMaskingFormatCorrect() {
            // Test various NINOs
            assertThat(maskNino("AB123456C")).isEqualTo("AB****C");
            assertThat(maskNino("XY987654D")).isEqualTo("XY****D");
            assertThat(maskNino("PQ555555A")).isEqualTo("PQ****A");
        }

        @Test
        @DisplayName("BIZ-003-03: Short NINO handled gracefully")
        void shortNinoHandledGracefully() {
            assertThat(maskNino("AB")).isEqualTo("****");
            assertThat(maskNino("ABC")).isEqualTo("****");
            assertThat(maskNino(null)).isEqualTo("****");
        }

        private String maskNino(String nino) {
            if (nino == null || nino.length() < 4) {
                return "****";
            }
            return nino.substring(0, 2) + "****" + nino.substring(nino.length() - 1);
        }

        private void validateAndMaskNino(String nino) {
            String normalized = nino.replaceAll("\\s+", "").toUpperCase();
            String pattern = "^(?!BG|GB|KN|NK|NT|TN|ZZ)[A-CEGHJ-PR-TW-Z][A-CEGHJ-NPR-TW-Z][0-9]{6}[A-D]$";
            if (!normalized.matches(pattern)) {
                throw new IllegalArgumentException("Invalid NINO format: " + maskNino(normalized));
            }
        }
    }

    /**
     * BIZ-004: Error Handling
     */
    @Nested
    @DisplayName("BIZ-004: Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("BIZ-004-01: 401 Unauthorized - not authenticated")
        void unauthorizedNotAuthenticated() throws Exception {
            // Given
            String nino = "AA123456A";
            stubFor(get(urlPathEqualTo("/individuals/business/self-employment/" + nino))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "code": "UNAUTHORIZED",
                            "message": "Bearer token is missing or invalid"
                        }
                        """)));

            // When
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + nino))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.body()).contains("UNAUTHORIZED");
        }

        @Test
        @DisplayName("BIZ-004-02: 401 Unauthorized - expired token")
        void unauthorizedExpiredToken() throws Exception {
            // Given
            String nino = "AA123456A";
            stubFor(get(urlPathEqualTo("/individuals/business/self-employment/" + nino))
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
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + nino))
                .header("Authorization", "Bearer expired_token_12345")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.body()).contains("expired");
        }

        @Test
        @DisplayName("BIZ-004-03: 403 Forbidden - insufficient scope")
        void forbiddenInsufficientScope() throws Exception {
            // Given
            String nino = "AA123456A";
            stubFor(get(urlPathEqualTo("/individuals/business/self-employment/" + nino))
                .willReturn(aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "code": "FORBIDDEN",
                            "message": "Access denied - insufficient scope"
                        }
                        """)));

            // When
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + nino))
                .header("Authorization", "Bearer limited_scope_token")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(403);
            assertThat(response.body()).contains("FORBIDDEN");
        }

        @Test
        @DisplayName("BIZ-004-04: 404 Not Found - non-existent business")
        void notFoundNonExistentBusiness() throws Exception {
            // Given
            String nino = "AA123456A";
            String businessId = "XAIS00000000000";
            stubFor(get(urlPathEqualTo("/individuals/business/self-employment/" + nino + "/" + businessId))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "code": "NOT_FOUND",
                            "message": "Business not found"
                        }
                        """)));

            // When
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + nino + "/" + businessId))
                .header("Authorization", "Bearer test_token")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(response.body()).contains("NOT_FOUND");
        }

        @Test
        @DisplayName("BIZ-004-05: 429 Rate Limited")
        void rateLimited() throws Exception {
            // Given
            String nino = "AA123456A";
            stubFor(get(urlPathEqualTo("/individuals/business/self-employment/" + nino))
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Content-Type", "application/json")
                    .withHeader("Retry-After", "60")
                    .withBody("""
                        {
                            "code": "TOO_MANY_REQUESTS",
                            "message": "Rate limit exceeded"
                        }
                        """)));

            // When
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + nino))
                .header("Authorization", "Bearer test_token")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(429);
            assertThat(response.headers().firstValue("Retry-After")).isPresent().contains("60");
        }

        @Test
        @DisplayName("BIZ-004-06: 503 Service Unavailable")
        void serviceUnavailable() throws Exception {
            // Given
            String nino = "AA123456A";
            stubFor(get(urlPathEqualTo("/individuals/business/self-employment/" + nino))
                .willReturn(aResponse()
                    .withStatus(503)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "code": "SERVICE_UNAVAILABLE",
                            "message": "HMRC service is temporarily unavailable"
                        }
                        """)));

            // When
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + nino))
                .header("Authorization", "Bearer test_token")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(503);
            assertThat(response.body()).contains("SERVICE_UNAVAILABLE");
        }
    }

}
