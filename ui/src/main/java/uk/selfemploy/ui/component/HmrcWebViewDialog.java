package uk.selfemploy.ui.component;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.ui.help.HmrcLinkTopic;
import uk.selfemploy.ui.util.BrowserUtil;

import java.util.Objects;
import java.util.Set;

/**
 * In-app browser dialog for displaying HMRC guidance pages.
 *
 * <p>SE-7XX: In-App Browser for HMRC Guidance</p>
 *
 * <p>This dialog provides a WebView-based browser for viewing GOV.UK content
 * without leaving the application. Key features include:</p>
 * <ul>
 *   <li>URL whitelist validation (only GOV.UK domains allowed)</li>
 *   <li>Non-modal dialog (allows main window interaction)</li>
 *   <li>Navigation controls (Back, Forward, Reload)</li>
 *   <li>"Open in Browser" fallback button</li>
 *   <li>Loading indicator during page load</li>
 *   <li>Error state with user-friendly messages</li>
 * </ul>
 *
 * <p>Singleton pattern is used to reuse the same dialog instance and manage memory.</p>
 *
 * <p>Architecture Reference: docs/sprints/sprint-7/approvals/jorge-architecture-inapp-browser.md</p>
 */
public class HmrcWebViewDialog {

    private static final Logger LOG = LoggerFactory.getLogger(HmrcWebViewDialog.class);

    // === Constants ===

    /** Default dialog width in pixels. */
    public static final int DEFAULT_WIDTH = 1024;

    /** Default dialog height in pixels. */
    public static final int DEFAULT_HEIGHT = 768;

    /** Minimum dialog width in pixels. */
    public static final int MIN_WIDTH = 800;

    /** Minimum dialog height in pixels. */
    public static final int MIN_HEIGHT = 600;

    /** CSS stylesheet location. */
    private static final String STYLESHEET_LOCATION = "/css/hmrc-webview.css";

    // === Singleton Instance ===

    private static HmrcWebViewDialog instance;

    // === Instance Fields ===

    private final Stage stage;
    private final WebView webView;
    private final WebEngine webEngine;
    private final GovUkUrlValidator urlValidator;

    // UI Components
    private final Button backButton;
    private final Button forwardButton;
    private final Button reloadButton;
    private final Label urlLabel;
    private final Button openBrowserButton;
    private final ProgressBar loadProgress;
    private final Label statusLabel;
    private final VBox errorContainer;
    private final Label errorMessageLabel;

    private String currentUrl;

    // === Static Methods ===

    /**
     * Validates whether a URL is allowed in the in-app browser.
     *
     * @param url the URL to validate
     * @return true if the URL is allowed
     */
    public static boolean isValidUrl(String url) {
        return new GovUkUrlValidator().isAllowedUrl(url);
    }

    /**
     * Gets the URL for an HMRC link topic.
     *
     * @param topic the HMRC link topic
     * @return the URL string
     * @throws NullPointerException if topic is null
     */
    public static String getUrlForTopic(HmrcLinkTopic topic) {
        Objects.requireNonNull(topic, "topic must not be null");
        return topic.getUrl();
    }

    /** Known acronyms that should remain uppercase in titles. */
    private static final Set<String> KNOWN_ACRONYMS = Set.of(
            "NI", "MTD", "ITSA", "SA103", "HMRC", "API", "UK", "VAT"
    );

    /**
     * Gets a human-readable title for an HMRC link topic.
     *
     * @param topic the HMRC link topic
     * @return formatted title string
     * @throws NullPointerException if topic is null
     */
    public static String getTitleForTopic(HmrcLinkTopic topic) {
        Objects.requireNonNull(topic, "topic must not be null");
        // Convert enum name to title case, preserving known acronyms
        // (e.g., TAX_RATES -> Tax Rates, SA103_FORM -> SA103 Form, MTD_FOR_ITSA -> MTD For ITSA)
        String name = topic.name();
        String[] words = name.split("_");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(' ');
            }
            String word = words[i];
            if (KNOWN_ACRONYMS.contains(word)) {
                // Keep known acronyms in uppercase
                result.append(word);
            } else if (containsDigit(word)) {
                // Alphanumeric codes like SA103 stay uppercase
                result.append(word);
            } else {
                // Title case for regular words
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }

