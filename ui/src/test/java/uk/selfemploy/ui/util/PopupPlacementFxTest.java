package uk.selfemploy.ui.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

/**
 * Display-backed test that a popup anchored near the screen's bottom edge flips above its anchor
 * instead of rendering off-screen. Tagged {@code e2e} because it needs a JavaFX display.
 */
@ExtendWith(ApplicationExtension.class)
@Tag("e2e")
@DisplayName("PopupPlacement (on screen)")
class PopupPlacementFxTest {

    private Button anchor;

    @Start
    void start(Stage stage) {
        anchor = new Button("anchor");
        Pane root = new Pane(anchor);
        Scene scene = new Scene(root, 220, 50);
        stage.setScene(scene);

        // Park the window at the very bottom of the screen so the anchor has little room below it.
        Rectangle2D visual = Screen.getPrimary().getVisualBounds();
        stage.setX(visual.getMinX() + 20);
        stage.setY(visual.getMaxY() - 50);
        stage.show();
    }

    @Test
    @DisplayName("a tall popup near the bottom edge flips above its anchor and stays on-screen")
    void flipsUpNearBottom() {
        AtomicReference<Popup> ref = new AtomicReference<>();
        Platform.runLater(() -> {
            Region content = new Region();
            content.setMinSize(200, 300);
            content.setPrefSize(200, 300);
            Popup popup = new Popup();
            popup.getContent().add(content);
            PopupPlacement.showBelowOrAbove(popup, anchor);
            ref.set(popup);
        });
        WaitForAsyncUtils.waitForFxEvents();

        Popup popup = ref.get();
        Bounds anchorBounds = anchor.localToScreen(anchor.getBoundsInLocal());
        double screenMaxY = Screen.getPrimary().getVisualBounds().getMaxY();

        // Flipped: the popup opens above the anchor's top edge, not below it.
        assertThat(popup.getY()).isLessThan(anchorBounds.getMinY());
        // On-screen: the popup's bottom does not overflow the screen's bottom edge.
        assertThat(popup.getY() + popup.getHeight()).isLessThanOrEqualTo(screenMaxY + 1.0);

        Platform.runLater(popup::hide);
        WaitForAsyncUtils.waitForFxEvents();
    }
}
