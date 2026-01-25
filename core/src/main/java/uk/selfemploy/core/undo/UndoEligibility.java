package uk.selfemploy.core.undo;

/**
 * Result of checking whether an import can be undone.
 *
 * <p>Implements COND-F11: UndoEligibility check before any undo operation.</p>
 *
 * @param eligible true if the import can be undone
 * @param reason explanation of why undo is not allowed (null if eligible)
 */
public record UndoEligibility(
    boolean eligible,
    String reason
) {
    /**
     * Creates an eligible result.
     */
    public static UndoEligibility allowed() {
        return new UndoEligibility(true, null);
    }

    /**
     * Creates a blocked result with a reason.
     */
    public static UndoEligibility blocked(String reason) {
        return new UndoEligibility(false, reason);
    }
}
