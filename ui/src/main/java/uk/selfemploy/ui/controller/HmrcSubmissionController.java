package uk.selfemploy.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.service.CoreServiceFactory;

import java.math.BigDecimal;
import java.util.UUID;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the HMRC Submission hub page.
 * Provides navigation to Annual Submission, Quarterly Updates, and Submission History.
 *
 * <p>Sprint 13: Connection management removed from this page.
 * OAuth authentication now happens automatically during submission via AutoOAuthSubmissionService.
 * NINO setup is done in Settings page.</p>
 */
public class HmrcSubmissionController implements Initializable, MainController.TaxYearAware {

    private static final Logger LOG = Logger.getLogger(HmrcSubmissionController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy");

    @FXML private Label annualDeadlineLabel;
    @FXML private Label quarterlyStatusLabel;
    @FXML private Label historyCountLabel;

    // Feature cards for keyboard accessibility
    @FXML private VBox annualCard;
    @FXML private VBox quarterlyCard;
    @FXML private VBox historyCard;

    private TaxYear taxYear;
    private Runnable navigateToSettings;

    // Services for loading financial data
    private IncomeService incomeService;
    private ExpenseService expenseService;
    private UUID businessId;

    // Dialog stage reference for dynamic resizing
    private Stage dialogStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeServices();
        updateDeadlines();
    }

    /**
     * Initializes services for loading financial data.
     * Uses CoreServiceFactory for standalone UI mode.
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

    @Override
    public void setTaxYear(TaxYear taxYear) {
        this.taxYear = taxYear;
        updateDeadlines();
    }

    /**
     * Sets a callback to navigate to the Settings page.
     *
     * <p>SE-10E-003: Passed through to QuarterlyUpdatesController for
     * the "Open Settings" button in submission error dialogs.</p>
     *
     * @param navigateToSettings callback to navigate to Settings
     */
    public void setNavigateToSettings(Runnable navigateToSettings) {
        this.navigateToSettings = navigateToSettings;
    }

    private void updateDeadlines() {
        if (taxYear != null) {
            // Annual deadline is 31 January after tax year ends
            if (annualDeadlineLabel != null) {
                annualDeadlineLabel.setText(getFormattedAnnualDeadline());
            }

            // Determine current quarter
            if (quarterlyStatusLabel != null) {
                quarterlyStatusLabel.setText(determineQuarterStatus(LocalDate.now(), taxYear));
            }
        }

        // Submission history count - would be loaded from service
        if (historyCountLabel != null) {
            historyCountLabel.setText("View your submission history");
        }
    }

    /**
     * Returns the formatted annual deadline text.
     */
    public String getFormattedAnnualDeadline() {
        if (taxYear == null) {
            return "";
        }
        return "Deadline: " + taxYear.onlineFilingDeadline().format(DATE_FORMAT);
    }

    /**
     * Returns the current tax year.
     */
    public TaxYear getTaxYear() {
        return taxYear;
    }

    /**
     * Sets the dialog stage for dynamic resizing.
     * Package-private for testing.
     *
     * @param stage the dialog stage
     */
    void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * Initializes dependencies for testing.
     * Package-private for testing.
     *
     * @param incomeService the income service
     * @param expenseService the expense service
     * @param businessId the business ID
     */
    void initializeWithDependencies(IncomeService incomeService, ExpenseService expenseService, UUID businessId) {
        this.incomeService = incomeService;
        this.expenseService = expenseService;
        this.businessId = businessId;
    }

    /**
     * Initializes the Annual Submission controller with financial data.
     * Package-private for testing.
     *
     * @param controller The AnnualSubmissionController to initialize
     * @param taxYear The tax year for submission
     * @return The calculated net profit
     */
    BigDecimal initializeAnnualSubmissionForTest(AnnualSubmissionController controller, TaxYear taxYear) {
        this.taxYear = taxYear;
        initializeServices();

        BigDecimal totalIncome = incomeService.getTotalByTaxYear(businessId, taxYear);
        BigDecimal totalExpenses = expenseService.getDeductibleTotal(businessId, taxYear);
        BigDecimal netProfit = totalIncome.subtract(totalExpenses);

        if (controller != null) {
            controller.initializeSubmission(taxYear, totalIncome, totalExpenses, netProfit);
        }

        return netProfit;
    }

    /**
     * Gets the income service.
     * Package-private for testing.
     */
    IncomeService getIncomeService() {
        initializeServices();
        return incomeService;
    }

    /**
     * Gets the expense service.
     * Package-private for testing.
     */
    ExpenseService getExpenseService() {
        initializeServices();
        return expenseService;
    }

    /**
     * Gets the business ID.
     * Package-private for testing.
     */
    UUID getBusinessId() {
        initializeServices();
        return businessId;
    }

