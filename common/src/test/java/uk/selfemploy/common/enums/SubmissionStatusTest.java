package uk.selfemploy.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SubmissionStatus")
class SubmissionStatusTest {

    @Test
    @DisplayName("NOT_SUBMITTED exists for records that never reached HMRC")
    void notSubmittedExists() {
        SubmissionStatus status = SubmissionStatus.valueOf("NOT_SUBMITTED");
        assertThat(status).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
    }

    @Test
    @DisplayName("NOT_SUBMITTED is not a successful submission")
    void notSubmittedIsNotSuccessful() {
        assertThat(SubmissionStatus.NOT_SUBMITTED.isSuccessful()).isFalse();
    }

    @Test
    @DisplayName("NOT_SUBMITTED is terminal - it will never be processed by HMRC")
    void notSubmittedIsTerminal() {
        assertThat(SubmissionStatus.NOT_SUBMITTED.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("NOT_SUBMITTED was never sent to HMRC")
    void notSubmittedWasNotSentToHmrc() {
        assertThat(SubmissionStatus.NOT_SUBMITTED.isSentToHmrc()).isFalse();
    }

    @Test
    @DisplayName("the four HMRC-response statuses count as sent to HMRC")
    void hmrcResponseStatusesAreSentToHmrc() {
        assertThat(SubmissionStatus.PENDING.isSentToHmrc()).isTrue();
        assertThat(SubmissionStatus.SUBMITTED.isSentToHmrc()).isTrue();
        assertThat(SubmissionStatus.ACCEPTED.isSentToHmrc()).isTrue();
        assertThat(SubmissionStatus.REJECTED.isSentToHmrc()).isTrue();
    }

    @Test
    @DisplayName("in-flight and accepted submissions lock their period; rejected and local do not")
    void locksSubmittedPeriod() {
        assertThat(SubmissionStatus.PENDING.locksSubmittedPeriod()).isTrue();
        assertThat(SubmissionStatus.SUBMITTED.locksSubmittedPeriod()).isTrue();
        assertThat(SubmissionStatus.ACCEPTED.locksSubmittedPeriod()).isTrue();
        assertThat(SubmissionStatus.REJECTED.locksSubmittedPeriod()).isFalse();
        assertThat(SubmissionStatus.NOT_SUBMITTED.locksSubmittedPeriod()).isFalse();
    }

    @Test
    @DisplayName("NOT_SUBMITTED has a display name that reads as a local record")
    void notSubmittedDisplayName() {
        assertThat(SubmissionStatus.NOT_SUBMITTED.getDisplayName().toLowerCase())
            .contains("not submitted");
    }
}
