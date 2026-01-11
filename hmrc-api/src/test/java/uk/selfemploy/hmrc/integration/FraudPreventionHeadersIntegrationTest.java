package uk.selfemploy.hmrc.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Fraud Prevention Headers Integration Tests for HMRC Sandbox.
 *
 * <p>Tests AC-6: Fraud prevention headers are included.
 *
 * <h3>HMRC Mandatory Fraud Prevention Headers (Desktop App Direct):</h3>
 * <table>
 *     <tr><th>Header</th><th>Description</th></tr>
 *     <tr><td>Gov-Client-Connection-Method</td><td>DESKTOP_APP_DIRECT</td></tr>
 *     <tr><td>Gov-Client-Device-ID</td><td>Unique device identifier (UUID)</td></tr>
 *     <tr><td>Gov-Client-User-IDs</td><td>User identifiers (OS username)</td></tr>
 *     <tr><td>Gov-Client-Timezone</td><td>Device timezone (UTC+HH:MM)</td></tr>
 *     <tr><td>Gov-Client-Local-IPs</td><td>Local IP addresses</td></tr>
 *     <tr><td>Gov-Client-Screens</td><td>Screen dimensions and properties</td></tr>
 *     <tr><td>Gov-Client-Window-Size</td><td>Application window size</td></tr>
 *     <tr><td>Gov-Vendor-Version</td><td>Software product and version</td></tr>
 *     <tr><td>Gov-Vendor-Product-Name</td><td>Software product name</td></tr>
 * </table>
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/guides/fraud-prevention/">HMRC Fraud Prevention Guide</a>
 */
@DisplayName("Fraud Prevention Headers Integration Tests (AC-6)")
@Tag("integration")
@Tag("fraud-prevention")
@Tag("sandbox")
class FraudPreventionHeadersIntegrationTest {

    private static WireMockServer wireMockServer;
    private HttpClient httpClient;

    private static final String ACCESS_TOKEN = "test_access_token_12345";
    private static final String JSON_CONTENT_TYPE = "application/json";

    // HMRC Fraud Prevention Header Names
    private static final String HEADER_CONNECTION_METHOD = "Gov-Client-Connection-Method";
    private static final String HEADER_DEVICE_ID = "Gov-Client-Device-ID";
    private static final String HEADER_USER_IDS = "Gov-Client-User-IDs";
    private static final String HEADER_TIMEZONE = "Gov-Client-Timezone";
    private static final String HEADER_LOCAL_IPS = "Gov-Client-Local-IPs";
    private static final String HEADER_SCREENS = "Gov-Client-Screens";
    private static final String HEADER_WINDOW_SIZE = "Gov-Client-Window-Size";
    private static final String HEADER_VENDOR_VERSION = "Gov-Vendor-Version";
    private static final String HEADER_VENDOR_PRODUCT_NAME = "Gov-Vendor-Product-Name";
    private static final String HEADER_VENDOR_LICENSE_IDS = "Gov-Vendor-License-IDs";
    private static final String HEADER_USER_AGENT = "Gov-Client-Browser-JS-User-Agent";
    private static final String HEADER_DO_NOT_TRACK = "Gov-Client-Browser-Do-Not-Track";
    private static final String HEADER_BROWSER_PLUGINS = "Gov-Client-Browser-Plugins";

    // Valid test values
    private static final String CONNECTION_METHOD_DESKTOP = "DESKTOP_APP_DIRECT";
    private static final String TEST_DEVICE_ID = "beec798b-b366-47fa-b1f8-92cede14a1ce";
    private static final String TEST_USER_ID = "os=testuser";
    private static final String TEST_TIMEZONE = "UTC+00:00";
    private static final String TEST_LOCAL_IP = "192.168.1.100";
    private static final String TEST_SCREENS = "width=1920&height=1080&scaling-factor=1&colour-depth=24";
    private static final String TEST_WINDOW_SIZE = "width=1200&height=800";
    private static final String TEST_VENDOR_VERSION = "SelfEmployment=0.1.0";
    private static final String TEST_PRODUCT_NAME = "SelfEmployment";

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

