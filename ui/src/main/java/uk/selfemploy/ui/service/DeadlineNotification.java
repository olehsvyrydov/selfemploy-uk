package uk.selfemploy.ui.service;

import uk.selfemploy.ui.viewmodel.Deadline;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a deadline notification that has been triggered.
 * Immutable record with state tracking for read/snoozed status.
 */
public record DeadlineNotification(
    UUID id,
    Deadline deadline,
    int triggerDays,
    String title,
    String message,
    NotificationPriority priority,
    LocalDateTime triggeredAt,
    boolean isRead,
    boolean isSnoozed,
    LocalDateTime snoozeUntil
) {
    /**
     * Creates a new notification for a deadline trigger.
     */
    public static DeadlineNotification create(Deadline deadline, int triggerDays) {
        NotificationPriority priority = calculatePriority(triggerDays);
        String title = createTitle(deadline, triggerDays);
        String message = createMessage(deadline, triggerDays);

        return new DeadlineNotification(
            UUID.randomUUID(),
            deadline,
            triggerDays,
            title,
            message,
            priority,
            LocalDateTime.now(),
            false, // not read
            false, // not snoozed
            null   // no snooze time
        );
    }

    /**
     * Returns a copy of this notification marked as read.
     */
    public DeadlineNotification markAsRead() {
        return new DeadlineNotification(
            id, deadline, triggerDays, title, message, priority,
            triggeredAt, true, isSnoozed, snoozeUntil
        );
    }

    /**
     * Returns a copy of this notification snoozed until the specified time.
     */
    public DeadlineNotification snoozeUntil(LocalDateTime until) {
        return new DeadlineNotification(
            id, deadline, triggerDays, title, message, priority,
            triggeredAt, isRead, true, until
        );
    }

    /**
     * Checks if snooze has expired.
     */
    public boolean isSnoozeExpired() {
        if (!isSnoozed || snoozeUntil == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(snoozeUntil);
    }

    /**
     * Returns true if this notification should be shown (not read, not actively snoozed).
     */
    public boolean shouldShow() {
        if (isRead) return false;
        if (isSnoozed && !isSnoozeExpired()) return false;
        return true;
    }

    private static NotificationPriority calculatePriority(int triggerDays) {
        if (triggerDays <= 0) {
            return NotificationPriority.CRITICAL;
        } else if (triggerDays <= 1) {
            return NotificationPriority.HIGH;
        } else if (triggerDays <= 7) {
            return NotificationPriority.MEDIUM;
        } else {
            return NotificationPriority.LOW;
        }
    }

    private static String createTitle(Deadline deadline, int triggerDays) {
        if (triggerDays == 0) {
            return "Deadline Today: " + deadline.label();
        } else if (triggerDays == 1) {
            return "Deadline Tomorrow: " + deadline.label();
        } else {
            return "Upcoming Deadline: " + deadline.label();
        }
    }

    private static String createMessage(Deadline deadline, int triggerDays) {
        if (triggerDays == 0) {
            return deadline.label() + " is due today!";
        } else if (triggerDays == 1) {
            return deadline.label() + " is due tomorrow.";
        } else {
            return deadline.label() + " is due in " + triggerDays + " days.";
        }
    }
}
