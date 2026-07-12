package uk.selfemploy.ui.service.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SqliteMigrationRunner")
class SqliteMigrationRunnerTest {

    private Connection open() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    private SqliteMigrationRunner.Migration createTable(int version, String table) {
        return SqliteMigrationRunner.java(version, "create " + table, conn -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE " + table + " (id TEXT PRIMARY KEY)");
            }
        });
    }

    private boolean tableExists(Connection conn, String name) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type='table' AND name='" + name + "'")) {
            return rs.next();
        }
    }

    private List<Integer> recordedVersions(Connection conn) throws SQLException {
        List<Integer> versions = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version FROM schema_version ORDER BY version")) {
            while (rs.next()) {
                versions.add(rs.getInt(1));
            }
        }
        return versions;
    }

    @Test
    @DisplayName("applies all migrations on a fresh database and records their versions")
    void appliesAllAndRecords() throws Exception {
        try (Connection conn = open()) {
            int applied = new SqliteMigrationRunner(conn)
                .run(List.of(createTable(1, "alpha"), createTable(2, "beta")));

            assertThat(applied).isEqualTo(2);
            assertThat(tableExists(conn, "alpha")).isTrue();
            assertThat(tableExists(conn, "beta")).isTrue();
            assertThat(recordedVersions(conn)).containsExactly(1, 2);
        }
    }

    @Test
    @DisplayName("is idempotent: a second run applies nothing")
    void isIdempotent() throws Exception {
        try (Connection conn = open()) {
            List<SqliteMigrationRunner.Migration> migrations =
                List.of(createTable(1, "alpha"), createTable(2, "beta"));
            new SqliteMigrationRunner(conn).run(migrations);

            int appliedSecondTime = new SqliteMigrationRunner(conn).run(migrations);

            assertThat(appliedSecondTime).isZero();
            assertThat(recordedVersions(conn)).containsExactly(1, 2);
        }
    }

    @Test
    @DisplayName("skips versions already recorded and applies only newer ones")
    void skipsAlreadyApplied() throws Exception {
        try (Connection conn = open()) {
            new SqliteMigrationRunner(conn).run(List.of(createTable(1, "alpha")));

            int applied = new SqliteMigrationRunner(conn)
                .run(List.of(createTable(1, "alpha"), createTable(2, "beta")));

            assertThat(applied).isEqualTo(1); // only v2
            assertThat(tableExists(conn, "beta")).isTrue();
            assertThat(recordedVersions(conn)).containsExactly(1, 2);
        }
    }

    @Test
    @DisplayName("applies migrations in ascending version order regardless of list order")
    void appliesInVersionOrder() throws Exception {
        try (Connection conn = open()) {
            List<String> applyOrder = new ArrayList<>();
            SqliteMigrationRunner.Migration two =
                SqliteMigrationRunner.java(2, "two", c -> applyOrder.add("two"));
            SqliteMigrationRunner.Migration one =
                SqliteMigrationRunner.java(1, "one", c -> applyOrder.add("one"));

            new SqliteMigrationRunner(conn).run(List.of(two, one));

            assertThat(applyOrder).containsExactly("one", "two");
        }
    }

    @Test
    @DisplayName("script migration strips line comments so a semicolon in a comment does not split a statement")
    void scriptStripsCommentsWithSemicolons() throws Exception {
        try (Connection conn = open()) {
            new SqliteMigrationRunner(conn).run(List.of(
                SqliteMigrationRunner.script(1, "demo", "/db/migration-test/comment-with-semicolon.sql")));

            assertThat(tableExists(conn, "demo")).isTrue();
            assertThat(recordedVersions(conn)).containsExactly(1);
        }
    }

    @Test
    @DisplayName("baseline script creates the full application schema")
    void baselineScriptCreatesFullSchema() throws Exception {
        try (Connection conn = open()) {
            new SqliteMigrationRunner(conn).run(List.of(
                SqliteMigrationRunner.script(1, "baseline", "/db/migration-sqlite/V1__baseline.sql")));

            for (String table : List.of("settings", "business", "expenses", "income",
                    "terms_acceptance", "privacy_acknowledgment", "submissions",
                    "bank_transactions", "transaction_modification_log", "reconciliation_matches")) {
                assertThat(tableExists(conn, table)).as("table %s exists", table).isTrue();
            }
        }
    }
}
