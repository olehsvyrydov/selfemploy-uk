package uk.selfemploy.ui.help.markdown;

import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders a representative markdown document and checks the resulting JavaFX node tree, in particular
 * that each bullet becomes its own row (the run-on-bullets regression) and that tables and links map
 * to the expected node types. Tagged e2e because it instantiates JavaFX controls.
 */
@Tag("e2e")
@DisplayName("HelpMarkdownRenderer")
@ExtendWith(ApplicationExtension.class)
class HelpMarkdownRendererTest {

    private final HelpMarkdownRenderer renderer = new HelpMarkdownRenderer();

    @Start
    void start(Stage stage) {
        // Required only to initialise the JavaFX toolkit.
    }

    @Test
    @DisplayName("headings render as styled labels")
    void heading() {
        VBox body = renderer.render("## Getting started");
        assertThat(body.getChildren()).first()
            .isInstanceOfSatisfying(Label.class, label -> {
                assertThat(label.getText()).isEqualTo("Getting started");
                assertThat(label.getStyleClass()).contains("help-md-heading");
            });
    }

    @Test
    @DisplayName("each bullet becomes its own row (fixes run-on bullets)")
    void bulletsAreSeparateRows() {
        VBox body = renderer.render("""
            - First
            - Second
            - Third
            """);

        VBox list = (VBox) body.getChildren().getFirst();
        assertThat(list.getStyleClass()).contains("help-md-list");
        assertThat(list.getChildren()).hasSize(3).allSatisfy(row ->
            assertThat(row).isInstanceOf(HBox.class));
    }

    @Test
    @DisplayName("a GFM table renders as a GridPane")
    void table() {
        VBox body = renderer.render("""
            | Date | Requirement |
            |------|-------------|
            | April 2026 | Income over 50k |
            """);

        assertThat(body.getChildren()).anySatisfy(node ->
            assertThat(node).isInstanceOf(GridPane.class));
    }

    @Test
    @DisplayName("a link renders as a Hyperlink")
    void link() {
        VBox body = renderer.render("See [gov.uk](https://www.gov.uk/mtd) for details.");
        assertThat(containsHyperlink(body)).isTrue();
    }

    private boolean containsHyperlink(Node node) {
        if (node instanceof Hyperlink) {
            return true;
        }
        if (node instanceof javafx.scene.Parent parent) {
            return parent.getChildrenUnmodifiable().stream().anyMatch(this::containsHyperlink);
        }
        return false;
    }
}
