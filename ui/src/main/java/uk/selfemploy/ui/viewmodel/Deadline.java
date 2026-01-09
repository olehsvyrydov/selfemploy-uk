package uk.selfemploy.ui.viewmodel;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Represents an upcoming deadline with status indicator.
 */
public record Deadline(
    String label,
    LocalDate date,
    DeadlineStatus status
) {
    /**
     * Creates a deadline and automatically calculates status based on days remaining.
     */
    public static Deadline of(String label, LocalDate date) {
        DeadlineStatus status = calculateStatus(date);
        return new Deadline(label, date, status);
    }

    /**
     * Returns the number of days until this deadline.
     */
    public long daysRemaining() {
        return ChronoUnit.DAYS.between(LocalDate.now(), date);
    }

    /**
     * Returns true if this deadline has passed.
     */
    public boolean isPast() {
        return date.isBefore(LocalDate.now());
    }

    private static DeadlineStatus calculateStatus(LocalDate date) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), date);

        if (days < 0) {
            return DeadlineStatus.OVERDUE;
        } else if (days < 7) {
            return DeadlineStatus.CRITICAL;
        } else if (days < 30) {
            return DeadlineStatus.URGENT;
        } else if (days < 90) {
            return DeadlineStatus.WARNING;
        } else {
            return DeadlineStatus.SAFE;
        }
    }

    /**
     * Deadline status based on proximity.
     */
    public enum DeadlineStatus {
        SAFE("status-safe"),
        WARNING("status-warning"),
        URGENT("status-urgent"),
        CRITICAL("status-critical"),
        OVERDUE("status-critical");

        private final String styleClass;

        DeadlineStatus(String styleClass) {
            this.styleClass = styleClass;
        }

        public String getStyleClass() {
            return styleClass;
        }
    }
}
