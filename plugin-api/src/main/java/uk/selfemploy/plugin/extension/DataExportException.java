package uk.selfemploy.plugin.extension;

/**
 * Exception thrown when data export fails.
 *
 * <p>This exception indicates that a data export operation could not be
 * completed due to formatting errors, data access issues, or other problems.</p>
 *
 * @see DataExporter
 */
public class DataExportException extends RuntimeException {

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message the error message
     */
    public DataExportException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public DataExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
