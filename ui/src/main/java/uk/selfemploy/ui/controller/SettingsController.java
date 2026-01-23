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
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.util.BrowserUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Settings page.
 * Provides access to user profile, legal documents, and data management.
 */
public class SettingsController implements Initializable, MainController.TaxYearAware {

    private static final Logger LOG = Logger.getLogger(SettingsController.class.getName());
    private static final String APPLICATION_VERSION = "0.1.0-SNAPSHOT";
    private static final List<String> SETTINGS_CATEGORIES = Arrays.asList("Profile", "Legal", "Data", "About");

    // === FXML Injected Fields ===

    @FXML private Label versionLabel;
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
        updateDisplay();
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

    // === Application Version ===

    /**
     * Returns the application version.
     */
    public String getApplicationVersion() {
        return APPLICATION_VERSION;
    }

    /**
     * Returns the formatted version string for display.
     */
    public String getFormattedVersion() {
        return "Version " + APPLICATION_VERSION;
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

    private void updateDisplay() {
        if (versionLabel != null) {
            versionLabel.setText(getFormattedVersion());
        }
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
                updateDisplay();
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
            // TODO: Implement actual data export
            showInfo("Export", "Data export functionality will be available soon.\n\nFile would be saved to:\n" + file.getAbsolutePath());
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
            // TODO: Implement actual data import
            showInfo("Import", "Data import functionality will be available soon.\n\nSelected file:\n" + file.getAbsolutePath());
        }
    }

    @FXML
    void handleGitHubLink(ActionEvent event) {
        LOG.info("Opening GitHub Repository link");
        String url = getConfiguredUrl("app.url.github");
        if (url != null) {
            openExternalLink(url);
        } else {
            showInfo("GitHub Repository", "Repository URL not configured.\n\nThis will be available in a future release.");
        }
    }

    @FXML
    void handleReportIssueLink(ActionEvent event) {
        LOG.info("Opening Report Issue link");
        String url = getConfiguredUrl("app.url.issues");
        if (url != null) {
            openExternalLink(url);
        } else {
            showInfo("Report an Issue", "Issue tracker URL not configured.\n\nThis will be available in a future release.");
        }
    }

    /**
     * Gets a configured URL from system properties or environment.
     * Returns null if not configured.
     */
    String getConfiguredUrl(String key) {
        // Check system property first, then environment variable
        String url = System.getProperty(key);
        if (url == null || url.isEmpty()) {
            url = System.getenv(key.replace('.', '_').toUpperCase());
        }
        return (url != null && !url.isEmpty()) ? url : null;
    }

    /**
     * Opens an external URL in the system default browser.
     *
     * <p>Uses BrowserUtil to open URLs safely on a background thread,
     * avoiding crashes from Desktop.browse() on the JavaFX Application Thread.</p>
     */
    void openExternalLink(String url) {
        LOG.info("Opening external link: " + url);
        BrowserUtil.openUrl(url, error -> {
            LOG.log(Level.WARNING, "Failed to open URL: " + url + " - " + error);
            // Show error on JavaFX thread
            javafx.application.Platform.runLater(() ->
                    showError("Error", "Could not open link: " + error));
        });
    }

    private void showLegalDocument(String fxmlPath, String title, boolean settingsMode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Set settings mode on the controller if supported
            Object controller = loader.getController();
            if (controller instanceof TermsOfServiceController tos) {
                tos.setSettingsMode(settingsMode);
            } else if (controller instanceof PrivacyNoticeController privacy) {
                privacy.setSettingsMode(settingsMode);
            }

            Stage stage = new Stage();
            stage.setTitle(title + " - UK Self-Employment Manager");
            stage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root, 650, 750);
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
