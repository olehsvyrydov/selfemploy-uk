package uk.selfemploy.plugin.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PluginDependency}.
 */
@DisplayName("PluginDependency")
class PluginDependencyTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create dependency with valid parameters")
        void shouldCreateDependencyWithValidParameters() {
            var dependency = new PluginDependency(
                "uk.selfemploy.plugin.base",
                "^1.0.0",
                false
            );

            assertThat(dependency.pluginId()).isEqualTo("uk.selfemploy.plugin.base");
            assertThat(dependency.versionRange()).isEqualTo("^1.0.0");
            assertThat(dependency.optional()).isFalse();
        }

        @Test
        @DisplayName("should create optional dependency")
        void shouldCreateOptionalDependency() {
            var dependency = new PluginDependency(
                "uk.selfemploy.plugin.optional",
                ">=1.0.0",
                true
            );

            assertThat(dependency.optional()).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should reject null or blank pluginId")
        void shouldRejectNullOrBlankPluginId(String pluginId) {
            assertThatThrownBy(() -> new PluginDependency(pluginId, "^1.0.0", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginId must not be null or blank");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should reject null or blank versionRange")
        void shouldRejectNullOrBlankVersionRange(String versionRange) {
            assertThatThrownBy(() -> new PluginDependency("uk.selfemploy.plugin.base", versionRange, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("versionRange must not be null or blank");
        }
    }

    @Nested
    @DisplayName("isRequired()")
    class IsRequired {

        @Test
        @DisplayName("should return true when dependency is not optional")
        void shouldReturnTrueWhenNotOptional() {
            var dependency = new PluginDependency(
                "uk.selfemploy.plugin.required",
                "^1.0.0",
                false
            );

            assertThat(dependency.isRequired()).isTrue();
        }

        @Test
        @DisplayName("should return false when dependency is optional")
        void shouldReturnFalseWhenOptional() {
            var dependency = new PluginDependency(
                "uk.selfemploy.plugin.optional",
                "^1.0.0",
                true
            );

            assertThat(dependency.isRequired()).isFalse();
        }
    }

    @Nested
    @DisplayName("Record behavior")
    class RecordBehavior {

        @Test
        @DisplayName("should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            var dependency1 = new PluginDependency("plugin.a", "^1.0.0", false);
            var dependency2 = new PluginDependency("plugin.a", "^1.0.0", false);
            var dependency3 = new PluginDependency("plugin.b", "^1.0.0", false);

            assertThat(dependency1).isEqualTo(dependency2);
            assertThat(dependency1).isNotEqualTo(dependency3);
        }

        @Test
        @DisplayName("should implement hashCode correctly")
        void shouldImplementHashCodeCorrectly() {
            var dependency1 = new PluginDependency("plugin.a", "^1.0.0", false);
            var dependency2 = new PluginDependency("plugin.a", "^1.0.0", false);

            assertThat(dependency1.hashCode()).isEqualTo(dependency2.hashCode());
        }

        @Test
        @DisplayName("should implement toString")
        void shouldImplementToString() {
            var dependency = new PluginDependency("plugin.a", "^1.0.0", true);

            assertThat(dependency.toString())
                .contains("plugin.a")
                .contains("^1.0.0")
                .contains("true");
        }
    }

    @Nested
    @DisplayName("Version range formats")
    class VersionRangeFormats {

        @Test
        @DisplayName("should accept exact version")
        void shouldAcceptExactVersion() {
            var dependency = new PluginDependency("plugin.a", "1.0.0", false);
            assertThat(dependency.versionRange()).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("should accept caret range")
        void shouldAcceptCaretRange() {
            var dependency = new PluginDependency("plugin.a", "^1.2.3", false);
            assertThat(dependency.versionRange()).isEqualTo("^1.2.3");
        }

        @Test
        @DisplayName("should accept tilde range")
        void shouldAcceptTildeRange() {
            var dependency = new PluginDependency("plugin.a", "~1.2.3", false);
            assertThat(dependency.versionRange()).isEqualTo("~1.2.3");
        }

        @Test
        @DisplayName("should accept range with comparators")
        void shouldAcceptRangeWithComparators() {
            var dependency = new PluginDependency("plugin.a", ">=1.0.0 <2.0.0", false);
            assertThat(dependency.versionRange()).isEqualTo(">=1.0.0 <2.0.0");
        }
    }
}
