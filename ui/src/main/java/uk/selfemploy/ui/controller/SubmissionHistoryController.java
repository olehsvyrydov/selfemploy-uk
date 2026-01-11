package uk.selfemploy.ui.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.ui.viewmodel.SubmissionHistoryViewModel;
import uk.selfemploy.ui.viewmodel.SubmissionTableRow;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

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
    @FXML private Button retryBtn;
    @FXML private Button downloadPdfBtn;
    @FXML private Button viewDataBtn;

    private SubmissionHistoryViewModel viewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        viewModel = new SubmissionHistoryViewModel();
        setupTaxYearFilter();
        setupBindings();
        updateView();
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

        taxYearFilter.getItems().clear();
        taxYearFilter.getItems().addAll(taxYears);

        if (taxYears.contains(currentSelection)) {
            taxYearFilter.setValue(currentSelection);
        } else {
            taxYearFilter.setValue("All Years");
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

        // Status Badge
        HBox statusBadge = new HBox();
        statusBadge.getStyleClass().addAll("status-badge", submission.getStatusStyleClass());
        statusBadge.setAlignment(Pos.CENTER);
        statusBadge.setSpacing(6);

        Label statusIcon = new Label(getStatusIconText(submission.status()));
        statusIcon.getStyleClass().add("status-icon");
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

        // Error row for rejected submissions
        if (submission.isRejected() && submission.hasError()) {
            HBox errorRow = new HBox();
            errorRow.getStyleClass().add("error-row");
            errorRow.setAlignment(Pos.CENTER_LEFT);
            errorRow.setSpacing(8);

            Label errorIcon = new Label("!");
            errorIcon.getStyleClass().add("error-icon");
            Label errorMessage = new Label(submission.errorMessage());
            errorMessage.getStyleClass().add("error-text");
            errorMessage.setWrapText(true);

            errorRow.getChildren().addAll(errorIcon, errorMessage);
            card.getChildren().add(errorRow);
        }

        return card;
    }

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

        // Status card
        statusCard.getStyleClass().removeIf(s -> s.startsWith("status-"));
        statusCard.getStyleClass().add(submission.getStatusStyleClass());
        detailStatusIcon.setText(getStatusIconText(submission.status()));
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

        // Error section
        boolean hasError = submission.isRejected() && submission.hasError();
        errorSection.setVisible(hasError);
        errorSection.setManaged(hasError);
        if (hasError) {
            detailErrorMessage.setText(submission.errorMessage());
        }

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
        String selectedYear = taxYearFilter.getValue();
        if (viewModel != null && selectedYear != null) {
            viewModel.setSelectedTaxYear(selectedYear);
            updateView();
        }
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        LOG.info("Refreshing submission history");
        // In production, this would reload data from the service
        // For now, just update the view
        updateView();
    }

    @FXML
    void handleSubmitQuarterly(ActionEvent event) {
        LOG.info("Navigate to quarterly submission");
        // In production, this would navigate to the Tax Summary view
        // Implementation depends on navigation service
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

    @FXML
    void handleDownloadPDF(ActionEvent event) {
        LOG.info("Download PDF requested");
        // PDF download functionality - to be implemented
    }

    @FXML
    void handleViewData(ActionEvent event) {
        LOG.info("View full data requested");
        SubmissionTableRow selected = viewModel.getSelectedSubmission();
        if (selected != null) {
            // Show JSON data in a dialog - to be implemented
            showDataDialog(selected);
        }
    }

    private void showDataDialog(SubmissionTableRow submission) {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Submission Data");
        dialog.setHeaderText("Submission: " + submission.getPeriodDisplay());

        String data = String.format("""
            Reference: %s
            Tax Year: %s
            Type: %s
            Status: %s
            Submitted: %s

            Income: %s
            Expenses: %s
            Net Profit: %s
            Tax Due: %s
            """,
            submission.getReferenceDisplay(),
            submission.taxYear(),
            submission.getTypeDisplayName(),
            submission.getStatusDisplay(),
            submission.getFormattedDateTime(),
            submission.getFormattedIncome(),
            submission.getFormattedExpenses(),
            submission.getFormattedProfit(),
            submission.getFormattedTaxDue()
        );

        TextArea textArea = new TextArea(data);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(12);

        dialog.getDialogPane().setContent(textArea);
        dialog.showAndWait();
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
