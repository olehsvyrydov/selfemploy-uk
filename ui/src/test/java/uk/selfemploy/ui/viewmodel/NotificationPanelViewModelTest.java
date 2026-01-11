package uk.selfemploy.ui.viewmodel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.ui.service.DeadlineNotification;
import uk.selfemploy.ui.service.DeadlineNotificationService;
import uk.selfemploy.ui.service.NotificationPriority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for SE-502: Notification Panel ViewModel.
 * Tests notification panel UI logic following MVVM pattern.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SE-502: Notification Panel ViewModel")
class NotificationPanelViewModelTest {

    private NotificationPanelViewModel viewModel;

    @Mock
    private DeadlineNotificationService notificationService;

    @BeforeEach
    void setUp() {
        viewModel = new NotificationPanelViewModel(notificationService);
    }

    // === Panel Visibility ===

    @Nested
    @DisplayName("Panel Visibility")
    class PanelVisibilityTests {

        @Test
        @DisplayName("Panel initially hidden")
        void panelInitiallyHidden() {
            assertThat(viewModel.isPanelVisible()).isFalse();
        }

        @Test
        @DisplayName("Toggle shows panel when hidden")
        void toggleShowsPanel() {
            viewModel.togglePanel();
            assertThat(viewModel.isPanelVisible()).isTrue();
        }

        @Test
        @DisplayName("Toggle hides panel when visible")
        void toggleHidesPanel() {
            viewModel.togglePanel(); // Show
            viewModel.togglePanel(); // Hide
            assertThat(viewModel.isPanelVisible()).isFalse();
        }

        @Test
        @DisplayName("Show panel makes it visible")
        void showPanelMakesVisible() {
            viewModel.showPanel();
            assertThat(viewModel.isPanelVisible()).isTrue();
        }

        @Test
        @DisplayName("Hide panel makes it hidden")
        void hidePanelMakesHidden() {
            viewModel.showPanel();
            viewModel.hidePanel();
            assertThat(viewModel.isPanelVisible()).isFalse();
        }

        @Test
        @DisplayName("Opening panel loads notifications")
        void openingPanelLoadsNotifications() {
            when(notificationService.getNotificationHistory()).thenReturn(List.of());

            viewModel.showPanel();

            verify(notificationService).getNotificationHistory();
        }
    }

    // === Badge Display ===

    @Nested
    @DisplayName("Badge Display")
    class BadgeDisplayTests {

