package uk.selfemploy.plugin.runtime;

/**
 * Exception thrown when a plugin fails security validation.
 *
 * <p>This exception is thrown when security-related errors occur, such as:</p>
 * <ul>
 *   <li>Plugin JAR signature verification failure</li>
 *   <li>Unsigned plugin attempting to access protected APIs</li>
 *   <li>Plugin from untrusted publisher</li>
 *   <li>Permission denied for requested operation</li>
 * </ul>
 *
 * <h2>Security Violation Types</h2>
 * <p>The {@link SecurityViolationType} enum provides specific categorization
 * of the security violation, enabling callers to handle different cases
 * appropriately.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * try {
 *     pluginLoader.loadPlugin(jarPath);
 * } catch (PluginSecurityException e) {
 *     if (e.getViolationType() == SecurityViolationType.UNSIGNED_PLUGIN) {
 *         // Prompt user to allow unsigned plugins
 *     } else {
 *         // Report security issue
 *     }
 * }
 * }</pre>
 *
 * @see PluginLoader
 * @see PluginManager
 */
public class PluginSecurityException extends PluginException {

    /**
     * Types of security violations that can occur during plugin operations.
     */
    public enum SecurityViolationType {
        /** The plugin JAR signature is invalid or corrupted */
        INVALID_SIGNATURE,

        /** The plugin JAR is not signed */
        UNSIGNED_PLUGIN,

        /** The plugin is signed by an unknown or untrusted publisher */
        UNTRUSTED_PUBLISHER,

        /** The plugin requested a permission that was not granted */
        PERMISSION_DENIED,

        /** The plugin attempted to access a restricted API */
        RESTRICTED_API_ACCESS,

        /** General security violation */
        GENERAL
    }

    private final String pluginId;
    private final SecurityViolationType violationType;

    /**
     * Constructs a new PluginSecurityException for the specified plugin.
     *
     * @param pluginId the ID of the plugin that failed security validation
     * @param message  a description of the security failure
     */
    public PluginSecurityException(String pluginId, String message) {
        super("Security violation for plugin '" + pluginId + "': " + message);
        this.pluginId = pluginId;
        this.violationType = SecurityViolationType.GENERAL;
    }

    /**
     * Constructs a new PluginSecurityException for the specified plugin with a cause.
     *
     * @param pluginId the ID of the plugin that failed security validation
     * @param message  a description of the security failure
     * @param cause    the underlying cause of the failure
     */
    public PluginSecurityException(String pluginId, String message, Throwable cause) {
        super("Security violation for plugin '" + pluginId + "': " + message, cause);
        this.pluginId = pluginId;
        this.violationType = SecurityViolationType.GENERAL;
    }

    /**
     * Constructs a new PluginSecurityException with a specific violation type.
     *
     * @param pluginId      the ID of the plugin that failed security validation
     * @param violationType the type of security violation
     * @param details       additional details about the violation
     */
    public PluginSecurityException(String pluginId, SecurityViolationType violationType, String details) {
        super("Security violation for plugin '" + pluginId + "' [" + violationType + "]: " + details);
        this.pluginId = pluginId;
        this.violationType = violationType;
    }

    /**
     * Constructs a new PluginSecurityException with a specific violation type and cause.
     *
     * @param pluginId      the ID of the plugin that failed security validation
     * @param violationType the type of security violation
     * @param details       additional details about the violation
     * @param cause         the underlying cause of the failure
     */
    public PluginSecurityException(String pluginId, SecurityViolationType violationType,
                                   String details, Throwable cause) {
        super("Security violation for plugin '" + pluginId + "' [" + violationType + "]: " + details, cause);
        this.pluginId = pluginId;
        this.violationType = violationType;
    }

    /**
     * Returns the ID of the plugin that failed security validation.
     *
     * @return the plugin ID, never null
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Returns the type of security violation that occurred.
     *
     * @return the violation type, never null
     */
    public SecurityViolationType getViolationType() {
        return violationType;
    }
}
