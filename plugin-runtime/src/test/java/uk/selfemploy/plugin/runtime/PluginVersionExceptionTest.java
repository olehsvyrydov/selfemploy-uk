package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PluginVersionException}.
 */
@DisplayName("PluginVersionException")
class PluginVersionExceptionTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("Creates exception with plugin ID, current and required versions")
        void createsWithVersionInfo() {
            PluginVersionException exception = new PluginVersionException(
                "test-plugin",
                "1.0.0",
                "2.0.0"
            );

            assertThat(exception.getMessage())
                .contains("test-plugin")
                .contains("1.0.0")
                .contains("2.0.0");
            assertThat(exception.getPluginId()).isEqualTo("test-plugin");
            assertThat(exception.getCurrentVersion()).isEqualTo("1.0.0");
            assertThat(exception.getRequiredVersion()).isEqualTo("2.0.0");
        }

        @Test
        @DisplayName("Creates exception with custom message")
        void createsWithCustomMessage() {
            PluginVersionException exception = new PluginVersionException(
                "test-plugin",
                "1.0.0",
                "2.0.0",
                "Custom version mismatch message"
            );

            assertThat(exception.getMessage()).contains("Custom version mismatch message");
            assertThat(exception.getPluginId()).isEqualTo("test-plugin");
        }
    }

    @Nested
    @DisplayName("Version information")
    class VersionInformation {

        @Test
        @DisplayName("Returns correct current version")
        void returnsCurrentVersion() {
            PluginVersionException exception = new PluginVersionException(
                "plugin", "1.2.3", "2.0.0"
            );
            assertThat(exception.getCurrentVersion()).isEqualTo("1.2.3");
        }

        @Test
        @DisplayName("Returns correct required version")
        void returnsRequiredVersion() {
            PluginVersionException exception = new PluginVersionException(
                "plugin", "1.0.0", "3.5.0"
            );
            assertThat(exception.getRequiredVersion()).isEqualTo("3.5.0");
        }

        @Test
        @DisplayName("Handles SNAPSHOT versions")
        void handlesSnapshotVersions() {
            PluginVersionException exception = new PluginVersionException(
                "plugin", "1.0.0-SNAPSHOT", "1.0.0"
            );
            assertThat(exception.getCurrentVersion()).isEqualTo("1.0.0-SNAPSHOT");
        }
    }

    @Nested
    @DisplayName("Inheritance")
    class Inheritance {

        @Test
        @DisplayName("Extends PluginLoadException")
        void extendsPluginLoadException() {
            PluginVersionException exception = new PluginVersionException(
                "plugin", "1.0.0", "2.0.0"
            );
            assertThat(exception).isInstanceOf(PluginLoadException.class);
        }

        @Test
        @DisplayName("Extends PluginException")
        void extendsPluginException() {
            PluginVersionException exception = new PluginVersionException(
                "plugin", "1.0.0", "2.0.0"
            );
            assertThat(exception).isInstanceOf(PluginException.class);
        }
    }
}
