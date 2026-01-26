package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.export.DataExportService;
import uk.selfemploy.core.export.DataImportService;
import uk.selfemploy.core.export.ExportResult;
import uk.selfemploy.core.export.ImportException;
import uk.selfemploy.core.export.ImportOptions;
import uk.selfemploy.core.export.ImportPreview;
import uk.selfemploy.core.export.ImportResult;
import uk.selfemploy.core.export.ImportType;
import uk.selfemploy.core.service.PrivacyAcknowledgmentService;
import uk.selfemploy.core.service.TermsAcceptanceService;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.service.UiDuplicateDetectionService;
import uk.selfemploy.ui.viewmodel.ImportAction;
import uk.selfemploy.ui.viewmodel.ImportCandidateViewModel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Settings page.
 * Provides access to user profile, legal documents, and data management.
 */
public class SettingsController implements Initializable, MainController.TaxYearAware {

    private static final Logger LOG = Logger.getLogger(SettingsController.class.getName());
    private static final List<String> SETTINGS_CATEGORIES = Arrays.asList("Profile", "Legal", "Data");

    // === FXML Injected Fields ===

    @FXML private Label utrLabel;
    @FXML private TextField utrField;
    @FXML private Button saveUtrButton;
    @FXML private Button exportButton;
    @FXML private Button importButton;
    @FXML private Button termsButton;
    @FXML private Button privacyButton;

    // === State ===

