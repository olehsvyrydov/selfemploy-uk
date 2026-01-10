package uk.selfemploy.core.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for ErrorMessageMapper (SE-405).
 * Maps HMRC error codes to user-friendly messages.
 */
@DisplayName("ErrorMessageMapper")
class ErrorMessageMapperTest {

    private ErrorMessageMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ErrorMessageMapper();
    }

    @Nested
    @DisplayName("HMRC Error Code Mapping")
    class HmrcErrorCodeMapping {

        @Test
        @DisplayName("should map RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED")
        void shouldMapIncorrectBody() {
            String userMessage = mapper.mapErrorCode("RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED");

            assertThat(userMessage).isEqualTo("Please check your submission data");
        }

        @Test
        @DisplayName("should map RULE_PERIODIC_UPDATE_FOR_PERIOD_SUBMITTED")
        void shouldMapDuplicatePeriod() {
            String userMessage = mapper.mapErrorCode("RULE_PERIODIC_UPDATE_FOR_PERIOD_SUBMITTED");

            assertThat(userMessage).isEqualTo("This period has already been submitted");
        }

        @Test
        @DisplayName("should map RULE_TAX_YEAR_NOT_SUPPORTED")
        void shouldMapUnsupportedTaxYear() {
            String userMessage = mapper.mapErrorCode("RULE_TAX_YEAR_NOT_SUPPORTED");

            assertThat(userMessage).isEqualTo("Tax year not supported for MTD");
        }

        @Test
        @DisplayName("should map CLIENT_OR_AGENT_NOT_AUTHORISED")
        void shouldMapNotAuthorised() {
            String userMessage = mapper.mapErrorCode("CLIENT_OR_AGENT_NOT_AUTHORISED");

            assertThat(userMessage).isEqualTo("Please reconnect to HMRC");
        }

        @Test
        @DisplayName("should map MATCHING_RESOURCE_NOT_FOUND")
        void shouldMapResourceNotFound() {
            String userMessage = mapper.mapErrorCode("MATCHING_RESOURCE_NOT_FOUND");

            assertThat(userMessage).isEqualTo("Record not found at HMRC");
        }

        @ParameterizedTest
        @DisplayName("should map additional HMRC error codes")
        @CsvSource({
                "FORMAT_NINO, Invalid National Insurance number format",
                "FORMAT_TAX_YEAR, Invalid tax year format",
                "RULE_ALREADY_EXISTS, This record already exists at HMRC",
                "RULE_INSOLVENT_TRADER, Account is insolvent - contact HMRC",
                "FORMAT_VALUE, Invalid value format in submission",
                "RULE_BUSINESS_VALIDATION_FAILURE, Business validation failed - check your data"
        })
        void shouldMapAdditionalErrorCodes(String errorCode, String expectedMessage) {
            String userMessage = mapper.mapErrorCode(errorCode);

            assertThat(userMessage).isEqualTo(expectedMessage);
        }

        @Test
        @DisplayName("should return default message for unknown error code")
        void shouldReturnDefaultForUnknown() {
            String userMessage = mapper.mapErrorCode("UNKNOWN_ERROR_CODE");

            assertThat(userMessage).isEqualTo("An unexpected error occurred. Please try again or contact support.");
        }

        @Test
        @DisplayName("should handle null error code")
        void shouldHandleNullErrorCode() {
            String userMessage = mapper.mapErrorCode(null);

            assertThat(userMessage).isEqualTo("An unexpected error occurred. Please try again or contact support.");
        }

        @Test
        @DisplayName("should handle empty error code")
        void shouldHandleEmptyErrorCode() {
            String userMessage = mapper.mapErrorCode("");

            assertThat(userMessage).isEqualTo("An unexpected error occurred. Please try again or contact support.");
        }
    }

    @Nested
    @DisplayName("HTTP Status Code Mapping")
    class HttpStatusCodeMapping {

        @Test
        @DisplayName("should map 400 Bad Request")
        void shouldMap400() {
            String userMessage = mapper.mapHttpStatus(400);

            assertThat(userMessage).isEqualTo("The request was invalid. Please check your data and try again.");
        }

        @Test
        @DisplayName("should map 401 Unauthorized")
        void shouldMap401() {
            String userMessage = mapper.mapHttpStatus(401);

            assertThat(userMessage).isEqualTo("Your HMRC session has expired. Please reconnect to HMRC.");
        }

        @Test
        @DisplayName("should map 403 Forbidden")
        void shouldMap403() {
            String userMessage = mapper.mapHttpStatus(403);

            assertThat(userMessage).isEqualTo("You don't have permission to perform this action. Please check your HMRC access.");
        }

        @Test
        @DisplayName("should map 404 Not Found")
        void shouldMap404() {
            String userMessage = mapper.mapHttpStatus(404);

            assertThat(userMessage).isEqualTo("The requested record was not found at HMRC.");
        }

        @Test
        @DisplayName("should map 429 Too Many Requests")
        void shouldMap429() {
            String userMessage = mapper.mapHttpStatus(429);

            assertThat(userMessage).isEqualTo("Too many requests. Please wait a moment before trying again.");
        }

        @Test
        @DisplayName("should map 500 Internal Server Error")
        void shouldMap500() {
            String userMessage = mapper.mapHttpStatus(500);

            assertThat(userMessage).isEqualTo("HMRC is experiencing technical difficulties. Please try again later.");
        }

        @Test
        @DisplayName("should map 502 Bad Gateway")
        void shouldMap502() {
            String userMessage = mapper.mapHttpStatus(502);

            assertThat(userMessage).isEqualTo("HMRC services are temporarily unavailable. Please try again later.");
        }

        @Test
        @DisplayName("should map 503 Service Unavailable")
        void shouldMap503() {
            String userMessage = mapper.mapHttpStatus(503);

            assertThat(userMessage).isEqualTo("HMRC services are temporarily unavailable. Please try again later.");
        }

        @Test
        @DisplayName("should map 504 Gateway Timeout")
        void shouldMap504() {
            String userMessage = mapper.mapHttpStatus(504);

            assertThat(userMessage).isEqualTo("HMRC is taking too long to respond. Please try again later.");
        }

        @Test
        @DisplayName("should return default message for unknown status")
        void shouldReturnDefaultForUnknownStatus() {
            String userMessage = mapper.mapHttpStatus(418); // I'm a teapot

            assertThat(userMessage).isEqualTo("An error occurred while communicating with HMRC.");
        }
    }

    @Nested
    @DisplayName("Exception Category Detection")
    class ExceptionCategoryDetection {

        @Test
        @DisplayName("should detect 4xx as client error")
        void shouldDetect4xxAsClientError() {
            assertThat(mapper.isClientError(400)).isTrue();
            assertThat(mapper.isClientError(404)).isTrue();
            assertThat(mapper.isClientError(499)).isTrue();
            assertThat(mapper.isClientError(500)).isFalse();
        }

        @Test
        @DisplayName("should detect 5xx as server error")
        void shouldDetect5xxAsServerError() {
            assertThat(mapper.isServerError(500)).isTrue();
            assertThat(mapper.isServerError(503)).isTrue();
            assertThat(mapper.isServerError(599)).isTrue();
            assertThat(mapper.isServerError(400)).isFalse();
        }

        @Test
        @DisplayName("should detect retryable status codes")
        void shouldDetectRetryableStatusCodes() {
            // Retryable: 429, 500, 502, 503, 504
            assertThat(mapper.isRetryable(429)).isTrue();
            assertThat(mapper.isRetryable(500)).isTrue();
            assertThat(mapper.isRetryable(502)).isTrue();
            assertThat(mapper.isRetryable(503)).isTrue();
            assertThat(mapper.isRetryable(504)).isTrue();

            // Not retryable: 400, 401, 403, 404
            assertThat(mapper.isRetryable(400)).isFalse();
            assertThat(mapper.isRetryable(401)).isFalse();
            assertThat(mapper.isRetryable(403)).isFalse();
            assertThat(mapper.isRetryable(404)).isFalse();
        }
    }
}
