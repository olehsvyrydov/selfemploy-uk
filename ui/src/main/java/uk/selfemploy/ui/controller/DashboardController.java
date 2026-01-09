package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.viewmodel.DashboardViewModel;
import uk.selfemploy.ui.viewmodel.DashboardViewModel.ActivityItem;
import uk.selfemploy.ui.viewmodel.Deadline;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller for the Dashboard view.
 * Binds UI elements to DashboardViewModel and handles user actions.
 */
public class DashboardController implements Initializable, MainController.TaxYearAware {

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

    // === Action Handlers ===

    @FXML
    void handleAddIncome(ActionEvent event) {
        // TODO: Navigate to Income view or open Add Income dialog
        System.out.println("Add Income clicked");
    }

    @FXML
    void handleAddExpense(ActionEvent event) {
        // TODO: Navigate to Expenses view or open Add Expense dialog
        System.out.println("Add Expense clicked");
    }

    @FXML
    void handleViewTax(ActionEvent event) {
        // TODO: Navigate to Tax Summary view
        System.out.println("View Tax clicked");
    }

    @FXML
    void handleViewAllActivity(ActionEvent event) {
        // TODO: Navigate to Activity view or expand activity section
        System.out.println("View All Activity clicked");
    }

    // === TaxYearAware Implementation ===

    @Override
    public void setTaxYear(TaxYear taxYear) {
        viewModel.setCurrentTaxYear(taxYear);
    }

    /**
     * Returns the underlying ViewModel for testing purposes.
     */
    public DashboardViewModel getViewModel() {
        return viewModel;
    }
}
