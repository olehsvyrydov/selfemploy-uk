package uk.selfemploy.hmrc.resilience;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration for HMRC API resilience (retry, circuit breaker, timeout).
 *
 * Configuration properties:
 * - hmrc.retry.max-attempts: Maximum retry attempts (default: 3)
 * - hmrc.retry.initial-delay-ms: Initial delay in ms (default: 1000)
 * - hmrc.retry.multiplier: Backoff multiplier (default: 2.0)
 * - hmrc.retry.max-delay-ms: Maximum delay in ms (default: 30000)
 * - hmrc.timeout-ms: Request timeout in ms (default: 30000)
 * - hmrc.circuit-breaker.request-volume-threshold: Min requests (default: 4)
 * - hmrc.circuit-breaker.failure-ratio: Failure ratio to trip (default: 0.5)
 * - hmrc.circuit-breaker.delay-ms: Delay before half-open in ms (default: 30000)
 * - hmrc.circuit-breaker.success-threshold: Successes to close (default: 2)
 */
@ConfigMapping(prefix = "hmrc")
public interface HmrcResilienceConfig {

    /**
     * Retry configuration.
     */
    Retry retry();

    /**
     * Request timeout in milliseconds.
     */
    @WithName("timeout-ms")
    @WithDefault("30000")
    long timeoutMs();

    /**
     * Circuit breaker configuration.
     */
    @WithName("circuit-breaker")
    CircuitBreaker circuitBreaker();

    interface Retry {
        /**
         * Maximum number of retry attempts.
         */
        @WithName("max-attempts")
        @WithDefault("3")
        int maxAttempts();

        /**
         * Initial delay before first retry in milliseconds.
         */
        @WithName("initial-delay-ms")
        @WithDefault("1000")
        long initialDelayMs();

        /**
         * Multiplier for exponential backoff.
         */
        @WithDefault("2.0")
        double multiplier();

        /**
         * Maximum delay between retries in milliseconds.
         */
        @WithName("max-delay-ms")
        @WithDefault("30000")
        long maxDelayMs();
    }

    interface CircuitBreaker {
        /**
         * Minimum number of calls before circuit breaker can trip.
         */
        @WithName("request-volume-threshold")
        @WithDefault("4")
        int requestVolumeThreshold();

        /**
         * Failure ratio to trip the circuit breaker (0.0 to 1.0).
         */
        @WithName("failure-ratio")
        @WithDefault("0.5")
        double failureRatio();

        /**
         * Delay in milliseconds before transitioning to half-open.
         */
        @WithName("delay-ms")
        @WithDefault("30000")
        long delayMs();

        /**
         * Number of successful calls to close the circuit.
         */
        @WithName("success-threshold")
        @WithDefault("2")
        int successThreshold();
    }
}
