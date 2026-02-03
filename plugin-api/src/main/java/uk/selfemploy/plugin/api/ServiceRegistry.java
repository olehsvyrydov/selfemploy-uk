package uk.selfemploy.plugin.api;

import java.util.List;
import java.util.Optional;

/**
 * Registry for plugin-provided services.
 *
 * <p>The Service Registry allows plugins to provide services that other plugins
 * can consume. This enables loose coupling between plugins through shared
 * interfaces.</p>
 *
 * <h2>Service Provider Pattern</h2>
 * <ol>
 *   <li>Define a service interface extending {@link PluginService}</li>
 *   <li>Provider plugin registers implementation via {@link #register}</li>
 *   <li>Consumer plugin retrieves service via {@link #getService} or {@link #getServices}</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe. Service registration and lookup can occur
 * concurrently from multiple threads.</p>
 *
 * <h2>Lifecycle</h2>
 * <p>Services are automatically unregistered when the provider plugin is unloaded.
 * Consumer plugins should handle service unavailability gracefully, especially
 * when using optional services.</p>
 *
 * <h2>Permissions</h2>
 * <p>Registering services requires the {@code SERVICE_PROVIDER} permission.</p>
 *
 * @see PluginService
 * @see ServiceReference
 * @see PluginPermission#SERVICE_PROVIDER
 */
public interface ServiceRegistry {

    /**
     * Registers a service implementation.
     *
     * <p>The provider must have the {@code SERVICE_PROVIDER} permission.
     * Multiple implementations of the same service type can be registered
     * by different providers.</p>
     *
     * @param <T>            the service type
     * @param serviceType    the service interface class
     * @param implementation the implementation instance
     * @param providerId     the plugin ID providing this service
     * @throws SecurityException        if provider lacks SERVICE_PROVIDER permission
     * @throws IllegalArgumentException if any parameter is null
     * @throws IllegalStateException    if the same provider already registered this service type
     */
    <T extends PluginService> void register(
        Class<T> serviceType,
        T implementation,
        String providerId
    );

    /**
     * Gets all implementations of a service type.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @return list of all registered implementations, empty if none
     */
    <T extends PluginService> List<T> getServices(Class<T> serviceType);

    /**
     * Gets a specific implementation by provider ID.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @param providerId  the plugin ID that registered the service
     * @return the implementation wrapped in Optional, or empty if not found
     */
    <T extends PluginService> Optional<T> getService(
        Class<T> serviceType,
        String providerId
    );

    /**
     * Gets the first available implementation of a service type.
     *
     * <p>Use this when you don't care which provider supplies the service.</p>
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @return the first available implementation, or empty if none registered
     */
    default <T extends PluginService> Optional<T> getService(Class<T> serviceType) {
        List<T> services = getServices(serviceType);
        return services.isEmpty() ? Optional.empty() : Optional.of(services.get(0));
    }

    /**
     * Checks if any implementation of the service type is available.
     *
     * @param serviceType the service interface class
     * @return true if at least one implementation is registered
     */
    boolean hasService(Class<? extends PluginService> serviceType);

    /**
     * Unregisters all services from a provider.
     *
     * <p>Called automatically when a plugin is unloaded. Should not be
     * called directly by plugins.</p>
     *
     * @param providerId the plugin ID whose services should be unregistered
     */
    void unregisterAll(String providerId);

    /**
     * Gets a late-bound reference to a service.
     *
     * <p>The reference resolves at call time, so services registered after
     * this method is called will be found. This is useful for optional
     * dependencies.</p>
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @return a service reference that resolves at call time
     */
    <T extends PluginService> ServiceReference<T> getReference(Class<T> serviceType);
}
