package uk.selfemploy.plugin.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hot-reload support for plugins during development.
 *
 * <p>This class monitors plugin JAR files for changes and automatically reloads
 * them when modifications are detected. This feature is designed for development
 * use only and is disabled by default.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Uses NIO WatchService for efficient file change detection (not polling)</li>
 *   <li>Configurable debounce to prevent multiple reloads during compilation</li>
 *   <li>Disabled by default for safety; enable via system property</li>
 *   <li>Optional state preservation across reloads</li>
 *   <li>Clean reload sequence: disable → unload → load → enable</li>
 *   <li>UI notifications via {@link HotReloadListener}</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>Enable hot-reload by setting the system property:</p>
 * <pre>
 * java -Dplugin.hotreload=true -jar app.jar
 * </pre>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * if (PluginHotReloader.isEnabled()) {
 *     HotReloadConfig config = HotReloadConfig.builder()
 *         .watchDirectory(pluginsDir)
 *         .build();
 *
 *     PluginHotReloader reloader = new PluginHotReloader(
 *         config, pluginManager, uiListener);
 *
 *     // Register plugins to watch
 *     reloader.registerPlugin("my-plugin", pluginJarPath);
 *
 *     // Start watching
 *     reloader.start();
 *
 *     // ... later ...
 *     reloader.stop();
 * }
 * }</pre>
 *
 * @see HotReloadConfig
 * @see HotReloadListener
 */
public class PluginHotReloader {

    private static final Logger LOG = LoggerFactory.getLogger(PluginHotReloader.class);

    /**
     * System property to enable hot-reload.
     *
     * <p>Hot-reload is disabled by default for safety. Set this property
     * to "true" to enable: {@code -Dplugin.hotreload=true}</p>
     */
    public static final String HOTRELOAD_ENABLED_PROPERTY = "plugin.hotreload";

    private final HotReloadConfig config;
    private final PluginReloadOperations pluginOperations;
    private final HotReloadListener listener;

    /**
     * Map of pluginId -> JAR path for registered plugins.
     */
    private final Map<String, Path> registeredPlugins = new ConcurrentHashMap<>();

    /**
     * Map of JAR path -> pluginId for reverse lookup when file changes detected.
     */
    private final Map<Path, String> pathToPluginId = new ConcurrentHashMap<>();

    /**
     * Map of pluginId -> pending reload task for debouncing.
     */
    private final Map<String, ScheduledFuture<?>> pendingReloads = new ConcurrentHashMap<>();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile WatchService watchService;
    private volatile Thread watchThread;
    private volatile ScheduledExecutorService debounceExecutor;

    /**
     * Creates a new PluginHotReloader.
     *
     * @param config          the hot-reload configuration
     * @param pluginManager   the plugin manager for lifecycle operations
     * @param listener        the listener for reload notifications (may be null)
     * @throws NullPointerException if config or pluginManager is null
     */
    public PluginHotReloader(
            HotReloadConfig config,
            PluginManager pluginManager,
            HotReloadListener listener) {
        this(config, new PluginManagerOperations(pluginManager), listener);
    }

