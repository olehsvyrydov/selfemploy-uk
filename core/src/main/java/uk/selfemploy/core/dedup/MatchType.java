package uk.selfemploy.core.dedup;

/**
 * Type of duplicate match detected during import analysis.
 *
 * <p>Three-tier detection system per ADR-10B-003:</p>
 * <ul>
 *   <li>EXACT - Same date + amount + normalized description</li>
 *   <li>LIKELY - Same date + amount + similar description (Levenshtein &lt;= 3)</li>
 *   <li>DATE_ONLY - Same date + similar amount (within 5%)</li>
 *   <li>NONE - No match detected</li>
 * </ul>
 */
public enum MatchType {

    /**
     * Exact match: same date, amount, and normalized description.
     * Confidence: HIGH (1.0)
     * Suggested action: SKIP
     */
    EXACT("Exact Match", 1.0, "Same date, amount, and description"),

    /**
     * Likely match: same date and amount, similar description.
     * Confidence: MEDIUM (0.6-0.9)
     * Suggested action: REVIEW
     */
    LIKELY("Likely Match", 0.8, "Same date and amount, similar description"),

    /**
     * Date-only match: same date, different amount or description.
     * Confidence: LOW (0.1-0.4)
     * Suggested action: IMPORT with warning
     */
    DATE_ONLY("Possible Match", 0.3, "Same date, different amount or description"),

    /**
     * No match found.
     * Suggested action: IMPORT
     */
    NONE("No Match", 0.0, "No matching records found");

    private final String displayName;
    private final double defaultConfidence;
    private final String description;

    MatchType(String displayName, double defaultConfidence, String description) {
        this.displayName = displayName;
        this.defaultConfidence = defaultConfidence;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getDefaultConfidence() {
        return defaultConfidence;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns true if this match type suggests skipping the import.
     */
    public boolean shouldSkip() {
        return this == EXACT;
    }

    /**
     * Returns true if this match type requires user review.
     */
    public boolean requiresReview() {
        return this == LIKELY;
    }

    /**
     * Returns true if this record can be imported (with or without warning).
     */
    public boolean canImport() {
        return this == NONE || this == DATE_ONLY;
    }
}
