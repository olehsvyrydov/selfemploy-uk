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
import uk.selfemploy.ui.viewmodel.AppProtectViewModel;
import uk.selfemploy.ui.viewmodel.AppUnlockViewModel;

import java.util.Arrays;

/**
 * Changes the passphrase that unlocks the database key. Only the key vault's passphrase slot is
 * re-wrapped — the database itself is untouched, so this is quick regardless of how much data there is.
 * The current passphrase is verified through the same rate-limited unlock path as the startup screen,
 * so this dialog cannot be used as an unthrottled oracle for guessing it.
 */
public class ChangePassphraseController {

    @FXML private PasswordField currentField;
    @FXML private PasswordField newField;
    @FXML private PasswordField confirmField;
    @FXML private Label passphraseHintLabel;
    @FXML private Label errorLabel;
    @FXML private Button submitButton;
    @FXML private Hyperlink cancelLink;

    private final AppProtectViewModel passphraseRules = new AppProtectViewModel();
    private final AppUnlockViewModel failureMessages = new AppUnlockViewModel();

    private AppLockService appLock;
    private Stage dialogStage;
    private boolean changed;

    @FXML
    private void initialize() {
        passphraseHintLabel.setText(
                Messages.format("protect.passphrase.hint", AppProtectViewModel.MIN_PASSPHRASE_LENGTH));
    }

    public void setAppLockService(AppLockService appLock) {
        this.appLock = appLock;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /** Whether the passphrase was actually changed, so the caller can confirm it to the user. */
    public boolean wasChanged() {
        return changed;
    }

    @FXML
    private void handleSubmit() {
        if (appLock == null || currentField.getText().isEmpty()) {
            return;
        }
        AppProtectViewModel.Validation validation =
                passphraseRules.validate(newField.getText(), confirmField.getText());
        if (validation != AppProtectViewModel.Validation.OK) {
            showError(Messages.format(validation.messageKey(), validation.messageArgs()));
            return;
        }
        hideError();

        // JavaFX hands the text over as a String, which cannot be wiped; the char[] copies below are
        // zeroed on every path, but the String remains until it is collected.
        char[] current = currentField.getText().toCharArray();
        char[] next = newField.getText().toCharArray();
        setBusy(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                appLock.changePassphrase(current, next);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            wipe(current, next);
            changed = true;
            close();
        });
        task.setOnFailed(e -> {
            wipe(current, next);
            setBusy(false);
            AppUnlockViewModel.ErrorMessage error = failureMessages.errorFor(task.getException());
            showError("unlock.error.generic".equals(error.key())
                    ? Messages.get("changePassphrase.error.generic")
                    : Messages.format(error.key(), error.args()));
            currentField.requestFocus();
        });
        Thread thread = new Thread(task, "app-change-passphrase");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private static void wipe(char[]... secrets) {
        for (char[] secret : secrets) {
            Arrays.fill(secret, '\0');
        }
    }

    private void setBusy(boolean busy) {
        submitButton.setDisable(busy);
        currentField.setDisable(busy);
        newField.setDisable(busy);
        confirmField.setDisable(busy);
        cancelLink.setDisable(busy);
        submitButton.setText(Messages.get(busy ? "changePassphrase.working" : "changePassphrase.submit"));
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
