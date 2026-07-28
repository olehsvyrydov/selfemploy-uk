package uk.selfemploy.ui.component;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two aids on the unlock screen: revealing what was typed, and noticing caps lock.
 *
 * <p>Tagged {@code e2e} because JavaFX controls need a live toolkit, which the default fork does not
 * start. Run with {@code -DexcludedGroups=} to include it.
 */
@Tag("e2e")
@DisplayName("PassphraseField - revealing a passphrase and spotting caps lock")
class PassphraseFieldTest {

    private static boolean fxReady;

    @BeforeAll
    static void initToolkit() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            fxReady = latch.await(15, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyStarted) {
            fxReady = true;
        } catch (Exception noDisplay) {
            fxReady = false;
        }
        if (fxReady) {
            Platform.setImplicitExit(false);
        }
        Assumptions.assumeTrue(fxReady, "Skipping — no JavaFX toolkit available");
    }

    /** Runs on the FX thread and waits, so assertions see the finished state. */
    private static void onFxThread(Runnable work) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                work.run();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });
        assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @Test
    @DisplayName("what the user typed is readable when revealed, and the caller still reads one field")
    void revealShowsTheSameText() throws Exception {
        onFxThread(() -> {
            PasswordField hidden = new PasswordField();
            TextField shown = new TextField();
            ToggleButton toggle = new ToggleButton();
            PassphraseField.wireReveal(hidden, shown, toggle, "Show passphrase", "Hide passphrase");

            hidden.setText("correct horse battery staple");

            assertThat(shown.getText()).isEqualTo("correct horse battery staple");
            assertThat(hidden.isVisible()).isTrue();
            assertThat(shown.isVisible()).isFalse();

            toggle.setSelected(true);
            assertThat(shown.isVisible()).as("revealed").isTrue();
            assertThat(hidden.isVisible()).as("masked field steps aside").isFalse();

            // Typing while revealed must still reach the field callers read.
            shown.setText("typed while visible");
            assertThat(hidden.getText())
                    .as("callers keep reading the password field and never ask which is showing")
                    .isEqualTo("typed while visible");
        });
    }

    @Test
    @DisplayName("the caret stays where it was, so revealing mid-edit does not move the user")
    void revealKeepsTheCaret() throws Exception {
        onFxThread(() -> {
            PasswordField hidden = new PasswordField();
            TextField shown = new TextField();
            ToggleButton toggle = new ToggleButton();
            PassphraseField.wireReveal(hidden, shown, toggle, "Show passphrase", "Hide passphrase");

            hidden.setText("correct horse battery staple");
            hidden.positionCaret(7);

            toggle.setSelected(true);
            assertThat(shown.getCaretPosition())
                    .as("checking a passphrase mid-word must not send the next keystroke to the end")
                    .isEqualTo(7);

            shown.positionCaret(3);
            toggle.setSelected(false);
            assertThat(hidden.getCaretPosition()).isEqualTo(3);
        });
    }

    @Test
    @DisplayName("the toggle announces the action it will perform, not the one it just did")
    void accessibleTextFollowsTheState() throws Exception {
        onFxThread(() -> {
            PasswordField hidden = new PasswordField();
            TextField shown = new TextField();
            ToggleButton toggle = new ToggleButton();
            PassphraseField.wireReveal(hidden, shown, toggle, "Show passphrase", "Hide passphrase");

            assertThat(toggle.getAccessibleText()).isEqualTo("Show passphrase");

            toggle.setSelected(true);
            assertThat(toggle.getAccessibleText())
                    .as("a fixed label would tell a screen-reader user to show what is already shown")
                    .isEqualTo("Hide passphrase");

            toggle.setSelected(false);
            assertThat(toggle.getAccessibleText()).isEqualTo("Show passphrase");
        });
    }

    @Test
    @DisplayName("caps lock is spotted from an upper-case letter arriving without shift")
    void capsLockIsSpotted() throws Exception {
        onFxThread(() -> {
            TextField field = new TextField();
            Label hint = new Label();
            hint.setVisible(false);
            PassphraseField.warnAboutCapsLock(field, hint);

            field.fireEvent(typed("A", false));
            assertThat(hint.isVisible()).as("upper case with no shift means caps lock").isTrue();

            field.fireEvent(typed("a", false));
            assertThat(hint.isVisible()).as("and lower case says it is off again").isFalse();
        });
    }

    @Test
    @DisplayName("a deliberate capital with shift is not mistaken for caps lock")
    void shiftIsNotMistakenForCapsLock() throws Exception {
        onFxThread(() -> {
            TextField field = new TextField();
            Label hint = new Label();
            PassphraseField.warnAboutCapsLock(field, hint);

            field.fireEvent(typed("A", true));
            assertThat(hint.isVisible()).isFalse();

            // Lower case *with* shift is the other way caps lock shows itself.
            field.fireEvent(typed("a", true));
            assertThat(hint.isVisible()).isTrue();
        });
    }

    @Test
    @DisplayName("digits and symbols say nothing either way, since they carry no case")
    void nonLettersAreIgnored() throws Exception {
        onFxThread(() -> {
            TextField field = new TextField();
            Label hint = new Label();
            PassphraseField.warnAboutCapsLock(field, hint);

            field.fireEvent(typed("A", false));
            assertThat(hint.isVisible()).isTrue();

            field.fireEvent(typed("7", false));
            assertThat(hint.isVisible()).as("a digit must not clear a warning it cannot disprove").isTrue();
        });
    }

    private static KeyEvent typed(String character, boolean shiftDown) {
        return new KeyEvent(KeyEvent.KEY_TYPED, character, "", KeyCode.UNDEFINED,
                shiftDown, false, false, false);
    }
}
