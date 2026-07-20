package uk.selfemploy.ui.service.security;

/** Thrown when too many failed unlock attempts have temporarily locked out further tries. */
public class RateLimitedException extends Exception {

    private final long retryAfterMillis;

    public RateLimitedException(long retryAfterMillis) {
        super("Too many attempts. Try again shortly.");
        this.retryAfterMillis = retryAfterMillis;
    }

    /** Milliseconds the caller must wait before another attempt is accepted. */
    public long retryAfterMillis() {
        return retryAfterMillis;
    }
}
