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
 * <p>Two datasets. {@link #seedBase} is the one the quality-gates epic specifies, mixing an allowable
 * expense with one in a category HMRC disallows. {@link #seedWithPartBusinessExpense} adds an expense
 * used only partly for business, because a share below 100% is where three of the four known
 * divergences occurred: anything summing raw amounts gets a different profit from anything using
 * {@link uk.selfemploy.common.domain.Expense#allowableAmount()}.
 */
public final class OneProfitFixture {

    /**
     * Fixed deliberately. Turnover of £7,100 sits below the 2026/27 Class 2 small-profits threshold of
     * £7,105 but above 2025/26's £6,845, so leaving the year to the calendar would silently switch
     * Class 2 National Insurance on and off as the machine clock passed April.
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

    private OneProfitFixture() {
    }

    /**
     * Writes the dataset through the real services, so what is read back has been through the same
     * validation, rounding and persistence as a user's own records.
     */
    public static void seedBase(IncomeService incomeService, ExpenseService expenseService,
                                UUID businessId) {
        incomeService.create(businessId, DATE, TURNOVER, "Consulting", IncomeCategory.SALES, null);
        expenseService.create(businessId, DATE, OFFICE_COSTS, "Office rent",
                ExpenseCategory.OFFICE_COSTS, null, null);
        expenseService.create(businessId, DATE, ENTERTAINMENT, "Client dinner",
                ExpenseCategory.BUSINESS_ENTERTAINMENT, null, null);
    }

    /** The base dataset, plus a phone bill the user has marked 60% business. */
    public static void seedWithPartBusinessExpense(IncomeService incomeService,
                                                  ExpenseService expenseService, UUID businessId) {
        seedBase(incomeService, expenseService, businessId);
        expenseService.create(businessId, DATE, PHONE_BILL, "Phone bill",
                ExpenseCategory.OFFICE_COSTS, null, null, PHONE_BUSINESS_USE);
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
        incomeService.create(businessId, today, TURNOVER, "Consulting", IncomeCategory.SALES, null);
        expenseService.create(businessId, today, OFFICE_COSTS, "Office rent",
                ExpenseCategory.OFFICE_COSTS, null, null);
        expenseService.create(businessId, today, ENTERTAINMENT, "Client dinner",
                ExpenseCategory.BUSINESS_ENTERTAINMENT, null, null);
        expenseService.create(businessId, today, PHONE_BILL, "Phone bill",
                ExpenseCategory.OFFICE_COSTS, null, null, PHONE_BUSINESS_USE);
    }
}
