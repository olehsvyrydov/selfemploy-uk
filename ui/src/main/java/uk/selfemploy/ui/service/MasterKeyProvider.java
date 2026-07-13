package uk.selfemploy.ui.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.logging.Logger;

/**
 * Supplies the master secret used to encrypt credentials and OAuth tokens at rest.
 *
 * <p>The secret is 32 bytes from a CSPRNG, generated once per installation and held in a file
 * readable only by its owner. It replaces a key that was derived from {@code user.name},
 * {@code os.name}, {@code user.home} and the hostname — all values an attacker recovers from the
 * same disk image as the data, which made the resulting ciphertext obfuscation rather than
 * encryption.</p>
 *
 * <p><strong>What this does and does not protect.</strong> A key file stored beside the database
 * defends against other users of the machine and against a copy of the database alone (a stray
 * backup of just the {@code .db}). It does not defend against an attacker who obtains the key file
 * and the database together — a stolen laptop, or a full home-directory backup. Binding the key to
 * the platform keystore is the follow-up that closes that gap; this class is the seam for it.</p>
 */
public final class MasterKeyProvider {

    private static final Logger LOG = Logger.getLogger(MasterKeyProvider.class.getName());

    private static final String KEY_FILE = "master.key";
    private static final int KEY_LENGTH = 32;

    private static volatile MasterKeyProvider instance;

    private final Path keyPath;
    private final SecureRandom secureRandom = new SecureRandom();
    private volatile byte[] cached;

    MasterKeyProvider(Path keyPath) {
        this.keyPath = keyPath;
    }

    /**
     * Returns the shared provider, keyed on a file in the application data directory.
     *
     * @return the singleton provider
     */
    public static MasterKeyProvider getInstance() {
        if (instance == null) {
            synchronized (MasterKeyProvider.class) {
                if (instance == null) {
                    instance = new MasterKeyProvider(AppDataDirectory.resolve().resolve(KEY_FILE));
                }
            }
        }
        return instance;
    }

    /**
     * Returns the master secret, generating and persisting it on first use.
     *
     * @return a copy of the 32-byte secret
     * @throws CredentialEncryptionException if the secret cannot be read or created
     */
    public byte[] secret() {
        byte[] local = cached;
        if (local == null) {
            synchronized (this) {
                if (cached == null) {
                    cached = readOrCreate();
                }
                local = cached;
            }
        }
        return local.clone();
    }

    private byte[] readOrCreate() {
        try {
            if (Files.exists(keyPath)) {
                byte[] existing = Files.readAllBytes(keyPath);
                if (existing.length == KEY_LENGTH) {
                    return existing;
                }
                // A key file of the wrong length is a damaged or foreign key, not a signal to mint
                // a new one. Every credential and token already at rest was encrypted under the
                // real key; regenerating here would orphan all of them beyond recovery. Fail loudly
                // so the file can be restored from a backup or removed as a deliberate reset.
                throw new CredentialEncryptionException(
                    "Master key file " + keyPath + " has an unexpected length (" + existing.length
                        + " bytes); refusing to regenerate and discard existing encrypted data");
            }

            byte[] secret = new byte[KEY_LENGTH];
            secureRandom.nextBytes(secret);

            Path parent = keyPath.getParent();
            if (parent != null) {
                AppDataDirectory.createRestricted(parent);
            }
            AppDataDirectory.writeRestricted(keyPath, secret);
            LOG.info("Generated a new master key");
            return secret;
        } catch (IOException e) {
            throw new CredentialEncryptionException("Could not read or create the master key", e);
        }
    }
}
