package uk.selfemploy.ui.service.security;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import uk.selfemploy.ui.service.AppDataDirectory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.logging.Logger;

/**
 * Owns the app-lock: the passphrase-derived protection of the database key. It never stores the
 * passphrase or the database key on disk — only a {@link Vault} of wrapped copies of the key.
 *
 * <p>Key hierarchy: {@code passphrase → Argon2id(salt) → key-encryption-key (KEK) → AES-256-GCM
 * unwraps a random database key}. Enabling protection generates the database key once; the passphrase
 * slot and a recovery code each wrap that same key, so changing the passphrase re-wraps one 32-byte
 * secret instead of re-encrypting the database. There is no escrow and no backdoor: losing the
 * passphrase and the recovery code means the data cannot be recovered.
 */
public final class AppLockService {

    private static final Logger LOG = Logger.getLogger(AppLockService.class.getName());

    // OWASP-current Argon2id parameters (2026): 64 MiB memory, 3 iterations, single lane.
    private static final int ARGON2_MEMORY_KIB = 65536;
    private static final int ARGON2_ITERATIONS = 3;
    private static final int ARGON2_PARALLELISM = 1;
    private static final int KEY_LEN = 32;      // 256-bit KEK and database key
    private static final int SALT_LEN = 16;
    private static final int NONCE_LEN = 12;
    private static final int GCM_TAG_BITS = 128;

    // Rate limiting: no delay for the first few tries, then escalating and capped, persisted across
    // restarts so relaunching cannot reset the throttle.
    private static final int FREE_ATTEMPTS = 3;
    private static final int MAX_SHIFT = 20;    // guards the 1000L << shift against overflow
    private static final long MAX_DELAY_MILLIS = 30_000;

    private final Path vaultPath;
    private final Path lockStatePath;
    private final LongSupplier nowMillis;
    private final SecureRandom random = new SecureRandom();

    private int failedAttempts;
    private long lockedUntilMillis;

    public AppLockService() {
        this(Vault.defaultPath(), System::currentTimeMillis);
    }

    AppLockService(Path vaultPath, LongSupplier nowMillis) {
        this.vaultPath = vaultPath;
        this.lockStatePath = vaultPath.resolveSibling(vaultPath.getFileName() + ".lockstate");
        this.nowMillis = nowMillis;
        loadLockState();
    }

    /** Whether the database is protected by a passphrase (a vault file exists). */
    public boolean isProtectionEnabled() {
        try {
            return Vault.read(vaultPath) != null;
        } catch (IOException e) {
            // A present-but-unreadable vault still means protection is on; unlock will surface the error.
            return Files.exists(vaultPath);
        }
    }

    // ==================== Enable (two-phase) ====================

    /**
     * A prepared-but-not-yet-committed protection: the vault is built in memory and only written to
     * disk by {@link #commit()}. This lets the UI show and confirm the recovery code <em>before</em>
     * anything is persisted, so abandoning the flow never leaves a half-enabled, un-recoverable state.
     */
    public final class PendingProtection {
        private final Vault vault;
        private final String recoveryCode;
        private final byte[] dbKey;    // kept only to zero it after commit; never leaves this class

        private PendingProtection(Vault vault, String recoveryCode, byte[] dbKey) {
            this.vault = vault;
            this.recoveryCode = recoveryCode;
            this.dbKey = dbKey;
        }

        /** The one-time-displayed recovery code to show the user. */
        public String recoveryCode() {
            return recoveryCode;
        }

        /** Writes the vault to disk, enabling protection from the next launch. Idempotent-safe. */
        public void commit() throws IOException {
            try {
                vault.write(vaultPath);
            } finally {
                Arrays.fill(dbKey, (byte) 0);
            }
        }
    }

