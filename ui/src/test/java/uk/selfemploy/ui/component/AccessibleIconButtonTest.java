package uk.selfemploy.ui.component;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AccessibleIconButton component.
 * TD-011: Component Library Basic
 *
 * <p>Tagged as "e2e" to exclude from CI headless environment -
 * requires JavaFX platform with display.</p>
 */
@DisplayName("TD-011: AccessibleIconButton")
@ExtendWith(ApplicationExtension.class)
@Tag("e2e")
class AccessibleIconButtonTest {

    private AccessibleIconButton iconButton;

    @Start
    void start(Stage stage) {
        // Required for JavaFX initialization
    }

    @BeforeEach
    void setUp() {
        iconButton = new AccessibleIconButton("[?]", "Help");
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create icon button with icon and accessible text")
        void shouldCreateIconButtonWithIconAndAccessibleText() {
            assertThat(iconButton.getIcon()).isEqualTo("[?]");
            assertThat(iconButton.getAccessibleText()).isEqualTo("Help");
        }

        @Test
        @DisplayName("should create icon button with action")
        void shouldCreateIconButtonWithAction() {
            AtomicBoolean fired = new AtomicBoolean(false);
            AccessibleIconButton btn = new AccessibleIconButton("[X]", "Close", e -> fired.set(true));

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
            assertThat(iconButton.isFocusTraversable()).isTrue();
        }

        @Test
        @DisplayName("should fire action when Enter key is pressed")
        void shouldFireActionWhenEnterKeyPressed() {
            AtomicBoolean actionFired = new AtomicBoolean(false);
            iconButton.setOnAction(event -> actionFired.set(true));

            KeyEvent enterEvent = new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                false, false, false, false
            );
            iconButton.fireEvent(enterEvent);

            assertThat(actionFired.get()).isTrue();
        }

        @Test
        @DisplayName("should fire action when Space key is pressed")
        void shouldFireActionWhenSpaceKeyPressed() {
            AtomicBoolean actionFired = new AtomicBoolean(false);
            iconButton.setOnAction(event -> actionFired.set(true));

            KeyEvent spaceEvent = new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE,
                false, false, false, false
            );
            iconButton.fireEvent(spaceEvent);

            assertThat(actionFired.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Screen Reader Accessibility")
    class ScreenReaderAccessibility {

        @Test
        @DisplayName("should have BUTTON accessible role")
        void shouldHaveButtonAccessibleRole() {
            assertThat(iconButton.getAccessibleRole()).isEqualTo(AccessibleRole.BUTTON);
        }

        @Test
        @DisplayName("should have accessible text")
        void shouldHaveAccessibleText() {
            assertThat(iconButton.getAccessibleText()).isEqualTo("Help");
        }

        @Test
        @DisplayName("should have accessible help text")
        void shouldHaveAccessibleHelpText() {
            assertThat(iconButton.getAccessibleHelp()).isEqualTo("Press Enter or Space to activate");
        }
    }

    @Nested
    @DisplayName("Properties")
    class Properties {

        @Test
        @DisplayName("should support icon property")
        void shouldSupportIconProperty() {
            iconButton.setIcon("[!]");
            assertThat(iconButton.getIcon()).isEqualTo("[!]");
        }

        @Test
        @DisplayName("should update icon label when icon changes")
        void shouldUpdateIconLabelWhenIconChanges() {
            iconButton.setIcon("[*]");
            assertThat(iconButton.getIconLabel().getText()).isEqualTo("[*]");
        }

        @Test
        @DisplayName("should support icon property binding")
        void shouldSupportIconPropertyBinding() {
            iconButton.setIcon("[+]");
            assertThat(iconButton.iconProperty().get()).isEqualTo("[+]");
        }
    }

    @Nested
    @DisplayName("Styling")
    class Styling {

        @Test
        @DisplayName("should have accessible-icon-button CSS class")
        void shouldHaveAccessibleIconButtonCssClass() {
            assertThat(iconButton.getStyleClass()).contains("accessible-icon-button");
        }

        @Test
        @DisplayName("should show hand cursor")
        void shouldShowHandCursor() {
            assertThat(iconButton.getCursor()).isEqualTo(Cursor.HAND);
        }

        @Test
        @DisplayName("circle should have correct CSS class")
        void circleShouldHaveCorrectCssClass() {
            assertThat(iconButton.getCircle().getStyleClass()).contains("accessible-icon-button-circle");
        }

        @Test
        @DisplayName("icon label should have correct CSS class")
        void iconLabelShouldHaveCorrectCssClass() {
            assertThat(iconButton.getIconLabel().getStyleClass()).contains("accessible-icon-button-icon");
        }
    }

    @Nested
    @DisplayName("Structure")
    class Structure {

        @Test
        @DisplayName("should have circle container")
        void shouldHaveCircleContainer() {
            assertThat(iconButton.getCircle()).isNotNull();
            assertThat(iconButton.getChildren()).contains(iconButton.getCircle());
        }

        @Test
        @DisplayName("should have icon label inside circle")
        void shouldHaveIconLabelInsideCircle() {
            assertThat(iconButton.getCircle().getChildren()).contains(iconButton.getIconLabel());
        }
    }

    @Nested
    @DisplayName("Action Handling")
    class ActionHandling {

        @Test
        @DisplayName("should fire action on simulateClick")
        void shouldFireActionOnSimulateClick() {
            AtomicBoolean fired = new AtomicBoolean(false);
            iconButton.setOnAction(e -> fired.set(true));

            iconButton.simulateClick();

            assertThat(fired.get()).isTrue();
        }

        @Test
        @DisplayName("action property should be bindable")
        void actionPropertyShouldBeBindable() {
            assertThat(iconButton.onActionProperty()).isNotNull();
        }
    }
}
