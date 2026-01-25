package uk.selfemploy.plugin.runtime;

/**
 * Exception thrown when a plugin has version compatibility issues.
 *
 * <p>This exception is thrown when a plugin requires a different application
 * version than what is currently running. It provides detailed information
 * about the version mismatch to help diagnose and resolve the issue.</p>
 *
 * <h2>Common Scenarios</h2>
 * <ul>
 *   <li>Plugin requires a newer application version</li>
 *   <li>Plugin is built for an older, incompatible API version</li>
 *   <li>Application was downgraded but plugins remain from newer version</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * try {
 *     pluginManager.loadPlugin(pluginId);
 * } catch (PluginVersionException e) {
 *     log.warn("Plugin {} requires app version {} but current is {}",
 *         e.getPluginId(),
 *         e.getRequiredVersion(),
 *         e.getCurrentVersion());
 * }
 * }</pre>
 *
 * @see PluginLoader
 * @see PluginManager
 */
public class PluginVersionException extends PluginLoadException {

    private final String currentVersion;
    private final String requiredVersion;

    /**
     * Constructs a new PluginVersionException with version details.
     *
     * @param pluginId        the ID of the incompatible plugin
     * @param currentVersion  the current application version
     * @param requiredVersion the version required by the plugin
     */
    public PluginVersionException(String pluginId, String currentVersion, String requiredVersion) {
        super(pluginId, String.format(
            "Version incompatibility - plugin requires app version %s but current is %s",
            requiredVersion, currentVersion
        ));
        this.currentVersion = currentVersion;
        this.requiredVersion = requiredVersion;
    }

    /**
     * Constructs a new PluginVersionException with a custom message.
     *
     * @param pluginId        the ID of the incompatible plugin
     * @param currentVersion  the current application version
     * @param requiredVersion the version required by the plugin
     * @param message         a custom error message
     */
    public PluginVersionException(String pluginId, String currentVersion,
                                  String requiredVersion, String message) {
        super(pluginId, message);
        this.currentVersion = currentVersion;
        this.requiredVersion = requiredVersion;
    }

    /**
     * Returns the current application version.
     *
     * @return the current version string
     */
    public String getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Returns the version required by the plugin.
     *
     * @return the required version string
     */
    public String getRequiredVersion() {
        return requiredVersion;
    }
}
