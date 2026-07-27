package uk.selfemploy.ui.service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.viewmodel.AutoLockViewModel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The session's wiring around the lock decision: which clock readings it feeds in, and that activity
 * genuinely restarts the countdown.
 *
 * <p>Driven through {@code tickDecision()} with a fake clock and an injected modal check, so no JavaFX
 * toolkit is needed and these run in the ordinary test pass rather than the excluded {@code e2e} tag.
 */
@DisplayName("AppLockSession - idle tracking")
class AppLockSessionTest {

    private static final long MINUTE = 60_000L;

    private AtomicLong clock;
    private AtomicBoolean modalOpen;
    private AtomicBoolean locked;
    private AppLockSession session;

    @BeforeEach
    void setUp() {
        clock = new AtomicLong(0);
        modalOpen = new AtomicBoolean(false);
        locked = new AtomicBoolean(false);
        session = new AppLockSession(clock::get, modalOpen::get, () -> locked.set(true));
        session.configure(15, true);
    }

    /** The real tick interval. Advancing in these steps is what distinguishes idling from a suspend. */
    private static final long TICK_MILLIS = 15_000;

    /**
     * Lets {@code millis} pass the way it does in a running app: a tick every 15 seconds. Jumping the
     * clock in one step would instead look like a suspended machine, which is a different case entirely
     * (see {@link #suspendLocks()}).
     */
    private boolean idleFor(long millis) {
        for (long elapsed = 0; elapsed < millis; elapsed += TICK_MILLIS) {
            clock.addAndGet(TICK_MILLIS);
            Runnable lock = session.tickDecision();
            if (lock != null) {
                lock.run();
            }
        }
        return locked.get();
    }

    /** One tick after a jump in the wall clock, which is what waking from suspend looks like. */
    private boolean tickAfterClockJump(long millis) {
        clock.addAndGet(millis);
        Runnable lock = session.tickDecision();
        if (lock != null) {
            lock.run();
        }
        return locked.get();
    }

    @Test
    @DisplayName("stays unlocked while ticks keep arriving inside the timeout")
    void staysUnlockedWithinTheTimeout() {
        assertThat(idleFor(14 * MINUTE)).isFalse();
    }

    @Test
    @DisplayName("locks once the idle time passes the timeout")
    void locksAfterTheTimeout() {
        assertThat(idleFor(14 * MINUTE)).isFalse();
        assertThat(idleFor(MINUTE)).isTrue();
    }

    @Test
    @DisplayName("activity restarts the countdown")
    void activityRestartsTheCountdown() {
        assertThat(idleFor(14 * MINUTE)).isFalse();

        session.recordActivity();       // the user came back

        assertThat(idleFor(14 * MINUTE)).as("14 more minutes from the new baseline").isFalse();
        assertThat(idleFor(MINUTE)).isTrue();
    }

    @Test
    @DisplayName("a modal holds the lock off, and it fires once the modal closes")
    void modalHoldsTheLockOff() {
        modalOpen.set(true);
        assertThat(idleFor(20 * MINUTE)).isFalse();

        modalOpen.set(false);
        assertThat(idleFor(MINUTE)).isTrue();
    }

    @Test
    @DisplayName("a long gap between ticks is read as a suspend and locks, even when barely idle")
    void suspendLocks() {
        // Barely idle, then the machine slept for two hours between one tick and the next.
        assertThat(idleFor(MINUTE)).isFalse();
        assertThat(tickAfterClockJump(2 * 60 * MINUTE)).isTrue();
    }

    @Test
    @DisplayName("Off never locks, however long the gaps")
    void offNeverLocks() {
        session.configure(AutoLockViewModel.OFF, true);
        assertThat(idleFor(60 * MINUTE)).isFalse();
        assertThat(tickAfterClockJump(8 * 60 * MINUTE)).isFalse();
    }

    @Test
    @DisplayName("an unprotected app never locks")
    void unprotectedNeverLocks() {
        session.configure(15, false);
        assertThat(idleFor(60 * MINUTE)).isFalse();
        assertThat(tickAfterClockJump(8 * 60 * MINUTE)).isFalse();
    }

    @Test
    @DisplayName("configure restarts the countdown, so shortening the timeout does not lock instantly")
    void configureRestartsTheCountdown() {
        assertThat(idleFor(14 * MINUTE)).isFalse();

        session.configure(5, true);     // the user picked a shorter timeout in Settings

        assertThat(idleFor(4 * MINUTE)).as("measured from the change, not from before it").isFalse();
        assertThat(idleFor(2 * MINUTE)).isTrue();
    }
}
