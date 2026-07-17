package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import uk.selfemploy.ui.service.HmrcConnectionSelfTest.CheckStatus;
import uk.selfemploy.ui.service.HmrcConnectionSelfTest.SelfTestReport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live self-test against the real HMRC sandbox. Excluded from the default build (tagged
 * {@code hmrc-sandbox}); it runs only when {@code HMRC_CLIENT_ID} and {@code HMRC_CLIENT_SECRET}
 * for a sandbox application are present in the environment.
 *
 * <p>This is the end-to-end acceptance for {@link HmrcConnectionSelfTest} — the unit tests stub the
 * HTTP layer; this proves the three checks behave against HMRC itself. Run with, e.g.:
 * {@code mvn -pl ui test -Dtest=HmrcConnectionSelfTestSandboxIT -Dgroups=hmrc-sandbox}.
 */
@Tag("hmrc-sandbox")
@DisplayName("HmrcConnectionSelfTest — live HMRC sandbox")
class HmrcConnectionSelfTestSandboxIT {

    @Test
    @DisplayName("valid sandbox credentials pass all three checks")
    @EnabledIfEnvironmentVariable(named = "HMRC_CLIENT_ID", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "HMRC_CLIENT_SECRET", matches = ".+")
    void validSandboxCredentialsPass() {
        String clientId = System.getenv("HMRC_CLIENT_ID");
        String clientSecret = System.getenv("HMRC_CLIENT_SECRET");

        SelfTestReport report = new HmrcConnectionSelfTest().run("sandbox", clientId, clientSecret);

        assertThat(report.checks())
            .as("every check should have run and passed against the sandbox")
            .allSatisfy(check -> assertThat(check.status()).isEqualTo(CheckStatus.PASS));
        assertThat(report.allPassed()).isTrue();
    }

    @Test
    @DisplayName("a wrong secret is reported as an HMRC rejection, not a network error")
    @EnabledIfEnvironmentVariable(named = "HMRC_CLIENT_ID", matches = ".+")
    void wrongSecretIsRejectedByHmrc() {
        String clientId = System.getenv("HMRC_CLIENT_ID");

        SelfTestReport report = new HmrcConnectionSelfTest()
            .run("sandbox", clientId, "deliberately-wrong-secret-value");

        HmrcConnectionSelfTest.Check roundtrip = report.checks().stream()
            .filter(c -> c.name().toLowerCase().contains("round"))
            .findFirst().orElseThrow();
        assertThat(roundtrip.status()).isEqualTo(CheckStatus.FAIL);
    }
}
