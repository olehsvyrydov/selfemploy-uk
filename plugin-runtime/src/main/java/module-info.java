/**
 * UK Self-Employment Manager Plugin Runtime module.
 *
 * <p>This module provides the plugin loading, management, and lifecycle
 * infrastructure for the UK Self-Employment Manager application.</p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link uk.selfemploy.plugin.runtime.PluginManager} - Central lifecycle manager</li>
 *   <li>{@link uk.selfemploy.plugin.runtime.PluginLoader} - ServiceLoader-based discovery</li>
 *   <li>{@link uk.selfemploy.plugin.runtime.ExtensionRegistry} - Extension point management</li>
 *   <li>{@link uk.selfemploy.plugin.runtime.PluginRegistry} - Plugin container storage</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <p>This module requires:</p>
 * <ul>
 *   <li>{@code uk.selfemploy.plugin.api} - Plugin API interfaces</li>
 *   <li>{@code org.slf4j} - Logging</li>
 * </ul>
 *
 * @see uk.selfemploy.plugin.runtime.PluginManager
 */
module uk.selfemploy.plugin.runtime {
    // Required modules
    requires uk.selfemploy.plugin.api;
    requires org.slf4j;

    // JSON serialization for revocation list
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    // Exported packages
    exports uk.selfemploy.plugin.runtime;

    // Open to Jackson for JSON serialization of revocation list
    opens uk.selfemploy.plugin.runtime to com.fasterxml.jackson.databind;

    // ServiceLoader support for plugin discovery
    uses uk.selfemploy.plugin.api.Plugin;
}
