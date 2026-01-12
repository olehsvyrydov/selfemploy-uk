package uk.selfemploy.common.enums;

/**
 * Types of business entities supported by the application.
 *
 * MVP focuses on sole traders and contractors, with PARTNERSHIP and LIMITED_COMPANY
 * planned for future phases.
 */
public enum BusinessType {

    /**
     * Sole trader - self-employed individual working independently.
     * Uses SA103 for Self Assessment.
     */
    SOLE_TRADER("Sole Trader / Freelancer", "SA103", true),

    /**
     * Freelancer - self-employed working on projects/gigs.
     * Uses SA103 for Self Assessment.
     */
    FREELANCER("Freelancer", "SA103", true),

    /**
     * Contractor - providing services to businesses.
     * Uses SA103 for Self Assessment.
     */
    CONTRACTOR("Contractor", "SA103", true),

    /**
     * Sole trader / self-employed individual (legacy).
     * @deprecated Use SOLE_TRADER instead
     */
    @Deprecated
    SELF_EMPLOYED("Self-Employed", "SA103", true),

    /**
     * Partnership business.
     * Uses SA104 for Partnership Self Assessment.
     * Coming soon - Phase 2.
     */
    PARTNERSHIP("Partnership", "SA104", false),

    /**
     * Limited company.
     * Uses CT600 for Corporation Tax.
     * Coming soon - Phase 3.
     */
    LIMITED_COMPANY("Limited Company", "CT600", false);

    private final String displayName;
    private final String taxFormCode;
    private final boolean enabled;

    BusinessType(String displayName, String taxFormCode, boolean enabled) {
        this.displayName = displayName;
        this.taxFormCode = taxFormCode;
        this.enabled = enabled;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTaxFormCode() {
        return taxFormCode;
    }

    /**
     * Returns whether this business type is currently enabled.
     * Disabled types are coming in future phases.
     */
    public boolean isEnabled() {
        return enabled;
    }
}