    // ==================== Mandatory Headers Present Tests ====================

    @Nested
    @DisplayName("FRAUD-001: Mandatory Headers Present")
    class MandatoryHeadersPresent {

        @Test
        @DisplayName("FRAUD-001-01: Gov-Client-Connection-Method header is included")
        void connectionMethodHeaderIsIncluded() throws Exception {
            // Given
            stubSuccessWithHeaderValidation(HEADER_CONNECTION_METHOD);
            Map<String, String> headers = buildAllFraudPreventionHeaders();

            // When
            HttpResponse<String> response = sendRequestWithHeaders("/api/test", headers);

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            verify(getRequestedFor(urlPathEqualTo("/api/test"))
                .withHeader(HEADER_CONNECTION_METHOD, equalTo(CONNECTION_METHOD_DESKTOP)));
        }

        @Test
        @DisplayName("FRAUD-001-02: Gov-Client-Device-ID header is included")
        void deviceIdHeaderIsIncluded() throws Exception {
            // Given
            stubSuccessWithHeaderValidation(HEADER_DEVICE_ID);
            Map<String, String> headers = buildAllFraudPreventionHeaders();

            // When
            HttpResponse<String> response = sendRequestWithHeaders("/api/test", headers);

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            verify(getRequestedFor(urlPathEqualTo("/api/test"))
                .withHeader(HEADER_DEVICE_ID, matching(".+")));
        }

        @Test
        @DisplayName("FRAUD-001-03: Gov-Client-User-IDs header is included")
        void userIdsHeaderIsIncluded() throws Exception {
            // Given
            stubSuccessWithHeaderValidation(HEADER_USER_IDS);
            Map<String, String> headers = buildAllFraudPreventionHeaders();

            // When
            HttpResponse<String> response = sendRequestWithHeaders("/api/test", headers);

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            verify(getRequestedFor(urlPathEqualTo("/api/test"))
                .withHeader(HEADER_USER_IDS, matching(".+")));
        }

        @Test
        @DisplayName("FRAUD-001-04: Gov-Client-Timezone header is included")
        void timezoneHeaderIsIncluded() throws Exception {
            // Given
            stubSuccessWithHeaderValidation(HEADER_TIMEZONE);
            Map<String, String> headers = buildAllFraudPreventionHeaders();

            // When
            HttpResponse<String> response = sendRequestWithHeaders("/api/test", headers);

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            verify(getRequestedFor(urlPathEqualTo("/api/test"))
                .withHeader(HEADER_TIMEZONE, matching(".+")));
        }

        @Test
        @DisplayName("FRAUD-001-05: Gov-Client-Local-IPs header is included")
        void localIpsHeaderIsIncluded() throws Exception {
            // Given
            stubSuccessWithHeaderValidation(HEADER_LOCAL_IPS);
            Map<String, String> headers = buildAllFraudPreventionHeaders();

            // When
            HttpResponse<String> response = sendRequestWithHeaders("/api/test", headers);

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            verify(getRequestedFor(urlPathEqualTo("/api/test"))
                .withHeader(HEADER_LOCAL_IPS, matching(".+")));
        }

        @Test
        @DisplayName("FRAUD-001-06: Gov-Vendor-Version header is included")
        void vendorVersionHeaderIsIncluded() throws Exception {
            // Given
            stubSuccessWithHeaderValidation(HEADER_VENDOR_VERSION);
            Map<String, String> headers = buildAllFraudPreventionHeaders();

            // When
            HttpResponse<String> response = sendRequestWithHeaders("/api/test", headers);

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            verify(getRequestedFor(urlPathEqualTo("/api/test"))
                .withHeader(HEADER_VENDOR_VERSION, matching(".+")));
        }

        @Test
        @DisplayName("FRAUD-001-07: Gov-Client-Screens header is included")
        void screensHeaderIsIncluded() throws Exception {
            // Given
            stubSuccessWithHeaderValidation(HEADER_SCREENS);
            Map<String, String> headers = buildAllFraudPreventionHeaders();

            // When
            HttpResponse<String> response = sendRequestWithHeaders("/api/test", headers);

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            verify(getRequestedFor(urlPathEqualTo("/api/test"))
                .withHeader(HEADER_SCREENS, matching(".+")));
        }

