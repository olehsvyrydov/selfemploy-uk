package uk.selfemploy.hmrc.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Reusable WireMock stub configurations for HMRC API integration tests.
 *
 * <p>Provides stubs for all HMRC sandbox API endpoints following official test scenarios:
 * https://developer.service.hmrc.gov.uk/api-documentation/docs/testing
 *
 * <h3>HMRC Sandbox Test NINOs:</h3>
 * <ul>
 *     <li>AA000001A - Happy path (success)</li>
 *     <li>AA000404A - Not found (404)</li>
 *     <li>AA000500A - Server error (500)</li>
 *     <li>AA000422A - Validation error (422)</li>
 * </ul>
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/testing">HMRC API Testing Guide</a>
 */
public final class HmrcWireMockStubs {

    // HMRC Sandbox Test NINOs
    public static final String NINO_HAPPY_PATH = "AA000001A";
    public static final String NINO_NOT_FOUND = "AA000404A";
    public static final String NINO_SERVER_ERROR = "AA000500A";
    public static final String NINO_VALIDATION_ERROR = "AA000422A";
    public static final String NINO_UNAUTHORIZED = "AA000401A";
    public static final String NINO_FORBIDDEN = "AA000403A";
    public static final String NINO_RATE_LIMITED = "AA000429A";
    public static final String NINO_DUPLICATE = "AA000409A";

    // Test Business ID
    public static final String TEST_BUSINESS_ID = "XAIS12345678901";

    // Test Tax Year
    public static final String TEST_TAX_YEAR = "2025-26";

    // Test Calculation ID
    public static final String TEST_CALCULATION_ID = "f2fb30e5-4ab6-4a29-b3c1-c7264259ff1e";

    // Media Types
    public static final String HMRC_CONTENT_TYPE = "application/vnd.hmrc.1.0+json";
    public static final String JSON_CONTENT_TYPE = "application/json";

    private HmrcWireMockStubs() {
        // Utility class
    }

    // ==================== OAuth Stubs ====================

