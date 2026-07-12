package uk.selfemploy.ui.service;

import uk.selfemploy.core.reconciliation.MatchTier;
import uk.selfemploy.core.reconciliation.ReconciliationMatch;
import uk.selfemploy.core.reconciliation.ReconciliationStatus;
import uk.selfemploy.ui.service.sql.NamedSql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite JDBC adapter for {@link ReconciliationMatchRepository}.
 *
 * <p>Owns its own SQL (loaded from {@code /sql/reconciliation-match.sql}) and its row mapper,
 * running against the shared connection from {@link SqliteDataStore}. The reconciliation_matches
 * table DDL is handled by {@code SqliteDataStore}'s schema initialisation.</p>
 */
public class SqliteReconciliationMatchRepository implements ReconciliationMatchRepository {

    private static final Logger LOG = Logger.getLogger(SqliteReconciliationMatchRepository.class.getName());

    private static final NamedSql SQL = NamedSql.load("/sql/reconciliation-match.sql");

    private final SqliteDataStore dataStore;

    public SqliteReconciliationMatchRepository() {
        this.dataStore = SqliteDataStore.getInstance();
    }

    @Override
    public void save(ReconciliationMatch match) {
        if (match == null) {
            throw new IllegalArgumentException("Reconciliation match cannot be null");
        }
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("insertReconciliationMatch"))) {
            pstmt.setString(1, match.id().toString());
            pstmt.setString(2, match.bankTransactionId().toString());
            pstmt.setString(3, match.manualTransactionId().toString());
            pstmt.setString(4, match.manualTransactionType());
            pstmt.setDouble(5, match.confidence());
            pstmt.setString(6, match.matchTier().name());
            pstmt.setString(7, match.status().name());
            pstmt.setString(8, match.businessId().toString());
            pstmt.setString(9, match.createdAt().toString());
            pstmt.setString(10, match.resolvedAt() != null ? match.resolvedAt().toString() : null);
            pstmt.setString(11, match.resolvedBy());
            pstmt.executeUpdate();
            LOG.fine("Saved reconciliation match: " + match.id());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to save reconciliation match: " + match.id(), e);
            throw new DataStoreException("Failed to save reconciliation match", e);
        }
    }

    @Override
    public void saveAll(List<ReconciliationMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return;
        }
        dataStore.executeInTransaction(() -> {
            for (ReconciliationMatch match : matches) {
                save(match);
            }
        });
    }

    @Override
    public Optional<ReconciliationMatch> findById(UUID id) {
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("findReconciliationMatchById"))) {
            pstmt.setString(1, id.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapReconciliationMatch(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to find reconciliation match: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<ReconciliationMatch> findByBankTransactionId(UUID bankTransactionId) {
        List<ReconciliationMatch> matches = new ArrayList<>();
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("findReconciliationMatchesByBankTransaction"))) {
            pstmt.setString(1, bankTransactionId.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                matches.add(mapReconciliationMatch(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find reconciliation matches by bank tx: " + bankTransactionId, e);
        }
        return matches;
    }

    @Override
    public List<ReconciliationMatch> findByBusinessId(UUID businessId) {
        List<ReconciliationMatch> matches = new ArrayList<>();
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("findReconciliationMatchesByBusiness"))) {
            pstmt.setString(1, businessId.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                matches.add(mapReconciliationMatch(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find reconciliation matches by business: " + businessId, e);
        }
        return matches;
    }

    @Override
    public List<ReconciliationMatch> findUnresolvedByBusinessId(UUID businessId) {
        List<ReconciliationMatch> matches = new ArrayList<>();
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("findUnresolvedReconciliationMatchesByBusiness"))) {
            pstmt.setString(1, businessId.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                matches.add(mapReconciliationMatch(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find unresolved reconciliation matches", e);
        }
        return matches;
    }

    @Override
    public long countUnresolvedByBusinessId(UUID businessId) {
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("countUnresolvedReconciliationMatchesByBusiness"))) {
            pstmt.setString(1, businessId.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to count unresolved reconciliation matches", e);
        }
        return 0;
    }

    @Override
    public boolean updateStatus(UUID matchId, ReconciliationStatus status, Instant resolvedAt, String resolvedBy) {
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("updateReconciliationMatchStatus"))) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, resolvedAt != null ? resolvedAt.toString() : null);
            pstmt.setString(3, resolvedBy);
            pstmt.setString(4, matchId.toString());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to update reconciliation match status: " + matchId, e);
            return false;
        }
    }

    private ReconciliationMatch mapReconciliationMatch(ResultSet rs) throws SQLException {
        String resolvedAtStr = rs.getString("resolved_at");
        return new ReconciliationMatch(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("bank_transaction_id")),
            UUID.fromString(rs.getString("manual_transaction_id")),
            rs.getString("manual_transaction_type"),
            rs.getDouble("confidence"),
            MatchTier.valueOf(rs.getString("match_tier")),
            ReconciliationStatus.valueOf(rs.getString("status")),
            UUID.fromString(rs.getString("business_id")),
            Instant.parse(rs.getString("created_at")),
            resolvedAtStr != null ? Instant.parse(resolvedAtStr) : null,
            rs.getString("resolved_by")
        );
    }
}
