package uk.selfemploy.ui.service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DatabaseMigrator - safe plaintext -> encrypted migration")
class DatabaseMigratorTest {

    @TempDir
    Path dir;

    private Path db;
    private DbKey key;

    @BeforeEach
    void setUp() {
        db = dir.resolve("selfemploy.db");
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        key = new DbKey(raw);
    }

    private void seedPlaintext() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE income (id INTEGER PRIMARY KEY AUTOINCREMENT, amount TEXT, note TEXT)");
            s.execute("CREATE TABLE expenses (id INTEGER PRIMARY KEY, category TEXT)");
            s.execute("CREATE INDEX idx_income_amount ON income(amount)");
            s.execute("INSERT INTO income (amount, note) VALUES ('1000.00','a'),('2500.00','b')");
            s.execute("INSERT INTO expenses (id, category) VALUES (1,'office')");
        }
    }

    @Test
    @DisplayName("encrypts the database, preserving tables, rows, indexes and autoincrement counters")
    void encryptsPreservingData() throws Exception {
        seedPlaintext();

        DatabaseMigrator.encrypt(db, key);

        // The file is now genuinely encrypted: opening without the key cannot read it.
        assertThatThrownBy(() -> {
            try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db); Statement s = c.createStatement()) {
                s.executeQuery("SELECT COUNT(*) FROM income");
            }
        }).isInstanceOf(Exception.class);

        // With the key, all data, the index, and the sequence counter are intact.
        try (Connection c = SqlCipherSupport.openEncrypted("jdbc:sqlite:" + db, key); Statement s = c.createStatement()) {
            try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM income")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(2);
            }
            try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM expenses")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
            try (ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='idx_income_amount'")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
            try (ResultSet rs = s.executeQuery("SELECT seq FROM sqlite_sequence WHERE name='income'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(2);
            }
            try (ResultSet rs = s.executeQuery("PRAGMA integrity_check")) {
                rs.next();
                assertThat(rs.getString(1)).isEqualToIgnoringCase("ok");
            }
        }

        // A plaintext backup is retained until explicitly deleted.
        Path bak = db.resolveSibling("selfemploy.db.plaintext.bak");
        assertThat(Files.exists(bak)).isTrue();
        DatabaseMigrator.deleteBackup(db);
        assertThat(Files.exists(bak)).isFalse();
    }

    @Test
    @DisplayName("a wrong key cannot open the encrypted database")
    void wrongKeyFails() throws Exception {
        seedPlaintext();
        DatabaseMigrator.encrypt(db, key);

        byte[] raw = new byte[32];
        java.util.Arrays.fill(raw, (byte) 0x11);
        DbKey wrong = new DbKey(raw);
        assertThatThrownBy(() -> {
            try (Connection c = SqlCipherSupport.openEncrypted("jdbc:sqlite:" + db, wrong); Statement s = c.createStatement()) {
                s.executeQuery("SELECT COUNT(*) FROM income");
            }
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("startup recovery restores the plaintext backup if the swap was interrupted")
    void recoversInterruptedSwap() throws Exception {
        seedPlaintext();
        // Simulate a crash after the first move: db is gone, only the backup remains.
        Path bak = db.resolveSibling("selfemploy.db.plaintext.bak");
        Files.move(db, bak);
        assertThat(Files.exists(db)).isFalse();

        DatabaseMigrator.restoreFromBackupIfInterrupted(db);

        assertThat(Files.exists(db)).isTrue();
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM income")) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(2);
        }
    }
}