    /**
     * Determines the MTD quarter status based on the given date.
     * Package-private for testing.
     */
    String determineQuarterStatus(LocalDate now, TaxYear year) {
        // MTD quarters: Q1 (Apr-Jun), Q2 (Jul-Sep), Q3 (Oct-Dec), Q4 (Jan-Mar)
        int month = now.getMonthValue();
        if (month >= 4 && month <= 6) {
            return "Current: Q1 (Apr-Jun) - Due by 7 Aug";
        } else if (month >= 7 && month <= 9) {
            return "Current: Q2 (Jul-Sep) - Due by 7 Nov";
        } else if (month >= 10 && month <= 12) {
            return "Current: Q3 (Oct-Dec) - Due by 7 Feb";
        } else {
            return "Current: Q4 (Jan-Mar) - Due by 7 May";
        }
    }

    @FXML
    void handleAnnualSubmission(MouseEvent event) {
        openAnnualSubmission();
    }

    @FXML
    void handleAnnualSubmissionKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            openAnnualSubmission();
            event.consume();
        }
    }

    private void openAnnualSubmission() {
        LOG.info("Opening Annual Submission view");
        openSubmissionView("/fxml/annual-submission.fxml", "Annual Self Assessment");
    }

    @FXML
    void handleQuarterlySubmission(MouseEvent event) {
        openQuarterlyUpdates();
    }

    @FXML
    void handleQuarterlySubmissionKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            openQuarterlyUpdates();
            event.consume();
        }
    }

    /**
     * Opens the Quarterly Updates dashboard.
     * Sprint 10D: SE-10D-001, SE-10D-002, SE-10D-003
     */
    private void openQuarterlyUpdates() {
        LOG.info("Opening Quarterly Updates view");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/quarterly-updates.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Quarterly Updates - UK Self-Employment Manager");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(true);
            stage.setMinWidth(700);
            stage.setMinHeight(700);
            stage.setWidth(900);
            stage.setHeight(800);

            // Pass tax year and back callback to controller
            Object controller = loader.getController();
            if (controller instanceof MainController.TaxYearAware && taxYear != null) {
                ((MainController.TaxYearAware) controller).setTaxYear(taxYear);
            }
            if (controller instanceof QuarterlyUpdatesController quarterlyController) {
                quarterlyController.setOnBack(stage::close);
                // SE-10E-003: Wire "Open Settings" - close this modal then navigate to settings
                if (navigateToSettings != null) {
                    quarterlyController.setNavigateToSettings(() -> {
                        stage.close();
                        navigateToSettings.run();
                    });
                }
            }

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/quarterly-updates.css").toExternalForm());

            stage.setScene(scene);
            stage.showAndWait();

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to load Quarterly Updates view", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open Quarterly Updates");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    void handleSubmissionHistory(MouseEvent event) {
        openSubmissionHistory();
    }

    @FXML
    void handleSubmissionHistoryKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            openSubmissionHistory();
            event.consume();
        }
    }

    private void openSubmissionHistory() {
        LOG.info("Opening Submission History view");
        openSubmissionView("/fxml/submission-history.fxml", "Submission History");
    }

    private void openSubmissionView(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Pass tax year if controller supports it
            Object controller = loader.getController();
            if (controller instanceof MainController.TaxYearAware && taxYear != null) {
                ((MainController.TaxYearAware) controller).setTaxYear(taxYear);
            }

            Stage stage = new Stage();
            stage.setTitle(title + " - UK Self-Employment Manager");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(true);

            // Dynamic width: Annual Submission starts at 700px (step 1), others at 1200px
            int initialWidth = (controller instanceof AnnualSubmissionController) ? 700 : 1200;

            // Set stage size constraints BEFORE setting scene
            stage.setMinWidth(700);
            stage.setMinHeight(750);
            stage.setWidth(initialWidth);
            stage.setHeight(950);

            Scene scene = new Scene(root);
            // Load stylesheets
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/annual-submission.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/submission-history.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/legal.css").toExternalForm());

            stage.setScene(scene);

            // Initialize Annual Submission controller AFTER scene is set
            if (controller instanceof AnnualSubmissionController annualController && taxYear != null) {
                annualController.setDialogStage(stage);
                initializeAnnualSubmission(annualController);
            }

            stage.showAndWait();

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to load view: " + fxmlPath, e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open " + title);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Initializes the Annual Submission controller with financial data from services.
     *
     * @param controller The AnnualSubmissionController to initialize
     */
    private void initializeAnnualSubmission(AnnualSubmissionController controller) {
        // Ensure services are initialized
        initializeServices();

        // Calculate totals for the tax year using service methods
        BigDecimal totalIncome = incomeService.getTotalByTaxYear(businessId, taxYear);
        BigDecimal totalExpenses = expenseService.getDeductibleTotal(businessId, taxYear);
        BigDecimal netProfit = totalIncome.subtract(totalExpenses);

        LOG.info("Initializing Annual Submission for " + taxYear.label() +
                 ": Income=" + totalIncome + ", Expenses=" + totalExpenses + ", NetProfit=" + netProfit);

        // Initialize the submission with financial data
        controller.initializeSubmission(taxYear, totalIncome, totalExpenses, netProfit);
    }
}
