package uk.selfemploy.ui.viewmodel;

/**
 * Filter options for the notification panel.
 */
public enum NotificationFilter {
    ALL("All"),
    UNREAD("Unread"),
    DEADLINES("Deadlines"),
    UPDATES("Updates");

    private final String displayName;

    NotificationFilter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
