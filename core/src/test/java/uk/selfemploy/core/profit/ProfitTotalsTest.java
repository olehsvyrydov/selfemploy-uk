package uk.selfemploy.core.profit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

    @Test
    @DisplayName("an allowable category at 0% business use is reported but claims nothing")
    void anAllowableCategoryAtZeroPercentClaimsNothing() {
        // The other end of the same rule as 100%, and reachable: the expense dialog accepts 0.
        ProfitTotals totals = ProfitTotals.of(
                List.of(income("1000.00")),
                List.of(expense("600.00", ExpenseCategory.OFFICE_COSTS, 0)));

        assertThat(totals.grossSpend()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(totals.allowableSpend())
                .as("an allowable category is not the same as an allowed claim")
                .isZero();
        assertThat(totals.netProfit()).isEqualByComparingTo(new BigDecimal("1000.00"));

        CategorySpend office = totals.byCategory().get(ExpenseCategory.OFFICE_COSTS);
        assertThat(office.spent()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(office.claimable()).isZero();
    }

    @Test
    @DisplayName("the claim is the sum of each expense's own rounded claim, not a share of the total")
    void theClaimIsSummedPerExpenseNotApportionedFromTheTotal() {
        // Three 1p expenses at 50% each claim 0.01 after rounding half up, so the category claims
        // 0.03. Apportioning the 0.03 total instead would claim 0.02 — one penny adrift per category,
        // every year, in whichever direction the total happens to round.
        ProfitTotals totals = ProfitTotals.of(List.of(),
                List.of(expense("0.01", ExpenseCategory.OFFICE_COSTS, 50),
                        expense("0.01", ExpenseCategory.OFFICE_COSTS, 50),
                        expense("0.01", ExpenseCategory.OFFICE_COSTS, 50)));

        assertThat(totals.allowableSpend())
                .as("summed per expense, which is what Expense.allowableAmount() decides")
                .isEqualByComparingTo(new BigDecimal("0.03"));
        assertThat(totals.byCategory().get(ExpenseCategory.OFFICE_COSTS).claimable())
                .as("and per category, which is the figure actually filed — asserting only the "
                    + "grand total would let the same rounding defect through where it lands")
                .isEqualByComparingTo(new BigDecimal("0.03"));
    }

    @Test
    @DisplayName("a caller cannot change the breakdown after the totals were derived from it")
    void theBreakdownCannotBeChangedThroughTheCallersMap() {
        Map<ExpenseCategory, CategorySpend> callersMap = new EnumMap<>(ExpenseCategory.class);
        callersMap.put(ExpenseCategory.OFFICE_COSTS, new CategorySpend(
                new BigDecimal("100.00"), new BigDecimal("100.00")));

        ProfitTotals totals = new ProfitTotals(new BigDecimal("500.00"), new BigDecimal("100.00"),
                new BigDecimal("100.00"), callersMap);
        callersMap.put(ExpenseCategory.TRAVEL, new CategorySpend(
                new BigDecimal("9999.00"), new BigDecimal("9999.00")));

        assertThat(totals.byCategory())
                .as("wrapping the caller's map rather than copying it would let the breakdown stop "
                    + "summing to the totals beside it, with nothing here changing")
                .hasSize(1)
                .containsOnlyKeys(ExpenseCategory.OFFICE_COSTS);
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
