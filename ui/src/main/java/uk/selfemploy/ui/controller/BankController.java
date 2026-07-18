package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TabPane;
import uk.selfemploy.common.domain.TaxYear;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Hosts the single "Bank" section: an "Import statement" action plus three tabs — Review
 * Transactions, Imports and Records check — over the same bank data. The tabbed screens are
 * embedded via {@code fx:include}; MainController wires each embedded controller to its data,
 * and this controller forwards the selected tax year to the ones that need it.
 */
public class BankController implements Initializable, MainController.TaxYearAware {

    /** Tab indices, in the order declared in bank.fxml. */
    public static final int REVIEW_TAB = 0;
    public static final int IMPORTS_TAB = 1;
    public static final int RECORDS_CHECK_TAB = 2;

    @FXML private TabPane bankTabs;

    // Controllers of the embedded screens (injected by fx:include as <includeId>Controller).
    @FXML private TransactionReviewController reviewController;
    @FXML private ImportHistoryController importsController;
    @FXML private ReconciliationDashboardController recordsCheckController;

    private Runnable onImportStatement;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Wiring is done by MainController via the getters below.
    }

    public TransactionReviewController getReviewController() {
        return reviewController;
    }

    public ImportHistoryController getImportsController() {
        return importsController;
    }

    public ReconciliationDashboardController getRecordsCheckController() {
        return recordsCheckController;
    }

    /** Sets the action for the header "Import statement" button. */
    public void setOnImportStatement(Runnable onImportStatement) {
        this.onImportStatement = onImportStatement;
    }

    /** Selects a tab by index (see the *_TAB constants). */
    public void selectTab(int index) {
        bankTabs.getSelectionModel().select(index);
    }

    @FXML
    void handleImportStatement(ActionEvent event) {
        if (onImportStatement != null) {
            onImportStatement.run();
        }
    }

    @Override
    public void setTaxYear(TaxYear taxYear) {
        // Imports span tax years, so only the year-scoped tabs receive the change.
        if (reviewController != null) {
            reviewController.setTaxYear(taxYear);
        }
        if (recordsCheckController != null) {
            recordsCheckController.setTaxYear(taxYear);
        }
    }
}
