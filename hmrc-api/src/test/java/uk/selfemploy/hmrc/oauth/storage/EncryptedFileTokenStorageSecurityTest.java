package uk.selfemploy.hmrc.oauth.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Security-property tests for the S17-06 hardened {@link EncryptedFileTokenStorage}.
 *
 * <p>These tests exist specifically to lock in the threat-model improvements made
 * for SLFEMPUK-30: a stolen token file is useless without the per-install random
 * key seed; the key seed file is 0600 on POSIX systems; two installs of the app
 * produce uncorrelated ciphertexts; and the salt is fresh per write so the same
 * tokens never encrypt to identical bytes.
 *
 * <p>Tests use a low PBKDF2 iteration count via the package-private constructor
 * so the suite is fast (production uses {@code DEFAULT_ITERATIONS} = 600 000).
 */
@DisplayName("EncryptedFileTokenStorage — S17-06 security properties (SLFEMPUK-30)")
class EncryptedFileTokenStorageSecurityTest {

    @TempDir
    Path tempDir;

    /** Fast iteration count for tests — production is {@link EncryptedFileTokenStorage#DEFAULT_ITERATIONS}. */
    private static final int FAST_ITERATIONS = 1_000;

    @Nested
    @DisplayName("Key seed file")
    class KeySeedFile {

        @Test
        @DisplayName("is created next to the token file on first save")
        void keySeedCreatedOnFirstSave() {
            Path tokenFile = tempDir.resolve("tokens.enc");
            new EncryptedFileTokenStorage(tokenFile, FAST_ITERATIONS).save(sampleTokens());

            Path keySeed = tempDir.resolve("tokens.enc.keyseed");
            assertThat(Files.exists(keySeed))
                .as("key seed file should be created at <tokenFile>.keyseed on first save")
                .isTrue();
        }

        @Test
        @DisplayName("contains exactly 32 random bytes (CSPRNG entropy)")
        void keySeedIs32Bytes() throws Exception {
            Path tokenFile = tempDir.resolve("tokens.enc");
            new EncryptedFileTokenStorage(tokenFile, FAST_ITERATIONS).save(sampleTokens());

            byte[] seed = Files.readAllBytes(tempDir.resolve("tokens.enc.keyseed"));
            assertThat(seed).hasSize(32);
            // Sanity: should not be all-zeros (would mean SecureRandom returned constant entropy)
            boolean anyNonZero = false;
            for (byte b : seed) {
                if (b != 0) { anyNonZero = true; break; }
            }
            assertThat(anyNonZero).as("key seed should not be all-zero").isTrue();
        }

        @Test
        @DisplayName("has POSIX 0600 permissions on POSIX filesystems (best-effort on Windows)")
        void keySeedHasOwnerOnlyPermissions() throws Exception {
            assumePosix();

            Path tokenFile = tempDir.resolve("tokens.enc");
            new EncryptedFileTokenStorage(tokenFile, FAST_ITERATIONS).save(sampleTokens());

            Path keySeed = tempDir.resolve("tokens.enc.keyseed");
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(keySeed);
            assertThat(perms)
                .as("key seed must be readable/writable by owner only — never group or other")
                .containsExactlyInAnyOrder(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE);
        }

        @Test
        @DisplayName("token file also has POSIX 0600 permissions on POSIX filesystems")
        void tokenFileHasOwnerOnlyPermissions() throws Exception {
            assumePosix();

            Path tokenFile = tempDir.resolve("tokens.enc");
            new EncryptedFileTokenStorage(tokenFile, FAST_ITERATIONS).save(sampleTokens());

            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(tokenFile);
            assertThat(perms).containsExactlyInAnyOrder(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE);
        }

        @Test
        @DisplayName("delete() removes the key seed alongside the token file")
        void deleteRemovesKeySeedToo() throws Exception {
            Path tokenFile = tempDir.resolve("tokens.enc");
            Path keySeed = tempDir.resolve("tokens.enc.keyseed");

            EncryptedFileTokenStorage storage = new EncryptedFileTokenStorage(tokenFile, FAST_ITERATIONS);
            storage.save(sampleTokens());
            assertThat(Files.exists(tokenFile)).isTrue();
            assertThat(Files.exists(keySeed)).isTrue();

            storage.delete();

            assertThat(Files.exists(tokenFile)).isFalse();
            assertThat(Files.exists(keySeed))
                .as("orphan key seed left on disk after delete() would leak data minimisation guarantees")
                .isFalse();
        }
    }

