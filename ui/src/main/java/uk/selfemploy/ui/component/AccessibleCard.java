package uk.selfemploy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

/**
 * An accessible card component that supports keyboard navigation.
 *
 * <p>SE-810: Keyboard Accessibility for Cards</p>
 * <p>SE-811: Focus Indicators for Cards</p>
 *
 * <p>This component extends VBox and adds:</p>
 * <ul>
 *   <li>Focus traversability for Tab key navigation</li>
 *   <li>Enter/Space key handlers to trigger actions</li>
 *   <li>BUTTON accessible role for screen readers</li>
 *   <li>CSS class for focus indicator styling</li>
 *   <li>Hand cursor to indicate clickability</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 * AccessibleCard card = new AccessibleCard("View Income Details");
 * card.setOnAction(event -&gt; navigateToIncome());
 * card.getChildren().addAll(iconLabel, titleLabel, valueLabel);
 * </pre>
 *
 * <p>CSS classes:</p>
 * <ul>
 *   <li>.accessible-card - Base class for all accessible cards</li>
 *   <li>.accessible-card:focused - Focus state with visible focus ring</li>
 * </ul>
 *
 * <p>WAI-ARIA pattern: This component follows the Card widget pattern
 * where the card acts as a button that can be activated.</p>
 *
 * @see <a href="https://www.w3.org/WAI/WCAG21/quickref/#focus-visible">WCAG 2.1 Focus Visible</a>
 */
public class AccessibleCard extends VBox {

    private static final String DEFAULT_STYLE_CLASS = "accessible-card";
    private static final String DEFAULT_ACCESSIBLE_TEXT = "Card";
    private static final String DEFAULT_ACCESSIBLE_HELP = "Press Enter or Space to activate";

    // Action handler property
    private final ObjectProperty<EventHandler<ActionEvent>> onAction = new SimpleObjectProperty<>(this, "onAction");

    // Focus style class property
    private final StringProperty focusStyleClass = new SimpleStringProperty(this, "focusStyleClass", null);

    /**
     * Creates an AccessibleCard with default settings.
     */
    public AccessibleCard() {
        this(DEFAULT_ACCESSIBLE_TEXT);
    }

    /**
     * Creates an AccessibleCard with the specified accessible text.
     *
     * @param accessibleText the text for screen readers
     */
    public AccessibleCard(String accessibleText) {
        this(accessibleText, null);
    }

    /**
     * Creates an AccessibleCard with accessible text and action handler.
     *
     * @param accessibleText the text for screen readers
     * @param onAction       the action handler to call on activation
     */
    public AccessibleCard(String accessibleText, EventHandler<ActionEvent> onAction) {
        super();

        // Add base style class
        getStyleClass().add(DEFAULT_STYLE_CLASS);

        // Set up accessibility
        setAccessibleRole(AccessibleRole.BUTTON);
        setAccessibleText(accessibleText != null ? accessibleText : DEFAULT_ACCESSIBLE_TEXT);
        setAccessibleHelp(DEFAULT_ACCESSIBLE_HELP);

        // Enable focus traversal for Tab key navigation
        setFocusTraversable(true);

        // Set hand cursor to indicate clickability
        setCursor(Cursor.HAND);

        // Set action handler if provided
        if (onAction != null) {
            setOnAction(onAction);
        }

        // Set up keyboard event handler
        addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        // Set up mouse click handler
        addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClick);
    }

    /**
     * Handles keyboard events for Enter and Space keys.
     *
     * @param event the key event
     */
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            fireAction();
            event.consume();
        }
    }

    /**
     * Handles mouse click events.
     *
     * @param event the mouse event
     */
    private void handleMouseClick(MouseEvent event) {
        fireAction();
        event.consume();
    }

    /**
     * Fires the action event if a handler is set.
     */
    private void fireAction() {
        EventHandler<ActionEvent> handler = getOnAction();
        if (handler != null) {
            handler.handle(new ActionEvent(this, this));
        }
    }

    /**
     * Simulates a click on this card, firing the action event.
     * This is useful for testing.
     */
    public void simulateClick() {
        fireAction();
    }

    // === Property Accessors ===

    /**
     * Gets the action handler called when the card is activated.
     *
     * @return the action handler, or null if not set
     */
    public EventHandler<ActionEvent> getOnAction() {
        return onAction.get();
    }

    /**
     * Sets the action handler called when the card is activated.
     *
     * @param handler the action handler
     */
    public void setOnAction(EventHandler<ActionEvent> handler) {
        onAction.set(handler);
    }

    /**
     * Returns the onAction property for binding.
     *
     * @return the onAction property
     */
    public ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        return onAction;
    }

    /**
     * Gets the custom focus style class.
     *
     * @return the focus style class, or null if using default
     */
    public String getFocusStyleClass() {
        return focusStyleClass.get();
    }

    /**
     * Sets a custom focus style class to apply when focused.
     *
     * @param styleClass the focus style class
     */
    public void setFocusStyleClass(String styleClass) {
        focusStyleClass.set(styleClass);
    }

    /**
     * Returns the focusStyleClass property for binding.
     *
     * @return the focusStyleClass property
     */
    public StringProperty focusStyleClassProperty() {
        return focusStyleClass;
    }
}
