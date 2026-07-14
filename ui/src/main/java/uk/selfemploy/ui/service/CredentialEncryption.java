package uk.selfemploy.ui.service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
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
 * AES-256-GCM encryption for secrets held at rest: HMRC API credentials and OAuth tokens.
 *
 * <p>The key is derived from the installation's {@link MasterKeyProvider} secret and a random
 * per-record salt. Because the master secret is 32 CSPRNG bytes rather than a memorable password,
 * the salt is mixed in with an HMAC extraction step; password stretching would add cost without
 * adding strength.</p>
 *
 * <p>Values written before the master key existed were encrypted under a key derived from public
 * machine metadata. Those are still readable — {@link #decrypt} detects the older layout — so an
 * upgrade does not destroy stored credentials. {@link #isLegacy} lets callers re-encrypt such a
 * value under the current key when they next load it.</p>
 */
public class CredentialEncryption {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int SALT_LENGTH = 16;

    /** Marks the layout that is keyed on the master secret, distinguishing it from the legacy one. */
    private static final String PREFIX = "v2:";

    private static final int LEGACY_KEY_LENGTH = 256;
    private static final int LEGACY_ITERATIONS = 65536;
    private static final String LEGACY_KEY_ALGORITHM = "PBKDF2WithHmacSHA256";

    private final SecureRandom secureRandom = new SecureRandom();
    private final MasterKeyProvider masterKeyProvider;

    public CredentialEncryption() {
        this(MasterKeyProvider.getInstance());
    }

    CredentialEncryption(MasterKeyProvider masterKeyProvider) {
        this.masterKeyProvider = masterKeyProvider;
    }

    /**
     * Encrypts a plaintext secret.
     *
     * @param plaintext the value to encrypt
     * @return the encrypted value, safe to store as text
     * @throws IllegalArgumentException if plaintext is null
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext cannot be null");
        }
        try {
            byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(salt), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(SALT_LENGTH + GCM_IV_LENGTH + ciphertext.length);
            buffer.put(salt);
            buffer.put(iv);
            buffer.put(ciphertext);

            return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new CredentialEncryptionException("Failed to encrypt credential", e);
        }
    }

    /**
     * Decrypts a value produced by {@link #encrypt}, or one written by an earlier version.
     *
     * @param encoded the stored value
     * @return the original plaintext
     * @throws IllegalArgumentException if encoded is null
     * @throws CredentialEncryptionException if decryption fails
     */
    public String decrypt(String encoded) {
        if (encoded == null) {
            throw new IllegalArgumentException("Encoded value cannot be null");
        }
        return isLegacy(encoded) ? decryptLegacy(encoded) : decryptCurrent(encoded);
    }

    /**
     * Whether a stored value predates the master key and should be rewritten under the current key.
     *
     * @param encoded the stored value
     * @return true if the value uses the superseded machine-derived key
     */
    public boolean isLegacy(String encoded) {
        return encoded != null && !encoded.startsWith(PREFIX);
    }

    private String decryptCurrent(String encoded) {
        try {
            byte[] blob = Base64.getDecoder().decode(encoded.substring(PREFIX.length()));
            if (blob.length < SALT_LENGTH + GCM_IV_LENGTH) {
                throw new GeneralSecurityException("Encrypted value is too short");
            }

            ByteBuffer buffer = ByteBuffer.wrap(blob);
            byte[] salt = new byte[SALT_LENGTH];
            buffer.get(salt);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(salt), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new CredentialEncryptionException("Invalid Base64 encoding", e);
        } catch (GeneralSecurityException e) {
            throw new CredentialEncryptionException("Failed to decrypt credential", e);
        }
    }

    /**
     * Derives an AES key by extracting from the master secret with the record's salt, the HKDF
     * extract step. Valid because the master secret is already full-entropy key material.
     */
    private SecretKey deriveKey(byte[] salt) throws GeneralSecurityException {
        byte[] secret = masterKeyProvider.secret();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(salt, "HmacSHA256"));
            return new SecretKeySpec(mac.doFinal(secret), "AES");
        } finally {
            java.util.Arrays.fill(secret, (byte) 0);
        }
    }

    private String decryptLegacy(String encoded) {
        try {
            byte[] encryptedData = Base64.getDecoder().decode(encoded);
            if (encryptedData.length < GCM_IV_LENGTH) {
                throw new GeneralSecurityException("Encrypted value is too short");
            }

            ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, deriveLegacyKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new CredentialEncryptionException("Invalid Base64 encoding", e);
        } catch (GeneralSecurityException e) {
            throw new CredentialEncryptionException("Failed to decrypt credential", e);
        }
    }

    private SecretKey deriveLegacyKey() throws GeneralSecurityException {
        StringBuilder password = new StringBuilder();
        password.append(System.getProperty("user.name", ""));
        password.append(System.getProperty("os.name", ""));
        password.append(System.getProperty("user.home", ""));
        try {
            password.append(java.net.InetAddress.getLocalHost().getHostName());
        } catch (Exception unavailable) {
            // The hostname was also optional when these values were written.
        }

        SecretKeyFactory factory = SecretKeyFactory.getInstance(LEGACY_KEY_ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(
            password.toString().toCharArray(), legacySalt(), LEGACY_ITERATIONS, LEGACY_KEY_LENGTH);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    private byte[] legacySalt() throws GeneralSecurityException {
        return MessageDigest.getInstance("SHA-256")
            .digest("uk.selfemploy.hmrc.credentials".getBytes(StandardCharsets.UTF_8));
    }
}
