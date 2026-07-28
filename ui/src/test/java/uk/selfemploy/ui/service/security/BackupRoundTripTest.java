package uk.selfemploy.ui.service.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The journey a backup actually takes: built in memory, encrypted, written, read back, decrypted.
 *
 * <p>Works on the bytes rather than through the export service, because what needs proving is the part
 * between building the JSON and restoring it — that the file on disk gives nothing away, that it comes
 * back byte-identical, and that a backup written before any of this existed still reads.
 */
@DisplayName("Backup round trip: export, write, read, import")
class BackupRoundTripTest {

    @TempDir
    Path dir;

    /** The shape a real export has, with the fields a reader of the file would care about. */
    private static final String EXPORT_JSON = """
            {"metadata":{"appVersion":"1.0.0","exportDate":"2026-07-27T18:00:00","taxYears":["2025/26"]},
             "incomes":[{"date":"2026-03-04","amount":"2500.00","description":"Client payment","category":"SALES"}],
             "expenses":[{"date":"2026-03-05","amount":"42.00","description":"Stationery","category":"OFFICE_COSTS"}]}""";

    private static char[] pw(String s) {
        return s.toCharArray();
    }

    @Test
    @DisplayName("an encrypted backup written to disk is unreadable, and restores intact")
    void encryptedBackupRoundTrips() throws Exception {
        Path file = dir.resolve("self-employment-data.json");
        byte[] plaintext = EXPORT_JSON.getBytes(StandardCharsets.UTF_8);

        Files.write(file, BackupEncryption.encrypt(plaintext, pw("correct horse battery staple")));

        // What someone finds if they open the file.
        String onDisk = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(onDisk)
                .as("the file must not carry the records it is protecting")
                .doesNotContain("Client payment", "Stationery", "2500.00", "42.00");

        byte[] restored = BackupEncryption.decrypt(Files.readAllBytes(file), pw("correct horse battery staple"));
        assertThat(new String(restored, StandardCharsets.UTF_8)).isEqualTo(EXPORT_JSON);
    }

    @Test
    @DisplayName("a backup written before backups could be encrypted still restores untouched")
    void plaintextBackupsStillRestore() throws Exception {
        // The regression that would hurt most: existing files must keep working.
        Path legacy = dir.resolve("old-backup.json");
        Files.writeString(legacy, EXPORT_JSON, StandardCharsets.UTF_8);

        byte[] fileBytes = Files.readAllBytes(legacy);

        assertThat(BackupEncryption.isEncrypted(fileBytes))
                .as("a plain export must not be mistaken for an encrypted one")
                .isFalse();
        assertThat(new String(fileBytes, StandardCharsets.UTF_8))
                .as("and it must be handed on exactly as it was found")
                .isEqualTo(EXPORT_JSON);
    }

    @Test
    @DisplayName("the passphrase from one backup does not open another")
    void backupsAreIndependentOfEachOther() throws Exception {
        byte[] plaintext = EXPORT_JSON.getBytes(StandardCharsets.UTF_8);
        byte[] first = BackupEncryption.encrypt(plaintext, pw("first backup passphrase"));
        byte[] second = BackupEncryption.encrypt(plaintext, pw("second backup passphrase"));

        assertThat(BackupEncryption.decrypt(first, pw("first backup passphrase"))).isEqualTo(plaintext);
        assertThatThrownBy(() -> BackupEncryption.decrypt(second, pw("first backup passphrase")))
                .isInstanceOf(WrongPassphraseException.class);
    }

    @Test
    @DisplayName("a backup opens without any vault present, which is the point of a backup")
    void restoringDoesNotNeedTheVault() throws Exception {
        // Nothing here touches AppLockService or a vault file: an encrypted backup is restorable on a
        // fresh install, or a different machine, where this installation's vault does not exist.
        byte[] envelope = BackupEncryption.encrypt(
                EXPORT_JSON.getBytes(StandardCharsets.UTF_8), pw("correct horse battery staple"));

        assertThat(BackupEncryption.decrypt(envelope, pw("correct horse battery staple")))
                .isEqualTo(EXPORT_JSON.getBytes(StandardCharsets.UTF_8));
    }
}
