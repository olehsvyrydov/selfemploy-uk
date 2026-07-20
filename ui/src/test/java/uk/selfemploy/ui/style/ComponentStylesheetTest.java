package uk.selfemploy.ui.style;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the SCSS→CSS→runtime chain: the {@code category-help-popover} rules authored in
 * {@code scss/components/_popover.scss}, compiled by the Sass build to {@code /css/components.css},
 * resolve to the expected colours when the class is applied to a node at runtime.
 *
 * <p>Tagged {@code e2e} because it boots the real JavaFX toolkit (process-global), so it runs with the
 * display-backed tests rather than the plain unit fork.</p>
 */
@Tag("e2e")
@DisplayName("SCSS-compiled component stylesheet")
class ComponentStylesheetTest {

    private static boolean fxReady;

    @BeforeAll
    static void initToolkit() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            fxReady = latch.await(15, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyStarted) {
            fxReady = true;
        } catch (Exception noDisplay) {
            fxReady = false;
        }
        Assumptions.assumeTrue(fxReady, "JavaFX toolkit unavailable (no display); skipping");
    }

    @Test
    @DisplayName("the category-help-popover class from compiled components.css applies at runtime")
    void popoverStyleApplies() throws Exception {
        AtomicReference<VBox> boxRef = new AtomicReference<>();
        AtomicReference<Label> titleRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                VBox box = new VBox();
                box.getStyleClass().add("category-help-popover");
                box.getStylesheets().add(getClass().getResource("/css/components.css").toExternalForm());
                Label title = new Label("Title");
                title.getStyleClass().add("popover-title");
                box.getChildren().add(title);
                new Scene(box);
                box.applyCss();
                box.layout();
                boxRef.set(box);
                titleRef.set(title);
            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                done.countDown();
            }
        });

        assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
        if (errorRef.get() != null) {
            throw new AssertionError("styling the popover failed", errorRef.get());
        }

        Color background = (Color) boxRef.get().getBackground().getFills().get(0).getFill();
        assertThat(background).as("popover background from compiled SCSS").isEqualTo(Color.web("#1e293b"));
        assertThat((Color) titleRef.get().getTextFill())
                .as("popover title colour from compiled SCSS").isEqualTo(Color.web("#f8fafc"));
    }
}
