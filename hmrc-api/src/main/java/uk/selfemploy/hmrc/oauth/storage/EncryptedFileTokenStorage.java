package uk.selfemploy.hmrc.oauth.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.hmrc.oauth.storage.TokenStorageException.StorageError;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

/**
 * Encrypted file-based token storage using AES-256-GCM with PBKDF2-HMAC-SHA256 key
 * derivation against a random, user-private 32-byte key seed.
 *
 * <p><strong>SLFEMPUK-30 / S17-06:</strong> the previous implementation derived the
 * KDF password from publicly-inferable machine properties
 * ({@code user.name + os.name + user.home + hostname}) with a deterministic salt.
 * Any local process running as the user could re-derive the key trivially. This
 * implementation replaces both:
 *
 * <ul>
 *   <li>The password source is a random 32-byte seed persisted in a sibling
 *       {@code .keyseed} file with 0600 POSIX permissions (best-effort on Windows).
 *       Generated once on first save; not derivable from public machine metadata.</li>
 *   <li>The salt is freshly randomised per encryption (16 bytes) and stored
 *       prepended to the ciphertext.</li>
 *   <li>PBKDF2 iteration count is 600 000 — OWASP 2023 minimum for HMAC-SHA256.</li>
 *   <li>Both files are written with restrictive POSIX permissions where supported.</li>
 * </ul>
 *
 * <p>File format: {@code [salt(16) | iv(12) | ciphertext]}. The old format is not
 * forward-compatible; any tokens stored under the previous implementation must be
 * re-acquired via the OAuth flow (hobby-mode / pre-GA acceptable).
 *
 * <p>Future work (out of scope for S17-06): OS-native keystore integration —
 * Windows DPAPI, macOS Keychain Services, Linux libsecret / KWallet — would
 * remove the key seed file entirely. Tracked as a follow-up after Sprint 17.
 */
public class EncryptedFileTokenStorage implements TokenStorage {

    private static final Logger log = LoggerFactory.getLogger(EncryptedFileTokenStorage.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int SALT_LENGTH = 16;
    private static final int KEY_SEED_LENGTH = 32;
    private static final int KEY_LENGTH = 256;
    /** OWASP 2023 minimum for PBKDF2-HMAC-SHA256. */
    static final int DEFAULT_ITERATIONS = 600_000;
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String STORAGE_TYPE = "Encrypted File";
    private static final String KEY_SEED_SUFFIX = ".keyseed";

    private static final Set<PosixFilePermission> OWNER_READ_WRITE_ONLY =
        PosixFilePermissions.fromString("rw-------");

    private final Path filePath;
    private final Path keySeedPath;
    private final int iterations;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom;

    public EncryptedFileTokenStorage(Path filePath) {
        this(filePath, DEFAULT_ITERATIONS);
    }

    /**
     * Package-private constructor allowing tests to use a lower PBKDF2 iteration
     * count for speed. Production code should always use the
     * {@link #EncryptedFileTokenStorage(Path)} constructor.
     */
    EncryptedFileTokenStorage(Path filePath, int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be >= 1");
        }
        this.filePath = filePath;
        this.keySeedPath = filePath.resolveSibling(filePath.getFileName() + KEY_SEED_SUFFIX);
        this.iterations = iterations;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.secureRandom = new SecureRandom();
    }

    @Override
    public void save(OAuthTokens tokens) throws TokenStorageException {
        log.debug("Saving tokens to encrypted file");

        try {
            TokenData data = new TokenData(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.expiresIn(),
                tokens.tokenType(),
                tokens.scope(),
                tokens.issuedAt()
            );
            String json = objectMapper.writeValueAsString(data);

            byte[] keySeed = readOrCreateKeySeed();
            byte[] encrypted = encrypt(json.getBytes(StandardCharsets.UTF_8), keySeed);

            Files.createDirectories(filePath.getParent());
            Files.write(filePath, encrypted);
            restrictPermissions(filePath);

            log.info("Tokens saved to encrypted storage");
        } catch (GeneralSecurityException e) {
            log.error("Encryption failed", e);
            throw new TokenStorageException(StorageError.ENCRYPTION_FAILED, e);
        } catch (IOException e) {
            log.error("Failed to write token file", e);
            throw new TokenStorageException(StorageError.FILE_ACCESS_ERROR, e);
        }
    }

