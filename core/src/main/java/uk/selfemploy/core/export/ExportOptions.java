package uk.selfemploy.core.export;

import java.time.LocalDate;

/**
 * Options for customizing data export behavior.
 */
public record ExportOptions(
    LocalDate startDate,
    LocalDate endDate
) {
    /**
     * Creates export options with no date filtering.
     */
    public static ExportOptions noFilter() {
        return new ExportOptions(null, null);
    }

    /**
     * Checks if a date is within the filter range.
     * Returns true if no filter is set or if the date is within range.
     */
    public boolean isWithinRange(LocalDate date) {
        if (startDate == null && endDate == null) {
            return true;
        }
        if (date == null) {
            return false;
        }
        boolean afterStart = startDate == null || !date.isBefore(startDate);
        boolean beforeEnd = endDate == null || !date.isAfter(endDate);
        return afterStart && beforeEnd;
    }

    /**
     * Returns true if date filtering is enabled.
     */
    public boolean hasDateFilter() {
        return startDate != null || endDate != null;
    }
}
