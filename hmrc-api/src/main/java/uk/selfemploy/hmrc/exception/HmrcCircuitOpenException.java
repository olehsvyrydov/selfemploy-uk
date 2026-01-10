package uk.selfemploy.hmrc.exception;

import java.time.Duration;

/**
 * Exception thrown when the circuit breaker is open.
 * This indicates HMRC services have been experiencing repeated failures
 * and requests should not be attempted for a period of time.
 */
public class HmrcCircuitOpenException extends HmrcApiException {

    private final Duration retryAfter;

    public HmrcCircuitOpenException(Duration retryAfter) {
        super("Circuit breaker is open", "CIRCUIT_OPEN", 0, formatUserMessage(retryAfter));
        this.retryAfter = retryAfter;
    }

    public HmrcCircuitOpenException(Duration retryAfter, Throwable cause) {
        super("Circuit breaker is open", "CIRCUIT_OPEN", 0, formatUserMessage(retryAfter), cause);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }

    @Override
    public boolean isRetryable() {
        // Circuit open exceptions should NOT be automatically retried
        // The circuit breaker manages when to allow requests through
        return false;
    }

    private static String formatUserMessage(Duration retryAfter) {
        long seconds = retryAfter.getSeconds();
        if (seconds >= 60) {
            long minutes = seconds / 60;
            return String.format("HMRC services are experiencing issues. Please try again in %d minute%s.",
                    minutes, minutes == 1 ? "" : "s");
        }
        return String.format("HMRC services are experiencing issues. Please try again in %d seconds.", seconds);
    }
}
