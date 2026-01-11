package uk.selfemploy.ui.viewmodel;

/**
 * Type of imported transaction - income or expense.
 *
 * SE-601: CSV Bank Import Wizard
 */
public enum TransactionType {

    /**
     * Income transaction (money received).
     * Positive amount in bank statement.
     */
    INCOME("Income", "income"),

    /**
     * Expense transaction (money paid out).
     * Negative amount in bank statement.
     */
    EXPENSE("Expense", "expense");

    private final String displayName;
    private final String cssClass;

    TransactionType(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns CSS class for styling (type-badge-income or type-badge-expense).
     */
    public String getBadgeCssClass() {
        return "type-badge-" + cssClass;
    }
}