        return result.toString();
    }

    /**
     * Checks if a word contains at least one digit.
     *
     * @param word the word to check
     * @return true if the word contains a digit
     */
    private static boolean containsDigit(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        for (char c : word.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the error message for offline/network errors.
     *
     * @return the error message
     */
    public static String getOfflineErrorMessage() {
        return "Unable to load page. Please check your internet connection and try again.";
    }

    /**
     * Returns the error message for general load failures.
     *
     * @return the error message
     */
    public static String getLoadErrorMessage() {
        return "Unable to load this page. You can try using the \"Open in Browser\" button instead.";
    }

    /**
     * Returns the error message when a URL is rejected by the whitelist.
     *
     * @param url the rejected URL
     * @return the error message
     */
    public static String getUrlRejectedMessage(String url) {
        return "This URL is not allowed. The in-app browser only supports GOV.UK domains.";
    }

    /**
     * Returns the location of the CSS stylesheet for this component.
     *
     * @return the stylesheet resource path
     */
    public static String getStylesheetLocation() {
        return STYLESHEET_LOCATION;
    }

    // === Public Methods ===

    /**
     * Shows the dialog and loads the specified HMRC topic.
     *
     * <p>This method should be called from the JavaFX Application Thread.</p>
     *
     * @param topic the HMRC link topic to display
     */
    public static void showTopic(HmrcLinkTopic topic) {
        Objects.requireNonNull(topic, "topic must not be null");
        showUrl(getUrlForTopic(topic), getTitleForTopic(topic));
    }

    /**
     * Shows the dialog and loads the specified URL.
     *
     * <p>This method should be called from the JavaFX Application Thread.</p>
     *
     * @param url   the URL to load (must be a valid GOV.UK URL)
     * @param title the dialog title
     */
    public static void showUrl(String url, String title) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showUrl(url, title));
            return;
        }

        HmrcWebViewDialog dialog = getInstance();
        dialog.stage.setTitle("HMRC Guidance - " + (title != null ? title : ""));
        dialog.loadUrl(url);
        dialog.stage.show();
        dialog.stage.toFront();
    }

    /**
     * Closes the dialog.
     */
    public static void closeDialog() {
        if (instance != null) {
            instance.stage.hide();
        }
    }

    // === Private Constructor ===

    private HmrcWebViewDialog() {
        this.urlValidator = new GovUkUrlValidator();

        // Create WebView
        webView = new WebView();
        webEngine = webView.getEngine();

        // Create toolbar components
        backButton = createToolbarButton("<", "Go back");
        forwardButton = createToolbarButton(">", "Go forward");
        reloadButton = createToolbarButton("Reload", "Reload page");
        urlLabel = new Label();
        urlLabel.getStyleClass().add("webview-url-label");
        urlLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(urlLabel, Priority.ALWAYS);

        openBrowserButton = new Button("Open in Browser");
        openBrowserButton.getStyleClass().addAll("button-secondary", "webview-open-browser");
        openBrowserButton.setTooltip(new Tooltip("Open this page in your default web browser"));

        Button closeButton = createToolbarButton("X", "Close");

        // Create toolbar
        HBox toolbar = new HBox(8);
        toolbar.getStyleClass().add("webview-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8, 12, 8, 12));
        toolbar.getChildren().addAll(
                backButton, forwardButton, reloadButton,
                urlLabel,
                openBrowserButton, closeButton
        );

        // Create status bar
        loadProgress = new ProgressBar();
        loadProgress.getStyleClass().add("webview-progress");
        loadProgress.setMaxWidth(Double.MAX_VALUE);
        loadProgress.setVisible(false);

        statusLabel = new Label();
        statusLabel.getStyleClass().add("webview-status-label");

        HBox statusBar = new HBox(8);
        statusBar.getStyleClass().add("webview-status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(4, 12, 4, 12));
        statusBar.getChildren().addAll(loadProgress, statusLabel);
        HBox.setHgrow(loadProgress, Priority.ALWAYS);

        // Create error container (hidden by default)
        errorMessageLabel = new Label();
        errorMessageLabel.getStyleClass().add("webview-error-message");
        errorMessageLabel.setWrapText(true);

        Button retryButton = new Button("Try Again");
        retryButton.getStyleClass().add("button-primary");
        retryButton.setOnAction(e -> reloadPage());

        Button errorOpenBrowserButton = new Button("Open in Browser");
        errorOpenBrowserButton.getStyleClass().add("button-secondary");
        errorOpenBrowserButton.setOnAction(e -> openInExternalBrowser());

        HBox errorButtons = new HBox(12);
        errorButtons.setAlignment(Pos.CENTER);
        errorButtons.getChildren().addAll(retryButton, errorOpenBrowserButton);

        errorContainer = new VBox(16);
        errorContainer.getStyleClass().add("webview-error-container");
        errorContainer.setAlignment(Pos.CENTER);
        errorContainer.setPadding(new Insets(40));
        errorContainer.getChildren().addAll(errorMessageLabel, errorButtons);
        errorContainer.setVisible(false);
        errorContainer.setManaged(false);

        // Stack WebView and error container
        StackPane contentStack = new StackPane(webView, errorContainer);
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        // Main layout
        VBox root = new VBox();
        root.getStyleClass().add("hmrc-webview-dialog");
        root.getChildren().addAll(toolbar, contentStack, statusBar);

        // Create scene
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);

        // Add stylesheets
        String mainCss = getClass().getResource("/css/main.css").toExternalForm();
        String webviewCss = getClass().getResource(STYLESHEET_LOCATION).toExternalForm();
        scene.getStylesheets().addAll(mainCss, webviewCss);

        // Create stage
        stage = new Stage();
        stage.initModality(Modality.NONE); // Non-modal - allows main window interaction
        stage.initStyle(StageStyle.DECORATED);
        stage.setScene(scene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setTitle("HMRC Guidance");

        // Wire up event handlers
        setupEventHandlers(closeButton);
        setupWebEngineListeners();
    }

    private static HmrcWebViewDialog getInstance() {
        if (instance == null) {
            instance = new HmrcWebViewDialog();
        }
        return instance;
    }

    private Button createToolbarButton(String text, String tooltip) {
        Button button = new Button(text);
        button.getStyleClass().add("webview-toolbar-button");
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    private void setupEventHandlers(Button closeButton) {
        backButton.setOnAction(e -> goBack());
        forwardButton.setOnAction(e -> goForward());
        reloadButton.setOnAction(e -> reloadPage());
        openBrowserButton.setOnAction(e -> openInExternalBrowser());
        closeButton.setOnAction(e -> stage.hide());

        // Close on escape key
        stage.getScene().setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                stage.hide();
            }
        });
    }

    private void setupWebEngineListeners() {
        // Track loading state
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.RUNNING) {
                showLoading();
            } else if (newState == Worker.State.SUCCEEDED) {
                hideLoading();
                hideError();
                updateNavigationButtons();
                updateUrlLabel();
            } else if (newState == Worker.State.FAILED) {
                hideLoading();
                showError(getLoadErrorMessage());
            }
        });

        // Track progress
        webEngine.getLoadWorker().progressProperty().addListener((obs, oldVal, newVal) -> {
            double progress = newVal.doubleValue();
            if (progress > 0 && progress < 1) {
                loadProgress.setProgress(progress);
            }
        });

        // Track URL changes and validate
        webEngine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            if (newUrl != null && !newUrl.isBlank() && !urlValidator.isAllowedUrl(newUrl)) {
                LOG.warn("Navigation to non-GOV.UK URL blocked: {}", newUrl);
                // Cancel the navigation by loading the previous URL or showing error
                Platform.runLater(() -> {
                    if (currentUrl != null) {
                        webEngine.load(currentUrl);
                    }
                    showError(getUrlRejectedMessage(newUrl));
                });
            } else if (newUrl != null && !newUrl.isBlank()) {
                currentUrl = newUrl;
                updateUrlLabel();
            }
        });

        // Track title changes
        webEngine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            if (newTitle != null && !newTitle.isBlank()) {
                statusLabel.setText(newTitle);
            }
        });
    }

    private void loadUrl(String url) {
        if (url == null || url.isBlank()) {
            showError("No URL provided");
            return;
        }

        if (!urlValidator.isAllowedUrl(url)) {
            showError(getUrlRejectedMessage(url));
            return;
        }

        currentUrl = url;
        hideError();
        webEngine.load(url);
    }

    private void goBack() {
        WebHistory history = webEngine.getHistory();
        if (history.getCurrentIndex() > 0) {
            history.go(-1);
        }
    }

    private void goForward() {
        WebHistory history = webEngine.getHistory();
        if (history.getCurrentIndex() < history.getEntries().size() - 1) {
            history.go(1);
        }
    }

    private void reloadPage() {
        if (currentUrl != null) {
            hideError();
            webEngine.reload();
        }
    }

    private void openInExternalBrowser() {
        if (currentUrl != null) {
            LOG.debug("Opening in external browser: {}", currentUrl);
            BrowserUtil.openUrl(currentUrl);
        }
    }

    private void showLoading() {
        loadProgress.setVisible(true);
        loadProgress.setProgress(-1); // Indeterminate
        statusLabel.setText("Loading...");
    }

    private void hideLoading() {
        loadProgress.setVisible(false);
        loadProgress.setProgress(1.0); // Complete
        // Update status with page title or clear "Loading..."
        String title = webEngine.getTitle();
        if (title != null && !title.isBlank()) {
            statusLabel.setText(title);
        } else {
            statusLabel.setText("Page loaded");
        }
    }

    private void showError(String message) {
        errorMessageLabel.setText(message);
        errorContainer.setVisible(true);
        errorContainer.setManaged(true);
        webView.setVisible(false);
    }

    private void hideError() {
        errorContainer.setVisible(false);
        errorContainer.setManaged(false);
        webView.setVisible(true);
    }

    private void updateNavigationButtons() {
        WebHistory history = webEngine.getHistory();
        backButton.setDisable(history.getCurrentIndex() <= 0);
        forwardButton.setDisable(history.getCurrentIndex() >= history.getEntries().size() - 1);
    }

    private void updateUrlLabel() {
        String location = webEngine.getLocation();
        if (location != null && !location.isBlank()) {
            // Truncate long URLs for display
            String displayUrl = location.length() > 80
                    ? location.substring(0, 77) + "..."
                    : location;
            urlLabel.setText(displayUrl);
            urlLabel.setTooltip(new Tooltip(location));
        }
    }
}
