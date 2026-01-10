package uk.selfemploy.hmrc.exception;

/**
 * Base exception for all HMRC API errors.
 */
public class HmrcApiException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public HmrcApiException(String message) {
        super(message);
        this.errorCode = null;
        this.httpStatus = 0;
    }

    public HmrcApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.httpStatus = 0;
    }

    public HmrcApiException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public HmrcApiException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
