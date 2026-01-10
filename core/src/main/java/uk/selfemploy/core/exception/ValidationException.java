package uk.selfemploy.core.exception;

/**
 * Exception thrown when validation fails in service layer.
 */
public class ValidationException extends RuntimeException {

    private final String field;

    public ValidationException(String message) {
        super(message);
        this.field = null;
    }

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.field = null;
    }

    public String getField() {
        return field;
    }
}
