package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.calculator.TaxLiabilityCalculator;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ViewModel for the Dashboard view.
 * Manages financial summary data and provides formatted bindings for the UI.
 */
public class DashboardViewModel {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);

    // Financial metrics
    private final ObjectProperty<BigDecimal> totalIncome = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalExpenses = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> netProfit = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> estimatedTax = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // Monthly trends
    private final ObjectProperty<BigDecimal> incomeThisMonth = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> expensesThisMonth = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // Tax year
    private final ObjectProperty<TaxYear> currentTaxYear = new SimpleObjectProperty<>();
    private final DoubleProperty yearProgress = new SimpleDoubleProperty(0.0);
    private final IntegerProperty daysRemaining = new SimpleIntegerProperty(0);

    // Lists
    private final ObservableList<Deadline> deadlines = FXCollections.observableArrayList();
    private final ObservableList<ActivityItem> recentActivity = FXCollections.observableArrayList();

    public DashboardViewModel() {
        // Recalculate net profit when income or expenses change
        totalIncome.addListener((obs, oldVal, newVal) -> updateNetProfit());
        totalExpenses.addListener((obs, oldVal, newVal) -> updateNetProfit());

        // Update progress and deadlines when tax year changes
        currentTaxYear.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateYearProgress();
                updateDeadlines();
            }
        });

        // Initialize with current tax year (triggers listener to populate deadlines)
        setCurrentTaxYear(TaxYear.current());
    }

    // === Getters and Setters ===

    public BigDecimal getTotalIncome() {
        return totalIncome.get();
    }

    public void setTotalIncome(BigDecimal value) {
        totalIncome.set(value);
    }

    public ObjectProperty<BigDecimal> totalIncomeProperty() {
        return totalIncome;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses.get();
    }

    public void setTotalExpenses(BigDecimal value) {
        totalExpenses.set(value);
    }

    public ObjectProperty<BigDecimal> totalExpensesProperty() {
        return totalExpenses;
    }

    public BigDecimal getNetProfit() {
        return netProfit.get();
    }

    public void setNetProfit(BigDecimal value) {
        netProfit.set(value);
    }

    public ObjectProperty<BigDecimal> netProfitProperty() {
        return netProfit;
    }

    public BigDecimal getEstimatedTax() {
        return estimatedTax.get();
    }

    public void setEstimatedTax(BigDecimal value) {
        estimatedTax.set(value);
    }

    public ObjectProperty<BigDecimal> estimatedTaxProperty() {
        return estimatedTax;
    }

    public BigDecimal getIncomeThisMonth() {
        return incomeThisMonth.get();
    }

    public void setIncomeThisMonth(BigDecimal value) {
        incomeThisMonth.set(value);
    }

    public ObjectProperty<BigDecimal> incomeThisMonthProperty() {
        return incomeThisMonth;
    }

    public BigDecimal getExpensesThisMonth() {
        return expensesThisMonth.get();
    }

    public void setExpensesThisMonth(BigDecimal value) {
        expensesThisMonth.set(value);
    }

    public ObjectProperty<BigDecimal> expensesThisMonthProperty() {
        return expensesThisMonth;
    }

    public TaxYear getCurrentTaxYear() {
        return currentTaxYear.get();
    }

    public void setCurrentTaxYear(TaxYear year) {
        currentTaxYear.set(year);
    }

    public ObjectProperty<TaxYear> currentTaxYearProperty() {
        return currentTaxYear;
    }

    public double getYearProgress() {
        return yearProgress.get();
    }

    public DoubleProperty yearProgressProperty() {
        return yearProgress;
    }

    public int getDaysRemaining() {
        return daysRemaining.get();
    }

    public IntegerProperty daysRemainingProperty() {
        return daysRemaining;
    }

    public ObservableList<Deadline> getDeadlines() {
        return deadlines;
    }

    public ObservableList<ActivityItem> getRecentActivity() {
        return recentActivity;
    }

    // === Formatted Values ===

    public String getFormattedIncome() {
        return formatCurrency(getTotalIncome());
    }

    public String getFormattedExpenses() {
        return formatCurrency(getTotalExpenses());
    }

    public String getFormattedProfit() {
        return formatCurrency(getNetProfit());
    }

    public String getFormattedTax() {
        return formatCurrency(getEstimatedTax());
    }

    public String getFormattedIncomeTrend() {
        return formatTrend(getIncomeThisMonth());
    }

    public String getFormattedExpensesTrend() {
        return formatTrend(getExpensesThisMonth());
    }

    public String getYearProgressText() {
        return getDaysRemaining() + " days remaining";
    }

    // === Private Methods ===

    private void updateNetProfit() {
        BigDecimal income = getTotalIncome() != null ? getTotalIncome() : BigDecimal.ZERO;
        BigDecimal expenses = getTotalExpenses() != null ? getTotalExpenses() : BigDecimal.ZERO;
        netProfit.set(income.subtract(expenses));
    }

    private void updateYearProgress() {
        TaxYear year = getCurrentTaxYear();
        if (year == null) return;

        LocalDate today = LocalDate.now();
        LocalDate start = year.startDate();
        LocalDate end = year.endDate();

        if (today.isBefore(start)) {
            yearProgress.set(0.0);
            daysRemaining.set((int) ChronoUnit.DAYS.between(start, end));
        } else if (today.isAfter(end)) {
            yearProgress.set(1.0);
            daysRemaining.set(0);
        } else {
            long totalDays = ChronoUnit.DAYS.between(start, end);
            long elapsedDays = ChronoUnit.DAYS.between(start, today);
            yearProgress.set((double) elapsedDays / totalDays);
            daysRemaining.set((int) ChronoUnit.DAYS.between(today, end));
        }
    }

    private void updateDeadlines() {
        TaxYear year = getCurrentTaxYear();
        if (year == null) return;

        List<Deadline> newDeadlines = new ArrayList<>();
        newDeadlines.add(Deadline.of("Online Filing Deadline", year.onlineFilingDeadline()));
        newDeadlines.add(Deadline.of("Payment Due", year.paymentDeadline()));

        // POA deadline is 31 July (6 months after filing)
        LocalDate poaDeadline = year.paymentDeadline().plusMonths(6);
        newDeadlines.add(Deadline.of("Payment on Account Due", poaDeadline));

        deadlines.setAll(newDeadlines);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return CURRENCY_FORMAT.format(amount);
    }

    private String formatTrend(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        String formatted = formatCurrency(amount.abs());

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + formatted + " this month";
        } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return "-" + formatted + " this month";
        } else {
            return formatted + " this month";
        }
    }

    // === Data Loading (SE-207) ===

    private static final int MAX_RECENT_ACTIVITY = 10;

    /**
     * Loads dashboard data from income and expense services.
     * This integrates real data into the dashboard view.
     *
     * @param incomeService  the income service
     * @param expenseService the expense service
     * @param businessId     the current business ID
     * @param taxYear        the tax year to load data for
     */
    public void loadData(IncomeService incomeService, ExpenseService expenseService,
                         UUID businessId, TaxYear taxYear) {
        if (incomeService == null || expenseService == null || businessId == null || taxYear == null) {
            return;
        }

        // Update current tax year
        setCurrentTaxYear(taxYear);

        // Load totals
        BigDecimal incomeTotal = incomeService.getTotalByTaxYear(businessId, taxYear);
        BigDecimal expenseTotal = expenseService.getTotalByTaxYear(businessId, taxYear);

        setTotalIncome(incomeTotal != null ? incomeTotal : BigDecimal.ZERO);
        setTotalExpenses(expenseTotal != null ? expenseTotal : BigDecimal.ZERO);

        // Calculate estimated tax
        calculateEstimatedTax(taxYear);

        // Load entries for monthly trends and activity
        List<Income> incomes = incomeService.findByTaxYear(businessId, taxYear);
        List<Expense> expenses = expenseService.findByTaxYear(businessId, taxYear);

        // Calculate monthly trends
        calculateMonthlyTrends(incomes, expenses);

        // Load recent activity
        loadRecentActivity(incomes, expenses);
    }

    private void calculateEstimatedTax(TaxYear taxYear) {
        BigDecimal profit = getNetProfit();
        if (profit == null || profit.compareTo(BigDecimal.ZERO) <= 0) {
            setEstimatedTax(BigDecimal.ZERO);
            return;
        }

        try {
            TaxLiabilityCalculator calculator = new TaxLiabilityCalculator(taxYear.startYear());
            var result = calculator.calculate(profit);
            setEstimatedTax(result.totalLiability());
        } catch (Exception e) {
            // If tax calculation fails, set to zero
            setEstimatedTax(BigDecimal.ZERO);
        }
    }

    private void calculateMonthlyTrends(List<Income> incomes, List<Expense> expenses) {
        YearMonth currentMonth = YearMonth.now();

        // Calculate income this month
        BigDecimal incomeThisMonthTotal = incomes.stream()
            .filter(i -> YearMonth.from(i.date()).equals(currentMonth))
            .map(Income::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate expenses this month
        BigDecimal expensesThisMonthTotal = expenses.stream()
            .filter(e -> YearMonth.from(e.date()).equals(currentMonth))
            .map(Expense::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        setIncomeThisMonth(incomeThisMonthTotal);
        setExpensesThisMonth(expensesThisMonthTotal);
    }

    private void loadRecentActivity(List<Income> incomes, List<Expense> expenses) {
        List<ActivityItem> allActivity = new ArrayList<>();

        // Convert incomes to activity items
        for (Income income : incomes) {
            allActivity.add(new ActivityItem(
                income.date(),
                income.description(),
                income.amount(),
                true // isIncome
            ));
        }

        // Convert expenses to activity items
        for (Expense expense : expenses) {
            allActivity.add(new ActivityItem(
                expense.date(),
                expense.description(),
                expense.amount(),
                false // isIncome
            ));
        }

        // Sort by date descending and limit to MAX_RECENT_ACTIVITY
        List<ActivityItem> sortedActivity = allActivity.stream()
            .sorted(Comparator.comparing(ActivityItem::date).reversed())
            .limit(MAX_RECENT_ACTIVITY)
            .collect(Collectors.toList());

        recentActivity.setAll(sortedActivity);
    }

    /**
     * Represents an activity item (income or expense) for the recent activity list.
     */
    public record ActivityItem(
        LocalDate date,
        String description,
        BigDecimal amount,
        boolean isIncome
    ) {
        public String getFormattedDate() {
            LocalDate today = LocalDate.now();
            if (date.equals(today)) {
                return "Today";
            } else if (date.equals(today.minusDays(1))) {
                return "Yesterday";
            } else {
                return date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM"));
            }
        }

        public String getFormattedAmount() {
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.UK);
            String prefix = isIncome ? "+ " : "- ";
            return prefix + format.format(amount.abs());
        }
    }
}
