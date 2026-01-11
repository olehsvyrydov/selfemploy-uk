package uk.selfemploy.hmrc.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static uk.selfemploy.hmrc.integration.HmrcWireMockStubs.*;

/**
 * Quarterly Update Integration Tests for HMRC Sandbox.
 *
 * <p>Tests AC-3: Quarterly Update submission (all 4 quarters).
 *
 * <h3>MTD Quarterly Update Requirements:</h3>
 * <ul>
 *     <li>Q1: 6 April - 5 July (deadline: 5 August)</li>
 *     <li>Q2: 6 July - 5 October (deadline: 5 November)</li>
 *     <li>Q3: 6 October - 5 January (deadline: 5 February)</li>
 *     <li>Q4: 6 January - 5 April (deadline: 5 May)</li>
 * </ul>
 *
 * <h3>Cumulative Totals:</h3>
 * <p>Each quarter submission should include CUMULATIVE totals from the start
 * of the tax year, not just the quarter's figures.</p>
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/self-employment-business-api">
 *     HMRC Self-Employment Business API</a>
 */
@DisplayName("Quarterly Update Integration Tests (AC-3)")
@Tag("integration")
@Tag("quarterly-update")
@Tag("sandbox")
class QuarterlyUpdateIntegrationTest {

    private static WireMockServer wireMockServer;
    private HttpClient httpClient;

    private static final String ACCESS_TOKEN = "test_access_token_12345";
    private static final String JSON_CONTENT_TYPE = "application/json";

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

    // ==================== Q1 Submission Tests ====================

    @Nested
    @DisplayName("QTR-001: Q1 Submission (6 April - 5 July)")
    class Q1Submission {

        @Test
        @DisplayName("QTR-001-01: Successfully submit Q1 update")
        void successfullySubmitQ1Update() throws Exception {
            // Given
            stubQuarterlyUpdateSuccess(NINO_HAPPY_PATH, TEST_BUSINESS_ID);

            String requestBody = buildQuarterlyUpdateJson(
                "2025-04-06", "2025-07-05",
                new BigDecimal("10000.00"), new BigDecimal("2500.00"));

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(201);
            assertThat(response.body())
                .contains("ACCEPTED")
                .contains("QTR-2025-Q1");
        }

        @Test
        @DisplayName("QTR-001-02: Q1 includes correct period dates")
        void q1IncludesCorrectPeriodDates() throws Exception {
            // Given
            stubQuarterlyUpdateSuccess(NINO_HAPPY_PATH, TEST_BUSINESS_ID);

            String requestBody = buildQuarterlyUpdateJson(
                "2025-04-06", "2025-07-05",
                new BigDecimal("10000.00"), new BigDecimal("2500.00"));

            // When
            sendPostRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then - Verify period dates are included (flexible whitespace)
            verify(postRequestedFor(urlPathMatching("/individuals/business/self-employment/.*"))
                .withRequestBody(containing("periodFromDate"))
                .withRequestBody(containing("2025-04-06"))
                .withRequestBody(containing("periodToDate"))
                .withRequestBody(containing("2025-07-05")));
        }
    }

    // ==================== Q2 Submission Tests ====================

    @Nested
    @DisplayName("QTR-002: Q2 Submission (6 July - 5 October)")
    class Q2Submission {

        @Test
        @DisplayName("QTR-002-01: Successfully submit Q2 update")
        void successfullySubmitQ2Update() throws Exception {
            // Given
            stubFor(post(urlPathMatching("/individuals/business/self-employment/.*/period"))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", JSON_CONTENT_TYPE)
                    .withBody("""
                        {
                            "id": "QTR-2025-Q2-DEF456GHI789",
                            "status": "ACCEPTED",
                            "periodFromDate": "2025-07-06",
                            "periodToDate": "2025-10-05"
                        }
                        """)));