    @Override
    public Optional<OAuthTokens> load() throws TokenStorageException {
        if (!Files.exists(filePath)) {
            log.debug("Token file does not exist");
            return Optional.empty();
        }

        log.debug("Loading tokens from encrypted file");

        try {
            byte[] encrypted = Files.readAllBytes(filePath);
            byte[] keySeed = readKeySeedForLoad();
            byte[] decrypted = decrypt(encrypted, keySeed);
            String json = new String(decrypted, StandardCharsets.UTF_8);

            TokenData data = objectMapper.readValue(json, TokenData.class);

            OAuthTokens tokens = new OAuthTokens(
                data.accessToken,
                data.refreshToken,
                data.expiresIn,
                data.tokenType,
                data.scope,
                data.issuedAt
            );

            log.info("Tokens loaded from encrypted storage");
            return Optional.of(tokens);
        } catch (GeneralSecurityException e) {
            log.error("Decryption failed — file may be corrupted, key seed lost, or tokens were "
                + "written by an older (pre-SLFEMPUK-30) version. Re-authenticate to recover.", e);
            throw new TokenStorageException(StorageError.DECRYPTION_FAILED, e);
        } catch (IOException e) {
            log.error("Failed to read token file", e);
            throw new TokenStorageException(StorageError.FILE_ACCESS_ERROR, e);
        } catch (Exception e) {
            log.error("Failed to parse token data", e);
            throw new TokenStorageException(StorageError.STORAGE_CORRUPTED, e);
        }
    }

    @Override
    public void delete() throws TokenStorageException {
        log.debug("Deleting token file and key seed");

        try {
            Files.deleteIfExists(filePath);
            Files.deleteIfExists(keySeedPath);
            log.info("Token file and key seed deleted");
        } catch (IOException e) {
            log.error("Failed to delete token file", e);
            throw new TokenStorageException(StorageError.FILE_ACCESS_ERROR, e);
        }
    }

    @Override
    public boolean exists() {
        return Files.exists(filePath);
    }

    @Override
    public String getStorageType() {
        return STORAGE_TYPE;
    }

    private byte[] encrypt(byte[] plaintext, byte[] keySeed) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        SecretKey key = deriveKey(keySeed, salt);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] ciphertext = cipher.doFinal(plaintext);

        ByteBuffer buffer = ByteBuffer.allocate(SALT_LENGTH + GCM_IV_LENGTH + ciphertext.length);
        buffer.put(salt);
        buffer.put(iv);
        buffer.put(ciphertext);

        return buffer.array();
    }

    private byte[] decrypt(byte[] encryptedData, byte[] keySeed) throws GeneralSecurityException {
        if (encryptedData.length < SALT_LENGTH + GCM_IV_LENGTH) {
            throw new GeneralSecurityException("Invalid encrypted data — too short for salt + IV header");
        }

        ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
        byte[] salt = new byte[SALT_LENGTH];
        buffer.get(salt);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        SecretKey key = deriveKey(keySeed, salt);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(ciphertext);
    }

    private SecretKey deriveKey(byte[] keySeed, byte[] salt) throws GeneralSecurityException {
        // Use the hex-encoded random key seed as the PBKDF2 password input. This is not
        // a user-memorable secret — it is a 32-byte CSPRNG-generated value persisted in
        // a 0600 file alongside the token store. Combined with the per-file random salt,
        // two attackers each guessing username/hostname cannot collide on the derived key.
        char[] password = HexFormat.of().formatHex(keySeed).toCharArray();
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), "AES");
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    private byte[] readOrCreateKeySeed() throws IOException {
        if (Files.exists(keySeedPath)) {
            byte[] existing = Files.readAllBytes(keySeedPath);
            if (existing.length == KEY_SEED_LENGTH) {
                return existing;
            }
            log.warn("Existing key seed has wrong length ({} bytes), regenerating", existing.length);
        }

        byte[] seed = new byte[KEY_SEED_LENGTH];
        secureRandom.nextBytes(seed);

        Files.createDirectories(keySeedPath.getParent());
        Files.write(keySeedPath, seed);
        restrictPermissions(keySeedPath);

        log.info("Generated new 32-byte key seed at {}", keySeedPath);
        return seed;
    }

    private byte[] readKeySeedForLoad() throws IOException, GeneralSecurityException {
        if (!Files.exists(keySeedPath)) {
            throw new GeneralSecurityException(
                "Key seed file missing at " + keySeedPath
                    + " — token store cannot be decrypted. Delete the token file and re-authenticate.");
        }
        byte[] seed = Files.readAllBytes(keySeedPath);
        if (seed.length != KEY_SEED_LENGTH) {
            throw new GeneralSecurityException(
                "Key seed file has wrong length: expected " + KEY_SEED_LENGTH + " bytes, got " + seed.length);
        }
        return seed;
    }

    /**
     * Best-effort restriction of file permissions to owner-only read/write (POSIX 0600).
     * No-op on non-POSIX filesystems (typically Windows); on Windows, ACLs are inherited
     * from the parent directory which is normally {@code %APPDATA%} or {@code %LOCALAPPDATA%}
     * — both user-private by default.
     */
    private void restrictPermissions(Path path) {
        try {
            Files.setPosixFilePermissions(path, OWNER_READ_WRITE_ONLY);
        } catch (UnsupportedOperationException unsupported) {
            // Non-POSIX filesystem (e.g. Windows NTFS) — rely on inherited ACLs.
        } catch (IOException e) {
            log.warn("Could not set restrictive permissions on {} — {}", path, e.getMessage());
        }
    }

    /**
     * Internal data class for JSON serialization.
     */
    private record TokenData(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        String scope,
        Instant issuedAt
    ) {
    }
}
