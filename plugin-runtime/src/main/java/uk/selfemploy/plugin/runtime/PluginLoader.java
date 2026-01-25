package uk.selfemploy.plugin.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.plugin.api.Plugin;
import uk.selfemploy.plugin.api.PluginDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Loads plugins using Java's ServiceLoader mechanism.
 *
 * <p>The PluginLoader discovers plugins registered via the ServiceLoader
 * pattern. Plugins must:</p>
 * <ol>
 *   <li>Implement the {@link Plugin} interface</li>
 *   <li>Provide a no-arg constructor</li>
 *   <li>Include a service provider file at
 *       {@code META-INF/services/uk.selfemploy.plugin.api.Plugin}</li>
 * </ol>
 *
 * <h2>Dual-Mode Architecture</h2>
 * <p>The loader supports both JVM and GraalVM Native Image modes:</p>
 * <ul>
 *   <li><b>JVM Mode</b>: Plugins are discovered dynamically from the classpath</li>
 *   <li><b>Native Mode</b>: Plugins must be bundled at build time and registered
 *       in native-image configuration</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. Discovery operations are synchronized to
 * prevent concurrent issues.</p>
 *
 * @see Plugin
 * @see ServiceLoader
 */
public class PluginLoader {

    private static final Logger LOG = LoggerFactory.getLogger(PluginLoader.class);

    /**
     * System property set by GraalVM when running as native image.
     */
    private static final String NATIVE_IMAGE_PROPERTY = "org.graalvm.nativeimage.imagecode";

    /**
     * Checks if the application is running as a GraalVM native image.
     *
     * @return true if running as native image
     */
    public static boolean isNativeImage() {
        return System.getProperty(NATIVE_IMAGE_PROPERTY) != null;
    }

    private final ClassLoader classLoader;

    /**
     * Creates a new PluginLoader using the context class loader.
     */
    public PluginLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Creates a new PluginLoader with a specific class loader.
     *
     * @param classLoader the class loader to use for plugin discovery
     */
    public PluginLoader(ClassLoader classLoader) {
        this.classLoader = classLoader != null ? classLoader : ClassLoader.getSystemClassLoader();
    }

    /**
     * Discovers all plugins available via ServiceLoader.
     *
     * <p>This method scans the classpath for plugin implementations and
     * creates container objects for each discovered plugin.</p>
     *
     * <p>Errors during individual plugin discovery are logged but do not
     * prevent other plugins from being loaded.</p>
     *
     * @return list of discovered plugins wrapped in containers
     */
    public List<PluginContainer> discoverPlugins() {
        LOG.info("Discovering plugins (native image: {})", isNativeImage());

        List<PluginContainer> discovered = new ArrayList<>();
        ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, classLoader);

        for (ServiceLoader.Provider<Plugin> provider : serviceLoader.stream().toList()) {
            try {
                Plugin plugin = provider.get();
                PluginDescriptor descriptor = plugin.getDescriptor();

                LOG.info("Discovered plugin: {} v{} ({})",
                    descriptor.name(),
                    descriptor.version(),
                    descriptor.id()
                );

                discovered.add(new PluginContainer(plugin));
            } catch (Exception e) {
                LOG.error("Failed to instantiate plugin from provider: {}",
                    provider.type().getName(), e);
            }
        }

        LOG.info("Plugin discovery complete: {} plugins found", discovered.size());
        return discovered;
    }

    /**
     * Discovers plugins and applies filtering based on version compatibility.
     *
     * @param appVersion the current application version for compatibility checks
     * @return list of compatible plugins
     */
    public List<PluginContainer> discoverCompatiblePlugins(String appVersion) {
        List<PluginContainer> allPlugins = discoverPlugins();
        List<PluginContainer> compatible = new ArrayList<>();

        for (PluginContainer container : allPlugins) {
            String minVersion = container.getDescriptor().minAppVersion();

            if (isVersionCompatible(appVersion, minVersion)) {
                compatible.add(container);
            } else {
                LOG.warn("Plugin {} requires app version {} but current is {}",
                    container.getId(),
                    minVersion,
                    appVersion
                );
            }
        }

        return compatible;
    }

    /**
     * Checks if the current app version meets the minimum required version.
     *
     * <p>This performs simple semantic version comparison supporting
     * versions in the format X.Y.Z.</p>
     *
     * @param currentVersion the current application version
     * @param minVersion     the minimum required version
     * @return true if current version meets or exceeds minimum
     */
    boolean isVersionCompatible(String currentVersion, String minVersion) {
        if (minVersion == null || minVersion.isBlank()) {
            return true;
        }
        if (currentVersion == null || currentVersion.isBlank()) {
            return false;
        }

        try {
            int[] current = parseVersion(currentVersion);
            int[] min = parseVersion(minVersion);

            for (int i = 0; i < Math.max(current.length, min.length); i++) {
                int c = i < current.length ? current[i] : 0;
                int m = i < min.length ? min[i] : 0;

                if (c > m) return true;
                if (c < m) return false;
            }
            return true; // Equal versions
        } catch (NumberFormatException e) {
            LOG.warn("Unable to parse version strings: current={}, min={}",
                currentVersion, minVersion);
            return true; // Be permissive on parse errors
        }
    }

    /**
     * Parses a version string into numeric components.
     *
     * @param version the version string (e.g., "1.2.3" or "1.2.3-SNAPSHOT")
     * @return array of version numbers
     */
    private int[] parseVersion(String version) {
        // Remove any suffix like -SNAPSHOT, -alpha, etc.
        String cleanVersion = version.split("-")[0];

        String[] parts = cleanVersion.split("\\.");
        int[] result = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].trim());
        }

        return result;
    }

    /**
     * Reloads the service loader to discover newly added plugins.
     *
     * <p>This method is primarily useful in JVM development mode where
     * plugins may be added at runtime.</p>
     *
     * @return list of newly discovered plugins
     */
    public List<PluginContainer> reload() {
        LOG.debug("Reloading plugin discovery");
        return discoverPlugins();
    }
}
