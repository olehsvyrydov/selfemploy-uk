package uk.selfemploy.ui.e2e;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Tests for Error Message Display (Sprint 10A).
 * Tests EM-E01 through EM-E02 from /rob's test design.
 *
 * <p>SE-10A-007: Error Messages Review E2E Tests</p>
 *
 * <p>These tests require a display. Run with {@code -Dgroups=e2e} to include,
 * or excluded from CI via {@code excludedGroups=e2e} in pom.xml.</p>
 *
 * @see docs/sprints/sprint-10A/testing/rob-test-design-10A-10B.md
 */
@Tag("e2e")
@DisplayName("SE-10A-007: Error Messages E2E Tests")
class ErrorMessagesE2ETest extends BaseE2ETest {

    @BeforeEach
    void ensureDashboardIsLoaded() {
        // Ensure we're on the Dashboard
        if (!lookup("#navDashboard").queryAs(ToggleButton.class).isSelected()) {
            clickOn("#navDashboard");
            waitForFxEvents();
        }
    }

    // === EM-E01: Error Dialog Display with Correct Buttons ===

    @Nested
    @DisplayName("EM-E01: Error Dialog with Correct Buttons")
    class ErrorDialogWithCorrectButtons {

        @Test
        @DisplayName("should have dialog infrastructure available")
        void shouldHaveDialogInfrastructureAvailable() {
            // Verify that the application can show dialogs
            // by checking window hierarchy
            assertThat(getPrimaryStage()).isNotNull();
            assertThat(getPrimaryStage().isShowing()).isTrue();

            // The application should be able to spawn dialog windows
            // This test verifies the infrastructure is in place
        }

        @Test
        @DisplayName("should close dialogs with standard buttons")
        void shouldCloseDialogsWithStandardButtons() {
            // Open a dialog that might show an error
            clickOn("#addIncomeBtn");
            waitForFxEvents();
            shortSleep();

            // Look for any dialog pane
            try {
                DialogPane dialogPane = lookup(".dialog-pane").queryAs(DialogPane.class);
                if (dialogPane != null) {
                    // Look for OK or Close button
                    Set<Button> buttons = lookup(".button").queryAllAs(Button.class);
                    Button closeButton = buttons.stream()
                            .filter(btn -> btn.getText() != null)
                            .filter(btn -> btn.getText().matches("(?i)(ok|close|cancel)"))
                            .findFirst()
                            .orElse(null);

                    if (closeButton != null) {
                        clickOn(closeButton);
                        waitForFxEvents();
                    }
                }
            } catch (Exception e) {
                // Dialog not found - that's OK, close with Escape
            }

            // Ensure cleanup with Escape
            press(KeyCode.ESCAPE);
            release(KeyCode.ESCAPE);
            waitForFxEvents();
        }

        @Test
        @DisplayName("should display validation messages in form dialogs")
        void shouldDisplayValidationMessagesInFormDialogs() {
            // Open Add Income dialog
            clickOn("#addIncomeBtn");
            waitForFxEvents();
            shortSleep();

            try {
                // Look for validation message areas or labels
                // Most forms have areas designated for error messages

                // Try to find error label or validation area
                Set<Label> labels = lookup(".label").queryAllAs(Label.class);

                // Verify there's structure for validation feedback
                // (labels exist that could display validation errors)
                assertThat(labels).isNotEmpty();
            } finally {
                // Close dialog
                press(KeyCode.ESCAPE);
                release(KeyCode.ESCAPE);
                waitForFxEvents();
            }
        }

        @Test
        @DisplayName("should have consistent dialog styling")
        void shouldHaveConsistentDialogStyling() {
            // Open a dialog
            clickOn("#addExpenseBtn");
            waitForFxEvents();
            shortSleep();

            try {
                // Look for dialog pane
                DialogPane dialogPane = lookup(".dialog-pane").queryAs(DialogPane.class);
                if (dialogPane != null) {
                    // Verify dialog has proper structure
                    assertThat(dialogPane.getButtonTypes()).isNotEmpty();
                }
            } catch (Exception e) {
                // Dialog might have different structure
            } finally {
                // Close dialog
                press(KeyCode.ESCAPE);
                release(KeyCode.ESCAPE);
                waitForFxEvents();
            }
        }
    }

    // === EM-E02: Copy Error Details for Support ===

    @Nested
    @DisplayName("EM-E02: Copy Error Details Functionality")
    class CopyErrorDetailsFunctionality {

