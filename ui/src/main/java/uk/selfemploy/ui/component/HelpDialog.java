package uk.selfemploy.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import uk.selfemploy.ui.help.HelpContent;
import uk.selfemploy.ui.help.HelpService;

import java.util.Objects;

/**
 * A custom styled dialog for displaying help content.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Gradient header with category-colored icon</li>
 *   <li>Proper typography for title and body</li>
 *   <li>Optional "Learn More" link to HMRC guidance</li>
 *   <li>Styled close button</li>
 * </ul>
 *
 * <p>Design: /aura (Senior UI/UX Design Architect)</p>
 * <p>Implementation: /james</p>
 */
public class HelpDialog extends Dialog<Void> {

    private static final String STYLESHEET = "/css/help.css";

    private final HelpContent content;
    private final String categoryColor;
    private final String icon;
    private final HelpService helpService;

    /**
     * Creates a new HelpDialog.
     *
     * @param content the help content to display
     * @param icon the emoji icon for this topic
     * @param categoryColor the hex color for this category
     * @param helpService the help service for opening HMRC links
     */
    public HelpDialog(HelpContent content, String icon, String categoryColor, HelpService helpService) {
        this.content = Objects.requireNonNull(content, "content must not be null");
        this.icon = icon != null ? icon : "ℹ";
        this.categoryColor = categoryColor != null ? categoryColor : "#0066cc";
        this.helpService = helpService;

        setTitle("Help - " + content.title());
        setResizable(false);

        initializeDialog();
    }

    /**
     * Creates a new HelpDialog with default styling.
     *
     * @param content the help content to display
     * @param helpService the help service for opening HMRC links
     */
    public HelpDialog(HelpContent content, HelpService helpService) {
        this(content, "ℹ", "#0066cc", helpService);
    }

    private void initializeDialog() {
        DialogPane dialogPane = getDialogPane();

        // Load stylesheet
        try {
            String css = getClass().getResource(STYLESHEET).toExternalForm();
            dialogPane.getStylesheets().add(css);
        } catch (Exception e) {
            // Stylesheet not found, continue with default styling
        }

        dialogPane.getStyleClass().add("help-dialog-pane");
        dialogPane.setMinWidth(520);
        dialogPane.setMaxWidth(600);

        // Create the content
        VBox mainContainer = new VBox();
        mainContainer.getStyleClass().add("help-dialog-container");
        mainContainer.setSpacing(0);

        // Header with gradient background
        HBox header = createHeader();

        // Body content
        VBox body = createBody();

        mainContainer.getChildren().addAll(header, body);

        dialogPane.setContent(mainContainer);

        // Add buttons
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().add(closeButton);

        // Style the close button
        Button closeBtn = (Button) dialogPane.lookupButton(closeButton);
        if (closeBtn != null) {
            closeBtn.getStyleClass().add("help-dialog-close-btn");
        }
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("help-dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(16);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: linear-gradient(to right, " + categoryColor + ", " + adjustColor(categoryColor, 20) + ");" +
                       "-fx-background-radius: 0;");

        // Icon circle
        StackPane iconWrapper = new StackPane();
        iconWrapper.getStyleClass().add("help-dialog-icon-wrapper");
        iconWrapper.setMinSize(48, 48);
        iconWrapper.setMaxSize(48, 48);
        iconWrapper.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 24;");

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("help-dialog-icon");
        iconLabel.setStyle("-fx-font-size: 24px;");
        iconWrapper.getChildren().add(iconLabel);

        // Title
        Label titleLabel = new Label(content.title());
        titleLabel.getStyleClass().add("help-dialog-title");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        header.getChildren().addAll(iconWrapper, titleLabel);

        return header;
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

            body.getChildren().addAll(spacer, learnMoreBtn);
        }

        return body;
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
        showAndWait();
    }
}
