package uk.selfemploy.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.i18n.Messages;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads every app-lock screen through the real {@link FXMLLoader}. The view-model tests cover the
 * decision logic but never touch the FXML, so a mistyped handler or a renamed field is invisible to
 * them and surfaces as a runtime failure on a screen that stands between the user and their data.
 *
 * <p>What this proves, verified by deliberately breaking each case: an {@code onAction} naming a method
 * the controller does not have fails the load, and so does an {@code fx:id} whose field is dereferenced
 * during {@code initialize()}. An unused {@code fx:id} that no longer matches a field is <em>not</em>
 * caught — FXMLLoader leaves such a field null and the failure appears when the control is first used.
 *
 * <p>Tagged {@code e2e} because it boots the JavaFX toolkit, which is process-global; the default test
 * fork excludes that tag. Run with {@code -DexcludedGroups=} to include it.
 */
@Tag("e2e")
@DisplayName("App-lock FXML wiring")
class AppLockFxmlWiringTest {

    private static boolean fxReady;
    private static String skipReason = "";

    @BeforeAll
    static void initToolkit() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            fxReady = latch.await(15, TimeUnit.SECONDS);
            if (!fxReady) {
                skipReason = "JavaFX toolkit did not start within 15s";
            }
        } catch (IllegalStateException alreadyStarted) {
            fxReady = true;
        } catch (Exception noDisplay) {
            fxReady = false;
            skipReason = "JavaFX toolkit unavailable: " + noDisplay;
        }
        if (fxReady) {
            Platform.setImplicitExit(false);
        }
        Assumptions.assumeTrue(fxReady, "Skipping FXML wiring check — " + skipReason);
    }

    @Test
    @DisplayName("every app-lock screen loads and yields its controller")
    void allAppLockScreensLoad() throws Exception {
        assertThat(loadOnFxThread("/fxml/app-unlock.fxml")).isInstanceOf(AppUnlockController.class);
        assertThat(loadOnFxThread("/fxml/app-protect.fxml")).isInstanceOf(AppProtectController.class);
        assertThat(loadOnFxThread("/fxml/change-passphrase.fxml")).isInstanceOf(ChangePassphraseController.class);
        assertThat(loadOnFxThread("/fxml/regenerate-recovery.fxml")).isInstanceOf(RegenerateRecoveryController.class);
    }

    @Test
    @DisplayName("the shell's lock action resolves against MainController")
    void headerLockActionResolves() throws Exception {
        // main.fxml carries the manual lock button; a renamed handler would only show up here.
        assertThat(loadOnFxThread("/fxml/main.fxml")).isInstanceOf(MainController.class);
    }

    /** Loads an FXML on the FX thread and returns its controller, rethrowing whatever the loader threw. */
    private Object loadOnFxThread(String fxml) throws Exception {
        AtomicReference<Object> controller = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = Messages.loader(getClass().getResource(fxml));
                loader.load();
                controller.set(loader.getController());
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });

        assertThat(done.await(20, TimeUnit.SECONDS)).as("loading %s timed out", fxml).isTrue();
        if (failure.get() != null) {
            throw new AssertionError("Failed to load " + fxml, failure.get());
        }
        return controller.get();
    }
}
