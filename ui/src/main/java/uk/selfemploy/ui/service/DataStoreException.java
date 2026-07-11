package uk.selfemploy.ui.service;

/**
 * Unchecked exception signalling that a data-store operation failed.
 *
 * <p>Persistence failures used to be swallowed and logged, which let the UI report
 * a successful save when nothing was written. Throwing this instead lets callers
 * surface the failure to the user rather than silently discarding their data.
 */
public class DataStoreException extends RuntimeException {

    public DataStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
