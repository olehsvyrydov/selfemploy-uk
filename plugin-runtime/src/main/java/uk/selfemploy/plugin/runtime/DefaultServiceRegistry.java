package uk.selfemploy.plugin.runtime;

import uk.selfemploy.plugin.api.PluginPermission;
import uk.selfemploy.plugin.api.PluginService;
import uk.selfemploy.plugin.api.ServiceReference;
import uk.selfemploy.plugin.api.ServiceRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Default thread-safe implementation of {@link ServiceRegistry}.
 *
 * <p>This implementation uses ConcurrentHashMap for thread-safe operations
 * without requiring explicit synchronization for most operations.</p>
 *
 * <h2>Permission Enforcement</h2>
 * <p>Service registration requires the {@link PluginPermission#SERVICE_PROVIDER}
 * permission. The permission checker function is provided at construction time
 * to allow for flexible permission verification strategies.</p>
 *
 * <h2>Service Lifecycle</h2>
 * <p>Services are automatically unregistered when {@link #unregisterAll(String)}
 * is called, typically when a plugin is unloaded.</p>
 *
 * @see ServiceRegistry
 * @see ServiceReference
 */
public class DefaultServiceRegistry implements ServiceRegistry {

    /**
     * Map of service type to map of provider ID to implementation.
     * Structure: serviceType -> (providerId -> implementation)
     */
    private final Map<Class<? extends PluginService>, Map<String, PluginService>> services;

    /**
     * Function to check if a plugin has the SERVICE_PROVIDER permission.
     * Returns true if the plugin has the permission, false otherwise.
     */
    private final Function<String, Boolean> permissionChecker;

    /**
     * Creates a new DefaultServiceRegistry with no permission checking.
     *
     * <p>This constructor is primarily for testing. In production,
     * use the constructor that accepts a permission checker.</p>
     */
    public DefaultServiceRegistry() {
        this(providerId -> true);
    }

    /**
     * Creates a new DefaultServiceRegistry with the given permission checker.
     *
     * @param permissionChecker function that checks if a plugin has SERVICE_PROVIDER permission
     * @throws NullPointerException if permissionChecker is null
     */
    public DefaultServiceRegistry(Function<String, Boolean> permissionChecker) {
        this.services = new ConcurrentHashMap<>();
        this.permissionChecker = Objects.requireNonNull(
            permissionChecker,
            "permissionChecker must not be null"
        );
    }

    @Override
    public <T extends PluginService> void register(
            Class<T> serviceType,
            T implementation,
            String providerId) {
        Objects.requireNonNull(serviceType, "serviceType must not be null");
        Objects.requireNonNull(implementation, "implementation must not be null");
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId must not be null or blank");
        }

        // Check permission
        if (!permissionChecker.apply(providerId)) {
            throw new SecurityException(
                "Plugin '" + providerId + "' does not have SERVICE_PROVIDER permission"
            );
        }

        // Get or create the map for this service type
        Map<String, PluginService> providerMap = services.computeIfAbsent(
            serviceType,
            k -> new ConcurrentHashMap<>()
        );

        // Check if already registered
        if (providerMap.containsKey(providerId)) {
            throw new IllegalStateException(
                "Plugin '" + providerId + "' has already registered service type " +
                serviceType.getName()
            );
        }

        providerMap.put(providerId, implementation);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PluginService> List<T> getServices(Class<T> serviceType) {
        Objects.requireNonNull(serviceType, "serviceType must not be null");

        Map<String, PluginService> providerMap = services.get(serviceType);
        if (providerMap == null || providerMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>(providerMap.size());
        for (PluginService service : providerMap.values()) {
            result.add((T) service);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PluginService> Optional<T> getService(
            Class<T> serviceType,
            String providerId) {
        Objects.requireNonNull(serviceType, "serviceType must not be null");
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId must not be null or blank");
        }

        Map<String, PluginService> providerMap = services.get(serviceType);
        if (providerMap == null) {
            return Optional.empty();
        }

        PluginService service = providerMap.get(providerId);
        return Optional.ofNullable((T) service);
    }

    @Override
    public boolean hasService(Class<? extends PluginService> serviceType) {
        Objects.requireNonNull(serviceType, "serviceType must not be null");

        Map<String, PluginService> providerMap = services.get(serviceType);
        return providerMap != null && !providerMap.isEmpty();
    }

    @Override
    public void unregisterAll(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return;
        }

        // Remove this provider's services from all service types
        for (Map<String, PluginService> providerMap : services.values()) {
            providerMap.remove(providerId);
        }

        // Clean up empty service type entries
        services.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    @Override
    public <T extends PluginService> ServiceReference<T> getReference(Class<T> serviceType) {
        Objects.requireNonNull(serviceType, "serviceType must not be null");
        return new ServiceReferenceImpl<>(this, serviceType);
    }

    /**
     * Returns the number of registered service types.
     *
     * <p>This method is primarily for testing and monitoring.</p>
     *
     * @return the number of service types with at least one registration
     */
    public int getServiceTypeCount() {
        return services.size();
    }

    /**
     * Returns the number of providers registered for a service type.
     *
     * <p>This method is primarily for testing and monitoring.</p>
     *
     * @param serviceType the service type to check
     * @return the number of providers, or 0 if none registered
     */
    public int getProviderCount(Class<? extends PluginService> serviceType) {
        Map<String, PluginService> providerMap = services.get(serviceType);
        return providerMap == null ? 0 : providerMap.size();
    }

    /**
     * Returns the set of provider IDs registered for a service type.
     *
     * <p>This method is primarily for testing and monitoring.</p>
     *
     * @param serviceType the service type to check
     * @return unmodifiable set of provider IDs, or empty set if none
     */
    public Set<String> getProviderIds(Class<? extends PluginService> serviceType) {
        Map<String, PluginService> providerMap = services.get(serviceType);
        if (providerMap == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(providerMap.keySet());
    }
}
