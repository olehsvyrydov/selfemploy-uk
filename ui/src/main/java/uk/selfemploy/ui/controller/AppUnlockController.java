package uk.selfemploy.ui.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import uk.selfemploy.ui.i18n.Messages;
import uk.selfemploy.ui.service.security.AppLockService;
import uk.selfemploy.ui.service.security.DbKey;
import uk.selfemploy.ui.service.security.RateLimitedException;
import uk.selfemploy.ui.service.security.WrongPassphraseException;

import java.util.Arrays;

/**
 * The lock screen shown at startup when the database is passphrase-protected. It unwraps the database
 * key (a deliberately slow Argon2id step, run off the FX thread) and hands it back to the launcher via
 * {@link #getUnlockedKey()}. Closing the window without unlocking leaves the key null, and the launcher
 * exits — the app never opens against an encrypted database it cannot read.
 */
public class AppUnlockController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label errorLabel;
    @FXML private PasswordField secretField;
    @FXML private Button unlockButton;
    @FXML private Hyperlink recoveryLink;

    private AppLockService appLock;
    private Stage dialogStage;
    private DbKey unlockedKey;
    private boolean recoveryMode;

    public void setAppLockService(AppLockService appLock) {
        this.appLock = appLock;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
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
            Throwable ex = task.getException();
            if (ex instanceof RateLimitedException rle) {
                long secs = Math.max(1, (rle.retryAfterMillis() + 999) / 1000);
                showError(Messages.format("unlock.error.rateLimited", secs));
            } else if (ex instanceof WrongPassphraseException) {
                showError(Messages.get("unlock.error.wrong"));
            } else {
                showError(Messages.get("unlock.error.generic"));
            }
            secretField.requestFocus();
        });
        Thread thread = new Thread(task, "app-unlock");
        thread.setDaemon(true);
        thread.start();
    }

    private void setBusy(boolean busy) {
        unlockButton.setDisable(busy);
        secretField.setDisable(busy);
        recoveryLink.setDisable(busy);
        unlockButton.setText(Messages.get(busy ? "unlock.unlocking" : "unlock.button"));
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
