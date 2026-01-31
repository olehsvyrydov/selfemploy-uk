package uk.selfemploy.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A reusable info card component with icon, optional title, and content.
 * Sprint 12 - SE-12-003: Government Gateway Explainer Screen
 *
 * <p>Supports multiple content types:</p>
 * <ul>
 *   <li>Simple description text</li>
 *   <li>Bullet point lists</li>
 *   <li>Numbered step lists</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Simple description
 * InfoCard card = new InfoCard("LOCK", "Security", "Your data is protected.");
 *
 * // Bullet points
 * InfoCard card = new InfoCard("SHIELD_ALT", null, "Uses:",
 *     Arrays.asList("Filing taxes", "Viewing account"));
 *
 * // Numbered steps
 * InfoCard card = new InfoCard("INFO_CIRCLE", "Next Steps",
 *     Arrays.asList("Open browser", "Sign in"), true);
 * }</pre>
 */
public class InfoCard extends VBox {

    private final String iconType;
    private final String title;
    private final String description;
    private final List<String> bulletPoints;
    private final boolean numbered;

    /**
     * Creates an info card with a description.
     *
     * @param iconType    the icon type (e.g., "LOCK", "SHIELD_ALT", "INFO_CIRCLE")
     * @param title       optional title (can be null)
     * @param description the description text
     */
    public InfoCard(String iconType, String title, String description) {
        this.iconType = iconType;
        this.title = title;
        this.description = description;
        this.bulletPoints = Collections.emptyList();
        this.numbered = false;

        buildContent();
    }

    /**
     * Creates an info card with bullet points.
     *
     * @param iconType     the icon type
     * @param title        optional title (can be null)
     * @param description  optional intro text (can be null)
     * @param bulletPoints the list of bullet points
     */
    public InfoCard(String iconType, String title, String description, List<String> bulletPoints) {
        this.iconType = iconType;
        this.title = title;
        this.description = description;
        this.bulletPoints = bulletPoints != null ? new ArrayList<>(bulletPoints) : Collections.emptyList();
        this.numbered = false;

        buildContent();
    }

    /**
     * Creates an info card with numbered steps.
     *
     * @param iconType the icon type
     * @param title    the title
     * @param steps    the list of steps
     * @param numbered true for numbered steps
     */
    public InfoCard(String iconType, String title, List<String> steps, boolean numbered) {
        this.iconType = iconType;
        this.title = title;
        this.description = null;
        this.bulletPoints = steps != null ? new ArrayList<>(steps) : Collections.emptyList();
        this.numbered = numbered;

        buildContent();
    }

    /**
     * Builds the card content.
     */
    private void buildContent() {
        getStyleClass().add("info-card");
        setSpacing(8);
        setPadding(new Insets(12));
        setAccessibleRole(AccessibleRole.TEXT);

        // Content wrapper with icon and text
        HBox contentWrapper = new HBox(12);
        contentWrapper.setAlignment(Pos.TOP_LEFT);

        // Icon
        FontIcon icon = createIcon();
        if (icon != null) {
            VBox iconWrapper = new VBox(icon);
            iconWrapper.setAlignment(Pos.TOP_CENTER);
            iconWrapper.getStyleClass().add("info-card-icon-wrapper");
            contentWrapper.getChildren().add(iconWrapper);
        }

        // Text content
        VBox textContent = new VBox(6);
        textContent.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(textContent, Priority.ALWAYS);

        // Title
        if (title != null && !title.isEmpty()) {
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("info-card-title");
            titleLabel.setWrapText(true);
            textContent.getChildren().add(titleLabel);
        }

        // Description
        if (description != null && !description.isEmpty()) {
            Label descLabel = new Label(description);
            descLabel.getStyleClass().add("info-card-description");
            descLabel.setWrapText(true);
            textContent.getChildren().add(descLabel);
        }

        // Bullet points or numbered steps
        if (!bulletPoints.isEmpty()) {
            VBox listBox = createListBox();
            textContent.getChildren().add(listBox);
        }

        contentWrapper.getChildren().add(textContent);
        getChildren().add(contentWrapper);

        // Set accessible text for screen readers
        setAccessibleText(buildAccessibleText());
    }

