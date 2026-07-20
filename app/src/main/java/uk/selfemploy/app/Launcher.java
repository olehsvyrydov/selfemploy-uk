package uk.selfemploy.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;
import uk.selfemploy.common.util.EnvLoader;
import uk.selfemploy.ui.controller.MainController;
import uk.selfemploy.ui.i18n.Messages;
import uk.selfemploy.ui.controller.OnboardingController;
import uk.selfemploy.ui.controller.SettingsController;
import uk.selfemploy.ui.controller.TermsOfServiceController;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.service.OnboardingSetupService;
import uk.selfemploy.ui.util.DialogBounds;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application launcher for UK Self-Employment Manager.
 *
 * This class serves as the entry point for the JavaFX application. The packaged app
 * starts it through {@link Main}, whose class does not extend {@code Application} and so
 * passes the java launcher's class-path check.
 */
public class Launcher extends Application {

    private static final Logger LOG = Logger.getLogger(Launcher.class.getName());
    private static final String APP_TITLE = "UK Self-Employment Manager";
    private static final int DEFAULT_WIDTH = 1280;
    private static final int DEFAULT_HEIGHT = 720;

    @Override
    public void start(Stage primaryStage) throws Exception {
        List<String> stylesheets = stylesheetUrls();

        // Data-protection gate: unlock the encrypted database before any database access. Fails closed
        // (a locked, un-unlocked app exits rather than opening against a database it cannot read).
        if (!requireUnlock(stylesheets)) {
            return; // locked out — the app is exiting
        }

        // Apply stored HMRC settings (environment URLs and credentials) before UI loads
        SettingsController.loadAndApplyStoredEnvironment();
        SettingsController.loadAndApplyStoredCredentials();

        // Load the main FXML layout (with the message bundle so FXML can use %key text).
        FXMLLoader loader = Messages.loader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        MainController mainController = loader.getController();

        // Create the scene
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        scene.getStylesheets().addAll(stylesheets);

        // Configure the stage
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();

        // First launch: require terms acceptance, then gather the user's details, before use.
        if (!requireTermsAcceptance(primaryStage, scene.getStylesheets())) {
            return; // terms declined — the app is exiting
        }
        boolean firstRun = maybeRunFirstRunOnboarding(primaryStage, scene.getStylesheets());
        if (firstRun) {
            // Offer to protect the data with a passphrase right after the details are gathered.
            maybeOfferProtection(primaryStage, scene.getStylesheets());
            if (mainController != null) {
                // Introduce the app with the guided tour, once the main window is laid out.
                Platform.runLater(mainController::startTour);
            }
        }
    }

    /** The app's stylesheet URLs (resolved once, reused by the main scene and the modal gates). */
    private List<String> stylesheetUrls() {
        String[] paths = {
            "/css/main.css", "/css/tax-summary.css", "/css/legal.css", "/css/annual-submission.css",
            "/css/submission-history.css", "/css/onboarding.css", "/css/bank-import.css",
            "/css/notifications.css", "/css/receipt-attachment.css", "/css/help.css"
        };
        List<String> urls = new java.util.ArrayList<>();
        for (String css : paths) {
            var resource = getClass().getResource(css);
            if (resource != null) {
                urls.add(resource.toExternalForm());
            }
        }
        return urls;
    }