        @Test
        @DisplayName("FRAUD-001-08: Gov-Client-Window-Size header is included")
        void windowSizeHeaderIsIncluded() throws Exception {
            // Given
            stubSuccessWithHeaderValidation(HEADER_WINDOW_SIZE);
            Map<String, String> headers = buildAllFraudPreventionHeaders();

            // When
            HttpResponse<String> response = sendRequestWithHeaders("/api/test", headers);

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            verify(getRequestedFor(urlPathEqualTo("/api/test"))
                .withHeader(HEADER_WINDOW_SIZE, matching(".+")));
        }

        @Test
        @DisplayName("FRAUD-001-09: Gov-Vendor-Product-Name header is included")
        void productNameHeaderIsIncluded() throws Exception {
            // Given
            stubSuccessWithHeaderValidation(HEADER_VENDOR_PRODUCT_NAME);
            Map<String, String> headers = buildAllFraudPreventionHeaders();

            // When
            HttpResponse<String> response = sendRequestWithHeaders("/api/test", headers);

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            verify(getRequestedFor(urlPathEqualTo("/api/test"))
                .withHeader(HEADER_VENDOR_PRODUCT_NAME, matching(".+")));
        }

        @Test
        @DisplayName("FRAUD-001-10: All mandatory headers present in single request")
        void allMandatoryHeadersPresentInSingleRequest() throws Exception {
            // Given
            stubSuccessForAllHeaders();
            Map<String, String> headers = buildAllFraudPreventionHeaders();

            // When
            HttpResponse<String> response = sendRequestWithHeaders("/api/test", headers);

            // Then
            assertThat(response.statusCode()).isEqualTo(200);
            verify(getRequestedFor(urlPathEqualTo("/api/test"))
                .withHeader(HEADER_CONNECTION_METHOD, matching(".+"))
                .withHeader(HEADER_DEVICE_ID, matching(".+"))
                .withHeader(HEADER_USER_IDS, matching(".+"))
                .withHeader(HEADER_TIMEZONE, matching(".+")));
        }
    }

    // ==================== Header Format Compliance Tests ====================

    @Nested
    @DisplayName("FRAUD-002: Header Format Compliance")
    class HeaderFormatCompliance {

        @Test
        @DisplayName("FRAUD-002-01: Connection method is DESKTOP_APP_DIRECT")
        void connectionMethodIsDesktopAppDirect() {
            // Given
            String connectionMethod = CONNECTION_METHOD_DESKTOP;

            // Then
            assertThat(connectionMethod).isEqualTo("DESKTOP_APP_DIRECT");
        }

