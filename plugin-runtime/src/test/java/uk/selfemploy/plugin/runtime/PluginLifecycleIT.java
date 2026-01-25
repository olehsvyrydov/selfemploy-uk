package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.selfemploy.plugin.api.Plugin;
import uk.selfemploy.plugin.api.PluginContext;
import uk.selfemploy.plugin.api.PluginDescriptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the complete plugin lifecycle.
 * Tests INT-001 to INT-005 from the test design document.
 */
@DisplayName("Plugin Lifecycle Integration Tests")
class PluginLifecycleIT {

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

    @Nested
    @DisplayName("Full Plugin Lifecycle (INT-001 to INT-005)")
    class FullPluginLifecycle {

        @Test
        @DisplayName("INT-001: Complete lifecycle: discover to unload")
        void completeLifecycleDiscoverToUnload() {
            // Create a plugin that tracks lifecycle calls
            LifecycleTrackingPlugin plugin = new LifecycleTrackingPlugin(
                "lifecycle-plugin", "Lifecycle Plugin", "1.0.0", "0.1.0"
            );
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);

            // DISCOVERED state
            assertThat(container.getState()).isEqualTo(PluginState.DISCOVERED);
            assertThat(plugin.loadCalled.get()).isFalse();

            // Initialize - triggers discovery and load
            manager.initialize();

            // LOADED state
            assertThat(container.getState()).isEqualTo(PluginState.LOADED);
            assertThat(plugin.loadCalled.get()).isTrue();
            assertThat(plugin.receivedContext).isNotNull();

            // Enable
            manager.enablePlugin("lifecycle-plugin");
            assertThat(container.getState()).isEqualTo(PluginState.ENABLED);

            // Disable
            manager.disablePlugin("lifecycle-plugin");
            assertThat(container.getState()).isEqualTo(PluginState.DISABLED);

            // Unload
            manager.unloadPlugin("lifecycle-plugin");
            assertThat(container.getState()).isEqualTo(PluginState.UNLOADED);
            assertThat(plugin.unloadCalled.get()).isTrue();
        }

        @Test
        @DisplayName("INT-002: Plugin manager shutdown cleans up all resources")
        void shutdownCleansUpResources() {
            TestPlugin plugin1 = new TestPlugin("plugin-1", "Plugin 1", "1.0.0", "0.1.0");
            TestPlugin plugin2 = new TestPlugin("plugin-2", "Plugin 2", "1.0.0", "0.1.0");
            PluginContainer container1 = new PluginContainer(plugin1);
            PluginContainer container2 = new PluginContainer(plugin2);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container1, container2));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            manager.initialize();

            // Enable both plugins
            manager.enablePlugin("plugin-1");
            manager.enablePlugin("plugin-2");

            // Register some extensions
            TestExtension ext1 = createExtension("ext1");
            TestExtension ext2 = createExtension("ext2");
            extensionRegistry.register("plugin-1", TestExtension.class, ext1);
            extensionRegistry.register("plugin-2", TestExtension.class, ext2);

            assertThat(extensionRegistry.getTotalExtensionCount()).isEqualTo(2);

            // Shutdown
            manager.shutdown();

            // Verify cleanup
            assertThat(manager.isShutdown()).isTrue();
            assertThat(pluginRegistry.isEmpty()).isTrue();
            assertThat(extensionRegistry.getTotalExtensionCount()).isEqualTo(0);
            assertThat(plugin1.isUnloaded()).isTrue();
            assertThat(plugin2.isUnloaded()).isTrue();
        }

        @Test
        @DisplayName("INT-003: Initialize is idempotent - no duplicate plugins")
        void initializeIsIdempotent() {
            AtomicInteger discoveryCount = new AtomicInteger(0);
            TestPlugin plugin = new TestPlugin("test-plugin", "Test", "1.0.0", "0.1.0");
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenAnswer(inv -> {
                    discoveryCount.incrementAndGet();
                    return List.of(container);
                });

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);

            // First initialization
            manager.initialize();
            assertThat(manager.isInitialized()).isTrue();
            assertThat(plugin.getLoadCount()).isEqualTo(1);

            // Second initialization - should be idempotent
            manager.initialize();
            assertThat(manager.isInitialized()).isTrue();
            assertThat(plugin.getLoadCount()).isEqualTo(1); // Still 1, not incremented
        }

        @Test
        @DisplayName("INT-004: Shutdown is idempotent - no errors on double shutdown")
        void shutdownIsIdempotent() {
            TestPlugin plugin = new TestPlugin("test-plugin", "Test", "1.0.0", "0.1.0");
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            manager.initialize();
            manager.enablePlugin("test-plugin");

            // First shutdown
            manager.shutdown();
            assertThat(manager.isShutdown()).isTrue();
            assertThat(plugin.getUnloadCount()).isEqualTo(1);

            // Second shutdown - should not throw or call unload again
            manager.shutdown();
            assertThat(manager.isShutdown()).isTrue();
            assertThat(plugin.getUnloadCount()).isEqualTo(1); // Still 1
        }

        @Test
        @DisplayName("INT-005: Plugin context persists data during lifecycle")
        void pluginContextPersistsData() throws IOException {
            TestPlugin plugin = new TestPlugin("data-plugin", "Data Plugin", "1.0.0", "0.1.0");
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            manager.initialize();

            // Write data to plugin directory
            PluginContext context = plugin.getContext();
            Path dataDir = context.getPluginDataDirectory();
            Path dataFile = dataDir.resolve("test-data.txt");
            Files.writeString(dataFile, "test content");

            // Enable and disable plugin
            manager.enablePlugin("data-plugin");
            manager.disablePlugin("data-plugin");

            // Re-enable and verify data persists
            manager.enablePlugin("data-plugin");
            assertThat(Files.exists(dataFile)).isTrue();
            assertThat(Files.readString(dataFile)).isEqualTo("test content");
        }
    }

    /**
     * Plugin that tracks lifecycle method calls.
     */
    private static class LifecycleTrackingPlugin implements Plugin {
        private final String id;
        private final String name;
        private final String version;
        private final String minAppVersion;

        final AtomicBoolean loadCalled = new AtomicBoolean(false);
        final AtomicBoolean unloadCalled = new AtomicBoolean(false);
        PluginContext receivedContext;

        LifecycleTrackingPlugin(String id, String name, String version, String minAppVersion) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.minAppVersion = minAppVersion;
        }

        @Override
        public PluginDescriptor getDescriptor() {
            return new PluginDescriptor(id, name, version, "Test", "Author", minAppVersion);
        }

        @Override
        public void onLoad(PluginContext context) {
            loadCalled.set(true);
            receivedContext = context;
        }

        @Override
        public void onUnload() {
            unloadCalled.set(true);
        }
    }

    private TestExtension createExtension(String name) {
        return new TestExtension() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public void execute() {
                // no-op
            }
        };
    }
}
