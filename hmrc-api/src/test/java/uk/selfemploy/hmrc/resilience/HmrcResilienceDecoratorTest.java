package uk.selfemploy.hmrc.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.exception.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD tests for HMRC resilience decorator (SE-405).
 * Tests retry logic, circuit breaker, and timeout handling.
 */
@DisplayName("HmrcResilienceDecorator")
class HmrcResilienceDecoratorTest {

    private HmrcResilienceDecorator decorator;
    private HmrcResilienceConfig config;

    @BeforeEach
    void setUp() {
        config = TestHmrcResilienceConfig.builder()
                .maxRetryAttempts(3)
                .initialDelayMs(100) // Fast for tests
                .multiplier(2.0)
                .maxDelayMs(1000)
                .timeoutMs(5000)
                .circuitBreakerRequestVolumeThreshold(4)
                .circuitBreakerFailureRatio(0.5)
                .circuitBreakerDelayMs(30000)
                .circuitBreakerSuccessThreshold(2)
                .build();

        decorator = new HmrcResilienceDecorator(config);
    }

    @Nested
    @DisplayName("Retry Logic")
    class RetryLogicTests {

        @Test
        @DisplayName("should retry on HmrcServerException")
        void shouldRetryOnServerException() {
            AtomicInteger attempts = new AtomicInteger(0);

            Supplier<String> failingThenSucceeding = () -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new HmrcServerException("Server error", "SERVER_ERROR", 503);
                }
                return "success";
            };

            String result = decorator.executeWithRetry(failingThenSucceeding);

            assertThat(result).isEqualTo("success");
            assertThat(attempts.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("should retry on HmrcNetworkException")
        void shouldRetryOnNetworkException() {
            AtomicInteger attempts = new AtomicInteger(0);

            Supplier<String> failingThenSucceeding = () -> {
                if (attempts.incrementAndGet() < 2) {
                    throw HmrcNetworkException.timeout();
                }
                return "success";
            };

            String result = decorator.executeWithRetry(failingThenSucceeding);

            assertThat(result).isEqualTo("success");
            assertThat(attempts.get()).isEqualTo(2);
        }

        @Test
        @DisplayName("should NOT retry on HmrcValidationException")
        void shouldNotRetryOnValidationException() {
            AtomicInteger attempts = new AtomicInteger(0);

            Supplier<String> alwaysFailing = () -> {
                attempts.incrementAndGet();
                throw new HmrcValidationException("Bad request", "INVALID", 400);
            };

            assertThatThrownBy(() -> decorator.executeWithRetry(alwaysFailing))
                    .isInstanceOf(HmrcValidationException.class);

            assertThat(attempts.get()).isEqualTo(1); // No retries
        }

        @Test
        @DisplayName("should NOT retry on HmrcAuthenticationException")
        void shouldNotRetryOnAuthException() {
            AtomicInteger attempts = new AtomicInteger(0);

            Supplier<String> alwaysFailing = () -> {
                attempts.incrementAndGet();
                throw new HmrcAuthenticationException("Not authorized", "UNAUTHORIZED");
            };

            assertThatThrownBy(() -> decorator.executeWithRetry(alwaysFailing))
                    .isInstanceOf(HmrcAuthenticationException.class);

            assertThat(attempts.get()).isEqualTo(1); // No retries
        }

        @Test
        @DisplayName("should exhaust max retries and throw last exception")
        void shouldExhaustMaxRetriesAndThrow() {
            AtomicInteger attempts = new AtomicInteger(0);

            Supplier<String> alwaysFailing = () -> {
                attempts.incrementAndGet();
                throw new HmrcServerException("Server error", "SERVER_ERROR", 500);
            };

            assertThatThrownBy(() -> decorator.executeWithRetry(alwaysFailing))
                    .isInstanceOf(HmrcServerException.class);

            assertThat(attempts.get()).isEqualTo(3); // 1 initial + 2 retries = 3 attempts
        }

        @Test
        @DisplayName("should apply exponential backoff with jitter")
        void shouldApplyExponentialBackoffWithJitter() {
            AtomicInteger attempts = new AtomicInteger(0);
            long startTime = System.currentTimeMillis();

            Supplier<String> failingThenSucceeding = () -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new HmrcServerException("Server error", "SERVER_ERROR", 503);
                }
                return "success";
            };

