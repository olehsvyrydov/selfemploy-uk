package uk.selfemploy.ui.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SubmissionEnvironment")
class SubmissionEnvironmentTest {

    @AfterEach
    void clearProperty() {
        System.clearProperty("HMRC_API_BASE_URL");
    }

    @Test
    @DisplayName("defaults to the HMRC sandbox")
    void defaultsToSandbox() {
        System.clearProperty("HMRC_API_BASE_URL");
        assertThat(SubmissionEnvironment.current()).isEqualTo(SubmissionEnvironment.SANDBOX);
        assertThat(SubmissionEnvironment.current().badgeLabel()).isEqualTo("HMRC Sandbox");
        assertThat(SubmissionEnvironment.current().isSandbox()).isTrue();
    }

    @Test
    @DisplayName("a test-api base URL is the sandbox")
    void testApiIsSandbox() {
        System.setProperty("HMRC_API_BASE_URL", "https://test-api.service.hmrc.gov.uk");
        assertThat(SubmissionEnvironment.current()).isEqualTo(SubmissionEnvironment.SANDBOX);
    }

    @Test
    @DisplayName("the live API base URL is production")
    void liveApiIsProduction() {
        System.setProperty("HMRC_API_BASE_URL", "https://api.service.hmrc.gov.uk");
        assertThat(SubmissionEnvironment.current()).isEqualTo(SubmissionEnvironment.PRODUCTION);
        assertThat(SubmissionEnvironment.current().badgeLabel()).isEqualTo("HMRC Production");
    }
}
