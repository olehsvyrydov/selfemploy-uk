package uk.selfemploy.ui.component;

import javafx.event.ActionEvent;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AccessibleButton component.
 * TD-011: Component Library Basic
 *
 * <p>Tagged as "e2e" to exclude from CI headless environment -
 * requires JavaFX platform with display.</p>
 */
@DisplayName("TD-011: AccessibleButton")
@ExtendWith(ApplicationExtension.class)
@Tag("e2e")
class AccessibleButtonTest {

    private AccessibleButton button;

    @Start
    void start(Stage stage) {
        // Required for JavaFX initialization
    }

    @BeforeEach
    void setUp() {
        button = new AccessibleButton("Test Button");
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create button with text only")
        void shouldCreateButtonWithTextOnly() {
            AccessibleButton btn = new AccessibleButton("Click Me");
            assertThat(btn.getText()).isEqualTo("Click Me");
            assertThat(btn.getIcon()).isNull();
        }

        @Test
        @DisplayName("should create button with text and icon")
        void shouldCreateButtonWithTextAndIcon() {
            AccessibleButton btn = new AccessibleButton("Save", "[S]");
            assertThat(btn.getText()).isEqualTo("Save");
            assertThat(btn.getIcon()).isEqualTo("[S]");
        }

        @Test
        @DisplayName("should create button with text, icon and action")
        void shouldCreateButtonWithTextIconAndAction() {
            AtomicBoolean fired = new AtomicBoolean(false);
            AccessibleButton btn = new AccessibleButton("Save", "[S]", e -> fired.set(true));

            btn.simulateClick();

            assertThat(fired.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Keyboard Accessibility")
    class KeyboardAccessibility {

        @Test
        @DisplayName("should be focusable by default")
        void shouldBeFocusableByDefault() {
            assertThat(button.isFocusTraversable()).isTrue();
        }

        @Test
        @DisplayName("should fire action when Enter key is pressed")
        void shouldFireActionWhenEnterKeyPressed() {
            AtomicBoolean actionFired = new AtomicBoolean(false);
            button.setOnAction(event -> actionFired.set(true));

            KeyEvent enterEvent = new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                false, false, false, false
            );
            button.fireEvent(enterEvent);

            assertThat(actionFired.get()).isTrue();
        }

        @Test
        @DisplayName("should fire action when Space key is pressed")
        void shouldFireActionWhenSpaceKeyPressed() {
            AtomicBoolean actionFired = new AtomicBoolean(false);
            button.setOnAction(event -> actionFired.set(true));

            KeyEvent spaceEvent = new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE,
                false, false, false, false
            );
            button.fireEvent(spaceEvent);

            assertThat(actionFired.get()).isTrue();
        }

        @Test
        @DisplayName("should not fire action for other keys")
        void shouldNotFireActionForOtherKeys() {
            AtomicBoolean actionFired = new AtomicBoolean(false);
            button.setOnAction(event -> actionFired.set(true));

            KeyEvent tabEvent = new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB,
                false, false, false, false
            );
            button.fireEvent(tabEvent);

            assertThat(actionFired.get()).isFalse();
        }
    }

    @Nested
    @DisplayName("Screen Reader Accessibility")
    class ScreenReaderAccessibility {

        @Test
        @DisplayName("should have BUTTON accessible role")
        void shouldHaveButtonAccessibleRole() {
            assertThat(button.getAccessibleRole()).isEqualTo(AccessibleRole.BUTTON);
        }

        @Test
        @DisplayName("should have accessible text from button text")
        void shouldHaveAccessibleTextFromButtonText() {
            assertThat(button.getAccessibleText()).isEqualTo("Test Button");
        }

        @Test
        @DisplayName("should update accessible text when text changes")
        void shouldUpdateAccessibleTextWhenTextChanges() {
            button.setText("New Text");
            assertThat(button.getAccessibleText()).isEqualTo("New Text");
        }

        @Test
        @DisplayName("should have accessible help text")
        void shouldHaveAccessibleHelpText() {
            assertThat(button.getAccessibleHelp()).isEqualTo("Press Enter or Space to activate");
        }
    }

    @Nested
    @DisplayName("Properties")
    class Properties {

        @Test
        @DisplayName("should support text property binding")
        void shouldSupportTextPropertyBinding() {
            button.setText("New Text");
            assertThat(button.getText()).isEqualTo("New Text");
            assertThat(button.textProperty().get()).isEqualTo("New Text");
        }

        @Test
        @DisplayName("should update text label when text changes")
        void shouldUpdateTextLabelWhenTextChanges() {
            button.setText("Updated");
            assertThat(button.getTextLabel().getText()).isEqualTo("Updated");
        }

        @Test
        @DisplayName("should show icon when set")
        void shouldShowIconWhenSet() {
            button.setIcon("[+]");
            assertThat(button.getIcon()).isEqualTo("[+]");
            assertThat(button.getIconLabel().isVisible()).isTrue();
            assertThat(button.getIconLabel().isManaged()).isTrue();
        }

        @Test
        @DisplayName("should hide icon when null")
        void shouldHideIconWhenNull() {
            button.setIcon("[+]");
            button.setIcon(null);
            assertThat(button.getIconLabel().isVisible()).isFalse();
            assertThat(button.getIconLabel().isManaged()).isFalse();
        }

        @Test
        @DisplayName("should hide icon when blank")
        void shouldHideIconWhenBlank() {
            button.setIcon("[+]");
            button.setIcon("   ");
            assertThat(button.getIconLabel().isVisible()).isFalse();
        }
    }

    @Nested
    @DisplayName("Styling")
    class Styling {

        @Test
        @DisplayName("should have accessible-button CSS class")
        void shouldHaveAccessibleButtonCssClass() {
            assertThat(button.getStyleClass()).contains("accessible-button");
        }

        @Test
        @DisplayName("should show hand cursor")
        void shouldShowHandCursor() {
            assertThat(button.getCursor()).isEqualTo(Cursor.HAND);
        }

        @Test
        @DisplayName("icon label should have correct CSS class")
        void iconLabelShouldHaveCorrectCssClass() {
            assertThat(button.getIconLabel().getStyleClass()).contains("accessible-button-icon");
        }

        @Test
        @DisplayName("text label should have correct CSS class")
        void textLabelShouldHaveCorrectCssClass() {
            assertThat(button.getTextLabel().getStyleClass()).contains("accessible-button-text");
        }
    }

    @Nested
    @DisplayName("Action Handling")
    class ActionHandling {

        @Test
        @DisplayName("should fire action on simulateClick")
        void shouldFireActionOnSimulateClick() {
            AtomicInteger count = new AtomicInteger(0);
            button.setOnAction(e -> count.incrementAndGet());

            button.simulateClick();
            button.simulateClick();

            assertThat(count.get()).isEqualTo(2);
        }

        @Test
        @DisplayName("should not fire when disabled")
        void shouldNotFireWhenDisabled() {
            AtomicBoolean fired = new AtomicBoolean(false);
            button.setOnAction(e -> fired.set(true));
            button.setDisable(true);

            button.simulateClick();

            assertThat(fired.get()).isFalse();
        }

        @Test
        @DisplayName("should pass correct event source")
        void shouldPassCorrectEventSource() {
            AtomicBoolean correctSource = new AtomicBoolean(false);
            button.setOnAction(e -> correctSource.set(e.getSource() == button));

            button.simulateClick();

            assertThat(correctSource.get()).isTrue();
        }
    }
}
