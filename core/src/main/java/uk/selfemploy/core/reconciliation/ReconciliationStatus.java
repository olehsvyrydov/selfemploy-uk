package uk.selfemploy.core.reconciliation;

/**
 * Status of a reconciliation match between a bank-imported transaction
 * and a manually entered income/expense record.
 *
 * <p>Reconciliation matches are statutory records that must never be hard-deleted
 * (TMA 1970 s.12B record-keeping requirements).</p>
 */
public enum ReconciliationStatus {

    /**
     * Match has been detected but not yet reviewed by the user.
     */
    UNRESOLVED,

    /**
     * User has confirmed this is a genuine duplicate.
     * The bank transaction should be excluded from promotion to Income/Expense.
     */
    CONFIRMED,

    /**
     * User has dismissed this match as not a duplicate.
     * The bank transaction remains available for categorisation.
     */
    DISMISSED
}
