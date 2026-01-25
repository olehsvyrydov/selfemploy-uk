package uk.selfemploy.plugin.extension;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of a data import operation.
 *
 * <p>This record provides details about the outcome of importing transactions,
 * including counts of successfully imported, skipped, and duplicate records,
 * as well as any errors encountered.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ImportResult result = importer.importData(file, context);
 * if (result.hasErrors()) {
 *     for (String error : result.errors()) {
 *         log.error("Import error: {}", error);
 *     }
 * }
 * log.info("Imported {} transactions, skipped {} duplicates",
 *     result.importedCount(), result.duplicateCount());
 * }</pre>
 *
 * @param importedCount  number of successfully imported transactions
 * @param skippedCount   number of transactions skipped (e.g., invalid data)
 * @param duplicateCount number of duplicate transactions detected
 * @param errors         list of error messages for failed records
 * @param warnings       list of warning messages for potential issues
 *
 * @see DataImporter
 */
public record ImportResult(
    int importedCount,
    int skippedCount,
    int duplicateCount,
    List<String> errors,
    List<String> warnings
) {

    /**
     * Constructs an ImportResult with validation.
     *
     * @param importedCount  number of successfully imported transactions
     * @param skippedCount   number of transactions skipped
     * @param duplicateCount number of duplicate transactions detected
     * @param errors         list of error messages (may be null)
     * @param warnings       list of warning messages (may be null)
     * @throws IllegalArgumentException if counts are negative
     */
    public ImportResult {
        if (importedCount < 0) {
            throw new IllegalArgumentException("Imported count must not be negative");
        }
        if (skippedCount < 0) {
            throw new IllegalArgumentException("Skipped count must not be negative");
        }
        if (duplicateCount < 0) {
            throw new IllegalArgumentException("Duplicate count must not be negative");
        }
        errors = errors == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(errors);
        warnings = warnings == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(warnings);
    }

    /**
     * Creates a successful import result.
     *
     * @param imported   number of imported transactions
     * @param duplicates number of duplicates skipped
     * @return a successful result
     */
    public static ImportResult success(int imported, int duplicates) {
        return new ImportResult(imported, 0, duplicates, null, null);
    }

    /**
     * Creates a result indicating complete failure.
     *
     * @param error the error message
     * @return a failure result
     */
    public static ImportResult failure(String error) {
        return new ImportResult(0, 0, 0, List.of(error), null);
    }

    /**
     * Returns whether any errors occurred during import.
     *
     * @return true if there are errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Returns whether any warnings occurred during import.
     *
     * @return true if there are warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Returns the total number of records processed.
     *
     * @return total processed count
     */
    public int totalProcessed() {
        return importedCount + skippedCount + duplicateCount;
    }
}
