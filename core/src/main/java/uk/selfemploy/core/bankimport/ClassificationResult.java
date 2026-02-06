package uk.selfemploy.core.bankimport;

import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;

/**
 * Result of classifying a bank transaction.
 *
 * <p>Contains the suggested category, confidence score, and direction classification.
 * Confidence thresholds determine the UI behavior:</p>
 * <ul>
 *   <li>HIGH (&gt;90%): Auto-suggest, user confirms with one click</li>
 *   <li>MEDIUM (60-90%): Suggest with confirmation dialog</li>
 *   <li>LOW (&lt;60%): Manual categorization required</li>
 * </ul>
 */
public record ClassificationResult(
    boolean isIncome,
    ExpenseCategory suggestedCategory,
    BigDecimal confidenceScore,
    Confidence confidenceLevel
) {
    /** Threshold above which suggestions are auto-applied. */
    public static final BigDecimal HIGH_THRESHOLD = new BigDecimal("0.90");

    /** Threshold above which suggestions are shown with confirmation. */
    public static final BigDecimal MEDIUM_THRESHOLD = new BigDecimal("0.60");

    /**
     * Returns true if the confidence is high enough for auto-suggestion.
     */
    public boolean isHighConfidence() {
        return confidenceScore.compareTo(HIGH_THRESHOLD) > 0;
    }

    /**
     * Returns true if the confidence is at least medium (worthy of a suggestion).
     */
    public boolean isSuggestionWorthy() {
        return confidenceScore.compareTo(MEDIUM_THRESHOLD) >= 0;
    }

    /**
     * Returns true if manual categorization is required.
     */
    public boolean requiresManualReview() {
        return confidenceScore.compareTo(MEDIUM_THRESHOLD) < 0;
    }
}
