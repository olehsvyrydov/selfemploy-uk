package uk.selfemploy.core.hmrc;

import java.time.Instant;

/**
 * Current status of the HMRC connection.
 */
public record ConnectionStatus(
    ConnectionState state,
    String message,
    Instant expiryTime,
    boolean canRefresh
) {
    /**
     * Creates a connected status.
     */
    public static ConnectionStatus connected(Instant expiryTime) {
        return new ConnectionStatus(
            ConnectionState.CONNECTED,
            "Connected to HMRC",
            expiryTime,
            true
        );
    }

    /**
     * Creates a not connected status.
     */
    public static ConnectionStatus notConnected() {
        return new ConnectionStatus(
            ConnectionState.NOT_CONNECTED,
            "Not connected to HMRC. Click 'Connect' to authorize.",
            null,
            false
        );
    }

    /**
     * Creates an expired status.
     */
    public static ConnectionStatus expired() {
        return new ConnectionStatus(
            ConnectionState.EXPIRED,
            "HMRC connection expired. Click 'Refresh' to reconnect.",
            null,
            true
        );
    }

    /**
     * Creates a not configured status.
     */
    public static ConnectionStatus notConfigured() {
        return new ConnectionStatus(
            ConnectionState.NOT_CONFIGURED,
            "HMRC API credentials not configured. Contact support.",
            null,
            false
        );
    }

    /**
     * Creates an error status.
     */
    public static ConnectionStatus error(String errorMessage) {
        return new ConnectionStatus(
            ConnectionState.ERROR,
            errorMessage,
            null,
            false
        );
    }

    /**
     * Creates a connecting status.
     */
    public static ConnectionStatus connecting() {
        return new ConnectionStatus(
            ConnectionState.CONNECTING,
            "Connecting to HMRC...",
            null,
            false
        );
    }

    /**
     * Returns true if currently connected with valid tokens.
     */
    public boolean isConnected() {
        return state == ConnectionState.CONNECTED;
    }
}
