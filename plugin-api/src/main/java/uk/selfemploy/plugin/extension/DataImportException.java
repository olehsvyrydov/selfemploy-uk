package uk.selfemploy.plugin.extension;

/**
 * Exception thrown when data import fails.
 *
 * <p>This exception indicates that a data import operation could not be
 * completed due to file format errors, invalid data, or other issues.</p>
 *
 * @see DataImporter
 */
public class DataImportException extends RuntimeException {

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message the error message
     */
    public DataImportException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public DataImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
