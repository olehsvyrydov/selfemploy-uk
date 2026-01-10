package uk.selfemploy.hmrc.exception;

/**
 * Exception for HMRC 5xx server errors.
 * These errors ARE retryable as they indicate temporary server issues.
 */
public class HmrcServerException extends HmrcApiException {

    private static final String DEFAULT_USER_MESSAGE =
            "HMRC services are temporarily unavailable. The system will retry automatically.";

    public HmrcServerException(String message, String errorCode, int httpStatus) {
        super(message, errorCode, httpStatus, DEFAULT_USER_MESSAGE);
    }

    public HmrcServerException(String message, String errorCode, int httpStatus, String userMessage) {
        super(message, errorCode, httpStatus, userMessage);
    }

    public HmrcServerException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, errorCode, httpStatus, DEFAULT_USER_MESSAGE, cause);
    }

    @Override
    public boolean isRetryable() {
        return true;
    }
}
