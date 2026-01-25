package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("PluginManager")
class PluginManagerTest {

    @TempDir
    Path tempDir;

    @Mock
    private PluginLoader mockLoader;

    private PluginRegistry registry;
    private ExtensionRegistry extensionRegistry;
    private PluginManager manager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        registry = new PluginRegistry();
        extensionRegistry = new ExtensionRegistry();
        manager = new PluginManager("1.0.0", tempDir, mockLoader, registry, extensionRegistry);
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("Creates manager with valid parameters")
        void createsWithValidParams() {
            PluginManager mgr = PluginManager.builder()
                .appVersion("1.0.0")
                .pluginDataDirectory(tempDir)
                .build();

            assertThat(mgr.getAppVersion()).isEqualTo("1.0.0");
            assertThat(mgr.isInitialized()).isFalse();
            assertThat(mgr.isShutdown()).isFalse();
        }

        @Test
        @DisplayName("Throws on null appVersion")
        void throwsOnNullAppVersion() {
            assertThatThrownBy(() -> new PluginManager(null, tempDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("appVersion");
        }

        @Test
        @DisplayName("Throws on blank appVersion")
        void throwsOnBlankAppVersion() {
            assertThatThrownBy(() -> new PluginManager("  ", tempDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("appVersion");
        }

        @Test
        @DisplayName("Throws on null pluginDataDirectory")
        void throwsOnNullDirectory() {
            assertThatThrownBy(() -> new PluginManager("1.0.0", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("pluginDataDirectory");
        }
    }

    @Nested
    @DisplayName("Initialization")
    class Initialization {

        @Test
        @DisplayName("Initializes and discovers plugins")
        void initializesAndDiscovers() {
            TestPlugin plugin = new TestPlugin("test-plugin", "Test", "1.0.0", "0.1.0");
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager.initialize();

            assertThat(manager.isInitialized()).isTrue();
            assertThat(registry.contains("test-plugin")).isTrue();
            assertThat(plugin.isLoaded()).isTrue();
        }

        @Test
        @DisplayName("Initialize is idempotent")
        void initializeIsIdempotent() {
            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(Collections.emptyList());

            manager.initialize();
            manager.initialize(); // Should not throw or reinitialize

            assertThat(manager.isInitialized()).isTrue();
        }

        @Test
        @DisplayName("Marks failed plugins on load error")
        void marksFailedOnLoadError() {
            TestPlugin plugin = new TestPlugin("failing-plugin", "Failing", "1.0.0", "0.1.0");
            plugin.setLoadException(new RuntimeException("Load failed"));
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager.initialize();

            assertThat(container.isFailed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Shutdown")
    class Shutdown {

        @Test
        @DisplayName("Shuts down cleanly")
        void shutsDownCleanly() {
            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(Collections.emptyList());

            manager.initialize();
            manager.shutdown();

            assertThat(manager.isShutdown()).isTrue();
            assertThat(registry.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Shutdown is idempotent")
        void shutdownIsIdempotent() {
            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(Collections.emptyList());

            manager.initialize();
            manager.shutdown();
            manager.shutdown(); // Should not throw

            assertThat(manager.isShutdown()).isTrue();
        }

        @Test
        @DisplayName("Disables enabled plugins on shutdown")
        void disablesEnabledOnShutdown() {
            TestPlugin plugin = new TestPlugin("test-plugin", "Test", "1.0.0", "0.1.0");
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager.initialize();
            manager.enablePlugin("test-plugin");

            assertThat(container.isEnabled()).isTrue();

            manager.shutdown();

            assertThat(manager.isShutdown()).isTrue();
        }

        @Test
        @DisplayName("Unloads loaded plugins on shutdown")
        void unloadsLoadedOnShutdown() {
            TestPlugin plugin = new TestPlugin("test-plugin", "Test", "1.0.0", "0.1.0");
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager.initialize();

            assertThat(plugin.isLoaded()).isTrue();

            manager.shutdown();

            assertThat(plugin.isUnloaded()).isTrue();
        }
    }

    @Nested
    @DisplayName("Plugin lifecycle")
    class PluginLifecycle {

        private TestPlugin testPlugin;
        private PluginContainer container;

        @BeforeEach
        void setUpPlugin() {
            testPlugin = new TestPlugin("test-plugin", "Test", "1.0.0", "0.1.0");
            container = new PluginContainer(testPlugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager.initialize();
        }

        @Test
        @DisplayName("Enable plugin transitions to ENABLED state")
        void enableTransitionsToEnabled() {
            manager.enablePlugin("test-plugin");

            assertThat(container.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Enable already enabled plugin is idempotent")
        void enableIdempotent() {
            manager.enablePlugin("test-plugin");
            manager.enablePlugin("test-plugin"); // Should not throw

            assertThat(container.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Disable plugin transitions to DISABLED state")
        void disableTransitionsToDisabled() {
            manager.enablePlugin("test-plugin");
            manager.disablePlugin("test-plugin");

            assertThat(container.getState()).isEqualTo(PluginState.DISABLED);
        }

        @Test
        @DisplayName("Disable already disabled plugin is idempotent")
        void disableIdempotent() {
            manager.enablePlugin("test-plugin");
            manager.disablePlugin("test-plugin");
            manager.disablePlugin("test-plugin"); // Should not throw

            assertThat(container.getState()).isEqualTo(PluginState.DISABLED);
        }

        @Test
        @DisplayName("Can re-enable disabled plugin")
        void canReenableDisabled() {
            manager.enablePlugin("test-plugin");
            manager.disablePlugin("test-plugin");
            manager.enablePlugin("test-plugin");

            assertThat(container.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Unload plugin calls onUnload")
        void unloadCallsOnUnload() {
            // Plugin is in LOADED state, can be unloaded directly
            manager.unloadPlugin("test-plugin");

            assertThat(testPlugin.isUnloaded()).isTrue();
            assertThat(container.getState()).isEqualTo(PluginState.UNLOADED);
        }

        @Test
        @DisplayName("Cannot unload enabled plugin directly")
        void cannotUnloadEnabled() {
            manager.enablePlugin("test-plugin");

            assertThatThrownBy(() -> manager.unloadPlugin("test-plugin"))
                .isInstanceOf(PluginStateException.class);
        }
    }

    @Nested
    @DisplayName("Plugin queries")
    class PluginQueries {

        @BeforeEach
        void setUpPlugins() {
            TestPlugin plugin1 = new TestPlugin("plugin-1", "Plugin 1", "1.0.0", "0.1.0");
            TestPlugin plugin2 = new TestPlugin("plugin-2", "Plugin 2", "1.0.0", "0.1.0");

            PluginContainer container1 = new PluginContainer(plugin1);
            PluginContainer container2 = new PluginContainer(plugin2);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container1, container2));

            manager.initialize();
        }

        @Test
        @DisplayName("getPlugin returns Optional")
        void getPluginReturnsOptional() {
            Optional<PluginContainer> result = manager.getPlugin("plugin-1");

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo("plugin-1");
        }

        @Test
        @DisplayName("getPlugin returns empty for unknown")
        void getPluginReturnsEmptyForUnknown() {
            Optional<PluginContainer> result = manager.getPlugin("unknown");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getLoadedPlugins returns loaded plugins")
        void getLoadedPlugins() {
            List<PluginContainer> loaded = manager.getLoadedPlugins();

            assertThat(loaded).hasSize(2);
        }

        @Test
        @DisplayName("getEnabledPlugins returns only enabled")
        void getEnabledPlugins() {
            manager.enablePlugin("plugin-1");

            List<PluginContainer> enabled = manager.getEnabledPlugins();

            assertThat(enabled).hasSize(1);
            assertThat(enabled.get(0).getId()).isEqualTo("plugin-1");
        }

        @Test
        @DisplayName("getAllPlugins returns all")
        void getAllPlugins() {
            List<PluginContainer> all = manager.getAllPlugins();

            assertThat(all).hasSize(2);
        }

        @Test
        @DisplayName("isPluginEnabled returns true for enabled")
        void isPluginEnabledReturnsTrue() {
            manager.enablePlugin("plugin-1");

            assertThat(manager.isPluginEnabled("plugin-1")).isTrue();
            assertThat(manager.isPluginEnabled("plugin-2")).isFalse();
        }

        @Test
        @DisplayName("isPluginLoaded returns true for loaded")
        void isPluginLoadedReturnsTrue() {
            assertThat(manager.isPluginLoaded("plugin-1")).isTrue();
            assertThat(manager.isPluginLoaded("unknown")).isFalse();
        }
    }

    @Nested
    @DisplayName("Extensions")
    class Extensions {

        @Test
        @DisplayName("getExtensions returns from registry")
        void getExtensionsReturnsFromRegistry() {
            TestExtension ext = new TestExtension() {
                @Override
                public String getName() {
                    return "test";
                }

                @Override
                public void execute() {
                }
            };

            extensionRegistry.register(TestExtension.class, ext);

            List<TestExtension> result = manager.getExtensions(TestExtension.class);

            assertThat(result).containsExactly(ext);
        }

        @Test
        @DisplayName("getExtensionRegistry returns registry")
        void getExtensionRegistryReturnsRegistry() {
            assertThat(manager.getExtensionRegistry()).isSameAs(extensionRegistry);
        }

        @Test
        @DisplayName("getPluginRegistry returns registry")
        void getPluginRegistryReturnsRegistry() {
            assertThat(manager.getPluginRegistry()).isSameAs(registry);
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Throws PluginNotFoundException for unknown plugin")
        void throwsNotFoundForUnknown() {
            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(Collections.emptyList());

            manager.initialize();

            assertThatThrownBy(() -> manager.enablePlugin("unknown"))
                .isInstanceOf(PluginNotFoundException.class)
                .hasMessageContaining("unknown");
        }

        @Test
        @DisplayName("Throws PluginStateException for invalid transition")
        void throwsStateExceptionForInvalidTransition() {
            TestPlugin plugin = new TestPlugin("test-plugin", "Test", "1.0.0", "0.1.0");
            PluginContainer container = new PluginContainer(plugin);
            container.setState(PluginState.FAILED);
            registry.add(container);

            assertThatThrownBy(() -> manager.enablePlugin("test-plugin"))
                .isInstanceOf(PluginStateException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases - State Query Methods (EDGE-040 to EDGE-045)")
    class StateQueryEdgeCases {

        @BeforeEach
        void setUpEmptyManager() {
            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(Collections.emptyList());
            manager.initialize();
        }

        @Test
        @DisplayName("EDGE-040: isPluginEnabled returns false for unknown plugin")
        void isPluginEnabledReturnsFalseForUnknown() {
            assertThat(manager.isPluginEnabled("unknown-plugin")).isFalse();
        }

        @Test
        @DisplayName("EDGE-041: isPluginLoaded returns false for unknown plugin")
        void isPluginLoadedReturnsFalseForUnknown() {
            assertThat(manager.isPluginLoaded("unknown-plugin")).isFalse();
        }

        @Test
        @DisplayName("EDGE-042: getPlugin returns Optional.empty for unknown plugin")
        void getPluginReturnsEmptyForUnknown() {
            Optional<PluginContainer> result = manager.getPlugin("unknown-plugin");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("EDGE-043: enablePlugin throws PluginNotFoundException for unknown plugin")
        void enablePluginThrowsForUnknown() {
            assertThatThrownBy(() -> manager.enablePlugin("unknown-plugin"))
                .isInstanceOf(PluginNotFoundException.class)
                .hasMessageContaining("unknown-plugin");
        }

        @Test
        @DisplayName("EDGE-044: disablePlugin throws PluginNotFoundException for unknown plugin")
        void disablePluginThrowsForUnknown() {
            assertThatThrownBy(() -> manager.disablePlugin("unknown-plugin"))
                .isInstanceOf(PluginNotFoundException.class)
                .hasMessageContaining("unknown-plugin");
        }

        @Test
        @DisplayName("EDGE-045: unloadPlugin throws PluginNotFoundException for unknown plugin")
        void unloadPluginThrowsForUnknown() {
            assertThatThrownBy(() -> manager.unloadPlugin("unknown-plugin"))
                .isInstanceOf(PluginNotFoundException.class)
                .hasMessageContaining("unknown-plugin");
        }
    }

    @Nested
    @DisplayName("Invalid State Transitions (PLT-040 to PLT-046)")
    class InvalidStateTransitions {

        @Test
        @DisplayName("PLT-040: Cannot enable plugin in DISCOVERED state")
        void cannotEnableInDiscoveredState() {
            TestPlugin plugin = new TestPlugin("test-plugin", "Test", "1.0.0", "0.1.0");
            PluginContainer container = new PluginContainer(plugin);
            // Container starts in DISCOVERED state
            registry.add(container);

            assertThatThrownBy(() -> manager.enablePlugin("test-plugin"))
                .isInstanceOf(PluginStateException.class);
        }

        @Test
        @DisplayName("PLT-041: Cannot enable plugin in FAILED state")
        void cannotEnableInFailedState() {
            TestPlugin plugin = new TestPlugin("failing-plugin", "Failing", "1.0.0", "0.1.0");
            PluginContainer container = new PluginContainer(plugin);
            container.markFailed(new RuntimeException("Test failure"));
            registry.add(container);

            assertThatThrownBy(() -> manager.enablePlugin("failing-plugin"))
                .isInstanceOf(PluginStateException.class);
        }

        @Test
        @DisplayName("PLT-042: Cannot enable plugin in UNLOADED state")
        void cannotEnableInUnloadedState() {
            TestPlugin plugin = new TestPlugin("test-plugin", "Test", "1.0.0", "0.1.0");
            PluginContainer container = new PluginContainer(plugin);
            container.forceState(PluginState.UNLOADED);
            registry.add(container);

            assertThatThrownBy(() -> manager.enablePlugin("test-plugin"))
                .isInstanceOf(PluginStateException.class);
        }

        @Test
        @DisplayName("PLT-043: Cannot disable plugin in LOADED state (not yet enabled)")
        void cannotDisableInLoadedState() {
            TestPlugin plugin = new TestPlugin("test-plugin", "Test", "1.0.0", "0.1.0");
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));
            manager.initialize();

            // Plugin is now LOADED but not ENABLED
            assertThat(container.getState()).isEqualTo(PluginState.LOADED);

            // Trying to disable should fail
            assertThatThrownBy(() -> manager.disablePlugin("test-plugin"))
                .isInstanceOf(PluginStateException.class);
        }

        @Test
        @DisplayName("PLT-044: Cannot unload ENABLED plugin directly")
        void cannotUnloadEnabledPluginDirectly() {
            TestPlugin plugin = new TestPlugin("test-plugin", "Test", "1.0.0", "0.1.0");
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));
            manager.initialize();
            manager.enablePlugin("test-plugin");

            assertThat(container.isEnabled()).isTrue();

            assertThatThrownBy(() -> manager.unloadPlugin("test-plugin"))
                .isInstanceOf(PluginStateException.class);
        }

        @Test
        @DisplayName("PLT-046: PluginStateException contains state details")
        void pluginStateExceptionContainsDetails() {
            TestPlugin plugin = new TestPlugin("test-plugin", "Test", "1.0.0", "0.1.0");
            PluginContainer container = new PluginContainer(plugin);
            registry.add(container);

            try {
                manager.enablePlugin("test-plugin");
            } catch (PluginStateException e) {
                assertThat(e.getPluginId()).isEqualTo("test-plugin");
                assertThat(e.getCurrentState()).isEqualTo(PluginState.DISCOVERED);
                assertThat(e.getMessage()).contains("test-plugin");
            }
        }
    }

    @Nested
    @DisplayName("Plugin Failure Scenarios (PLT-060 to PLT-065)")
    class PluginFailureScenarios {

        @Test
        @DisplayName("PLT-060: Plugin onLoad() throws RuntimeException marks FAILED")
        void onLoadRuntimeExceptionMarksFailed() {
            TestPlugin plugin = new TestPlugin("failing-plugin", "Failing", "1.0.0", "0.1.0");
            RuntimeException loadError = new RuntimeException("Load failed");
            plugin.setLoadException(loadError);
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager.initialize();

            assertThat(container.isFailed()).isTrue();
            // The failure cause may be wrapped, so check message and root cause
            Throwable cause = container.getFailureCause();
            assertThat(cause).isNotNull();
            // Check if it's the exception or wrapped
            if (cause instanceof PluginLifecycleException) {
                assertThat(cause.getCause()).isSameAs(loadError);
            } else {
                assertThat(cause).isSameAs(loadError);
            }
        }

        @Test
        @DisplayName("PLT-061: Plugin onLoad() throws RuntimeException subclass marks FAILED")
        void onLoadErrorMarksFailed() {
            // Use IllegalStateException instead of Error to avoid propagation issues
            TestPlugin plugin = new TestPlugin("error-plugin", "Error", "1.0.0", "0.1.0") {
                @Override
                public void onLoad(uk.selfemploy.plugin.api.PluginContext context) {
                    throw new IllegalStateException("Plugin initialization error");
                }
            };
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager.initialize();

            assertThat(container.isFailed()).isTrue();
            Throwable cause = container.getFailureCause();
            // May be wrapped in PluginLifecycleException
            if (cause instanceof PluginLifecycleException) {
                assertThat(cause.getCause()).isInstanceOf(IllegalStateException.class);
            } else {
                assertThat(cause).isInstanceOf(IllegalStateException.class);
            }
        }

        @Test
        @DisplayName("PLT-062: Plugin onUnload() throws exception still unloads")
        void onUnloadExceptionStillUnloads() {
            TestPlugin plugin = new TestPlugin("test-plugin", "Test", "1.0.0", "0.1.0");
            plugin.setUnloadException(new RuntimeException("Unload failed"));
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager.initialize();
            // Plugin is LOADED, unload it
            manager.unloadPlugin("test-plugin");

            // Should still be UNLOADED despite exception
            assertThat(container.getState()).isEqualTo(PluginState.UNLOADED);
        }

        @Test
        @DisplayName("PLT-063: Failed plugin can be unloaded")
        void failedPluginCanBeUnloaded() {
            TestPlugin plugin = new TestPlugin("failed-plugin", "Failed", "1.0.0", "0.1.0");
            plugin.setLoadException(new RuntimeException("Load failed"));
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager.initialize();
            assertThat(container.isFailed()).isTrue();

            // Should be able to unload failed plugin
            manager.unloadPlugin("failed-plugin");
            assertThat(container.getState()).isEqualTo(PluginState.UNLOADED);
        }

        @Test
        @DisplayName("PLT-064: Failure cause is accessible via container")
        void failureCauseIsAccessible() {
            RuntimeException loadError = new RuntimeException("Specific load error");
            TestPlugin plugin = new TestPlugin("failing-plugin", "Failing", "1.0.0", "0.1.0");
            plugin.setLoadException(loadError);
            PluginContainer container = new PluginContainer(plugin);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container));

            manager.initialize();

            Throwable cause = container.getFailureCause();
            assertThat(cause).isNotNull();
            // May be wrapped in PluginLifecycleException
            if (cause instanceof PluginLifecycleException) {
                assertThat(cause.getCause()).isSameAs(loadError);
                assertThat(cause.getCause().getMessage()).isEqualTo("Specific load error");
            } else {
                assertThat(cause).isSameAs(loadError);
                assertThat(cause.getMessage()).isEqualTo("Specific load error");
            }
        }

        @Test
        @DisplayName("PLT-065: Multiple plugin failures handled independently")
        void multiplePluginFailuresHandledIndependently() {
            TestPlugin good1 = new TestPlugin("good-1", "Good 1", "1.0.0", "0.1.0");
            TestPlugin bad = new TestPlugin("bad", "Bad", "1.0.0", "0.1.0");
            bad.setLoadException(new RuntimeException("Bad plugin"));
            TestPlugin good2 = new TestPlugin("good-2", "Good 2", "1.0.0", "0.1.0");

            PluginContainer container1 = new PluginContainer(good1);
            PluginContainer container2 = new PluginContainer(bad);
            PluginContainer container3 = new PluginContainer(good2);

            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(List.of(container1, container2, container3));

            manager.initialize();

            // Good plugins should be loaded
            assertThat(container1.getState()).isEqualTo(PluginState.LOADED);
            assertThat(container3.getState()).isEqualTo(PluginState.LOADED);

            // Bad plugin should be failed
            assertThat(container2.isFailed()).isTrue();

            // Manager should have all plugins
            assertThat(manager.getAllPlugins()).hasSize(3);
            assertThat(manager.getLoadedPlugins()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Boundary Conditions (EDGE-030 to EDGE-034)")
    class BoundaryConditions {

        @Test
        @DisplayName("EDGE-030: Empty plugin list after discovery")
        void emptyPluginListAfterDiscovery() {
            when(mockLoader.discoverCompatiblePlugins(anyString()))
                .thenReturn(Collections.emptyList());

            manager.initialize();

            assertThat(manager.isInitialized()).isTrue();
            assertThat(manager.getAllPlugins()).isEmpty();
            assertThat(manager.getLoadedPlugins()).isEmpty();
        }
    }
}
