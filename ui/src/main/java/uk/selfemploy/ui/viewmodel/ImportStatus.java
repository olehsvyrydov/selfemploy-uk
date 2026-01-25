package uk.selfemploy.ui.viewmodel;

/**
 * Status of an import operation.
 */
public enum ImportStatus {
    /**
     * Import is active - records exist in the system.
     */
    ACTIVE("Active", "import-status-active"),

    /**
     * Import was undone - records were removed.
     */
    UNDONE("Undone", "import-status-undone"),

    /**
     * Import is locked - cannot be undone because records are used in tax submission.
     */
    LOCKED("Locked", "import-status-locked");

    private final String displayText;
    private final String styleClass;

    ImportStatus(String displayText, String styleClass) {
        this.displayText = displayText;
        this.styleClass = styleClass;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getStyleClass() {
        return styleClass;
    }

    @Override
    public String toString() {
        return displayText;
    }
}
