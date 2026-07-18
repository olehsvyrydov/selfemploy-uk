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

    /**
     * Registers a listener that notifies {@link #setOnTabSelected} whenever the selected tab
     * changes, so the newly-shown tab's data can be refreshed and never left stale.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bankTabs.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            if (onTabSelected != null) {
                onTabSelected.accept(newIndex.intValue());
            }
        });
    }

    /**
     * Sets the callback invoked with the tab index whenever the selected tab changes.
     *
     * @param onTabSelected the callback to run on tab selection
     */
    public void setOnTabSelected(IntConsumer onTabSelected) {
        this.onTabSelected = onTabSelected;
    }

    /**
     * Returns the index of the currently selected tab.
     *
     * @return the selected tab index (see the {@code *_TAB} constants)
     */
    public int getSelectedTab() {
        return bankTabs.getSelectionModel().getSelectedIndex();
    }

    /**
     * Returns the embedded Review Transactions controller, for wiring by the host.
     *
     * @return the review-tab controller
     */
    public TransactionReviewController getReviewController() {
        return reviewController;
    }

    /**
     * Returns the embedded Imports controller, for wiring by the host.
     *
     * @return the imports-tab controller
     */
    public ImportHistoryController getImportsController() {
        return importsController;
    }

    /**
     * Returns the embedded Records check controller, for wiring by the host.
     *
     * @return the records-check-tab controller
     */
    public ReconciliationDashboardController getRecordsCheckController() {
        return recordsCheckController;
    }

    /**
     * Sets the action for the header "Import statement" button.
     *
     * @param onImportStatement the action to run when the button is pressed
     */
    public void setOnImportStatement(Runnable onImportStatement) {
        this.onImportStatement = onImportStatement;
    }

    /**
     * Selects a tab by index.
     *
     * @param index the tab index (see the {@code *_TAB} constants)
     */
    public void selectTab(int index) {
        bankTabs.getSelectionModel().select(index);
    }

    @FXML
    void handleImportStatement(ActionEvent event) {
        if (onImportStatement != null) {
            onImportStatement.run();
        }
    }

    /**
     * Forwards the selected tax year to the year-scoped tabs (Review Transactions and Records check).
     * Imports span tax years, so the Imports tab is intentionally left unchanged.
     *
     * @param taxYear the newly selected tax year
     */
    @Override
    public void setTaxYear(TaxYear taxYear) {
        reviewController.setTaxYear(taxYear);
        recordsCheckController.setTaxYear(taxYear);
    }
}
