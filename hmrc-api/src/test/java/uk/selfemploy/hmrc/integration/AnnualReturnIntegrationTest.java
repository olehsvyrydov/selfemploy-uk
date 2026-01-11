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
 * Annual Return (Final Declaration) Integration Tests for HMRC Sandbox.
 *
 * <p>Tests AC-4: Annual Return submission.
 *
 * <h3>MTD Final Declaration Process:</h3>
 * <ol>
 *     <li>All 4 quarterly updates must be submitted</li>
 *     <li>Trigger a tax calculation</li>
 *     <li>User reviews the calculation</li>
 *     <li>Submit final declaration (crystallisation)</li>
 * </ol>
 *
 * <h3>Key Deadlines:</h3>
 * <ul>
 *     <li>Paper returns: 31 October following the tax year</li>
 *     <li>Online returns: 31 January following the tax year</li>
 * </ul>
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individual-calculations-api">
 *     HMRC Individual Calculations API</a>
 */
@DisplayName("Annual Return Integration Tests (AC-4)")
@Tag("integration")
@Tag("annual-return")
@Tag("sandbox")
class AnnualReturnIntegrationTest {

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

    // ==================== Tax Calculation Trigger Tests ====================

    @Nested
    @DisplayName("ANNUAL-001: Tax Calculation Trigger")
    class TaxCalculationTrigger {

        @Test
        @DisplayName("ANNUAL-001-01: Successfully trigger tax calculation")
        void successfullyTriggerTaxCalculation() throws Exception {
            // Given
            stubTriggerCalculationSuccess(NINO_HAPPY_PATH, TEST_TAX_YEAR);

            String requestBody = """
                {
                    "taxYear": "%s"
                }
                """.formatted(TEST_TAX_YEAR);

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/calculations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(202);
            assertThat(response.body()).contains("calculationId");
            assertThat(response.body()).contains(TEST_CALCULATION_ID);
        }

        @Test
        @DisplayName("ANNUAL-001-02: Calculation trigger returns calculation ID")
        void calculationTriggerReturnsCalculationId() throws Exception {
            // Given
            stubTriggerCalculationSuccess(NINO_HAPPY_PATH, TEST_TAX_YEAR);

            String requestBody = """
                {
                    "taxYear": "%s"
                }
                """.formatted(TEST_TAX_YEAR);

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/calculations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then - Check for calculationId with flexible whitespace
            assertThat(response.body())
                .contains("calculationId")
                .contains(TEST_CALCULATION_ID);
        }
    }

    // ==================== Get Calculation Result Tests ====================

    @Nested
    @DisplayName("ANNUAL-002: Get Calculation Result")
    class GetCalculationResult {

        @Test
        @DisplayName("ANNUAL-002-01: Successfully retrieve calculation result")
        void successfullyRetrieveCalculationResult() throws Exception {
            // Given
            stubGetCalculationResult(NINO_HAPPY_PATH, TEST_CALCULATION_ID);

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/calculations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_CALCULATION_ID);

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                .contains("calculationId")
                .contains("totalIncome")
                .contains("totalExpenses")
                .contains("totalTaxDue");
        }

        @Test
        @DisplayName("ANNUAL-002-02: Calculation result includes income tax breakdown")
        void calculationResultIncludesIncomeTaxBreakdown() throws Exception {
            // Given
            stubGetCalculationResult(NINO_HAPPY_PATH, TEST_CALCULATION_ID);

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/calculations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_CALCULATION_ID);

