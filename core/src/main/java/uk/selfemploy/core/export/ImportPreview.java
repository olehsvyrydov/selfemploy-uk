package uk.selfemploy.core.export;

import java.util.List;

/**
 * Preview of data to be imported, with validation results.
 */
public record ImportPreview(
    int recordCount,
    int validRecordCount,
    int invalidRecordCount,
    List<String> warnings,
    List<String> errors,
    boolean isValid
) {
    /**
     * Creates a successful preview with all valid records.
     */
    public static ImportPreview valid(int recordCount, List<String> warnings) {
        return new ImportPreview(recordCount, recordCount, 0, warnings, List.of(), true);
    }

    /**
     * Creates a preview with some invalid records.
     */
    public static ImportPreview partial(int validCount, int invalidCount,
            List<String> warnings, List<String> errors) {
        return new ImportPreview(
            validCount + invalidCount,
            validCount,
            invalidCount,
            warnings,
            errors,
            false
        );
    }

    /**
     * Creates a completely invalid preview (file structure issues).
     */
    public static ImportPreview invalid(List<String> errors) {
        return new ImportPreview(0, 0, 0, List.of(), errors, false);
    }
}
