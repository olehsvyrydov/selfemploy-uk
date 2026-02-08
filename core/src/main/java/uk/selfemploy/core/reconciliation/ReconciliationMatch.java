package uk.selfemploy.core.reconciliation;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a detected match between a bank-imported transaction and a
 * manually entered income or expense record.
 *
 * <p>Reconciliation matches are persisted to the reconciliation_matches table
 * and form part of the statutory audit trail. They must never be hard-deleted.</p>
 *
 * @param id                     unique identifier for this match
 * @param bankTransactionId      ID of the bank-imported transaction
 * @param manualTransactionId    ID of the manually entered income or expense
 * @param manualTransactionType  "INCOME" or "EXPENSE"
 * @param confidence             match confidence score (0.0 to 1.0)
 * @param matchTier              the tier of the match (LINKED, EXACT, LIKELY, POSSIBLE)
 * @param status                 resolution status (UNRESOLVED, CONFIRMED, DISMISSED)
 * @param businessId             the business this match belongs to
 * @param createdAt              when the match was detected
 * @param resolvedAt             when the user resolved the match (null if unresolved)
 * @param resolvedBy             who resolved the match (null if unresolved)
 */
public record ReconciliationMatch(
    UUID id,
    UUID bankTransactionId,
    UUID manualTransactionId,
    String manualTransactionType,
    double confidence,
    MatchTier matchTier,
    ReconciliationStatus status,
    UUID businessId,
    Instant createdAt,
    Instant resolvedAt,
    String resolvedBy
) {

    /**
     * Compact constructor for validation.
     */
    public ReconciliationMatch {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (bankTransactionId == null) {
            throw new IllegalArgumentException("bankTransactionId cannot be null");
        }
        if (manualTransactionId == null) {
            throw new IllegalArgumentException("manualTransactionId cannot be null");
        }
        if (manualTransactionType == null || manualTransactionType.isBlank()) {
            throw new IllegalArgumentException("manualTransactionType cannot be null or blank");
        }
        if (!manualTransactionType.equals("INCOME") && !manualTransactionType.equals("EXPENSE")) {
            throw new IllegalArgumentException("manualTransactionType must be INCOME or EXPENSE");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
        if (matchTier == null) {
            throw new IllegalArgumentException("matchTier cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        if (businessId == null) {
            throw new IllegalArgumentException("businessId cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt cannot be null");
        }
    }

    /**
     * Creates a new unresolved reconciliation match.
     */
    public static ReconciliationMatch create(
            UUID bankTransactionId,
            UUID manualTransactionId,
            String manualTransactionType,
            double confidence,
            MatchTier matchTier,
            UUID businessId,
            Instant now) {
        return new ReconciliationMatch(
            UUID.randomUUID(),
            bankTransactionId,
            manualTransactionId,
            manualTransactionType,
            confidence,
            matchTier,
            ReconciliationStatus.UNRESOLVED,
            businessId,
            now,
            null,
            null
        );
    }

    /**
     * Creates a copy with CONFIRMED status. The bank transaction should be
     * excluded from promotion to Income/Expense after confirmation.
     */
    public ReconciliationMatch withConfirmed(Instant resolvedAt, String resolvedBy) {
        return new ReconciliationMatch(
            id, bankTransactionId, manualTransactionId, manualTransactionType,
            confidence, matchTier, ReconciliationStatus.CONFIRMED, businessId,
            createdAt, resolvedAt, resolvedBy
        );
    }

    /**
     * Creates a copy with DISMISSED status. The bank transaction remains
     * available for categorisation.
     */
    public ReconciliationMatch withDismissed(Instant resolvedAt, String resolvedBy) {
        return new ReconciliationMatch(
            id, bankTransactionId, manualTransactionId, manualTransactionType,
            confidence, matchTier, ReconciliationStatus.DISMISSED, businessId,
            createdAt, resolvedAt, resolvedBy
        );
    }

    /**
     * Returns true if this match is still awaiting user review.
     */
    public boolean isUnresolved() {
        return status == ReconciliationStatus.UNRESOLVED;
    }

    /**
     * Returns true if the user confirmed this as a duplicate.
     */
    public boolean isConfirmed() {
        return status == ReconciliationStatus.CONFIRMED;
    }

    /**
     * Returns true if the user dismissed this match.
     */
    public boolean isDismissed() {
        return status == ReconciliationStatus.DISMISSED;
    }
}
