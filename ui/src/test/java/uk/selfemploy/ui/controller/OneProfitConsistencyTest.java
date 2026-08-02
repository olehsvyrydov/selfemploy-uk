package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.calculator.TaxLiabilityCalculator;
import uk.selfemploy.core.calculator.TaxLiabilityResult;
import uk.selfemploy.core.config.TaxRateConfiguration;
import uk.selfemploy.ui.OneProfitFixture;
import uk.selfemploy.ui.service.SqliteDataStore;
import uk.selfemploy.ui.service.SqliteExpenseService;
import uk.selfemploy.ui.service.SqliteIncomeService;
import uk.selfemploy.ui.service.SqliteTestSupport;
import uk.selfemploy.core.profit.CategorySpend;
import uk.selfemploy.ui.viewmodel.CategorySummary;
import uk.selfemploy.ui.viewmodel.DashboardViewModel;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;
import uk.selfemploy.ui.viewmodel.TaxSummaryViewModel;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One dataset, one profit, whichever part of the app is asked.
 *
 * <p>Four separate defects have been found where two parts of the app disagreed about the same money:
 * the quarterly submission filing gross expenses while the Dashboard showed the apportioned share, the
 * Tax Summary computing tax on a larger deduction than was filed, the in-memory service disagreeing
 * with the SQLite one, and SQLite returning a float sum where Java returned an exact figure. None was
 * caught by a test.
 *
 * <p>So this seeds records once and drives each consumer through the path it really uses — the view
 * model for the Dashboard, the controller for the Tax Summary, the aggregation that builds the HMRC
 * payload for the quarterly figures — rather than handing any of them a number. Every figure is
 * compared against {@link TaxLiabilityCalculator}, which is the one implementation of the tax rules
 * and therefore the arbiter.
 *
 * <p>It lives in this package because {@code QuarterlyUpdatesController.aggregateReviewData} is
 * package-private, and the figure filed to HMRC is the one most worth pinning.
 */
@DisplayName("One profit: every consumer agrees to the penny")
class OneProfitConsistencyTest {

    private static UUID businessId;