    /**
     * Creates a new PluginHotReloader with custom operations.
     * Package-private for testing.
     */
    PluginHotReloader(
            HotReloadConfig config,
            PluginReloadOperations pluginOperations,
            HotReloadListener listener) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.pluginOperations = Objects.requireNonNull(pluginOperations, "pluginManager must not be null");
        this.listener = listener != null ? listener : HotReloadListener.noOp();
    }

    /**
     * Checks if hot-reload is enabled via system property.
     *
     * <p>Returns true only if the system property {@code plugin.hotreload}
     * is explicitly set to "true". All other values (including unset)
     * result in hot-reload being disabled.</p>
     *
     * @return true if hot-reload is enabled
     */
    public static boolean isEnabled() {
        String property = System.getProperty(HOTRELOAD_ENABLED_PROPERTY);
        return "true".equalsIgnoreCase(property);
    }

    /**
     * Starts the file watcher.
     *
     * <p>Uses NIO WatchService for efficient, event-driven file change
     * detection (as opposed to inefficient polling).</p>
     *
     * <p>This method is idempotent - calling it multiple times has no effect.</p>
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            LOG.debug("Hot-reloader already running");
            return;
        }

        LOG.info("Starting plugin hot-reloader for directory: {}", config.getWatchDirectory());

        try {
            // Use NIO WatchService for efficient file monitoring
            watchService = FileSystems.getDefault().newWatchService();

            // Register the watch directory
            config.getWatchDirectory().register(
                watchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE
            );

            // Create debounce executor
            debounceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "plugin-hotreload-debounce");
                t.setDaemon(true);
                return t;
            });

            // Start watch thread
            watchThread = new Thread(this::watchLoop, "plugin-hotreload-watcher");
            watchThread.setDaemon(true);
            watchThread.start();

            LOG.info("Plugin hot-reloader started");
        } catch (IOException e) {
            running.set(false);
            LOG.error("Failed to start hot-reloader", e);
            throw new RuntimeException("Failed to start hot-reloader", e);
        }
    }

    /**
     * Stops the file watcher.
     *
     * <p>This method is idempotent - calling it multiple times has no effect.</p>
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        LOG.info("Stopping plugin hot-reloader");

        // Cancel pending reloads
        pendingReloads.values().forEach(future -> future.cancel(false));
        pendingReloads.clear();

        // Shutdown debounce executor
        if (debounceExecutor != null) {
            debounceExecutor.shutdownNow();
            debounceExecutor = null;
        }

        // Close watch service (this will cause watchThread to exit)
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                LOG.warn("Error closing watch service", e);
            }
            watchService = null;
        }

        // Wait for watch thread to terminate
        if (watchThread != null) {
            try {
                watchThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            watchThread = null;
        }

        LOG.info("Plugin hot-reloader stopped");
    }

    /**
     * Checks if the reloader is currently running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Registers a plugin for hot-reload watching.
     *
     * @param pluginId the plugin ID
     * @param jarPath  the path to the plugin JAR file
     * @throws NullPointerException if any argument is null
     */
    public void registerPlugin(String pluginId, Path jarPath) {
        Objects.requireNonNull(pluginId, "pluginId must not be null");
        Objects.requireNonNull(jarPath, "jarPath must not be null");

        Path absolutePath = jarPath.toAbsolutePath().normalize();
        registeredPlugins.put(pluginId, absolutePath);
        pathToPluginId.put(absolutePath, pluginId);

        LOG.debug("Registered plugin for hot-reload: {} -> {}", pluginId, absolutePath);
    }

    /**
     * Unregisters a plugin from hot-reload watching.
     *
     * @param pluginId the plugin ID to unregister
     */
    public void unregisterPlugin(String pluginId) {
        Path path = registeredPlugins.remove(pluginId);
        if (path != null) {
            pathToPluginId.remove(path);
            LOG.debug("Unregistered plugin from hot-reload: {}", pluginId);
        }

        // Cancel any pending reload
        ScheduledFuture<?> pending = pendingReloads.remove(pluginId);
        if (pending != null) {
            pending.cancel(false);
        }
    }

    /**
     * Returns a read-only view of registered plugins.
     *
     * @return map of pluginId -> JAR path
     */
    public Map<String, Path> getRegisteredPlugins() {
        return Collections.unmodifiableMap(registeredPlugins);
    }

    /**
     * Manually triggers a reload for a specific plugin.
     *
     * <p>The reload follows a clean sequence to ensure proper cleanup:</p>
     * <ol>
     *   <li>Disable the plugin (unregister extensions)</li>
     *   <li>Unload the plugin (call onUnload)</li>
     *   <li>Load the plugin (re-read JAR, call onLoad)</li>
     *   <li>Enable the plugin (re-register extensions)</li>
     * </ol>
     *
     * @param pluginId the plugin ID to reload
     */
    public void reloadPlugin(String pluginId) {
        if (!registeredPlugins.containsKey(pluginId)) {
            LOG.warn("Cannot reload unregistered plugin: {}", pluginId);
            return;
        }

        LOG.info("Reloading plugin: {}", pluginId);

        // Notify listener that reload is starting
        listener.onReloadStarted(pluginId);

        boolean success = false;
        Object savedState = null;

        try {
            // State preservation is opt-in via plugin.yaml
            boolean preserveState = pluginOperations.isStatePreservationEnabled(pluginId);
            if (preserveState) {
                try {
                    savedState = pluginOperations.retrievePluginState(pluginId);
                    LOG.debug("Retrieved state for plugin: {}", pluginId);
                } catch (Exception e) {
                    LOG.warn("Failed to retrieve state for plugin: {}", pluginId, e);
                    savedState = null;
                }
            }

            // Execute reload sequence: disable -> unload -> load -> enable
            pluginOperations.disablePlugin(pluginId);
            pluginOperations.unloadPlugin(pluginId);
            pluginOperations.loadPlugin(pluginId);
            pluginOperations.enablePlugin(pluginId);

            // Restore state if it was preserved
            if (preserveState && savedState != null) {
                try {
                    pluginOperations.restorePluginState(pluginId, savedState);
                    LOG.debug("Restored state for plugin: {}", pluginId);
                } catch (Exception e) {
                    LOG.warn("Failed to restore state for plugin: {}", pluginId, e);
                }
            }

            success = true;
            LOG.info("Successfully reloaded plugin: {}", pluginId);

        } catch (Exception e) {
            LOG.error("Failed to reload plugin: {}", pluginId, e);
            listener.onReloadFailed(pluginId, e);
            return;
        }

        // Notify listener of completion
        listener.onReloadCompleted(pluginId, success);
    }

    /**
     * Main watch loop using NIO WatchService for efficient file monitoring.
     */
    private void watchLoop() {
        LOG.debug("Watch loop started");

        while (running.get()) {
            try {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        LOG.warn("File system event overflow");
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path changedFile = config.getWatchDirectory()
                        .resolve(pathEvent.context())
                        .toAbsolutePath()
                        .normalize();

                    handleFileChange(changedFile);
                }

                boolean valid = key.reset();
                if (!valid) {
                    LOG.warn("Watch key invalidated, stopping");
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                if (running.get()) {
                    LOG.error("Error in watch loop", e);
                }
            }
        }

        LOG.debug("Watch loop ended");
    }

    /**
     * Handles a file change event with debouncing.
     *
     * <p>Uses configurable debounce to coalesce rapid file changes
     * (e.g., during compilation) into a single reload.</p>
     */
    private void handleFileChange(Path changedFile) {
        String pluginId = pathToPluginId.get(changedFile);

        if (pluginId == null) {
            // File is not a registered plugin JAR
            return;
        }

        LOG.debug("Detected change to plugin JAR: {} ({})", pluginId, changedFile);

        // Debounce: cancel any pending reload and reschedule
        ScheduledFuture<?> existing = pendingReloads.get(pluginId);
        if (existing != null) {
            existing.cancel(false);
        }

        ScheduledFuture<?> scheduled = debounceExecutor.schedule(
            () -> {
                pendingReloads.remove(pluginId);
                reloadPlugin(pluginId);
            },
            config.getDebounceMillis(),
            TimeUnit.MILLISECONDS
        );

        pendingReloads.put(pluginId, scheduled);
    }

    /**
     * Interface for plugin reload operations.
     * Package-private for testing.
     */
    interface PluginReloadOperations {
        void disablePlugin(String pluginId);
        void unloadPlugin(String pluginId);
        void loadPlugin(String pluginId);
        void enablePlugin(String pluginId);
        boolean isStatePreservationEnabled(String pluginId);
        Object retrievePluginState(String pluginId);
        void restorePluginState(String pluginId, Object state);
    }

    /**
     * Default implementation that delegates to PluginManager.
     */
    private static class PluginManagerOperations implements PluginReloadOperations {
        private final PluginManager pluginManager;

        PluginManagerOperations(PluginManager pluginManager) {
            this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager must not be null");
        }

        @Override
        public void disablePlugin(String pluginId) {
            pluginManager.disablePlugin(pluginId);
        }

        @Override
        public void unloadPlugin(String pluginId) {
            pluginManager.unloadPlugin(pluginId);
        }

        @Override
        public void loadPlugin(String pluginId) {
            // Re-discover and load the plugin
            // For hot-reload, we need to re-scan the JAR file
            pluginManager.getPlugin(pluginId).ifPresent(container -> {
                // Force transition to allow reload
                container.forceState(PluginState.DISCOVERED);
            });
            // Trigger load
            pluginManager.getPlugin(pluginId).ifPresent(container -> {
                try {
                    var loadMethod = PluginManager.class.getDeclaredMethod(
                        "loadPluginInternal", PluginContainer.class);
                    loadMethod.setAccessible(true);
                    loadMethod.invoke(pluginManager, container);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to reload plugin", e);
                }
            });
        }

        @Override
        public void enablePlugin(String pluginId) {
            pluginManager.enablePlugin(pluginId);
        }

        @Override
        public boolean isStatePreservationEnabled(String pluginId) {
            // TODO: Read from plugin.yaml when descriptor includes preserveStateOnReload
            return false;
        }

        @Override
        public Object retrievePluginState(String pluginId) {
            // TODO: Implement state retrieval when Plugin interface supports it
            return null;
        }

        @Override
        public void restorePluginState(String pluginId, Object state) {
            // TODO: Implement state restoration when Plugin interface supports it
        }
    }

}
