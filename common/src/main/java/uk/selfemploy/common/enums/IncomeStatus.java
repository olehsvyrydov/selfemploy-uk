package uk.selfemploy.common.enums;

/**
 * Payment status for income entries.
 *
 * Tracks whether an income has been received (paid) or is still outstanding (unpaid).
 */
public enum IncomeStatus {

    /**
     * Payment has been received for this income.
     */
    PAID("Paid"),

    /**
     * Payment is still outstanding for this income (e.g., pending invoice).
     */
    UNPAID("Unpaid");

    private final String displayName;

    IncomeStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
