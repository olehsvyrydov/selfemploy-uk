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
import uk.selfemploy.ui.service.security.BackupEncryption;
import uk.selfemploy.ui.service.security.WrongPassphraseException;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Asks for the passphrase that protects an encrypted backup being restored.
 *
 * <p>Unrelated to the app-lock screen despite the resemblance: this opens a file, not the database, and
 * the passphrase is whatever the backup was written under — which may have been on another machine, or
 * before the current vault existed. Deriving the key is deliberately slow, so it runs off the FX thread.
 */
public class BackupUnlockController implements AppLockDialog {

    private static final Logger LOG = Logger.getLogger(BackupUnlockController.class.getName());

    @FXML private PasswordField passphraseField;
    @FXML private Label errorLabel;
    @FXML private Button unlockButton;
    @FXML private Hyperlink cancelLink;

    private Stage dialogStage;
    private byte[] encryptedBackup;
    private byte[] decrypted;

    @Override
    public void setAppLockService(AppLockService appLock) {
        // Not used: a backup's passphrase is the one it was written under, not the one guarding this
        // installation's database. Implemented so the shared dialog plumbing can load this screen.
    }

    @Override
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /** The file to open. Must be set before the dialog is shown. */
    public void setEncryptedBackup(byte[] encryptedBackup) {
        this.encryptedBackup = encryptedBackup;
    }

    /** The decrypted export, or {@code null} if the user closed the screen without unlocking it. */
    public byte[] getDecrypted() {
        return decrypted;
    }

    @FXML
    private void handleUnlock() {
        if (encryptedBackup == null || passphraseField.getText().isEmpty()) {
            return;
        }
        char[] passphrase = passphraseField.getText().toCharArray();
        setBusy(true);
        hideError();

        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                return BackupEncryption.decrypt(encryptedBackup, passphrase);
            }
        };
        task.setOnSucceeded(e -> {
            Arrays.fill(passphrase, '\0');
            decrypted = task.getValue();
            close();
        });
        task.setOnFailed(e -> {
            Arrays.fill(passphrase, '\0');
            setBusy(false);
            Throwable failure = task.getException();
            if (failure instanceof WrongPassphraseException) {
                // A wrong passphrase and a tampered file are indistinguishable here, and the user can do
                // nothing different about either, so both read as "this did not open".
                showError(Messages.get("backupUnlock.error.wrong"));
            } else {
                LOG.log(Level.SEVERE, "Failed to open the encrypted backup", failure);
                showError(Messages.get("backupUnlock.error.generic"));
            }
            passphraseField.requestFocus();
        });
        Thread thread = new Thread(task, "backup-unlock");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void setBusy(boolean busy) {
        unlockButton.setDisable(busy);
        passphraseField.setDisable(busy);
        cancelLink.setDisable(busy);
        unlockButton.setText(Messages.get(busy ? "backupUnlock.working" : "backupUnlock.button"));
    }

    private void close() {
        if (dialogStage != null) {
            dialogStage.close();
        }
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
