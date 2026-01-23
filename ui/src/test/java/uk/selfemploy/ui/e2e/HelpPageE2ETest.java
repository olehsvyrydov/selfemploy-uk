package uk.selfemploy.ui.e2e;

import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Tests for Help Page functionality.
 * Tests that Help Topic cards work when loaded via main navigation.
 *
 * Updated for text-based icons (no emoji) per /aura design v2.
 */
@Tag("e2e")
@DisplayName("Help Page E2E Tests")
class HelpPageE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Navigate to Help page and verify help topic cards exist")
    void navigateToHelpPageAndVerifyCards() {
        // Navigate to Help page
        clickOn("#helpButton");
        waitForFxEvents();

        // Verify Help page loaded
        assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Help & Support");

        // Find help topic cards (HBox with .help-topic-card class)
        Set<HBox> cards = lookup(".help-topic-card").queryAllAs(HBox.class);
        System.out.println("Found " + cards.size() + " help topic cards on Help page");

        // We expect 11 help topic cards
        assertThat(cards.size()).isGreaterThanOrEqualTo(9);

        // Verify all cards have onMouseClicked handlers
        for (HBox card : cards) {
            assertThat(card.getOnMouseClicked())
                .as("Help topic card should have onMouseClicked handler")
                .isNotNull();
        }
    }

    @Test
    @DisplayName("Click Net Profit help topic card shows dialog")
    void clickNetProfitCardShowsDialog() {
        // Navigate to Help page
        clickOn("#helpButton");
        waitForFxEvents();

        // Verify Help page loaded
        assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Help & Support");

        // Find the first help topic card (Net Profit)
        HBox netProfitCard = lookup(".help-topic-card").nth(0).queryAs(HBox.class);
        assertThat(netProfitCard).isNotNull();

        // Count windows before click
        List<Window> windowsBefore = Window.getWindows().stream().toList();

        // Click the card
        clickOn(netProfitCard);
        waitForFxEvents();
        sleep(500);

        // Count windows after click
        List<Window> windowsAfter = Window.getWindows().stream().toList();

        // Look for dialog
        var dialogPanes = lookup(".dialog-pane").queryAllAs(DialogPane.class);
        var helpDialogPanes = lookup(".help-dialog-pane").queryAll();

        // Verify a dialog appeared
        boolean dialogFound = windowsAfter.size() > windowsBefore.size()
            || !dialogPanes.isEmpty()
            || !helpDialogPanes.isEmpty();

        if (dialogFound) {
            System.out.println("SUCCESS: Dialog was shown!");
            type(javafx.scene.input.KeyCode.ESCAPE);
            waitForFxEvents();
        }

        assertThat(dialogFound)
            .as("Clicking Net Profit card should show a help dialog")
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
            assertThat(btn.getOnAction())
                .as("Quick link should have onAction handler")
                .isNotNull();
        }
    }

    @Test
    @DisplayName("Help topic cards are interactive")
    void helpTopicCardsAreInteractive() {
        // Navigate to Help page
        clickOn("#helpButton");
        waitForFxEvents();

        // Find the first help topic card
        HBox firstCard = lookup(".help-topic-card").nth(0).queryAs(HBox.class);
        assertThat(firstCard).isNotNull();

        // Verify card properties
        assertThat(firstCard.isVisible()).as("Card should be visible").isTrue();
        assertThat(firstCard.isDisabled()).as("Card should not be disabled").isFalse();
        assertThat(firstCard.isMouseTransparent()).as("Card should not be mouse transparent").isFalse();
        assertThat(firstCard.getOnMouseClicked()).as("Card should have onMouseClicked handler").isNotNull();
        assertThat(firstCard.getWidth()).as("Card width should be positive").isGreaterThan(0);
        assertThat(firstCard.getHeight()).as("Card height should be positive").isGreaterThan(0);
    }

    @Test
    @DisplayName("Help page has category headers")
    void helpPageHasCategoryHeaders() {
        // Navigate to Help page
        clickOn("#helpButton");
        waitForFxEvents();

        // Find category headers
        var categoryHeaders = lookup(".help-category-header").queryAll();
        System.out.println("Found " + categoryHeaders.size() + " category headers");

        // We expect 4 categories
        assertThat(categoryHeaders.size()).isEqualTo(4);
    }

    @Test
    @DisplayName("Topic icons use text-based icons")
    void topicIconsUseTextBasedIcons() {
        // Navigate to Help page
        clickOn("#helpButton");
        waitForFxEvents();

        // Find topic icon text labels
        var iconTexts = lookup(".topic-icon-text").queryAllAs(Label.class);
        System.out.println("Found " + iconTexts.size() + " topic icon texts");

        // Verify they use text-based abbreviations
        for (Label iconText : iconTexts) {
            String text = iconText.getText();
            // Should be text like "NP", "IT", "PA", not emoji
            assertThat(text).as("Icon should be text-based").matches("[A-Za-z0-9£%!?]+");
        }
    }

    @Test
    @DisplayName("Support section exists with buttons")
    void supportSectionExists() {
        // Navigate to Help page
        clickOn("#helpButton");
        waitForFxEvents();

        // Find support card
        var supportCards = lookup(".help-support-card").queryAll();
        assertThat(supportCards.size()).isGreaterThanOrEqualTo(1);

        // Find support buttons
        var supportButtons = lookup(".support-link-button").queryAllAs(Button.class);
        assertThat(supportButtons.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Disclaimer section exists")
    void disclaimerSectionExists() {
        // Navigate to Help page
        clickOn("#helpButton");
        waitForFxEvents();

        // Find disclaimer card
        var disclaimerCards = lookup(".help-disclaimer-card").queryAll();
        assertThat(disclaimerCards.size()).isEqualTo(1);

        // Find disclaimer title
        var disclaimerTitle = lookup(".disclaimer-title").queryAs(Label.class);
        assertThat(disclaimerTitle).isNotNull();
        assertThat(disclaimerTitle.getText()).contains("Disclaimer");
    }
}
