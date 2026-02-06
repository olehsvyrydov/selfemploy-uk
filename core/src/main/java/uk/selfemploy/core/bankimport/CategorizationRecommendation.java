package uk.selfemploy.core.bankimport;

import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;

/**
 * Comprehensive categorization recommendation for a bank transaction.
 *
 * <p>Combines direction classification, exclusion evaluation, expense category
 * suggestion, and SA103 box mapping into a single recommendation.</p>
 */
public record CategorizationRecommendation(
    boolean isIncome,
    boolean shouldExclude,
    String exclusionReason,
    ExpenseCategory expenseCategory,
    String sa103Box,
    BigDecimal confidenceScore,
    Confidence confidenceLevel
) {
    /**
     * Creates a recommendation for an excluded transaction.
     */
    static CategorizationRecommendation excluded(boolean isIncome, String reason) {
        return new CategorizationRecommendation(
            isIncome, true, reason, null, null,
            new BigDecimal("0.95"), Confidence.HIGH
        );
    }

    /**
     * Creates a recommendation for an income transaction.
     */
    static CategorizationRecommendation income(BigDecimal confidenceScore, Confidence level) {
        return new CategorizationRecommendation(
            true, false, null, null, null,
            confidenceScore, level
        );
    }

    /**
     * Creates a recommendation for an expense transaction with category.
     */
    static CategorizationRecommendation expense(
            ExpenseCategory category, BigDecimal confidenceScore, Confidence level) {
        String box = category != null ? "Box " + category.getSa103Box() : null;
        return new CategorizationRecommendation(
            false, false, null, category, box,
            confidenceScore, level
        );
    }
}
