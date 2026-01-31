package uk.selfemploy.ui.component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

/**
 * A reusable prerequisite checklist item component.
 * Sprint 12 - SE-12-001: Prerequisites Checklist Screen
 *
 * <p>Displays a prerequisite with icon, title, description, and external link.</p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Colored icon wrapper (teal, blue, violet variants)</li>
 *   <li>Title and description text</li>
 *   <li>Accessible external link</li>
 *   <li>Keyboard navigation support</li>
 * </ul>
 *
 * <h2>CSS Classes</h2>
 * <ul>
 *   <li>.prerequisite-item - Container</li>
 *   <li>.prerequisite-icon-wrapper - Icon background</li>
 *   <li>.prerequisite-icon-wrapper.teal/.blue/.violet - Color variants</li>
 *   <li>.prerequisite-title - Title label</li>
 *   <li>.prerequisite-description - Description text</li>
 *   <li>.prerequisite-link - External link</li>
 * </ul>
 */
public class PrerequisiteItem extends HBox {

    private static final String STYLE_CLASS = "prerequisite-item";
    private static final String ICON_WRAPPER_CLASS = "prerequisite-icon-wrapper";
    private static final String TITLE_CLASS = "prerequisite-title";
    private static final String DESC_CLASS = "prerequisite-description";
    private static final String LINK_CLASS = "prerequisite-link";

    private final String iconType;
    private final String colorVariant;
    private final String title;
    private final String description;
    private final String linkText;
    private final String linkUrl;

    private final StackPane iconWrapper;
    private final Label linkLabel;
    private final CheckBox checkBox;
    private final BooleanProperty checked = new SimpleBooleanProperty(false);
    private Consumer<String> onLinkClick;

    /**
     * Creates a new prerequisite item.
     *
     * @param iconType the FontAwesome icon name (e.g., "FILE_ALT")
     * @param colorVariant the color variant ("teal", "blue", or "violet")
     * @param title the prerequisite title
     * @param description the prerequisite description
     * @param linkText the text for the help link
     * @param linkUrl the URL the link points to
     */
    public PrerequisiteItem(String iconType, String colorVariant, String title,
                            String description, String linkText, String linkUrl) {
        super(12); // 12px gap between icon and text
        this.iconType = iconType;
        this.colorVariant = colorVariant;
        this.title = title;
        this.description = description;
        this.linkText = linkText;
        this.linkUrl = linkUrl;

        getStyleClass().add(STYLE_CLASS);
        setAlignment(Pos.TOP_LEFT);

        // Create icon wrapper
        iconWrapper = createIconWrapper();

        // Create text content
        VBox textContent = createTextContent();
        HBox.setHgrow(textContent, Priority.ALWAYS);

        // Create link
        linkLabel = createLink();

        // Create checkbox for user confirmation
        checkBox = createCheckBox();

        // Assemble: icon | text content | link | checkbox
        getChildren().addAll(iconWrapper, textContent, linkLabel, checkBox);

        // Set accessible text
        setAccessibleText(title + ". " + description + ". " + linkText + ". Click checkbox to confirm.");
    }

    /**
     * Creates the confirmation checkbox.
     */
    private CheckBox createCheckBox() {
        CheckBox cb = new CheckBox();
        cb.getStyleClass().add("prerequisite-checkbox");
        cb.setAccessibleText("Confirm " + title);
        cb.selectedProperty().bindBidirectional(checked);
        return cb;
    }

    /**
     * Creates the icon wrapper with colored background.
     */
    private StackPane createIconWrapper() {
        StackPane wrapper = new StackPane();
        wrapper.getStyleClass().addAll(ICON_WRAPPER_CLASS, colorVariant);
        wrapper.setMinSize(36, 36);
        wrapper.setMaxSize(36, 36);
        wrapper.setAlignment(Pos.CENTER);

        FontIcon icon = createIcon();
        wrapper.getChildren().add(icon);

        return wrapper;
    }

    /**
     * Creates the FontAwesome icon based on iconType.
     */
    private FontIcon createIcon() {
        FontAwesomeSolid iconEnum = switch (iconType) {
            case "FILE_ALT" -> FontAwesomeSolid.FILE_ALT;
            case "USER_SHIELD" -> FontAwesomeSolid.USER_SHIELD;
            case "ID_CARD" -> FontAwesomeSolid.ID_CARD;
            default -> FontAwesomeSolid.QUESTION_CIRCLE;
        };
        FontIcon icon = FontIcon.of(iconEnum, 16);
        icon.getStyleClass().add("prerequisite-icon");
        return icon;
    }

    /**
     * Creates the text content section with title and description.
     */
    private VBox createTextContent() {
        VBox content = new VBox(4);
        content.setAlignment(Pos.TOP_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(TITLE_CLASS);

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add(DESC_CLASS);
        descLabel.setWrapText(true);

        content.getChildren().addAll(titleLabel, descLabel);
        return content;
    }

    /**
     * Creates the external help link.
     */
    private Label createLink() {
        Label link = new Label(linkText + " >");
        link.getStyleClass().add(LINK_CLASS);
        link.setCursor(Cursor.HAND);
        link.setFocusTraversable(true);
        link.setAccessibleRole(AccessibleRole.HYPERLINK);
        link.setAccessibleText(linkText + ", opens external website");

        // Mouse click handler
        link.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            fireLinkClick();
            e.consume();
        });

        // Keyboard handler
        link.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                fireLinkClick();
                e.consume();
            }
        });

        // Hover underline effect
        link.setOnMouseEntered(e -> link.setUnderline(true));
        link.setOnMouseExited(e -> link.setUnderline(false));

        return link;
    }

    /**
     * Fires the link click handler if set.
     */
    private void fireLinkClick() {
        if (onLinkClick != null) {
            onLinkClick.accept(linkUrl);
        }
    }

    // === Public API ===

    /**
     * Returns the prerequisite title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the prerequisite description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the link text.
     */
    public String getLinkText() {
        return linkText;
    }

    /**
     * Returns the link URL.
     */
    public String getLinkUrl() {
        return linkUrl;
    }

    /**
     * Returns the icon type name.
     */
    public String getIconType() {
        return iconType;
    }

    /**
     * Returns the color variant.
     */
    public String getColorVariant() {
        return colorVariant;
    }

    /**
     * Returns the icon wrapper style classes.
     */
    public String getIconWrapperStyleClass() {
        return String.join(" ", iconWrapper.getStyleClass());
    }

    /**
     * Checks if the link is focusable.
     */
    public boolean isLinkFocusable() {
        return linkLabel.isFocusTraversable();
    }

    /**
     * Checks if this item has a link.
     */
    public boolean hasLink() {
        return linkUrl != null && !linkUrl.isEmpty();
    }

    /**
     * Sets the link click handler.
     *
     * @param handler consumer that receives the URL when link is clicked
     */
    public void setOnLinkClick(Consumer<String> handler) {
        this.onLinkClick = handler;
    }

    /**
     * Checks if a link click handler is set.
     */
    public boolean hasLinkClickHandler() {
        return onLinkClick != null;
    }

    // === Checkbox API ===

    /**
     * Returns whether this prerequisite is checked (confirmed by user).
     *
     * @return true if checked
     */
    public boolean isChecked() {
        return checked.get();
    }

    /**
     * Sets the checked state.
     *
     * @param value true to check, false to uncheck
     */
    public void setChecked(boolean value) {
        checked.set(value);
    }

    /**
     * Returns the checked property for binding.
     *
     * @return the checked property
     */
    public BooleanProperty checkedProperty() {
        return checked;
    }
}
