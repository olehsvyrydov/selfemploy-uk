package uk.selfemploy.hmrc.oauth.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for EncryptedFileTokenStorage.
 */
@DisplayName("EncryptedFileTokenStorage")
class EncryptedFileTokenStorageTest {

    @TempDir
    Path tempDir;

    private EncryptedFileTokenStorage storage;
    private Path tokenFile;

    @BeforeEach
    void setup() {
        tokenFile = tempDir.resolve("hmrc-tokens.enc");
        storage = new EncryptedFileTokenStorage(tokenFile);
    }

    @AfterEach
    void cleanup() throws Exception {
        if (Files.exists(tokenFile)) {
            Files.delete(tokenFile);
        }
    }

    @Nested
    @DisplayName("Save Tokens")
    class SaveTokens {

        @Test
        @DisplayName("should save tokens to encrypted file")
        void shouldSaveTokensToEncryptedFile() {
            OAuthTokens tokens = createTestTokens();

            storage.save(tokens);

            assertThat(Files.exists(tokenFile)).isTrue();
        }

        @Test
        @DisplayName("should encrypt file contents")
        void shouldEncryptFileContents() throws Exception {
            OAuthTokens tokens = createTestTokens();

            storage.save(tokens);

            // File should exist and not contain plaintext tokens (binary data)
            byte[] fileContent = Files.readAllBytes(tokenFile);
            String fileAsHex = bytesToHex(fileContent);

            // Plaintext tokens should not appear in the file
            assertThat(fileAsHex).doesNotContain(bytesToHex("access_token_12345".getBytes()));
            assertThat(fileAsHex).doesNotContain(bytesToHex("refresh_token_67890".getBytes()));
        }

        private String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }

        @Test
        @DisplayName("should overwrite existing tokens")
        void shouldOverwriteExistingTokens() {
            OAuthTokens oldTokens = createTestTokens();
            OAuthTokens newTokens = OAuthTokens.create(
                "new_access", "new_refresh", 7200, "Bearer", "scope"
            );

            storage.save(oldTokens);
            storage.save(newTokens);

            Optional<OAuthTokens> loaded = storage.load();
            assertThat(loaded).isPresent();
            assertThat(loaded.get().accessToken()).isEqualTo("new_access");
        }
    }

    @Nested
    @DisplayName("Load Tokens")
    class LoadTokens {

        @Test
        @DisplayName("should load saved tokens")
        void shouldLoadSavedTokens() {
            OAuthTokens tokens = createTestTokens();
            storage.save(tokens);

            Optional<OAuthTokens> loaded = storage.load();

            assertThat(loaded).isPresent();
            assertThat(loaded.get().accessToken()).isEqualTo("access_token_12345");
            assertThat(loaded.get().refreshToken()).isEqualTo("refresh_token_67890");
            assertThat(loaded.get().expiresIn()).isEqualTo(14400);
            assertThat(loaded.get().tokenType()).isEqualTo("Bearer");
            assertThat(loaded.get().scope()).isEqualTo("read:self-assessment");
        }

        @Test
        @DisplayName("should return empty when file does not exist")
        void shouldReturnEmptyWhenFileDoesNotExist() {
            Optional<OAuthTokens> loaded = storage.load();

            assertThat(loaded).isEmpty();
        }

        @Test
        @DisplayName("should throw exception for corrupted file")
        void shouldThrowExceptionForCorruptedFile() throws Exception {
            // Write garbage to the file
            Files.writeString(tokenFile, "corrupted data that is not valid");

            assertThatThrownBy(() -> storage.load())
                .isInstanceOf(TokenStorageException.class);
        }
    }

    @Nested
    @DisplayName("Delete Tokens")
    class DeleteTokens {

        @Test
        @DisplayName("should delete token file")
        void shouldDeleteTokenFile() {
            storage.save(createTestTokens());
            assertThat(Files.exists(tokenFile)).isTrue();

            storage.delete();

            assertThat(Files.exists(tokenFile)).isFalse();
        }

        @Test
        @DisplayName("should not throw when file does not exist")
        void shouldNotThrowWhenFileDoesNotExist() {
            // Should not throw
            storage.delete();

            assertThat(Files.exists(tokenFile)).isFalse();
        }
    }

    @Nested
    @DisplayName("Exists")
    class Exists {

        @Test
        @DisplayName("should return true when tokens exist")
        void shouldReturnTrueWhenTokensExist() {
            storage.save(createTestTokens());

            assertThat(storage.exists()).isTrue();
        }

        @Test
        @DisplayName("should return false when tokens do not exist")
        void shouldReturnFalseWhenTokensDoNotExist() {
            assertThat(storage.exists()).isFalse();
        }
    }

    @Nested
    @DisplayName("Storage Type")
    class StorageType {

        @Test
        @DisplayName("should return correct storage type")
        void shouldReturnCorrectStorageType() {
            assertThat(storage.getStorageType()).isEqualTo("Encrypted File");
        }
    }

    @Nested
    @DisplayName("Encryption Key Derivation")
    class EncryptionKeyDerivation {

        @Test
        @DisplayName("should generate consistent key for same machine")
        void shouldGenerateConsistentKeyForSameMachine() {
            // Save and load should work - meaning key derivation is consistent
            OAuthTokens tokens = createTestTokens();
            storage.save(tokens);

            // Create new storage instance with same path
            EncryptedFileTokenStorage newStorage = new EncryptedFileTokenStorage(tokenFile);

            Optional<OAuthTokens> loaded = newStorage.load();
            assertThat(loaded).isPresent();
            assertThat(loaded.get().accessToken()).isEqualTo("access_token_12345");
        }
    }

    private OAuthTokens createTestTokens() {
        return OAuthTokens.create(
            "access_token_12345",
            "refresh_token_67890",
            14400,
            "Bearer",
            "read:self-assessment"
        );
    }
}
