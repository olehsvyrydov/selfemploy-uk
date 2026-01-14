package uk.selfemploy.ui.component;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * A reusable help icon component that shows contextual help via tooltip.
 *
 * <p>SE-701: In-App Help System</p>
 *
 * <p>The HelpIcon displays a circular "?" icon that shows a tooltip
 * with help content when hovered or focused. It supports:</p>
 * <ul>
 *   <li>Title and body text</li>
 *   <li>Optional HMRC external link</li>
 *   <li>Keyboard accessibility (Tab + Enter)</li>
 *   <li>Property binding for dynamic content</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 * HelpIcon help = new HelpIcon("Tax Help", "Information about tax rates");
 * help.setLinkUrl("https://www.gov.uk/income-tax-rates");
 * help.setLinkText("View HMRC rates");
 * </pre>
 *
 * <p>CSS classes:</p>
 * <ul>
 *   <li>.help-icon-btn - The button container</li>
 *   <li>.help-icon-circle - The circular background</li>
 *   <li>.help-icon-text - The "?" character</li>
 * </ul>
 */
public class HelpIcon extends StackPane {

    private static final String DEFAULT_ACCESSIBLE_TEXT = "Help";
    private static final Duration TOOLTIP_SHOW_DELAY = Duration.millis(400);
    private static final double TOOLTIP_MAX_WIDTH = 300;

    // Properties
    private final StringProperty title = new SimpleStringProperty(this, "title", "");
    private final StringProperty content = new SimpleStringProperty(this, "content", "");
    private final StringProperty linkUrl = new SimpleStringProperty(this, "linkUrl", null);
    private final StringProperty linkText = new SimpleStringProperty(this, "linkText", null);

    // Components
    private final StackPane circle;
    private final Label questionMark;
    private final Tooltip tooltip;

    /**
     * Creates a HelpIcon with empty content.
     */
    public HelpIcon() {
        this("", "");
    }

    /**
     * Creates a HelpIcon with title and content.
     *
     * @param title   the help title
     * @param content the help body text
     */
    public HelpIcon(String title, String content) {
        this(title, content, null, null);
    }

    /**
     * Creates a HelpIcon with all properties.
     *
     * @param title    the help title
     * @param content  the help body text
     * @param linkUrl  optional HMRC link URL
     * @param linkText optional text for the link
     */
    public HelpIcon(String title, String content, String linkUrl, String linkText) {
        // Initialize properties
        this.title.set(title != null ? title : "");
        this.content.set(content != null ? content : "");
        this.linkUrl.set(linkUrl);
        this.linkText.set(linkText);

        // Create the visual structure
        questionMark = new Label("?");
        questionMark.getStyleClass().add("help-icon-text");

        circle = new StackPane(questionMark);
        circle.getStyleClass().add("help-icon-circle");

        getChildren().add(circle);

        // Style classes
        getStyleClass().add("help-icon-btn");

        // Accessibility
        setAccessibleRole(AccessibleRole.BUTTON);
        setAccessibleText(DEFAULT_ACCESSIBLE_TEXT);
        setFocusTraversable(true);

        // Create and configure tooltip
        tooltip = new Tooltip();
        tooltip.setShowDelay(TOOLTIP_SHOW_DELAY);
        tooltip.setMaxWidth(TOOLTIP_MAX_WIDTH);
        tooltip.setWrapText(true);
        tooltip.getStyleClass().add("custom-tooltip");
        updateTooltipContent();
        Tooltip.install(this, tooltip);

        // Listen for property changes to update tooltip
        this.title.addListener((obs, oldVal, newVal) -> updateTooltipContent());
        this.content.addListener((obs, oldVal, newVal) -> updateTooltipContent());
        this.linkUrl.addListener((obs, oldVal, newVal) -> updateTooltipContent());
        this.linkText.addListener((obs, oldVal, newVal) -> updateTooltipContent());

        // Update accessible text when title changes
        this.title.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                setAccessibleText("Help for " + newVal);
            } else {
                setAccessibleText(DEFAULT_ACCESSIBLE_TEXT);
            }
        });
    }

    // === Property Accessors ===

    /**
     * Gets the help title.
     *
     * @return the title
     */
    public String getTitle() {
        return title.get();
    }

    /**
     * Sets the help title.
     *
     * @param title the title
     */
    public void setTitle(String title) {
        this.title.set(title);
    }

    /**
     * Returns the title property for binding.
     *
     * @return the title property
     */
    public StringProperty titleProperty() {
        return title;
    }

    /**
     * Gets the help content/body text.
     *
     * @return the content
     */
    public String getContent() {
        return content.get();
    }

    /**
     * Sets the help content/body text.
     *
     * @param content the content
     */
    public void setContent(String content) {
        this.content.set(content);
    }

    /**
     * Returns the content property for binding.
     *
     * @return the content property
     */
    public StringProperty contentProperty() {
        return content;
    }

    /**
     * Gets the HMRC link URL.
     *
     * @return the link URL, or null if not set
     */
    public String getLinkUrl() {
        return linkUrl.get();
    }

    /**
     * Sets the HMRC link URL.
     *
     * @param url the link URL
     */
    public void setLinkUrl(String url) {
        this.linkUrl.set(url);
    }

    /**
     * Returns the linkUrl property for binding.
     *
     * @return the linkUrl property
     */
    public StringProperty linkUrlProperty() {
        return linkUrl;
    }

    /**
     * Gets the link display text.
     *
     * @return the link text, or null if not set
     */
    public String getLinkText() {
        return linkText.get();
    }

    /**
     * Sets the link display text.
     *
     * @param text the link text
     */
    public void setLinkText(String text) {
        this.linkText.set(text);
    }

    /**
     * Returns the linkText property for binding.
     *
     * @return the linkText property
     */
    public StringProperty linkTextProperty() {
        return linkText;
    }

    /**
     * Returns true if this help icon has a link URL set.
     *
     * @return true if link is present
     */
    public boolean hasLink() {
        String url = linkUrl.get();
        return url != null && !url.isBlank();
    }

    /**
     * Gets the tooltip associated with this help icon.
     *
     * @return the tooltip
     */
    public Tooltip getTooltip() {
        return tooltip;
    }

    // === Private Methods ===

    /**
     * Updates the tooltip content when properties change.
     */
    private void updateTooltipContent() {
        StringBuilder sb = new StringBuilder();

        String titleText = title.get();
        if (titleText != null && !titleText.isBlank()) {
            sb.append(titleText);
            sb.append("\n\n");
        }

        String contentText = content.get();
        if (contentText != null && !contentText.isBlank()) {
            sb.append(contentText);
        }

        String url = linkUrl.get();
        String text = linkText.get();
        if (url != null && !url.isBlank()) {
            sb.append("\n\n");
            if (text != null && !text.isBlank()) {
                sb.append(text);
            } else {
                sb.append("View HMRC guidance");
            }
        }

        tooltip.setText(sb.toString().trim());
    }
}
