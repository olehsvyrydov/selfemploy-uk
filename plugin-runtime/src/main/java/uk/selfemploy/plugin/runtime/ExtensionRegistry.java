package uk.selfemploy.plugin.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.plugin.extension.ExtensionPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for managing extension point implementations.
 *
 * <p>The ExtensionRegistry maintains a mapping of extension point types to
 * their implementations. Plugins register their extensions during the
 * enable phase and unregister them when disabled.</p>
 *
 * <h2>Extension Registration</h2>
 * <p>Extensions are registered by type (Class) and implementation instance.
 * Multiple implementations can be registered for the same extension type.</p>
 *
 * <pre>{@code
 * extensionRegistry.register(NavigationExtension.class, myNavExtension);
 * extensionRegistry.register(DashboardWidget.class, myWidget);
 * }</pre>
 *
 * <h2>Extension Lookup</h2>
 * <p>Extensions can be retrieved by type to get all registered implementations:</p>
 *
 * <pre>{@code
 * List<NavigationExtension> navExtensions =
 *     extensionRegistry.getExtensions(NavigationExtension.class);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is fully thread-safe. It uses concurrent data structures
 * to support concurrent registration and lookup operations.</p>
 *
 * @see ExtensionPoint
 * @see PluginManager
 */
public class ExtensionRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ExtensionRegistry.class);

    /**
     * Map of extension type to list of implementations.
     * Uses CopyOnWriteArrayList for thread-safe iteration.
     */
    private final Map<Class<? extends ExtensionPoint>, List<ExtensionEntry<?>>> extensions =
        new ConcurrentHashMap<>();

    /**
     * Tracks which extensions belong to which plugin for bulk unregistration.
     * Key: plugin ID, Value: set of (type, extension) pairs
     */
    private final Map<String, Set<ExtensionEntry<?>>> extensionsByPlugin =
        new ConcurrentHashMap<>();

    /**
     * Registers an extension for the given type.
     *
     * @param <T>       the extension point type
     * @param type      the extension point class
     * @param extension the extension implementation
     * @throws NullPointerException if any argument is null
     */
    public <T extends ExtensionPoint> void register(Class<T> type, T extension) {
        register(null, type, extension);
    }

    /**
     * Registers an extension for the given type, associated with a plugin.
     *
     * @param <T>       the extension point type
     * @param pluginId  the ID of the plugin registering the extension (may be null)
     * @param type      the extension point class
     * @param extension the extension implementation
     * @throws NullPointerException if type or extension is null
     */
    public <T extends ExtensionPoint> void register(String pluginId, Class<T> type, T extension) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(extension, "extension must not be null");

        ExtensionEntry<T> entry = new ExtensionEntry<>(pluginId, type, extension);

        // Add to type-based registry
        extensions.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
            .add(entry);

        // Track by plugin if pluginId is provided
        if (pluginId != null) {
            extensionsByPlugin.computeIfAbsent(pluginId, k -> ConcurrentHashMap.newKeySet())
                .add(entry);
        }

        LOG.debug("Registered extension: type={}, plugin={}, class={}",
            type.getSimpleName(),
            pluginId != null ? pluginId : "core",
            extension.getClass().getName()
        );
    }

    /**
     * Unregisters an extension.
     *
     * @param <T>       the extension point type
     * @param type      the extension point class
     * @param extension the extension implementation to unregister
     * @return true if the extension was found and removed
     */
    public <T extends ExtensionPoint> boolean unregister(Class<T> type, T extension) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(extension, "extension must not be null");

        List<ExtensionEntry<?>> entries = extensions.get(type);
        if (entries == null) {
            return false;
        }

        boolean removed = entries.removeIf(e -> e.extension().equals(extension));

        if (removed) {
            LOG.debug("Unregistered extension: type={}, class={}",
                type.getSimpleName(),
                extension.getClass().getName()
            );

            // Remove from plugin tracking
            extensionsByPlugin.values().forEach(set ->
                set.removeIf(e -> e.extension().equals(extension))
            );
        }

        return removed;
    }

    /**
     * Unregisters all extensions registered by a specific plugin.
     *
     * @param pluginId the ID of the plugin
     * @return the number of extensions unregistered
     */
    public int unregisterAll(String pluginId) {
        Objects.requireNonNull(pluginId, "pluginId must not be null");

        Set<ExtensionEntry<?>> pluginExtensions = extensionsByPlugin.remove(pluginId);
        if (pluginExtensions == null || pluginExtensions.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (ExtensionEntry<?> entry : pluginExtensions) {
            List<ExtensionEntry<?>> entries = extensions.get(entry.type());
            if (entries != null && entries.remove(entry)) {
                count++;
            }
        }

        LOG.debug("Unregistered {} extensions for plugin: {}", count, pluginId);
        return count;
    }

    /**
     * Returns all registered extensions of the given type.
     *
     * <p>The returned list is unmodifiable. The order of extensions is
     * the order in which they were registered.</p>
     *
     * @param <T>  the extension point type
     * @param type the extension point class
     * @return unmodifiable list of extensions, never null
     */
    @SuppressWarnings("unchecked")
    public <T extends ExtensionPoint> List<T> getExtensions(Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");

        List<ExtensionEntry<?>> entries = extensions.get(type);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>(entries.size());
        for (ExtensionEntry<?> entry : entries) {
            result.add((T) entry.extension());
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Checks if any extensions are registered for the given type.
     *
     * @param type the extension point class
     * @return true if at least one extension is registered
     */
    public boolean hasExtensions(Class<? extends ExtensionPoint> type) {
        List<ExtensionEntry<?>> entries = extensions.get(type);
        return entries != null && !entries.isEmpty();
    }

    /**
     * Returns the count of registered extensions for the given type.
     *
     * @param type the extension point class
     * @return the number of registered extensions
     */
    public int getExtensionCount(Class<? extends ExtensionPoint> type) {
        List<ExtensionEntry<?>> entries = extensions.get(type);
        return entries != null ? entries.size() : 0;
    }

    /**
     * Returns the total number of registered extensions across all types.
     *
     * @return the total extension count
     */
    public int getTotalExtensionCount() {
        return extensions.values().stream()
            .mapToInt(List::size)
            .sum();
    }

    /**
     * Returns all registered extension types.
     *
     * @return unmodifiable set of extension types
     */
    public Set<Class<? extends ExtensionPoint>> getRegisteredTypes() {
        return Collections.unmodifiableSet(extensions.keySet());
    }

    /**
     * Clears all registered extensions.
     *
     * <p>This method is typically called during shutdown.</p>
     */
    public void clear() {
        extensions.clear();
        extensionsByPlugin.clear();
        LOG.debug("Cleared all registered extensions");
    }

    /**
     * Internal record to hold extension registration information.
     */
    private record ExtensionEntry<T extends ExtensionPoint>(
        String pluginId,
        Class<T> type,
        T extension
    ) {}
}
