package uk.selfemploy.plugin.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.plugin.api.Plugin;
import uk.selfemploy.plugin.api.PluginContext;
import uk.selfemploy.plugin.extension.ExtensionPoint;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central manager for the plugin system lifecycle.
 *
 * <p>The PluginManager is responsible for:</p>
 * <ul>
 *   <li>Initializing the plugin system</li>
 *   <li>Discovering and loading plugins</li>
 *   <li>Managing plugin lifecycle (enable/disable)</li>
 *   <li>Providing access to extension points</li>
 *   <li>Shutting down the plugin system</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <p>The typical lifecycle is:</p>
 * <ol>
 *   <li>Create PluginManager with configuration</li>
 *   <li>Call {@link #initialize()} to discover and load plugins</li>
 *   <li>Enable desired plugins with {@link #enablePlugin(String)}</li>
 *   <li>Access extensions via {@link #getExtensions(Class)}</li>
 *   <li>Call {@link #shutdown()} when application exits</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is fully thread-safe. All state modifications are synchronized
 * and concurrent access is supported.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PluginManager manager = PluginManager.builder()
 *     .appVersion("0.1.0")
 *     .pluginDataDirectory(Paths.get("~/.selfemploy/plugin-data"))
 *     .build();
 *
 * manager.initialize();
 *
 * // Enable all loaded plugins
 * for (PluginContainer plugin : manager.getLoadedPlugins()) {
 *     manager.enablePlugin(plugin.getId());
 * }
 *
 * // Get dashboard widgets from all plugins
 * List<DashboardWidget> widgets = manager.getExtensions(DashboardWidget.class);
 *
 * // Shutdown when done
 * manager.shutdown();
 * }</pre>
 *
 * @see Plugin
 * @see PluginLoader
 * @see ExtensionRegistry
 */
public class PluginManager {

    private static final Logger LOG = LoggerFactory.getLogger(PluginManager.class);

    private final String appVersion;
    private final Path pluginDataDirectory;
    private final PluginLoader loader;
    private final PluginRegistry registry;
    private final ExtensionRegistry extensionRegistry;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    /**
     * Creates a new PluginManager.
     *
     * @param appVersion          the application version for compatibility checks
     * @param pluginDataDirectory the base directory for plugin data storage
     */
    public PluginManager(String appVersion, Path pluginDataDirectory) {
        this(appVersion, pluginDataDirectory, new PluginLoader(), new PluginRegistry(), new ExtensionRegistry());
    }

    /**
     * Creates a new PluginManager with custom components (for testing).
     *
     * @param appVersion          the application version
     * @param pluginDataDirectory the base directory for plugin data
     * @param loader              the plugin loader
     * @param registry            the plugin registry
     * @param extensionRegistry   the extension registry
     */
    PluginManager(String appVersion, Path pluginDataDirectory,
                  PluginLoader loader, PluginRegistry registry,
                  ExtensionRegistry extensionRegistry) {
        if (appVersion == null || appVersion.isBlank()) {
            throw new IllegalArgumentException("appVersion must not be null or blank");
        }
        this.appVersion = appVersion;
        this.pluginDataDirectory = Objects.requireNonNull(pluginDataDirectory,
            "pluginDataDirectory must not be null");
        this.loader = Objects.requireNonNull(loader, "loader must not be null");
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.extensionRegistry = Objects.requireNonNull(extensionRegistry,
            "extensionRegistry must not be null");
    }

    /**
     * Initializes the plugin system.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Discovers available plugins via ServiceLoader</li>
     *   <li>Filters incompatible plugins</li>
     *   <li>Loads compatible plugins (calls onLoad)</li>
     * </ol>
     *
     * <p>This method is idempotent - calling it multiple times has no effect.</p>
     *
     * @throws PluginException if initialization fails
     */
    public void initialize() {
        if (!initialized.compareAndSet(false, true)) {
            LOG.debug("Plugin manager already initialized");
            return;
        }

        LOG.info("Initializing plugin manager (app version: {})", appVersion);

        // Discover compatible plugins
        List<PluginContainer> discovered = loader.discoverCompatiblePlugins(appVersion);
        registry.addAll(discovered);

        LOG.info("Discovered {} compatible plugins", discovered.size());

        // Load each discovered plugin
        for (PluginContainer container : discovered) {
            try {
                loadPluginInternal(container);
            } catch (Exception e) {
                LOG.error("Failed to load plugin: {}", container.getId(), e);
                container.markFailed(e);
            }
        }

        int loaded = registry.getLoaded().size();
        int failed = registry.getFailed().size();
        LOG.info("Plugin initialization complete: {} loaded, {} failed", loaded, failed);
    }

    /**
     * Shuts down the plugin system.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Disables all enabled plugins</li>
     *   <li>Unloads all loaded plugins</li>
     *   <li>Clears the registries</li>
     * </ol>
     *
     * <p>This method is idempotent - calling it multiple times has no effect.</p>
     */
    public void shutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            LOG.debug("Plugin manager already shut down");
            return;
        }

        LOG.info("Shutting down plugin manager");

        // Disable all enabled plugins
        for (PluginContainer container : registry.getEnabled()) {
            try {
                disablePluginInternal(container);
            } catch (Exception e) {
                LOG.error("Error disabling plugin during shutdown: {}", container.getId(), e);
            }
        }

        // Unload all loaded plugins
        for (PluginContainer container : registry.getLoaded()) {
            try {
                unloadPluginInternal(container);
            } catch (Exception e) {
                LOG.error("Error unloading plugin during shutdown: {}", container.getId(), e);
            }
        }

        // Clear registries
        extensionRegistry.clear();
        registry.clear();

        LOG.info("Plugin manager shutdown complete");
    }

    /**
     * Loads a discovered plugin.
     *
     * <p>The plugin must be in DISCOVERED state. This method:</p>
     * <ol>
     *   <li>Creates a PluginContext for the plugin</li>
     *   <li>Calls the plugin's onLoad method</li>
     *   <li>Transitions the plugin to LOADED state</li>
     * </ol>
     *
     * @param plugin the plugin instance to load
     * @throws PluginLoadException     if loading fails
     * @throws PluginStateException    if plugin is not in DISCOVERED state
     * @throws NullPointerException    if plugin is null
     */
    public void loadPlugin(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin must not be null");

        String pluginId = plugin.getDescriptor().id();
        PluginContainer container = registry.get(pluginId)
            .orElseGet(() -> {
                PluginContainer newContainer = new PluginContainer(plugin);
                registry.add(newContainer);
                return newContainer;
            });

        loadPluginInternal(container);
    }

    /**
     * Internal method to load a plugin from its container.
     */
    private void loadPluginInternal(PluginContainer container) {
        String pluginId = container.getId();

        if (container.getState() != PluginState.DISCOVERED) {
            throw new PluginStateException(pluginId, container.getState(), PluginState.LOADED);
        }

        LOG.debug("Loading plugin: {}", pluginId);

        try {
            // Create context for this plugin
            PluginContext context = PluginContextImpl.builder()
                .appVersion(appVersion)
                .baseDataDirectory(pluginDataDirectory)
                .pluginId(pluginId)
                .build();

            container.setContext(context);

            // Call plugin's onLoad
            container.getPlugin().onLoad(context);

            // Transition to LOADED state
            container.setState(PluginState.LOADED);

            LOG.info("Loaded plugin: {} v{}",
                container.getDescriptor().name(),
                container.getDescriptor().version()
            );
        } catch (Exception e) {
            container.markFailed(e);
            throw new PluginLifecycleException(pluginId, "onLoad", e);
        }
    }

    /**
     * Unloads a plugin.
     *
     * <p>The plugin must be in LOADED, DISABLED, or FAILED state.
     * Enabled plugins must be disabled first.</p>
     *
     * @param pluginId the ID of the plugin to unload
     * @throws PluginNotFoundException if plugin is not found
     * @throws PluginStateException    if plugin cannot be unloaded from current state
     */
    public void unloadPlugin(String pluginId) {
        PluginContainer container = registry.getOrThrow(pluginId);
        unloadPluginInternal(container);
    }

    /**
     * Internal method to unload a plugin.
     */
    private void unloadPluginInternal(PluginContainer container) {
        String pluginId = container.getId();
        PluginState currentState = container.getState();

        if (currentState == PluginState.ENABLED) {
            throw new PluginStateException(pluginId, currentState, PluginState.UNLOADED);
        }

        if (currentState == PluginState.UNLOADED || currentState == PluginState.DISCOVERED) {
            return; // Already unloaded or never loaded
        }

        LOG.debug("Unloading plugin: {}", pluginId);

        try {
            container.getPlugin().onUnload();
        } catch (Exception e) {
            LOG.warn("Error during plugin onUnload: {}", pluginId, e);
        }

        container.forceState(PluginState.UNLOADED);
        LOG.info("Unloaded plugin: {}", pluginId);
    }

    /**
     * Enables a plugin.
     *
     * <p>The plugin must be in LOADED or DISABLED state.</p>
     *
     * @param pluginId the ID of the plugin to enable
     * @throws PluginNotFoundException  if plugin is not found
     * @throws PluginStateException     if plugin cannot be enabled from current state
     * @throws PluginLifecycleException if onEnable fails
     */
    public void enablePlugin(String pluginId) {
        PluginContainer container = registry.getOrThrow(pluginId);
        enablePluginInternal(container);
    }

    /**
     * Internal method to enable a plugin.
     */
    private void enablePluginInternal(PluginContainer container) {
        String pluginId = container.getId();
        PluginState currentState = container.getState();

        if (currentState == PluginState.ENABLED) {
            LOG.debug("Plugin already enabled: {}", pluginId);
            return;
        }

        if (currentState != PluginState.LOADED && currentState != PluginState.DISABLED) {
            throw new PluginStateException(pluginId, currentState, PluginState.ENABLED);
        }

        LOG.debug("Enabling plugin: {}", pluginId);

        try {
            // Transition to ENABLED state
            container.setState(PluginState.ENABLED);
            LOG.info("Enabled plugin: {}", pluginId);
        } catch (Exception e) {
            container.markFailed(e);
            throw new PluginLifecycleException(pluginId, "enable", e);
        }
    }

    /**
     * Disables a plugin.
     *
     * <p>The plugin must be in ENABLED state. Disabling a plugin
     * unregisters all its extensions but keeps it loaded.</p>
     *
     * @param pluginId the ID of the plugin to disable
     * @throws PluginNotFoundException  if plugin is not found
     * @throws PluginStateException     if plugin is not enabled
     */
    public void disablePlugin(String pluginId) {
        PluginContainer container = registry.getOrThrow(pluginId);
        disablePluginInternal(container);
    }

    /**
     * Internal method to disable a plugin.
     */
    private void disablePluginInternal(PluginContainer container) {
        String pluginId = container.getId();
        PluginState currentState = container.getState();

        if (currentState == PluginState.DISABLED) {
            LOG.debug("Plugin already disabled: {}", pluginId);
            return;
        }

        if (currentState != PluginState.ENABLED) {
            throw new PluginStateException(pluginId, currentState, PluginState.DISABLED);
        }

        LOG.debug("Disabling plugin: {}", pluginId);

        // Unregister all extensions for this plugin
        int unregistered = extensionRegistry.unregisterAll(pluginId);
        LOG.debug("Unregistered {} extensions for plugin: {}", unregistered, pluginId);

        container.setState(PluginState.DISABLED);
        LOG.info("Disabled plugin: {}", pluginId);
    }

    /**
     * Returns all loaded plugins (in LOADED, ENABLED, or DISABLED state).
     *
     * @return unmodifiable list of loaded plugins
     */
    public List<PluginContainer> getLoadedPlugins() {
        return registry.getLoaded();
    }

    /**
     * Returns all enabled plugins.
     *
     * @return unmodifiable list of enabled plugins
     */
    public List<PluginContainer> getEnabledPlugins() {
        return registry.getEnabled();
    }

    /**
     * Returns all registered plugins regardless of state.
     *
     * @return unmodifiable list of all plugins
     */
    public List<PluginContainer> getAllPlugins() {
        return registry.getAll();
    }

    /**
     * Gets a plugin by its ID.
     *
     * @param pluginId the plugin ID
     * @return the plugin container, or empty if not found
     */
    public Optional<PluginContainer> getPlugin(String pluginId) {
        return registry.get(pluginId);
    }

    /**
     * Checks if a plugin is enabled.
     *
     * @param pluginId the plugin ID
     * @return true if the plugin exists and is enabled
     */
    public boolean isPluginEnabled(String pluginId) {
        return registry.get(pluginId)
            .map(PluginContainer::isEnabled)
            .orElse(false);
    }

    /**
     * Checks if a plugin is loaded.
     *
     * @param pluginId the plugin ID
     * @return true if the plugin exists and is loaded
     */
    public boolean isPluginLoaded(String pluginId) {
        return registry.get(pluginId)
            .map(PluginContainer::isLoaded)
            .orElse(false);
    }

    /**
     * Returns all extensions of the specified type from enabled plugins.
     *
     * @param <T>           the extension point type
     * @param extensionType the extension point class
     * @return unmodifiable list of extensions
     */
    public <T extends ExtensionPoint> List<T> getExtensions(Class<T> extensionType) {
        return extensionRegistry.getExtensions(extensionType);
    }

    /**
     * Returns the extension registry.
     *
     * <p>This provides direct access to register/unregister extensions.</p>
     *
     * @return the extension registry
     */
    public ExtensionRegistry getExtensionRegistry() {
        return extensionRegistry;
    }

    /**
     * Returns the plugin registry.
     *
     * @return the plugin registry
     */
    public PluginRegistry getPluginRegistry() {
        return registry;
    }

    /**
     * Checks if the plugin manager has been initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * Checks if the plugin manager has been shut down.
     *
     * @return true if shut down
     */
    public boolean isShutdown() {
        return shutdown.get();
    }

    /**
     * Returns the application version.
     *
     * @return the application version
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * Creates a new builder for PluginManager.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating PluginManager instances.
     */
    public static class Builder {
        private String appVersion;
        private Path pluginDataDirectory;

        /**
         * Sets the application version.
         *
         * @param appVersion the application version
         * @return this builder
         */
        public Builder appVersion(String appVersion) {
            this.appVersion = appVersion;
            return this;
        }

        /**
         * Sets the plugin data directory.
         *
         * @param pluginDataDirectory the plugin data directory
         * @return this builder
         */
        public Builder pluginDataDirectory(Path pluginDataDirectory) {
            this.pluginDataDirectory = pluginDataDirectory;
            return this;
        }

        /**
         * Builds the PluginManager.
         *
         * @return the new PluginManager instance
         */
        public PluginManager build() {
            return new PluginManager(appVersion, pluginDataDirectory);
        }
    }
}
