package uk.selfemploy.hmrc.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static uk.selfemploy.hmrc.integration.HmrcWireMockStubs.*;

/**
 * Business Details API Integration Tests for HMRC Sandbox.
 *
 * <p>Tests AC-2: Business Details API retrieval.
 *
 * <h3>HMRC Sandbox Test Scenarios:</h3>
 * <table>
 *     <tr><th>NINO</th><th>Expected Result</th></tr>
 *     <tr><td>AA000001A</td><td>Success - returns business details</td></tr>
 *     <tr><td>AA000404A</td><td>404 Not Found</td></tr>
 *     <tr><td>AA000401A</td><td>401 Unauthorized</td></tr>
 *     <tr><td>AA000403A</td><td>403 Forbidden</td></tr>
 *     <tr><td>AA000500A</td><td>500 Server Error</td></tr>
 * </table>
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/self-employment-business-api">
 *     HMRC Self-Employment Business API</a>
 */
@DisplayName("Business Details API Integration Tests (AC-2)")
@Tag("integration")
@Tag("business-details")
@Tag("sandbox")
class BusinessDetailsIntegrationTest {

    private static WireMockServer wireMockServer;
    private HttpClient httpClient;

    private static final String ACCESS_TOKEN = "test_access_token_12345";
    private static final String HMRC_ACCEPT_HEADER = "application/vnd.hmrc.1.0+json";

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

    // ==================== List Businesses Tests ====================

    @Nested
    @DisplayName("BIZ-API-001: List Businesses")
    class ListBusinesses {

