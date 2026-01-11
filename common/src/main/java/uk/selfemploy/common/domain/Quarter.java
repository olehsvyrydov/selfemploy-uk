package uk.selfemploy.common.domain;

import java.time.LocalDate;
import java.time.Month;

/**
 * Represents a UK tax year quarter for MTD submissions.
 *
 * <p>UK tax year quarters:</p>
 * <ul>
 *   <li>Q1: 6 April - 5 July (Deadline: 7 August)</li>
 *   <li>Q2: 6 July - 5 October (Deadline: 7 November)</li>
 *   <li>Q3: 6 October - 5 January (Deadline: 7 February)</li>
 *   <li>Q4: 6 January - 5 April (Deadline: 7 May)</li>
 * </ul>
 */
public enum Quarter {

    Q1(1, Month.APRIL, 6, Month.JULY, 5, Month.AUGUST, 7),
    Q2(2, Month.JULY, 6, Month.OCTOBER, 5, Month.NOVEMBER, 7),
    Q3(3, Month.OCTOBER, 6, Month.JANUARY, 5, Month.FEBRUARY, 7),
    Q4(4, Month.JANUARY, 6, Month.APRIL, 5, Month.MAY, 7);

    private final int number;
    private final Month startMonth;
    private final int startDay;
    private final Month endMonth;
    private final int endDay;
    private final Month deadlineMonth;
    private final int deadlineDay;

    Quarter(int number, Month startMonth, int startDay, Month endMonth, int endDay,
            Month deadlineMonth, int deadlineDay) {
        this.number = number;
        this.startMonth = startMonth;
        this.startDay = startDay;
        this.endMonth = endMonth;
        this.endDay = endDay;
        this.deadlineMonth = deadlineMonth;
        this.deadlineDay = deadlineDay;
    }

    /**
     * Returns the quarter number (1-4).
     */
    public int getNumber() {
        return number;
    }

    /**
     * Returns the start date of this quarter for the given tax year.
     *
     * @param taxYear The tax year
     * @return The start date of this quarter
     */
    public LocalDate getStartDate(TaxYear taxYear) {
        int year = taxYear.startYear();
        // Q3 and Q4 start months are in the following calendar year
        if (this == Q3 || this == Q4) {
            // Q3 starts in October of the tax year start year
            // Q4 starts in January of the following year
            if (this == Q4) {
                year = taxYear.startYear() + 1;
            }
        }
        return LocalDate.of(year, startMonth, startDay);
    }

    /**
     * Returns the end date of this quarter for the given tax year.
     *
     * @param taxYear The tax year
     * @return The end date of this quarter
     */
    public LocalDate getEndDate(TaxYear taxYear) {
        int year = taxYear.startYear();
        // Adjust year based on quarter end month
        if (this == Q3) {
            // Q3 ends in January of the following year
            year = taxYear.startYear() + 1;
        } else if (this == Q4) {
            // Q4 ends in April of the following year
            year = taxYear.startYear() + 1;
        }
        return LocalDate.of(year, endMonth, endDay);
    }

    /**
     * Returns the submission deadline for this quarter.
     *
     * @param taxYear The tax year
     * @return The deadline date for submitting this quarter's data
     */
    public LocalDate getDeadline(TaxYear taxYear) {
        int year = taxYear.startYear();
        // Adjust year for deadlines that fall in the following calendar year
        if (this == Q3) {
            // Q3 deadline is February of the following year
            year = taxYear.startYear() + 1;
        } else if (this == Q4) {
            // Q4 deadline is May of the following year
            year = taxYear.startYear() + 1;
        }
        return LocalDate.of(year, deadlineMonth, deadlineDay);
    }

    /**
     * Checks if this quarter's deadline has passed.
     *
     * @param taxYear The tax year
     * @return true if the deadline has passed
     */
    public boolean isDeadlinePassed(TaxYear taxYear) {
        return LocalDate.now().isAfter(getDeadline(taxYear));
    }

    /**
     * Returns a human-readable label for this quarter.
     *
     * @param taxYear The tax year
     * @return Label like "Q1 2025/26"
     */
    public String getLabel(TaxYear taxYear) {
        return String.format("Q%d %s", number, taxYear.label());
    }

    /**
     * Returns the quarter that contains the given date.
     *
     * @param date The date to check
     * @return The quarter containing the date, or null if outside tax year bounds
     */
    public static Quarter forDate(LocalDate date) {
        if (date == null) {
            return null;
        }

        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // Q1: April 6 - July 5
        if ((month == 4 && day >= 6) || month == 5 || month == 6 || (month == 7 && day <= 5)) {
            return Q1;
        }
        // Q2: July 6 - October 5
        if ((month == 7 && day >= 6) || month == 8 || month == 9 || (month == 10 && day <= 5)) {
            return Q2;
        }
        // Q3: October 6 - January 5
        if ((month == 10 && day >= 6) || month == 11 || month == 12 || (month == 1 && day <= 5)) {
            return Q3;
        }
        // Q4: January 6 - April 5
        if ((month == 1 && day >= 6) || month == 2 || month == 3 || (month == 4 && day <= 5)) {
            return Q4;
        }

        return null;
    }

    /**
     * Returns the current quarter based on today's date.
     */
    public static Quarter current() {
        return forDate(LocalDate.now());
    }

    /**
     * Returns the previous quarter.
     */
    public Quarter previous() {
        return switch (this) {
            case Q1 -> Q4;
            case Q2 -> Q1;
            case Q3 -> Q2;
            case Q4 -> Q3;
        };
    }

    /**
     * Returns the next quarter.
     */
    public Quarter next() {
        return switch (this) {
            case Q1 -> Q2;
            case Q2 -> Q3;
            case Q3 -> Q4;
            case Q4 -> Q1;
        };
    }
}
