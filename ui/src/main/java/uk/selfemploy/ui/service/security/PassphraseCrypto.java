package uk.selfemploy.ui.service.security;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

/**
 * The two primitives everything passphrase-protected in this app is built from: an Argon2id key
 * derivation, and AES-256-GCM with additional authenticated data.
 *
 * <p>Shared rather than duplicated. The key vault and an encrypted backup protect different things but
 * must protect them the same way; two copies of this would be two places to get the parameters wrong,
 * and two places to fix when they change.
 *
 * <p>Parameters are passed in rather than read from a container type, so the vault's on-disk format and
 * a backup's envelope stay independent of each other.
 */
final class PassphraseCrypto {

    /** OWASP-current Argon2id parameters (2026): 64 MiB memory, 3 iterations, single lane. */
    static final int ARGON2_MEMORY_KIB = 65536;
    static final int ARGON2_ITERATIONS = 3;
    static final int ARGON2_PARALLELISM = 1;

    /** 256-bit derived keys. */
    static final int KEY_LEN = 32;
    static final int SALT_LEN = 16;
    static final int NONCE_LEN = 12;
    static final int GCM_TAG_BITS = 128;

    private PassphraseCrypto() {
    }

    /**
     * Derives a key from a passphrase. Deliberately slow: this is the only thing standing between an
     * attacker holding the file and the data inside it.
     *
     * @param secret      the passphrase or recovery code, not wiped here — the caller owns it
     * @param salt        per-record salt, so the same passphrase yields a different key each time
     * @param memoryKib   Argon2id memory cost, read from the record rather than assumed, so stored
     *                    records remain readable if the defaults are raised later
     */
    static byte[] deriveKey(char[] secret, byte[] salt, int memoryKib, int iterations, int parallelism) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(salt)
                .withMemoryAsKB(memoryKib)
                .withIterations(iterations)
                .withParallelism(parallelism)
                .build();
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);
        byte[] key = new byte[KEY_LEN];
        generator.generateBytes(secret, key);
        return key;
    }

    /**
     * Encrypts or decrypts with AES-256-GCM.
     *
     * @param aad additional authenticated data — the surrounding header. Binding it here is what makes
     *            tampering with the header fail the tag rather than quietly change how the record reads.
     */
    static byte[] gcm(int mode, byte[] key, byte[] nonce, byte[] aad, byte[] input)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
        cipher.updateAAD(aad);
        return cipher.doFinal(input);
    }
}
