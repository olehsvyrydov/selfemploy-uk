package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.enums.ReviewStatus;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * SQLite-backed service for managing bank transactions in the review workflow.
 * Thin wrapper over SqliteDataStore following the SqliteIncomeService pattern.
 *
 * <p>All operations go directly to SQLite - no in-memory caching.
 * This ensures transaction data is never lost.</p>
 */
public class SqliteBankTransactionService {

    private static final Logger LOG = Logger.getLogger(SqliteBankTransactionService.class.getName());

    private final SqliteDataStore dataStore;
    private final UUID businessId;

    public SqliteBankTransactionService(UUID businessId) {
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID cannot be null");
        }
        this.businessId = businessId;
        this.dataStore = SqliteDataStore.getInstance();
    }

    /**
     * Finds all bank transactions for this business.
     */
    public List<BankTransaction> findAll() {
        return dataStore.findBankTransactions(businessId);
    }

    /**
     * Finds a bank transaction by ID.
     */
    public Optional<BankTransaction> findById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        return dataStore.findBankTransactionById(id);
    }

    /**
     * Saves a bank transaction.
     */
    public void save(BankTransaction tx) {
        if (tx == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        dataStore.saveBankTransaction(tx);
    }

    /**
     * Updates a bank transaction (same as save, uses INSERT OR REPLACE).
     */
    public void update(BankTransaction tx) {
        save(tx);
    }

    /**
     * Categorizes a transaction as an expense by linking to an expense record.
     */
    public void categorizeAsExpense(UUID txId, UUID expenseId, Instant now) {
        BankTransaction tx = findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txId));
        save(tx.withCategorizedAsExpense(expenseId, now));
        LOG.fine("Categorized transaction " + txId + " as expense " + expenseId);
    }

    /**
     * Categorizes a transaction as income by linking to an income record.
     */
    public void categorizeAsIncome(UUID txId, UUID incomeId, Instant now) {
        BankTransaction tx = findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txId));
        save(tx.withCategorizedAsIncome(incomeId, now));
        LOG.fine("Categorized transaction " + txId + " as income " + incomeId);
    }

    /**
     * Excludes a transaction from accounting with a reason.
     */
    public void exclude(UUID txId, String reason, Instant now) {
        BankTransaction tx = findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txId));
        save(tx.withExcluded(reason, now));
        LOG.fine("Excluded transaction " + txId + ": " + reason);
    }

    /**
     * Skips a transaction during review (can be revisited later).
     */
    public void skip(UUID txId, Instant now) {
        BankTransaction tx = findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txId));
        // Create a skipped version using the same pattern
        BankTransaction skipped = new BankTransaction(
            tx.id(), tx.businessId(), tx.importAuditId(), tx.sourceFormatId(),
            tx.date(), tx.amount(), tx.description(), tx.accountLastFour(),
            tx.bankTransactionId(), tx.transactionHash(),
            ReviewStatus.SKIPPED, tx.incomeId(), tx.expenseId(), tx.exclusionReason(),
            tx.isBusiness(), tx.confidenceScore(), tx.suggestedCategory(),
            tx.createdAt(), now, tx.deletedAt(), tx.deletedBy(), tx.deletionReason()
        );
        save(skipped);
        LOG.fine("Skipped transaction " + txId);
    }

    /**
     * Sets the business/personal flag on a transaction.
     */
    public void setBusinessFlag(UUID txId, Boolean isBusiness, Instant now) {
        BankTransaction tx = findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txId));
        save(tx.withBusinessFlag(isBusiness, now));
        LOG.fine("Set business flag on " + txId + " to " + isBusiness);
    }

    /**
     * Returns counts for each review status.
     */
    public Map<ReviewStatus, Long> getStatusCounts() {
        Map<ReviewStatus, Long> counts = new EnumMap<>(ReviewStatus.class);
        for (ReviewStatus status : ReviewStatus.values()) {
            counts.put(status, dataStore.countBankTransactionsByStatus(businessId, status.name()));
        }
        return counts;
    }

    /**
     * Returns total count of all bank transactions for this business.
     */
    public long count() {
        return dataStore.countBankTransactions(businessId);
    }

    /**
     * Checks if a transaction with the given hash already exists.
     */
    public boolean existsByHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("Transaction hash cannot be null or empty");
        }
        return dataStore.existsByTransactionHash(businessId, hash);
    }

    /**
     * Deletes a bank transaction.
     */
    public boolean delete(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        return dataStore.deleteBankTransaction(id);
    }

    /**
     * Returns the business ID for this service.
     */
    public UUID getBusinessId() {
        return businessId;
    }
}
