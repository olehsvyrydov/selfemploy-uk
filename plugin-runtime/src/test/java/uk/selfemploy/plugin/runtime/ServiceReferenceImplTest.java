package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.plugin.api.PluginService;
import uk.selfemploy.plugin.api.ServiceRegistry;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ServiceReferenceImpl}.
 */
@DisplayName("ServiceReferenceImpl")
class ServiceReferenceImplTest {

    private DefaultServiceRegistry registry;
    private ServiceReferenceImpl<TestService> reference;

    // Test service interface
    interface TestService extends PluginService {
        String getValue();
    }

    // Test implementation
    static class TestServiceImpl implements TestService {
        private final String value;

        TestServiceImpl(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    @BeforeEach
    void setUp() {
        registry = new DefaultServiceRegistry();
        reference = new ServiceReferenceImpl<>(registry, TestService.class);
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should create reference with valid parameters")
        void shouldCreateReferenceWithValidParameters() {
            var ref = new ServiceReferenceImpl<>(registry, TestService.class);

            assertThat(ref).isNotNull();
            assertThat(ref.getServiceType()).isEqualTo(TestService.class);
        }

        @Test
        @DisplayName("should reject null registry")
        void shouldRejectNullRegistry() {
            assertThatThrownBy(() -> new ServiceReferenceImpl<>(null, TestService.class))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("registry");
        }

        @Test
        @DisplayName("should reject null service type")
        void shouldRejectNullServiceType() {
            assertThatThrownBy(() -> new ServiceReferenceImpl<>(registry, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("serviceType");
        }
    }

    @Nested
    @DisplayName("get()")
    class GetTests {

        @Test
        @DisplayName("should return empty when no service registered")
        void shouldReturnEmptyWhenNoServiceRegistered() {
            Optional<TestService> result = reference.get();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return service when registered")
        void shouldReturnServiceWhenRegistered() {
            var service = new TestServiceImpl("test");
            registry.register(TestService.class, service, "provider.a");

            Optional<TestService> result = reference.get();

            assertThat(result).isPresent();
            assertThat(result.get().getValue()).isEqualTo("test");
        }

        @Test
        @DisplayName("should resolve late-bound services")
        void shouldResolveLateBooundServices() {
            // Reference created before service registered
            assertThat(reference.get()).isEmpty();

            // Register service later
            registry.register(TestService.class, new TestServiceImpl("late"), "provider.a");

            // Reference should now find it
            assertThat(reference.get()).isPresent();
            assertThat(reference.get().get().getValue()).isEqualTo("late");
        }

        @Test
        @DisplayName("should return empty after service unregistered")
        void shouldReturnEmptyAfterServiceUnregistered() {
            var service = new TestServiceImpl("test");
            registry.register(TestService.class, service, "provider.a");
            assertThat(reference.get()).isPresent();

            registry.unregisterAll("provider.a");

            assertThat(reference.get()).isEmpty();
        }
    }

    @Nested
    @DisplayName("isAvailable()")
    class IsAvailableTests {

        @Test
        @DisplayName("should return false when no service registered")
        void shouldReturnFalseWhenNoServiceRegistered() {
            assertThat(reference.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("should return true when service registered")
        void shouldReturnTrueWhenServiceRegistered() {
            registry.register(TestService.class, new TestServiceImpl("test"), "provider.a");

            assertThat(reference.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("should update dynamically")
        void shouldUpdateDynamically() {
            assertThat(reference.isAvailable()).isFalse();

            registry.register(TestService.class, new TestServiceImpl("test"), "provider.a");
            assertThat(reference.isAvailable()).isTrue();

            registry.unregisterAll("provider.a");
            assertThat(reference.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("ifAvailable()")
    class IfAvailableTests {

        @Test
        @DisplayName("should not execute consumer when service not available")
        void shouldNotExecuteConsumerWhenServiceNotAvailable() {
            AtomicBoolean executed = new AtomicBoolean(false);

            reference.ifAvailable(service -> executed.set(true));

            assertThat(executed.get()).isFalse();
        }

        @Test
        @DisplayName("should execute consumer when service available")
        void shouldExecuteConsumerWhenServiceAvailable() {
            registry.register(TestService.class, new TestServiceImpl("test"), "provider.a");
            AtomicReference<String> capturedValue = new AtomicReference<>();

            reference.ifAvailable(service -> capturedValue.set(service.getValue()));

            assertThat(capturedValue.get()).isEqualTo("test");
        }
    }

    @Nested
    @DisplayName("orElse()")
    class OrElseTests {

        @Test
        @DisplayName("should return service when available")
        void shouldReturnServiceWhenAvailable() {
            var registered = new TestServiceImpl("registered");
            var fallback = new TestServiceImpl("fallback");
            registry.register(TestService.class, registered, "provider.a");

            TestService result = reference.orElse(fallback);

            assertThat(result.getValue()).isEqualTo("registered");
        }

        @Test
        @DisplayName("should return default when service not available")
        void shouldReturnDefaultWhenServiceNotAvailable() {
            var fallback = new TestServiceImpl("fallback");

            TestService result = reference.orElse(fallback);

            assertThat(result.getValue()).isEqualTo("fallback");
        }

        @Test
        @DisplayName("should return null when service not available and default is null")
        void shouldReturnNullWhenServiceNotAvailableAndDefaultIsNull() {
            TestService result = reference.orElse(null);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getServiceType()")
    class GetServiceTypeTests {

        @Test
        @DisplayName("should return the service type")
        void shouldReturnServiceType() {
            assertThat(reference.getServiceType()).isEqualTo(TestService.class);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("should include service type name")
        void shouldIncludeServiceTypeName() {
            String result = reference.toString();

            assertThat(result).contains("TestService");
        }

        @Test
        @DisplayName("should include availability status")
        void shouldIncludeAvailabilityStatus() {
            assertThat(reference.toString()).contains("available=false");

            registry.register(TestService.class, new TestServiceImpl("test"), "provider.a");

            assertThat(reference.toString()).contains("available=true");
        }
    }

    @Nested
    @DisplayName("Integration with ServiceRegistry")
    class IntegrationTests {

        @Test
        @DisplayName("should work with multiple providers")
        void shouldWorkWithMultipleProviders() {
            registry.register(TestService.class, new TestServiceImpl("first"), "provider.a");
            registry.register(TestService.class, new TestServiceImpl("second"), "provider.b");

            assertThat(reference.isAvailable()).isTrue();
            assertThat(reference.get()).isPresent();
        }

        @Test
        @DisplayName("should survive partial unregistration")
        void shouldSurvivePartialUnregistration() {
            registry.register(TestService.class, new TestServiceImpl("first"), "provider.a");
            registry.register(TestService.class, new TestServiceImpl("second"), "provider.b");

            registry.unregisterAll("provider.a");

            assertThat(reference.isAvailable()).isTrue();
            assertThat(reference.get()).isPresent();
        }
    }
}
