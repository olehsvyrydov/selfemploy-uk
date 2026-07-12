package uk.selfemploy.ui.service;

import uk.selfemploy.core.reconciliation.ReconciliationMatch;
import uk.selfemploy.core.reconciliation.ReconciliationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and querying reconciliation matches.
 *
 * <p>Unlike the income/expense ledgers, matches are queried across businesses (by bank
 * transaction and by business), so the repository is not scoped to a single business.
 * The shipping desktop app implements this with {@link SqliteReconciliationMatchRepository}
 * (a JDBC adapter). Matches are never hard-deleted; the only mutation is a status change.</p>
 */
public interface ReconciliationMatchRepository {

    void save(ReconciliationMatch match);

    void saveAll(List<ReconciliationMatch> matches);

    Optional<ReconciliationMatch> findById(UUID id);

    List<ReconciliationMatch> findByBankTransactionId(UUID bankTransactionId);

    List<ReconciliationMatch> findByBusinessId(UUID businessId);

    List<ReconciliationMatch> findUnresolvedByBusinessId(UUID businessId);

    long countUnresolvedByBusinessId(UUID businessId);

    boolean updateStatus(UUID matchId, ReconciliationStatus status, Instant resolvedAt, String resolvedBy);
}
