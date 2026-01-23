package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.viewmodel.DashboardViewModel;
import uk.selfemploy.ui.viewmodel.DashboardViewModel.ActivityItem;
import uk.selfemploy.ui.viewmodel.Deadline;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Controller for the Dashboard view.
 * Binds UI elements to DashboardViewModel and handles user actions.
 */
public class DashboardController implements Initializable, MainController.TaxYearAware, Refreshable {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy");

    // Metric cards
    @FXML private Label incomeValue;
    @FXML private Label incomeTrend;
    @FXML private Label expensesValue;
    @FXML private Label expensesTrend;
    @FXML private Label profitValue;
    @FXML private Label taxValue;

    // Tax year progress
    @FXML private Label taxYearBadge;
    @FXML private ProgressBar yearProgress;
    @FXML private Label yearStart;
    @FXML private Label daysRemaining;
    @FXML private Label yearEnd;

    // Deadlines and activity
    @FXML private VBox deadlinesList;
    @FXML private VBox activityList;
    @FXML private Label emptyActivityLabel;

    private final DashboardViewModel viewModel = new DashboardViewModel();

    // Service dependencies (SE-207, SE-208)
    private IncomeService incomeService;
    private ExpenseService expenseService;
    private UUID businessId;
    private TaxYear currentTaxYear;
    private Runnable onNavigateToIncome;
    private Runnable onNavigateToExpenses;
    private Runnable onNavigateToTax;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bindMetricCards();
        bindYearProgress();
        populateDeadlines();
        populateActivity();