            String requestBody = buildQuarterlyUpdateJson(
                "2025-07-06", "2025-10-05",
                new BigDecimal("25000.00"), new BigDecimal("6000.00")); // Cumulative

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(201);
            assertThat(response.body())
                .contains("ACCEPTED")
                .contains("QTR-2025-Q2");
        }
    }

    // ==================== Q3 Submission Tests ====================

    @Nested
    @DisplayName("QTR-003: Q3 Submission (6 October - 5 January)")
    class Q3Submission {

        @Test
        @DisplayName("QTR-003-01: Successfully submit Q3 update")
        void successfullySubmitQ3Update() throws Exception {
            // Given
            stubFor(post(urlPathMatching("/individuals/business/self-employment/.*/period"))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", JSON_CONTENT_TYPE)
                    .withBody("""
                        {
                            "id": "QTR-2025-Q3-GHI789JKL012",
                            "status": "ACCEPTED",
                            "periodFromDate": "2025-10-06",
                            "periodToDate": "2026-01-05"
                        }
                        """)));

            String requestBody = buildQuarterlyUpdateJson(
                "2025-10-06", "2026-01-05",
                new BigDecimal("38000.00"), new BigDecimal("10000.00")); // Cumulative

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(201);
            assertThat(response.body())
                .contains("ACCEPTED")
                .contains("QTR-2025-Q3");
        }
    }

    // ==================== Q4 Submission Tests ====================

    @Nested
    @DisplayName("QTR-004: Q4 Submission (6 January - 5 April)")
    class Q4Submission {

        @Test
        @DisplayName("QTR-004-01: Successfully submit Q4 update")
        void successfullySubmitQ4Update() throws Exception {
            // Given
            stubFor(post(urlPathMatching("/individuals/business/self-employment/.*/period"))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", JSON_CONTENT_TYPE)
                    .withBody("""
                        {
                            "id": "QTR-2025-Q4-JKL012MNO345",
                            "status": "ACCEPTED",
                            "periodFromDate": "2026-01-06",
                            "periodToDate": "2026-04-05"
                        }
                        """)));

            String requestBody = buildQuarterlyUpdateJson(
                "2026-01-06", "2026-04-05",
                new BigDecimal("50000.00"), new BigDecimal("13000.00")); // Cumulative - full year

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(201);
            assertThat(response.body())
                .contains("ACCEPTED")
                .contains("QTR-2025-Q4");
        }
    }

    // ==================== All Quarters Parameterized Test ====================

    @Nested
    @DisplayName("QTR-005: All Quarters Submission")
    class AllQuartersSubmission {

        @ParameterizedTest
        @DisplayName("QTR-005-01: Successfully submit all 4 quarters")
        @CsvSource({
            "Q1, 2025-04-06, 2025-07-05, 10000.00, 2500.00",
            "Q2, 2025-07-06, 2025-10-05, 25000.00, 6000.00",
            "Q3, 2025-10-06, 2026-01-05, 38000.00, 10000.00",
            "Q4, 2026-01-06, 2026-04-05, 50000.00, 13000.00"
        })
        void successfullySubmitAllQuarters(String quarter, String fromDate, String toDate,
                                           BigDecimal cumulativeIncome, BigDecimal cumulativeExpenses) throws Exception {
            // Given
            stubFor(post(urlPathMatching("/individuals/business/self-employment/.*/period"))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", JSON_CONTENT_TYPE)
                    .withBody("""
                        {
                            "id": "QTR-2025-%s-ABC123",
                            "status": "ACCEPTED",
                            "periodFromDate": "%s",
                            "periodToDate": "%s"
                        }
                        """.formatted(quarter, fromDate, toDate))));

            String requestBody = buildQuarterlyUpdateJson(fromDate, toDate, cumulativeIncome, cumulativeExpenses);

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(201);
            assertThat(response.body()).contains("ACCEPTED");
        }
    }

    // ==================== Cumulative Totals Tests ====================

    @Nested
    @DisplayName("QTR-006: Cumulative Totals")
    class CumulativeTotals {

        @Test
        @DisplayName("QTR-006-01: Q2 totals include Q1 figures")
        void q2TotalsIncludeQ1Figures() throws Exception {
            // Given
            BigDecimal q1Income = new BigDecimal("10000.00");
            BigDecimal q2QuarterIncome = new BigDecimal("15000.00");
            BigDecimal q2CumulativeIncome = q1Income.add(q2QuarterIncome); // 25000.00

            stubQuarterlyUpdateSuccess(NINO_HAPPY_PATH, TEST_BUSINESS_ID);

            String requestBody = buildQuarterlyUpdateJson(
                "2025-07-06", "2025-10-05",
                q2CumulativeIncome, new BigDecimal("6000.00"));

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isIn(200, 201);

            verify(postRequestedFor(urlPathMatching("/individuals/business/self-employment/.*/period"))
                .withRequestBody(containing("25000.00"))); // Cumulative total
        }

        @Test
        @DisplayName("QTR-006-02: Q4 totals represent full year")
        void q4TotalsRepresentFullYear() throws Exception {
            // Given - Full year cumulative totals
            BigDecimal fullYearIncome = new BigDecimal("50000.00");
            BigDecimal fullYearExpenses = new BigDecimal("13000.00");

            stubQuarterlyUpdateSuccess(NINO_HAPPY_PATH, TEST_BUSINESS_ID);

            String requestBody = buildQuarterlyUpdateJson(
                "2026-01-06", "2026-04-05",
                fullYearIncome, fullYearExpenses);

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isIn(200, 201);
        }
    }

    // ==================== Duplicate Submission Tests ====================

    @Nested
    @DisplayName("QTR-007: Duplicate Submission Error")
    class DuplicateSubmission {

        @Test
        @DisplayName("QTR-007-01: Duplicate period submission returns 409")
        void duplicatePeriodSubmissionReturns409() throws Exception {
            // Given
            stubQuarterlyUpdateDuplicate(NINO_DUPLICATE, TEST_BUSINESS_ID);

            String requestBody = buildQuarterlyUpdateJson(
                "2025-04-06", "2025-07-05",
                new BigDecimal("10000.00"), new BigDecimal("2500.00"));

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/business/self-employment/" + NINO_DUPLICATE + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(response.body())
                .contains("RULE_OVERLAPPING_PERIOD")
                .contains("already exists");
        }
    }

    // ==================== Validation Error Tests ====================

    @Nested
    @DisplayName("QTR-008: Validation Errors (422)")
    class ValidationErrors {

        @Test
        @DisplayName("QTR-008-01: Invalid income value returns 422")
        void invalidIncomeValueReturns422() throws Exception {
            // Given
            stubQuarterlyUpdateValidationError(NINO_VALIDATION_ERROR, TEST_BUSINESS_ID);

            String requestBody = buildQuarterlyUpdateJson(
                "2025-04-06", "2025-07-05",
                new BigDecimal("-1000.00"), // Negative value - invalid
                new BigDecimal("2500.00"));

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/business/self-employment/" + NINO_VALIDATION_ERROR + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(422);
            assertThat(response.body()).contains("FORMAT_VALUE");
        }

        @Test
        @DisplayName("QTR-008-02: Missing required field returns 422")
        void missingRequiredFieldReturns422() throws Exception {
            // Given
            stubFor(post(urlPathMatching("/individuals/business/self-employment/.*/period"))
                .willReturn(aResponse()
                    .withStatus(422)
                    .withHeader("Content-Type", JSON_CONTENT_TYPE)
                    .withBody("""
                        {
                            "code": "RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED",
                            "message": "An empty or non-matching body was submitted",
                            "errors": [
                                {
                                    "code": "MISSING_FIELD",
                                    "message": "Required field is missing",
                                    "paths": ["/periodFromDate"]
                                }
                            ]
                        }
                        """)));

            // Incomplete request body
            String requestBody = """
                {
                    "periodToDate": "2025-07-05",
                    "periodIncome": {"turnover": 10000.00}
                }
                """;

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(422);
            assertThat(response.body()).contains("MISSING_FIELD");
        }

        @Test
        @DisplayName("QTR-008-03: Invalid date range returns 422")
        void invalidDateRangeReturns422() throws Exception {
            // Given
            stubFor(post(urlPathMatching("/individuals/business/self-employment/.*/period"))
                .willReturn(aResponse()
                    .withStatus(422)
                    .withHeader("Content-Type", JSON_CONTENT_TYPE)
                    .withBody("""
                        {
                            "code": "RULE_END_DATE_BEFORE_START_DATE",
                            "message": "The end date must be after the start date"
                        }
                        """)));

            String requestBody = buildQuarterlyUpdateJson(
                "2025-07-05", "2025-04-06", // Reversed dates
                new BigDecimal("10000.00"), new BigDecimal("2500.00"));

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(422);
            assertThat(response.body()).contains("END_DATE_BEFORE_START_DATE");
        }
    }

    // ==================== Amendment Tests ====================

    @Nested
    @DisplayName("QTR-009: Quarterly Update Amendment")
    class QuarterlyUpdateAmendment {

        @Test
        @DisplayName("QTR-009-01: Successfully amend existing quarterly update")
        void successfullyAmendExistingUpdate() throws Exception {
            // Given
            String periodId = "QTR-2025-Q1-ABC123DEF456";
            stubQuarterlyUpdateAmendSuccess(NINO_HAPPY_PATH, TEST_BUSINESS_ID, periodId);

            String requestBody = buildQuarterlyUpdateJson(
                "2025-04-06", "2025-07-05",
                new BigDecimal("12000.00"), // Updated income
                new BigDecimal("3000.00")); // Updated expenses

            // When
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() +
                    "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period/" + periodId))
                .header("Content-Type", JSON_CONTENT_TYPE)
                .header("Authorization", "Bearer " + ACCESS_TOKEN)
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("ACCEPTED");
        }
    }

    // ==================== Request Format Tests ====================

    @Nested
    @DisplayName("QTR-010: Request Format")
    class RequestFormat {

        @Test
        @DisplayName("QTR-010-01: Request body contains required fields")
        void requestBodyContainsRequiredFields() throws Exception {
            // Given
            stubQuarterlyUpdateSuccess(NINO_HAPPY_PATH, TEST_BUSINESS_ID);

            String requestBody = buildQuarterlyUpdateJson(
                "2025-04-06", "2025-07-05",
                new BigDecimal("10000.00"), new BigDecimal("2500.00"));

            // When
            sendPostRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            verify(postRequestedFor(urlPathMatching("/individuals/business/self-employment/.*/period"))
                .withHeader("Content-Type", equalTo(JSON_CONTENT_TYPE))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(containing("periodFromDate"))
                .withRequestBody(containing("periodToDate"))
                .withRequestBody(containing("periodIncome"))
                .withRequestBody(containing("turnover")));
        }

        @Test
        @DisplayName("QTR-010-02: Response includes submission reference")
        void responseIncludesSubmissionReference() throws Exception {
            // Given
            stubQuarterlyUpdateSuccess(NINO_HAPPY_PATH, TEST_BUSINESS_ID);

            String requestBody = buildQuarterlyUpdateJson(
                "2025-04-06", "2025-07-05",
                new BigDecimal("10000.00"), new BigDecimal("2500.00"));

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.body()).contains("\"id\":");
        }
    }

    // ==================== HMRC Sandbox Test Scenarios ====================

    @Nested
    @DisplayName("QTR-011: HMRC Sandbox Test Scenarios")
    class HmrcSandboxScenarios {

        @Test
        @DisplayName("QTR-011-01: Happy path NINO (AA000001A) returns 201")
        void happyPathNinoReturns201() throws Exception {
            // Given
            stubQuarterlyUpdateSuccess(NINO_HAPPY_PATH, TEST_BUSINESS_ID);

            String requestBody = buildQuarterlyUpdateJson(
                "2025-04-06", "2025-07-05",
                new BigDecimal("10000.00"), new BigDecimal("2500.00"));

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/business/self-employment/" + NINO_HAPPY_PATH + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(201);
        }

        @Test
        @DisplayName("QTR-011-02: Validation error NINO (AA000422A) returns 422")
        void validationErrorNinoReturns422() throws Exception {
            // Given
            stubQuarterlyUpdateValidationError(NINO_VALIDATION_ERROR, TEST_BUSINESS_ID);

            String requestBody = buildQuarterlyUpdateJson(
                "2025-04-06", "2025-07-05",
                new BigDecimal("10000.00"), new BigDecimal("2500.00"));

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/business/self-employment/" + NINO_VALIDATION_ERROR + "/" + TEST_BUSINESS_ID + "/period",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(422);
        }
    }

    // ==================== Helper Methods ====================

    private HttpResponse<String> sendPostRequest(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(wireMockServer.baseUrl() + path))
            .header("Content-Type", JSON_CONTENT_TYPE)
            .header("Authorization", "Bearer " + ACCESS_TOKEN)
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
