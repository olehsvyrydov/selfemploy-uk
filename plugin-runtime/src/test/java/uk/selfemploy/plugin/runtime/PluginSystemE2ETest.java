package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.selfemploy.plugin.api.Plugin;
import uk.selfemploy.plugin.api.PluginContext;
import uk.selfemploy.plugin.api.PluginDependency;
import uk.selfemploy.plugin.api.PluginDescriptor;
import uk.selfemploy.plugin.api.PluginPermission;
import uk.selfemploy.plugin.extension.ExtensionPoint;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end tests for the complete plugin system.
 *
 * <p>These tests verify the full plugin lifecycle, extension registration,
 * dependency resolution, error handling, and hot-reload functionality.</p>
 *
 * <p>Tests are organized by scenario:</p>
 * <ul>
 *   <li>Full lifecycle E2E - plugin discovery through unload</li>
 *   <li>Extension registration E2E - navigation and widget extensions</li>
 *   <li>Dependency loading E2E - correct load order based on dependencies</li>
 *   <li>Permission system E2E - permission checking</li>
 *   <li>Error handling E2E - failure scenarios</li>
 *   <li>Hot-reload E2E - hot-reload functionality</li>
 * </ul>
 *
 * @see PluginManager
 * @see ExtensionRegistry
 * @see DependencyResolver
 */
@DisplayName("Plugin System E2E Tests")
class PluginSystemE2ETest {

    @TempDir
    Path tempDir;

    private PluginManager manager;
    private PluginRegistry pluginRegistry;
    private ExtensionRegistry extensionRegistry;
    private PluginLoader mockLoader;

    @BeforeEach
    void setUp() {
        pluginRegistry = new PluginRegistry();
        extensionRegistry = new ExtensionRegistry();
        mockLoader = mock(PluginLoader.class);
    }

    @AfterEach
    void tearDown() {
        if (manager != null && !manager.isShutdown()) {
            manager.shutdown();
        }
    }

    // ========================================================================
    // Full Lifecycle E2E Tests
    // ========================================================================

    @Nested
    @DisplayName("Full Lifecycle E2E")
    class FullLifecycleE2E {

        @Test
        @DisplayName("should complete full lifecycle: discover → load → enable → disable → unload")
        void shouldCompleteFullLifecycle() {
            // Given - a plugin that tracks all lifecycle events
            LifecycleTrackingPlugin plugin = new LifecycleTrackingPlugin(
                "test-plugin", "Test Plugin", "1.0.0"
            );
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);

            // Phase 1: Discovery
            assertThat(container.getState()).isEqualTo(PluginState.DISCOVERED);
            assertThat(plugin.getLifecycleEvents()).isEmpty();

            // Phase 2: Initialize (triggers load)
            manager.initialize();

            assertThat(container.getState()).isEqualTo(PluginState.LOADED);
            assertThat(plugin.getLifecycleEvents()).containsExactly("onLoad");
            assertThat(plugin.getContext()).isNotNull();
            assertThat(plugin.getContext().getAppVersion()).isEqualTo("1.0.0");

            // Phase 3: Enable
            manager.enablePlugin("test-plugin");

            assertThat(container.getState()).isEqualTo(PluginState.ENABLED);
            assertThat(manager.isPluginEnabled("test-plugin")).isTrue();

            // Phase 4: Disable
            manager.disablePlugin("test-plugin");

            assertThat(container.getState()).isEqualTo(PluginState.DISABLED);
            assertThat(manager.isPluginEnabled("test-plugin")).isFalse();

            // Phase 5: Unload
            manager.unloadPlugin("test-plugin");

            assertThat(container.getState()).isEqualTo(PluginState.UNLOADED);
            assertThat(plugin.getLifecycleEvents()).containsExactly("onLoad", "onUnload");
        }

