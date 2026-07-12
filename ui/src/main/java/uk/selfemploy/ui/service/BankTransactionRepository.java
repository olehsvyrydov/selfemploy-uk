package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.BankTransaction;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and querying bank transactions and their modification audit log for a
 * single business.
 *
 * <p>The shipping desktop app implements this with {@link SqliteBankTransactionRepository} (a JDBC
 * adapter). Instances are scoped to one business, fixed at construction. Transactions are never
 * hard-deleted; {@link #softDelete(UUID)} sets a tombstone to satisfy HMRC record retention.</p>
 */
public interface BankTransactionRepository {

    void save(BankTransaction tx);

    Optional<BankTransaction> findById(UUID id);

    List<BankTransaction> findAll();

    List<BankTransaction> findByImportAuditId(UUID importAuditId);

    long countByStatus(String status);

    long count();

    boolean existsByHash(String hash);

    boolean softDelete(UUID id);

    void logModification(UUID bankTransactionId, String modificationType, String fieldName,
                         String previousValue, String newValue, String modifiedBy);

    List<Map<String, String>> findModificationLogs(UUID bankTransactionId);

    UUID getBusinessId();
}
