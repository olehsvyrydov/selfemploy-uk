package uk.selfemploy.ui.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.viewmodel.Deadline;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for SE-309: Deadline Notification Service.
 * Tests notification scheduling, preferences, and delivery.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SE-309: Deadline Notification Service")
class DeadlineNotificationServiceTest {

    private DeadlineNotificationService service;

    @Mock
    private Consumer<DeadlineNotification> notificationHandler;

    @BeforeEach
    void setUp() {
        service = new DeadlineNotificationService();
    }

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
    }

    // === Notification Preferences ===

    @Nested
    @DisplayName("Notification Preferences")
    class PreferencesTests {

        @Test
        @DisplayName("Default preferences enable all notification types")
        void defaultPreferencesEnableAllTypes() {
            NotificationPreferences prefs = service.getPreferences();

            assertThat(prefs.isEnabled()).isTrue();
            assertThat(prefs.isFilingDeadlineEnabled()).isTrue();
            assertThat(prefs.isPaymentDeadlineEnabled()).isTrue();
            assertThat(prefs.isQuarterlyReminderEnabled()).isTrue();
        }

        @Test
        @DisplayName("Default notification triggers are 14, 3, 0 days before")
        void defaultTriggerDays() {
            NotificationPreferences prefs = service.getPreferences();

            assertThat(prefs.getTriggerDays()).containsExactly(14, 3, 0);
        }

        @Test
        @DisplayName("Can disable all notifications")
        void canDisableAllNotifications() {
            NotificationPreferences prefs = service.getPreferences();
            prefs.setEnabled(false);

            assertThat(prefs.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Can disable specific notification types")
        void canDisableSpecificTypes() {
            NotificationPreferences prefs = service.getPreferences();
            prefs.setFilingDeadlineEnabled(false);
            prefs.setPaymentDeadlineEnabled(false);

            assertThat(prefs.isFilingDeadlineEnabled()).isFalse();
            assertThat(prefs.isPaymentDeadlineEnabled()).isFalse();
            assertThat(prefs.isQuarterlyReminderEnabled()).isTrue();
        }

        @Test
        @DisplayName("Can customize trigger days")
        void canCustomizeTriggerDays() {
            NotificationPreferences prefs = service.getPreferences();
            prefs.setTriggerDays(List.of(14, 3));

            assertThat(prefs.getTriggerDays()).containsExactly(14, 3);
        }
    }

    // === Deadline Detection ===

    @Nested
    @DisplayName("Deadline Detection")
    class DeadlineDetectionTests {

        @Test
        @DisplayName("Detects deadline 14 days away (T-14 trigger)")
        void detectsDeadline14DaysAway() {
            LocalDate deadlineDate = LocalDate.now().plusDays(14);
            Deadline deadline = Deadline.of("Test Deadline", deadlineDate);

            List<DeadlineNotification> notifications = service.checkDeadline(deadline);

            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).triggerDays()).isEqualTo(14);
        }

        @Test
        @DisplayName("Detects deadline 3 days away (T-3 trigger)")
        void detectsDeadline3DaysAway() {
            LocalDate deadlineDate = LocalDate.now().plusDays(3);
            Deadline deadline = Deadline.of("Test Deadline", deadlineDate);

            List<DeadlineNotification> notifications = service.checkDeadline(deadline);

            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).triggerDays()).isEqualTo(3);
        }

        @Test
        @DisplayName("Detects deadline today (T-0 trigger)")
        void detectsDeadlineTodayInDeadlineDetection() {
            LocalDate deadlineDate = LocalDate.now();
            Deadline deadline = Deadline.of("Test Deadline", deadlineDate);

            List<DeadlineNotification> notifications = service.checkDeadline(deadline);

            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).triggerDays()).isZero();
        }

        @Test
        @DisplayName("No notification for deadline 15 days away")
        void noNotificationFor15DaysAway() {
            LocalDate deadlineDate = LocalDate.now().plusDays(15);
            Deadline deadline = Deadline.of("Test Deadline", deadlineDate);

            List<DeadlineNotification> notifications = service.checkDeadline(deadline);

            assertThat(notifications).isEmpty();
        }

        @Test
        @DisplayName("No notification for past deadlines")
        void noNotificationForPastDeadlines() {
            LocalDate deadlineDate = LocalDate.now().minusDays(5);
            Deadline deadline = Deadline.of("Test Deadline", deadlineDate);

            List<DeadlineNotification> notifications = service.checkDeadline(deadline);

            assertThat(notifications).isEmpty();
        }

        @Test
        @DisplayName("Deadline today triggers urgent notification")
        void deadlineTodayTriggersUrgent() {
            LocalDate deadlineDate = LocalDate.now();
            Deadline deadline = Deadline.of("Test Deadline", deadlineDate);

            List<DeadlineNotification> notifications = service.checkDeadline(deadline);

            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).triggerDays()).isEqualTo(0);
            assertThat(notifications.get(0).priority()).isEqualTo(NotificationPriority.CRITICAL);
        }
    }

    // === Tax Year Deadlines ===

    @Nested
    @DisplayName("Tax Year Deadlines")
    class TaxYearDeadlinesTests {

        @Test
        @DisplayName("Generates deadlines from tax year")
        void generatesDeadlinesFromTaxYear() {
            TaxYear taxYear = TaxYear.of(2025);

            List<Deadline> deadlines = service.getDeadlinesForTaxYear(taxYear);

            assertThat(deadlines).hasSizeGreaterThanOrEqualTo(3);

            List<String> labels = deadlines.stream().map(Deadline::label).toList();
            assertThat(labels).contains("Online Filing Deadline");
            assertThat(labels).contains("Payment Due");
            assertThat(labels).contains("Payment on Account Due");
        }

        @Test
        @DisplayName("Filing deadline is 31 January following tax year")
        void filingDeadlineCorrect() {
            TaxYear taxYear = TaxYear.of(2025); // 2025/26 tax year

            List<Deadline> deadlines = service.getDeadlinesForTaxYear(taxYear);
            Deadline filingDeadline = deadlines.stream()
                .filter(d -> d.label().contains("Filing"))
                .findFirst()
                .orElseThrow();

            // For 2025/26 tax year, filing deadline is 31 Jan 2027
            assertThat(filingDeadline.date()).isEqualTo(LocalDate.of(2027, 1, 31));
        }

        @Test
        @DisplayName("Payment deadline is 31 January following tax year")
        void paymentDeadlineCorrect() {
            TaxYear taxYear = TaxYear.of(2025);

            List<Deadline> deadlines = service.getDeadlinesForTaxYear(taxYear);
            Deadline paymentDeadline = deadlines.stream()
                .filter(d -> d.label().equals("Payment Due"))
                .findFirst()
                .orElseThrow();

            assertThat(paymentDeadline.date()).isEqualTo(LocalDate.of(2027, 1, 31));
        }

        @Test
        @DisplayName("POA deadline is 31 July following filing")
        void poaDeadlineCorrect() {
            TaxYear taxYear = TaxYear.of(2025);

            List<Deadline> deadlines = service.getDeadlinesForTaxYear(taxYear);
            Deadline poaDeadline = deadlines.stream()
                .filter(d -> d.label().contains("Account"))
                .findFirst()
                .orElseThrow();

            // POA is 6 months after payment deadline
            assertThat(poaDeadline.date()).isEqualTo(LocalDate.of(2027, 7, 31));
        }
    }

    // === Notification Delivery ===

    @Nested
    @DisplayName("Notification Delivery")
    class DeliveryTests {

        @Test
        @DisplayName("Registers notification handler")
        void registersNotificationHandler() {
            service.setNotificationHandler(notificationHandler);

            assertThat(service.hasNotificationHandler()).isTrue();
        }

        @Test
        @DisplayName("Calls handler when notification triggered")
        void callsHandlerWhenTriggered() {
            service.setNotificationHandler(notificationHandler);

            LocalDate deadlineDate = LocalDate.now().plusDays(7);
            Deadline deadline = Deadline.of("Test Deadline", deadlineDate);

            service.triggerNotification(deadline, 7);

            verify(notificationHandler, times(1)).accept(any(DeadlineNotification.class));
        }

        @Test
        @DisplayName("Notification contains deadline details")
        void notificationContainsDetails() {
            service.setNotificationHandler(notification -> {
                assertThat(notification.title()).contains("Deadline");
                assertThat(notification.message()).contains("7 days");
                assertThat(notification.deadline().label()).isEqualTo("Test Deadline");
            });

            LocalDate deadlineDate = LocalDate.now().plusDays(7);
            Deadline deadline = Deadline.of("Test Deadline", deadlineDate);

            service.triggerNotification(deadline, 7);
        }
    }

    // === Notification History ===

    @Nested
    @DisplayName("Notification History")
    class HistoryTests {

        @Test
        @DisplayName("Stores notification history")
        void storesNotificationHistory() {
            LocalDate deadlineDate = LocalDate.now().plusDays(7);
            Deadline deadline = Deadline.of("Test Deadline", deadlineDate);

            service.triggerNotification(deadline, 7);

            assertThat(service.getNotificationHistory()).hasSize(1);
        }

        @Test
        @DisplayName("History ordered by most recent first")
        void historyOrderedByRecent() {
            Deadline deadline1 = Deadline.of("Deadline 1", LocalDate.now().plusDays(30));
            Deadline deadline2 = Deadline.of("Deadline 2", LocalDate.now().plusDays(7));

            service.triggerNotification(deadline1, 30);
            service.triggerNotification(deadline2, 7);

            List<DeadlineNotification> history = service.getNotificationHistory();
            assertThat(history.get(0).deadline().label()).isEqualTo("Deadline 2");
        }

        @Test
        @DisplayName("Can clear notification history")
        void canClearHistory() {
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            service.triggerNotification(deadline, 7);

            service.clearHistory();

            assertThat(service.getNotificationHistory()).isEmpty();
        }

        @Test
        @DisplayName("Can mark notification as read")
        void canMarkAsRead() {
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            service.triggerNotification(deadline, 7);

            DeadlineNotification notification = service.getNotificationHistory().get(0);
            service.markAsRead(notification.id());

            DeadlineNotification updated = service.getNotificationHistory().get(0);
            assertThat(updated.isRead()).isTrue();
        }

        @Test
        @DisplayName("Unread count updates correctly")
        void unreadCountUpdates() {
            Deadline deadline1 = Deadline.of("D1", LocalDate.now().plusDays(30));
            Deadline deadline2 = Deadline.of("D2", LocalDate.now().plusDays(7));

            service.triggerNotification(deadline1, 30);
            service.triggerNotification(deadline2, 7);

            assertThat(service.getUnreadCount()).isEqualTo(2);

            DeadlineNotification first = service.getNotificationHistory().get(0);
            service.markAsRead(first.id());

            assertThat(service.getUnreadCount()).isEqualTo(1);
        }
    }

    // === Snooze Functionality ===

    @Nested
    @DisplayName("Snooze Functionality")
    class SnoozeTests {

        @Test
        @DisplayName("Can snooze notification for specified hours")
        void canSnoozeNotification() {
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            service.triggerNotification(deadline, 7);

            DeadlineNotification notification = service.getNotificationHistory().get(0);
            service.snooze(notification.id(), 24); // Snooze for 24 hours

            DeadlineNotification snoozed = service.getNotificationHistory().get(0);
            assertThat(snoozed.isSnoozed()).isTrue();
        }

        @Test
        @DisplayName("Snoozed notification not counted as unread")
        void snoozedNotCountedUnread() {
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            service.triggerNotification(deadline, 7);

            assertThat(service.getUnreadCount()).isEqualTo(1);

            DeadlineNotification notification = service.getNotificationHistory().get(0);
            service.snooze(notification.id(), 24);

            assertThat(service.getUnreadCount()).isEqualTo(0);
        }
    }

    // === Thread Safety ===

    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafetyTests {

        @Test
        @DisplayName("updateUnreadCount can be called from background thread without exception")
        void updateUnreadCountCanBeCalledFromBackgroundThread() throws Exception {
            // This test verifies that updateUnreadCount() handles threading correctly
            // by not throwing IllegalStateException when called from a background thread
            CountDownLatch latch = new CountDownLatch(1);
            final Exception[] caughtException = {null};

            // Add a notification first (to have something to count)
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            service.triggerNotification(deadline, 7);

            // Now trigger updateUnreadCount from a background thread
            Thread backgroundThread = new Thread(() -> {
                try {
                    // Mark as read to trigger updateUnreadCount
                    DeadlineNotification notification = service.getNotificationHistory().get(0);
                    service.markAsRead(notification.id());
                } catch (Exception e) {
                    caughtException[0] = e;
                } finally {
                    latch.countDown();
                }
            });

            backgroundThread.start();
            boolean completed = latch.await(5, TimeUnit.SECONDS);

            assertThat(completed).isTrue();
            assertThat(caughtException[0]).isNull();
            assertThat(service.getUnreadCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("unreadCount property updates correctly when called from any thread")
        void unreadCountPropertyUpdatesCorrectly() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            final int[] observedCount = {-1};

            // Add two notifications
            Deadline d1 = Deadline.of("D1", LocalDate.now().plusDays(30));
            Deadline d2 = Deadline.of("D2", LocalDate.now().plusDays(7));
            service.triggerNotification(d1, 30);
            service.triggerNotification(d2, 7);

            assertThat(service.getUnreadCount()).isEqualTo(2);

            // Mark one as read from background thread
            Thread backgroundThread = new Thread(() -> {
                try {
                    DeadlineNotification first = service.getNotificationHistory().get(0);
                    service.markAsRead(first.id());
                    observedCount[0] = service.getUnreadCount();
                } finally {
                    latch.countDown();
                }
            });

            backgroundThread.start();
            boolean completed = latch.await(5, TimeUnit.SECONDS);

            assertThat(completed).isTrue();
            assertThat(observedCount[0]).isEqualTo(1);
        }
    }

    // === Duplicate Prevention ===

    @Nested
    @DisplayName("Duplicate Prevention")
    class DuplicatePreventionTests {

        @Test
        @DisplayName("Does not trigger same notification twice on same day")
        void preventsDuplicateSameDay() {
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));

            service.triggerNotification(deadline, 7);
            service.triggerNotification(deadline, 7);

            assertThat(service.getNotificationHistory()).hasSize(1);
        }

        @Test
        @DisplayName("Allows same deadline different trigger days")
        void allowsDifferentTriggerDays() {
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));

            service.triggerNotification(deadline, 30);
            service.triggerNotification(deadline, 7);

            assertThat(service.getNotificationHistory()).hasSize(2);
        }
    }

    // === Priority Levels ===

    @Nested
    @DisplayName("Notification Priority")
    class PriorityTests {

        @Test
        @DisplayName("14 days triggers LOW priority ( cadence)")
        void fourteenDaysIsLow() {
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(14));
            List<DeadlineNotification> notifications = service.checkDeadline(deadline);

            assertThat(notifications.get(0).priority()).isEqualTo(NotificationPriority.LOW);
        }

        @Test
        @DisplayName("3 days triggers MEDIUM priority ( cadence)")
        void threeDaysIsMedium() {
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(3));
            List<DeadlineNotification> notifications = service.checkDeadline(deadline);

            assertThat(notifications.get(0).priority()).isEqualTo(NotificationPriority.MEDIUM);
        }

        @Test
        @DisplayName("1 day still triggers HIGH priority when a user configures a 1-day reminder")
        void oneDayIsHigh() {
            // Default cadence is T-14/T-3/T-0 — HIGH priority (1 day) is unreachable via
            // defaults by design. This test verifies the priority mapping still works for
            // users who reinstate a 1-day reminder via NotificationPreferences.
            service.getPreferences().setTriggerDays(java.util.List.of(1));

            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(1));
            List<DeadlineNotification> notifications = service.checkDeadline(deadline);

            assertThat(notifications.get(0).priority()).isEqualTo(NotificationPriority.HIGH);
        }

        @Test
        @DisplayName("Today triggers CRITICAL priority")
        void todayIsCritical() {
            Deadline deadline = Deadline.of("Test", LocalDate.now());
            List<DeadlineNotification> notifications = service.checkDeadline(deadline);

            assertThat(notifications.get(0).priority()).isEqualTo(NotificationPriority.CRITICAL);
        }
    }

    @Nested
    @DisplayName("Obligation-specific reminder offsets (fixed clock)")
    class ObligationOffsets {

        private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);

        @BeforeEach
        void fixClock() {
            service.setClock(fixedAt(TODAY));
        }

        @Test
        @DisplayName("Annual Self Assessment deadlines remind at T-60, T-30 and T-7")
        void annualOffsets() {
            assertThat(triggerDaysFor("Online Filing Deadline", 60)).containsExactly(60);
            assertThat(triggerDaysFor("Online Filing Deadline", 30)).containsExactly(30);
            assertThat(triggerDaysFor("Online Filing Deadline", 7)).containsExactly(7);
        }

        @Test
        @DisplayName("Annual deadlines do not use the generic T-14 offset")
        void annualSkipsGenericOffset() {
            assertThat(service.checkDeadline(Deadline.of("Online Filing Deadline", TODAY.plusDays(14))))
                .isEmpty();
        }

        @Test
        @DisplayName("MTD quarterly updates remind at T-30, T-7 and T-1")
        void quarterlyOffsets() {
            assertThat(triggerDaysFor("MTD Q1 Update Due", 30)).containsExactly(30);
            assertThat(triggerDaysFor("MTD Q1 Update Due", 7)).containsExactly(7);
            assertThat(triggerDaysFor("MTD Q1 Update Due", 1)).containsExactly(1);
        }

        @Test
        @DisplayName("A deadline on the day still fires an urgent reminder")
        void dayOfFires() {
            List<DeadlineNotification> n = service.checkDeadline(Deadline.of("Payment Due", TODAY));
            assertThat(n).hasSize(1);
            assertThat(n.get(0).triggerDays()).isZero();
        }

        @Test
        @DisplayName("checkAllDeadlines fires the quarterly T-1 reminder when nothing is snoozed")
        void schedulerFiresQuarterlyT1() {
            TaxYear year = TaxYear.of(2026);
            Deadline quarterly = firstQuarterly(year);

            service.setClock(fixedAt(quarterly.date().minusDays(1)));
            service.checkAllDeadlines(year);

            assertThat(countFired(quarterly, 1)).isEqualTo(1);
        }

        @Test
        @DisplayName("An active snooze suppresses a later reminder for the same deadline")
        void activeSnoozeSuppressesLaterReminder() {
            TaxYear year = TaxYear.of(2026);
            Deadline quarterly = firstQuarterly(year);

            // T-7: fire the reminder, then snooze it for a week.
            service.setClock(fixedAt(quarterly.date().minusDays(7)));
            service.checkAllDeadlines(year);
            service.getNotificationHistory().stream()
                .filter(n -> n.deadline().label().equals(quarterly.label()))
                .forEach(n -> service.snooze(n.id(), 168));

            // T-1: while the snooze is still active, the T-1 reminder must not fire.
            service.setClock(fixedAt(quarterly.date().minusDays(1)));
            service.checkAllDeadlines(year);

            assertThat(countFired(quarterly, 1)).isZero();
        }

        private Deadline firstQuarterly(TaxYear year) {
            return service.getDeadlinesForTaxYear(year).stream()
                .filter(d -> d.label().toLowerCase(java.util.Locale.ROOT).contains("mtd"))
                .findFirst().orElseThrow();
        }

        private long countFired(Deadline deadline, int triggerDays) {
            return service.getNotificationHistory().stream()
                .filter(n -> n.deadline().label().equals(deadline.label())
                    && n.triggerDays() == triggerDays)
                .count();
        }

        private List<Integer> triggerDaysFor(String label, int daysAway) {
            return service.checkDeadline(Deadline.of(label, TODAY.plusDays(daysAway)))
                .stream().map(DeadlineNotification::triggerDays).toList();
        }
    }

    private static Clock fixedAt(LocalDate date) {
        return Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    }

    @Nested
    @DisplayName("Read/snooze state persistence across restarts")
    class StatePersistence {

        /** In-memory store standing in for the SQLite one, so persistence is tested without a DB. */
        private final NotificationStateStore store = new NotificationStateStore() {
            private final java.util.Map<String, PersistedState> map = new java.util.HashMap<>();

            @Override
            public void save(String key, boolean read, java.time.LocalDateTime snoozeUntil) {
                map.put(key, new PersistedState(read, snoozeUntil));
            }

            @Override
            public java.util.Map<String, PersistedState> loadAll() {
                return new java.util.HashMap<>(map);
            }
        };

        private final Deadline deadline = Deadline.of("Payment Due", LocalDate.now().plusDays(30));

        @Test
        @DisplayName("a dismissed reminder stays dismissed after the app restarts")
        void dismissedReminderStaysDismissed() {
            DeadlineNotificationService first = new DeadlineNotificationService();
            first.setStateStore(store);
            first.triggerNotification(deadline, 30);
            first.markAsRead(first.getNotificationHistory().get(0).id());

            // Simulate a restart: a fresh service loading the same store regenerates the reminder.
            DeadlineNotificationService restarted = new DeadlineNotificationService();
            restarted.setStateStore(store);
            restarted.triggerNotification(deadline, 30);

            assertThat(restarted.getNotificationHistory().get(0).isRead()).isTrue();
            assertThat(restarted.getUnreadCount()).isZero();
            restarted.shutdown();
            first.shutdown();
        }

        @Test
        @DisplayName("a snoozed reminder stays snoozed after the app restarts")
        void snoozedReminderStaysSnoozed() {
            DeadlineNotificationService first = new DeadlineNotificationService();
            first.setStateStore(store);
            first.triggerNotification(deadline, 30);
            first.snooze(first.getNotificationHistory().get(0).id(), 168); // one week

            DeadlineNotificationService restarted = new DeadlineNotificationService();
            restarted.setStateStore(store);
            restarted.triggerNotification(deadline, 30);

            assertThat(restarted.getNotificationHistory().get(0).isSnoozed()).isTrue();
            assertThat(restarted.getUnreadCount()).isZero();
            restarted.shutdown();
            first.shutdown();
        }
    }
}
