package uk.selfemploy.ui.component;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Toast notification component for non-intrusive user feedback.
 *
 * <p>Designed by /aura - Professional slate palette with subtle animations.
 * Used to notify users when actions occur without requiring interaction,
 * such as opening links in an external browser.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Auto-dismiss after 2.5 seconds</li>
 *   <li>Fade-in entrance animation</li>
 *   <li>Fade-out exit animation</li>
 *   <li>Accessible - screen reader friendly</li>
 *   <li>Non-blocking - does not steal focus</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>
 * // Show a simple toast
 * ToastNotification.showExternalBrowserToast("Opening in your browser...");
 *
 * // Show toast with URL domain
 * ToastNotification.showExternalBrowserToast("Opening GitHub...", "github.com");
 * </pre>
 */
public final class ToastNotification {

    private static final Logger LOG = Logger.getLogger(ToastNotification.class.getName());

    /** Display duration before auto-dismiss (milliseconds) */
    private static final int DISPLAY_DURATION_MS = 2500;

    /** Animation duration (milliseconds) */
    private static final int ANIMATION_DURATION_MS = 200;

    /** External link icon (Unicode arrow pointing up-right) */
    private static final String EXTERNAL_LINK_ICON = "↗";

    private ToastNotification() {
        // Utility class - no instantiation
    }

    /**
     * Shows a toast notification for opening a URL in external browser.
     *
     * @param message the primary message to display (e.g., "Opening in your browser...")
     */
    public static void showExternalBrowserToast(String message) {
        showExternalBrowserToast(message, null);
    }

    /**
     * Shows a toast notification for opening a URL in external browser.
     *
     * @param message the primary message to display
     * @param url     the URL being opened (domain will be extracted and shown)
     */
    public static void showExternalBrowserToast(String message, String url) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showExternalBrowserToast(message, url));
            return;
        }

        try {
            Window ownerWindow = findActiveWindow();
            if (ownerWindow == null) {
                LOG.fine("No active window found for toast notification");
                return;
            }

            // Create popup
            Popup popup = new Popup();
            popup.setAutoHide(false);
            popup.setAutoFix(true);

            // Create toast content
            HBox toast = createToastContent(message, url);
            popup.getContent().add(toast);

            // Position at bottom center of owner window (inside the window, not at edge)
            double toastWidth = 280;
            double toastHeight = 60;
            double windowX = ownerWindow.getX();
            double windowY = ownerWindow.getY();
            double windowW = ownerWindow.getWidth();
            double windowH = ownerWindow.getHeight();

            // Position centered horizontally, 100px from bottom of window
            double x = windowX + (windowW - toastWidth) / 2;
            double y = windowY + windowH - toastHeight - 100;

            // Initial state for animation
            toast.setOpacity(0);

            // Show popup
            popup.show(ownerWindow, x, y);
            LOG.fine("Toast shown at position: " + x + ", " + y);

            // Entry animation (fade in)
            FadeTransition fadeIn = new FadeTransition(Duration.millis(ANIMATION_DURATION_MS), toast);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            // Schedule auto-dismiss
            PauseTransition pause = new PauseTransition(Duration.millis(DISPLAY_DURATION_MS));
            pause.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(ANIMATION_DURATION_MS), toast);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(ev -> popup.hide());
                fadeOut.play();
            });
            pause.play();

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not show toast notification", e);
        }
    }

    /**
     * Creates the toast content HBox.
     */
    private static HBox createToastContent(String message, String url) {
        // Icon
        Label iconLabel = new Label(EXTERNAL_LINK_ICON);
        iconLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");

        // Text content
        VBox textBox = new VBox(2);

        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #f8fafc;");
        textBox.getChildren().add(messageLabel);

        // Add URL domain if provided
        if (url != null && !url.isBlank()) {
            String domain = extractDomain(url);
            if (domain != null) {
                Label urlLabel = new Label(domain);
                urlLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
                textBox.getChildren().add(urlLabel);
            }
        }

        // Container with inline styles (for popup that doesn't have CSS loaded)
        HBox toast = new HBox(10);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setPadding(new Insets(12, 16, 12, 16));
        // Use solid color instead of rgba for better visibility
        toast.setStyle(
            "-fx-background-color: #1e293b;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #475569;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 15, 0, 0, 5);" +
            "-fx-min-width: 250;" +
            "-fx-min-height: 50;"
        );
        toast.getChildren().addAll(iconLabel, textBox);

        // Accessibility
        toast.setAccessibleText(message);

        return toast;
    }

    /**
     * Finds the currently active window.
     */
    private static Window findActiveWindow() {
        return Window.getWindows().stream()
                .filter(Window::isFocused)
                .findFirst()
                .orElse(Window.getWindows().isEmpty() ? null : Window.getWindows().get(0));
    }

    /**
     * Extracts the domain from a URL.
     *
     * @param url the URL string
     * @return the domain (e.g., "github.com") or null if extraction fails
     */
    private static String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host != null) {
                // Remove 'www.' prefix if present
                if (host.startsWith("www.")) {
                    host = host.substring(4);
                }
                return host;
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Could not extract domain from URL: " + url, e);
        }
        return null;
    }
}
