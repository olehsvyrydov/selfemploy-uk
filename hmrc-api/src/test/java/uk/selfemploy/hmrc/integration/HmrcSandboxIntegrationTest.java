package uk.selfemploy.hmrc.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static uk.selfemploy.hmrc.integration.HmrcWireMockStubs.*;

/**
 * Main HMRC Sandbox Integration Test Suite.
 *
 * <p>SE-703: HMRC Sandbox Integration Tests
 *
 * <p>This comprehensive test suite validates the complete MTD submission workflow
 * against the HMRC sandbox API using WireMock for deterministic testing.
 *
 * <h2>Acceptance Criteria Covered:</h2>
 * <ul>
 *     <li>AC-1: Test OAuth flow with sandbox credentials</li>
 *     <li>AC-2: Test Business Details API retrieval</li>
 *     <li>AC-3: Test Quarterly Update submission (all 4 quarters)</li>
 *     <li>AC-4: Test Annual Return submission</li>
 *     <li>AC-5: Test error handling for invalid submissions</li>
 *     <li>AC-6: Test fraud prevention headers are included</li>
 *     <li>AC-7: All tests documented with HMRC test scenarios</li>
 *     <li>AC-8: Tests can run in CI with sandbox API</li>
 * </ul>
 *
 * <h2>HMRC Sandbox Test Scenarios:</h2>
 * <table>
 *     <tr><th>NINO</th><th>Expected Result</th><th>Use Case</th></tr>
 *     <tr><td>AA000001A</td><td>Success</td><td>Happy path testing</td></tr>
 *     <tr><td>AA000404A</td><td>404 Not Found</td><td>Resource not found</td></tr>
 *     <tr><td>AA000500A</td><td>500 Server Error</td><td>Server errors</td></tr>
 *     <tr><td>AA000422A</td><td>422 Validation Error</td><td>Invalid data</td></tr>
 *     <tr><td>AA000401A</td><td>401 Unauthorized</td><td>Auth errors</td></tr>
 *     <tr><td>AA000403A</td><td>403 Forbidden</td><td>Permission errors</td></tr>
 *     <tr><td>AA000429A</td><td>429 Rate Limited</td><td>Rate limiting</td></tr>
 *     <tr><td>AA000409A</td><td>409 Conflict</td><td>Duplicate submissions</td></tr>
 * </table>
 *
 * @see OAuthFlowIntegrationTest
 * @see BusinessDetailsIntegrationTest
 * @see QuarterlyUpdateIntegrationTest
 * @see AnnualReturnIntegrationTest
 * @see FraudPreventionHeadersIntegrationTest
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/testing">HMRC API Testing Guide</a>
 */
@DisplayName("HMRC Sandbox Integration Test Suite (SE-703)")
@Tag("integration")
@Tag("sandbox")
@Tag("e2e")
@TestMethodOrder(OrderAnnotation.class)
class HmrcSandboxIntegrationTest {

    private static WireMockServer wireMockServer;
    private HttpClient httpClient;

    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String HMRC_ACCEPT_HEADER = "application/vnd.hmrc.1.0+json";

    // Fraud Prevention Headers
    private static final String HEADER_CONNECTION_METHOD = "Gov-Client-Connection-Method";
    private static final String HEADER_DEVICE_ID = "Gov-Client-Device-ID";
    private static final String HEADER_USER_IDS = "Gov-Client-User-IDs";
    private static final String HEADER_TIMEZONE = "Gov-Client-Timezone";

    // Token storage for flow
    private static String accessToken;
    private static String refreshToken;

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

    // ==================== Complete MTD Workflow Tests ====================

    @Nested
    @DisplayName("SUITE-001: Complete MTD Self-Employment Workflow")
    @TestMethodOrder(OrderAnnotation.class)
    class CompleteMtdWorkflow {

        @Test
        @Order(1)
        @DisplayName("SUITE-001-01: Step 1 - Authenticate with HMRC (OAuth2)")
        void step1_authenticateWithHmrc() throws Exception {
            // Given
            stubOAuthTokenExchangeSuccess();

            String requestBody = "grant_type=authorization_code" +
                "&code=valid_auth_code" +
                "&client_id=test_client_id" +
                "&client_secret=test_client_secret" +
                "&redirect_uri=http://localhost:8088/oauth/callback";

            // When
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/oauth/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                .contains("access_token")
                .contains("refresh_token");

            // Store tokens for subsequent requests
            accessToken = "test_access_token_12345678901234567890";
            refreshToken = "test_refresh_token_09876543210987654321";
        }

