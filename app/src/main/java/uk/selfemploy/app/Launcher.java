package uk.selfemploy.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import uk.selfemploy.common.util.EnvLoader;
import uk.selfemploy.ui.controller.OnboardingController;
import uk.selfemploy.ui.controller.SettingsController;
import uk.selfemploy.ui.service.OnboardingSetupService;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application launcher for UK Self-Employment Manager.
 *
 * This class serves as the entry point for the JavaFX application.
 * It initializes the Quarkus container and launches the UI.
 */
public class Launcher extends Application {

    private static final Logger LOG = Logger.getLogger(Launcher.class.getName());
    private static final String APP_TITLE = "UK Self-Employment Manager";
    private static final int DEFAULT_WIDTH = 1280;
    private static final int DEFAULT_HEIGHT = 720;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Apply stored HMRC settings (environment URLs and credentials) before UI loads
        SettingsController.loadAndApplyStoredEnvironment();
        SettingsController.loadAndApplyStoredCredentials();

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

        // First launch: gather the user's details before they use the app.
        maybeRunFirstRunOnboarding(primaryStage, scene.getStylesheets());
    }

    /**
     * Shows the onboarding wizard modally on first launch. Completing or skipping it records the
     * result (via {@link OnboardingSetupService}) so it never appears again. Any failure to show it
     * is swallowed and onboarding is marked complete, so a wizard problem never blocks the app.
     */
    private void maybeRunFirstRunOnboarding(Stage owner, List<String> stylesheets) {
        OnboardingSetupService setup = new OnboardingSetupService();
        if (!setup.isRequired()) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/onboarding-wizard.fxml"));
            Parent root = loader.load();
            OnboardingController controller = loader.getController();

            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Welcome to " + APP_TITLE);
            Scene dialogScene = new Scene(root);
            dialogScene.getStylesheets().addAll(stylesheets);
            dialog.setScene(dialogScene);

            controller.setDialogStage(dialog);
            controller.setOnCompleteCallback(setup::complete);
            dialog.showAndWait();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to show first-run onboarding; continuing to the app", e);
            setup.complete(null);
        }
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
