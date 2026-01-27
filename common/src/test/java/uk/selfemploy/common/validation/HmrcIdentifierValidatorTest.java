package uk.selfemploy.common.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for HMRC identifier validation (UTR and NINO).
 */
@DisplayName("HmrcIdentifierValidator Tests")
class HmrcIdentifierValidatorTest {

    @Nested
    @DisplayName("UTR Validation Tests")
    class UtrValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"1234567890", "9876543210", "0000000001", "1111111111"})
        @DisplayName("should accept valid 10-digit UTR")
        void shouldAcceptValidUtr(String validUtr) {
            assertThat(HmrcIdentifierValidator.isValidUtr(validUtr)).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should reject null or empty UTR")
        void shouldRejectNullOrEmptyUtr(String invalidUtr) {
            assertThat(HmrcIdentifierValidator.isValidUtr(invalidUtr)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"123456789", "12345678901", "ABCDEFGHIJ", "12-34-56-78", "123 456 7890"})
        @DisplayName("should reject invalid UTR format")
        void shouldRejectInvalidUtrFormat(String invalidUtr) {
            assertThat(HmrcIdentifierValidator.isValidUtr(invalidUtr)).isFalse();
        }

        @Test
        @DisplayName("should normalize UTR by removing spaces and hyphens")
        void shouldNormalizeUtr() {
            assertThat(HmrcIdentifierValidator.normalizeUtr("12 34 56 78 90")).isEqualTo("1234567890");
            assertThat(HmrcIdentifierValidator.normalizeUtr("12-34-56-78-90")).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("should return null when normalizing null UTR")
        void shouldReturnNullWhenNormalizingNull() {
            assertThat(HmrcIdentifierValidator.normalizeUtr(null)).isNull();
        }
    }

    @Nested
    @DisplayName("NINO Validation Tests")
    class NinoValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "AB123456A", "AB123456B", "AB123456C", "AB123456D",
            "CE123456A", "HJ123456B", "PR123456C", "TW123456D",
            "AA000000A", "WY999999D"  // WY is valid prefix (ZZ is not allowed)
        })
        @DisplayName("should accept valid NINO format")
        void shouldAcceptValidNino(String validNino) {
            assertThat(HmrcIdentifierValidator.isValidNino(validNino)).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should reject null or empty NINO")
        void shouldRejectNullOrEmptyNino(String invalidNino) {
            assertThat(HmrcIdentifierValidator.isValidNino(invalidNino)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "BG123456A",  // BG prefix not allowed
            "GB123456A",  // GB prefix not allowed
            "KN123456A",  // KN prefix not allowed
            "NK123456A",  // NK prefix not allowed
            "NT123456A",  // NT prefix not allowed
            "TN123456A",  // TN prefix not allowed
            "ZZ123456A"   // ZZ prefix not allowed
        })
        @DisplayName("should reject NINO with disallowed prefixes")
        void shouldRejectDisallowedPrefixes(String invalidNino) {
            assertThat(HmrcIdentifierValidator.isValidNino(invalidNino)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "AB123456E",  // Invalid suffix (must be A-D)
            "AB123456Z",  // Invalid suffix
            "AB1234567A", // Too many digits
            "AB12345A",   // Too few digits
            "12123456A",  // Digits in prefix
            "ABabcdefA",  // Letters in number section
            "AB 12 34 56 A"  // Spaces
        })
        @DisplayName("should reject NINO with invalid format")
        void shouldRejectInvalidNinoFormat(String invalidNino) {
            assertThat(HmrcIdentifierValidator.isValidNino(invalidNino)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "DA123456A",  // D in first position not allowed
            "FA123456A",  // F in first position not allowed
            "IA123456A",  // I in first position not allowed
            "QA123456A",  // Q in first position not allowed
            "UA123456A",  // U in first position not allowed
            "VA123456A"   // V in first position not allowed
        })
        @DisplayName("should reject NINO with invalid first letter")
        void shouldRejectInvalidFirstLetter(String invalidNino) {
            assertThat(HmrcIdentifierValidator.isValidNino(invalidNino)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "AD123456A",  // D in second position not allowed
            "AF123456A",  // F in second position not allowed
            "AI123456A",  // I in second position not allowed
            "AO123456A",  // O in second position not allowed
            "AQ123456A",  // Q in second position not allowed
            "AU123456A",  // U in second position not allowed
            "AV123456A"   // V in second position not allowed
        })
        @DisplayName("should reject NINO with invalid second letter")
        void shouldRejectInvalidSecondLetter(String invalidNino) {
            assertThat(HmrcIdentifierValidator.isValidNino(invalidNino)).isFalse();
        }

        @Test
        @DisplayName("should normalize NINO to uppercase without spaces")
        void shouldNormalizeNino() {
            assertThat(HmrcIdentifierValidator.normalizeNino("ab 12 34 56 a")).isEqualTo("AB123456A");
            assertThat(HmrcIdentifierValidator.normalizeNino("ab123456a")).isEqualTo("AB123456A");
            assertThat(HmrcIdentifierValidator.normalizeNino("AB 123456 A")).isEqualTo("AB123456A");
        }

        @Test
        @DisplayName("should return null when normalizing null NINO")
        void shouldReturnNullWhenNormalizingNull() {
            assertThat(HmrcIdentifierValidator.normalizeNino(null)).isNull();
        }

        @Test
        @DisplayName("should validate after normalization")
        void shouldValidateAfterNormalization() {
            String nino = HmrcIdentifierValidator.normalizeNino("ab 12 34 56 a");
            assertThat(HmrcIdentifierValidator.isValidNino(nino)).isTrue();
        }
    }

    @Nested
    @DisplayName("Validation Exception Tests")
    class ValidationExceptionTests {

        @Test
        @DisplayName("should throw exception for invalid UTR when validated strictly")
        void shouldThrowForInvalidUtr() {
            assertThatThrownBy(() -> HmrcIdentifierValidator.validateUtr("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UTR");
        }

        @Test
        @DisplayName("should throw exception for invalid NINO when validated strictly")
        void shouldThrowForInvalidNino() {
            assertThatThrownBy(() -> HmrcIdentifierValidator.validateNino("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NINO");
        }

        @Test
        @DisplayName("should not throw for valid UTR when validated strictly")
        void shouldNotThrowForValidUtr() {
            HmrcIdentifierValidator.validateUtr("1234567890"); // Should not throw
        }

        @Test
        @DisplayName("should not throw for valid NINO when validated strictly")
        void shouldNotThrowForValidNino() {
            HmrcIdentifierValidator.validateNino("AB123456A"); // Should not throw
        }
    }
}
