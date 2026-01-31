package uk.selfemploy.ui.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.core.exception.SubmissionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HmrcErrorGuidance.
 *
 * SE-SH-006: Error Resolution Guidance
 *
 * Tests cover:
 * - Error code to help text mapping
 * - Default guidance for unknown error codes
 * - HMRC guidance URL
 * - Error code extraction from error messages
 */
@DisplayName("HmrcErrorGuidance Tests")
class HmrcErrorGuidanceTest {

    private HmrcErrorGuidance guidance;

    @BeforeEach
    void setUp() {
        guidance = new HmrcErrorGuidance();
    }

    @Nested
    @DisplayName("Error Code Mapping")
    class ErrorCodeMappingTests {

        @Test
        @DisplayName("should map INVALID_NINO to help text about National Insurance number")
        void shouldMapInvalidNino() {
            String helpText = guidance.getGuidanceForErrorCode("INVALID_NINO");

            assertThat(helpText).contains("National Insurance number");
            assertThat(helpText).containsIgnoringCase("correct");
        }

        @Test
        @DisplayName("should map INVALID_TAX_YEAR to help text about tax year format")
        void shouldMapInvalidTaxYear() {
            String helpText = guidance.getGuidanceForErrorCode("INVALID_TAX_YEAR");

            assertThat(helpText).contains("tax year");
            assertThat(helpText).contains("YYYY-YY");
        }

        @Test
        @DisplayName("should map DUPLICATE_SUBMISSION to help text about existing submission")
        void shouldMapDuplicateSubmission() {
            String helpText = guidance.getGuidanceForErrorCode("DUPLICATE_SUBMISSION");

            assertThat(helpText).containsIgnoringCase("already submitted");
        }

        @Test
        @DisplayName("should map CALCULATION_ERROR to help text about HMRC calculation")
        void shouldMapCalculationError() {
            String helpText = guidance.getGuidanceForErrorCode("CALCULATION_ERROR");

            assertThat(helpText).containsIgnoringCase("HMRC");
            assertThat(helpText).containsIgnoringCase("calculate");
            assertThat(helpText).containsIgnoringCase("figures");
        }

        @Test
        @DisplayName("should map BUSINESS_VALIDATION to help text about data mismatch")
        void shouldMapBusinessValidation() {
            String helpText = guidance.getGuidanceForErrorCode("BUSINESS_VALIDATION");

            assertThat(helpText).containsIgnoringCase("HMRC records");
        }

        @Test
        @DisplayName("should map FORMAT_NINO to help text about NI format")
        void shouldMapFormatNino() {
            String helpText = guidance.getGuidanceForErrorCode("FORMAT_NINO");

            assertThat(helpText).contains("National Insurance number");
        }

        @Test
        @DisplayName("should map RULE_TAX_YEAR_NOT_SUPPORTED to help text")
        void shouldMapRuleTaxYearNotSupported() {
            String helpText = guidance.getGuidanceForErrorCode("RULE_TAX_YEAR_NOT_SUPPORTED");

            assertThat(helpText).containsIgnoringCase("tax year");
        }

        @Test
        @DisplayName("should map RULE_ALREADY_EXISTS to help text about existing record")
        void shouldMapRuleAlreadyExists() {
            String helpText = guidance.getGuidanceForErrorCode("RULE_ALREADY_EXISTS");

            assertThat(helpText).containsIgnoringCase("already");
        }

        @Test
        @DisplayName("should return default guidance for unknown error codes")
        void shouldReturnDefaultForUnknownCode() {
            String helpText = guidance.getGuidanceForErrorCode("UNKNOWN_ERROR_CODE");

            assertThat(helpText).isNotNull();
            assertThat(helpText).isNotEmpty();
            assertThat(helpText).containsIgnoringCase("HMRC");
        }

