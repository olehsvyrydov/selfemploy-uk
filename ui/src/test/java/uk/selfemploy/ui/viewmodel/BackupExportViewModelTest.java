package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Whether a backup leaves the app in the clear, and what protects it when it does not.
 */
@DisplayName("BackupExportViewModel - protecting an exported backup")
class BackupExportViewModelTest {

    private BackupExportViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new BackupExportViewModel();
    }

    @Test
    @DisplayName("an encrypted database gets an encrypted backup by default")
    void encryptsByDefaultWhenProtectionIsOn() {
        assertThat(viewModel.encryptByDefault(true))
                .as("a plaintext backup would hand away exactly what the database encryption protects")
                .isTrue();
    }

    @Test
    @DisplayName("an unprotected database does not, since the backup would outdo the original")
    void doesNotEncryptByDefaultWhenProtectionIsOff() {
        assertThat(viewModel.encryptByDefault(false)).isFalse();
    }

    @Test
    @DisplayName("a matching passphrase of at least the app's minimum is accepted")
    void acceptsAValidPassphrase() {
        String passphrase = "correct horse battery staple";
        assertThat(viewModel.validate(passphrase, passphrase, false))
                .isEqualTo(BackupExportViewModel.Validation.OK);
    }

    @Test
    @DisplayName("a backup passphrase is held to the same length as the app passphrase")
    void enforcesTheSameMinimumAsTheApp() {
        String tooShort = "a".repeat(AppProtectViewModel.MIN_PASSPHRASE_LENGTH - 1);
        String exact = "a".repeat(AppProtectViewModel.MIN_PASSPHRASE_LENGTH);

        assertThat(viewModel.validate(tooShort, tooShort, false))
                .isEqualTo(BackupExportViewModel.Validation.TOO_SHORT);
        assertThat(viewModel.validate(exact, exact, false))
                .isEqualTo(BackupExportViewModel.Validation.OK);
    }

    @Test
    @DisplayName("a mistyped confirmation is rejected")
    void rejectsAMismatch() {
        assertThat(viewModel.validate("correct horse battery", "correct horse battary", false))
                .isEqualTo(BackupExportViewModel.Validation.MISMATCH);
    }

    @Test
    @DisplayName("reusing the app passphrase needs no confirmation, since the vault verifies it")
    void reusingTheAppPassphraseSkipsConfirmation() {
        assertThat(viewModel.validate("correct horse battery", "", true))
                .isEqualTo(BackupExportViewModel.Validation.OK);
    }

    @Test
    @DisplayName("an unencrypted backup is always allowed: a readable file is a real need")
    void plaintextExportIsAlwaysAllowed() {
        assertThat(viewModel.canExport(false, BackupExportViewModel.Validation.TOO_SHORT)).isTrue();
        assertThat(viewModel.canExport(false, BackupExportViewModel.Validation.OK)).isTrue();
    }

    @Test
    @DisplayName("an encrypted backup may not proceed on a passphrase that was rejected")
    void encryptedExportRequiresAValidPassphrase() {
        assertThat(viewModel.canExport(true, BackupExportViewModel.Validation.OK)).isTrue();
        assertThat(viewModel.canExport(true, BackupExportViewModel.Validation.TOO_SHORT)).isFalse();
        assertThat(viewModel.canExport(true, BackupExportViewModel.Validation.MISMATCH)).isFalse();
    }

    @Test
    @DisplayName("each rejection carries its message; only the length one formats an argument")
    void rejectionsCarryTheirMessages() {
        assertThat(BackupExportViewModel.Validation.TOO_SHORT.messageKey()).isEqualTo("backup.error.tooShort");
        assertThat(BackupExportViewModel.Validation.TOO_SHORT.messageArgs())
                .containsExactly(AppProtectViewModel.MIN_PASSPHRASE_LENGTH);
        assertThat(BackupExportViewModel.Validation.MISMATCH.messageArgs()).isEmpty();
        assertThat(BackupExportViewModel.Validation.OK.messageKey()).isNull();
    }
}
