package uk.selfemploy.ui.viewmodel;

import java.math.BigDecimal;

/**
 * Summary of expenses for a single SA103 category.
 *
 * <p>Used by {@link QuarterlyReviewData} to display grouped expense totals
 * with transaction counts in the Quarterly Review Dialog.</p>
 *
 * @param amount the total amount for this category
 * @param transactionCount the number of transactions in this category
 */
public record CategorySummary(BigDecimal amount, int transactionCount) {

    /**
     * Creates a CategorySummary with zero amount and count.
     */
    public static CategorySummary zero() {
        return new CategorySummary(BigDecimal.ZERO, 0);
    }

    /**
     * Adds another CategorySummary to this one.
     *
     * @param other the other summary to add
     * @return a new CategorySummary with combined values
     */
    public CategorySummary add(CategorySummary other) {
        if (other == null) {
            return this;
        }
        return new CategorySummary(
                this.amount.add(other.amount),
                this.transactionCount + other.transactionCount
        );
    }

    /**
     * Returns true if this summary has any transactions.
     */
    public boolean hasTransactions() {
        return transactionCount > 0;
    }
}
