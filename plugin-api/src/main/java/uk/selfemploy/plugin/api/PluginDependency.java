package uk.selfemploy.plugin.api;

/**
 * Represents a dependency on another plugin.
 *
 * <p>Plugins can declare dependencies on other plugins using semantic version ranges.
 * The plugin system will ensure dependencies are loaded before dependents and
 * block plugins whose required dependencies are not available.</p>
 *
 * <h2>Version Range Formats</h2>
 * <ul>
 *   <li>{@code 1.0.0} - Exact version match</li>
 *   <li>{@code ^1.0.0} - Compatible with 1.x.x (>=1.0.0 <2.0.0)</li>
 *   <li>{@code ~1.2.0} - Patch updates only (>=1.2.0 <1.3.0)</li>
 *   <li>{@code >=1.0.0 <2.0.0} - Explicit range</li>
 *   <li>{@code >=1.0.0} - Minimum version</li>
 *   <li>{@code <2.0.0} - Maximum version</li>
 * </ul>
 *
 * <h2>Example Usage in plugin.yaml</h2>
 * <pre>
 * dependencies:
 *   - pluginId: uk.selfemploy.plugin.base
 *     versionRange: "^1.0.0"
 *     optional: false
 *   - pluginId: uk.selfemploy.plugin.extra
 *     versionRange: ">=2.0.0"
 *     optional: true
 * </pre>
 *
 * @param pluginId     the unique identifier of the dependency plugin
 * @param versionRange the semantic version range required
 * @param optional     true if the dependency is optional (warning if missing),
 *                     false if required (blocks loading if missing)
 *
 * @see PluginDescriptor
 */
public record PluginDependency(
    String pluginId,
    String versionRange,
    boolean optional
) {

    /**
     * Constructs a new PluginDependency with validation.
     *
     * @throws IllegalArgumentException if pluginId or versionRange is null or blank
     */
    public PluginDependency {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("pluginId must not be null or blank");
        }
        if (versionRange == null || versionRange.isBlank()) {
            throw new IllegalArgumentException("versionRange must not be null or blank");
        }
    }

    /**
     * Returns true if this dependency is required (not optional).
     *
     * <p>Required dependencies will cause the dependent plugin to be BLOCKED
     * if they are not available or version requirements are not met.</p>
     *
     * @return true if required, false if optional
     */
    public boolean isRequired() {
        return !optional;
    }
}