        @Test
        @DisplayName("should handle multiple plugins through full lifecycle")
        void shouldHandleMultiplePlugins() {
            // Given - multiple plugins
            LifecycleTrackingPlugin plugin1 = new LifecycleTrackingPlugin("plugin-1", "Plugin 1", "1.0.0");
            LifecycleTrackingPlugin plugin2 = new LifecycleTrackingPlugin("plugin-2", "Plugin 2", "2.0.0");
            LifecycleTrackingPlugin plugin3 = new LifecycleTrackingPlugin("plugin-3", "Plugin 3", "1.5.0");

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(
                    new PluginContainer(plugin1),
                    new PluginContainer(plugin2),
                    new PluginContainer(plugin3)
                ));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);

            // When - initialize all
            manager.initialize();

            // Then - all loaded
            assertThat(manager.getLoadedPlugins()).hasSize(3);
            assertThat(plugin1.isLoaded()).isTrue();
            assertThat(plugin2.isLoaded()).isTrue();
            assertThat(plugin3.isLoaded()).isTrue();

            // When - enable all
            manager.enablePlugin("plugin-1");
            manager.enablePlugin("plugin-2");
            manager.enablePlugin("plugin-3");

            // Then - all enabled
            assertThat(manager.getEnabledPlugins()).hasSize(3);

            // When - shutdown
            manager.shutdown();

            // Then - all unloaded
            assertThat(plugin1.isUnloaded()).isTrue();
            assertThat(plugin2.isUnloaded()).isTrue();
            assertThat(plugin3.isUnloaded()).isTrue();
        }

        @Test
        @DisplayName("should preserve plugin data directory across enable/disable cycles")
        void shouldPreserveDataDirectory() throws Exception {
            // Given
            LifecycleTrackingPlugin plugin = new LifecycleTrackingPlugin("data-plugin", "Data Plugin", "1.0.0");
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            manager.initialize();

            // When - write data to plugin directory
            PluginContext context = plugin.getContext();
            Path dataDir = context.getPluginDataDirectory();
            Path testFile = dataDir.resolve("test.txt");
            java.nio.file.Files.writeString(testFile, "test data");

            // And - cycle through enable/disable
            manager.enablePlugin("data-plugin");
            manager.disablePlugin("data-plugin");
            manager.enablePlugin("data-plugin");

            // Then - data persists
            assertThat(java.nio.file.Files.exists(testFile)).isTrue();
            assertThat(java.nio.file.Files.readString(testFile)).isEqualTo("test data");
        }
    }

    // ========================================================================
    // Extension Registration E2E Tests
    // ========================================================================

    @Nested
    @DisplayName("Extension Registration E2E")
    class ExtensionRegistrationE2E {

        @Test
        @DisplayName("should register and retrieve extensions from enabled plugins")
        void shouldRegisterAndRetrieveExtensions() {
            // Given - a plugin with extensions
            LifecycleTrackingPlugin plugin = new LifecycleTrackingPlugin("ext-plugin", "Extension Plugin", "1.0.0");
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            manager.initialize();
            manager.enablePlugin("ext-plugin");

            // When - register extensions
            TestExtension ext1 = new TestExtensionImpl("ext1");
            TestExtension ext2 = new TestExtensionImpl("ext2");
            extensionRegistry.register("ext-plugin", TestExtension.class, ext1);
            extensionRegistry.register("ext-plugin", TestExtension.class, ext2);

            // Then - extensions are retrievable
            List<TestExtension> extensions = manager.getExtensions(TestExtension.class);
            assertThat(extensions).hasSize(2);
            assertThat(extensions).extracting(TestExtension::getName).containsExactlyInAnyOrder("ext1", "ext2");
        }

        @Test
        @DisplayName("should unregister extensions when plugin is disabled")
        void shouldUnregisterExtensionsWhenDisabled() {
            // Given - a plugin with registered extensions
            LifecycleTrackingPlugin plugin = new LifecycleTrackingPlugin("ext-plugin", "Extension Plugin", "1.0.0");
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            manager.initialize();
            manager.enablePlugin("ext-plugin");

            extensionRegistry.register("ext-plugin", TestExtension.class, new TestExtensionImpl("ext1"));
            assertThat(manager.getExtensions(TestExtension.class)).hasSize(1);

            // When - disable plugin
            manager.disablePlugin("ext-plugin");

            // Then - extensions are unregistered
            assertThat(manager.getExtensions(TestExtension.class)).isEmpty();
        }

        @Test
        @DisplayName("should handle multiple extension types from same plugin")
        void shouldHandleMultipleExtensionTypes() {
            // Given
            LifecycleTrackingPlugin plugin = new LifecycleTrackingPlugin("multi-ext", "Multi Extension", "1.0.0");
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            manager.initialize();
            manager.enablePlugin("multi-ext");

            // When - register different extension types
            extensionRegistry.register("multi-ext", TestExtension.class, new TestExtensionImpl("test-ext"));
            extensionRegistry.register("multi-ext", AnotherExtension.class, new AnotherExtensionImpl("another-ext"));

            // Then - each type is retrievable separately
            assertThat(manager.getExtensions(TestExtension.class)).hasSize(1);
            assertThat(manager.getExtensions(AnotherExtension.class)).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list for unregistered extension type")
        void shouldReturnEmptyForUnregisteredType() {
            // Given
            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            manager.initialize();

            // When/Then
            assertThat(manager.getExtensions(TestExtension.class)).isEmpty();
        }
    }

    // ========================================================================
    // Dependency Loading E2E Tests
    // ========================================================================

    @Nested
    @DisplayName("Dependency Loading E2E")
    class DependencyLoadingE2E {

        @Test
        @DisplayName("should resolve simple dependency chain")
        void shouldResolveSimpleDependencyChain() {
            // Given - A depends on B, B depends on C (all required, optional=false)
            // Expected load order: C → B → A
            DependencyResolver resolver = new DependencyResolver();

            Map<String, PluginDescriptor> plugins = Map.of(
                "plugin-a", new PluginDescriptor("plugin-a", "Plugin A", "1.0.0", "desc", "author", "0.1.0"),
                "plugin-b", new PluginDescriptor("plugin-b", "Plugin B", "1.0.0", "desc", "author", "0.1.0"),
                "plugin-c", new PluginDescriptor("plugin-c", "Plugin C", "1.0.0", "desc", "author", "0.1.0")
            );

            Map<String, List<PluginDependency>> dependencies = Map.of(
                "plugin-a", List.of(new PluginDependency("plugin-b", ">=1.0.0", false)),  // optional=false = REQUIRED
                "plugin-b", List.of(new PluginDependency("plugin-c", ">=1.0.0", false)),
                "plugin-c", List.of()
            );

            // When
            DependencyResolver.ResolutionResult result = resolver.resolve(plugins, dependencies);

            // Then - C loads first, then B, then A
            assertThat(result.loadOrder()).hasSize(3);
            assertThat(result.loadOrder().indexOf("plugin-c"))
                .isLessThan(result.loadOrder().indexOf("plugin-b"));
            assertThat(result.loadOrder().indexOf("plugin-b"))
                .isLessThan(result.loadOrder().indexOf("plugin-a"));
        }

        @Test
        @DisplayName("should block plugins with missing required dependencies")
        void shouldBlockPluginsWithMissingDependencies() {
            // Given - A depends on non-existent plugin (optional=false means REQUIRED)
            DependencyResolver resolver = new DependencyResolver();

            Map<String, PluginDescriptor> plugins = Map.of(
                "plugin-a", new PluginDescriptor("plugin-a", "Plugin A", "1.0.0", "desc", "author", "0.1.0")
            );

            Map<String, List<PluginDependency>> dependencies = Map.of(
                "plugin-a", List.of(new PluginDependency("non-existent", ">=1.0.0", false))  // optional=false = REQUIRED
            );

            // When
            DependencyResolver.ResolutionResult result = resolver.resolve(plugins, dependencies);

            // Then
            assertThat(result.hasBlockedPlugins()).isTrue();
            assertThat(result.isPluginBlocked("plugin-a")).isTrue();
            assertThat(result.blocked().get("plugin-a")).contains("Missing required dependency");
        }

        @Test
        @DisplayName("should warn but continue for optional missing dependencies")
        void shouldWarnForOptionalMissingDependencies() {
            // Given - A has optional dependency on non-existent plugin (optional=true)
            DependencyResolver resolver = new DependencyResolver();

            Map<String, PluginDescriptor> plugins = Map.of(
                "plugin-a", new PluginDescriptor("plugin-a", "Plugin A", "1.0.0", "desc", "author", "0.1.0")
            );

            Map<String, List<PluginDependency>> dependencies = Map.of(
                "plugin-a", List.of(new PluginDependency("optional-plugin", ">=1.0.0", true))  // optional=true
            );

            // When
            DependencyResolver.ResolutionResult result = resolver.resolve(plugins, dependencies);

            // Then - plugin loads but with warning
            assertThat(result.hasBlockedPlugins()).isFalse();
            assertThat(result.loadOrder()).contains("plugin-a");
            assertThat(result.warnings()).anyMatch(w -> w.contains("Optional dependency missing"));
        }

        @Test
        @DisplayName("should detect circular dependencies")
        void shouldDetectCircularDependencies() {
            // Given - A → B → C → A (circular) - all required dependencies (optional=false)
            DependencyResolver resolver = new DependencyResolver();

            Map<String, PluginDescriptor> plugins = Map.of(
                "plugin-a", new PluginDescriptor("plugin-a", "Plugin A", "1.0.0", "desc", "author", "0.1.0"),
                "plugin-b", new PluginDescriptor("plugin-b", "Plugin B", "1.0.0", "desc", "author", "0.1.0"),
                "plugin-c", new PluginDescriptor("plugin-c", "Plugin C", "1.0.0", "desc", "author", "0.1.0")
            );

            Map<String, List<PluginDependency>> dependencies = Map.of(
                "plugin-a", List.of(new PluginDependency("plugin-b", ">=1.0.0", false)),  // optional=false = REQUIRED
                "plugin-b", List.of(new PluginDependency("plugin-c", ">=1.0.0", false)),
                "plugin-c", List.of(new PluginDependency("plugin-a", ">=1.0.0", false))
            );

            // When/Then
            assertThatThrownBy(() -> resolver.resolve(plugins, dependencies))
                .isInstanceOf(DependencyResolver.CircularDependencyException.class)
                .hasMessageContaining("Circular dependency");
        }

        @Test
        @DisplayName("should block plugins with incompatible dependency versions")
        void shouldBlockPluginsWithIncompatibleVersions() {
            // Given - A depends on B version 2.0.0, but B is 1.0.0 (optional=false = REQUIRED)
            DependencyResolver resolver = new DependencyResolver();

            Map<String, PluginDescriptor> plugins = Map.of(
                "plugin-a", new PluginDescriptor("plugin-a", "Plugin A", "1.0.0", "desc", "author", "0.1.0"),
                "plugin-b", new PluginDescriptor("plugin-b", "Plugin B", "1.0.0", "desc", "author", "0.1.0")
            );

            Map<String, List<PluginDependency>> dependencies = Map.of(
                "plugin-a", List.of(new PluginDependency("plugin-b", ">=2.0.0", false)),  // optional=false = REQUIRED
                "plugin-b", List.of()
            );

            // When
            DependencyResolver.ResolutionResult result = resolver.resolve(plugins, dependencies);

            // Then
            assertThat(result.isPluginBlocked("plugin-a")).isTrue();
            assertThat(result.blocked().get("plugin-a")).contains("version mismatch");
        }
    }

    // ========================================================================
    // Permission System E2E Tests
    // ========================================================================

    @Nested
    @DisplayName("Permission System E2E")
    class PermissionSystemE2E {

        @Test
        @DisplayName("should check permissions through plugin context")
        void shouldCheckPermissionsThroughContext() {
            // Given
            LifecycleTrackingPlugin plugin = new LifecycleTrackingPlugin("perm-plugin", "Permission Plugin", "1.0.0");
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            manager.initialize();

            // When
            PluginContext context = plugin.getContext();

            // Then - context can check permissions (default behavior)
            assertThat(context).isNotNull();
            // Permission checking is available through context
            assertThat(context.hasPermission(PluginPermission.NETWORK_ACCESS)).isFalse();
        }

        @Test
        @DisplayName("should enforce permission on plugin context operations")
        void shouldEnforcePermissionOnOperations() {
            // Given
            LifecycleTrackingPlugin plugin = new LifecycleTrackingPlugin("secure-plugin", "Secure Plugin", "1.0.0");
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            manager.initialize();

            // When - plugin tries to access data directory (allowed)
            PluginContext context = plugin.getContext();
            Path dataDir = context.getPluginDataDirectory();

            // Then - data directory access is allowed
            assertThat(dataDir).isNotNull();
            assertThat(dataDir.toString()).contains("secure-plugin");
        }
    }

    // ========================================================================
    // Error Handling E2E Tests
    // ========================================================================

    @Nested
    @DisplayName("Error Handling E2E")
    class ErrorHandlingE2E {

        @Test
        @DisplayName("should handle plugin load failure gracefully")
        void shouldHandlePluginLoadFailure() {
            // Given - a plugin that fails to load
            FailingPlugin failingPlugin = new FailingPlugin("failing-plugin", "Failing Plugin", "1.0.0");
            failingPlugin.setFailOnLoad(true);

            LifecycleTrackingPlugin goodPlugin = new LifecycleTrackingPlugin("good-plugin", "Good Plugin", "1.0.0");

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(
                    new PluginContainer(failingPlugin),
                    new PluginContainer(goodPlugin)
                ));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);

            // When - initialize (one plugin fails)
            manager.initialize();

            // Then - good plugin still loads, failing plugin is in FAILED state
            assertThat(goodPlugin.isLoaded()).isTrue();
            assertThat(manager.isPluginLoaded("good-plugin")).isTrue();

            assertThat(manager.getPlugin("failing-plugin"))
                .isPresent()
                .hasValueSatisfying(c -> assertThat(c.getState()).isEqualTo(PluginState.FAILED));
        }

        @Test
        @DisplayName("should handle plugin unload failure gracefully during shutdown")
        void shouldHandlePluginUnloadFailure() {
            // Given - a plugin that fails to unload
            FailingPlugin failingPlugin = new FailingPlugin("failing-plugin", "Failing Plugin", "1.0.0");
            LifecycleTrackingPlugin goodPlugin = new LifecycleTrackingPlugin("good-plugin", "Good Plugin", "1.0.0");

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(
                    new PluginContainer(failingPlugin),
                    new PluginContainer(goodPlugin)
                ));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            manager.initialize();

            manager.enablePlugin("failing-plugin");
            manager.enablePlugin("good-plugin");

            // Set to fail on unload after initialization
            failingPlugin.setFailOnUnload(true);

            // When - shutdown (one plugin fails to unload)
            manager.shutdown();

            // Then - shutdown completes despite failure
            assertThat(manager.isShutdown()).isTrue();
            assertThat(goodPlugin.isUnloaded()).isTrue();
        }

        @Test
        @DisplayName("should provide meaningful error for invalid state transitions")
        void shouldProvideErrorForInvalidStateTransition() {
            // Given - a plugin in DISCOVERED state (not yet loaded)
            LifecycleTrackingPlugin plugin = new LifecycleTrackingPlugin("test-plugin", "Test Plugin", "1.0.0");
            PluginContainer container = new PluginContainer(plugin);

            // Add plugin to registry directly (bypassing loader)
            pluginRegistry.add(container);

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            // Note: NOT calling initialize() - plugin stays in DISCOVERED state

            // Verify the plugin is in DISCOVERED state
            assertThat(container.getState()).isEqualTo(PluginState.DISCOVERED);

            // When/Then - trying to enable without loading should fail
            assertThatThrownBy(() -> manager.enablePlugin("test-plugin"))
                .isInstanceOf(PluginStateException.class);
        }

        @Test
        @DisplayName("should handle enabling non-existent plugin")
        void shouldHandleEnablingNonExistentPlugin() {
            // Given
            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            when(mockLoader.discoverCompatiblePlugins(anyString())).thenReturn(List.of());
            manager.initialize();

            // When/Then
            assertThatThrownBy(() -> manager.enablePlugin("non-existent"))
                .isInstanceOf(PluginNotFoundException.class);
        }
    }

    // ========================================================================
    // Hot-Reload E2E Tests
    // ========================================================================

    @Nested
    @DisplayName("Hot-Reload E2E")
    class HotReloadE2E {

        @Test
        @DisplayName("should check if hot-reload is disabled by default")
        void shouldBeDisabledByDefault() {
            // When/Then
            assertThat(PluginHotReloader.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should create hot-reloader with valid config")
        void shouldCreateHotReloaderWithValidConfig() throws Exception {
            // Given
            Path watchDir = tempDir.resolve("plugins");
            java.nio.file.Files.createDirectories(watchDir);

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(watchDir)
                .debounceMillis(100)
                .build();

            // Mock plugin manager
            PluginManager pluginManager = mock(PluginManager.class);

            // When
            PluginHotReloader reloader = new PluginHotReloader(config, pluginManager, null);

            // Then
            assertThat(reloader.isRunning()).isFalse();
            assertThat(reloader.getRegisteredPlugins()).isEmpty();
        }

        @Test
        @DisplayName("should register and unregister plugins for watching")
        void shouldRegisterAndUnregisterPlugins() throws Exception {
            // Given
            Path watchDir = tempDir.resolve("plugins");
            java.nio.file.Files.createDirectories(watchDir);

            Path jarPath = watchDir.resolve("test-plugin.jar");
            java.nio.file.Files.createFile(jarPath);

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(watchDir)
                .build();

            PluginManager pluginManager = mock(PluginManager.class);
            PluginHotReloader reloader = new PluginHotReloader(config, pluginManager, null);

            // When - register
            reloader.registerPlugin("test-plugin", jarPath);

            // Then
            assertThat(reloader.getRegisteredPlugins()).containsKey("test-plugin");
            assertThat(reloader.getRegisteredPlugins().get("test-plugin")).isEqualTo(jarPath.toAbsolutePath().normalize());

            // When - unregister
            reloader.unregisterPlugin("test-plugin");

            // Then
            assertThat(reloader.getRegisteredPlugins()).doesNotContainKey("test-plugin");
        }

        @Test
        @DisplayName("should start and stop watching")
        void shouldStartAndStopWatching() throws Exception {
            // Given
            Path watchDir = tempDir.resolve("plugins");
            java.nio.file.Files.createDirectories(watchDir);

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(watchDir)
                .build();

            PluginManager pluginManager = mock(PluginManager.class);
            PluginHotReloader reloader = new PluginHotReloader(config, pluginManager, null);

            // When - start
            reloader.start();

            // Then
            assertThat(reloader.isRunning()).isTrue();

            // When - stop
            reloader.stop();

            // Then
            assertThat(reloader.isRunning()).isFalse();
        }

        @Test
        @DisplayName("should notify listener on reload events")
        void shouldNotifyListenerOnReloadEvents() throws Exception {
            // Given
            Path watchDir = tempDir.resolve("plugins");
            java.nio.file.Files.createDirectories(watchDir);

            List<String> events = new CopyOnWriteArrayList<>();

            HotReloadListener listener = new HotReloadListener() {
                @Override
                public void onReloadStarted(String pluginId) {
                    events.add("started:" + pluginId);
                }

                @Override
                public void onReloadCompleted(String pluginId, boolean success) {
                    events.add("completed:" + pluginId + ":" + success);
                }

                @Override
                public void onReloadFailed(String pluginId, Throwable error) {
                    events.add("failed:" + pluginId);
                }
            };

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(watchDir)
                .build();

            // Use mock operations to trigger reload
            PluginHotReloader.PluginReloadOperations mockOps = new PluginHotReloader.PluginReloadOperations() {
                @Override public void disablePlugin(String pluginId) {}
                @Override public void unloadPlugin(String pluginId) {}
                @Override public void loadPlugin(String pluginId) {}
                @Override public void enablePlugin(String pluginId) {}
                @Override public boolean isStatePreservationEnabled(String pluginId) { return false; }
                @Override public Object retrievePluginState(String pluginId) { return null; }
                @Override public void restorePluginState(String pluginId, Object state) {}
            };

            PluginHotReloader reloader = new PluginHotReloader(config, mockOps, listener);

            Path jarPath = watchDir.resolve("test-plugin.jar");
            java.nio.file.Files.createFile(jarPath);
            reloader.registerPlugin("test-plugin", jarPath);

            // When - manually trigger reload
            reloader.reloadPlugin("test-plugin");

            // Then
            assertThat(events).contains("started:test-plugin");
            assertThat(events).contains("completed:test-plugin:true");
        }
    }

    // ========================================================================
    // Service Registry E2E Tests
    // ========================================================================

    @Nested
    @DisplayName("Service Registry E2E")
    class ServiceRegistryE2E {

        @Test
        @DisplayName("should register and retrieve services")
        void shouldRegisterAndRetrieveServices() {
            // Given
            DefaultServiceRegistry registry = new DefaultServiceRegistry();
            TestPluginService service = new TestPluginServiceImpl("test-service");

            // When
            registry.register(TestPluginService.class, service, "test-plugin");

            // Then
            assertThat(registry.hasService(TestPluginService.class)).isTrue();
            assertThat(registry.getService(TestPluginService.class)).contains(service);
        }

        @Test
        @DisplayName("should unregister all services for a plugin")
        void shouldUnregisterAllServicesForPlugin() {
            // Given
            DefaultServiceRegistry registry = new DefaultServiceRegistry();
            TestPluginService service1 = new TestPluginServiceImpl("service1");
            TestPluginService service2 = new TestPluginServiceImpl("service2");

            registry.register(TestPluginService.class, service1, "plugin-a");
            registry.register(TestPluginService.class, service2, "plugin-b");

            // When
            registry.unregisterAll("plugin-a");

            // Then
            assertThat(registry.getServices(TestPluginService.class)).hasSize(1);
            assertThat(registry.getServices(TestPluginService.class)).containsExactly(service2);
        }
    }

    // ========================================================================
    // Event Bus E2E Tests
    // ========================================================================

    @Nested
    @DisplayName("Event Bus E2E")
    class EventBusE2E {

        @Test
        @DisplayName("should publish events to subscribers")
        void shouldPublishEventsToSubscribers() throws Exception {
            // Given
            DefaultPluginEventBus eventBus = new DefaultPluginEventBus();
            List<String> receivedEvents = new CopyOnWriteArrayList<>();

            eventBus.subscribe(
                TestPluginEvent.class,
                event -> receivedEvents.add(event.getMessage()),
                uk.selfemploy.plugin.api.ThreadAffinity.BACKGROUND,
                "test-plugin"
            );

            // When
            eventBus.publish(new TestPluginEvent("test-plugin", "Hello"));
            eventBus.publish(new TestPluginEvent("test-plugin", "World"));

            // Give async handlers time to complete
            Thread.sleep(100);

            // Then
            assertThat(receivedEvents).containsExactly("Hello", "World");

            // Cleanup
            eventBus.close();
        }

        @Test
        @DisplayName("should unsubscribe all handlers for a plugin")
        void shouldUnsubscribeAllHandlersForPlugin() throws Exception {
            // Given
            DefaultPluginEventBus eventBus = new DefaultPluginEventBus();
            List<String> receivedEvents = new CopyOnWriteArrayList<>();

            eventBus.subscribe(
                TestPluginEvent.class,
                event -> receivedEvents.add(event.getMessage()),
                uk.selfemploy.plugin.api.ThreadAffinity.BACKGROUND,
                "plugin-a"
            );
            eventBus.subscribe(
                TestPluginEvent.class,
                event -> receivedEvents.add("B:" + event.getMessage()),
                uk.selfemploy.plugin.api.ThreadAffinity.BACKGROUND,
                "plugin-b"
            );

            // When
            eventBus.unsubscribeAll("plugin-a");
            eventBus.publish(new TestPluginEvent("plugin-b", "Test"));

            // Give async handlers time to complete
            Thread.sleep(100);

            // Then - only plugin-b handler receives the event
            assertThat(receivedEvents).containsExactly("B:Test");

            // Cleanup
            eventBus.close();
        }
    }

    // ========================================================================
    // Test Helper Classes
    // ========================================================================

    /**
     * Plugin that tracks all lifecycle events.
     */
    static class LifecycleTrackingPlugin implements Plugin {
        private final String id;
        private final String name;
        private final String version;
        private final List<String> lifecycleEvents = new ArrayList<>();
        private PluginContext context;
        private final AtomicBoolean loaded = new AtomicBoolean(false);
        private final AtomicBoolean unloaded = new AtomicBoolean(false);

        LifecycleTrackingPlugin(String id, String name, String version) {
            this.id = id;
            this.name = name;
            this.version = version;
        }

        @Override
        public PluginDescriptor getDescriptor() {
            return new PluginDescriptor(id, name, version, "Test plugin", "Test Author", "0.1.0");
        }

        @Override
        public void onLoad(PluginContext context) {
            this.context = context;
            this.loaded.set(true);
            lifecycleEvents.add("onLoad");
        }

        @Override
        public void onUnload() {
            this.unloaded.set(true);
            lifecycleEvents.add("onUnload");
        }

        List<String> getLifecycleEvents() {
            return new ArrayList<>(lifecycleEvents);
        }

        PluginContext getContext() {
            return context;
        }

        boolean isLoaded() {
            return loaded.get();
        }

        boolean isUnloaded() {
            return unloaded.get();
        }
    }

    /**
     * Plugin that can be configured to fail at specific lifecycle points.
     */
    static class FailingPlugin implements Plugin {
        private final String id;
        private final String name;
        private final String version;
        private boolean failOnLoad = false;
        private boolean failOnUnload = false;

        FailingPlugin(String id, String name, String version) {
            this.id = id;
            this.name = name;
            this.version = version;
        }

        @Override
        public PluginDescriptor getDescriptor() {
            return new PluginDescriptor(id, name, version, "Failing plugin", "Test Author", "0.1.0");
        }

        @Override
        public void onLoad(PluginContext context) {
            if (failOnLoad) {
                throw new RuntimeException("Simulated load failure");
            }
        }

        @Override
        public void onUnload() {
            if (failOnUnload) {
                throw new RuntimeException("Simulated unload failure");
            }
        }

        void setFailOnLoad(boolean fail) {
            this.failOnLoad = fail;
        }

        void setFailOnUnload(boolean fail) {
            this.failOnUnload = fail;
        }
    }

    /**
     * Test extension interface.
     */
    interface TestExtension extends ExtensionPoint {
        String getName();
        void execute();
    }

    /**
     * Test extension implementation.
     */
    static class TestExtensionImpl implements TestExtension {
        private final String name;

        TestExtensionImpl(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void execute() {
            // no-op
        }
    }

    /**
     * Another extension interface for testing multiple extension types.
     */
    interface AnotherExtension extends ExtensionPoint {
        String getId();
    }

    /**
     * Another extension implementation.
     */
    static class AnotherExtensionImpl implements AnotherExtension {
        private final String id;

        AnotherExtensionImpl(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }
    }

    /**
     * Test service interface - extends PluginService for registry compatibility.
     */
    interface TestPluginService extends uk.selfemploy.plugin.api.PluginService {
        String getName();
    }

    /**
     * Test service implementation.
     */
    static class TestPluginServiceImpl implements TestPluginService {
        private final String name;

        TestPluginServiceImpl(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * Test event for event bus testing - extends PluginEvent.
     */
    static class TestPluginEvent extends uk.selfemploy.plugin.api.PluginEvent {
        private final String message;

        TestPluginEvent(String sourcePluginId, String message) {
            super(sourcePluginId);
            this.message = message;
        }

        String getMessage() {
            return message;
        }
    }
}
