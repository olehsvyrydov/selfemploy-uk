package uk.selfemploy.ui.service.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Applies ordered, run-once schema migrations against a SQLite connection, tracking applied
 * versions in a {@code schema_version} ledger table.
 *
 * <p>Each {@link Migration} runs at most once: the runner applies every migration whose
 * {@link Migration#version()} is greater than the highest recorded version, in ascending order,
 * and records it on success. A pre-migration database (no {@code schema_version} table) is treated
 * as version 0, so every migration runs once on first upgrade; migrations are therefore written to
 * be idempotent (e.g. {@code CREATE TABLE IF NOT EXISTS}, add-column-if-missing, rebuild-if-needed)
 * so that path converges to the same schema as a fresh install.</p>
 *
 * <p>The runner does not wrap a migration in its own transaction — a migration that needs
 * transactional atomicity or {@code PRAGMA} changes (which SQLite forbids inside a transaction)
 * manages that itself.</p>
 */
public class SqliteMigrationRunner {

    private static final Logger LOG = Logger.getLogger(SqliteMigrationRunner.class.getName());

    /** A single ordered schema change. */
    public interface Migration {
        int version();

        String name();

        void apply(Connection connection) throws SQLException;
    }

    private final Connection connection;

    public SqliteMigrationRunner(Connection connection) {
        this.connection = connection;
    }

    /**
     * Applies every migration newer than the recorded version, in ascending order.
     *
     * @return the number of migrations applied in this run
     */
    public int run(List<Migration> migrations) throws SQLException {
        ensureSchemaVersionTable();
        int current = currentVersion();
        int applied = 0;

        List<Migration> ordered = migrations.stream()
            .sorted((a, b) -> Integer.compare(a.version(), b.version()))
            .toList();

        for (Migration migration : ordered) {
            if (migration.version() <= current) {
                continue;
            }
            LOG.info("Applying schema migration v" + migration.version() + " (" + migration.name() + ")");
            migration.apply(connection);
            record(migration);
            applied++;
        }
        if (applied == 0) {
            LOG.fine("Schema is up to date at version " + current);
        }
        return applied;
    }

    private void ensureSchemaVersionTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    applied_at TEXT NOT NULL DEFAULT (datetime('now'))
                )
            """);
        }
    }

    private int currentVersion() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void record(Migration migration) throws SQLException {
        try (PreparedStatement pstmt =
                 connection.prepareStatement("INSERT INTO schema_version (version, name) VALUES (?, ?)")) {
            pstmt.setInt(1, migration.version());
            pstmt.setString(2, migration.name());
            pstmt.executeUpdate();
        }
    }

    /**
     * Builds a {@link Migration} from a classpath {@code .sql} resource. Statements are separated by
     * semicolons (the SelfEmploy migration scripts contain no semicolons inside a statement).
     */
    public static Migration script(int version, String name, String resourcePath) {
        return new Migration() {
            @Override
            public int version() {
                return version;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public void apply(Connection connection) throws SQLException {
                String sql = stripLineComments(loadResource(resourcePath));
                try (Statement stmt = connection.createStatement()) {
                    for (String statement : sql.split(";")) {
                        String trimmed = statement.strip();
                        if (!trimmed.isEmpty()) {
                            stmt.execute(trimmed);
                        }
                    }
                }
            }
        };
    }

    /**
     * Builds a {@link Migration} from a Java action, for changes that cannot be expressed as plain
     * idempotent SQL (add-column-if-missing, conditional table rebuilds).
     */
    public static Migration java(int version, String name, JdbcMigration action) {
        return new Migration() {
            @Override
            public int version() {
                return version;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public void apply(Connection connection) throws SQLException {
                action.apply(connection);
            }
        };
    }

    /** A schema change expressed as Java code operating on the connection. */
    @FunctionalInterface
    public interface JdbcMigration {
        void apply(Connection connection) throws SQLException;
    }

    /**
     * Removes {@code --} line comments (to end of line) so a semicolon inside a comment does not
     * split a statement. The migration scripts contain no string literals holding {@code --}.
     */
    private static String stripLineComments(String sql) {
        StringBuilder cleaned = new StringBuilder(sql.length());
        for (String line : sql.split("\n", -1)) {
            int comment = line.indexOf("--");
            cleaned.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');
        }
        return cleaned.toString();
    }

    private static String loadResource(String resourcePath) {
        try (InputStream in = SqliteMigrationRunner.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Migration resource not found: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to read migration resource: " + resourcePath, e);
            throw new IllegalStateException("Failed to read migration resource: " + resourcePath, e);
        }
    }
}
