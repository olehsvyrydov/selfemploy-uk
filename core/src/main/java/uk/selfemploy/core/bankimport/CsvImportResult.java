package uk.selfemploy.core.bankimport;

import uk.selfemploy.common.domain.ImportBatch;

import java.util.UUID;

/**
 * Result of a CSV import operation.
 *
 * @param importBatchId the ID of the created import batch
 * @param bankName the detected or specified bank name
 * @param totalTransactions total number of transactions in the file
 * @param incomeCount number of income transactions imported
 * @param expenseCount number of expense transactions imported
 * @param duplicateCount number of duplicate transactions skipped
 * @param successCount total number of successfully imported transactions
 */
public record CsvImportResult(
    UUID importBatchId,
    String bankName,
    int totalTransactions,
    int incomeCount,
    int expenseCount,
    int duplicateCount,
    int successCount
) {
    /**
     * Creates a result from an ImportBatch.
     */
    public static CsvImportResult fromBatch(ImportBatch batch) {
        return new CsvImportResult(
            batch.id(),
            batch.bankName(),
            batch.totalTransactions(),
            batch.incomeCount(),
            batch.expenseCount(),
            batch.duplicateCount(),
            batch.incomeCount() + batch.expenseCount()
        );
    }

    /**
     * Returns true if any transactions were imported.
     */
    public boolean hasImportedTransactions() {
        return successCount > 0;
    }

    /**
     * Returns true if there were duplicates.
     */
    public boolean hasDuplicates() {
        return duplicateCount > 0;
    }
}