        @Test
        @DisplayName("should return default guidance for null error code")
        void shouldReturnDefaultForNullCode() {
            String helpText = guidance.getGuidanceForErrorCode(null);

            assertThat(helpText).isNotNull();
            assertThat(helpText).isNotEmpty();
        }

        @Test
        @DisplayName("should return default guidance for empty error code")
        void shouldReturnDefaultForEmptyCode() {
            String helpText = guidance.getGuidanceForErrorCode("");

            assertThat(helpText).isNotNull();
            assertThat(helpText).isNotEmpty();
        }

        @Test
        @DisplayName("should handle error codes case-insensitively")
        void shouldHandleCaseInsensitively() {
            String lowerCase = guidance.getGuidanceForErrorCode("invalid_nino");
            String upperCase = guidance.getGuidanceForErrorCode("INVALID_NINO");

            assertThat(lowerCase).isEqualTo(upperCase);
        }
    }

    @Nested
    @DisplayName("Error Code Extraction")
    class ErrorCodeExtractionTests {

        @Test
        @DisplayName("should extract error code from message containing code")
        void shouldExtractErrorCode() {
            String message = "Validation failed: INVALID_NINO - The National Insurance number is not valid";

            String code = guidance.extractErrorCode(message);

            assertThat(code).isEqualTo("INVALID_NINO");
        }

        @Test
        @DisplayName("should extract HMRC-style error code")
        void shouldExtractHmrcStyleErrorCode() {
            String message = "Error: FORMAT_NINO detected in submission";

            String code = guidance.extractErrorCode(message);

            assertThat(code).isEqualTo("FORMAT_NINO");
        }

        @Test
        @DisplayName("should return null when no error code in message")
        void shouldReturnNullWhenNoCode() {
            String message = "Something went wrong with your submission";

            String code = guidance.extractErrorCode(message);

            assertThat(code).isNull();
        }

        @Test
        @DisplayName("should return null for null message")
        void shouldReturnNullForNullMessage() {
            String code = guidance.extractErrorCode(null);

            assertThat(code).isNull();
        }

        @Test
        @DisplayName("should return null for empty message")
        void shouldReturnNullForEmptyMessage() {
            String code = guidance.extractErrorCode("");

            assertThat(code).isNull();
        }
    }

    @Nested
    @DisplayName("HMRC Guidance URL")
    class GuidanceUrlTests {

        @Test
        @DisplayName("should return HMRC corrections guidance URL")
        void shouldReturnCorrectionsUrl() {
            String url = guidance.getGuidanceUrl();

            assertThat(url).isEqualTo("https://www.gov.uk/self-assessment-tax-returns/corrections");
        }

        @Test
        @DisplayName("should return valid HTTPS URL")
        void shouldReturnValidHttpsUrl() {
            String url = guidance.getGuidanceUrl();

            assertThat(url).startsWith("https://");
            assertThat(url).contains("gov.uk");
        }
    }

    @Nested
    @DisplayName("Guidance Text Formatting")
    class GuidanceFormattingTests {

        @Test
        @DisplayName("should return guidance with error code when available")
        void shouldReturnFormattedGuidance() {
            String formatted = guidance.getFormattedGuidance("INVALID_NINO", "Your NI number is invalid");

            assertThat(formatted).isNotNull();
            assertThat(formatted).isNotEmpty();
        }

        @Test
        @DisplayName("should include original error message when code unknown")
        void shouldIncludeOriginalMessageForUnknownCode() {
            String errorMessage = "Custom error from HMRC";
            String formatted = guidance.getFormattedGuidance("UNKNOWN_CODE", errorMessage);

            assertThat(formatted).isNotNull();
        }

