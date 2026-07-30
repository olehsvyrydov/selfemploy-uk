package uk.selfemploy.ui.viewmodel;

import java.math.BigDecimal;

/**
 * What was spent in one SA103 category, and how much of it may be claimed.
 *
 * <p>The two are held together because they are one fact about one category and are wrong apart. They
 * were previously two maps, and the pair had already drifted: one writer took the claimable share from
 * the expense, another re-derived it from the category alone and so claimed the private share of a
 * part-business expense in full.
 *
 * <p>They differ for two reasons — a category HMRC disallows claims nothing, and an expense marked part
 * business use claims only its share — which is why {@code claimable} is carried rather than computed.
 *
 * @param spent what left the bank account, which is what a return has to declare
 * @param claimable the part of it that reduces profit, which may be zero
 */
public record CategorySpend(BigDecimal spent, BigDecimal claimable) {

    public static final CategorySpend ZERO = new CategorySpend(BigDecimal.ZERO, BigDecimal.ZERO);

    public CategorySpend {
        spent = spent == null ? BigDecimal.ZERO : spent;
        claimable = claimable == null ? BigDecimal.ZERO : claimable;
    }

    /** This category's running totals with another expense's figures folded in. */
    public CategorySpend plus(BigDecimal moreSpent, BigDecimal moreClaimable) {
        return new CategorySpend(
                spent.add(moreSpent == null ? BigDecimal.ZERO : moreSpent),
                claimable.add(moreClaimable == null ? BigDecimal.ZERO : moreClaimable));
    }

    /** Whether any part of this category's spend cannot be claimed. */
    public boolean isPartlyClaimable() {
        return claimable.signum() > 0 && claimable.compareTo(spent) < 0;
    }

    /** Whether none of this category's spend may be claimed. */
    public boolean isNotClaimable() {
        return claimable.signum() == 0 && spent.signum() != 0;
    }
}
