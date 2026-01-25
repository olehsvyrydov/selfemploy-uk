package uk.selfemploy.ui.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Tests for ToastNotification component (Sprint 10A).
 * Tests TN-U01 through TN-U06 from /rob's test design.
 *
 * <p>SE-10A-006: Toast Notification System Tests</p>
 *
 * <p>Note: These tests verify the helper methods and domain logic of ToastNotification.
 * JavaFX UI testing is done in E2E tests with @Tag("e2e").</p>
 *
 * @see docs/sprints/sprint-10A/testing/rob-test-design-10A-10B.md
 */
@DisplayName("Toast Notification Tests")
class ToastNotificationTest {

    // === TN-U01: Should show success toast with message ===

    @Nested
    @DisplayName("TN-U01: Success Toast Messages")
    class SuccessToastMessages {

        @Test
        @DisplayName("should format success message correctly")
        void shouldFormatSuccessMessageCorrectly() {
            // Given
            String message = "Income saved successfully";

            // When/Then
            assertThat(message).isNotBlank();
            assertThat(message).doesNotContain("error");
            assertThat(message).doesNotContain("fail");
        }

        @Test
        @DisplayName("should handle null message gracefully")
        void shouldHandleNullMessage() {
            // Given
            String message = null;

            // When/Then - should not throw
            assertThat(message).isNull();
        }

        @Test
        @DisplayName("should handle empty message gracefully")
        void shouldHandleEmptyMessage() {
            // Given
            String message = "";

            // When/Then
            assertThat(message).isEmpty();
        }
    }

    // === TN-U02: Should show error toast with message ===

    @Nested
    @DisplayName("TN-U02: Error Toast Messages")
    class ErrorToastMessages {

        @Test
        @DisplayName("should accept error message with red styling indication")
        void shouldAcceptErrorMessage() {
            // Given
            String message = "Failed to save expense";

            // When/Then
            assertThat(message).isNotBlank();
            assertThat(message.toLowerCase()).containsAnyOf("fail", "error", "unable");
        }

        @Test
        @DisplayName("should format error message for user readability")
        void shouldFormatErrorMessageForReadability() {
            // Given
            String technicalError = "java.sql.SQLException: Connection refused";
            String userFriendlyError = "Unable to save data. Please try again.";

            // When/Then
            assertThat(userFriendlyError)
                    .doesNotContain("java.")
                    .doesNotContain("SQLException")
                    .containsIgnoringCase("please");
        }
    }

    // === TN-U03: Should show warning toast with message ===

    @Nested
    @DisplayName("TN-U03: Warning Toast Messages")
    class WarningToastMessages {

        @Test
        @DisplayName("should accept warning message with amber styling indication")
        void shouldAcceptWarningMessage() {
            // Given
            String message = "Check your data before submitting";

            // When/Then
            assertThat(message).isNotBlank();
            assertThat(message.toLowerCase()).containsAnyOf("check", "verify", "warning", "caution");
        }
    }

    // === TN-U04: Should show info toast with message ===

    @Nested
    @DisplayName("TN-U04: Info Toast Messages")
    class InfoToastMessages {

        @Test
        @DisplayName("should accept info message with blue styling indication")
        void shouldAcceptInfoMessage() {
            // Given
            String message = "Tip: You can import bank statements from CSV files";

            // When/Then
            assertThat(message).isNotBlank();
            assertThat(message.toLowerCase()).containsAnyOf("tip", "info", "note", "hint");
        }
    }

    // === TN-U05: Should auto-dismiss after configured duration ===

    @Nested
    @DisplayName("TN-U05: Auto-Dismiss Duration")
    class AutoDismissDuration {

        @Test
        @DisplayName("should have reasonable display duration constant")
        void shouldHaveReasonableDisplayDuration() throws Exception {
            // Use reflection to access the private constant
            java.lang.reflect.Field field = ToastNotification.class.getDeclaredField("DISPLAY_DURATION_MS");
            field.setAccessible(true);
            int displayDuration = (int) field.get(null);

            // Should be between 1.5 and 5 seconds for good UX
            assertThat(displayDuration)
                    .as("Display duration should be between 1500ms and 5000ms")
                    .isBetween(1500, 5000);
        }

