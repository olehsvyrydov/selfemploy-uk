package uk.selfemploy.ui.service;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.viewmodel.Deadline;

import java.time.Clock;
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

    // Obligation-specific reminder offsets (days before the deadline). Annual Self Assessment
    // deadlines warn earlier and for longer; MTD quarterly updates warn on a tighter cadence.
    private static final List<Integer> ANNUAL_OFFSETS = List.of(60, 30, 7);
    private static final List<Integer> QUARTERLY_OFFSETS = List.of(30, 7, 1);

    // Injectable time source so reminder scheduling can be tested deterministically.
    private Clock clock = Clock.systemDefaultZone();

    // Persists read/snooze state across restarts; defaults to a no-op until a real store is wired in.
    private NotificationStateStore stateStore = NotificationStateStore.NOOP;
    private Map<String, NotificationStateStore.PersistedState> persistedState = new ConcurrentHashMap<>();

    public DeadlineNotificationService() {
        // Initialize with default preferences
    }

    /** Overrides the time source (tests only); production uses the system clock. */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Wires the store used to persist read/snooze state across restarts and loads any existing state
     * so regenerated reminders inherit it.
     */
    public void setStateStore(NotificationStateStore stateStore) {
        this.stateStore = stateStore != null ? stateStore : NotificationStateStore.NOOP;
        this.persistedState = new ConcurrentHashMap<>(this.stateStore.loadAll());
    }

    private void persist(DeadlineNotification notification) {
        NotificationStateStore.PersistedState state =
            new NotificationStateStore.PersistedState(notification.isRead(), notification.snoozeUntil());
        persistedState.put(notification.stableKey(), state);
        try {
            stateStore.save(notification.stableKey(), notification.isRead(), notification.snoozeUntil());
        } catch (RuntimeException e) {
            LOG.warning("Failed to persist notification state: " + e.getMessage());
        }
    }

    private DeadlineNotification applyPersistedState(DeadlineNotification notification) {
        NotificationStateStore.PersistedState state = persistedState.get(notification.stableKey());
        if (state == null) {
            return notification;
        }
        DeadlineNotification result = notification;
        if (state.read()) {
            result = result.markAsRead();
        }
        if (state.snoozeUntil() != null && LocalDateTime.now().isBefore(state.snoozeUntil())) {
            result = result.snoozeUntil(state.snoozeUntil());
        }
        return result;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
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

        long daysRemaining = ChronoUnit.DAYS.between(today(), deadline.date());
        if (daysRemaining < 0) {
            return Collections.emptyList(); // deadline already passed
        }

        List<DeadlineNotification> notifications = new ArrayList<>();

        // Check if deadline is today (special case)
        if (daysRemaining == 0) {
            notifications.add(DeadlineNotification.create(deadline, 0));
            return notifications;
        }

        // Fire at each reminder offset appropriate to this obligation type.
        for (int triggerDay : offsetsFor(deadline)) {
            if (daysRemaining == triggerDay) {
                notifications.add(DeadlineNotification.create(deadline, triggerDay));
            }
        }

        return notifications;
    }

    /**
     * Returns the reminder offsets (days before) for a deadline: MTD quarterly updates use a tight
     * cadence, annual Self Assessment deadlines a longer one, and any other deadline falls back to
     * the user-configurable preference.
     */
    private List<Integer> offsetsFor(Deadline deadline) {
        String label = deadline.label().toLowerCase(Locale.ROOT);
        if (label.contains("mtd") || label.contains("quarter")) {
            return QUARTERLY_OFFSETS;
        }
        if (label.contains("filing") || label.contains("payment")
                || label.contains("account") || label.contains("annual")) {
            return ANNUAL_OFFSETS;
        }
        return preferences.getTriggerDays();
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
        // MTD ITSA quarterly deadlines fall on the 7th of the month following quarter end
        // (Obligations API v3 cadence, aligned with VAT MTD; deployed in production 2026-03-24).
        // The Quarter enum already encodes the correct 7th-of-month dates; this method delegates
        // to it so the source of truth is one place. Previously this method
        // hardcoded the 5th — wrong cadence, late-submission penalty risk under FA 2021 Sch 24.
        for (Quarter quarter : Quarter.values()) {
            deadlines.add(Deadline.of(
                "MTD " + quarter.name() + " Update Due",
                quarter.getDeadline(taxYear)
            ));
        }
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

        // Create notification, inheriting any read/snooze state persisted from a previous run so a
        // dismissed or snoozed reminder does not re-nag after a restart.
        DeadlineNotification notification = applyPersistedState(
            DeadlineNotification.create(deadline, triggerDays));

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
        return deadline.label() + "_" + deadline.date() + "_" + triggerDays + "_" + today();
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
                DeadlineNotification updated = notification.markAsRead();
                history.set(i, updated);
                persist(updated);
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
                DeadlineNotification updated = notification.markAsRead();
                history.set(i, updated);
                persist(updated);
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
                DeadlineNotification updated = notification.snoozeUntil(snoozeUntil);
                history.set(i, updated);
                persist(updated);
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

        // JavaFX property listeners may update UI components, which must happen on
        // the JavaFX Application Thread. If we're on a background thread (e.g., from
        // the scheduler or a CompletableFuture callback), we must use Platform.runLater()
        // to safely update the property and avoid IllegalStateException.
        //
        // Note: In unit tests where the JavaFX toolkit isn't initialized,
        // Platform.isFxApplicationThread() will throw IllegalStateException.
        // We catch this and update synchronously, which is safe in non-UI contexts.
        try {
            if (javafx.application.Platform.isFxApplicationThread()) {
                unreadCount.set(count);
            } else {
                javafx.application.Platform.runLater(() -> unreadCount.set(count));
            }
        } catch (IllegalStateException e) {
            // JavaFX toolkit not initialized (e.g., in unit tests without Application)
            // Safe to update synchronously since there's no UI thread to conflict with
            unreadCount.set(count);
        }
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
            // Respect snooze: don't re-nag while an unexpired snoozed reminder for this deadline exists.
            if (hasActiveSnooze(deadline)) {
                continue;
            }
            List<DeadlineNotification> notifications = checkDeadline(deadline);
            for (DeadlineNotification notification : notifications) {
                // Use the notification's trigger days
                triggerNotification(deadline, notification.triggerDays());
            }
        }
    }

    private boolean hasActiveSnooze(Deadline deadline) {
        return history.stream().anyMatch(n ->
            n.deadline().label().equals(deadline.label())
                && n.isSnoozed() && !n.isSnoozeExpired());
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
