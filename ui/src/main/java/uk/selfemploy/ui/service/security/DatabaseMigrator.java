package uk.selfemploy.ui.service.security;

import uk.selfemploy.ui.service.AppDataDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Encrypts an existing plaintext SQLite database in place, safely. The original file is never mutated:
 * a new encrypted copy is built and verified, then atomically swapped in, keeping the plaintext as a
 * {@code .plaintext.bak} until the encrypted database has opened cleanly.
 *
 * <p>The encrypted copy is built by opening the destination as an SQLCipher database, attaching the
 * plaintext source with an empty key, cloning every schema object (tables, then their data and
 * autoincrement counters, then indexes/triggers/views), and verifying row counts and integrity before
 * the swap. Any failure before the swap leaves the original untouched; a failure mid-swap is recovered
 * from the {@code .bak} on the next launch via {@link #restoreFromBackupIfInterrupted}.
 */
public final class DatabaseMigrator {

    private static final Logger LOG = Logger.getLogger(DatabaseMigrator.class.getName());
    private static final String ENC_TMP_SUFFIX = ".enc.tmp";
    private static final String BAK_SUFFIX = ".plaintext.bak";

    private DatabaseMigrator() {
    }

    /** Thrown when migration fails; the original plaintext database is left intact. */
    public static class MigrationException extends Exception {
        public MigrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Encrypts {@code dbPath} in place with {@code key}. On success the file at {@code dbPath} is an
     * SQLCipher database and a {@code .plaintext.bak} sibling remains until {@code deleteBackup()} is
     * called (after the app confirms it can open the encrypted database).
     */
    public static void encrypt(Path dbPath, DbKey key) throws MigrationException {
        Path encTmp = sibling(dbPath, ENC_TMP_SUFFIX);
        Path bak = sibling(dbPath, BAK_SUFFIX);
        String plainUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        try {
            // 1. Fold the WAL back into the main file so the clone sees all committed data. If the
            // checkpoint reports busy, another process holds the database open — abort before touching
            // any files, so a second running instance can never have the database swapped out from under it.
            try (Connection c = DriverManager.getConnection(plainUrl); Statement s = c.createStatement();
                 java.sql.ResultSet rs = s.executeQuery("PRAGMA wal_checkpoint(TRUNCATE)")) {
                if (rs.next() && rs.getInt(1) != 0) {
                    throw new MigrationException(
                            "The database is open in another window; cannot encrypt it now", null);
                }
            }
            Map<String, Long> before = tableCounts(plainUrl, null);

            // 2. Build the encrypted copy.
            Files.deleteIfExists(encTmp);
            cloneToEncrypted(dbPath, encTmp, key);

            // 3. Verify the copy opens with the key, is intact, and has identical row counts.
            String encUrl = "jdbc:sqlite:" + encTmp.toAbsolutePath();
            verifyIntegrity(encUrl, key);
            Map<String, Long> after = tableCounts(encUrl, key);
            if (!before.equals(after)) {
                throw new MigrationException("Row counts changed during encryption: "
                        + before + " -> " + after, null);
            }

            // 4. Atomic swap, keeping the plaintext as a backup.
            deleteSidecars(dbPath);
            Files.move(dbPath, bak, StandardCopyOption.ATOMIC_MOVE);
            Files.move(encTmp, dbPath, StandardCopyOption.ATOMIC_MOVE);
            AppDataDirectory.restrictFile(dbPath);
            AppDataDirectory.restrictFile(bak);
            LOG.info("Database encrypted; plaintext backup retained until first successful unlock");
        } catch (MigrationException e) {
            safeDelete(encTmp);
            throw e;
        } catch (SQLException | IOException | RuntimeException e) {
            safeDelete(encTmp);
            throw new MigrationException("Failed to encrypt the database", e);
        }
    }

    /** Deletes the plaintext backup once the encrypted database is confirmed working. */
    public static void deleteBackup(Path dbPath) {
        safeDelete(sibling(dbPath, BAK_SUFFIX));
    }

    /**
     * Whether an existing database file is still plaintext (readable without a key). Used at startup to
     * decide whether protection has been enabled but the one-time encryption has not yet run. Returns
     * false if the file is absent (nothing to migrate) or already encrypted.
     */
    public static boolean databaseIsPlaintext(Path dbPath) {
        if (!Files.exists(dbPath)) {
            return false;
        }
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
             Statement s = c.createStatement()) {
            s.executeQuery("SELECT count(*) FROM sqlite_master").close();
            return true;
        } catch (SQLException e) {
            return false;   // not readable as plaintext -> already encrypted
        }
    }

    /**
     * Recovers from a crash during the atomic swap: if the database file is missing but a plaintext
     * backup exists, restore it. Called at startup before opening the store.
     */
    public static void restoreFromBackupIfInterrupted(Path dbPath) {
        Path bak = sibling(dbPath, BAK_SUFFIX);
        try {
            if (!Files.exists(dbPath) && Files.exists(bak)) {
                LOG.warning("Database missing but a plaintext backup exists; restoring it");
                Files.move(bak, dbPath, StandardCopyOption.ATOMIC_MOVE);
                AppDataDirectory.restrictFile(dbPath);
            }
        } catch (IOException e) {
            LOG.log(java.util.logging.Level.SEVERE, "Failed to restore database from backup", e);
        }
    }

    // ==================== internals ====================

    private static void cloneToEncrypted(Path plain, Path encTmp, DbKey key)
            throws SQLException, MigrationException {
        String plainAttach = plain.toAbsolutePath().toString().replace("'", "''");
        try (Connection c = SqlCipherSupport.openEncrypted("jdbc:sqlite:" + encTmp.toAbsolutePath(), key);
             Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = OFF");
            s.execute("ATTACH DATABASE '" + plainAttach + "' AS src KEY ''");

            List<String> tables = new ArrayList<>();
            for (Object[] obj : objects(c, "table")) {
                String name = (String) obj[0];
                if (name.startsWith("sqlite_")) {
                    continue;   // sqlite_sequence / sqlite_stat* are managed implicitly
                }
                s.execute((String) obj[1]);   // CREATE TABLE ... in the encrypted main
                tables.add(name);
            }
            for (String name : tables) {
                String q = quoteIdentifier(name);
                s.execute("INSERT INTO main." + q + " SELECT * FROM src." + q);
            }
            // Preserve AUTOINCREMENT counters if the source tracked any.
            if (hasTable(s, "src", "sqlite_sequence") && hasTable(s, "main", "sqlite_sequence")) {
                s.execute("DELETE FROM main.sqlite_sequence");
                s.execute("INSERT INTO main.sqlite_sequence SELECT * FROM src.sqlite_sequence");
            }
            for (String type : List.of("index", "trigger", "view")) {
                for (Object[] obj : objects(c, type)) {
                    if (!((String) obj[0]).startsWith("sqlite_")) {
                        s.execute((String) obj[1]);
                    }
                }
            }
            s.execute("DETACH DATABASE src");
        }
    }

    /**
     * Validates and double-quotes a table identifier taken from the local schema before it is used in a
     * statement. The names come from the app's own sqlite_master, but validating against a strict pattern
     * keeps the clone injection-free even if the schema ever changes.
     */
    private static String quoteIdentifier(String name) {
        if (!name.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Unexpected table name in schema: " + name);
        }
        return "\"" + name + "\"";
    }

    /** Returns {name, sql} for each non-autogenerated object of the given type in the src schema. */
    private static List<Object[]> objects(Connection c, String type) throws SQLException {
        List<Object[]> out = new ArrayList<>();
        try (java.sql.PreparedStatement ps = c.prepareStatement(
                "SELECT name, sql FROM src.sqlite_master WHERE type = ? AND sql IS NOT NULL")) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Object[]{rs.getString(1), rs.getString(2)});
                }
            }
        }
        return out;
    }

    private static boolean hasTable(Statement s, String schema, String name) throws SQLException {
        try (ResultSet rs = s.executeQuery("SELECT 1 FROM " + schema
                + ".sqlite_master WHERE type='table' AND name='" + name + "'")) {
            return rs.next();
        }
    }

    private static void verifyIntegrity(String url, DbKey key) throws SQLException, MigrationException {
        try (Connection c = SqlCipherSupport.openEncrypted(url, key); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("PRAGMA integrity_check")) {
            if (!rs.next() || !"ok".equalsIgnoreCase(rs.getString(1))) {
                throw new MigrationException("Encrypted copy failed its integrity check", null);
            }
        }
    }

    private static Map<String, Long> tableCounts(String url, DbKey key) throws SQLException {
        Map<String, Long> counts = new LinkedHashMap<>();
        try (Connection c = key == null ? DriverManager.getConnection(url) : SqlCipherSupport.openEncrypted(url, key);
             Statement s = c.createStatement()) {
            List<String> names = new ArrayList<>();
            try (ResultSet rs = s.executeQuery("SELECT name FROM sqlite_master WHERE type='table' "
                    + "AND name NOT LIKE 'sqlite_%' ORDER BY name")) {
                while (rs.next()) {
                    names.add(rs.getString(1));
                }
            }
            for (String name : names) {
                try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + quoteIdentifier(name))) {
                    rs.next();
                    counts.put(name, rs.getLong(1));
                }
            }
        }
        return counts;
    }

    private static void deleteSidecars(Path dbPath) {
        String name = dbPath.getFileName().toString();
        safeDelete(dbPath.resolveSibling(name + "-wal"));
        safeDelete(dbPath.resolveSibling(name + "-shm"));
    }

    private static Path sibling(Path dbPath, String suffix) {
        return dbPath.resolveSibling(dbPath.getFileName().toString() + suffix);
    }

    private static void safeDelete(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // best effort
        }
    }
}
