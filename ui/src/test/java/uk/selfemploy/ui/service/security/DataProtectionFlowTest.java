package uk.selfemploy.ui.service.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end journey the launcher orchestrates: enable protection at onboarding (writes the vault but
 * leaves the database plaintext this session), then on the "next launch" unlock, notice the database is
 * still plaintext, run the one-time encryption, and read the data back through the encrypted database.
 */
@DisplayName("Data-protection flow: enable -> next-launch migrate -> unlock -> read")
class DataProtectionFlowTest {

    @TempDir
    Path dir;

    @Test
    void enableThenNextLaunchMigrateAndUnlock() throws Exception {
        Path db = dir.resolve("selfemploy.db");
        Path vault = dir.resolve("selfemploy.vault");

        // Seed the plaintext database with the user's data.
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE income (id INTEGER PRIMARY KEY, amount TEXT)");
            s.execute("INSERT INTO income (amount) VALUES ('4200.00')");
        }

        // Onboarding: enable protection. The vault is written; the database is NOT touched this session.
        AppLockService enableService = new AppLockService(vault, System::currentTimeMillis);
        AppLockService.EnableResult enabled = enableService.enableProtection("river-otter-sunrise".toCharArray());
        assertThat(DatabaseMigrator.databaseIsPlaintext(db)).isTrue();

        // Next launch: a fresh service unlocks, and because the DB is still plaintext it is encrypted now.
        AppLockService launch = new AppLockService(vault, System::currentTimeMillis);
        DbKey key = launch.unlock("river-otter-sunrise".toCharArray());
        assertThat(key.hex()).isEqualTo(enabled.dbKey().hex());
        if (DatabaseMigrator.databaseIsPlaintext(db)) {
            DatabaseMigrator.encrypt(db, key);
        }
        assertThat(DatabaseMigrator.databaseIsPlaintext(db)).isFalse();

        // The data is intact and readable only through the encrypted database.
        try (Connection c = SqlCipherSupport.openEncrypted("jdbc:sqlite:" + db, key); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT amount FROM income")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("4200.00");
        }

        // The recovery code opens the same (already-encrypted) database too.
        DbKey viaRecovery = new AppLockService(vault, System::currentTimeMillis)
                .unlockWithRecovery(enabled.recoveryCode().toCharArray());
        assertThat(viaRecovery.hex()).isEqualTo(key.hex());
    }
}
