package uk.selfemploy.common.enums;

/**
 * Type of import operation.
 *
 * <p>Identifies the type of data that was imported.</p>
 */
public enum ImportAuditType {

    /**
     * JSON file containing full data export.
     */
    JSON("JSON Import"),

    /**
     * CSV file containing income records.
     */
    CSV_INCOME("CSV Income Import"),

    /**
     * CSV file containing expense records.
     */
    CSV_EXPENSE("CSV Expense Import"),

    /**
     * Bank CSV file containing mixed income/expense.
     */
    BANK_CSV("Bank CSV Import");

    private final String displayName;

    ImportAuditType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