    private SqliteIncomeService incomeService;
    private SqliteExpenseService expenseService;

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
        businessId = UUID.randomUUID();
        SqliteDataStore.getInstance().ensureBusinessExists(businessId);
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetTestData();
        SqliteDataStore.getInstance().ensureBusinessExists(businessId);
        incomeService = new SqliteIncomeService(businessId);
        expenseService = new SqliteExpenseService(businessId);
    }

    private TaxLiabilityResult canonical(BigDecimal profit) {
        return new TaxLiabilityCalculator(OneProfitFixture.TAX_YEAR.startYear()).calculate(profit);
    }

    private DashboardViewModel dashboard() {
        DashboardViewModel dashboard = new DashboardViewModel();
        dashboard.loadData(incomeService, expenseService, businessId, OneProfitFixture.TAX_YEAR);
        return dashboard;
    }

    /** The real Tax Summary path: the controller aggregates, the view model calculates. */
    private TaxSummaryViewModel taxSummary() {
        TaxSummaryController controller = new TaxSummaryController();
        TaxSummaryViewModel viewModel = new TaxSummaryViewModel();
        controller.setViewModel(viewModel);
        // Dependencies first: setTaxYear falls back to the live CoreServiceFactory, and therefore the
        // real on-disk database, if the services have not been supplied.
        controller.initializeWithDependencies(incomeService, expenseService, businessId);
        controller.setTaxYear(OneProfitFixture.TAX_YEAR);
        return viewModel;
    }

    private QuarterlyReviewData filedQuarter() {
        QuarterlyUpdatesController controller = new QuarterlyUpdatesController();
        controller.initializeWithDependencies(incomeService, expenseService, businessId);
        controller.setTaxYear(OneProfitFixture.TAX_YEAR);
        return controller.aggregateReviewData(OneProfitFixture.QUARTER);
    }

    @Test
    @DisplayName("the tax year's rates are configured, not silently defaulted")
    void theTaxYearIsConfigured() {
        assertThat(TaxRateConfiguration.getInstance()
                .isTaxYearSupported(OneProfitFixture.TAX_YEAR.startYear()))
                .as("a missing rate file falls back to hardcoded defaults with only a log warning, so "
                    + "every figure below would be plausible and wrong")
                .isTrue();
    }

    @Test
    @DisplayName("the Dashboard derives the canonical profit")
    void dashboardAgrees() {
        OneProfitFixture.seedBase(incomeService, expenseService, businessId);

        DashboardViewModel dashboard = dashboard();

        assertThat(dashboard.getTotalIncome()).isEqualByComparingTo(OneProfitFixture.TURNOVER);
        assertThat(dashboard.getTotalExpenses())
                .as("what was spent, which is what reconciles with the bank")
                .isEqualByComparingTo(OneProfitFixture.GROSS_SPEND);
        assertThat(dashboard.getAllowableExpenses())
                .as("entertainment is never claimable, whatever it cost")
                .isEqualByComparingTo(OneProfitFixture.ALLOWABLE);
        assertThat(dashboard.getNetProfit()).isEqualByComparingTo(OneProfitFixture.TAXABLE_PROFIT);
    }

    @Test
    @DisplayName("the Tax Summary derives the canonical profit")
    void taxSummaryAgrees() {
        OneProfitFixture.seedBase(incomeService, expenseService, businessId);

        TaxSummaryViewModel taxSummary = taxSummary();

        assertThat(taxSummary.getTurnover()).isEqualByComparingTo(OneProfitFixture.TURNOVER);
        assertThat(taxSummary.getAllowableExpenses()).isEqualByComparingTo(OneProfitFixture.ALLOWABLE);
        assertThat(taxSummary.getNetProfit()).isEqualByComparingTo(OneProfitFixture.TAXABLE_PROFIT);
        assertThat(taxSummary.getTotalExpenses())
                .as("getTotalExpenses means what was spent on the Dashboard, so it must mean the same "
                    + "here rather than quietly excluding disallowed categories")
                .isEqualByComparingTo(OneProfitFixture.GROSS_SPEND);
    }

    @Test
    @DisplayName("the figure filed to HMRC is the canonical one")
    void theFiledQuarterAgrees() {
        OneProfitFixture.seedBase(incomeService, expenseService, businessId);

        QuarterlyReviewData filed = filedQuarter();

        assertThat(filed.getTotalIncome()).isEqualByComparingTo(OneProfitFixture.TURNOVER);
        assertThat(filed.getTotalExpenses())
                .as("the deduction declared to HMRC must be the claimable one, not the whole spend")
                .isEqualByComparingTo(OneProfitFixture.ALLOWABLE);
        assertThat(filed.getNetProfit()).isEqualByComparingTo(OneProfitFixture.TAXABLE_PROFIT);
    }

    @Test
    @DisplayName("every consumer reports the same profit")
    void allConsumersAgree() {
        OneProfitFixture.seedBase(incomeService, expenseService, businessId);

        BigDecimal fromDashboard = dashboard().getNetProfit();
        BigDecimal fromTaxSummary = taxSummary().getNetProfit();
        BigDecimal fromFiling = filedQuarter().getNetProfit();

        assertThat(fromDashboard)
                .as("Dashboard and Tax Summary must not show different profits for one year")
                .isEqualByComparingTo(fromTaxSummary);
        assertThat(fromFiling)
                .as("and what is filed must be what the user was shown")
                .isEqualByComparingTo(fromDashboard);
        assertThat(fromDashboard).isEqualByComparingTo(OneProfitFixture.TAXABLE_PROFIT);
    }

    @Test
    @DisplayName("the tax and National Insurance on that profit are the calculator's")
    void taxAndNiMatchTheCalculator() {
        OneProfitFixture.seedHighProfit(incomeService, expenseService, businessId);
        TaxLiabilityResult expected = canonical(OneProfitFixture.HIGH_TAXABLE_PROFIT);

        TaxSummaryViewModel taxSummary = taxSummary();

        assertThat(taxSummary.getNetProfit())
                .isEqualByComparingTo(OneProfitFixture.HIGH_TAXABLE_PROFIT);
        // Without these the four assertions below would hold on a calculation that never ran: the
        // view model's tax getters start at zero and calculateTax() resets them to zero on failure.
        assertThat(expected.incomeTax()).as("the dataset must owe income tax").isPositive();
        assertThat(expected.niClass4()).as("the dataset must owe Class 4").isPositive();
        assertThat(expected.niClass2()).as("the dataset must owe Class 2").isPositive();

        assertThat(taxSummary.getIncomeTax()).isEqualByComparingTo(expected.incomeTax());
        assertThat(taxSummary.getNiClass4()).isEqualByComparingTo(expected.niClass4());
        assertThat(taxSummary.getNiClass2()).isEqualByComparingTo(expected.niClass2());
        assertThat(taxSummary.getTotalTax()).isEqualByComparingTo(expected.totalLiability());
    }

    @Test
    @DisplayName("Class 2 follows the pinned year's threshold, not the calendar's")
    void class2FollowsThePinnedYear() {
        OneProfitFixture.seedClass2Boundary(incomeService, expenseService, businessId);
        TaxLiabilityResult expected = canonical(OneProfitFixture.BOUNDARY_TAXABLE_PROFIT);

        TaxSummaryViewModel taxSummary = taxSummary();

        assertThat(taxSummary.getNetProfit())
                .isEqualByComparingTo(OneProfitFixture.BOUNDARY_TAXABLE_PROFIT);
        assertThat(expected.niClass2())
                .as("£7,000 is above 2025/26's small-profits threshold of £6,845 and below 2026/27's "
                    + "£7,105, so a Class 2 charge here is what proves the pinned year was used")
                .isPositive();
        assertThat(taxSummary.getNiClass2()).isEqualByComparingTo(expected.niClass2());
    }

    @Test
    @DisplayName("no tax is invented on a profit below every threshold")
    void noTaxBelowTheThresholds() {
        OneProfitFixture.seedBase(incomeService, expenseService, businessId);

        TaxSummaryViewModel taxSummary = taxSummary();

        assertThat(taxSummary.getNetProfit()).isEqualByComparingTo(OneProfitFixture.TAXABLE_PROFIT);
        assertThat(taxSummary.getTotalTax())
                .as("£5,941.62 is below the personal allowance, the Class 4 lower profits limit and "
                    + "the Class 2 small-profits threshold alike")
                .isZero();
    }

    @Test
    @DisplayName("a part-business expense is claimed at its share by every consumer")
    void everyConsumerApportions() {
        OneProfitFixture.seedWithPartBusinessExpense(incomeService, expenseService, businessId);

        DashboardViewModel dashboard = dashboard();
        TaxSummaryViewModel taxSummary = taxSummary();
        QuarterlyReviewData filed = filedQuarter();

        assertThat(dashboard.getTotalExpenses())
                .as("the full phone bill is still what left the bank account")
                .isEqualByComparingTo(OneProfitFixture.GROSS_SPEND_WITH_PHONE);
        assertThat(dashboard.getAllowableExpenses())
                .isEqualByComparingTo(OneProfitFixture.ALLOWABLE_WITH_PHONE);
        assertThat(dashboard.getNetProfit())
                .isEqualByComparingTo(OneProfitFixture.TAXABLE_PROFIT_WITH_PHONE);
        assertThat(taxSummary.getNetProfit())
                .isEqualByComparingTo(OneProfitFixture.TAXABLE_PROFIT_WITH_PHONE);
        assertThat(filed.getNetProfit())
                .as("filing the whole phone bill would overstate the claim by the private share")
                .isEqualByComparingTo(OneProfitFixture.TAXABLE_PROFIT_WITH_PHONE);
    }

    @Test
    @DisplayName("the expense breakdown reports disallowed spend rather than hiding it")
    void theBreakdownReportsDisallowedSpend() {
        OneProfitFixture.seedBase(incomeService, expenseService, businessId);

        CategorySpend entertainment = taxSummary().getExpenseBreakdown()
                .get(ExpenseCategory.BUSINESS_ENTERTAINMENT);

        assertThat(entertainment).isNotNull();
        assertThat(entertainment.spent())
                .as("a return has to report what was spent in a disallowed category, not omit it: the "
                    + "amount is disallowed for profit, which is not the same as unreportable")
                .isEqualByComparingTo(OneProfitFixture.ENTERTAINMENT);
        assertThat(entertainment.claimable())
                .as("reported, but claimed for nothing — the row carries both so the screen can say so")
                .isZero();
    }

    @Test
    @DisplayName("a part-business category reports the whole spend and only its share as claimable")
    void theBreakdownApportionsWithinACategory() {
        OneProfitFixture.seedWithPartBusinessExpense(incomeService, expenseService, businessId);

        CategorySpend officeCosts = taxSummary().getExpenseBreakdown()
                .get(ExpenseCategory.OFFICE_COSTS);

        assertThat(officeCosts).isNotNull();
        assertThat(officeCosts.spent())
                .isEqualByComparingTo(OneProfitFixture.OFFICE_COSTS.add(OneProfitFixture.PHONE_BILL));
        assertThat(officeCosts.claimable())
                .as("the rent in full plus %s of the phone bill, never the private share",
                    OneProfitFixture.PHONE_CLAIMABLE)
                .isEqualByComparingTo(OneProfitFixture.ALLOWABLE_WITH_PHONE);
    }

    @Test
    @DisplayName("the filed category breakdown carries the claimable amount, per category")
    void theFiledBreakdownApportions() {
        OneProfitFixture.seedWithPartBusinessExpense(incomeService, expenseService, businessId);

        QuarterlyReviewData filed = filedQuarter();
        CategorySummary officeCosts = filed.getExpensesByCategory().get(ExpenseCategory.OFFICE_COSTS);

        assertThat(officeCosts).isNotNull();
        assertThat(officeCosts.amount())
                .as("office rent in full plus %s of the phone bill", OneProfitFixture.PHONE_CLAIMABLE)
                .isEqualByComparingTo(OneProfitFixture.ALLOWABLE_WITH_PHONE);
        assertThat(filed.getExpensesByCategory().values().stream()
                .map(CategorySummary::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .as("the breakdown adds up to the total that is filed")
                .isEqualByComparingTo(filed.getTotalExpenses());
        assertThat(officeCosts.transactionCount())
                .as("the rent and the phone bill are two records, and the count is derived by a "
                    + "separate pass from the money — a filed count that disagrees with the filed "
                    + "amount is a wrong submission")
                .isEqualTo(2);
        assertThat(filed.getExpenseTransactionCount())
                .as("and the totals agree with the per-category counts they are built from")
                .isEqualTo(filed.getExpensesByCategory().values().stream()
                        .mapToInt(CategorySummary::transactionCount).sum());
    }
}
