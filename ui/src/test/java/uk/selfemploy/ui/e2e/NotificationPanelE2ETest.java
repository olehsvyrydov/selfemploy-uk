package uk.selfemploy.ui.e2e;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import uk.selfemploy.ui.service.DeadlineNotification;
import uk.selfemploy.ui.service.DeadlineNotificationService;
import uk.selfemploy.ui.service.NotificationPriority;
import uk.selfemploy.ui.viewmodel.Deadline;
import uk.selfemploy.ui.viewmodel.NotificationFilter;
import uk.selfemploy.ui.viewmodel.NotificationPanelViewModel;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E/Integration Tests for SE-502: Desktop Notifications.
 * Tests the notification panel ViewModel with real service integration.
 *
 * <p>Test categories:</p>
 * <ul>
 *   <li>P0 (Critical): Badge display, panel visibility, filter functionality, mark as read</li>
 *   <li>P1 (High): Snooze, navigation callbacks, preference integration</li>
 *   <li>P2 (Medium): Full lifecycle, edge cases, priority styling</li>
 * </ul>
 *
 * <p>Note: These tests focus on ViewModel/Service integration and do not require
 * the JavaFX toolkit. For UI-specific tests requiring TestFX and a display,
 * see the full E2E tests that extend BaseE2ETest.</p>
 *
 * @see uk.selfemploy.ui.viewmodel.NotificationPanelViewModel
 * @see uk.selfemploy.ui.service.DeadlineNotificationService
 */
@Tag("e2e")
@Tag("integration")
@DisplayName("SE-502: Desktop Notifications E2E")
class NotificationPanelE2ETest {

    private DeadlineNotificationService notificationService;
    private NotificationPanelViewModel viewModel;

    @BeforeEach
    void setUp() {
        // Create notification service
        notificationService = new DeadlineNotificationService();

        // Create ViewModel directly (no FXML/controller needed for these tests)
        viewModel = new NotificationPanelViewModel(notificationService);

        // Make the panel visible for testing
        viewModel.showPanel();
    }

    @AfterEach
    void cleanUp() {
        if (notificationService != null) {
            notificationService.shutdown();
            notificationService.clearHistory();
        }
    }

    /**
     * Helper to wait for potential async operations.
     * In non-FX tests, this is a no-op but kept for consistency.
     */
    private void waitForFxEvents() {
        // No-op for non-JavaFX tests
        // Keep method for consistency with UI tests
    }

    // ========================================================================
    // P0 - CRITICAL TEST CASES
    // ========================================================================

    @Nested
    @DisplayName("P0: Badge Display Tests")
    class BadgeDisplayTests {

        @Test
        @DisplayName("TC-502-001: Badge initially hidden when no notifications")
        void badgeInitiallyHidden() {
            // Given: Fresh notification service with no notifications
            // When: Check badge state
            // Then: Badge should not be visible
            assertThat(viewModel.isBadgeVisible()).isFalse();
            assertThat(viewModel.getBadgeCount()).isZero();
        }

        @Test
        @DisplayName("TC-502-002: Badge shows count when notifications exist")
        void badgeShowsCountWhenNotificationsExist() {
            // Given: 3 notifications triggered
            triggerTestNotifications(3);

            // When: Refresh badge
            viewModel.refreshBadge();
            waitForFxEvents();

            // Then: Badge shows count 3
            assertThat(viewModel.getBadgeCount()).isEqualTo(3);
            assertThat(viewModel.isBadgeVisible()).isTrue();
            assertThat(viewModel.getBadgeText()).isEqualTo("3");
        }

        @Test
        @DisplayName("TC-502-003: Badge shows 9+ when count exceeds 9")
        void badgeShowsNinePlusForHighCounts() {
            // Given: 12 notifications triggered
            triggerTestNotifications(12);

            // When: Refresh badge
            viewModel.refreshBadge();
            waitForFxEvents();

            // Then: Badge shows "9+"
            assertThat(viewModel.getBadgeCount()).isEqualTo(12);
            assertThat(viewModel.getBadgeText()).isEqualTo("9+");
        }

        @Test
        @DisplayName("TC-502-004: Badge updates dynamically on new notification")
        void badgeUpdatesDynamically() {
            // Given: No notifications
            assertThat(viewModel.getBadgeCount()).isZero();

            // When: Add notification
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(deadline, 7);
            viewModel.refreshBadge();
            waitForFxEvents();

            // Then: Badge updated to 1
            assertThat(viewModel.getBadgeCount()).isEqualTo(1);
            assertThat(viewModel.isBadgeVisible()).isTrue();
        }
    }

