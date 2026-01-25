package uk.selfemploy.core.hmrc;

/**
 * State of the HMRC connection.
 */
public enum ConnectionState {
    /**
     * Application credentials not configured.
     */
    NOT_CONFIGURED,

    /**
     * User not connected to HMRC.
     */
    NOT_CONNECTED,

    /**
     * Actively connected with valid tokens.
     */
    CONNECTED,

    /**
     * Access token expired but refresh token available.
     */
    EXPIRED,

    /**
     * Connection in progress (OAuth flow active).
     */
    CONNECTING,

    /**
     * Connection error occurred.
     */
    ERROR
}
