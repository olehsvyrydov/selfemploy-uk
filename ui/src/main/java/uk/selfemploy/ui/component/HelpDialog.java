package uk.selfemploy.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import uk.selfemploy.ui.help.HelpContent;
import uk.selfemploy.ui.help.HelpService;
import uk.selfemploy.ui.util.DialogStyler;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * A custom styled dialog for displaying help content.
 *
 * <p>Refactored to use Stage with StageStyle.TRANSPARENT and DialogStyler
 * for consistent popup styling with rounded corners and shadows - matching
 * the NotificationDialog pattern.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Gradient header with category-colored icon</li>
 *   <li>Proper typography for title and body</li>
 *   <li>Optional "Learn More" link to HMRC guidance</li>
 *   <li>Styled close button</li>
 *   <li>Large mode for User Guide (1200x900)</li>
 *   <li>Rounded corners and drop shadow (via DialogStyler)</li>
 * </ul>
 *
 * <p>Design: /aura (Senior UI/UX Design Architect)</p>
 * <p>Implementation: /james</p>
 */
public class HelpDialog {

    private static final Logger LOG = Logger.getLogger(HelpDialog.class.getName());
    private static final String STYLESHEET = "/css/help.css";

    /**
     * Dialog size options for different content types.
     */
    public enum DialogSize {
        /** Standard small popup (380px width) for brief help topics */
        STANDARD,
        /** Medium dialog (600x500) for User Guide section topics */
        MEDIUM,
        /** Large dialog (900x700) for comprehensive User Guide */
        LARGE
    }

    // Large dialog dimensions for User Guide
    private static final double LARGE_WIDTH = 900;
    private static final double LARGE_HEIGHT = 700;

    // Medium dialog dimensions for User Guide section topics
    private static final double MEDIUM_WIDTH = 600;
    private static final double MEDIUM_HEIGHT = 500;

    // Standard dialog dimensions (same as NotificationDialog)
    private static final double STANDARD_WIDTH = 420;

    private final HelpContent content;
    private final String categoryColor;
    private final Ikon icon;
    private final HelpService helpService;
    private final DialogSize dialogSize;
    private final Stage stage;

    /**
     * Creates a new HelpDialog with standard size.
     *
     * @param content the help content to display
     * @param icon the FontAwesome icon for this topic
     * @param categoryColor the hex color for this category
     * @param helpService the help service for opening HMRC links
     */
    public HelpDialog(HelpContent content, Ikon icon, String categoryColor, HelpService helpService) {
        this(content, icon, categoryColor, helpService, DialogSize.STANDARD);
    }

    /**
     * Creates a new HelpDialog with optional large mode (for backwards compatibility).
     *
     * @param content the help content to display
     * @param icon the FontAwesome icon for this topic
     * @param categoryColor the hex color for this category
     * @param helpService the help service for opening HMRC links
     * @param largeMode if true, uses large size for comprehensive guides
     */
    public HelpDialog(HelpContent content, Ikon icon, String categoryColor, HelpService helpService, boolean largeMode) {
        this(content, icon, categoryColor, helpService, largeMode ? DialogSize.LARGE : DialogSize.STANDARD);
    }

    /**
     * Creates a new HelpDialog with specified size.
     *
     * @param content the help content to display
     * @param icon the FontAwesome icon for this topic
     * @param categoryColor the hex color for this category
     * @param helpService the help service for opening HMRC links
     * @param dialogSize the dialog size (STANDARD, MEDIUM, or LARGE)
     */
    public HelpDialog(HelpContent content, Ikon icon, String categoryColor, HelpService helpService, DialogSize dialogSize) {
        this.content = Objects.requireNonNull(content, "content must not be null");
        this.icon = icon != null ? icon : FontAwesomeSolid.INFO_CIRCLE;
        this.categoryColor = categoryColor != null ? categoryColor : "#0066cc";
        this.helpService = helpService;
        this.dialogSize = dialogSize != null ? dialogSize : DialogSize.STANDARD;

        this.stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);  // Must be first, before other init methods
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Help - " + content.title());

        // Find owner window
        Window owner = Window.getWindows().stream()
                .filter(Window::isFocused)
                .findFirst()
                .orElse(null);
        if (owner != null) {
            stage.initOwner(owner);
        }

