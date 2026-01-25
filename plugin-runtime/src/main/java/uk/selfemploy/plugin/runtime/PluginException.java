package uk.selfemploy.plugin.runtime;

/**
 * Base exception for all plugin-related errors.
 *
 * <p>This exception serves as the root of the plugin exception hierarchy,
 * allowing callers to catch all plugin-related errors with a single catch clause.</p>
 *
 * <h2>Exception Hierarchy</h2>
 * <ul>
 *   <li>{@link PluginLoadException} - Errors during plugin loading</li>
 *   <li>{@link PluginNotFoundException} - Plugin not found in registry</li>
 *   <li>{@link PluginStateException} - Invalid state transitions</li>
 *   <li>{@link PluginLifecycleException} - Errors during lifecycle methods</li>
 * </ul>
 *
 * @see PluginManager
 */
public class PluginException extends RuntimeException {

    /**
     * Constructs a new plugin exception with the specified message.
     *
     * @param message the detail message
     */
    public PluginException(String message) {
        super(message);
    }

    /**
     * Constructs a new plugin exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
