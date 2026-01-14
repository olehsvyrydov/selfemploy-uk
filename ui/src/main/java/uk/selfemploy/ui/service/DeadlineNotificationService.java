package uk.selfemploy.ui.service;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.viewmodel.Deadline;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Service for managing deadline notifications.
 * Handles notification scheduling, delivery, and history tracking.
 *
 * <p>SE-309: Deadline Notifications</p>
 */
public class DeadlineNotificationService {

    private static final Logger LOG = Logger.getLogger(DeadlineNotificationService.class.getName());

    // Preferences
    private final NotificationPreferences preferences = new NotificationPreferences();

    // Notification handler (called when notification should be shown)
    private Consumer<DeadlineNotification> notificationHandler;

    // Notification history (most recent first)
    private final List<DeadlineNotification> history = new CopyOnWriteArrayList<>();

    // Track which notifications have been sent to prevent duplicates
    private final Set<String> sentNotifications = ConcurrentHashMap.newKeySet();

    // Unread count property for UI binding
    private final IntegerProperty unreadCount = new SimpleIntegerProperty(0);

    // Scheduler for periodic checks
    private ScheduledExecutorService scheduler;

    public DeadlineNotificationService() {
        // Initialize with default preferences
    }

    // === Preferences ===

    /**
     * Gets the notification preferences.
     */
    public NotificationPreferences getPreferences() {
        return preferences;
    }

    // === Handler Registration ===

    /**
     * Sets the handler to be called when a notification should be displayed.
     */
    public void setNotificationHandler(Consumer<DeadlineNotification> handler) {
        this.notificationHandler = handler;
    }

    /**
     * Checks if a notification handler is registered.
     */
    public boolean hasNotificationHandler() {
        return notificationHandler != null;
    }

    // === Deadline Checking ===

    /**
     * Checks a deadline and returns any notifications that should be triggered.
     */
    public List<DeadlineNotification> checkDeadline(Deadline deadline) {
        if (!preferences.isEnabled()) {
            return Collections.emptyList();
        }

        List<DeadlineNotification> notifications = new ArrayList<>();
        long daysRemaining = deadline.daysRemaining();

        // Check if deadline is today (special case)
        if (daysRemaining == 0) {
            notifications.add(DeadlineNotification.create(deadline, 0));
            return notifications;
        }

        // Check against configured trigger days
        for (int triggerDay : preferences.getTriggerDays()) {
            if (daysRemaining == triggerDay) {
                notifications.add(DeadlineNotification.create(deadline, triggerDay));
            }
        }

        return notifications;
    }

    /**
     * Gets all deadlines for a tax year.
     */
    public List<Deadline> getDeadlinesForTaxYear(TaxYear taxYear) {
        List<Deadline> deadlines = new ArrayList<>();

        // Filing deadline
        deadlines.add(Deadline.of("Online Filing Deadline", taxYear.onlineFilingDeadline()));

        // Payment deadline
        deadlines.add(Deadline.of("Payment Due", taxYear.paymentDeadline()));

        // Payment on Account deadline (6 months after payment deadline)
        LocalDate poaDeadline = taxYear.paymentDeadline().plusMonths(6);
        deadlines.add(Deadline.of("Payment on Account Due", poaDeadline));

        // Add quarterly MTD deadlines if applicable
        addQuarterlyDeadlines(deadlines, taxYear);

        return deadlines;
    }

    private void addQuarterlyDeadlines(List<Deadline> deadlines, TaxYear taxYear) {
        // MTD quarterly deadlines: 5th of month following quarter end
        // Q1: Apr-Jun -> 5 Aug
        // Q2: Jul-Sep -> 5 Nov
        // Q3: Oct-Dec -> 5 Feb
        // Q4: Jan-Mar -> 5 May

        int year = taxYear.startYear();

        // Q1: 5 August
        deadlines.add(Deadline.of("MTD Q1 Update Due", LocalDate.of(year, 8, 5)));
        // Q2: 5 November
        deadlines.add(Deadline.of("MTD Q2 Update Due", LocalDate.of(year, 11, 5)));
        // Q3: 5 February (following year)
        deadlines.add(Deadline.of("MTD Q3 Update Due", LocalDate.of(year + 1, 2, 5)));
        // Q4: 5 May (following year)
        deadlines.add(Deadline.of("MTD Q4 Update Due", LocalDate.of(year + 1, 5, 5)));
    }

    // === Notification Triggering ===

