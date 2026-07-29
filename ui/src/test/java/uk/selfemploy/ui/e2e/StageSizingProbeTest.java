package uk.selfemploy.ui.e2e;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

/**
 * Reports what the display actually does with a stage that asks for a size.
 *
 * <p>Temporary. The annual-submission width assertions fail only in CI, where a stage asking for
 * 700px is measured at 900, and no constant in the codebase is 900 — so the number is coming from
 * the environment. This prints enough to say which part of it.
 */
@ExtendWith(ApplicationExtension.class)
@Tag("e2e")
class StageSizingProbeTest {

    private Stage stage;

    @Start
    private void start(Stage stage) {
        this.stage = stage;
        stage.setScene(new Scene(new StackPane(), 700, 700));
        stage.setWidth(700);
        report("after setWidth, before show");
        stage.show();
        report("after show");
    }

    private void report(String when) {
        System.out.printf(
            "PROBE[%s] stage=%.1fx%.1f min=%.1f max=%.1f resizable=%s scene=%.1f screen=%s%n",
            when, stage.getWidth(), stage.getHeight(), stage.getMinWidth(), stage.getMaxWidth(),
            stage.isResizable(),
            stage.getScene() == null ? -1 : stage.getScene().getWidth(),
            Screen.getPrimary().getVisualBounds());
    }

    @Test
    void whatTheDisplayDoesWithASevenHundredPixelStage() {
        report("in test body");
        stage.setWidth(840);
        report("after asking for 840");

        stage.setMinWidth(0);
        report("after clearing minWidth");
        stage.setWidth(700);
        report("after clearing minWidth and asking for 700");
    }
}
