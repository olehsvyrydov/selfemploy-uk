package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.component.NotificationDialog;
import uk.selfemploy.ui.service.DeadlineNotificationService;
import uk.selfemploy.ui.viewmodel.NavigationViewModel;
import uk.selfemploy.ui.viewmodel.View;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Main controller for the application's root layout.
 * Manages navigation, tax year selection, and content view switching.
 */
public class MainController implements Initializable {

    private static final Logger LOG = Logger.getLogger(MainController.class.getName());

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

    // Notification Bell (SE-309)
    @FXML private StackPane notificationBell;
    @FXML private Button notificationButton;
    @FXML private Label notificationBadge;

    private final NavigationViewModel navigationViewModel = new NavigationViewModel();
    private final Map<View, Node> viewCache = new HashMap<>();
    private final Map<View, Object> controllerCache = new HashMap<>();
    private final DeadlineNotificationService notificationService = new DeadlineNotificationService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTaxYearSelector();
        setupNavigationBindings();
        setupNotifications();

        // Load dashboard by default
        loadView(View.DASHBOARD);
    }

    private void setupNotifications() {
        // Bind badge visibility to unread count
        notificationService.unreadCountProperty().addListener((obs, oldVal, newVal) -> {
            int count = newVal.intValue();
            notificationBadge.setText(count > 9 ? "9+" : String.valueOf(count));
            notificationBadge.setVisible(count > 0);
            notificationBadge.setManaged(count > 0);
        });

        // Start notification scheduler for current tax year
        TaxYear currentYear = navigationViewModel.getSelectedTaxYear();
        if (currentYear != null) {
            notificationService.startScheduler(currentYear);
        }

        LOG.info("Notification service initialized");
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
                // Restart notification scheduler for new tax year
                notificationService.startScheduler(newVal);
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
        Object controller = controllerCache.get(view);

        if (viewNode == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(view.getFxmlPath()));
                viewNode = loader.load();

                // Get and cache controller
                controller = loader.getController();
                controllerCache.put(view, controller);

                // Pass tax year to controller if it supports it
                if (controller instanceof TaxYearAware taxYearAware) {
                    taxYearAware.setTaxYear(navigationViewModel.getSelectedTaxYear());
                }

                // Set navigation callbacks for DashboardController (SE-BUG: View Tax Breakdown button)
                if (controller instanceof DashboardController dashboardController) {
                    dashboardController.setNavigationCallbacks(
                        () -> loadView(View.INCOME),
                        () -> loadView(View.EXPENSES),
                        () -> loadView(View.TAX_SUMMARY)
                    );
                }

                viewCache.put(view, viewNode);
            } catch (IOException e) {
                showError("Failed to load view: " + view.getTitle(), e);
                return;
            }
        } else {
            // Refresh data when loading from cache
            if (controller instanceof Refreshable refreshable) {
                refreshable.refreshData();
            }
        }

        contentPane.getChildren().setAll(viewNode);
    }

    private void refreshCurrentView() {
        View currentView = navigationViewModel.getCurrentView();
        viewCache.remove(currentView);
        controllerCache.remove(currentView);
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

    @FXML
    void handleNotificationsAction(ActionEvent event) {
        showNotificationPanel();
    }

    @FXML
    void handleNotificationsClick(MouseEvent event) {
        showNotificationPanel();
    }

    private void showNotificationPanel() {
        var notifications = notificationService.getNotificationHistory();
        TaxYear currentYear = navigationViewModel.getSelectedTaxYear();

        // Create and show custom notification dialog with /aura's design
        NotificationDialog dialog = new NotificationDialog(
            notifications,
            currentYear,
            () -> notificationService.markAllAsRead()
        );

        dialog.showDialog();
        LOG.info("Notification dialog shown, " + notifications.size() + " notifications");
    }

    /**
     * Returns the currently selected tax year.
     */
    public TaxYear getSelectedTaxYear() {
        return navigationViewModel.getSelectedTaxYear();
    }

    /**
     * Returns the notification service for testing and external access.
     */
    public DeadlineNotificationService getNotificationService() {
        return notificationService;
    }

    /**
     * Shuts down the notification scheduler.
     * Should be called when the application is closing.
     */
    public void shutdown() {
        notificationService.shutdown();
        LOG.info("MainController shutdown complete");
    }

    /**
     * Interface for controllers that need to know the current tax year.
     */
    public interface TaxYearAware {
        void setTaxYear(TaxYear taxYear);
    }
}
