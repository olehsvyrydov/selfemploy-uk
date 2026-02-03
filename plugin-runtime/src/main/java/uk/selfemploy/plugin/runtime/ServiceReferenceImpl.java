package uk.selfemploy.plugin.runtime;

import uk.selfemploy.plugin.api.PluginService;
import uk.selfemploy.plugin.api.ServiceReference;
import uk.selfemploy.plugin.api.ServiceRegistry;

import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of {@link ServiceReference} that provides late binding to services.
 *
 * <p>This implementation resolves services at call time, meaning that services
 * registered after this reference was created will be found when {@link #get()}
 * or {@link #isAvailable()} is called.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe as it delegates all operations to the
 * thread-safe {@link ServiceRegistry} implementation.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ServiceReference<EncryptionService> ref = registry.getReference(EncryptionService.class);
 *
 * // Service might not be available yet
 * if (!ref.isAvailable()) {
 *     System.out.println("No encryption service registered");
 * }
 *
 * // Later, after another plugin registers the service
 * ref.ifAvailable(encryption -> {
 *     byte[] encrypted = encryption.encrypt(data);
 * });
 * }</pre>
 *
 * @param <T> the service type
 * @see ServiceReference
 * @see ServiceRegistry#getReference(Class)
 */
public class ServiceReferenceImpl<T extends PluginService> implements ServiceReference<T> {

    private final ServiceRegistry registry;
    private final Class<T> serviceType;

    /**
     * Creates a new ServiceReferenceImpl.
     *
     * @param registry    the service registry to resolve services from
     * @param serviceType the service type to look up
     * @throws NullPointerException if any argument is null
     */
    public ServiceReferenceImpl(ServiceRegistry registry, Class<T> serviceType) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.serviceType = Objects.requireNonNull(serviceType, "serviceType must not be null");
    }

    @Override
    public Optional<T> get() {
        return registry.getService(serviceType);
    }

    @Override
    public boolean isAvailable() {
        return registry.hasService(serviceType);
    }

    /**
     * Returns the service type this reference points to.
     *
     * @return the service type class
     */
    public Class<T> getServiceType() {
        return serviceType;
    }

    @Override
    public String toString() {
        return String.format(
            "ServiceReference[type=%s, available=%s]",
            serviceType.getSimpleName(),
            isAvailable()
        );
    }
}
