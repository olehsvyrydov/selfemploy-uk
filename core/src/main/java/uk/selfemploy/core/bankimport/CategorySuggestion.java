package uk.selfemploy.core.bankimport;

/**
 * Represents a suggested category for an imported transaction.
 *
 * @param category the suggested category (either ExpenseCategory or IncomeCategory)
 * @param confidence the confidence level of the suggestion
 */
public record CategorySuggestion<T extends Enum<T>>(
    T category,
    Confidence confidence
) {
    /**
     * Returns true if this is a high confidence suggestion.
     */
    public boolean isHighConfidence() {
        return confidence == Confidence.HIGH;
    }

    /**
     * Returns true if this is at least medium confidence.
     */
    public boolean isReasonablyConfident() {
        return confidence == Confidence.HIGH || confidence == Confidence.MEDIUM;
    }
}