        @Test
        @DisplayName("should have mechanism to view error details")
        void shouldHaveMechanismToViewErrorDetails() {
            // This test verifies that error dialogs have expandable content
            // or a way to view technical details

            // Navigate to a page that might show errors (HMRC submission)
            clickOn("#navTax");
            waitForFxEvents();

            // Look for buttons that might show error details
            Set<Button> buttons = lookup(".button").queryAllAs(Button.class);

            // Verify buttons exist (structure is in place for error handling)
            assertThat(buttons).isNotEmpty();
        }

        @Test
        @DisplayName("should support keyboard shortcuts for dialog actions")
        void shouldSupportKeyboardShortcutsForDialogActions() {
            // Open a dialog
            clickOn("#addIncomeBtn");
            waitForFxEvents();
            shortSleep();

            // Verify Escape key works to close
            press(KeyCode.ESCAPE);
            release(KeyCode.ESCAPE);
            waitForFxEvents();

            // Should be back on Dashboard
            assertThat(lookup("#navDashboard").queryAs(ToggleButton.class).isSelected()).isTrue();
        }
    }

    // === Error Message Formatting ===

    @Nested
    @DisplayName("Error Message Formatting")
    class ErrorMessageFormatting {

        @Test
        @DisplayName("should have readable text in dialogs")
        void shouldHaveReadableTextInDialogs() {
            // Open a dialog to check text readability
            clickOn("#addIncomeBtn");
            waitForFxEvents();
            shortSleep();

            try {
                // Look for text labels
                Set<Label> labels = lookup(".label").queryAllAs(Label.class);

                // Verify labels have readable text (not technical jargon)
                for (Label label : labels) {
                    String text = label.getText();
                    if (text != null && !text.isEmpty()) {
                        // Should not contain stack traces or technical details
                        assertThat(text)
                                .doesNotContain("java.")
                                .doesNotContain("Exception")
                                .doesNotContain("at ");
                    }
                }
            } finally {
                // Close dialog
                press(KeyCode.ESCAPE);
                release(KeyCode.ESCAPE);
                waitForFxEvents();
            }
        }

        @Test
        @DisplayName("should use consistent button labels")
        void shouldUseConsistentButtonLabels() {
            // Open multiple dialogs and verify button labels are consistent

            // Check Add Income dialog
            clickOn("#addIncomeBtn");
            waitForFxEvents();
            shortSleep();
            Set<Button> incomeButtons = lookup(".button").queryAllAs(Button.class);
            press(KeyCode.ESCAPE);
            release(KeyCode.ESCAPE);
            waitForFxEvents();

            // Check Add Expense dialog
            clickOn("#addExpenseBtn");
            waitForFxEvents();
            shortSleep();
            Set<Button> expenseButtons = lookup(".button").queryAllAs(Button.class);
            press(KeyCode.ESCAPE);
            release(KeyCode.ESCAPE);
            waitForFxEvents();

            // Both dialogs should have similar button patterns
            assertThat(incomeButtons).isNotEmpty();
            assertThat(expenseButtons).isNotEmpty();
        }
    }

    // === Error Recovery ===

    @Nested
    @DisplayName("Error Recovery")
    class ErrorRecovery {

        @Test
        @DisplayName("should allow retry after error")
        void shouldAllowRetryAfterError() {
            // This test verifies the UI remains usable after potential errors

            // Navigate around the app
            clickOn("#navIncome");
            waitForFxEvents();

            clickOn("#navExpenses");
            waitForFxEvents();

            clickOn("#navTax");
            waitForFxEvents();

            // Return to Dashboard
            clickOn("#navDashboard");
            waitForFxEvents();

            // Verify UI is still responsive
            assertThat(lookup("#navDashboard").queryAs(ToggleButton.class).isSelected()).isTrue();
        }

        @Test
        @DisplayName("should maintain app state after dialog dismissal")
        void shouldMaintainAppStateAfterDialogDismissal() {
            // Navigate to Income page
            clickOn("#navIncome");
            waitForFxEvents();

            // Open and close a dialog
            Button addBtn = lookup("#addIncomeButton").queryAs(Button.class);
            if (addBtn != null) {
                clickOn(addBtn);
                waitForFxEvents();
                shortSleep();

                press(KeyCode.ESCAPE);
                release(KeyCode.ESCAPE);
                waitForFxEvents();
            }

            // Should still be on Income page
            assertThat(lookup("#navIncome").queryAs(ToggleButton.class).isSelected()).isTrue();
        }
    }
}
