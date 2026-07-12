package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.ui.service.InMemoryExpenseService;
import uk.selfemploy.ui.service.InMemoryIncomeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden test for expense allowability consistency (M11).
 *
 * <p>On a fixed dataset that mixes allowable expenses with a disallowable one
 * (business entertainment), the taxable net profit must be turnover minus only the
 * allowable expenses, and the Dashboard and Tax Summary must agree on that figure.
 * Before the fix both subtracted gross expenses, understating profit and tax.</p>
 */
@DisplayName("Expense allowability consistency (M11)")
class ExpenseAllowabilityConsistencyTest {

    private static final TaxYear TAX_YEAR = TaxYear.of(2025);
    private static final LocalDate DATE = LocalDate.of(2025, 6, 15);

    private static final BigDecimal TURNOVER = new BigDecimal("7100.00");
    private static final BigDecimal ALLOWABLE = new BigDecimal("1158.38");   // office costs
    private static final BigDecimal DISALLOWABLE = new BigDecimal("79.11");  // business entertainment
    private static final BigDecimal GROSS_EXPENSES = new BigDecimal("1237.49");
    private static final BigDecimal TAXABLE_PROFIT = new BigDecimal("5941.62"); // 7100 - 1158.38

    private UUID businessId;
    private InMemoryIncomeService incomeService;
    private InMemoryExpenseService expenseService;

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        incomeService = new InMemoryIncomeService();
        expenseService = new InMemoryExpenseService();

        incomeService.create(businessId, DATE, TURNOVER, "Consulting", IncomeCategory.SALES, null);
        expenseService.create(businessId, DATE, ALLOWABLE, "Office rent",
            ExpenseCategory.OFFICE_COSTS, null, null);
        expenseService.create(businessId, DATE, DISALLOWABLE, "Client dinner",
            ExpenseCategory.BUSINESS_ENTERTAINMENT, null, null);
    }

    @Test
    @DisplayName("Dashboard net profit deducts only allowable expenses")
    void dashboardUsesAllowableExpenses() {
        DashboardViewModel dashboard = new DashboardViewModel();
        dashboard.loadData(incomeService, expenseService, businessId, TAX_YEAR);

        assertThat(dashboard.getTotalExpenses()).isEqualByComparingTo(GROSS_EXPENSES);
        assertThat(dashboard.getAllowableExpenses()).isEqualByComparingTo(ALLOWABLE);
        assertThat(dashboard.getNetProfit()).isEqualByComparingTo(TAXABLE_PROFIT);
    }

    @Test
    @DisplayName("Tax Summary net profit deducts only allowable expenses")
    void taxSummaryUsesAllowableExpenses() {
        TaxSummaryViewModel taxSummary = buildTaxSummary();

        assertThat(taxSummary.getTotalExpenses()).isEqualByComparingTo(GROSS_EXPENSES);
        assertThat(taxSummary.getAllowableExpenses()).isEqualByComparingTo(ALLOWABLE);
        assertThat(taxSummary.getNetProfit()).isEqualByComparingTo(TAXABLE_PROFIT);
    }

    @Test
    @DisplayName("Dashboard and Tax Summary agree on taxable profit")
    void dashboardAndTaxSummaryAgree() {
        DashboardViewModel dashboard = new DashboardViewModel();
        dashboard.loadData(incomeService, expenseService, businessId, TAX_YEAR);
        TaxSummaryViewModel taxSummary = buildTaxSummary();

        assertThat(dashboard.getNetProfit()).isEqualByComparingTo(taxSummary.getNetProfit());
    }

    private TaxSummaryViewModel buildTaxSummary() {
        TaxSummaryViewModel taxSummary = new TaxSummaryViewModel();
        taxSummary.setTaxYear(TAX_YEAR);
        taxSummary.setTurnover(TURNOVER);
        Map<ExpenseCategory, BigDecimal> breakdown = new LinkedHashMap<>();
        breakdown.put(ExpenseCategory.OFFICE_COSTS, ALLOWABLE);
        breakdown.put(ExpenseCategory.BUSINESS_ENTERTAINMENT, DISALLOWABLE);
        taxSummary.setExpenseBreakdown(breakdown);
        return taxSummary;
    }
}
