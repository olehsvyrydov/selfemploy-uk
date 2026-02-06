package uk.selfemploy.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.common.domain.TransactionModificationLog;
import uk.selfemploy.common.enums.ModificationType;
import uk.selfemploy.persistence.entity.TransactionModificationLogEntity;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository for TransactionModificationLog entries.
 *
 * <p>Provides write-once, read-many access to the modification audit trail.
 * Log entries are never updated or deleted once created.</p>
 */
@ApplicationScoped
public class TransactionModificationLogRepository
        implements PanacheRepositoryBase<TransactionModificationLogEntity, UUID> {

    /**
     * Saves a new modification log entry.
     */
    public TransactionModificationLog save(TransactionModificationLog log) {
        TransactionModificationLogEntity entity = TransactionModificationLogEntity.fromDomain(log);
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Finds all modification log entries for a bank transaction, ordered chronologically.
     */
    public List<TransactionModificationLog> findByBankTransactionId(UUID bankTransactionId) {
        return find("bankTransactionId = ?1 order by modifiedAt asc", bankTransactionId)
            .stream()
            .map(TransactionModificationLogEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds modification log entries by type for a specific transaction.
     */
    public List<TransactionModificationLog> findByBankTransactionIdAndType(
            UUID bankTransactionId, ModificationType type) {
        return find("bankTransactionId = ?1 and modificationType = ?2 order by modifiedAt asc",
                bankTransactionId, type)
            .stream()
            .map(TransactionModificationLogEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Counts all modification entries for a bank transaction.
     */
    public long countByBankTransactionId(UUID bankTransactionId) {
        return count("bankTransactionId", bankTransactionId);
    }
}