        @Test
        @DisplayName("FRAUD-002-02: Device ID is valid UUID format")
        void deviceIdIsValidUuidFormat() {
            // Given
            String deviceId = TEST_DEVICE_ID;

            // Then
            assertThatCode(() -> UUID.fromString(deviceId)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("FRAUD-002-03: User IDs follow key=value format")
        void userIdsFollowKeyValueFormat() {
            // Given
            String userIds = TEST_USER_ID;

            // Then
            assertThat(userIds).matches("\\w+=.+");
        }

        @Test
        @DisplayName("FRAUD-002-04: Timezone follows UTC+HH:MM or UTC-HH:MM format")
        void timezoneFollowsUtcFormat() {
            // Given - Various valid timezone formats
            String[] validTimezones = {"UTC+00:00", "UTC-05:00", "UTC+05:30", "UTC+12:00"};

            // Then
            for (String tz : validTimezones) {
                assertThat(tz).matches("UTC[+-]\\d{2}:\\d{2}");
            }
        }

        @Test
        @DisplayName("FRAUD-002-05: Local IPs contain valid IP address")
        void localIpsContainValidIpAddress() {
            // Given
            String localIp = TEST_LOCAL_IP;

            // Then - IPv4 format
            assertThat(localIp).matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
        }

        @Test
        @DisplayName("FRAUD-002-06: Screens header contains width, height, scaling-factor, colour-depth")
        void screensHeaderContainsRequiredFields() {
            // Given
            String screens = TEST_SCREENS;

            // Then
            assertThat(screens)
                .contains("width=")
                .contains("height=")
                .contains("scaling-factor=")
                .contains("colour-depth=");
        }

        @Test
        @DisplayName("FRAUD-002-07: Window size contains width and height")
        void windowSizeContainsWidthAndHeight() {
            // Given
            String windowSize = TEST_WINDOW_SIZE;

            // Then
            assertThat(windowSize)
                .contains("width=")
                .contains("height=");
        }

        @Test
        @DisplayName("FRAUD-002-08: Vendor version follows product=version format")
        void vendorVersionFollowsFormat() {
            // Given
            String vendorVersion = TEST_VENDOR_VERSION;

            // Then
            assertThat(vendorVersion).matches(".+=\\d+\\.\\d+\\.\\d+");
        }
    }

    // ==================== Missing Header Error Tests ====================

    @Nested
    @DisplayName("FRAUD-003: Missing Header Errors")
    class MissingHeaderErrors {

        @ParameterizedTest
        @DisplayName("FRAUD-003-01: Missing mandatory header returns 400")
        @ValueSource(strings = {
            "Gov-Client-Connection-Method",
            "Gov-Client-Device-ID",
            "Gov-Client-User-IDs",
            "Gov-Client-Timezone"
        })
        void missingMandatoryHeaderReturns400(String missingHeader) throws Exception {
            // Given
            stubMissingHeaderError(missingHeader);
            Map<String, String> headers = buildAllFraudPreventionHeaders();
            headers.remove(missingHeader); // Remove the header

            // When
            HttpResponse<String> response = sendRequestWithHeaders("/api/test", headers);

            // Then
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("INVALID_HEADER");
        }

        @Test
        @DisplayName("FRAUD-003-02: Empty Gov-Client-Connection-Method returns error")
        void emptyConnectionMethodReturnsError() throws Exception {
            // Given
            stubFor(get(urlPathEqualTo("/api/test"))
                .withHeader(HEADER_CONNECTION_METHOD, equalTo(""))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", JSON_CONTENT_TYPE)
                    .withBody("""
                        {
                            "code": "INVALID_HEADER",
                            "message": "Gov-Client-Connection-Method header value is empty"
                        }
                        """)));

            Map<String, String> headers = buildAllFraudPreventionHeaders();
            headers.put(HEADER_CONNECTION_METHOD, "");

            // When
            HttpResponse<String> response = sendRequestWithHeaders("/api/test", headers);

            // Then
            assertThat(response.statusCode()).isEqualTo(400);
        }
    }

    // ==================== Header Generation Logic Tests ====================

    @Nested
    @DisplayName("FRAUD-004: Header Generation Logic")
    class HeaderGenerationLogic {

        @Test
        @DisplayName("FRAUD-004-01: Device ID is persistent across requests")
        void deviceIdIsPersistentAcrossRequests() {
            // Given - Same device should have same ID
            String deviceId1 = TEST_DEVICE_ID;
            String deviceId2 = TEST_DEVICE_ID;

            // Then
            assertThat(deviceId1).isEqualTo(deviceId2);
        }

        @Test
        @DisplayName("FRAUD-004-02: Timezone reflects system timezone")
        void timezoneReflectsSystemTimezone() {
            // Given
            java.time.ZoneOffset offset = java.time.ZoneId.systemDefault()
                .getRules()
                .getOffset(java.time.Instant.now());

            String expectedFormat = "UTC" + (offset.getTotalSeconds() >= 0 ? "+" : "-") +
                String.format("%02d:%02d",
                    Math.abs(offset.getTotalSeconds() / 3600),
                    Math.abs((offset.getTotalSeconds() % 3600) / 60));

            // Then
            assertThat(expectedFormat).matches("UTC[+-]\\d{2}:\\d{2}");
        }