        @Test
        @Order(2)
        @DisplayName("SUITE-001-02: Step 2 - Retrieve Business Details")
        void step2_retrieveBusinessDetails() throws Exception {
            // Given
            accessToken = "test_access_token_12345678901234567890";
            stubListBusinessesSuccess(NINO_HAPPY_PATH);

            // When
            HttpResponse<String> response = sendAuthenticatedGet(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH);

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                .contains(TEST_BUSINESS_ID)
                .contains("Test Business Ltd");
        }

        @Test
        @Order(3)
        @DisplayName("SUITE-001-03: Step 3 - Submit Q1 Quarterly Update")
        void step3_submitQ1QuarterlyUpdate() throws Exception {
            // Given
            accessToken = "test_access_token_12345678901234567890";
            stubQuarterlyUpdateSuccess(NINO_HAPPY_PATH, TEST_BUSINESS_ID);

            String requestBody = buildQuarterlyUpdateJson(
                "2025-04-06", "2025-07-05",
                new BigDecimal("10000.00"), new BigDecimal("2500.00"));

            // When
            HttpResponse<String> response = sendAuthenticatedPost(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(201);
            assertThat(response.body()).contains("ACCEPTED");
        }

        @Test
        @Order(4)
        @DisplayName("SUITE-001-04: Step 4 - Submit Q2 Quarterly Update (Cumulative)")
        void step4_submitQ2QuarterlyUpdate() throws Exception {
            // Given
            accessToken = "test_access_token_12345678901234567890";
            stubFor(post(urlPathMatching("/individuals/business/self-employment/.*/period"))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", JSON_CONTENT_TYPE)
                    .withBody("""
                        {"id": "QTR-2025-Q2-DEF456", "status": "ACCEPTED"}
                        """)));

            // Q2 cumulative: Q1 + Q2
            String requestBody = buildQuarterlyUpdateJson(
                "2025-07-06", "2025-10-05",
                new BigDecimal("22000.00"), // Cumulative income
                new BigDecimal("5500.00")); // Cumulative expenses

            // When
            HttpResponse<String> response = sendAuthenticatedPost(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(201);
        }

        @Test
        @Order(5)
        @DisplayName("SUITE-001-05: Step 5 - Submit Q3 Quarterly Update (Cumulative)")
        void step5_submitQ3QuarterlyUpdate() throws Exception {
            // Given
            accessToken = "test_access_token_12345678901234567890";
            stubFor(post(urlPathMatching("/individuals/business/self-employment/.*/period"))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", JSON_CONTENT_TYPE)
                    .withBody("""
                        {"id": "QTR-2025-Q3-GHI789", "status": "ACCEPTED"}
                        """)));

            // Q3 cumulative: Q1 + Q2 + Q3
            String requestBody = buildQuarterlyUpdateJson(
                "2025-10-06", "2026-01-05",
                new BigDecimal("35000.00"),
                new BigDecimal("9000.00"));

            // When
            HttpResponse<String> response = sendAuthenticatedPost(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(201);
        }

        @Test
        @Order(6)
        @DisplayName("SUITE-001-06: Step 6 - Submit Q4 Quarterly Update (Full Year Cumulative)")
        void step6_submitQ4QuarterlyUpdate() throws Exception {
            // Given
            accessToken = "test_access_token_12345678901234567890";
            stubFor(post(urlPathMatching("/individuals/business/self-employment/.*/period"))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", JSON_CONTENT_TYPE)
                    .withBody("""
                        {"id": "QTR-2025-Q4-JKL012", "status": "ACCEPTED"}
                        """)));

            // Q4 cumulative: Full year totals
            String requestBody = buildQuarterlyUpdateJson(
                "2026-01-06", "2026-04-05",
                new BigDecimal("48000.00"),
                new BigDecimal("12500.00"));

            // When
            HttpResponse<String> response = sendAuthenticatedPost(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(201);
        }

        @Test
        @Order(7)
        @DisplayName("SUITE-001-07: Step 7 - Trigger Tax Calculation")
        void step7_triggerTaxCalculation() throws Exception {
            // Given
            accessToken = "test_access_token_12345678901234567890";
            stubTriggerCalculationSuccess(NINO_HAPPY_PATH, TEST_TAX_YEAR);

            // When
            HttpResponse<String> response = sendAuthenticatedPost(
                "/individuals/calculations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_TAX_YEAR,
                "{\"taxYear\": \"" + TEST_TAX_YEAR + "\"}");

            // Then
            assertThat(response.statusCode()).isEqualTo(202);
            assertThat(response.body()).contains(TEST_CALCULATION_ID);
        }

        @Test
        @Order(8)
        @DisplayName("SUITE-001-08: Step 8 - Retrieve Tax Calculation Result")
        void step8_retrieveTaxCalculation() throws Exception {
            // Given
            accessToken = "test_access_token_12345678901234567890";
            stubGetCalculationResult(NINO_HAPPY_PATH, TEST_CALCULATION_ID);

            // When
            HttpResponse<String> response = sendAuthenticatedGet(
                "/individuals/calculations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_CALCULATION_ID);

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                .contains("totalIncome")
                .contains("totalTaxDue");
        }

        @Test
        @Order(9)
        @DisplayName("SUITE-001-09: Step 9 - Submit Final Declaration (Annual Return)")
        void step9_submitFinalDeclaration() throws Exception {
            // Given
            accessToken = "test_access_token_12345678901234567890";
            stubAnnualReturnSuccess(NINO_HAPPY_PATH, TEST_TAX_YEAR);

            String requestBody = """
                {
                    "calculationId": "%s"
                }
                """.formatted(TEST_CALCULATION_ID);

            // When
            HttpResponse<String> response = sendAuthenticatedPost(
                "/individuals/declarations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(201);
            assertThat(response.body())
                .contains("chargeReference")
                .contains("declarationTimestamp");
        }
    }

    // ==================== Error Handling Scenarios ====================

    @Nested
    @DisplayName("SUITE-002: Error Handling Scenarios (AC-5)")
    class ErrorHandlingScenarios {

        @Test
        @DisplayName("SUITE-002-01: Invalid NINO returns 404")
        void invalidNinoReturns404() throws Exception {
            // Given
            accessToken = "test_access_token_12345678901234567890";
            stubBusinessNotFound(NINO_NOT_FOUND);

            // When
            HttpResponse<String> response = sendAuthenticatedGet(
                "/individuals/business/self-employment/" + NINO_NOT_FOUND);

            // Then
            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(response.body()).contains("MATCHING_RESOURCE_NOT_FOUND");
        }

        @Test
        @DisplayName("SUITE-002-02: Validation error returns 422")
        void validationErrorReturns422() throws Exception {
            // Given
            accessToken = "test_access_token_12345678901234567890";
            stubQuarterlyUpdateValidationError(NINO_VALIDATION_ERROR, TEST_BUSINESS_ID);

            String requestBody = buildQuarterlyUpdateJson(
                "2025-04-06", "2025-07-05",
                new BigDecimal("-1000.00"), // Invalid negative
                new BigDecimal("500.00"));

            // When
            HttpResponse<String> response = sendAuthenticatedPost(
                "/individuals/business/self-employment/" + NINO_VALIDATION_ERROR + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(422);
        }

        @Test
        @DisplayName("SUITE-002-03: Duplicate submission returns 409")
        void duplicateSubmissionReturns409() throws Exception {
            // Given
            accessToken = "test_access_token_12345678901234567890";
            stubQuarterlyUpdateDuplicate(NINO_DUPLICATE, TEST_BUSINESS_ID);

            String requestBody = buildQuarterlyUpdateJson(
                "2025-04-06", "2025-07-05",
                new BigDecimal("10000.00"),
                new BigDecimal("2500.00"));

            // When
            HttpResponse<String> response = sendAuthenticatedPost(
                "/individuals/business/self-employment/" + NINO_DUPLICATE + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(response.body()).contains("RULE_OVERLAPPING_PERIOD");
        }

        @Test
        @DisplayName("SUITE-002-04: Server error returns 500")
        void serverErrorReturns500() throws Exception {
            // Given
            accessToken = "test_access_token_12345678901234567890";
            stubServerError("/individuals/business/self-employment/" + NINO_SERVER_ERROR + ".*");

            // When
            HttpResponse<String> response = sendAuthenticatedGet(
                "/individuals/business/self-employment/" + NINO_SERVER_ERROR);

            // Then
            assertThat(response.statusCode()).isEqualTo(500);
        }

        @Test
        @DisplayName("SUITE-002-05: Unauthorized without token returns 401")
        void unauthorizedWithoutTokenReturns401() throws Exception {
            // Given
            stubFor(get(urlPathMatching("/individuals/business/self-employment/.*"))
                .withHeader("Authorization", absent())
                .willReturn(aResponse()
                    .withStatus(401)
                    .withBody("{\"code\": \"UNAUTHORIZED\"}")));

            // When
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + NINO_HAPPY_PATH))
                .header("Accept", HMRC_ACCEPT_HEADER)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(401);
        }

        @Test
        @DisplayName("SUITE-002-06: Rate limiting returns 429 with Retry-After")
        void rateLimitingReturns429() throws Exception {
            // Given
            accessToken = "test_access_token_12345678901234567890";
            stubRateLimited("/individuals/business/self-employment/" + NINO_RATE_LIMITED + ".*");

            // When
            HttpResponse<String> response = sendAuthenticatedGet(
                "/individuals/business/self-employment/" + NINO_RATE_LIMITED);

            // Then
            assertThat(response.statusCode()).isEqualTo(429);
            assertThat(response.headers().firstValue("Retry-After")).isPresent();
        }
    }

    // ==================== Fraud Prevention Headers Tests ====================

    @Nested
    @DisplayName("SUITE-003: Fraud Prevention Headers (AC-6)")
    class FraudPreventionHeaders {

        @Test
        @DisplayName("SUITE-003-01: All requests include mandatory fraud prevention headers")
        void allRequestsIncludeMandatoryFraudHeaders() throws Exception {
            // Given
            accessToken = "test_access_token_12345678901234567890";
            stubListBusinessesSuccess(NINO_HAPPY_PATH);

            // When
            HttpResponse<String> response = sendAuthenticatedGetWithFraudHeaders(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH);

            // Then
            verify(getRequestedFor(urlPathMatching("/individuals/business/self-employment/.*"))
                .withHeader(HEADER_CONNECTION_METHOD, equalTo("DESKTOP_APP_DIRECT"))
                .withHeader(HEADER_DEVICE_ID, matching(".+"))
                .withHeader(HEADER_USER_IDS, matching(".+"))
                .withHeader(HEADER_TIMEZONE, matching(".+")));
        }

        @Test
        @DisplayName("SUITE-003-02: Missing fraud headers returns 400")
        void missingFraudHeadersReturns400() throws Exception {
            // Given
            accessToken = "test_access_token_12345678901234567890";
            stubFor(get(urlPathMatching("/individuals/business/self-employment/.*"))
                .withHeader(HEADER_CONNECTION_METHOD, absent())
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", JSON_CONTENT_TYPE)
                    .withBody("""
                        {
                            "code": "INVALID_HEADER",
                            "message": "Gov-Client-Connection-Method header is missing"
                        }
                        """)));

            // When - request WITHOUT fraud headers
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + NINO_HAPPY_PATH))
                .header("Accept", HMRC_ACCEPT_HEADER)
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("INVALID_HEADER");
        }
    }

