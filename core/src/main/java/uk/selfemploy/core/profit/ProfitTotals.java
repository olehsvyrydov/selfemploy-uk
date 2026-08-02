package uk.selfemploy.core.profit;

import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The figures every screen and every submission derives from one set of records.
 *
 * <p>Four consumers used to aggregate the same records with four loops of their own, and four times
 * two of them disagreed about the same money. Every one of those defects was the same slip: summing
 * {@link Expense#amount()} where the claim rule wanted {@link Expense#allowableAmount()}. A test
 * suite can assert afterwards that the four agree; this type removes the opportunity, because there
 * is one loop and everything else formats its result.
 *
 * <p>Range-agnostic on purpose. Callers pass the records for whatever period they are reporting — a
 * quarter, a tax year — so the quarterly and annual paths cannot drift apart by computing the same
 * thing two ways.
 *
 * @param turnover what came in
 * @param grossSpend what went out, which is what reconciles against a bank statement
 * @param allowableSpend the part of it that may be claimed, which is what reduces profit
 * @param byCategory the same split per SA103 category, summing to the two totals above
 */
public record ProfitTotals(
        BigDecimal turnover,
        BigDecimal grossSpend,
        BigDecimal allowableSpend,
        Map<ExpenseCategory, CategorySpend> byCategory) {

    /** A period with no records at all. */
    public static final ProfitTotals EMPTY = new ProfitTotals(
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Map.of());

    public ProfitTotals {
        turnover = turnover == null ? BigDecimal.ZERO : turnover;
        grossSpend = grossSpend == null ? BigDecimal.ZERO : grossSpend;
        allowableSpend = allowableSpend == null ? BigDecimal.ZERO : allowableSpend;
        byCategory = byCategory == null ? Map.of() : Collections.unmodifiableMap(byCategory);
    }

    /**
     * Derives the totals for whatever records are handed in.
     *
     * <p>Null collections are treated as empty rather than thrown: these are called from screens
     * during loading, and a service that has not answered yet must show an empty year, not an error
     * dialog.
     */
    public static ProfitTotals of(Collection<Income> incomes, Collection<Expense> expenses) {
        BigDecimal turnover = BigDecimal.ZERO;
        for (Income income : incomes == null ? List.<Income>of() : incomes) {
            if (income != null && income.amount() != null) {
                turnover = turnover.add(income.amount());
            }
        }

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal allowable = BigDecimal.ZERO;
        Map<ExpenseCategory, CategorySpend> byCategory = new EnumMap<>(ExpenseCategory.class);
        for (Expense expense : expenses == null ? List.<Expense>of() : expenses) {
            if (expense == null || expense.category() == null || expense.amount() == null) {
                continue;
            }
            BigDecimal claim = expense.allowableAmount();
            gross = gross.add(expense.amount());
            allowable = allowable.add(claim);
            byCategory.merge(expense.category(),
                    new CategorySpend(expense.amount(), claim),
                    (existing, added) -> existing.plus(added.spent(), added.claimable()));
        }

        return new ProfitTotals(turnover, gross, allowable, byCategory);
    }

    /**
     * Turnover less what may be claimed — the figure a return is built on and tax is computed from.
     *
     * <p>A loss stays negative. Clamping it to zero would hide it from the return that has to report
     * it.
     */
    public BigDecimal netProfit() {
        return turnover.subtract(allowableSpend);
    }
}
