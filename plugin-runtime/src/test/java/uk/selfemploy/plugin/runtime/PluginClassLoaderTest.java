package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link PluginClassLoader}.
 */
@DisplayName("PluginClassLoader")
class PluginClassLoaderTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("Creates classloader with plugin name and URL")
        void createsWithPluginNameAndUrl() throws Exception {
            Path jarPath = createEmptyJar("test-plugin.jar");
            URL[] urls = new URL[]{jarPath.toUri().toURL()};

            PluginClassLoader classLoader = new PluginClassLoader(
                "test-plugin",
                urls,
                Thread.currentThread().getContextClassLoader()
            );

            assertThat(classLoader).isNotNull();
            assertThat(classLoader.getPluginId()).isEqualTo("test-plugin");
            classLoader.close();
        }

        @Test
        @DisplayName("Accepts null plugin name")
        void acceptsNullPluginName() throws Exception {
            Path jarPath = createEmptyJar("plugin.jar");
            URL[] urls = new URL[]{jarPath.toUri().toURL()};

            PluginClassLoader classLoader = new PluginClassLoader(
                null,
                urls,
                Thread.currentThread().getContextClassLoader()
            );

            assertThat(classLoader.getPluginId()).isNull();
            classLoader.close();
        }

        @Test
        @DisplayName("Requires URLs array")
        void requiresUrls() {
            assertThatThrownBy(() ->
                new PluginClassLoader("plugin", null, null)
            ).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Class loading strategy")
    class ClassLoadingStrategy {

        @Test
        @DisplayName("Parent-first for API classes")
        void parentFirstForApiClasses() throws Exception {
            Path jarPath = createEmptyJar("plugin.jar");
            URL[] urls = new URL[]{jarPath.toUri().toURL()};

            PluginClassLoader classLoader = new PluginClassLoader(
                "test-plugin",
                urls,
                Thread.currentThread().getContextClassLoader()
            );

            // API classes should be loaded from parent
            Class<?> pluginClass = classLoader.loadClass("uk.selfemploy.plugin.api.Plugin");
            assertThat(pluginClass.getClassLoader())
                .isNotEqualTo(classLoader);

            classLoader.close();
        }

        @Test
        @DisplayName("Parent-first for JDK classes")
        void parentFirstForJdkClasses() throws Exception {
            Path jarPath = createEmptyJar("plugin.jar");
            URL[] urls = new URL[]{jarPath.toUri().toURL()};

            PluginClassLoader classLoader = new PluginClassLoader(
                "test-plugin",
                urls,
                Thread.currentThread().getContextClassLoader()
            );

            // JDK classes should be loaded from bootstrap/platform classloader
            Class<?> stringClass = classLoader.loadClass("java.lang.String");
            assertThat(stringClass).isEqualTo(String.class);

            classLoader.close();
        }

        @Test
        @DisplayName("Correctly identifies API packages")
        void identifiesApiPackages() throws Exception {
            Path jarPath = createEmptyJar("plugin.jar");
            URL[] urls = new URL[]{jarPath.toUri().toURL()};

            PluginClassLoader classLoader = new PluginClassLoader(
                "test-plugin",
                urls,
                Thread.currentThread().getContextClassLoader()
            );

            assertThat(classLoader.isApiClass("uk.selfemploy.plugin.api.Plugin")).isTrue();
            assertThat(classLoader.isApiClass("uk.selfemploy.plugin.api.PluginContext")).isTrue();
            assertThat(classLoader.isApiClass("uk.selfemploy.plugin.extension.NavigationExtension")).isTrue();
            assertThat(classLoader.isApiClass("com.example.MyPlugin")).isFalse();
            assertThat(classLoader.isApiClass("java.lang.String")).isFalse();

            classLoader.close();
        }
    }

    @Nested
    @DisplayName("Resource loading")
    class ResourceLoading {

        @Test
        @DisplayName("Finds resources in plugin JAR")
        void findsResourcesInPluginJar() throws Exception {
            Path jarPath = createJarWithResource("resource-plugin.jar",
                "plugin.properties", "plugin.name=Test");

            URL[] urls = new URL[]{jarPath.toUri().toURL()};
            PluginClassLoader classLoader = new PluginClassLoader(
                "resource-plugin",
                urls,
                Thread.currentThread().getContextClassLoader()
            );

            URL resource = classLoader.getResource("plugin.properties");
            assertThat(resource).isNotNull();

            classLoader.close();
        }

        @Test
        @DisplayName("Returns null for missing resources")
        void returnsNullForMissingResources() throws Exception {
            Path jarPath = createEmptyJar("plugin.jar");
            URL[] urls = new URL[]{jarPath.toUri().toURL()};

            PluginClassLoader classLoader = new PluginClassLoader(
                "test-plugin",
                urls,
                Thread.currentThread().getContextClassLoader()
            );

            URL resource = classLoader.getResource("non-existent.txt");
            assertThat(resource).isNull();

            classLoader.close();
        }
    }

    @Nested
    @DisplayName("Isolation")
    class Isolation {

        @Test
        @DisplayName("Different plugins use different classloaders")
        void differentPluginsUseDifferentClassloaders() throws Exception {
            Path jar1 = createEmptyJar("plugin1.jar");
            Path jar2 = createEmptyJar("plugin2.jar");

            PluginClassLoader loader1 = new PluginClassLoader(
                "plugin1",
                new URL[]{jar1.toUri().toURL()},
                Thread.currentThread().getContextClassLoader()
            );

            PluginClassLoader loader2 = new PluginClassLoader(
                "plugin2",
                new URL[]{jar2.toUri().toURL()},
                Thread.currentThread().getContextClassLoader()
            );

            assertThat(loader1).isNotEqualTo(loader2);
            assertThat(loader1.getPluginId()).isNotEqualTo(loader2.getPluginId());

            loader1.close();
            loader2.close();
        }

        @Test
        @DisplayName("Classloader can be closed")
        void classloaderCanBeClosed() throws Exception {
            Path jarPath = createEmptyJar("closeable-plugin.jar");
            URL[] urls = new URL[]{jarPath.toUri().toURL()};

            PluginClassLoader classLoader = new PluginClassLoader(
                "closeable-plugin",
                urls,
                Thread.currentThread().getContextClassLoader()
            );

            assertThat(classLoader.isClosed()).isFalse();

            classLoader.close();

            assertThat(classLoader.isClosed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Additional API packages")
    class AdditionalApiPackages {

        @Test
        @DisplayName("Can add custom API packages")
        void canAddCustomApiPackages() throws Exception {
            Path jarPath = createEmptyJar("plugin.jar");
            URL[] urls = new URL[]{jarPath.toUri().toURL()};

            PluginClassLoader classLoader = new PluginClassLoader(
                "test-plugin",
                urls,
                Thread.currentThread().getContextClassLoader()
            );

            classLoader.addApiPackage("com.example.shared");

            assertThat(classLoader.isApiClass("com.example.shared.SharedService")).isTrue();
            assertThat(classLoader.isApiClass("com.example.other.OtherClass")).isFalse();

            classLoader.close();
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("Provides useful debug information")
        void providesUsefulDebugInfo() throws Exception {
            Path jarPath = createEmptyJar("plugin.jar");
            URL[] urls = new URL[]{jarPath.toUri().toURL()};

            PluginClassLoader classLoader = new PluginClassLoader(
                "my-plugin",
                urls,
                Thread.currentThread().getContextClassLoader()
            );

            String str = classLoader.toString();
            assertThat(str).contains("my-plugin");
            assertThat(str).contains("PluginClassLoader");

            classLoader.close();
        }
    }

    // Helper methods

    private Path createEmptyJar(String name) throws Exception {
        Path jarPath = tempDir.resolve(name);

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            JarEntry entry = new JarEntry("META-INF/MANIFEST.MF");
            jos.putNextEntry(entry);
            jos.write("Manifest-Version: 1.0\n".getBytes());
            jos.closeEntry();
        }

        return jarPath;
    }

    private Path createJarWithResource(String name, String resourceName, String content)
        throws Exception {
        Path jarPath = tempDir.resolve(name);

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            JarEntry manifestEntry = new JarEntry("META-INF/MANIFEST.MF");
            jos.putNextEntry(manifestEntry);
            jos.write("Manifest-Version: 1.0\n".getBytes());
            jos.closeEntry();

            JarEntry resourceEntry = new JarEntry(resourceName);
            jos.putNextEntry(resourceEntry);
            jos.write(content.getBytes());
            jos.closeEntry();
        }

        return jarPath;
    }
}