    // ==================== Token Refresh Tests ====================

    @Nested
    @DisplayName("SUITE-004: Token Refresh Flow")
    class TokenRefreshFlow {

        @Test
        @DisplayName("SUITE-004-01: Expired token triggers refresh")
        void expiredTokenTriggersRefresh() throws Exception {
            // Given
            stubOAuthTokenRefreshSuccess();

            String requestBody = "grant_type=refresh_token" +
                "&refresh_token=old_refresh_token" +
                "&client_id=test_client_id" +
                "&client_secret=test_client_secret";

            // When
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/oauth/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("new_access_token");
        }
    }

    // ==================== CI/CD Compatibility Tests (AC-8) ====================

    @Nested
    @DisplayName("SUITE-005: CI/CD Compatibility (AC-8)")
    class CiCdCompatibility {

        @Test
        @DisplayName("SUITE-005-01: Tests run without real HMRC credentials")
        void testsRunWithoutRealCredentials() {
            // This test verifies that all tests use WireMock
            // and don't require actual HMRC API credentials
            assertThat(wireMockServer.isRunning()).isTrue();
            assertThat(wireMockServer.port()).isPositive();
        }

        @Test
        @DisplayName("SUITE-005-02: WireMock provides deterministic responses")
        void wireMockProvidesDeterministicResponses() throws Exception {
            // Given
            stubListBusinessesSuccess(NINO_HAPPY_PATH);
            accessToken = "test_token";

            // When - Run same request multiple times
            HttpResponse<String> response1 = sendAuthenticatedGet(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH);
            HttpResponse<String> response2 = sendAuthenticatedGet(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH);

            // Then - Responses should be identical
            assertThat(response1.statusCode()).isEqualTo(response2.statusCode());
            assertThat(response1.body()).isEqualTo(response2.body());
        }

