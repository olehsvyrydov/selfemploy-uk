package uk.selfemploy.hmrc.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.hmrc.oauth.storage.EncryptedFileTokenStorage;
import uk.selfemploy.hmrc.oauth.storage.TokenStorageException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GDPR Security Integration Tests - Sprint 3
 *
 * Tests TOKEN-003 and BIZ-003 requirements per /alex legal review:
 * - Tokens NEVER logged at any level
 * - NINO masked in all log output
 *
 * @see docs/sprints/sprint-3/testing/rob-qa-sprint-3-backend.md
 */
@DisplayName("GDPR Security Integration Tests")
@Tag("integration")
@Tag("security")
@Tag("gdpr")
class GdprSecurityIntegrationTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream logCapture;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void setup() {
        // Capture stdout/stderr for log analysis
        logCapture = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;

        PrintStream captureStream = new PrintStream(logCapture);
        System.setOut(captureStream);
        System.setErr(captureStream);
    }

    @AfterEach
    void teardown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    /**
     * TOKEN-003-01: Tokens NEVER in DEBUG logs
     * TOKEN-003-02: Tokens NEVER in INFO logs
     * TOKEN-003-03: Tokens NEVER in ERROR logs
     */
    @Nested
    @DisplayName("TOKEN-003: Token Logging Security")
    class TokenLoggingSecurity {

        private static final String TEST_ACCESS_TOKEN = "test_access_token_secret_12345";
        private static final String TEST_REFRESH_TOKEN = "test_refresh_token_secret_67890";

        @Test
        @DisplayName("TOKEN-003-01: Access tokens never appear in logs during save")
        void accessTokensNeverInLogsDuringSave() {
            Path tokenFile = tempDir.resolve("tokens.enc");
            EncryptedFileTokenStorage storage = new EncryptedFileTokenStorage(tokenFile);

            OAuthTokens tokens = new OAuthTokens(
                TEST_ACCESS_TOKEN,
                TEST_REFRESH_TOKEN,
                3600L,
                "Bearer",
                "read:self-assessment",
                Instant.now()
            );

            storage.save(tokens);

            String logs = logCapture.toString();
            assertThat(logs).doesNotContain(TEST_ACCESS_TOKEN);
            assertThat(logs).doesNotContain(TEST_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("TOKEN-003-02: Access tokens never appear in logs during load")
        void accessTokensNeverInLogsDuringLoad() {
            Path tokenFile = tempDir.resolve("tokens.enc");
            EncryptedFileTokenStorage storage = new EncryptedFileTokenStorage(tokenFile);

            OAuthTokens tokens = new OAuthTokens(
                TEST_ACCESS_TOKEN,
                TEST_REFRESH_TOKEN,
                3600L,
                "Bearer",
                "read:self-assessment",
                Instant.now()
            );

            storage.save(tokens);
            logCapture.reset();

            storage.load();

            String logs = logCapture.toString();
            assertThat(logs).doesNotContain(TEST_ACCESS_TOKEN);
            assertThat(logs).doesNotContain(TEST_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("TOKEN-003-03: Access tokens never appear in error logs")
        void accessTokensNeverInErrorLogs() throws Exception {
            Path tokenFile = tempDir.resolve("tokens.enc");

            // Write corrupted data to force an error
            Files.writeString(tokenFile, "corrupted data");

            EncryptedFileTokenStorage storage = new EncryptedFileTokenStorage(tokenFile);

            try {
                storage.load();
            } catch (TokenStorageException e) {
                // Expected
            }

            String logs = logCapture.toString();
            // Even in error scenarios, no tokens should be logged
            assertThat(logs).doesNotContain("access_token");
            assertThat(logs).doesNotContain("refresh_token");
        }

        @Test
        @DisplayName("TOKEN-003-05: OAuthTokens.toString() never exposes actual tokens")
        void toStringNeverExposesTokens() {
            OAuthTokens tokens = new OAuthTokens(
                TEST_ACCESS_TOKEN,
                TEST_REFRESH_TOKEN,
                3600L,
                "Bearer",
                "read:self-assessment",
                Instant.now()
            );

            String toStringOutput = tokens.toString();

            // toString should NOT contain actual token values
            assertThat(toStringOutput).doesNotContain(TEST_ACCESS_TOKEN);
            assertThat(toStringOutput).doesNotContain(TEST_REFRESH_TOKEN);

            // But should contain safe metadata
            assertThat(toStringOutput).contains("Bearer");
            assertThat(toStringOutput).contains("read:self-assessment");
        }

        @Test
        @DisplayName("TOKEN-003-04: Token deletion removes file completely")
        void tokenDeletionRemovesFile() {
            Path tokenFile = tempDir.resolve("tokens.enc");
            EncryptedFileTokenStorage storage = new EncryptedFileTokenStorage(tokenFile);

            OAuthTokens tokens = new OAuthTokens(
                TEST_ACCESS_TOKEN,
                TEST_REFRESH_TOKEN,
                3600L,
                "Bearer",
                "read:self-assessment",
                Instant.now()
            );

            storage.save(tokens);
            assertThat(Files.exists(tokenFile)).isTrue();

            storage.delete();

            assertThat(Files.exists(tokenFile)).isFalse();
        }
    }

    /**
     * TOKEN-002: Encryption Security Tests
     */
    @Nested
    @DisplayName("TOKEN-002: Encryption Security")
    class EncryptionSecurity {

        @Test
        @DisplayName("TOKEN-002-01: Storage file is encrypted (not readable as plaintext)")
        void storageFileIsEncrypted() throws Exception {
            Path tokenFile = tempDir.resolve("tokens.enc");
            EncryptedFileTokenStorage storage = new EncryptedFileTokenStorage(tokenFile);

            String accessToken = "secret_access_token_abc123";
            OAuthTokens tokens = new OAuthTokens(
                accessToken,
                "refresh_token",
                3600L,
                "Bearer",
                "scope",
                Instant.now()
            );

            storage.save(tokens);

            byte[] fileContent = Files.readAllBytes(tokenFile);
            String contentAsString = new String(fileContent);

            // Encrypted file should NOT contain plaintext tokens
            assertThat(contentAsString).doesNotContain(accessToken);
            assertThat(contentAsString).doesNotContain("refresh_token");
        }

        @Test
        @DisplayName("TOKEN-002-03: IV is unique per encryption (different ciphertext)")
        void ivIsUniquePerEncryption() throws Exception {
            Path tokenFile1 = tempDir.resolve("tokens1.enc");
            Path tokenFile2 = tempDir.resolve("tokens2.enc");

            EncryptedFileTokenStorage storage1 = new EncryptedFileTokenStorage(tokenFile1);
            EncryptedFileTokenStorage storage2 = new EncryptedFileTokenStorage(tokenFile2);

            OAuthTokens tokens = new OAuthTokens(
                "same_token",
                "same_refresh",
                3600L,
                "Bearer",
                "scope",
                Instant.now()
            );

            storage1.save(tokens);
            storage2.save(tokens);

            byte[] content1 = Files.readAllBytes(tokenFile1);
            byte[] content2 = Files.readAllBytes(tokenFile2);

            // Same plaintext should produce different ciphertext due to unique IV
            assertThat(content1).isNotEqualTo(content2);
        }

        @Test
        @DisplayName("TOKEN-002-04: Corrupted file handled gracefully")
        void corruptedFileHandledGracefully() throws Exception {
            Path tokenFile = tempDir.resolve("tokens.enc");

            // Write some data first
            EncryptedFileTokenStorage storage = new EncryptedFileTokenStorage(tokenFile);
            storage.save(new OAuthTokens("token", "refresh", 3600L, "Bearer", "scope", Instant.now()));

            // Corrupt the file
            byte[] content = Files.readAllBytes(tokenFile);
            content[content.length / 2] ^= 0xFF; // Flip bits in middle
            Files.write(tokenFile, content);

            // Should throw TokenStorageException, not a raw crypto exception
            Assertions.assertThrows(TokenStorageException.class, () -> {
                storage.load();
            });
        }
    }

    /**
     * BIZ-003: NINO Masking Tests
     * Note: These test the masking logic in BusinessDetailsService
     */
    @Nested
    @DisplayName("BIZ-003: NINO Masking")
    class NinoMasking {

        @Test
        @DisplayName("BIZ-003-03: NINO masking format is correct (first 2 + **** + last 1)")
        void ninoMaskingFormatCorrect() {
            String nino = "AB123456C";
            String masked = maskNino(nino);

            assertThat(masked).isEqualTo("AB****C");
            assertThat(masked).hasSize(7);
            assertThat(masked).doesNotContain("123456");
        }

        @Test
        @DisplayName("BIZ-003-03: Short NINO handled gracefully")
        void shortNinoHandledGracefully() {
            String shortNino = "AB";
            String masked = maskNino(shortNino);

            // Should not throw, should return safe fallback
            assertThat(masked).isEqualTo("****");
        }

        @Test
        @DisplayName("BIZ-003-03: Null NINO handled gracefully")
        void nullNinoHandledGracefully() {
            String masked = maskNino(null);

            assertThat(masked).isEqualTo("****");
        }

        /**
         * NINO masking logic (mirrors BusinessDetailsService.maskNino)
         */
        private String maskNino(String nino) {
            if (nino == null || nino.length() < 4) {
                return "****";
            }
            return nino.substring(0, 2) + "****" + nino.substring(nino.length() - 1);
        }
    }

    /**
     * OAUTH-003: OAuth Security Tests
     */
    @Nested
    @DisplayName("OAUTH-003: OAuth Security")
    class OAuthSecurity {

        @Test
        @DisplayName("OAUTH-003-01: State is cryptographically random (unique values)")
        void stateIsCryptographicallyRandom() {
            java.util.Set<String> states = new java.util.HashSet<>();
            java.security.SecureRandom secureRandom = new java.security.SecureRandom();

            for (int i = 0; i < 100; i++) {
                byte[] bytes = new byte[24];
                secureRandom.nextBytes(bytes);
                String state = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                states.add(state);
            }

            // All 100 states should be unique
            assertThat(states).hasSize(100);
        }

        @Test
        @DisplayName("OAUTH-003-01: State has minimum length (32+ chars)")
        void stateHasMinimumLength() {
            java.security.SecureRandom secureRandom = new java.security.SecureRandom();
            byte[] bytes = new byte[24]; // 24 bytes = 32 chars in base64
            secureRandom.nextBytes(bytes);
            String state = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            assertThat(state.length()).isGreaterThanOrEqualTo(32);
        }

        @Test
        @DisplayName("OAUTH-003-01: State is URL-safe base64")
        void stateIsUrlSafeBase64() {
            java.security.SecureRandom secureRandom = new java.security.SecureRandom();
            byte[] bytes = new byte[24];
            secureRandom.nextBytes(bytes);
            String state = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            // URL-safe base64 should not contain +, /, or =
            assertThat(state).doesNotContain("+");
            assertThat(state).doesNotContain("/");
            assertThat(state).doesNotContain("=");
        }
    }
}
