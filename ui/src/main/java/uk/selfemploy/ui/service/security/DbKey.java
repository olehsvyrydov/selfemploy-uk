package uk.selfemploy.ui.service.security;

import java.util.Arrays;
import java.util.HexFormat;

/**
 * Holds the 256-bit database encryption key in memory. The key is a random secret generated once when
 * the user first enables protection; the passphrase never derives it directly (it unwraps it — see
 * {@link AppLockService}). The bytes are handed to SQLCipher as a hex key string.
 *
 * <p>The raw bytes are never logged or exposed via {@link #toString()}; call {@link #destroy()} to best
 * a effort-wipe them when the app relocks or exits (JVM memory guarantees are limited, documented honestly).
 */
public final class DbKey {

    private final byte[] key;

    /**
     * @param key exactly 32 bytes (256 bits). The array is copied defensively.
     */
    public DbKey(byte[] key) {
        if (key == null || key.length != 32) {
            throw new IllegalArgumentException("Database key must be 32 bytes");
        }
        this.key = key.clone();
    }

    /** Lower-case hex form used as the SQLCipher key string. */
    public String hex() {
        return HexFormat.of().formatHex(key);
    }

    /** A defensive copy of the raw key bytes. Callers should wipe their copy when done. */
    byte[] bytes() {
        return key.clone();
    }

    /** Best-effort zeroing of the in-memory key material. */
    public void destroy() {
        Arrays.fill(key, (byte) 0);
    }

    @Override
    public String toString() {
        return "DbKey[REDACTED]";
    }
}
