package uk.selfemploy.ui.viewmodel;

/**
 * Defines how to interpret the sign of amount values in bank statements.
 * Different banks use different conventions for positive/negative amounts.
 *
 * SE-802: Bank Import Column Mapping Wizard
 */
public enum AmountInterpretation {

    /**
     * Standard interpretation: positive values = income, negative values = expense.
     * Most common format used by modern digital banks.
     */
    STANDARD("Standard", "Positive = Income, Negative = Expense"),

    /**
     * Inverted interpretation: positive values = expense (debit), negative values = income (credit).
     * Used by some traditional banks that show debits as positive.
     */
    INVERTED("Inverted", "Positive = Expense, Negative = Income"),

    /**
     * Separate columns: income and expense amounts are in different columns.
     * Common in UK high street bank exports (Barclays, HSBC, Lloyds).
     */
    SEPARATE_COLUMNS("Separate Columns", "Income and Expense in different columns");

    private final String displayName;
    private final String description;

    AmountInterpretation(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
