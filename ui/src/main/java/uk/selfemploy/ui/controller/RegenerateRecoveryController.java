package uk.selfemploy.ui.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import uk.selfemploy.ui.i18n.Messages;
import uk.selfemploy.ui.service.security.AppLockService;
import uk.selfemploy.ui.util.RecoveryCodeClipboard;
import uk.selfemploy.ui.viewmodel.AppProtectViewModel;
import uk.selfemploy.ui.viewmodel.AppUnlockViewModel;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Replaces the recovery code with a freshly generated one, wrapping the same database key.
 *
 * <p>The replacement is built in memory and only written once the user has seen it and confirmed they
 * saved it. Writing first would retire a working recovery code and leave nothing in its place if the
 * user closed the window without reading the replacement — a loss they would discover only after
 * forgetting their passphrase.
 */
public class RegenerateRecoveryController {

    private static final Logger LOG = Logger.getLogger(RegenerateRecoveryController.class.getName());

    @FXML private VBox verifyPane;
    @FXML private VBox recoveryPane;
    @FXML private PasswordField passphraseField;
    @FXML private Label errorLabel;
    @FXML private Button submitButton;
    @FXML private Hyperlink cancelLink;
    @FXML private Label recoveryCodeLabel;
    @FXML private Label copyStatusLabel;
    @FXML private CheckBox savedCheckbox;
    @FXML private Button continueButton;

    private final AppProtectViewModel commitRules = new AppProtectViewModel();
    private final AppUnlockViewModel failureMessages = new AppUnlockViewModel();

    private AppLockService appLock;
    private AppLockService.PendingRecoveryCode pending;
    private Stage dialogStage;
    private boolean regenerated;

    public void setAppLockService(AppLockService appLock) {
        this.appLock = appLock;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /** Whether a replacement code was actually written, so the caller can confirm it to the user. */
    public boolean wasRegenerated() {
        return regenerated;
    }

    @FXML
    private void handleSubmit() {
        if (appLock == null || passphraseField.getText().isEmpty()) {
            return;
        }
        hideError();
        char[] passphrase = passphraseField.getText().toCharArray();
        setBusy(true);

        Task<AppLockService.PendingRecoveryCode> task = new Task<>() {
            @Override
            protected AppLockService.PendingRecoveryCode call() throws Exception {
                return appLock.prepareRecoveryCode(passphrase);
            }
        };
        task.setOnSucceeded(e -> {
            Arrays.fill(passphrase, '\0');
            passphraseField.clear();
            pending = task.getValue();
            showRecovery(pending.recoveryCode());
        });
        task.setOnFailed(e -> {
            Arrays.fill(passphrase, '\0');
            setBusy(false);
            AppUnlockViewModel.ErrorMessage error = failureMessages.errorFor(task.getException());
            showError("unlock.error.generic".equals(error.key())
                    ? Messages.get("regenerateRecovery.error.generic")
                    : Messages.format(error.key(), error.args()));
            passphraseField.requestFocus();
        });
        Thread thread = new Thread(task, "app-regenerate-recovery");
        thread.setDaemon(true);
        thread.start();
    }

    private void showRecovery(String recoveryCode) {
        recoveryCodeLabel.setText(recoveryCode);
        verifyPane.setVisible(false);
        verifyPane.setManaged(false);
        recoveryPane.setVisible(true);
        recoveryPane.setManaged(true);
    }

    @FXML
    private void handleCopy() {
        RecoveryCodeClipboard.copyWithAutoClear(recoveryCodeLabel.getText(), this::hideCopyStatus);
        copyStatusLabel.setText(
                Messages.format("protect.recovery.copied", RecoveryCodeClipboard.CLEAR_AFTER_SECONDS));
        copyStatusLabel.setVisible(true);
        copyStatusLabel.setManaged(true);
    }

    private void hideCopyStatus() {
        copyStatusLabel.setVisible(false);
        copyStatusLabel.setManaged(false);
    }

    @FXML
    private void handleSavedToggled() {
        continueButton.setDisable(!commitRules.canCommit(pending != null, savedCheckbox.isSelected()));
    }

    @FXML
    private void handleContinue() {
        // Point of no return: the previous recovery code stops working here, not before.
        if (commitRules.canCommit(pending != null, savedCheckbox.isSelected())) {
            try {
                pending.commit();
                regenerated = true;
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to write the replacement recovery code", e);
                showError(Messages.get("regenerateRecovery.error.generic"));
                return;
            }
        }
        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void setBusy(boolean busy) {
        submitButton.setDisable(busy);
        passphraseField.setDisable(busy);
        cancelLink.setDisable(busy);
        submitButton.setText(Messages.get(busy ? "regenerateRecovery.working" : "regenerateRecovery.submit"));
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
