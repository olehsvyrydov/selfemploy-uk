package uk.selfemploy.core.bankimport;

import java.util.UUID;

/**
 * Result of a bank statement import operation.
 *
 * <p>Transactions are staged for review rather than committed directly
 * to income/expense records.</p>
 */
public record BankStatementImportResult(
    UUID importAuditId,
    String bankName,
    int totalParsed,
    int importedCount,
    int duplicateCount,
    int skippedCount
) {
    /**
     * Returns the number of new transactions ready for review.
     */
    public int pendingReviewCount() {
        return importedCount;
    }
}
