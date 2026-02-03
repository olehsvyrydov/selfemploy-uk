package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.plugin.api.PluginDependency;
import uk.selfemploy.plugin.api.PluginDescriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DependencyResolver}.
 */
@DisplayName("DependencyResolver")
class DependencyResolverTest {

    private DependencyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DependencyResolver();
    }

    private PluginDescriptor createDescriptor(String id, String version) {
        return new PluginDescriptor(id, "Test " + id, version, null, null, "1.0.0");
    }

    @Nested
    @DisplayName("resolve()")
    class Resolve {

        @Test
        @DisplayName("should handle empty plugin list")
        void shouldHandleEmptyPluginList() {
            var result = resolver.resolve(Map.of(), Map.of());

            assertThat(result.loadOrder()).isEmpty();
            assertThat(result.blocked()).isEmpty();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("should handle null plugin list")
        void shouldHandleNullPluginList() {
            var result = resolver.resolve(null, Map.of());

            assertThat(result.loadOrder()).isEmpty();
        }

        @Test
        @DisplayName("should return single plugin with no dependencies")
        void shouldReturnSinglePluginWithNoDependencies() {
            Map<String, PluginDescriptor> plugins = Map.of(
                "plugin.a", createDescriptor("plugin.a", "1.0.0")
            );

            var result = resolver.resolve(plugins, Map.of());

            assertThat(result.loadOrder()).containsExactly("plugin.a");
            assertThat(result.blocked()).isEmpty();
        }

        @Test
        @DisplayName("should order dependencies before dependents")
        void shouldOrderDependenciesBeforeDependents() {
            Map<String, PluginDescriptor> plugins = Map.of(
                "plugin.a", createDescriptor("plugin.a", "1.0.0"),
                "plugin.b", createDescriptor("plugin.b", "1.0.0")
            );
            Map<String, List<PluginDependency>> deps = Map.of(
                "plugin.b", List.of(new PluginDependency("plugin.a", "^1.0.0", false))
            );

            var result = resolver.resolve(plugins, deps);

            assertThat(result.loadOrder()).containsExactly("plugin.a", "plugin.b");
        }

        @Test
        @DisplayName("should handle complex dependency graph")
        void shouldHandleComplexDependencyGraph() {
            // A -> B -> D
            // A -> C -> D
            Map<String, PluginDescriptor> plugins = new HashMap<>();
            plugins.put("plugin.a", createDescriptor("plugin.a", "1.0.0"));
            plugins.put("plugin.b", createDescriptor("plugin.b", "1.0.0"));
            plugins.put("plugin.c", createDescriptor("plugin.c", "1.0.0"));
            plugins.put("plugin.d", createDescriptor("plugin.d", "1.0.0"));

            Map<String, List<PluginDependency>> deps = new HashMap<>();
            deps.put("plugin.b", List.of(new PluginDependency("plugin.a", "^1.0.0", false)));
            deps.put("plugin.c", List.of(new PluginDependency("plugin.a", "^1.0.0", false)));
            deps.put("plugin.d", List.of(
                new PluginDependency("plugin.b", "^1.0.0", false),
                new PluginDependency("plugin.c", "^1.0.0", false)
            ));

            var result = resolver.resolve(plugins, deps);

            // A must be first, D must be last
            assertThat(result.loadOrder().get(0)).isEqualTo("plugin.a");
            assertThat(result.loadOrder().get(3)).isEqualTo("plugin.d");
            // B and C must be after A and before D
            assertThat(result.loadOrder()).containsAll(List.of("plugin.b", "plugin.c"));
        }
    }

    @Nested
    @DisplayName("Missing dependencies")
    class MissingDependencies {

        @Test
        @DisplayName("should block plugin with missing required dependency")
        void shouldBlockPluginWithMissingRequiredDependency() {
            Map<String, PluginDescriptor> plugins = Map.of(
                "plugin.a", createDescriptor("plugin.a", "1.0.0")
            );
            Map<String, List<PluginDependency>> deps = Map.of(
                "plugin.a", List.of(new PluginDependency("plugin.missing", "^1.0.0", false))
            );

            var result = resolver.resolve(plugins, deps);

            assertThat(result.blocked()).containsKey("plugin.a");
            assertThat(result.blocked().get("plugin.a")).contains("Missing required dependency");
            assertThat(result.loadOrder()).doesNotContain("plugin.a");
        }

        @Test
        @DisplayName("should warn but not block for missing optional dependency")
        void shouldWarnButNotBlockForMissingOptionalDependency() {
            Map<String, PluginDescriptor> plugins = Map.of(
                "plugin.a", createDescriptor("plugin.a", "1.0.0")
            );
            Map<String, List<PluginDependency>> deps = Map.of(
                "plugin.a", List.of(new PluginDependency("plugin.optional", "^1.0.0", true))
            );

            var result = resolver.resolve(plugins, deps);

            assertThat(result.loadOrder()).contains("plugin.a");
            assertThat(result.blocked()).isEmpty();
            assertThat(result.warnings()).anyMatch(w -> w.contains("Optional dependency missing"));
        }
    }

    @Nested
    @DisplayName("Version matching")
    class VersionMatching {

        @Test
        @DisplayName("should block plugin with incompatible dependency version")
        void shouldBlockPluginWithIncompatibleVersion() {
            Map<String, PluginDescriptor> plugins = Map.of(
                "plugin.a", createDescriptor("plugin.a", "1.0.0"),
                "plugin.b", createDescriptor("plugin.b", "2.0.0")
            );
            Map<String, List<PluginDependency>> deps = Map.of(
                "plugin.b", List.of(new PluginDependency("plugin.a", ">=2.0.0", false))
            );

            var result = resolver.resolve(plugins, deps);

            assertThat(result.blocked()).containsKey("plugin.b");
            assertThat(result.blocked().get("plugin.b")).contains("version mismatch");
        }

        @Test
        @DisplayName("should allow compatible versions")
        void shouldAllowCompatibleVersions() {
            Map<String, PluginDescriptor> plugins = Map.of(
                "plugin.a", createDescriptor("plugin.a", "1.5.0"),
                "plugin.b", createDescriptor("plugin.b", "1.0.0")
            );
            Map<String, List<PluginDependency>> deps = Map.of(
                "plugin.b", List.of(new PluginDependency("plugin.a", "^1.0.0", false))
            );

            var result = resolver.resolve(plugins, deps);

            assertThat(result.loadOrder()).containsExactly("plugin.a", "plugin.b");
            assertThat(result.blocked()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Circular dependency detection")
    class CircularDependencyDetection {

        @Test
        @DisplayName("should detect simple circular dependency")
        void shouldDetectSimpleCircularDependency() {
            Map<String, PluginDescriptor> plugins = Map.of(
                "plugin.a", createDescriptor("plugin.a", "1.0.0"),
                "plugin.b", createDescriptor("plugin.b", "1.0.0")
            );
            Map<String, List<PluginDependency>> deps = Map.of(
                "plugin.a", List.of(new PluginDependency("plugin.b", "^1.0.0", false)),
                "plugin.b", List.of(new PluginDependency("plugin.a", "^1.0.0", false))
            );

            assertThatThrownBy(() -> resolver.resolve(plugins, deps))
                .isInstanceOf(DependencyResolver.CircularDependencyException.class)
                .hasMessageContaining("Circular dependency");
        }

        @Test
        @DisplayName("should detect self-dependency")
        void shouldDetectSelfDependency() {
            Map<String, PluginDescriptor> plugins = Map.of(
                "plugin.a", createDescriptor("plugin.a", "1.0.0")
            );
            Map<String, List<PluginDependency>> deps = Map.of(
                "plugin.a", List.of(new PluginDependency("plugin.a", "^1.0.0", false))
            );

            assertThatThrownBy(() -> resolver.resolve(plugins, deps))
                .isInstanceOf(DependencyResolver.CircularDependencyException.class);
        }

        @Test
        @DisplayName("should detect transitive circular dependency")
        void shouldDetectTransitiveCircularDependency() {
            // A -> B -> C -> A
            Map<String, PluginDescriptor> plugins = new HashMap<>();
            plugins.put("plugin.a", createDescriptor("plugin.a", "1.0.0"));
            plugins.put("plugin.b", createDescriptor("plugin.b", "1.0.0"));
            plugins.put("plugin.c", createDescriptor("plugin.c", "1.0.0"));

            Map<String, List<PluginDependency>> deps = new HashMap<>();
            deps.put("plugin.a", List.of(new PluginDependency("plugin.b", "^1.0.0", false)));
            deps.put("plugin.b", List.of(new PluginDependency("plugin.c", "^1.0.0", false)));
            deps.put("plugin.c", List.of(new PluginDependency("plugin.a", "^1.0.0", false)));

            assertThatThrownBy(() -> resolver.resolve(plugins, deps))
                .isInstanceOf(DependencyResolver.CircularDependencyException.class);
        }
    }

    @Nested
    @DisplayName("detectCycles()")
    class DetectCycles {

        @Test
        @DisplayName("should return empty list when no cycles")
        void shouldReturnEmptyListWhenNoCycles() {
            Map<String, PluginDescriptor> plugins = Map.of(
                "plugin.a", createDescriptor("plugin.a", "1.0.0"),
                "plugin.b", createDescriptor("plugin.b", "1.0.0")
            );
            Map<String, List<PluginDependency>> deps = Map.of(
                "plugin.b", List.of(new PluginDependency("plugin.a", "^1.0.0", false))
            );

            var cycles = resolver.detectCycles(plugins, deps);

            assertThat(cycles).isEmpty();
        }

        @Test
        @DisplayName("should return plugins involved in cycles")
        void shouldReturnPluginsInvolvedInCycles() {
            Map<String, PluginDescriptor> plugins = Map.of(
                "plugin.a", createDescriptor("plugin.a", "1.0.0"),
                "plugin.b", createDescriptor("plugin.b", "1.0.0")
            );
            Map<String, List<PluginDependency>> deps = Map.of(
                "plugin.a", List.of(new PluginDependency("plugin.b", "^1.0.0", false)),
                "plugin.b", List.of(new PluginDependency("plugin.a", "^1.0.0", false))
            );

            var cycles = resolver.detectCycles(plugins, deps);

            assertThat(cycles).containsExactlyInAnyOrder("plugin.a", "plugin.b");
        }
    }

    @Nested
    @DisplayName("ResolutionResult")
    class ResolutionResultTest {

        @Test
        @DisplayName("should report hasBlockedPlugins correctly")
        void shouldReportHasBlockedPluginsCorrectly() {
            var resultWithBlocked = new DependencyResolver.ResolutionResult(
                List.of(), Map.of("plugin.a", "reason"), List.of()
            );
            var resultWithoutBlocked = new DependencyResolver.ResolutionResult(
                List.of("plugin.a"), Map.of(), List.of()
            );

            assertThat(resultWithBlocked.hasBlockedPlugins()).isTrue();
            assertThat(resultWithoutBlocked.hasBlockedPlugins()).isFalse();
        }

        @Test
        @DisplayName("should report isPluginBlocked correctly")
        void shouldReportIsPluginBlockedCorrectly() {
            var result = new DependencyResolver.ResolutionResult(
                List.of("plugin.b"), Map.of("plugin.a", "reason"), List.of()
            );

            assertThat(result.isPluginBlocked("plugin.a")).isTrue();
            assertThat(result.isPluginBlocked("plugin.b")).isFalse();
        }
    }
}
