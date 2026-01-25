package uk.selfemploy.plugin.runtime;

/**
 * Exception thrown when a requested plugin is not found in the registry.
 *
 * <p>This exception is thrown by {@link PluginManager} methods that expect
 * a plugin to exist, such as {@code getPlugin()} or {@code enablePlugin()}.</p>
 *
 * @see PluginManager
 * @see PluginRegistry
 */
public class PluginNotFoundException extends PluginException {

    private final String pluginId;

    /**
     * Constructs a new PluginNotFoundException for the specified plugin.
     *
     * @param pluginId the ID of the plugin that was not found
     */
    public PluginNotFoundException(String pluginId) {
        super("Plugin not found: " + pluginId);
        this.pluginId = pluginId;
    }

    /**
     * Returns the ID of the plugin that was not found.
     *
     * @return the plugin ID
     */
    public String getPluginId() {
        return pluginId;
    }
}
