package uk.selfemploy.ui.controller;

import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import uk.selfemploy.ui.component.PassphraseField;
import uk.selfemploy.ui.i18n.Messages;
import uk.selfemploy.ui.service.security.AppLockService;
import uk.selfemploy.ui.service.security.DbKey;
import uk.selfemploy.ui.viewmodel.AppUnlockViewModel;

import java.util.Arrays;

/**
 * The lock screen shown at startup when the database is passphrase-protected. It unwraps the database
 * key (a deliberately slow Argon2id step, run off the FX thread) and hands it back to the launcher via
 * {@link #getUnlockedKey()}. Closing the window without unlocking leaves the key null, and the launcher
 * exits — the app never opens against an encrypted database it cannot read.
 */
public class AppUnlockController implements AppLockDialog {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label errorLabel;
    @FXML private PasswordField secretField;
    @FXML private TextField revealedField;
    @FXML private ToggleButton revealButton;
    @FXML private Label capsLockHint;
    @FXML private Button unlockButton;
    @FXML private Hyperlink recoveryLink;
    @FXML private VBox card;

    private final AppUnlockViewModel viewModel = new AppUnlockViewModel();

    @FXML
    private void initialize() {
        PassphraseField.wireReveal(secretField, revealedField, revealButton,
                Messages.get("unlock.reveal"), Messages.get("unlock.hide"));
        PassphraseField.warnAboutCapsLock(secretField, capsLockHint);
        PassphraseField.warnAboutCapsLock(revealedField, capsLockHint);
    }

    private AppLockService appLock;
    private Stage dialogStage;
    private DbKey unlockedKey;
    private boolean recoveryMode;

    @Override
    public void setAppLockService(AppLockService appLock) {
        this.appLock = appLock;
    }

    @Override
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Explains a mid-session lock. Without this the screen appears with startup wording, leaving a user
     * who stepped away unsure whether the app restarted or something went wrong — and telling someone who
     * locked deliberately that they timed out.
     *
     * @param messageKey the subtitle describing why the session locked
     */
    public void setLockReason(String messageKey) {
        subtitleLabel.setText(Messages.get(messageKey));
    }

    /** The unlocked database key, or {@code null} if the user closed the screen without unlocking. */
    public DbKey getUnlockedKey() {
        return unlockedKey;
    }

    @FXML
    private void handleToggleRecovery() {
        recoveryMode = !recoveryMode;
        secretField.clear();
        hideError();
        titleLabel.setText(Messages.get(recoveryMode ? "unlock.recovery.title" : "unlock.title"));
        subtitleLabel.setText(Messages.get(recoveryMode ? "unlock.recovery.subtitle" : "unlock.subtitle"));
        secretField.setPromptText(Messages.get(recoveryMode ? "unlock.recovery.prompt" : "unlock.passphrase.prompt"));
        recoveryLink.setText(Messages.get(recoveryMode ? "unlock.usePassphrase" : "unlock.useRecovery"));
        secretField.requestFocus();
    }

    @FXML
    private void handleUnlock() {
        if (appLock == null || secretField.getText().isEmpty()) {
            return;
        }
        char[] secret = secretField.getText().toCharArray();
        boolean recovery = recoveryMode;
        setBusy(true);
        hideError();

        Task<DbKey> task = new Task<>() {
            @Override
            protected DbKey call() throws Exception {
                return recovery ? appLock.unlockWithRecovery(secret) : appLock.unlock(secret);
            }
        };
        task.setOnSucceeded(e -> {
            Arrays.fill(secret, '\0');
            unlockedKey = task.getValue();
            if (dialogStage != null) {
                dialogStage.close();
            }
        });
        task.setOnFailed(e -> {
            Arrays.fill(secret, '\0');
            setBusy(false);
            AppUnlockViewModel.ErrorMessage error = viewModel.errorFor(task.getException());
            showError(Messages.format(error.key(), error.args()));
            secretField.requestFocus();
        });
        Thread thread = new Thread(task, "app-unlock");
        thread.setDaemon(true);
        thread.start();
    }

    private void setBusy(boolean busy) {
        unlockButton.setDisable(busy);
        secretField.setDisable(busy);   // the revealed field follows it
        revealButton.setDisable(busy);
        recoveryLink.setDisable(busy);
        unlockButton.setText(Messages.get(busy ? "unlock.unlocking" : "unlock.button"));
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        shakeCard();
    }

    /**
     * Nudges the card sideways once. A message that merely appears below a field is easy to miss when you
     * are already looking at the field, and this screen is the one place where not noticing costs a
     * retry against a throttle that grows.
     */
    private void shakeCard() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), card);
        shake.setFromX(0);
        shake.setByX(8);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(event -> card.setTranslateX(0));
        shake.play();
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
