package uk.selfemploy.ui.viewmodel;

import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;

import java.time.LocalDate;

/**
 * Pure derivation of a quarter's display state, independent of JavaFX.
 *
 * <p>Extracted so the state rules can be unit-tested around the tax-year quarter
 * boundaries (6 Jul, 5 Oct, 6 Jan) without spinning up the controller.</p>
 */
public final class QuarterState {

    private QuarterState() {
    }

    /**
     * A quarter is "current" only when the tax year being viewed is the one that
     * contains today. Viewing a past or future tax year has no current quarter,
     * which is why a card must never show "(Current)" while also being FUTURE.
     *
     * @param quarter the quarter under consideration
     * @param taxYear the tax year currently being displayed
     * @param today   today's date
     * @return true only if {@code taxYear} contains {@code today} and {@code quarter}
     *         is the quarter that contains {@code today}
     */
    public static boolean isCurrentQuarter(Quarter quarter, TaxYear taxYear, LocalDate today) {
        if (quarter == null || taxYear == null || today == null) {
            return false;
        }
        return taxYear.contains(today) && quarter == Quarter.forDate(today);
    }

    /**
     * Resolves the display status of a quarter from the calendar and whether the
     * quarter has any recorded data.
     *
     * <p>Note: a passed deadline is currently reported as OVERDUE regardless of an
     * actual HMRC submission; wiring real submission state is tracked separately.</p>
     *
     * @param quarter the quarter
     * @param taxYear the tax year being displayed
     * @param today   today's date
     * @param hasData whether the quarter has any income or expense data recorded
     * @return the derived status
     */
    public static QuarterStatus resolveStatus(Quarter quarter, TaxYear taxYear,
                                              LocalDate today, boolean hasData) {
        if (quarter == null || taxYear == null || today == null) {
            // Fail closed: with no calendar context there is nothing due yet.
            return QuarterStatus.FUTURE;
        }
        LocalDate start = quarter.getStartDate(taxYear);
        LocalDate end = quarter.getEndDate(taxYear);
        LocalDate deadline = quarter.getDeadline(taxYear);

        if (today.isBefore(start)) {
            return QuarterStatus.FUTURE;
        }
        if (today.isAfter(deadline)) {
            return QuarterStatus.OVERDUE;
        }
        if (today.isAfter(end) || hasData) {
            return QuarterStatus.DRAFT;
        }
        if (isCurrentQuarter(quarter, taxYear, today)) {
            return QuarterStatus.DRAFT;
        }
        return QuarterStatus.FUTURE;
    }
}
