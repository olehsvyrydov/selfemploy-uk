package uk.selfemploy.plugin.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A ClassLoader that provides isolation between plugins.
 *
 * <p>The PluginClassLoader implements a parent-first strategy for API classes
 * and platform classes, but child-first for plugin-specific classes. This
 * ensures that:</p>
 * <ul>
 *   <li>All plugins share the same API interfaces from the host</li>
 *   <li>Plugins can have their own versions of third-party libraries</li>
 *   <li>Plugins cannot interfere with each other's class loading</li>
 * </ul>
 *
 * <h2>Class Loading Strategy</h2>
 * <ol>
 *   <li><b>JDK classes</b> - Always loaded from bootstrap/platform classloader</li>
 *   <li><b>API classes</b> - Always loaded from parent (uk.selfemploy.plugin.api.*)</li>
 *   <li><b>Runtime classes</b> - Always loaded from parent (uk.selfemploy.plugin.runtime.*)</li>
 *   <li><b>Plugin classes</b> - Loaded from plugin JAR first (child-first)</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The closed state and API packages are
 * synchronized appropriately.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * URL[] pluginUrls = new URL[]{pluginJarPath.toUri().toURL()};
 * PluginClassLoader classLoader = new PluginClassLoader(
 *     pluginId,
 *     pluginUrls,
 *     getClass().getClassLoader()
 * );
 *
 * try {
 *     Class<?> pluginClass = classLoader.loadClass("com.example.MyPlugin");
 *     // Use the class
 * } finally {
 *     classLoader.close();
 * }
 * }</pre>
 *
 * @see PluginLoader
 * @see PluginManager
 */
public class PluginClassLoader extends URLClassLoader {

    private static final Logger LOG = LoggerFactory.getLogger(PluginClassLoader.class);

    /**
     * Default API packages that should always be loaded from parent.
     */
    private static final Set<String> DEFAULT_API_PACKAGES = Set.of(
        "uk.selfemploy.plugin.api",
        "uk.selfemploy.plugin.extension",
        "uk.selfemploy.plugin.runtime"
    );

    /**
     * Packages that should always be loaded from the platform classloader.
     */
    private static final Set<String> PLATFORM_PACKAGES = Set.of(
        "java.",
        "javax.",
        "jdk.",
        "sun.",
        "com.sun.",
        "org.slf4j.",
        "javafx.",
        "org.kordamp.ikonli."
    );

    private final String pluginId;
    private final Set<String> apiPackages;
    private volatile boolean closed = false;

    /**
     * Creates a new PluginClassLoader.
     *
     * @param pluginId the ID of the plugin (for debugging and logging)
     * @param urls     the URLs to search for classes and resources
     * @param parent   the parent classloader
     * @throws NullPointerException if urls is null
     */
    public PluginClassLoader(String pluginId, URL[] urls, ClassLoader parent) {
        super(Objects.requireNonNull(urls, "urls must not be null"), parent);
        this.pluginId = pluginId;
        this.apiPackages = new CopyOnWriteArraySet<>(DEFAULT_API_PACKAGES);
        LOG.debug("Created PluginClassLoader for plugin: {} with {} URLs",
            pluginId, urls.length);
    }

    /**
     * Returns the plugin ID.
     *
     * @return the plugin ID, or null if not set
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Checks if this classloader has been closed.
     *
     * @return true if closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Adds an additional package to be treated as API (loaded from parent).
     *
     * @param packageName the package name prefix (e.g., "com.example.shared")
     */
    public void addApiPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName must not be null");
        apiPackages.add(packageName);
        LOG.debug("Added API package for plugin {}: {}", pluginId, packageName);
    }

    /**
     * Checks if a class should be loaded from the parent classloader.
     *
     * @param className the fully qualified class name
     * @return true if this is an API class
     */
    public boolean isApiClass(String className) {
        // Check platform packages
        for (String prefix : PLATFORM_PACKAGES) {
            if (className.startsWith(prefix)) {
                return false; // Platform classes handled by JVM
            }
        }

        // Check API packages
        for (String apiPackage : apiPackages) {
            if (className.startsWith(apiPackage + ".") ||
                className.equals(apiPackage)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // Check if already loaded
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass != null) {
                return loadedClass;
            }

            // Platform classes always from parent
            if (isPlatformClass(name)) {
                return super.loadClass(name, resolve);
            }

            // API classes always from parent
            if (isApiClass(name)) {
                LOG.trace("Loading API class from parent: {}", name);
                return super.loadClass(name, resolve);
            }

            // For plugin classes, try child-first
            try {
                Class<?> clazz = findClass(name);
                if (resolve) {
                    resolveClass(clazz);
                }
                LOG.trace("Loaded class from plugin JAR: {}", name);
                return clazz;
            } catch (ClassNotFoundException e) {
                // Fall back to parent
                return super.loadClass(name, resolve);
            }
        }
    }

    /**
     * Checks if a class is a platform class (JDK, JavaFX, etc.).
     *
     * @param className the class name
     * @return true if this is a platform class
     */
    private boolean isPlatformClass(String className) {
        for (String prefix : PLATFORM_PACKAGES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public URL getResource(String name) {
        // For API resources, parent first
        URL url = getParent().getResource(name);
        if (url != null) {
            return url;
        }
        // Then check plugin resources
        return findResource(name);
    }

    @Override
    public void close() throws IOException {
        closed = true;
        LOG.debug("Closing PluginClassLoader for plugin: {}", pluginId);
        super.close();
    }

    @Override
    public String toString() {
        return String.format("PluginClassLoader[pluginId=%s, closed=%s, urls=%d]",
            pluginId, closed, getURLs().length);
    }
}
