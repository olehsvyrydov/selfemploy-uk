package uk.selfemploy.ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CredentialEncryption")
class CredentialEncryptionTest {

    private CredentialEncryption encryption;

    @BeforeEach
    void setUp() {
        encryption = new CredentialEncryption();
    }

    @Nested
    @DisplayName("encrypt and decrypt round-trip")
    class RoundTrip {

        @Test
        @DisplayName("encrypts and decrypts a client ID")
        void encryptsAndDecryptsClientId() {
            String clientId = "abc123def456";

            String encrypted = encryption.encrypt(clientId);
            String decrypted = encryption.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(clientId);
        }

        @Test
        @DisplayName("encrypts and decrypts a client secret")
        void encryptsAndDecryptsClientSecret() {
            String secret = "super-secret-value-with-special-chars!@#$%";

            String encrypted = encryption.encrypt(secret);
            String decrypted = encryption.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(secret);
        }

        @Test
        @DisplayName("encrypted value differs from plaintext")
        void encryptedDiffersFromPlaintext() {
            String plaintext = "my-client-id";

            String encrypted = encryption.encrypt(plaintext);

            assertThat(encrypted).isNotEqualTo(plaintext);
        }

        @Test
        @DisplayName("different encryptions of same value produce different ciphertexts")
        void differentEncryptionsProduceDifferentCiphertexts() {
            String plaintext = "same-value";

            String encrypted1 = encryption.encrypt(plaintext);
            String encrypted2 = encryption.encrypt(plaintext);

            // Due to random IV, encryptions should differ
            assertThat(encrypted1).isNotEqualTo(encrypted2);
        }

        @Test
        @DisplayName("handles empty string")
        void handlesEmptyString() {
            String encrypted = encryption.encrypt("");
            String decrypted = encryption.decrypt(encrypted);

            assertThat(decrypted).isEmpty();
        }

        @Test
        @DisplayName("handles unicode characters")
        void handlesUnicode() {
            String value = "credential-with-unicode-\u00e9\u00e8\u00ea";

            String encrypted = encryption.encrypt(value);
            String decrypted = encryption.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("encrypt throws on null input")
        void encryptThrowsOnNull() {
            assertThatThrownBy(() -> encryption.encrypt(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("decrypt throws on null input")
        void decryptThrowsOnNull() {
            assertThatThrownBy(() -> encryption.decrypt(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("decrypt throws on invalid ciphertext")
        void decryptThrowsOnInvalidCiphertext() {
            assertThatThrownBy(() -> encryption.decrypt("not-a-valid-ciphertext"))
                .isInstanceOf(CredentialEncryptionException.class);
        }

        @Test
        @DisplayName("decrypt throws on tampered ciphertext")
        void decryptThrowsOnTamperedCiphertext() {
            String encrypted = encryption.encrypt("test-value");
            // Tamper with the ciphertext by changing a character
            char[] chars = encrypted.toCharArray();
            chars[chars.length / 2] = (chars[chars.length / 2] == 'A') ? 'B' : 'A';
            String tampered = new String(chars);

            assertThatThrownBy(() -> encryption.decrypt(tampered))
                .isInstanceOf(CredentialEncryptionException.class);
        }
    }
}
