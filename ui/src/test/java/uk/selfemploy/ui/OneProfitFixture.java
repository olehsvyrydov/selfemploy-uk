package uk.selfemploy.ui;

import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One year of records, and the figures every part of the app must derive from them.
 *
 * <p>The point of holding both here is that no consumer can be handed a number somebody typed. A test
 * that seeds records in one place and asserts hand-written expectations in another proves the two agree
 * with its author, not with each other — which is how the previous consistency test came to pass while
 * the paths it compared disagreed.
 *
 * <p>Four datasets, each answering a question the others cannot. {@link #seedBase} mixes an allowable
 * expense with one in a category HMRC disallows. {@link #seedWithPartBusinessExpense} adds an expense
 * used only partly for business, because a share below 100% is where three of the four known
 * divergences occurred: anything summing raw amounts gets a different profit from anything using
 * {@link uk.selfemploy.common.domain.Expense#allowableAmount()}. Those two produce a profit below every
 * threshold, so {@link #seedHighProfit} and {@link #seedClass2Boundary} exist to make the tax and
 * National Insurance figures non-zero — an assertion that zero equals zero holds however broken the
 * calculation is.
 */
public final class OneProfitFixture {

    /**
     * Pinned, because every rate and threshold the expected figures depend on is a property of the
     * year. Leaving it to the calendar would make the tax and National Insurance assertions change
     * answer as the machine clock passed April.
     */
    public static final TaxYear TAX_YEAR = TaxYear.of(2025);

    /** Inside Q1 of the tax year (6 April to 5 July), so quarterly figures see the whole dataset. */
    public static final LocalDate DATE = LocalDate.of(2025, 6, 15);
    public static final Quarter QUARTER = Quarter.Q1;

    public static final BigDecimal TURNOVER = new BigDecimal("7100.00");
    public static final BigDecimal OFFICE_COSTS = new BigDecimal("1158.38");
    public static final BigDecimal ENTERTAINMENT = new BigDecimal("79.11");

    /** Everything spent, which is what reconciles against a bank statement. */
    public static final BigDecimal GROSS_SPEND = new BigDecimal("1237.49");

    /** Office costs in full. Entertainment is never claimable, whatever it cost. */
    public static final BigDecimal ALLOWABLE = new BigDecimal("1158.38");

    /** Turnover less what may be claimed. This is the figure a return is built on. */
    public static final BigDecimal TAXABLE_PROFIT = new BigDecimal("5941.62");

    public static final BigDecimal PHONE_BILL = new BigDecimal("60.00");
    public static final int PHONE_BUSINESS_USE = 60;
    public static final BigDecimal PHONE_CLAIMABLE = new BigDecimal("36.00");

    /** The base dataset plus the whole phone bill. */
    public static final BigDecimal GROSS_SPEND_WITH_PHONE = new BigDecimal("1297.49");

    /** The base allowable total plus 60% of the phone bill. */
    public static final BigDecimal ALLOWABLE_WITH_PHONE = new BigDecimal("1194.38");

    public static final BigDecimal TAXABLE_PROFIT_WITH_PHONE = new BigDecimal("5905.62");

    /**
     * A second dataset whose profit is large enough to owe tax.
     *
     * <p>The dataset above yields £5,941.62, which is below the personal allowance, the Class 4 lower
     * profits limit and the Class 2 small-profits threshold alike — so every tax and NI figure for it
     * is legitimately zero, and an assertion that they match the calculator holds even if the
     * calculation never ran. £55,000 crosses the personal allowance, the basic-rate limit and the Class
     * 4 upper profits limit, so each of those figures is non-zero and a broken calculation shows.
     */
    public static final BigDecimal HIGH_TURNOVER = new BigDecimal("60000.00");
    public static final BigDecimal HIGH_OFFICE_COSTS = new BigDecimal("5000.00");
    public static final BigDecimal HIGH_TAXABLE_PROFIT = new BigDecimal("55000.00");

    /**
     * A dataset whose profit lands between the two Class 2 small-profits thresholds — above 2025/26's
     * £6,845 and below 2026/27's £7,105 — so the pinned year is doing visible work here: run it against
     * the wrong year and Class 2 changes.
     */
    public static final BigDecimal BOUNDARY_TURNOVER = new BigDecimal("8000.00");
    public static final BigDecimal BOUNDARY_OFFICE_COSTS = new BigDecimal("1000.00");
    public static final BigDecimal BOUNDARY_TAXABLE_PROFIT = new BigDecimal("7000.00");

    private OneProfitFixture() {
    }

    /**
     * Writes the dataset through the real services, so what is read back has been through the same
     * validation, rounding and persistence as a user's own records.
     */
    public static void seedBase(IncomeService incomeService, ExpenseService expenseService,
                                UUID businessId) {
        seedBaseOn(incomeService, expenseService, businessId, DATE);
    }

    /** The base dataset, plus a phone bill the user has marked 60% business. */
    public static void seedWithPartBusinessExpense(IncomeService incomeService,
                                                  ExpenseService expenseService, UUID businessId) {
        seedBaseOn(incomeService, expenseService, businessId, DATE);
        seedPhoneBillOn(expenseService, businessId, DATE);
    }

    /**
     * The same records dated today, for tests that read the screens.
     *
     * <p>The app opens on the current tax year, and {@link #TAX_YEAR} is pinned to a past one so the
     * Class 2 threshold cannot drift. Dating these records today puts them in the year the screens are
     * showing. The totals are unaffected: turnover less what may be claimed does not depend on which
     * year it falls in — only the National Insurance does, and that is asserted in the headless suite
     * against the pinned year.
     */
    public static void seedToday(IncomeService incomeService, ExpenseService expenseService,
                                 UUID businessId) {
        LocalDate today = LocalDate.now();
        seedBaseOn(incomeService, expenseService, businessId, today);
        seedPhoneBillOn(expenseService, businessId, today);
    }

    /** Turnover and one allowable expense, sized so the profit owes tax at more than one rate. */
    public static void seedHighProfit(IncomeService incomeService, ExpenseService expenseService,
                                      UUID businessId) {
        seedSimple(incomeService, expenseService, businessId, HIGH_TURNOVER, HIGH_OFFICE_COSTS);
    }

    /** Turnover and one allowable expense, sized so the profit sits between the two Class 2 thresholds. */
    public static void seedClass2Boundary(IncomeService incomeService, ExpenseService expenseService,
                                          UUID businessId) {
        seedSimple(incomeService, expenseService, businessId, BOUNDARY_TURNOVER, BOUNDARY_OFFICE_COSTS);
    }

    private static void seedSimple(IncomeService incomeService, ExpenseService expenseService,
                                   UUID businessId, BigDecimal turnover, BigDecimal officeCosts) {
        incomeService.create(businessId, DATE, turnover, "Consulting", IncomeCategory.SALES, null);
        expenseService.create(businessId, DATE, officeCosts, "Office rent",
                ExpenseCategory.OFFICE_COSTS, null, null);
    }

    private static void seedBaseOn(IncomeService incomeService, ExpenseService expenseService,
                                   UUID businessId, LocalDate date) {
        incomeService.create(businessId, date, TURNOVER, "Consulting", IncomeCategory.SALES, null);
        expenseService.create(businessId, date, OFFICE_COSTS, "Office rent",
                ExpenseCategory.OFFICE_COSTS, null, null);
        expenseService.create(businessId, date, ENTERTAINMENT, "Client dinner",
                ExpenseCategory.BUSINESS_ENTERTAINMENT, null, null);
    }

    private static void seedPhoneBillOn(ExpenseService expenseService, UUID businessId,
                                        LocalDate date) {
        expenseService.create(businessId, date, PHONE_BILL, "Phone bill",
                ExpenseCategory.OFFICE_COSTS, null, null, PHONE_BUSINESS_USE);
    }
}
