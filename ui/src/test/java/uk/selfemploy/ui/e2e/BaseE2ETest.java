package uk.selfemploy.ui.e2e;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeoutException;

/**
 * Base class for E2E tests using TestFX.
 * Provides common setup, teardown, and utility methods.
 *
 * <p>E2E tests require a display. In headless CI environments,
 * set system property {@code skipE2ETests=true} to skip these tests.</p>
 */
@DisabledIfSystemProperty(named = "skipE2ETests", matches = "true")
public abstract class BaseE2ETest extends ApplicationTest {

    protected Stage primaryStage;

    @BeforeAll
    static void setupHeadlessMode() {
        // Configure TestFX for headless mode if requested
        if (Boolean.getBoolean("headless") || Boolean.getBoolean("testfx.headless")) {
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
            System.setProperty("java.awt.headless", "true");
            System.setProperty("glass.platform", "Monocle");
            System.setProperty("monocle.platform", "Headless");
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());

        stage.setTitle("UK Self-Employment Manager");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();

        // Wait for the stage to be fully shown
        WaitForAsyncUtils.waitForFxEvents();
    }

    @AfterEach
    void tearDown() throws TimeoutException {
        // Release all keys and mouse buttons
        release(new KeyCode[]{});
        release(new MouseButton[]{});

        // Close any open dialogs/stages
        FxToolkit.hideStage();
    }

    /**
     * Waits for JavaFX events to complete.
     */
    protected void waitForFxEvents() {
        WaitForAsyncUtils.waitForFxEvents();
    }

    /**
     * Sleeps for a short period to allow UI updates.
     */
    protected void shortSleep() {
        sleep(100);
    }

    /**
     * Gets the primary stage.
     */
    protected Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Gets the current scene.
     */
    protected Scene getCurrentScene() {
        return primaryStage.getScene();
    }

    /**
     * Verifies that a node with the given selector exists and is visible.
     */
    protected boolean nodeExists(String selector) {
        try {
            return lookup(selector).tryQuery().isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}
