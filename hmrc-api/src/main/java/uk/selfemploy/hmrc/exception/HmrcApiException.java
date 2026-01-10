package uk.selfemploy.hmrc.exception;

/**
 * Base exception for all HMRC API errors.
 * Provides errorCode, httpStatus, and user-friendly message.
 */
public class HmrcApiException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;
    private final String userMessage;

    public HmrcApiException(String message) {
        super(message);
        this.errorCode = null;
        this.httpStatus = 0;
        this.userMessage = message;
    }

    public HmrcApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.httpStatus = 0;
        this.userMessage = message;
    }

    public HmrcApiException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.userMessage = message;
    }

    public HmrcApiException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.userMessage = message;
    }

    public HmrcApiException(String message, String errorCode, int httpStatus, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.userMessage = userMessage;
    }

    public HmrcApiException(String message, String errorCode, int httpStatus, String userMessage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.userMessage = userMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Indicates if this exception represents a retryable error.
     * Subclasses override to indicate retryability.
     */
    public boolean isRetryable() {
        return false;
    }
}
