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
 * Unit tests for AccessibleLink component.
 * TD-011: Component Library Basic
 *
 * <p>Tagged as "e2e" to exclude from CI headless environment -
 * requires JavaFX platform with display.</p>
 */
@DisplayName("TD-011: AccessibleLink")
@ExtendWith(ApplicationExtension.class)
@Tag("e2e")
class AccessibleLinkTest {

    private AccessibleLink link;

    @Start
    void start(Stage stage) {
        // Required for JavaFX initialization
    }

    @BeforeEach
    void setUp() {
        link = new AccessibleLink("Learn more");
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create empty link")
        void shouldCreateEmptyLink() {
            AccessibleLink emptyLink = new AccessibleLink();
            assertThat(emptyLink.getText()).isEmpty();
        }

        @Test
        @DisplayName("should create link with text")
        void shouldCreateLinkWithText() {
            assertThat(link.getText()).isEqualTo("Learn more");
        }

        @Test
        @DisplayName("should create link with text and action")
        void shouldCreateLinkWithTextAndAction() {
            AtomicBoolean fired = new AtomicBoolean(false);
            AccessibleLink actionLink = new AccessibleLink("Click", e -> fired.set(true));

            actionLink.simulateClick();

            assertThat(fired.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Keyboard Accessibility")
    class KeyboardAccessibility {

        @Test
        @DisplayName("should be focusable by default")
        void shouldBeFocusableByDefault() {
            assertThat(link.isFocusTraversable()).isTrue();
        }

        @Test
        @DisplayName("should fire action when Enter key is pressed")
        void shouldFireActionWhenEnterKeyPressed() {
            AtomicBoolean actionFired = new AtomicBoolean(false);
            link.setOnAction(event -> actionFired.set(true));

            KeyEvent enterEvent = new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                false, false, false, false
            );
            link.fireEvent(enterEvent);

            assertThat(actionFired.get()).isTrue();
        }

        @Test
        @DisplayName("should fire action when Space key is pressed")
        void shouldFireActionWhenSpaceKeyPressed() {
            AtomicBoolean actionFired = new AtomicBoolean(false);
            link.setOnAction(event -> actionFired.set(true));

            KeyEvent spaceEvent = new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE,
                false, false, false, false
            );
            link.fireEvent(spaceEvent);

            assertThat(actionFired.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Screen Reader Accessibility")
    class ScreenReaderAccessibility {

        @Test
        @DisplayName("should have HYPERLINK accessible role")
        void shouldHaveHyperlinkAccessibleRole() {
            assertThat(link.getAccessibleRole()).isEqualTo(AccessibleRole.HYPERLINK);
        }

        @Test
        @DisplayName("should update accessible text when text changes")
        void shouldUpdateAccessibleTextWhenTextChanges() {
            link.setText("New Link Text");
            assertThat(link.getAccessibleText()).isEqualTo("New Link Text");
        }
    }

    @Nested
    @DisplayName("Properties")
    class Properties {

        @Test
        @DisplayName("should support url property")
        void shouldSupportUrlProperty() {
            link.setUrl("https://example.com");
            assertThat(link.getUrl()).isEqualTo("https://example.com");
        }

        @Test
        @DisplayName("should support url property binding")
        void shouldSupportUrlPropertyBinding() {
            link.setUrl("https://gov.uk");
            assertThat(link.urlProperty().get()).isEqualTo("https://gov.uk");
        }
    }

    @Nested
    @DisplayName("Styling")
    class Styling {

        @Test
        @DisplayName("should have accessible-link CSS class")
        void shouldHaveAccessibleLinkCssClass() {
            assertThat(link.getStyleClass()).contains("accessible-link");
        }

        @Test
        @DisplayName("should show hand cursor")
        void shouldShowHandCursor() {
            assertThat(link.getCursor()).isEqualTo(Cursor.HAND);
        }
    }

    @Nested
    @DisplayName("Action Handling")
    class ActionHandling {

        @Test
        @DisplayName("should fire action on simulateClick")
        void shouldFireActionOnSimulateClick() {
            AtomicBoolean fired = new AtomicBoolean(false);
            link.setOnAction(e -> fired.set(true));

            link.simulateClick();

            assertThat(fired.get()).isTrue();
        }

        @Test
        @DisplayName("action property should be bindable")
        void actionPropertyShouldBeBindable() {
            assertThat(link.onActionProperty()).isNotNull();
        }

        @Test
        @DisplayName("should not fire when disabled")
        void shouldNotFireWhenDisabled() {
            AtomicBoolean fired = new AtomicBoolean(false);
            link.setOnAction(e -> fired.set(true));
            link.setDisable(true);

            link.simulateClick();

            assertThat(fired.get()).isFalse();
        }
    }
}
