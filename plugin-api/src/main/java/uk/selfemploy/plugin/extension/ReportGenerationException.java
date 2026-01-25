package uk.selfemploy.plugin.extension;

/**
 * Exception thrown when report generation fails.
 *
 * <p>This exception indicates that a report could not be generated due to
 * an error in data retrieval, formatting, or output writing.</p>
 *
 * @see ReportGenerator
 */
public class ReportGenerationException extends RuntimeException {

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message the error message
     */
    public ReportGenerationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
