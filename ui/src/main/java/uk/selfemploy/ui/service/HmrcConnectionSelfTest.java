package uk.selfemploy.ui.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

/**
 * Runs a real connection self-test against HMRC after credentials are saved, surfacing three
 * independent checks the user can act on: the credentials' shape, whether HMRC is reachable, and
 * whether HMRC accepts the credentials over a live OAuth round-trip.
 *
 * <p>Security posture (task T4.3 gate conditions):</p>
 * <ul>
 *   <li>Endpoints are resolved from this class's own {@code sandbox}/{@code production} constants,
 *       never from the {@code HMRC_*} system properties — those are populated from a {@code .env}
 *       in the working directory and could otherwise redirect the client secret to another host.
 *       {@link #isHmrcHost(URI)} is a second, defensive gate: nothing is sent to a non-HMRC host.</li>
 *   <li>Nothing is sent to HMRC when the credential shape check fails — a bad secret never leaves
 *       the machine.</li>
 *   <li>Check messages are derived from HTTP status codes only. The request body (which carries the
 *       secret) and HMRC's response body are never logged or placed in a message.</li>
 * </ul>
 */
public final class HmrcConnectionSelfTest {

    private static final Logger LOG = Logger.getLogger(HmrcConnectionSelfTest.class.getName());
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private static final String SANDBOX_BASE = "https://test-api.service.hmrc.gov.uk";
    private static final String PRODUCTION_BASE = "https://api.service.hmrc.gov.uk";

    /** HMRC hosts the self-test is permitted to contact. */
    private static final List<String> ALLOWED_HOST_SUFFIXES =
        List.of(".service.hmrc.gov.uk", ".tax.service.gov.uk");

    private final HttpClient httpClient;

    public HmrcConnectionSelfTest() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    HmrcConnectionSelfTest(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /** Whether a URL is one the self-test may contact: HTTPS on a known HMRC host. */
    static boolean isHmrcHost(URI uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            return false;
        }
        String host = uri.getHost().toLowerCase();
        return ALLOWED_HOST_SUFFIXES.stream().anyMatch(host::endsWith);
    }

    /** The status of one check. */
    public enum CheckStatus {
        PASS, FAIL, SKIPPED
    }

    /** One line of the self-test report; {@code message} is safe to display and log. */
    public record Check(String name, CheckStatus status, String message) {
    }

    /** The full self-test outcome. */
    public record SelfTestReport(List<Check> checks) {

        /** Whether every check passed. */
        public boolean allPassed() {
            return checks.stream().allMatch(c -> c.status() == CheckStatus.PASS);
        }
    }

    /**
     * Runs the self-test for the given environment and credentials.
     *
     * @param environment {@code "production"} for live HMRC, anything else for sandbox
     * @param clientId    the entered client id
     * @param clientSecret the entered client secret
     * @return a report with one {@link Check} per stage; never throws for expected failures
     */
    public SelfTestReport run(String environment, String clientId, String clientSecret) {
        Check format = formatCheck(clientId, clientSecret);
        if (format.status() != CheckStatus.PASS) {
            return new SelfTestReport(List.of(
                format,
                skipped("HMRC reachable", "Skipped until the credentials look valid."),
                skipped("OAuth round-trip", "Skipped until the credentials look valid.")));
        }

        String base = "production".equalsIgnoreCase(environment) ? PRODUCTION_BASE : SANDBOX_BASE;
        Check reachable = reachableCheck(base);
        if (reachable.status() != CheckStatus.PASS) {
            return new SelfTestReport(List.of(
                format,
                reachable,
                skipped("OAuth round-trip", "Skipped because HMRC could not be reached.")));
        }

        Check roundtrip = roundtripCheck(base + "/oauth/token", clientId, clientSecret);
        return new SelfTestReport(List.of(format, reachable, roundtrip));
    }

    private Check formatCheck(String clientId, String clientSecret) {
        HmrcCredentialValidator.Result id = HmrcCredentialValidator.validateClientId(clientId);
        if (!id.valid()) {
            return new Check("Credentials format", CheckStatus.FAIL, id.message());
        }
        HmrcCredentialValidator.Result secret = HmrcCredentialValidator.validateClientSecret(clientSecret);
        if (!secret.valid()) {
            return new Check("Credentials format", CheckStatus.FAIL, secret.message());
        }
        return new Check("Credentials format", CheckStatus.PASS, "Client ID and Secret look valid.");
    }

    private Check reachableCheck(String base) {
        URI uri = URI.create(base + "/hello/world");
        if (!isHmrcHost(uri)) {
            return new Check("HMRC reachable", CheckStatus.FAIL,
                "The configured HMRC address is not an official HMRC host; refusing to connect.");
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .header("Accept", "application/vnd.hmrc.1.0+json")
            .GET()
            .build();
        try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new Check("HMRC reachable", CheckStatus.PASS, "Reached HMRC's service.");
        } catch (java.io.IOException e) {
            return new Check("HMRC reachable", CheckStatus.FAIL,
                "Couldn't reach HMRC. Check your internet connection and try again.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Check("HMRC reachable", CheckStatus.FAIL, "The connection check was interrupted.");
        }
    }

    private Check roundtripCheck(String tokenUrl, String clientId, String clientSecret) {
        URI uri = URI.create(tokenUrl);
        if (!isHmrcHost(uri)) {
            return new Check("OAuth round-trip", CheckStatus.FAIL,
                "The configured HMRC token address is not an official HMRC host; refusing to send credentials.");
        }
        String body = "grant_type=client_credentials"
            + "&client_id=" + enc(clientId)
            + "&client_secret=" + enc(clientSecret);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/vnd.hmrc.1.0+json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return roundtripResult(response.statusCode());
        } catch (java.io.IOException e) {
            return new Check("OAuth round-trip", CheckStatus.FAIL,
                "Couldn't complete the HMRC sign-in check. Check your connection and try again.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Check("OAuth round-trip", CheckStatus.FAIL, "The sign-in check was interrupted.");
        }
    }

    /**
     * Maps the token-endpoint status to a check result. The response body is deliberately not read:
     * it can echo the submitted secret, and the status code alone tells the user what to do.
     */
    private Check roundtripResult(int status) {
        if (status >= 200 && status < 300) {
            return new Check("OAuth round-trip", CheckStatus.PASS, "HMRC accepted your credentials.");
        }
        String message = switch (status) {
            case 401, 403 -> "HMRC rejected your Client ID or Client Secret. "
                + "Check they were copied exactly from the Developer Hub.";
            case 429 -> "HMRC is rate-limiting connection attempts. Wait a moment and try again.";
            default -> status >= 500
                ? "HMRC's service returned an error. Try again shortly."
                : "HMRC did not accept the connection (HTTP " + status + ").";
        };
        return new Check("OAuth round-trip", CheckStatus.FAIL, message);
    }

    private static Check skipped(String name, String message) {
        return new Check(name, CheckStatus.SKIPPED, message);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
