package uk.selfemploy.plugin.extension;

/**
 * Exception thrown when an HMRC API operation fails.
 *
 * <p>This exception indicates that communication with HMRC APIs has failed
 * due to network errors, authentication issues, validation failures, or
 * other API-related problems.</p>
 *
 * @see HmrcApiExtension
 */
public class HmrcApiException extends RuntimeException {

    private final String errorCode;
    private final String correlationId;

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message the error message
     */
    public HmrcApiException(String message) {
        super(message);
        this.errorCode = null;
        this.correlationId = null;
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public HmrcApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.correlationId = null;
    }

    /**
     * Constructs a new exception with HMRC error details.
     *
     * @param message       the error message
     * @param errorCode     the HMRC error code
     * @param correlationId the HMRC correlation ID for tracking
     */
    public HmrcApiException(String message, String errorCode, String correlationId) {
        super(message);
        this.errorCode = errorCode;
        this.correlationId = correlationId;
    }

    /**
     * Constructs a new exception with HMRC error details and cause.
     *
     * @param message       the error message
     * @param errorCode     the HMRC error code
     * @param correlationId the HMRC correlation ID for tracking
     * @param cause         the underlying cause
     */
    public HmrcApiException(String message, String errorCode, String correlationId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.correlationId = correlationId;
    }

    /**
     * Returns the HMRC error code, if available.
     *
     * @return the error code, or null if not available
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the HMRC correlation ID for tracking, if available.
     *
     * @return the correlation ID, or null if not available
     */
    public String getCorrelationId() {
        return correlationId;
    }
}
