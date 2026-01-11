package uk.selfemploy.core.auth;

/**
 * Exception thrown when token operations fail.
 *
 * <p>Provides user-friendly error messages suitable for display
 * and detailed information for logging.</p>
 */
public class TokenException extends RuntimeException {

    private final TokenError error;

    public enum TokenError {
        /**
         * No stored token available - user needs to authenticate.
         */
        NO_TOKEN("Please connect to HMRC to continue"),

        /**
         * Token is expired and cannot be refreshed.
         */
        EXPIRED("Your HMRC session has expired. Please re-authenticate with HMRC"),

        /**
         * Token refresh failed due to invalid refresh token.
         */
        REFRESH_FAILED("Please re-authenticate with HMRC"),

        /**
         * Token storage/retrieval failed.
         */
        STORAGE_ERROR("Unable to access stored credentials. Please try again"),

        /**
         * Network error during token refresh.
         */
        NETWORK_ERROR("Unable to connect to HMRC. Please check your internet connection");

        private final String userMessage;

        TokenError(String userMessage) {
            this.userMessage = userMessage;
        }

        public String getUserMessage() {
            return userMessage;
        }
    }

    public TokenException(TokenError error) {
        super(error.getUserMessage());
        this.error = error;
    }

    public TokenException(TokenError error, String technicalMessage) {
        super(error.getUserMessage());
        this.error = error;
    }

    public TokenException(TokenError error, Throwable cause) {
        super(error.getUserMessage(), cause);
        this.error = error;
    }

    public TokenException(TokenError error, String technicalMessage, Throwable cause) {
        super(error.getUserMessage(), cause);
        this.error = error;
    }

    /**
     * Returns the error type.
     */
    public TokenError getError() {
        return error;
    }

    /**
     * Returns user-friendly message suitable for UI display.
     */
    public String getUserMessage() {
        return error.getUserMessage();
    }

    /**
     * Checks if this error indicates the user needs to re-authenticate.
     */
    public boolean requiresReauthentication() {
        return error == TokenError.NO_TOKEN ||
               error == TokenError.EXPIRED ||
               error == TokenError.REFRESH_FAILED;
    }
}
