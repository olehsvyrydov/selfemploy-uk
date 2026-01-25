package uk.selfemploy.core.undo;

/**
 * Result of an undo operation.
 *
 * @param success true if the undo completed successfully
 * @param recordsUndone number of records that were soft-deleted
 * @param recordsSkipped number of records that were already deleted or not found
 * @param message description of the result
 */
public record UndoResult(
    boolean success,
    int recordsUndone,
    int recordsSkipped,
    String message
) {
    /**
     * Creates a successful undo result.
     */
    public static UndoResult success(int recordsUndone, int recordsSkipped) {
        String message = String.format(
            "Successfully undone import. %d records soft-deleted, %d skipped.",
            recordsUndone, recordsSkipped
        );
        return new UndoResult(true, recordsUndone, recordsSkipped, message);
    }

    /**
     * Creates a failed undo result.
     */
    public static UndoResult failure(String reason) {
        return new UndoResult(false, 0, 0, reason);
    }
}
