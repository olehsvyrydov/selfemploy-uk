package uk.selfemploy.hmrc.exception;

/**
 * Exception thrown during OAuth2 flow errors.
 */
public class HmrcOAuthException extends HmrcAuthenticationException {

    private final OAuthError error;

    public enum OAuthError {
        INVALID_STATE("State parameter mismatch - possible CSRF attack"),
        USER_CANCELLED("User cancelled the authentication"),
        TIMEOUT("Authentication flow timed out"),
        ACCESS_DENIED("User denied access"),
        SERVER_ERROR("OAuth server error"),
        INVALID_GRANT("Invalid or expired authorization code"),
        INVALID_CLIENT("Invalid client credentials"),
        CALLBACK_ERROR("Failed to receive callback"),
        TOKEN_EXCHANGE_FAILED("Failed to exchange code for tokens"),
        CONFIGURATION_ERROR("HMRC credentials not configured"),
        PORT_IN_USE("OAuth callback port is already in use"),
        BROWSER_ERROR("Failed to open system browser");

        private final String description;

        OAuthError(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public HmrcOAuthException(OAuthError error) {
        super(error.getDescription());
        this.error = error;
    }

    public HmrcOAuthException(OAuthError error, String additionalInfo) {
        super(error.getDescription() + ": " + additionalInfo);
        this.error = error;
    }

    public HmrcOAuthException(OAuthError error, Throwable cause) {
        super(error.getDescription(), cause);
        this.error = error;
    }

    public OAuthError getError() {
        return error;
    }
}
