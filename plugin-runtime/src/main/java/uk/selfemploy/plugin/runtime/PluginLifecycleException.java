package uk.selfemploy.plugin.runtime;

/**
 * Exception thrown when a plugin lifecycle method fails.
 *
 * <p>This exception wraps errors that occur during plugin lifecycle
 * method invocations, such as:</p>
 * <ul>
 *   <li>{@code onLoad()} - During plugin initialization</li>
 *   <li>{@code onEnable()} - When enabling the plugin</li>
 *   <li>{@code onDisable()} - When disabling the plugin</li>
 *   <li>{@code onUnload()} - During plugin cleanup</li>
 * </ul>
 *
 * @see uk.selfemploy.plugin.api.Plugin
 * @see PluginManager
 */
public class PluginLifecycleException extends PluginException {

    private final String pluginId;
    private final String lifecycleMethod;

    /**
     * Constructs a new PluginLifecycleException.
     *
     * @param pluginId        the ID of the plugin
     * @param lifecycleMethod the name of the lifecycle method that failed
     * @param cause           the underlying cause of the failure
     */
    public PluginLifecycleException(String pluginId, String lifecycleMethod, Throwable cause) {
        super(String.format(
            "Plugin '%s' failed during %s",
            pluginId, lifecycleMethod
        ), cause);
        this.pluginId = pluginId;
        this.lifecycleMethod = lifecycleMethod;
    }

    /**
     * Returns the ID of the plugin.
     *
     * @return the plugin ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Returns the name of the lifecycle method that failed.
     *
     * @return the lifecycle method name (e.g., "onLoad", "onEnable")
     */
    public String getLifecycleMethod() {
        return lifecycleMethod;
    }
}
