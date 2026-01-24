package uk.selfemploy.ui.help;

/**
 * Enumeration of help topics available in the application.
 *
 * <p>SE-701: In-App Help System</p>
 *
 * <p>Each topic maps to a specific help content entry
 * managed by {@link HelpService}.</p>
 */
public enum HelpTopic {

    // === Tax Summary Topics ===

    /**
     * Help about Net Profit calculation (Box 31).
     */
    NET_PROFIT,

    /**
     * Help about Income Tax calculation and bands.
     */
    INCOME_TAX,

    /**
     * Help about Personal Allowance and taper.
     */
    PERSONAL_ALLOWANCE,

    /**
     * Help about National Insurance Class 4.
     */
    NI_CLASS_4,

    /**
     * Help about National Insurance Class 2.
     */
    NI_CLASS_2,

    /**
     * Help about Payments on Account.
     */
    PAYMENTS_ON_ACCOUNT,

    // === Income Topics ===

    /**
     * Help about paid income (received payments).
     */
    PAID_INCOME,

    /**
     * Help about unpaid income (outstanding invoices).
     */
    UNPAID_INCOME,

    // === Expense Topics ===

    /**
     * Help about expense categories and SA103 mapping.
     */
    EXPENSE_CATEGORY,

    /**
     * Help about allowable expenses rules.
     */
    ALLOWABLE_EXPENSES,

    /**
     * Help about non-deductible expenses.
     */
    NON_DEDUCTIBLE_EXPENSES,

    // === Submission Topics ===

    /**
     * Help about the declaration checkbox and its implications.
     */
    DECLARATION,

    /**
     * Help about HMRC submission process.
     */
    HMRC_SUBMISSION,

    // === General Topics ===

    /**
     * Help about the tax year and deadlines.
     */
    TAX_YEAR,

    /**
     * Help about SA103 form in general.
     */
    SA103_FORM
}