    /**
     * Creates the icon from the icon type string.
     *
     * @return the FontIcon, or null if unknown
     */
    private FontIcon createIcon() {
        FontAwesomeSolid iconCode = switch (iconType) {
            case "LOCK" -> FontAwesomeSolid.LOCK;
            case "SHIELD_ALT" -> FontAwesomeSolid.SHIELD_ALT;
            case "INFO_CIRCLE" -> FontAwesomeSolid.INFO_CIRCLE;
            case "CHECK_CIRCLE" -> FontAwesomeSolid.CHECK_CIRCLE;
            case "EXCLAMATION_TRIANGLE" -> FontAwesomeSolid.EXCLAMATION_TRIANGLE;
            case "EXCLAMATION_CIRCLE" -> FontAwesomeSolid.EXCLAMATION_CIRCLE;
            default -> {
                // Return a default icon for unknown types
                yield FontAwesomeSolid.QUESTION_CIRCLE;
            }
        };

        FontIcon icon = FontIcon.of(iconCode, 18);
        icon.getStyleClass().add("info-card-icon");
        return icon;
    }

    /**
     * Creates the list box for bullet points or numbered steps.
     *
     * @return the VBox containing the list
     */
    private VBox createListBox() {
        VBox listBox = new VBox(6);
        listBox.getStyleClass().add("info-card-list");

        for (int i = 0; i < bulletPoints.size(); i++) {
            HBox itemRow = new HBox(8);
            itemRow.setAlignment(Pos.TOP_LEFT);

            Label prefixLabel;
            if (numbered) {
                prefixLabel = new Label((i + 1) + ".");
                prefixLabel.getStyleClass().add("info-card-number");
            } else {
                prefixLabel = new Label("\u2022"); // Bullet character
                prefixLabel.getStyleClass().add("info-card-bullet");
            }
            prefixLabel.setMinWidth(16);

            Label textLabel = new Label(bulletPoints.get(i));
            textLabel.getStyleClass().add("info-card-list-item");
            textLabel.setWrapText(true);
            HBox.setHgrow(textLabel, Priority.ALWAYS);

            itemRow.getChildren().addAll(prefixLabel, textLabel);
            listBox.getChildren().add(itemRow);
        }

        return listBox;
    }

    // === Getters ===

    /**
     * Returns the icon type.
     *
     * @return the icon type string
     */
    public String getIconType() {
        return iconType;
    }

    /**
     * Returns the title.
     *
     * @return the title, or null if not set
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the description.
     *
     * @return the description, or null if not set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the bullet points.
     *
     * @return unmodifiable list of bullet points
     */
    public List<String> getBulletPoints() {
        return Collections.unmodifiableList(bulletPoints);
    }

    /**
     * Checks if the card has an icon.
     *
     * @return true if an icon is displayed
     */
    public boolean hasIcon() {
        return iconType != null && !iconType.isEmpty();
    }

    /**
     * Checks if the card has a title.
     *
     * @return true if a title is displayed
     */
    public boolean hasTitle() {
        return title != null && !title.isEmpty();
    }

    /**
     * Checks if the card has bullet points.
     *
     * @return true if bullet points are displayed
     */
    public boolean hasBulletPoints() {
        return !bulletPoints.isEmpty();
    }

    /**
     * Checks if the list is numbered.
     *
     * @return true if numbered steps
     */
    public boolean isNumbered() {
        return numbered;
    }

    /**
     * Returns accessible description for screen readers.
     * Note: Named buildAccessibleText to avoid conflict with JavaFX Node.getAccessibleText()
     *
     * @return the accessible description
     */
    public String buildAccessibleText() {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            sb.append(title);
        }
        if (description != null && !description.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(". ");
            }
            sb.append(description);
        }
        if (!bulletPoints.isEmpty()) {
            for (String point : bulletPoints) {
                sb.append(". ").append(point);
            }
        }
        return sb.toString();
    }

    // === Factory Methods ===

    /**
     * Creates a security info card with a lock icon.
     *
     * @param title       the title
     * @param description the description
     * @return the security info card
     */
    public static InfoCard security(String title, String description) {
        return new InfoCard("LOCK", title, description);
    }

    /**
     * Creates an info card with bullet points.
     *
     * @param introText    the intro text
     * @param bulletPoints the bullet points
     * @return the bullet point info card
     */
    public static InfoCard infoBullets(String introText, List<String> bulletPoints) {
        return new InfoCard("SHIELD_ALT", null, introText, bulletPoints);
    }

    /**
     * Creates an info card with numbered steps.
     *
     * @param title the title
     * @param steps the steps
     * @return the numbered steps info card
     */
    public static InfoCard steps(String title, List<String> steps) {
        return new InfoCard("INFO_CIRCLE", title, steps, true);
    }
}
