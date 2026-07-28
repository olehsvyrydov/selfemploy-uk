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
import uk.selfemploy.ui.service.SqliteDataStore;
import uk.selfemploy.ui.service.security.AppLockService;
import uk.selfemploy.ui.service.security.RateLimitedException;
import uk.selfemploy.ui.service.security.WrongPassphraseException;
import uk.selfemploy.ui.viewmodel.AppProtectViewModel;
import uk.selfemploy.ui.viewmodel.BackupExportViewModel;
import uk.selfemploy.ui.viewmodel.SecuritySettingsViewModel;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Asks how a backup should be protected before it is written.
 *
 * <p>A backup holds the same records as the database, so when the database is encrypted this defaults to
 * encrypting the file too. Choosing not to is allowed and warned about rather than prevented — a readable
 * file is what a user hands their accountant.
 *
 * <p>When app-lock is on, the passphrase already typed at every startup can be reused, which is checked
 * against the vault rather than retyped. The file itself never depends on the vault: it can be restored
 * on a machine that has no vault at all.
 */
public class BackupPassphraseController implements AppLockDialog {

    private static final Logger LOG = Logger.getLogger(BackupPassphraseController.class.getName());

    @FXML private Label subtitleLabel;
    @FXML private CheckBox encryptCheckbox;
    @FXML private VBox passphrasePane;
    @FXML private CheckBox useAppPassphraseCheckbox;
    @FXML private Label useAppPassphraseHint;
    @FXML private PasswordField passphraseField;
    @FXML private PasswordField confirmField;
    @FXML private Label hintLabel;
    @FXML private Label errorLabel;
    @FXML private VBox plaintextWarningPane;
    @FXML private Button exportButton;
    @FXML private Hyperlink cancelLink;

    private final BackupExportViewModel viewModel = new BackupExportViewModel();
    private final SecuritySettingsViewModel securityViewModel = new SecuritySettingsViewModel();

    private AppLockService appLock;
    private Stage dialogStage;
    private boolean confirmed;
    private char[] passphrase;

    @FXML
    private void initialize() {
        hintLabel.setText(Messages.format("backup.passphrase.hint", AppProtectViewModel.MIN_PASSPHRASE_LENGTH));
    }

    @Override
    public void setAppLockService(AppLockService appLock) {
        this.appLock = appLock;
        boolean protectionEnabled = appLock.isProtectionEnabled();
        SecuritySettingsViewModel.ProtectionStatus status = securityViewModel.status(
                protectionEnabled, !SqliteDataStore.isKeyProvisioned());

        encryptCheckbox.setSelected(viewModel.encryptByDefault(protectionEnabled));
        useAppPassphraseCheckbox.setVisible(protectionEnabled);
        useAppPassphraseCheckbox.setManaged(protectionEnabled);
        useAppPassphraseHint.setVisible(protectionEnabled);
        useAppPassphraseHint.setManaged(protectionEnabled);
        useAppPassphraseCheckbox.setSelected(protectionEnabled);
        subtitleLabel.setText(Messages.get(switch (status) {
            case ON -> "backup.subtitle.protected";
            case PENDING_RESTART -> "backup.subtitle.pending";
            case OFF -> "backup.subtitle.unprotected";
        }));
        applyMode();
    }

    @Override
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /** Whether the user chose to go ahead, as opposed to closing the dialog. */
    public boolean isConfirmed() {
        return confirmed;
    }

    /** Whether the backup should be encrypted. */
    public boolean isEncrypting() {
        return encryptCheckbox.isSelected();
    }

    /**
     * The passphrase to encrypt with, or null for a plaintext backup. The caller owns it from here and
     * should zero it once the file is written.
     */
    public char[] getPassphrase() {
        return passphrase;
    }

    @FXML
    private void handleEncryptToggled() {
        hideError();
        applyMode();
    }

    @FXML
    private void handleUseAppPassphraseToggled() {
        hideError();
        applyMode();
    }

