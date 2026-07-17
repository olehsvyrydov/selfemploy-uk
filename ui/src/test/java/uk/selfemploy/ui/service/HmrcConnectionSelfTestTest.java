package uk.selfemploy.ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.service.HmrcConnectionSelfTest.Check;
import uk.selfemploy.ui.service.HmrcConnectionSelfTest.CheckStatus;
import uk.selfemploy.ui.service.HmrcConnectionSelfTest.SelfTestReport;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("HmrcConnectionSelfTest")
class HmrcConnectionSelfTestTest {

    private static final String CLIENT_ID = "dK9fJ2mNpQ4rSt7uVwXy";
    private static final String CLIENT_SECRET = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    private HttpClient httpClient;
    private HmrcConnectionSelfTest selfTest;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        selfTest = new HmrcConnectionSelfTest(httpClient);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(status);
        when(resp.body()).thenReturn(body);
        return resp;
    }

    private Check checkNamed(SelfTestReport report, String partialName) {
        return report.checks().stream()
            .filter(c -> c.name().toLowerCase().replaceAll("[^a-z]", "").contains(partialName))
            .findFirst().orElseThrow();
    }

    @Nested
    @DisplayName("host allowlist")
    class HostAllowlist {

        @Test
        @DisplayName("accepts HMRC sandbox and production hosts")
        void acceptsHmrcHosts() {
            assertThat(HmrcConnectionSelfTest.isHmrcHost(
                URI.create("https://test-api.service.hmrc.gov.uk/oauth/token"))).isTrue();
            assertThat(HmrcConnectionSelfTest.isHmrcHost(
                URI.create("https://api.service.hmrc.gov.uk/oauth/token"))).isTrue();
            assertThat(HmrcConnectionSelfTest.isHmrcHost(
                URI.create("https://www.tax.service.gov.uk/oauth/authorize"))).isTrue();
        }

        @Test
        @DisplayName("rejects a look-alike or attacker host")
        void rejectsNonHmrcHosts() {
            assertThat(HmrcConnectionSelfTest.isHmrcHost(
                URI.create("https://attacker.example/collect"))).isFalse();
            assertThat(HmrcConnectionSelfTest.isHmrcHost(
                URI.create("https://service.hmrc.gov.uk.attacker.example/x"))).isFalse();
            assertThat(HmrcConnectionSelfTest.isHmrcHost(
                URI.create("http://api.service.hmrc.gov.uk/oauth/token"))).isFalse(); // not https
        }
    }

    @Nested
    @DisplayName("credentials-format check")
    class FormatCheck {

        @Test
        @DisplayName("garbage credentials fail format and the network checks are skipped, not sent")
        void garbageSkipsNetwork() {
            SelfTestReport report = selfTest.run("sandbox", "abc", "abc");

            assertThat(checkNamed(report, "format").status()).isEqualTo(CheckStatus.FAIL);
            assertThat(checkNamed(report, "reachable").status()).isEqualTo(CheckStatus.SKIPPED);
            assertThat(checkNamed(report, "roundtrip").status()).isEqualTo(CheckStatus.SKIPPED);
            // Nothing was sent to HMRC — a bad secret must not leave the machine.
            verifyNoNetwork();
        }

        @SuppressWarnings("unchecked")
        private void verifyNoNetwork() {
            try {
                org.mockito.Mockito.verify(httpClient, org.mockito.Mockito.never())
                    .send(any(HttpRequest.class), any());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    @DisplayName("network checks")
    class NetworkChecks {

        @Test
        @DisplayName("all three pass when HMRC is reachable and the credentials are accepted")
        void allPass() throws Exception {
            HttpResponse<String> hello = response(200, "{\"message\":\"Hello World\"}");
            HttpResponse<String> token = response(200, "{\"access_token\":\"app-tok\",\"expires_in\":14400}");
            when(httpClient.<String>send(any(HttpRequest.class), any()))
                .thenReturn(hello).thenReturn(token);

            SelfTestReport report = selfTest.run("sandbox", CLIENT_ID, CLIENT_SECRET);

            assertThat(checkNamed(report, "format").status()).isEqualTo(CheckStatus.PASS);
            assertThat(checkNamed(report, "reachable").status()).isEqualTo(CheckStatus.PASS);
            assertThat(checkNamed(report, "roundtrip").status()).isEqualTo(CheckStatus.PASS);
            assertThat(report.allPassed()).isTrue();
        }

        @Test
        @DisplayName("reachable passes but roundtrip fails when HMRC rejects the credentials (401 invalid_client)")
        void roundtripFailsOnBadCredentials() throws Exception {
            HttpResponse<String> hello = response(200, "{\"message\":\"Hello World\"}");
            HttpResponse<String> token = response(401, "{\"error\":\"invalid_client\"}");
            when(httpClient.<String>send(any(HttpRequest.class), any()))
                .thenReturn(hello).thenReturn(token);

            SelfTestReport report = selfTest.run("sandbox", CLIENT_ID, CLIENT_SECRET);

            assertThat(checkNamed(report, "reachable").status()).isEqualTo(CheckStatus.PASS);
            assertThat(checkNamed(report, "roundtrip").status()).isEqualTo(CheckStatus.FAIL);
            assertThat(report.allPassed()).isFalse();
        }

        @Test
        @DisplayName("reachable fails on a network error and roundtrip is skipped")
        void reachableFailsOnIoError() throws Exception {
            when(httpClient.<String>send(any(HttpRequest.class), any()))
                .thenThrow(new IOException("connection refused"));

            SelfTestReport report = selfTest.run("sandbox", CLIENT_ID, CLIENT_SECRET);

            assertThat(checkNamed(report, "reachable").status()).isEqualTo(CheckStatus.FAIL);
            assertThat(checkNamed(report, "roundtrip").status()).isEqualTo(CheckStatus.SKIPPED);
        }

        @Test
        @DisplayName("a 429 on the roundtrip is reported as rate-limited, not a credential failure")
        void roundtripRateLimited() throws Exception {
            HttpResponse<String> hello = response(200, "{\"message\":\"Hello World\"}");
            HttpResponse<String> token = response(429, "{\"error\":\"too_many_requests\"}");
            when(httpClient.<String>send(any(HttpRequest.class), any()))
                .thenReturn(hello).thenReturn(token);

            SelfTestReport report = selfTest.run("sandbox", CLIENT_ID, CLIENT_SECRET);

            Check roundtrip = checkNamed(report, "roundtrip");
            assertThat(roundtrip.status()).isEqualTo(CheckStatus.FAIL);
            assertThat(roundtrip.message().toLowerCase()).contains("rate");
        }
    }

    @Nested
    @DisplayName("does not leak the secret")
    class NoLeak {

        @Test
        @DisplayName("no check message contains the client secret, even when HMRC echoes it back")
        void noMessageContainsSecret() throws Exception {
            HttpResponse<String> hello = response(200, "{\"message\":\"Hello World\"}");
            // A hostile/echoing endpoint reflects the secret in its error body.
            HttpResponse<String> token = response(400,
                "{\"error\":\"bad\",\"detail\":\"" + CLIENT_SECRET + "\"}");
            when(httpClient.<String>send(any(HttpRequest.class), any()))
                .thenReturn(hello).thenReturn(token);

            SelfTestReport report = selfTest.run("sandbox", CLIENT_ID, CLIENT_SECRET);

            for (Check check : report.checks()) {
                assertThat(check.message() == null ? "" : check.message())
                    .doesNotContain(CLIENT_SECRET);
            }
        }
    }
}
