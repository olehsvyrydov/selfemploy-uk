package uk.selfemploy.common.enums;

/**
 * Types of HMRC submissions for self-employment.
 *
 * <p>MTD requires quarterly updates and an annual self assessment.</p>
 *
 * <p>Quarters follow the UK tax year:</p>
 * <ul>
 *   <li>Q1: 6 April - 5 July</li>
 *   <li>Q2: 6 July - 5 October</li>
 *   <li>Q3: 6 October - 5 January</li>
 *   <li>Q4: 6 January - 5 April</li>
 * </ul>
 */
public enum SubmissionType {

    /**
     * Quarter 1 (6 April - 5 July).
     */
    QUARTERLY_Q1("Q1", "Quarter 1 (Apr-Jul)"),

    /**
     * Quarter 2 (6 July - 5 October).
     */
    QUARTERLY_Q2("Q2", "Quarter 2 (Jul-Oct)"),

    /**
     * Quarter 3 (6 October - 5 January).
     */
    QUARTERLY_Q3("Q3", "Quarter 3 (Oct-Jan)"),

    /**
     * Quarter 4 (6 January - 5 April).
     */
    QUARTERLY_Q4("Q4", "Quarter 4 (Jan-Apr)"),

    /**
     * Annual Self Assessment (SA100).
     */
    ANNUAL("Annual", "Annual Self Assessment");

    private final String shortName;
    private final String displayName;

    SubmissionType(String shortName, String displayName) {
        this.shortName = shortName;
        this.displayName = displayName;
    }

    /**
     * Returns the short name (e.g., "Q1", "Annual").
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Returns the full display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this is a quarterly submission type.
     */
    public boolean isQuarterly() {
        return this != ANNUAL;
    }

    /**
     * Returns the quarter number (1-4) for quarterly types, or 0 for annual.
     */
    public int getQuarterNumber() {
        return switch (this) {
            case QUARTERLY_Q1 -> 1;
            case QUARTERLY_Q2 -> 2;
            case QUARTERLY_Q3 -> 3;
            case QUARTERLY_Q4 -> 4;
            case ANNUAL -> 0;
        };
    }
}
