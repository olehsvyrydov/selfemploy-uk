package uk.selfemploy.ui.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import uk.selfemploy.ui.i18n.Messages;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every way into the help page is actually wired to something.
 *
 * <p>The failure this exists for is silent: a topic row or a quick link added to the FXML without its
 * handler looks perfectly correct on screen and does nothing when clicked. Only loading the real file
 * catches it, because FXML wiring is resolved at load time.
 *
 * <p>It verifies the wiring rather than clicking. Opening a topic calls {@code showAndWaitDialog()},
 * and a modal in a test run blocks the thread it was invoked on rather than failing.
 *
 * <p>Tagged {@code e2e} because it needs a JavaFX toolkit. Run it with
 * {@code -Dsurefire.excludedGroups=} — {@code -DexcludedGroups=} matches no property and silently
 * runs nothing.
 */
@ExtendWith(ApplicationExtension.class)
@Tag("e2e")
@DisplayName("Help page - every topic and link goes somewhere")
class HelpTopicButtonTest {

    @Start
    private void start(Stage stage) throws Exception {
        FXMLLoader loader = Messages.loader(getClass().getResource("/fxml/help.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/css/help.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    @Test
    @DisplayName("every topic row opens something when clicked")
    void everyTopicRowIsWired(FxRobot robot) {
        var rows = robot.lookup(".help-topic-row").queryAllAs(HBox.class);

        assertThat(rows).as("the help page lists topics").isNotEmpty();
        for (HBox row : rows) {
            assertThat(row.getOnMouseClicked())
                    .as("a topic row with no handler looks right and does nothing: %s", labelIn(row, "help-topic-title"))
                    .isNotNull();
        }
    }

    @Test
    @DisplayName("every topic row carries the text a reader needs to choose it")
    void everyTopicRowIsLegible(FxRobot robot) {
        var rows = robot.lookup(".help-topic-row").queryAllAs(HBox.class);

        for (HBox row : rows) {
            assertThat(labelIn(row, "help-topic-title")).as("topic title").isNotBlank();
            assertThat(labelIn(row, "help-topic-desc")).as("topic description").isNotBlank();
        }
    }

    @Test
    @DisplayName("every quick link is wired")
    void everyQuickLinkIsWired(FxRobot robot) {
        var links = robot.lookup(".help-quick-link").queryAllAs(Button.class);

        assertThat(links).as("the help page offers quick links").isNotEmpty();
        for (Button link : links) {
            assertThat(link.getOnAction())
                    .as("quick link with no handler: %s", link.getText())
                    .isNotNull();
        }
    }

    @Test
    @DisplayName("the page keeps its sections")
    void theSectionsArePresent(FxRobot robot) {
        // Named individually rather than counted, so a removal names what went missing.
        for (String section : new String[] {
                "help-section-guide", "help-section-tax", "help-section-expenses",
                "help-section-hmrc", "help-section-general", "help-section-support",
                "help-section-about"}) {
            assertThat(robot.lookup("." + section).queryAll()).as(section).isNotEmpty();
        }
    }

    @Test
    @DisplayName("topic badges stay short text, never emoji")
    void badgesAreShortText(FxRobot robot) {
        var badges = robot.lookup(".help-topic-badge").queryAllAs(Label.class);

        assertThat(badges).isNotEmpty();
        for (Label badge : badges) {
            assertThat(badge.getText())
                    .as("badges are drawn as text, so an emoji renders inconsistently across platforms")
                    .matches("[A-Za-z0-9£%!?]{1,3}");
        }
    }

    /** The text of the first label carrying {@code styleClass} inside {@code row}, or empty. */
    private static String labelIn(HBox row, String styleClass) {
        return row.lookupAll("." + styleClass).stream()
                .filter(Label.class::isInstance)
                .map(node -> ((Label) node).getText())
                .findFirst()
                .orElse("");
    }
}
