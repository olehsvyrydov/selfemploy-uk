package uk.selfemploy.core.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Extended Tests for ErrorMessageMapper (Sprint 10A).
 * Tests EM-U01 through EM-U08 from /rob's test design.
 *
 * <p>SE-10A-007: Error Messages Review Tests</p>
 *
 * <p>These tests extend the existing ErrorMessageMapperTest with additional
 * test cases focusing on user-friendly message formatting and security.</p>
 *
 * @see docs/sprints/sprint-10A/testing/rob-test-design-10A-10B.md
 */
@DisplayName("Error Message Mapper Extended Tests")
class ErrorMessageMapperExtendedTest {

    private ErrorMessageMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ErrorMessageMapper();
    }

    // === EM-U01: Should map validation error to user-friendly message ===

    @Nested
    @DisplayName("EM-U01: Validation Error Mapping")
    class ValidationErrorMapping {

        @Test
        @DisplayName("should map validation error to human-readable message")
        void shouldMapValidationErrorToHumanReadable() {
            // Given
            String errorCode = "FORMAT_VALUE";

            // When
            String userMessage = mapper.mapErrorCode(errorCode);

            // Then
            assertThat(userMessage)
                    .isNotBlank()
                    .doesNotContain("java.")
                    .doesNotContain("Exception")
                    .doesNotContain("null")
                    .contains("format"); // Should mention format issue
        }

        @Test
        @DisplayName("should not include stack trace in validation error")
        void shouldNotIncludeStackTrace() {
            // Given
            String errorCode = "RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED";

            // When
            String userMessage = mapper.mapErrorCode(errorCode);

            // Then
            assertThat(userMessage)
                    .doesNotContain("at ")
                    .doesNotContain(".java:")
                    .doesNotContain("stacktrace")
                    .doesNotContain("Caused by:");
        }

        @Test
        @DisplayName("should use plain language for validation errors")
        void shouldUsePlainLanguageForValidation() {
            // Given
            String errorCode = "RULE_BUSINESS_VALIDATION_FAILURE";

            // When
            String userMessage = mapper.mapErrorCode(errorCode);

            // Then
            assertThat(userMessage)
                    .isNotBlank()
                    .doesNotContain("RULE_")
                    .doesNotContain("_");  // No underscores from error codes
        }
    }

    // === EM-U02: Should map database error to generic message ===

    @Nested
    @DisplayName("EM-U02: Database Error Mapping")
    class DatabaseErrorMapping {

        @Test
        @DisplayName("should map 500 error to generic safe message")
        void shouldMap500ToGenericMessage() {
            // When
            String userMessage = mapper.mapHttpStatus(500);

            // Then
            assertThat(userMessage)
                    .isNotBlank()
                    .doesNotContain("SQL")
                    .doesNotContain("database")
                    .doesNotContain("connection")
                    .containsIgnoringCase("try again");
        }

        @Test
        @DisplayName("should not expose database internals")
        void shouldNotExposeDatabaseInternals() {
            // Given - simulate mapping for a database-related status
            String userMessage = mapper.mapHttpStatus(500);

            // Then
            assertThat(userMessage)
                    .doesNotContain("H2")
                    .doesNotContain("SQLite")
                    .doesNotContain("PostgreSQL")
                    .doesNotContain("MySQL")
                    .doesNotContain("table")
                    .doesNotContain("column")
                    .doesNotContain("constraint");
        }
    }

    // === EM-U03: Should map network error with retry hint ===

    @Nested
    @DisplayName("EM-U03: Network Error Mapping")
    class NetworkErrorMapping {

        @Test
        @DisplayName("should include retry hint for network timeout")
        void shouldIncludeRetryHintForTimeout() {
            // When
            String userMessage = mapper.mapHttpStatus(504); // Gateway Timeout

            // Then
            assertThat(userMessage)
                    .containsIgnoringCase("try again")
                    .containsAnyOf("later", "moment", "wait");
        }

        @Test
        @DisplayName("should mention connection for service unavailable")
        void shouldMentionConnectionForServiceUnavailable() {
            // When
            String userMessage = mapper.mapHttpStatus(503);

            // Then
            assertThat(userMessage)
                    .containsAnyOf("unavailable", "temporarily");
        }

        @Test
        @DisplayName("should mark 429 as retryable with rate limit hint")
        void shouldMark429AsRetryable() {
            // When
            String userMessage = mapper.mapHttpStatus(429);
            boolean isRetryable = mapper.isRetryable(429);

            // Then
            assertThat(isRetryable).isTrue();
            assertThat(userMessage).containsIgnoringCase("wait");
        }

        @ParameterizedTest
        @DisplayName("should mark server errors as retryable")
        @ValueSource(ints = {500, 502, 503, 504})
        void shouldMarkServerErrorsAsRetryable(int statusCode) {
            // When
            boolean isRetryable = mapper.isRetryable(statusCode);

            // Then
            assertThat(isRetryable)
                    .as("Status %d should be retryable", statusCode)
                    .isTrue();
        }
    }

    // === EM-U04: Should map HMRC error with specific guidance ===

    @Nested
    @DisplayName("EM-U04: HMRC Error Mapping")
    class HmrcErrorMapping {

        @Test
        @DisplayName("should provide HMRC-specific message for auth errors")
        void shouldProvideHmrcSpecificMessageForAuth() {
            // When
            String userMessage = mapper.mapErrorCode("UNAUTHORISED");

            // Then
            assertThat(userMessage)
                    .containsIgnoringCase("HMRC")
                    .containsIgnoringCase("session");
        }

        @Test
        @DisplayName("should suggest reconnection for authorization errors")
        void shouldSuggestReconnectionForAuthErrors() {
            // When
            String userMessage = mapper.mapErrorCode("CLIENT_OR_AGENT_NOT_AUTHORISED");

            // Then
            assertThat(userMessage).containsIgnoringCase("reconnect");
        }

        @Test
        @DisplayName("should mention HMRC in server error messages")
        void shouldMentionHmrcInServerErrors() {
            // When
            String userMessage = mapper.mapErrorCode("SERVER_ERROR");

            // Then
            assertThat(userMessage).containsIgnoringCase("HMRC");
        }

        @Test
        @DisplayName("should handle 403 Forbidden with clear guidance")
        void shouldHandle403WithClearGuidance() {
            // When
            String userMessage = mapper.mapHttpStatus(403);

            // Then
            assertThat(userMessage)
                    .containsIgnoringCase("permission")
                    .containsIgnoringCase("HMRC");
        }
    }

    // === EM-U05: Should map file not found with path ===

    @Nested
    @DisplayName("EM-U05: File Not Found Mapping")
    class FileNotFoundMapping {

        @Test
        @DisplayName("should suggest selecting another file for 404")
        void shouldSuggestSelectingAnotherFile() {
            // When
            String userMessage = mapper.mapHttpStatus(404);

            // Then
            assertThat(userMessage)
                    .containsIgnoringCase("not found")
                    .doesNotContain("java.io")
                    .doesNotContain("path/to");
        }
    }

    // === EM-U06: Should map permission denied with action ===

    @Nested
    @DisplayName("EM-U06: Permission Denied Mapping")
    class PermissionDeniedMapping {

        @Test
        @DisplayName("should provide action for permission error")
        void shouldProvideActionForPermissionError() {
            // When
            String userMessage = mapper.mapHttpStatus(403);

            // Then
            assertThat(userMessage)
                    .containsIgnoringCase("permission")
                    .containsAnyOf("check", "access");
        }
    }

    // === EM-U07: Should not expose internal details ===

    @Nested
    @DisplayName("EM-U07: No Internal Details Exposed")
    class NoInternalDetailsExposed {

        @ParameterizedTest
        @DisplayName("should not expose internal details for any error code")
        @MethodSource("errorCodeProvider")
        void shouldNotExposeInternalDetails(String errorCode) {
            // When
            String userMessage = mapper.mapErrorCode(errorCode);

            // Then
            assertThat(userMessage)
                    .doesNotContain("java.")
                    .doesNotContain("javax.")
                    .doesNotContain("jakarta.")
                    .doesNotContain(".class")
                    .doesNotContain("NullPointerException")
                    .doesNotContain("ClassNotFoundException")
                    .doesNotContain("src/main")
                    .doesNotContain("target/")
                    .doesNotContain("SQLException")
                    .doesNotContain("IOException")
                    .doesNotContainPattern("\\d+\\.\\d+\\.\\d+\\.\\d+"); // No IP addresses
        }

        static Stream<String> errorCodeProvider() {
            return Stream.of(
                    "RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED",
                    "CLIENT_OR_AGENT_NOT_AUTHORISED",
                    "FORMAT_NINO",
                    "SERVER_ERROR",
                    "UNKNOWN_ERROR"
            );
        }

        @ParameterizedTest
        @DisplayName("should not expose internal details for any HTTP status")
        @ValueSource(ints = {400, 401, 403, 404, 429, 500, 502, 503, 504, 418})
        void shouldNotExposeInternalDetailsForHttpStatus(int status) {
            // When
            String userMessage = mapper.mapHttpStatus(status);

            // Then
            assertThat(userMessage)
                    .doesNotContain("java.")
                    .doesNotContain(".class")
                    .doesNotContain("Exception")
                    .doesNotContain("stacktrace");
        }

        @Test
        @DisplayName("should not expose SQL queries")
        void shouldNotExposeSqlQueries() {
            // When
            String userMessage = mapper.mapHttpStatus(500);

            // Then
            assertThat(userMessage)
                    .doesNotContainIgnoringCase("SELECT")
                    .doesNotContainIgnoringCase("INSERT")
                    .doesNotContainIgnoringCase("UPDATE")
                    .doesNotContainIgnoringCase("DELETE")
                    .doesNotContainIgnoringCase("FROM")
                    .doesNotContainIgnoringCase("WHERE");
        }

        @Test
        @DisplayName("should not expose file paths")
        void shouldNotExposeFilePaths() {
            // When
            String userMessage = mapper.mapHttpStatus(500);

            // Then
            assertThat(userMessage)
                    .doesNotContain("/home/")
                    .doesNotContain("C:\\")
                    .doesNotContain("/Users/")
                    .doesNotContain("file://")
                    .doesNotContain("/var/");
        }
    }

    // === EM-U08: Should include suggested action ===

    @Nested
    @DisplayName("EM-U08: Suggested Actions Included")
    class SuggestedActionsIncluded {

        @Test
        @DisplayName("should include action for 400 Bad Request")
        void shouldIncludeActionFor400() {
            // When
            String userMessage = mapper.mapHttpStatus(400);

            // Then
            assertThat(userMessage)
                    .containsAnyOf("check", "try again", "Please");
        }

        @Test
        @DisplayName("should include action for 401 Unauthorized")
        void shouldIncludeActionFor401() {
            // When
            String userMessage = mapper.mapHttpStatus(401);

            // Then
            assertThat(userMessage)
                    .containsAnyOf("reconnect", "login", "Please");
        }

        @Test
        @DisplayName("should include action for server errors")
        void shouldIncludeActionForServerErrors() {
            // When
            String userMessage = mapper.mapHttpStatus(500);

            // Then
            assertThat(userMessage)
                    .containsAnyOf("try again", "later", "Please");
        }

        @Test
        @DisplayName("should include action for rate limit errors")
        void shouldIncludeActionForRateLimit() {
            // When
            String userMessage = mapper.mapHttpStatus(429);

            // Then
            assertThat(userMessage)
                    .containsAnyOf("wait", "moment", "Please");
        }

        @ParameterizedTest
        @DisplayName("should have actionable guidance for retryable errors")
        @ValueSource(ints = {400, 401, 403, 429, 500, 502, 503, 504})
        void shouldHaveActionableGuidanceForRetryableErrors(int status) {
            // When
            String userMessage = mapper.mapHttpStatus(status);

            // Then - these statuses should have actionable guidance
            assertThat(userMessage)
                    .as("Message for status %d should contain actionable guidance", status)
                    .matches(".*(?i)(please|try|check|wait|contact|reconnect|access).*");
        }

        @Test
        @DisplayName("should have descriptive message for 404")
        void shouldHaveDescriptiveMessageFor404() {
            // 404 might not have action since resource doesn't exist
            String userMessage = mapper.mapHttpStatus(404);

            assertThat(userMessage)
                    .containsIgnoringCase("not found");
        }
    }

    // === Error Category Classification ===

    @Nested
    @DisplayName("Error Category Classification")
    class ErrorCategoryClassification {

        @ParameterizedTest
        @DisplayName("should correctly classify client errors (4xx)")
        @ValueSource(ints = {400, 401, 403, 404, 429, 499})
        void shouldClassifyClientErrors(int status) {
            assertThat(mapper.isClientError(status)).isTrue();
            assertThat(mapper.isServerError(status)).isFalse();
        }

        @ParameterizedTest
        @DisplayName("should correctly classify server errors (5xx)")
        @ValueSource(ints = {500, 501, 502, 503, 504, 599})
        void shouldClassifyServerErrors(int status) {
            assertThat(mapper.isServerError(status)).isTrue();
            assertThat(mapper.isClientError(status)).isFalse();
        }

        @ParameterizedTest
        @DisplayName("should not classify success codes as errors")
        @ValueSource(ints = {200, 201, 204, 301, 302})
        void shouldNotClassifySuccessAsError(int status) {
            assertThat(mapper.isClientError(status)).isFalse();
            assertThat(mapper.isServerError(status)).isFalse();
        }
    }
}