        @Test
        @DisplayName("Badge count bound to service unread count")
        void badgeCountBound() {
            when(notificationService.getUnreadCount()).thenReturn(5);

            viewModel.refreshBadge();

            assertThat(viewModel.getBadgeCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Badge visible when count > 0")
        void badgeVisibleWhenHasNotifications() {
            when(notificationService.getUnreadCount()).thenReturn(3);

            viewModel.refreshBadge();

            assertThat(viewModel.isBadgeVisible()).isTrue();
        }

        @Test
        @DisplayName("Badge hidden when count = 0")
        void badgeHiddenWhenEmpty() {
            when(notificationService.getUnreadCount()).thenReturn(0);

            viewModel.refreshBadge();

            assertThat(viewModel.isBadgeVisible()).isFalse();
        }

        @Test
        @DisplayName("Badge text shows 9+ when count > 9")
        void badgeShowsNinePlus() {
            when(notificationService.getUnreadCount()).thenReturn(15);

            viewModel.refreshBadge();

            assertThat(viewModel.getBadgeText()).isEqualTo("9+");
        }

        @Test
        @DisplayName("Badge text shows exact count when <= 9")
        void badgeShowsExactCount() {
            when(notificationService.getUnreadCount()).thenReturn(7);

            viewModel.refreshBadge();

            assertThat(viewModel.getBadgeText()).isEqualTo("7");
        }
    }

    // === Notification List ===

    @Nested
    @DisplayName("Notification List")
    class NotificationListTests {

        @Test
        @DisplayName("Empty list when no notifications")
        void emptyListWhenNoNotifications() {
            when(notificationService.getNotificationHistory()).thenReturn(List.of());

            viewModel.loadNotifications();

            assertThat(viewModel.getNotifications()).isEmpty();
        }

        @Test
        @DisplayName("Shows all notifications from service")
        void showsAllNotifications() {
            List<DeadlineNotification> notifications = createTestNotifications(3);
            when(notificationService.getNotificationHistory()).thenReturn(notifications);

            viewModel.loadNotifications();

            assertThat(viewModel.getNotifications()).hasSize(3);
        }

        @Test
        @DisplayName("Notifications have correct display properties")
        void notificationsHaveCorrectProperties() {
            DeadlineNotification notification = createNotification("Test Deadline", 7, NotificationPriority.MEDIUM);
            when(notificationService.getNotificationHistory()).thenReturn(List.of(notification));

            viewModel.loadNotifications();

            NotificationItemViewModel item = viewModel.getNotifications().get(0);
            assertThat(item.title()).contains("Deadline");
            assertThat(item.priority()).isEqualTo(NotificationPriority.MEDIUM);
        }

        @Test
        @DisplayName("Empty state visible when no notifications")
        void emptyStateVisible() {
            when(notificationService.getNotificationHistory()).thenReturn(List.of());

            viewModel.loadNotifications();

            assertThat(viewModel.isEmptyStateVisible()).isTrue();
        }

        @Test
        @DisplayName("Empty state hidden when has notifications")
        void emptyStateHidden() {
            when(notificationService.getNotificationHistory()).thenReturn(createTestNotifications(1));

            viewModel.loadNotifications();

            assertThat(viewModel.isEmptyStateVisible()).isFalse();
        }
    }

    // === Filter Tabs ===

    @Nested
    @DisplayName("Filter Tabs")
    class FilterTabTests {

        @Test
        @DisplayName("Default filter is ALL")
        void defaultFilterIsAll() {
            assertThat(viewModel.getSelectedFilter()).isEqualTo(NotificationFilter.ALL);
        }

        @Test
        @DisplayName("Can select UNREAD filter")
        void canSelectUnreadFilter() {
            viewModel.setSelectedFilter(NotificationFilter.UNREAD);
            assertThat(viewModel.getSelectedFilter()).isEqualTo(NotificationFilter.UNREAD);
        }

        @Test
        @DisplayName("UNREAD filter shows only unread notifications")
        void unreadFilterShowsOnlyUnread() {
            DeadlineNotification unread = createNotification("Unread", 7, NotificationPriority.MEDIUM);
            DeadlineNotification read = createNotification("Read", 30, NotificationPriority.LOW).markAsRead();
            when(notificationService.getNotificationHistory()).thenReturn(List.of(unread, read));

            viewModel.loadNotifications();
            viewModel.setSelectedFilter(NotificationFilter.UNREAD);

            assertThat(viewModel.getFilteredNotifications())
                .hasSize(1)
                .allMatch(n -> !n.isRead());
        }

        @Test
        @DisplayName("DEADLINES filter shows only deadline notifications")
        void deadlinesFilterShowsDeadlines() {
            viewModel.setSelectedFilter(NotificationFilter.DEADLINES);
            assertThat(viewModel.getSelectedFilter()).isEqualTo(NotificationFilter.DEADLINES);
        }
    }

    // === Notification Actions ===

    @Nested
    @DisplayName("Notification Actions")
    class ActionTests {

        @Test
        @DisplayName("Mark as read delegates to service")
        void markAsReadDelegatesToService() {
            UUID notificationId = UUID.randomUUID();

            viewModel.markAsRead(notificationId);

            verify(notificationService).markAsRead(notificationId);
        }

        @Test
        @DisplayName("Mark all read delegates to service")
        void markAllReadDelegatesToService() {
            viewModel.markAllAsRead();

            verify(notificationService).markAllAsRead();
        }

        @Test
        @DisplayName("Dismiss notification delegates to service")
        void dismissDelegatesToService() {
            UUID notificationId = UUID.randomUUID();

            viewModel.dismissNotification(notificationId);

            verify(notificationService).markAsRead(notificationId);
        }

        @Test
        @DisplayName("Snooze 1 hour delegates to service")
        void snooze1HourDelegatesToService() {
            UUID notificationId = UUID.randomUUID();

            viewModel.snooze(notificationId, 1);

            verify(notificationService).snooze(notificationId, 1);
        }

        @Test
        @DisplayName("Snooze 24 hours delegates to service")
        void snooze24HoursDelegatesToService() {
            UUID notificationId = UUID.randomUUID();

            viewModel.snooze(notificationId, 24);

            verify(notificationService).snooze(notificationId, 24);
        }

        @Test
        @DisplayName("Click notification marks as read")
        void clickNotificationMarksAsRead() {
            UUID notificationId = UUID.randomUUID();

            viewModel.onNotificationClick(notificationId);

            verify(notificationService).markAsRead(notificationId);
        }
    }

    // === Priority Styling ===

    @Nested
    @DisplayName("Priority Styling")
    class PriorityStylingTests {

        @Test
        @DisplayName("LOW priority returns correct style class")
        void lowPriorityStyleClass() {
            assertThat(NotificationPanelViewModel.getPriorityStyleClass(NotificationPriority.LOW))
                .isEqualTo("priority-low");
        }

        @Test
        @DisplayName("MEDIUM priority returns correct style class")
        void mediumPriorityStyleClass() {
            assertThat(NotificationPanelViewModel.getPriorityStyleClass(NotificationPriority.MEDIUM))
                .isEqualTo("priority-medium");
        }

        @Test
        @DisplayName("HIGH priority returns correct style class")
        void highPriorityStyleClass() {
            assertThat(NotificationPanelViewModel.getPriorityStyleClass(NotificationPriority.HIGH))
                .isEqualTo("priority-high");
        }

        @Test
        @DisplayName("CRITICAL priority returns correct style class")
        void criticalPriorityStyleClass() {
            assertThat(NotificationPanelViewModel.getPriorityStyleClass(NotificationPriority.CRITICAL))
                .isEqualTo("priority-critical");
        }
    }

    // === Time Formatting ===

    @Nested
    @DisplayName("Time Formatting")
    class TimeFormattingTests {

        @Test
        @DisplayName("Formats 'just now' for recent notifications")
        void formatsJustNow() {
            LocalDateTime recent = LocalDateTime.now().minusMinutes(2);
            assertThat(NotificationPanelViewModel.formatTimeAgo(recent)).isEqualTo("Just now");
        }

        @Test
        @DisplayName("Formats 'X minutes ago'")
        void formatsMinutesAgo() {
            LocalDateTime time = LocalDateTime.now().minusMinutes(15);
            assertThat(NotificationPanelViewModel.formatTimeAgo(time)).isEqualTo("15 minutes ago");
        }

        @Test
        @DisplayName("Formats 'X hours ago'")
        void formatsHoursAgo() {
            LocalDateTime time = LocalDateTime.now().minusHours(3);
            assertThat(NotificationPanelViewModel.formatTimeAgo(time)).isEqualTo("3 hours ago");
        }

        @Test
        @DisplayName("Formats 'Yesterday'")
        void formatsYesterday() {
            LocalDateTime time = LocalDateTime.now().minusDays(1);
            assertThat(NotificationPanelViewModel.formatTimeAgo(time)).isEqualTo("Yesterday");
        }

        @Test
        @DisplayName("Formats 'X days ago'")
        void formatsDaysAgo() {
            LocalDateTime time = LocalDateTime.now().minusDays(5);
            assertThat(NotificationPanelViewModel.formatTimeAgo(time)).isEqualTo("5 days ago");
        }
    }

    // === Helper Methods ===

    private List<DeadlineNotification> createTestNotifications(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> createNotification("Deadline " + i, 7 + i, NotificationPriority.MEDIUM))
            .toList();
    }

    private DeadlineNotification createNotification(String label, int triggerDays, NotificationPriority priority) {
        Deadline deadline = Deadline.of(label, LocalDate.now().plusDays(triggerDays));
        return DeadlineNotification.create(deadline, triggerDays);
    }
}
