package uk.selfemploy.core.bankimport;

/**
 * Thrown when a {@link StatementSource} cannot read or parse its data — for example a
 * CSV whose format no parser recognises, or (later) an Open Banking feed that fails to
 * authenticate. Checked, so callers must decide how to surface the failure.
 */
public class StatementSourceException extends Exception {

    public StatementSourceException(String message) {
        super(message);
    }

    public StatementSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
