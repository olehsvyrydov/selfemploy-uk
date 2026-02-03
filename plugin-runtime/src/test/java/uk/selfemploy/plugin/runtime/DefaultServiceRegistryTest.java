package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.plugin.api.PluginService;
import uk.selfemploy.plugin.api.ServiceReference;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultServiceRegistry}.
 */
@DisplayName("DefaultServiceRegistry")
class DefaultServiceRegistryTest {

    private DefaultServiceRegistry registry;

    // Test service interfaces
    interface TestService extends PluginService {
        String getName();
    }

    interface AnotherService extends PluginService {
        int getValue();
    }

    // Test implementations
    static class TestServiceImpl implements TestService {
        private final String name;

        TestServiceImpl(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    static class AnotherServiceImpl implements AnotherService {
        private final int value;

        AnotherServiceImpl(int value) {
            this.value = value;
        }

        @Override
        public int getValue() {
            return value;
        }
    }

    @BeforeEach
    void setUp() {
        registry = new DefaultServiceRegistry();
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should create registry with no permission checking")
        void shouldCreateRegistryWithNoPermissionChecking() {
            var reg = new DefaultServiceRegistry();
            assertThat(reg.getServiceTypeCount()).isZero();
        }

        @Test
        @DisplayName("should create registry with permission checker")
        void shouldCreateRegistryWithPermissionChecker() {
            var reg = new DefaultServiceRegistry(providerId -> true);
            assertThat(reg.getServiceTypeCount()).isZero();
        }

        @Test
        @DisplayName("should reject null permission checker")
        void shouldRejectNullPermissionChecker() {
            assertThatThrownBy(() -> new DefaultServiceRegistry(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("permissionChecker");
        }
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("should register a service")
        void shouldRegisterService() {
            var service = new TestServiceImpl("test");

            registry.register(TestService.class, service, "provider.a");

            assertThat(registry.hasService(TestService.class)).isTrue();
            assertThat(registry.getProviderCount(TestService.class)).isEqualTo(1);
        }

        @Test
        @DisplayName("should allow multiple providers for same service type")
        void shouldAllowMultipleProvidersForSameServiceType() {
            var service1 = new TestServiceImpl("service1");
            var service2 = new TestServiceImpl("service2");

            registry.register(TestService.class, service1, "provider.a");
            registry.register(TestService.class, service2, "provider.b");

            assertThat(registry.getProviderCount(TestService.class)).isEqualTo(2);
            assertThat(registry.getProviderIds(TestService.class))
                .containsExactlyInAnyOrder("provider.a", "provider.b");
        }

        @Test
        @DisplayName("should allow same provider to register different service types")
        void shouldAllowSameProviderToRegisterDifferentServiceTypes() {
            var testService = new TestServiceImpl("test");
            var anotherService = new AnotherServiceImpl(42);

            registry.register(TestService.class, testService, "provider.a");
            registry.register(AnotherService.class, anotherService, "provider.a");

            assertThat(registry.getServiceTypeCount()).isEqualTo(2);
            assertThat(registry.hasService(TestService.class)).isTrue();
            assertThat(registry.hasService(AnotherService.class)).isTrue();
        }

        @Test
        @DisplayName("should reject null service type")
        void shouldRejectNullServiceType() {
            var service = new TestServiceImpl("test");

            assertThatThrownBy(() -> registry.register(null, service, "provider.a"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("serviceType");
        }

        @Test
        @DisplayName("should reject null implementation")
        void shouldRejectNullImplementation() {
            assertThatThrownBy(() -> registry.register(TestService.class, null, "provider.a"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("implementation");
        }

        @Test
        @DisplayName("should reject null provider ID")
        void shouldRejectNullProviderId() {
            var service = new TestServiceImpl("test");

            assertThatThrownBy(() -> registry.register(TestService.class, service, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("providerId");
        }

        @Test
        @DisplayName("should reject blank provider ID")
        void shouldRejectBlankProviderId() {
            var service = new TestServiceImpl("test");

            assertThatThrownBy(() -> registry.register(TestService.class, service, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("providerId");
        }

        @Test
        @DisplayName("should reject duplicate registration from same provider")
        void shouldRejectDuplicateRegistrationFromSameProvider() {
            var service1 = new TestServiceImpl("service1");
            var service2 = new TestServiceImpl("service2");

            registry.register(TestService.class, service1, "provider.a");

            assertThatThrownBy(() -> registry.register(TestService.class, service2, "provider.a"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already registered");
        }

        @Test
        @DisplayName("should throw SecurityException when permission denied")
        void shouldThrowSecurityExceptionWhenPermissionDenied() {
            var permissionDeniedRegistry = new DefaultServiceRegistry(providerId -> false);
            var service = new TestServiceImpl("test");

            assertThatThrownBy(() ->
                permissionDeniedRegistry.register(TestService.class, service, "provider.a")
            )
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("SERVICE_PROVIDER permission");
        }

        @Test
        @DisplayName("should allow registration when permission granted")
        void shouldAllowRegistrationWhenPermissionGranted() {
            Set<String> allowedProviders = Set.of("provider.allowed");
            var selectiveRegistry = new DefaultServiceRegistry(allowedProviders::contains);
            var service = new TestServiceImpl("test");

            selectiveRegistry.register(TestService.class, service, "provider.allowed");

            assertThat(selectiveRegistry.hasService(TestService.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("getServices()")
    class GetServicesTests {

        @Test
        @DisplayName("should return empty list when no services registered")
        void shouldReturnEmptyListWhenNoServicesRegistered() {
            List<TestService> services = registry.getServices(TestService.class);

            assertThat(services).isEmpty();
        }

        @Test
        @DisplayName("should return all registered implementations")
        void shouldReturnAllRegisteredImplementations() {
            var service1 = new TestServiceImpl("service1");
            var service2 = new TestServiceImpl("service2");
            var service3 = new TestServiceImpl("service3");

            registry.register(TestService.class, service1, "provider.a");
            registry.register(TestService.class, service2, "provider.b");
            registry.register(TestService.class, service3, "provider.c");

            List<TestService> services = registry.getServices(TestService.class);

            assertThat(services).hasSize(3);
            assertThat(services.stream().map(TestService::getName))
                .containsExactlyInAnyOrder("service1", "service2", "service3");
        }

        @Test
        @DisplayName("should return unmodifiable list")
        void shouldReturnUnmodifiableList() {
            var service = new TestServiceImpl("test");
            registry.register(TestService.class, service, "provider.a");

            List<TestService> services = registry.getServices(TestService.class);

            assertThatThrownBy(() -> services.add(new TestServiceImpl("hack")))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should reject null service type")
        void shouldRejectNullServiceType() {
            assertThatThrownBy(() -> registry.getServices(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("serviceType");
        }
    }

    @Nested
    @DisplayName("getService(Class, String)")
    class GetServiceByProviderTests {

        @Test
        @DisplayName("should return service from specific provider")
        void shouldReturnServiceFromSpecificProvider() {
            var service1 = new TestServiceImpl("service1");
            var service2 = new TestServiceImpl("service2");
            registry.register(TestService.class, service1, "provider.a");
            registry.register(TestService.class, service2, "provider.b");

            Optional<TestService> result = registry.getService(TestService.class, "provider.a");

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("service1");
        }

        @Test
        @DisplayName("should return empty when provider not found")
        void shouldReturnEmptyWhenProviderNotFound() {
            var service = new TestServiceImpl("test");
            registry.register(TestService.class, service, "provider.a");

            Optional<TestService> result = registry.getService(TestService.class, "provider.unknown");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when service type not registered")
        void shouldReturnEmptyWhenServiceTypeNotRegistered() {
            Optional<TestService> result = registry.getService(TestService.class, "provider.a");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should reject null service type")
        void shouldRejectNullServiceType() {
            assertThatThrownBy(() -> registry.getService(null, "provider.a"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("serviceType");
        }

        @Test
        @DisplayName("should reject null provider ID")
        void shouldRejectNullProviderId() {
            assertThatThrownBy(() -> registry.getService(TestService.class, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("providerId");
        }

        @Test
        @DisplayName("should reject blank provider ID")
        void shouldRejectBlankProviderId() {
            assertThatThrownBy(() -> registry.getService(TestService.class, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("providerId");
        }
    }

    @Nested
    @DisplayName("getService(Class) - default method")
    class GetServiceDefaultTests {

        @Test
        @DisplayName("should return first available service")
        void shouldReturnFirstAvailableService() {
            var service = new TestServiceImpl("test");
            registry.register(TestService.class, service, "provider.a");

            Optional<TestService> result = registry.getService(TestService.class);

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("test");
        }

        @Test
        @DisplayName("should return empty when no services registered")
        void shouldReturnEmptyWhenNoServicesRegistered() {
            Optional<TestService> result = registry.getService(TestService.class);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasService()")
    class HasServiceTests {

        @Test
        @DisplayName("should return true when service registered")
        void shouldReturnTrueWhenServiceRegistered() {
            var service = new TestServiceImpl("test");
            registry.register(TestService.class, service, "provider.a");

            assertThat(registry.hasService(TestService.class)).isTrue();
        }

        @Test
        @DisplayName("should return false when no service registered")
        void shouldReturnFalseWhenNoServiceRegistered() {
            assertThat(registry.hasService(TestService.class)).isFalse();
        }

        @Test
        @DisplayName("should reject null service type")
        void shouldRejectNullServiceType() {
            assertThatThrownBy(() -> registry.hasService(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("serviceType");
        }

        @Test
        @DisplayName("should return false after all providers unregistered")
        void shouldReturnFalseAfterAllProvidersUnregistered() {
            var service = new TestServiceImpl("test");
            registry.register(TestService.class, service, "provider.a");

            registry.unregisterAll("provider.a");

            assertThat(registry.hasService(TestService.class)).isFalse();
        }
    }

    @Nested
    @DisplayName("unregisterAll()")
    class UnregisterAllTests {

        @Test
        @DisplayName("should unregister all services from provider")
        void shouldUnregisterAllServicesFromProvider() {
            var testService = new TestServiceImpl("test");
            var anotherService = new AnotherServiceImpl(42);
            registry.register(TestService.class, testService, "provider.a");
            registry.register(AnotherService.class, anotherService, "provider.a");

            registry.unregisterAll("provider.a");

            assertThat(registry.hasService(TestService.class)).isFalse();
            assertThat(registry.hasService(AnotherService.class)).isFalse();
            assertThat(registry.getServiceTypeCount()).isZero();
        }

        @Test
        @DisplayName("should not affect other providers")
        void shouldNotAffectOtherProviders() {
            var service1 = new TestServiceImpl("service1");
            var service2 = new TestServiceImpl("service2");
            registry.register(TestService.class, service1, "provider.a");
            registry.register(TestService.class, service2, "provider.b");

            registry.unregisterAll("provider.a");

            assertThat(registry.hasService(TestService.class)).isTrue();
            assertThat(registry.getProviderCount(TestService.class)).isEqualTo(1);
            assertThat(registry.getProviderIds(TestService.class)).containsExactly("provider.b");
        }

        @Test
        @DisplayName("should handle null provider ID gracefully")
        void shouldHandleNullProviderIdGracefully() {
            var service = new TestServiceImpl("test");
            registry.register(TestService.class, service, "provider.a");

            registry.unregisterAll(null);

            assertThat(registry.hasService(TestService.class)).isTrue();
        }

        @Test
        @DisplayName("should handle blank provider ID gracefully")
        void shouldHandleBlankProviderIdGracefully() {
            var service = new TestServiceImpl("test");
            registry.register(TestService.class, service, "provider.a");

            registry.unregisterAll("  ");

            assertThat(registry.hasService(TestService.class)).isTrue();
        }

        @Test
        @DisplayName("should handle unknown provider ID gracefully")
        void shouldHandleUnknownProviderIdGracefully() {
            var service = new TestServiceImpl("test");
            registry.register(TestService.class, service, "provider.a");

            registry.unregisterAll("provider.unknown");

            assertThat(registry.hasService(TestService.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("getReference()")
    class GetReferenceTests {

        @Test
        @DisplayName("should return a service reference")
        void shouldReturnServiceReference() {
            ServiceReference<TestService> ref = registry.getReference(TestService.class);

            assertThat(ref).isNotNull();
            assertThat(ref.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("should reject null service type")
        void shouldRejectNullServiceType() {
            assertThatThrownBy(() -> registry.getReference(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("serviceType");
        }

        @Test
        @DisplayName("reference should resolve late-bound services")
        void referenceShouldResolveLateBooundServices() {
            // Get reference before service is registered
            ServiceReference<TestService> ref = registry.getReference(TestService.class);
            assertThat(ref.isAvailable()).isFalse();

            // Register service
            var service = new TestServiceImpl("test");
            registry.register(TestService.class, service, "provider.a");

            // Reference should now find the service
            assertThat(ref.isAvailable()).isTrue();
            assertThat(ref.get()).isPresent();
            assertThat(ref.get().get().getName()).isEqualTo("test");
        }

        @Test
        @DisplayName("reference should reflect unregistration")
        void referenceShouldReflectUnregistration() {
            var service = new TestServiceImpl("test");
            registry.register(TestService.class, service, "provider.a");

            ServiceReference<TestService> ref = registry.getReference(TestService.class);
            assertThat(ref.isAvailable()).isTrue();

            registry.unregisterAll("provider.a");

            assertThat(ref.isAvailable()).isFalse();
            assertThat(ref.get()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafetyTests {

        @Test
        @DisplayName("should handle concurrent registrations")
        void shouldHandleConcurrentRegistrations() throws InterruptedException {
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    var service = new TestServiceImpl("service" + index);
                    registry.register(TestService.class, service, "provider." + index);
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            assertThat(registry.getProviderCount(TestService.class)).isEqualTo(threadCount);
        }

        @Test
        @DisplayName("should handle concurrent reads and writes")
        void shouldHandleConcurrentReadsAndWrites() throws InterruptedException {
            int iterations = 100;
            Set<String> results = new HashSet<>();

            // Register some initial services
            registry.register(TestService.class, new TestServiceImpl("initial"), "provider.0");

            Thread writer = new Thread(() -> {
                for (int i = 1; i <= iterations; i++) {
                    var service = new TestServiceImpl("service" + i);
                    registry.register(TestService.class, service, "provider." + i);
                }
            });

            Thread reader = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    List<TestService> services = registry.getServices(TestService.class);
                    for (TestService service : services) {
                        results.add(service.getName());
                    }
                }
            });

            writer.start();
            reader.start();

            writer.join();
            reader.join();

            // Should have all services registered
            assertThat(registry.getProviderCount(TestService.class)).isEqualTo(iterations + 1);
        }
    }

    @Nested
    @DisplayName("Monitoring methods")
    class MonitoringMethodsTests {

        @Test
        @DisplayName("getServiceTypeCount should return correct count")
        void getServiceTypeCountShouldReturnCorrectCount() {
            assertThat(registry.getServiceTypeCount()).isZero();

            registry.register(TestService.class, new TestServiceImpl("test"), "provider.a");
            assertThat(registry.getServiceTypeCount()).isEqualTo(1);

            registry.register(AnotherService.class, new AnotherServiceImpl(1), "provider.a");
            assertThat(registry.getServiceTypeCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("getProviderCount should return correct count")
        void getProviderCountShouldReturnCorrectCount() {
            assertThat(registry.getProviderCount(TestService.class)).isZero();

            registry.register(TestService.class, new TestServiceImpl("test1"), "provider.a");
            assertThat(registry.getProviderCount(TestService.class)).isEqualTo(1);

            registry.register(TestService.class, new TestServiceImpl("test2"), "provider.b");
            assertThat(registry.getProviderCount(TestService.class)).isEqualTo(2);
        }

        @Test
        @DisplayName("getProviderIds should return correct IDs")
        void getProviderIdsShouldReturnCorrectIds() {
            assertThat(registry.getProviderIds(TestService.class)).isEmpty();

            registry.register(TestService.class, new TestServiceImpl("test1"), "provider.a");
            registry.register(TestService.class, new TestServiceImpl("test2"), "provider.b");

            assertThat(registry.getProviderIds(TestService.class))
                .containsExactlyInAnyOrder("provider.a", "provider.b");
        }
    }
}
