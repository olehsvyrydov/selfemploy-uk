package uk.selfemploy.plugin.api;

/**
 * Core interface that all plugins must implement.
 *
 * <p>Plugins are discovered via Java's {@link java.util.ServiceLoader} mechanism
 * at application startup. Each plugin JAR should include a service provider
 * configuration file at {@code META-INF/services/uk.selfemploy.plugin.api.Plugin}
 * listing the implementation class.</p>
 *
 * <h2>Lifecycle</h2>
 * <p>Plugins go through the following lifecycle:</p>
 * <ol>
 *   <li><b>Discovery</b> - Plugin JAR found in plugins directory</li>
 *   <li><b>Load</b> - {@link #onLoad(PluginContext)} called with context</li>
 *   <li><b>Enable</b> - Plugin becomes active (future enhancement)</li>
 *   <li><b>Disable</b> - Plugin becomes inactive (future enhancement)</li>
 *   <li><b>Unload</b> - {@link #onUnload()} called for cleanup</li>
 * </ol>
 *
 * <h2>Implementation Example</h2>
 * <pre>{@code
 * public class MyPlugin implements Plugin {
 *     private PluginContext context;
 *
 *     @Override
 *     public PluginDescriptor getDescriptor() {
 *         return new PluginDescriptor(
 *             "uk.selfemploy.plugin.my-plugin",
 *             "My Plugin",
 *             "1.0.0",
 *             "A sample plugin",
 *             "Author",
 *             "0.1.0"
 *         );
 *     }
 *
 *     @Override
 *     public void onLoad(PluginContext context) {
 *         this.context = context;
 *         // Initialize resources
 *     }
 *
 *     @Override
 *     public void onUnload() {
 *         // Cleanup resources
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Plugin lifecycle methods are called from the main application thread.
 * Plugins must be thread-safe if they perform background operations.</p>
 *
 * @see PluginDescriptor
 * @see PluginContext
 */
public interface Plugin {

    /**
     * Returns the plugin descriptor containing metadata.
     *
     * <p>This method should return a consistent descriptor throughout
     * the plugin's lifecycle. The descriptor is used for:</p>
     * <ul>
     *   <li>Plugin identification and display in the UI</li>
     *   <li>Version compatibility checks</li>
     *   <li>Dependency resolution</li>
     * </ul>
     *
     * @return the plugin descriptor, never null
     */
    PluginDescriptor getDescriptor();

    /**
     * Called when the plugin is loaded.
     *
     * <p>This method is invoked after the plugin class is instantiated
     * and before any extensions are registered. Use this method to:</p>
     * <ul>
     *   <li>Store the context reference for later use</li>
     *   <li>Initialize plugin resources</li>
     *   <li>Read configuration</li>
     *   <li>Set up logging</li>
     * </ul>
     *
     * <p>If this method throws an exception, the plugin will not be
     * enabled and will be marked as failed.</p>
     *
     * @param context the plugin context providing access to application services
     * @throws RuntimeException if plugin initialization fails
     */
    void onLoad(PluginContext context);

    /**
     * Called when the plugin is being unloaded.
     *
     * <p>This method is invoked when the plugin is being removed or
     * when the application is shutting down. Use this method to:</p>
     * <ul>
     *   <li>Release resources (file handles, network connections)</li>
     *   <li>Save any unsaved state</li>
     *   <li>Cancel background tasks</li>
     *   <li>Unregister event handlers</li>
     * </ul>
     *
     * <p>This method should not throw exceptions. If cleanup fails,
     * log the error and continue.</p>
     */
    void onUnload();
}
