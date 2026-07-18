package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TabPane;
import uk.selfemploy.common.domain.TaxYear;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.IntConsumer;

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
    private IntConsumer onTabSelected;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Refresh a tab's data whenever it becomes selected, so an action taken on one tab (e.g.
        // undoing an import) can't leave stale rows showing on another.
        bankTabs.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            if (onTabSelected != null) {
                onTabSelected.accept(newIndex.intValue());
            }
        });
    }

    /** Sets the callback invoked (with the tab index) whenever the selected tab changes. */
    public void setOnTabSelected(IntConsumer onTabSelected) {
        this.onTabSelected = onTabSelected;
    }

    public int getSelectedTab() {
        return bankTabs.getSelectionModel().getSelectedIndex();
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
        reviewController.setTaxYear(taxYear);
        recordsCheckController.setTaxYear(taxYear);
    }
}
