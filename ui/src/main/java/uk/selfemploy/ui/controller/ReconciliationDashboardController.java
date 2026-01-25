package uk.selfemploy.ui.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.ui.viewmodel.*;

import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

/**
 * Controller for the Reconciliation Dashboard.
 * Displays data health metrics and reconciliation issues.
 *
 * SE-10B-008: Reconciliation Dashboard
 */
public class ReconciliationDashboardController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationDashboardController.class);

    // Header
    @FXML private Label lastCheckedLabel;
    @FXML private Button refreshBtn;

    // Loading state
    @FXML private VBox loadingState;

    // Dashboard content
    @FXML private VBox dashboardContent;

    // Summary cards
    @FXML private Label totalIncomeValue;
    @FXML private Label incomeCountLabel;
    @FXML private Label totalExpensesValue;
    @FXML private Label expenseCountLabel;
    @FXML private Label duplicateCountValue;
    @FXML private StackPane duplicateBadge;
    @FXML private Label duplicateBadgeText;
    @FXML private Label uncategorizedCountValue;

    // All clear box
    @FXML private VBox allClearBox;

    // Issues section
    @FXML private VBox issuesSection;
    @FXML private Label issueCountLabel;
    @FXML private VBox issuesList;

    // Quick action buttons
    @FXML private Button reviewDuplicatesBtn;
    @FXML private Button fixCategoriesBtn;
    @FXML private Button checkGapsBtn;

    // ViewModel
    private ReconciliationViewModel viewModel;

    // Callbacks
    private Runnable onViewIncome;
    private Runnable onViewExpenses;
    private Runnable onReviewDuplicates;
    private Runnable onFixCategories;
    private Runnable onCheckGaps;
    private Consumer<ReconciliationIssue> onIssueAction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        viewModel = new ReconciliationViewModel();

        setupBindings();
        setupKeyboardNavigation();
    }

    /**
     * Sets the dashboard data.
     */
    public void setData(BigDecimal income, BigDecimal expenses,
                        int incomeCount, int expenseCount,
                        int duplicates, int uncategorized,
                        List<ReconciliationIssue> issues) {
        viewModel.setMetrics(income, expenses, incomeCount, expenseCount, duplicates, uncategorized);
        viewModel.setIssues(issues);
        refreshUI();
    }

    /**
     * Sets callback for viewing income.
     */
    public void setOnViewIncome(Runnable callback) {
        this.onViewIncome = callback;
    }

    /**
     * Sets callback for viewing expenses.
     */
    public void setOnViewExpenses(Runnable callback) {
        this.onViewExpenses = callback;
    }

    /**
     * Sets callback for reviewing duplicates.
     */
    public void setOnReviewDuplicates(Runnable callback) {
        this.onReviewDuplicates = callback;
    }

    /**
     * Sets callback for fixing categories.
     */
    public void setOnFixCategories(Runnable callback) {
        this.onFixCategories = callback;
    }

    /**
     * Sets callback for checking gaps.
     */
    public void setOnCheckGaps(Runnable callback) {
        this.onCheckGaps = callback;
    }

    /**
     * Sets callback for issue actions.
     */
    public void setOnIssueAction(Consumer<ReconciliationIssue> callback) {
        this.onIssueAction = callback;
    }

    private void setupBindings() {
        // Bind loading state
        viewModel.loadingProperty().addListener((obs, oldVal, newVal) -> {
            loadingState.setVisible(newVal);
            loadingState.setManaged(newVal);
            dashboardContent.setVisible(!newVal);
            dashboardContent.setManaged(!newVal);
        });

        // Bind summary values
        viewModel.totalIncomeProperty().addListener((obs, oldVal, newVal) ->
            totalIncomeValue.setText(viewModel.getFormattedTotalIncome()));

        viewModel.totalExpensesProperty().addListener((obs, oldVal, newVal) ->
            totalExpensesValue.setText(viewModel.getFormattedTotalExpenses()));

        viewModel.incomeCountProperty().addListener((obs, oldVal, newVal) ->
            incomeCountLabel.setText(viewModel.getIncomeCountText()));

        viewModel.expenseCountProperty().addListener((obs, oldVal, newVal) ->
            expenseCountLabel.setText(viewModel.getExpenseCountText()));

        viewModel.duplicateCountProperty().addListener((obs, oldVal, newVal) -> {
            duplicateCountValue.setText(viewModel.getDuplicateCountText());
            boolean hasDuplicates = newVal.intValue() > 0;
            duplicateBadge.setVisible(hasDuplicates);
            duplicateBadge.setManaged(hasDuplicates);
            if (hasDuplicates) {
                duplicateBadgeText.setText(String.valueOf(newVal));
            }
        });

        viewModel.uncategorizedCountProperty().addListener((obs, oldVal, newVal) ->
            uncategorizedCountValue.setText(viewModel.getUncategorizedCountText()));

        // Bind all clear state
        viewModel.allClearProperty().addListener((obs, oldVal, newVal) -> {
            allClearBox.setVisible(newVal);
            allClearBox.setManaged(newVal);
            issuesSection.setVisible(!newVal);
            issuesSection.setManaged(!newVal);
        });

        // Bind last checked
        viewModel.lastCheckedProperty().addListener((obs, oldVal, newVal) ->
            lastCheckedLabel.setText("Last checked: " + viewModel.getLastCheckedText()));
    }

    private void setupKeyboardNavigation() {
        // Summary cards are already focusable via FXML
        // Add keyboard handlers for Enter/Space
    }

    private void refreshUI() {
        // Update summary cards
        totalIncomeValue.setText(viewModel.getFormattedTotalIncome());
        incomeCountLabel.setText(viewModel.getIncomeCountText());
        totalExpensesValue.setText(viewModel.getFormattedTotalExpenses());
        expenseCountLabel.setText(viewModel.getExpenseCountText());
        duplicateCountValue.setText(viewModel.getDuplicateCountText());
        uncategorizedCountValue.setText(viewModel.getUncategorizedCountText());

        // Update duplicate badge
        boolean hasDuplicates = viewModel.hasDuplicates();
        duplicateBadge.setVisible(hasDuplicates);
        duplicateBadge.setManaged(hasDuplicates);
        if (hasDuplicates) {
            duplicateBadgeText.setText(viewModel.getDuplicateCountText());
        }

        // Update all clear state
        boolean isAllClear = viewModel.isAllClear();
        allClearBox.setVisible(isAllClear);
        allClearBox.setManaged(isAllClear);
        issuesSection.setVisible(!isAllClear);
        issuesSection.setManaged(!isAllClear);

        // Update issues list
        refreshIssuesList();

        // Update last checked
        lastCheckedLabel.setText("Last checked: " + viewModel.getLastCheckedText());

        // Update quick action button states
        reviewDuplicatesBtn.setDisable(!viewModel.hasDuplicates());
        fixCategoriesBtn.setDisable(!viewModel.hasUncategorized());
    }

    private void refreshIssuesList() {
        issuesList.getChildren().clear();

        int issueCount = viewModel.getIssueCount();
        issueCountLabel.setText("(" + issueCount + ")");

        for (ReconciliationIssue issue : viewModel.getIssues()) {
            issuesList.getChildren().add(createIssueCard(issue));
        }
    }

    /**
     * Creates an issue card following /aura design specification.
     */
    private HBox createIssueCard(ReconciliationIssue issue) {
        HBox card = new HBox(12);
        card.getStyleClass().addAll("issue-card", issue.getSeverity().getStyleClass());
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setFocusTraversable(true);
        card.setAccessibleText(issue.getAccessibleText());

        // Severity indicator
        Label severityIcon = new Label(issue.getSeverity().getIcon());
        severityIcon.getStyleClass().addAll("severity-icon", issue.getSeverity().getStyleClass());

        // Issue content
        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label title = new Label(issue.getTitle());
        title.getStyleClass().add("issue-title");

        Label description = new Label(issue.getDescription());
        description.getStyleClass().add("issue-description");
        description.setWrapText(true);

        content.getChildren().addAll(title, description);

        // Details (if any)
        if (issue.hasDetails() && issue.getDetails().size() <= 3) {
            HBox detailsBox = new HBox(8);
            detailsBox.getStyleClass().add("issue-details");
            for (String detail : issue.getDetails()) {
                Label detailChip = new Label(detail);
                detailChip.getStyleClass().add("detail-chip");
                detailsBox.getChildren().add(detailChip);
            }
            content.getChildren().add(detailsBox);
        }

        // Action button
        Button actionBtn = new Button(issue.getActionText());
        actionBtn.getStyleClass().addAll("issue-action-btn", issue.getSeverity().getStyleClass());
        actionBtn.setOnAction(e -> {
            if (onIssueAction != null) {
                onIssueAction.accept(issue);
            }
        });

        card.getChildren().addAll(severityIcon, content, actionBtn);

        // Keyboard support
        card.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                if (onIssueAction != null) {
                    onIssueAction.accept(issue);
                }
                e.consume();
            }
        });

        return card;
    }

    // === Card Click Handlers ===

    @FXML
    void handleIncomeCardClick(MouseEvent event) {
        if (onViewIncome != null) {
            onViewIncome.run();
        }
        LOG.debug("Income card clicked");
    }

    @FXML
    void handleExpensesCardClick(MouseEvent event) {
        if (onViewExpenses != null) {
            onViewExpenses.run();
        }
        LOG.debug("Expenses card clicked");
    }

    @FXML
    void handleDuplicatesCardClick(MouseEvent event) {
        if (viewModel.hasDuplicates() && onReviewDuplicates != null) {
            onReviewDuplicates.run();
        }
        LOG.debug("Duplicates card clicked");
    }

    @FXML
    void handleCategoriesCardClick(MouseEvent event) {
        if (viewModel.hasUncategorized() && onFixCategories != null) {
            onFixCategories.run();
        }
        LOG.debug("Categories card clicked");
    }

    // === Quick Action Handlers ===

    @FXML
    void handleRefresh(ActionEvent event) {
        viewModel.setLoading(true);
        LOG.info("Refreshing reconciliation data");

        // Simulate async refresh (in real implementation, call service)
        Platform.runLater(() -> {
            viewModel.refresh();
            viewModel.setLoading(false);
        });
    }

    @FXML
    void handleReviewDuplicates(ActionEvent event) {
        if (onReviewDuplicates != null) {
            onReviewDuplicates.run();
        }
        LOG.info("Review duplicates clicked");
    }

    @FXML
    void handleFixCategories(ActionEvent event) {
        if (onFixCategories != null) {
            onFixCategories.run();
        }
        LOG.info("Fix categories clicked");
    }

    @FXML
    void handleCheckGaps(ActionEvent event) {
        if (onCheckGaps != null) {
            onCheckGaps.run();
        }
        LOG.info("Check gaps clicked");
    }

    // === For Testing ===

    /**
     * Returns the ViewModel for testing.
     */
    public ReconciliationViewModel getViewModel() {
        return viewModel;
    }

    /**
     * Sets the ViewModel directly (for testing).
     */
    public void setViewModel(ReconciliationViewModel viewModel) {
        this.viewModel = viewModel;
        setupBindings();
        refreshUI();
    }

    /**
     * Creates sample data for testing.
     */
    public static void loadSampleData(ReconciliationDashboardController controller) {
        List<ReconciliationIssue> issues = new ArrayList<>();

        // High priority: duplicates
        issues.add(ReconciliationIssue.duplicates(3, List.of(
            "Amazon - 23.99",
            "Amazon - 23.99",
            "Office Depot - 156.00"
        )));

        // Medium priority: missing categories
        issues.add(ReconciliationIssue.missingCategories(8));

        // Low priority: date gaps
        issues.add(ReconciliationIssue.dateGaps(2, List.of("October 2025", "November 2025")));

        controller.setData(
            new BigDecimal("45230.00"),
            new BigDecimal("12450.75"),
            142, 387,
            3, 8,
            issues
        );
    }
}
