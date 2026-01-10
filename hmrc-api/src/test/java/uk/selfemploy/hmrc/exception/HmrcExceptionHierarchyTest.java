package uk.selfemploy.hmrc.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for HMRC exception hierarchy (SE-405).
 */
@DisplayName("HMRC Exception Hierarchy")
class HmrcExceptionHierarchyTest {

    @Nested
    @DisplayName("HmrcApiException (base)")
    class HmrcApiExceptionTests {

        @Test
        @DisplayName("should have userMessage field")
        void shouldHaveUserMessageField() {
            var exception = new HmrcApiException(
                    "Technical error",
                    "INTERNAL_ERROR",
                    500,
                    "Something went wrong. Please try again."
            );

            assertThat(exception.getMessage()).isEqualTo("Technical error");
            assertThat(exception.getErrorCode()).isEqualTo("INTERNAL_ERROR");
            assertThat(exception.getHttpStatus()).isEqualTo(500);
            assertThat(exception.getUserMessage()).isEqualTo("Something went wrong. Please try again.");
        }

        @Test
        @DisplayName("should default userMessage to message when not provided")
        void shouldDefaultUserMessageToMessage() {
            var exception = new HmrcApiException("Error message");

            assertThat(exception.getUserMessage()).isEqualTo("Error message");
        }

        @Test
        @DisplayName("should indicate if retryable")
        void shouldIndicateIfRetryable() {
            var exception = new HmrcApiException("Error");

            assertThat(exception.isRetryable()).isFalse();
        }
    }

    @Nested
    @DisplayName("HmrcValidationException (4xx)")
    class HmrcValidationExceptionTests {

        @Test
        @DisplayName("should extend HmrcApiException")
        void shouldExtendHmrcApiException() {
            var exception = new HmrcValidationException(
                    "Validation failed",
                    "RULE_INCORRECT_BODY",
                    400
            );

            assertThat(exception).isInstanceOf(HmrcApiException.class);
        }

        @Test
        @DisplayName("should hold validation errors list")
        void shouldHoldValidationErrorsList() {
            var errors = List.of(
                    new ValidationError("field1", "Field 1 is required"),
                    new ValidationError("field2", "Invalid format")
            );

            var exception = new HmrcValidationException(
                    "Validation failed",
                    "RULE_INCORRECT_BODY",
                    400,
                    errors
            );

            assertThat(exception.getValidationErrors()).hasSize(2);
            assertThat(exception.getValidationErrors().get(0).field()).isEqualTo("field1");
            assertThat(exception.getValidationErrors().get(0).message()).isEqualTo("Field 1 is required");
        }

