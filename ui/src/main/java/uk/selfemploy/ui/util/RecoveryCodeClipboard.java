package uk.selfemploy.ui.util;

import javafx.animation.PauseTransition;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

/**
 * Copies a recovery code to the system clipboard and takes it back out again shortly afterwards.
 *
 * <p>A recovery code unlocks the whole database, so leaving it on the clipboard indefinitely hands it
 * to whatever runs next — including anything the user pastes into by accident. Clearing is best-effort:
 * a clipboard manager that has already captured the value keeps its own copy, which no application can
 * revoke. The copy button exists because password managers are the sane place to put this code.
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
     * Clears the clipboard only if it still holds this code, so a value the user copied in the meantime
     * is not destroyed.
     */
    private static void clearIfStillHolding(String recoveryCode) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (recoveryCode.equals(clipboard.getString())) {
            clipboard.setContent(new ClipboardContent());
        }
    }
}
