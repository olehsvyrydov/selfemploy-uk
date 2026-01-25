package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Plugin Exceptions")
class PluginExceptionTest {

    @Nested
    @DisplayName("PluginException")
    class PluginExceptionTests {

        @Test
        @DisplayName("Creates with message")
        void createsWithMessage() {
            PluginException ex = new PluginException("Test error");

            assertThat(ex.getMessage()).isEqualTo("Test error");
        }

        @Test
        @DisplayName("Creates with message and cause")
        void createsWithMessageAndCause() {
            RuntimeException cause = new RuntimeException("Cause");
            PluginException ex = new PluginException("Test error", cause);

            assertThat(ex.getMessage()).isEqualTo("Test error");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("PluginNotFoundException")
    class PluginNotFoundExceptionTests {

        @Test
        @DisplayName("Creates with plugin ID")
        void createsWithPluginId() {
            PluginNotFoundException ex = new PluginNotFoundException("my-plugin");

            assertThat(ex.getMessage()).contains("my-plugin");
            assertThat(ex.getPluginId()).isEqualTo("my-plugin");
        }
    }

    @Nested
    @DisplayName("PluginLoadException")
    class PluginLoadExceptionTests {

        @Test
        @DisplayName("Creates with plugin ID and message")
        void createsWithPluginIdAndMessage() {
            PluginLoadException ex = new PluginLoadException("my-plugin", "class not found");

            assertThat(ex.getMessage()).contains("my-plugin");
            assertThat(ex.getMessage()).contains("class not found");
            assertThat(ex.getPluginId()).isEqualTo("my-plugin");
        }

        @Test
        @DisplayName("Creates with cause")
        void createsWithCause() {
            RuntimeException cause = new RuntimeException("Root cause");
            PluginLoadException ex = new PluginLoadException("my-plugin", "error", cause);

            assertThat(ex.getCause()).isSameAs(cause);
            assertThat(ex.getPluginId()).isEqualTo("my-plugin");
        }
    }

    @Nested
    @DisplayName("PluginStateException")
    class PluginStateExceptionTests {

        @Test
        @DisplayName("Creates with state information")
        void createsWithStateInfo() {
            PluginStateException ex = new PluginStateException(
                "my-plugin",
                PluginState.DISCOVERED,
                PluginState.ENABLED
            );

            assertThat(ex.getMessage()).contains("my-plugin");
            assertThat(ex.getMessage()).contains("DISCOVERED");
            assertThat(ex.getMessage()).contains("ENABLED");
            assertThat(ex.getPluginId()).isEqualTo("my-plugin");
            assertThat(ex.getCurrentState()).isEqualTo(PluginState.DISCOVERED);
            assertThat(ex.getTargetState()).isEqualTo(PluginState.ENABLED);
        }
    }

    @Nested
    @DisplayName("PluginLifecycleException")
    class PluginLifecycleExceptionTests {

        @Test
        @DisplayName("Creates with lifecycle method info")
        void createsWithLifecycleMethod() {
            RuntimeException cause = new RuntimeException("Error in onLoad");
            PluginLifecycleException ex = new PluginLifecycleException(
                "my-plugin",
                "onLoad",
                cause
            );

            assertThat(ex.getMessage()).contains("my-plugin");
            assertThat(ex.getMessage()).contains("onLoad");
            assertThat(ex.getPluginId()).isEqualTo("my-plugin");
            assertThat(ex.getLifecycleMethod()).isEqualTo("onLoad");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }
}
