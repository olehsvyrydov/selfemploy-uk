package uk.selfemploy.ui.viewmodel;

/**
 * Actions that can be taken for an import candidate.
 */
public enum ImportAction {
    /**
     * Import as a new record.
     */
    IMPORT("Import", "+", "import-action-import"),

    /**
     * Skip this record - do not import.
     */
    SKIP("Skip", "-", "import-action-skip"),

    /**
     * Update the existing record with this data.
     */
    UPDATE("Update Existing", "<->", "import-action-update");

    private final String displayText;
    private final String icon;
    private final String styleClass;

    ImportAction(String displayText, String icon, String styleClass) {
        this.displayText = displayText;
        this.icon = icon;
        this.styleClass = styleClass;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getIcon() {
        return icon;
    }

    public String getStyleClass() {
        return styleClass;
    }

    @Override
    public String toString() {
        return displayText;
    }
}
