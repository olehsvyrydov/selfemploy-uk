package uk.selfemploy.common.legal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD tests for {@link SubmissionConfirmation} — SLFEMPUK-35 / S17-11.
 */
@DisplayName("SubmissionConfirmation (SLFEMPUK-35)")
class SubmissionConfirmationTest {

    @Test
    @DisplayName("should accept a valid confirmation")
    void shouldAcceptValidConfirmation() {
        Instant now = Instant.parse("2026-01-15T09:30:00Z");
        SubmissionConfirmation c = new SubmissionConfirmation("alice", true, now);

        assertThat(c.userId()).isEqualTo("alice");
        assertThat(c.confirmedByUser()).isTrue();
        assertThat(c.confirmedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("should reject null userId")
    void shouldRejectNullUserId() {
        assertThatThrownBy(() -> new SubmissionConfirmation(null, true, Instant.now()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("userId");
    }

    @Test
    @DisplayName("should reject blank userId")
    void shouldRejectBlankUserId() {
        assertThatThrownBy(() -> new SubmissionConfirmation("   ", true, Instant.now()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should reject null timestamp")
    void shouldRejectNullTimestamp() {
        assertThatThrownBy(() -> new SubmissionConfirmation("alice", true, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("confirmedAt");
    }

    @Test
    @DisplayName("should allow confirmedByUser=false (rejection handled by service gate)")
    void shouldAllowFalseConfirmedByUser() {
        // The value object itself permits a false flag; the service gate rejects it.
        SubmissionConfirmation c = new SubmissionConfirmation("alice", false, Instant.now());
        assertThat(c.confirmedByUser()).isFalse();
    }

    @Test
    @DisplayName("confirmedNow factory should set confirmedByUser=true")
    void confirmedNowShouldBeConfirmed() {
        Instant now = Instant.parse("2026-01-15T09:30:00Z");
        SubmissionConfirmation c = SubmissionConfirmation.confirmedNow("alice", now);

        assertThat(c.confirmedByUser()).isTrue();
        assertThat(c.confirmedAt()).isEqualTo(now);
        assertThat(c.userId()).isEqualTo("alice");
    }
}
