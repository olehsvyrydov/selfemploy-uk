package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the move from a key derived from public machine metadata to a random master key, and the
 * upgrade path that must not discard credentials already stored under the old key.
 */
@DisplayName("CredentialEncryption with a master key")
class CredentialEncryptionMasterKeyTest {

    @TempDir
    Path tempDir;

    private CredentialEncryption encryptionBackedBy(Path keyDir) {
        return new CredentialEncryption(new MasterKeyProvider(keyDir.resolve("master.key")));
    }

    @Test
    @DisplayName("round-trips a secret under the master key")
    void roundTripsUnderMasterKey() {
        CredentialEncryption encryption = encryptionBackedBy(tempDir);

        String encrypted = encryption.encrypt("client-secret-value");

        assertThat(encrypted).doesNotContain("client-secret-value");
        assertThat(encryption.decrypt(encrypted)).isEqualTo("client-secret-value");
    }

    @Test
    @DisplayName("produces different ciphertext for the same plaintext")
    void producesDistinctCiphertextPerEncryption() {
        CredentialEncryption encryption = encryptionBackedBy(tempDir);

        assertThat(encryption.encrypt("same")).isNotEqualTo(encryption.encrypt("same"));
    }

    @Test
    @DisplayName("a value encrypted under one master key is unreadable under another")
    void isNotReadableWithADifferentMasterKey() {
        String encrypted = encryptionBackedBy(tempDir.resolve("a")).encrypt("client-secret-value");

        CredentialEncryption other = encryptionBackedBy(tempDir.resolve("b"));

        assertThat(other.isLegacy(encrypted)).isFalse();
        org.assertj.core.api.Assertions
            .assertThatThrownBy(() -> other.decrypt(encrypted))
            .isInstanceOf(CredentialEncryptionException.class);
    }

    @Test
    @DisplayName("still decrypts a credential stored under the superseded machine-derived key")
    void decryptsLegacyMachineDerivedValue() throws Exception {
        String legacy = encryptWithSupersededScheme("legacy-client-secret");
        CredentialEncryption encryption = encryptionBackedBy(tempDir);

        assertThat(encryption.isLegacy(legacy)).isTrue();
        assertThat(encryption.decrypt(legacy)).isEqualTo("legacy-client-secret");
    }

    /**
     * Reproduces the layout written before the master key existed: AES-GCM under a PBKDF2 key
     * derived from username, OS, home directory and hostname, with a constant salt, and no
     * version marker.
     */
    private String encryptWithSupersededScheme(String plaintext) throws Exception {
        StringBuilder password = new StringBuilder();
        password.append(System.getProperty("user.name", ""));
        password.append(System.getProperty("os.name", ""));
        password.append(System.getProperty("user.home", ""));
        try {
            password.append(java.net.InetAddress.getLocalHost().getHostName());
        } catch (Exception unavailable) {
            // Matches the original, which also tolerated an unavailable hostname.
        }

        byte[] salt = MessageDigest.getInstance("SHA-256")
            .digest("uk.selfemploy.hmrc.credentials".getBytes(StandardCharsets.UTF_8));
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(password.toString().toCharArray(), salt, 65536, 256);
        SecretKeySpec key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);
        return Base64.getEncoder().encodeToString(buffer.array());
    }
}
