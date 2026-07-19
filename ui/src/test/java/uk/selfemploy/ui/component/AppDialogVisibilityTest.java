package uk.selfemploy.ui.component;

import javafx.application.Platform;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PR-CI regression guard for the invisible-dialog defect (B6).
 *
 * <p>The original bug was a {@link javafx.scene.control.Alert} that blocked the app while
 * rendering nothing on screen. {@link AppDialog} replaces it with a real, owned {@link Stage}.
 * This test boots the JavaFX toolkit directly (no TestFX / Monocle dependency) so it runs in the
 * ordinary PR test job under a virtual display (Xvfb in CI), and asserts that a shown AppDialog is
 * a genuine, on-screen, non-degenerate window built on the transparent application-modal Stage
 * recipe — the properties a raw Alert regression would violate.</p>
 *
 * <p>If no JavaFX-capable display is available at all (a truly headless box with no Xvfb), the
 * toolkit cannot start and the assertions are skipped rather than failing the build.</p>
 */
@DisplayName("AppDialog visibility (PR CI)")
class AppDialogVisibilityTest {

    private static boolean fxReady;

    @BeforeAll
    static void initToolkit() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            fxReady = latch.await(15, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyStarted) {
            // Toolkit was initialised by another test in the same JVM — fine to reuse.
            fxReady = true;
        } catch (Throwable noDisplay) {
            fxReady = false;
        }
        if (fxReady) {
            Platform.setImplicitExit(false);
        }
        Assumptions.assumeTrue(fxReady, "JavaFX toolkit unavailable (no display); skipping visibility guard");
    }

    @AfterAll
    static void tearDown() {
        // Leave the toolkit running for any other FX tests in the JVM; setImplicitExit(false)
        // above already prevents Platform from exiting when the last window closes.
    }

    @Test
    @DisplayName("a shown AppDialog is a real, on-screen, non-degenerate Stage (not an invisible Alert)")
    void shownDialogIsVisibleStage() throws Exception {
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch shown = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                AppDialog dialog = new AppDialog(AppDialog.Kind.INFO, "Saved",
                        "Your changes were saved.", "OK", null);
                Stage stage = dialog.stageForTest();
                stage.show();
                stageRef.set(stage);
            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                shown.countDown();
            }
        });
        assertThat(shown.await(15, TimeUnit.SECONDS)).as("dialog construction ran on FX thread").isTrue();
        if (errorRef.get() != null) {
            throw new AssertionError("AppDialog failed to build/show", errorRef.get());
        }

        Stage stage = stageRef.get();
        assertThat(stage).isNotNull();
        assertThat(stage.isShowing()).as("stage is actually showing").isTrue();
        assertThat(stage.getScene()).as("stage has a scene").isNotNull();
        assertThat(stage.getScene().getRoot()).as("scene has content").isNotNull();
        // The invisible-Alert defect produced a zero-size / off-screen window.
        assertThat(stage.getWidth()).as("stage has a real width").isGreaterThan(0);
        assertThat(stage.getHeight()).as("stage has a real height").isGreaterThan(0);
        // Proves this is the visible-Stage recipe, not a JavaFX Alert.
        assertThat(stage.getStyle()).isEqualTo(StageStyle.TRANSPARENT);
        assertThat(stage.getModality()).isEqualTo(Modality.APPLICATION_MODAL);

        CountDownLatch closed = new CountDownLatch(1);
        Platform.runLater(() -> {
            stage.close();
            closed.countDown();
        });
        assertThat(closed.await(10, TimeUnit.SECONDS)).isTrue();
    }
}
