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
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Encrypted file-based token storage using AES-256-GCM.
 * Uses a machine-specific key derived from hardware identifiers.
 */
public class EncryptedFileTokenStorage implements TokenStorage {

    private static final Logger log = LoggerFactory.getLogger(EncryptedFileTokenStorage.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATIONS = 65536;
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String STORAGE_TYPE = "Encrypted File";

    private final Path filePath;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom;
    private final byte[] salt;

    public EncryptedFileTokenStorage(Path filePath) {
        this.filePath = filePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.secureRandom = new SecureRandom();
        this.salt = deriveSalt();
    }

    @Override
    public void save(OAuthTokens tokens) throws TokenStorageException {
        log.debug("Saving tokens to encrypted file");

        try {
            // Serialize tokens to JSON
            TokenData data = new TokenData(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.expiresIn(),
                tokens.tokenType(),
                tokens.scope(),
                tokens.issuedAt()
            );
            String json = objectMapper.writeValueAsString(data);

            // Encrypt
            byte[] encrypted = encrypt(json.getBytes(StandardCharsets.UTF_8));

            // Write to file
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, encrypted);

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
            // Read encrypted data
            byte[] encrypted = Files.readAllBytes(filePath);

            // Decrypt
            byte[] decrypted = decrypt(encrypted);
            String json = new String(decrypted, StandardCharsets.UTF_8);

            // Deserialize
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
            log.error("Decryption failed - file may be corrupted", e);
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
        log.debug("Deleting token file");

        try {
            Files.deleteIfExists(filePath);
            log.info("Token file deleted");
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

    private byte[] encrypt(byte[] plaintext) throws GeneralSecurityException {
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        // Derive key
        SecretKey key = deriveKey();

        // Encrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
        byte[] ciphertext = cipher.doFinal(plaintext);

        // Prepend IV to ciphertext
        ByteBuffer buffer = ByteBuffer.allocate(GCM_IV_LENGTH + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);

        return buffer.array();
    }

    private byte[] decrypt(byte[] encryptedData) throws GeneralSecurityException {
        if (encryptedData.length < GCM_IV_LENGTH) {
            throw new GeneralSecurityException("Invalid encrypted data - too short");
        }

        // Extract IV
        ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);

        // Extract ciphertext
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        // Derive key
        SecretKey key = deriveKey();

        // Decrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

        return cipher.doFinal(ciphertext);
    }

    private SecretKey deriveKey() throws GeneralSecurityException {
        // Derive password from machine identifiers
        String password = deriveMachinePassword();

        // Use PBKDF2 to derive key
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);

        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    private String deriveMachinePassword() {
        // Combine machine-specific identifiers
        StringBuilder sb = new StringBuilder();
        sb.append(System.getProperty("user.name", ""));
        sb.append(System.getProperty("os.name", ""));
        sb.append(System.getProperty("user.home", ""));

        // Add some additional entropy
        try {
            java.net.InetAddress localhost = java.net.InetAddress.getLocalHost();
            sb.append(localhost.getHostName());
        } catch (Exception e) {
            // Ignore - use what we have
        }

        return sb.toString();
    }

    private byte[] deriveSalt() {
        // Use consistent salt based on application name
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest("uk.selfemploy.hmrc.oauth.tokens".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // Fallback to hardcoded salt
            return "selfemploy-hmrc-token-storage".getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Internal data class for JSON serialization.
     */
    private static class TokenData {
        public String accessToken;
        public String refreshToken;
        public long expiresIn;
        public String tokenType;
        public String scope;
        public Instant issuedAt;

        // For Jackson
        public TokenData() {}

        public TokenData(String accessToken, String refreshToken, long expiresIn,
                        String tokenType, String scope, Instant issuedAt) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
            this.tokenType = tokenType;
            this.scope = scope;
            this.issuedAt = issuedAt;
        }
    }
}
