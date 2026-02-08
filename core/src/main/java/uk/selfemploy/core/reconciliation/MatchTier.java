package uk.selfemploy.core.reconciliation;

/**
 * Tier of confidence for reconciliation matches between bank-imported
 * and manually entered transactions.
 *
 * <p>Tiers are evaluated in priority order (LINKED first, POSSIBLE last).
 * Higher tiers have stricter matching criteria and higher confidence.</p>
 */
public enum MatchTier {

    /**
     * Tier 0: The manual Income/Expense record already has a bankTransactionId FK
     * linking it to the bank transaction. This is an intentionally linked record,
     * not a duplicate candidate.
     */
    LINKED("Already Linked", 1.0),

    /**
     * Tier 1: Same date, exact absolute amount, and normalized description
     * similarity of 1.0 (identical after normalization).
     */
    EXACT("Exact Match", 1.0),

    /**
     * Tier 2: Same date, exact absolute amount, and Levenshtein similarity
     * of 0.80 or higher.
     */
    LIKELY("Likely Match", 0.80),

    /**
     * Tier 3: Same date, amount within tolerance (1% or GBP 1.00, whichever
     * is greater). Description matching is not required at this tier.
     */
    POSSIBLE("Possible Match", 0.30);

    private final String displayName;
    private final double minimumConfidence;

    MatchTier(String displayName, double minimumConfidence) {
        this.displayName = displayName;
        this.minimumConfidence = minimumConfidence;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMinimumConfidence() {
        return minimumConfidence;
    }
}
