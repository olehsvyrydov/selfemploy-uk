package uk.selfemploy.core.bankimport;

/**
 * Result of evaluating a bank transaction against exclusion rules.
 *
 * @param shouldExclude true if the transaction matches an exclusion rule
 * @param reason        the exclusion reason (null if not excluded)
 * @param confidence    confidence level of the exclusion decision
 */
public record ExclusionResult(
    boolean shouldExclude,
    String reason,
    Confidence confidence
) {
    /** Creates a result indicating the transaction should be excluded. */
    static ExclusionResult excluded(String reason) {
        return new ExclusionResult(true, reason, Confidence.HIGH);
    }

    /** Creates a result indicating no exclusion applies. */
    static ExclusionResult notExcluded() {
        return new ExclusionResult(false, null, Confidence.LOW);
    }
}
