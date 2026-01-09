package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.viewmodel.NavigationViewModel;
import uk.selfemploy.ui.viewmodel.View;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Main controller for the application's root layout.
 * Manages navigation, tax year selection, and content view switching.
 */
public class MainController implements Initializable {

    @FXML private StackPane contentPane;
    @FXML private ToggleGroup navGroup;
    @FXML private ToggleButton navDashboard;
    @FXML private ToggleButton navIncome;
    @FXML private ToggleButton navExpenses;
    @FXML private ToggleButton navTax;
    @FXML private ToggleButton navHmrc;
    @FXML private ComboBox<TaxYear> taxYearSelector;
    @FXML private Label taxYearLabel;
    @FXML private Label deadlineLabel;

    private final NavigationViewModel navigationViewModel = new NavigationViewModel();
    private final Map<View, Node> viewCache = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTaxYearSelector();
        setupNavigationBindings();

        // Load dashboard by default
        loadView(View.DASHBOARD);
    }

    private void setupTaxYearSelector() {
        taxYearSelector.getItems().addAll(navigationViewModel.getAvailableTaxYears());
        taxYearSelector.setValue(navigationViewModel.getSelectedTaxYear());

        taxYearSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(TaxYear year) {
                return year != null ? year.label() : "";
            }

            @Override
            public TaxYear fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });

        taxYearSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                navigationViewModel.setSelectedTaxYear(newVal);
                updateStatusBar();
                refreshCurrentView();
            }
        });

        updateStatusBar();
    }

    private void setupNavigationBindings() {
        // Bind navigation state to toggle buttons
        navigationViewModel.currentViewProperty().addListener((obs, oldView, newView) -> {
            selectNavButton(newView);
        });
    }

    private void selectNavButton(View view) {
        switch (view) {
            case DASHBOARD -> navDashboard.setSelected(true);
            case INCOME -> navIncome.setSelected(true);
            case EXPENSES -> navExpenses.setSelected(true);
            case TAX_SUMMARY -> navTax.setSelected(true);
            case HMRC_SUBMISSION -> navHmrc.setSelected(true);
            default -> {} // Help and Settings don't have nav buttons
        }
    }

    private void updateStatusBar() {
        TaxYear year = navigationViewModel.getSelectedTaxYear();
        if (year != null) {
            taxYearLabel.setText("Tax Year " + year.label());

            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), year.onlineFilingDeadline());
            if (daysUntil > 0) {
                deadlineLabel.setText(daysUntil + " days until filing deadline");
            } else if (daysUntil == 0) {
                deadlineLabel.setText("Filing deadline is TODAY!");
            } else {
                deadlineLabel.setText("Filing deadline has passed");
            }
        }
    }

    private void loadView(View view) {
        navigationViewModel.navigateTo(view);

        Node viewNode = viewCache.get(view);
        if (viewNode == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(view.getFxmlPath()));
                viewNode = loader.load();

                // Pass tax year to controller if it supports it
                Object controller = loader.getController();
                if (controller instanceof TaxYearAware taxYearAware) {
                    taxYearAware.setTaxYear(navigationViewModel.getSelectedTaxYear());
                }

                viewCache.put(view, viewNode);
            } catch (IOException e) {
                showError("Failed to load view: " + view.getTitle(), e);
                return;
            }
        }

        contentPane.getChildren().setAll(viewNode);
    }

    private void refreshCurrentView() {
        View currentView = navigationViewModel.getCurrentView();
        viewCache.remove(currentView);
        loadView(currentView);
    }

    private void showError(String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(message);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }

    // === Navigation Handlers ===

    @FXML
    void navigateToDashboard(ActionEvent event) {
        loadView(View.DASHBOARD);
    }

    @FXML
    void navigateToIncome(ActionEvent event) {
        loadView(View.INCOME);
    }

    @FXML
    void navigateToExpenses(ActionEvent event) {
        loadView(View.EXPENSES);
    }

    @FXML
    void navigateToTax(ActionEvent event) {
        loadView(View.TAX_SUMMARY);
    }

    @FXML
    void navigateToHmrc(ActionEvent event) {
        loadView(View.HMRC_SUBMISSION);
    }

    @FXML
    void handleSettings(ActionEvent event) {
        loadView(View.SETTINGS);
    }

    @FXML
    void handleHelp(ActionEvent event) {
        loadView(View.HELP);
    }

    /**
     * Returns the currently selected tax year.
     */
    public TaxYear getSelectedTaxYear() {
        return navigationViewModel.getSelectedTaxYear();
    }

    /**
     * Interface for controllers that need to know the current tax year.
     */
    public interface TaxYearAware {
        void setTaxYear(TaxYear taxYear);
    }
}
