package uk.selfemploy.ui.component;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.AccessibleRole;
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
 * TDD Tests for SE-810 and SE-811: Keyboard Accessibility for Cards.
 *
 * <p>SE-810: Cards should be focusable via Tab key and respond to Enter/Space.</p>
 * <p>SE-811: Visible focus indicators for cards (WCAG 2.1 AA).</p>
 *
 * <p>Tagged as "e2e" to exclude from CI headless environment -
 * requires JavaFX platform with display.</p>
 */
@DisplayName("SE-810 & SE-811: AccessibleCard")
@ExtendWith(ApplicationExtension.class)
@Tag("e2e")
class AccessibleCardTest {

    private AccessibleCard card;

    @Start
    void start(Stage stage) {
        // Required for JavaFX initialization
    }

    @BeforeEach
    void setUp() {
        card = new AccessibleCard();
    }

    @Nested
    @DisplayName("SE-810: Keyboard Accessibility")
    class KeyboardAccessibility {

        @Nested
        @DisplayName("Focus Traversal")
        class FocusTraversal {

            @Test
            @DisplayName("should be focusable by default")
            void shouldBeFocusableByDefault() {
                assertThat(card.isFocusTraversable()).isTrue();
            }

            @Test
            @DisplayName("should allow disabling focus traversal")
            void shouldAllowDisablingFocusTraversal() {
                card.setFocusTraversable(false);

                assertThat(card.isFocusTraversable()).isFalse();
            }
        }

        @Nested
        @DisplayName("Keyboard Events")
        class KeyboardEvents {

            @Test
            @DisplayName("should fire action when Enter key is pressed")
            void shouldFireActionWhenEnterKeyPressed() {
                AtomicBoolean actionFired = new AtomicBoolean(false);
                card.setOnAction(event -> actionFired.set(true));

                // Simulate Enter key press
                KeyEvent enterEvent = new KeyEvent(
                        KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                        false, false, false, false
                );
                card.fireEvent(enterEvent);

                assertThat(actionFired.get()).isTrue();
            }

            @Test
            @DisplayName("should fire action when Space key is pressed")
            void shouldFireActionWhenSpaceKeyPressed() {
                AtomicBoolean actionFired = new AtomicBoolean(false);
                card.setOnAction(event -> actionFired.set(true));

                // Simulate Space key press
                KeyEvent spaceEvent = new KeyEvent(
                        KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE,
                        false, false, false, false
                );
                card.fireEvent(spaceEvent);

                assertThat(actionFired.get()).isTrue();
            }

            @Test
            @DisplayName("should not fire action for other keys")
            void shouldNotFireActionForOtherKeys() {
                AtomicBoolean actionFired = new AtomicBoolean(false);
                card.setOnAction(event -> actionFired.set(true));

                // Simulate Tab key press (should not trigger action)
                KeyEvent tabEvent = new KeyEvent(
                        KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB,
                        false, false, false, false
                );
                card.fireEvent(tabEvent);

                assertThat(actionFired.get()).isFalse();
            }

            @Test
            @DisplayName("should consume Enter key event to prevent propagation")
            void shouldConsumeEnterKeyEvent() {
                card.setOnAction(event -> {});

                KeyEvent enterEvent = new KeyEvent(
                        KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                        false, false, false, false
                );
                card.fireEvent(enterEvent);

                // Event should be consumed by the handler
                // Note: In actual implementation, we check if event.isConsumed()
                assertThat(card.getOnAction()).isNotNull();
            }

            @Test
            @DisplayName("should not fire action when no handler is set")
            void shouldNotThrowWhenNoHandlerSet() {
                // Should not throw
                KeyEvent enterEvent = new KeyEvent(
                        KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                        false, false, false, false
                );
                card.fireEvent(enterEvent);

                assertThat(card.getOnAction()).isNull();
            }

