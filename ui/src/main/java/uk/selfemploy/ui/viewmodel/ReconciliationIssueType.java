package uk.selfemploy.ui.viewmodel;

/**
 * Types of reconciliation issues.
 */
public enum ReconciliationIssueType {
    /**
     * Potential duplicate transactions.
     */
    POTENTIAL_DUPLICATES("Potential Duplicates", "Review Duplicates"),

    /**
     * Transactions without category assignments.
     */
    MISSING_CATEGORIES("Missing Categories", "Fix Categories"),

    /**
     * Gaps in income/expense dates.
     */
    DATE_GAPS("Date Gaps", "View Details"),

    /**
     * Transactions with unusual amounts.
     */
    UNUSUAL_AMOUNTS("Unusual Amounts", "Review"),

    /**
     * Incomplete records.
     */
    INCOMPLETE_RECORDS("Incomplete Records", "Complete");

    private final String displayText;
    private final String actionText;

    ReconciliationIssueType(String displayText, String actionText) {
        this.displayText = displayText;
        this.actionText = actionText;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getActionText() {
        return actionText;
    }
}
