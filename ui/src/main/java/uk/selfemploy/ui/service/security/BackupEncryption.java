package uk.selfemploy.ui.service.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Encrypts a backup file under a passphrase of the user's choosing.
 *
 * <p>A backup holds the same income, expenses, NINO and UTR as the database, so leaving it in the clear
 * hands away exactly what the at-rest encryption protects. It is encrypted here rather than in the export
 * service so the plaintext never reaches disk: it exists only between building the JSON and this cipher.
 *
 * <p>Deliberately independent of the key vault. A backup exists to survive the app — a lost machine, a
 * fresh install — so it cannot depend on a vault that may not be there. The passphrase may well be the
 * same one the user types at startup, but the file does not know that.
 *
 * <p>The envelope mirrors {@link Vault}: a readable JSON header naming the cipher and the KDF parameters,
 * with those parameters bound in as additional authenticated data. Editing the header to claim weaker
 * parameters therefore fails the tag instead of quietly changing how the file is read.
 */
public final class BackupEncryption {

    /** Marks a file as one of ours, and is what {@link #isEncrypted} looks for. */
    public static final String TYPE = "selfemploy.backup";
    public static final int VERSION = 1;
    public static final String CIPHER = "AES-256-GCM";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();

    /** The encrypted backup file's structure. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Envelope(String type, int version, String cipher, Kdf kdf,
                           String nonceB64, String ciphertextB64) {}

    /** Argon2id parameters, stored so a file written under older settings still opens. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Kdf(String algo, int memoryKib, int iterations, int parallelism, String saltB64) {}

    private BackupEncryption() {
    }

    /**
     * Wraps {@code plaintext} under {@code passphrase}.
     *
     * @param passphrase not wiped here; the caller owns it and should zero it afterwards
     * @return the envelope, ready to write to the user's chosen file
     */
    public static byte[] encrypt(byte[] plaintext, char[] passphrase) {
        byte[] salt = new byte[PassphraseCrypto.SALT_LEN];
        RANDOM.nextBytes(salt);
        byte[] nonce = new byte[PassphraseCrypto.NONCE_LEN];
        RANDOM.nextBytes(nonce);

        Kdf kdf = new Kdf("argon2id", PassphraseCrypto.ARGON2_MEMORY_KIB, PassphraseCrypto.ARGON2_ITERATIONS,
                PassphraseCrypto.ARGON2_PARALLELISM, Base64.getEncoder().encodeToString(salt));
        byte[] key = deriveKey(passphrase, kdf);
        try {
            byte[] ciphertext = PassphraseCrypto.gcm(
                    Cipher.ENCRYPT_MODE, key, nonce, aad(VERSION, kdf), plaintext);
            Envelope envelope = new Envelope(TYPE, VERSION, CIPHER, kdf,
                    Base64.getEncoder().encodeToString(nonce),
                    Base64.getEncoder().encodeToString(ciphertext));
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(envelope);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt the backup", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write the backup envelope", e);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    /**
     * Unwraps an envelope produced by {@link #encrypt}.
     *
     * @throws WrongPassphraseException if the passphrase is wrong, or the file has been altered — an
     *         auth-tag failure cannot tell those apart, and neither can the user do anything different
     */
    public static byte[] decrypt(byte[] envelopeBytes, char[] passphrase)
            throws WrongPassphraseException, IOException {
        Envelope envelope = MAPPER.readValue(envelopeBytes, Envelope.class);
        if (envelope.kdf() == null || envelope.nonceB64() == null || envelope.ciphertextB64() == null) {
            throw new IOException("This file is not a readable encrypted backup");
        }
        if (envelope.version() > VERSION) {
            throw new IOException("This backup was made by a newer version of the app");
        }
        if (!CIPHER.equals(envelope.cipher())) {
            // Checked rather than assumed. Decryption is AES-GCM whatever the header claims, so an
            // unchecked field would quietly become a downgrade vector the day a second cipher exists.
            throw new IOException("This backup uses an unsupported cipher: " + envelope.cipher());
        }
        byte[] key = deriveKey(passphrase, envelope.kdf());
        try {
            return PassphraseCrypto.gcm(Cipher.DECRYPT_MODE, key,
                    Base64.getDecoder().decode(envelope.nonceB64()),
                    aad(envelope.version(), envelope.kdf()),
                    Base64.getDecoder().decode(envelope.ciphertextB64()));
        } catch (AEADBadTagException | IllegalArgumentException e) {
            // Wrong passphrase, or a tampered file. Both are "this did not open".
            throw new WrongPassphraseException();
        } catch (GeneralSecurityException e) {
            // A provider or policy problem must not be reported to the user as a wrong passphrase.
            throw new IllegalStateException("Failed to decrypt the backup", e);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    /**
     * Whether these bytes are an encrypted backup rather than a plain export.
     *
     * <p>Both are JSON, so this checks the marker rather than whether parsing succeeds. Anything that is
     * not our envelope — including a plain export and any other file the user picks — reads as false, and
     * the import path then treats it as it always did.
     */
    public static boolean isEncrypted(byte[] fileBytes) {
        try {
            JsonNode root = MAPPER.readTree(fileBytes);
            return root != null && root.has("type") && TYPE.equals(root.get("type").asText());
        } catch (IOException | RuntimeException notOurs) {
            return false;
        }
    }

    private static byte[] deriveKey(char[] passphrase, Kdf kdf) {
        return PassphraseCrypto.deriveKey(passphrase, Base64.getDecoder().decode(kdf.saltB64()),
                kdf.memoryKib(), kdf.iterations(), kdf.parallelism());
    }

    /** Binds the header to the ciphertext, so downgrading the stated KDF parameters fails the tag. */
    private static byte[] aad(int version, Kdf kdf) {
        String bound = TYPE + "|" + version + "|" + CIPHER + "|" + kdf.algo() + "|" + kdf.memoryKib()
                + "|" + kdf.iterations() + "|" + kdf.parallelism() + "|" + kdf.saltB64();
        return bound.getBytes(StandardCharsets.UTF_8);
    }
}
