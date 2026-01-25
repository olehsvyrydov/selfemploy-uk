package uk.selfemploy.core.undo;

/**
 * Exception thrown when an undo operation is blocked.
 *
 * <p>This exception is thrown when an import cannot be undone due to:</p>
 * <ul>
 *   <li>Import is older than the 7-day undo window (ADR-10B-004)</li>
 *   <li>Import has already been undone</li>
 *   <li>Records were used in a non-DRAFT tax submission (COND-F1, COND-F2)</li>
 * </ul>
 */
public class UndoBlockedException extends RuntimeException {

    private final String reason;

    public UndoBlockedException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
