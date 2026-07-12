package uk.selfemploy.ui.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link SqliteDataStore} initialises its schema through the versioned migration
 * runner and records the applied versions in the {@code schema_version} ledger.
 */
@DisplayName("SqliteDataStore schema versioning")
class SqliteSchemaVersionTest {

    private SqliteDataStore dataStore;

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetInstance();
        dataStore = SqliteDataStore.getInstance();
    }

    private List<Integer> appliedVersions() throws Exception {
        List<Integer> versions = new ArrayList<>();
        try (Statement stmt = dataStore.connection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version FROM schema_version ORDER BY version")) {
            while (rs.next()) {
                versions.add(rs.getInt(1));
            }
        }
        return versions;
    }

    @Test
    @DisplayName("records every defined migration version after initialisation")
    void recordsMigrationVersions() throws Exception {
        assertThat(appliedVersions()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("initialised schema exposes the honest submissions status constraint")
    void submissionsConstraintIsHonest() throws Exception {
        String ddl;
        try (Statement stmt = dataStore.connection().createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT sql FROM sqlite_master WHERE type='table' AND name='submissions'")) {
            assertThat(rs.next()).isTrue();
            ddl = rs.getString(1);
        }
        assertThat(ddl).contains("NOT_SUBMITTED");
    }
}
