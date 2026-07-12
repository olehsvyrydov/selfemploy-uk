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
 * SQLite JDBC adapter for {@link TermsAcceptanceRepository}.
 *
 * <p>Owns its own SQL (loaded from {@code /sql/terms-acceptance.sql}) and runs it against the
 * shared connection from {@link SqliteDataStore}. The {@code terms_acceptance} table is created
 * by {@code SqliteDataStore}'s schema initialisation.</p>
 *
 * <p>SE-508: Terms of Service UI</p>
 */
public class SqliteTermsAcceptanceRepository implements TermsAcceptanceRepository {

    private static final Logger LOG = Logger.getLogger(SqliteTermsAcceptanceRepository.class.getName());

    private static final NamedSql SQL = NamedSql.load("/sql/terms-acceptance.sql");

    private final SqliteDataStore dataStore;

    public SqliteTermsAcceptanceRepository() {
        this(SqliteDataStore.getInstance());
    }

    SqliteTermsAcceptanceRepository(SqliteDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public boolean save(String tosVersion, Instant acceptedAt, Instant scrollCompletedAt,
                        String applicationVersion) {
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("insertTermsAcceptance"))) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, tosVersion);
            pstmt.setString(3, acceptedAt.toString());
            pstmt.setString(4, scrollCompletedAt.toString());
            pstmt.setString(5, applicationVersion);
            pstmt.executeUpdate();
            LOG.info("Saved Terms acceptance for version: " + tosVersion);
            return true;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to save Terms acceptance", e);
            return false;
        }
    }

    @Override
    public Optional<String> getLatestAcceptedVersion() {
        return queryLatest("findLatestTermsVersion", rs -> rs.getString("tos_version"));
    }

    @Override
    public Optional<Instant> getLatestAcceptanceTimestamp() {
        return queryLatest("findLatestTermsAcceptedAt",
            rs -> Instant.parse(rs.getString("accepted_at")));
    }

    @Override
    public Optional<Instant> getLatestScrollCompletedTimestamp() {
        return queryLatest("findLatestTermsScrollCompletedAt",
            rs -> Instant.parse(rs.getString("scroll_completed_at")));
    }

    private <T> Optional<T> queryLatest(String statementName, RowMapper<T> mapper) {
        try (Statement stmt = dataStore.connection().createStatement();
             ResultSet rs = stmt.executeQuery(SQL.get(statementName))) {
            if (rs.next()) {
                return Optional.ofNullable(mapper.map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to read terms acceptance (" + statementName + ")", e);
        }
        return Optional.empty();
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}