    @Nested
    @DisplayName("P0: Panel Visibility Tests")
    class PanelVisibilityTests {

        @Test
        @DisplayName("TC-502-005: Panel toggle shows/hides panel")
        void panelToggleShowsAndHides() {
            // Given: Panel is visible (from setup)
            assertThat(viewModel.isPanelVisible()).isTrue();

            // When: Toggle panel
            viewModel.togglePanel();
            waitForFxEvents();

            // Then: Panel is hidden
            assertThat(viewModel.isPanelVisible()).isFalse();

            // When: Toggle again
            viewModel.togglePanel();
            waitForFxEvents();

            // Then: Panel is visible
            assertThat(viewModel.isPanelVisible()).isTrue();
        }

        @Test
        @DisplayName("TC-502-006: Show panel loads notifications")
        void showPanelLoadsNotifications() {
            // Given: Panel hidden and notifications exist
            viewModel.hidePanel();
            triggerTestNotifications(2);
            waitForFxEvents();

            // When: Show panel
            viewModel.showPanel();
            waitForFxEvents();

            // Then: Notifications loaded into view
            assertThat(viewModel.getNotifications()).hasSize(2);
        }

        @Test
        @DisplayName("TC-502-007: Hide panel updates panel visibility property")
        void hidePanelUpdatesProperty() {
            // Given: Panel visible
            assertThat(viewModel.isPanelVisible()).isTrue();

            // When: Hide panel
            viewModel.hidePanel();
            waitForFxEvents();

            // Then: Panel visibility property is false
            assertThat(viewModel.isPanelVisible()).isFalse();
        }
    }

    @Nested
    @DisplayName("P0: Filter Functionality Tests")
    class FilterFunctionalityTests {

        @Test
        @DisplayName("TC-502-008: Default filter is ALL")
        void defaultFilterIsAll() {
            assertThat(viewModel.getSelectedFilter()).isEqualTo(NotificationFilter.ALL);
        }

