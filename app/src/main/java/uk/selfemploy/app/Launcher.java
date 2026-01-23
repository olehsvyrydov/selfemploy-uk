package uk.selfemploy.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import uk.selfemploy.common.util.EnvLoader;

/**
 * Main application launcher for UK Self-Employment Manager.
 *
 * This class serves as the entry point for the JavaFX application.
 * It initializes the Quarkus container and launches the UI.
 */
public class Launcher extends Application {

    private static final String APP_TITLE = "UK Self-Employment Manager";
    private static final int DEFAULT_WIDTH = 1280;
    private static final int DEFAULT_HEIGHT = 720;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the main FXML layout
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();

        // Create the scene
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);

        // Load all CSS stylesheets (loaded here to avoid JPMS issues with FXML)
        String[] stylesheets = {
            "/css/main.css",
            "/css/tax-summary.css",
            "/css/legal.css",
            "/css/annual-submission.css",
            "/css/submission-history.css",
            "/css/onboarding.css",
            "/css/bank-import.css",
            "/css/notifications.css",
            "/css/receipt-attachment.css",
            "/css/help.css"
        };
        for (String css : stylesheets) {
            var resource = getClass().getResource(css);
            if (resource != null) {
                scene.getStylesheets().add(resource.toExternalForm());
            }
        }

        // Configure the stage
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        // Cleanup resources when application closes
        super.stop();
    }

    /**
     * Main entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // Load .env file before starting the application
        EnvLoader.loadFromDefaultLocations();
        launch(args);
    }
}
