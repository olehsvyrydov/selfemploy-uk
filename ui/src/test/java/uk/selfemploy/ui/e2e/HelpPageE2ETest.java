package uk.selfemploy.ui.e2e;

import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Tests for Help Page functionality.
 * Tests that Help Topic buttons work when loaded via main navigation.
 */
@Tag("e2e")
@DisplayName("Help Page E2E Tests")
class HelpPageE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Navigate to Help page and verify buttons exist")
    void navigateToHelpPageAndVerifyButtons() {
        // Navigate to Help page
        clickOn("#helpButton");
        waitForFxEvents();

        // Verify Help page loaded
        assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Help & Support");

        // Find help topic buttons
        var buttons = lookup(".help-topic-button").queryAllAs(Button.class);
        System.out.println("Found " + buttons.size() + " help topic buttons on Help page");

        // We expect 11 help topic buttons
        assertThat(buttons.size()).isGreaterThanOrEqualTo(9);

        // Verify all buttons have onAction handlers
        for (Button btn : buttons) {
            System.out.println("Button: " + btn.getText() +
                " - onAction: " + (btn.getOnAction() != null ? "SET" : "NULL") +
                " - visible: " + btn.isVisible() +
                " - disabled: " + btn.isDisabled() +
                " - mouseTransparent: " + btn.isMouseTransparent());
            assertThat(btn.getOnAction())
                .as("Button '%s' should have onAction handler", btn.getText())
                .isNotNull();
        }
    }

    @Test
    @DisplayName("Click Net Profit help topic button shows dialog")
    void clickNetProfitButtonShowsDialog() {
        // Navigate to Help page
        clickOn("#helpButton");
        waitForFxEvents();

        // Verify Help page loaded
        assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Help & Support");

        // Find the Net Profit button
        Button netProfitBtn = lookup(".help-topic-button").nth(0).queryButton();
        assertThat(netProfitBtn).isNotNull();
        assertThat(netProfitBtn.getText()).contains("Net Profit");

        System.out.println("=== Before clicking Net Profit button ===");
        System.out.println("Button: " + netProfitBtn.getText());
        System.out.println("Button bounds: " + netProfitBtn.getBoundsInParent());
        System.out.println("Button scene: " + (netProfitBtn.getScene() != null ? "present" : "null"));

        // Count windows before click
        List<Window> windowsBefore = Window.getWindows().stream().toList();
        System.out.println("Windows before click: " + windowsBefore.size());

        // Click the button
        clickOn(netProfitBtn);
        waitForFxEvents();

        // Wait a bit for dialog to appear
        sleep(500);

        System.out.println("=== After clicking Net Profit button ===");

        // Count windows after click - should have one more (the dialog)
        List<Window> windowsAfter = Window.getWindows().stream().toList();
        System.out.println("Windows after click: " + windowsAfter.size());

        // Look for dialog
        var dialogPanes = lookup(".dialog-pane").queryAllAs(DialogPane.class);
        System.out.println("Dialog panes found: " + dialogPanes.size());

        // Verify a dialog appeared (either as new window or dialog pane)
        boolean dialogFound = windowsAfter.size() > windowsBefore.size() || !dialogPanes.isEmpty();

        if (dialogFound) {
            System.out.println("SUCCESS: Dialog was shown!");

            // Close the dialog if it's open
            if (!dialogPanes.isEmpty()) {
                type(javafx.scene.input.KeyCode.ESCAPE);
                waitForFxEvents();
            }
        } else {
            System.out.println("WARNING: No dialog found after click");
            // Print more debug info
            for (Window w : windowsAfter) {
                if (w instanceof Stage stage) {
                    System.out.println("  Stage: " + stage.getTitle() + " showing=" + stage.isShowing());
                }
            }
        }

        // Assert that dialog appeared
        assertThat(dialogFound)
            .as("Clicking Net Profit button should show a help dialog")
            .isTrue();
    }

    @Test
    @DisplayName("Quick Links buttons have handlers")
    void quickLinksButtonsHaveHandlers() {
        // Navigate to Help page
        clickOn("#helpButton");
        waitForFxEvents();

        // Verify Help page loaded
        assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Help & Support");

        // Find quick link buttons
        var quickLinkButtons = lookup(".help-link-button").queryAllAs(Button.class);
        System.out.println("Found " + quickLinkButtons.size() + " quick link buttons");

        // We expect 6 quick link buttons
        assertThat(quickLinkButtons.size()).isGreaterThanOrEqualTo(6);

        // Verify all buttons have onAction handlers
        for (Button btn : quickLinkButtons) {
            System.out.println("Quick Link: " + btn.getText() +
                " - onAction: " + (btn.getOnAction() != null ? "SET" : "NULL"));
            assertThat(btn.getOnAction())
                .as("Quick link '%s' should have onAction handler", btn.getText())
                .isNotNull();
        }
    }

    @Test
    @DisplayName("Help page buttons are interactive")
    void helpPageButtonsAreInteractive() {
        // Navigate to Help page
        clickOn("#helpButton");
        waitForFxEvents();

        // Find the first help topic button
        Button firstBtn = lookup(".help-topic-button").nth(0).queryButton();
        assertThat(firstBtn).isNotNull();

        // Verify button properties that affect interactivity
        assertThat(firstBtn.isVisible()).as("Button should be visible").isTrue();
        assertThat(firstBtn.isDisabled()).as("Button should not be disabled").isFalse();
        assertThat(firstBtn.isMouseTransparent()).as("Button should not be mouse transparent").isFalse();
        assertThat(firstBtn.getOnAction()).as("Button should have onAction handler").isNotNull();

        // Verify button has non-zero dimensions
        assertThat(firstBtn.getWidth()).as("Button width should be positive").isGreaterThan(0);
        assertThat(firstBtn.getHeight()).as("Button height should be positive").isGreaterThan(0);

        System.out.println("Button is fully interactive:");
        System.out.println("  - visible: " + firstBtn.isVisible());
        System.out.println("  - disabled: " + firstBtn.isDisabled());
        System.out.println("  - mouseTransparent: " + firstBtn.isMouseTransparent());
        System.out.println("  - width: " + firstBtn.getWidth());
        System.out.println("  - height: " + firstBtn.getHeight());
        System.out.println("  - onAction: " + firstBtn.getOnAction());
    }
}
