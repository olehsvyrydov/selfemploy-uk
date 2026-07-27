package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import uk.selfemploy.ui.service.security.DbKey;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Locking a running session. The store keeps its identity across a lock because repositories and services
 * hold it in their fields — so what has to be proven is that the same instance genuinely stops serving
 * data while locked, and serves it again after a reopen.
 */
@DisplayName("SqliteDataStore - locking a running session")
class SqliteDataStoreLockTest {

    @TempDir
    Path dir;

    private static DbKey randomKey() {
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        return new DbKey(raw);
    }

    @Test
    @DisplayName("a locked store refuses to serve data, and serves it again once reopened")
    void lockThenReopen() throws Exception {
        Path db = dir.resolve("selfemploy.db");
        DbKey key = randomKey();
        SqliteDataStore store = new SqliteDataStore(db, new CredentialEncryption(), key);
        try {
            store.saveDisplayName("Ada Lovelace");
            assertThat(store.loadDisplayName()).isEqualTo("Ada Lovelace");
            assertThat(store.isLocked()).isFalse();

            store.lock();

            assertThat(store.isLocked()).isTrue();
            // Refused outright, rather than lazily reopening an unkeyed connection to an encrypted file.
            assertThatThrownBy(store::loadDisplayName)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("locked");

            store.reopen(key);

            assertThat(store.isLocked()).isFalse();
            assertThat(store.loadDisplayName()).isEqualTo("Ada Lovelace");
        } finally {
            store.close();
        }
    }

    @Test
    @DisplayName("the file stays unreadable without the key while the store is locked")
    void lockedFileIsStillEncryptedOnDisk() throws Exception {
        Path db = dir.resolve("selfemploy.db");
        DbKey key = randomKey();
        SqliteDataStore store = new SqliteDataStore(db, new CredentialEncryption(), key);
        store.saveDisplayName("Ada Lovelace");
        store.lock();

        try {
            // Opening unkeyed fails outright on an SQLCipher file, so the whole attempt is the assertion.
            assertThatThrownBy(() -> {
                try (Connection plain = DriverManager.getConnection("jdbc:sqlite:" + db);
                     Statement statement = plain.createStatement()) {
                    statement.executeQuery("SELECT count(*) FROM sqlite_master").close();
                }
            }).isInstanceOf(Exception.class);
        } finally {
            store.close();
        }
    }

    @Test
    @DisplayName("reopening without a key is refused rather than silently falling back to plaintext")
    void reopenRequiresAKey() {
        Path db = dir.resolve("selfemploy.db");
        SqliteDataStore store = new SqliteDataStore(db, new CredentialEncryption(), randomKey());
        try {
            store.lock();
            assertThatThrownBy(() -> store.reopen(null))
                    .isInstanceOf(IllegalArgumentException.class);
        } finally {
            store.close();
        }
    }

    @Test
    @DisplayName("the auto-lock timeout round-trips, and is absent until chosen")
    void autoLockSettingRoundTrips() {
        Path db = dir.resolve("selfemploy.db");
        SqliteDataStore store = new SqliteDataStore(db, new CredentialEncryption(), randomKey());
        try {
            assertThat(store.loadAutoLockMinutes()).isNull();

            store.saveAutoLockMinutes(5);
            assertThat(store.loadAutoLockMinutes()).isEqualTo(5);

            store.saveAutoLockMinutes(0);
            assertThat(store.loadAutoLockMinutes()).isZero();
        } finally {
            store.close();
        }
    }
}
