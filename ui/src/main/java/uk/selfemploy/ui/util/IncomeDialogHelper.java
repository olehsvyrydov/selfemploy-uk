package uk.selfemploy.ui.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeStatus;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.controller.IncomeDialogController;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Utility class for opening the Income Dialog consistently across the application.
 * Ensures the same dialog configuration is used whether opened from Dashboard, Income page, or elsewhere.
 */
public final class IncomeDialogHelper {

    private static final Logger LOG = LoggerFactory.getLogger(IncomeDialogHelper.class);

    private IncomeDialogHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Opens the income dialog in "Add" mode.
     *
     * @param owner         The owner window (for modal positioning)
     * @param incomeService The income service
     * @param businessId    The business ID
     * @param taxYear       The current tax year
     * @param onSave        Callback when income is saved (can be null)
     * @return true if dialog was opened successfully, false otherwise
     */
    public static boolean openAddDialog(
            Window owner,
            IncomeService incomeService,
            UUID businessId,
            TaxYear taxYear,
            Consumer<Income> onSave) {
        return openDialog(owner, incomeService, businessId, taxYear, null, null, null, onSave, null);
    }

    /**
     * Opens the income dialog in "Edit" mode.
     *
     * @param owner         The owner window (for modal positioning)
     * @param incomeService The income service
     * @param businessId    The business ID
     * @param taxYear       The current tax year
     * @param income        The income to edit
     * @param clientName    The client name (for display)
     * @param status        The income status
     * @param onSave        Callback when income is saved (can be null)
     * @param onDelete      Callback when income is deleted (can be null)
     * @return true if dialog was opened successfully, false otherwise
     */
    public static boolean openEditDialog(
            Window owner,
            IncomeService incomeService,
            UUID businessId,
            TaxYear taxYear,
            Income income,
            String clientName,
            IncomeStatus status,
            Consumer<Income> onSave,
            Runnable onDelete) {
        return openDialog(owner, incomeService, businessId, taxYear, income, clientName, status, onSave, onDelete);
    }

    /**
     * Opens the income dialog with full configuration options.
     *
     * @param owner         The owner window (for modal positioning)
     * @param incomeService The income service
     * @param businessId    The business ID
     * @param taxYear       The current tax year
     * @param income        The income to edit (null for add mode)
     * @param clientName    The client name (for edit mode)
     * @param status        The income status (for edit mode)
     * @param onSave        Callback when income is saved (can be null)
     * @param onDelete      Callback when income is deleted (can be null)
     * @return true if dialog was opened successfully, false otherwise
     */
    public static boolean openDialog(
            Window owner,
            IncomeService incomeService,
            UUID businessId,
            TaxYear taxYear,
            Income income,
            String clientName,
            IncomeStatus status,
            Consumer<Income> onSave,
            Runnable onDelete) {

        // Validate required parameters
        if (incomeService == null) {
            LOG.error("Cannot open income dialog: incomeService is null");
            return false;
        }
        if (businessId == null) {
            LOG.error("Cannot open income dialog: businessId is null");
            return false;
        }
        if (taxYear == null) {
            LOG.error("Cannot open income dialog: taxYear is null");
            return false;
        }

        try {
            // Load FXML
            FXMLLoader loader = new FXMLLoader(
                    IncomeDialogHelper.class.getResource("/fxml/income-dialog.fxml"));
            VBox dialogContent = loader.load();

            // Configure controller
            IncomeDialogController dialogController = loader.getController();
            dialogController.initializeWithDependencies(incomeService, businessId, taxYear);

            // Set mode (add or edit)
            if (income != null) {
                dialogController.setEditMode(income, clientName, status);
            }

            // Create dialog stage
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.setTitle(income != null ? "Edit Income" : "Add Income");

            if (owner != null) {
                dialogStage.initOwner(owner);
                // Center dialog on owner window's screen (multi-monitor fix)
                dialogStage.setOnShown(event -> {
                    double centerX = owner.getX() + (owner.getWidth() - dialogStage.getWidth()) / 2;
                    double centerY = owner.getY() + (owner.getHeight() - dialogStage.getHeight()) / 2;
                    dialogStage.setX(centerX);
                    dialogStage.setY(centerY);
                });
            }

            // Create scene with required stylesheets
            Scene scene = new Scene(dialogContent);
            scene.getStylesheets().add(
                    IncomeDialogHelper.class.getResource("/css/main.css").toExternalForm());
            dialogStage.setScene(scene);

            // Set callbacks
            dialogController.setOnSaveCallback(savedIncome -> {
                dialogStage.close();
                if (onSave != null) {
                    onSave.accept(savedIncome);
                }
            });

            dialogController.setOnDeleteCallback(() -> {
                dialogStage.close();
                if (onDelete != null) {
                    onDelete.run();
                }
            });

            dialogController.setDialogStage(dialogStage);

            // Show dialog and wait
            dialogStage.showAndWait();

            return true;

        } catch (IOException e) {
            LOG.error("Failed to open income dialog", e);
            return false;
        }
    }
}