    /**
     * Shows the unlock screen when the database is passphrase-protected, provisioning the database key
     * on success. Also runs the one-time encryption if protection was enabled but the database is still
     * plaintext (deferred migration, done here with no open connections). Fails closed: any failure, or
     * closing the screen without unlocking, exits the app rather than opening unprotected.
     *
     * @return true to continue starting the app, false if it is exiting
     */
    private boolean requireUnlock(List<String> stylesheets) {
        java.nio.file.Path dbPath = uk.selfemploy.ui.service.SqliteDataStore.databaseFilePath();
        uk.selfemploy.ui.service.security.DatabaseMigrator.restoreFromBackupIfInterrupted(dbPath);
        uk.selfemploy.ui.service.security.AppLockService appLock =
                new uk.selfemploy.ui.service.security.AppLockService();
        if (!appLock.isProtectionEnabled()) {
            return true; // no passphrase set — the database is plaintext, unchanged behaviour
        }
        try {
            FXMLLoader loader = Messages.loader(getClass().getResource("/fxml/app-unlock.fxml"));
            Parent root = loader.load();
            uk.selfemploy.ui.controller.AppUnlockController controller = loader.getController();
            controller.setAppLockService(appLock);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(APP_TITLE);
            Scene dialogScene = new Scene(root);
            dialogScene.getStylesheets().addAll(stylesheets);
            dialog.setScene(dialogScene);
            controller.setDialogStage(dialog);
            dialog.showAndWait();

            uk.selfemploy.ui.service.security.DbKey key = controller.getUnlockedKey();
            if (key == null) {
                LOG.info("Unlock cancelled; exiting");
                Platform.exit();
                return false;
            }
            if (uk.selfemploy.ui.service.security.DatabaseMigrator.databaseIsPlaintext(dbPath)) {
                uk.selfemploy.ui.service.security.DatabaseMigrator.encrypt(dbPath, key);
            }
            uk.selfemploy.ui.service.SqliteDataStore.provisionKey(key);
            uk.selfemploy.ui.service.security.DatabaseMigrator.deleteBackup(dbPath);
            return true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "App-lock gate failed; exiting to avoid opening unprotected", e);
            Platform.exit();
            return false;
        }
    }

    /**
     * Offers the optional "protect your data" step after first-run onboarding. Enabling writes the key
     * vault; the database is encrypted on the next launch (see {@link #requireUnlock}). A failure to show
     * it is swallowed — an optional step never blocks the app.
     */
    private void maybeOfferProtection(Stage owner, List<String> stylesheets) {
        uk.selfemploy.ui.service.security.AppLockService appLock =
                new uk.selfemploy.ui.service.security.AppLockService();
        if (appLock.isProtectionEnabled()) {
            return;
        }
        try {
            FXMLLoader loader = Messages.loader(getClass().getResource("/fxml/app-protect.fxml"));
            Parent root = loader.load();
            uk.selfemploy.ui.controller.AppProtectController controller = loader.getController();
            controller.setAppLockService(appLock);

            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(APP_TITLE);
            Scene dialogScene = new Scene(root);
            dialogScene.getStylesheets().addAll(stylesheets);
            dialog.setScene(dialogScene);
            controller.setDialogStage(dialog);
            fitDialogToScreen(dialog);
            dialog.showAndWait();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not show the data-protection step; continuing", e);
        }
    }

    /**
     * Shows the Terms of Service modally on first launch (or after a version change) and blocks the
     * app until the user accepts. Declining exits the app (handled inside the dialog). Returns true
     * if the terms are satisfied (accepted or not required), false if they must still be accepted
     * (i.e. the user declined and the app is shutting down).
     */
    private boolean requireTermsAcceptance(Stage owner, List<String> stylesheets) {
        try {
            FXMLLoader loader = Messages.loader(getClass().getResource("/fxml/terms-of-service.fxml"));
            Parent root = loader.load();
            TermsOfServiceController controller = loader.getController();
            // The controller builds its view model only when given the acceptance service; without
            // this, requiresAcceptance() is always false and the gate would never fire.
            controller.initializeWithDependencies(CoreServiceFactory.getTermsAcceptanceService());
            controller.setSettingsMode(false); // first-launch mode: Accept/Decline shown

            if (!controller.requiresAcceptance()) {
                return true; // current version already accepted
            }

            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Terms of Service");
            Scene dialogScene = new Scene(root);
            dialogScene.getStylesheets().addAll(stylesheets);
            dialog.setScene(dialogScene);
            fitDialogToScreen(dialog);
            controller.setDialogStage(dialog);
            dialog.showAndWait();

            // Accepted → requiresAcceptance() is now false; declined → Platform.exit() already called.
            return !controller.requiresAcceptance();
        } catch (Exception e) {
            // A compliance gate must fail closed: if it cannot verify acceptance, do not let the user
            // into the app. Exit rather than continuing unprotected.
            LOG.log(Level.SEVERE, "Failed to show the terms-of-service gate; exiting to avoid bypassing it", e);
            Platform.exit();
            return false;
        }
    }

    /**
     * Shows the onboarding wizard modally on first launch. Completing or skipping it records the
     * result (via {@link OnboardingSetupService}) so it never appears again. Any failure to show it
     * is swallowed and onboarding is marked complete, so a wizard problem never blocks the app.
     */
    private boolean maybeRunFirstRunOnboarding(Stage owner, List<String> stylesheets) {
        OnboardingSetupService setup = new OnboardingSetupService();
        if (!setup.isRequired()) {
            return false;
        }
        try {
            FXMLLoader loader = Messages.loader(getClass().getResource("/fxml/onboarding-wizard.fxml"));
            Parent root = loader.load();
            OnboardingController controller = loader.getController();

            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Welcome to " + APP_TITLE);
            Scene dialogScene = new Scene(root);
            dialogScene.getStylesheets().addAll(stylesheets);
            dialog.setScene(dialogScene);
            fitDialogToScreen(dialog);

            controller.setDialogStage(dialog);
            controller.setOnCompleteCallback(setup::complete);
            dialog.showAndWait();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to show first-run onboarding; continuing to the app", e);
            setup.complete(null);
        }
        return true;
    }

    /**
     * Bounds a modal dialog to the visible screen and centres it, so a tall dialog (such as the
     * Terms of Service) never pushes its footer buttons off the bottom edge on smaller displays.
     */
    private static void fitDialogToScreen(Stage dialog) {
        Rectangle2D visual = DialogBounds.visualBoundsForOwner(dialog.getOwner());
        dialog.setResizable(true);
        dialog.setWidth(Math.min(1000, visual.getWidth() - 40));
        dialog.setHeight(Math.min(920, visual.getHeight() - 60));
        dialog.centerOnScreen();
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
