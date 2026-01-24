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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * An accessible button component with icon support.
 *
 * <p>TD-011: Component Library Basic</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Focus traversability for Tab key navigation</li>
 *   <li>Enter/Space key triggers action</li>
 *   <li>BUTTON accessible role for screen readers</li>
 *   <li>Optional leading icon</li>
 *   <li>CSS class support for custom styling</li>
 * </ul>
 *
 * <p>CSS classes:</p>
 * <ul>
 *   <li>.accessible-button - Base class</li>
 *   <li>.accessible-button:focused - Focus state</li>
 *   <li>.accessible-button-icon - Icon styling</li>
 *   <li>.accessible-button-text - Text styling</li>
 * </ul>
 */
public class AccessibleButton extends HBox {

    private static final String DEFAULT_STYLE_CLASS = "accessible-button";
    private static final String DEFAULT_ACCESSIBLE_HELP = "Press Enter or Space to activate";

    private final Label textLabel;
    private final Label iconLabel;

    private final ObjectProperty<EventHandler<ActionEvent>> onAction =
        new SimpleObjectProperty<>(this, "onAction");
    private final StringProperty text = new SimpleStringProperty(this, "text", "");
    private final StringProperty icon = new SimpleStringProperty(this, "icon", null);

    public AccessibleButton(String text) {
        this(text, null, null);
    }

    public AccessibleButton(String text, String icon) {
        this(text, icon, null);
    }

    public AccessibleButton(String text, String icon, EventHandler<ActionEvent> onAction) {
        super(8);

        iconLabel = new Label();
        iconLabel.getStyleClass().add("accessible-button-icon");
        iconLabel.setManaged(false);
        iconLabel.setVisible(false);

        textLabel = new Label();
        textLabel.getStyleClass().add("accessible-button-text");
        HBox.setHgrow(textLabel, Priority.NEVER);

        getChildren().addAll(iconLabel, textLabel);
        getStyleClass().add(DEFAULT_STYLE_CLASS);

        setAccessibleRole(AccessibleRole.BUTTON);
        setAccessibleHelp(DEFAULT_ACCESSIBLE_HELP);
        setFocusTraversable(true);
        setCursor(Cursor.HAND);

        this.text.addListener((obs, oldVal, newVal) -> {
            textLabel.setText(newVal);
            updateAccessibleText();
        });

        this.icon.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                iconLabel.setText(newVal);
                iconLabel.setManaged(true);
                iconLabel.setVisible(true);
            } else {
                iconLabel.setManaged(false);
                iconLabel.setVisible(false);
            }
        });

        setText(text);
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

    private void updateAccessibleText() {
        String currentText = getText();
        if (currentText != null && !currentText.isBlank()) {
            setAccessibleText(currentText);
        }
    }

    public void simulateClick() {
        if (!isDisabled()) {
            fireAction();
        }
    }

    // Property accessors
    public String getText() { return text.get(); }
    public void setText(String value) { text.set(value); }
    public StringProperty textProperty() { return text; }

    public String getIcon() { return icon.get(); }
    public void setIcon(String value) { icon.set(value); }
    public StringProperty iconProperty() { return icon; }

    public EventHandler<ActionEvent> getOnAction() { return onAction.get(); }
    public void setOnAction(EventHandler<ActionEvent> handler) { onAction.set(handler); }
    public ObjectProperty<EventHandler<ActionEvent>> onActionProperty() { return onAction; }

    public Label getTextLabel() { return textLabel; }
    public Label getIconLabel() { return iconLabel; }
}
