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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SqliteDataStore - encrypted (keyed) connection path")
class SqliteDataStoreEncryptionTest {

    @TempDir
    Path dir;

    private static DbKey randomKey() {
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        return new DbKey(raw);
    }

    @Test
    @DisplayName("a keyed store encrypts the file, persists across reopen, and rejects a wrong key")
    void keyedRoundTrip() throws Exception {
        Path db = dir.resolve("selfemploy.db");
        DbKey key = randomKey();

        SqliteDataStore store = new SqliteDataStore(db, new CredentialEncryption(), key);
        try {
            store.saveDisplayName("Encrypted Ada");
            assertThat(store.loadDisplayName()).isEqualTo("Encrypted Ada");
        } finally {
            store.close();
        }

        // The file on disk is genuinely encrypted — plaintext open cannot read the schema.
        assertThatThrownBy(() -> {
            try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db); Statement s = c.createStatement()) {
                s.executeQuery("SELECT COUNT(*) FROM settings");
            }
        }).isInstanceOf(Exception.class);

        // Reopen with the same key: data survives, and a second thread (per-thread connection) reads it too.
        SqliteDataStore reopened = new SqliteDataStore(db, new CredentialEncryption(), key);
        try {
            assertThat(reopened.loadDisplayName()).isEqualTo("Encrypted Ada");

            AtomicReference<String> fromThread = new AtomicReference<>();
            Thread t = new Thread(() -> fromThread.set(reopened.loadDisplayName()));
            t.start();
            t.join();
            assertThat(fromThread.get()).isEqualTo("Encrypted Ada");
        } finally {
            reopened.close();
        }

        // A different key cannot open it.
        DbKey wrong = randomKey();
        assertThatThrownBy(() -> new SqliteDataStore(db, new CredentialEncryption(), wrong))
                .isInstanceOf(RuntimeException.class);
    }
}
