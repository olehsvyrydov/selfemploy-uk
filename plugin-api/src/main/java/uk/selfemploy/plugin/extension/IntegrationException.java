package uk.selfemploy.plugin.extension;

/**
 * Exception thrown when an integration operation fails.
 *
 * <p>This exception indicates that connecting to, synchronizing with, or
 * otherwise interacting with an external service has failed.</p>
 *
 * @see IntegrationExtension
 */
public class IntegrationException extends RuntimeException {

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message the error message
     */
    public IntegrationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public IntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
