package uk.selfemploy.hmrc.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HmrcHosts")
class HmrcHostsTest {

    @AfterEach
    void clearLoopbackOptIn() {
        System.clearProperty(HmrcHosts.ALLOW_LOOPBACK_PROPERTY);
    }

    @Test
    @DisplayName("allows HTTPS on official HMRC sandbox and production hosts")
    void allowsHmrcHosts() {
        assertThat(HmrcHosts.isAllowed(URI.create("https://test-api.service.hmrc.gov.uk/oauth/token"))).isTrue();
        assertThat(HmrcHosts.isAllowed(URI.create("https://api.service.hmrc.gov.uk/oauth/token"))).isTrue();
        assertThat(HmrcHosts.isAllowed(URI.create("https://www.tax.service.gov.uk/oauth/authorize"))).isTrue();
        assertThat(HmrcHosts.isAllowed(URI.create("https://test-www.tax.service.gov.uk/oauth/authorize"))).isTrue();
    }

    @Test
    @DisplayName("rejects non-HMRC hosts, suffix look-alikes, and plaintext HTTP")
    void rejectsEverythingElse() {
        assertThat(HmrcHosts.isAllowed(URI.create("https://attacker.example/collect"))).isFalse();
        assertThat(HmrcHosts.isAllowed(URI.create("https://service.hmrc.gov.uk.attacker.example/x"))).isFalse();
        assertThat(HmrcHosts.isAllowed(URI.create("http://api.service.hmrc.gov.uk/oauth/token"))).isFalse();
        assertThat(HmrcHosts.isAllowed(null)).isFalse();
    }

    @Test
    @DisplayName("rejects loopback by default")
    void rejectsLoopbackByDefault() {
        assertThat(HmrcHosts.isAllowed(URI.create("http://localhost:8088/oauth/token"))).isFalse();
        assertThat(HmrcHosts.isAllowed(URI.create("http://127.0.0.1:8088/oauth/token"))).isFalse();
    }

    @Test
    @DisplayName("allows loopback only when the test opt-in property is set")
    void allowsLoopbackUnderOptIn() {
        System.setProperty(HmrcHosts.ALLOW_LOOPBACK_PROPERTY, "true");

        assertThat(HmrcHosts.isAllowed(URI.create("http://localhost:8088/oauth/token"))).isTrue();
        assertThat(HmrcHosts.isAllowed(URI.create("http://127.0.0.1:9999/x"))).isTrue();
        // The opt-in never widens the allowlist to arbitrary remote hosts.
        assertThat(HmrcHosts.isAllowed(URI.create("http://attacker.example/x"))).isFalse();
    }
}
