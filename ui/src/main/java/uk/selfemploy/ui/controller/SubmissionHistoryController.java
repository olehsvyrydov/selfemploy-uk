package uk.selfemploy.ui.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.ui.component.ToastNotification;
import uk.selfemploy.ui.service.SqliteDataStore;
import uk.selfemploy.ui.service.SqliteSubmissionRepository;
import uk.selfemploy.ui.service.SubmissionPdfDownloadService;
import uk.selfemploy.ui.service.SubmissionRecord;
import uk.selfemploy.ui.util.HmrcErrorGuidance;
import uk.selfemploy.ui.viewmodel.SubmissionHistoryViewModel;
import uk.selfemploy.ui.viewmodel.SubmissionTableRow;
import uk.selfemploy.ui.viewmodel.View;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Controller for the Submission History view.
 *
 * <p>Manages the display of HMRC submission history, including:</p>
 * <ul>
 *   <li>List of submissions with status badges</li>
 *   <li>Tax year filtering</li>
 *   <li>Submission detail panel</li>
 *   <li>Statistics display</li>
 * </ul>
 */
public class SubmissionHistoryController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(SubmissionHistoryController.class);

    // Container
    @FXML private VBox historyContainer;

    // Filter
    @FXML private ComboBox<String> taxYearFilter;
    @FXML private Button refreshBtn;

    // Stats
    @FXML private HBox statsRow;
    @FXML private Label totalSubmissionsLabel;
    @FXML private Label acceptedCountLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label rejectedCountLabel;

    // Submissions list
    @FXML private VBox submissionsList;

    // Empty state
    @FXML private VBox emptyState;
    @FXML private HBox infoBox;
    @FXML private Label infoBoxMessage;
    @FXML private Button goToHmrcBtn;

    // Detail panel
    @FXML private VBox detailPanel;
    @FXML private Button backBtn;
    @FXML private Label detailTypeBadge;
    @FXML private VBox statusCard;
    @FXML private Label detailStatusIcon;
    @FXML private Label detailStatusText;
    @FXML private Label detailStatusDate;
    @FXML private Label detailReference;
    @FXML private Label detailTaxYear;
    @FXML private Label detailType;
    @FXML private Label detailSubmitted;
    @FXML private Label detailIncome;
    @FXML private Label detailExpenses;
    @FXML private Label detailProfit;
    @FXML private Label detailTaxDue;
    @FXML private VBox errorSection;
    @FXML private Label detailErrorMessage;
    @FXML private VBox errorGuidanceBox;
    @FXML private Label errorGuidanceText;
    @FXML private Hyperlink learnMoreLink;
    @FXML private Button retryBtn;
    @FXML private Button downloadPdfBtn;

    private SubmissionHistoryViewModel viewModel;
    private Consumer<View> navigationCallback;

    // Services for PDF download and error guidance (SE-SH-005, SE-SH-006)
    private final SubmissionPdfDownloadService pdfDownloadService = new SubmissionPdfDownloadService();
    private final HmrcErrorGuidance errorGuidance = new HmrcErrorGuidance();

    /** Guard flag to prevent infinite recursion when updating tax year filter programmatically */
    private boolean updatingTaxYearFilter = false;

    /** Loading indicator for async SQLite operations */
    @FXML private VBox loadingState;
    @FXML private ProgressIndicator loadingIndicator;

    /** Submission repository for SQLite persistence (BUG-10H-001) */
    private SqliteSubmissionRepository submissionRepository;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        viewModel = new SubmissionHistoryViewModel();
        setupTaxYearFilter();
        setupBindings();

        // BUG-10H-001: Load submissions from SQLite
        loadSubmissionsFromDatabase();
    }

    /**
     * Loads submissions from SQLite database asynchronously.
     * Shows loading state while fetching data.
     * BUG-10H-001: Submission History persistence
     */
    private void loadSubmissionsFromDatabase() {
        // Get business ID from settings
        UUID businessId = SqliteDataStore.getInstance().loadBusinessId();
        if (businessId == null) {
            LOG.debug("No business ID set, showing empty state");
            updateView();
            return;
        }

        // Show loading state if available
        showLoadingState();

        // Load asynchronously to not block UI
        Task<List<SubmissionTableRow>> loadTask = new Task<>() {
            @Override
            protected List<SubmissionTableRow> call() {
                submissionRepository = new SqliteSubmissionRepository(businessId);
                return submissionRepository.findAll()
                    .stream()
                    .map(SubmissionRecord::toTableRow)
                    .toList();
            }
        };

        loadTask.setOnSucceeded(event -> {
            List<SubmissionTableRow> submissions = loadTask.getValue();
            LOG.info("Loaded {} submissions from SQLite", submissions.size());
            viewModel.setSubmissions(submissions);
            hideLoadingState();
            updateView();
        });

        loadTask.setOnFailed(event -> {
            LOG.error("Failed to load submissions from SQLite", loadTask.getException());
            hideLoadingState();
            updateView();
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Shows the loading state indicator.
     */
    private void showLoadingState() {
        if (loadingState != null) {
            loadingState.setVisible(true);
            loadingState.setManaged(true);
        }
        if (submissionsList != null) {
            submissionsList.setVisible(false);
            submissionsList.setManaged(false);
        }
        if (emptyState != null) {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
        }
    }

    /**
     * Hides the loading state indicator.
     */
    private void hideLoadingState() {
        if (loadingState != null) {
            loadingState.setVisible(false);
            loadingState.setManaged(false);
        }
    }

    /**
     * Sets the ViewModel directly (for testing).
     */
    public void setViewModel(SubmissionHistoryViewModel viewModel) {
        this.viewModel = viewModel;
        setupBindings();
        updateView();
    }

    /**
     * Returns the ViewModel for testing.
     */
    public SubmissionHistoryViewModel getViewModel() {
        return viewModel;
    }

    private void setupTaxYearFilter() {
        taxYearFilter.setValue("All Years");
        taxYearFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Skip if we're programmatically updating (prevents infinite recursion)
            if (updatingTaxYearFilter) {
                return;
            }
            if (viewModel != null && newVal != null) {
                viewModel.setSelectedTaxYear(newVal);
                updateView();
            }
        });
    }

    private void setupBindings() {
        if (viewModel == null) return;

        // Bind statistics labels
        viewModel.filteredTotalCountProperty().addListener((obs, oldVal, newVal) ->
            totalSubmissionsLabel.setText(String.valueOf(newVal.intValue())));
        viewModel.filteredAcceptedCountProperty().addListener((obs, oldVal, newVal) ->
            acceptedCountLabel.setText(String.valueOf(newVal.intValue())));
        viewModel.filteredPendingCountProperty().addListener((obs, oldVal, newVal) ->
            pendingCountLabel.setText(String.valueOf(newVal.intValue())));
        viewModel.filteredRejectedCountProperty().addListener((obs, oldVal, newVal) ->
            rejectedCountLabel.setText(String.valueOf(newVal.intValue())));

        // Initial values
        totalSubmissionsLabel.setText(String.valueOf(viewModel.getFilteredTotalCount()));
        acceptedCountLabel.setText(String.valueOf(viewModel.getFilteredAcceptedCount()));
        pendingCountLabel.setText(String.valueOf(viewModel.getFilteredPendingCount()));
        rejectedCountLabel.setText(String.valueOf(viewModel.getFilteredRejectedCount()));
    }

    private void updateView() {
        if (viewModel == null) return;

        // Update empty state visibility
        boolean isEmpty = viewModel.isEmptyState();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        submissionsList.setVisible(!isEmpty);
        submissionsList.setManaged(!isEmpty);
        statsRow.setVisible(!isEmpty);
        statsRow.setManaged(!isEmpty);

        // Hide detail panel when updating list
        if (!viewModel.hasSelection()) {
            hideDetailPanel();
        }

        // Populate submissions list
        populateSubmissionsList();

        // Update tax year dropdown options
        updateTaxYearOptions();
    }

    private void updateTaxYearOptions() {
        List<String> taxYears = viewModel.getAvailableTaxYears();
        String currentSelection = taxYearFilter.getValue();

        // Set guard flag to prevent listener from triggering updateView recursively
        updatingTaxYearFilter = true;
        try {
            taxYearFilter.getItems().clear();
            taxYearFilter.getItems().addAll(taxYears);

            if (taxYears.contains(currentSelection)) {
                taxYearFilter.setValue(currentSelection);
            } else {
                taxYearFilter.setValue("All Years");
            }
        } finally {
            updatingTaxYearFilter = false;
        }
    }

    private void populateSubmissionsList() {
        submissionsList.getChildren().clear();

        List<SubmissionTableRow> submissions = viewModel.getFilteredSubmissions();
        for (SubmissionTableRow submission : submissions) {
            VBox card = createSubmissionCard(submission);
            submissionsList.getChildren().add(card);
        }
    }

    /**
     * Creates a submission card component.
     */
    private VBox createSubmissionCard(SubmissionTableRow submission) {
        VBox card = new VBox();
        card.getStyleClass().add("submission-card");
        card.setSpacing(12);
        card.setOnMouseClicked(event -> handleCardClick(event, submission));

        // Top Row: Type + Date + Status
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setSpacing(12);

        // Type Badge
        Label typeBadge = new Label(submission.getTypeBadgeText());
        typeBadge.getStyleClass().addAll("type-badge", submission.getTypeStyleClass());

        // Date and Time
        VBox dateBox = new VBox();
        dateBox.setSpacing(2);
        Label dateLabel = new Label(submission.getFormattedDate());
        dateLabel.getStyleClass().add("submission-date");
        Label timeLabel = new Label(submission.getFormattedTime());
        timeLabel.getStyleClass().add("submission-time");
        dateBox.getChildren().addAll(dateLabel, timeLabel);

        // Spacer
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        // Status Badge - using FontIcon for cross-platform icons (BUG-10H-001)
        HBox statusBadge = new HBox();
        statusBadge.getStyleClass().addAll("status-badge", submission.getStatusStyleClass());
        statusBadge.setAlignment(Pos.CENTER);
        statusBadge.setSpacing(6);

        Node statusIcon = getStatusIcon(submission.status());
        Label statusText = new Label(submission.getStatusDisplay());
        statusText.getStyleClass().add("status-text");
        statusBadge.getChildren().addAll(statusIcon, statusText);

        topRow.getChildren().addAll(typeBadge, dateBox, spacer1, statusBadge);

        // Separator
        Separator separator = new Separator();
        separator.getStyleClass().add("card-separator");

        // Bottom Row: Reference + Value
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        bottomRow.setSpacing(16);

        // Reference
        VBox refBox = new VBox();
        refBox.setSpacing(2);
        Label refLabel = new Label("Reference");
        refLabel.getStyleClass().add("field-label");
        Label refValue = new Label(submission.getReferenceDisplay());
        refValue.getStyleClass().add("reference-text");
        refBox.getChildren().addAll(refLabel, refValue);

        // Spacer
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        // Value
        VBox valueBox = new VBox();
        valueBox.setSpacing(2);
        valueBox.setAlignment(Pos.CENTER_RIGHT);
        Label valueTypeLabel = new Label(submission.getPrimaryValueLabel());
        valueTypeLabel.getStyleClass().add("field-label");
        Label valueLabel = new Label(submission.getPrimaryValueDisplay());
        valueLabel.getStyleClass().add("value-text");
        valueBox.getChildren().addAll(valueTypeLabel, valueLabel);

        // Expand Arrow
        Label expandArrow = new Label(">");
        expandArrow.getStyleClass().add("expand-arrow");

        bottomRow.getChildren().addAll(refBox, spacer2, valueBox, expandArrow);

        card.getChildren().addAll(topRow, separator, bottomRow);

        // Error row for rejected submissions - using FontIcon (BUG-10H-001)
        if (submission.isRejected() && submission.hasError()) {
            HBox errorRow = new HBox();
            errorRow.getStyleClass().add("error-row");
            errorRow.setAlignment(Pos.CENTER_LEFT);
            errorRow.setSpacing(8);

            FontIcon errorIcon = FontIcon.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE, 14);
            errorIcon.getStyleClass().add("error-icon");
            Label errorMessage = new Label(submission.errorMessage());
            errorMessage.getStyleClass().add("error-text");
            errorMessage.setWrapText(true);

            errorRow.getChildren().addAll(errorIcon, errorMessage);
            card.getChildren().add(errorRow);
        }

        return card;
    }

    /**
     * Creates a FontIcon for the submission status.
     * Uses Ikonli FontAwesome5 icons for cross-platform compatibility.
     * BUG-10H-001: Replace emoji icons per /aura design review.
     *
     * @param status The submission status
     * @return A FontIcon representing the status
     */
    private Node getStatusIcon(SubmissionStatus status) {
        if (status == null) return new Label("");
        FontIcon icon = switch (status) {
            case ACCEPTED -> FontIcon.of(FontAwesomeSolid.CHECK_CIRCLE, 14);
            case REJECTED -> FontIcon.of(FontAwesomeSolid.TIMES_CIRCLE, 14);
            case PENDING -> FontIcon.of(FontAwesomeSolid.CLOCK, 14);
            case SUBMITTED -> FontIcon.of(FontAwesomeSolid.PAPER_PLANE, 14);
        };
        icon.getStyleClass().add("status-icon");
        return icon;
    }

    /**
     * @deprecated Use {@link #getStatusIcon(SubmissionStatus)} instead.
     * Kept for backward compatibility with existing code.
     */
    @Deprecated
    private String getStatusIconText(SubmissionStatus status) {
        if (status == null) return "";
        return switch (status) {
            case ACCEPTED -> "\u2713"; // checkmark
            case REJECTED -> "\u2717"; // x mark
            case PENDING -> "\u23F1"; // stopwatch
            case SUBMITTED -> "\u2709"; // envelope
        };
    }

    private void handleCardClick(MouseEvent event, SubmissionTableRow submission) {
        viewModel.selectSubmission(submission);
        showDetailPanel(submission);
    }

    private void showDetailPanel(SubmissionTableRow submission) {
        // Update detail panel content
        detailTypeBadge.setText(submission.getTypeBadgeText());
        detailTypeBadge.getStyleClass().removeIf(s -> s.startsWith("type-"));
        detailTypeBadge.getStyleClass().add(submission.getTypeStyleClass());

        // Status card - using FontIcon (BUG-10H-001)
        statusCard.getStyleClass().removeIf(s -> s.startsWith("status-"));
        statusCard.getStyleClass().add(submission.getStatusStyleClass());
        // Set graphic on the label instead of text for proper icon rendering
        detailStatusIcon.setGraphic(getStatusIcon(submission.status()));
        detailStatusIcon.setText(""); // Clear any text
        detailStatusText.setText(submission.getStatusDisplay());
        detailStatusDate.setText(submission.getStatusDisplay() + " on " + submission.getFormattedDateTime());

        // Reference
        detailReference.setText(submission.getReferenceDisplay());

        // Summary
        detailTaxYear.setText(submission.taxYear());
        detailType.setText(submission.getTypeDisplayName());
        detailSubmitted.setText(submission.getFormattedDateTime());

        // Financial
        detailIncome.setText(submission.getFormattedIncome());
        detailExpenses.setText(submission.getFormattedExpenses());
        detailProfit.setText(submission.getFormattedProfit());
        detailTaxDue.setText(submission.getFormattedTaxDue());

        // Error section with guidance (SE-SH-006)
        boolean hasError = submission.isRejected() && submission.hasError();
        errorSection.setVisible(hasError);
        errorSection.setManaged(hasError);
        if (hasError) {
            detailErrorMessage.setText(submission.errorMessage());
            updateErrorGuidance(submission.errorMessage());
        }

        // Enable/disable PDF download button (SE-SH-005)
        // PDF can be downloaded for any submission (even rejected/pending)
        downloadPdfBtn.setDisable(false);
        downloadPdfBtn.setAccessibleText("Download PDF confirmation for " + submission.getPeriodDisplay());

        // Show detail panel, hide list
        submissionsList.setVisible(false);
        submissionsList.setManaged(false);
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
    }

    private void hideDetailPanel() {
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
        submissionsList.setVisible(!viewModel.isEmptyState());
        submissionsList.setManaged(!viewModel.isEmptyState());
        viewModel.clearSelection();
    }

    // === Action Handlers ===

    @FXML
    void handleTaxYearFilter(ActionEvent event) {
        // Skip if we're programmatically updating (prevents infinite recursion)
        if (updatingTaxYearFilter) {
            return;
        }
        String selectedYear = taxYearFilter.getValue();
        if (viewModel != null && selectedYear != null) {
            viewModel.setSelectedTaxYear(selectedYear);
            updateView();
        }
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        LOG.info("Refreshing submission history");
        loadSubmissionsFromDatabase();
    }

    @FXML
    void handleSubmitQuarterly(ActionEvent event) {
        LOG.info("Navigate to quarterly submission");
        navigateToView(View.HMRC_SUBMISSION);
    }

    /**
     * Handles navigation to the HMRC Submission page (PS11-005).
     * Used by the empty state "Go to HMRC Submission" button.
     */
    @FXML
    void handleGoToHmrcSubmission(ActionEvent event) {
        LOG.info("Navigate to HMRC Submission page");
        navigateToView(View.HMRC_SUBMISSION);
    }

    /**
     * Sets the navigation callback for view switching.
     *
     * @param callback Consumer that accepts a View to navigate to
     */
    public void setNavigationCallback(Consumer<View> callback) {
        this.navigationCallback = callback;
    }

    /**
     * Navigates to the specified view using the navigation callback.
     */
    private void navigateToView(View view) {
        if (navigationCallback != null) {
            navigationCallback.accept(view);
        } else {
            LOG.warn("Navigation callback not set, cannot navigate to: {}", view);
        }
    }

    @FXML
    void handleBack(ActionEvent event) {
        hideDetailPanel();
    }

    @FXML
    void handleCopyReference(ActionEvent event) {
        SubmissionTableRow selected = viewModel.getSelectedSubmission();
        if (selected != null && selected.hmrcReference() != null) {
            ClipboardContent content = new ClipboardContent();
            content.putString(selected.hmrcReference());
            Clipboard.getSystemClipboard().setContent(content);
            LOG.info("Reference copied to clipboard: {}", selected.hmrcReference());

            // Show brief feedback (could be a toast in production)
        }
    }

    @FXML
    void handleRetry(ActionEvent event) {
        LOG.info("Retry submission requested");
        // Retry functionality - disabled until SE-405 UI is complete
        // Will be implemented to call the error handling retry service
    }

    /**
     * Handles PDF download for the currently selected submission.
     * SE-SH-005: Implement PDF download with auto-open functionality.
     *
     * <p>After generating the PDF, attempts to open it automatically using
     * the system's default PDF viewer. Falls back to showing a "saved to Downloads"
     * message if opening fails (e.g., headless environment, no PDF viewer).</p>
     */
    @FXML
    void handleDownloadPDF(ActionEvent event) {
        SubmissionTableRow selected = viewModel.getSelectedSubmission();
        if (selected == null) {
            LOG.warn("Download PDF requested but no submission selected");
            return;
        }

        LOG.info("Download PDF requested for submission: {}", selected.hmrcReference());

        try {
            // Generate filename and determine output path
            String filename = pdfDownloadService.generateFilename(selected);
            Path downloadsDir = pdfDownloadService.getDownloadsDirectory();
            Path outputPath = downloadsDir.resolve(filename);

            // Generate and save PDF
            pdfDownloadService.generatePdf(selected, outputPath);

            LOG.info("PDF saved successfully to: {}", outputPath);

            // Show toast immediately - don't wait for PDF viewer to open
            ToastNotification.showExternalBrowserToast("Opening PDF...", outputPath.toString());

            // Open PDF asynchronously on a background thread to avoid blocking UI
            tryOpenPdfAsync(outputPath);

        } catch (IOException e) {
            LOG.error("Failed to generate PDF for submission: {}", selected.hmrcReference(), e);

            // Show error dialog
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("PDF Generation Failed");
            errorAlert.setHeaderText("Could not generate PDF");
            errorAlert.setContentText(
                "An error occurred while generating the PDF confirmation. " +
                "Please try again. Error: " + e.getMessage()
            );
            errorAlert.showAndWait();
        } catch (Exception e) {
            LOG.error("Unexpected error generating PDF: {}", e.getMessage(), e);
        }
    }

    /**
     * Attempts to open the PDF file using the system's default PDF viewer.
     * Runs on a background thread to avoid blocking the JavaFX Application Thread.
     *
     * @param pdfPath Path to the PDF file to open
     */
    private void tryOpenPdfAsync(Path pdfPath) {
        // Run Desktop.open() on a background thread - it can block on Linux
        Thread openThread = new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.OPEN)) {
                        desktop.open(pdfPath.toFile());
                        LOG.info("PDF opened successfully: {}", pdfPath);
                    } else {
                        LOG.debug("Desktop OPEN action not supported on this platform");
                    }
                } else {
                    LOG.debug("Desktop not supported (headless environment)");
                }
            } catch (IOException e) {
                LOG.warn("Failed to open PDF (no suitable viewer?): {}", e.getMessage());
            } catch (UnsupportedOperationException e) {
                LOG.debug("Desktop operations not supported: {}", e.getMessage());
            } catch (SecurityException e) {
                LOG.warn("Security restriction prevented opening PDF: {}", e.getMessage());
            }
        }, "PDF-Opener");
        openThread.setDaemon(true);
        openThread.start();
    }

    /**
     * Updates the error guidance section based on the error message.
     * SE-SH-006: Error resolution guidance
     */
    private void updateErrorGuidance(String errorMessage) {
        if (errorGuidanceBox == null || errorGuidanceText == null) {
            // FXML elements not present - may be old FXML version
            LOG.debug("Error guidance UI elements not available");
            return;
        }

        // Extract error code and get guidance
        String errorCode = errorGuidance.extractErrorCode(errorMessage);
        String guidanceText = errorGuidance.getFormattedGuidance(errorCode, errorMessage);

        errorGuidanceText.setText(guidanceText);
        errorGuidanceBox.setVisible(true);
        errorGuidanceBox.setManaged(true);
    }

    /**
     * Handles the "Learn more" link click to open HMRC guidance.
     * SE-SH-006: Error resolution guidance
     */
    @FXML
    void handleLearnMore(ActionEvent event) {
        String url = errorGuidance.getGuidanceUrl();
        LOG.info("Opening HMRC guidance URL: {}", url);

        // Run on background thread to avoid blocking the UI
        Thread browserThread = new Thread(() -> {
            try {
                // Try to open in default browser
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                    Platform.runLater(() ->
                        ToastNotification.showExternalBrowserToast("Opening HMRC guidance...", url));
                } else {
                    // Fallback: show URL in dialog
                    Platform.runLater(() -> showUrlDialog(url));
                }
            } catch (Exception e) {
                LOG.error("Failed to open HMRC guidance URL: {}", url, e);
                Platform.runLater(() -> showUrlDialog(url));
            }
        }, "browser-launcher");
        browserThread.setDaemon(true);
        browserThread.start();
    }

    private void showUrlDialog(String url) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("HMRC Guidance");
        alert.setHeaderText("Visit HMRC website for more information");
        alert.setContentText("Please visit:\n" + url);
        alert.showAndWait();
    }

    /**
     * Loads submissions data.
     * Called by the navigation system or parent controller.
     *
     * @param submissions The list of submissions to display
     */
    public void loadSubmissions(List<SubmissionTableRow> submissions) {
        if (viewModel != null) {
            viewModel.setSubmissions(submissions);
            updateView();
        }
    }

    /**
     * Refreshes the view.
     */
    public void refreshData() {
        updateView();
    }
}
