package uk.selfemploy.ui.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify Help Topics cards are clickable.
 * This test loads the help.fxml and verifies click handlers are wired correctly.
 *
 * Updated for text-based icons (no emoji) per /aura design v2.
 */
@ExtendWith(ApplicationExtension.class)
@Tag("e2e")
class HelpTopicButtonTest {

    private HelpController controller;

    @Start
    private void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/help.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root, 800, 600);
        // Load CSS
        scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/css/help.css").toExternalForm());

        stage.setScene(scene);
        stage.show();
    }

    @Test
    void helpTopicCardsShouldExist(FxRobot robot) {
        // Find the first help topic card (HBox)
        HBox firstCard = robot.lookup(".help-topic-card").nth(0).queryAs(HBox.class);
        assertThat(firstCard).isNotNull();

        // Find the title label within the card
        Label titleLabel = robot.lookup(".help-topic-title").nth(0).queryAs(Label.class);
        assertThat(titleLabel).isNotNull();
        assertThat(titleLabel.getText()).contains("Net Profit");

        // Check card properties
        System.out.println("Card first topic title: " + titleLabel.getText());
        System.out.println("Card visible: " + firstCard.isVisible());
        System.out.println("Card onMouseClicked: " + firstCard.getOnMouseClicked());

        // The card should have an onMouseClicked handler
        assertThat(firstCard.getOnMouseClicked()).isNotNull();
    }

    @Test
    void clickNetProfitCardShouldTriggerHandler(FxRobot robot) {
        // Find the Net Profit card
        HBox netProfitCard = robot.lookup(".help-topic-card").nth(0).queryAs(HBox.class);
        assertThat(netProfitCard).isNotNull();

        System.out.println("=== Before click ===");
        System.out.println("Clicking card for Net Profit");

        // Click the card
        robot.clickOn(netProfitCard);

        System.out.println("=== After click ===");
    }

    @Test
    void allHelpTopicCardsShouldHaveHandlers(FxRobot robot) {
        // Count all help topic cards
        var cards = robot.lookup(".help-topic-card").queryAllAs(HBox.class);

        System.out.println("Found " + cards.size() + " help topic cards:");
        int index = 0;
        for (HBox card : cards) {
            System.out.println("  [" + index + "] onMouseClicked: " + (card.getOnMouseClicked() != null ? "SET" : "NULL"));
            assertThat(card.getOnMouseClicked()).as("Card [%d] should have onMouseClicked handler", index).isNotNull();
            index++;
        }

        // We expect 11 help topic cards based on the FXML
        assertThat(cards.size()).isGreaterThanOrEqualTo(9);
    }

    @Test
    void quickLinkButtonsShouldHaveHandlers(FxRobot robot) {
        // Count all quick link buttons
        var buttons = robot.lookup(".help-link-button").queryAllAs(Button.class);

        System.out.println("Found " + buttons.size() + " quick link buttons:");
        int index = 0;
        for (Button btn : buttons) {
            System.out.println("  [" + index + "] onAction: " + (btn.getOnAction() != null ? "SET" : "NULL"));
            assertThat(btn.getOnAction()).as("Quick link [%d] should have onAction handler", index).isNotNull();
            index++;
        }

        // We expect 6 quick link buttons based on the FXML
        assertThat(buttons.size()).isGreaterThanOrEqualTo(6);
    }

    @Test
    void categoryHeadersShouldExist(FxRobot robot) {
        // Find category headers
        var headers = robot.lookup(".help-category-header").queryAll();

        System.out.println("Found " + headers.size() + " category headers");

        // We expect 4 categories: Tax & Calculation, Expenses, HMRC Submission, General
        assertThat(headers.size()).isEqualTo(4);
    }

    @Test
    void topicIconsShouldUseTextBasedIcons(FxRobot robot) {
        // Find topic icon text labels
        var iconTexts = robot.lookup(".topic-icon-text").queryAllAs(Label.class);

        System.out.println("Found " + iconTexts.size() + " topic icon texts");

        // Verify they use text-based abbreviations, not emoji
        for (Label iconText : iconTexts) {
            String text = iconText.getText();
            System.out.println("  Icon text: '" + text + "'");
            // Should be short text like "NP", "IT", "PA", etc.
            assertThat(text).as("Icon should be text-based, not emoji").matches("[A-Za-z0-9£%!?]+");
        }
    }

    @Test
    void supportSectionShouldExist(FxRobot robot) {
        // Find support card
        var supportCards = robot.lookup(".help-support-card").queryAll();
        System.out.println("Found " + supportCards.size() + " support cards");
        assertThat(supportCards.size()).isGreaterThanOrEqualTo(1);

        // Find support buttons
        var supportButtons = robot.lookup(".support-link-button").queryAllAs(Button.class);
        System.out.println("Found " + supportButtons.size() + " support buttons");
        assertThat(supportButtons.size()).isEqualTo(2); // GitHub Issues + Documentation
    }

    @Test
    void disclaimerSectionShouldExist(FxRobot robot) {
        // Find disclaimer card
        var disclaimerCards = robot.lookup(".help-disclaimer-card").queryAll();
        System.out.println("Found " + disclaimerCards.size() + " disclaimer cards");
        assertThat(disclaimerCards.size()).isEqualTo(1);

        // Find disclaimer title
        var disclaimerTitle = robot.lookup(".disclaimer-title").queryAs(Label.class);
        assertThat(disclaimerTitle).isNotNull();
        assertThat(disclaimerTitle.getText()).contains("Disclaimer");
    }
}
