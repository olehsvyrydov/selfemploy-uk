package uk.selfemploy.common.domain;

import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

/**
 * Represents a UK tax year.
 *
 * UK tax years run from 6 April to 5 April of the following year.
 * For example, tax year 2025/26 runs from 6 April 2025 to 5 April 2026.
 */
public record TaxYear(
    UUID id,
    int startYear,
    LocalDate startDate,
    LocalDate endDate,
    String label
) {
    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;
    private static final int TAX_YEAR_START_DAY = 6;
    private static final int TAX_YEAR_END_DAY = 5;

    /**
     * Compact constructor for validation.
     */
    public TaxYear {
        // Validation is done in factory method
    }

    /**
     * Creates a TaxYear for the given start year.
     *
     * @param startYear The year the tax year starts (e.g., 2025 for 2025/26)
     * @return A new TaxYear instance
     */
    public static TaxYear of(int startYear) {
        validateYear(startYear);

        LocalDate start = LocalDate.of(startYear, Month.APRIL, TAX_YEAR_START_DAY);
        LocalDate end = LocalDate.of(startYear + 1, Month.APRIL, TAX_YEAR_END_DAY);
        String label = formatLabel(startYear);

        return new TaxYear(UUID.randomUUID(), startYear, start, end, label);
    }

    /**
     * Returns the current tax year based on today's date.
     */
    public static TaxYear current() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();

        // If before April 6th, we're still in the previous tax year
        if (today.getMonthValue() < 4 || (today.getMonthValue() == 4 && today.getDayOfMonth() < TAX_YEAR_START_DAY)) {
            year = year - 1;
        }

        return of(year);
    }

    /**
     * Checks if a given date falls within this tax year.
     *
     * @param date The date to check
     * @return true if the date is within this tax year, false otherwise
     */
    public boolean contains(LocalDate date) {
        if (date == null) {
            return false;
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Returns the previous tax year.
     */
    public TaxYear previous() {
        return of(startYear - 1);
    }

    /**
     * Returns the next tax year.
     */
    public TaxYear next() {
        return of(startYear + 1);
    }

    /**
     * Returns the online Self Assessment filing deadline for this tax year.
     * This is 31 January following the end of the tax year.
     */
    public LocalDate onlineFilingDeadline() {
        return LocalDate.of(startYear + 2, Month.JANUARY, 31);
    }

    /**
     * Returns the paper Self Assessment filing deadline for this tax year.
     * This is 31 October following the end of the tax year.
     */
    public LocalDate paperFilingDeadline() {
        return LocalDate.of(startYear + 1, Month.OCTOBER, 31);
    }

    /**
     * Returns the tax payment deadline for this tax year.
     * This is 31 January following the end of the tax year.
     */
    public LocalDate paymentDeadline() {
        return LocalDate.of(startYear + 2, Month.JANUARY, 31);
    }

    /**
     * Returns the tax year in HMRC API format (e.g., "2025-26").
     * This is used in HMRC MTD API endpoint URLs.
     *
     * @return the tax year in "YYYY-YY" format with hyphen separator
     */
    public String hmrcFormat() {
        int endYearShort = (startYear + 1) % 100;
        return String.format("%d-%02d", startYear, endYearShort);
    }

    private static void validateYear(int year) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new IllegalArgumentException(
                String.format("Tax year must be between %d and %d", MIN_YEAR, MAX_YEAR)
            );
        }
    }

    private static String formatLabel(int startYear) {
        int endYearShort = (startYear + 1) % 100;
        return String.format("%d/%02d", startYear, endYearShort);
    }
}