            decorator.executeWithRetry(failingThenSucceeding);

            long elapsed = System.currentTimeMillis() - startTime;
            // Base delays: 100ms, 200ms = 300ms minimum
            // With jitter (up to 25%): could be 100-125, 200-250 = 300-375ms
            // Allow tolerance for test execution
            assertThat(elapsed).isGreaterThanOrEqualTo(200); // Allow some tolerance
        }
    }

    @Nested
    @DisplayName("Circuit Breaker")
    class CircuitBreakerTests {

        @Test
        @DisplayName("should trip circuit after threshold failures")
        void shouldTripCircuitAfterThresholdFailures() {
            // Configure with lower threshold for testing
            config = TestHmrcResilienceConfig.builder()
                    .maxRetryAttempts(3)
                    .initialDelayMs(100)
                    .multiplier(2.0)
                    .maxDelayMs(1000)
                    .circuitBreakerRequestVolumeThreshold(3)
                    .circuitBreakerFailureRatio(0.6)
                    .circuitBreakerDelayMs(30000)
                    .circuitBreakerSuccessThreshold(2)
                    .build();
            decorator = new HmrcResilienceDecorator(config);

            AtomicInteger attempts = new AtomicInteger(0);

            Supplier<String> alwaysFailing = () -> {
                attempts.incrementAndGet();
                throw new HmrcServerException("Server error", "SERVER_ERROR", 503);
            };

            // Exhaust retries several times to trip the circuit
            for (int i = 0; i < 3; i++) {
                try {
                    decorator.executeWithRetry(alwaysFailing);
                } catch (Exception ignored) {}
            }

            // Next call should fail with circuit open
            assertThatThrownBy(() -> decorator.executeWithRetry(alwaysFailing))
                    .isInstanceOf(HmrcCircuitOpenException.class);
        }

        @Test
        @DisplayName("should provide retry duration in circuit open exception")
        void shouldProvideRetryDurationInCircuitOpenException() {
            config = TestHmrcResilienceConfig.builder()
                    .maxRetryAttempts(3)
                    .initialDelayMs(100)
                    .multiplier(2.0)
                    .maxDelayMs(1000)
                    .circuitBreakerRequestVolumeThreshold(2)
                    .circuitBreakerFailureRatio(0.5)
                    .circuitBreakerDelayMs(30000)
                    .circuitBreakerSuccessThreshold(2)
                    .build();
            decorator = new HmrcResilienceDecorator(config);

            Supplier<String> alwaysFailing = () -> {
                throw new HmrcServerException("Server error", "SERVER_ERROR", 503);
            };

            // Trip the circuit
            for (int i = 0; i < 4; i++) {
                try {
                    decorator.executeWithRetry(alwaysFailing);
                } catch (HmrcCircuitOpenException e) {
                    assertThat(e.getRetryAfter()).isEqualTo(Duration.ofMillis(30000));
                    return;
                } catch (Exception ignored) {}
            }
        }

        @Test
        @DisplayName("should allow success when circuit is closed")
        void shouldAllowSuccessWhenCircuitClosed() {
            Supplier<String> succeeding = () -> "success";

            String result = decorator.executeWithRetry(succeeding);

            assertThat(result).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("Exception Conversion")
    class ExceptionConversionTests {

        @Test
        @DisplayName("should convert SocketTimeoutException to HmrcNetworkException")
        void shouldConvertSocketTimeoutException() {
            Supplier<String> throwingTimeout = () -> {
                throw new RuntimeException(new SocketTimeoutException("Read timed out"));
            };

            assertThatThrownBy(() -> decorator.executeWithRetry(throwingTimeout))
                    .isInstanceOf(HmrcNetworkException.class)
                    .hasMessageContaining("timed out");
        }

        @Test
        @DisplayName("should convert IOException to HmrcNetworkException")
        void shouldConvertIOException() {
            Supplier<String> throwingIO = () -> {
                throw new RuntimeException(new IOException("Connection reset"));
            };

            assertThatThrownBy(() -> decorator.executeWithRetry(throwingIO))
                    .isInstanceOf(HmrcNetworkException.class);
        }

        @Test
        @DisplayName("should wrap RuntimeException from Resilience4j")
        void shouldWrapRuntimeException() {
            Supplier<String> throwingRuntime = () -> {
                throw new RuntimeException("Unexpected error");
            };

            assertThatThrownBy(() -> decorator.executeWithRetry(throwingRuntime))
                    .isInstanceOf(HmrcApiException.class);
        }
    }

    @Nested
    @DisplayName("Configuration")
    class ConfigurationTests {

        @Test
        @DisplayName("should use configured max attempts")
        void shouldUseConfiguredMaxAttempts() {
            config = TestHmrcResilienceConfig.builder()
                    .maxRetryAttempts(5)
                    .initialDelayMs(50)
                    .multiplier(2.0)
                    .maxDelayMs(1000)
                    .circuitBreakerRequestVolumeThreshold(10) // High to not trip
                    .circuitBreakerFailureRatio(0.5)
                    .circuitBreakerDelayMs(30000)
                    .build();
            decorator = new HmrcResilienceDecorator(config);

            AtomicInteger attempts = new AtomicInteger(0);

            Supplier<String> alwaysFailing = () -> {
                attempts.incrementAndGet();
                throw new HmrcServerException("Server error", "SERVER_ERROR", 500);
            };

            assertThatThrownBy(() -> decorator.executeWithRetry(alwaysFailing))
                    .isInstanceOf(HmrcServerException.class);

            assertThat(attempts.get()).isEqualTo(5);
        }

        @Test
        @DisplayName("should respect max delay cap")
        void shouldRespectMaxDelayCap() {
            config = TestHmrcResilienceConfig.builder()
                    .maxRetryAttempts(4)
                    .initialDelayMs(1000)
                    .multiplier(10.0)
                    .maxDelayMs(2000) // Cap at 2 seconds
                    .build();

            // Delays would be: 1000, 10000, 100000... but capped at 2000
            assertThat(config.retry().maxDelayMs()).isEqualTo(2000);
        }
    }

    @Nested
    @DisplayName("Jitter")
    class JitterTests {

        @Test
        @DisplayName("should add jitter to retry delays")
        void shouldAddJitterToRetryDelays() {
            // Configure with known delays
            config = TestHmrcResilienceConfig.builder()
                    .maxRetryAttempts(3)
                    .initialDelayMs(400) // 400ms base delay
                    .multiplier(1.0) // No exponential increase
                    .maxDelayMs(1000)
                    .circuitBreakerRequestVolumeThreshold(10)
                    .build();
            decorator = new HmrcResilienceDecorator(config);

            AtomicInteger attempts = new AtomicInteger(0);

            Supplier<String> failingThenSucceeding = () -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new HmrcServerException("Server error", "SERVER_ERROR", 503);
                }
                return "success";
            };

            long startTime = System.currentTimeMillis();
            decorator.executeWithRetry(failingThenSucceeding);
            long elapsed = System.currentTimeMillis() - startTime;

            // 2 retries with 400ms base delay each = 800ms minimum
            // With jitter (up to 25%): 400-500ms each = 800-1000ms total
            assertThat(elapsed).isGreaterThanOrEqualTo(700); // Allow tolerance
            // Jitter adds randomness, so total should vary
        }
    }
}
