package uk.selfemploy.core.export;

/**
 * Exception thrown when data import fails.
 */
public class ImportException extends RuntimeException {

    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
