package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PluginRegistry")
class PluginRegistryTest {

    private PluginRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PluginRegistry();
    }

    @Nested
    @DisplayName("Add operations")
    class AddOperations {

        @Test
        @DisplayName("Adds plugin to registry")
        void addsPlugin() {
            PluginContainer container = createContainer("plugin-1");

            registry.add(container);

            assertThat(registry.contains("plugin-1")).isTrue();
        }

        @Test
        @DisplayName("Replaces existing plugin with same ID")
        void replacesExisting() {
            PluginContainer first = createContainer("plugin-1");
            PluginContainer second = createContainer("plugin-1");

            registry.add(first);
            registry.add(second);

            assertThat(registry.get("plugin-1")).contains(second);
            assertThat(registry.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("Adds multiple plugins")
        void addsMultiple() {
            registry.addAll(List.of(
                createContainer("plugin-1"),
                createContainer("plugin-2"),
                createContainer("plugin-3")
            ));

            assertThat(registry.size()).isEqualTo(3);
        }

        @Test
        @DisplayName("Throws on null container")
        void throwsOnNull() {
            assertThatThrownBy(() -> registry.add(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("container");
        }

        @Test
        @DisplayName("EDGE-020: Duplicate plugin ID replaces existing")
        void duplicatePluginIdReplacesExisting() {
            PluginContainer first = createContainer("duplicate-plugin");
            PluginContainer second = createContainer("duplicate-plugin");

            registry.add(first);
            registry.add(second);

            // Second should replace first
            assertThat(registry.get("duplicate-plugin")).contains(second);
            assertThat(registry.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Remove operations")
    class RemoveOperations {

        @Test
        @DisplayName("Removes plugin from registry")
        void removesPlugin() {
            registry.add(createContainer("plugin-1"));

            Optional<PluginContainer> removed = registry.remove("plugin-1");

            assertThat(removed).isPresent();
            assertThat(registry.contains("plugin-1")).isFalse();
        }

        @Test
        @DisplayName("Returns empty when plugin not found")
        void returnsEmptyWhenNotFound() {
            Optional<PluginContainer> removed = registry.remove("unknown");

            assertThat(removed).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get operations")
    class GetOperations {

        @Test
        @DisplayName("Gets plugin by ID")
        void getsById() {
            PluginContainer container = createContainer("plugin-1");
            registry.add(container);

            Optional<PluginContainer> result = registry.get("plugin-1");

            assertThat(result).contains(container);
        }

        @Test
        @DisplayName("Returns empty when not found")
        void returnsEmptyWhenNotFound() {
            Optional<PluginContainer> result = registry.get("unknown");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getOrThrow returns plugin")
        void getOrThrowReturns() {
            PluginContainer container = createContainer("plugin-1");
            registry.add(container);

            PluginContainer result = registry.getOrThrow("plugin-1");

            assertThat(result).isSameAs(container);
        }

        @Test
        @DisplayName("getOrThrow throws when not found")
        void getOrThrowThrows() {
            assertThatThrownBy(() -> registry.getOrThrow("unknown"))
                .isInstanceOf(PluginNotFoundException.class)
                .hasMessageContaining("unknown");
        }

        @Test
        @DisplayName("EDGE-005: get with null pluginId throws NullPointerException")
        void getWithNullPluginIdThrows() {
            assertThatThrownBy(() -> registry.get(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Query operations")
    class QueryOperations {

        @Test
        @DisplayName("Gets all plugins")
        void getsAll() {
            registry.add(createContainer("plugin-1"));
            registry.add(createContainer("plugin-2"));

            List<PluginContainer> all = registry.getAll();

            assertThat(all).hasSize(2);
        }

        @Test
        @DisplayName("getAll returns unmodifiable list")
        void getAllReturnsUnmodifiable() {
            registry.add(createContainer("plugin-1"));

            List<PluginContainer> all = registry.getAll();

            assertThatThrownBy(() -> all.add(createContainer("new")))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Gets plugins by state")
        void getsByState() {
            PluginContainer discovered = createContainer("plugin-1");
            PluginContainer loaded = createContainer("plugin-2");
            loaded.setState(PluginState.LOADED);

            registry.add(discovered);
            registry.add(loaded);

            List<PluginContainer> result = registry.getByState(PluginState.LOADED);

            assertThat(result).containsExactly(loaded);
        }

        @Test
        @DisplayName("Gets enabled plugins")
        void getsEnabled() {
            PluginContainer enabled = createContainer("plugin-1");
            enabled.setState(PluginState.LOADED);
            enabled.setState(PluginState.ENABLED);

            PluginContainer disabled = createContainer("plugin-2");
            disabled.setState(PluginState.LOADED);

            registry.add(enabled);
            registry.add(disabled);

            List<PluginContainer> result = registry.getEnabled();

            assertThat(result).containsExactly(enabled);
        }

        @Test
        @DisplayName("Gets loaded plugins")
        void getsLoaded() {
            PluginContainer loaded = createContainer("plugin-1");
            loaded.setState(PluginState.LOADED);

            PluginContainer enabled = createContainer("plugin-2");
            enabled.setState(PluginState.LOADED);
            enabled.setState(PluginState.ENABLED);

            PluginContainer discovered = createContainer("plugin-3");

            registry.add(loaded);
            registry.add(enabled);
            registry.add(discovered);

            List<PluginContainer> result = registry.getLoaded();

            assertThat(result).containsExactlyInAnyOrder(loaded, enabled);
        }

        @Test
        @DisplayName("Gets failed plugins")
        void getsFailed() {
            PluginContainer failed = createContainer("plugin-1");
            failed.markFailed(new RuntimeException("error"));

            PluginContainer loaded = createContainer("plugin-2");
            loaded.setState(PluginState.LOADED);

            registry.add(failed);
            registry.add(loaded);

            List<PluginContainer> result = registry.getFailed();

            assertThat(result).containsExactly(failed);
        }

        @Test
        @DisplayName("Finds plugins by predicate")
        void findsByPredicate() {
            registry.add(createContainer("plugin-1"));
            registry.add(createContainer("plugin-2"));
            registry.add(createContainer("other-plugin"));

            List<PluginContainer> result = registry.find(c -> c.getId().startsWith("plugin"));

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Utility methods")
    class UtilityMethods {

        @Test
        @DisplayName("isEmpty returns true when empty")
        void isEmptyWhenEmpty() {
            assertThat(registry.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("isEmpty returns false when not empty")
        void isNotEmptyWhenNotEmpty() {
            registry.add(createContainer("plugin-1"));
            assertThat(registry.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("clear removes all plugins")
        void clearRemovesAll() {
            registry.add(createContainer("plugin-1"));
            registry.add(createContainer("plugin-2"));

            registry.clear();

            assertThat(registry.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("getStateSummary returns counts by state")
        void getStateSummary() {
            PluginContainer discovered = createContainer("plugin-1");

            PluginContainer loaded = createContainer("plugin-2");
            loaded.setState(PluginState.LOADED);

            PluginContainer enabled = createContainer("plugin-3");
            enabled.setState(PluginState.LOADED);
            enabled.setState(PluginState.ENABLED);

            registry.add(discovered);
            registry.add(loaded);
            registry.add(enabled);

            Map<PluginState, Long> summary = registry.getStateSummary();

            assertThat(summary.get(PluginState.DISCOVERED)).isEqualTo(1L);
            assertThat(summary.get(PluginState.LOADED)).isEqualTo(1L);
            assertThat(summary.get(PluginState.ENABLED)).isEqualTo(1L);
        }
    }

    private PluginContainer createContainer(String pluginId) {
        return new PluginContainer(
            new TestPlugin(pluginId, "Test Plugin " + pluginId, "1.0.0", "0.1.0")
        );
    }
}
