package uk.selfemploy.ui.service;

import uk.selfemploy.ui.service.sql.NamedSql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite JDBC adapter for {@link PrivacyAcknowledgmentRepository}.
 *
 * <p>Owns its own SQL (loaded from {@code /sql/privacy-acknowledgment.sql}) and runs it against
 * the shared connection from {@link SqliteDataStore}. The {@code privacy_acknowledgment} table is
 * created by {@code SqliteDataStore}'s schema initialisation.</p>
 *
 * <p>SE-507: Privacy Notice UI</p>
 */
public class SqlitePrivacyAcknowledgmentRepository implements PrivacyAcknowledgmentRepository {

    private static final Logger LOG =
        Logger.getLogger(SqlitePrivacyAcknowledgmentRepository.class.getName());

    private static final NamedSql SQL = NamedSql.load("/sql/privacy-acknowledgment.sql");

    private final SqliteDataStore dataStore;

    public SqlitePrivacyAcknowledgmentRepository() {
        this(SqliteDataStore.getInstance());
    }

    SqlitePrivacyAcknowledgmentRepository(SqliteDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public boolean save(String privacyVersion, Instant acknowledgedAt, String applicationVersion) {
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("insertPrivacyAcknowledgment"))) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, privacyVersion);
            pstmt.setString(3, acknowledgedAt.toString());
            pstmt.setString(4, applicationVersion);
            pstmt.executeUpdate();
            LOG.info("Saved Privacy acknowledgment for version: " + privacyVersion);
            return true;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to save Privacy acknowledgment", e);
            return false;
        }
    }

    @Override
    public Optional<String> getLatestAcknowledgedVersion() {
        return queryLatest("findLatestPrivacyVersion", rs -> rs.getString("privacy_version"));
    }

    @Override
    public Optional<Instant> getLatestAcknowledgmentTimestamp() {
        return queryLatest("findLatestPrivacyAcknowledgedAt",
            rs -> Instant.parse(rs.getString("acknowledged_at")));
    }

    private <T> Optional<T> queryLatest(String statementName, RowMapper<T> mapper) {
        try (Statement stmt = dataStore.connection().createStatement();
             ResultSet rs = stmt.executeQuery(SQL.get(statementName))) {
            if (rs.next()) {
                return Optional.ofNullable(mapper.map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to read privacy acknowledgment (" + statementName + ")", e);
        }
        return Optional.empty();
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}