        @Test
        @DisplayName("should work with null error message")
        void shouldWorkWithNullErrorMessage() {
            String formatted = guidance.getFormattedGuidance("INVALID_NINO", null);

            assertThat(formatted).isNotNull();
            assertThat(formatted).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Known Error Codes")
    class KnownErrorCodesTests {

        @Test
        @DisplayName("should identify known error codes")
        void shouldIdentifyKnownCodes() {
            assertThat(guidance.isKnownErrorCode("INVALID_NINO")).isTrue();
            assertThat(guidance.isKnownErrorCode("INVALID_TAX_YEAR")).isTrue();
            assertThat(guidance.isKnownErrorCode("DUPLICATE_SUBMISSION")).isTrue();
            assertThat(guidance.isKnownErrorCode("CALCULATION_ERROR")).isTrue();
            assertThat(guidance.isKnownErrorCode("BUSINESS_VALIDATION")).isTrue();
        }

        @Test
        @DisplayName("should identify unknown error codes")
        void shouldIdentifyUnknownCodes() {
            assertThat(guidance.isKnownErrorCode("RANDOM_ERROR")).isFalse();
            assertThat(guidance.isKnownErrorCode(null)).isFalse();
            assertThat(guidance.isKnownErrorCode("")).isFalse();
        }
    }

    // ==================== SE-10E-002: Pre-Validation and Auth Error Codes ====================

    @Nested
    @DisplayName("SE-10E-002: Pre-Validation Error Codes")
    class PreValidationErrorCodeTests {

        @Test
        @DisplayName("should map NINO_REQUIRED to guidance about Settings")
        void shouldMapNinoRequired() {
            String helpText = guidance.getGuidanceForErrorCode("NINO_REQUIRED");

            assertThat(helpText).containsIgnoringCase("National Insurance Number");
            assertThat(helpText).containsIgnoringCase("Settings");
        }

        @Test
        @DisplayName("should map BUSINESS_ID_REQUIRED to guidance")
        void shouldMapBusinessIdRequired() {
            String helpText = guidance.getGuidanceForErrorCode("BUSINESS_ID_REQUIRED");

            assertThat(helpText).containsIgnoringCase("business profile");
            assertThat(helpText).containsIgnoringCase("Settings");
            assertThat(helpText).doesNotContainIgnoringCase("reinstall");
        }

        @Test
        @DisplayName("should map DECLARATION_REQUIRED to guidance about checkboxes")
        void shouldMapDeclarationRequired() {
            String helpText = guidance.getGuidanceForErrorCode("DECLARATION_REQUIRED");

            assertThat(helpText).containsIgnoringCase("checkbox");
        }

        @Test
        @DisplayName("should map TOKEN_EXPIRED to guidance about reconnecting")
        void shouldMapTokenExpired() {
            String helpText = guidance.getGuidanceForErrorCode("TOKEN_EXPIRED");

            assertThat(helpText).containsIgnoringCase("expired");
            assertThat(helpText).containsIgnoringCase("Settings");
        }

        @Test
        @DisplayName("should map NOT_CONNECTED to guidance about connecting to HMRC")
        void shouldMapNotConnected() {
            String helpText = guidance.getGuidanceForErrorCode("NOT_CONNECTED");

            assertThat(helpText).containsIgnoringCase("not connected");
            assertThat(helpText).containsIgnoringCase("Settings");
        }

        @Test
        @DisplayName("should map AUTH_FAILED to guidance about reconnecting")
        void shouldMapAuthFailed() {
            String helpText = guidance.getGuidanceForErrorCode("AUTH_FAILED");

            assertThat(helpText).containsIgnoringCase("Government Gateway");
            assertThat(helpText).containsIgnoringCase("Settings");
        }

        @Test
        @DisplayName("TC-026: should extract NINO_REQUIRED from prefixed message")
        void shouldExtractNinoRequired() {
            String code = guidance.extractErrorCode("NINO_REQUIRED: Please set your National Insurance Number");

            assertThat(code).isEqualTo("NINO_REQUIRED");
        }

        @Test
        @DisplayName("should extract TOKEN_EXPIRED from message")
        void shouldExtractTokenExpired() {
            String code = guidance.extractErrorCode("TOKEN_EXPIRED: Your HMRC connection has expired");

            assertThat(code).isEqualTo("TOKEN_EXPIRED");
        }

        @Test
        @DisplayName("should extract NOT_CONNECTED from message")
        void shouldExtractNotConnected() {
            String code = guidance.extractErrorCode("NOT_CONNECTED: You are not connected to HMRC");

            assertThat(code).isEqualTo("NOT_CONNECTED");
        }
    }


        @Test
        @DisplayName("TC-001: MATCHING_RESOURCE_NOT_FOUND guidance directs to Settings")
        void shouldMapMatchingResourceNotFoundToNewGuidance() {
            String helpText = guidance.getGuidanceForErrorCode("MATCHING_RESOURCE_NOT_FOUND");

            assertThat(helpText).containsIgnoringCase("business profile");
            assertThat(helpText).containsIgnoringCase("Settings");
            assertThat(helpText).containsIgnoringCase("reconnect");
        }

        @Test
        @DisplayName("TC-004: HMRC_PROFILE_NOT_SYNCED guidance directs to Settings")
        void shouldMapHmrcProfileNotSyncedToGuidance() {
            String helpText = guidance.getGuidanceForErrorCode("HMRC_PROFILE_NOT_SYNCED");

            assertThat(helpText).containsIgnoringCase("business profile");
            assertThat(helpText).containsIgnoringCase("Settings");
            assertThat(helpText).containsIgnoringCase("reconnect");
        }

        @Test
        @DisplayName("TC-007: BUSINESS_ID_REQUIRED guidance no longer contains reinstall")
        void shouldNotContainReinstallInBusinessIdGuidance() {
            String helpText = guidance.getGuidanceForErrorCode("BUSINESS_ID_REQUIRED");

            assertThat(helpText).doesNotContainIgnoringCase("reinstall");
            assertThat(helpText).containsIgnoringCase("Settings");
        }

    // ==================== SE-10E-002: Error Titles ====================

    @Nested
    @DisplayName("SE-10E-002: Error Titles")
    class ErrorTitleTests {

        @Test
        @DisplayName("TC-020: should return specific title for NINO_REQUIRED")
        void shouldReturnNinoRequiredTitle() {
            assertThat(guidance.getTitle("NINO_REQUIRED"))
                .isEqualTo("National Insurance Number Not Set");
        }

        @Test
        @DisplayName("should return specific title for BUSINESS_ID_REQUIRED")
        void shouldReturnBusinessIdTitle() {
            assertThat(guidance.getTitle("BUSINESS_ID_REQUIRED"))
                .isEqualTo("HMRC Business Profile Not Synced");
        }

        @Test
        @DisplayName("should return specific title for DECLARATION_REQUIRED")
        void shouldReturnDeclarationTitle() {
            assertThat(guidance.getTitle("DECLARATION_REQUIRED"))
                .isEqualTo("Declaration Not Accepted");
        }

        @Test
        @DisplayName("should return specific title for TOKEN_EXPIRED")
        void shouldReturnTokenExpiredTitle() {
            assertThat(guidance.getTitle("TOKEN_EXPIRED"))
                .isEqualTo("HMRC Session Expired");
        }

        @Test
        @DisplayName("should return specific title for NOT_CONNECTED")
        void shouldReturnNotConnectedTitle() {
            assertThat(guidance.getTitle("NOT_CONNECTED"))
                .isEqualTo("Not Connected to HMRC");
        }

        @Test
        @DisplayName("should return specific title for AUTH_FAILED")
        void shouldReturnAuthFailedTitle() {
            assertThat(guidance.getTitle("AUTH_FAILED"))
                .isEqualTo("HMRC Authentication Failed");
        }

        @Test
        @DisplayName("should return specific title for INVALID_NINO")
        void shouldReturnInvalidNinoTitle() {
            assertThat(guidance.getTitle("INVALID_NINO"))
                .isEqualTo("Invalid National Insurance Number");
        }

        @Test
        @DisplayName("should return specific title for DUPLICATE_SUBMISSION")
        void shouldReturnDuplicateTitle() {
            assertThat(guidance.getTitle("DUPLICATE_SUBMISSION"))
                .isEqualTo("Already Submitted");
        }

        @Test
        @DisplayName("should return specific title for SERVER_ERROR")
        void shouldReturnServerErrorTitle() {
            assertThat(guidance.getTitle("SERVER_ERROR"))
                .isEqualTo("HMRC Service Unavailable");
        }

        @Test
        @DisplayName("TC-021: should return default title for unknown code")
        void shouldReturnDefaultTitleForUnknown() {
            assertThat(guidance.getTitle("RANDOM_CODE"))
                .isEqualTo("Submission Failed");
        }

        @Test
        @DisplayName("TC-022: should return default title for null")
        void shouldReturnDefaultTitleForNull() {
            assertThat(guidance.getTitle(null))
                .isEqualTo("Submission Failed");
        }

        @Test
        @DisplayName("should handle title lookup case-insensitively")
        void shouldHandleTitleCaseInsensitively() {
            assertThat(guidance.getTitle("nino_required"))
                .isEqualTo("National Insurance Number Not Set");
        }
    }


        @Test
        @DisplayName("TC-002: MATCHING_RESOURCE_NOT_FOUND has profile not synced title")
        void shouldReturnMatchingResourceNotFoundTitle() {
            assertThat(guidance.getTitle("MATCHING_RESOURCE_NOT_FOUND"))
                .isEqualTo("HMRC Business Profile Not Synced");
        }

        @Test
        @DisplayName("TC-005: HMRC_PROFILE_NOT_SYNCED has correct title")
        void shouldReturnHmrcProfileNotSyncedTitle() {
            assertThat(guidance.getTitle("HMRC_PROFILE_NOT_SYNCED"))
                .isEqualTo("HMRC Profile Not Synced");
        }

    // ==================== SE-10E-002: Settings Error Codes ====================

    @Nested
    @DisplayName("SE-10E-002: Settings Error Codes")
    class SettingsErrorCodeTests {

        @Test
        @DisplayName("TC-023: NINO_REQUIRED is a settings error")
        void shouldIdentifyNinoAsSettingsError() {
            assertThat(guidance.isSettingsError("NINO_REQUIRED")).isTrue();
        }

        @Test
        @DisplayName("TC-024: TOKEN_EXPIRED is a settings error")
        void shouldIdentifyTokenExpiredAsSettingsError() {
            assertThat(guidance.isSettingsError("TOKEN_EXPIRED")).isTrue();
        }

        @Test
        @DisplayName("should identify NOT_CONNECTED as settings error")
        void shouldIdentifyNotConnectedAsSettingsError() {
            assertThat(guidance.isSettingsError("NOT_CONNECTED")).isTrue();
        }

        @Test
        @DisplayName("should identify AUTH_FAILED as settings error")
        void shouldIdentifyAuthFailedAsSettingsError() {
            assertThat(guidance.isSettingsError("AUTH_FAILED")).isTrue();
        }

        @Test
        @DisplayName("should identify CLIENT_OR_AGENT_NOT_AUTHORISED as settings error")
        void shouldIdentifyClientNotAuthorisedAsSettingsError() {
            assertThat(guidance.isSettingsError("CLIENT_OR_AGENT_NOT_AUTHORISED")).isTrue();
        }

        @Test
        @DisplayName("should identify UNAUTHORISED as settings error")
        void shouldIdentifyUnauthorisedAsSettingsError() {
            assertThat(guidance.isSettingsError("UNAUTHORISED")).isTrue();
        }

        @Test
        @DisplayName("TC-025: INVALID_NINO is NOT a settings error")
        void shouldNotIdentifyInvalidNinoAsSettingsError() {
            assertThat(guidance.isSettingsError("INVALID_NINO")).isFalse();
        }

        @Test
        @DisplayName("DECLARATION_REQUIRED is NOT a settings error")
        void shouldNotIdentifyDeclarationAsSettingsError() {
            assertThat(guidance.isSettingsError("DECLARATION_REQUIRED")).isFalse();
        }


        @Test
        @DisplayName("TC-003: MATCHING_RESOURCE_NOT_FOUND is a settings error")
        void shouldIdentifyMatchingResourceNotFoundAsSettingsError() {
            assertThat(guidance.isSettingsError("MATCHING_RESOURCE_NOT_FOUND")).isTrue();
        }

        @Test
        @DisplayName("TC-006: HMRC_PROFILE_NOT_SYNCED is a settings error")
        void shouldIdentifyHmrcProfileNotSyncedAsSettingsError() {
            assertThat(guidance.isSettingsError("HMRC_PROFILE_NOT_SYNCED")).isTrue();
        }

        @Test
        @DisplayName("TC-008: BUSINESS_ID_REQUIRED is a settings error")
        void shouldIdentifyBusinessIdRequiredAsSettingsError() {
            assertThat(guidance.isSettingsError("BUSINESS_ID_REQUIRED")).isTrue();
        }

        @Test
        @DisplayName("null is NOT a settings error")
        void shouldNotIdentifyNullAsSettingsError() {
            assertThat(guidance.isSettingsError(null)).isFalse();
        }
    }

    // ==================== SE-10E-002: buildErrorDisplay ====================

    @Nested
    @DisplayName("SE-10E-002: buildErrorDisplay")
    class BuildErrorDisplayTests {

        @Test
        @DisplayName("TC-010: NINO_REQUIRED gives correct display")
        void shouldBuildDisplayForNinoRequired() {
            var ex = new SubmissionException("NINO_REQUIRED: National Insurance Number is not set");

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("National Insurance Number Not Set");
            assertThat(display.errorCode()).isEqualTo("NINO_REQUIRED");
            assertThat(display.settingsError()).isTrue();
            assertThat(display.retryable()).isFalse();
            assertThat(display.message()).isNotEmpty();
        }

        @Test
        @DisplayName("TC-011: BUSINESS_ID_REQUIRED gives correct display")
        void shouldBuildDisplayForBusinessIdRequired() {
            var ex = new SubmissionException("BUSINESS_ID_REQUIRED: Business ID is missing");

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("HMRC Business Profile Not Synced");
            assertThat(display.errorCode()).isEqualTo("BUSINESS_ID_REQUIRED");
            assertThat(display.settingsError()).isTrue();
            assertThat(display.retryable()).isFalse();
        }


        @Test
        @DisplayName("TC-009: MATCHING_RESOURCE_NOT_FOUND gives settingsError=true display")
        void shouldBuildDisplayForMatchingResourceNotFound() {
            var ex = new SubmissionException("MATCHING_RESOURCE_NOT_FOUND: Resource not found");

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("HMRC Business Profile Not Synced");
            assertThat(display.errorCode()).isEqualTo("MATCHING_RESOURCE_NOT_FOUND");
            assertThat(display.settingsError()).isTrue();
            assertThat(display.retryable()).isFalse();
        }

        @Test
        @DisplayName("TC-012: DECLARATION_REQUIRED gives correct display")
        void shouldBuildDisplayForDeclarationRequired() {
            var ex = new SubmissionException("DECLARATION_REQUIRED: Declaration not accepted");

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("Declaration Not Accepted");
            assertThat(display.errorCode()).isEqualTo("DECLARATION_REQUIRED");
            assertThat(display.settingsError()).isFalse();
            assertThat(display.retryable()).isFalse();
        }

        @Test
        @DisplayName("TC-013: TOKEN_EXPIRED gives correct display")
        void shouldBuildDisplayForTokenExpired() {
            var ex = new SubmissionException("TOKEN_EXPIRED: Your HMRC connection has expired");

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("HMRC Session Expired");
            assertThat(display.errorCode()).isEqualTo("TOKEN_EXPIRED");
            assertThat(display.settingsError()).isTrue();
            assertThat(display.retryable()).isFalse();
        }

        @Test
        @DisplayName("TC-014: retryable server error gives correct display")
        void shouldBuildDisplayForRetryableServerError() {
            var ex = new SubmissionException("SERVER_ERROR: HMRC is down", null, true);

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("HMRC Service Unavailable");
            assertThat(display.retryable()).isTrue();
            assertThat(display.settingsError()).isFalse();
        }

        @Test
        @DisplayName("TC-015: INVALID_NINO gives correct display")
        void shouldBuildDisplayForInvalidNino() {
            var ex = new SubmissionException("INVALID_NINO: NI number is invalid");

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("Invalid National Insurance Number");
            assertThat(display.settingsError()).isFalse();
        }

        @Test
        @DisplayName("TC-016: DUPLICATE_SUBMISSION gives correct display")
        void shouldBuildDisplayForDuplicateSubmission() {
            var ex = new SubmissionException("DUPLICATE_SUBMISSION: Already submitted");

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("Already Submitted");
            assertThat(display.settingsError()).isFalse();
        }

        @Test
        @DisplayName("TC-017: unknown error gives default display")
        void shouldBuildDisplayForUnknownError() {
            var ex = new RuntimeException("Something broke");

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("Submission Failed");
            assertThat(display.settingsError()).isFalse();
            assertThat(display.retryable()).isFalse();
        }

        @Test
        @DisplayName("TC-018: null exception gives safe default")
        void shouldBuildDisplayForNullException() {
            SubmissionErrorDisplay display = guidance.buildErrorDisplay(null);

            assertThat(display.title()).isEqualTo("Submission Failed");
            assertThat(display.errorCode()).isNull();
            assertThat(display.settingsError()).isFalse();
            assertThat(display.retryable()).isFalse();
            assertThat(display.message()).isNotEmpty();
        }

        @Test
        @DisplayName("TC-019: network timeout with retryable flag")
        void shouldBuildDisplayForRetryableNetworkError() {
            var ex = new SubmissionException("Connection timed out", new java.io.IOException(), true);

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.retryable()).isTrue();
        }
    }

