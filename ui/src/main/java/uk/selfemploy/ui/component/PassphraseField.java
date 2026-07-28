package uk.selfemploy.ui.component;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyEvent;

/**
 * Wires a passphrase field that can be revealed, and that warns about caps lock.
 *
 * <p>Both exist for the same reason: a passphrase you cannot see is a passphrase you cannot check, and
 * the commonest cause of "my passphrase is wrong" is a lock key nobody looked at. On a screen with no
 * account recovery behind it, a user who cannot get in has lost their data — so it is worth removing the
 * two reasons they might be typing the right thing and not know it.
 *
 * <p>The hidden and revealed fields share their text bidirectionally, so callers keep reading the
 * {@link PasswordField} and never need to know which one the user can currently see.
 */
public final class PassphraseField {

    private PassphraseField() {
    }

    /**
     * Binds a revealed {@link TextField} to a {@link PasswordField} and drives both from {@code toggle}.
     *
     * <p>The toggle's accessible text is driven from its state. A fixed "Show passphrase" would keep
     * announcing "Show" once the passphrase was already shown, describing the wrong action to the one
     * user who cannot see which state the control is in.
     *
     * @param hidden      the masked field, which stays the one callers read
     * @param shown       the revealed field, hidden until the toggle is selected
     * @param toggle      the control that swaps them
     * @param revealLabel accessible text while the passphrase is masked
     * @param hideLabel   accessible text while it is visible
     */
    public static void wireReveal(PasswordField hidden, TextField shown, ToggleButton toggle,
                                  String revealLabel, String hideLabel) {
        shown.textProperty().bindBidirectional(hidden.textProperty());
        shown.promptTextProperty().bind(hidden.promptTextProperty());
        shown.disableProperty().bind(hidden.disableProperty());

        shown.visibleProperty().bind(toggle.selectedProperty());
        shown.managedProperty().bind(toggle.selectedProperty());
        hidden.visibleProperty().bind(toggle.selectedProperty().not());
        hidden.managedProperty().bind(toggle.selectedProperty().not());

        toggle.setAccessibleText(revealLabel);
        // Carry the caret across rather than dropping the user at either end of what they typed. The two
        // fields share their text, so the offset means the same thing in both.
        toggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            TextField from = wasSelected ? shown : hidden;
            TextField target = isSelected ? shown : hidden;
            int caret = from.getCaretPosition();
            toggle.setAccessibleText(isSelected ? hideLabel : revealLabel);
            target.requestFocus();
            target.positionCaret(caret);
        });
    }

    /**
     * Shows {@code hint} while caps lock appears to be on.
     *
     * <p>JavaFX does not expose the lock state, so this reads it from what arrives: a letter that comes
     * through upper-case without shift — or lower-case with it — means caps lock. That only reacts once
     * the user has typed, which is exactly when it is useful.
     */
    public static void warnAboutCapsLock(TextField field, Label hint) {
        field.addEventHandler(KeyEvent.KEY_TYPED, event -> {
            String typed = event.getCharacter();
            if (typed.length() != 1 || !Character.isLetter(typed.charAt(0))) {
                return;
            }
            boolean capsLockOn = Character.isUpperCase(typed.charAt(0)) != event.isShiftDown();
            hint.setVisible(capsLockOn);
            hint.setManaged(capsLockOn);
        });
    }
}