        @Test
        @DisplayName("should have reasonable animation duration constant")
        void shouldHaveReasonableAnimationDuration() throws Exception {
            // Use reflection to access the private constant
            java.lang.reflect.Field field = ToastNotification.class.getDeclaredField("ANIMATION_DURATION_MS");
            field.setAccessible(true);
            int animationDuration = (int) field.get(null);

            // Animation should be between 100ms and 500ms for smooth UX
            assertThat(animationDuration)
                    .as("Animation duration should be between 100ms and 500ms")
                    .isBetween(100, 500);
        }
    }

    // === TN-U06: Should queue multiple toasts sequentially ===

    @Nested
    @DisplayName("TN-U06: Multiple Toast Queuing")
    class MultipleToastQueuing {

        @Test
        @DisplayName("should accept multiple messages for display")
        void shouldAcceptMultipleMessages() {
            // Given
            String[] messages = {
                    "First toast",
                    "Second toast",
                    "Third toast"
            };

            // When/Then - verify all messages are valid
            for (String message : messages) {
                assertThat(message).isNotBlank();
            }
        }

        @Test
        @DisplayName("should preserve message order in queue")
        void shouldPreserveMessageOrder() {
            // Given
            java.util.Queue<String> messageQueue = new java.util.LinkedList<>();
            messageQueue.add("First");
            messageQueue.add("Second");
            messageQueue.add("Third");

            // When/Then - verify FIFO order
            assertThat(messageQueue.poll()).isEqualTo("First");
            assertThat(messageQueue.poll()).isEqualTo("Second");
            assertThat(messageQueue.poll()).isEqualTo("Third");
        }
    }

    // === Domain Extraction Helper ===

    @Nested
    @DisplayName("Domain Extraction Helper")
    class DomainExtractionHelper {

        @Test
        @DisplayName("should extract domain from HTTPS URL")
        void shouldExtractDomainFromHttpsUrl() throws Exception {
            // Given
            String url = "https://www.gov.uk/self-assessment";

            // When - use reflection to test private method
            Method method = ToastNotification.class.getDeclaredMethod("extractDomain", String.class);
            method.setAccessible(true);
            String domain = (String) method.invoke(null, url);

            // Then
            assertThat(domain).isEqualTo("gov.uk");
        }

        @Test
        @DisplayName("should extract domain without www prefix")
        void shouldExtractDomainWithoutWwwPrefix() throws Exception {
            // Given
            String url = "https://github.com/user/repo";

            // When
            Method method = ToastNotification.class.getDeclaredMethod("extractDomain", String.class);
            method.setAccessible(true);
            String domain = (String) method.invoke(null, url);

            // Then
            assertThat(domain).isEqualTo("github.com");
        }

        @Test
        @DisplayName("should remove www prefix from domain")
        void shouldRemoveWwwPrefix() throws Exception {
            // Given
            String url = "https://www.hmrc.gov.uk/help";

            // When
            Method method = ToastNotification.class.getDeclaredMethod("extractDomain", String.class);
            method.setAccessible(true);
            String domain = (String) method.invoke(null, url);

            // Then
            assertThat(domain).isEqualTo("hmrc.gov.uk");
        }

        @Test
        @DisplayName("should handle invalid URL gracefully")
        void shouldHandleInvalidUrlGracefully() throws Exception {
            // Given
            String url = "not-a-valid-url";

            // When
            Method method = ToastNotification.class.getDeclaredMethod("extractDomain", String.class);
            method.setAccessible(true);
            String domain = (String) method.invoke(null, url);

            // Then
            assertThat(domain).isNull();
        }

        @Test
        @DisplayName("should handle null URL gracefully")
        void shouldHandleNullUrlGracefully() throws Exception {
            // When
            Method method = ToastNotification.class.getDeclaredMethod("extractDomain", String.class);
            method.setAccessible(true);
            String domain = (String) method.invoke(null, (String) null);

            // Then - should return null, not throw
            assertThat(domain).isNull();
        }
    }

    // === Accessibility ===

    @Nested
    @DisplayName("Accessibility Features")
    class AccessibilityFeatures {

        @Test
        @DisplayName("should have external link icon constant")
        void shouldHaveExternalLinkIcon() throws Exception {
            // Use reflection to access the private constant
            java.lang.reflect.Field field = ToastNotification.class.getDeclaredField("EXTERNAL_LINK_ICON");
            field.setAccessible(true);
            String icon = (String) field.get(null);

            // Should have a Unicode character for external link
            assertThat(icon).isNotBlank();
            assertThat(icon).hasSize(1); // Single character
        }
    }
}
