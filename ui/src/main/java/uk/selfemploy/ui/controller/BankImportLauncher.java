package uk.selfemploy.ui.controller;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import uk.selfemploy.ui.component.AppDialog;
import uk.selfemploy.ui.util.DialogBounds;
import uk.selfemploy.ui.i18n.Messages;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Opens the bank-statement import wizard modally over an owner window. Shared so every entry point
 * (Income, Expenses, Bank Review and Import History) launches the same wizard the same way.
 */
public final class BankImportLauncher {

    private static final Logger LOG = Logger.getLogger(BankImportLauncher.class.getName());

    private BankImportLauncher() {
    }

    /**
     * Shows the import wizard and, if the user completed an import, invokes {@code onImported} with
     * the result message and the new batch id.
     */
    public static void launch(Window owner, BiConsumer<String, UUID> onImported) {
        try {
            FXMLLoader loader = Messages.loader(BankImportLauncher.class.getResource("/fxml/bank-import-wizard.fxml"));
            Parent root = loader.load();
            BankImportWizardController wizard = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Import Bank Statement");
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) {
                stage.initOwner(owner);
            }
            stage.setResizable(true);
            stage.setMinWidth(800);
            stage.setMinHeight(500);
            Rectangle2D visual = DialogBounds.visualBoundsForOwner(owner);
            stage.setX(visual.getMinX());
            stage.setY(visual.getMinY());
            stage.setWidth(visual.getWidth());
            stage.setHeight(visual.getHeight());

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                BankImportLauncher.class.getResource("/css/main.css").toExternalForm());
            scene.getStylesheets().add(
                BankImportLauncher.class.getResource("/css/bank-import.css").toExternalForm());
            stage.setScene(scene);
            wizard.setDialogStage(stage);
            stage.showAndWait();

            String resultMessage = wizard.getImportResultMessage();
            if (resultMessage != null && onImported != null) {
                onImported.accept(resultMessage, wizard.getImportResultBatchId());
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to open the Bank Import Wizard", e);
            AppDialog.error("Import Error", "Failed to open the Bank Import Wizard: " + e.getMessage());
        }
    }
}
