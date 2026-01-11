package uk.selfemploy.ui.viewmodel;

/**
 * Supported UK bank formats for CSV import.
 * Each bank has a unique column structure that can be auto-detected.
 *
 * SE-601: CSV Bank Import Wizard
 */
public enum BankFormat {

    /**
     * Barclays Bank - columns: Date, Type, Description, Money out, Money in, Balance
     */
    BARCLAYS("Barclays", "Date, Type, Description, Money out, Money in, Balance"),

    /**
     * HSBC Bank - columns: Date, Type, Paid out, Paid in, Balance
     */
    HSBC("HSBC", "Date, Type, Paid out, Paid in, Balance"),

    /**
     * Lloyds Bank - columns: Transaction Date, Transaction Type, Sort Code,
     * Account Number, Transaction Description, Debit Amount, Credit Amount, Balance
     */
    LLOYDS("Lloyds", "Transaction Date, Transaction Type, Sort Code, Account Number, Transaction Description, Debit Amount, Credit Amount, Balance"),

    /**
     * Nationwide Building Society - columns: Date, Transaction type, Description,
     * Paid out, Paid in, Balance
     */
    NATIONWIDE("Nationwide", "Date, Transaction type, Description, Paid out, Paid in, Balance"),

    /**
     * Starling Bank - columns: Date, Counter Party, Reference, Type, Amount (GBP), Balance (GBP)
     */
    STARLING("Starling", "Date, Counter Party, Reference, Type, Amount (GBP), Balance (GBP)"),

    /**
     * Monzo Bank - columns: Transaction ID, Date, Time, Type, Name, Emoji, Category, Amount, Currency, Notes
     */
    MONZO("Monzo", "Transaction ID, Date, Time, Type, Name, Emoji, Category, Amount, Currency, Notes"),

    /**
     * Unknown or custom format - requires manual column mapping
     */
    UNKNOWN("Unknown", "Custom format - manual mapping required");

    private final String displayName;
    private final String columnStructure;

    BankFormat(String displayName, String columnStructure) {
        this.displayName = displayName;
        this.columnStructure = columnStructure;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColumnStructure() {
        return columnStructure;
    }

    /**
     * Returns a user-friendly detection message.
     */
    public String getDetectionMessage() {
        if (this == UNKNOWN) {
            return "Unknown bank format";
        }
        return displayName + " format detected";
    }

    /**
     * Returns hint text for unknown format.
     */
    public String getHintText() {
        if (this == UNKNOWN) {
            return "You'll need to map columns manually in the next step";
        }
        return "Column mapping will be applied automatically";
    }
}
