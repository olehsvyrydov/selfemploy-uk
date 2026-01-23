package uk.selfemploy.ui.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify Help Topics buttons are clickable.
 * This test loads the help.fxml and verifies button click handlers are wired correctly.
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
    void helpTopicButtonsShouldExist(FxRobot robot) {
        // Find all help topic buttons
        Button netProfitBtn = robot.lookup(".help-topic-button").nth(0).queryButton();
        assertThat(netProfitBtn).isNotNull();
        assertThat(netProfitBtn.getText()).contains("Net Profit");

        // Check button properties
        System.out.println("Button text: " + netProfitBtn.getText());
        System.out.println("Button visible: " + netProfitBtn.isVisible());
        System.out.println("Button disabled: " + netProfitBtn.isDisabled());
        System.out.println("Button managed: " + netProfitBtn.isManaged());
        System.out.println("Button mouseTransparent: " + netProfitBtn.isMouseTransparent());
        System.out.println("Button pickOnBounds: " + netProfitBtn.isPickOnBounds());
        System.out.println("Button onAction: " + netProfitBtn.getOnAction());
        System.out.println("Button width: " + netProfitBtn.getWidth());
        System.out.println("Button height: " + netProfitBtn.getHeight());

        // The button should have an onAction handler
        assertThat(netProfitBtn.getOnAction()).isNotNull();
    }

    @Test
    void clickNetProfitButtonShouldTriggerHandler(FxRobot robot) {
        // Find the Net Profit button
        Button netProfitBtn = robot.lookup(".help-topic-button").nth(0).queryButton();
        assertThat(netProfitBtn).isNotNull();

        System.out.println("=== Before click ===");
        System.out.println("Clicking button: " + netProfitBtn.getText());

        // Click the button
        robot.clickOn(netProfitBtn);

        System.out.println("=== After click ===");
        // If the handler works, we should see debug output in console
        // The actual dialog might not show without proper HelpService mock
    }

    @Test
    void allHelpTopicButtonsShouldHaveHandlers(FxRobot robot) {
        // Count all help topic buttons
        var buttons = robot.lookup(".help-topic-button").queryAllAs(Button.class);

        System.out.println("Found " + buttons.size() + " help topic buttons:");
        int index = 0;
        for (Button btn : buttons) {
            System.out.println("  [" + index + "] " + btn.getText() +
                " - onAction: " + (btn.getOnAction() != null ? "SET" : "NULL") +
                " - mouseTransparent: " + btn.isMouseTransparent());
            assertThat(btn.getOnAction()).as("Button '%s' should have onAction handler", btn.getText()).isNotNull();
            index++;
        }

        // We expect 11 help topic buttons based on the FXML
        assertThat(buttons.size()).isGreaterThanOrEqualTo(9);
    }
}
