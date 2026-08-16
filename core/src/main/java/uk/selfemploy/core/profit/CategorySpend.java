package uk.selfemploy.core.profit;

import java.math.BigDecimal;

/**
 * What was spent in one SA103 category, and how much of it may be claimed.
 *
 * <p>The two are held together because they are one fact about one category and are wrong apart.
 * They were once two parallel maps, and the pair had already drifted: one writer took the claimable
 * share from the expense, another re-derived it from the category alone and so claimed the private
 * share of a part-business expense in full.
 *
 * <p>They differ for two reasons — a category HMRC disallows claims nothing, and an expense marked
 * part business use claims only its share — which is why {@code claimable} is carried rather than
 * computed from {@code spent}.
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

    /**
     * The part of the spend that cannot be claimed, which is what SA103F boxes 32-45 report.
     *
     * <p>Derived rather than carried so it cannot disagree with the two figures it comes from. A
     * return declares the whole spend and this alongside it; HMRC subtracts one from the other to
     * reach the deduction, which is why filing either column without the other changes the tax.
     */
    public BigDecimal disallowable() {
        return spent.subtract(claimable);
    }

    /** This category's running totals with another expense's figures folded in. */
    public CategorySpend plus(BigDecimal moreSpent, BigDecimal moreClaimable) {
        return new CategorySpend(
                spent.add(moreSpent == null ? BigDecimal.ZERO : moreSpent),
                claimable.add(moreClaimable == null ? BigDecimal.ZERO : moreClaimable));
    }
}
