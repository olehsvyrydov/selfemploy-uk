package uk.selfemploy.common.enums;

/**
 * Source of a transaction (income or expense).
 *
 * <p>Used to track how transactions were entered into the system:
 * <ul>
 *   <li>{@link #MANUAL} - Entered manually by the user</li>
 *   <li>{@link #BANK_IMPORT} - Imported from a bank CSV file</li>
 * </ul>
 */
public enum TransactionSource {

    /**
     * Transaction entered manually by the user.
     */
    MANUAL("Manual Entry"),

    /**
     * Transaction imported from a bank CSV file.
     */
    BANK_IMPORT("Bank Import");

    private final String displayName;

    TransactionSource(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name for this source.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
