package uk.selfemploy.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.component.QuarterInfoDialog;
import uk.selfemploy.ui.component.QuarterlyReviewDialog;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.ui.service.AutoOAuthSubmissionService;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.service.HmrcConnectionService;
import uk.selfemploy.ui.service.OAuthServiceFactory;
import uk.selfemploy.ui.viewmodel.CategorySummary;
import uk.selfemploy.ui.viewmodel.QuarterStatus;
import uk.selfemploy.ui.viewmodel.QuarterViewModel;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Controller for the Quarterly Updates dashboard.
 * Sprint 10D: SE-10D-001, SE-10D-002, SE-10D-003
 *
 * <p>Displays all 4 quarters for the current tax year with their status,
 * financial totals, and submission deadlines.</p>
 */
public class QuarterlyUpdatesController implements Initializable, MainController.TaxYearAware {

    private static final Logger LOG = Logger.getLogger(QuarterlyUpdatesController.class.getName());
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    // FXML-injected components
    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;
    @FXML private Label currentQuarterLabel;
    @FXML private GridPane quarterGrid;
    @FXML private Button backButton;

    // Quarter cards (Q1-Q4)
    @FXML private VBox q1Card;
    @FXML private VBox q2Card;
    @FXML private VBox q3Card;
    @FXML private VBox q4Card;

    // Quarter action buttons
    @FXML private Button q1ActionBtn;
    @FXML private Button q2ActionBtn;
    @FXML private Button q3ActionBtn;
    @FXML private Button q4ActionBtn;

    // Tax year label
    @FXML private Label taxYearLabel;

    // Quarter data labels - Q1
    @FXML private Label q1StatusBadge;
    @FXML private Label q1DateRange;
    @FXML private Label q1Income;
    @FXML private Label q1Expenses;
    @FXML private Label q1Net;
    @FXML private Label q1Deadline;

    // Quarter data labels - Q2
    @FXML private Label q2StatusBadge;
    @FXML private Label q2DateRange;
    @FXML private Label q2Income;
    @FXML private Label q2Expenses;
    @FXML private Label q2Net;
    @FXML private Label q2Deadline;

    // Quarter data labels - Q3
    @FXML private Label q3StatusBadge;
    @FXML private Label q3DateRange;
    @FXML private Label q3Income;
    @FXML private Label q3Expenses;
    @FXML private Label q3Net;
    @FXML private Label q3Deadline;

    // Quarter data labels - Q4
    @FXML private Label q4StatusBadge;
    @FXML private Label q4DateRange;
    @FXML private Label q4Income;
    @FXML private Label q4Expenses;
    @FXML private Label q4Net;
    @FXML private Label q4Deadline;

    // Services
    private IncomeService incomeService;
    private ExpenseService expenseService;
    private UUID businessId;

    // State
    private TaxYear taxYear;
    private Clock clock = Clock.systemDefaultZone();
    private List<QuarterViewModel> quarterViewModels = new ArrayList<>();

    // Callback for navigation
    private Runnable onBack;
    private Runnable navigateToSettings;

    // Last clicked quarter (for testing/navigation)
    private Quarter lastClickedQuarter;

    // Dialog suppression for testing (avoids blocking dialogs in unit tests)
    private boolean dialogSuppressed = false;

    // Tracks which quarters have had dialogs shown (for testing)
    private Set<Quarter> dialogsShownForQuarters = EnumSet.noneOf(Quarter.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOG.info("QuarterlyUpdatesController.initialize()");
        initializeServices();

        // Lazy verification: if session needs verification, verify before loading data
        performLazyVerificationIfNeeded();
    }

