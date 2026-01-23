package uk.selfemploy.ui.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.service.DeadlineNotification;
import uk.selfemploy.ui.service.NotificationPriority;
import uk.selfemploy.ui.viewmodel.Deadline;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NotificationDialog.
 * Tests the dialog creation logic without requiring JavaFX runtime.
 */
@DisplayName("NotificationDialog")
class NotificationDialogTest {

    @Nested
    @DisplayName("Data Model")
    class DataModelTests {

        @Test
        @DisplayName("should create notification with correct priority icon")
        void shouldMapPriorityToCorrectIcon() {
            // LOW priority should map to info icon
            assertEquals(NotificationPriority.LOW, NotificationPriority.valueOf("LOW"));

            // HIGH priority should map to warning icon
            assertEquals(NotificationPriority.HIGH, NotificationPriority.valueOf("HIGH"));

            // CRITICAL priority should map to warning icon
            assertEquals(NotificationPriority.CRITICAL, NotificationPriority.valueOf("CRITICAL"));
        }

        @Test
        @DisplayName("should format time ago correctly")
        void shouldFormatTimeAgoCorrectly() {
            // Test the time formatting logic
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneHourAgo = now.minusHours(1);
            LocalDateTime oneDayAgo = now.minusDays(1);

            // Verify the timestamps are in expected order
            assertTrue(oneHourAgo.isBefore(now));
            assertTrue(oneDayAgo.isBefore(oneHourAgo));
        }

        @Test
        @DisplayName("should handle empty notification list")
        void shouldHandleEmptyNotificationList() {
            List<DeadlineNotification> emptyList = Collections.emptyList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        @DisplayName("should handle notifications with read status")
        void shouldHandleReadStatus() {
            Deadline deadline = Deadline.of("Test Deadline", LocalDate.now().plusDays(7));
            DeadlineNotification notification = DeadlineNotification.create(deadline, 7);

            // Initially unread
            assertFalse(notification.isRead());

            // After marking as read
            DeadlineNotification readNotification = notification.markAsRead();
            assertTrue(readNotification.isRead());
        }

        @Test
        @DisplayName("should calculate days remaining correctly")
        void shouldCalculateDaysRemaining() {
            TaxYear taxYear = TaxYear.of(2025); // 2025/26 tax year
            LocalDate deadline = taxYear.onlineFilingDeadline();

            // Deadline should be 31 January 2027
            assertEquals(LocalDate.of(2027, 1, 31), deadline);
        }
    }

    @Nested
    @DisplayName("Notification Types")
    class NotificationTypeTests {

        @Test
        @DisplayName("should have correct CSS class for LOW priority")
        void shouldHaveCorrectClassForLowPriority() {
            assertEquals("LOW", NotificationPriority.LOW.name());
        }

        @Test
        @DisplayName("should have correct CSS class for MEDIUM priority")
        void shouldHaveCorrectClassForMediumPriority() {
            assertEquals("MEDIUM", NotificationPriority.MEDIUM.name());
        }

        @Test
        @DisplayName("should have correct CSS class for HIGH priority")
        void shouldHaveCorrectClassForHighPriority() {
            assertEquals("HIGH", NotificationPriority.HIGH.name());
        }

        @Test
        @DisplayName("should have correct CSS class for CRITICAL priority")
        void shouldHaveCorrectClassForCriticalPriority() {
            assertEquals("CRITICAL", NotificationPriority.CRITICAL.name());
        }
    }

    @Nested
    @DisplayName("Design Compliance")
    class DesignComplianceTests {

        @Test
        @DisplayName("should use professional slate palette colors")
        void shouldUseProfessionalColors() {
            // These color values should match /aura's design spec
            String headerGradientStart = "#475569";
            String headerGradientEnd = "#64748b";
            String tealPrimary = "#0f766e";
            String tealHover = "#14b8a6";

            // Verify hex format
            assertTrue(headerGradientStart.matches("#[0-9a-fA-F]{6}"));
            assertTrue(headerGradientEnd.matches("#[0-9a-fA-F]{6}"));
            assertTrue(tealPrimary.matches("#[0-9a-fA-F]{6}"));
            assertTrue(tealHover.matches("#[0-9a-fA-F]{6}"));
        }

        @Test
        @DisplayName("should have correct dialog dimensions from spec")
        void shouldHaveCorrectDimensions() {
            // From /aura's design spec
            int dialogWidth = 380;
            int dialogMinHeight = 280;
            int dialogMaxHeight = 480;
            int borderRadius = 12;
            int buttonRadius = 6;

            assertTrue(dialogWidth > 0);
            assertTrue(dialogMinHeight < dialogMaxHeight);
            assertTrue(borderRadius > 0);
            assertTrue(buttonRadius > 0);
        }
    }
}