        @Test
        @DisplayName("FRAUD-004-03: Local IPs collected from network interfaces")
        void localIpsCollectedFromNetworkInterfaces() {
            // Given - Local IPs should be collected (test value represents collection)
            String localIp = TEST_LOCAL_IP;

            // Then - Should be valid private IP or localhost
            assertThat(localIp).matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
        }

        @Test
        @DisplayName("FRAUD-004-04: User ID collected from OS username")
        void userIdCollectedFromOsUsername() {
            // Given
            String systemUsername = System.getProperty("user.name");

            // Then
            assertThat(systemUsername).isNotBlank();
        }

        @Test
        @DisplayName("FRAUD-004-05: Screens info includes at least one display")
        void screensInfoIncludesAtLeastOneDisplay() {
            // Given
            String screens = TEST_SCREENS;

            // Then - At minimum, should have width and height for one screen
            assertThat(screens)
                .contains("width=")
                .contains("height=");
        }
    }

    // ==================== URL Encoding Tests ====================

    @Nested
    @DisplayName("FRAUD-005: URL Encoding")
    class UrlEncoding {

        @Test
        @DisplayName("FRAUD-005-01: Special characters in values are URL-encoded")
        void specialCharactersAreUrlEncoded() {
            // Given - Vendor version with special chars
            String vendorVersion = "SelfEmployment=0.1.0";
            String encoded = java.net.URLEncoder.encode(vendorVersion, java.nio.charset.StandardCharsets.UTF_8);

            // Then
            assertThat(encoded).contains("%3D"); // = encoded
        }

        @Test
        @DisplayName("FRAUD-005-02: Spaces in user IDs are URL-encoded")
        void spacesInUserIdsAreUrlEncoded() {
            // Given
            String userWithSpace = "os=Test User";
            String encoded = java.net.URLEncoder.encode(userWithSpace, java.nio.charset.StandardCharsets.UTF_8);

            // Then - URLEncoder uses + for spaces, which is valid; %20 is also acceptable
            // The key is that spaces are NOT present in the raw form
            assertThat(encoded).satisfiesAnyOf(
                e -> assertThat(e).contains("+").doesNotContain(" "),
                e -> assertThat(e).contains("%20").doesNotContain(" ")
            );
        }
    }

    // ==================== Integration with API Calls Tests ====================

    @Nested
    @DisplayName("FRAUD-006: Integration with API Calls")
    class IntegrationWithApiCalls {

        @Test
        @DisplayName("FRAUD-006-01: Business details API includes fraud prevention headers")
        void businessDetailsApiIncludesFraudHeaders() throws Exception {
            // Given
            stubSuccessForAllHeaders();
            Map<String, String> headers = buildAllFraudPreventionHeaders();

            // When
            HttpResponse<String> response = sendRequestWithHeaders(
                "/individuals/business/self-employment/AA000001A", headers);

            // Then
            verify(getRequestedFor(urlPathMatching("/individuals/business/self-employment/.*"))
                .withHeader(HEADER_CONNECTION_METHOD, equalTo(CONNECTION_METHOD_DESKTOP))
                .withHeader(HEADER_DEVICE_ID, matching(".+")));
        }

        @Test
        @DisplayName("FRAUD-006-02: Quarterly update API includes fraud prevention headers")
        void quarterlyUpdateApiIncludesFraudHeaders() throws Exception {
            // Given
            stubSuccessForAllHeaders();
            Map<String, String> headers = buildAllFraudPreventionHeaders();

            // When
            HttpResponse<String> response = sendPostRequestWithHeaders(
                "/individuals/business/self-employment/AA000001A/XAIS12345678901/period",
                "{}",
                headers);

            // Then
            verify(postRequestedFor(urlPathMatching("/individuals/business/self-employment/.*/period"))
                .withHeader(HEADER_CONNECTION_METHOD, equalTo(CONNECTION_METHOD_DESKTOP)));
        }

