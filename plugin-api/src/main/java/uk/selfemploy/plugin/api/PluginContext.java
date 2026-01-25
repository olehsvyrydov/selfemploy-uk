package uk.selfemploy.plugin.api;

import java.nio.file.Path;
import java.util.Set;

/**
 * Runtime context provided to plugins.
 *
 * <p>The {@code PluginContext} provides plugins with access to application
 * services and resources in a controlled manner. Each plugin receives its
 * own context instance that respects the plugin's declared permissions.</p>
 *
 * <h2>Permission System</h2>
 * <p>Plugins must declare required permissions in their {@link PluginDescriptor}.
 * The context enforces these permissions at runtime. Attempting to access
 * functionality without the required permission will throw a security exception.</p>
 *
 * <h2>Context Lifecycle</h2>
 * <p>A context is created for each plugin when it is loaded and remains
 * valid until the plugin is unloaded. Plugins should not store references
 * to context beyond their own lifecycle.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this interface are thread-safe. Plugins may call
 * context methods from any thread.</p>
 *
 * <h2>Future Extensions</h2>
 * <p>This interface will be expanded in future versions to include:</p>
 * <ul>
 *   <li>Event bus for pub/sub messaging</li>
 *   <li>Plugin storage for data persistence</li>
 *   <li>Secure storage for credentials</li>
 *   <li>Read-only access to transaction data</li>
 *   <li>Extension registration</li>
 * </ul>
 *
 * @see Plugin#onLoad(PluginContext)
 * @see PluginDescriptor
 * @see PluginPermission
 */
public interface PluginContext {

    /**
     * Returns the application version string.
     *
     * <p>Plugins can use this to check runtime compatibility or
     * conditionally enable features based on the application version.</p>
     *
     * <p>The version follows semantic versioning (e.g., "0.1.0").</p>
     *
     * @return the application version, never null
     */
    String getAppVersion();

    /**
     * Returns the plugin's private data directory.
     *
     * <p>Each plugin receives an isolated directory for storing data.
     * The directory is created automatically when first accessed.
     * The typical location is:</p>
     * <pre>{@code ~/.selfemploy/plugin-data/{plugin-id}/}</pre>
     *
     * <p>Plugins may create subdirectories and files within this
     * directory. The contents are preserved across application restarts.</p>
     *
     * <h3>Usage Example</h3>
     * <pre>{@code
     * Path dataDir = context.getPluginDataDirectory();
     * Path configFile = dataDir.resolve("config.json");
     * }</pre>
     *
     * @return the plugin's data directory path, never null
     */
    Path getPluginDataDirectory();

    /**
     * Returns the set of permissions granted to this plugin.
     *
     * <p>The granted permissions are a subset of the permissions declared
     * in the plugin's descriptor, filtered by user approval.</p>
     *
     * @return unmodifiable set of granted permissions
     */
    Set<PluginPermission> getGrantedPermissions();

    /**
     * Checks if the plugin has been granted a specific permission.
     *
     * @param permission the permission to check
     * @return true if the permission has been granted
     */
    default boolean hasPermission(PluginPermission permission) {
        return getGrantedPermissions().contains(permission);
    }

    /**
     * Verifies that the plugin has the required permission, throwing
     * an exception if it does not.
     *
     * <p>This method should be called before performing any sensitive
     * operation that requires a specific permission.</p>
     *
     * <h3>Usage Example</h3>
     * <pre>{@code
     * public void submitToHmrc() {
     *     context.requirePermission(PluginPermission.HMRC_API);
     *     // Proceed with HMRC submission
     * }
     * }</pre>
     *
     * @param permission the required permission
     * @throws SecurityException if the permission has not been granted
     */
    default void requirePermission(PluginPermission permission) {
        if (!hasPermission(permission)) {
            throw new SecurityException(
                "Plugin requires permission " + permission.name() +
                " (" + permission.getDisplayName() + ") which has not been granted"
            );
        }
    }
}
