package uk.selfemploy.hmrc.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.exception.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Decorates HMRC API calls with retry and circuit breaker capabilities.
 *
 * Retry behavior:
 * - Retries on HmrcServerException (5xx)
 * - Retries on HmrcNetworkException (timeout/network)
 * - Does NOT retry on HmrcValidationException (4xx)
 * - Does NOT retry on HmrcAuthenticationException
 * - Uses exponential backoff with jitter
 *
 * Circuit breaker:
 * - Trips after configured failure threshold
 * - Returns HmrcCircuitOpenException when open
 *
 * Metrics (Micrometer):
 * - hmrc.api.retry.attempts: Counter for retry attempts
 * - hmrc.api.calls.success: Counter for successful calls
 * - hmrc.api.calls.failed: Counter for failed calls
 * - hmrc.api.circuitbreaker.state: Gauge for circuit breaker state
 */
@ApplicationScoped
public class HmrcResilienceDecorator {

    private static final Logger log = LoggerFactory.getLogger(HmrcResilienceDecorator.class);
    private static final String CIRCUIT_BREAKER_NAME = "hmrc-api";
    private static final String RETRY_NAME = "hmrc-api";

    /** Jitter factor: adds up to 25% randomness to delay. */
    private static final double JITTER_FACTOR = 0.25;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final HmrcResilienceConfig config;

    // Metrics
    private final Counter retryAttemptsCounter;
    private final Counter successCounter;
    private final Counter failedCounter;
    private final AtomicInteger circuitBreakerStateGauge;

    @Inject
    public HmrcResilienceDecorator(HmrcResilienceConfig config, MeterRegistry meterRegistry) {
        this.config = config;
        this.circuitBreaker = createCircuitBreaker(config);
        this.retry = createRetry(config);

        // Initialize metrics
        this.retryAttemptsCounter = Counter.builder("hmrc.api.retry.attempts")
                .description("Number of retry attempts for HMRC API calls")
                .register(meterRegistry);

        this.successCounter = Counter.builder("hmrc.api.calls.success")
                .description("Number of successful HMRC API calls")
                .register(meterRegistry);

        this.failedCounter = Counter.builder("hmrc.api.calls.failed")
                .description("Number of failed HMRC API calls")
                .register(meterRegistry);

        this.circuitBreakerStateGauge = new AtomicInteger(circuitBreaker.getState().ordinal());
        Gauge.builder("hmrc.api.circuitbreaker.state", circuitBreakerStateGauge, AtomicInteger::get)
                .description("Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN, 3=DISABLED, 4=FORCED_OPEN)")
                .register(meterRegistry);
    }

    /**
     * Constructor for testing without CDI.
     * Creates a decorator with a simple no-op meter registry.
     */
    public HmrcResilienceDecorator(HmrcResilienceConfig config) {
        this.config = config;
        this.circuitBreaker = createCircuitBreaker(config);
        this.retry = createRetry(config);

        // No-op metrics for testing
        this.retryAttemptsCounter = new NoOpCounter();
        this.successCounter = new NoOpCounter();
        this.failedCounter = new NoOpCounter();
        this.circuitBreakerStateGauge = new AtomicInteger(0);
    }