    /** Shows only the fields the current choices call for. */
    private void applyMode() {
        boolean encrypting = encryptCheckbox.isSelected();
        passphrasePane.setVisible(encrypting);
        passphrasePane.setManaged(encrypting);
        plaintextWarningPane.setVisible(!encrypting);
        plaintextWarningPane.setManaged(!encrypting);

        // Reusing the app passphrase needs no confirmation: the vault is what verifies it.
        boolean reusing = useAppPassphraseCheckbox.isVisible() && useAppPassphraseCheckbox.isSelected();
        confirmField.setVisible(!reusing);
        confirmField.setManaged(!reusing);
        passphraseField.setPromptText(Messages.get(reusing
                ? "backup.appPassphrase.prompt" : "backup.passphrase.prompt"));
    }

    @FXML
    private void handleExport() {
        hideError();
        if (!encryptCheckbox.isSelected()) {
            passphrase = null;
            confirmed = true;
            close();
            return;
        }

        boolean reusing = useAppPassphraseCheckbox.isVisible() && useAppPassphraseCheckbox.isSelected();
        BackupExportViewModel.Validation validation =
                viewModel.validate(passphraseField.getText(), confirmField.getText(), reusing);
        if (!viewModel.canExport(true, validation)) {
            showError(Messages.format(validation.messageKey(), validation.messageArgs()));
            return;
        }

        char[] chosen = passphraseField.getText().toCharArray();
        if (!reusing) {
            passphrase = chosen;
            confirmed = true;
            close();
            return;
        }

        // Checking it against the vault runs Argon2id, which is deliberately slow — on the FX thread
        // that is a frozen window for the duration, which reads as the app having hung.
        setBusy(true);
        Task<String> verification = new Task<>() {
            @Override
            protected String call() {
                return whyNotTheAppPassphrase(chosen);
            }
        };
        verification.setOnSucceeded(e -> {
            String problem = verification.getValue();
            if (problem != null) {
                Arrays.fill(chosen, '\0');
                setBusy(false);
                showError(problem);
                passphraseField.requestFocus();
                return;
            }
            passphrase = chosen;
            confirmed = true;
            close();
        });
        verification.setOnFailed(e -> {
            Arrays.fill(chosen, '\0');
            setBusy(false);
            LOG.log(Level.WARNING, "Could not verify the app passphrase for a backup",
                    verification.getException());
            showError(Messages.get("backup.error.generic"));
        });
        Thread thread = new Thread(verification, "backup-verify-passphrase");
        thread.setDaemon(true);
        thread.start();
    }

    private void setBusy(boolean busy) {
        exportButton.setDisable(busy);
        encryptCheckbox.setDisable(busy);
        useAppPassphraseCheckbox.setDisable(busy);
        passphraseField.setDisable(busy);
        confirmField.setDisable(busy);
        cancelLink.setDisable(busy);
        exportButton.setText(Messages.get(busy ? "backup.checking" : "backup.export"));
    }

    /**
     * Confirms a passphrase really is the app one before a backup is written under it. Getting this wrong
     * would produce a file the user believes their app passphrase opens, and it does not.
     *
     * <p>Verification goes through the same rate-limited unlock as the startup screen, so this dialog
     * cannot be used to guess the app passphrase without penalty. A throttled attempt is reported as
     * such: telling someone their passphrase is wrong when the app simply refused to check it would send
     * them looking for the wrong problem.
     *
     * @return the message to show, or null if the passphrase is the app one
     */
    private String whyNotTheAppPassphrase(char[] candidate) {
        try {
            appLock.unlock(candidate).destroy();
            return null;
        } catch (WrongPassphraseException e) {
            return Messages.get("backup.error.notAppPassphrase");
        } catch (RateLimitedException e) {
            long seconds = Math.max(1, (e.retryAfterMillis() + 999) / 1000);
            return Messages.format("unlock.error.rateLimited", seconds);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not verify the app passphrase for a backup", e);
            return Messages.get("backup.error.generic");
        }
    }

    @FXML
    private void handleCancel() {
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
