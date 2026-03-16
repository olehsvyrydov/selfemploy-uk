package uk.selfemploy.ui.service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for HMRC API credentials (client_id, client_secret).
 *
 * <p>Uses the same encryption approach as EncryptedFileTokenStorage:
 * PBKDF2 key derivation with machine-specific password, random IV per encryption,
 * and Base64 encoding for safe storage in SQLite settings table.</p>
 */
public class CredentialEncryption {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATIONS = 65536;
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";

    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] salt;

    public CredentialEncryption() {
        this.salt = deriveSalt();
    }

    /**
     * Encrypts a plaintext credential value to a Base64-encoded ciphertext.
     *
     * @param plaintext the credential value to encrypt
     * @return Base64-encoded encrypted value (with prepended IV)
     * @throws IllegalArgumentException if plaintext is null
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext cannot be null");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            SecretKey key = deriveKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(GCM_IV_LENGTH + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new CredentialEncryptionException("Failed to encrypt credential", e);
        }
    }

    /**
     * Decrypts a Base64-encoded ciphertext back to the original credential value.
     *
     * @param encoded Base64-encoded encrypted value
     * @return the original plaintext credential
     * @throws IllegalArgumentException if encoded is null
     * @throws CredentialEncryptionException if decryption fails
     */
    public String decrypt(String encoded) {
        if (encoded == null) {
            throw new IllegalArgumentException("Encoded value cannot be null");
        }
        try {
            byte[] encryptedData = Base64.getDecoder().decode(encoded);
            if (encryptedData.length < GCM_IV_LENGTH) {
                throw new GeneralSecurityException("Invalid encrypted data - too short");
            }

            ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            SecretKey key = deriveKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new CredentialEncryptionException("Invalid Base64 encoding", e);
        } catch (GeneralSecurityException e) {
            throw new CredentialEncryptionException("Failed to decrypt credential", e);
        }
    }

    private SecretKey deriveKey() throws GeneralSecurityException {
        String password = deriveMachinePassword();
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    private String deriveMachinePassword() {
        StringBuilder sb = new StringBuilder();
        sb.append(System.getProperty("user.name", ""));
        sb.append(System.getProperty("os.name", ""));
        sb.append(System.getProperty("user.home", ""));
        try {
            sb.append(java.net.InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            // Use what we have
        }
        return sb.toString();
    }

    private byte[] deriveSalt() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest("uk.selfemploy.hmrc.credentials".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return "selfemploy-hmrc-credentials".getBytes(StandardCharsets.UTF_8);
        }
    }
}
