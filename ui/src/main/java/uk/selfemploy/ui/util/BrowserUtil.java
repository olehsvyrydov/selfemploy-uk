package uk.selfemploy.ui.util;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.ui.component.ToastNotification;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

/**
 * Utility for opening URLs in the system's default browser.
 *
 * <p>This utility solves the "Force Quit" crash that occurred when clicking
 * HMRC guidance links in help dialogs. The crash was caused by calling
 * {@link Desktop#browse(URI)} directly on the JavaFX Application Thread.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Runs browser opening on a background thread (non-blocking)</li>
 *   <li>Uses platform-specific commands (xdg-open, open, cmd) for reliability</li>
 *   <li>Handles errors gracefully without crashing the application</li>
 *   <li>Provides optional error callback for user notification</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * // Simple usage - fire and forget
 * BrowserUtil.openUrl("https://www.gov.uk/expenses-if-youre-self-employed");
 *
 * // With error callback
 * BrowserUtil.openUrl("https://example.com", error -&gt; {
 *     Platform.runLater(() -&gt; showErrorDialog(error));
 * });
 * </pre>
 */
public final class BrowserUtil {

    private static final Logger LOG = LoggerFactory.getLogger(BrowserUtil.class);

    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase();

    private BrowserUtil() {
        // Utility class - no instantiation
    }

    /**
     * Opens the specified URL in the system's default browser.
     *
     * <p>This method returns immediately - the browser opening happens
     * asynchronously on a background thread.</p>
     *
     * @param url the URL to open (null/blank URLs are ignored)
     */
    public static void openUrl(String url) {
        openUrl(url, true);
    }

    /**
     * Opens the specified URL in the system's default browser.
     *
     * <p>This method returns immediately - the browser opening happens
     * asynchronously on a background thread.</p>
     *
     * @param url       the URL to open (null/blank URLs are ignored)
     * @param showToast whether to show a toast notification
     */
    public static void openUrl(String url, boolean showToast) {
        openUrl(url, showToast, null);
    }

    /**
     * Opens the specified URL in the system's default browser with error callback.
     *
     * <p>This method returns immediately - the browser opening happens
     * asynchronously on a background thread.</p>
     *
     * @param url           the URL to open (null/blank URLs are ignored)
     * @param errorCallback optional callback for error handling (called on background thread)
     */
    public static void openUrl(String url, Consumer<String> errorCallback) {
        openUrl(url, true, errorCallback);
    }

    /**
     * Opens the specified URL in the system's default browser with options.
     *
     * <p>This method returns immediately - the browser opening happens
     * asynchronously on a background thread.</p>
     *
     * @param url           the URL to open (null/blank URLs are ignored)
     * @param showToast     whether to show a toast notification
     * @param errorCallback optional callback for error handling (called on background thread)
     */
    public static void openUrl(String url, boolean showToast, Consumer<String> errorCallback) {
        if (url == null || url.isBlank()) {
            LOG.warn("Attempted to open null or blank URL");
            return;
        }

        // Show toast notification on JavaFX thread before opening browser
        if (showToast) {
            Platform.runLater(() ->
                ToastNotification.showExternalBrowserToast("Opening in your browser...", url));
        }

        // Run on background thread to avoid blocking JavaFX Application Thread
        Thread browserThread = new Thread(() -> openUrlInternal(url, errorCallback), "browser-open");
        browserThread.setDaemon(true);
        browserThread.start();
    }

    /**
     * Returns true if running on Linux.
     *
     * @return true if the OS is Linux
     */
    public static boolean isLinux() {
        return OS_NAME.contains("linux");
    }

    /**
     * Returns true if running on macOS.
     *
     * @return true if the OS is macOS
     */
    public static boolean isMacOs() {
        return OS_NAME.contains("mac");
    }

    /**
     * Returns true if running on Windows.
     *
     * @return true if the OS is Windows
     */
    public static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    /**
     * Returns the platform-specific command to open a URL in the browser.
     *
     * @param url the URL to open
     * @return command array for ProcessBuilder
     */
    public static String[] getBrowserCommand(String url) {
        if (isLinux()) {
            return new String[]{"xdg-open", url};
        } else if (isMacOs()) {
            return new String[]{"open", url};
        } else if (isWindows()) {
            // Windows needs special handling to avoid URL parsing issues
            return new String[]{"cmd", "/c", "start", "", url};
        } else {
            // Fallback - try xdg-open as it's common on Unix-like systems
            return new String[]{"xdg-open", url};
        }
    }

    // === Private Implementation ===

    private static void openUrlInternal(String url, Consumer<String> errorCallback) {
        try {
            // First, try platform-specific command (most reliable)
            if (tryOpenWithProcessBuilder(url)) {
                LOG.debug("Opened URL with platform command: {}", url);
                return;
            }

            // Fallback to AWT Desktop if available
            if (tryOpenWithDesktop(url)) {
                LOG.debug("Opened URL with Desktop.browse: {}", url);
                return;
            }

            // If all methods fail
            String error = "Could not open URL: no supported method available";
            LOG.warn(error);
            if (errorCallback != null) {
                errorCallback.accept(error);
            }

        } catch (Exception e) {
            String error = "Failed to open URL: " + e.getMessage();
            LOG.warn(error, e);
            if (errorCallback != null) {
                errorCallback.accept(error);
            }
        }
    }

    private static boolean tryOpenWithProcessBuilder(String url) {
        try {
            String[] command = getBrowserCommand(url);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Wait briefly to check if the process started successfully
            // We don't wait for completion as the browser runs independently
            Thread.sleep(100);

            // Check if process is still running or completed normally
            if (process.isAlive()) {
                return true;
            }

            // If process completed, check exit code
            int exitCode = process.exitValue();
            return exitCode == 0;

        } catch (IOException | InterruptedException e) {
            LOG.debug("ProcessBuilder approach failed: {}", e.getMessage());
            return false;
        }
    }

    private static boolean tryOpenWithDesktop(String url) {
        try {
            if (!Desktop.isDesktopSupported()) {
                LOG.debug("Desktop not supported on this platform");
                return false;
            }

            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                LOG.debug("Desktop BROWSE action not supported");
                return false;
            }

            URI uri = new URI(url);
            desktop.browse(uri);
            return true;

        } catch (Exception e) {
            LOG.debug("Desktop.browse failed: {}", e.getMessage());
            return false;
        }
    }
}