        @Test
        @DisplayName("BIZ-API-001-01: Successfully list all businesses for NINO")
        void successfullyListAllBusinesses() throws Exception {
            // Given
            stubListBusinessesSuccess(NINO_HAPPY_PATH);

            // When
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + NINO_HAPPY_PATH))
                .header("Accept", HMRC_ACCEPT_HEADER)
                .header("Authorization", "Bearer " + ACCESS_TOKEN)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                .contains("selfEmployments")
                .contains(TEST_BUSINESS_ID)
                .contains("Test Business Ltd")
                .contains("self-employment");
        }

        @Test
        @DisplayName("BIZ-API-001-02: Response includes business ID")
        void responseIncludesBusinessId() throws Exception {
            // Given
            stubListBusinessesSuccess(NINO_HAPPY_PATH);

            // When
            HttpResponse<String> response = sendGetRequest("/individuals/business/self-employment/" + NINO_HAPPY_PATH);

            // Then - Check for businessId with flexible whitespace
            assertThat(response.body())
                .contains("businessId")
                .contains(TEST_BUSINESS_ID);
        }

        @Test
        @DisplayName("BIZ-API-001-03: Response includes trading name")
        void responseIncludesTradingName() throws Exception {
            // Given
            stubListBusinessesSuccess(NINO_HAPPY_PATH);

            // When
            HttpResponse<String> response = sendGetRequest("/individuals/business/self-employment/" + NINO_HAPPY_PATH);

            // Then - Check for tradingName with flexible whitespace
            assertThat(response.body())
                .contains("tradingName")
                .contains("Test Business Ltd");
        }

        @Test
        @DisplayName("BIZ-API-001-04: Response includes accounting periods")
        void responseIncludesAccountingPeriods() throws Exception {
            // Given
            stubListBusinessesSuccess(NINO_HAPPY_PATH);

            // When
            HttpResponse<String> response = sendGetRequest("/individuals/business/self-employment/" + NINO_HAPPY_PATH);

            // Then
            assertThat(response.body())
                .contains("accountingPeriods")
                .contains("2025-04-06")
                .contains("2026-04-05");
        }

        @Test
        @DisplayName("BIZ-API-001-05: Response includes address information")
        void responseIncludesAddressInformation() throws Exception {
            // Given
            stubListBusinessesSuccess(NINO_HAPPY_PATH);

            // When
            HttpResponse<String> response = sendGetRequest("/individuals/business/self-employment/" + NINO_HAPPY_PATH);

            // Then
            assertThat(response.body())
                .contains("123 Test Street")
                .contains("AB12 3CD")
                .contains("GB");
        }
    }

    // ==================== Get Single Business Tests ====================

    @Nested
    @DisplayName("BIZ-API-002: Get Single Business Details")
    class GetSingleBusiness {

        @Test
        @DisplayName("BIZ-API-002-01: Successfully retrieve single business by ID")
        void successfullyRetrieveSingleBusiness() throws Exception {
            // Given
            stubGetBusinessSuccess(NINO_HAPPY_PATH, TEST_BUSINESS_ID);

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID);

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                .contains(TEST_BUSINESS_ID)
                .contains("Test Business Ltd");
        }

        @Test
        @DisplayName("BIZ-API-002-02: Single business includes full details")
        void singleBusinessIncludesFullDetails() throws Exception {
            // Given
            stubGetBusinessSuccess(NINO_HAPPY_PATH, TEST_BUSINESS_ID);

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID);

            // Then
            assertThat(response.body())
                .contains("businessId")
                .contains("typeOfBusiness")
                .contains("tradingName")
                .contains("tradingStartDate")
                .contains("accountingPeriods")
                .contains("addressLineOne")
                .contains("postalCode");
        }
    }

    // ==================== Not Found Tests ====================

    @Nested
    @DisplayName("BIZ-API-003: Business Not Found (404)")
    class BusinessNotFound {

        @Test
        @DisplayName("BIZ-API-003-01: NINO with no businesses returns 404")
        void ninoWithNoBusinessesReturns404() throws Exception {
            // Given
            stubBusinessNotFound(NINO_NOT_FOUND);

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/business/self-employment/" + NINO_NOT_FOUND);

            // Then
            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(response.body())
                .contains("MATCHING_RESOURCE_NOT_FOUND")
                .contains("No self-employment business found");
        }

        @Test
        @DisplayName("BIZ-API-003-02: Non-existent business ID returns 404")
        void nonExistentBusinessIdReturns404() throws Exception {
            // Given
            String nonExistentBusinessId = "XAIS00000000000";
            stubFor(get(urlPathEqualTo("/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + nonExistentBusinessId))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "code": "MATCHING_RESOURCE_NOT_FOUND",
                            "message": "Business not found"
                        }
                        """)));

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + nonExistentBusinessId);

            // Then
            assertThat(response.statusCode()).isEqualTo(404);
        }
    }

    // ==================== Unauthorized Tests ====================

    @Nested
    @DisplayName("BIZ-API-004: Unauthorized (401)")
    class Unauthorized {

        @Test
        @DisplayName("BIZ-API-004-01: Missing Authorization header returns 401")
        void missingAuthorizationHeaderReturns401() throws Exception {
            // Given
            stubFor(get(urlPathMatching("/individuals/business/self-employment/.*"))
                .withHeader("Authorization", absent())
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
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + NINO_HAPPY_PATH))
                .header("Accept", HMRC_ACCEPT_HEADER)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.body()).contains("UNAUTHORIZED");
        }

        @Test
        @DisplayName("BIZ-API-004-02: Invalid token returns 401")
        void invalidTokenReturns401() throws Exception {
            // Given
            stubFor(get(urlPathMatching("/individuals/business/self-employment/.*"))
                .withHeader("Authorization", matching("Bearer invalid.*"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "code": "INVALID_CREDENTIALS",
                            "message": "Invalid authentication credentials"
                        }
                        """)));

            // When
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + NINO_HAPPY_PATH))
                .header("Accept", HMRC_ACCEPT_HEADER)
                .header("Authorization", "Bearer invalid_token")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(401);
        }

        @Test
        @DisplayName("BIZ-API-004-03: Expired token returns 401")
        void expiredTokenReturns401() throws Exception {
            // Given
            stubFor(get(urlPathMatching("/individuals/business/self-employment/.*"))
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
                .uri(URI.create(wireMockServer.baseUrl() + "/individuals/business/self-employment/" + NINO_HAPPY_PATH))
                .header("Accept", HMRC_ACCEPT_HEADER)
                .header("Authorization", "Bearer expired_token_12345")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.body()).contains("expired");
        }
    }

    // ==================== Forbidden Tests ====================

    @Nested
    @DisplayName("BIZ-API-005: Forbidden (403)")
    class Forbidden {

        @Test
        @DisplayName("BIZ-API-005-01: Insufficient scope returns 403")
        void insufficientScopeReturns403() throws Exception {
            // Given
            stubFor(get(urlPathMatching("/individuals/business/self-employment/" + NINO_FORBIDDEN + ".*"))
                .willReturn(aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "code": "CLIENT_OR_AGENT_NOT_AUTHORISED",
                            "message": "The client and/or agent is not authorised"
                        }
                        """)));

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/business/self-employment/" + NINO_FORBIDDEN);

            // Then
            assertThat(response.statusCode()).isEqualTo(403);
            assertThat(response.body()).contains("CLIENT_OR_AGENT_NOT_AUTHORISED");
        }
    }

    // ==================== Server Error Tests ====================

    @Nested
    @DisplayName("BIZ-API-006: Server Errors")
    class ServerErrors {

        @Test
        @DisplayName("BIZ-API-006-01: Server error returns 500")
        void serverErrorReturns500() throws Exception {
            // Given
            stubServerError("/individuals/business/self-employment/" + NINO_SERVER_ERROR + ".*");

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/business/self-employment/" + NINO_SERVER_ERROR);

            // Then
            assertThat(response.statusCode()).isEqualTo(500);
            assertThat(response.body()).contains("SERVER_ERROR");
        }

        @Test
        @DisplayName("BIZ-API-006-02: Service unavailable returns 503")
        void serviceUnavailableReturns503() throws Exception {
            // Given
            stubServiceUnavailable("/individuals/business/self-employment/.*");

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH);

            // Then
            assertThat(response.statusCode()).isEqualTo(503);
            assertThat(response.body()).contains("SERVICE_UNAVAILABLE");
            assertThat(response.headers().firstValue("Retry-After")).isPresent();
        }

        @Test
        @DisplayName("BIZ-API-006-03: Rate limited returns 429 with Retry-After header")
        void rateLimitedReturns429() throws Exception {
            // Given
            stubRateLimited("/individuals/business/self-employment/.*");

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/business/self-employment/" + NINO_RATE_LIMITED);

            // Then
            assertThat(response.statusCode()).isEqualTo(429);
            assertThat(response.body()).contains("MESSAGE_THROTTLED_OUT");
            assertThat(response.headers().firstValue("Retry-After"))
                .isPresent()
                .contains("60");
        }
    }

    // ==================== Request Format Tests ====================

    @Nested
    @DisplayName("BIZ-API-007: Request Format")
    class RequestFormat {

        @Test
        @DisplayName("BIZ-API-007-01: Request includes correct Accept header")
        void requestIncludesCorrectAcceptHeader() throws Exception {
            // Given
            stubListBusinessesSuccess(NINO_HAPPY_PATH);

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH);

            // Then
            verify(getRequestedFor(urlPathEqualTo("/individuals/business/self-employment/" + NINO_HAPPY_PATH))
                .withHeader("Accept", equalTo(HMRC_ACCEPT_HEADER)));
        }

        @Test
        @DisplayName("BIZ-API-007-02: Request includes Authorization header with Bearer prefix")
        void requestIncludesAuthorizationHeader() throws Exception {
            // Given
            stubListBusinessesSuccess(NINO_HAPPY_PATH);

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH);

            // Then
            verify(getRequestedFor(urlPathEqualTo("/individuals/business/self-employment/" + NINO_HAPPY_PATH))
                .withHeader("Authorization", matching("Bearer .+")));
        }
    }

    // ==================== HMRC Sandbox Test Scenarios ====================

    @Nested
    @DisplayName("BIZ-API-008: HMRC Sandbox Test Scenarios")
    class HmrcSandboxScenarios {

        @Test
        @DisplayName("BIZ-API-008-01: Happy path NINO (AA000001A) returns success")
        void happyPathNinoReturnsSuccess() throws Exception {
            // Given
            stubListBusinessesSuccess(NINO_HAPPY_PATH);

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH);

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("BIZ-API-008-02: Not found NINO (AA000404A) returns 404")
        void notFoundNinoReturns404() throws Exception {
            // Given
            stubBusinessNotFound(NINO_NOT_FOUND);

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/business/self-employment/" + NINO_NOT_FOUND);

            // Then
            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        @DisplayName("BIZ-API-008-03: Server error NINO (AA000500A) returns 500")
        void serverErrorNinoReturns500() throws Exception {
            // Given
            stubServerError("/individuals/business/self-employment/" + NINO_SERVER_ERROR + ".*");

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/business/self-employment/" + NINO_SERVER_ERROR);

            // Then
            assertThat(response.statusCode()).isEqualTo(500);
        }
    }

    // ==================== Helper Methods ====================

    private HttpResponse<String> sendGetRequest(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(wireMockServer.baseUrl() + path))
            .header("Accept", HMRC_ACCEPT_HEADER)
            .header("Authorization", "Bearer " + ACCESS_TOKEN)
            .GET()
            .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