        @Test
        @DisplayName("TC-502-009: ALL filter shows all notifications")
        void allFilterShowsAllNotifications() {
            // Given: Mixed read/unread notifications
            Deadline d1 = Deadline.of("Unread 1", LocalDate.now().plusDays(30));
            Deadline d2 = Deadline.of("Unread 2", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(d1, 30);
            notificationService.triggerNotification(d2, 7);

            // Mark one as read
            UUID firstId = notificationService.getNotificationHistory().get(0).id();
            notificationService.markAsRead(firstId);

            // When: Set filter to ALL and load
            viewModel.setSelectedFilter(NotificationFilter.ALL);
            viewModel.loadNotifications();
            waitForFxEvents();

            // Then: Both notifications shown
            assertThat(viewModel.getFilteredNotifications()).hasSize(2);
        }

        @Test
        @DisplayName("TC-502-010: UNREAD filter shows only unread notifications")
        void unreadFilterShowsOnlyUnread() {
            // Given: 3 notifications, 1 read
            triggerTestNotifications(3);
            UUID firstId = notificationService.getNotificationHistory().get(0).id();
            notificationService.markAsRead(firstId);

            // When: Set filter to UNREAD
            viewModel.setSelectedFilter(NotificationFilter.UNREAD);
            viewModel.loadNotifications();
            waitForFxEvents();

            // Then: Only 2 unread notifications shown
            assertThat(viewModel.getFilteredNotifications()).hasSize(2);
            assertThat(viewModel.getFilteredNotifications()).allMatch(n -> !n.isRead());
        }

        @Test
        @DisplayName("TC-502-011: DEADLINES filter shows deadline notifications")
        void deadlinesFilterShowsDeadlines() {
            // Given: Deadline notifications (all current notifications are deadline-based)
            triggerTestNotifications(2);

            // When: Set filter to DEADLINES
            viewModel.setSelectedFilter(NotificationFilter.DEADLINES);
            viewModel.loadNotifications();
            waitForFxEvents();

            // Then: All deadline notifications shown
            assertThat(viewModel.getFilteredNotifications()).hasSize(2);
        }

        @Test
        @DisplayName("TC-502-012: Filter change updates filtered list")
        void filterChangeUpdatesFilteredList() {
            // Given: 3 notifications, 2 read
            triggerTestNotifications(3);
            List<DeadlineNotification> history = notificationService.getNotificationHistory();
            notificationService.markAsRead(history.get(0).id());
            notificationService.markAsRead(history.get(1).id());

            viewModel.loadNotifications();
            waitForFxEvents();

            // Verify ALL filter shows 3
            viewModel.setSelectedFilter(NotificationFilter.ALL);
            assertThat(viewModel.getFilteredNotifications()).hasSize(3);

            // When: Change to UNREAD filter
            viewModel.setSelectedFilter(NotificationFilter.UNREAD);
            waitForFxEvents();

            // Then: Only 1 unread shown
            assertThat(viewModel.getFilteredNotifications()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("P0: Mark as Read Tests")
    class MarkAsReadTests {

        @Test
        @DisplayName("TC-502-013: Mark single notification as read")
        void markSingleAsRead() {
            // Given: Unread notification
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(deadline, 7);
            UUID notificationId = notificationService.getNotificationHistory().get(0).id();

            viewModel.loadNotifications();
            waitForFxEvents();
            assertThat(viewModel.getNotifications().get(0).isRead()).isFalse();

            // When: Mark as read
            viewModel.markAsRead(notificationId);
            waitForFxEvents();

            // Then: Notification is read
            assertThat(notificationService.getNotificationHistory().get(0).isRead()).isTrue();
        }

        @Test
        @DisplayName("TC-502-014: Mark all as read clears badge")
        void markAllAsReadClearsBadge() {
            // Given: 3 unread notifications
            triggerTestNotifications(3);
            viewModel.refreshBadge();
            waitForFxEvents();
            assertThat(viewModel.getBadgeCount()).isEqualTo(3);

            // When: Mark all as read
            viewModel.markAllAsRead();
            viewModel.refreshBadge();
            waitForFxEvents();

            // Then: Badge shows 0, hidden
            assertThat(viewModel.getBadgeCount()).isZero();
            assertThat(viewModel.isBadgeVisible()).isFalse();
        }

        @Test
        @DisplayName("TC-502-015: Mark as read updates UNREAD filter")
        void markAsReadUpdatesUnreadFilter() {
            // Given: 2 unread notifications with UNREAD filter
            triggerTestNotifications(2);
            viewModel.setSelectedFilter(NotificationFilter.UNREAD);
            viewModel.loadNotifications();
            waitForFxEvents();
            assertThat(viewModel.getFilteredNotifications()).hasSize(2);

            // When: Mark one as read
            UUID notificationId = notificationService.getNotificationHistory().get(0).id();
            viewModel.markAsRead(notificationId);
            waitForFxEvents();

            // Then: UNREAD filter shows 1
            assertThat(viewModel.getFilteredNotifications()).hasSize(1);
        }
    }

    // ========================================================================
    // P1 - HIGH PRIORITY TEST CASES
    // ========================================================================

    @Nested
    @DisplayName("P1: Snooze Functionality Tests")
    class SnoozeFunctionalityTests {

        @Test
        @DisplayName("TC-502-016: Snooze notification for 1 hour")
        void snoozeFor1Hour() {
            // Given: Unread notification
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(deadline, 7);
            UUID notificationId = notificationService.getNotificationHistory().get(0).id();

            // When: Snooze for 1 hour
            viewModel.snooze(notificationId, 1);
            waitForFxEvents();

            // Then: Notification is snoozed
            DeadlineNotification snoozed = notificationService.getNotificationHistory().get(0);
            assertThat(snoozed.isSnoozed()).isTrue();
        }

        @Test
        @DisplayName("TC-502-017: Snooze notification for 24 hours")
        void snoozeFor24Hours() {
            // Given: Unread notification
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(deadline, 7);
            UUID notificationId = notificationService.getNotificationHistory().get(0).id();

            // When: Snooze for 24 hours
            viewModel.snooze(notificationId, 24);
            waitForFxEvents();

            // Then: Notification is snoozed
            DeadlineNotification snoozed = notificationService.getNotificationHistory().get(0);
            assertThat(snoozed.isSnoozed()).isTrue();
        }

        @Test
        @DisplayName("TC-502-018: Snoozed notification not counted in badge")
        void snoozedNotCountedInBadge() {
            // Given: 2 unread notifications
            triggerTestNotifications(2);
            viewModel.refreshBadge();
            waitForFxEvents();
            assertThat(viewModel.getBadgeCount()).isEqualTo(2);

            // When: Snooze one
            UUID notificationId = notificationService.getNotificationHistory().get(0).id();
            viewModel.snooze(notificationId, 24);
            viewModel.refreshBadge();
            waitForFxEvents();

            // Then: Badge shows 1
            assertThat(viewModel.getBadgeCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("P1: Navigation Callback Tests")
    class NavigationCallbackTests {

        @Test
        @DisplayName("TC-502-019: Click notification triggers navigation handler")
        void clickNotificationTriggersNavigation() {
            // Given: Notification with filing deadline and navigation handler
            Deadline deadline = Deadline.of("Online Filing Deadline", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(deadline, 7);
            viewModel.loadNotifications();
            waitForFxEvents();

            AtomicReference<String> navigatedUrl = new AtomicReference<>();
            viewModel.setNavigationHandler(navigatedUrl::set);

            UUID notificationId = notificationService.getNotificationHistory().get(0).id();

            // When: Click notification
            viewModel.onNotificationClick(notificationId);
            waitForFxEvents();

            // Then: Navigation handler called with correct URL
            assertThat(navigatedUrl.get()).isEqualTo("/submission");
        }

        @Test
        @DisplayName("TC-502-020: Click payment deadline navigates to tax summary")
        void clickPaymentDeadlineNavigatesToTaxSummary() {
            // Given: Payment deadline notification
            Deadline deadline = Deadline.of("Payment Due", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(deadline, 7);
            viewModel.loadNotifications();
            waitForFxEvents();

            AtomicReference<String> navigatedUrl = new AtomicReference<>();
            viewModel.setNavigationHandler(navigatedUrl::set);

            UUID notificationId = notificationService.getNotificationHistory().get(0).id();

            // When: Click notification
            viewModel.onNotificationClick(notificationId);
            waitForFxEvents();

            // Then: Navigation to tax-summary
            assertThat(navigatedUrl.get()).isEqualTo("/tax-summary");
        }

        @Test
        @DisplayName("TC-502-021: Click notification marks it as read")
        void clickNotificationMarksAsRead() {
            // Given: Unread notification
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(deadline, 7);
            UUID notificationId = notificationService.getNotificationHistory().get(0).id();
            assertThat(notificationService.getNotificationHistory().get(0).isRead()).isFalse();

            // When: Click notification
            viewModel.onNotificationClick(notificationId);
            waitForFxEvents();

            // Then: Notification is marked as read
            assertThat(notificationService.getNotificationHistory().get(0).isRead()).isTrue();
        }
    }

    @Nested
    @DisplayName("P1: Preference Integration Tests")
    class PreferenceIntegrationTests {

        @Test
        @DisplayName("TC-502-022: Disabled notifications don't trigger")
        void disabledNotificationsDontTrigger() {
            // Given: Notifications disabled
            notificationService.getPreferences().setEnabled(false);

            // When: Check deadline
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            List<DeadlineNotification> notifications = notificationService.checkDeadline(deadline);

            // Then: No notifications triggered
            assertThat(notifications).isEmpty();
        }

        @Test
        @DisplayName("TC-502-023: Custom trigger days work")
        void customTriggerDaysWork() {
            // Given: Custom trigger days
            notificationService.getPreferences().setTriggerDays(List.of(14, 3));

            // When: Check deadline at 14 days
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(14));
            List<DeadlineNotification> notifications = notificationService.checkDeadline(deadline);

            // Then: Notification triggered
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).triggerDays()).isEqualTo(14);
        }
    }

    // ========================================================================
    // P2 - MEDIUM PRIORITY TEST CASES
    // ========================================================================

    @Nested
    @DisplayName("P2: Notification Lifecycle Tests")
    class NotificationLifecycleTests {

        @Test
        @DisplayName("TC-502-024: Full notification lifecycle")
        void fullNotificationLifecycle() {
            // 1. Initial empty state
            assertThat(viewModel.getBadgeCount()).isZero();
            assertThat(viewModel.isEmptyStateVisible()).isTrue();

            // 2. Trigger notifications
            triggerTestNotifications(3);
            viewModel.loadNotifications();
            viewModel.refreshBadge();
            waitForFxEvents();

            assertThat(viewModel.getBadgeCount()).isEqualTo(3);
            assertThat(viewModel.isEmptyStateVisible()).isFalse();
            assertThat(viewModel.getNotifications()).hasSize(3);

            // 3. Mark one as read
            UUID firstId = notificationService.getNotificationHistory().get(0).id();
            viewModel.markAsRead(firstId);
            viewModel.refreshBadge();
            waitForFxEvents();

            assertThat(viewModel.getBadgeCount()).isEqualTo(2);

            // 4. Snooze one
            UUID secondId = notificationService.getNotificationHistory().get(1).id();
            viewModel.snooze(secondId, 24);
            viewModel.refreshBadge();
            waitForFxEvents();

            assertThat(viewModel.getBadgeCount()).isEqualTo(1);

            // 5. Mark all as read
            viewModel.markAllAsRead();
            viewModel.refreshBadge();
            waitForFxEvents();

            assertThat(viewModel.getBadgeCount()).isZero();
            assertThat(viewModel.isBadgeVisible()).isFalse();
        }

        @Test
        @DisplayName("TC-502-025: Dismiss notification removes from unread")
        void dismissNotificationRemovesFromUnread() {
            // Given: Unread notification
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(7));
            notificationService.triggerNotification(deadline, 7);
            viewModel.loadNotifications();
            viewModel.refreshBadge();
            waitForFxEvents();
            assertThat(viewModel.getBadgeCount()).isEqualTo(1);

            // When: Dismiss notification
            UUID notificationId = notificationService.getNotificationHistory().get(0).id();
            viewModel.dismissNotification(notificationId);
            viewModel.refreshBadge();
            waitForFxEvents();

            // Then: Badge cleared
            assertThat(viewModel.getBadgeCount()).isZero();
        }
    }

    @Nested
    @DisplayName("P2: Empty State Tests")
    class EmptyStateTests {

        @Test
        @DisplayName("TC-502-026: Empty state visible when no notifications")
        void emptyStateVisibleWhenNoNotifications() {
            // Given: No notifications
            viewModel.loadNotifications();
            waitForFxEvents();

            // Then: Empty state visible
            assertThat(viewModel.isEmptyStateVisible()).isTrue();
        }

        @Test
        @DisplayName("TC-502-027: Empty state hidden when notifications exist")
        void emptyStateHiddenWhenNotificationsExist() {
            // Given: Notifications exist
            triggerTestNotifications(1);
            viewModel.loadNotifications();
            waitForFxEvents();

            // Then: Empty state hidden
            assertThat(viewModel.isEmptyStateVisible()).isFalse();
        }

        @Test
        @DisplayName("TC-502-028: Empty state shown when filter returns no results")
        void emptyStateShownWhenFilterReturnsNoResults() {
            // Given: All notifications are read
            triggerTestNotifications(2);
            notificationService.markAllAsRead();

            // When: Filter by UNREAD
            viewModel.setSelectedFilter(NotificationFilter.UNREAD);
            viewModel.loadNotifications();
            waitForFxEvents();

            // Then: Empty state visible (no unread notifications)
            assertThat(viewModel.isEmptyStateVisible()).isTrue();
            assertThat(viewModel.getFilteredNotifications()).isEmpty();
        }
    }

    @Nested
    @DisplayName("P2: Priority Styling Tests")
    class PriorityStylingTests {

        @Test
        @DisplayName("TC-502-029: LOW priority has correct style class")
        void lowPriorityStyleClass() {
            assertThat(NotificationPanelViewModel.getPriorityStyleClass(NotificationPriority.LOW))
                .isEqualTo("priority-low");
        }

        @Test
        @DisplayName("TC-502-030: MEDIUM priority has correct style class")
        void mediumPriorityStyleClass() {
            assertThat(NotificationPanelViewModel.getPriorityStyleClass(NotificationPriority.MEDIUM))
                .isEqualTo("priority-medium");
        }

        @Test
        @DisplayName("TC-502-031: HIGH priority has correct style class")
        void highPriorityStyleClass() {
            assertThat(NotificationPanelViewModel.getPriorityStyleClass(NotificationPriority.HIGH))
                .isEqualTo("priority-high");
        }

        @Test
        @DisplayName("TC-502-032: CRITICAL priority has correct style class")
        void criticalPriorityStyleClass() {
            assertThat(NotificationPanelViewModel.getPriorityStyleClass(NotificationPriority.CRITICAL))
                .isEqualTo("priority-critical");
        }

        @Test
        @DisplayName("TC-502-033: Notification item has correct priority style")
        void notificationItemHasCorrectPriorityStyle() {
            // Given: HIGH priority notification (1 day away)
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(1));
            notificationService.triggerNotification(deadline, 1);
            viewModel.loadNotifications();
            waitForFxEvents();

            // Then: Notification has HIGH priority styling
            assertThat(viewModel.getNotifications().get(0).priority()).isEqualTo(NotificationPriority.HIGH);
            assertThat(viewModel.getNotifications().get(0).getPriorityStyleClass()).isEqualTo("priority-high");
        }
    }

    @Nested
    @DisplayName("P2: Time Formatting Tests")
    class TimeFormattingTests {

        @Test
        @DisplayName("TC-502-034: Format 'Just now' for recent")
        void formatJustNowForRecent() {
            var time = java.time.LocalDateTime.now().minusMinutes(2);
            assertThat(NotificationPanelViewModel.formatTimeAgo(time)).isEqualTo("Just now");
        }

        @Test
        @DisplayName("TC-502-035: Format 'X minutes ago'")
        void formatMinutesAgo() {
            var time = java.time.LocalDateTime.now().minusMinutes(15);
            assertThat(NotificationPanelViewModel.formatTimeAgo(time)).isEqualTo("15 minutes ago");
        }

        @Test
        @DisplayName("TC-502-036: Format 'X hours ago'")
        void formatHoursAgo() {
            var time = java.time.LocalDateTime.now().minusHours(3);
            assertThat(NotificationPanelViewModel.formatTimeAgo(time)).isEqualTo("3 hours ago");
        }

        @Test
        @DisplayName("TC-502-037: Format 'Yesterday'")
        void formatYesterday() {
            var time = java.time.LocalDateTime.now().minusDays(1);
            assertThat(NotificationPanelViewModel.formatTimeAgo(time)).isEqualTo("Yesterday");
        }

        @Test
        @DisplayName("TC-502-038: Format 'X days ago'")
        void formatDaysAgo() {
            var time = java.time.LocalDateTime.now().minusDays(5);
            assertThat(NotificationPanelViewModel.formatTimeAgo(time)).isEqualTo("5 days ago");
        }

        @Test
        @DisplayName("TC-502-039: Null time returns empty string")
        void nullTimeReturnsEmptyString() {
            assertThat(NotificationPanelViewModel.formatTimeAgo(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("P2: Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("TC-502-040: Marking non-existent notification is safe")
        void markingNonExistentNotificationIsSafe() {
            // Given: Random UUID that doesn't exist
            UUID randomId = UUID.randomUUID();

            // When: Mark as read - should not throw
            viewModel.markAsRead(randomId);
            waitForFxEvents();

            // Then: No exception thrown, service is stable
            assertThat(notificationService.getNotificationHistory()).isEmpty();
        }

        @Test
        @DisplayName("TC-502-041: Snoozing non-existent notification is safe")
        void snoozingNonExistentNotificationIsSafe() {
            // Given: Random UUID that doesn't exist
            UUID randomId = UUID.randomUUID();

            // When: Snooze - should not throw
            viewModel.snooze(randomId, 24);
            waitForFxEvents();

            // Then: No exception thrown
            assertThat(notificationService.getNotificationHistory()).isEmpty();
        }

        @Test
        @DisplayName("TC-502-042: Load notifications with empty history")
        void loadNotificationsWithEmptyHistory() {
            // Given: Empty history
            notificationService.clearHistory();

            // When: Load notifications
            viewModel.loadNotifications();
            waitForFxEvents();

            // Then: Empty list, empty state visible
            assertThat(viewModel.getNotifications()).isEmpty();
            assertThat(viewModel.isEmptyStateVisible()).isTrue();
        }

        @Test
        @DisplayName("TC-502-043: High priority notification has actions")
        void highPriorityNotificationHasActions() {
            // Given: HIGH priority notification (1 day away)
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(1));
            notificationService.triggerNotification(deadline, 1);
            viewModel.loadNotifications();
            waitForFxEvents();

            // Then: Notification has actions
            assertThat(viewModel.getNotifications().get(0).hasActions()).isTrue();
        }

        @Test
        @DisplayName("TC-502-044: Low priority notification has no actions")
        void lowPriorityNotificationHasNoActions() {
            // Given: LOW priority notification (30 days away)
            Deadline deadline = Deadline.of("Test", LocalDate.now().plusDays(30));
            notificationService.triggerNotification(deadline, 30);
            viewModel.loadNotifications();
            waitForFxEvents();

            // Then: Notification has no actions
            assertThat(viewModel.getNotifications().get(0).hasActions()).isFalse();
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Triggers the specified number of test notifications.
     */
    private void triggerTestNotifications(int count) {
        for (int i = 0; i < count; i++) {
            Deadline deadline = Deadline.of("Deadline " + i, LocalDate.now().plusDays(30 + i));
            notificationService.triggerNotification(deadline, 30);
        }
    }
}
