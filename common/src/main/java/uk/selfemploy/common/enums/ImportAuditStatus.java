package uk.selfemploy.common.enums;

/**
 * Status of an import audit record.
 *
 * <p>Import audit records track the lifecycle of import operations:</p>
 * <ul>
 *   <li>ACTIVE - Import completed successfully, records are active</li>
 *   <li>UNDONE - Import was undone, records are soft-deleted</li>
 * </ul>
 */
public enum ImportAuditStatus {

    /**
     * Import completed and records are active.
     */
    ACTIVE("Active", "Records from this import are active"),

    /**
     * Import was undone and records are soft-deleted.
     */
    UNDONE("Undone", "Records from this import have been soft-deleted");

    private final String displayName;
    private final String description;

    ImportAuditStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this status allows undo.
     */
    public boolean canUndo() {
        return this == ACTIVE;
    }
}
