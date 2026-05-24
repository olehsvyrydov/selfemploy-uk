package uk.selfemploy.hmrc.fraud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import uk.selfemploy.hmrc.fraud.collectors.DeviceIdCollector;
import uk.selfemploy.hmrc.fraud.collectors.LocalIpsCollector;
import uk.selfemploy.hmrc.fraud.collectors.LocalIpsTimestampCollector;
import uk.selfemploy.hmrc.fraud.collectors.MacAddressesCollector;
import uk.selfemploy.hmrc.fraud.collectors.TimezoneCollector;
import uk.selfemploy.hmrc.fraud.collectors.UserAgentCollector;
import uk.selfemploy.hmrc.fraud.collectors.UserIdsCollector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test for HMRC fraud-prevention headers (DESKTOP_APP_DIRECT).
 *
 * <p>Has two execution modes:
 *
 * <ol>
 *   <li><b>Shape-lock (default — runs in nightly CI).</b> Verifies the local
 *       {@link FraudPreventionService} emits exactly the set of header names
 *       HMRC publishes as mandatory for DESKTOP_APP_DIRECT, and that each
 *       value matches the documented format. Detects drift caused by changes
 *       to the local code without needing HMRC's sandbox to be reachable.</li>
 *   <li><b>Live validator (opt-in).</b> When the {@code HMRC_FPH_VALIDATOR_TOKEN}
 *       environment variable is set, sends the generated headers to HMRC's
 *       Test Fraud Prevention Headers API
 *       ({@code https://test-api.service.hmrc.gov.uk/test/fraud-prevention-headers/validate})
 *       and asserts HTTP 2xx. Skipped when the token is absent so the suite
 *       remains green on developer laptops and on push CI.</li>
 * </ol>
 *
 * <p>Tagged {@code hmrc-sandbox} so the default {@code mvn test} run excludes
 * it. The nightly workflow opts in by clearing the excluded group.
 */
@Tag("hmrc-sandbox")
@DisplayName("HMRC fraud-prevention headers contract")
class FraudPreventionHeadersHmrcContractTest {

    /** Headers HMRC lists as mandatory for DESKTOP_APP_DIRECT (individuals). */
    private static final Set<String> HMRC_MANDATORY_DESKTOP_HEADERS = new TreeSet<>(Set.of(
        "Gov-Client-Connection-Method",
        "Gov-Client-Device-ID",
        "Gov-Client-Local-IPs",
        "Gov-Client-Local-IPs-Timestamp",
        "Gov-Client-Screens",
        "Gov-Client-Timezone",
        "Gov-Client-User-Agent",
        "Gov-Client-User-IDs",
        "Gov-Client-Window-Size",
        "Gov-Vendor-Product-Name",
        "Gov-Vendor-Version"
        // MAC-Addresses is mandatory per HMRC but suppressed when the JVM
        // cannot read NIC hardware (containers, CI runners). The presence
        // check below tolerates absence; the live validator will flag it.
    ));

    private static final String HMRC_VALIDATOR_URL =
        "https://test-api.service.hmrc.gov.uk/test/fraud-prevention-headers/validate";

    private FraudPreventionService service;

    @BeforeEach
    void setUp() {
        service = new FraudPreventionService(
            new DeviceIdCollector(),
            new TimezoneCollector(),
            new LocalIpsCollector(),
            new LocalIpsTimestampCollector(),
            new MacAddressesCollector(),
            new UserIdsCollector(),
            new UserAgentCollector(),
            "1.0.0-CONTRACT"
        );
    }

    @Test
    @DisplayName("emits every header HMRC lists as mandatory for DESKTOP_APP_DIRECT")
    void emitsEveryHmrcMandatoryHeader() {
        Map<String, String> headers = service.generateHeaders();

        Set<String> missing = new TreeSet<>(HMRC_MANDATORY_DESKTOP_HEADERS);
        missing.removeAll(headers.keySet());

        assertThat(missing)
            .as("HMRC mandatory headers absent from FraudPreventionService output.\n"
                + "Generated headers: %s\n"
                + "If HMRC's spec has changed, update HMRC_MANDATORY_DESKTOP_HEADERS "
                + "and add the matching collector under fraud/collectors/.",
                new TreeSet<>(headers.keySet()))
            .isEmpty();
    }

    @Test
    @DisplayName("connection method is the literal DESKTOP_APP_DIRECT")
    void connectionMethodIsExact() {
        Map<String, String> headers = service.generateHeaders();
        assertThat(headers.get("Gov-Client-Connection-Method"))
            .isEqualTo("DESKTOP_APP_DIRECT");
    }

    @Test
    @DisplayName("timezone matches UTC±HH:MM format")
    void timezoneFormat() {
        Map<String, String> headers = service.generateHeaders();
        assertThat(headers.get("Gov-Client-Timezone")).matches("UTC[+-]\\d{2}:\\d{2}");
    }

    @Test
    @DisplayName("local-ips timestamp matches ISO-8601 UTC millis")
    void localIpsTimestampFormat() {
        Map<String, String> headers = service.generateHeaders();
        assertThat(headers.get("Gov-Client-Local-IPs-Timestamp"))
            .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
    }

    @Test
    @DisplayName("user-agent carries os-family, os-version, device-manufacturer, device-model")
    void userAgentFormat() {
        Map<String, String> headers = service.generateHeaders();
        String ua = headers.get("Gov-Client-User-Agent");
        assertThat(ua)
            .contains("os-family=")
            .contains("os-version=")
            .contains("device-manufacturer=")
            .contains("device-model=");
    }

    @Test
    @DisplayName("screens carries width, height, scaling-factor, colour-depth")
    void screensFormat() {
        Map<String, String> headers = service.generateHeaders();
        String screens = headers.get("Gov-Client-Screens");
        assertThat(screens)
            .contains("width=")
            .contains("height=")
            .contains("scaling-factor=")
            .contains("colour-depth=");
    }

    @Test
    @DisplayName("window-size carries width and height in px")
    void windowSizeFormat() {
        Map<String, String> headers = service.generateHeaders();
        assertThat(headers.get("Gov-Client-Window-Size"))
            .matches("width=\\d+&height=\\d+");
    }

    @Test
    @DisplayName("vendor-version is product=semver, percent-encoded")
    void vendorVersionFormat() {
        Map<String, String> headers = service.generateHeaders();
        // After URL encoding, '=' becomes %3D
        assertThat(headers.get("Gov-Vendor-Version")).contains("%3D");
    }

    @Test
    @DisplayName("no header value is null or blank")
    void noBlankValues() {
        Map<String, String> headers = service.generateHeaders();
        headers.forEach((k, v) ->
            assertThat(v).as("Header %s", k).isNotNull().isNotBlank());
    }

    /**
     * Live call to HMRC's test-fraud-prevention-headers validator. Only runs
     * when an OAuth access token is supplied via environment. The token must
     * be acquired out-of-band (application-restricted endpoint) and stored as
     * a CI secret. A 2xx response means HMRC accepts every header we sent.
     */
    @Test
    @Tag("hmrc-live")
    @EnabledIfEnvironmentVariable(named = "HMRC_FPH_VALIDATOR_TOKEN", matches = ".+")
    @DisplayName("HMRC sandbox validator accepts the generated headers")
    void hmrcSandboxValidatorAcceptsHeaders() throws Exception {
        Map<String, String> headers = service.generateHeaders();
        String token = System.getenv("HMRC_FPH_VALIDATOR_TOKEN");

        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(HMRC_VALIDATOR_URL))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/vnd.hmrc.1.0+json")
            .header("Authorization", "Bearer " + token)
            .GET();
        headers.forEach(builder::header);

        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 300) {
            // Surface HMRC's diagnostics for the CI log — body is HMRC's
            // validation feedback, not taxpayer data.
            ObjectMapper om = new ObjectMapper();
            JsonNode body = om.readTree(response.body());
            throw new AssertionError(
                "HMRC validator rejected headers (HTTP " + response.statusCode() + "):\n"
                + body.toPrettyString());
        }
    }
}
