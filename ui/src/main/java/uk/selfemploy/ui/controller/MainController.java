package uk.selfemploy.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main controller for the application's root layout.
 *
 * This controller manages the main window, navigation, and content area.
 */
public class MainController implements Initializable {

    @FXML
    private VBox contentArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize the main controller
        // TODO: Load dashboard view by default
    }

    /**
     * Navigate to the Dashboard view.
     */
    @FXML
    public void showDashboard() {
        // TODO: Load dashboard FXML into content area
    }

    /**
     * Navigate to the Income view.
     */
    @FXML
    public void showIncome() {
        // TODO: Load income FXML into content area
    }

    /**
     * Navigate to the Expenses view.
     */
    @FXML
    public void showExpenses() {
        // TODO: Load expenses FXML into content area
    }

    /**
     * Navigate to the Tax Summary view.
     */
    @FXML
    public void showTaxSummary() {
        // TODO: Load tax summary FXML into content area
    }

    /**
     * Navigate to the HMRC Submission view.
     */
    @FXML
    public void showHmrcSubmission() {
        // TODO: Load HMRC submission FXML into content area
    }
}
