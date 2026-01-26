package uk.selfemploy.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.viewmodel.QuarterStatus;
import uk.selfemploy.ui.viewmodel.QuarterViewModel;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Controller for the Quarterly Updates dashboard.
 * Sprint 10D: SE-10D-001, SE-10D-002, SE-10D-003
 *
 * <p>Displays all 4 quarters for the current tax year with their status,
 * financial totals, and submission deadlines.</p>
 */
public class QuarterlyUpdatesController implements Initializable, MainController.TaxYearAware {

    private static final Logger LOG = Logger.getLogger(QuarterlyUpdatesController.class.getName());

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOG.info("QuarterlyUpdatesController.initialize()");
        initializeServices();
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

        // Update current quarter label
        if (currentQuarterLabel != null) {
            Quarter current = Quarter.forDate(LocalDate.now(clock));
            if (current != null) {
                currentQuarterLabel.setText("Current Quarter: " + current.name());
            }
        }

        // Update quarter cards
        // This would be implemented when FXML is created
        LOG.info("UI updated with " + quarterViewModels.size() + " quarters");
    }

    /**
     * Sets the callback for the back button.
     */
    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
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
}
