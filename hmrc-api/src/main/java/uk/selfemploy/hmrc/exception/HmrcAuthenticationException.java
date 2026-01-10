package uk.selfemploy.hmrc.exception;

/**
 * Exception thrown when HMRC authentication fails.
 * This includes OAuth2 errors, invalid tokens, and authorization issues.
 */
public class HmrcAuthenticationException extends HmrcApiException {

    public HmrcAuthenticationException(String message) {
        super(message);
    }

    public HmrcAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public HmrcAuthenticationException(String message, String errorCode) {
        super(message, errorCode, 401);
    }

    public HmrcAuthenticationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, 401, cause);
    }
}