            // Then
            assertThat(response.body())
                .contains("incomeTax")
                .contains("taxBands")
                .contains("basic-rate");
        }

        @Test
        @DisplayName("ANNUAL-002-03: Calculation result includes National Insurance")
        void calculationResultIncludesNationalInsurance() throws Exception {
            // Given
            stubGetCalculationResult(NINO_HAPPY_PATH, TEST_CALCULATION_ID);

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/calculations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_CALCULATION_ID);

            // Then
            assertThat(response.body())
                .contains("nationalInsurance")
                .contains("class2")
                .contains("class4");
        }

        @Test
        @DisplayName("ANNUAL-002-04: Calculation result includes payment on account")
        void calculationResultIncludesPaymentOnAccount() throws Exception {
            // Given
            stubGetCalculationResult(NINO_HAPPY_PATH, TEST_CALCULATION_ID);

            // When
            HttpResponse<String> response = sendGetRequest(
                "/individuals/calculations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_CALCULATION_ID);

            // Then
            assertThat(response.body())
                .contains("paymentOnAccount")
                .contains("firstPayment")
                .contains("secondPayment");
        }
    }

    // ==================== Final Declaration Submission Tests ====================

    @Nested
    @DisplayName("ANNUAL-003: Final Declaration Submission")
    class FinalDeclarationSubmission {

        @Test
        @DisplayName("ANNUAL-003-01: Successfully submit final declaration")
        void successfullySubmitFinalDeclaration() throws Exception {
            // Given
            stubAnnualReturnSuccess(NINO_HAPPY_PATH, TEST_TAX_YEAR);

            String requestBody = buildFinalDeclarationRequest(TEST_CALCULATION_ID);

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/declarations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(201);
            assertThat(response.body())
                .contains("chargeReference")
                .contains("declarationTimestamp");
        }

        @Test
        @DisplayName("ANNUAL-003-02: Final declaration returns charge reference")
        void finalDeclarationReturnsChargeReference() throws Exception {
            // Given
            stubAnnualReturnSuccess(NINO_HAPPY_PATH, TEST_TAX_YEAR);

            String requestBody = buildFinalDeclarationRequest(TEST_CALCULATION_ID);

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/declarations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then - Check for chargeReference with flexible whitespace
            assertThat(response.body())
                .contains("chargeReference")
                .contains("XA123456789012");
        }

        @Test
        @DisplayName("ANNUAL-003-03: Final declaration returns timestamp")
        void finalDeclarationReturnsTimestamp() throws Exception {
            // Given
            stubAnnualReturnSuccess(NINO_HAPPY_PATH, TEST_TAX_YEAR);

            String requestBody = buildFinalDeclarationRequest(TEST_CALCULATION_ID);

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/declarations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then
            assertThat(response.body()).contains("declarationTimestamp");
        }

        @Test
        @DisplayName("ANNUAL-003-04: Final declaration requires calculation ID")
        void finalDeclarationRequiresCalculationId() throws Exception {
            // Given
            stubAnnualReturnSuccess(NINO_HAPPY_PATH, TEST_TAX_YEAR);

            String requestBody = buildFinalDeclarationRequest(TEST_CALCULATION_ID);

            // When
            sendPostRequest(
                "/individuals/declarations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then
            verify(postRequestedFor(urlPathMatching("/individuals/declarations/self-assessment/.*"))
                .withRequestBody(containing("calculationId"))
                .withRequestBody(containing(TEST_CALCULATION_ID)));
        }
    }

    // ==================== Error Handling Tests ====================

    @Nested
    @DisplayName("ANNUAL-004: Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("ANNUAL-004-01: Incomplete quarters returns 422")
        void incompleteQuartersReturns422() throws Exception {
            // Given
            stubAnnualReturnValidationError(NINO_VALIDATION_ERROR, TEST_TAX_YEAR);

            String requestBody = buildFinalDeclarationRequest(TEST_CALCULATION_ID);

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/declarations/self-assessment/" + NINO_VALIDATION_ERROR + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(422);
            assertThat(response.body())
                .contains("RULE_INCOMPLETE_SUBMISSION")
                .contains("quarterly updates must be submitted");
        }

        @Test
        @DisplayName("ANNUAL-004-02: Already submitted declaration returns 409")
        void alreadySubmittedDeclarationReturns409() throws Exception {
            // Given
            stubAnnualReturnAlreadySubmitted(NINO_DUPLICATE, TEST_TAX_YEAR);

            String requestBody = buildFinalDeclarationRequest(TEST_CALCULATION_ID);

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/declarations/self-assessment/" + NINO_DUPLICATE + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(response.body())
                .contains("RULE_ALREADY_SUBMITTED")
                .contains("already been submitted");
        }

        @Test
        @DisplayName("ANNUAL-004-03: Invalid calculation ID returns 404")
        void invalidCalculationIdReturns404() throws Exception {
            // Given
            stubFor(post(urlPathMatching("/individuals/declarations/self-assessment/.*"))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", JSON_CONTENT_TYPE)
                    .withBody("""
                        {
                            "code": "MATCHING_RESOURCE_NOT_FOUND",
                            "message": "Calculation not found"
                        }
                        """)));

            String requestBody = buildFinalDeclarationRequest("invalid-calculation-id");

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/declarations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(response.body()).contains("MATCHING_RESOURCE_NOT_FOUND");
        }

        @Test
        @DisplayName("ANNUAL-004-04: Invalid tax year format returns 400")
        void invalidTaxYearFormatReturns400() throws Exception {
            // Given
            stubFor(post(urlPathMatching("/individuals/declarations/self-assessment/.*"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", JSON_CONTENT_TYPE)
                    .withBody("""
                        {
                            "code": "FORMAT_TAX_YEAR",
                            "message": "Tax year format should be YYYY-YY"
                        }
                        """)));

            String requestBody = buildFinalDeclarationRequest(TEST_CALCULATION_ID);

            // When - Invalid tax year format (2025 instead of 2025-26)
            HttpResponse<String> response = sendPostRequest(
                "/individuals/declarations/self-assessment/" + NINO_HAPPY_PATH + "/2025",
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("FORMAT_TAX_YEAR");
        }

        @Test
        @DisplayName("ANNUAL-004-05: Missing Authorization header returns 401")
        void missingAuthorizationReturns401() throws Exception {
            // Given
            stubFor(post(urlPathMatching("/individuals/declarations/self-assessment/.*"))
                .withHeader("Authorization", absent())
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", JSON_CONTENT_TYPE)
                    .withBody("""
                        {
                            "code": "UNAUTHORIZED",
                            "message": "Bearer token is missing or invalid"
                        }
                        """)));

            String requestBody = buildFinalDeclarationRequest(TEST_CALCULATION_ID);

            // When
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() +
                    "/individuals/declarations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_TAX_YEAR))
                .header("Content-Type", JSON_CONTENT_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            assertThat(response.statusCode()).isEqualTo(401);
        }

        @Test
        @DisplayName("ANNUAL-004-06: Server error returns 500")
        void serverErrorReturns500() throws Exception {
            // Given
            stubServerError("/individuals/declarations/self-assessment/" + NINO_SERVER_ERROR + ".*");

            String requestBody = buildFinalDeclarationRequest(TEST_CALCULATION_ID);

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/declarations/self-assessment/" + NINO_SERVER_ERROR + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(500);
            assertThat(response.body()).contains("SERVER_ERROR");
        }
    }

    // ==================== Full Flow Tests ====================

    @Nested
    @DisplayName("ANNUAL-005: Complete Annual Return Flow")
    class CompleteAnnualReturnFlow {

        @Test
        @DisplayName("ANNUAL-005-01: Complete flow: trigger calculation -> get result -> submit declaration")
        void completeAnnualReturnFlow() throws Exception {
            // Given
            stubTriggerCalculationSuccess(NINO_HAPPY_PATH, TEST_TAX_YEAR);
            stubGetCalculationResult(NINO_HAPPY_PATH, TEST_CALCULATION_ID);
            stubAnnualReturnSuccess(NINO_HAPPY_PATH, TEST_TAX_YEAR);

            // Step 1: Trigger calculation
            HttpResponse<String> triggerResponse = sendPostRequest(
                "/individuals/calculations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_TAX_YEAR,
                "{\"taxYear\": \"" + TEST_TAX_YEAR + "\"}");

            assertThat(triggerResponse.statusCode()).isEqualTo(202);
            assertThat(triggerResponse.body()).contains(TEST_CALCULATION_ID);

            // Step 2: Get calculation result
            HttpResponse<String> calcResponse = sendGetRequest(
                "/individuals/calculations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_CALCULATION_ID);

            assertThat(calcResponse.statusCode()).isEqualTo(200);
            assertThat(calcResponse.body()).contains("totalTaxDue");

            // Step 3: Submit final declaration
            HttpResponse<String> declarationResponse = sendPostRequest(
                "/individuals/declarations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_TAX_YEAR,
                buildFinalDeclarationRequest(TEST_CALCULATION_ID));

            assertThat(declarationResponse.statusCode()).isEqualTo(201);
            assertThat(declarationResponse.body()).contains("chargeReference");
        }
    }

    // ==================== Request Format Tests ====================

    @Nested
    @DisplayName("ANNUAL-006: Request Format")
    class RequestFormat {

        @Test
        @DisplayName("ANNUAL-006-01: Declaration request contains calculation ID")
        void declarationRequestContainsCalculationId() throws Exception {
            // Given
            stubAnnualReturnSuccess(NINO_HAPPY_PATH, TEST_TAX_YEAR);

            String requestBody = buildFinalDeclarationRequest(TEST_CALCULATION_ID);

            // When
            sendPostRequest(
                "/individuals/declarations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then - Verify request contains calculationId (flexible whitespace)
            verify(postRequestedFor(urlPathMatching("/individuals/declarations/self-assessment/.*"))
                .withHeader("Content-Type", equalTo(JSON_CONTENT_TYPE))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(containing("calculationId"))
                .withRequestBody(containing(TEST_CALCULATION_ID)));
        }

        @Test
        @DisplayName("ANNUAL-006-02: Tax year in URL matches format YYYY-YY")
        void taxYearInUrlMatchesFormat() throws Exception {
            // Given
            stubAnnualReturnSuccess(NINO_HAPPY_PATH, TEST_TAX_YEAR);

            String requestBody = buildFinalDeclarationRequest(TEST_CALCULATION_ID);

            // When
            sendPostRequest(
                "/individuals/declarations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then
            verify(postRequestedFor(urlPathMatching("/individuals/declarations/self-assessment/.*/2025-26")));
        }
    }

    // ==================== HMRC Sandbox Test Scenarios ====================

    @Nested
    @DisplayName("ANNUAL-007: HMRC Sandbox Test Scenarios")
    class HmrcSandboxScenarios {

        @Test
        @DisplayName("ANNUAL-007-01: Happy path NINO (AA000001A) returns 201")
        void happyPathNinoReturns201() throws Exception {
            // Given
            stubAnnualReturnSuccess(NINO_HAPPY_PATH, TEST_TAX_YEAR);

            String requestBody = buildFinalDeclarationRequest(TEST_CALCULATION_ID);

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/declarations/self-assessment/" + NINO_HAPPY_PATH + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(201);
        }

        @Test
        @DisplayName("ANNUAL-007-02: Validation error NINO (AA000422A) returns 422")
        void validationErrorNinoReturns422() throws Exception {
            // Given
            stubAnnualReturnValidationError(NINO_VALIDATION_ERROR, TEST_TAX_YEAR);

            String requestBody = buildFinalDeclarationRequest(TEST_CALCULATION_ID);

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/declarations/self-assessment/" + NINO_VALIDATION_ERROR + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(422);
        }

        @Test
        @DisplayName("ANNUAL-007-03: Duplicate NINO (AA000409A) returns 409")
        void duplicateNinoReturns409() throws Exception {
            // Given
            stubAnnualReturnAlreadySubmitted(NINO_DUPLICATE, TEST_TAX_YEAR);

            String requestBody = buildFinalDeclarationRequest(TEST_CALCULATION_ID);

            // When
            HttpResponse<String> response = sendPostRequest(
                "/individuals/declarations/self-assessment/" + NINO_DUPLICATE + "/" + TEST_TAX_YEAR,
                requestBody);

            // Then
            assertThat(response.statusCode()).isEqualTo(409);
        }
    }

    // ==================== Helper Methods ====================

    private HttpResponse<String> sendGetRequest(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(wireMockServer.baseUrl() + path))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + ACCESS_TOKEN)
            .GET()
            .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPostRequest(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(wireMockServer.baseUrl() + path))
            .header("Content-Type", JSON_CONTENT_TYPE)
            .header("Authorization", "Bearer " + ACCESS_TOKEN)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String buildFinalDeclarationRequest(String calculationId) {
        return """
            {
                "calculationId": "%s"
            }
            """.formatted(calculationId);
    }
}
