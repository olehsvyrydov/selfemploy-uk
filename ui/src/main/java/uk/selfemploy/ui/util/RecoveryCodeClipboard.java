package uk.selfemploy.ui.util;

import javafx.animation.PauseTransition;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

/**
 * Copies a recovery code to the system clipboard and takes it back out again shortly afterwards.
 *
 * <p>A recovery code unlocks the whole database, so leaving it on the clipboard indefinitely hands it
 * to whatever runs next — including anything the user pastes into by accident. The copy button exists
 * because password managers are the sane place to put this code.
 *
 * <p>Clearing is best-effort and has two real limits. A clipboard manager that already captured the
 * value keeps its own copy, which no application can revoke. And the timer only runs while the app
 * does: on Windows and macOS the clipboard outlives the process, so a user who copies and quits within
 * the window would keep the code on the clipboard — which is why callers also clear on close rather
 * than relying on the timer alone.
 */
public final class RecoveryCodeClipboard {

    /** How long the code stays on the clipboard. Long enough to paste, short enough to forget. */
    public static final int CLEAR_AFTER_SECONDS = 30;

    private RecoveryCodeClipboard() {
    }

    /**
     * Puts {@code recoveryCode} on the system clipboard and schedules its removal.
     *
     * @param recoveryCode the code to copy
     * @param onCleared    run on the FX thread once the clipboard has been cleared, may be null
     */
    public static void copyWithAutoClear(String recoveryCode, Runnable onCleared) {
        ClipboardContent content = new ClipboardContent();
        content.putString(recoveryCode);
        Clipboard.getSystemClipboard().setContent(content);

        PauseTransition delay = new PauseTransition(Duration.seconds(CLEAR_AFTER_SECONDS));
        delay.setOnFinished(event -> {
            clearIfStillHolding(recoveryCode);
            if (onCleared != null) {
                onCleared.run();
            }
        });
        delay.play();
    }

    /**
     * Clears the clipboard now, if it still holds this code. Callers use this when the screen showing
     * the code closes, so the code does not outlive the window that displayed it.
     *
     * <p>Only clears an exact match, so a value the user copied in the meantime is not destroyed.
     *
     * @param recoveryCode the code that was copied, may be null
     */
    public static void clearIfStillHolding(String recoveryCode) {
        if (recoveryCode == null) {
            return;
        }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (recoveryCode.equals(clipboard.getString())) {
            clipboard.setContent(new ClipboardContent());
        }
    }
}