        // Re-populate deadlines when tax year changes
        viewModel.currentTaxYearProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateTaxYearBadge();
                updateYearProgressLabels();
                populateDeadlines();
            }
        });
    }

    private void bindMetricCards() {
        // Bind metric values
        viewModel.totalIncomeProperty().addListener((obs, oldVal, newVal) ->
            incomeValue.setText(viewModel.getFormattedIncome()));
        viewModel.totalExpensesProperty().addListener((obs, oldVal, newVal) ->
            expensesValue.setText(viewModel.getFormattedExpenses()));
        viewModel.netProfitProperty().addListener((obs, oldVal, newVal) ->
            profitValue.setText(viewModel.getFormattedProfit()));
        viewModel.estimatedTaxProperty().addListener((obs, oldVal, newVal) ->
            taxValue.setText(viewModel.getFormattedTax()));

        // Bind trends
        viewModel.incomeThisMonthProperty().addListener((obs, oldVal, newVal) ->
            incomeTrend.setText(viewModel.getFormattedIncomeTrend()));
        viewModel.expensesThisMonthProperty().addListener((obs, oldVal, newVal) ->
            expensesTrend.setText(viewModel.getFormattedExpensesTrend()));

        // Set initial values
        incomeValue.setText(viewModel.getFormattedIncome());
        expensesValue.setText(viewModel.getFormattedExpenses());
        profitValue.setText(viewModel.getFormattedProfit());
        taxValue.setText(viewModel.getFormattedTax());
        incomeTrend.setText(viewModel.getFormattedIncomeTrend());
        expensesTrend.setText(viewModel.getFormattedExpensesTrend());
    }

    private void bindYearProgress() {
        // Bind progress bar
        yearProgress.progressProperty().bind(viewModel.yearProgressProperty());

        // Update progress text when days remaining changes
        viewModel.daysRemainingProperty().addListener((obs, oldVal, newVal) ->
            daysRemaining.setText(viewModel.getYearProgressText()));

        // Set initial values
        updateTaxYearBadge();
        updateYearProgressLabels();
        daysRemaining.setText(viewModel.getYearProgressText());
    }

    private void updateTaxYearBadge() {
        TaxYear year = viewModel.getCurrentTaxYear();
        if (year != null) {
            taxYearBadge.setText("Tax Year " + year.label());
        }
    }

    private void updateYearProgressLabels() {
        TaxYear year = viewModel.getCurrentTaxYear();
        if (year != null) {
            yearStart.setText("Started: " + year.startDate().format(DATE_FORMATTER));
            yearEnd.setText("Ends: " + year.endDate().format(DATE_FORMATTER));
        }
    }

    private void populateDeadlines() {
        deadlinesList.getChildren().clear();

        for (Deadline deadline : viewModel.getDeadlines()) {
            HBox deadlineItem = createDeadlineItem(deadline);
            deadlinesList.getChildren().add(deadlineItem);
        }
    }

    private HBox createDeadlineItem(Deadline deadline) {
        HBox item = new HBox(10);
        item.getStyleClass().add("deadline-item");

        // Status indicator
        Label status = new Label();
        status.getStyleClass().addAll("deadline-status", getStatusStyleClass(deadline));

        // Date
        Label date = new Label(deadline.date().format(DATE_FORMATTER));
        date.getStyleClass().add("deadline-date");

        // Label
        Label label = new Label(deadline.label());
        label.getStyleClass().add("deadline-label");

        item.getChildren().addAll(status, date, label);
        return item;
    }

    private String getStatusStyleClass(Deadline deadline) {
        return deadline.status().getStyleClass();
    }

    private void populateActivity() {
        // Skip if FXML elements not initialized (unit test scenario)
        if (activityList == null) return;

        activityList.getChildren().clear();

        if (viewModel.getRecentActivity().isEmpty()) {
            activityList.getChildren().add(emptyActivityLabel);
        } else {
            for (ActivityItem activity : viewModel.getRecentActivity()) {
                HBox activityItem = createActivityItem(activity);
                activityList.getChildren().add(activityItem);
            }
        }

        // Listen for changes to activity list
        viewModel.getRecentActivity().addListener((javafx.collections.ListChangeListener<ActivityItem>) change -> {
            activityList.getChildren().clear();
            if (viewModel.getRecentActivity().isEmpty()) {
                activityList.getChildren().add(emptyActivityLabel);
            } else {
                for (ActivityItem item : viewModel.getRecentActivity()) {
                    activityList.getChildren().add(createActivityItem(item));
                }
            }
        });
    }

    private HBox createActivityItem(ActivityItem activity) {
        HBox item = new HBox(10);
        item.getStyleClass().add("activity-item");

        // Date
        Label date = new Label(activity.getFormattedDate());
        date.getStyleClass().add("activity-date");

        // Description
        Label description = new Label(activity.description());
        description.getStyleClass().add("activity-description");

        // Amount
        Label amount = new Label(activity.getFormattedAmount());
        amount.getStyleClass().addAll("activity-amount",
            activity.isIncome() ? "amount-income" : "amount-expense");

        item.getChildren().addAll(date, description, amount);
        return item;
    }

    // === Service Initialization (SE-207) ===

    /**
     * Initializes the controller with required service dependencies.
     * This enables dashboard data integration with real backend services.
     *
     * @param incomeService  the income service for loading income data
     * @param expenseService the expense service for loading expense data
     * @param businessId     the current business ID
     */
    public void initializeWithDependencies(IncomeService incomeService, ExpenseService expenseService, UUID businessId) {
        this.incomeService = incomeService;
        this.expenseService = expenseService;
        this.businessId = businessId;

        // Load data if tax year is already set
        if (currentTaxYear != null) {
            loadDashboardData();
        }
    }

    /**
     * Sets navigation callbacks for quick actions.
     * These allow the dashboard to navigate to other views.
     *
     * @param onNavigateToIncome   callback to navigate to Income view
     * @param onNavigateToExpenses callback to navigate to Expenses view
     * @param onNavigateToTax      callback to navigate to Tax view
     */
    public void setNavigationCallbacks(Runnable onNavigateToIncome, Runnable onNavigateToExpenses, Runnable onNavigateToTax) {
        this.onNavigateToIncome = onNavigateToIncome;
        this.onNavigateToExpenses = onNavigateToExpenses;
        this.onNavigateToTax = onNavigateToTax;
    }

    private void loadDashboardData() {
        if (incomeService != null && expenseService != null && businessId != null && currentTaxYear != null) {
            viewModel.loadData(incomeService, expenseService, businessId, currentTaxYear);
            // Refresh UI after loading data
            populateActivity();
        }
    }

    /**
     * Refreshes the dashboard data.
     * Call this after adding income/expenses from quick actions.
     */
    public void refresh() {
        loadDashboardData();
    }

    // === Action Handlers (SE-208) ===

    @FXML
    void handleAddIncome(ActionEvent event) {
        if (incomeService == null || businessId == null || currentTaxYear == null) {
            // If services not available, just navigate to Income view
            if (onNavigateToIncome != null) {
                onNavigateToIncome.run();
            }
            return;
        }

        // Use the shared IncomeDialogHelper for consistent behavior
        javafx.stage.Window owner = dashboardContainer.getScene().getWindow();
        boolean success = uk.selfemploy.ui.util.IncomeDialogHelper.openAddDialog(
                owner,
                incomeService,
                businessId,
                currentTaxYear,
                income -> refresh()
        );

        if (!success && onNavigateToIncome != null) {
            // Fallback: navigate to Income view
            onNavigateToIncome.run();
        }
    }

    @FXML
    void handleAddExpense(ActionEvent event) {
        if (expenseService == null || businessId == null || currentTaxYear == null) {
            // If services not available, just navigate to Expenses view
            if (onNavigateToExpenses != null) {
                onNavigateToExpenses.run();
            }
            return;
        }

        // Use the shared ExpenseDialogHelper for consistent behavior
        javafx.stage.Window owner = dashboardContainer.getScene().getWindow();
        boolean success = uk.selfemploy.ui.util.ExpenseDialogHelper.openAddDialog(
                owner,
                expenseService,
                businessId,
                currentTaxYear,
                expense -> refresh()
        );

        if (!success && onNavigateToExpenses != null) {
            // Fallback: navigate to Expenses view
            onNavigateToExpenses.run();
        }
    }

    @FXML
    void handleViewTax(ActionEvent event) {
        if (onNavigateToTax != null) {
            onNavigateToTax.run();
        }
    }

    @FXML
    void handleViewAllActivity(ActionEvent event) {
        // Navigate to Income view (shows all activity)
        if (onNavigateToIncome != null) {
            onNavigateToIncome.run();
        }
    }

    // === TaxYearAware Implementation ===

    @Override
    public void setTaxYear(TaxYear taxYear) {
        this.currentTaxYear = taxYear;
        viewModel.setCurrentTaxYear(taxYear);

        // Fallback to CoreServiceFactory if not initialized via CDI
        if (incomeService == null) {
            incomeService = CoreServiceFactory.getIncomeService();
        }
        if (expenseService == null) {
            expenseService = CoreServiceFactory.getExpenseService();
        }
        if (businessId == null) {
            businessId = CoreServiceFactory.getDefaultBusinessId();
        }

        // Reload data with new tax year
        loadDashboardData();
    }

    /**
     * Returns the underlying ViewModel for testing purposes.
     */
    public DashboardViewModel getViewModel() {
        return viewModel;
    }

    // Expose dashboard container for dialog centering
    @FXML private VBox dashboardContainer;

    // === Refreshable Implementation ===

    @Override
    public void refreshData() {
        loadDashboardData();
    }
}
