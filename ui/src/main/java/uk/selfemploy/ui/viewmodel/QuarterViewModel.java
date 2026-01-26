package uk.selfemploy.ui.viewmodel;

import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * ViewModel for displaying a quarter in the Quarterly Updates dashboard.
 * Sprint 10D: SE-10D-001, SE-10D-002, SE-10D-003
 */
public class QuarterViewModel {

    private static final DateTimeFormatter DATE_RANGE_FORMAT = DateTimeFormatter.ofPattern("d MMM");
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final Quarter quarter;
    private final TaxYear taxYear;
    private final QuarterStatus status;
    private final boolean current;
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpenses;

    /**
     * Creates a QuarterViewModel.
     *
     * @param quarter The quarter
     * @param taxYear The tax year
     * @param status The submission status
     * @param current Whether this is the current quarter
     * @param totalIncome Total income for the quarter (null for future quarters)
     * @param totalExpenses Total expenses for the quarter (null for future quarters)
     */
    public QuarterViewModel(Quarter quarter, TaxYear taxYear, QuarterStatus status,
                            boolean current, BigDecimal totalIncome, BigDecimal totalExpenses) {
        this.quarter = quarter;
        this.taxYear = taxYear;
        this.status = status;
        this.current = current;
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
    }

    public Quarter getQuarter() {
        return quarter;
    }

    public TaxYear getTaxYear() {
        return taxYear;
    }

    public QuarterStatus getStatus() {
        return status;
    }

    public boolean isCurrent() {
        return current;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    /**
     * Returns the net profit/loss for this quarter.
     * Returns null if no data available (future quarters).
     */
    public BigDecimal getNetProfitLoss() {
        if (totalIncome == null || totalExpenses == null) {
            return null;
        }
        return totalIncome.subtract(totalExpenses);
    }

    /**
     * Returns the start date of this quarter.
     */
    public LocalDate getStartDate() {
        return quarter.getStartDate(taxYear);
    }

    /**
     * Returns the end date of this quarter.
     */
    public LocalDate getEndDate() {
        return quarter.getEndDate(taxYear);
    }

    /**
     * Returns the submission deadline for this quarter.
     */
    public LocalDate getDeadline() {
        return quarter.getDeadline(taxYear);
    }

    /**
     * Returns the quarter label (e.g., "Q1 2025/26").
     */
    public String getLabel() {
        return quarter.getLabel(taxYear);
    }

    /**
     * Returns the date range text (e.g., "6 Apr - 5 Jul").
     */
    public String getDateRangeText() {
        LocalDate start = getStartDate();
        LocalDate end = getEndDate();
        return start.format(DATE_RANGE_FORMAT) + " - " + end.format(DATE_RANGE_FORMAT);
    }

    /**
     * Returns the deadline text (e.g., "Deadline: 7 Aug 2025").
     */
    public String getDeadlineText() {
        return "Deadline: " + getDeadline().format(DEADLINE_FORMAT);
    }

    /**
     * Returns formatted income text for display.
     */
    public String getFormattedIncome() {
        if (totalIncome == null) {
            return "--";
        }
        return String.format("\u00A3%,.2f", totalIncome);
    }

    /**
     * Returns formatted expenses text for display.
     */
    public String getFormattedExpenses() {
        if (totalExpenses == null) {
            return "--";
        }
        return String.format("\u00A3%,.2f", totalExpenses);
    }

    /**
     * Returns formatted net profit/loss text for display.
     */
    public String getFormattedNetProfitLoss() {
        BigDecimal net = getNetProfitLoss();
        if (net == null) {
            return "--";
        }
        return String.format("\u00A3%,.2f", net);
    }

    /**
     * Returns true if this quarter has any financial data.
     */
    public boolean hasData() {
        return totalIncome != null || totalExpenses != null;
    }

    /**
     * Returns the accessible text for screen readers.
     */
    public String getAccessibleText() {
        StringBuilder sb = new StringBuilder();
        sb.append(getLabel());
        if (current) {
            sb.append(", current quarter");
        }
        sb.append(", ").append(status.getDisplayText());
        if (hasData()) {
            sb.append(", income ").append(getFormattedIncome());
            sb.append(", expenses ").append(getFormattedExpenses());
            sb.append(", net ").append(getFormattedNetProfitLoss());
        }
        sb.append(", ").append(getDeadlineText());
        return sb.toString();
    }

    @Override
    public String toString() {
        return "QuarterViewModel{" +
                "quarter=" + quarter +
                ", taxYear=" + taxYear +
                ", status=" + status +
                ", current=" + current +
                ", totalIncome=" + totalIncome +
                ", totalExpenses=" + totalExpenses +
                '}';
    }
}
