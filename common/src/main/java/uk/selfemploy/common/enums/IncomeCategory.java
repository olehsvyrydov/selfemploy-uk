package uk.selfemploy.common.enums;

/**
 * Income categories aligned with HMRC SA103 form.
 *
 * These categories map to the Self-Employment (Full) SA103F
 * supplementary pages of the Self Assessment tax return.
 */
public enum IncomeCategory {

    /**
     * Turnover - the takings, fees, sales or money earned by your business.
     * SA103F Box 9
     */
    SALES("Turnover from business", "9"),

    /**
     * Other business income not included in box 9.
     * SA103F Box 10
     */
    OTHER_INCOME("Other business income", "10");

    private final String displayName;
    private final String sa103Box;

    IncomeCategory(String displayName, String sa103Box) {
        this.displayName = displayName;
        this.sa103Box = sa103Box;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSa103Box() {
        return sa103Box;
    }
}
