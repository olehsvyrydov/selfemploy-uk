package uk.selfemploy.hmrc.exception;

/**
 * Exception for network/timeout errors when communicating with HMRC.
 * These errors ARE retryable as they indicate temporary network issues.
 */
public class HmrcNetworkException extends HmrcApiException {

    private static final String TIMEOUT_MESSAGE = "HMRC is taking too long to respond. Please try again in a few minutes.";
    private static final String CONNECTION_MESSAGE = "Unable to reach HMRC. Please check your internet connection and try again.";

    public HmrcNetworkException(String message) {
        super(message, null, 0, message);
    }

    public HmrcNetworkException(String message, Throwable cause) {
        super(message, null, 0, message, cause);
    }

    public HmrcNetworkException(String message, String userMessage) {
        super(message, null, 0, userMessage);
    }

    public HmrcNetworkException(String message, String userMessage, Throwable cause) {
        super(message, null, 0, userMessage, cause);
    }

    @Override
    public boolean isRetryable() {
        return true;
    }

    /**
     * Creates a timeout exception with user-friendly message.
     */
    public static HmrcNetworkException timeout() {
        return new HmrcNetworkException("Request to HMRC timed out", TIMEOUT_MESSAGE);
    }

    /**
     * Creates a timeout exception with cause.
     */
    public static HmrcNetworkException timeout(Throwable cause) {
        return new HmrcNetworkException("Request to HMRC timed out", TIMEOUT_MESSAGE, cause);
    }

    /**
     * Creates a connection failed exception with user-friendly message.
     */
    public static HmrcNetworkException connectionFailed() {
        return new HmrcNetworkException("Failed to connect to HMRC", CONNECTION_MESSAGE);
    }

    /**
     * Creates a connection failed exception with cause.
     */
    public static HmrcNetworkException connectionFailed(Throwable cause) {
        return new HmrcNetworkException("Failed to connect to HMRC", CONNECTION_MESSAGE, cause);
    }
}
