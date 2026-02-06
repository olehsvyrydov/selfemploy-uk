package uk.selfemploy.common.enums;

/**
 * Status of a bank transaction during the review workflow.
 *
 * <p>State machine: PENDING → CATEGORIZED | EXCLUDED | SKIPPED
 *
 * <p>Tracks the user's review decision for each imported bank transaction.</p>
 */
public enum ReviewStatus {

    /** Transaction imported but not yet reviewed by user. */
    PENDING,

    /** Transaction categorized as income or expense and linked to a record. */
    CATEGORIZED,

    /** Transaction excluded from accounting (transfer, loan, tax payment, etc). */
    EXCLUDED,

    /** Transaction skipped by user during review (can be revisited). */
    SKIPPED;

    /**
     * Returns true if this transaction has been reviewed (any terminal state).
     */
    public boolean isReviewed() {
        return this != PENDING;
    }

    /**
     * Returns true if this transaction resulted in an Income or Expense record.
     */
    public boolean isCategorized() {
        return this == CATEGORIZED;
    }
}
