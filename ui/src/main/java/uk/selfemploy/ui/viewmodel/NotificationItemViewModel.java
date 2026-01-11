package uk.selfemploy.ui.viewmodel;

import uk.selfemploy.ui.service.DeadlineNotification;
import uk.selfemploy.ui.service.NotificationPriority;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ViewModel for a single notification item in the panel.
 * Wraps DeadlineNotification with display-friendly properties.
 */
public record NotificationItemViewModel(
    UUID id,
    String title,
    String message,
    NotificationPriority priority,
    LocalDateTime triggeredAt,
    boolean isRead,
    boolean isSnoozed,
    String deadlineLabel,
    String actionUrl
) {
    /**
     * Creates a ViewModel from a DeadlineNotification.
     */
    public static NotificationItemViewModel from(DeadlineNotification notification) {
        return new NotificationItemViewModel(
            notification.id(),
            notification.title(),
            notification.message(),
            notification.priority(),
            notification.triggeredAt(),
            notification.isRead(),
            notification.isSnoozed(),
            notification.deadline().label(),
            createActionUrl(notification)
        );
    }

    /**
     * Gets the priority style class for CSS.
     */
    public String getPriorityStyleClass() {
        return switch (priority) {
            case LOW -> "priority-low";
            case MEDIUM -> "priority-medium";
            case HIGH -> "priority-high";
            case CRITICAL -> "priority-critical";
        };
    }

    /**
     * Checks if this notification should show action buttons.
     */
    public boolean hasActions() {
        return priority == NotificationPriority.HIGH || priority == NotificationPriority.CRITICAL;
    }

    private static String createActionUrl(DeadlineNotification notification) {
        // Create deep link based on deadline type
        String label = notification.deadline().label().toLowerCase();
        if (label.contains("filing") || label.contains("annual")) {
            return "/submission";
        } else if (label.contains("payment") || label.contains("account")) {
            return "/tax-summary";
        } else if (label.contains("mtd") || label.contains("quarterly")) {
            return "/submission";
        }
        return "/dashboard";
    }
}