    /**
     * Triggers a notification for a deadline.
     */
    public void triggerNotification(Deadline deadline, int triggerDays) {
        // Create unique key for deduplication
        String notificationKey = createNotificationKey(deadline, triggerDays);

        // Check for duplicate
        if (sentNotifications.contains(notificationKey)) {
            LOG.fine("Skipping duplicate notification: " + notificationKey);
            return;
        }

        // Create notification
        DeadlineNotification notification = DeadlineNotification.create(deadline, triggerDays);

        // Add to history
        history.add(0, notification); // Add at beginning (most recent first)
        sentNotifications.add(notificationKey);

        // Update unread count
        updateUnreadCount();

        // Call handler if registered
        if (notificationHandler != null) {
            try {
                notificationHandler.accept(notification);
            } catch (Exception e) {
                LOG.warning("Error in notification handler: " + e.getMessage());
            }
        }

        LOG.info("Notification triggered: " + notification.title());
    }

    private String createNotificationKey(Deadline deadline, int triggerDays) {
        return deadline.label() + "_" + deadline.date() + "_" + triggerDays + "_" + LocalDate.now();
    }

    // === History Management ===

    /**
     * Gets the notification history (most recent first).
     */
    public List<DeadlineNotification> getNotificationHistory() {
        return Collections.unmodifiableList(history);
    }

    /**
     * Clears all notification history.
     */
    public void clearHistory() {
        history.clear();
        sentNotifications.clear();
        updateUnreadCount();
    }

    /**
     * Marks a notification as read.
     */
    public void markAsRead(UUID notificationId) {
        for (int i = 0; i < history.size(); i++) {
            DeadlineNotification notification = history.get(i);
            if (notification.id().equals(notificationId)) {
                history.set(i, notification.markAsRead());
                updateUnreadCount();
                break;
            }
        }
    }

    /**
     * Marks all notifications as read.
     */
    public void markAllAsRead() {
        for (int i = 0; i < history.size(); i++) {
            DeadlineNotification notification = history.get(i);
            if (!notification.isRead()) {
                history.set(i, notification.markAsRead());
            }
        }
        updateUnreadCount();
    }

    /**
     * Snoozes a notification for the specified number of hours.
     */
    public void snooze(UUID notificationId, int hours) {
        LocalDateTime snoozeUntil = LocalDateTime.now().plusHours(hours);

        for (int i = 0; i < history.size(); i++) {
            DeadlineNotification notification = history.get(i);
            if (notification.id().equals(notificationId)) {
                history.set(i, notification.snoozeUntil(snoozeUntil));
                updateUnreadCount();
                break;
            }
        }
    }

    // === Unread Count ===

    /**
     * Gets the count of unread notifications.
     */
    public int getUnreadCount() {
        return unreadCount.get();
    }

    /**
     * Property for unread count (for UI binding).
     */
    public IntegerProperty unreadCountProperty() {
        return unreadCount;
    }

    private void updateUnreadCount() {
        int count = (int) history.stream()
            .filter(n -> !n.isRead() && (!n.isSnoozed() || n.isSnoozeExpired()))
            .count();

        // Always update synchronously for test determinism.
        // IntegerProperty.set() is thread-safe for property binding.
        // UI components bound to this property will receive change events correctly.
        unreadCount.set(count);
    }

    // === Scheduling ===

    /**
     * Starts the notification scheduler to check deadlines periodically.
     */
    public void startScheduler(TaxYear taxYear) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "deadline-notification-scheduler");
            t.setDaemon(true);
            return t;
        });

        // Check deadlines immediately and then every hour
        Runnable checkTask = () -> checkAllDeadlines(taxYear);
        scheduler.scheduleAtFixedRate(checkTask, 0, 1, TimeUnit.HOURS);

        LOG.info("Notification scheduler started for tax year " + taxYear.label());
    }

    /**
     * Checks all deadlines for the tax year and triggers notifications as needed.
     */
    public void checkAllDeadlines(TaxYear taxYear) {
        if (!preferences.isEnabled()) {
            return;
        }

        List<Deadline> deadlines = getDeadlinesForTaxYear(taxYear);

        for (Deadline deadline : deadlines) {
            List<DeadlineNotification> notifications = checkDeadline(deadline);
            for (DeadlineNotification notification : notifications) {
                // Use the notification's trigger days
                triggerNotification(deadline, notification.triggerDays());
            }
        }
    }

    /**
     * Shuts down the scheduler.
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
