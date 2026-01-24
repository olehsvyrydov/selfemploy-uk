package uk.selfemploy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

/**
 * An accessible icon-only button (circular).
 *
 * <p>TD-011: Component Library Basic</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Circular icon button design</li>
 *   <li>Focus traversability for Tab key navigation</li>
 *   <li>Enter/Space key triggers action</li>
 *   <li>BUTTON accessible role for screen readers</li>
 *   <li>Tooltip support for accessibility</li>
 * </ul>
 *
 * <p>CSS classes:</p>
 * <ul>
 *   <li>.accessible-icon-button - Base container</li>
 *   <li>.accessible-icon-button-circle - Circular background</li>
 *   <li>.accessible-icon-button-icon - Icon styling</li>
 * </ul>
 */
public class AccessibleIconButton extends StackPane {

    private static final String DEFAULT_STYLE_CLASS = "accessible-icon-button";

    private final StackPane circle;
    private final Label iconLabel;

    private final ObjectProperty<EventHandler<ActionEvent>> onAction =
        new SimpleObjectProperty<>(this, "onAction");
    private final StringProperty icon = new SimpleStringProperty(this, "icon", "");

    public AccessibleIconButton(String icon, String accessibleText) {
        this(icon, accessibleText, null);
    }

    public AccessibleIconButton(String icon, String accessibleText, EventHandler<ActionEvent> onAction) {
        super();

        iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("accessible-icon-button-icon");

        circle = new StackPane(iconLabel);
        circle.getStyleClass().add("accessible-icon-button-circle");
        circle.setAlignment(Pos.CENTER);

        getChildren().add(circle);
        getStyleClass().add(DEFAULT_STYLE_CLASS);

        setAccessibleRole(AccessibleRole.BUTTON);
        setAccessibleText(accessibleText);
        setAccessibleHelp("Press Enter or Space to activate");
        setFocusTraversable(true);
        setCursor(Cursor.HAND);

        this.icon.addListener((obs, oldVal, newVal) -> iconLabel.setText(newVal));
        setIcon(icon);

        if (onAction != null) {
            setOnAction(onAction);
        }

        addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPress);
        addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClick);
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            fireAction();
            event.consume();
        }
    }

    private void handleMouseClick(MouseEvent event) {
        fireAction();
        event.consume();
    }

    private void fireAction() {
        EventHandler<ActionEvent> handler = getOnAction();
        if (handler != null) {
            handler.handle(new ActionEvent(this, this));
        }
    }

    public void simulateClick() {
        fireAction();
    }

    // Property accessors
    public String getIcon() { return icon.get(); }
    public void setIcon(String value) { icon.set(value); }
    public StringProperty iconProperty() { return icon; }

    public EventHandler<ActionEvent> getOnAction() { return onAction.get(); }
    public void setOnAction(EventHandler<ActionEvent> handler) { onAction.set(handler); }
    public ObjectProperty<EventHandler<ActionEvent>> onActionProperty() { return onAction; }

    public StackPane getCircle() { return circle; }
    public Label getIconLabel() { return iconLabel; }
}