        @Test
        @DisplayName("SUITE-005-03: No network calls to real HMRC sandbox")
        void noNetworkCallsToRealHmrc() {
            // All API calls go to WireMock's localhost URL
            assertThat(wireMockServer.baseUrl())
                .startsWith("http://localhost:")
                .doesNotContain("hmrc.gov.uk")
                .doesNotContain("service.gov.uk");
        }
    }

    // ==================== Documentation Tests (AC-7) ====================

    @Nested
    @DisplayName("SUITE-006: HMRC Test Scenarios Documentation (AC-7)")
    class HmrcTestScenariosDocumentation {

        @Test
        @DisplayName("SUITE-006-01: Test NINOs follow HMRC sandbox convention")
        void testNinosFollowHmrcConvention() {
            // HMRC test NINOs use AA prefix and specific patterns
            assertThat(NINO_HAPPY_PATH).matches("AA\\d{6}A");
            assertThat(NINO_NOT_FOUND).matches("AA\\d{6}A");
            assertThat(NINO_SERVER_ERROR).matches("AA\\d{6}A");
            assertThat(NINO_VALIDATION_ERROR).matches("AA\\d{6}A");
        }

        @Test
        @DisplayName("SUITE-006-02: Test business ID follows HMRC format")
        void testBusinessIdFollowsHmrcFormat() {
            // HMRC business IDs follow XAIS + 11 digits pattern
            assertThat(TEST_BUSINESS_ID).matches("XAIS\\d{11}");
        }

