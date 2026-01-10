package uk.selfemploy.ui.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.viewmodel.Deadline;

import java.time.LocalDate;
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
        @DisplayName("Default notification triggers are 30, 7, 1 days before")
        void defaultTriggerDays() {
            NotificationPreferences prefs = service.getPreferences();

            assertThat(prefs.getTriggerDays()).containsExactly(30, 7, 1);
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
        @DisplayName("Detects deadline 30 days away")
        void detectsDeadline30DaysAway() {
            LocalDate deadlineDate = LocalDate.now().plusDays(30);
            Deadline deadline = Deadline.of("Test Deadline", deadlineDate);

            List<DeadlineNotification> notifications = service.checkDeadline(deadline);

            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).triggerDays()).isEqualTo(30);
        }

        @Test
        @DisplayName("Detects deadline 7 days away")
        void detectsDeadline7DaysAway() {
            LocalDate deadlineDate = LocalDate.now().plusDays(7);
            Deadline deadline = Deadline.of("Test Deadline", deadlineDate);

            List<DeadlineNotification> notifications = service.checkDeadline(deadline);

            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).triggerDays()).isEqualTo(7);
        }

        @Test
        @DisplayName("Detects deadline 1 day away")
        void detectsDeadline1DayAway() {
            LocalDate deadlineDate = LocalDate.now().plusDays(1);
            Deadline deadline = Deadline.of("Test Deadline", deadlineDate);

            List<DeadlineNotification> notifications = service.checkDeadline(deadline);

            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).triggerDays()).isEqualTo(1);
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
        @DisplayName("30 days triggers LOW priority")
        void thirtyDaysIsLow() {
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(30));
            List<DeadlineNotification> notifications = service.checkDeadline(deadline);

            assertThat(notifications.get(0).priority()).isEqualTo(NotificationPriority.LOW);
        }

        @Test
        @DisplayName("7 days triggers MEDIUM priority")
        void sevenDaysIsMedium() {
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            List<DeadlineNotification> notifications = service.checkDeadline(deadline);

            assertThat(notifications.get(0).priority()).isEqualTo(NotificationPriority.MEDIUM);
        }

        @Test
        @DisplayName("1 day triggers HIGH priority")
        void oneDayIsHigh() {
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
}