    // ==================== BUG FIX: Duplicate message/guidance text ====================

    @Nested
    @DisplayName("Bug Fix: buildErrorDisplay should differentiate message from guidance")
    class MessageGuidanceDifferentiationTests {

        @Test
        @DisplayName("known error code should produce different message and guidance")
        void shouldProduceDifferentMessageAndGuidanceForKnownCode() {
            var ex = new SubmissionException("NINO_REQUIRED: National Insurance Number is not set");

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.message()).isNotEqualTo(display.guidance());
        }

        @Test
        @DisplayName("known error code message should contain exception text")
        void shouldSetMessageFromExceptionForKnownCode() {
            var ex = new SubmissionException("NINO_REQUIRED: National Insurance Number is not set");

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            // Message should be the original exception message (cleaned up)
            assertThat(display.message()).isNotNull();
            assertThat(display.message()).isNotEmpty();
        }

        @Test
        @DisplayName("known error code guidance should contain actionable steps")
        void shouldSetGuidanceFromLookupForKnownCode() {
            var ex = new SubmissionException("NINO_REQUIRED: National Insurance Number is not set");

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            // Guidance should be the looked-up actionable guidance
            assertThat(display.guidance()).containsIgnoringCase("Settings");
        }

        @Test
        @DisplayName("unknown error code should set message from exception")
        void shouldSetMessageFromExceptionForUnknownCode() {
            var ex = new RuntimeException("Something unexpected happened");

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.message()).contains("Something unexpected happened");
        }

