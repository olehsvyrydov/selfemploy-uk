package uk.selfemploy.ui.service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The envelope that keeps a backup from being a plaintext copy of everything the database encrypts.
 */
@DisplayName("BackupEncryption - protecting an exported backup")
class BackupEncryptionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Stands in for an export: the shape the real one has, including the fields that matter. */
    private static final String EXPORT_JSON = """
            {"metadata":{"appVersion":"1.0","exportDate":"2026-07-27T12:00:00"},
             "incomes":[{"amount":"2500.00","description":"Client payment"}],
             "expenses":[{"amount":"42.00","description":"Stationery"}]}""";

    private static char[] pw(String s) {
        return s.toCharArray();
    }

    @Test
    @DisplayName("a backup round-trips through the passphrase that protected it")
    void roundTrip() throws Exception {
        byte[] plaintext = EXPORT_JSON.getBytes(StandardCharsets.UTF_8);

        byte[] envelope = BackupEncryption.encrypt(plaintext, pw("correct horse battery staple"));
        byte[] recovered = BackupEncryption.decrypt(envelope, pw("correct horse battery staple"));

        assertThat(recovered).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("the encrypted file does not contain the data it protects")
    void ciphertextDoesNotLeakContent() {
        byte[] envelope = BackupEncryption.encrypt(
                EXPORT_JSON.getBytes(StandardCharsets.UTF_8), pw("correct horse battery staple"));
        String onDisk = new String(envelope, StandardCharsets.UTF_8);

        // The whole point: someone reading the file learns nothing about the records inside it.
        assertThat(onDisk).doesNotContain("Client payment", "Stationery", "2500.00", "incomes", "expenses");
        assertThat(onDisk).contains(BackupEncryption.TYPE);
    }

    @Test
    @DisplayName("a wrong passphrase is refused rather than returning rubbish")
    void wrongPassphraseIsRefused() {
        byte[] envelope = BackupEncryption.encrypt(
                EXPORT_JSON.getBytes(StandardCharsets.UTF_8), pw("correct horse battery staple"));

        assertThatThrownBy(() -> BackupEncryption.decrypt(envelope, pw("not the passphrase")))
                .isInstanceOf(WrongPassphraseException.class);
    }

    @Test
    @DisplayName("altering the ciphertext fails the tag")
    void tamperedCiphertextIsRefused() throws Exception {
        byte[] envelope = BackupEncryption.encrypt(
                EXPORT_JSON.getBytes(StandardCharsets.UTF_8), pw("correct horse battery staple"));

        Map<String, Object> parsed = MAPPER.readValue(envelope, Map.class);
        byte[] ciphertext = Base64.getDecoder().decode((String) parsed.get("ciphertextB64"));
        ciphertext[ciphertext.length / 2] ^= 0x01;
        parsed.put("ciphertextB64", Base64.getEncoder().encodeToString(ciphertext));

        byte[] tampered = MAPPER.writeValueAsBytes(parsed);
        assertThatThrownBy(() -> BackupEncryption.decrypt(tampered, pw("correct horse battery staple")))
                .isInstanceOf(WrongPassphraseException.class);
    }

    @Test
    @DisplayName("weakening the stated KDF parameters fails the tag, rather than being obeyed")
    void tamperedKdfHeaderIsRefused() throws Exception {
        byte[] envelope = BackupEncryption.encrypt(
                EXPORT_JSON.getBytes(StandardCharsets.UTF_8), pw("correct horse battery staple"));

        // An attacker rewriting the header to claim a cheap derivation is exactly what binding the
        // parameters as AAD is meant to stop.
        Map<String, Object> parsed = MAPPER.readValue(envelope, Map.class);
        Map<String, Object> kdf = (Map<String, Object>) parsed.get("kdf");
        kdf.put("iterations", 1);
        kdf.put("memoryKib", 8);

        byte[] tampered = MAPPER.writeValueAsBytes(parsed);
        assertThatThrownBy(() -> BackupEncryption.decrypt(tampered, pw("correct horse battery staple")))
                .isInstanceOf(WrongPassphraseException.class);
    }

    @Test
    @DisplayName("each backup gets its own salt and nonce, so the same input never encrypts alike")
    void everyBackupIsDistinct() {
        byte[] plaintext = EXPORT_JSON.getBytes(StandardCharsets.UTF_8);

        byte[] first = BackupEncryption.encrypt(plaintext, pw("correct horse battery staple"));
        byte[] second = BackupEncryption.encrypt(plaintext, pw("correct horse battery staple"));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("an encrypted backup is recognised, a plain export and other files are not")
    void detectsItsOwnFiles() {
        byte[] encrypted = BackupEncryption.encrypt(
                EXPORT_JSON.getBytes(StandardCharsets.UTF_8), pw("correct horse battery staple"));

        assertThat(BackupEncryption.isEncrypted(encrypted)).isTrue();
        assertThat(BackupEncryption.isEncrypted(EXPORT_JSON.getBytes(StandardCharsets.UTF_8)))
                .as("a plain export must still import the way it always did")
                .isFalse();
        assertThat(BackupEncryption.isEncrypted("not json at all".getBytes(StandardCharsets.UTF_8))).isFalse();
        assertThat(BackupEncryption.isEncrypted(new byte[0])).isFalse();
    }

    @Test
    @DisplayName("a backup declaring a cipher we do not implement is refused, not decrypted anyway")
    void unsupportedCipherIsRefused() throws Exception {
        byte[] envelope = BackupEncryption.encrypt(
                EXPORT_JSON.getBytes(StandardCharsets.UTF_8), pw("correct horse battery staple"));

        // Decryption is AES-GCM whatever the header says, so an unchecked cipher field would become a
        // downgrade vector the day a second cipher exists. It must be checked now, not then.
        Map<String, Object> parsed = MAPPER.readValue(envelope, Map.class);
        parsed.put("cipher", "AES-128-CBC");

        byte[] relabelled = MAPPER.writeValueAsBytes(parsed);
        assertThatThrownBy(() -> BackupEncryption.decrypt(relabelled, pw("correct horse battery staple")))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("unsupported cipher");
    }

    @Test
    @DisplayName("a backup from a newer version says so, rather than blaming the passphrase")
    void newerVersionIsReportedPlainly() throws Exception {
        byte[] envelope = BackupEncryption.encrypt(
                EXPORT_JSON.getBytes(StandardCharsets.UTF_8), pw("correct horse battery staple"));

        Map<String, Object> parsed = MAPPER.readValue(envelope, Map.class);
        parsed.put("version", BackupEncryption.VERSION + 1);

        byte[] fromTheFuture = MAPPER.writeValueAsBytes(parsed);
        assertThatThrownBy(() -> BackupEncryption.decrypt(fromTheFuture, pw("correct horse battery staple")))
                .as("sending someone to check their passphrase would point them at the wrong problem")
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("newer version");
    }

    @Test
    @DisplayName("a corrupted header fails as a damaged file, not as a raw crash")
    void corruptedKdfHeaderFailsCleanly() throws Exception {
        byte[] envelope = BackupEncryption.encrypt(
                EXPORT_JSON.getBytes(StandardCharsets.UTF_8), pw("correct horse battery staple"));

        // An unreadable salt makes Base64 decoding throw during key derivation. That has to come out as
        // "this did not open" like every other damaged-file case, not as an unhandled runtime failure.
        Map<String, Object> parsed = MAPPER.readValue(envelope, Map.class);
        Map<String, Object> kdf = (Map<String, Object>) parsed.get("kdf");
        kdf.put("saltB64", "!!! not base64 !!!");

        byte[] corrupted = MAPPER.writeValueAsBytes(parsed);
        assertThatThrownBy(() -> BackupEncryption.decrypt(corrupted, pw("correct horse battery staple")))
                .isInstanceOf(WrongPassphraseException.class);
    }

    @Test
    @DisplayName("a file with the right shape but not our marker is refused")
    void aFileWithoutOurMarkerIsRefused() throws Exception {
        byte[] envelope = BackupEncryption.encrypt(
                EXPORT_JSON.getBytes(StandardCharsets.UTF_8), pw("correct horse battery staple"));

        Map<String, Object> parsed = MAPPER.readValue(envelope, Map.class);
        parsed.put("type", "something.else");

        byte[] notOurs = MAPPER.writeValueAsBytes(parsed);
        assertThatThrownBy(() -> BackupEncryption.decrypt(notOurs, pw("correct horse battery staple")))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("not a readable encrypted backup");
    }

    @Test
    @DisplayName("a file claiming to be a backup but missing its parts is reported as unreadable")
    void malformedEnvelopeIsReported() {
        byte[] claimsToBeOurs = ("{\"type\":\"" + BackupEncryption.TYPE + "\",\"version\":1}")
                .getBytes(StandardCharsets.UTF_8);

        assertThat(BackupEncryption.isEncrypted(claimsToBeOurs)).isTrue();
        assertThatThrownBy(() -> BackupEncryption.decrypt(claimsToBeOurs, pw("correct horse battery staple")))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("not a readable encrypted backup");
    }
}
