package uk.selfemploy.hmrc.resilience;

/**
 * Test implementation of HmrcResilienceConfig for unit testing.
 * Provides a builder pattern for easy configuration.
 */
public class TestHmrcResilienceConfig implements HmrcResilienceConfig {

    private final TestRetry retry;
    private final long timeoutMs;
    private final TestCircuitBreaker circuitBreaker;

    private TestHmrcResilienceConfig(Builder builder) {
        this.retry = new TestRetry(
                builder.maxRetryAttempts,
                builder.initialDelayMs,
                builder.multiplier,
                builder.maxDelayMs
        );
        this.timeoutMs = builder.timeoutMs;
        this.circuitBreaker = new TestCircuitBreaker(
                builder.circuitBreakerRequestVolumeThreshold,
                builder.circuitBreakerFailureRatio,
                builder.circuitBreakerDelayMs,
                builder.circuitBreakerSuccessThreshold
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Retry retry() {
        return retry;
    }

    @Override
    public long timeoutMs() {
        return timeoutMs;
    }

    @Override
    public CircuitBreaker circuitBreaker() {
        return circuitBreaker;
    }

    public static class Builder {
        private int maxRetryAttempts = 3;
        private long initialDelayMs = 1000;
        private double multiplier = 2.0;
        private long maxDelayMs = 30000;
        private long timeoutMs = 30000;
        private int circuitBreakerRequestVolumeThreshold = 4;
        private double circuitBreakerFailureRatio = 0.5;
        private long circuitBreakerDelayMs = 30000;
        private int circuitBreakerSuccessThreshold = 2;

        public Builder maxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
            return this;
        }

        public Builder initialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
            return this;
        }

        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public Builder maxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
            return this;
        }

        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder circuitBreakerRequestVolumeThreshold(int threshold) {
            this.circuitBreakerRequestVolumeThreshold = threshold;
            return this;
        }

        public Builder circuitBreakerFailureRatio(double ratio) {
            this.circuitBreakerFailureRatio = ratio;
            return this;
        }

        public Builder circuitBreakerDelayMs(long delayMs) {
            this.circuitBreakerDelayMs = delayMs;
            return this;
        }

        public Builder circuitBreakerSuccessThreshold(int threshold) {
            this.circuitBreakerSuccessThreshold = threshold;
            return this;
        }

        public TestHmrcResilienceConfig build() {
            return new TestHmrcResilienceConfig(this);
        }
    }

    private record TestRetry(int maxAttempts, long initialDelayMs, double multiplier, long maxDelayMs)
            implements Retry {
    }

    private record TestCircuitBreaker(int requestVolumeThreshold, double failureRatio, long delayMs,
                                      int successThreshold) implements CircuitBreaker {
    }
}