        initializeDialog();
    }

    /**
     * Creates a new HelpDialog with default styling.
     *
     * @param content the help content to display
     * @param helpService the help service for opening HMRC links
     */
    public HelpDialog(HelpContent content, HelpService helpService) {
        this(content, FontAwesomeSolid.INFO_CIRCLE, "#0066cc", helpService, DialogSize.STANDARD);
    }

    private void initializeDialog() {
        // Build content container
        VBox container = new VBox(0);
        container.getStyleClass().add("help-dialog-container");

        // Set container dimensions based on dialog size
        switch (dialogSize) {
            case LARGE -> {
                container.setMinWidth(LARGE_WIDTH);
                container.setMaxWidth(LARGE_WIDTH);
                container.setMinHeight(LARGE_HEIGHT);
                container.setMaxHeight(LARGE_HEIGHT);
            }
            case MEDIUM -> {
                container.setMinWidth(MEDIUM_WIDTH);
                container.setMaxWidth(MEDIUM_WIDTH);
                container.setMinHeight(MEDIUM_HEIGHT);
                container.setMaxHeight(MEDIUM_HEIGHT);
            }
            default -> {
                container.setMinWidth(STANDARD_WIDTH);
                container.setMaxWidth(STANDARD_WIDTH);
            }
        }

        // Header
        container.getChildren().add(createHeader());

        // Body content with scroll support
        Region bodyContent = (dialogSize == DialogSize.LARGE || dialogSize == DialogSize.MEDIUM)
                ? createStyledBody() : createBody();

        // Wrap body in ScrollPane
        ScrollPane scrollPane = new ScrollPane(bodyContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("help-scroll-pane");

        if (dialogSize == DialogSize.LARGE || dialogSize == DialogSize.MEDIUM) {
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        } else {
            // Standard mode - cap height
            double screenHeight = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
            double maxBodyHeight = Math.min(400, screenHeight * 0.5);
            scrollPane.setMaxHeight(maxBodyHeight);
            scrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
        }

        container.getChildren().add(scrollPane);

        // Footer with buttons
        container.getChildren().add(createFooter());

        // Apply styling using DialogStyler utility (same as NotificationDialog)
        DialogStyler.applyRoundedClip(container, DialogStyler.CORNER_RADIUS);
        StackPane shadowWrapper = DialogStyler.createShadowWrapper(container);
        DialogStyler.setupStyledDialog(stage, shadowWrapper, STYLESHEET);
        DialogStyler.centerOnOwner(stage);

        LOG.fine("HelpDialog created for topic: " + content.title());
    }

    /**
     * Creates the footer with the Close button.
     */
    private HBox createFooter() {
        HBox footer = new HBox(12);
        footer.getStyleClass().add("help-dialog-buttons");
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 20, 20, 20));

        // OK button (primary)
        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("help-btn-primary");
        okBtn.setOnAction(e -> stage.close());
        okBtn.setDefaultButton(true);
        footer.getChildren().add(okBtn);

        return footer;
    }

    /**
     * Creates the gradient header with icon, title, and close button.
     * Matches NotificationDialog pattern for consistent styling.
     * Uses the category-specific color for the gradient.
     */
    private HBox createHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("help-dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));
        // Apply category-specific gradient color
        header.setStyle("-fx-background-color: linear-gradient(to right, " + categoryColor + ", " + adjustColor(categoryColor, 20) + ");");

        // Icon circle with FontIcon
        StackPane iconWrapper = new StackPane();
        iconWrapper.getStyleClass().add("help-dialog-icon-wrapper");
        iconWrapper.setMinSize(40, 40);
        iconWrapper.setMaxSize(40, 40);
        iconWrapper.setAlignment(Pos.CENTER);
        iconWrapper.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 20;");

        FontIcon fontIcon = FontIcon.of(icon, 18);
        fontIcon.setIconColor(Color.WHITE);
        iconWrapper.getChildren().add(fontIcon);

        // Title
        Label titleLabel = new Label(content.title());
        titleLabel.getStyleClass().add("help-dialog-title");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 600;");
        titleLabel.setWrapText(true);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Close button
        Button closeBtn = new Button("\u2715");
        closeBtn.getStyleClass().add("help-dialog-close");
        closeBtn.setOnAction(e -> stage.close());

        header.getChildren().addAll(iconWrapper, titleLabel, spacer, closeBtn);

        return header;
    }

    /**
     * Creates body content with styled section titles for User Guide.
     * Section titles are detected by lines ending with ━━━ characters.
     */
    private VBox createStyledBody() {
        VBox body = new VBox();
        body.getStyleClass().add("help-dialog-body");
        body.setSpacing(12);  // Improved paragraph spacing per /aura review
        body.setPadding(new Insets(24, 32, 24, 32));
        body.setStyle("-fx-background-color: white;");

        String bodyText = content.body();
        String[] lines = bodyText.split("\n");

        StringBuilder currentParagraph = new StringBuilder();

        for (String line : lines) {
            // Check if this line is a section underline (━━━━━)
            if (line.contains("━━━")) {
                // Previous line was a section title - flush current paragraph first
                if (!currentParagraph.isEmpty()) {
                    // Check if the last part is a title (short line before ━━━)
                    String paragraphText = currentParagraph.toString().trim();
                    if (!paragraphText.isEmpty()) {
                        // This is the section title - styled per /aura review
                        Label titleLabel = new Label(paragraphText);
                        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1f2937; -fx-padding: 24 0 8 0;");
                        titleLabel.setWrapText(true);
                        titleLabel.setMaxWidth(Double.MAX_VALUE);
                        body.getChildren().add(titleLabel);
                    }
                    currentParagraph = new StringBuilder();
                }
                // Skip the underline itself
                continue;
            }

            // Empty line means end of paragraph
            if (line.trim().isEmpty()) {
                if (!currentParagraph.isEmpty()) {
                    addParagraph(body, currentParagraph.toString().trim());
                    currentParagraph = new StringBuilder();
                }
            } else {
                if (!currentParagraph.isEmpty()) {
                    currentParagraph.append(" ");
                }
                currentParagraph.append(line);
            }
        }

        // Add final paragraph
        if (!currentParagraph.isEmpty()) {
            addParagraph(body, currentParagraph.toString().trim());
        }

        // HMRC Link button if available
        if (content.hasLink() && helpService != null) {
            Region spacer = new Region();
            spacer.setMinHeight(16);

            Button learnMoreBtn = createLinkButton();
            body.getChildren().addAll(spacer, learnMoreBtn);
        }

        return body;
    }

    private void addParagraph(VBox body, String text) {
        if (text.isEmpty()) return;

        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-line-spacing: 4;");
        label.setMaxWidth(Double.MAX_VALUE);
        body.getChildren().add(label);
    }

    private VBox createBody() {
        VBox body = new VBox();
        body.getStyleClass().add("help-dialog-body");
        body.setSpacing(16);
        body.setPadding(new Insets(24));
        body.setStyle("-fx-background-color: white;");

        // Body text
        Label bodyLabel = new Label(content.body());
        bodyLabel.getStyleClass().add("help-dialog-text");
        bodyLabel.setWrapText(true);
        bodyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-line-spacing: 4;");
        bodyLabel.setMaxWidth(Double.MAX_VALUE);

        body.getChildren().add(bodyLabel);

        // HMRC Link button if available
        if (content.hasLink() && helpService != null) {
            Region spacer = new Region();
            spacer.setMinHeight(8);

            Button learnMoreBtn = createLinkButton();
            body.getChildren().addAll(spacer, learnMoreBtn);
        }

        return body;
    }

    private Button createLinkButton() {
        Button learnMoreBtn = new Button(content.linkText() + " ↗");
        learnMoreBtn.getStyleClass().add("help-dialog-link-btn");
        learnMoreBtn.setStyle(
            "-fx-background-color: " + categoryColor + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-padding: 10 20;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;"
        );
        learnMoreBtn.setOnAction(e -> {
            helpService.openHmrcGuidance(content.hmrcLink(), content.title());
        });

        // Hover effect
        learnMoreBtn.setOnMouseEntered(e ->
            learnMoreBtn.setStyle(
                "-fx-background-color: " + adjustColor(categoryColor, -15) + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
            )
        );
        learnMoreBtn.setOnMouseExited(e ->
            learnMoreBtn.setStyle(
                "-fx-background-color: " + categoryColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
            )
        );

        return learnMoreBtn;
    }

    /**
     * Adjusts a hex color by the given amount (positive = lighter, negative = darker).
     */
    private String adjustColor(String hexColor, int amount) {
        try {
            String hex = hexColor.replace("#", "");
            int r = Math.min(255, Math.max(0, Integer.parseInt(hex.substring(0, 2), 16) + amount));
            int g = Math.min(255, Math.max(0, Integer.parseInt(hex.substring(2, 4), 16) + amount));
            int b = Math.min(255, Math.max(0, Integer.parseInt(hex.substring(4, 6), 16) + amount));
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            return hexColor;
        }
    }

    /**
     * Shows the dialog and waits for the user to close it.
     */
    public void showAndWaitDialog() {
        stage.showAndWait();
    }

    /**
     * Shows the dialog and waits for the user to close it.
     * Alternative method name matching Dialog API for compatibility.
     */
    public void showAndWait() {
        stage.showAndWait();
    }

    /**
     * Shows the dialog (non-blocking).
     */
    public void show() {
        stage.show();
    }

    /**
     * Closes the dialog.
     */
    public void close() {
        stage.close();
    }
}
