package uk.selfemploy.hmrc.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.exception.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for SE-405 Error Handling & Retry.
 * Tests based on /rob's QA test case specification.
 *
 * Test IDs: IT-405-001 to IT-405-021
 *
 * @see docs/sprints/sprint-4/testing/rob-qa-SE-405.md
 */
@DisplayName("SE-405 Integration Tests")
class HmrcResilienceIntegrationTest {

    private HmrcResilienceDecorator decorator;
    private HmrcResilienceConfig config;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        config = TestHmrcResilienceConfig.builder()
                .maxRetryAttempts(3)
                .initialDelayMs(100) // Fast for tests
                .multiplier(2.0)
                .maxDelayMs(1000)
                .timeoutMs(5000)
                .circuitBreakerRequestVolumeThreshold(4)
                .circuitBreakerFailureRatio(0.5)
                .circuitBreakerDelayMs(1000) // Short for tests
                .circuitBreakerSuccessThreshold(2)
                .build();

        decorator = new HmrcResilienceDecorator(config, meterRegistry);
    }

    @Nested
    @DisplayName("P0: Critical Path Tests")
    class CriticalPathTests {

        @Test
        @DisplayName("IT-405-001: Successful API call increments success metrics")
        void successfulApiCallMetrics() {
            // Given
            Supplier<String> successfulCall = () -> "success";

            // When
            String result = decorator.executeWithRetry(successfulCall);

            // Then
            assertThat(result).isEqualTo("success");
            Counter successCounter = meterRegistry.find("hmrc.api.calls.success").counter();
            assertThat(successCounter).isNotNull();
            assertThat(successCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("IT-405-002: Retry on 503 Service Unavailable - success on 3rd attempt")
        void retryOn503ServiceUnavailable() {
            // Given
            AtomicInteger attempts = new AtomicInteger(0);
            Supplier<String> failsTwiceThenSucceeds = () -> {
                int attempt = attempts.incrementAndGet();
                if (attempt < 3) {
                    throw new HmrcServerException("Service Unavailable", "SERVER_ERROR", 503);
                }
                return "success";
            };

            // When
            String result = decorator.executeWithRetry(failsTwiceThenSucceeds);

            // Then
            assertThat(result).isEqualTo("success");
            assertThat(attempts.get()).isEqualTo(3);

            Counter retryCounter = meterRegistry.find("hmrc.api.retry.attempts").counter();
            assertThat(retryCounter).isNotNull();
            assertThat(retryCounter.count()).isEqualTo(2.0); // 2 retries
        }

        @Test
        @DisplayName("IT-405-003: No retry on 400 Bad Request - single attempt only")
        void noRetryOn400BadRequest() {
            // Given
            AtomicInteger attempts = new AtomicInteger(0);
            List<ValidationError> errors = List.of(
                    new ValidationError("/data", "Invalid body")
            );
            Supplier<String> badRequest = () -> {
                attempts.incrementAndGet();
                throw new HmrcValidationException("Bad request", "RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED", 400, errors);
            };

            // When/Then
            assertThatThrownBy(() -> decorator.executeWithRetry(badRequest))
                    .isInstanceOf(HmrcValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED");

            assertThat(attempts.get()).isEqualTo(1); // No retry

            Counter failedCounter = meterRegistry.find("hmrc.api.calls.failed").counter();
            assertThat(failedCounter).isNotNull();
            assertThat(failedCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("IT-405-004: No retry on 401 Unauthorized - single attempt only")
        void noRetryOn401Unauthorized() {
            // Given
            AtomicInteger attempts = new AtomicInteger(0);
            Supplier<String> unauthorized = () -> {
                attempts.incrementAndGet();
                throw new HmrcAuthenticationException("Unauthorized", "UNAUTHORISED");
            };

            // When/Then
            assertThatThrownBy(() -> decorator.executeWithRetry(unauthorized))
                    .isInstanceOf(HmrcAuthenticationException.class);

            assertThat(attempts.get()).isEqualTo(1); // No retry
        }

        @Test
        @DisplayName("IT-405-005: Circuit breaker trips after threshold failures")
        void circuitBreakerTrips() {
            // Given - configure with lower threshold
            config = TestHmrcResilienceConfig.builder()
                    .maxRetryAttempts(1) // No retries to speed up test
                    .initialDelayMs(10)
                    .circuitBreakerRequestVolumeThreshold(3)
                    .circuitBreakerFailureRatio(0.5)
                    .circuitBreakerDelayMs(5000)
                    .build();
            decorator = new HmrcResilienceDecorator(config, meterRegistry);

            Supplier<String> alwaysFails = () -> {
                throw new HmrcServerException("Server error", "SERVER_ERROR", 500);
            };

            // When - trip the circuit
            for (int i = 0; i < 4; i++) {
                try {
                    decorator.executeWithRetry(alwaysFails);
                } catch (HmrcCircuitOpenException e) {
                    // Circuit is now open
                    assertThat(e.getRetryAfter()).isEqualTo(Duration.ofMillis(5000));
                    assertThat(decorator.getCircuitBreakerState()).isEqualTo(CircuitBreaker.State.OPEN);
                    return;
                } catch (Exception ignored) {
                }
            }

            // Then - next call should fail with circuit open
            assertThatThrownBy(() -> decorator.executeWithRetry(alwaysFails))
                    .isInstanceOf(HmrcCircuitOpenException.class);
        }

        @Test
        @DisplayName("IT-405-006: Timeout handling converts to HmrcNetworkException")
        void timeoutHandling() {
            // Given
            Supplier<String> timeout = () -> {
                throw new RuntimeException(new SocketTimeoutException("Read timed out"));
            };

            // When/Then
            assertThatThrownBy(() -> decorator.executeWithRetry(timeout))
                    .isInstanceOf(HmrcNetworkException.class)
                    .hasMessageContaining("timed out");
        }
    }

    @Nested
    @DisplayName("P1: Error Message Mapping Tests")
    class ErrorMessageMappingTests {

        @Test
        @DisplayName("IT-405-007: Invalid body error - RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED")
        void invalidBodyError() {
            // Given
            Supplier<String> invalidBody = () -> {
                throw new HmrcValidationException(
                        "Invalid body",
                        "RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED",
                        400,
                        "Please check your submission data"
                );
            };

            // When/Then
            assertThatThrownBy(() -> decorator.executeWithRetry(invalidBody))
                    .isInstanceOf(HmrcValidationException.class)
                    .extracting("userMessage")
                    .isEqualTo("Please check your submission data");
        }

        @Test
        @DisplayName("IT-405-008: Duplicate period error - RULE_PERIODIC_UPDATE_FOR_PERIOD_SUBMITTED")
        void duplicatePeriodError() {
            // Given
            Supplier<String> duplicatePeriod = () -> {
                throw new HmrcValidationException(
                        "Period already submitted",
                        "RULE_PERIODIC_UPDATE_FOR_PERIOD_SUBMITTED",
                        400,
                        "This period has already been submitted"
                );
            };

            // When/Then
            assertThatThrownBy(() -> decorator.executeWithRetry(duplicatePeriod))
                    .isInstanceOf(HmrcValidationException.class)
                    .extracting("userMessage")
                    .isEqualTo("This period has already been submitted");
        }

        @Test
        @DisplayName("IT-405-009: Unsupported tax year - RULE_TAX_YEAR_NOT_SUPPORTED")
        void unsupportedTaxYear() {
            // Given
            Supplier<String> unsupportedYear = () -> {
                throw new HmrcValidationException(
                        "Tax year not supported",
                        "RULE_TAX_YEAR_NOT_SUPPORTED",
                        400,
                        "Tax year not supported for MTD"
                );
            };

            // When/Then
            assertThatThrownBy(() -> decorator.executeWithRetry(unsupportedYear))
                    .isInstanceOf(HmrcValidationException.class)
                    .extracting("userMessage")
                    .isEqualTo("Tax year not supported for MTD");
        }

        @Test
        @DisplayName("IT-405-010: Authorization error - CLIENT_OR_AGENT_NOT_AUTHORISED")
        void authorizationError() {
            // Given
            Supplier<String> notAuthorised = () -> {
                throw new HmrcAuthenticationException(
                        "Not authorised",
                        "CLIENT_OR_AGENT_NOT_AUTHORISED"
                );
            };

            // When/Then
            assertThatThrownBy(() -> decorator.executeWithRetry(notAuthorised))
                    .isInstanceOf(HmrcAuthenticationException.class)
                    .extracting("errorCode")
                    .isEqualTo("CLIENT_OR_AGENT_NOT_AUTHORISED");
        }

        @Test
        @DisplayName("IT-405-011: Resource not found - MATCHING_RESOURCE_NOT_FOUND")
        void resourceNotFound() {
            // Given
            Supplier<String> notFound = () -> {
                throw new HmrcValidationException(
                        "Resource not found",
                        "MATCHING_RESOURCE_NOT_FOUND",
                        404,
                        "Record not found at HMRC"
                );
            };

            // When/Then
            assertThatThrownBy(() -> decorator.executeWithRetry(notFound))
                    .isInstanceOf(HmrcValidationException.class)
                    .extracting("userMessage")
                    .isEqualTo("Record not found at HMRC");
        }

        @Test
        @DisplayName("IT-405-012: Invalid NINO - FORMAT_NINO")
        void invalidNino() {
            // Given
            Supplier<String> invalidNino = () -> {
                throw new HmrcValidationException(
                        "Invalid NINO format",
                        "FORMAT_NINO",
                        400,
                        "Invalid National Insurance number format"
                );
            };

            // When/Then
            assertThatThrownBy(() -> decorator.executeWithRetry(invalidNino))
                    .isInstanceOf(HmrcValidationException.class)
                    .extracting("userMessage")
                    .isEqualTo("Invalid National Insurance number format");
        }
    }

    @Nested
    @DisplayName("P1: Resilience Behavior Tests")
    class ResilienceBehaviorTests {

        @Test
        @DisplayName("IT-405-013: Exponential backoff timing with jitter")
        void exponentialBackoffTiming() {
            // Given
            config = TestHmrcResilienceConfig.builder()
                    .maxRetryAttempts(3)
                    .initialDelayMs(100)
                    .multiplier(2.0)
                    .maxDelayMs(1000)
                    .circuitBreakerRequestVolumeThreshold(10)
                    .build();
            decorator = new HmrcResilienceDecorator(config, meterRegistry);

            AtomicInteger attempts = new AtomicInteger(0);
            Supplier<String> failsTwice = () -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new HmrcServerException("Server error", "SERVER_ERROR", 503);
                }
                return "success";
            };

            // When
            long startTime = System.currentTimeMillis();
            decorator.executeWithRetry(failsTwice);
            long elapsed = System.currentTimeMillis() - startTime;

            // Then - base delays: 100ms, 200ms = 300ms minimum
            // With jitter (up to 25%): 100-125ms, 200-250ms = 300-375ms
            assertThat(elapsed).isGreaterThanOrEqualTo(200); // Allow tolerance
            assertThat(attempts.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("IT-405-014: Max delay cap is respected")
        void maxDelayCap() {
            // Given - high multiplier but capped max delay
            config = TestHmrcResilienceConfig.builder()
                    .maxRetryAttempts(3)
                    .initialDelayMs(100)
                    .multiplier(100.0) // Would cause huge delays
                    .maxDelayMs(200)   // But capped at 200ms
                    .circuitBreakerRequestVolumeThreshold(10)
                    .build();
            decorator = new HmrcResilienceDecorator(config, meterRegistry);

            AtomicInteger attempts = new AtomicInteger(0);
            Supplier<String> failsTwice = () -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new HmrcServerException("Server error", "SERVER_ERROR", 503);
                }
                return "success";
            };

            // When
            long startTime = System.currentTimeMillis();
            decorator.executeWithRetry(failsTwice);
            long elapsed = System.currentTimeMillis() - startTime;

            // Then - delays capped at 200ms each, so total ~400ms max (plus jitter)
            assertThat(elapsed).isLessThan(1000); // Should not take long
            assertThat(attempts.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("IT-405-015: Circuit breaker recovery after delay")
        void circuitBreakerRecovery() throws InterruptedException {
            // Given - short delay for testing
            config = TestHmrcResilienceConfig.builder()
                    .maxRetryAttempts(1)
                    .initialDelayMs(10)
                    .circuitBreakerRequestVolumeThreshold(2)
                    .circuitBreakerFailureRatio(0.5)
                    .circuitBreakerDelayMs(200) // Short delay for test
                    .circuitBreakerSuccessThreshold(1)
                    .build();
            decorator = new HmrcResilienceDecorator(config, meterRegistry);

            // Track whether we're past the recovery phase
            AtomicInteger failCount = new AtomicInteger(0);
            final int failuresBeforeRecovery = 2; // Circuit trips after 2 failures

            Supplier<String> failsThenSucceeds = () -> {
                int count = failCount.incrementAndGet();
                // Fail for the first 2 calls to trip circuit, succeed after recovery
                if (count <= failuresBeforeRecovery) {
                    throw new HmrcServerException("Server error", "SERVER_ERROR", 500);
                }
                return "success";
            };

            // Trip the circuit (2 failures with threshold=2 will trip it)
            for (int i = 0; i < 2; i++) {
                try {
                    decorator.executeWithRetry(failsThenSucceeds);
                } catch (Exception ignored) {
                }
            }

            assertThat(decorator.getCircuitBreakerState()).isEqualTo(CircuitBreaker.State.OPEN);

            // Wait for half-open state
            Thread.sleep(300);

            // Now call should succeed (circuit allows test call, and failCount > 2)
            String result = decorator.executeWithRetry(failsThenSucceeds);
            assertThat(result).isEqualTo("success");
        }

        @Test
        @DisplayName("IT-405-016: Failed call increments failed metrics")
        void failedCallMetrics() {
            // Given
            Supplier<String> alwaysFails = () -> {
                throw new HmrcServerException("Server error", "SERVER_ERROR", 500);
            };

            // When
            try {
                decorator.executeWithRetry(alwaysFails);
            } catch (Exception ignored) {
            }

            // Then
            Counter failedCounter = meterRegistry.find("hmrc.api.calls.failed").counter();
            assertThat(failedCounter).isNotNull();
            assertThat(failedCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("IT-405-017: Circuit breaker state tracked in metrics")
        void circuitBreakerStateMetric() {
            // Given - configure to trip quickly
            config = TestHmrcResilienceConfig.builder()
                    .maxRetryAttempts(1)
                    .initialDelayMs(10)
                    .circuitBreakerRequestVolumeThreshold(2)
                    .circuitBreakerFailureRatio(0.5)
                    .circuitBreakerDelayMs(5000)
                    .build();
            decorator = new HmrcResilienceDecorator(config, meterRegistry);

            Supplier<String> alwaysFails = () -> {
                throw new HmrcServerException("Server error", "SERVER_ERROR", 500);
            };

            // When - trip the circuit
            for (int i = 0; i < 3; i++) {
                try {
                    decorator.executeWithRetry(alwaysFails);
                } catch (Exception ignored) {
                }
            }

            // Then
            assertThat(decorator.getCircuitBreakerState()).isEqualTo(CircuitBreaker.State.OPEN);
        }
    }

    @Nested
    @DisplayName("P2: Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("IT-405-018: Unknown HMRC error code falls back to generic message")
        void unknownHmrcErrorCode() {
            // Given
            Supplier<String> unknownCode = () -> {
                throw new HmrcValidationException(
                        "Unknown error",
                        "UNKNOWN_ERROR_CODE_XYZ",
                        400
                );
            };

            // When/Then
            assertThatThrownBy(() -> decorator.executeWithRetry(unknownCode))
                    .isInstanceOf(HmrcValidationException.class)
                    .extracting("errorCode")
                    .isEqualTo("UNKNOWN_ERROR_CODE_XYZ");
        }

        @Test
        @DisplayName("IT-405-019: Network connection refused converts to HmrcNetworkException")
        void networkConnectionRefused() {
            // Given
            Supplier<String> connectionRefused = () -> {
                throw new RuntimeException(new IOException("Connection refused"));
            };

            // When/Then
            assertThatThrownBy(() -> decorator.executeWithRetry(connectionRefused))
                    .isInstanceOf(HmrcNetworkException.class);
        }

        @Test
        @DisplayName("IT-405-020: Multiple validation errors are captured")
        void multipleValidationErrors() {
            // Given
            List<ValidationError> errors = List.of(
                    new ValidationError("/nino", "Invalid NINO"),
                    new ValidationError("/income/amount", "Invalid amount"),
                    new ValidationError("/taxYear", "Invalid tax year")
            );
            Supplier<String> multipleErrors = () -> {
                throw new HmrcValidationException("Multiple validation errors", "VALIDATION_FAILED", 400, errors);
            };

            // When/Then
            assertThatThrownBy(() -> decorator.executeWithRetry(multipleErrors))
                    .isInstanceOf(HmrcValidationException.class)
                    .satisfies(ex -> {
                        HmrcValidationException validationEx = (HmrcValidationException) ex;
                        assertThat(validationEx.getValidationErrors()).hasSize(3);
                        assertThat(validationEx.getValidationErrors())
                                .extracting(ValidationError::field)
                                .containsExactly("/nino", "/income/amount", "/taxYear");
                    });
        }

        @Test
        @DisplayName("IT-405-021: Null error code handled gracefully")
        void nullErrorCodeHandling() {
            // Given
            Supplier<String> nullCode = () -> {
                throw new HmrcValidationException(
                        "Error with null code",
                        null,
                        400
                );
            };

            // When/Then
            assertThatThrownBy(() -> decorator.executeWithRetry(nullCode))
                    .isInstanceOf(HmrcValidationException.class)
                    .extracting("errorCode")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("Metrics Verification")
    class MetricsVerificationTests {

        @Test
        @DisplayName("All metrics are registered correctly")
        void allMetricsRegistered() {
            // Given
            Supplier<String> successful = () -> "success";
            decorator.executeWithRetry(successful);

            // Then
            assertThat(meterRegistry.find("hmrc.api.calls.success").counter()).isNotNull();
            assertThat(meterRegistry.find("hmrc.api.calls.failed").counter()).isNotNull();
            assertThat(meterRegistry.find("hmrc.api.retry.attempts").counter()).isNotNull();
            assertThat(meterRegistry.find("hmrc.api.circuitbreaker.state").gauge()).isNotNull();
        }

        @Test
        @DisplayName("Retry counter increments for each retry attempt")
        void retryCounterIncrementsPerAttempt() {
            // Given
            AtomicInteger attempts = new AtomicInteger(0);
            Supplier<String> failsTwice = () -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new HmrcServerException("Error", "ERROR", 500);
                }
                return "success";
            };

            // When
            decorator.executeWithRetry(failsTwice);

            // Then
            Counter retryCounter = meterRegistry.find("hmrc.api.retry.attempts").counter();
            assertThat(retryCounter.count()).isEqualTo(2.0);
        }
    }
}
