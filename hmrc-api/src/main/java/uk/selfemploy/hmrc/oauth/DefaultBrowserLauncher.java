package uk.selfemploy.hmrc.oauth;

import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.hmrc.exception.HmrcOAuthException;
import uk.selfemploy.hmrc.exception.HmrcOAuthException.OAuthError;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default browser launcher using OS-specific commands.
 * Avoids java.awt.Desktop which can conflict with JavaFX.
 */
@ApplicationScoped
public class DefaultBrowserLauncher implements BrowserLauncher {

    private static final Logger LOG = Logger.getLogger(DefaultBrowserLauncher.class.getName());

    @Override
    public void openUrl(String url) throws HmrcOAuthException {
        LOG.info("Opening browser with URL: " + url);

        try {
            openUrlWithOsCommand(url);
            LOG.info("Browser launch command executed");
        } catch (IOException | UnsupportedOperationException e) {
            LOG.log(Level.SEVERE, "Failed to open browser", e);
            throw new HmrcOAuthException(OAuthError.BROWSER_ERROR, e);
        }
    }

    private void openUrlWithOsCommand(String url) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        LOG.info("Detected OS: " + os);

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

        pb.inheritIO();
        Process process = pb.start();
        LOG.info("Browser process started with PID: " + process.pid());
    }
}
