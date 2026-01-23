package uk.selfemploy.common.enums;

/**
 * Expense categories aligned with HMRC SA103F form categories (2024-25 tax year).
 *
 * These categories map directly to the Self-Employment (Full) SA103F
 * supplementary pages of the Self Assessment tax return.
 *
 * <p>Box numbers correspond to SA103F 2024-25 (boxes 17-30 for expenses).</p>
 *
 * @see <a href="https://www.gov.uk/government/publications/self-assessment-self-employment-full-sa103f">HMRC SA103F</a>
 */
public enum ExpenseCategory {

    // SA103F Box Categories (2024-25)

    /**
     * Cost of goods bought for resale or goods used.
     * SA103F Box 17
     */
    COST_OF_GOODS("Cost of goods bought for resale", "17", true, false),

    /**
     * Construction industry - payments to subcontractors.
     * SA103F Box 18
     * <p>Only applicable to businesses registered for Construction Industry Scheme (CIS).</p>
     */
    SUBCONTRACTOR_COSTS("Construction industry subcontractor costs", "18", true, true),

    /**
     * Wages, salaries and other staff costs.
     * SA103F Box 19
     * <p>Includes employer's NICs, agency fees, and staff entertainment (up to £150/head/year).</p>
     */
    STAFF_COSTS("Wages, salaries and other staff costs", "19", true, false),

    /**
     * Car, van and travel expenses - actual costs method.
     * SA103F Box 20
     * <p>Use this for actual vehicle costs (fuel, insurance, repairs, etc.).
     * For mileage rate method, use {@link #TRAVEL_MILEAGE} instead.</p>
     */
    TRAVEL("Car, van and travel expenses (actual costs)", "20", true, false),

    /**
     * Car, van and travel expenses - simplified mileage rate method.
     * SA103F Box 20
     * <p>Rates (2024-25): Cars 45p/mile (first 10,000), 25p/mile after.
     * Motorcycles 24p/mile. Bicycles 20p/mile.</p>
     * <p><strong>Warning:</strong> Once you use mileage rates for a vehicle,
     * you must continue using them for that vehicle's entire business use.</p>
     */
    TRAVEL_MILEAGE("Car, van and travel expenses (mileage rate)", "20", true, false),

    /**
     * Rent, rates, power and insurance costs.
     * SA103F Box 21
     * <p>For business premises. Home office actual costs also go here.</p>
     */
    PREMISES("Rent, rates, power and insurance costs", "21", true, false),

    /**
     * Repairs and maintenance of property and equipment.
     * SA103F Box 22
     */
    REPAIRS("Repairs and maintenance", "22", true, false),

    /**
     * Phone, fax, stationery and other office costs.
     * SA103F Box 23
     * <p>Includes postage, printing, and small office equipment/software.</p>
     */
    OFFICE_COSTS("Phone, stationery and office costs", "23", true, false),

    /**
     * Advertising and marketing costs.
     * SA103F Box 24
     * <p><strong>Note:</strong> Business entertainment is NOT allowable.
     * Use {@link #BUSINESS_ENTERTAINMENT} to track entertainment expenses separately.</p>
     */
    ADVERTISING("Advertising and marketing costs", "24", true, false),

    /**
     * Interest on bank and other business loans.
     * SA103F Box 25
     * <p><strong>Note:</strong> Not available under Cash Basis accounting for property loans.</p>
     */
    INTEREST("Interest on business loans", "25", true, false),

    /**
     * Bank, credit card and other financial charges.
     * SA103F Box 26
     */
    FINANCIAL_CHARGES("Bank, credit card and financial charges", "26", true, false),

    /**
     * Irrecoverable debts written off.
     * SA103F Box 27
     * <p><strong>Note:</strong> Not available under Cash Basis for credit sales already recorded.</p>
     */
    BAD_DEBTS("Irrecoverable debts written off", "27", true, false),

    /**
     * Accountancy, legal and other professional fees.
     * SA103F Box 28
     * <p>Includes professional indemnity insurance premiums.</p>
     */
    PROFESSIONAL_FEES("Accountancy, legal and professional fees", "28", true, false),

    /**
     * Depreciation and loss/profit on sale of assets.
     * SA103F Box 29 - NOT ALLOWABLE
     * <p>Depreciation is never allowable for tax. Instead, claim Capital Allowances
     * (boxes 49-52) such as Annual Investment Allowance.</p>
     */
    DEPRECIATION("Depreciation (not allowable)", "29", false, false),

    /**
     * Other business expenses.
     * SA103F Box 30
     * <p>Includes trade subscriptions, sundry expenses, and net VAT payments.</p>
     */
    OTHER_EXPENSES("Other business expenses", "30", true, false),

    /**
     * Use of home as office - simplified flat rate method.
     * Aggregated to SA103F Box 30
     * <p>Flat rates (2024-25): 25-50 hrs/month: £10, 51-100 hrs: £18, 101+ hrs: £26.</p>
     * <p>For actual home office costs, use {@link #PREMISES} instead.</p>
     */
    HOME_OFFICE_SIMPLIFIED("Use of home as office (flat rate)", "30", true, false),

    /**
     * Business entertainment expenses - NOT ALLOWABLE.
     * Tracked for Box 24 but not tax-deductible.
     * <p>Business entertainment (client meals, hospitality) is NOT an allowable expense.
     * Only staff entertainment (up to £150/head/year) is allowable under {@link #STAFF_COSTS}.</p>
     */
    BUSINESS_ENTERTAINMENT("Business entertainment (not allowable)", "24", false, false);

    private final String displayName;
    private final String sa103Box;
    private final boolean allowable;
    private final boolean cisOnly;

    ExpenseCategory(String displayName, String sa103Box, boolean allowable, boolean cisOnly) {
        this.displayName = displayName;
        this.sa103Box = sa103Box;
        this.allowable = allowable;
        this.cisOnly = cisOnly;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a shorter, user-friendly display name for UI lists and tables.
     */
    public String getShortDisplayName() {
        return switch (this) {
            case COST_OF_GOODS -> "Cost of Goods";
            case SUBCONTRACTOR_COSTS -> "Subcontractor Costs";
            case STAFF_COSTS -> "Staff Costs";
            case TRAVEL -> "Travel (Actual)";
            case TRAVEL_MILEAGE -> "Travel (Mileage)";
            case PREMISES -> "Premises";
            case REPAIRS -> "Repairs";
            case OFFICE_COSTS -> "Office Costs";
            case ADVERTISING -> "Advertising";
            case INTEREST -> "Interest";
            case FINANCIAL_CHARGES -> "Financial Charges";
            case BAD_DEBTS -> "Bad Debts";
            case PROFESSIONAL_FEES -> "Professional Fees";
            case DEPRECIATION -> "Depreciation";
            case OTHER_EXPENSES -> "Other Expenses";
            case HOME_OFFICE_SIMPLIFIED -> "Home Office";
            case BUSINESS_ENTERTAINMENT -> "Entertainment";
        };
    }

    public String getSa103Box() {
        return sa103Box;
    }

    /**
     * Whether this expense is allowable for tax deduction.
     * <p>Non-allowable expenses (e.g., depreciation, business entertainment)
     * should still be recorded for completeness but are excluded from tax calculations.</p>
     */
    public boolean isAllowable() {
        return allowable;
    }

    /**
     * Whether this category is only applicable to Construction Industry Scheme (CIS) businesses.
     * <p>CIS-only categories should be hidden/disabled for non-CIS businesses.</p>
     */
    public boolean isCisOnly() {
        return cisOnly;
    }
}
