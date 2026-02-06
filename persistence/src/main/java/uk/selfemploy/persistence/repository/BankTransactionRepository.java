package uk.selfemploy.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.enums.ReviewStatus;
import uk.selfemploy.persistence.entity.BankTransactionEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository for BankTransaction staging records.
 *
 * <p>Provides data access for imported bank transactions in the review workflow.
 * All queries exclude soft-deleted records unless explicitly requested.</p>
 */
@ApplicationScoped
public class BankTransactionRepository implements PanacheRepositoryBase<BankTransactionEntity, UUID> {

    /**
     * Saves a new bank transaction.
     */
    public BankTransaction save(BankTransaction tx) {
        BankTransactionEntity entity = BankTransactionEntity.fromDomain(tx);
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Updates an existing bank transaction (e.g., after review).
     */
    public BankTransaction update(BankTransaction tx) {
        BankTransactionEntity entity = BankTransactionEntity.fromDomain(tx);
        BankTransactionEntity merged = getEntityManager().merge(entity);
        return merged.toDomain();
    }

    /**
     * Finds a bank transaction by ID (excludes deleted).
     */
    public Optional<BankTransaction> findByIdActive(UUID id) {
        return find("id = ?1 and deletedAt is null", id)
            .firstResultOptional()
            .map(BankTransactionEntity::toDomain);
    }

    /**
     * Finds all active bank transactions for a business, ordered by date descending.
     */
    public List<BankTransaction> findByBusinessId(UUID businessId) {
        return find("businessId = ?1 and deletedAt is null order by date desc",
                businessId)
            .stream()
            .map(BankTransactionEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all bank transactions for a specific import audit.
     */
    public List<BankTransaction> findByImportAuditId(UUID importAuditId) {
        return find("importAuditId = ?1 and deletedAt is null order by date asc",
                importAuditId)
            .stream()
            .map(BankTransactionEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds bank transactions by review status for a business.
     */
    public List<BankTransaction> findByReviewStatus(UUID businessId, ReviewStatus status) {
        return find("businessId = ?1 and reviewStatus = ?2 and deletedAt is null order by date desc",
                businessId, status)
            .stream()
            .map(BankTransactionEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all pending (unreviewed) transactions for a business.
     */
    public List<BankTransaction> findPending(UUID businessId) {
        return findByReviewStatus(businessId, ReviewStatus.PENDING);
    }

    /**
     * Checks if a transaction with the given hash already exists for a business.
     * Used for duplicate detection during import.
     */
    public boolean existsByHash(UUID businessId, String transactionHash) {
        return count("businessId = ?1 and transactionHash = ?2 and deletedAt is null",
                businessId, transactionHash) > 0;
    }

    /**
     * Counts active transactions by review status for a business.
     */
    public long countByStatus(UUID businessId, ReviewStatus status) {
        return count("businessId = ?1 and reviewStatus = ?2 and deletedAt is null",
                businessId, status);
    }

    /**
     * Counts all active transactions for a business.
     */
    public long countActive(UUID businessId) {
        return count("businessId = ?1 and deletedAt is null", businessId);
    }

    /**
     * Soft-deletes all transactions for a specific import audit (used for undo).
     *
     * @return the number of records soft-deleted
     */
    public int softDeleteByImportAuditId(UUID importAuditId, Instant timestamp, String deletedBy, String reason) {
        return update("deletedAt = ?1, deletedBy = ?2, deletionReason = ?3, updatedAt = ?1 " +
                       "where importAuditId = ?4 and deletedAt is null",
                timestamp, deletedBy, reason, importAuditId);
    }

    /**
     * Saves a batch of bank transactions.
     */
    public List<BankTransaction> saveAll(List<BankTransaction> transactions) {
        return transactions.stream()
            .map(this::save)
            .collect(Collectors.toList());
    }
}
