package uk.selfemploy.ui.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;

import uk.selfemploy.ui.i18n.Messages;
import uk.selfemploy.ui.service.security.AppLockService;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The optional "protect your data" step shown after first-run onboarding. Choosing a passphrase writes
 * the key vault (see {@link AppLockService}); the database itself is encrypted on the next launch, so
 * this never disturbs the open database in the current session. Skipping leaves the app unprotected
 * (plaintext), unchanged. The one-time recovery code is shown once and never stored in the clear.
 */
public class AppProtectController {

    private static final Logger LOG = Logger.getLogger(AppProtectController.class.getName());
    private static final int MIN_PASSPHRASE_LENGTH = 8;

    @FXML private javafx.scene.layout.VBox setupPane;
    @FXML private javafx.scene.layout.VBox recoveryPane;
    @FXML private PasswordField passphraseField;
    @FXML private PasswordField confirmField;
    @FXML private Label errorLabel;
    @FXML private Button enableButton;
    @FXML private Label recoveryCodeLabel;
    @FXML private CheckBox savedCheckbox;
    @FXML private Button continueButton;

    private AppLockService appLock;
    private AppLockService.PendingProtection pending;
    private Stage dialogStage;

    public void setAppLockService(AppLockService appLock) {
        this.appLock = appLock;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    private void handleEnable() {
        if (appLock == null) {
            return;
        }
        String p1 = passphraseField.getText();
        String p2 = confirmField.getText();
        if (p1.length() < MIN_PASSPHRASE_LENGTH) {
            showError(Messages.format("protect.error.tooShort", MIN_PASSPHRASE_LENGTH));
            return;
        }
        if (!p1.equals(p2)) {
            showError(Messages.get("protect.error.mismatch"));
            return;
        }
        hideError();
        char[] passphrase = p1.toCharArray();
        enableButton.setDisable(true);
        enableButton.setText(Messages.get("protect.enabling"));

        // Prepare the vault in memory only; nothing is written to disk until the user acknowledges the
        // recovery code (handleContinue -> commit). Abandoning here therefore leaves the app unprotected.
        Task<AppLockService.PendingProtection> task = new Task<>() {
            @Override
            protected AppLockService.PendingProtection call() {
                return appLock.prepareProtection(passphrase);
            }
        };
        task.setOnSucceeded(e -> {
            Arrays.fill(passphrase, '\0');
            passphraseField.clear();
            confirmField.clear();
            pending = task.getValue();
            showRecovery(pending.recoveryCode());
        });
        task.setOnFailed(e -> {
            Arrays.fill(passphrase, '\0');
            enableButton.setDisable(false);
            enableButton.setText(Messages.get("protect.enable"));
            LOG.log(Level.SEVERE, "Failed to prepare data protection", task.getException());
            showError(Messages.get("protect.error.generic"));
        });
        Thread thread = new Thread(task, "app-protect-enable");
        thread.setDaemon(true);
        thread.start();
    }

    private void showRecovery(String recoveryCode) {
        recoveryCodeLabel.setText(recoveryCode);
        setupPane.setVisible(false);
        setupPane.setManaged(false);
        recoveryPane.setVisible(true);
        recoveryPane.setManaged(true);
    }

    @FXML
    private void handleCopy() {
        ClipboardContent content = new ClipboardContent();
        content.putString(recoveryCodeLabel.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }

    @FXML
    private void handleSavedToggled() {
        continueButton.setDisable(!savedCheckbox.isSelected());
    }

    @FXML
    private void handleContinue() {
        // Point of no return: write the vault now that the recovery code has been shown and acknowledged.
        if (pending != null) {
            try {
                pending.commit();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to write the protection vault", e);
                showError(Messages.get("protect.error.generic"));
                return;
            }
        }
        close();
    }

    @FXML
    private void handleSkip() {
        close();
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
