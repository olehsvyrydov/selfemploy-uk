package uk.selfemploy.plugin.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Plugin} interface.
 * Tests validate the plugin interface contract and ServiceLoader integration.
 */
@DisplayName("Plugin")
class PluginTest {

    @Nested
    @DisplayName("interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("should be loadable via ServiceLoader")
        void shouldBeLoadableViaServiceLoader() {
            // Verify that ServiceLoader can work with Plugin interface
            ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class);
            assertThat(loader).isNotNull();
        }

        @Test
        @DisplayName("should allow implementing all methods")
        void shouldAllowImplementingAllMethods() {
            // Create a test implementation
            Plugin plugin = new TestPlugin();

            assertThat(plugin.getDescriptor()).isNotNull();
            assertThat(plugin.getDescriptor().id()).isEqualTo("test.plugin");
        }
    }

    @Nested
    @DisplayName("when used with PluginContext")
    class WithPluginContext {

        @Test
        @DisplayName("should receive context on load")
        void shouldReceiveContextOnLoad() {
            TestPlugin plugin = new TestPlugin();
            PluginContext context = new TestPluginContext();

            plugin.onLoad(context);

            assertThat(plugin.isLoaded()).isTrue();
            assertThat(plugin.getReceivedContext()).isEqualTo(context);
        }

        @Test
        @DisplayName("should clean up on unload")
        void shouldCleanUpOnUnload() {
            TestPlugin plugin = new TestPlugin();
            plugin.onLoad(new TestPluginContext());

            plugin.onUnload();

            assertThat(plugin.isLoaded()).isFalse();
        }
    }

    /**
     * Test implementation of Plugin for testing purposes.
     */
    private static class TestPlugin implements Plugin {
        private boolean loaded = false;
        private PluginContext context;

        @Override
        public PluginDescriptor getDescriptor() {
            return new PluginDescriptor(
                "test.plugin",
                "Test Plugin",
                "1.0.0",
                "A test plugin",
                "Test Author",
                "0.1.0"
            );
        }

        @Override
        public void onLoad(PluginContext context) {
            this.context = context;
            this.loaded = true;
        }

        @Override
        public void onUnload() {
            this.loaded = false;
            this.context = null;
        }

        public boolean isLoaded() {
            return loaded;
        }

        public PluginContext getReceivedContext() {
            return context;
        }
    }

    /**
     * Test implementation of PluginContext for testing purposes.
     */
    private static class TestPluginContext implements PluginContext {
        @Override
        public String getAppVersion() {
            return "0.1.0";
        }

        @Override
        public Path getPluginDataDirectory() {
            return Path.of(System.getProperty("java.io.tmpdir"), "test-plugin-data");
        }

        @Override
        public Set<PluginPermission> getGrantedPermissions() {
            return Collections.emptySet();
        }
    }
}
