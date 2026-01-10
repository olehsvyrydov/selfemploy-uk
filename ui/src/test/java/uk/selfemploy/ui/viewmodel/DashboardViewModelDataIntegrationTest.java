package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests for SE-207: Dashboard Data Integration.
 * Tests that DashboardViewModel correctly loads and displays real data from services.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardViewModel Data Integration (SE-207)")
class DashboardViewModelDataIntegrationTest {

    @Mock
    private IncomeService incomeService;

    @Mock
    private ExpenseService expenseService;

    private DashboardViewModel viewModel;
    private TaxYear taxYear;
    private UUID businessId;

    @BeforeEach
    void setUp() {
        viewModel = new DashboardViewModel();
        taxYear = TaxYear.current();
        businessId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Loading Totals")
    class LoadingTotals {

        @Test
        @DisplayName("should load total income from service")
        void shouldLoadTotalIncomeFromService() {
            // Given
            when(incomeService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(new BigDecimal("50000.00"));
            when(expenseService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(BigDecimal.ZERO);

            // When
            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            // Then
            assertThat(viewModel.getTotalIncome()).isEqualByComparingTo("50000.00");
        }

        @Test
        @DisplayName("should load total expenses from service")
        void shouldLoadTotalExpensesFromService() {
            // Given
            when(incomeService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(BigDecimal.ZERO);
            when(expenseService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(new BigDecimal("15000.00"));

            // When
            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            // Then
            assertThat(viewModel.getTotalExpenses()).isEqualByComparingTo("15000.00");
        }

        @Test
        @DisplayName("should calculate net profit as income minus expenses")
        void shouldCalculateNetProfit() {
            // Given
            when(incomeService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(new BigDecimal("50000.00"));
            when(expenseService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(new BigDecimal("15000.00"));

            // When
            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            // Then
            assertThat(viewModel.getNetProfit()).isEqualByComparingTo("35000.00");
        }

        @Test
        @DisplayName("should calculate estimated tax for net profit")
        void shouldCalculateEstimatedTax() {
            // Given - net profit of 35000 (above personal allowance)
            when(incomeService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(new BigDecimal("50000.00"));
            when(expenseService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(new BigDecimal("15000.00"));

            // When
            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            // Then - should have some tax liability
            assertThat(viewModel.getEstimatedTax()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should handle null totals as zero")
        void shouldHandleNullTotalsAsZero() {
            // Given
            when(incomeService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(null);
            when(expenseService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(null);

            // When
            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            // Then
            assertThat(viewModel.getTotalIncome()).isEqualByComparingTo("0.00");
            assertThat(viewModel.getTotalExpenses()).isEqualByComparingTo("0.00");
            assertThat(viewModel.getNetProfit()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("Monthly Trends")
    class MonthlyTrends {

        @Test
        @DisplayName("should calculate income this month")
        void shouldCalculateIncomeThisMonth() {
            // Given - income entries including some from this month
            LocalDate today = LocalDate.now();
            LocalDate thisMonth = today.withDayOfMonth(1);

            Income thisMonthIncome = createIncome(thisMonth, new BigDecimal("5000.00"));
            Income lastMonthIncome = createIncome(today.minusMonths(1), new BigDecimal("3000.00"));

            when(incomeService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(List.of(thisMonthIncome, lastMonthIncome));
            when(expenseService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(List.of());
            when(incomeService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(new BigDecimal("8000.00"));
            when(expenseService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(BigDecimal.ZERO);

            // When
            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            // Then
            assertThat(viewModel.getIncomeThisMonth()).isEqualByComparingTo("5000.00");
        }

        @Test
        @DisplayName("should calculate expenses this month")
        void shouldCalculateExpensesThisMonth() {
            // Given - expense entries including some from this month
            LocalDate today = LocalDate.now();
            LocalDate thisMonth = today.withDayOfMonth(1);

            Expense thisMonthExpense = createExpense(thisMonth.plusDays(5), new BigDecimal("1500.00"));
            Expense lastMonthExpense = createExpense(today.minusMonths(1), new BigDecimal("800.00"));

            when(incomeService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(List.of());
            when(expenseService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(List.of(thisMonthExpense, lastMonthExpense));
            when(incomeService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(BigDecimal.ZERO);
            when(expenseService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(new BigDecimal("2300.00"));

            // When
            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            // Then
            assertThat(viewModel.getExpensesThisMonth()).isEqualByComparingTo("1500.00");
        }
    }

    @Nested
    @DisplayName("Recent Activity")
    class RecentActivity {

        @Test
        @DisplayName("should load recent activity from income and expenses")
        void shouldLoadRecentActivity() {
            // Given
            LocalDate today = LocalDate.now();
            Income income = createIncome(today, new BigDecimal("5000.00"));
            Expense expense = createExpense(today.minusDays(1), new BigDecimal("200.00"));

            when(incomeService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(List.of(income));
            when(expenseService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(List.of(expense));
            when(incomeService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(new BigDecimal("5000.00"));
            when(expenseService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(new BigDecimal("200.00"));

            // When
            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            // Then
            assertThat(viewModel.getRecentActivity()).hasSize(2);
        }

        @Test
        @DisplayName("should sort recent activity by date descending")
        void shouldSortRecentActivityByDateDescending() {
            // Given
            LocalDate today = LocalDate.now();
            Income incomeToday = createIncome(today, new BigDecimal("5000.00"));
            Expense expenseYesterday = createExpense(today.minusDays(1), new BigDecimal("200.00"));
            Income incomeLastWeek = createIncome(today.minusDays(7), new BigDecimal("3000.00"));

            when(incomeService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(List.of(incomeToday, incomeLastWeek));
            when(expenseService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(List.of(expenseYesterday));
            when(incomeService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(new BigDecimal("8000.00"));
            when(expenseService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(new BigDecimal("200.00"));

            // When
            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            // Then
            assertThat(viewModel.getRecentActivity()).hasSize(3);
            assertThat(viewModel.getRecentActivity().get(0).date()).isEqualTo(today);
            assertThat(viewModel.getRecentActivity().get(1).date()).isEqualTo(today.minusDays(1));
            assertThat(viewModel.getRecentActivity().get(2).date()).isEqualTo(today.minusDays(7));
        }

        @Test
        @DisplayName("should limit recent activity to 10 items")
        void shouldLimitRecentActivityTo10Items() {
            // Given - create 15 income entries
            LocalDate today = LocalDate.now();
            List<Income> manyIncomes = java.util.stream.IntStream.range(0, 15)
                .mapToObj(i -> createIncome(today.minusDays(i), new BigDecimal("100.00")))
                .toList();

            when(incomeService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(manyIncomes);
            when(expenseService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(List.of());
            when(incomeService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(new BigDecimal("1500.00"));
            when(expenseService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(BigDecimal.ZERO);

            // When
            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            // Then
            assertThat(viewModel.getRecentActivity()).hasSize(10);
        }

        @Test
        @DisplayName("should mark income items correctly")
        void shouldMarkIncomeItemsCorrectly() {
            // Given
            LocalDate today = LocalDate.now();
            Income income = createIncome(today, new BigDecimal("5000.00"));

            when(incomeService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(List.of(income));
            when(expenseService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(List.of());
            when(incomeService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(new BigDecimal("5000.00"));
            when(expenseService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(BigDecimal.ZERO);

            // When
            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            // Then
            assertThat(viewModel.getRecentActivity().get(0).isIncome()).isTrue();
        }

        @Test
        @DisplayName("should mark expense items correctly")
        void shouldMarkExpenseItemsCorrectly() {
            // Given
            LocalDate today = LocalDate.now();
            Expense expense = createExpense(today, new BigDecimal("200.00"));

            when(incomeService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(List.of());
            when(expenseService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(List.of(expense));
            when(incomeService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(BigDecimal.ZERO);
            when(expenseService.getTotalByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(new BigDecimal("200.00"));

            // When
            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            // Then
            assertThat(viewModel.getRecentActivity().get(0).isIncome()).isFalse();
        }
    }

    // Helper methods
    private Income createIncome(LocalDate date, BigDecimal amount) {
        return Income.create(
            businessId,
            date,
            amount,
            "Test income",
            IncomeCategory.SALES,
            null
        );
    }

    private Expense createExpense(LocalDate date, BigDecimal amount) {
        return Expense.create(
            businessId,
            date,
            amount,
            "Test expense",
            ExpenseCategory.OFFICE_COSTS,
            null,
            null
        );
    }
}
