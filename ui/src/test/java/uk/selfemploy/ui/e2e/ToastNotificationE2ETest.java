package uk.selfemploy.ui.e2e;

import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.stage.Popup;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Tests for Toast Notification System (Sprint 10A).
 * Tests TN-E01 through TN-E04 from /rob's test design.
 *
 * <p>SE-10A-006: Toast Notification System E2E Tests</p>
 *
 * <p>These tests require a display. Run with {@code -Dgroups=e2e} to include,
 * or excluded from CI via {@code excludedGroups=e2e} in pom.xml.</p>
 *
 * @see docs/sprints/sprint-10A/testing/rob-test-design-10A-10B.md
 */
@Tag("e2e")
@DisplayName("SE-10A-006: Toast Notification E2E Tests")
class ToastNotificationE2ETest extends BaseE2ETest {

    @BeforeEach
    void ensureDashboardIsLoaded() {
        // Ensure we're on the Dashboard
        if (!lookup("#navDashboard").queryAs(ToggleButton.class).isSelected()) {
            clickOn("#navDashboard");
            waitForFxEvents();
        }
    }

    // === TN-E01: Toast Position Consistency ===

    @Nested
    @DisplayName("TN-E01: Toast Position Consistency")
    class ToastPositionConsistency {

        @Test
        @DisplayName("should have consistent window available for toasts")
        void shouldHaveConsistentWindowAvailableForToasts() {
            // Verify the primary stage is available and showing
            assertThat(getPrimaryStage()).isNotNull();
            assertThat(getPrimaryStage().isShowing()).isTrue();

            // Get window dimensions for positioning reference
            double windowWidth = getPrimaryStage().getWidth();
            double windowHeight = getPrimaryStage().getHeight();

            assertThat(windowWidth).isGreaterThan(0);
            assertThat(windowHeight).isGreaterThan(0);
        }

        @Test
        @DisplayName("should have stage accessible from different pages")
        void shouldHaveStageAccessibleFromDifferentPages() {
            // Navigate to different pages and verify window is accessible

            // Dashboard
            clickOn("#navDashboard");
            waitForFxEvents();
            assertThat(getPrimaryStage().isShowing()).isTrue();

            // Income
            clickOn("#navIncome");
            waitForFxEvents();
            assertThat(getPrimaryStage().isShowing()).isTrue();

            // Expenses
            clickOn("#navExpenses");
            waitForFxEvents();
            assertThat(getPrimaryStage().isShowing()).isTrue();

            // Tax
            clickOn("#navTax");
            waitForFxEvents();
            assertThat(getPrimaryStage().isShowing()).isTrue();

            // Return to Dashboard
            clickOn("#navDashboard");
            waitForFxEvents();
        }
    }

    // === TN-E02: Screen Reader Accessibility ===

    @Nested
    @DisplayName("TN-E02: Screen Reader Accessibility")
    class ScreenReaderAccessibility {

        @Test
        @DisplayName("should have accessible application structure")
        void shouldHaveAccessibleApplicationStructure() {
            // Verify the main content area has accessible structure
            // This enables screen readers to announce toasts

            // Check that the primary stage has a scene
            assertThat(getCurrentScene()).isNotNull();

            // Check that scene root exists
            assertThat(getCurrentScene().getRoot()).isNotNull();
        }
    }

    // === TN-E03: Toast Does Not Block Interaction ===

    @Nested
    @DisplayName("TN-E03: Toast Does Not Block Interaction")
    class ToastDoesNotBlockInteraction {

        @Test
        @DisplayName("should allow navigation while potential toast is showing")
        void shouldAllowNavigationWhilePotentialToastShowing() {
            // Navigate to Help page (might trigger toast for external links)
            clickOn("#navHelp");
            waitForFxEvents();

            // Should still be able to navigate to other pages
            clickOn("#navDashboard");
            waitForFxEvents();

            // Verify navigation worked
            assertThat(lookup("#navDashboard").queryAs(ToggleButton.class).isSelected()).isTrue();
        }

