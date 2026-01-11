package uk.selfemploy.ui.viewmodel;

/**
 * Status indicator for imported transactions in the preview table.
 *
 * SE-601: CSV Bank Import Wizard
 */
public enum TransactionStatus {

    /**
     * Transaction is valid and ready for import.
     */
    OK("OK", "row-status-ok"),

    /**
     * Transaction has a warning (e.g., uncategorized, low confidence).
     */
    WARNING("Warning", "row-status-warning"),

    /**
     * Transaction is a duplicate and will be skipped.
     */
    DUPLICATE("Duplicate", "row-status-duplicate");

    private final String displayName;
    private final String cssClass;

    TransactionStatus(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClass() {
        return cssClass;
    }

    /**
     * Returns the status icon character.
     */
    public String getIcon() {
        return switch (this) {
            case OK -> "[check]";
            case WARNING -> "[!]";
            case DUPLICATE -> "[x]";
        };
    }
}
