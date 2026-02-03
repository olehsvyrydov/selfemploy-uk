package uk.selfemploy.plugin.api;

import java.util.EnumSet;
import java.util.Set;

/**
 * Permissions that plugins can request for accessing sensitive functionality.
 *
 * <p>The permission system provides fine-grained control over what plugins
 * can do. Plugins declare their required permissions in their descriptor,
 * and the runtime enforces these permissions at execution time.</p>
 *
 * <h2>Permission Declaration</h2>
 * <p>Plugins declare permissions in their {@link PluginDescriptor}:</p>
 * <pre>{@code
 * @Override
 * public PluginDescriptor getDescriptor() {
 *     return new PluginDescriptor(
 *         "my-plugin",
 *         "My Plugin",
 *         "1.0.0",
 *         "Description",
 *         "Author",
 *         "1.0.0",
 *         Set.of(PluginPermission.FILE_ACCESS, PluginPermission.DATA_READ)
 *     );
 * }
 * }</pre>
 *
 * <h2>Sensitivity Levels</h2>
 * <p>Permissions are categorized by sensitivity:</p>
 * <ul>
 *   <li><b>LOW</b> - UI modifications, non-sensitive data access</li>
 *   <li><b>MEDIUM</b> - Data modification, settings changes</li>
 *   <li><b>HIGH</b> - Financial APIs, system-wide effects</li>
 * </ul>
 *
 * <p>HIGH sensitivity permissions may require additional user confirmation
 * and plugin signing.</p>
 *
 * @see PluginDescriptor
 * @see PluginContext
 */
public enum PluginPermission {

    /**
     * Permission to access the file system beyond the plugin's data directory.
     *
     * <p>Without this permission, plugins can only read/write to their
     * designated data directory returned by
     * {@link PluginContext#getPluginDataDirectory()}.</p>
     */
    FILE_ACCESS(
        "File System Access",
        "Allows the plugin to read and write files outside its designated data directory",
        Sensitivity.MEDIUM
    ),

    /**
     * Permission to make network connections.
     *
     * <p>Required for plugins that need to call external APIs or services
     * other than the built-in HMRC integration.</p>
     */
    NETWORK_ACCESS(
        "Network Access",
        "Allows the plugin to make network connections to external services",
        Sensitivity.MEDIUM
    ),

    /**
     * Permission to interact with HMRC APIs.
     *
     * <p>This is a high-sensitivity permission that allows plugins to
     * submit data to HMRC or access user's HMRC account information.
     * Plugins requesting this permission must be signed.</p>
     */
    HMRC_API(
        "HMRC API Access",
        "Allows the plugin to interact with HMRC Making Tax Digital APIs",
        Sensitivity.HIGH
    ),

    /**
     * Permission to read transaction data (income, expenses).
     *
     * <p>Required for plugins that need to analyze or export user's
     * financial data.</p>
     */
    DATA_READ(
        "Read Transaction Data",
        "Allows the plugin to read income and expense records",
        Sensitivity.LOW
    ),

    /**
     * Permission to write transaction data (income, expenses).
     *
     * <p>Required for plugins that import data or create transactions
     * on behalf of the user.</p>
     */
    DATA_WRITE(
        "Write Transaction Data",
        "Allows the plugin to create, modify, or delete income and expense records",
        Sensitivity.MEDIUM
    ),

    /**
     * Permission to read user settings and preferences.
     */
    SETTINGS_READ(
        "Read User Settings",
        "Allows the plugin to read application settings and user preferences",
        Sensitivity.LOW
    ),

    /**
     * Permission to modify user settings and preferences.
     */
    SETTINGS_WRITE(
        "Write User Settings",
        "Allows the plugin to modify application settings and user preferences",
        Sensitivity.MEDIUM
    ),

    /**
     * Permission to extend the user interface.
     *
     * <p>This is a basic permission that allows plugins to add navigation
     * items, dashboard widgets, and other UI elements.</p>
     */
    UI_EXTENSION(
        "UI Extension",
        "Allows the plugin to add navigation items, widgets, and UI elements",
        Sensitivity.LOW
    ),

    /**
     * Permission to execute system commands.
     *
     * <p>This is a very high-risk permission that allows plugins to
     * execute external programs. Requires explicit user approval.</p>
     */
    SYSTEM_EXEC(
        "System Command Execution",
        "Allows the plugin to execute external system commands",
        Sensitivity.HIGH
    ),

    /**
     * Permission to access the clipboard.
     *
     * <p>Required for plugins that need to copy or paste data.</p>
     */
    CLIPBOARD_ACCESS(
        "Clipboard Access",
        "Allows the plugin to read from and write to the system clipboard",
        Sensitivity.LOW
    ),

    /**
     * Permission to provide services to other plugins.
     *
     * <p>Required for plugins that want to register services with the
     * {@link ServiceRegistry}. This allows the plugin to expose functionality
     * that other plugins can consume.</p>
     *
     * <p>Consuming services (via {@link ServiceRegistry#getService} or
     * {@link ServiceRegistry#getServices}) does not require this permission.</p>
     *
     * @see ServiceRegistry
     * @see PluginService
     */
    SERVICE_PROVIDER(
        "Service Provider",
        "Allows the plugin to register services that other plugins can consume",
        Sensitivity.MEDIUM
    );

    /**
     * Sensitivity level of a permission.
     */
    public enum Sensitivity {
        /** Low risk, minimal user impact */
        LOW,
        /** Medium risk, user data affected */
        MEDIUM,
        /** High risk, financial or system impact */
        HIGH
    }

    private final String displayName;
    private final String description;
    private final Sensitivity sensitivity;

    /**
     * Creates a permission with the specified properties.
     *
     * @param displayName the human-readable permission name
     * @param description the description of what this permission allows
     * @param sensitivity the sensitivity level
     */
    PluginPermission(String displayName, String description, Sensitivity sensitivity) {
        this.displayName = displayName;
        this.description = description;
        this.sensitivity = sensitivity;
    }

    /**
     * Returns the human-readable display name for this permission.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a detailed description of what this permission allows.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the sensitivity level of this permission.
     *
     * @return the sensitivity level
     */
    public Sensitivity getSensitivity() {
        return sensitivity;
    }

    /**
     * Returns all permissions with the specified sensitivity level.
     *
     * @param sensitivity the sensitivity level to filter by
     * @return the set of matching permissions
     */
    public static Set<PluginPermission> getPermissionsBySensitivity(Sensitivity sensitivity) {
        EnumSet<PluginPermission> result = EnumSet.noneOf(PluginPermission.class);
        for (PluginPermission permission : values()) {
            if (permission.getSensitivity() == sensitivity) {
                result.add(permission);
            }
        }
        return result;
    }

    /**
     * Returns the set of all high-sensitivity permissions.
     *
     * <p>These permissions require additional scrutiny and may require
     * the plugin to be signed.</p>
     *
     * @return the set of high-sensitivity permissions
     */
    public static Set<PluginPermission> getHighSensitivityPermissions() {
        return getPermissionsBySensitivity(Sensitivity.HIGH);
    }

    @Override
    public String toString() {
        return displayName + " (" + name() + ")";
    }
}
