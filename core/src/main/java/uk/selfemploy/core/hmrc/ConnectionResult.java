package uk.selfemploy.core.hmrc;

/**
 * Result of an HMRC connection attempt.
 */
public record ConnectionResult(
    boolean successful,
    String message,
    ConnectionState state
) {
    /**
     * Creates a successful connection result.
     */
    public static ConnectionResult connected() {
        return new ConnectionResult(true, "Successfully connected to HMRC", ConnectionState.CONNECTED);
    }

    /**
     * Creates a successful refresh result.
     */
    public static ConnectionResult refreshed() {
        return new ConnectionResult(true, "Successfully refreshed HMRC connection", ConnectionState.CONNECTED);
    }

    /**
     * Creates a failed connection result.
     */
    public static ConnectionResult failure(String message) {
        return new ConnectionResult(false, message, ConnectionState.ERROR);
    }

    /**
     * Creates a cancelled connection result.
     */
    public static ConnectionResult cancelled() {
        return new ConnectionResult(false, "Connection cancelled by user", ConnectionState.NOT_CONNECTED);
    }

    /**
     * Returns true if the operation was successful.
     */
    public boolean success() {
        return successful;
    }
}