    /**
     * Stubs successful OAuth token exchange.
     */
    public static void stubOAuthTokenExchangeSuccess() {
        stubFor(post(urlPathEqualTo("/oauth/token"))
            .withRequestBody(containing("grant_type=authorization_code"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody(loadFixture("oauth-token-response.json"))));
    }

    /**
     * Stubs successful OAuth token refresh.
     */
    public static void stubOAuthTokenRefreshSuccess() {
        stubFor(post(urlPathEqualTo("/oauth/token"))
            .withRequestBody(containing("grant_type=refresh_token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody(loadFixture("oauth-token-refresh-response.json"))));
    }

    /**
     * Stubs OAuth token exchange with invalid grant error.
     */
    public static void stubOAuthInvalidGrant() {
        stubFor(post(urlPathEqualTo("/oauth/token"))
            .withRequestBody(containing("code=invalid"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody("""
                    {
                        "error": "invalid_grant",
                        "error_description": "The authorization code has expired or is invalid"
                    }
                    """)));
    }

    /**
     * Stubs OAuth token refresh with expired refresh token.
     */
    public static void stubOAuthExpiredRefreshToken() {
        stubFor(post(urlPathEqualTo("/oauth/token"))
            .withRequestBody(containing("refresh_token=expired"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody("""
                    {
                        "error": "invalid_grant",
                        "error_description": "The refresh token has expired"
                    }
                    """)));
    }

    // ==================== Business Details Stubs ====================

    /**
     * Stubs successful business list retrieval.
     */
    public static void stubListBusinessesSuccess(String nino) {
        stubFor(get(urlPathEqualTo("/individuals/business/self-employment/" + nino))
            .withHeader("Authorization", matching("Bearer .+"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", HMRC_CONTENT_TYPE)
                .withBody(loadFixture("business-details-list-response.json"))));
    }

    /**
     * Stubs successful single business retrieval.
     */
    public static void stubGetBusinessSuccess(String nino, String businessId) {
        stubFor(get(urlPathEqualTo("/individuals/business/self-employment/" + nino + "/" + businessId))
            .withHeader("Authorization", matching("Bearer .+"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", HMRC_CONTENT_TYPE)
                .withBody(loadFixture("business-details-response.json"))));
    }

    /**
     * Stubs business not found (404) response.
     */
    public static void stubBusinessNotFound(String nino) {
        stubFor(get(urlPathMatching("/individuals/business/self-employment/" + nino + ".*"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody("""
                    {
                        "code": "MATCHING_RESOURCE_NOT_FOUND",
                        "message": "No self-employment business found for this NINO"
                    }
                    """)));
    }

    // ==================== Quarterly Update Stubs ====================

    /**
     * Stubs successful quarterly update submission.
     */
    public static void stubQuarterlyUpdateSuccess(String nino, String businessId) {
        stubFor(post(urlPathEqualTo("/individuals/business/self-employment/" + nino + "/" + businessId + "/period"))
            .withHeader("Authorization", matching("Bearer .+"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody(loadFixture("quarterly-update-success.json"))));
    }

    /**
     * Stubs quarterly update with duplicate period error.
     */
    public static void stubQuarterlyUpdateDuplicate(String nino, String businessId) {
        stubFor(post(urlPathEqualTo("/individuals/business/self-employment/" + nino + "/" + businessId + "/period"))
            .willReturn(aResponse()
                .withStatus(409)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody("""
                    {
                        "code": "RULE_OVERLAPPING_PERIOD",
                        "message": "A periodic update already exists for this period"
                    }
                    """)));
    }

    /**
     * Stubs quarterly update with validation error.
     */
    public static void stubQuarterlyUpdateValidationError(String nino, String businessId) {
        stubFor(post(urlPathEqualTo("/individuals/business/self-employment/" + nino + "/" + businessId + "/period"))
            .willReturn(aResponse()
                .withStatus(422)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody(loadFixture("quarterly-update-error.json"))));
    }

    /**
     * Stubs successful quarterly update amendment.
     */
    public static void stubQuarterlyUpdateAmendSuccess(String nino, String businessId, String periodId) {
        stubFor(put(urlPathEqualTo("/individuals/business/self-employment/" + nino + "/" + businessId + "/period/" + periodId))
            .withHeader("Authorization", matching("Bearer .+"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody(loadFixture("quarterly-update-success.json"))));
    }

    // ==================== Annual Return Stubs ====================

    /**
     * Stubs successful annual return (final declaration) submission.
     */
    public static void stubAnnualReturnSuccess(String nino, String taxYear) {
        stubFor(post(urlPathEqualTo("/individuals/declarations/self-assessment/" + nino + "/" + taxYear))
            .withHeader("Authorization", matching("Bearer .+"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody(loadFixture("annual-return-success.json"))));
    }

    /**
     * Stubs annual return with validation error (e.g., incomplete quarters).
     */
    public static void stubAnnualReturnValidationError(String nino, String taxYear) {
        stubFor(post(urlPathEqualTo("/individuals/declarations/self-assessment/" + nino + "/" + taxYear))
            .willReturn(aResponse()
                .withStatus(422)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody("""
                    {
                        "code": "RULE_INCOMPLETE_SUBMISSION",
                        "message": "All quarterly updates must be submitted before making a final declaration"
                    }
                    """)));
    }

    /**
     * Stubs annual return with already submitted error.
     */
    public static void stubAnnualReturnAlreadySubmitted(String nino, String taxYear) {
        stubFor(post(urlPathEqualTo("/individuals/declarations/self-assessment/" + nino + "/" + taxYear))
            .willReturn(aResponse()
                .withStatus(409)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody("""
                    {
                        "code": "RULE_ALREADY_SUBMITTED",
                        "message": "A final declaration has already been submitted for this tax year"
                    }
                    """)));
    }

    // ==================== Error Response Stubs ====================

    /**
     * Stubs 401 Unauthorized response for missing/invalid token.
     */
    public static void stubUnauthorized(String pathPattern) {
        stubFor(any(urlPathMatching(pathPattern))
            .withHeader("Authorization", absent())
            .willReturn(unauthorizedResponse("Bearer token is missing or invalid")));

        stubFor(any(urlPathMatching(pathPattern))
            .withHeader("Authorization", matching("Bearer expired.*"))
            .willReturn(unauthorizedResponse("Bearer token has expired")));
    }

    /**
     * Stubs 403 Forbidden response.
     */
    public static void stubForbidden(String pathPattern) {
        stubFor(any(urlPathMatching(pathPattern))
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody("""
                    {
                        "code": "CLIENT_OR_AGENT_NOT_AUTHORISED",
                        "message": "The client and/or agent is not authorised"
                    }
                    """)));
    }

    /**
     * Stubs 429 Rate Limited response.
     */
    public static void stubRateLimited(String pathPattern) {
        stubFor(any(urlPathMatching(pathPattern))
            .willReturn(aResponse()
                .withStatus(429)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withHeader("Retry-After", "60")
                .withBody("""
                    {
                        "code": "MESSAGE_THROTTLED_OUT",
                        "message": "Rate limit exceeded. Please retry after the period specified in the Retry-After header"
                    }
                    """)));
    }

    /**
     * Stubs 500 Server Error response.
     */
    public static void stubServerError(String pathPattern) {
        stubFor(any(urlPathMatching(pathPattern))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody("""
                    {
                        "code": "SERVER_ERROR",
                        "message": "An unexpected error occurred on the server"
                    }
                    """)));
    }

    /**
     * Stubs 503 Service Unavailable response.
     */
    public static void stubServiceUnavailable(String pathPattern) {
        stubFor(any(urlPathMatching(pathPattern))
            .willReturn(aResponse()
                .withStatus(503)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withHeader("Retry-After", "300")
                .withBody("""
                    {
                        "code": "SERVICE_UNAVAILABLE",
                        "message": "The service is temporarily unavailable. Please try again later"
                    }
                    """)));
    }

    // ==================== Fraud Prevention Header Validation ====================

    /**
     * Stubs endpoint that validates fraud prevention headers are present.
     * Returns 400 if mandatory headers are missing.
     */
    public static void stubWithFraudPreventionHeaderValidation(String pathPattern) {
        // Success with all headers
        stubFor(any(urlPathMatching(pathPattern))
            .withHeader("Gov-Client-Connection-Method", matching(".+"))
            .withHeader("Gov-Client-Device-ID", matching(".+"))
            .withHeader("Gov-Client-User-IDs", matching(".+"))
            .withHeader("Gov-Client-Timezone", matching(".+"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody("{\"success\": true}")));

        // Error without headers
        stubFor(any(urlPathMatching(pathPattern))
            .withHeader("Gov-Client-Connection-Method", absent())
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody("""
                    {
                        "code": "INVALID_HEADER",
                        "message": "Gov-Client-Connection-Method header is missing or invalid"
                    }
                    """)));
    }

    // ==================== Calculation Stubs ====================

    /**
     * Stubs successful tax calculation trigger.
     */
    public static void stubTriggerCalculationSuccess(String nino, String taxYear) {
        stubFor(post(urlPathEqualTo("/individuals/calculations/self-assessment/" + nino + "/" + taxYear))
            .withHeader("Authorization", matching("Bearer .+"))
            .willReturn(aResponse()
                .withStatus(202)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody("""
                    {
                        "calculationId": "%s"
                    }
                    """.formatted(TEST_CALCULATION_ID))));
    }

    /**
     * Stubs get calculation result.
     */
    public static void stubGetCalculationResult(String nino, String calculationId) {
        stubFor(get(urlPathEqualTo("/individuals/calculations/self-assessment/" + nino + "/" + calculationId))
            .withHeader("Authorization", matching("Bearer .+"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody(loadFixture("calculation-result.json"))));
    }

    // ==================== Helper Methods ====================

    private static ResponseDefinitionBuilder unauthorizedResponse(String message) {
        return aResponse()
            .withStatus(401)
            .withHeader("Content-Type", JSON_CONTENT_TYPE)
            .withBody("""
                {
                    "code": "UNAUTHORIZED",
                    "message": "%s"
                }
                """.formatted(message));
    }

    /**
     * Loads a JSON fixture file from test resources.
     *
     * @param filename Fixture filename (relative to hmrc-sandbox directory)
     * @return Fixture content as string
     */
    public static String loadFixture(String filename) {
        String path = "hmrc-sandbox/" + filename;
        try (InputStream is = HmrcWireMockStubs.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Fixture not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load fixture: " + path, e);
        }
    }

    /**
     * Creates inline JSON fixture content (for tests that don't use files).
     */
    public static String inlineFixture(String jsonContent) {
        return jsonContent;
    }
}
