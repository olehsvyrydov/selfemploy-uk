package uk.selfemploy.common.domain;

import uk.selfemploy.common.enums.ModificationType;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit log entry for changes to bank transactions.
 *
 * <p>Records every modification to a BankTransaction after import, forming
 * part of the digital link chain required for MTD compliance. Each entry
 * captures what changed, from what value, to what value, and who made the change.</p>
 *
 * <p>Log entries are write-once and never updated or deleted.</p>
 */
public record TransactionModificationLog(
    UUID id,
    UUID bankTransactionId,
    ModificationType modificationType,
    String fieldName,
    String previousValue,
    String newValue,
    String modifiedBy,
    Instant modifiedAt
) {
    /**
     * Compact constructor for validation.
     */
    public TransactionModificationLog {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (bankTransactionId == null) {
            throw new IllegalArgumentException("bankTransactionId cannot be null");
        }
        if (modificationType == null) {
            throw new IllegalArgumentException("modificationType cannot be null");
        }
        if (modifiedBy == null || modifiedBy.isBlank()) {
            throw new IllegalArgumentException("modifiedBy cannot be null or blank");
        }
        if (modifiedAt == null) {
            throw new IllegalArgumentException("modifiedAt cannot be null");
        }
    }

    /**
     * Creates a new modification log entry.
     *
     * @param bankTransactionId the transaction that was modified
     * @param modificationType  the type of modification
     * @param fieldName         the field that changed (nullable for status-only changes)
     * @param previousValue     the value before the change (nullable for initial categorization)
     * @param newValue          the new value after the change (nullable for removals)
     * @param modifiedBy        who made the change
     * @param modifiedAt        when the change was made
     */
    public static TransactionModificationLog create(
            UUID bankTransactionId,
            ModificationType modificationType,
            String fieldName,
            String previousValue,
            String newValue,
            String modifiedBy,
            Instant modifiedAt) {
        return new TransactionModificationLog(
            UUID.randomUUID(),
            bankTransactionId,
            modificationType,
            fieldName,
            previousValue,
            newValue,
            modifiedBy,
            modifiedAt
        );
    }
}
