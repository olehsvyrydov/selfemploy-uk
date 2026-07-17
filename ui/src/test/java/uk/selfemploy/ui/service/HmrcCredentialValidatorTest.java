package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HmrcCredentialValidator")
class HmrcCredentialValidatorTest {

    @Nested
    @DisplayName("rejects unambiguous garbage")
    class Rejects {

        @Test
        @DisplayName("null is invalid")
        void nullIsInvalid() {
            assertThat(HmrcCredentialValidator.validateClientId(null).valid()).isFalse();
            assertThat(HmrcCredentialValidator.validateClientSecret(null).valid()).isFalse();
        }

        @Test
        @DisplayName("blank and whitespace-only are invalid")
        void blankIsInvalid() {
            assertThat(HmrcCredentialValidator.validateClientId("").valid()).isFalse();
            assertThat(HmrcCredentialValidator.validateClientId("   ").valid()).isFalse();
            assertThat(HmrcCredentialValidator.validateClientSecret("\t\n").valid()).isFalse();
        }

        @Test
        @DisplayName("a short placeholder like \"abc\" is rejected as too short")
        void shortGarbageIsInvalid() {
            assertThat(HmrcCredentialValidator.validateClientId("abc").valid()).isFalse();
            assertThat(HmrcCredentialValidator.validateClientSecret("abc").valid()).isFalse();
        }

        @Test
        @DisplayName("embedded whitespace is invalid — a real credential has none")
        void embeddedWhitespaceIsInvalid() {
            assertThat(HmrcCredentialValidator.validateClientId("abcd 1234 wxyz").valid()).isFalse();
            assertThat(HmrcCredentialValidator.validateClientSecret("secret value here").valid()).isFalse();
        }

        @Test
        @DisplayName("control characters are invalid")
        void controlCharsAreInvalid() {
            assertThat(HmrcCredentialValidator.validateClientId("abcdef\u0000ghij").valid()).isFalse();
            assertThat(HmrcCredentialValidator.validateClientId("abcdef\tghij").valid()).isFalse();
        }

        @Test
        @DisplayName("non-ASCII characters are invalid")
        void nonAsciiIsInvalid() {
            assertThat(HmrcCredentialValidator.validateClientId("abcdéfghij123").valid()).isFalse();
        }
    }

    @Nested
    @DisplayName("accepts plausible credentials")
    class Accepts {

        @Test
        @DisplayName("a typical HMRC-style client id is valid")
        void typicalClientIdIsValid() {
            assertThat(HmrcCredentialValidator.validateClientId("dK9fJ2mNpQ4rSt7uVwXy0zAbCdEf").valid()).isTrue();
        }

        @Test
        @DisplayName("a long high-entropy secret is valid")
        void typicalSecretIsValid() {
            assertThat(HmrcCredentialValidator.validateClientSecret(
                "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee").valid()).isTrue();
        }

        @Test
        @DisplayName("exactly the minimum length is accepted")
        void minimumLengthIsAccepted() {
            String eight = "abcd1234";
            assertThat(HmrcCredentialValidator.validateClientId(eight).valid()).isTrue();
        }

        @Test
        @DisplayName("the validator does not assume a fixed HMRC format — assorted lengths and symbols pass")
        void assortedRealisticShapesAreValid() {
            assertThat(HmrcCredentialValidator.validateClientId("0oXyZ12345abcdeFGHIJ").valid()).isTrue();
            assertThat(HmrcCredentialValidator.validateClientSecret(
                "1234567890abcdef1234567890ABCDEF!._-~").valid()).isTrue();
        }
    }

    @Nested
    @DisplayName("does not leak the value")
    class NoLeak {

        @Test
        @DisplayName("the rejection message names the rule and never contains the submitted value")
        void messageDoesNotEchoValue() {
            String secret = "hunter2withspace here";
            HmrcCredentialValidator.Result result = HmrcCredentialValidator.validateClientSecret(secret);

            assertThat(result.valid()).isFalse();
            assertThat(result.message()).isNotBlank();
            assertThat(result.message()).doesNotContain("hunter2");
            assertThat(result.message()).doesNotContain(secret);
        }

        @Test
        @DisplayName("a valid result carries no message to render")
        void validResultHasNoMessage() {
            assertThat(HmrcCredentialValidator.validateClientId("abcd1234efgh").message()).isNull();
        }
    }
}