    /**
     * Performs lazy session verification if needed.
     * Called on page load to verify stored tokens before user tries to submit.
     */
    private void performLazyVerificationIfNeeded() {
        HmrcConnectionService connectionService = HmrcConnectionService.getInstance();

        if (connectionService.needsVerification()) {
            LOG.info("Session needs verification - performing lazy verification");

            // Show verifying state in UI
            if (pageSubtitle != null) {
                pageSubtitle.setText("Verifying HMRC connection...");
            }

            connectionService.verifySession()
                .thenAccept(result -> javafx.application.Platform.runLater(() -> {
                    switch (result) {
                        case VERIFIED -> {
                            LOG.info("Lazy verification succeeded");
                            if (pageSubtitle != null) {
                                pageSubtitle.setText("Submit your income and expenses to HMRC each quarter");
                            }
                            refreshQuarterData();
                        }
                        case EXPIRED -> {
                            LOG.info("Lazy verification failed - session expired");
                            if (pageSubtitle != null) {
                                pageSubtitle.setText("Session expired. Please reconnect via HMRC Submission page.");
                            }
                        }
                        case NOT_CONNECTED -> {
                            LOG.info("Not connected to HMRC");
                            if (pageSubtitle != null) {
                                pageSubtitle.setText("Connect to HMRC via the HMRC Submission page to submit quarterly updates.");
                            }
                        }
                    }
                }))
                .exceptionally(ex -> {
                    LOG.warning("Lazy verification error: " + ex.getMessage());
                    javafx.application.Platform.runLater(() -> {
                        if (pageSubtitle != null) {
                            pageSubtitle.setText("Connection verification failed. Please try again.");
                        }
                    });
                    return null;
                });
        }
    }

    /**
     * Initializes services for loading financial data.
     */
    private void initializeServices() {
        if (incomeService == null) {
            incomeService = CoreServiceFactory.getIncomeService();
        }
        if (expenseService == null) {
            expenseService = CoreServiceFactory.getExpenseService();
        }
        if (businessId == null) {
            businessId = CoreServiceFactory.getDefaultBusinessId();
        }
    }

    /**
     * Initializes controller with dependencies for testing.
     * Package-private for testing.
     */
    void initializeWithDependencies(IncomeService incomeService, ExpenseService expenseService, UUID businessId) {
        this.incomeService = incomeService;
        this.expenseService = expenseService;
        this.businessId = businessId;
    }

    /**
     * Sets the clock for testing time-dependent behavior.
     * Package-private for testing.
     */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void setTaxYear(TaxYear taxYear) {
        this.taxYear = taxYear;
        refreshQuarterData();
    }

    /**
     * Returns the tax year.
     */
    public TaxYear getTaxYear() {
        return taxYear;
    }

    /**
     * Returns the list of quarter view models.
     * Creates them if not yet initialized.
     */
    public List<QuarterViewModel> getQuarterViewModels() {
        if (quarterViewModels.isEmpty() && taxYear != null) {
            refreshQuarterData();
        }
        return quarterViewModels;
    }

    /**
     * Refreshes quarter data from services.
     */
    private void refreshQuarterData() {
        if (taxYear == null) {
            LOG.warning("Cannot refresh quarter data - tax year not set");
            return;
        }

        LOG.info("Refreshing quarter data for tax year: " + taxYear.label());
        quarterViewModels.clear();

        LocalDate today = LocalDate.now(clock);
        Quarter currentQuarter = Quarter.forDate(today);

        for (Quarter quarter : Quarter.values()) {
            QuarterViewModel viewModel = createQuarterViewModel(quarter, currentQuarter, today);
            quarterViewModels.add(viewModel);
        }

        updateUI();
    }

    /**
     * Creates a QuarterViewModel for the given quarter.
     */
    private QuarterViewModel createQuarterViewModel(Quarter quarter, Quarter currentQuarter, LocalDate today) {
        boolean isCurrent = quarter == currentQuarter;
        QuarterStatus status = determineStatus(quarter, today);

        BigDecimal totalIncome = null;
        BigDecimal totalExpenses = null;

        // Only load financial data for non-future quarters
        if (status != QuarterStatus.FUTURE) {
            try {
                totalIncome = incomeService.getTotalByQuarter(businessId, taxYear, quarter);
                totalExpenses = expenseService.getDeductibleTotalByQuarter(businessId, taxYear, quarter);
            } catch (Exception e) {
                LOG.warning("Failed to load financial data for " + quarter + ": " + e.getMessage());
                // Leave as null - will show "--" in UI
            }
        }

        return new QuarterViewModel(quarter, taxYear, status, isCurrent, totalIncome, totalExpenses);
    }

