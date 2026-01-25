package uk.selfemploy.core.export;

import java.nio.file.Path;

/**
 * Result of a combined data export operation that produces multiple files.
 */
public record CombinedExportResult(
    boolean success,
    Path incomeFilePath,
    Path expenseFilePath,
    Path summaryFilePath,
    int incomeCount,
    int expenseCount,
    String errorMessage
) {
    /**
     * Creates a successful combined export result.
     */
    public static CombinedExportResult success(
            Path incomeFilePath,
            Path expenseFilePath,
            Path summaryFilePath,
            int incomeCount,
            int expenseCount) {
        return new CombinedExportResult(
            true, incomeFilePath, expenseFilePath, summaryFilePath,
            incomeCount, expenseCount, null
        );
    }

    /**
     * Creates a failed combined export result.
     */
    public static CombinedExportResult failure(String errorMessage) {
        return new CombinedExportResult(false, null, null, null, 0, 0, errorMessage);
    }

    /**
     * Returns the total number of records exported.
     */
    public int totalCount() {
        return incomeCount + expenseCount;
    }
}
