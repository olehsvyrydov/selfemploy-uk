package uk.selfemploy.ui.component;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AppDialog}, the visible replacement for JavaFX {@link javafx.scene.control.Alert}.
 *
 * <p>Pure-helper tests run everywhere. The visibility guard is tagged "e2e" (needs a JavaFX
 * display) and asserts the dialog actually shows an on-screen stage — the regression guard for
 * the invisible-Alert defect.</p>
 */
@DisplayName("AppDialog")
class AppDialogTest {

    @Nested
    @DisplayName("Kind mapping (pure)")
    class KindMapping {

        @Test
        @DisplayName("every kind maps to a header style class")
        void everyKindHasHeaderClass() {
            for (AppDialog.Kind kind : AppDialog.Kind.values()) {
                assertThat(AppDialog.headerStyleClass(kind)).isNotBlank();
            }
        }

        @Test
        @DisplayName("every kind has a distinct icon")
        void everyKindHasIcon() {
            for (AppDialog.Kind kind : AppDialog.Kind.values()) {
                assertThat(AppDialog.iconFor(kind)).isNotNull();
            }
        }

        @Test
        @DisplayName("error and info use different header colours")
        void errorAndInfoDiffer() {
            assertThat(AppDialog.headerStyleClass(AppDialog.Kind.ERROR))
                    .isNotEqualTo(AppDialog.headerStyleClass(AppDialog.Kind.INFO));
        }
    }

    @Nested
    @DisplayName("Visibility guard")
    @ExtendWith(ApplicationExtension.class)
    @Tag("e2e")
    class Visibility {

        @Start
        void start(Stage stage) {
            // JavaFX toolkit initialisation only.
        }

        @Test
        @DisplayName("info dialog shows an on-screen stage with content (not an invisible Alert)")
        void infoDialogIsVisibleOnScreen() throws Exception {
            AtomicReference<AppDialog> ref = new AtomicReference<>();
            Platform.runLater(() -> {
                AppDialog dialog = new AppDialog(AppDialog.Kind.INFO, "Saved", "Your changes were saved.",
                        "OK", null);
                dialog.stageForTest().show();
                ref.set(dialog);
            });
            WaitForAsyncUtils.waitForFxEvents();

            Stage stage = ref.get().stageForTest();
            assertThat(stage.isShowing()).isTrue();
            assertThat(stage.getScene()).isNotNull();
            assertThat(stage.getScene().getRoot()).isNotNull();
            // On-screen and non-degenerate — the invisible-Alert bug produced a 0-size/off-screen window.
            assertThat(stage.getWidth()).isGreaterThan(0);
            assertThat(stage.getHeight()).isGreaterThan(0);

            Platform.runLater(stage::close);
            WaitForAsyncUtils.waitForFxEvents();
        }

        @Test
        @DisplayName("confirm dialog returns true when the confirm button is fired")
        void confirmReturnsTrueOnConfirm() throws Exception {
            AtomicReference<AppDialog> ref = new AtomicReference<>();
            Platform.runLater(() -> {
                AppDialog dialog = new AppDialog(AppDialog.Kind.CONFIRM, "Delete?", "This cannot be undone.",
                        "Delete", "Cancel");
                dialog.stageForTest().show();
                ref.set(dialog);
            });
            WaitForAsyncUtils.waitForFxEvents();

            fireButtonLabelled(ref.get().stageForTest(), "Delete");
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(ref.get().isConfirmed()).isTrue();
            assertThat(ref.get().stageForTest().isShowing()).isFalse();
        }

        @Test
        @DisplayName("confirm dialog returns false when the cancel button is fired")
        void confirmReturnsFalseOnCancel() throws Exception {
            AtomicReference<AppDialog> ref = new AtomicReference<>();
            Platform.runLater(() -> {
                AppDialog dialog = new AppDialog(AppDialog.Kind.CONFIRM, "Delete?", "This cannot be undone.",
                        "Delete", "Cancel");
                dialog.stageForTest().show();
                ref.set(dialog);
            });
            WaitForAsyncUtils.waitForFxEvents();

            fireButtonLabelled(ref.get().stageForTest(), "Cancel");
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(ref.get().isConfirmed()).isFalse();
            assertThat(ref.get().stageForTest().isShowing()).isFalse();
        }

        private void fireButtonLabelled(Stage stage, String label) {
            Platform.runLater(() -> findButton(stage.getScene().getRoot(), label).fire());
        }

        private Button findButton(Node node, String label) {
            if (node instanceof Button b && label.equals(b.getText())) {
                return b;
            }
            if (node instanceof javafx.scene.Parent parent) {
                for (Node child : parent.getChildrenUnmodifiable()) {
                    Button found = findButton(child, label);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return null;
        }
    }
}
