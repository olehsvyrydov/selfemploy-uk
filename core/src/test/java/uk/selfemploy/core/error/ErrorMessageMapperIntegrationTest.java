package uk.selfemploy.core.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for Error Message Mapping (Sprint 10A).
 * Tests EM-I01 through EM-I04 from /rob's test design.
 *
 * <p>SE-10A-007: Error Messages Review Integration Tests</p>
 *
 * <p>These tests verify error message mapping integration with
 * application services and exception handling.</p>
 *
 * @see docs/sprints/sprint-10A/testing/rob-test-design-10A-10B.md
 */
@DisplayName("Error Message Mapper Integration Tests")
class ErrorMessageMapperIntegrationTest {

    private ErrorMessageMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ErrorMessageMapper();
    }

    // === EM-I01: User-Friendly Error on Import Failure ===

    @Nested
    @DisplayName("EM-I01: Import Failure Error Messages")
    class ImportFailureErrorMessages {

        @Test
        @DisplayName("should provide clear guidance for malformed CSV")
        void shouldProvideClearGuidanceForMalformedCsv() {
            // Given - simulated import validation error
            String errorCode = "FORMAT_VALUE";

            // When
            String userMessage = mapper.mapErrorCode(errorCode);

            // Then
            assertThat(userMessage)
                    .isNotBlank()
                    .doesNotContain("CSV")  // Don't expose internal format
                    .doesNotContain("parse")  // Don't use technical terms
                    .doesNotContain("java.");
        }

        @Test
        @DisplayName("should provide guidance for file not found during import")
        void shouldProvideGuidanceForFileNotFound() {
            // Given - HTTP 404 scenario (resource not found)
            int httpStatus = 404;

            // When
            String userMessage = mapper.mapHttpStatus(httpStatus);

            // Then
            assertThat(userMessage)
                    .containsIgnoringCase("not found");
        }

        @Test
        @DisplayName("should handle import validation errors gracefully")
        void shouldHandleImportValidationErrorsGracefully() {
            // Given - validation error during import
            String errorCode = "RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED";

            // When
            String userMessage = mapper.mapErrorCode(errorCode);

            // Then
            assertThat(userMessage)
                    .isNotBlank()
                    .containsAnyOf("check", "Check", "data");
        }
    }

    // === EM-I02: User-Friendly Error on Export Failure ===

    @Nested
    @DisplayName("EM-I02: Export Failure Error Messages")
    class ExportFailureErrorMessages {

        @Test
        @DisplayName("should provide alternative for permission denied")
        void shouldProvideAlternativeForPermissionDenied() {
            // Given - HTTP 403 scenario
            int httpStatus = 403;

            // When
            String userMessage = mapper.mapHttpStatus(httpStatus);

            // Then
            assertThat(userMessage)
                    .containsIgnoringCase("permission")
                    .doesNotContain("AccessDeniedException");
        }

        @Test
        @DisplayName("should not expose file system paths in error")
        void shouldNotExposeFileSystemPaths() {
            // When
            String userMessage = mapper.mapHttpStatus(403);

            // Then
            assertThat(userMessage)
                    .doesNotContain("/home/")
                    .doesNotContain("/Users/")
                    .doesNotContain("C:\\");
        }
    }

    // === EM-I03: User-Friendly Error on Save Failure ===

    @Nested
    @DisplayName("EM-I03: Save Failure Error Messages")
    class SaveFailureErrorMessages {

        @Test
        @DisplayName("should provide retry option for database errors")
        void shouldProvideRetryOptionForDatabaseErrors() {
            // Given - simulated database error (HTTP 500)
            int httpStatus = 500;

            // When
            String userMessage = mapper.mapHttpStatus(httpStatus);
            boolean isRetryable = mapper.isRetryable(httpStatus);

            // Then
            assertThat(isRetryable).isTrue();
            assertThat(userMessage)
                    .containsAnyOf("try again", "later", "Try");
        }

        @Test
        @DisplayName("should not expose database details")
        void shouldNotExposeDatabaseDetails() {
            // When
            String userMessage = mapper.mapHttpStatus(500);

            // Then
            assertThat(userMessage)
                    .doesNotContain("SQL")
                    .doesNotContain("H2")
                    .doesNotContain("database")
                    .doesNotContain("constraint")
                    .doesNotContain("transaction");
        }

        @Test
        @DisplayName("should classify save errors correctly")
        void shouldClassifySaveErrorsCorrectly() {
            // Given - various save error scenarios
            int serverError = 500;
            int clientError = 400;

            // When/Then
            assertThat(mapper.isServerError(serverError)).isTrue();
            assertThat(mapper.isClientError(clientError)).isTrue();
            assertThat(mapper.isRetryable(serverError)).isTrue();
            assertThat(mapper.isRetryable(clientError)).isFalse();
        }
    }

    // === EM-I04: User-Friendly Error on HMRC Failure ===

    @Nested
    @DisplayName("EM-I04: HMRC API Failure Error Messages")
    class HmrcApiFailureErrorMessages {

        @Test
        @DisplayName("should provide HMRC-specific message for API errors")
        void shouldProvideHmrcSpecificMessage() {
            // Given - HMRC API error
            String errorCode = "SERVER_ERROR";

            // When
            String userMessage = mapper.mapErrorCode(errorCode);

            // Then
            assertThat(userMessage).containsIgnoringCase("HMRC");
        }

        @Test
        @DisplayName("should guide user to reconnect for auth errors")
        void shouldGuideUserToReconnectForAuthErrors() {
            // Given - HMRC auth error
            String errorCode = "UNAUTHORISED";

            // When
            String userMessage = mapper.mapErrorCode(errorCode);

            // Then
            assertThat(userMessage)
                    .containsAnyOf("session", "reconnect", "expired");
        }

        @Test
        @DisplayName("should handle HMRC service unavailable")
        void shouldHandleHmrcServiceUnavailable() {
            // Given - HMRC service down
            String errorCode = "SERVICE_UNAVAILABLE";

            // When
            String userMessage = mapper.mapErrorCode(errorCode);

            // Then
            assertThat(userMessage)
                    .containsAnyOf("unavailable", "temporarily");
        }

        @Test
        @DisplayName("should handle rate limiting gracefully")
        void shouldHandleRateLimitingGracefully() {
            // Given - HTTP 429
            int httpStatus = 429;

            // When
            String userMessage = mapper.mapHttpStatus(httpStatus);
            boolean isRetryable = mapper.isRetryable(httpStatus);

            // Then
            assertThat(isRetryable).isTrue();
            assertThat(userMessage)
                    .containsAnyOf("wait", "moment", "requests");
        }

        @Test
        @DisplayName("should handle timeout errors")
        void shouldHandleTimeoutErrors() {
            // Given - HTTP 504
            int httpStatus = 504;

            // When
            String userMessage = mapper.mapHttpStatus(httpStatus);
            boolean isRetryable = mapper.isRetryable(httpStatus);

            // Then
            assertThat(isRetryable).isTrue();
            assertThat(userMessage)
                    .containsAnyOf("taking too long", "later", "response");
        }
    }

    // === Error Message Chain Integration ===

    @Nested
    @DisplayName("Error Message Chain Integration")
    class ErrorMessageChainIntegration {

        @Test
        @DisplayName("should handle error code then HTTP status fallback")
        void shouldHandleErrorCodeThenHttpStatusFallback() {
            // Given - unknown error code with known HTTP status
            String unknownCode = "UNKNOWN_ERROR";
            int httpStatus = 500;

            // When
            String codeMessage = mapper.mapErrorCode(unknownCode);
            String httpMessage = mapper.mapHttpStatus(httpStatus);

            // Then - both should provide useful messages
            assertThat(codeMessage).isNotBlank();
            assertThat(httpMessage).isNotBlank();

            // HTTP message is more specific for server errors
            assertThat(httpMessage).containsIgnoringCase("HMRC");
        }

        @Test
        @DisplayName("should provide consistent messaging across error types")
        void shouldProvideConsistentMessagingAcrossErrorTypes() {
            // Various error scenarios should all be user-friendly
            String[] errorCodes = {
                    "FORMAT_NINO",
                    "RULE_TAX_YEAR_NOT_SUPPORTED",
                    "CLIENT_OR_AGENT_NOT_AUTHORISED",
                    "MATCHING_RESOURCE_NOT_FOUND"
            };

            for (String code : errorCodes) {
                String message = mapper.mapErrorCode(code);

                assertThat(message)
                        .as("Message for %s should be user-friendly", code)
                        .isNotBlank()
                        .doesNotContain("java.")
                        .doesNotContain("Exception")
                        .doesNotContain(".java:");  // No stack trace file references
            }
        }

        @Test
        @DisplayName("should categorize errors for proper handling")
        void shouldCategorizeErrorsForProperHandling() {
            // Client errors - user needs to fix something
            assertThat(mapper.isClientError(400)).isTrue();
            assertThat(mapper.isClientError(401)).isTrue();
            assertThat(mapper.isClientError(403)).isTrue();
            assertThat(mapper.isClientError(404)).isTrue();

            // Server errors - system issue
            assertThat(mapper.isServerError(500)).isTrue();
            assertThat(mapper.isServerError(502)).isTrue();
            assertThat(mapper.isServerError(503)).isTrue();

            // Retryable - can try again
            assertThat(mapper.isRetryable(429)).isTrue();
            assertThat(mapper.isRetryable(500)).isTrue();
            assertThat(mapper.isRetryable(503)).isTrue();

            // Not retryable - user action needed
            assertThat(mapper.isRetryable(400)).isFalse();
            assertThat(mapper.isRetryable(401)).isFalse();
            assertThat(mapper.isRetryable(404)).isFalse();
        }
    }
}
