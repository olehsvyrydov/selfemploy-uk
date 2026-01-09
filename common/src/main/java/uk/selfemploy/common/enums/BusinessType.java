package uk.selfemploy.common.enums;

/**
 * Types of business entities supported by the application.
 *
 * MVP focuses on SELF_EMPLOYED, with PARTNERSHIP and LIMITED_COMPANY
 * planned for future phases.
 */
public enum BusinessType {

    /**
     * Sole trader / self-employed individual.
     * Uses SA103 for Self Assessment.
     */
    SELF_EMPLOYED("Self-Employed", "SA103"),

    /**
     * Partnership business.
     * Uses SA104 for Partnership Self Assessment.
     */
    PARTNERSHIP("Partnership", "SA104"),

    /**
     * Limited company.
     * Uses CT600 for Corporation Tax.
     */
    LIMITED_COMPANY("Limited Company", "CT600");

    private final String displayName;
    private final String taxFormCode;

    BusinessType(String displayName, String taxFormCode) {
        this.displayName = displayName;
        this.taxFormCode = taxFormCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTaxFormCode() {
        return taxFormCode;
    }
}
