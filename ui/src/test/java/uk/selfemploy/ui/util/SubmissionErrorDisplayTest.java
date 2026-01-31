package uk.selfemploy.ui.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SubmissionErrorDisplay record.
 *
 * SE-10E-001: Verifies the data carrier record works correctly.
 */
@DisplayName("SubmissionErrorDisplay Tests")
class SubmissionErrorDisplayTest {

    @Nested
    @DisplayName("Record Creation")
    class RecordCreationTests {

        @Test
        @DisplayName("TC-001: should create record with all fields")
        void shouldCreateWithAllFields() {
            var display = new SubmissionErrorDisplay(
                "National Insurance Number Not Set",
                "Your NINO is required for MTD submissions.",
                "Go to Settings > Profile and enter your NINO.",
                "NINO_REQUIRED",
                false,
                true
            );

            assertThat(display.title()).isEqualTo("National Insurance Number Not Set");
            assertThat(display.message()).isEqualTo("Your NINO is required for MTD submissions.");
            assertThat(display.guidance()).isEqualTo("Go to Settings > Profile and enter your NINO.");
            assertThat(display.errorCode()).isEqualTo("NINO_REQUIRED");
            assertThat(display.retryable()).isFalse();
            assertThat(display.settingsError()).isTrue();
        }

        @Test
        @DisplayName("TC-002: should allow null errorCode")
        void shouldAllowNullErrorCode() {
            var display = new SubmissionErrorDisplay(
                "Submission Failed",
                "An unknown error occurred.",
                "Please try again.",
                null,
                false,
                false
            );

            assertThat(display.errorCode()).isNull();
        }

        @Test
        @DisplayName("TC-003: records with same values should be equal")
        void shouldBeEqualWithSameValues() {
            var display1 = new SubmissionErrorDisplay("Title", "Msg", "Guide", "CODE", true, false);
            var display2 = new SubmissionErrorDisplay("Title", "Msg", "Guide", "CODE", true, false);

            assertThat(display1).isEqualTo(display2);
            assertThat(display1.hashCode()).isEqualTo(display2.hashCode());
        }

        @Test
        @DisplayName("TC-004: toString should include all fields")
        void shouldIncludeAllFieldsInToString() {
            var display = new SubmissionErrorDisplay(
                "Test Title", "Test Message", "Test Guidance", "TEST_CODE", true, false);

            String str = display.toString();
            assertThat(str).contains("Test Title");
            assertThat(str).contains("Test Message");
            assertThat(str).contains("Test Guidance");
            assertThat(str).contains("TEST_CODE");
            assertThat(str).contains("true");  // retryable
            assertThat(str).contains("false"); // settingsError
        }
    }
}
