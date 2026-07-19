package uk.selfemploy.ui.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;
import uk.selfemploy.ui.i18n.Messages;

/**
 * Layout tests for the annual submission wizard (T1.8): the content must not
 * scroll horizontally at the 840px design width, and the primary action and step
 * indicator must stay pinned in the viewport.
 *
 * <p>Tagged e2e because it needs the JavaFX toolkit to load the FXML.</p>
 */
@Tag("e2e")
@DisplayName("Annual submission layout")
@ExtendWith(ApplicationExtension.class)
class AnnualSubmissionLayoutTest {

    private static final int DESIGN_WIDTH = 840;

    private BorderPane root;
    private ScrollPane scroll;

    @Start
    void start(Stage stage) throws Exception {
        FXMLLoader loader = Messages.loader(getClass().getResource("/fxml/annual-submission.fxml"));
        root = loader.load();
        scroll = (ScrollPane) root.lookup("#rootScroll");
        Scene scene = new Scene(root, DESIGN_WIDTH, 950);
        stage.setScene(scene);
        stage.setWidth(DESIGN_WIDTH);
        stage.show();
    }

    @Test
    @DisplayName("never shows a horizontal scrollbar and fits content to width")
    void noHorizontalScroll() {
        assertThat(scroll).isNotNull();
        assertThat(scroll.getHbarPolicy()).isEqualTo(ScrollPane.ScrollBarPolicy.NEVER);
        assertThat(scroll.isFitToWidth()).isTrue();
    }

    @Test
    @DisplayName("pins the step indicator at the top and the action bar at the bottom")
    void pinsHeaderAndActionBar() {
        Node actionBar = root.lookup("#actionBar");
        Node progressIndicator = root.lookup("#progressIndicator");

        assertThat(actionBar).isInstanceOf(HBox.class);
        assertThat(progressIndicator).isNotNull();

        // The action bar is pinned in the BorderPane's bottom region, not inside the
        // scrolling content, so it never scrolls out of view.
        assertThat(isDescendantOf(actionBar, root.getBottom())).isTrue();
        assertThat(isDescendantOf(actionBar, scroll)).isFalse();
        assertThat(isDescendantOf(progressIndicator, root.getTop())).isTrue();
    }

    @Test
    @DisplayName("provides a Done button on the success step")
    void hasDoneButton() {
        Node done = root.lookup("#doneButton");
        assertThat(done).isInstanceOf(Button.class);
        assertThat(((Button) done).getText()).isEqualTo("Done");
    }

    @Test
    @DisplayName("resolves the legal declaration labels from the message bundle, not raw keys")
    void resolvesDeclarationText() {
        for (int row = 1; row <= 6; row++) {
            Node node = root.lookup("#decl" + row + "Text");
            assertThat(node).as("declaration row %d label", row).isInstanceOf(Label.class);
            String text = ((Label) node).getText();
            assertThat(text)
                .as("declaration row %d must show real wording, not the %%key", row)
                .doesNotStartWith("%")
                .isEqualTo(Messages.get("annual.declaration.row" + row));
        }
    }

    private static boolean isDescendantOf(Node node, Node ancestor) {
        if (node == null || ancestor == null) {
            return false;
        }
        for (Node current = node; current != null; current = current.getParent()) {
            if (current == ancestor) {
                return true;
            }
        }
        return false;
    }
}
