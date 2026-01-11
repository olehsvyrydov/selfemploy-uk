package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.selfemploy.ui.service.DeadlineNotification;
import uk.selfemploy.ui.service.DeadlineNotificationService;
import uk.selfemploy.ui.service.NotificationPriority;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * ViewModel for the notification panel component.
 * Manages notification display, filtering, and user interactions.
 *
 * <p>SE-502: Desktop Notifications</p>
 */
public class NotificationPanelViewModel {

    private final DeadlineNotificationService notificationService;

    // Panel visibility
    private final BooleanProperty panelVisible = new SimpleBooleanProperty(false);

    // Badge
    private final IntegerProperty badgeCount = new SimpleIntegerProperty(0);
    private final BooleanProperty badgeVisible = new SimpleBooleanProperty(false);
    private final StringProperty badgeText = new SimpleStringProperty("");

    // Notifications list
    private final ObservableList<NotificationItemViewModel> notifications = FXCollections.observableArrayList();
    private final ObservableList<NotificationItemViewModel> filteredNotifications = FXCollections.observableArrayList();

    // Filter
    private final ObjectProperty<NotificationFilter> selectedFilter = new SimpleObjectProperty<>(NotificationFilter.ALL);

    // Empty state
    private final BooleanProperty emptyStateVisible = new SimpleBooleanProperty(true);

    // Navigation callback
    private Consumer<String> navigationHandler;

    public NotificationPanelViewModel(DeadlineNotificationService notificationService) {
        this.notificationService = notificationService;

        // Listen for filter changes
        selectedFilter.addListener((obs, oldVal, newVal) -> applyFilter());

        // Initial badge refresh
        refreshBadge();
    }

    // === Panel Visibility ===

    public boolean isPanelVisible() {
        return panelVisible.get();
    }

    public BooleanProperty panelVisibleProperty() {
        return panelVisible;
    }

    public void togglePanel() {
        if (panelVisible.get()) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    public void showPanel() {
        loadNotifications();
        panelVisible.set(true);
    }

    public void hidePanel() {
        panelVisible.set(false);
    }

    // === Badge ===

    public int getBadgeCount() {
        return badgeCount.get();
    }

    public IntegerProperty badgeCountProperty() {
        return badgeCount;
    }

    public boolean isBadgeVisible() {
        return badgeVisible.get();
    }

    public BooleanProperty badgeVisibleProperty() {
        return badgeVisible;
    }

    public String getBadgeText() {
        return badgeText.get();
    }

    public StringProperty badgeTextProperty() {
        return badgeText;
    }

    public void refreshBadge() {
        int count = notificationService.getUnreadCount();
        badgeCount.set(count);
        badgeVisible.set(count > 0);
        badgeText.set(count > 9 ? "9+" : String.valueOf(count));
    }

    // === Notifications List ===

    public ObservableList<NotificationItemViewModel> getNotifications() {
        return notifications;
    }

    public ObservableList<NotificationItemViewModel> getFilteredNotifications() {
        return filteredNotifications;
    }

    public void loadNotifications() {
        List<DeadlineNotification> history = notificationService.getNotificationHistory();

        notifications.clear();
        for (DeadlineNotification notification : history) {
            notifications.add(NotificationItemViewModel.from(notification));
        }

        applyFilter();
        updateEmptyState();
    }

    // === Filter ===

    public NotificationFilter getSelectedFilter() {
        return selectedFilter.get();
    }

    public ObjectProperty<NotificationFilter> selectedFilterProperty() {
        return selectedFilter;
    }

    public void setSelectedFilter(NotificationFilter filter) {
        selectedFilter.set(filter);
    }

    private void applyFilter() {
        filteredNotifications.clear();

        NotificationFilter filter = selectedFilter.get();
        for (NotificationItemViewModel notification : notifications) {
            boolean include = switch (filter) {
                case ALL -> true;
                case UNREAD -> !notification.isRead();
                case DEADLINES -> true; // All our notifications are deadline-based
                case UPDATES -> false; // No update notifications yet
            };
            if (include) {
                filteredNotifications.add(notification);
            }
        }

        updateEmptyState();
    }

    // === Empty State ===

    public boolean isEmptyStateVisible() {
        return emptyStateVisible.get();
    }

    public BooleanProperty emptyStateVisibleProperty() {
        return emptyStateVisible;
    }

    private void updateEmptyState() {
        emptyStateVisible.set(filteredNotifications.isEmpty());
    }

    // === Actions ===

    public void markAsRead(UUID notificationId) {
        notificationService.markAsRead(notificationId);
        loadNotifications();
        refreshBadge();
    }

    public void markAllAsRead() {
        notificationService.markAllAsRead();
        loadNotifications();
        refreshBadge();
    }

    public void dismissNotification(UUID notificationId) {
        notificationService.markAsRead(notificationId);
        loadNotifications();
        refreshBadge();
    }

    public void snooze(UUID notificationId, int hours) {
        notificationService.snooze(notificationId, hours);
        loadNotifications();
        refreshBadge();
    }

    public void onNotificationClick(UUID notificationId) {
        markAsRead(notificationId);

        // Find the notification and navigate
        notifications.stream()
            .filter(n -> n.id().equals(notificationId))
            .findFirst()
            .ifPresent(notification -> {
                if (navigationHandler != null && notification.actionUrl() != null) {
                    navigationHandler.accept(notification.actionUrl());
                }
            });
    }

    public void setNavigationHandler(Consumer<String> handler) {
        this.navigationHandler = handler;
    }

    // === Static Utility Methods ===

    /**
     * Gets the CSS style class for a priority level.
     */
    public static String getPriorityStyleClass(NotificationPriority priority) {
        return switch (priority) {
            case LOW -> "priority-low";
            case MEDIUM -> "priority-medium";
            case HIGH -> "priority-high";
            case CRITICAL -> "priority-critical";
        };
    }

    /**
     * Formats a timestamp as a human-readable "time ago" string.
     */
    public static String formatTimeAgo(LocalDateTime time) {
        if (time == null) {
            return "";
        }

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(time, now);
        long hours = ChronoUnit.HOURS.between(time, now);
        long days = ChronoUnit.DAYS.between(time, now);

        if (minutes < 5) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " minutes ago";
        } else if (hours < 24) {
            return hours + " hours ago";
        } else if (days == 1) {
            return "Yesterday";
        } else {
            return days + " days ago";
        }
    }
}