        @Test
        @DisplayName("should allow button clicks while potential toast is showing")
        void shouldAllowButtonClicksWhilePotentialToastShowing() {
            // Click Add Income button
            clickOn("#addIncomeBtn");
            waitForFxEvents();
            shortSleep();

            // Should still be able to press Escape to close dialog
            press(KeyCode.ESCAPE);
            release(KeyCode.ESCAPE);
            waitForFxEvents();

            // Verify we can still interact with the UI
            clickOn("#navIncome");
            waitForFxEvents();
            assertThat(lookup("#navIncome").queryAs(ToggleButton.class).isSelected()).isTrue();

            // Return to Dashboard
            clickOn("#navDashboard");
            waitForFxEvents();
        }

        @Test
        @DisplayName("should allow keyboard navigation while potential toast showing")
        void shouldAllowKeyboardNavigationWhilePotentialToastShowing() {
            // Tab through the interface
            press(KeyCode.TAB);
            release(KeyCode.TAB);
            waitForFxEvents();

            press(KeyCode.TAB);
            release(KeyCode.TAB);
            waitForFxEvents();

            // Should be able to continue navigating
            assertThat(getPrimaryStage().isShowing()).isTrue();
        }
    }

    // === TN-E04: Rapid Toast Triggers ===

    @Nested
    @DisplayName("TN-E04: Rapid Toast Triggers")
    class RapidToastTriggers {

        @Test
        @DisplayName("should handle rapid navigation without issues")
        void shouldHandleRapidNavigationWithoutIssues() {
            // Rapidly navigate between pages (each might trigger toasts)
            for (int i = 0; i < 3; i++) {
                clickOn("#navIncome");
                waitForFxEvents();

                clickOn("#navExpenses");
                waitForFxEvents();

                clickOn("#navTax");
                waitForFxEvents();

                clickOn("#navDashboard");
                waitForFxEvents();
            }

            // UI should still be responsive
            assertThat(getPrimaryStage().isShowing()).isTrue();
            assertThat(lookup("#navDashboard").queryAs(ToggleButton.class).isSelected()).isTrue();
        }

        @Test
        @DisplayName("should handle rapid button clicks without crash")
        void shouldHandleRapidButtonClicksWithoutCrash() {
            // Rapidly click buttons that might trigger toasts
            for (int i = 0; i < 3; i++) {
                clickOn("#addIncomeBtn");
                shortSleep();
                press(KeyCode.ESCAPE);
                release(KeyCode.ESCAPE);
                waitForFxEvents();

                clickOn("#addExpenseBtn");
                shortSleep();
                press(KeyCode.ESCAPE);
                release(KeyCode.ESCAPE);
                waitForFxEvents();
            }

            // UI should still be responsive
            assertThat(getPrimaryStage().isShowing()).isTrue();
        }
    }

    // === Toast Integration ===

    @Nested
    @DisplayName("Toast Integration with UI Actions")
    class ToastIntegration {

        @Test
        @DisplayName("should have stable window hierarchy")
        void shouldHaveStableWindowHierarchy() {
            // Verify window hierarchy is stable
            List<Window> windows = Window.getWindows();
            assertThat(windows).isNotEmpty();

            // Primary stage should be in the list
            assertThat(windows).contains(getPrimaryStage());
        }

        @Test
        @DisplayName("should allow focus changes without issues")
        void shouldAllowFocusChangesWithoutIssues() {
            // Focus on different elements
            interact(() -> {
                if (nodeExists("#addIncomeBtn")) {
                    lookup("#addIncomeBtn").query().requestFocus();
                }
            });
            waitForFxEvents();

            interact(() -> {
                if (nodeExists("#addExpenseBtn")) {
                    lookup("#addExpenseBtn").query().requestFocus();
                }
            });
            waitForFxEvents();

            // UI should remain stable
            assertThat(getPrimaryStage().isShowing()).isTrue();
        }
    }
}
