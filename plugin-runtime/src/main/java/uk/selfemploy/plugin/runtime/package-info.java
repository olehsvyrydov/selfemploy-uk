/**
 * Plugin runtime infrastructure for the UK Self-Employment Manager.
 *
 * <p>This package provides the core components for loading, managing,
 * and coordinating plugins within the application.</p>
 *
 * <h2>Core Components</h2>
 *
 * <h3>Plugin Management</h3>
 * <ul>
 *   <li>{@link uk.selfemploy.plugin.runtime.PluginManager} - Central manager for plugin lifecycle</li>
 *   <li>{@link uk.selfemploy.plugin.runtime.PluginLoader} - ServiceLoader-based plugin discovery</li>
 *   <li>{@link uk.selfemploy.plugin.runtime.PluginRegistry} - Storage and lookup for plugins</li>
 * </ul>
 *
 * <h3>Plugin State</h3>
 * <ul>
 *   <li>{@link uk.selfemploy.plugin.runtime.PluginState} - Lifecycle state enumeration</li>
 *   <li>{@link uk.selfemploy.plugin.runtime.PluginContainer} - Plugin instance wrapper with state</li>
 *   <li>{@link uk.selfemploy.plugin.runtime.PluginContextImpl} - PluginContext implementation</li>
 * </ul>
 *
 * <h3>Extension Points</h3>
 * <ul>
 *   <li>{@link uk.selfemploy.plugin.runtime.ExtensionRegistry} - Registry for extension implementations</li>
 * </ul>
 *
 * <h3>Exceptions</h3>
 * <ul>
 *   <li>{@link uk.selfemploy.plugin.runtime.PluginException} - Base exception class</li>
 *   <li>{@link uk.selfemploy.plugin.runtime.PluginNotFoundException} - Plugin not in registry</li>
 *   <li>{@link uk.selfemploy.plugin.runtime.PluginLoadException} - Loading failures</li>
 *   <li>{@link uk.selfemploy.plugin.runtime.PluginStateException} - Invalid state transitions</li>
 *   <li>{@link uk.selfemploy.plugin.runtime.PluginLifecycleException} - Lifecycle method errors</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create and initialize the plugin manager
 * PluginManager manager = PluginManager.builder()
 *     .appVersion("0.1.0")
 *     .pluginDataDirectory(Paths.get("~/.selfemploy/plugin-data"))
 *     .build();
 *
 * manager.initialize();
 *
 * // Enable desired plugins
 * for (PluginContainer plugin : manager.getLoadedPlugins()) {
 *     manager.enablePlugin(plugin.getId());
 * }
 *
 * // Use extensions
 * List<DashboardWidget> widgets = manager.getExtensions(DashboardWidget.class);
 *
 * // Shutdown when done
 * manager.shutdown();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All classes in this package are designed to be thread-safe. Concurrent
 * access from multiple threads is fully supported.</p>
 *
 * @see uk.selfemploy.plugin.api
 * @see uk.selfemploy.plugin.extension
 */
package uk.selfemploy.plugin.runtime;