    /**
     * Builds the vault, database key and recovery code in memory without writing anything. The caller
     * shows the recovery code and, once the user confirms it is saved, calls {@link PendingProtection#commit()}.
     * The database is encrypted on the next launch (see the app-lock gate), so no key is returned here.
     */
    public PendingProtection prepareProtection(char[] passphrase) {
        byte[] dbKey = new byte[KEY_LEN];
        random.nextBytes(dbKey);

        char[] recovery = generateRecoveryCode();
        Vault.Slot passphraseSlot = wrapSlot(Vault.VERSION, Vault.SLOT_PASSPHRASE, passphrase, dbKey);
        Vault.Slot recoverySlot = wrapSlot(Vault.VERSION, Vault.SLOT_RECOVERY, recovery, dbKey);
        Vault vault = new Vault(Vault.VERSION, Vault.CIPHER, List.of(passphraseSlot, recoverySlot));

        String recoveryString = new String(recovery);
        Arrays.fill(recovery, '\0');
        return new PendingProtection(vault, recoveryString, dbKey);
    }

    // ==================== Unlock ====================

    /** Unlocks the database key with the passphrase. Applies rate limiting on repeated failures. */
    public DbKey unlock(char[] passphrase) throws WrongPassphraseException, RateLimitedException, IOException {
        return unlockSlot(Vault.SLOT_PASSPHRASE, passphrase);
    }

    /** Unlocks with the recovery code. */
    public DbKey unlockWithRecovery(char[] code) throws WrongPassphraseException, RateLimitedException, IOException {
        return unlockSlot(Vault.SLOT_RECOVERY, code);
    }

    private DbKey unlockSlot(String slotName, char[] secret)
            throws WrongPassphraseException, RateLimitedException, IOException {
        enforceRateLimit();
        Vault vault = Vault.read(vaultPath);
        Vault.Slot slot = vault == null ? null : vault.slot(slotName);
        if (slot == null) {
            throw new WrongPassphraseException();
        }
        try {
            byte[] dbKey = unwrap(vault.vaultVersion(), slot, secret);
            recordSuccess();
            DbKey key = new DbKey(dbKey);
            Arrays.fill(dbKey, (byte) 0);
            return key;
        } catch (WrongPassphraseException e) {
            recordFailure();
            throw e;
        }
    }

    /**
     * Re-wraps the same database key under a new passphrase (verifying the current one first). The
     * database itself is never touched — only the vault's passphrase slot changes.
     */
    public void changePassphrase(char[] current, char[] next)
            throws WrongPassphraseException, RateLimitedException, IOException {
        DbKey key = unlock(current);
        byte[] dbKey = key.bytes();
        try {
            Vault vault = Vault.read(vaultPath);
            // Bind the new slot's AAD to the vault's stored version (not the compiled constant), so a
            // vault written by a different-versioned binary still unwraps after a passphrase change.
            Vault.Slot slot = wrapSlot(vault.vaultVersion(), Vault.SLOT_PASSPHRASE, next, dbKey);
            vault.withSlot(slot).write(vaultPath);
        } finally {
            Arrays.fill(dbKey, (byte) 0);
            key.destroy();
        }
    }

    // ==================== Argon2id + GCM wrap/unwrap ====================

    private Vault.Slot wrapSlot(int version, String name, char[] secret, byte[] dbKey) {
        byte[] salt = new byte[SALT_LEN];
        random.nextBytes(salt);
        Vault.KdfParams kdf = new Vault.KdfParams("argon2id", ARGON2_MEMORY_KIB, ARGON2_ITERATIONS,
                ARGON2_PARALLELISM, Base64.getEncoder().encodeToString(salt));
        byte[] kek = deriveKek(secret, kdf);
        try {
            byte[] nonce = new byte[NONCE_LEN];
            random.nextBytes(nonce);
            Vault.Slot slotForAad = new Vault.Slot(name, kdf, null, null);
            byte[] wrapped = gcm(Cipher.ENCRYPT_MODE, kek, nonce, Vault.aad(version, slotForAad), dbKey);
            return new Vault.Slot(name, kdf,
                    Base64.getEncoder().encodeToString(nonce),
                    Base64.getEncoder().encodeToString(wrapped));
        } finally {
            Arrays.fill(kek, (byte) 0);
        }
    }

