package uk.selfemploy.common.enums;

/**
 * Types of modifications tracked in the transaction modification log.
 *
 * <p>Records all changes to bank transactions after import for audit trail purposes.
 * This supports the digital link chain required for MTD compliance.</p>
 */
public enum ModificationType {

    /** Transaction categorized as income or expense for the first time. */
    CATEGORIZED("Categorized"),

    /** Transaction excluded from processing. */
    EXCLUDED("Excluded"),

    /** Transaction re-categorized after initial categorization. */
    RECATEGORIZED("Recategorized"),

    /** Previously excluded or categorized transaction restored to pending. */
    RESTORED("Restored"),

    /** Business/personal flag changed. */
    BUSINESS_PERSONAL_CHANGED("Business/Personal Changed"),

    /** Expense or income category changed. */
    CATEGORY_CHANGED("Category Changed");

    private final String displayName;

    ModificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
