package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.service.security.BackupEncryption;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What the export actually writes, given the user's choice.
 *
 * <p>The dialog and the crypto are covered separately; this covers the join between them, which is where
 * a mistake would mean records leaving the app in the clear while the user believed otherwise.
 */
@DisplayName("Backup export - turning the choice into the file")
class SettingsBackupContentTest {

    private static final byte[] EXPORT = """
            {"metadata":{"appVersion":"1.0"},"incomes":[{"amount":"2500.00","description":"Client payment"}]}"""
            .getBytes(StandardCharsets.UTF_8);

    private static char[] pw(String s) {
        return s.toCharArray();
    }

    @Test
    @DisplayName("choosing encryption produces an envelope, not the records")
    void encryptingProducesAnEnvelope() {
        byte[] written = SettingsController.backupContent(true, EXPORT, pw("correct horse battery staple"));

        assertThat(BackupEncryption.isEncrypted(written)).isTrue();
        assertThat(new String(written, StandardCharsets.UTF_8))
                .as("the written file must not carry what it is protecting")
                .doesNotContain("Client payment", "2500.00");
    }

    @Test
    @DisplayName("an encrypted backup opens with the passphrase it was written under")
    void encryptedBackupOpensAgain() throws Exception {
        byte[] written = SettingsController.backupContent(true, EXPORT, pw("correct horse battery staple"));

        assertThat(BackupEncryption.decrypt(written, pw("correct horse battery staple"))).isEqualTo(EXPORT);
    }

    @Test
    @DisplayName("declining encryption writes the export unchanged, which is a deliberate choice")
    void plaintextIsWrittenAsIs() {
        byte[] written = SettingsController.backupContent(false, EXPORT, null);

        assertThat(written).isEqualTo(EXPORT);
        assertThat(BackupEncryption.isEncrypted(written)).isFalse();
    }

    @Test
    @DisplayName("encrypting without a passphrase fails loudly rather than writing the records out")
    void encryptingWithoutAPassphraseIsRefused() {
        // The failure that matters: a bug upstream that loses the passphrase must not quietly downgrade
        // an encrypted backup into a plaintext one.
        assertThatThrownBy(() -> SettingsController.backupContent(true, EXPORT, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SettingsController.backupContent(true, EXPORT, new char[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
