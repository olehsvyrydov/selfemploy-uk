package uk.selfemploy.ui.util;

import javafx.animation.PauseTransition;
import javafx.scene.control.Button;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

/**
 * Copies text to the system clipboard and gives the triggering button brief "Copied!" feedback.
 */
public final class ClipboardUtil {

    private static final Duration FEEDBACK_DURATION = Duration.millis(1500);

    private ClipboardUtil() {
    }

    /**
     * Puts {@code value} on the clipboard and momentarily shows "Copied!" on {@code button},
     * restoring its label and enabled state after a short delay.
     *
     * @param button the button that was clicked
     * @param value  the text to copy
     */
    public static void copyWithFeedback(Button button, String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);

        if (button == null) {
            return;
        }
        String original = button.getText();
        button.setText("Copied!");
        button.setDisable(true);
        PauseTransition pause = new PauseTransition(FEEDBACK_DURATION);
        pause.setOnFinished(e -> {
            button.setText(original);
            button.setDisable(false);
        });
        pause.play();
    }
}
