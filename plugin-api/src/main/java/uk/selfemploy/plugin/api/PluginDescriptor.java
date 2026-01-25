package uk.selfemploy.plugin.api;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Immutable record containing plugin metadata.
 *
 * <p>This record defines the essential information about a plugin that is used
 * for identification, versioning, and compatibility checks.</p>
 *
 * <h2>Required Fields</h2>
 * <ul>
 *   <li>{@code id} - Unique identifier for the plugin (e.g., "uk.selfemploy.plugin.barclays-import")</li>
 *   <li>{@code name} - Human-readable display name</li>
 *   <li>{@code version} - Semantic version string (e.g., "1.0.0")</li>
 *   <li>{@code minAppVersion} - Minimum application version required</li>
 * </ul>
 *
 * <h2>Optional Fields</h2>
 * <ul>
 *   <li>{@code description} - Plugin description (may be null)</li>
 *   <li>{@code author} - Plugin author or organization (may be null)</li>
 *   <li>{@code permissions} - Required permissions (empty set by default)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PluginDescriptor descriptor = new PluginDescriptor(
 *     "uk.selfemploy.plugin.my-plugin",
 *     "My Plugin",
 *     "1.0.0",
 *     "A plugin that does something useful",
 *     "Author Name",
 *     "0.1.0",
 *     Set.of(PluginPermission.DATA_READ, PluginPermission.UI_EXTENSION)
 * );
 * }</pre>
 *
 * @param id            unique plugin identifier, must not be null or blank
 * @param name          human-readable plugin name, must not be null or blank
 * @param version       semantic version string, must not be null or blank
 * @param description   optional description of the plugin (may be null)
 * @param author        optional author or organization name (may be null)
 * @param minAppVersion minimum required application version, must not be null or blank
 * @param permissions   permissions required by this plugin (may be null, treated as empty)
 *
 * @see Plugin
 * @see PluginContext
 * @see PluginPermission
 */
public record PluginDescriptor(
    String id,
    String name,
    String version,
    String description,
    String author,
    String minAppVersion,
    Set<PluginPermission> permissions
) {

    /**
     * Constructs a new PluginDescriptor with validation.
     *
     * @param id            unique plugin identifier, must not be null or blank
     * @param name          human-readable plugin name, must not be null or blank
     * @param version       semantic version string, must not be null or blank
     * @param description   optional description of the plugin (may be null)
     * @param author        optional author or organization name (may be null)
     * @param minAppVersion minimum required application version, must not be null or blank
     * @param permissions   permissions required by this plugin (may be null, treated as empty)
     * @throws IllegalArgumentException if required fields are null or blank
     */
    public PluginDescriptor {
        if (isBlank(id)) {
            throw new IllegalArgumentException("Plugin id must not be null or blank");
        }
        if (isBlank(name)) {
            throw new IllegalArgumentException("Plugin name must not be null or blank");
        }
        if (isBlank(version)) {
            throw new IllegalArgumentException("Plugin version must not be null or blank");
        }
        if (isBlank(minAppVersion)) {
            throw new IllegalArgumentException("Plugin minAppVersion must not be null or blank");
        }
        // Normalize permissions to immutable set
        if (permissions == null || permissions.isEmpty()) {
            permissions = Collections.emptySet();
        } else {
            permissions = Collections.unmodifiableSet(EnumSet.copyOf(permissions));
        }
    }

    /**
     * Constructs a new PluginDescriptor without permissions (for backward compatibility).
     *
     * @param id            unique plugin identifier, must not be null or blank
     * @param name          human-readable plugin name, must not be null or blank
     * @param version       semantic version string, must not be null or blank
     * @param description   optional description of the plugin (may be null)
     * @param author        optional author or organization name (may be null)
     * @param minAppVersion minimum required application version, must not be null or blank
     */
    public PluginDescriptor(
        String id,
        String name,
        String version,
        String description,
        String author,
        String minAppVersion
    ) {
        this(id, name, version, description, author, minAppVersion, Collections.emptySet());
    }

    /**
     * Checks if the plugin has requested the specified permission.
     *
     * @param permission the permission to check
     * @return true if the plugin has requested this permission
     */
    public boolean hasPermission(PluginPermission permission) {
        return permissions.contains(permission);
    }

    /**
     * Checks if the plugin has requested any high-sensitivity permissions.
     *
     * @return true if the plugin requests any HIGH sensitivity permissions
     */
    public boolean hasHighSensitivityPermissions() {
        for (PluginPermission permission : permissions) {
            if (permission.getSensitivity() == PluginPermission.Sensitivity.HIGH) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a string is null, empty, or contains only whitespace.
     *
     * @param str the string to check
     * @return true if the string is blank, false otherwise
     */
    private static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
}
