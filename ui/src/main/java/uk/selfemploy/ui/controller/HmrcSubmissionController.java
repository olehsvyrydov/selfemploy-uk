package uk.selfemploy.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.service.OAuthServiceFactory;

import java.math.BigDecimal;
import java.util.UUID;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the HMRC Submission hub page.
 * Provides navigation to Annual Submission, Quarterly Updates, and Submission History.
 */
public class HmrcSubmissionController implements Initializable, MainController.TaxYearAware {

    private static final Logger LOG = Logger.getLogger(HmrcSubmissionController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy");

    @FXML private HBox connectionStatus;
    @FXML private Label statusIcon;
    @FXML private Label statusTitle;
    @FXML private Label statusMessage;
    @FXML private Button connectButton;
    @FXML private Button testButton;

    @FXML private Label annualDeadlineLabel;
    @FXML private Label quarterlyStatusLabel;
    @FXML private Label historyCountLabel;

    // Feature cards for keyboard accessibility
    @FXML private VBox annualCard;
    @FXML private VBox quarterlyCard;
    @FXML private VBox historyCard;

    private TaxYear taxYear;
    private boolean isConnected = false;

    // Services for loading financial data
    private IncomeService incomeService;
    private ExpenseService expenseService;
    private UUID businessId;

    // Dialog stage reference for dynamic resizing
    private Stage dialogStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeServices();
        updateConnectionStatus();
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

    private void updateConnectionStatus() {
        if (statusIcon != null) {
            statusIcon.setText("●");
            statusIcon.setStyle(isConnected ? "-fx-text-fill: #28a745;" : "-fx-text-fill: #dc3545;");
        }
        if (statusTitle != null) {
            statusTitle.setText(isConnected ? "Connected to HMRC" : "Not Connected");
        }
        if (statusMessage != null) {
            statusMessage.setText(isConnected
                ? "Your account is linked and ready to submit"
                : "Click 'Connect' to authorize with HMRC via Government Gateway");
        }
        if (connectButton != null) {
            connectButton.setText(isConnected ? "Disconnect" : "Connect to HMRC");
        }
        if (connectionStatus != null) {
            connectionStatus.setStyle(isConnected
                ? "-fx-background-color: #d4edda; -fx-border-color: #28a745;"
                : "-fx-background-color: #fff3cd; -fx-border-color: #ffc107;");
        }
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
     * Returns whether the controller is connected to HMRC.
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Sets the connection status.
     * Package-private for testing.
     */
    void setConnected(boolean connected) {
        this.isConnected = connected;
        updateConnectionStatus();
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
    void handleTestConnection() {
        LOG.info("Testing HMRC API connection using open endpoint");

        if (testButton != null) {
            testButton.setDisable(true);
            testButton.setText("Testing...");
        }

        // Use HMRC's open "Hello World" endpoint to test connectivity
        String testUrl = "https://test-api.service.hmrc.gov.uk/hello/world";

        // Run in background thread to not block UI
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(testUrl))
                    .header("Accept", "application/vnd.hmrc.1.0+json")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (testButton != null) {
                        testButton.setDisable(false);
                        testButton.setText("Test API Connection");
                    }

                    if (response.statusCode() == 200) {
                        LOG.info("HMRC API test successful: " + response.body());
                        showSuccess("Connection Successful",
                            "Successfully connected to HMRC Sandbox API!\n\n" +
                            "Response: " + response.body() + "\n\n" +
                            "Your network can reach HMRC servers.");
                    } else {
                        LOG.warning("HMRC API test failed: HTTP " + response.statusCode());
                        showError("Connection Issue",
                            "Received HTTP " + response.statusCode() + " from HMRC.\n\n" +
                            "Response: " + response.body());
                    }
                });

            } catch (Exception e) {
                LOG.log(Level.WARNING, "HMRC API test failed", e);
                Platform.runLater(() -> {
                    if (testButton != null) {
                        testButton.setDisable(false);
                        testButton.setText("Test API Connection");
                    }
                    showError("Connection Failed",
                        "Could not reach HMRC servers:\n\n" + e.getMessage() +
                        "\n\nCheck your internet connection and firewall settings.");
                });
            }
        }).start();
    }

    @FXML
    void handleConnect() {
        if (isConnected) {
            // Disconnect
            isConnected = false;
            updateConnectionStatus();
            LOG.info("Disconnected from HMRC");
        } else {
            // Show connection dialog
            showOAuthDialog();
        }
    }

    private void showOAuthDialog() {
        // Check if credentials are configured
        if (!OAuthServiceFactory.isConfigured()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Configuration Required");
            alert.setHeaderText("HMRC Credentials Not Configured");
            alert.setContentText(
                "To connect to HMRC, you need to configure your credentials:\n\n" +
                "1. Create a .env file in the project root\n" +
                "2. Add your HMRC_CLIENT_ID and HMRC_CLIENT_SECRET\n" +
                "3. Restart the application\n\n" +
                "Get credentials from: https://developer.service.hmrc.gov.uk"
            );
            alert.showAndWait();
            return;
        }

        // Show info dialog
        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.setTitle("Connect to HMRC");
        infoAlert.setHeaderText("HMRC Authorization");
        infoAlert.setContentText(
            "A browser window will open for you to log in via Government Gateway.\n\n" +
            "After logging in, grant permission for this app to submit on your behalf.\n\n" +
            "Note: Using HMRC Sandbox for testing."
        );
        infoAlert.showAndWait();

        // Start OAuth flow
        LOG.info("Starting OAuth authentication flow");

        HmrcOAuthService oauthService;
        try {
            oauthService = OAuthServiceFactory.getOAuthService();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to initialize OAuth service", e);
            showError("Initialization Error",
                "Failed to initialize HMRC connection:\n\n" + e.getMessage());
            return;
        }

        // Disable connect button while authenticating
        if (connectButton != null) {
            connectButton.setDisable(true);
            connectButton.setText("Connecting...");
        }

        LOG.info("Calling OAuth authenticate()...");
        oauthService.authenticate()
            .thenAccept(tokens -> {
                LOG.info("OAuth authentication successful");
                Platform.runLater(() -> {
                    isConnected = true;
                    updateConnectionStatus();
                    showSuccess("Connected to HMRC",
                        "Your account has been successfully linked.\n" +
                        "You can now submit your Self Assessment.");
                });
            })
            .exceptionally(error -> {
                LOG.log(Level.WARNING, "OAuth authentication failed", error);
                Platform.runLater(() -> {
                    if (connectButton != null) {
                        connectButton.setDisable(false);
                    }
                    updateConnectionStatus();
                    showError("Connection Failed",
                        "Failed to connect to HMRC:\n\n" + error.getMessage() +
                        "\n\nPlease check your credentials and try again.");
                });
                return null;
            });
    }

    private void showSuccess(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
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
        showQuarterlyComingSoon();
    }

    @FXML
    void handleQuarterlySubmissionKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            showQuarterlyComingSoon();
            event.consume();
        }
    }

    /**
     * Shows improved quarterly updates placeholder with MTD guidance.
     * PS11-004: Architecture decision to DEFER quarterly updates feature.
     */
    private void showQuarterlyComingSoon() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Quarterly Updates");
        alert.setHeaderText("Feature Coming Q4 2025");
        alert.setContentText(
            "Quarterly MTD updates will be available in Q4 2025.\n\n" +
            "WHO NEEDS QUARTERLY UPDATES?\n" +
            "From April 2026, Making Tax Digital for Income Tax (MTD ITSA) will require\n" +
            "self-employed individuals with income over " + CURRENCY_SYMBOL + "50,000 to submit quarterly updates.\n\n" +
            "TIMELINE:\n" +
            "  - April 2026: Income > " + CURRENCY_SYMBOL + "50,000\n" +
            "  - April 2027: Income > " + CURRENCY_SYMBOL + "30,000\n" +
            "  - April 2028: Income > " + CURRENCY_SYMBOL + "20,000\n\n" +
            "WHAT THIS MEANS:\n" +
            "Instead of one annual return, you'll submit income and expense\n" +
            "summaries four times per year (quarterly).\n\n" +
            "MORE INFORMATION:\n" +
            "Visit gov.uk/guidance/check-if-youre-eligible-for-making-tax-digital-for-income-tax\n" +
            "to check your eligibility and register for MTD."
        );
        alert.getDialogPane().setMinWidth(550);
        alert.showAndWait();
    }

    /** Currency symbol for display */
    private static final String CURRENCY_SYMBOL = "\u00A3";

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
