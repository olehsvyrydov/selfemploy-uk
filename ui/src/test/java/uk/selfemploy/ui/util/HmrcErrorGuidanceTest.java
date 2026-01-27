package uk.selfemploy.ui.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
}
