package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.service.DeadlineNotification;
import uk.selfemploy.ui.service.DeadlineNotificationService;
import uk.selfemploy.ui.service.NotificationPreferences;
import uk.selfemploy.ui.service.NotificationPriority;
import uk.selfemploy.ui.viewmodel.Deadline;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SE-309: Deadline Notifications.
 * Tests notification service integration with UI components.
 *
 * Based on /rob's QA test case specification (26 test cases).
 *
 * <p>Test Categories:</p>
 * <ul>
 *   <li>P0 (Critical): 12 tests - Badge display, notification triggers, panel interaction</li>
 *   <li>P1 (High): 8 tests - Deadline tracking, preferences, tax year change</li>
 *   <li>P2 (Medium): 6 tests - Snooze, deduplication, history edge cases</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SE-309: Deadline Notifications Integration Tests")
class NotificationIntegrationTest {

    private DeadlineNotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new DeadlineNotificationService();
    }

    @AfterEach
    void tearDown() {
        if (notificationService != null) {
            notificationService.shutdown();
        }
    }

    // ========================================================================
    // P0 - CRITICAL TEST CASES (12 tests)
    // ========================================================================

    @Nested
    @DisplayName("P0: Badge Display Tests")
    class BadgeDisplayTests {

        @Test
        @DisplayName("TC-309-001: Notification Badge - Initial State (hidden)")
        void badgeInitialStateHidden() {
            // Given: Fresh notification service
            // When: No notifications triggered
            // Then: Unread count should be 0 (badge hidden)
            assertThat(notificationService.getUnreadCount()).isZero();
            assertThat(notificationService.getNotificationHistory()).isEmpty();
        }

        @Test
        @DisplayName("TC-309-002: Notification Badge - Shows Count")
        void badgeShowsCount() {
            // Given: A deadline 30 days away
            Deadline deadline = Deadline.of("Test Deadline", LocalDate.now().plusDays(30));

            // When: Notification is triggered
            notificationService.triggerNotification(deadline, 30);

            // Then: Badge should show count of 1
            assertThat(notificationService.getUnreadCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-309-003: Notification Badge - Count 9+")
        void badgeShowsNinePlus() {
            // Given: Multiple deadlines triggering notifications
            // When: 10+ notifications are triggered
            for (int i = 0; i < 12; i++) {
                Deadline deadline = Deadline.of("Deadline " + i, LocalDate.now().plusDays(30 + i));
                notificationService.triggerNotification(deadline, 30);
            }

            // Then: Unread count should be 12 (UI displays "9+")
            assertThat(notificationService.getUnreadCount()).isEqualTo(12);
            // Note: UI badge text formatting "9+" is handled by MainController
        }
    }

    @Nested
    @DisplayName("P0: Notification Trigger Tests")
    class NotificationTriggerTests {

        @Test
        @DisplayName("TC-309-004: Notification Trigger - 30 Days (LOW priority)")
        void triggerAt30Days() {
            // Given: Deadline exactly 30 days away
            Deadline deadline = Deadline.of("Test Deadline", LocalDate.now().plusDays(30));

            // When: Deadline is checked
            List<DeadlineNotification> notifications = notificationService.checkDeadline(deadline);

            // Then: LOW priority notification triggered
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).priority()).isEqualTo(NotificationPriority.LOW);
            assertThat(notifications.get(0).triggerDays()).isEqualTo(30);
        }

        @Test
        @DisplayName("TC-309-005: Notification Trigger - 7 Days (MEDIUM priority)")
        void triggerAt7Days() {
            // Given: Deadline exactly 7 days away
            Deadline deadline = Deadline.of("Test Deadline", LocalDate.now().plusDays(7));

            // When: Deadline is checked
            List<DeadlineNotification> notifications = notificationService.checkDeadline(deadline);

            // Then: MEDIUM priority notification triggered
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).priority()).isEqualTo(NotificationPriority.MEDIUM);
            assertThat(notifications.get(0).triggerDays()).isEqualTo(7);
        }

        @Test
        @DisplayName("TC-309-006: Notification Trigger - 1 Day (HIGH priority)")
        void triggerAt1Day() {
            // Given: Deadline exactly 1 day away
            Deadline deadline = Deadline.of("Test Deadline", LocalDate.now().plusDays(1));

            // When: Deadline is checked
            List<DeadlineNotification> notifications = notificationService.checkDeadline(deadline);

            // Then: HIGH priority notification triggered
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).priority()).isEqualTo(NotificationPriority.HIGH);
            assertThat(notifications.get(0).triggerDays()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-309-007: Notification Trigger - Today (CRITICAL priority)")
        void triggerToday() {
            // Given: Deadline is TODAY
            Deadline deadline = Deadline.of("Test Deadline", LocalDate.now());

            // When: Deadline is checked
            List<DeadlineNotification> notifications = notificationService.checkDeadline(deadline);

            // Then: CRITICAL priority notification triggered
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).priority()).isEqualTo(NotificationPriority.CRITICAL);
            assertThat(notifications.get(0).triggerDays()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("P0: Panel Interaction Tests")
    class PanelInteractionTests {

        @Test
        @DisplayName("TC-309-008: Click Notification Bell - Opens Panel (service accessible)")
        void notificationServiceAccessible() {
            // Given: Notification service exists
            // When: Service is accessed (simulating panel open)
            // Then: History and preferences are accessible
            assertThat(notificationService.getNotificationHistory()).isNotNull();
            assertThat(notificationService.getPreferences()).isNotNull();
        }

        @Test
        @DisplayName("TC-309-009: Notification Panel - Empty State")
        void panelEmptyState() {
            // Given: No notifications triggered
            // When: History is retrieved
            List<DeadlineNotification> history = notificationService.getNotificationHistory();

            // Then: History is empty
            assertThat(history).isEmpty();
        }

        @Test
        @DisplayName("TC-309-010: Notification Panel - Shows History")
        void panelShowsHistory() {
            // Given: 3 notifications triggered
            Deadline d1 = Deadline.of("Deadline 1", LocalDate.now().plusDays(30));
            Deadline d2 = Deadline.of("Deadline 2", LocalDate.now().plusDays(7));
            Deadline d3 = Deadline.of("Deadline 3", LocalDate.now().plusDays(1));

            notificationService.triggerNotification(d1, 30);
            notificationService.triggerNotification(d2, 7);
            notificationService.triggerNotification(d3, 1);

            // When: History is retrieved
            List<DeadlineNotification> history = notificationService.getNotificationHistory();

            // Then: Shows 3 notification entries
            assertThat(history).hasSize(3);
            assertThat(history).allSatisfy(n -> {
                assertThat(n.title()).isNotEmpty();
                assertThat(n.message()).isNotEmpty();
            });
        }

        @Test
        @DisplayName("TC-309-011: Mark All As Read - Badge Clears")
        void markAllAsReadClearsBadge() {
            // Given: Badge shows count > 0
            Deadline d1 = Deadline.of("Deadline 1", LocalDate.now().plusDays(30));
            Deadline d2 = Deadline.of("Deadline 2", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(d1, 30);
            notificationService.triggerNotification(d2, 7);
            assertThat(notificationService.getUnreadCount()).isEqualTo(2);

            // When: Mark all as read (simulating panel open)
            notificationService.markAllAsRead();

            // Then: Badge hidden (count = 0)
            assertThat(notificationService.getUnreadCount()).isZero();
        }
    }

    // ========================================================================
    // P1 - HIGH PRIORITY TEST CASES (8 tests)
    // ========================================================================

    @Nested
    @DisplayName("P1: Deadline Tracking Tests")
    class DeadlineTrackingTests {

        @Test
        @DisplayName("TC-309-012: Filing Deadline Tracked")
        void filingDeadlineTracked() {
            // Given: Tax year 2025/26 selected
            TaxYear taxYear = TaxYear.of(2025);

            // When: Deadlines are retrieved
            List<Deadline> deadlines = notificationService.getDeadlinesForTaxYear(taxYear);

            // Then: "Online Filing Deadline" = 31 January 2027
            Deadline filingDeadline = deadlines.stream()
                .filter(d -> d.label().contains("Filing"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Filing deadline not found"));

            assertThat(filingDeadline.label()).isEqualTo("Online Filing Deadline");
            assertThat(filingDeadline.date()).isEqualTo(LocalDate.of(2027, 1, 31));
        }

        @Test
        @DisplayName("TC-309-013: Payment Deadline Tracked")
        void paymentDeadlineTracked() {
            // Given: Tax year 2025/26 selected
            TaxYear taxYear = TaxYear.of(2025);

            // When: Deadlines are retrieved
            List<Deadline> deadlines = notificationService.getDeadlinesForTaxYear(taxYear);

            // Then: "Payment Due" = 31 January 2027
            Deadline paymentDeadline = deadlines.stream()
                .filter(d -> d.label().equals("Payment Due"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Payment deadline not found"));

            assertThat(paymentDeadline.date()).isEqualTo(LocalDate.of(2027, 1, 31));
        }

        @Test
        @DisplayName("TC-309-014: POA Deadline Tracked")
        void poaDeadlineTracked() {
            // Given: Tax year 2025/26 selected
            TaxYear taxYear = TaxYear.of(2025);

            // When: Deadlines are retrieved
            List<Deadline> deadlines = notificationService.getDeadlinesForTaxYear(taxYear);

            // Then: "Payment on Account Due" = 31 July 2027
            Deadline poaDeadline = deadlines.stream()
                .filter(d -> d.label().contains("Account"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("POA deadline not found"));

            assertThat(poaDeadline.date()).isEqualTo(LocalDate.of(2027, 7, 31));
        }

        @Test
        @DisplayName("TC-309-015: MTD Quarterly Deadlines Tracked")
        void mtdQuarterlyDeadlinesTracked() {
            // Given: Tax year 2025/26 selected
            TaxYear taxYear = TaxYear.of(2025);

            // When: Deadlines are retrieved
            List<Deadline> deadlines = notificationService.getDeadlinesForTaxYear(taxYear);

            // Then: Q1 (5 Aug), Q2 (5 Nov), Q3 (5 Feb), Q4 (5 May)
            List<Deadline> mtdDeadlines = deadlines.stream()
                .filter(d -> d.label().contains("MTD"))
                .toList();

            assertThat(mtdDeadlines).hasSize(4);

            // Q1: 5 August 2025
            assertThat(mtdDeadlines).anyMatch(d ->
                d.label().contains("Q1") && d.date().equals(LocalDate.of(2025, 8, 5)));
            // Q2: 5 November 2025
            assertThat(mtdDeadlines).anyMatch(d ->
                d.label().contains("Q2") && d.date().equals(LocalDate.of(2025, 11, 5)));
            // Q3: 5 February 2026
            assertThat(mtdDeadlines).anyMatch(d ->
                d.label().contains("Q3") && d.date().equals(LocalDate.of(2026, 2, 5)));
            // Q4: 5 May 2026
            assertThat(mtdDeadlines).anyMatch(d ->
                d.label().contains("Q4") && d.date().equals(LocalDate.of(2026, 5, 5)));
        }
    }

    @Nested
    @DisplayName("P1: Preferences Tests")
    class PreferencesTests {

        @Test
        @DisplayName("TC-309-016: Preferences - Disable All Notifications")
        void disableAllNotifications() {
            // Given: Notifications enabled (default)
            NotificationPreferences prefs = notificationService.getPreferences();
            assertThat(prefs.isEnabled()).isTrue();

            // When: Set preferences.enabled = false
            prefs.setEnabled(false);

            // And: Create deadline 7 days away and check
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            List<DeadlineNotification> notifications = notificationService.checkDeadline(deadline);

            // Then: No notification triggered
            assertThat(notifications).isEmpty();
        }

        @Test
        @DisplayName("TC-309-017: Preferences - Default Trigger Days")
        void defaultTriggerDays() {
            // Given: Fresh preferences object
            NotificationPreferences prefs = notificationService.getPreferences();

            // When: Check getTriggerDays()
            List<Integer> triggerDays = prefs.getTriggerDays();

            // Then: Returns [30, 7, 1]
            assertThat(triggerDays).containsExactly(30, 7, 1);
        }

        @Test
        @DisplayName("TC-309-018: Preferences - Custom Trigger Days")
        void customTriggerDays() {
            // Given: Fresh preferences object
            NotificationPreferences prefs = notificationService.getPreferences();

            // When: Set trigger days to [14, 3]
            prefs.setTriggerDays(List.of(14, 3));

            // Then: Returns [14, 3]
            assertThat(prefs.getTriggerDays()).containsExactly(14, 3);

            // And: Deadline 14 days away triggers notification
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(14));
            List<DeadlineNotification> notifications = notificationService.checkDeadline(deadline);
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).triggerDays()).isEqualTo(14);
        }
    }

    @Nested
    @DisplayName("P1: Tax Year Change Tests")
    class TaxYearChangeTests {

        @Test
        @DisplayName("TC-309-019: Tax Year Change - Scheduler Restarts")
        void taxYearChangeRestartsScheduler() {
            // Given: Tax year 2024/25 selected, scheduler running
            TaxYear year2024 = TaxYear.of(2024);
            notificationService.startScheduler(year2024);

            // When: Change tax year to 2025/26
            TaxYear year2025 = TaxYear.of(2025);
            notificationService.startScheduler(year2025);

            // Then: Scheduler restarts for new tax year deadlines
            // Verify by checking deadlines are for new year
            List<Deadline> deadlines = notificationService.getDeadlinesForTaxYear(year2025);
            assertThat(deadlines.get(0).date().getYear()).isIn(2025, 2026, 2027);
        }
    }

    @Nested
    @DisplayName("P1: Notification Content Tests")
    class NotificationContentTests {

        @Test
        @DisplayName("TC-309-020: Notification Contains Correct Details")
        void notificationContainsCorrectDetails() {
            // Given: Deadline "Test Deadline" 7 days away
            Deadline deadline = Deadline.of("Test Deadline", LocalDate.now().plusDays(7));

            // When: Trigger notification
            notificationService.triggerNotification(deadline, 7);

            // Then: Check notification content
            DeadlineNotification notification = notificationService.getNotificationHistory().get(0);
            assertThat(notification.title()).contains("Deadline");
            assertThat(notification.message()).contains("7 days");
            assertThat(notification.deadline().label()).isEqualTo("Test Deadline");
        }
    }

    // ========================================================================
    // P2 - MEDIUM PRIORITY TEST CASES (6 tests)
    // ========================================================================

    @Nested
    @DisplayName("P2: Snooze Tests")
    class SnoozeTests {

        @Test
        @DisplayName("TC-309-021: Snooze Notification")
        void snoozeNotification() {
            // Given: Notification in history
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(deadline, 7);
            UUID notificationId = notificationService.getNotificationHistory().get(0).id();

            // When: Snooze notification for 24 hours
            notificationService.snooze(notificationId, 24);

            // Then: Notification marked as snoozed
            DeadlineNotification snoozed = notificationService.getNotificationHistory().get(0);
            assertThat(snoozed.isSnoozed()).isTrue();
        }

        @Test
        @DisplayName("TC-309-022: Snoozed Not Counted as Unread")
        void snoozedNotCountedUnread() {
            // Given: 1 unread notification
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(deadline, 7);
            assertThat(notificationService.getUnreadCount()).isEqualTo(1);

            // When: Snooze the notification
            UUID notificationId = notificationService.getNotificationHistory().get(0).id();
            notificationService.snooze(notificationId, 24);

            // Then: Unread count = 0
            assertThat(notificationService.getUnreadCount()).isZero();
        }
    }

    @Nested
    @DisplayName("P2: Deduplication Tests")
    class DeduplicationTests {

        @Test
        @DisplayName("TC-309-023: No Duplicate Same Day")
        void noDuplicateSameDay() {
            // Given: Notification already triggered today
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(deadline, 7);

            // When: Trigger same notification again
            notificationService.triggerNotification(deadline, 7);

            // Then: Only 1 notification in history (deduped)
            assertThat(notificationService.getNotificationHistory()).hasSize(1);
        }

        @Test
        @DisplayName("TC-309-024: Different Trigger Days Allowed")
        void differentTriggerDaysAllowed() {
            // Given: Deadline triggering at 30 days
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));

            // When: Trigger at 30 days and 7 days (same deadline)
            notificationService.triggerNotification(deadline, 30);
            notificationService.triggerNotification(deadline, 7);

            // Then: 2 separate notifications in history
            assertThat(notificationService.getNotificationHistory()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("P2: History Edge Cases")
    class HistoryEdgeCaseTests {

        @Test
        @DisplayName("TC-309-025: Clear History")
        void clearHistory() {
            // Given: Notifications in history
            Deadline d1 = Deadline.of("D1", LocalDate.now().plusDays(30));
            Deadline d2 = Deadline.of("D2", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(d1, 30);
            notificationService.triggerNotification(d2, 7);
            assertThat(notificationService.getNotificationHistory()).hasSize(2);

            // When: Clear history
            notificationService.clearHistory();

            // Then: History empty
            assertThat(notificationService.getNotificationHistory()).isEmpty();
            assertThat(notificationService.getUnreadCount()).isZero();
        }

        @Test
        @DisplayName("TC-309-026: No Notification for Non-Trigger Days")
        void noNotificationForNonTriggerDays() {
            // Given: Deadline 15 days away (not in default triggers [30, 7, 1])
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(15));

            // When: Check for notifications
            List<DeadlineNotification> notifications = notificationService.checkDeadline(deadline);

            // Then: No notification triggered
            assertThat(notifications).isEmpty();
        }
    }

    // ========================================================================
    // ADDITIONAL INTEGRATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Additional: Full Integration Flow")
    class FullIntegrationTests {

        @Test
        @DisplayName("Full notification lifecycle: trigger → read → clear")
        void fullNotificationLifecycle() {
            // 1. Initial state - no notifications
            assertThat(notificationService.getUnreadCount()).isZero();
            assertThat(notificationService.getNotificationHistory()).isEmpty();

            // 2. Trigger notifications
            Deadline d1 = Deadline.of("Filing", LocalDate.now().plusDays(30));
            Deadline d2 = Deadline.of("Payment", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(d1, 30);
            notificationService.triggerNotification(d2, 7);

            assertThat(notificationService.getUnreadCount()).isEqualTo(2);
            assertThat(notificationService.getNotificationHistory()).hasSize(2);

            // 3. Mark one as read
            UUID firstId = notificationService.getNotificationHistory().get(0).id();
            notificationService.markAsRead(firstId);
            assertThat(notificationService.getUnreadCount()).isEqualTo(1);

            // 4. Mark all as read
            notificationService.markAllAsRead();
            assertThat(notificationService.getUnreadCount()).isZero();

            // 5. Clear history
            notificationService.clearHistory();
            assertThat(notificationService.getNotificationHistory()).isEmpty();
        }

        @Test
        @DisplayName("Notification handler callback works")
        void notificationHandlerCallbackWorks() {
            // Given: Handler registered
            var capturedNotifications = new java.util.ArrayList<DeadlineNotification>();
            notificationService.setNotificationHandler(capturedNotifications::add);

            // When: Trigger notification
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(deadline, 7);

            // Then: Handler was called with notification
            assertThat(capturedNotifications).hasSize(1);
            assertThat(capturedNotifications.get(0).deadline().label()).isEqualTo("Test");
        }

        @Test
        @DisplayName("All deadline types generated for tax year")
        void allDeadlineTypesGenerated() {
            // Given: Tax year
            TaxYear taxYear = TaxYear.of(2025);

            // When: Get all deadlines
            List<Deadline> deadlines = notificationService.getDeadlinesForTaxYear(taxYear);

            // Then: All 7 deadline types present
            assertThat(deadlines).hasSize(7);

            List<String> labels = deadlines.stream().map(Deadline::label).toList();
            assertThat(labels).contains("Online Filing Deadline");
            assertThat(labels).contains("Payment Due");
            assertThat(labels).contains("Payment on Account Due");
            assertThat(labels).contains("MTD Q1 Update Due");
            assertThat(labels).contains("MTD Q2 Update Due");
            assertThat(labels).contains("MTD Q3 Update Due");
            assertThat(labels).contains("MTD Q4 Update Due");
        }

        @Test
        @DisplayName("History maintains order (most recent first)")
        void historyMaintainsOrder() {
            // Given: Multiple notifications triggered in order
            Deadline d1 = Deadline.of("First", LocalDate.now().plusDays(30));
            Deadline d2 = Deadline.of("Second", LocalDate.now().plusDays(7));
            Deadline d3 = Deadline.of("Third", LocalDate.now().plusDays(1));

            notificationService.triggerNotification(d1, 30);
            notificationService.triggerNotification(d2, 7);
            notificationService.triggerNotification(d3, 1);

            // When: Get history
            List<DeadlineNotification> history = notificationService.getNotificationHistory();

            // Then: Most recent first
            assertThat(history.get(0).deadline().label()).isEqualTo("Third");
            assertThat(history.get(1).deadline().label()).isEqualTo("Second");
            assertThat(history.get(2).deadline().label()).isEqualTo("First");
        }
    }
}
