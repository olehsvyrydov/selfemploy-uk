package uk.selfemploy.plugin.extension;

/**
 * Exception thrown when tax calculation fails.
 *
 * <p>This exception indicates that a tax calculation could not be completed
 * due to invalid input data, missing configuration, or calculation errors.</p>
 *
 * @see TaxCalculatorExtension
 */
public class TaxCalculationException extends RuntimeException {

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message the error message
     */
    public TaxCalculationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public TaxCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
