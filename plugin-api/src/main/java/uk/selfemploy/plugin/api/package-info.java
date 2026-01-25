/**
 * Core plugin API interfaces and types.
 *
 * <p>This package contains the essential interfaces that plugins must implement
 * or interact with:</p>
 *
 * <ul>
 *   <li>{@link uk.selfemploy.plugin.api.Plugin} - Core interface all plugins implement</li>
 *   <li>{@link uk.selfemploy.plugin.api.PluginContext} - Runtime context provided to plugins</li>
 *   <li>{@link uk.selfemploy.plugin.api.PluginDescriptor} - Plugin metadata record</li>
 * </ul>
 *
 * <h2>Plugin Development</h2>
 * <p>To create a plugin, implement the {@link uk.selfemploy.plugin.api.Plugin}
 * interface and provide a {@link uk.selfemploy.plugin.api.PluginDescriptor}
 * with your plugin's metadata.</p>
 *
 * <h2>ServiceLoader Integration</h2>
 * <p>Plugins are discovered via Java's {@link java.util.ServiceLoader}.
 * Add a file at {@code META-INF/services/uk.selfemploy.plugin.api.Plugin}
 * containing your plugin class name.</p>
 *
 * @see uk.selfemploy.plugin.extension
 */
package uk.selfemploy.plugin.api;
