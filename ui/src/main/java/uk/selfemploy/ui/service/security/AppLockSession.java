package uk.selfemploy.ui.service.security;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import uk.selfemploy.ui.viewmodel.AutoLockViewModel;

import java.util.function.LongSupplier;

/**
 * Watches an unlocked session for inactivity and asks to lock when it has gone on long enough.
 *
 * <p>Activity is anything the user does in the window; the timer is checked periodically rather than
 * scheduled to fire exactly at the deadline, because a periodic check is also what detects a suspended
 * machine — a tick that arrives far later than it should means the machine was asleep in between.
 *
 * <p>The decision itself lives in {@link AutoLockViewModel}; this class supplies it with the world.
 */
public final class AppLockSession {

    /** How often idleness is re-checked. Also the baseline for spotting a suspend. */
    private static final Duration TICK = Duration.seconds(15);

    private final AutoLockViewModel viewModel = new AutoLockViewModel();
    private final LongSupplier nowMillis;
    private final Runnable onLockRequested;
    private final Timeline timer;

    private long lastActivityMillis;
    private long lastTickMillis;
    private int timeoutMinutes = AutoLockViewModel.OFF;
    private boolean protectionEnabled;

    /**
     * @param nowMillis       the clock, injectable so tests can drive it
     * @param onLockRequested run on the FX thread when the session should lock
     */
    public AppLockSession(LongSupplier nowMillis, Runnable onLockRequested) {
        this.nowMillis = nowMillis;
        this.onLockRequested = onLockRequested;
        this.lastActivityMillis = nowMillis.getAsLong();
        this.lastTickMillis = this.lastActivityMillis;
        this.timer = new Timeline(new KeyFrame(TICK, event -> tick()));
        this.timer.setCycleCount(Animation.INDEFINITE);
    }

    /**
     * Starts watching {@code scene} for activity. Filters are used rather than handlers so a control that
     * consumes an event still counts as the user being present.
     */
    public void attachTo(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, event -> recordActivity());
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> recordActivity());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> recordActivity());
        scene.addEventFilter(ScrollEvent.SCROLL, event -> recordActivity());
        timer.play();
    }

    /** Applies the configured timeout and whether there is a passphrase to unlock with. */
    public void configure(int timeoutMinutes, boolean protectionEnabled) {
        this.timeoutMinutes = timeoutMinutes;
        this.protectionEnabled = protectionEnabled;
        recordActivity();
    }

    /** Treats now as activity — also used to restart the countdown after an unlock. */
    public void recordActivity() {
        lastActivityMillis = nowMillis.getAsLong();
    }

    /** Stops watching, so a locked session does not ask to lock again while the unlock screen is up. */
    public void pause() {
        timer.pause();
    }

    /** Resumes watching after an unlock, with the countdown restarted. */
    public void resume() {
        recordActivity();
        lastTickMillis = nowMillis.getAsLong();
        timer.play();
    }

    /**
     * One evaluation. Package-private return so tests can drive the decision with a fake clock without
     * running a JavaFX timeline.
     */
    Runnable tickDecision() {
        long now = nowMillis.getAsLong();
        long clockGap = now - lastTickMillis;
        lastTickMillis = now;
        AutoLockViewModel.Decision decision = viewModel.decide(
                now - lastActivityMillis, clockGap, isModalOpen(), protectionEnabled, timeoutMinutes);
        return decision == AutoLockViewModel.Decision.LOCK ? onLockRequested : null;
    }

    private void tick() {
        Runnable lock = tickDecision();
        if (lock != null) {
            lock.run();
        }
    }

    /**
     * Whether a modal dialog is on screen. Such a window may hold input the user has typed but not saved,
     * which an idle timer must not throw away.
     */
    private static boolean isModalOpen() {
        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .filter(Stage.class::isInstance)
                .map(Stage.class::cast)
                .anyMatch(stage -> stage.getModality() != Modality.NONE);
    }
}
