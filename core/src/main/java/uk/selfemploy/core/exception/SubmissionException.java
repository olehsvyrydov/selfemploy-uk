package uk.selfemploy.core.exception;

/**
 * Exception thrown when an HMRC submission fails.
 */
public class SubmissionException extends RuntimeException {

    private final boolean retryable;

    public SubmissionException(String message) {
        super(message);
        this.retryable = false;
    }

    public SubmissionException(String message, Throwable cause) {
        super(message, cause);
        this.retryable = false;
    }

    public SubmissionException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    /**
     * Returns true if the submission can be retried.
     */
    public boolean isRetryable() {
        return retryable;
    }
}
