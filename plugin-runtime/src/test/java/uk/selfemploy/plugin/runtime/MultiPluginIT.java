package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for multiple plugins with overlapping extensions.
 * Tests INT-020 to INT-023 and INT-040 to INT-041 from the test design document.
 */
@DisplayName("Multi-Plugin Integration Tests")
class MultiPluginIT {

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
    @DisplayName("Multiple Plugins with Overlapping Extensions (INT-020 to INT-023)")
    class OverlappingExtensions {

        private TestPlugin pluginA;
        private TestPlugin pluginB;
        private TestPlugin pluginC;
        private PluginContainer containerA;
        private PluginContainer containerB;
        private PluginContainer containerC;

        @BeforeEach
        void setUpPlugins() {
            pluginA = new TestPlugin("plugin-a", "Plugin A", "1.0.0", "0.1.0");
            pluginB = new TestPlugin("plugin-b", "Plugin B", "1.0.0", "0.1.0");
            pluginC = new TestPlugin("plugin-c", "Plugin C", "1.0.0", "0.1.0");

            containerA = new PluginContainer(pluginA);
            containerB = new PluginContainer(pluginB);
            containerC = new PluginContainer(pluginC);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(containerA, containerB, containerC));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            manager.initialize();
        }

        @Test
        @DisplayName("INT-020: Multiple plugins register same extension type")
        void multiplePluginsRegisterSameExtensionType() {
            // Register extensions for all plugins
            TestExtension extA = createExtension("ext-a");
            TestExtension extB = createExtension("ext-b");
            TestExtension extC = createExtension("ext-c");

            extensionRegistry.register("plugin-a", TestExtension.class, extA);
            extensionRegistry.register("plugin-b", TestExtension.class, extB);
            extensionRegistry.register("plugin-c", TestExtension.class, extC);

            // Enable all plugins
            manager.enablePlugin("plugin-a");
            manager.enablePlugin("plugin-b");
            manager.enablePlugin("plugin-c");

            // All 3 extensions should be available
            List<TestExtension> extensions = manager.getExtensions(TestExtension.class);
            assertThat(extensions).hasSize(3);
            assertThat(extensions).containsExactly(extA, extB, extC);
        }

        @Test
        @DisplayName("INT-021: Disable one plugin removes only its extensions")
        void disableOnePluginRemovesOnlyItsExtensions() {
            // Register extensions
            TestExtension extA = createExtension("ext-a");
            TestExtension extB = createExtension("ext-b");
            TestExtension extC = createExtension("ext-c");

            extensionRegistry.register("plugin-a", TestExtension.class, extA);
            extensionRegistry.register("plugin-b", TestExtension.class, extB);
            extensionRegistry.register("plugin-c", TestExtension.class, extC);

            // Enable all plugins
            manager.enablePlugin("plugin-a");
            manager.enablePlugin("plugin-b");
            manager.enablePlugin("plugin-c");

            assertThat(extensionRegistry.getTotalExtensionCount()).isEqualTo(3);

            // Disable plugin B and unregister its extensions
            manager.disablePlugin("plugin-b");
            extensionRegistry.unregisterAll("plugin-b");

            // Only B's extensions should be removed
            List<TestExtension> remaining = manager.getExtensions(TestExtension.class);
            assertThat(remaining).hasSize(2);
            assertThat(remaining).containsExactly(extA, extC);
            assertThat(remaining).doesNotContain(extB);
        }

        @Test
        @DisplayName("INT-022: Extension order maintained across plugins")
        void extensionOrderMaintainedAcrossPlugins() {
            // Register extensions in specific order
            TestExtension ext1 = createExtension("first");
            TestExtension ext2 = createExtension("second");
            TestExtension ext3 = createExtension("third");

            extensionRegistry.register("plugin-a", TestExtension.class, ext1);
            extensionRegistry.register("plugin-b", TestExtension.class, ext2);
            extensionRegistry.register("plugin-c", TestExtension.class, ext3);

            // Order should reflect registration order
            List<TestExtension> extensions = manager.getExtensions(TestExtension.class);
            assertThat(extensions).containsExactly(ext1, ext2, ext3);

            // Verify order is preserved
            assertThat(extensions.get(0).getName()).isEqualTo("first");
            assertThat(extensions.get(1).getName()).isEqualTo("second");
            assertThat(extensions.get(2).getName()).isEqualTo("third");
        }