    /**
     * Executes a supplier with retry and circuit breaker protection.
     *
     * @param supplier the operation to execute
     * @param <T> the return type
     * @return the result of the operation
     * @throws HmrcApiException if the operation fails after retries
     * @throws HmrcCircuitOpenException if the circuit breaker is open
     */
    public <T> T executeWithRetry(Supplier<T> supplier) {
        try {
            // Wrap with circuit breaker first, then retry
            Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker,
                    Retry.decorateSupplier(retry, () -> executeWithExceptionConversion(supplier)));
            T result = decoratedSupplier.get();
            successCounter.increment();
            return result;
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker is open, rejecting request");
            failedCounter.increment();
            throw new HmrcCircuitOpenException(Duration.ofMillis(config.circuitBreaker().delayMs()), e);
        } catch (HmrcApiException e) {
            failedCounter.increment();
            throw e;
        } catch (Exception e) {
            failedCounter.increment();
            throw convertException(e);
        }
    }

    /**
     * Executes a callable with retry and circuit breaker protection.
     *
     * @param callable the operation to execute
     * @param <T> the return type
     * @return the result of the operation
     * @throws HmrcApiException if the operation fails after retries
     */
    public <T> T executeCallableWithRetry(Callable<T> callable) {
        Supplier<T> supplier = () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        return executeWithRetry(supplier);
    }

    private <T> T executeWithExceptionConversion(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (HmrcApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw convertException(e);
        }
    }

    private HmrcApiException convertException(Throwable e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;

        // Already an HMRC exception
        if (cause instanceof HmrcApiException hmrcEx) {
            return hmrcEx;
        }

        // Socket timeout -> Network exception
        if (cause instanceof SocketTimeoutException) {
            return HmrcNetworkException.timeout(cause);
        }

        // IO exception -> Network exception
        if (cause instanceof IOException) {
            return HmrcNetworkException.connectionFailed(cause);
        }

        // Generic exception
        return new HmrcApiException("Unexpected error: " + cause.getMessage(), cause);
    }

    private CircuitBreaker createCircuitBreaker(HmrcResilienceConfig config) {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold((float) (config.circuitBreaker().failureRatio() * 100))
                .minimumNumberOfCalls(config.circuitBreaker().requestVolumeThreshold())
                .waitDurationInOpenState(Duration.ofMillis(config.circuitBreaker().delayMs()))
                .permittedNumberOfCallsInHalfOpenState(config.circuitBreaker().successThreshold())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(config.circuitBreaker().requestVolumeThreshold())
                .recordExceptions(
                        HmrcServerException.class,
                        HmrcNetworkException.class,
                        IOException.class,
                        SocketTimeoutException.class
                )
                .ignoreExceptions(
                        HmrcValidationException.class,
                        HmrcAuthenticationException.class,
                        HmrcCircuitOpenException.class
                )
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(cbConfig);
        CircuitBreaker cb = registry.circuitBreaker(CIRCUIT_BREAKER_NAME);

        // Add event listeners for logging and metrics
        cb.getEventPublisher()
                .onStateTransition(event -> {
                    log.info("Circuit breaker state transition: {} -> {}",
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState());
                    circuitBreakerStateGauge.set(event.getStateTransition().getToState().ordinal());
                })
                .onCallNotPermitted(event -> log.warn("Circuit breaker rejected call"));

        return cb;
    }

    private Retry createRetry(HmrcResilienceConfig config) {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(config.retry().maxAttempts())
                .intervalFunction(attempt -> {
                    // Exponential backoff with cap
                    long baseDelay = (long) (config.retry().initialDelayMs() *
                            Math.pow(config.retry().multiplier(), attempt - 1));
                    long cappedDelay = Math.min(baseDelay, config.retry().maxDelayMs());

                    // Add jitter (up to 25% of the delay)
                    long jitter = ThreadLocalRandom.current().nextLong((long) (cappedDelay * JITTER_FACTOR));
                    return cappedDelay + jitter;
                })
                .retryOnException(this::isRetryable)
                .build();

        RetryRegistry registry = RetryRegistry.of(retryConfig);
        Retry r = registry.retry(RETRY_NAME);

        // Add event listeners for logging and metrics
        r.getEventPublisher()
                .onRetry(event -> {
                    log.info("Retry attempt {} for HMRC call, waiting {}ms",
                            event.getNumberOfRetryAttempts(),
                            event.getWaitInterval().toMillis());
                    retryAttemptsCounter.increment();
                })
                .onError(event -> log.error("HMRC call failed after {} attempts: {}",
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable().getMessage()));

        return r;
    }

    private boolean isRetryable(Throwable throwable) {
        // Check if it's an HMRC exception with retryable flag
        if (throwable instanceof HmrcApiException hmrcEx) {
            return hmrcEx.isRetryable();
        }

        // Check wrapped exceptions
        Throwable cause = throwable.getCause();
        if (cause instanceof HmrcApiException hmrcEx) {
            return hmrcEx.isRetryable();
        }

        // Network errors are retryable
        if (throwable instanceof IOException || cause instanceof IOException) {
            return true;
        }
        if (throwable instanceof SocketTimeoutException || cause instanceof SocketTimeoutException) {
            return true;
        }

        return false;
    }

    /**
     * Resets the circuit breaker state. Useful for testing.
     */
    public void resetCircuitBreaker() {
        circuitBreaker.reset();
    }

    /**
     * Gets the current state of the circuit breaker.
     */
    public CircuitBreaker.State getCircuitBreakerState() {
        return circuitBreaker.getState();
    }

    /**
     * No-op counter for testing without a full MeterRegistry.
     */
    private static class NoOpCounter implements Counter {
        private double count = 0;

        @Override
        public void increment(double amount) {
            count += amount;
        }

        @Override
        public double count() {
            return count;
        }

        @Override
        public Id getId() {
            return null;
        }
    }
}
