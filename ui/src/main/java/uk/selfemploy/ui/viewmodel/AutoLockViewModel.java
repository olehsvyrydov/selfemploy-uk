package uk.selfemploy.ui.viewmodel;

import java.util.List;

/**
 * Decides when an idle session should be locked. Held here, free of JavaFX types, so the rule that
 * governs whether a person's financial data is left readable on an unattended machine is covered by the
 * ordinary test run rather than by a toolkit test the pipeline skips.
 */
public class AutoLockViewModel {

    /** "Off" as a stored timeout. Auto-lock never fires; manual lock still works. */
    public static final int OFF = 0;

    /** Applied when the user has never chosen a timeout. */
    public static final int DEFAULT_MINUTES = 15;

    /** The timeouts offered in Settings, in minutes. {@link #OFF} first. */
    public static final List<Integer> TIMEOUT_CHOICES = List.of(OFF, 5, 15, 60);

    /**
     * How far the wall clock may run past a tick before the gap is read as the machine having been
     * suspended rather than as time simply passing. Generous enough that a stalled UI thread or a long
     * garbage-collection pause does not trip it.
     */
    private static final long SUSPEND_THRESHOLD_MILLIS = 90_000;

    /** What to do at this moment. */
    public enum Decision {
        /** Lock now. */
        LOCK,
        /** Idle long enough, but something is in the way — re-evaluate on the next tick. */
        POSTPONE,
        /** Nothing to do. */
        STAY_UNLOCKED
    }

    /**
     * Whether the session should lock.
     *
     * <p>A suspend beats the idle timeout and the postpone rule: a closed laptop lid is the case the
     * feature exists for, and the machine may have been elsewhere for days. It does not beat "off",
     * which is the user's explicit choice, or an unprotected app, which has no passphrase to unlock with.
     *
     * @param idleMillis         time since the last user activity
     * @param clockGapMillis     wall-clock time that passed since the previous tick, which exceeds the
     *                           tick interval when the machine was suspended
     * @param modalOpen          whether a modal dialog is open, which may hold unsaved input
     * @param protectionEnabled  whether a passphrase vault exists
     * @param timeoutMinutes     the configured timeout, or {@link #OFF}
     */
    public Decision decide(long idleMillis, long clockGapMillis, boolean modalOpen,
                           boolean protectionEnabled, int timeoutMinutes) {
        if (!protectionEnabled || timeoutMinutes <= OFF) {
            return Decision.STAY_UNLOCKED;
        }
        if (clockGapMillis >= SUSPEND_THRESHOLD_MILLIS) {
            return Decision.LOCK;
        }
        if (idleMillis < timeoutMinutes * 60_000L) {
            return Decision.STAY_UNLOCKED;
        }
        // Idle long enough. A modal may hold input the user has not saved, and a dialog sitting open on
        // screen is the same physical exposure as the main window, so waiting costs nothing real.
        return modalOpen ? Decision.POSTPONE : Decision.LOCK;
    }

    /**
     * Why a lock is happening, given the gap since the previous check. Used to tell the user whether the
     * machine slept or simply sat idle, which are different enough that guessing would be misleading.
     */
    public LockReason reasonFor(long clockGapMillis) {
        return clockGapMillis >= SUSPEND_THRESHOLD_MILLIS ? LockReason.SUSPEND : LockReason.IDLE;
    }

    /** The stored timeout, or the default when the user has never chosen one. */
    public int timeoutOrDefault(Integer stored) {
        return stored == null ? DEFAULT_MINUTES : stored;
    }

    /** The i18n key naming a timeout choice in Settings. */
    public String choiceKey(int minutes) {
        return minutes == OFF ? "settings.security.autoLock.off" : "settings.security.autoLock.minutes";
    }
}
