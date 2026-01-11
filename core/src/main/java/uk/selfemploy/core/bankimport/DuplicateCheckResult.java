package uk.selfemploy.core.bankimport;

import java.util.List;

/**
 * Result of duplicate detection on a list of imported transactions.
 *
 * @param uniqueTransactions transactions that have no existing duplicates
 * @param duplicateTransactions transactions that match existing records
 */
public record DuplicateCheckResult(
    List<ImportedTransaction> uniqueTransactions,
    List<ImportedTransaction> duplicateTransactions
) {
    /**
     * Returns the total number of transactions checked.
     */
    public int totalChecked() {
        return uniqueTransactions.size() + duplicateTransactions.size();
    }

    /**
     * Returns true if there are any duplicates.
     */
    public boolean hasDuplicates() {
        return !duplicateTransactions.isEmpty();
    }

    /**
     * Returns the number of unique transactions.
     */
    public int uniqueCount() {
        return uniqueTransactions.size();
    }

    /**
     * Returns the number of duplicate transactions.
     */
    public int duplicateCount() {
        return duplicateTransactions.size();
    }
}
