package uk.selfemploy.plugin.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Thread-safe registry for managing plugin containers.
 *
 * <p>The PluginRegistry maintains a collection of all known plugins,
 * regardless of their current state. It provides methods for:</p>
 * <ul>
 *   <li>Adding and removing plugins</li>
 *   <li>Looking up plugins by ID</li>
 *   <li>Querying plugins by state or other criteria</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is fully thread-safe. All operations use concurrent data
 * structures and atomic operations.</p>
 *
 * @see PluginContainer
 * @see PluginManager
 */
public class PluginRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(PluginRegistry.class);

    /**
     * Map of plugin ID to container.
     */
    private final Map<String, PluginContainer> plugins = new ConcurrentHashMap<>();

    /**
     * Adds a plugin to the registry.
     *
     * <p>If a plugin with the same ID already exists, it will be replaced.</p>
     *
     * @param container the plugin container to add
     * @throws NullPointerException if container is null
     */
    public void add(PluginContainer container) {
        Objects.requireNonNull(container, "container must not be null");

        String pluginId = container.getId();
        PluginContainer previous = plugins.put(pluginId, container);

        if (previous != null) {
            LOG.warn("Replaced existing plugin: {}", pluginId);
        } else {
            LOG.debug("Added plugin to registry: {}", pluginId);
        }
    }

    /**
     * Adds multiple plugins to the registry.
     *
     * @param containers the plugin containers to add
     */
    public void addAll(Collection<PluginContainer> containers) {
        for (PluginContainer container : containers) {
            add(container);
        }
    }

    /**
     * Removes a plugin from the registry.
     *
     * @param pluginId the ID of the plugin to remove
     * @return the removed container, or empty if not found
     */
    public Optional<PluginContainer> remove(String pluginId) {
        Objects.requireNonNull(pluginId, "pluginId must not be null");

        PluginContainer removed = plugins.remove(pluginId);
        if (removed != null) {
            LOG.debug("Removed plugin from registry: {}", pluginId);
        }
        return Optional.ofNullable(removed);
    }

    /**
     * Gets a plugin by its ID.
     *
     * @param pluginId the plugin ID
     * @return the plugin container, or empty if not found
     */
    public Optional<PluginContainer> get(String pluginId) {
        Objects.requireNonNull(pluginId, "pluginId must not be null");
        return Optional.ofNullable(plugins.get(pluginId));
    }

    /**
     * Gets a plugin by its ID, throwing if not found.
     *
     * @param pluginId the plugin ID
     * @return the plugin container
     * @throws PluginNotFoundException if the plugin is not in the registry
     */
    public PluginContainer getOrThrow(String pluginId) {
        return get(pluginId)
            .orElseThrow(() -> new PluginNotFoundException(pluginId));
    }

    /**
     * Checks if a plugin exists in the registry.
     *
     * @param pluginId the plugin ID
     * @return true if the plugin is registered
     */
    public boolean contains(String pluginId) {
        return plugins.containsKey(pluginId);
    }

    /**
     * Returns all registered plugins.
     *
     * @return unmodifiable list of all plugins
     */
    public List<PluginContainer> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(plugins.values()));
    }

    /**
     * Returns all plugins in the specified state.
     *
     * @param state the state to filter by
     * @return unmodifiable list of matching plugins
     */
    public List<PluginContainer> getByState(PluginState state) {
        Objects.requireNonNull(state, "state must not be null");
        return plugins.values().stream()
            .filter(c -> c.getState() == state)
            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns all enabled plugins.
     *
     * @return unmodifiable list of enabled plugins
     */
    public List<PluginContainer> getEnabled() {
        return getByState(PluginState.ENABLED);
    }

    /**
     * Returns all loaded plugins (LOADED, ENABLED, or DISABLED state).
     *
     * @return unmodifiable list of loaded plugins
     */
    public List<PluginContainer> getLoaded() {
        return plugins.values().stream()
            .filter(c -> c.getState().isActive())
            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns all failed plugins.
     *
     * @return unmodifiable list of failed plugins
     */
    public List<PluginContainer> getFailed() {
        return getByState(PluginState.FAILED);
    }

    /**
     * Returns plugins matching a custom predicate.
     *
     * @param predicate the filter predicate
     * @return unmodifiable list of matching plugins
     */
    public List<PluginContainer> find(Predicate<PluginContainer> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        return plugins.values().stream()
            .filter(predicate)
            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns the number of registered plugins.
     *
     * @return the plugin count
     */
    public int size() {
        return plugins.size();
    }

    /**
     * Checks if the registry is empty.
     *
     * @return true if no plugins are registered
     */
    public boolean isEmpty() {
        return plugins.isEmpty();
    }

    /**
     * Clears all plugins from the registry.
     *
     * <p>This method is typically called during shutdown or reset.</p>
     */
    public void clear() {
        plugins.clear();
        LOG.debug("Cleared plugin registry");
    }

    /**
     * Returns a summary of plugin states.
     *
     * @return map of state to count
     */
    public Map<PluginState, Long> getStateSummary() {
        return plugins.values().stream()
            .collect(Collectors.groupingBy(
                PluginContainer::getState,
                Collectors.counting()
            ));
    }

    @Override
    public String toString() {
        return String.format("PluginRegistry[size=%d, states=%s]",
            size(),
            getStateSummary()
        );
    }
}
