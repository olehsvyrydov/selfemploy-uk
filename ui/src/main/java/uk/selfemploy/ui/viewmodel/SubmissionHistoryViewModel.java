package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ViewModel for the Submission History view.
 *
 * <p>Manages submission data, filtering by tax year, sorting, and selection.
 * Provides statistics for submission counts by status.</p>
 *
 * <p>This ViewModel follows the MVVM pattern and is designed to work with
 * JavaFX properties for data binding with the UI.</p>
 */
public class SubmissionHistoryViewModel {

    private static final String ALL_YEARS = "All Years";

    // Submission data
    private final ObservableList<SubmissionTableRow> submissions = FXCollections.observableArrayList();

    // Filter
    private final StringProperty selectedTaxYear = new SimpleStringProperty(ALL_YEARS);

    // Selection
    private final ObjectProperty<SubmissionTableRow> selectedSubmission = new SimpleObjectProperty<>();

    // Statistics - total counts (all data, regardless of filter)
    private final IntegerProperty totalCount = new SimpleIntegerProperty(0);
    private final IntegerProperty acceptedCount = new SimpleIntegerProperty(0);
    private final IntegerProperty pendingCount = new SimpleIntegerProperty(0);
    private final IntegerProperty rejectedCount = new SimpleIntegerProperty(0);
    private final IntegerProperty submittedCount = new SimpleIntegerProperty(0);

    // Statistics - filtered counts (respects tax year filter)
    private final IntegerProperty filteredTotalCount = new SimpleIntegerProperty(0);
    private final IntegerProperty filteredAcceptedCount = new SimpleIntegerProperty(0);
    private final IntegerProperty filteredPendingCount = new SimpleIntegerProperty(0);
    private final IntegerProperty filteredRejectedCount = new SimpleIntegerProperty(0);

    /**
     * Creates a new SubmissionHistoryViewModel with default state.
     */
    public SubmissionHistoryViewModel() {
        // Listen for tax year filter changes
        selectedTaxYear.addListener((obs, oldVal, newVal) -> updateFilteredStats());
    }

    /**
     * Adds a submission to the list.
     *
     * @param submission The submission to add
     */
    public void addSubmission(SubmissionTableRow submission) {
        submissions.add(submission);
        updateStats();
        updateFilteredStats();
    }

    /**
     * Sets the submissions list, replacing all existing entries.
     *
     * @param newSubmissions The new list of submissions
     */
    public void setSubmissions(List<SubmissionTableRow> newSubmissions) {
        submissions.clear();
        submissions.addAll(newSubmissions);
        updateStats();
        updateFilteredStats();
    }

    /**
     * Clears all submissions.
     */
    public void clearAll() {
        submissions.clear();
        selectedSubmission.set(null);
        updateStats();
        updateFilteredStats();
    }

    /**
     * Returns all submissions (unfiltered).
     */
    public ObservableList<SubmissionTableRow> getSubmissions() {
        return submissions;
    }

