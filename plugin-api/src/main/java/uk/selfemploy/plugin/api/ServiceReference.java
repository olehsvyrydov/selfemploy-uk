package uk.selfemploy.plugin.api;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Late-bound service reference for optional dependencies.
 *
 * <p>A ServiceReference allows plugins to consume services that may or may not
 * be available at runtime. The reference resolves at call time, meaning it will
 * return the service if it has been registered since the reference was created.</p>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Optional service consumption without hard dependency</li>
 *   <li>Services that may be registered after consumer plugin loads</li>
 *   <li>Graceful degradation when service is unavailable</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get a late-bound reference
 * ServiceReference<EncryptionService> encryptionRef =
 *     context.getServiceRegistry().getReference(EncryptionService.class);
 *
 * // Use the service if available
 * encryptionRef.ifAvailable(encryption -> {
 *     byte[] encrypted = encryption.encrypt(data);
 * });
 *
 * // Or check and get
 * if (encryptionRef.isAvailable()) {
 *     EncryptionService encryption = encryptionRef.get().orElseThrow();
 * }
 * }</pre>
 *
 * @param <T> the service type
 * @see ServiceRegistry#getReference(Class)
 */
public interface ServiceReference<T extends PluginService> {

    /**
     * Gets the service if available.
     *
     * <p>This method resolves at call time, so a service registered after
     * this reference was created will be found.</p>
     *
     * @return the service wrapped in Optional, or empty if not available
     */
    Optional<T> get();

    /**
     * Checks if the service is currently available.
     *
     * @return true if at least one implementation is registered
     */
    boolean isAvailable();

    /**
     * Executes the consumer if the service is available.
     *
     * <p>This is a convenience method for optional service usage.</p>
     *
     * @param consumer the consumer to execute with the service
     */
    default void ifAvailable(Consumer<T> consumer) {
        get().ifPresent(consumer);
    }

    /**
     * Gets the service or returns a default value.
     *
     * @param defaultValue the value to return if service is not available
     * @return the service or the default value
     */
    default T orElse(T defaultValue) {
        return get().orElse(defaultValue);
    }
}