    private TaxYear taxYear;
    private String utr = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateUtrDisplay();
    }

    @Override
    public void setTaxYear(TaxYear taxYear) {
        this.taxYear = taxYear;
    }

    /**
     * Returns the current tax year.
     */
    public TaxYear getTaxYear() {
        return taxYear;
    }

    // === Settings Categories ===

    /**
     * Returns the list of settings categories.
     */
    public List<String> getSettingsCategories() {
        return SETTINGS_CATEGORIES;
    }

    // === Legal Documents ===

    /**
     * Returns true if Terms of Service can be shown.
     */
    public boolean canShowTermsOfService() {
        return true;
    }

    /**
     * Returns true if Privacy Notice can be shown.
     */
    public boolean canShowPrivacyNotice() {
        return true;
    }

    // === Data Management ===

    /**
     * Returns true if data export is supported.
     */
    public boolean canExportData() {
        return true;
    }

    /**
     * Returns true if data import is supported.
     */
    public boolean canImportData() {
        return true;
    }

    // === UTR Management ===

    /**
     * Returns the stored UTR.
     */
    public String getUtr() {
        return utr;
    }

    /**
     * Sets the UTR.
     */
    public void setUtr(String utr) {
        this.utr = utr != null ? utr : "";
    }

    /**
     * Validates if the given UTR format is valid.
     * UTR must be exactly 10 digits.
     */
    public boolean isValidUtr(String utr) {
        if (utr == null || utr.isEmpty()) {
            return false;
        }
        return utr.matches("\\d{10}");
    }

    /**
     * Returns the UTR formatted for display.
     * Formats as "12345 67890" or "Not set" if empty.
     */
    public String getFormattedUtr() {
        if (utr == null || utr.isEmpty()) {
            return "Not set";
        }
        if (utr.length() == 10) {
            return utr.substring(0, 5) + " " + utr.substring(5);
        }
        return utr;
    }

    // === Private Helper Methods ===

    private void updateUtrDisplay() {
        if (utrLabel != null) {
            utrLabel.setText(getFormattedUtr());
        }
    }

    // === FXML Event Handlers ===

    @FXML
    void handleSaveUtr(ActionEvent event) {
        if (utrField != null) {
            String newUtr = utrField.getText().replaceAll("\\s", "");
            if (isValidUtr(newUtr)) {
                setUtr(newUtr);
                updateUtrDisplay();
                showInfo("UTR Saved", "Your Unique Taxpayer Reference has been saved.");
            } else {
                showError("Invalid UTR", "Please enter a valid 10-digit UTR number.");
            }
        }
    }

    @FXML
    void handleShowTerms(ActionEvent event) {
        showLegalDocument("/fxml/terms-of-service.fxml", "Terms of Service", true);
    }

    @FXML
    void handleShowPrivacy(ActionEvent event) {
        showLegalDocument("/fxml/privacy-notice.fxml", "Privacy Notice", true);
    }

    @FXML
    void handleExportData(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Data");
        fileChooser.setInitialFileName("self-employment-data.json");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("JSON Files", "*.json"),
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = null;
        if (exportButton != null && exportButton.getScene() != null) {
            file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        }

        if (file != null) {
            try {
                DataExportService exportService = CoreServiceFactory.getDataExportService();
                UUID businessId = CoreServiceFactory.getDefaultBusinessId();
                TaxYear[] taxYears = taxYear != null
                    ? new TaxYear[]{taxYear}
                    : new TaxYear[]{TaxYear.current()};

                ExportResult result;
                String fileName = file.getName().toLowerCase();

                if (fileName.endsWith(".json")) {
                    result = exportService.exportToJson(businessId, taxYears, file.toPath());
                } else if (fileName.endsWith(".csv")) {
                    // For CSV, export income by default
                    result = exportService.exportIncomeToCsv(businessId, taxYears, file.toPath());
                } else {
                    showError("Export Error", "Unsupported file format. Please use .json or .csv");
                    return;
                }

                if (result.success()) {
                    showInfo("Export Successful",
                        String.format("Exported %d income and %d expense records to:\n%s",
                            result.incomeCount(), result.expenseCount(), file.getAbsolutePath()));
                } else {
                    showError("Export Failed", result.errorMessage());
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Export failed", e);
                showError("Export Error", "Failed to export data: " + e.getMessage());
            }
        }
    }

    @FXML
    void handleImportData(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Data");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("JSON Files", "*.json"),
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = null;
        if (importButton != null && importButton.getScene() != null) {
            file = fileChooser.showOpenDialog(importButton.getScene().getWindow());
        }

        if (file != null) {
            try {
                DataImportService importService = CoreServiceFactory.getDataImportService();
                UUID businessId = CoreServiceFactory.getDefaultBusinessId();
                Path filePath = file.toPath();
                String fileName = file.getName().toLowerCase();

                // Step 1: Parse and validate the file
                List<Income> importedIncomes;
                List<Expense> importedExpenses;

                if (fileName.endsWith(".json")) {
                    // Preview first for validation
                    ImportPreview preview = importService.previewJsonImport(filePath);
                    if (!preview.isValid() && preview.errors() != null && !preview.errors().isEmpty()) {
                        int maxErrors = Math.min(5, preview.errors().size());
                        showError("Import Validation Failed",
                            "Found validation errors:\n" + String.join("\n", preview.errors().subList(0, maxErrors)));
                        return;
                    }
                    // Parse the JSON to get income and expense records
                    var parsedData = importService.parseJsonFile(filePath);
                    importedIncomes = parsedData.incomes();
                    importedExpenses = parsedData.expenses();
                } else if (fileName.endsWith(".csv")) {
                    // For CSV, default to income (could add dialog to choose)
                    ImportPreview preview = importService.previewCsvImport(filePath, ImportType.INCOME);
                    if (!preview.isValid() && preview.errors() != null && !preview.errors().isEmpty()) {
                        int maxErrors = Math.min(5, preview.errors().size());
                        showError("Import Validation Failed",
                            "Found validation errors:\n" + String.join("\n", preview.errors().subList(0, maxErrors)));
                        return;
                    }
                    // For CSV, we only import incomes
                    importedIncomes = importService.parseCsvFile(filePath, ImportType.INCOME);
                    importedExpenses = List.of();
                } else {
                    showError("Import Error", "Unsupported file format. Please use .json or .csv");
                    return;
                }

                // Step 2: Run duplicate detection
                TaxYear currentTaxYear = taxYear != null ? taxYear : TaxYear.current();
                UiDuplicateDetectionService dupService = CoreServiceFactory.getDuplicateDetectionService();

                List<ImportCandidateViewModel> candidates = new java.util.ArrayList<>();
                candidates.addAll(dupService.analyzeIncomes(importedIncomes, currentTaxYear));
                candidates.addAll(dupService.analyzeExpenses(importedExpenses, currentTaxYear));

                if (candidates.isEmpty()) {
                    showInfo("Import Empty", "No records found in the file to import.");
                    return;
                }

                // Step 3: Show Import Review dialog
                showImportReviewDialog(candidates, importedIncomes, importedExpenses, businessId, filePath);

            } catch (ImportException e) {
                LOG.log(Level.WARNING, "Import validation failed", e);
                showError("Import Error", e.getMessage());
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Import failed", e);
                showError("Import Error", "Failed to import data: " + e.getMessage());
            }
        }
    }

    /**
     * Shows the Import Review dialog for user to review duplicate detection results.
     * BUG-10B-002: Integration fix - Settings import now uses Import Review UI.
     */
    private void showImportReviewDialog(List<ImportCandidateViewModel> candidates,
                                         List<Income> importedIncomes,
                                         List<Expense> importedExpenses,
                                         UUID businessId,
                                         Path filePath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/import-review.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Import Review - UK Self-Employment Manager");
            stage.initModality(Modality.APPLICATION_MODAL);

            ImportReviewController controller = loader.getController();
            controller.setCandidates(candidates);

            // Hide the "Back to Mapping" button since there's no mapping step in Settings import
            controller.hideBackButton();

            // Set callbacks for import completion
            controller.setOnImportComplete(approvedCandidates -> {
                executeApprovedImport(approvedCandidates, importedIncomes, importedExpenses, businessId);
                stage.close();
            });

            controller.setOnCancel(() -> {
                LOG.info("Import cancelled by user");
                stage.close();
            });

            // Set dialog size
            double width = 1000;
            double height = 700;

            Scene scene = new Scene(root, width, height);
            var mainCss = getClass().getResource("/css/main.css");
            var importReviewCss = getClass().getResource("/css/import-review.css");
            var bankImportCss = getClass().getResource("/css/bank-import.css");
            if (mainCss != null) {
                scene.getStylesheets().add(mainCss.toExternalForm());
            }
            if (importReviewCss != null) {
                scene.getStylesheets().add(importReviewCss.toExternalForm());
            }
            if (bankImportCss != null) {
                scene.getStylesheets().add(bankImportCss.toExternalForm());
            }

            stage.setScene(scene);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.setResizable(true);
            stage.showAndWait();

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to load Import Review dialog", e);
            showError("Error", "Failed to open Import Review: " + e.getMessage());
        }
    }

    /**
     * Executes the import for approved candidates only.
     * BUG-10B-002: Only imports records that user approved in the review.
     */
    private void executeApprovedImport(List<ImportCandidateViewModel> approvedCandidates,
                                        List<Income> importedIncomes,
                                        List<Expense> importedExpenses,
                                        UUID businessId) {
        try {
            var incomeService = CoreServiceFactory.getIncomeService();
            var expenseService = CoreServiceFactory.getExpenseService();

            // Build maps for quick lookup
            Map<UUID, Income> incomeMap = new java.util.HashMap<>();
            for (Income income : importedIncomes) {
                incomeMap.put(income.id(), income);
            }
            Map<UUID, Expense> expenseMap = new java.util.HashMap<>();
            for (Expense expense : importedExpenses) {
                expenseMap.put(expense.id(), expense);
            }

            int imported = 0;
            int skipped = 0;
            int updated = 0;

            for (ImportCandidateViewModel candidate : approvedCandidates) {
                ImportAction action = candidate.getAction();

                if (action == ImportAction.SKIP) {
                    skipped++;
                    continue;
                }

                if (candidate.isIncome()) {
                    Income income = incomeMap.get(candidate.getId());
                    if (income != null) {
                        if (action == ImportAction.IMPORT) {
                            incomeService.create(businessId, income.date(), income.amount(),
                                income.description(), income.category(), income.reference());
                            imported++;
                        } else if (action == ImportAction.UPDATE && candidate.getMatchedRecordId() != null) {
                            // Update existing record
                            incomeService.update(candidate.getMatchedRecordId(), income.date(), income.amount(),
                                income.description(), income.category(), income.reference());
                            updated++;
                        }
                    }
                } else {
                    Expense expense = expenseMap.get(candidate.getId());
                    if (expense != null) {
                        if (action == ImportAction.IMPORT) {
                            expenseService.create(businessId, expense.date(), expense.amount(),
                                expense.description(), expense.category(), expense.receiptPath(), expense.notes());
                            imported++;
                        } else if (action == ImportAction.UPDATE && candidate.getMatchedRecordId() != null) {
                            // Update existing record
                            expenseService.update(candidate.getMatchedRecordId(), expense.date(), expense.amount(),
                                expense.description(), expense.category(), expense.receiptPath(), expense.notes());
                            updated++;
                        }
                    }
                }
            }

            LOG.info("Import completed: " + imported + " imported, " + updated + " updated, " + skipped + " skipped");
            showInfo("Import Successful",
                String.format("Import completed successfully.\nImported: %d\nUpdated: %d\nSkipped: %d",
                    imported, updated, skipped));

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to execute approved import", e);
            showError("Import Error", "Failed to import approved records: " + e.getMessage());
        }
    }

    private void showLegalDocument(String fxmlPath, String title, boolean settingsMode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title + " - UK Self-Employment Manager");
            stage.initModality(Modality.APPLICATION_MODAL);
            // stage.initStyle(StageStyle.UNDECORATED); // No system title bar - dialog has its own header with close button

            // Initialize the controller with required dependencies and set settings mode
            Object controller = loader.getController();

            // Determine dialog size based on document type
            double width;
            double height;

            if (controller instanceof TermsOfServiceController tos) {
                // Initialize with service and set settings mode
                TermsAcceptanceService termsService = CoreServiceFactory.getTermsAcceptanceService();
                tos.initializeWithDependencies(termsService);
                tos.setDialogStage(stage);
                tos.setSettingsMode(settingsMode);
                // Terms of Service needs wide view for table of contents + content (nearly full screen)
                width = 1200;
                height = 1200;
            } else if (controller instanceof PrivacyNoticeController privacy) {
                // Initialize with service and set settings mode
                PrivacyAcknowledgmentService privacyService = CoreServiceFactory.getPrivacyAcknowledgmentService();
                privacy.initializeWithDependencies(privacyService);
                privacy.setDialogStage(stage);
                privacy.setSettingsMode(settingsMode);
                // Privacy Notice modal dialog - matching app height
                width = 550;
                height = 1200;
            } else {
                // Default fallback
                width = 650;
                height = 750;
            }

            Scene scene = new Scene(root, width, height);
            // Load stylesheets
            var mainCss = getClass().getResource("/css/main.css");
            var legalCss = getClass().getResource("/css/legal.css");
            if (mainCss != null) {
                scene.getStylesheets().add(mainCss.toExternalForm());
            }
            if (legalCss != null) {
                scene.getStylesheets().add(legalCss.toExternalForm());
            }

            stage.setScene(scene);

            // Set minimum size to prevent dialog from becoming too small
            stage.setMinWidth(width * 0.8);
            stage.setMinHeight(height * 0.8);

            // Allow resizing
            stage.setResizable(true);

            stage.showAndWait();

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to load legal document: " + fxmlPath, e);
            showError("Error", "Failed to open " + title + ": " + e.getMessage());
        }
    }

    private void showInfo(String title, String content) {
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
}
