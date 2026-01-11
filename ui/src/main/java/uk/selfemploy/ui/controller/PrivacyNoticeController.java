package uk.selfemploy.ui.controller;

import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import uk.selfemploy.core.service.PrivacyAcknowledgmentService;
import uk.selfemploy.ui.viewmodel.PrivacyNoticeViewModel;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Privacy Notice dialog.
 * Manages form bindings, acknowledgment flow, and dialog actions.
 *
 * SE-507: Privacy Notice UI
 *
 * Supports two modes:
 * 1. First Launch Mode - User must acknowledge before using the app
 * 2. Settings Mode - User can view the privacy notice from settings (close button visible)
 */
public class PrivacyNoticeController implements Initializable {

    // Header
    @FXML private Button closeBtn;

    // Version info
    @FXML private Label versionLabel;
    @FXML private Label effectiveDateLabel;

    // Content
    @FXML private ScrollPane contentScroll;
    @FXML private Hyperlink fullPolicyLink;

    // Acknowledgment
    @FXML private CheckBox acknowledgeCheckbox;

    // Footer
    @FXML private Button continueBtn;

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

        // Bind continue button state
        continueBtn.disableProperty().bind(viewModel.continueEnabledProperty().not());

        // Bind close button visibility
        closeBtn.visibleProperty().bind(viewModel.closeButtonVisibleProperty());
        closeBtn.managedProperty().bind(viewModel.closeButtonVisibleProperty());
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

        viewModel.setOnOpenBrowserCallback(url -> {
            if (hostServices != null) {
                hostServices.showDocument(url);
            }
        });
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
