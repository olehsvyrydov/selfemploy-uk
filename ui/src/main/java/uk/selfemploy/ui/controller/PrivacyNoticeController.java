package uk.selfemploy.ui.controller;

import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.core.service.PrivacyAcknowledgmentService;
import uk.selfemploy.ui.component.ToastNotification;
import uk.selfemploy.ui.viewmodel.PrivacyNoticeViewModel;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Privacy Notice dialog.
 * Manages form bindings, acknowledgment flow, and dialog actions.
 *
 * SE-507: Privacy Notice UI
 * PS11-001: Privacy Notice UX Fixes
 *
 * Supports two modes:
 * 1. First Launch Mode - User must acknowledge before using the app
 * 2. Settings Mode - User can view the privacy notice from settings (close button visible)
 */
public class PrivacyNoticeController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(PrivacyNoticeController.class);

    // Header (Settings mode close button)
    @FXML private HBox headerRow;
    @FXML private Button headerCloseBtn;

    // Version info
    @FXML private Label versionLabel;
    @FXML private Label effectiveDateLabel;

    // Content
    @FXML private ScrollPane contentScroll;
    @FXML private VBox privacyContent;
    @FXML private Hyperlink fullPolicyLink;

    // Acknowledgment
    @FXML private CheckBox acknowledgeCheckbox;
    @FXML private Label acknowledgmentLabel;

    // Footer (mode-aware)
    @FXML private HBox firstLaunchFooter;
    @FXML private HBox settingsFooter;
    @FXML private Button continueBtn;
    @FXML private Button closeBtn;
    @FXML private Button acknowledgeBtn;

    private PrivacyNoticeViewModel viewModel;
    private Stage dialogStage;
    private HostServices hostServices;

    // Callbacks
    private Runnable onAcknowledgedCallback;
    private Runnable onCloseCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initial setup - bindings will be set up after dependencies are injected
    }

    /**
     * Initializes the controller with required dependencies.
     *
     * @param acknowledgmentService The service for managing acknowledgments
     */
    public void initializeWithDependencies(PrivacyAcknowledgmentService acknowledgmentService) {
        viewModel = new PrivacyNoticeViewModel(acknowledgmentService);
        setupBindings();
        setupCallbacks();
        updateVersionInfo();
    }

    /**
     * Sets the host services for opening external URLs.
     *
     * @param hostServices The JavaFX HostServices instance
     */
    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    private void setupBindings() {
        if (viewModel == null) return;

        // Bind checkbox to ViewModel
        acknowledgeCheckbox.selectedProperty().bindBidirectional(viewModel.acknowledgedProperty());

        // Bind continue button state (first launch mode)
        if (continueBtn != null) {
            continueBtn.disableProperty().bind(viewModel.continueEnabledProperty().not());
        }

        // Bind acknowledge button state (settings mode)
        if (acknowledgeBtn != null) {
            acknowledgeBtn.disableProperty().bind(viewModel.continueEnabledProperty().not());
        }

        // Update acknowledgment label text
        if (acknowledgmentLabel != null) {
            acknowledgmentLabel.setText(viewModel.getAcknowledgmentLabelText());
        }
    }

    private void setupCallbacks() {
        if (viewModel == null) return;

        // Set up ViewModel callbacks
        viewModel.setOnAcknowledgedCallback(() -> {
            if (onAcknowledgedCallback != null) {
                onAcknowledgedCallback.run();
            }
            closeDialog();
        });

        viewModel.setOnCloseCallback(() -> {
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
            closeDialog();
        });

        // Browser callback with toast notification and error handling (PS11-001)
        viewModel.setOnOpenBrowserCallback(url -> {
            openExternalUrl(url);
        });
    }

    /**
     * Opens a URL in the external browser with toast notification.
     * If browser opening fails, copies URL to clipboard and shows error toast.
     *
     * @param url The URL to open
     */
    private void openExternalUrl(String url) {
        try {
            if (hostServices != null) {
                hostServices.showDocument(url);
                // Show success toast
                ToastNotification.showExternalBrowserToast(
                        viewModel.getBrowserOpenedToastMessage(),
                        url
                );
                LOG.info("Opened privacy policy in browser: {}", url);
            } else {
                // No host services available - copy to clipboard as fallback
                copyUrlToClipboard(url);
                ToastNotification.showExternalBrowserToast(
                        viewModel.getBrowserOpenFailedToastMessage(),
                        url
                );
                LOG.warn("HostServices not available, copied URL to clipboard: {}", url);
            }
        } catch (Exception e) {
            LOG.error("Failed to open browser for URL: {}", url, e);
            // Copy to clipboard as fallback
            copyUrlToClipboard(url);
            ToastNotification.showExternalBrowserToast(
                    viewModel.getBrowserOpenFailedToastMessage(),
                    url
            );
        }
    }

    /**
     * Copies the URL to the system clipboard.
     */
    private void copyUrlToClipboard(String url) {
        try {
            ClipboardContent content = new ClipboardContent();
            content.putString(url);
            Clipboard.getSystemClipboard().setContent(content);
            LOG.debug("URL copied to clipboard: {}", url);
        } catch (Exception e) {
            LOG.error("Failed to copy URL to clipboard: {}", url, e);
        }
    }

    private void updateVersionInfo() {
        if (viewModel == null) return;

        versionLabel.setText("Version " + viewModel.getPrivacyVersion());
        effectiveDateLabel.setText("Effective: " + viewModel.getEffectiveDate());
    }

    // === Public Methods ===

    /**
     * Sets the dialog stage reference.
     *
     * @param stage The dialog Stage
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * Sets the controller to settings mode (shows close button, allows closing without acknowledgment).
     *
     * @param settingsMode true for settings access, false for first launch
     */
    public void setSettingsMode(boolean settingsMode) {
        if (viewModel != null) {
            viewModel.setSettingsMode(settingsMode);
        }
        updateModeVisibility(settingsMode);
    }

    /**
     * Updates UI visibility based on mode.
     * First Launch Mode: Single "Continue" button, no close button
     * Settings Mode: "Close" + "Acknowledge" buttons, X close button in header
     */
    private void updateModeVisibility(boolean settingsMode) {
        // Header close button (Settings mode only)
        if (headerCloseBtn != null) {
            headerCloseBtn.setVisible(settingsMode);
            headerCloseBtn.setManaged(settingsMode);
        }

        // Footer mode - First Launch
        if (firstLaunchFooter != null) {
            firstLaunchFooter.setVisible(!settingsMode);
            firstLaunchFooter.setManaged(!settingsMode);
        }

        // Footer mode - Settings
        if (settingsFooter != null) {
            settingsFooter.setVisible(settingsMode);
            settingsFooter.setManaged(settingsMode);
        }
    }

    /**
     * Sets the callback for successful acknowledgment.
     *
     * @param callback The callback to run after acknowledgment
     */
    public void setOnAcknowledgedCallback(Runnable callback) {
        this.onAcknowledgedCallback = callback;
    }

    /**
     * Sets the callback for dialog close (settings mode only).
     *
     * @param callback The callback to run when closed
     */
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    /**
     * Checks if the user needs to acknowledge the privacy notice.
     *
     * @return true if acknowledgment is required, false otherwise
     */
    public boolean requiresAcknowledgment() {
        return viewModel != null && viewModel.requiresAcknowledgment();
    }

    // === Action Handlers ===

    @FXML
    void handleAcknowledgeChange(ActionEvent event) {
        // The binding handles this automatically
    }

    @FXML
    void handleContinue(ActionEvent event) {
        if (viewModel != null) {
            viewModel.handleContinue();
        }
    }

    @FXML
    void handleClose(ActionEvent event) {
        if (viewModel != null) {
            viewModel.handleClose();
        }
    }

    @FXML
    void handleAcknowledge(ActionEvent event) {
        // Same as continue - saves acknowledgment (settings mode button)
        if (viewModel != null) {
            viewModel.handleContinue();
        }
    }

    @FXML
    void handleOpenFullPolicy(ActionEvent event) {
        if (viewModel != null) {
            viewModel.handleOpenFullPolicy();
        }
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Returns the ViewModel for testing.
     *
     * @return The PrivacyNoticeViewModel instance
     */
    public PrivacyNoticeViewModel getViewModel() {
        return viewModel;
    }
}
