package uk.selfemploy.ui.controller;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import uk.selfemploy.core.service.TermsAcceptanceService;
import uk.selfemploy.ui.viewmodel.TermsOfServiceViewModel;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Terms of Service dialog.
 * Manages scroll tracking, form bindings, acceptance flow, and dialog actions.
 *
 * SE-508: Terms of Service UI
 *
 * Supports two modes:
 * 1. First Launch Mode - User must scroll through and accept ToS before using the app
 * 2. Settings Mode - User can view the ToS from Settings > Legal (view-only)
 */
public class TermsOfServiceController implements Initializable {

    // Version info
    @FXML private Label versionLabel;
    @FXML private Label lastUpdatedLabel;

    // Content
    @FXML private ScrollPane contentScroll;
    @FXML private VBox tosContent;

    // Scroll progress
    @FXML private ProgressBar scrollProgress;
    @FXML private Label scrollPercentLabel;
    @FXML private Label scrollHint;

    // Footer buttons
    @FXML private Button printBtn;
    @FXML private Button declineBtn;
    @FXML private Button acceptBtn;

    private TermsOfServiceViewModel viewModel;
    private Stage dialogStage;
    private HostServices hostServices;

    // Callbacks
    private Runnable onAcceptedCallback;
    private Runnable onDeclinedCallback;
    private Runnable onCloseCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initial setup - bindings will be set up after dependencies are injected
    }

    /**
     * Initializes the controller with required dependencies.
     *
     * @param acceptanceService The service for managing ToS acceptances
     */
    public void initializeWithDependencies(TermsAcceptanceService acceptanceService) {
        viewModel = new TermsOfServiceViewModel(acceptanceService);
        setupBindings();
        setupCallbacks();
        setupScrollListener();
        updateVersionInfo();
    }

    /**
     * Sets the host services for printing and other operations.
     *
     * @param hostServices The JavaFX HostServices instance
     */
    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    private void setupBindings() {
        if (viewModel == null) return;

        // Bind scroll progress bar
        scrollProgress.progressProperty().bind(viewModel.scrollProgressProperty());

        // Bind scroll percentage label
        scrollPercentLabel.textProperty().bind(viewModel.scrollPercentageProperty());

        // Bind scroll hint text
        scrollHint.textProperty().bind(viewModel.scrollHintTextProperty());

        // Bind accept button state
        acceptBtn.disableProperty().bind(viewModel.acceptEnabledProperty().not());

        // Bind action buttons visibility (Accept/Decline)
        acceptBtn.visibleProperty().bind(viewModel.actionButtonsVisibleProperty());
        acceptBtn.managedProperty().bind(viewModel.actionButtonsVisibleProperty());
        declineBtn.visibleProperty().bind(viewModel.actionButtonsVisibleProperty());
        declineBtn.managedProperty().bind(viewModel.actionButtonsVisibleProperty());

        // Add style class when scrolled to bottom
        viewModel.scrolledToBottomProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                scrollProgress.getStyleClass().add("complete");
                scrollHint.getStyleClass().add("complete");
            }
        });
    }

    private void setupCallbacks() {
        if (viewModel == null) return;

        // Set up ViewModel callbacks
        viewModel.setOnAcceptedCallback(() -> {
            if (onAcceptedCallback != null) {
                onAcceptedCallback.run();
            }
            closeDialog();
        });

        viewModel.setOnDeclinedCallback(this::showDeclineConfirmation);

        viewModel.setOnCloseCallback(() -> {
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
            closeDialog();
        });

        viewModel.setOnPrintCallback(this::handlePrintAction);

        viewModel.setOnScrollToSectionCallback(this::scrollToSection);
    }

    private void setupScrollListener() {
        if (contentScroll == null) return;

        // Listen to scroll position changes
        contentScroll.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (viewModel != null) {
                viewModel.setScrollProgress(newVal.doubleValue());
            }
        });

        // Ensure scroll bar updates after content is laid out
        Platform.runLater(() -> {
            if (contentScroll != null) {
                contentScroll.setVvalue(0);
            }
        });
    }

    private void updateVersionInfo() {
        if (viewModel == null) return;

        versionLabel.setText("Version " + viewModel.getTosVersion());
        lastUpdatedLabel.setText("Last Updated: " + viewModel.getLastUpdatedDate());
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
     * Sets the controller to settings mode (shows close button, view-only).
     *
     * @param settingsMode true for settings access, false for first launch
     */
    public void setSettingsMode(boolean settingsMode) {
        if (viewModel != null) {
            viewModel.setSettingsMode(settingsMode);
        }
    }

    /**
     * Sets the callback for successful acceptance.
     *
     * @param callback The callback to run after acceptance
     */
    public void setOnAcceptedCallback(Runnable callback) {
        this.onAcceptedCallback = callback;
    }

    /**
     * Sets the callback for decline action.
     *
     * @param callback The callback to run when declined
     */
    public void setOnDeclinedCallback(Runnable callback) {
        this.onDeclinedCallback = callback;
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
     * Checks if the user needs to accept the Terms of Service.
     *
     * @return true if acceptance is required, false otherwise
     */
    public boolean requiresAcceptance() {
        return viewModel != null && viewModel.requiresAcceptance();
    }

    // === Action Handlers ===

    @FXML
    void handleAccept(ActionEvent event) {
        if (viewModel != null) {
            viewModel.handleAccept();
        }
    }

    @FXML
    void handleDecline(ActionEvent event) {
        if (viewModel != null) {
            viewModel.handleDecline();
        }
    }

    @FXML
    void handleClose(ActionEvent event) {
        if (viewModel != null) {
            viewModel.handleClose();
        }
    }

    @FXML
    void handlePrint(ActionEvent event) {
        if (viewModel != null) {
            viewModel.handlePrint();
        }
    }

    @FXML
    void scrollToSection(ActionEvent event) {
        if (event.getSource() instanceof Hyperlink link && link.getUserData() != null) {
            String sectionId = link.getUserData().toString();
            if (viewModel != null) {
                viewModel.handleScrollToSection(sectionId);
            }
        }
    }

    // === Private Helper Methods ===

    private void showDeclineConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(viewModel.getDeclineConfirmationTitle());
        alert.setHeaderText(viewModel.getDeclineConfirmationTitle());
        alert.setContentText(viewModel.getDeclineConfirmationMessage());

        ButtonType returnButton = new ButtonType("Return to Terms");
        ButtonType exitButton = new ButtonType("Exit Application", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(returnButton, exitButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == exitButton) {
            if (onDeclinedCallback != null) {
                onDeclinedCallback.run();
            }
            // Exit the application
            Platform.exit();
            System.exit(0);
        }
        // Otherwise, user chose to return to terms - do nothing
    }

    private void scrollToSection(String sectionId) {
        if (tosContent == null || sectionId == null) return;

        // Find the section node by ID
        Node section = tosContent.lookup("#" + sectionId);
        if (section != null) {
            // Calculate the scroll position to bring section into view
            double contentHeight = tosContent.getBoundsInLocal().getHeight();
            double scrollPaneHeight = contentScroll.getViewportBounds().getHeight();
            double sectionY = section.getBoundsInParent().getMinY();

            // Calculate vvalue (0.0 to 1.0)
            double maxScroll = contentHeight - scrollPaneHeight;
            if (maxScroll > 0) {
                double targetVvalue = Math.min(1.0, Math.max(0.0, sectionY / maxScroll));
                contentScroll.setVvalue(targetVvalue);
            }
        }
    }

    private void handlePrintAction() {
        // For now, show a simple info dialog
        // In production, this would generate a PDF or open print dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Print / Export");
        alert.setHeaderText("Print Terms of Service");
        alert.setContentText("The Terms of Service will be exported to a PDF file for printing or saving.");
        alert.showAndWait();

        // TODO: Implement actual PDF generation
        // This would use a PDF library to generate a document with the ToS content
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Returns the ViewModel for testing.
     *
     * @return The TermsOfServiceViewModel instance
     */
    public TermsOfServiceViewModel getViewModel() {
        return viewModel;
    }
}