        @Test
        @DisplayName("unknown error code should set guidance to default")
        void shouldSetGuidanceToDefaultForUnknownCode() {
            var ex = new RuntimeException("Something unexpected happened");

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.guidance()).containsIgnoringCase("review your submission");
        }

        @Test
        @DisplayName("null exception should have non-empty message and guidance")
        void shouldHandleNullExceptionGracefully() {
            SubmissionErrorDisplay display = guidance.buildErrorDisplay(null);

            assertThat(display.message()).isNotNull().isNotEmpty();
            assertThat(display.guidance()).isNotNull().isNotEmpty();
        }
    }

    // ==================== BUG FIX: Network errors show generic message ====================

    @Nested
    @DisplayName("Bug Fix: Network errors should show specific guidance")
    class NetworkErrorTests {

        @Test
        @DisplayName("SSLHandshakeException should produce Connection Error title")
        void shouldDetectSslHandshakeException() {
            var sslEx = new javax.net.ssl.SSLHandshakeException("SSL handshake failed");
            var ex = new SubmissionException("SSL handshake failed", sslEx, true);

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("Connection Error");
        }

        @Test
        @DisplayName("ConnectException should produce Connection Error title")
        void shouldDetectConnectException() {
            var connectEx = new java.net.ConnectException("Connection refused");
            var ex = new SubmissionException("Connection refused", connectEx, true);

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("Connection Error");
        }

        @Test
        @DisplayName("SocketTimeoutException should produce Connection Error title")
        void shouldDetectSocketTimeoutException() {
            var timeoutEx = new java.net.SocketTimeoutException("Read timed out");
            var ex = new SubmissionException("Read timed out", timeoutEx, true);

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("Connection Error");
        }

        @Test
        @DisplayName("UnknownHostException should produce Connection Error title")
        void shouldDetectUnknownHostException() {
            var hostEx = new java.net.UnknownHostException("api.service.hmrc.gov.uk");
            var ex = new SubmissionException("Unknown host", hostEx, true);

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("Connection Error");
        }

        @Test
        @DisplayName("network error should have network-specific message")
        void shouldShowNetworkSpecificMessage() {
            var connectEx = new java.net.ConnectException("Connection refused");
            var ex = new SubmissionException("Connection refused", connectEx, true);

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.message()).containsIgnoringCase("connect");
            assertThat(display.message()).containsIgnoringCase("HMRC");
        }

        @Test
        @DisplayName("network error should have network-specific guidance")
        void shouldShowNetworkSpecificGuidance() {
            var connectEx = new java.net.ConnectException("Connection refused");
            var ex = new SubmissionException("Connection refused", connectEx, true);

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.guidance()).containsIgnoringCase("internet connection");
        }

        @Test
        @DisplayName("network error should not be a settings error")
        void shouldNotBeSettingsError() {
            var connectEx = new java.net.ConnectException("Connection refused");
            var ex = new SubmissionException("Connection refused", connectEx, true);

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.settingsError()).isFalse();
        }

        @Test
        @DisplayName("exception message containing 'Network error' should be detected")
        void shouldDetectNetworkErrorInMessage() {
            var ex = new SubmissionException("Network error: could not reach server", null, true);

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("Connection Error");
        }

        @Test
        @DisplayName("exception message containing 'connection timeout' should be detected")
        void shouldDetectConnectionTimeoutInMessage() {
            var ex = new SubmissionException("connection timeout after 30s", null, true);

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("Connection Error");
        }

        @Test
        @DisplayName("network error with nested cause should be detected")
        void shouldDetectNestedNetworkCause() {
            var sslEx = new javax.net.ssl.SSLHandshakeException("cert invalid");
            var ioEx = new java.io.IOException("wrapped", sslEx);
            var ex = new SubmissionException("Request failed", ioEx, true);

            SubmissionErrorDisplay display = guidance.buildErrorDisplay(ex);

            assertThat(display.title()).isEqualTo("Connection Error");
        }
    }
}
