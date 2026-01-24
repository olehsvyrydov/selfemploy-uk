package uk.selfemploy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * An accessible hyperlink-style component.
 *
 * <p>TD-011: Component Library Basic</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Focus traversability for Tab key navigation</li>
 *   <li>Enter/Space key triggers action</li>
 *   <li>LINK accessible role for screen readers</li>
 *   <li>Underline on hover</li>
 *   <li>Hand cursor to indicate clickability</li>
 *   <li>Respects disabled state</li>
 * </ul>
 *
 * <p>CSS classes:</p>
 * <ul>
 *   <li>.accessible-link - Base class</li>
 *   <li>.accessible-link:focused - Focus state</li>
 *   <li>.accessible-link:hover - Hover state (underline)</li>
 * </ul>
 */
public class AccessibleLink extends Label {

    private static final String DEFAULT_STYLE_CLASS = "accessible-link";

    private final ObjectProperty<EventHandler<ActionEvent>> onAction =
        new SimpleObjectProperty<>(this, "onAction");
    private final StringProperty url = new SimpleStringProperty(this, "url", null);

    public AccessibleLink() {
        this("", null);
    }

    public AccessibleLink(String text) {
        this(text, null);
    }

    public AccessibleLink(String text, EventHandler<ActionEvent> onAction) {
        super(text);

        getStyleClass().add(DEFAULT_STYLE_CLASS);
        setAccessibleRole(AccessibleRole.HYPERLINK);
        setFocusTraversable(true);
        setCursor(Cursor.HAND);

        if (onAction != null) {
            setOnAction(onAction);
        }

        addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPress);
        addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClick);

        // Hover effect for underline
        setOnMouseEntered(e -> setUnderline(true));
        setOnMouseExited(e -> setUnderline(false));

        // Update accessible text when text changes
        textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                setAccessibleText(newVal);
            }
        });
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            if (!isDisabled()) {
                fireAction();
            }
            event.consume();
        }
    }

    private void handleMouseClick(MouseEvent event) {
        if (!isDisabled()) {
            fireAction();
            event.consume();
        }
    }

    private void fireAction() {
        EventHandler<ActionEvent> handler = getOnAction();
        if (handler != null) {
            handler.handle(new ActionEvent(this, this));
        }
    }

    public void simulateClick() {
        if (!isDisabled()) {
            fireAction();
        }
    }

    // Property accessors
    public EventHandler<ActionEvent> getOnAction() { return onAction.get(); }
    public void setOnAction(EventHandler<ActionEvent> handler) { onAction.set(handler); }
    public ObjectProperty<EventHandler<ActionEvent>> onActionProperty() { return onAction; }

    public String getUrl() { return url.get(); }
    public void setUrl(String value) { url.set(value); }
    public StringProperty urlProperty() { return url; }
}