    /**
     * Returns submissions filtered by the selected tax year, sorted by date (newest first).
     */
    public List<SubmissionTableRow> getFilteredSubmissions() {
        String taxYear = selectedTaxYear.get();

        return submissions.stream()
            .filter(s -> s.matchesTaxYear(taxYear))
            .sorted(Comparator.comparing(SubmissionTableRow::submittedAt).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Returns available tax years from the submissions, including "All Years".
     */
    public List<String> getAvailableTaxYears() {
        List<String> years = submissions.stream()
            .map(SubmissionTableRow::taxYear)
            .distinct()
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());

        return Stream.concat(Stream.of(ALL_YEARS), years.stream())
            .collect(Collectors.toList());
    }

    // === Selection ===

    /**
     * Selects a submission.
     */
    public void selectSubmission(SubmissionTableRow submission) {
        selectedSubmission.set(submission);
    }

    /**
     * Clears the current selection.
     */
    public void clearSelection() {
        selectedSubmission.set(null);
    }

    /**
     * Returns true if a submission is currently selected.
     */
    public boolean hasSelection() {
        return selectedSubmission.get() != null;
    }

    /**
     * Returns the currently selected submission.
     */
    public SubmissionTableRow getSelectedSubmission() {
        return selectedSubmission.get();
    }

    public ObjectProperty<SubmissionTableRow> selectedSubmissionProperty() {
        return selectedSubmission;
    }

    // === Tax Year Filter ===

    public String getSelectedTaxYear() {
        return selectedTaxYear.get();
    }

    public void setSelectedTaxYear(String taxYear) {
        selectedTaxYear.set(taxYear != null ? taxYear : ALL_YEARS);
    }

    public StringProperty selectedTaxYearProperty() {
        return selectedTaxYear;
    }

    // === Statistics (All Data) ===

    public int getTotalCount() {
        return totalCount.get();
    }

    public IntegerProperty totalCountProperty() {
        return totalCount;
    }

    public int getAcceptedCount() {
        return acceptedCount.get();
    }

    public IntegerProperty acceptedCountProperty() {
        return acceptedCount;
    }

    public int getPendingCount() {
        return pendingCount.get();
    }

    public IntegerProperty pendingCountProperty() {
        return pendingCount;
    }

    public int getRejectedCount() {
        return rejectedCount.get();
    }

    public IntegerProperty rejectedCountProperty() {
        return rejectedCount;
    }

    public int getSubmittedCount() {
        return submittedCount.get();
    }

    public IntegerProperty submittedCountProperty() {
        return submittedCount;
    }

    // === Statistics (Filtered) ===

    public int getFilteredTotalCount() {
        return filteredTotalCount.get();
    }

    public IntegerProperty filteredTotalCountProperty() {
        return filteredTotalCount;
    }

    public int getFilteredAcceptedCount() {
        return filteredAcceptedCount.get();
    }

    public IntegerProperty filteredAcceptedCountProperty() {
        return filteredAcceptedCount;
    }

    public int getFilteredPendingCount() {
        return filteredPendingCount.get();
    }

    public IntegerProperty filteredPendingCountProperty() {
        return filteredPendingCount;
    }

    public int getFilteredRejectedCount() {
        return filteredRejectedCount.get();
    }

    public IntegerProperty filteredRejectedCountProperty() {
        return filteredRejectedCount;
    }

    // === State Checks ===

    /**
     * Returns true if there are no submissions at all.
     */
    public boolean isEmptyState() {
        return submissions.isEmpty();
    }

    /**
     * Returns true if the filter returns no results but there are submissions.
     */
    public boolean isNoResults() {
        return !submissions.isEmpty() && getFilteredSubmissions().isEmpty();
    }

    // =========================================================================
    // PS11-004: Quarterly Updates Placeholder
    // =========================================================================

    private static final String QUARTERLY_UPDATES_MESSAGE =
        "Quarterly updates coming April 2026 for income over GBP50,000";
    private static final String QUARTERLY_UPDATES_AVAILABLE_DATE = "April 2026";
    private static final String QUARTERLY_UPDATES_INCOME_THRESHOLD = "GBP50,000";
    private static final String QUARTERLY_UPDATES_INFO_URL =
        "https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax";
    private static final String QUARTERLY_UPDATES_LEARN_MORE_TEXT =
        "Learn more about Making Tax Digital";

    /**
     * Returns the quarterly updates placeholder message.
     * PS11-004: Shows MTD timeline and income thresholds.
     */
    public String getQuarterlyUpdatesMessage() {
        return QUARTERLY_UPDATES_MESSAGE;
    }

    /**
     * Returns the date when quarterly updates become available.
     */
    public String getQuarterlyUpdatesAvailableDate() {
        return QUARTERLY_UPDATES_AVAILABLE_DATE;
    }

    /**
     * Returns the income threshold for mandatory quarterly updates.
     */
    public String getQuarterlyUpdatesIncomeThreshold() {
        return QUARTERLY_UPDATES_INCOME_THRESHOLD;
    }

    /**
     * Returns whether quarterly updates feature is available.
     * Currently returns false as the feature is planned for April 2026.
     */
    public boolean isQuarterlyUpdatesAvailable() {
        return false;
    }

    /**
     * Returns the HMRC MTD guidance URL.
     */
    public String getQuarterlyUpdatesInfoUrl() {
        return QUARTERLY_UPDATES_INFO_URL;
    }

    /**
     * Returns the "learn more" button text.
     */
    public String getQuarterlyUpdatesLearnMoreText() {
        return QUARTERLY_UPDATES_LEARN_MORE_TEXT;
    }

    /**
     * Returns the count of quarterly submissions (Q1-Q4).
     */
    public int getQuarterlySubmissionCount() {
        return (int) submissions.stream()
            .filter(s -> s.type() != null && s.type().name().startsWith("QUARTERLY"))
            .count();
    }

    /**
     * Returns information about the next quarterly deadline.
     */
    public String getNextQuarterlyDeadlineInfo() {
        // Returns placeholder - will be enhanced with actual deadline logic
        return "Q1 2026/27 - Not yet available";
    }

    // =========================================================================
    // PS11-005: Button State Fix
    // =========================================================================

    private final BooleanProperty viewDetailsEnabled = new SimpleBooleanProperty(false);

    {
        // Bind viewDetailsEnabled to selection state
        selectedSubmission.addListener((obs, oldVal, newVal) -> {
            viewDetailsEnabled.set(newVal != null);
        });
    }

    /**
     * Returns whether the "View Details" button should be enabled.
     * Enabled when a submission is selected.
     */
    public boolean isViewDetailsEnabled() {
        return hasSelection();
    }

    /**
     * Returns the view details enabled property for binding.
     */
    public BooleanProperty viewDetailsEnabledProperty() {
        return viewDetailsEnabled;
    }

    /**
     * Returns the appropriate tooltip for the "View Details" button.
     */
    public String getViewDetailsTooltip() {
        return hasSelection()
            ? "View submission details"
            : "Select a submission to view details";
    }

    /**
     * Returns whether the export button should be enabled.
     * Enabled when there are submissions to export.
     */
    public boolean isExportEnabled() {
        return !submissions.isEmpty();
    }

    /**
     * Returns the appropriate tooltip for the export button.
     */
    public String getExportTooltip() {
        return submissions.isEmpty()
            ? "No submissions to export"
            : "Export submission history";
    }

    // === Private Methods ===

    private void updateStats() {
        totalCount.set(submissions.size());
        acceptedCount.set(countByStatus(SubmissionStatus.ACCEPTED));
        pendingCount.set(countByStatus(SubmissionStatus.PENDING));
        rejectedCount.set(countByStatus(SubmissionStatus.REJECTED));
        submittedCount.set(countByStatus(SubmissionStatus.SUBMITTED));
    }

    private void updateFilteredStats() {
        List<SubmissionTableRow> filtered = getFilteredSubmissions();
        filteredTotalCount.set(filtered.size());
        filteredAcceptedCount.set(countByStatusInList(filtered, SubmissionStatus.ACCEPTED));
        filteredPendingCount.set(countByStatusInList(filtered, SubmissionStatus.PENDING));
        filteredRejectedCount.set(countByStatusInList(filtered, SubmissionStatus.REJECTED));
    }

    private int countByStatus(SubmissionStatus status) {
        return (int) submissions.stream()
            .filter(s -> s.status() == status)
            .count();
    }

    private int countByStatusInList(List<SubmissionTableRow> list, SubmissionStatus status) {
        return (int) list.stream()
            .filter(s -> s.status() == status)
            .count();
    }
}