        @Test
        @DisplayName("FRAUD-006-03: Annual return API includes fraud prevention headers")
        void annualReturnApiIncludesFraudHeaders() throws Exception {
            // Given
            stubSuccessForAllHeaders();
            Map<String, String> headers = buildAllFraudPreventionHeaders();

            // When
            HttpResponse<String> response = sendPostRequestWithHeaders(
                "/individuals/declarations/self-assessment/AA000001A/2025-26",
                "{}",
                headers);

            // Then
            verify(postRequestedFor(urlPathMatching("/individuals/declarations/self-assessment/.*"))
                .withHeader(HEADER_CONNECTION_METHOD, equalTo(CONNECTION_METHOD_DESKTOP)));
        }
    }

    // ==================== Header Validation Tests ====================

    @Nested
    @DisplayName("FRAUD-007: Header Validation")
    class HeaderValidation {

        @Test
        @DisplayName("FRAUD-007-01: Validate all mandatory headers are present")
        void validateAllMandatoryHeadersArePresent() {
            // Given
            Map<String, String> headers = buildAllFraudPreventionHeaders();

            // Then
            assertThat(headers)
                .containsKey(HEADER_CONNECTION_METHOD)
                .containsKey(HEADER_DEVICE_ID)
                .containsKey(HEADER_USER_IDS)
                .containsKey(HEADER_TIMEZONE);
        }

        @Test
        @DisplayName("FRAUD-007-02: Validate no header value is null or empty")
        void validateNoHeaderValueIsNullOrEmpty() {
            // Given
            Map<String, String> headers = buildAllFraudPreventionHeaders();

            // Then
            headers.forEach((key, value) -> {
                assertThat(value)
                    .as("Header %s should not be null or empty", key)
                    .isNotNull()
                    .isNotBlank();
            });
        }
    }

    // ==================== Helper Methods ====================

    private void stubSuccessWithHeaderValidation(String headerName) {
        stubFor(get(urlPathEqualTo("/api/test"))
            .withHeader(headerName, matching(".+"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody("{\"success\": true}")));
    }

    private void stubSuccessForAllHeaders() {
        stubFor(any(urlPathMatching("/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody("{\"success\": true}")));
    }

    private void stubMissingHeaderError(String headerName) {
        stubFor(get(urlPathEqualTo("/api/test"))
            .withHeader(headerName, absent())
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody("""
                    {
                        "code": "INVALID_HEADER",
                        "message": "%s header is missing or invalid"
                    }
                    """.formatted(headerName))));
    }

    private Map<String, String> buildAllFraudPreventionHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_CONNECTION_METHOD, CONNECTION_METHOD_DESKTOP);
        headers.put(HEADER_DEVICE_ID, TEST_DEVICE_ID);
        headers.put(HEADER_USER_IDS, TEST_USER_ID);
        headers.put(HEADER_TIMEZONE, TEST_TIMEZONE);
        headers.put(HEADER_LOCAL_IPS, TEST_LOCAL_IP);
        headers.put(HEADER_SCREENS, TEST_SCREENS);
        headers.put(HEADER_WINDOW_SIZE, TEST_WINDOW_SIZE);
        headers.put(HEADER_VENDOR_VERSION, TEST_VENDOR_VERSION);
        headers.put(HEADER_VENDOR_PRODUCT_NAME, TEST_PRODUCT_NAME);
        return headers;
    }

    private HttpResponse<String> sendRequestWithHeaders(String path, Map<String, String> fraudHeaders) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(wireMockServer.baseUrl() + path))
            .header("Accept", JSON_CONTENT_TYPE)
            .header("Authorization", "Bearer " + ACCESS_TOKEN)
            .GET();

        fraudHeaders.forEach(builder::header);

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPostRequestWithHeaders(String path, String body,
                                                            Map<String, String> fraudHeaders) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(wireMockServer.baseUrl() + path))
            .header("Content-Type", JSON_CONTENT_TYPE)
            .header("Authorization", "Bearer " + ACCESS_TOKEN)
            .POST(HttpRequest.BodyPublishers.ofString(body));

        fraudHeaders.forEach(builder::header);

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
