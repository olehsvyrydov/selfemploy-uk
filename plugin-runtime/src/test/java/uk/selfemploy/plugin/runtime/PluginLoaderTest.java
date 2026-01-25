package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PluginLoader")
class PluginLoaderTest {

    private PluginLoader loader;

    @BeforeEach
    void setUp() {
        loader = new PluginLoader();
    }

    @Nested
    @DisplayName("Version compatibility")
    class VersionCompatibility {

        @Test
        @DisplayName("Same version is compatible")
        void sameVersionIsCompatible() {
            assertThat(loader.isVersionCompatible("1.0.0", "1.0.0")).isTrue();
        }

        @Test
        @DisplayName("Higher major version is compatible")
        void higherMajorIsCompatible() {
            assertThat(loader.isVersionCompatible("2.0.0", "1.0.0")).isTrue();
        }

        @Test
        @DisplayName("Higher minor version is compatible")
        void higherMinorIsCompatible() {
            assertThat(loader.isVersionCompatible("1.5.0", "1.2.0")).isTrue();
        }

        @Test
        @DisplayName("Higher patch version is compatible")
        void higherPatchIsCompatible() {
            assertThat(loader.isVersionCompatible("1.0.5", "1.0.2")).isTrue();
        }

        @Test
        @DisplayName("Lower version is not compatible")
        void lowerVersionIsNotCompatible() {
            assertThat(loader.isVersionCompatible("1.0.0", "1.0.1")).isFalse();
        }

        @Test
        @DisplayName("Lower major version is not compatible")
        void lowerMajorIsNotCompatible() {
            assertThat(loader.isVersionCompatible("1.0.0", "2.0.0")).isFalse();
        }

        @Test
        @DisplayName("Null minVersion is compatible")
        void nullMinVersionIsCompatible() {
            assertThat(loader.isVersionCompatible("1.0.0", null)).isTrue();
        }

        @Test
        @DisplayName("Blank minVersion is compatible")
        void blankMinVersionIsCompatible() {
            assertThat(loader.isVersionCompatible("1.0.0", "  ")).isTrue();
        }

        @Test
        @DisplayName("Null currentVersion is not compatible")
        void nullCurrentVersionIsNotCompatible() {
            assertThat(loader.isVersionCompatible(null, "1.0.0")).isFalse();
        }

        @Test
        @DisplayName("SNAPSHOT versions are handled")
        void snapshotVersionsHandled() {
            assertThat(loader.isVersionCompatible("1.0.0-SNAPSHOT", "1.0.0")).isTrue();
            assertThat(loader.isVersionCompatible("1.0.0", "1.0.0-SNAPSHOT")).isTrue();
        }

        @Test
        @DisplayName("Versions with different segment counts")
        void differentSegmentCounts() {
            assertThat(loader.isVersionCompatible("1.0", "1.0.0")).isTrue();
            assertThat(loader.isVersionCompatible("1.0.0", "1.0")).isTrue();
            assertThat(loader.isVersionCompatible("1", "1.0.0")).isTrue();
        }

        @Test
        @DisplayName("EDGE-031: Version comparison with zero")
        void versionComparisonWithZero() {
            assertThat(loader.isVersionCompatible("0.0.0", "0.0.0")).isTrue();
            assertThat(loader.isVersionCompatible("0.0.1", "0.0.0")).isTrue();
            assertThat(loader.isVersionCompatible("0.1.0", "0.0.0")).isTrue();
            assertThat(loader.isVersionCompatible("1.0.0", "0.0.0")).isTrue();
        }

        @Test
        @DisplayName("EDGE-032: Version comparison with many parts")
        void versionComparisonWithManyParts() {
            // More parts on current version - compatible (current is newer)
            assertThat(loader.isVersionCompatible("1.2.3.4.5", "1.2.3")).isTrue();
            // More parts on min version - not compatible (current is older)
            assertThat(loader.isVersionCompatible("1.2.3", "1.2.3.4.5")).isFalse();
            // Same number of parts - compatible (equal)
            assertThat(loader.isVersionCompatible("1.2.3.4", "1.2.3.4")).isTrue();
        }

        @Test
        @DisplayName("EDGE-033: Version comparison with maximum version string")
        void versionComparisonWithLargeNumbers() {
            assertThat(loader.isVersionCompatible("999.999.999", "1.0.0")).isTrue();
            assertThat(loader.isVersionCompatible("1.0.0", "999.999.999")).isFalse();
            assertThat(loader.isVersionCompatible("999.999.999", "999.999.999")).isTrue();
        }
    }

    @Nested
    @DisplayName("Native image detection")
    class NativeImageDetection {

        @Test
        @DisplayName("isNativeImage returns false in test environment")
        void isNativeImageReturnsFalse() {
            // In normal JVM tests, this should return false
            assertThat(PluginLoader.isNativeImage()).isFalse();
        }
    }

    @Nested
    @DisplayName("Plugin discovery")
    class PluginDiscovery {

        @Test
        @DisplayName("discoverPlugins returns list (may be empty without service files)")
        void discoverPluginsReturnsList() {
            // Without actual service provider files, this returns empty list
            // This test verifies the method runs without error
            var result = loader.discoverPlugins();
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("discoverCompatiblePlugins filters by version")
        void discoverCompatiblePluginsFilters() {
            // Without actual service provider files, this returns empty list
            // This test verifies the method runs without error
            var result = loader.discoverCompatiblePlugins("1.0.0");
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("reload returns new list")
        void reloadReturnsNewList() {
            var result = loader.reload();
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Custom class loader")
    class CustomClassLoader {

        @Test
        @DisplayName("Accepts custom class loader")
        void acceptsCustomClassLoader() {
            ClassLoader customLoader = Thread.currentThread().getContextClassLoader();
            PluginLoader loaderWithCustomCl = new PluginLoader(customLoader);

            // Should not throw
            var result = loaderWithCustomCl.discoverPlugins();
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Handles null class loader gracefully")
        void handlesNullClassLoader() {
            PluginLoader loaderWithNull = new PluginLoader(null);

            // Should use system class loader and not throw
            var result = loaderWithNull.discoverPlugins();
            assertThat(result).isNotNull();
        }
    }
}