        @Test
        @DisplayName("should not be retryable")
        void shouldNotBeRetryable() {
            var exception = new HmrcValidationException("Bad request", "INVALID", 400);

            assertThat(exception.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("should handle 4xx status codes")
        void shouldHandle4xxStatusCodes() {
            assertThat(new HmrcValidationException("Not found", "NOT_FOUND", 404).getHttpStatus()).isEqualTo(404);
            assertThat(new HmrcValidationException("Forbidden", "FORBIDDEN", 403).getHttpStatus()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("HmrcServerException (5xx)")
    class HmrcServerExceptionTests {

        @Test
        @DisplayName("should extend HmrcApiException")
        void shouldExtendHmrcApiException() {
            var exception = new HmrcServerException("Server error", "SERVER_ERROR", 500);

            assertThat(exception).isInstanceOf(HmrcApiException.class);
        }

        @Test
        @DisplayName("should be retryable by default")
        void shouldBeRetryableByDefault() {
            var exception = new HmrcServerException("Server error", "SERVER_ERROR", 500);

            assertThat(exception.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("should handle 5xx status codes")
        void shouldHandle5xxStatusCodes() {
            assertThat(new HmrcServerException("Internal", "ERROR", 500).getHttpStatus()).isEqualTo(500);
            assertThat(new HmrcServerException("Unavailable", "ERROR", 503).getHttpStatus()).isEqualTo(503);
            assertThat(new HmrcServerException("Gateway", "ERROR", 502).getHttpStatus()).isEqualTo(502);
        }

        @Test
        @DisplayName("should provide user-friendly message")
        void shouldProvideUserFriendlyMessage() {
            var exception = new HmrcServerException(
                    "Internal server error",
                    "SERVER_ERROR",
                    500,
                    "HMRC services are temporarily unavailable. The system will retry automatically."
            );

            assertThat(exception.getUserMessage())
                    .isEqualTo("HMRC services are temporarily unavailable. The system will retry automatically.");
        }
    }

    @Nested
    @DisplayName("HmrcNetworkException")
    class HmrcNetworkExceptionTests {

        @Test
        @DisplayName("should extend HmrcApiException")
        void shouldExtendHmrcApiException() {
            var exception = new HmrcNetworkException("Connection timeout");

            assertThat(exception).isInstanceOf(HmrcApiException.class);
        }

        @Test
        @DisplayName("should be retryable")
        void shouldBeRetryable() {
            var exception = new HmrcNetworkException("Timeout", new java.net.SocketTimeoutException());

            assertThat(exception.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("should hold root cause")
        void shouldHoldRootCause() {
            var cause = new java.net.SocketTimeoutException("Read timed out");
            var exception = new HmrcNetworkException("Timeout", cause);

            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("should have zero http status for network errors")
        void shouldHaveZeroHttpStatusForNetworkErrors() {
            var exception = new HmrcNetworkException("Connection refused");

            assertThat(exception.getHttpStatus()).isEqualTo(0);
        }

        @Test
        @DisplayName("should provide user-friendly timeout message")
        void shouldProvideUserFriendlyTimeoutMessage() {
            var exception = HmrcNetworkException.timeout();

            assertThat(exception.getUserMessage())
                    .isEqualTo("HMRC is taking too long to respond. Please try again in a few minutes.");
        }

        @Test
        @DisplayName("should provide user-friendly connection message")
        void shouldProvideUserFriendlyConnectionMessage() {
            var exception = HmrcNetworkException.connectionFailed();

            assertThat(exception.getUserMessage())
                    .isEqualTo("Unable to reach HMRC. Please check your internet connection and try again.");
        }
    }

    @Nested
    @DisplayName("HmrcCircuitOpenException")
    class HmrcCircuitOpenExceptionTests {

        @Test
        @DisplayName("should extend HmrcApiException")
        void shouldExtendHmrcApiException() {
            var exception = new HmrcCircuitOpenException(Duration.ofSeconds(30));

            assertThat(exception).isInstanceOf(HmrcApiException.class);
        }

        @Test
        @DisplayName("should hold retryAfter duration")
        void shouldHoldRetryAfterDuration() {
            var exception = new HmrcCircuitOpenException(Duration.ofSeconds(45));

            assertThat(exception.getRetryAfter()).isEqualTo(Duration.ofSeconds(45));
        }

        @Test
        @DisplayName("should not be retryable immediately")
        void shouldNotBeRetryableImmediately() {
            var exception = new HmrcCircuitOpenException(Duration.ofSeconds(30));

            // Circuit open exceptions should NOT be automatically retried
            // The circuit breaker manages when to retry
            assertThat(exception.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("should provide user message with retry time")
        void shouldProvideUserMessageWithRetryTime() {
            var exception = new HmrcCircuitOpenException(Duration.ofSeconds(30));

            assertThat(exception.getUserMessage())
                    .isEqualTo("HMRC services are experiencing issues. Please try again in 30 seconds.");
        }

        @Test
        @DisplayName("should format minutes correctly")
        void shouldFormatMinutesCorrectly() {
            var exception = new HmrcCircuitOpenException(Duration.ofMinutes(2));

            assertThat(exception.getUserMessage())
                    .isEqualTo("HMRC services are experiencing issues. Please try again in 2 minutes.");
        }
    }
}
