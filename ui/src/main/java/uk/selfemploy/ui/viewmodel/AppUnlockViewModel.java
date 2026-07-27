package uk.selfemploy.ui.viewmodel;

import uk.selfemploy.ui.service.security.RateLimitedException;
import uk.selfemploy.ui.service.security.WrongPassphraseException;

/**
 * Decision logic behind the startup unlock screen: turning an unlock failure into the message the user
 * sees. Held here rather than in the controller so it can be tested without a JavaFX toolkit.
 */
public class AppUnlockViewModel {

    /** A message to display: the i18n key plus any arguments it formats in. */
    public record ErrorMessage(String key, Object... args) {}

    /**
     * The message for a failed unlock. A wrong secret and a rate-limit are told apart explicitly; every
     * other failure (an unreadable vault, a crypto provider problem) falls back to the generic message
     * rather than being misreported as a wrong passphrase.
     *
     * @param failure the exception the unlock task failed with, may be null
     */
    public ErrorMessage errorFor(Throwable failure) {
        if (failure instanceof RateLimitedException rateLimited) {
            return new ErrorMessage("unlock.error.rateLimited", retryAfterSeconds(rateLimited.retryAfterMillis()));
        }
        if (failure instanceof WrongPassphraseException) {
            return new ErrorMessage("unlock.error.wrong");
        }
        return new ErrorMessage("unlock.error.generic");
    }

    /**
     * The remaining lockout in whole seconds, rounded up so the countdown never tells the user to wait
     * "0 seconds" while the throttle is still in force.
     */
    public long retryAfterSeconds(long retryAfterMillis) {
        return Math.max(1, (retryAfterMillis + 999) / 1000);
    }
}
