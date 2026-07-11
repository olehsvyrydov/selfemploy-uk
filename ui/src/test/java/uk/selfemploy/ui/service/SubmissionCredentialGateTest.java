package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.service.SubmissionCredentialGate.Status;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SubmissionCredentialGate")
class SubmissionCredentialGateTest {

    @Test
    @DisplayName("blocks when not connected to HMRC, with an actionable message")
    void notConnected() {
        SubmissionCredentialGate.Decision decision = SubmissionCredentialGate.evaluate(false, "AA123456A");
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.status()).isEqualTo(Status.NOT_CONNECTED);
        assertThat(decision.message()).contains("HMRC Submission");
    }

    @Test
    @DisplayName("blocks when the NINO is missing")
    void missingNino() {
        assertThat(SubmissionCredentialGate.evaluate(true, "  ").status()).isEqualTo(Status.NO_NINO);
        assertThat(SubmissionCredentialGate.evaluate(true, null).status()).isEqualTo(Status.NO_NINO);
    }

    @Test
    @DisplayName("allows when connected and a NINO is present")
    void allowed() {
        SubmissionCredentialGate.Decision decision = SubmissionCredentialGate.evaluate(true, "AA123456A");
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.message()).isNull();
    }

    @Test
    @DisplayName("not-connected takes precedence over a missing NINO")
    void notConnectedFirst() {
        assertThat(SubmissionCredentialGate.evaluate(false, null).status()).isEqualTo(Status.NOT_CONNECTED);
    }
}