    @Nested
    @DisplayName("Encryption properties")
    class EncryptionProperties {

        @Test
        @DisplayName("two saves of the same tokens produce different ciphertexts (per-write random salt + IV)")
        void sameTokensProduceDifferentCiphertexts() throws Exception {
            Path tokenFile = tempDir.resolve("tokens.enc");
            EncryptedFileTokenStorage storage = new EncryptedFileTokenStorage(tokenFile, FAST_ITERATIONS);

            storage.save(sampleTokens());
            byte[] firstWrite = Files.readAllBytes(tokenFile);

            storage.save(sampleTokens());
            byte[] secondWrite = Files.readAllBytes(tokenFile);

            assertThat(firstWrite)
                .as("identical plaintext written twice with same key seed must still produce "
                    + "different ciphertexts thanks to per-write random salt and IV")
                .isNotEqualTo(secondWrite);
        }

        @Test
        @DisplayName("a stolen token file fails to decrypt against a different key seed")
        void stolenTokenFileWithWrongKeySeedFailsToDecrypt() throws Exception {
            Path victim = tempDir.resolve("victim.enc");
            Path attacker = tempDir.resolve("attacker.enc");

            // Victim saves real tokens.
            new EncryptedFileTokenStorage(victim, FAST_ITERATIONS).save(sampleTokens());

            // Attacker copies the victim's token file but does NOT have the victim's key seed
            // (instead, attacker has their own — generated by their own storage instance).
            new EncryptedFileTokenStorage(attacker, FAST_ITERATIONS).save(sampleTokens());
            Files.copy(victim, attacker, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            EncryptedFileTokenStorage attackerStorage = new EncryptedFileTokenStorage(attacker, FAST_ITERATIONS);
            assertThatThrownBy(attackerStorage::load)
                .as("ciphertext copied to a directory with the attacker's key seed must NOT decrypt — "
                    + "this is the core S17-06 threat model: the stolen file is useless without the seed")
                .isInstanceOf(TokenStorageException.class);
        }

        @Test
        @DisplayName("load() fails with a clear error when the key seed file is missing")
        void loadFailsWhenKeySeedIsMissing() throws Exception {
            Path tokenFile = tempDir.resolve("tokens.enc");
            Path keySeed = tempDir.resolve("tokens.enc.keyseed");

            EncryptedFileTokenStorage storage = new EncryptedFileTokenStorage(tokenFile, FAST_ITERATIONS);
            storage.save(sampleTokens());

            // Simulate accidental deletion / disk corruption of the key seed.
            Files.delete(keySeed);

            assertThatThrownBy(storage::load)
                .isInstanceOf(TokenStorageException.class);
        }

        @Test
        @DisplayName("round-trip: save then load returns equivalent tokens")
        void roundTrip() {
            Path tokenFile = tempDir.resolve("tokens.enc");
            EncryptedFileTokenStorage storage = new EncryptedFileTokenStorage(tokenFile, FAST_ITERATIONS);

            OAuthTokens original = sampleTokens();
            storage.save(original);
            Optional<OAuthTokens> loaded = storage.load();

            assertThat(loaded).isPresent();
            assertThat(loaded.get().accessToken()).isEqualTo(original.accessToken());
            assertThat(loaded.get().refreshToken()).isEqualTo(original.refreshToken());
        }
    }

    @Nested
    @DisplayName("Production iteration count")
    class IterationCount {

        @Test
        @DisplayName("DEFAULT_ITERATIONS meets OWASP 2023 minimum (>= 600 000) for PBKDF2-HMAC-SHA256")
        void defaultIterationsMeetsOwaspMinimum() {
            assertThat(EncryptedFileTokenStorage.DEFAULT_ITERATIONS)
                .as("OWASP 2023 minimum for PBKDF2-HMAC-SHA256 is 600 000 iterations — "
                    + "previously the project used 65 536 which is below modern recommendation")
                .isGreaterThanOrEqualTo(600_000);
        }
    }

    private static OAuthTokens sampleTokens() {
        return new OAuthTokens(
            "access_token_sample_12345",
            "refresh_token_sample_67890",
            3600L,
            "Bearer",
            "read:self-assessment write:self-assessment",
            Instant.now()
        );
    }

    private static void assumePosix() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
            FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
            "POSIX file permissions not supported on this filesystem — skipping");
    }
}
