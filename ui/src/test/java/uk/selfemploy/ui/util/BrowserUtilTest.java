package uk.selfemploy.ui.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * TDD Tests for BrowserUtil - utility for opening URLs in system browser.
 *
 * <p>Bug Fix: Force Quit when clicking HMRC guidance link</p>
 *
 * <p>Root Cause: Desktop.browse() was called directly on the JavaFX Application Thread,
 * which can cause crashes on Linux and other platforms where AWT Desktop is not supported
 * or blocks the UI thread.</p>
 *
 * <p>Solution: Create a utility that:</p>
 * <ul>
 *   <li>Runs browser opening on a background thread</li>
 *   <li>Uses platform-specific commands (xdg-open, open, start) as fallback</li>
 *   <li>Handles errors gracefully without crashing</li>
 * </ul>
 */
@DisplayName("BrowserUtil - URL Opening Utility")
class BrowserUtilTest {

    @Nested
    @DisplayName("URL Validation")
    class UrlValidation {

        @Test
        @DisplayName("should handle null URL gracefully")
        void shouldHandleNullUrlGracefully() {
            assertThatCode(() -> BrowserUtil.openUrl(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle empty URL gracefully")
        void shouldHandleEmptyUrlGracefully() {
            assertThatCode(() -> BrowserUtil.openUrl(""))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle blank URL gracefully")
        void shouldHandleBlankUrlGracefully() {
            assertThatCode(() -> BrowserUtil.openUrl("   "))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle malformed URL gracefully")
        void shouldHandleMalformedUrlGracefully() {
            assertThatCode(() -> BrowserUtil.openUrl("not a valid url"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Platform Detection")
    class PlatformDetection {

        @Test
        @DisplayName("should detect Linux platform")
        void shouldDetectLinuxPlatform() {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("linux")) {
                assertThat(BrowserUtil.isLinux()).isTrue();
            }
        }

        @Test
        @DisplayName("should detect macOS platform")
        void shouldDetectMacOsPlatform() {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("mac")) {
                assertThat(BrowserUtil.isMacOs()).isTrue();
            }
        }

        @Test
        @DisplayName("should detect Windows platform")
        void shouldDetectWindowsPlatform() {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                assertThat(BrowserUtil.isWindows()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Browser Command")
    class BrowserCommand {

        @Test
        @DisplayName("should return correct command for current platform")
        void shouldReturnCorrectCommandForCurrentPlatform() {
            String[] command = BrowserUtil.getBrowserCommand("https://example.com");

            assertThat(command).isNotNull();
            assertThat(command.length).isGreaterThan(0);

            // Verify platform-specific command
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("linux")) {
                assertThat(command[0]).isEqualTo("xdg-open");
            } else if (osName.contains("mac")) {
                assertThat(command[0]).isEqualTo("open");
            } else if (osName.contains("win")) {
                assertThat(command[0]).isEqualTo("cmd");
                assertThat(command).contains("/c", "start");
            }
        }

        @Test
        @DisplayName("should include URL in command")
        void shouldIncludeUrlInCommand() {
            String url = "https://www.gov.uk/expenses-if-youre-self-employed";
            String[] command = BrowserUtil.getBrowserCommand(url);

            assertThat(command).isNotNull();

            // The URL should be in the command array
            boolean containsUrl = false;
            for (String part : command) {
                if (part.equals(url)) {
                    containsUrl = true;
                    break;
                }
            }
            assertThat(containsUrl).isTrue();
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("should not throw exception when opening valid HMRC URL")
        void shouldNotThrowExceptionWhenOpeningValidHmrcUrl() {
            // This test verifies the method doesn't crash - actual browser opening
            // depends on system configuration
            String hmrcUrl = "https://www.gov.uk/expenses-if-youre-self-employed";

            assertThatCode(() -> BrowserUtil.openUrl(hmrcUrl))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle callback when opening URL")
        void shouldHandleCallbackWhenOpeningUrl() {
            String url = "https://example.com";

            // Should not throw even with error callback
            assertThatCode(() -> BrowserUtil.openUrl(url, error -> {
                // Error callback - just log for testing
                System.out.println("Browser open error: " + error);
            })).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafety {

        @Test
        @DisplayName("should not block calling thread")
        void shouldNotBlockCallingThread() throws InterruptedException {
            long startTime = System.currentTimeMillis();

            // Opening a URL should return immediately (not block)
            BrowserUtil.openUrl("https://example.com");

            long elapsed = System.currentTimeMillis() - startTime;

            // Should return in under 100ms (browser opens asynchronously)
            assertThat(elapsed).isLessThan(100);
        }
    }
}
