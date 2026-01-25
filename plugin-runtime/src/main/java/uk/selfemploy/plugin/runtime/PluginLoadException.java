package uk.selfemploy.plugin.runtime;

/**
 * Exception thrown when a plugin fails to load.
 *
 * <p>This exception is thrown when errors occur during plugin discovery
 * or instantiation, such as:</p>
 * <ul>
 *   <li>Plugin class not found</li>
 *   <li>Invalid plugin descriptor</li>
 *   <li>Missing dependencies</li>
 *   <li>Version incompatibility</li>
 * </ul>
 *
 * @see PluginLoader
 * @see PluginManager
 */
public class PluginLoadException extends PluginException {

    private final String pluginId;

    /**
     * Constructs a new PluginLoadException for the specified plugin.
     *
     * @param pluginId the ID of the plugin that failed to load
     * @param message  a description of the load failure
     */
    public PluginLoadException(String pluginId, String message) {
        super("Failed to load plugin '" + pluginId + "': " + message);
        this.pluginId = pluginId;
    }

    /**
     * Constructs a new PluginLoadException for the specified plugin with a cause.
     *
     * @param pluginId the ID of the plugin that failed to load
     * @param message  a description of the load failure
     * @param cause    the underlying cause of the failure
     */
    public PluginLoadException(String pluginId, String message, Throwable cause) {
        super("Failed to load plugin '" + pluginId + "': " + message, cause);
        this.pluginId = pluginId;
    }

    /**
     * Returns the ID of the plugin that failed to load.
     *
     * @return the plugin ID, or null if unknown during discovery
     */
    public String getPluginId() {
        return pluginId;
    }
}
