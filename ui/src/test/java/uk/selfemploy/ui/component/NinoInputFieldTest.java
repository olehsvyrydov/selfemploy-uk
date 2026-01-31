package uk.selfemploy.ui.component;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for NinoInputField component.
 * Sprint 12 - SE-12-002: NINO Entry with Validation Screen
 *
 * <p>Tagged as "e2e" to exclude from CI headless environment -
 * requires JavaFX platform with display.</p>
 */
@DisplayName("SE-12-002: NinoInputField")
@ExtendWith(ApplicationExtension.class)
@Tag("e2e")
class NinoInputFieldTest {

    private NinoInputField ninoInput;

    @Start
    void start(Stage stage) {
        // Required for JavaFX initialization
    }

    @BeforeEach
    void setUp() {
        ninoInput = new NinoInputField();
    }

    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {

        @Test
        @DisplayName("should be empty initially")
        void initialState_isEmpty() {
            assertThat(ninoInput.getNino()).isEmpty();
            assertThat(ninoInput.ninoProperty().get()).isEmpty();
        }

        @Test
        @DisplayName("should not be valid when empty")
        void initialState_isNotValid() {
            assertThat(ninoInput.isValid()).isFalse();
            assertThat(ninoInput.validProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should have placeholder text")
        void initialState_hasPlaceholder() {
            assertThat(ninoInput.getTextField().getPromptText())
                .isEqualTo("QQ 12 34 56 A");
        }

        @Test
        @DisplayName("should be focusable")
        void initialState_isFocusable() {
            assertThat(ninoInput.getTextField().isFocusTraversable()).isTrue();
        }

        @Test
        @DisplayName("should have correct CSS class")
        void initialState_hasCssClass() {
            assertThat(ninoInput.getStyleClass()).contains("nino-input-field");
        }
    }

    @Nested
    @DisplayName("Auto-Formatting Tests")
    class AutoFormattingTests {

        @Test
        @DisplayName("should auto-format as user types")
        void typing_autoFormatsWithSpaces() {
            // Simulate typing "QQ123456A"
            ninoInput.getTextField().setText("QQ123456A");
            WaitForAsyncUtils.waitForFxEvents();

            // Should be formatted as "QQ 12 34 56 A"
            assertThat(ninoInput.getTextField().getText()).isEqualTo("QQ 12 34 56 A");
        }

        @Test
        @DisplayName("should auto-format lowercase to uppercase")
        void typing_convertsToUppercase() {
            ninoInput.getTextField().setText("ab123456c");
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(ninoInput.getTextField().getText()).isEqualTo("AB 12 34 56 C");
        }

        @Test
        @DisplayName("should handle partial input correctly")
        void typing_handlesPartialInput() {
            ninoInput.getTextField().setText("QQ12");
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(ninoInput.getTextField().getText()).isEqualTo("QQ 12");
        }

        @Test
        @DisplayName("should not exceed max length")
        void typing_doesNotExceedMaxLength() {
            ninoInput.getTextField().setText("QQ123456ABCDEFG");
            WaitForAsyncUtils.waitForFxEvents();

            // Should only keep first 9 alphanumeric chars formatted
            String text = ninoInput.getTextField().getText();
            assertThat(text.replaceAll("\\s", "").length()).isLessThanOrEqualTo(9);
        }

        @Test
        @DisplayName("should strip non-alphanumeric characters")
        void typing_stripsInvalidCharacters() {
            ninoInput.getTextField().setText("QQ-12-34-56-A");
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(ninoInput.getTextField().getText()).isEqualTo("QQ 12 34 56 A");
        }
    }

    @Nested
    @DisplayName("Validation Visual Feedback Tests")
    class ValidationFeedbackTests {

        @Test
        @DisplayName("should show green checkmark for valid NINO")
        void validNino_showsGreenCheckmark() {
            ninoInput.getTextField().setText("AB123456A");
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(ninoInput.isValid()).isTrue();
            assertThat(ninoInput.getValidationIcon().getStyleClass()).contains("valid");
            assertThat(ninoInput.getValidationIcon().isVisible()).isTrue();
        }

        @Test
        @DisplayName("should show red X for invalid NINO")
        void invalidNino_showsRedX() {
            ninoInput.getTextField().setText("INVALID");
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(ninoInput.isValid()).isFalse();
            assertThat(ninoInput.getValidationIcon().getStyleClass()).contains("invalid");
            assertThat(ninoInput.getValidationIcon().isVisible()).isTrue();
        }

        @Test
        @DisplayName("should hide validation icon when empty")
        void emptyInput_hidesValidationIcon() {
            ninoInput.getTextField().setText("");
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(ninoInput.getValidationIcon().isVisible()).isFalse();
        }

        @Test
        @DisplayName("should update validation on text change")
        void validProperty_updatesOnChange() {
            // Start invalid
            assertThat(ninoInput.isValid()).isFalse();

            // Become valid
            ninoInput.getTextField().setText("AB123456A");
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(ninoInput.isValid()).isTrue();

            // Become invalid again
            ninoInput.getTextField().setText("INVALID");
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(ninoInput.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("NINO Property Tests")
    class NinoPropertyTests {

        @Test
        @DisplayName("should return clean NINO value without spaces")
        void ninoProperty_returnsCleanValue() {
            ninoInput.getTextField().setText("AB 12 34 56 A");
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(ninoInput.getNino()).isEqualTo("AB123456A");
        }

        @Test
        @DisplayName("should have bindable valid property")
        void validProperty_isBindable() {
            assertThat(ninoInput.validProperty()).isNotNull();

            boolean[] observed = {false};
            ninoInput.validProperty().addListener((obs, old, newVal) -> observed[0] = newVal);

            ninoInput.getTextField().setText("AB123456A");
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(observed[0]).isTrue();
        }

        @Test
        @DisplayName("should have bindable nino property")
        void ninoProperty_isBindable() {
            assertThat(ninoInput.ninoProperty()).isNotNull();

            String[] observed = {""};
            ninoInput.ninoProperty().addListener((obs, old, newVal) -> observed[0] = newVal);

            ninoInput.getTextField().setText("AB123456A");
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(observed[0]).isEqualTo("AB123456A");
        }
    }

    @Nested
    @DisplayName("NINO Validation Rules Tests")
    class NinoValidationRulesTests {

        @Test
        @DisplayName("should accept valid NINO with suffix A")
        void shouldAcceptValidNinoSuffixA() {
            ninoInput.getTextField().setText("AB123456A");
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(ninoInput.isValid()).isTrue();
        }

        @Test
        @DisplayName("should accept valid NINO with suffix D")
        void shouldAcceptValidNinoSuffixD() {
            ninoInput.getTextField().setText("CE123456D");
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(ninoInput.isValid()).isTrue();
        }

        @Test
        @DisplayName("should reject invalid suffix E")
        void shouldRejectInvalidSuffixE() {
            ninoInput.getTextField().setText("AB123456E");
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(ninoInput.isValid()).isFalse();
        }

        @Test
        @DisplayName("should reject disallowed prefix BG")
        void shouldRejectDisallowedPrefixBG() {
            ninoInput.getTextField().setText("BG123456A");
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(ninoInput.isValid()).isFalse();
        }

        @Test
        @DisplayName("should reject disallowed prefix GB")
        void shouldRejectDisallowedPrefixGB() {
            ninoInput.getTextField().setText("GB123456A");
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(ninoInput.isValid()).isFalse();
        }

        @Test
        @DisplayName("should reject D in first position")
        void shouldRejectDInFirstPosition() {
            ninoInput.getTextField().setText("DA123456A");
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(ninoInput.isValid()).isFalse();
        }

        @Test
        @DisplayName("should reject O in second position")
        void shouldRejectOInSecondPosition() {
            ninoInput.getTextField().setText("AO123456A");
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(ninoInput.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("Accessibility Tests")
    class AccessibilityTests {

        @Test
        @DisplayName("should have accessible text field")
        void shouldHaveAccessibleTextField() {
            assertThat(ninoInput.getTextField().getAccessibleText())
                .isNotNull();
        }

        @Test
        @DisplayName("should support keyboard input")
        void shouldSupportKeyboardInput() {
            // Component should be keyboard accessible
            assertThat(ninoInput.getTextField().isFocusTraversable()).isTrue();
        }
    }
}
