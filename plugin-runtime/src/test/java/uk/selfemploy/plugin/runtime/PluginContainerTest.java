package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PluginContainer")
class PluginContainerTest {

    private TestPlugin testPlugin;
    private PluginContainer container;

    @BeforeEach
    void setUp() {
        testPlugin = new TestPlugin("test-plugin", "Test Plugin", "1.0.0", "0.1.0");
        container = new PluginContainer(testPlugin);
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("Creates container with DISCOVERED state")
        void createsWithDiscoveredState() {
            assertThat(container.getState()).isEqualTo(PluginState.DISCOVERED);
        }

        @Test
        @DisplayName("Stores plugin reference")
        void storesPluginReference() {
            assertThat(container.getPlugin()).isSameAs(testPlugin);
        }

        @Test
        @DisplayName("Stores descriptor from plugin")
        void storesDescriptor() {
            assertThat(container.getDescriptor()).isEqualTo(testPlugin.getDescriptor());
        }

        @Test
        @DisplayName("Returns plugin ID from descriptor")
        void returnsPluginId() {
            assertThat(container.getId()).isEqualTo("test-plugin");
        }

        @Test
        @DisplayName("Throws on null plugin")
        void throwsOnNullPlugin() {
            assertThatThrownBy(() -> new PluginContainer(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("plugin");
        }
    }

    @Nested
    @DisplayName("State management")
    class StateManagement {

        @Test
        @DisplayName("Can transition from DISCOVERED to LOADED")
        void canTransitionToLoaded() {
            container.setState(PluginState.LOADED);
            assertThat(container.getState()).isEqualTo(PluginState.LOADED);
        }

        @Test
        @DisplayName("Throws on invalid state transition")
        void throwsOnInvalidTransition() {
            assertThatThrownBy(() -> container.setState(PluginState.ENABLED))
                .isInstanceOf(PluginStateException.class)
                .hasMessageContaining("DISCOVERED")
                .hasMessageContaining("ENABLED");
        }

        @Test
        @DisplayName("forceState bypasses transition validation")
        void forceStateBypassesValidation() {
            container.forceState(PluginState.ENABLED);
            assertThat(container.getState()).isEqualTo(PluginState.ENABLED);
        }
    }

    @Nested
    @DisplayName("Context management")
    class ContextManagement {

        @Test
        @DisplayName("Context is null initially")
        void contextIsNullInitially() {
            assertThat(container.getContext()).isNull();
        }

        @Test
        @DisplayName("Can set and get context")
        void canSetAndGetContext() {
            PluginContextImpl context = new PluginContextImpl("1.0.0",
                java.nio.file.Paths.get("/tmp/test-plugin"));
            container.setContext(context);
            assertThat(container.getContext()).isSameAs(context);
        }
    }

    @Nested
    @DisplayName("Failure handling")
    class FailureHandling {

        @Test
        @DisplayName("markFailed sets state to FAILED")
        void markFailedSetsState() {
            container.markFailed(new RuntimeException("Test error"));
            assertThat(container.getState()).isEqualTo(PluginState.FAILED);
        }

        @Test
        @DisplayName("markFailed stores failure cause")
        void markFailedStoresCause() {
            RuntimeException cause = new RuntimeException("Test error");
            container.markFailed(cause);
            assertThat(container.getFailureCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("isFailed returns true when failed")
        void isFailedReturnsTrue() {
            container.markFailed(new RuntimeException("Test error"));
            assertThat(container.isFailed()).isTrue();
        }
    }

    @Nested
    @DisplayName("State predicates")
    class StatePredicates {

        @Test
        @DisplayName("isEnabled returns true when ENABLED")
        void isEnabledWhenEnabled() {
            container.setState(PluginState.LOADED);
            container.setState(PluginState.ENABLED);
            assertThat(container.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("isEnabled returns false when LOADED")
        void isNotEnabledWhenLoaded() {
            container.setState(PluginState.LOADED);
            assertThat(container.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("isLoaded returns true when LOADED")
        void isLoadedWhenLoaded() {
            container.setState(PluginState.LOADED);
            assertThat(container.isLoaded()).isTrue();
        }

        @Test
        @DisplayName("isLoaded returns true when ENABLED")
        void isLoadedWhenEnabled() {
            container.setState(PluginState.LOADED);
            container.setState(PluginState.ENABLED);
            assertThat(container.isLoaded()).isTrue();
        }

        @Test
        @DisplayName("isLoaded returns true when DISABLED")
        void isLoadedWhenDisabled() {
            container.setState(PluginState.LOADED);
            container.setState(PluginState.ENABLED);
            container.setState(PluginState.DISABLED);
            assertThat(container.isLoaded()).isTrue();
        }

        @Test
        @DisplayName("isLoaded returns false when DISCOVERED")
        void isNotLoadedWhenDiscovered() {
            assertThat(container.isLoaded()).isFalse();
        }
    }

    @Test
    @DisplayName("toString includes plugin info")
    void toStringIncludesPluginInfo() {
        String str = container.toString();
        assertThat(str)
            .contains("test-plugin")
            .contains("Test Plugin")
            .contains("1.0.0")
            .contains("DISCOVERED");
    }
}
