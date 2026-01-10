package uk.selfemploy.hmrc.exception;

/**
 * Represents a single validation error from HMRC.
 *
 * @param field   The field that failed validation
 * @param message The validation error message
 */
public record ValidationError(String field, String message) {

    public ValidationError {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Validation error message cannot be null or blank");
        }
    }

    /**
     * Creates a ValidationError for a general error (not field-specific).
     */
    public static ValidationError general(String message) {
        return new ValidationError("", message);
    }
}
