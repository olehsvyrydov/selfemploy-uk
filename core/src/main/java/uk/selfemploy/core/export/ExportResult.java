package uk.selfemploy.core.export;

import java.nio.file.Path;

/**
 * Result of a data export operation.
 */
public record ExportResult(
    boolean success,
    Path filePath,
    int incomeCount,
    int expenseCount,
    String errorMessage
) {
    /**
     * Creates a successful export result.
     */
    public static ExportResult success(Path filePath, int incomeCount, int expenseCount) {
        return new ExportResult(true, filePath, incomeCount, expenseCount, null);
    }

    /**
     * Creates a failed export result.
     */
    public static ExportResult failure(String errorMessage) {
        return new ExportResult(false, null, 0, 0, errorMessage);
    }

    /**
     * Returns the total number of records exported.
     */
    public int totalCount() {
        return incomeCount + expenseCount;
    }
}
