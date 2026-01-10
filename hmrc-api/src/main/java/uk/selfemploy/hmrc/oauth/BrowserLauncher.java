package uk.selfemploy.hmrc.oauth;

import uk.selfemploy.hmrc.exception.HmrcOAuthException;

/**
 * Interface for launching the system browser.
 * Abstraction allows for testing and platform-specific implementations.
 */
public interface BrowserLauncher {

    /**
     * Opens the specified URL in the user's default browser.
     *
     * @param url The URL to open
     * @throws HmrcOAuthException if the browser cannot be opened
     */
    void openUrl(String url) throws HmrcOAuthException;
}
