package uk.selfemploy.ui.service;

/**
 * Priority levels for deadline notifications.
 * Used to determine urgency and visual styling.
 */
public enum NotificationPriority {
    /**
     * Low priority - deadline is 30+ days away.
     */
    LOW("notification-low", "info"),

    /**
     * Medium priority - deadline is 7-29 days away.
     */
    MEDIUM("notification-medium", "warning"),

    /**
     * High priority - deadline is 1-6 days away.
     */
    HIGH("notification-high", "urgent"),

    /**
     * Critical priority - deadline is today or overdue.
     */
    CRITICAL("notification-critical", "error");

    private final String styleClass;
    private final String iconType;

    NotificationPriority(String styleClass, String iconType) {
        this.styleClass = styleClass;
        this.iconType = iconType;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public String getIconType() {
        return iconType;
    }

    /**
     * Determines priority based on days until deadline.
     */
    public static NotificationPriority fromDaysRemaining(long days) {
        if (days <= 0) {
            return CRITICAL;
        } else if (days <= 1) {
            return HIGH;
        } else if (days <= 7) {
            return MEDIUM;
        } else {
            return LOW;
        }
    }
}