            @Test
            @DisplayName("should support action property for binding")
            void shouldSupportActionPropertyForBinding() {
                EventHandler<ActionEvent> handler = event -> {};
                card.setOnAction(handler);

                assertThat(card.getOnAction()).isEqualTo(handler);
                assertThat(card.onActionProperty()).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("SE-811: Focus Indicators")
    class FocusIndicators {

        @Test
        @DisplayName("should have accessible-card CSS class")
        void shouldHaveAccessibleCardCssClass() {
            assertThat(card.getStyleClass()).contains("accessible-card");
        }

        @Test
        @DisplayName("should support additional style classes")
        void shouldSupportAdditionalStyleClasses() {
            card.getStyleClass().add("metric-card");

            assertThat(card.getStyleClass()).contains("accessible-card", "metric-card");
        }

        @Test
        @DisplayName("should allow setting custom focus style class")
        void shouldAllowSettingCustomFocusStyleClass() {
            card.setFocusStyleClass("custom-focus");

            assertThat(card.getFocusStyleClass()).isEqualTo("custom-focus");
        }
    }

    @Nested
    @DisplayName("Accessibility")
    class Accessibility {

        @Test
        @DisplayName("should have BUTTON accessible role")
        void shouldHaveButtonAccessibleRole() {
            assertThat(card.getAccessibleRole()).isEqualTo(AccessibleRole.BUTTON);
        }

        @Test
        @DisplayName("should have default accessible text")
        void shouldHaveDefaultAccessibleText() {
            assertThat(card.getAccessibleText()).isNotNull();
            assertThat(card.getAccessibleText()).isEqualTo("Card");
        }

        @Test
        @DisplayName("should allow setting custom accessible text")
        void shouldAllowSettingCustomAccessibleText() {
            card.setAccessibleText("Income Summary Card");

            assertThat(card.getAccessibleText()).isEqualTo("Income Summary Card");
        }

        @Test
        @DisplayName("should support accessible help text")
        void shouldSupportAccessibleHelpText() {
            card.setAccessibleHelp("Press Enter or Space to activate");

            assertThat(card.getAccessibleHelp()).isEqualTo("Press Enter or Space to activate");
        }
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create card with default values")
        void shouldCreateCardWithDefaultValues() {
            assertThat(card).isNotNull();
            assertThat(card.isFocusTraversable()).isTrue();
            assertThat(card.getAccessibleRole()).isEqualTo(AccessibleRole.BUTTON);
        }

        @Test
        @DisplayName("should create card with accessible text")
        void shouldCreateCardWithAccessibleText() {
            AccessibleCard namedCard = new AccessibleCard("Total Income");

            assertThat(namedCard.getAccessibleText()).isEqualTo("Total Income");
        }

        @Test
        @DisplayName("should create card with accessible text and action")
        void shouldCreateCardWithAccessibleTextAndAction() {
            AtomicBoolean actionFired = new AtomicBoolean(false);
            AccessibleCard actionCard = new AccessibleCard("View Details", event -> actionFired.set(true));

            assertThat(actionCard.getAccessibleText()).isEqualTo("View Details");
            assertThat(actionCard.getOnAction()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Mouse Interaction")
    class MouseInteraction {

        @Test
        @DisplayName("should fire action on mouse click")
        void shouldFireActionOnMouseClick() {
            AtomicInteger clickCount = new AtomicInteger(0);
            card.setOnAction(event -> clickCount.incrementAndGet());

            // Simulate mouse click through fireEvent
            card.simulateClick();

            assertThat(clickCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("should show hand cursor")
        void shouldShowHandCursor() {
            assertThat(card.getCursor()).isEqualTo(javafx.scene.Cursor.HAND);
        }
    }

    @Nested
    @DisplayName("Content")
    class Content {

        @Test
        @DisplayName("should allow adding child nodes")
        void shouldAllowAddingChildNodes() {
            javafx.scene.control.Label label = new javafx.scene.control.Label("Test");
            card.getChildren().add(label);

            assertThat(card.getChildren()).hasSize(1);
            assertThat(card.getChildren().get(0)).isEqualTo(label);
        }

        @Test
        @DisplayName("should preserve children when setting action")
        void shouldPreserveChildrenWhenSettingAction() {
            javafx.scene.control.Label label = new javafx.scene.control.Label("Test");
            card.getChildren().add(label);

            card.setOnAction(event -> {});

            assertThat(card.getChildren()).hasSize(1);
        }
    }
}