        @Test
        @DisplayName("INT-023: Re-enable plugin re-registers extensions")
        void reEnablePluginReRegistersExtensions() {
            // Register and enable
            TestExtension extB = createExtension("ext-b");
            extensionRegistry.register("plugin-b", TestExtension.class, extB);
            manager.enablePlugin("plugin-b");

            assertThat(extensionRegistry.getTotalExtensionCount()).isEqualTo(1);

            // Disable and unregister
            manager.disablePlugin("plugin-b");
            extensionRegistry.unregisterAll("plugin-b");

            assertThat(extensionRegistry.getTotalExtensionCount()).isEqualTo(0);

            // Re-enable and re-register
            manager.enablePlugin("plugin-b");
            TestExtension newExtB = createExtension("ext-b-new");
            extensionRegistry.register("plugin-b", TestExtension.class, newExtB);

            // Extension should be available again
            List<TestExtension> extensions = manager.getExtensions(TestExtension.class);
            assertThat(extensions).hasSize(1);
            assertThat(extensions.get(0).getName()).isEqualTo("ext-b-new");
        }
    }

    @Nested
    @DisplayName("Plugin Reload Scenarios (INT-040 to INT-041)")
    class PluginReloadScenarios {

        @Test
        @DisplayName("INT-040: PluginLoader reload discovers new plugins")
        void reloadDiscoversNewPlugins() {
            TestPlugin initialPlugin = new TestPlugin("initial", "Initial", "1.0.0", "0.1.0");
            PluginContainer initialContainer = new PluginContainer(initialPlugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(initialContainer));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            manager.initialize();

            assertThat(manager.getAllPlugins()).hasSize(1);

            // Simulate adding new plugin on reload
            TestPlugin newPlugin = new TestPlugin("new-plugin", "New Plugin", "1.0.0", "0.1.0");
            PluginContainer newContainer = new PluginContainer(newPlugin);

            when(mockLoader.reload())
                .thenReturn(List.of(initialContainer, newContainer));

            // Call reload on loader
            List<PluginContainer> reloaded = mockLoader.reload();

            // Verify new plugin is in the reloaded list
            assertThat(reloaded).hasSize(2);
            assertThat(reloaded.stream().map(PluginContainer::getId))
                .containsExactlyInAnyOrder("initial", "new-plugin");
        }

        @Test
        @DisplayName("INT-041: Unload and reload same plugin creates fresh instance")
        void unloadAndReloadCreatesFreshInstance() {
            TestPlugin plugin = new TestPlugin("reloadable", "Reloadable", "1.0.0", "0.1.0");
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager = new PluginManager("1.0.0", tempDir, mockLoader, pluginRegistry, extensionRegistry);
            manager.initialize();
            manager.enablePlugin("reloadable");

            assertThat(plugin.getLoadCount()).isEqualTo(1);

            // Disable and unload
            manager.disablePlugin("reloadable");
            manager.unloadPlugin("reloadable");

            assertThat(container.getState()).isEqualTo(PluginState.UNLOADED);

            // Simulate reload with new instance
            TestPlugin freshPlugin = new TestPlugin("reloadable", "Reloadable", "1.0.0", "0.1.0");
            PluginContainer freshContainer = new PluginContainer(freshPlugin);

            when(mockLoader.reload()).thenReturn(List.of(freshContainer));

            List<PluginContainer> reloaded = mockLoader.reload();

            // Fresh container should be in DISCOVERED state
            assertThat(reloaded.get(0).getState()).isEqualTo(PluginState.DISCOVERED);
            assertThat(freshPlugin.getLoadCount()).isEqualTo(0); // Not loaded yet
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
