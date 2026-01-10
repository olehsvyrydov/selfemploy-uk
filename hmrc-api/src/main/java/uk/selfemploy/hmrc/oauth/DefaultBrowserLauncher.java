package uk.selfemploy.hmrc.oauth;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.exception.HmrcOAuthException;
import uk.selfemploy.hmrc.exception.HmrcOAuthException.OAuthError;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/**
 * Default browser launcher using java.awt.Desktop.
 * Falls back to OS-specific commands if Desktop is not supported.
 */
@ApplicationScoped
public class DefaultBrowserLauncher implements BrowserLauncher {

    private static final Logger log = LoggerFactory.getLogger(DefaultBrowserLauncher.class);

    @Override
    public void openUrl(String url) throws HmrcOAuthException {
        log.info("Opening browser with URL: {}", url);

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            } else {
                // Fallback to OS-specific commands
                openUrlWithOsCommand(url);
            }
        } catch (IOException | UnsupportedOperationException e) {
            log.error("Failed to open browser", e);
            throw new HmrcOAuthException(OAuthError.BROWSER_ERROR, e);
        }
    }

    private void openUrlWithOsCommand(String url) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;

        if (os.contains("win")) {
            pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
        } else if (os.contains("mac")) {
            pb = new ProcessBuilder("open", url);
        } else if (os.contains("nix") || os.contains("nux")) {
            pb = new ProcessBuilder("xdg-open", url);
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + os);
        }

        pb.start();
    }
}
