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
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.ReceiptStorageService;
import uk.selfemploy.ui.controller.ExpenseDialogController;
import uk.selfemploy.ui.service.CoreServiceFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Utility class for opening the Expense Dialog consistently across the application.
 * Ensures the same dialog configuration is used whether opened from Dashboard, Expenses page, or elsewhere.
 */
public final class ExpenseDialogHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ExpenseDialogHelper.class);

    private ExpenseDialogHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Opens the expense dialog in "Add" mode.
     *
     * @param owner          The owner window (for modal positioning)
     * @param expenseService The expense service
     * @param businessId     The business ID
     * @param taxYear        The current tax year
     * @param onSave         Callback when expense is saved (can be null)
     * @return true if dialog was opened successfully, false otherwise
     */
    public static boolean openAddDialog(
            Window owner,
            ExpenseService expenseService,
            UUID businessId,
            TaxYear taxYear,
            Consumer<Expense> onSave) {
        return openDialog(owner, expenseService, businessId, taxYear, false, null, onSave, null);
    }

    /**
     * Opens the expense dialog in "Edit" mode.
     *
     * @param owner          The owner window (for modal positioning)
     * @param expenseService The expense service
     * @param businessId     The business ID
     * @param taxYear        The current tax year
     * @param expense        The expense to edit
     * @param onSave         Callback when expense is saved (can be null)
     * @param onDelete       Callback when expense is deleted (can be null)
     * @return true if dialog was opened successfully, false otherwise
     */
    public static boolean openEditDialog(
            Window owner,
            ExpenseService expenseService,
            UUID businessId,
            TaxYear taxYear,
            Expense expense,
            Consumer<Expense> onSave,
            Runnable onDelete) {
        return openDialog(owner, expenseService, businessId, taxYear, false, expense, onSave, onDelete);
    }

    /**
     * Opens the expense dialog with full configuration options.
     *
     * @param owner          The owner window (for modal positioning)
     * @param expenseService The expense service
     * @param businessId     The business ID
     * @param taxYear        The current tax year
     * @param cisBusiness    Whether this is a CIS business
     * @param expense        The expense to edit (null for add mode)
     * @param onSave         Callback when expense is saved (can be null)
     * @param onDelete       Callback when expense is deleted (can be null)
     * @return true if dialog was opened successfully, false otherwise
     */
    public static boolean openDialog(
            Window owner,
            ExpenseService expenseService,
            UUID businessId,
            TaxYear taxYear,
            boolean cisBusiness,
            Expense expense,
            Consumer<Expense> onSave,
            Runnable onDelete) {

        // Validate required parameters
        if (expenseService == null) {
            LOG.error("Cannot open expense dialog: expenseService is null");
            return false;
        }
        if (businessId == null) {
            LOG.error("Cannot open expense dialog: businessId is null");
            return false;
        }
        if (taxYear == null) {
            LOG.error("Cannot open expense dialog: taxYear is null");
            return false;
        }

        try {
            // Load FXML
            FXMLLoader loader = new FXMLLoader(
                    ExpenseDialogHelper.class.getResource("/fxml/expense-dialog.fxml"));
            VBox dialogContent = loader.load();

            // Configure controller
            ExpenseDialogController dialogController = loader.getController();
            dialogController.setExpenseService(expenseService);
            dialogController.setReceiptStorageService(getReceiptStorageService());
            dialogController.setBusinessId(businessId);
            dialogController.setTaxYear(taxYear);
            dialogController.setCisBusiness(cisBusiness);

            // Set mode (add or edit)
            if (expense != null) {
                dialogController.setEditMode(expense);
            } else {
                dialogController.setAddMode();
            }

            // Create dialog stage
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.setTitle(expense != null ? "Edit Expense" : "Add Expense");

            if (owner != null) {
                dialogStage.initOwner(owner);
            }

            // Create scene with all required stylesheets
            Scene scene = new Scene(dialogContent);
            scene.getStylesheets().add(
                    ExpenseDialogHelper.class.getResource("/css/main.css").toExternalForm());
            scene.getStylesheets().add(
                    ExpenseDialogHelper.class.getResource("/css/receipt-attachment.css").toExternalForm());
            dialogStage.setScene(scene);

            // Set callbacks
            dialogController.setOnSave(savedExpense -> {
                dialogStage.close();
                if (onSave != null) {
                    onSave.accept(savedExpense);
                }
            });

            dialogController.setOnDelete(() -> {
                dialogStage.close();
                if (onDelete != null) {
                    onDelete.run();
                }
            });

            dialogController.setOnClose(dialogStage::close);
            dialogController.setDialogStage(dialogStage);

            // Show dialog and wait
            dialogStage.showAndWait();

            return true;

        } catch (IOException e) {
            LOG.error("Failed to open expense dialog", e);
            return false;
        }
    }

    private static ReceiptStorageService getReceiptStorageService() {
        return CoreServiceFactory.getReceiptStorageService();
    }
}
