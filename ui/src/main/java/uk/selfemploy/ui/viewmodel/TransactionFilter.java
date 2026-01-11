package uk.selfemploy.ui.viewmodel;

/**
 * Filter options for the transaction preview table.
 *
 * SE-601: CSV Bank Import Wizard
 */
public enum TransactionFilter {

    /**
     * Show all transactions.
     */
    ALL("All"),

    /**
     * Show only income transactions.
     */
    INCOME_ONLY("Income Only"),

    /**
     * Show only expense transactions.
     */
    EXPENSES_ONLY("Expenses Only"),

    /**
     * Show only transactions without a category.
     */
    UNCATEGORIZED("Uncategorized"),

    /**
     * Show only duplicate transactions.
     */
    DUPLICATES("Duplicates");

    private final String displayName;

    TransactionFilter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
