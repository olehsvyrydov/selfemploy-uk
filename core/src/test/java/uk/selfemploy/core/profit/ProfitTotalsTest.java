package uk.selfemploy.core.profit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One set of records, one set of totals.
 *
 * <p>Four screens used to derive these figures with four loops of their own, and four times two of
 * them disagreed about the same money — always because one summed {@code amount()} where the claim
 * rule wanted {@code allowableAmount()}. A consistency suite can only assert that they agree after
 * the fact; this type is what makes them agree by construction, so the tests here are the ones that
 * matter: everything downstream formats these figures rather than recomputing them.
 */
@DisplayName("Profit totals: derived once, for every consumer")
class ProfitTotalsTest {

    private static final LocalDate DATE = LocalDate.of(2025, 6, 15);
    private static final UUID BUSINESS = UUID.randomUUID();

    @Test
    @DisplayName("turnover is what came in; gross spend is what went out")
    void turnoverAndGrossSpendAreTheRawTotals() {
        ProfitTotals totals = ProfitTotals.of(
                List.of(income("4000.00"), income("3100.00")),
                List.of(expense("1158.38", ExpenseCategory.OFFICE_COSTS),
                        expense("79.11", ExpenseCategory.BUSINESS_ENTERTAINMENT)));

        assertThat(totals.turnover()).isEqualByComparingTo(new BigDecimal("7100.00"));
        assertThat(totals.grossSpend())
                .as("what reconciles against a bank statement, disallowed categories included")
                .isEqualByComparingTo(new BigDecimal("1237.49"));
    }

    @Test
    @DisplayName("a disallowed category is reported in full but claims nothing")
    void aDisallowedCategoryIsReportedButNotClaimed() {
        ProfitTotals totals = ProfitTotals.of(
                List.of(income("7100.00")),
                List.of(expense("1158.38", ExpenseCategory.OFFICE_COSTS),
                        expense("79.11", ExpenseCategory.BUSINESS_ENTERTAINMENT)));

        assertThat(totals.allowableSpend()).isEqualByComparingTo(new BigDecimal("1158.38"));
        assertThat(totals.netProfit()).isEqualByComparingTo(new BigDecimal("5941.62"));

        CategorySpend entertainment = totals.byCategory().get(ExpenseCategory.BUSINESS_ENTERTAINMENT);
        assertThat(entertainment).as("a return has to declare it, so it cannot be dropped").isNotNull();
        assertThat(entertainment.spent()).isEqualByComparingTo(new BigDecimal("79.11"));
        assertThat(entertainment.claimable()).isZero();
    }

    @Test
    @DisplayName("a part-business expense is claimed at its share, everywhere at once")
    void aPartBusinessExpenseIsClaimedAtItsShare() {
        ProfitTotals totals = ProfitTotals.of(
                List.of(income("7100.00")),
                List.of(expense("1158.38", ExpenseCategory.OFFICE_COSTS),
                        expense("79.11", ExpenseCategory.BUSINESS_ENTERTAINMENT),
                        expense("60.00", ExpenseCategory.OFFICE_COSTS, 60)));

        assertThat(totals.grossSpend()).isEqualByComparingTo(new BigDecimal("1297.49"));
        assertThat(totals.allowableSpend())
                .as("the rent in full plus 60% of the phone bill, never the private share")
                .isEqualByComparingTo(new BigDecimal("1194.38"));
        assertThat(totals.netProfit()).isEqualByComparingTo(new BigDecimal("5905.62"));

        CategorySpend office = totals.byCategory().get(ExpenseCategory.OFFICE_COSTS);
        assertThat(office.spent()).isEqualByComparingTo(new BigDecimal("1218.38"));
        assertThat(office.claimable()).isEqualByComparingTo(new BigDecimal("1194.38"));
    }

    @Test
    @DisplayName("the category breakdown adds up to the totals, so no screen can drift from another")
    void theBreakdownReconcilesWithTheTotals() {
        ProfitTotals totals = ProfitTotals.of(
                List.of(income("50000.00")),
                List.of(expense("1158.38", ExpenseCategory.OFFICE_COSTS),
                        expense("79.11", ExpenseCategory.BUSINESS_ENTERTAINMENT),
                        expense("4000.00", ExpenseCategory.DEPRECIATION),
                        expense("60.00", ExpenseCategory.OFFICE_COSTS, 60),
                        expense("250.00", ExpenseCategory.TRAVEL)));

        BigDecimal spentSum = totals.byCategory().values().stream()
                .map(CategorySpend::spent).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal claimSum = totals.byCategory().values().stream()
                .map(CategorySpend::claimable).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(spentSum)
                .as("a breakdown that does not sum to its own total is how two screens disagree")
                .isEqualByComparingTo(totals.grossSpend());
        assertThat(claimSum).isEqualByComparingTo(totals.allowableSpend());
    }

    @Test
    @DisplayName("an empty year is zero, not an error and not a null")
    void anEmptyYearIsZero() {
        ProfitTotals totals = ProfitTotals.of(List.of(), List.of());

        assertThat(totals.turnover()).isZero();
        assertThat(totals.grossSpend()).isZero();
        assertThat(totals.allowableSpend()).isZero();
        assertThat(totals.netProfit()).isZero();
        assertThat(totals.byCategory()).isEmpty();
    }

    @Test
    @DisplayName("null lists are treated as empty rather than thrown from inside a screen")
    void nullListsAreTreatedAsEmpty() {
        ProfitTotals totals = ProfitTotals.of(null, null);

        assertThat(totals.turnover()).isZero();
        assertThat(totals.netProfit()).isZero();
    }

    @Test
    @DisplayName("spending more than was earned gives a loss, not a floor of zero")
    void spendingMoreThanWasEarnedGivesALoss() {
        ProfitTotals totals = ProfitTotals.of(
                List.of(income("1000.00")),
                List.of(expense("2500.00", ExpenseCategory.OFFICE_COSTS)));

        assertThat(totals.netProfit())
                .as("clamping a loss to zero would hide it from the return that has to report it")
                .isEqualByComparingTo(new BigDecimal("-1500.00"));
    }

    @Test
    @DisplayName("the breakdown cannot be modified by a consumer holding it")
    void theBreakdownIsNotModifiableByConsumers() {
        ProfitTotals totals = ProfitTotals.of(List.of(income("100.00")),
                List.of(expense("10.00", ExpenseCategory.OFFICE_COSTS)));

        assertThat(totals.byCategory())
                .as("one screen editing the map would change the figures another screen shows")
                .isUnmodifiable();
    }

    private static Income income(String amount) {
        return Income.create(BUSINESS, DATE, new BigDecimal(amount), "Consulting",
                IncomeCategory.SALES, null);
    }

    private static Expense expense(String amount, ExpenseCategory category) {
        return Expense.create(BUSINESS, DATE, new BigDecimal(amount), category.getDisplayName(),
                category, null, null);
    }

    private static Expense expense(String amount, ExpenseCategory category, int businessUse) {
        return expense(amount, category).withBusinessUsePercentage(businessUse);
    }
}
