package uk.selfemploy.common.enums;

/**
 * Expense categories aligned with HMRC SA103 form categories.
 *
 * These categories map directly to the Self-Employment (Full) SA103F
 * supplementary pages of the Self Assessment tax return.
 */
public enum ExpenseCategory {

    // SA103 Box Categories

    /**
     * Cost of goods bought for resale or goods used.
     * SA103F Box 10
     */
    COST_OF_GOODS("Cost of goods bought for resale", "10", true),

    /**
     * Construction industry - payments to subcontractors.
     * SA103F Box 11
     */
    SUBCONTRACTOR_COSTS("Construction industry subcontractor costs", "11", true),

    /**
     * Wages, salaries and other staff costs.
     * SA103F Box 12
     */
    STAFF_COSTS("Wages, salaries and other staff costs", "12", true),

    /**
     * Car, van and travel expenses.
     * SA103F Box 13
     */
    TRAVEL("Car, van and travel expenses", "13", true),

    /**
     * Rent, rates, power and insurance costs.
     * SA103F Box 14
     */
    PREMISES("Rent, rates, power and insurance costs", "14", true),

    /**
     * Repairs and maintenance of property and equipment.
     * SA103F Box 15
     */
    REPAIRS("Repairs and maintenance", "15", true),

    /**
     * Phone, fax, stationery and other office costs.
     * SA103F Box 16
     */
    OFFICE_COSTS("Phone, fax, stationery and office costs", "16", true),

    /**
     * Advertising and business entertainment costs.
     * SA103F Box 17
     */
    ADVERTISING("Advertising and business entertainment", "17", true),

    /**
     * Interest on bank and other business loans.
     * SA103F Box 18
     */
    INTEREST("Interest on business loans", "18", true),

    /**
     * Bank, credit card and other financial charges.
     * SA103F Box 19
     */
    FINANCIAL_CHARGES("Bank, credit card and financial charges", "19", true),

    /**
     * Irrecoverable debts written off.
     * SA103F Box 20
     */
    BAD_DEBTS("Irrecoverable debts written off", "20", true),

    /**
     * Accountancy, legal and other professional fees.
     * SA103F Box 21
     */
    PROFESSIONAL_FEES("Accountancy, legal and professional fees", "21", true),

    /**
     * Depreciation and loss/profit on sale of assets.
     * SA103F Box 22 (not allowable, for info only)
     */
    DEPRECIATION("Depreciation", "22", false),

    /**
     * Other business expenses.
     * SA103F Box 23
     */
    OTHER_EXPENSES("Other business expenses", "23", true),

    /**
     * Use of home as office (simplified expenses or actual).
     * Calculated separately
     */
    HOME_OFFICE("Use of home as office", "N/A", true);

    private final String displayName;
    private final String sa103Box;
    private final boolean allowable;

    ExpenseCategory(String displayName, String sa103Box, boolean allowable) {
        this.displayName = displayName;
        this.sa103Box = sa103Box;
        this.allowable = allowable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSa103Box() {
        return sa103Box;
    }

    /**
     * Whether this expense is allowable for tax deduction.
     */
    public boolean isAllowable() {
        return allowable;
    }
}