    /**
     * Determines the status of a quarter based on the current date.
     */
    private QuarterStatus determineStatus(Quarter quarter, LocalDate today) {
        LocalDate quarterEnd = quarter.getEndDate(taxYear);
        LocalDate deadline = quarter.getDeadline(taxYear);

        // Future: quarter hasn't started yet
        if (today.isBefore(quarter.getStartDate(taxYear))) {
            return QuarterStatus.FUTURE;
        }

        // Overdue: deadline has passed and not submitted
        // TODO: Check submission status from database
        if (today.isAfter(deadline)) {
            // For now, assume not submitted if deadline passed
            // In future, check SubmissionRepository
            return QuarterStatus.OVERDUE;
        }

        // Draft: quarter has ended but deadline not yet passed, or current quarter with data
        if (today.isAfter(quarterEnd) || hasDataForQuarter(quarter)) {
            return QuarterStatus.DRAFT;
        }

        // Current quarter with no data yet - treat as Draft
        if (Quarter.forDate(today) == quarter) {
            return QuarterStatus.DRAFT;
        }

        return QuarterStatus.FUTURE;
    }

    /**
     * Checks if there is any financial data for the quarter.
     */
    private boolean hasDataForQuarter(Quarter quarter) {
        try {
            BigDecimal income = incomeService.getTotalByQuarter(businessId, taxYear, quarter);
            return income != null && income.compareTo(BigDecimal.ZERO) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Updates the UI with the current quarter data.
     */
    private void updateUI() {
        if (quarterViewModels.isEmpty()) {
            return;
        }

        // Update tax year label
        if (taxYearLabel != null) {
            taxYearLabel.setText(getTaxYearLabelText());
        }

        // Update current quarter label
        if (currentQuarterLabel != null) {
            Quarter current = Quarter.forDate(LocalDate.now(clock));
            if (current != null) {
                currentQuarterLabel.setText("Current Quarter: " + current.name());
            }
        }

        // Update each quarter card
        for (QuarterViewModel viewModel : quarterViewModels) {
            updateQuarterCard(viewModel);
        }

        LOG.info("UI updated with " + quarterViewModels.size() + " quarters");
    }

    /**
     * Updates a quarter card with data from the view model.
     */
    private void updateQuarterCard(QuarterViewModel viewModel) {
        Quarter quarter = viewModel.getQuarter();
        QuarterStatus status = viewModel.getStatus();

        // Get the labels and button for this quarter
        Label statusBadge = getStatusBadgeForQuarter(quarter);
        Label dateRange = getDateRangeForQuarter(quarter);
        Label income = getIncomeForQuarter(quarter);
        Label expenses = getExpensesForQuarter(quarter);
        Label net = getNetForQuarter(quarter);
        Label deadline = getDeadlineForQuarter(quarter);
        Button actionBtn = getActionBtnForQuarter(quarter);

        // Update status badge
        if (statusBadge != null) {
            statusBadge.setText(status.getDisplayText().toUpperCase());
            statusBadge.getStyleClass().removeAll(
                    "status-badge-draft", "status-badge-submitted",
                    "status-badge-overdue", "status-badge-future"
            );
            statusBadge.getStyleClass().add(status.getStyleClass());
        }

        // Update date range
        if (dateRange != null) {
            dateRange.setText(viewModel.getDateRangeText());
        }

        // Update financial data
        if (income != null) {
            income.setText(viewModel.getFormattedIncome());
        }
        if (expenses != null) {
            expenses.setText(viewModel.getFormattedExpenses());
        }
        if (net != null) {
            net.setText(viewModel.getFormattedNetProfitLoss());
        }

        // Update deadline
        if (deadline != null) {
            deadline.setText(viewModel.getDeadlineText());
            // Add overdue style if applicable
            if (status == QuarterStatus.OVERDUE) {
                if (!deadline.getStyleClass().contains("quarter-deadline-overdue")) {
                    deadline.getStyleClass().add("quarter-deadline-overdue");
                }
            } else {
                deadline.getStyleClass().remove("quarter-deadline-overdue");
            }
        }

        // Update action button
        if (actionBtn != null) {
            actionBtn.setText(getButtonTextForQuarter(quarter));
            actionBtn.setDisable(status == QuarterStatus.FUTURE);

            // Update button style based on status
            actionBtn.getStyleClass().removeAll(
                    "button-primary", "button-secondary", "button-warning", "button-disabled"
            );
            String buttonStyle = switch (status) {
                case OVERDUE -> "button-warning";
                case DRAFT -> viewModel.isCurrent() ? "button-primary" : "button-secondary";
                case SUBMITTED -> "button-secondary";
                case FUTURE -> "button-disabled";
            };
            actionBtn.getStyleClass().add(buttonStyle);
        }

        // Update card style
        VBox card = getCardForQuarter(quarter);
        if (card != null) {
            card.getStyleClass().removeAll(
                    "quarter-card-current", "quarter-card-future",
                    "quarter-card-submitted", "quarter-card-overdue"
            );
            if (viewModel.isCurrent()) {
                card.getStyleClass().add("quarter-card-current");
            } else {
                card.getStyleClass().add(status.getCardStyleClass());
            }
        }
    }

    // ====== Quarter-specific accessor methods ======

    private Label getStatusBadgeForQuarter(Quarter quarter) {
        return switch (quarter) {
            case Q1 -> q1StatusBadge;
            case Q2 -> q2StatusBadge;
            case Q3 -> q3StatusBadge;
            case Q4 -> q4StatusBadge;
        };
    }

    private Label getDateRangeForQuarter(Quarter quarter) {
        return switch (quarter) {
            case Q1 -> q1DateRange;
            case Q2 -> q2DateRange;
            case Q3 -> q3DateRange;
            case Q4 -> q4DateRange;
        };
    }

    private Label getIncomeForQuarter(Quarter quarter) {
        return switch (quarter) {
            case Q1 -> q1Income;
            case Q2 -> q2Income;
            case Q3 -> q3Income;
            case Q4 -> q4Income;
        };
    }

    private Label getExpensesForQuarter(Quarter quarter) {
        return switch (quarter) {
            case Q1 -> q1Expenses;
            case Q2 -> q2Expenses;
            case Q3 -> q3Expenses;
            case Q4 -> q4Expenses;
        };
    }

    private Label getNetForQuarter(Quarter quarter) {
        return switch (quarter) {
            case Q1 -> q1Net;
            case Q2 -> q2Net;
            case Q3 -> q3Net;
            case Q4 -> q4Net;
        };
    }

    private Label getDeadlineForQuarter(Quarter quarter) {
        return switch (quarter) {
            case Q1 -> q1Deadline;
            case Q2 -> q2Deadline;
            case Q3 -> q3Deadline;
            case Q4 -> q4Deadline;
        };
    }

    private Button getActionBtnForQuarter(Quarter quarter) {
        return switch (quarter) {
            case Q1 -> q1ActionBtn;
            case Q2 -> q2ActionBtn;
            case Q3 -> q3ActionBtn;
            case Q4 -> q4ActionBtn;
        };
    }

    private VBox getCardForQuarter(Quarter quarter) {
        return switch (quarter) {
            case Q1 -> q1Card;
            case Q2 -> q2Card;
            case Q3 -> q3Card;
            case Q4 -> q4Card;
        };
    }

    /**
     * Sets the callback for the back button.
     */
    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    /**
     * Sets a callback to navigate to the Settings page.
     *
     * <p>SE-10E-003: When a settings-related submission error occurs (e.g., NINO not set),
     * the error dialog shows "Open Settings". Clicking it closes the quarterly updates window
     * and navigates to Settings in the main app.</p>
     *
     * @param navigateToSettings callback to navigate to Settings
     */
    public void setNavigateToSettings(Runnable navigateToSettings) {
        this.navigateToSettings = navigateToSettings;
    }

    @FXML
    void handleBack() {
        if (onBack != null) {
            onBack.run();
        }
    }

    /**
     * Handles clicking on a quarter card.
     * Opens the quarter detail view.
     */
    void handleQuarterClick(Quarter quarter) {
        LOG.info("Quarter clicked: " + quarter);
        // TODO: Open quarter detail view
        // For Sprint 10D, just log the click
    }

    // ====== FXML Action Handlers ======

    /**
     * Handles Q1 action button click.
     * Called from FXML onAction.
     */
    @FXML
    void handleQ1Action() {
        processQuarterAction(Quarter.Q1);
    }

    /**
     * Handles Q2 action button click.
     * Called from FXML onAction.
     */
    @FXML
    void handleQ2Action() {
        processQuarterAction(Quarter.Q2);
    }

    /**
     * Handles Q3 action button click.
     * Called from FXML onAction.
     */
    @FXML
    void handleQ3Action() {
        processQuarterAction(Quarter.Q3);
    }

    /**
     * Handles Q4 action button click.
     * Called from FXML onAction.
     */
    @FXML
    void handleQ4Action() {
        processQuarterAction(Quarter.Q4);
    }

    /**
     * Processes the action for a quarter.
     * Ignores action if the quarter status is FUTURE.
     * Shows an information dialog with quarter details.
     */
    private void processQuarterAction(Quarter quarter) {
        if (quarterViewModels.isEmpty()) {
            LOG.warning("Cannot process quarter action - view models not initialized");
            return;
        }

        QuarterViewModel viewModel = getViewModelForQuarter(quarter);
        if (viewModel == null) {
            LOG.warning("No view model found for quarter: " + quarter);
            return;
        }

        QuarterStatus status = viewModel.getStatus();
        if (status == QuarterStatus.FUTURE) {
            LOG.info("Ignoring action for FUTURE quarter: " + quarter);
            // Don't set lastClickedQuarter for FUTURE quarters
            return;
        }

        lastClickedQuarter = quarter;
        LOG.info("Quarter action triggered: " + quarter + " with status: " + status);

        // Record that dialog was shown for this quarter
        dialogsShownForQuarters.add(quarter);

        // Show dialog with quarter information
        showQuarterDialog(quarter, viewModel);
    }

    /**
     * Shows the custom QuarterInfoDialog for the quarter action.
     *
     * <p>Displays a professionally styled dialog with:</p>
     * <ul>
     *   <li>Status-colored gradient header with icon</li>
     *   <li>Financial summary for the quarter</li>
     *   <li>Year-to-date totals</li>
     *   <li>Deadline countdown</li>
     *   <li>Contextual action button (Start Review / Submit Now)</li>
     * </ul>
     *
     * @param quarter The quarter
     * @param viewModel The quarter view model
     */
    private void showQuarterDialog(Quarter quarter, QuarterViewModel viewModel) {
        LOG.info("Showing QuarterInfoDialog for " + quarter + " with status: " + viewModel.getStatus());

        // Check suppression flag BEFORE creating dialog to avoid JavaFX initialization in tests
        if (dialogSuppressed) {
            LOG.info("Dialog suppressed for testing - quarter: " + quarter);
            return;
        }

        // Calculate YTD totals
        BigDecimal ytdIncome = calculateYtdIncome(quarter);
        BigDecimal ytdExpenses = calculateYtdExpenses(quarter);

        // Create and configure the dialog
        QuarterInfoDialog dialog = new QuarterInfoDialog(viewModel, ytdIncome, ytdExpenses);
        dialog.setOnReviewAction(() -> {
            LOG.info("Review action triggered for " + quarter);
            // Close the info dialog and show the review dialog
            showQuarterlyReviewDialog(quarter, viewModel);
        });

        // Show the dialog
        dialog.showAndWait();
    }

    /**
     * Shows the Quarterly Review Dialog with aggregated data for submission review.
     *
     * <p>When a {@link QuarterlyReviewDialog.SubmissionHandler} is available (HMRC auth
     * configured), the dialog will submit directly to HMRC. Otherwise, the dialog
     * operates in review-only mode.</p>
     *
     * @param quarter the quarter to review
     * @param viewModel the quarter view model
     */
    private void showQuarterlyReviewDialog(Quarter quarter, QuarterViewModel viewModel) {
        LOG.info("Showing QuarterlyReviewDialog for " + quarter);

        // Check suppression flag BEFORE creating dialog to avoid JavaFX initialization in tests
        if (dialogSuppressed) {
            LOG.info("QuarterlyReviewDialog suppressed for testing - quarter: " + quarter);
            return;
        }

        try {
            // Aggregate review data
            QuarterlyReviewData reviewData = aggregateReviewData(quarter);
            boolean isOverdue = viewModel.getStatus() == QuarterStatus.OVERDUE;

            // Create and configure the review dialog
            QuarterlyReviewDialog reviewDialog = new QuarterlyReviewDialog(reviewData, isOverdue);

            // Wire up submission handler if HMRC auth is available
            QuarterlyReviewDialog.SubmissionHandler handler = getSubmissionHandler(quarter);
            if (handler != null) {
                reviewDialog.setSubmissionHandler(handler);
                reviewDialog.setOnSubmitComplete(submission -> {
                    LOG.info("Submission complete: " + submission.hmrcReference());
                    refreshQuarterData();
                });
            }

            // SE-10E-003: Wire "Open Settings" navigation for settings errors
            if (navigateToSettings != null) {
                reviewDialog.setOnOpenSettings(navigateToSettings);
            }

            reviewDialog.setOnSubmitAction(() -> {
                LOG.info("Submit action triggered for " + quarter);
            });

            reviewDialog.showAndWait();
        } catch (Exception e) {
            LOG.warning("Failed to show QuarterlyReviewDialog: " + e.getMessage());
        }
    }

    /**
     * Returns a submission handler for the given quarter.
     *
     * <p>Sprint 13: Uses AutoOAuthSubmissionService which automatically triggers OAuth
     * when not connected or when session expires. No upfront connection check needed.</p>
     *
     * <p>The handler performs:</p>
     * <ul>
     *   <li>Pre-flight NINO validation (fails fast if not set)</li>
     *   <li>Auto-triggers OAuth when not connected or session expired</li>
     *   <li>Handles 401 errors with automatic re-authentication and retry</li>
     * </ul>
     *
     * @param quarter the quarter being submitted
     * @return a SubmissionHandler that wraps AutoOAuthSubmissionService
     */
    QuarterlyReviewDialog.SubmissionHandler getSubmissionHandler(Quarter quarter) {
        try {
            // Check if HMRC OAuth credentials are configured at all
            if (!OAuthServiceFactory.isConfigured()) {
                LOG.fine("HMRC OAuth not configured - submission handler unavailable");
                return null;
            }

            // Return a handler that delegates to AutoOAuthSubmissionService
            // OAuth will be triggered automatically when needed (not connected, expired, 401)
            return (reviewData, declarationAcceptedAt, declarationTextHash) -> {
                AutoOAuthSubmissionService autoOAuthService = new AutoOAuthSubmissionService();
                return autoOAuthService.submit(reviewData, declarationAcceptedAt, declarationTextHash);
            };

        } catch (Exception e) {
            LOG.warning("Failed to create submission handler: " + e.getMessage());
            return null;
        }
    }

    /**
     * Aggregates review data for the Quarterly Review Dialog.
     *
     * @param quarter the quarter to aggregate data for
     * @return the aggregated review data
     */
    QuarterlyReviewData aggregateReviewData(Quarter quarter) {
        LOG.fine("Aggregating review data for " + quarter);

        LocalDate periodStart = quarter.getStartDate(taxYear);
        LocalDate periodEnd = quarter.getEndDate(taxYear);

        // Get income data
        BigDecimal totalIncome = BigDecimal.ZERO;
        int incomeTransactionCount = 0;
        try {
            totalIncome = incomeService.getTotalByQuarter(businessId, taxYear, quarter);
            if (totalIncome == null) {
                totalIncome = BigDecimal.ZERO;
            }
            incomeTransactionCount = incomeService.countByQuarter(businessId, taxYear, quarter);
        } catch (Exception e) {
            LOG.warning("Failed to get income data for " + quarter + ": " + e.getMessage());
        }

        // Get expense data grouped by category
        Map<ExpenseCategory, CategorySummary> expensesByCategory = new EnumMap<>(ExpenseCategory.class);
        BigDecimal totalExpenses = BigDecimal.ZERO;
        int expenseTransactionCount = 0;
        try {
            List<Expense> expenses = expenseService.findByQuarter(businessId, taxYear, quarter);

            // Group by category with amount and count
            Map<ExpenseCategory, List<Expense>> grouped = expenses.stream()
                    .filter(Expense::isAllowable)
                    .collect(Collectors.groupingBy(Expense::category));

            for (Map.Entry<ExpenseCategory, List<Expense>> entry : grouped.entrySet()) {
                BigDecimal categoryTotal = entry.getValue().stream()
                        .map(Expense::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                int categoryCount = entry.getValue().size();
                expensesByCategory.put(entry.getKey(), new CategorySummary(categoryTotal, categoryCount));
                totalExpenses = totalExpenses.add(categoryTotal);
                expenseTransactionCount += categoryCount;
            }
        } catch (Exception e) {
            LOG.warning("Failed to get expense data for " + quarter + ": " + e.getMessage());
        }

        return QuarterlyReviewData.builder()
                .quarter(quarter)
                .taxYear(taxYear)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .totalIncome(totalIncome)
                .incomeTransactionCount(incomeTransactionCount)
                .expensesByCategory(expensesByCategory)
                .totalExpenses(totalExpenses)
                .expenseTransactionCount(expenseTransactionCount)
                .build();
    }

    /**
     * Calculates year-to-date income from Q1 up to and including the given quarter.
     *
     * @param upToQuarter the quarter to calculate YTD up to (inclusive)
     * @return the year-to-date income total
     */
    BigDecimal calculateYtdIncome(Quarter upToQuarter) {
        if (taxYear == null || businessId == null || incomeService == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Quarter q : Quarter.values()) {
            try {
                BigDecimal quarterIncome = incomeService.getTotalByQuarter(businessId, taxYear, q);
                if (quarterIncome != null) {
                    total = total.add(quarterIncome);
                }
            } catch (Exception e) {
                LOG.warning("Failed to get income for " + q + ": " + e.getMessage());
            }

            // Stop after the target quarter
            if (q == upToQuarter) {
                break;
            }
        }
        return total;
    }

    /**
     * Calculates year-to-date expenses from Q1 up to and including the given quarter.
     *
     * @param upToQuarter the quarter to calculate YTD up to (inclusive)
     * @return the year-to-date expenses total
     */
    BigDecimal calculateYtdExpenses(Quarter upToQuarter) {
        if (taxYear == null || businessId == null || expenseService == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Quarter q : Quarter.values()) {
            try {
                BigDecimal quarterExpenses = expenseService.getDeductibleTotalByQuarter(businessId, taxYear, q);
                if (quarterExpenses != null) {
                    total = total.add(quarterExpenses);
                }
            } catch (Exception e) {
                LOG.warning("Failed to get expenses for " + q + ": " + e.getMessage());
            }

            // Stop after the target quarter
            if (q == upToQuarter) {
                break;
            }
        }
        return total;
    }

    /**
     * Returns the QuarterViewModel for the given quarter.
     */
    private QuarterViewModel getViewModelForQuarter(Quarter quarter) {
        return quarterViewModels.stream()
                .filter(vm -> vm.getQuarter() == quarter)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the last clicked quarter.
     * Package-private for testing.
     */
    Quarter getLastClickedQuarter() {
        return lastClickedQuarter;
    }

    /**
     * Returns the button text for a quarter based on its status.
     * Package-private for testing.
     */
    String getButtonTextForQuarter(Quarter quarter) {
        QuarterViewModel viewModel = getViewModelForQuarter(quarter);
        if (viewModel == null) {
            return "View";
        }

        return switch (viewModel.getStatus()) {
            case SUBMITTED -> "View Details";
            case OVERDUE -> "Submit Now";
            case DRAFT -> "Review";
            case FUTURE -> "Future";
        };
    }

    /**
     * Returns the tax year label text.
     * Package-private for testing.
     */
    String getTaxYearLabelText() {
        if (taxYear == null) {
            return "";
        }
        return "Tax Year: " + taxYear.label();
    }

    // ====== Dialog Generation Methods (Package-private for testing) ======

    /**
     * Returns the dialog title for a quarter based on its status.
     * Package-private for testing.
     */
    String getDialogTitleForQuarter(Quarter quarter) {
        QuarterViewModel viewModel = getViewModelForQuarter(quarter);
        if (viewModel == null) {
            return "Quarter Details";
        }
        return getDialogTitleForStatus(viewModel.getStatus());
    }

    /**
     * Returns the dialog title for a given status.
     * Package-private for testing.
     */
    String getDialogTitleForStatus(QuarterStatus status) {
        return switch (status) {
            case DRAFT -> "Review Quarter";
            case OVERDUE -> "Submit Overdue Quarter";
            case SUBMITTED -> "View Submission";
            case FUTURE -> "Future Quarter";
        };
    }

    /**
     * Returns the dialog message for a quarter based on its status.
     * Package-private for testing.
     */
    String getDialogMessageForQuarter(Quarter quarter) {
        QuarterViewModel viewModel = getViewModelForQuarter(quarter);
        if (viewModel == null) {
            return "No data available for this quarter.";
        }
        return getDialogMessageForStatus(quarter, viewModel.getStatus(), viewModel);
    }

    /**
     * Returns the dialog message for a given quarter and status.
     * Package-private for testing.
     *
     * @param quarter The quarter
     * @param status The quarter status
     * @param viewModel The view model (may be null for testing)
     */
    String getDialogMessageForStatus(Quarter quarter, QuarterStatus status, QuarterViewModel viewModel) {
        return switch (status) {
            case DRAFT -> buildDraftMessage(quarter, viewModel);
            case OVERDUE -> buildOverdueMessage(quarter, viewModel);
            case SUBMITTED -> buildSubmittedMessage(quarter);
            case FUTURE -> "This quarter has not started yet.";
        };
    }

    /**
     * Builds the dialog message for a DRAFT quarter.
     */
    private String buildDraftMessage(Quarter quarter, QuarterViewModel viewModel) {
        StringBuilder sb = new StringBuilder();
        sb.append("Review your income and expenses for ").append(quarter.name());
        sb.append(" before submitting to HMRC.\n\n");

        if (taxYear != null) {
            sb.append("Deadline: ").append(quarter.getDeadline(taxYear).format(DEADLINE_FORMAT));
        }

        if (viewModel != null && viewModel.hasData()) {
            sb.append("\n\nIncome: ").append(viewModel.getFormattedIncome());
            sb.append("\nExpenses: ").append(viewModel.getFormattedExpenses());
            sb.append("\nNet: ").append(viewModel.getFormattedNetProfitLoss());
        }

        return sb.toString();
    }

    /**
     * Builds the dialog message for an OVERDUE quarter.
     */
    private String buildOverdueMessage(Quarter quarter, QuarterViewModel viewModel) {
        StringBuilder sb = new StringBuilder();
        sb.append("This quarter is OVERDUE! Submit as soon as possible.\n\n");
        sb.append("Quarter: ").append(quarter.name());

        if (viewModel != null) {
            sb.append("\nIncome: ").append(viewModel.getFormattedIncome());
            sb.append("\nExpenses: ").append(viewModel.getFormattedExpenses());
            sb.append("\nNet: ").append(viewModel.getFormattedNetProfitLoss());
        }

        return sb.toString();
    }

    /**
     * Builds the dialog message for a SUBMITTED quarter.
     */
    private String buildSubmittedMessage(Quarter quarter) {
        return "This quarter has already been submitted to HMRC.\n\n" +
               "You can view the submission details but cannot make changes.";
    }

    // ====== Test Support Methods (Package-private) ======

    /**
     * Sets whether dialogs should be suppressed (for testing).
     * Package-private for testing.
     */
    void setDialogSuppressed(boolean suppressed) {
        this.dialogSuppressed = suppressed;
    }

    /**
     * Returns whether a dialog was shown for the given quarter.
     * Package-private for testing.
     */
    boolean wasDialogShownForQuarter(Quarter quarter) {
        return dialogsShownForQuarters.contains(quarter);
    }

    /**
     * Clears the dialog tracking state.
     * Package-private for testing.
     */
    void clearDialogTracking() {
        dialogsShownForQuarters.clear();
    }
}
