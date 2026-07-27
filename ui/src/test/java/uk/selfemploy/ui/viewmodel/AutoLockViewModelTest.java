package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * When an idle session locks. This rule decides whether someone's financial data is left readable on an
 * unattended machine, so it is covered here rather than in a toolkit test the pipeline skips.
 */
@DisplayName("AutoLockViewModel - when an idle session locks")
class AutoLockViewModelTest {

    private static final long MINUTE = 60_000L;
    private static final int TIMEOUT = 15;

    private AutoLockViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new AutoLockViewModel();
    }

    /** Idle for the given minutes, no clock jump, nothing in the way, protection on. */
    private AutoLockViewModel.Decision afterIdleMinutes(long minutes) {
        return viewModel.decide(minutes * MINUTE, 0, false, true, TIMEOUT);
    }

    @Test
    @DisplayName("stays unlocked while the user is within the timeout")
    void staysUnlockedBeforeTheTimeout() {
        assertThat(afterIdleMinutes(0)).isEqualTo(AutoLockViewModel.Decision.STAY_UNLOCKED);
        assertThat(afterIdleMinutes(14)).isEqualTo(AutoLockViewModel.Decision.STAY_UNLOCKED);
    }

    @Test
    @DisplayName("locks once idle reaches the timeout")
    void locksAtTheTimeout() {
        assertThat(afterIdleMinutes(TIMEOUT)).isEqualTo(AutoLockViewModel.Decision.LOCK);
        assertThat(afterIdleMinutes(TIMEOUT + 30)).isEqualTo(AutoLockViewModel.Decision.LOCK);
    }

    @Test
    @DisplayName("Off never locks, however long the session sits idle")
    void offNeverLocks() {
        assertThat(viewModel.decide(24 * 60 * MINUTE, 0, false, true, AutoLockViewModel.OFF))
                .isEqualTo(AutoLockViewModel.Decision.STAY_UNLOCKED);
    }

    @Test
    @DisplayName("an unprotected app never locks: there would be no passphrase to unlock it with")
    void unprotectedNeverLocks() {
        assertThat(viewModel.decide(24 * 60 * MINUTE, 0, false, false, TIMEOUT))
                .isEqualTo(AutoLockViewModel.Decision.STAY_UNLOCKED);
    }

    @Test
    @DisplayName("a modal dialog postpones the lock rather than discarding what is typed in it")
    void modalPostpones() {
        assertThat(viewModel.decide(TIMEOUT * MINUTE, 0, true, true, TIMEOUT))
                .isEqualTo(AutoLockViewModel.Decision.POSTPONE);
    }

    @Test
    @DisplayName("once the modal closes, the next check locks")
    void locksOnceTheModalCloses() {
        assertThat(viewModel.decide(TIMEOUT * MINUTE, 0, true, true, TIMEOUT))
                .isEqualTo(AutoLockViewModel.Decision.POSTPONE);
        assertThat(viewModel.decide(TIMEOUT * MINUTE, 0, false, true, TIMEOUT))
                .isEqualTo(AutoLockViewModel.Decision.LOCK);
    }

    @Test
    @DisplayName("a suspended machine locks on resume even with idle budget left")
    void suspendLocksImmediately() {
        // The lid was closed a minute after the last keystroke and the machine woke two hours later.
        assertThat(viewModel.decide(MINUTE, 2 * 60 * MINUTE, false, true, TIMEOUT))
                .isEqualTo(AutoLockViewModel.Decision.LOCK);
    }

    @Test
    @DisplayName("a suspend beats an open modal: the machine may have been elsewhere entirely")
    void suspendBeatsAnOpenModal() {
        assertThat(viewModel.decide(MINUTE, 2 * 60 * MINUTE, true, true, TIMEOUT))
                .isEqualTo(AutoLockViewModel.Decision.LOCK);
    }

    @Test
    @DisplayName("a suspend does not override Off, which is the user's explicit choice")
    void suspendDoesNotOverrideOff() {
        assertThat(viewModel.decide(MINUTE, 2 * 60 * MINUTE, false, true, AutoLockViewModel.OFF))
                .isEqualTo(AutoLockViewModel.Decision.STAY_UNLOCKED);
    }

    @Test
    @DisplayName("an ordinary gap between checks is not mistaken for a suspend")
    void normalTickGapIsNotASuspend() {
        // Ticks are 15s apart; a slow tick, a long GC pause or a stalled UI thread must not lock.
        assertThat(viewModel.decide(MINUTE, 20_000, false, true, TIMEOUT))
                .isEqualTo(AutoLockViewModel.Decision.STAY_UNLOCKED);
    }

    @Test
    @DisplayName("the default applies until the user chooses a timeout")
    void defaultUntilChosen() {
        assertThat(viewModel.timeoutOrDefault(null)).isEqualTo(AutoLockViewModel.DEFAULT_MINUTES);
        assertThat(viewModel.timeoutOrDefault(AutoLockViewModel.OFF)).isEqualTo(AutoLockViewModel.OFF);
        assertThat(viewModel.timeoutOrDefault(60)).isEqualTo(60);
    }

    @Test
    @DisplayName("the offered choices include Off and the documented intervals")
    void offeredChoices() {
        assertThat(AutoLockViewModel.TIMEOUT_CHOICES)
                .containsExactly(AutoLockViewModel.OFF, 5, 15, 60);
        assertThat(AutoLockViewModel.DEFAULT_MINUTES).isIn(AutoLockViewModel.TIMEOUT_CHOICES);
    }
}
