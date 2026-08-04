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
import static org.mockito.Mockito.lenient;
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
        // These tests treat all expenses as allowable, so the deductible total mirrors the
        // gross total. lenient() because not every test loads data / reads net profit.
        lenient().when(expenseService.getDeductibleTotal(eq(businessId), any(TaxYear.class)))
            .thenAnswer(inv -> {
                BigDecimal gross = expenseService.getTotalByTaxYear(inv.getArgument(0), inv.getArgument(1));
                return gross != null ? gross : java.math.BigDecimal.ZERO;
            });
    }

    @Nested
    @DisplayName("Loading Totals")
    class LoadingTotals {

        /** The Dashboard derives its figures from the year's records, as every other consumer does. */
        private void givenRecords(List<Income> incomes, List<Expense> expenses) {
            when(incomeService.findByTaxYear(eq(businessId), any(TaxYear.class))).thenReturn(incomes);
            when(expenseService.findByTaxYear(eq(businessId), any(TaxYear.class))).thenReturn(expenses);
        }

        @Test
        @DisplayName("total income is the year's income")
        void shouldShowTheYearsIncome() {
            givenRecords(List.of(createIncome(LocalDate.of(2025, 6, 15), new BigDecimal("50000.00"))),
                    List.of());

            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            assertThat(viewModel.getTotalIncome()).isEqualByComparingTo("50000.00");
        }

        @Test
        @DisplayName("total expenses is what was spent, disallowed categories included")
        void shouldShowGrossSpendAsTotalExpenses() {
            givenRecords(List.of(), List.of(
                    createExpense(LocalDate.of(2025, 6, 15), new BigDecimal("14500.00"),
                            ExpenseCategory.OFFICE_COSTS),
                    createExpense(LocalDate.of(2025, 6, 15), new BigDecimal("500.00"),
                            ExpenseCategory.BUSINESS_ENTERTAINMENT)));

            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            assertThat(viewModel.getTotalExpenses())
                    .as("the card reconciles against a bank statement, so it shows the whole spend")
                    .isEqualByComparingTo("15000.00");
        }

        @Test
        @DisplayName("net profit deducts only what may be claimed")
        void shouldDeductOnlyTheClaimablePart() {
            givenRecords(List.of(createIncome(LocalDate.of(2025, 6, 15), new BigDecimal("50000.00"))),
                    List.of(
                        createExpense(LocalDate.of(2025, 6, 15), new BigDecimal("14500.00"),
                                ExpenseCategory.OFFICE_COSTS),
                        createExpense(LocalDate.of(2025, 6, 15), new BigDecimal("500.00"),
                                ExpenseCategory.BUSINESS_ENTERTAINMENT)));

            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            assertThat(viewModel.getAllowableExpenses()).isEqualByComparingTo("14500.00");
            assertThat(viewModel.getNetProfit())
                    .as("the entertainment is spent but not claimable, so £35,000 would be the "
                        + "figure only if the card deducted the whole spend")
                    .isEqualByComparingTo("35500.00");
        }

        @Test
        @DisplayName("a part-business expense is claimed at its share")
        void shouldClaimAPartBusinessExpenseAtItsShare() {
            givenRecords(List.of(createIncome(LocalDate.of(2025, 6, 15), new BigDecimal("50000.00"))),
                    List.of(createExpense(LocalDate.of(2025, 6, 15), new BigDecimal("1000.00"),
                            ExpenseCategory.OFFICE_COSTS).withBusinessUsePercentage(60)));

            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            assertThat(viewModel.getTotalExpenses()).isEqualByComparingTo("1000.00");
            assertThat(viewModel.getAllowableExpenses()).isEqualByComparingTo("600.00");
            assertThat(viewModel.getNetProfit()).isEqualByComparingTo("49400.00");
        }

        @Test
        @DisplayName("should calculate estimated tax for net profit")
        void shouldCalculateEstimatedTax() {
            givenRecords(List.of(createIncome(LocalDate.of(2025, 6, 15), new BigDecimal("50000.00"))),
                    List.of(createExpense(LocalDate.of(2025, 6, 15), new BigDecimal("15000.00"),
                            ExpenseCategory.OFFICE_COSTS)));

            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

            assertThat(viewModel.getEstimatedTax()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("a year with no records shows zeroes, not nulls")
        void shouldShowZeroesForAnEmptyYear() {
            givenRecords(List.of(), List.of());

            viewModel.loadData(incomeService, expenseService, businessId, taxYear);

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

            // Use today for this month's expense (guaranteed not in future)
            Expense thisMonthExpense = createExpense(today, new BigDecimal("1500.00"));
            Expense lastMonthExpense = createExpense(today.minusMonths(1), new BigDecimal("800.00"));

            when(incomeService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(List.of());
            when(expenseService.findByTaxYear(eq(businessId), any(TaxYear.class)))
                .thenReturn(List.of(thisMonthExpense, lastMonthExpense));

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
        return createExpense(date, amount, ExpenseCategory.OFFICE_COSTS);
    }

    private Expense createExpense(LocalDate date, BigDecimal amount, ExpenseCategory category) {
        return Expense.create(businessId, date, amount, "Test expense", category, null, null);
    }
}