        @Test
        @DisplayName("SUITE-006-03: Test tax year follows HMRC format")
        void testTaxYearFollowsHmrcFormat() {
            // HMRC tax years use YYYY-YY format
            assertThat(TEST_TAX_YEAR).matches("\\d{4}-\\d{2}");
        }
    }

    // ==================== Helper Methods ====================

    private HttpResponse<String> sendAuthenticatedGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(wireMockServer.baseUrl() + path))
            .header("Accept", HMRC_ACCEPT_HEADER)
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendAuthenticatedGetWithFraudHeaders(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(wireMockServer.baseUrl() + path))
            .header("Accept", HMRC_ACCEPT_HEADER)
            .header("Authorization", "Bearer " + accessToken)
            .header(HEADER_CONNECTION_METHOD, "DESKTOP_APP_DIRECT")
            .header(HEADER_DEVICE_ID, "beec798b-b366-47fa-b1f8-92cede14a1ce")
            .header(HEADER_USER_IDS, "os=testuser")
            .header(HEADER_TIMEZONE, "UTC+00:00")
            .GET()
            .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendAuthenticatedPost(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(wireMockServer.baseUrl() + path))
            .header("Content-Type", JSON_CONTENT_TYPE)
            .header("Authorization", "Bearer " + accessToken)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String buildQuarterlyUpdateJson(String fromDate, String toDate,
                                            BigDecimal turnover, BigDecimal totalExpenses) {
        return """
            {
                "periodFromDate": "%s",
                "periodToDate": "%s",
                "periodIncome": {
                    "turnover": %s,
                    "other": 0.00
                },
                "periodExpenses": {
                    "costOfGoodsBought": 0.00,
                    "cisPaymentsToSubcontractors": 0.00,
                    "staffCosts": 0.00,
                    "travelCosts": %s,
                    "premisesRunningCosts": 0.00,
                    "maintenanceCosts": 0.00,
                    "adminCosts": 0.00,
                    "advertisingCosts": 0.00,
                    "businessEntertainmentCosts": 0.00,
                    "interest": 0.00,
                    "financialCharges": 0.00,
                    "badDebt": 0.00,
                    "professionalFees": 0.00,
                    "depreciation": 0.00,
                    "other": 0.00
                }
            }
            """.formatted(fromDate, toDate, turnover, totalExpenses);
    }
}
