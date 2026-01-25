package uk.selfemploy.core.export;

import java.util.List;

/**
 * Result of a data import operation.
 */
public record ImportResult(
    boolean success,
    int importedCount,
    int skippedCount,
    int duplicateCount,
    int errorCount,
    List<String> errors
) {
    /**
     * Creates a successful import result.
     */
    public static ImportResult success(int importedCount, int skippedCount, int duplicateCount) {
        return new ImportResult(true, importedCount, skippedCount, duplicateCount, 0, List.of());
    }

    /**
     * Creates a partially successful import result.
     */
    public static ImportResult partial(int importedCount, int skippedCount,
            int duplicateCount, int errorCount, List<String> errors) {
        return new ImportResult(importedCount > 0, importedCount, skippedCount,
            duplicateCount, errorCount, errors);
    }

    /**
     * Creates a failed import result.
     */
    public static ImportResult failure(String errorMessage) {
        return new ImportResult(false, 0, 0, 0, 1, List.of(errorMessage));
    }

    /**
     * Returns total records processed.
     */
    public int totalProcessed() {
        return importedCount + skippedCount + duplicateCount + errorCount;
    }
}
