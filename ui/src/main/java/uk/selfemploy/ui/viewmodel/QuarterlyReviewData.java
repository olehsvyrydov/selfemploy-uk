package uk.selfemploy.ui.viewmodel;

import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data transfer object containing aggregated data for the Quarterly Review Dialog.
 *
 * <p>This DTO holds all the information needed to display the quarterly review
 * before submission to HMRC, including:</p>
 * <ul>
 *   <li>Quarter and tax year identification</li>
 *   <li>Period start and end dates</li>
 *   <li>Total income with transaction count</li>
 *   <li>Expenses grouped by SA103 category with transaction counts</li>
 *   <li>Calculated net profit/loss</li>
 *   <li>Nil return detection</li>
 * </ul>
 *
 * <p>Implementation: /james</p>
 *
 * @see QuarterlyReviewDialog
 * @see CategorySummary
 */
public class QuarterlyReviewData {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM");

    private final Quarter quarter;
    private final TaxYear taxYear;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final BigDecimal totalIncome;
    private final int incomeTransactionCount;
    private final Map<ExpenseCategory, CategorySummary> expensesByCategory;
    private final BigDecimal totalExpenses;
    private final int expenseTransactionCount;

    private QuarterlyReviewData(Builder builder) {
        this.quarter = Objects.requireNonNull(builder.quarter, "quarter must not be null");
        this.taxYear = Objects.requireNonNull(builder.taxYear, "taxYear must not be null");
        this.periodStart = Objects.requireNonNull(builder.periodStart, "periodStart must not be null");
        this.periodEnd = Objects.requireNonNull(builder.periodEnd, "periodEnd must not be null");
        this.totalIncome = Objects.requireNonNull(builder.totalIncome, "totalIncome must not be null");
        this.incomeTransactionCount = builder.incomeTransactionCount;
        this.expensesByCategory = builder.expensesByCategory != null
                ? new EnumMap<>(builder.expensesByCategory)
                : new EnumMap<>(ExpenseCategory.class);
        this.totalExpenses = Objects.requireNonNull(builder.totalExpenses, "totalExpenses must not be null");
        this.expenseTransactionCount = builder.expenseTransactionCount;
    }

    // ==================== Getters ====================

    public Quarter getQuarter() {
        return quarter;
    }

    public TaxYear getTaxYear() {
        return taxYear;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public int getIncomeTransactionCount() {
        return incomeTransactionCount;
    }

    public Map<ExpenseCategory, CategorySummary> getExpensesByCategory() {
        return new EnumMap<>(expensesByCategory);
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public int getExpenseTransactionCount() {
        return expenseTransactionCount;
    }

    // ==================== Calculated Properties ====================

    /**
     * Calculates the net profit (or loss if negative).
     *
     * @return totalIncome minus totalExpenses
     */
    public BigDecimal getNetProfit() {
        return totalIncome.subtract(totalExpenses);
    }

    /**
     * Determines if this is a nil return (no income and no expenses).
     *
     * @return true if both income and expenses are zero
     */
    public boolean isNilReturn() {
        return totalIncome.compareTo(BigDecimal.ZERO) == 0
                && totalExpenses.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Returns whether net profit is positive.
     *
     * @return true if net profit is greater than zero
     */
    public boolean isProfit() {
        return getNetProfit().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns whether net profit is negative (a loss).
     *
     * @return true if net profit is less than zero
     */
    public boolean isLoss() {
        return getNetProfit().compareTo(BigDecimal.ZERO) < 0;
    }

    // ==================== Display Formatting ====================

    /**
     * Returns the period header text (e.g., "Q1 2025/26").
     */
    public String getPeriodHeaderText() {
        return quarter.name() + " " + taxYear.label();
    }

    /**
     * Returns the date range text (e.g., "6 Apr - 5 Jul").
     */
    public String getDateRangeText() {
        return periodStart.format(SHORT_DATE_FORMAT) + " - " + periodEnd.format(SHORT_DATE_FORMAT);
    }

    /**
     * Returns the full date range text (e.g., "6 Apr 2025 - 5 Jul 2025").
     */
    public String getFullDateRangeText() {
        return periodStart.format(DATE_FORMAT) + " - " + periodEnd.format(DATE_FORMAT);
    }

    /**
     * Returns the formatted income amount.
     */
    public String getFormattedIncome() {
        return formatCurrency(totalIncome);
    }

    /**
     * Returns the formatted expenses amount.
     */
    public String getFormattedExpenses() {
        return formatCurrency(totalExpenses);
    }

    /**
     * Returns the formatted net profit amount.
     */
    public String getFormattedNetProfit() {
        return formatCurrency(getNetProfit());
    }

    /**
     * Formats a currency amount with pound sign and comma separators.
     */
    private static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "\u00A30.00";
        }
        return String.format("\u00A3%,.2f", amount);
    }

    // ==================== Builder ====================

    /**
     * Creates a new builder for QuarterlyReviewData.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for QuarterlyReviewData.
     */
    public static class Builder {
        private Quarter quarter;
        private TaxYear taxYear;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private BigDecimal totalIncome;
        private int incomeTransactionCount;
        private Map<ExpenseCategory, CategorySummary> expensesByCategory;
        private BigDecimal totalExpenses;
        private int expenseTransactionCount;

        public Builder quarter(Quarter quarter) {
            this.quarter = quarter;
            return this;
        }

        public Builder taxYear(TaxYear taxYear) {
            this.taxYear = taxYear;
            return this;
        }

        public Builder periodStart(LocalDate periodStart) {
            this.periodStart = periodStart;
            return this;
        }

        public Builder periodEnd(LocalDate periodEnd) {
            this.periodEnd = periodEnd;
            return this;
        }

        public Builder totalIncome(BigDecimal totalIncome) {
            this.totalIncome = totalIncome;
            return this;
        }

        public Builder incomeTransactionCount(int incomeTransactionCount) {
            this.incomeTransactionCount = incomeTransactionCount;
            return this;
        }

        public Builder expensesByCategory(Map<ExpenseCategory, CategorySummary> expensesByCategory) {
            this.expensesByCategory = expensesByCategory;
            return this;
        }

        public Builder totalExpenses(BigDecimal totalExpenses) {
            this.totalExpenses = totalExpenses;
            return this;
        }

        public Builder expenseTransactionCount(int expenseTransactionCount) {
            this.expenseTransactionCount = expenseTransactionCount;
            return this;
        }

        public QuarterlyReviewData build() {
            return new QuarterlyReviewData(this);
        }
    }

    @Override
    public String toString() {
        return "QuarterlyReviewData{" +
                "quarter=" + quarter +
                ", taxYear=" + taxYear +
                ", totalIncome=" + totalIncome +
                ", totalExpenses=" + totalExpenses +
                ", netProfit=" + getNetProfit() +
                ", isNilReturn=" + isNilReturn() +
                '}';
    }
}