    private byte[] unwrap(int version, Vault.Slot slot, char[] secret) throws WrongPassphraseException {
        byte[] kek = deriveKek(secret, slot.kdf());
        try {
            byte[] nonce = Base64.getDecoder().decode(slot.nonceB64());
            byte[] wrapped = Base64.getDecoder().decode(slot.wrappedKeyB64());
            Vault.Slot slotForAad = new Vault.Slot(slot.name(), slot.kdf(), null, null);
            return gcm(Cipher.DECRYPT_MODE, kek, nonce, Vault.aad(version, slotForAad), wrapped);
        } catch (Exception e) {
            // Auth-tag failure (wrong secret) or any decode error is an incorrect-secret outcome.
            throw new WrongPassphraseException();
        } finally {
            Arrays.fill(kek, (byte) 0);
        }
    }

    private static byte[] gcm(int mode, byte[] key, byte[] nonce, byte[] aad, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
            cipher.updateAAD(aad);
            return cipher.doFinal(input);
        } catch (Exception e) {
            throw new IllegalStateException("GCM operation failed", e);
        }
    }

    private static byte[] deriveKek(char[] secret, Vault.KdfParams kdf) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(Base64.getDecoder().decode(kdf.saltB64()))
                .withMemoryAsKB(kdf.memoryKib())
                .withIterations(kdf.iterations())
                .withParallelism(kdf.parallelism())
                .build();
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);
        byte[] kek = new byte[KEY_LEN];
        generator.generateBytes(secret, kek);
        return kek;
    }

    // ==================== Recovery code ====================

    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    /** A 125-bit recovery code as five groups of five base32 chars: XXXXX-XXXXX-... */
    private char[] generateRecoveryCode() {
        StringBuilder sb = new StringBuilder(29);
        for (int group = 0; group < 5; group++) {
            if (group > 0) {
                sb.append('-');
            }
            for (int i = 0; i < 5; i++) {
                sb.append(BASE32[random.nextInt(BASE32.length)]);
            }
        }
        char[] out = new char[sb.length()];
        sb.getChars(0, sb.length(), out, 0);
        return out;
    }

    // ==================== Rate limiting (persisted across restarts) ====================

    private synchronized void enforceRateLimit() throws RateLimitedException {
        long now = nowMillis.getAsLong();
        if (now < lockedUntilMillis) {
            throw new RateLimitedException(lockedUntilMillis - now);
        }
    }

    private synchronized void recordFailure() {
        failedAttempts++;
        if (failedAttempts > FREE_ATTEMPTS) {
            int shift = Math.min(MAX_SHIFT, failedAttempts - FREE_ATTEMPTS - 1);
            long delay = Math.min(MAX_DELAY_MILLIS, 1000L << shift);
            lockedUntilMillis = nowMillis.getAsLong() + delay;
        }
        saveLockState();
    }

    private synchronized void recordSuccess() {
        failedAttempts = 0;
        lockedUntilMillis = 0;
        saveLockState();
    }

    private void loadLockState() {
        try {
            if (Files.exists(lockStatePath)) {
                String[] parts = Files.readString(lockStatePath, StandardCharsets.UTF_8).trim().split(",");
                if (parts.length == 2) {
                    failedAttempts = Integer.parseInt(parts[0]);
                    lockedUntilMillis = Long.parseLong(parts[1]);
                }
            }
        } catch (Exception e) {
            // A corrupt lock-state file just means we start from zero this session; not fatal.
            LOG.fine("Could not read unlock-attempt state; starting fresh");
        }
    }

    private void saveLockState() {
        try {
            AppDataDirectory.writeRestricted(lockStatePath,
                    (failedAttempts + "," + lockedUntilMillis).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOG.fine("Could not persist unlock-attempt state");
        }
    }
}
