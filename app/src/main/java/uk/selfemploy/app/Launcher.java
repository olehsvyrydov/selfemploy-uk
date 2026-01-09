package uk.selfemploy.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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

        // Load CSS stylesheet
        scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());

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
        launch(args);
    }
}
