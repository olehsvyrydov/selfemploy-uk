package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for SubmissionHistoryViewModel.
 * Tests the ViewModel logic for the Submission History view.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Initial state</li>
 *   <li>Adding and displaying submissions</li>
 *   <li>Filtering by tax year</li>
 *   <li>Filtering by status</li>
 *   <li>Statistics calculation</li>
 *   <li>Sorting by date (newest first)</li>
 *   <li>Empty state handling</li>
 * </ul>
 */
@DisplayName("SubmissionHistoryViewModel")
class SubmissionHistoryViewModelTest {

    private SubmissionHistoryViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new SubmissionHistoryViewModel();
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("should initialize with empty submission list")
        void shouldInitializeWithEmptySubmissionList() {
            assertThat(viewModel.getSubmissions()).isEmpty();
        }

        @Test
        @DisplayName("should initialize with All Years tax year filter")
        void shouldInitializeWithAllYearsTaxYearFilter() {
            assertThat(viewModel.getSelectedTaxYear()).isEqualTo("All Years");
        }

        @Test
        @DisplayName("should initialize with no selected submission")
        void shouldInitializeWithNoSelectedSubmission() {
            assertThat(viewModel.getSelectedSubmission()).isNull();
        }

        @Test
        @DisplayName("should initialize with zero counts")
        void shouldInitializeWithZeroCounts() {
            assertThat(viewModel.getTotalCount()).isZero();
            assertThat(viewModel.getAcceptedCount()).isZero();
            assertThat(viewModel.getPendingCount()).isZero();
            assertThat(viewModel.getRejectedCount()).isZero();
        }

        @Test
        @DisplayName("should show empty state when no submissions")
        void shouldShowEmptyStateWhenNoSubmissions() {
            assertThat(viewModel.isEmptyState()).isTrue();
        }
    }

    @Nested
    @DisplayName("Adding Submissions")
    class AddingSubmissions {

        @Test
        @DisplayName("should add submission to list")
        void shouldAddSubmissionToList() {
            // Given
            SubmissionTableRow submission = createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            );

            // When
            viewModel.addSubmission(submission);

            // Then
            assertThat(viewModel.getSubmissions()).hasSize(1);
            assertThat(viewModel.getSubmissions().get(0)).isEqualTo(submission);
        }

        @Test
        @DisplayName("should update total count when adding submission")
        void shouldUpdateTotalCountWhenAddingSubmission() {
            // Given & When
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q2, "2025/26", SubmissionStatus.PENDING
            ));

            // Then
            assertThat(viewModel.getTotalCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should update accepted count")
        void shouldUpdateAcceptedCount() {
            // Given & When
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q2, "2025/26", SubmissionStatus.ACCEPTED
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q3, "2025/26", SubmissionStatus.PENDING
            ));

            // Then
            assertThat(viewModel.getAcceptedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should update pending count")
        void shouldUpdatePendingCount() {
            // Given & When
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.PENDING
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q2, "2025/26", SubmissionStatus.ACCEPTED
            ));

            // Then
            assertThat(viewModel.getPendingCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should update rejected count")
        void shouldUpdateRejectedCount() {
            // Given & When
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.REJECTED
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q2, "2025/26", SubmissionStatus.REJECTED
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q3, "2025/26", SubmissionStatus.ACCEPTED
            ));

            // Then
            assertThat(viewModel.getRejectedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should hide empty state when submission added")
        void shouldHideEmptyStateWhenSubmissionAdded() {
            // Given & When
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            ));

            // Then
            assertThat(viewModel.isEmptyState()).isFalse();
        }
    }

    @Nested
    @DisplayName("Tax Year Filtering")
    class TaxYearFiltering {

        @BeforeEach
        void setUpSubmissions() {
            viewModel.addSubmission(createSubmissionWithTaxYear("2025/26", SubmissionType.QUARTERLY_Q1));
            viewModel.addSubmission(createSubmissionWithTaxYear("2025/26", SubmissionType.QUARTERLY_Q2));
            viewModel.addSubmission(createSubmissionWithTaxYear("2024/25", SubmissionType.QUARTERLY_Q1));
            viewModel.addSubmission(createSubmissionWithTaxYear("2024/25", SubmissionType.ANNUAL));
            viewModel.addSubmission(createSubmissionWithTaxYear("2023/24", SubmissionType.ANNUAL));
        }

        @Test
        @DisplayName("should return all submissions when All Years selected")
        void shouldReturnAllSubmissionsWhenAllYearsSelected() {
            // Given
            viewModel.setSelectedTaxYear("All Years");

            // Then
            assertThat(viewModel.getFilteredSubmissions()).hasSize(5);
        }

        @Test
        @DisplayName("should filter by specific tax year")
        void shouldFilterBySpecificTaxYear() {
            // When
            viewModel.setSelectedTaxYear("2025/26");

            // Then
            List<SubmissionTableRow> filtered = viewModel.getFilteredSubmissions();
            assertThat(filtered).hasSize(2);
            assertThat(filtered).allMatch(s -> s.taxYear().equals("2025/26"));
        }

        @Test
        @DisplayName("should return empty list when no submissions match tax year")
        void shouldReturnEmptyListWhenNoSubmissionsMatchTaxYear() {
            // When
            viewModel.setSelectedTaxYear("2022/23");

            // Then
            assertThat(viewModel.getFilteredSubmissions()).isEmpty();
        }

        @Test
        @DisplayName("should update stats when tax year filter changes")
        void shouldUpdateStatsWhenTaxYearFilterChanges() {
            // Given - add submissions with different statuses
            viewModel.clearAll();
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q2, "2025/26", SubmissionStatus.PENDING
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.ANNUAL, "2024/25", SubmissionStatus.ACCEPTED
            ));

            // When - filter to 2025/26
            viewModel.setSelectedTaxYear("2025/26");

            // Then - stats should reflect filtered data
            assertThat(viewModel.getFilteredTotalCount()).isEqualTo(2);
            assertThat(viewModel.getFilteredAcceptedCount()).isEqualTo(1);
            assertThat(viewModel.getFilteredPendingCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return available tax years")
        void shouldReturnAvailableTaxYears() {
            // Then
            List<String> taxYears = viewModel.getAvailableTaxYears();
            assertThat(taxYears).contains("All Years", "2025/26", "2024/25", "2023/24");
        }
    }

    @Nested
    @DisplayName("Sorting")
    class Sorting {

        @Test
        @DisplayName("should sort by date descending by default (newest first)")
        void shouldSortByDateDescendingByDefault() {
            // Given
            LocalDateTime oldest = LocalDateTime.of(2025, 4, 10, 10, 0);
            LocalDateTime middle = LocalDateTime.of(2025, 7, 15, 14, 30);
            LocalDateTime newest = LocalDateTime.of(2026, 1, 24, 16, 0);

            viewModel.addSubmission(createSubmissionWithDate(middle, "2025/26"));
            viewModel.addSubmission(createSubmissionWithDate(oldest, "2025/26"));
            viewModel.addSubmission(createSubmissionWithDate(newest, "2025/26"));

            // When
            List<SubmissionTableRow> sorted = viewModel.getFilteredSubmissions();

            // Then - newest should be first
            assertThat(sorted.get(0).submittedAt()).isEqualTo(newest);
            assertThat(sorted.get(1).submittedAt()).isEqualTo(middle);
            assertThat(sorted.get(2).submittedAt()).isEqualTo(oldest);
        }
    }

    @Nested
    @DisplayName("Selection")
    class Selection {

        @Test
        @DisplayName("should select submission")
        void shouldSelectSubmission() {
            // Given
            SubmissionTableRow submission = createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            );
            viewModel.addSubmission(submission);

            // When
            viewModel.selectSubmission(submission);

            // Then
            assertThat(viewModel.getSelectedSubmission()).isEqualTo(submission);
        }

        @Test
        @DisplayName("should clear selection")
        void shouldClearSelection() {
            // Given
            SubmissionTableRow submission = createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            );
            viewModel.addSubmission(submission);
            viewModel.selectSubmission(submission);

            // When
            viewModel.clearSelection();

            // Then
            assertThat(viewModel.getSelectedSubmission()).isNull();
        }

        @Test
        @DisplayName("should report when submission is selected")
        void shouldReportWhenSubmissionIsSelected() {
            // Given
            SubmissionTableRow submission = createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            );
            viewModel.addSubmission(submission);

            // Then
            assertThat(viewModel.hasSelection()).isFalse();

            // When
            viewModel.selectSubmission(submission);

            // Then
            assertThat(viewModel.hasSelection()).isTrue();
        }
    }

    @Nested
    @DisplayName("Clear All")
    class ClearAll {

        @Test
        @DisplayName("should clear all submissions")
        void shouldClearAllSubmissions() {
            // Given
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q2, "2025/26", SubmissionStatus.PENDING
            ));

            // When
            viewModel.clearAll();

            // Then
            assertThat(viewModel.getSubmissions()).isEmpty();
            assertThat(viewModel.getTotalCount()).isZero();
            assertThat(viewModel.isEmptyState()).isTrue();
        }

        @Test
        @DisplayName("should reset counts when clearing")
        void shouldResetCountsWhenClearing() {
            // Given
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q2, "2025/26", SubmissionStatus.REJECTED
            ));

            // When
            viewModel.clearAll();

            // Then
            assertThat(viewModel.getAcceptedCount()).isZero();
            assertThat(viewModel.getRejectedCount()).isZero();
            assertThat(viewModel.getPendingCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Statistics Calculation")
    class StatisticsCalculation {

        @Test
        @DisplayName("should calculate counts for mixed statuses")
        void shouldCalculateCountsForMixedStatuses() {
            // Given & When
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q2, "2025/26", SubmissionStatus.ACCEPTED
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q3, "2025/26", SubmissionStatus.PENDING
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q4, "2025/26", SubmissionStatus.REJECTED
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.ANNUAL, "2025/26", SubmissionStatus.SUBMITTED
            ));

            // Then
            assertThat(viewModel.getTotalCount()).isEqualTo(5);
            assertThat(viewModel.getAcceptedCount()).isEqualTo(2);
            assertThat(viewModel.getPendingCount()).isEqualTo(1);
            assertThat(viewModel.getRejectedCount()).isEqualTo(1);
            // SUBMITTED counts separately (not in accepted/pending/rejected)
        }

        @Test
        @DisplayName("should count submitted status separately")
        void shouldCountSubmittedStatusSeparately() {
            // Given & When
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.SUBMITTED
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q2, "2025/26", SubmissionStatus.SUBMITTED
            ));

            // Then
            assertThat(viewModel.getSubmittedCount()).isEqualTo(2);
            assertThat(viewModel.getAcceptedCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Set Submissions List")
    class SetSubmissionsList {

        @Test
        @DisplayName("should set submissions from list")
        void shouldSetSubmissionsFromList() {
            // Given
            List<SubmissionTableRow> submissions = List.of(
                createSubmission(SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED),
                createSubmission(SubmissionType.QUARTERLY_Q2, "2025/26", SubmissionStatus.PENDING),
                createSubmission(SubmissionType.ANNUAL, "2024/25", SubmissionStatus.ACCEPTED)
            );

            // When
            viewModel.setSubmissions(submissions);

            // Then
            assertThat(viewModel.getSubmissions()).hasSize(3);
            assertThat(viewModel.getTotalCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should replace existing submissions")
        void shouldReplaceExistingSubmissions() {
            // Given
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            ));

            List<SubmissionTableRow> newSubmissions = List.of(
                createSubmission(SubmissionType.ANNUAL, "2024/25", SubmissionStatus.REJECTED)
            );

            // When
            viewModel.setSubmissions(newSubmissions);

            // Then
            assertThat(viewModel.getSubmissions()).hasSize(1);
            assertThat(viewModel.getTotalCount()).isEqualTo(1);
            assertThat(viewModel.getRejectedCount()).isEqualTo(1);
        }
    }

    // =========================================================================
    // PS11-004: Quarterly Updates Placeholder Tests
    // =========================================================================
    @Nested
    @DisplayName("PS11-004: Quarterly Updates Placeholder")
    class Ps11004QuarterlyUpdatesPlaceholder {

        @Test
        @DisplayName("QU-U01: should show quarterly submission placeholder message")
        void shouldShowQuarterlySubmissionPlaceholderMessage() {
            // The ViewModel should indicate quarterly updates feature status
            assertThat(viewModel.getQuarterlyUpdatesMessage())
                .isEqualTo("Quarterly updates coming April 2026 for income over GBP50,000");
        }

        @Test
        @DisplayName("QU-U02: should return correct feature coming date")
        void shouldReturnCorrectFeatureComingDate() {
            assertThat(viewModel.getQuarterlyUpdatesAvailableDate())
                .isEqualTo("April 2026");
        }

        @Test
        @DisplayName("QU-U03: should return correct income threshold for quarterly updates")
        void shouldReturnCorrectIncomeThresholdForQuarterlyUpdates() {
            assertThat(viewModel.getQuarterlyUpdatesIncomeThreshold())
                .isEqualTo("GBP50,000");
        }

        @Test
        @DisplayName("QU-U04: should indicate quarterly updates not yet available")
        void shouldIndicateQuarterlyUpdatesNotYetAvailable() {
            assertThat(viewModel.isQuarterlyUpdatesAvailable()).isFalse();
        }

        @Test
        @DisplayName("QU-U05: should return HMRC MTD information link")
        void shouldReturnHmrcMtdInformationLink() {
            assertThat(viewModel.getQuarterlyUpdatesInfoUrl())
                .isEqualTo("https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax");
        }

        @Test
        @DisplayName("QU-U06: should return learn more button text")
        void shouldReturnLearnMoreButtonText() {
            assertThat(viewModel.getQuarterlyUpdatesLearnMoreText())
                .isEqualTo("Learn more about Making Tax Digital");
        }

        @Test
        @DisplayName("QU-U07: should count quarterly submissions by type")
        void shouldCountQuarterlySubmissionsByType() {
            // Given
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q2, "2025/26", SubmissionStatus.PENDING
            ));
            viewModel.addSubmission(createSubmission(
                SubmissionType.ANNUAL, "2025/26", SubmissionStatus.ACCEPTED
            ));

            // When
            int quarterlyCount = viewModel.getQuarterlySubmissionCount();

            // Then
            assertThat(quarterlyCount).isEqualTo(2);
        }

        @Test
        @DisplayName("QU-U08: should identify next quarterly deadline")
        void shouldIdentifyNextQuarterlyDeadline() {
            // The ViewModel should provide information about quarterly deadlines
            assertThat(viewModel.getNextQuarterlyDeadlineInfo())
                .contains("Q"); // Should contain quarter info
        }

        @Test
        @DisplayName("QU-U09: should show placeholder for quarterly submission types")
        void shouldShowPlaceholderForQuarterlySubmissionTypes() {
            // Given - no quarterly submissions
            assertThat(viewModel.isEmptyState()).isTrue();

            // Then - should indicate quarterly updates message
            assertThat(viewModel.getQuarterlyUpdatesMessage()).isNotBlank();
        }

        @Test
        @DisplayName("QU-U10: should hide placeholder after quarterly submissions exist")
        void shouldHidePlaceholderAfterQuarterlySubmissionsExist() {
            // Given
            viewModel.addSubmission(createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            ));

            // Then - empty state should be hidden
            assertThat(viewModel.isEmptyState()).isFalse();
        }
    }

    // =========================================================================
    // PS11-005: Button State Fix Tests
    // =========================================================================
    @Nested
    @DisplayName("PS11-005: Button State Tests")
    class Ps11005ButtonStateTests {

        @Test
        @DisplayName("BTN-U01: should indicate when action button is enabled")
        void shouldIndicateWhenActionButtonIsEnabled() {
            // Given - a submission is selected
            SubmissionTableRow submission = createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            );
            viewModel.addSubmission(submission);
            viewModel.selectSubmission(submission);

            // Then
            assertThat(viewModel.isViewDetailsEnabled()).isTrue();
        }

        @Test
        @DisplayName("BTN-U02: should indicate when action button is disabled")
        void shouldIndicateWhenActionButtonIsDisabled() {
            // Given - no submission selected
            assertThat(viewModel.hasSelection()).isFalse();

            // Then
            assertThat(viewModel.isViewDetailsEnabled()).isFalse();
        }

        @Test
        @DisplayName("BTN-U03: should return tooltip for disabled state (no selection)")
        void shouldReturnTooltipForDisabledStateNoSelection() {
            // Given - no selection
            assertThat(viewModel.hasSelection()).isFalse();

            // Then
            assertThat(viewModel.getViewDetailsTooltip())
                .isEqualTo("Select a submission to view details");
        }

        @Test
        @DisplayName("BTN-U04: should return tooltip for enabled state")
        void shouldReturnTooltipForEnabledState() {
            // Given
            SubmissionTableRow submission = createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            );
            viewModel.addSubmission(submission);
            viewModel.selectSubmission(submission);

            // Then
            assertThat(viewModel.getViewDetailsTooltip())
                .isEqualTo("View submission details");
        }

        @Test
        @DisplayName("BTN-U05: should expose view details enabled property for binding")
        void shouldExposeViewDetailsEnabledPropertyForBinding() {
            assertThat(viewModel.viewDetailsEnabledProperty()).isNotNull();
        }

        @Test
        @DisplayName("BTN-U06: viewDetailsEnabled should update when selection changes")
        void viewDetailsEnabledShouldUpdateWhenSelectionChanges() {
            // Given
            SubmissionTableRow submission = createSubmission(
                SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED
            );
            viewModel.addSubmission(submission);
            assertThat(viewModel.viewDetailsEnabledProperty().get()).isFalse();

            // When
            viewModel.selectSubmission(submission);

            // Then
            assertThat(viewModel.viewDetailsEnabledProperty().get()).isTrue();

            // When
            viewModel.clearSelection();

            // Then
            assertThat(viewModel.viewDetailsEnabledProperty().get()).isFalse();
        }

        @Test
        @DisplayName("BTN-U07: should disable export button when no submissions")
        void shouldDisableExportButtonWhenNoSubmissions() {
            // Given - empty state
            assertThat(viewModel.isEmptyState()).isTrue();

            // Then
            assertThat(viewModel.isExportEnabled()).isFalse();
        }

        @Test
        @DisplayName("BTN-U08: should enable export button when submissions exist")
        void shouldEnableExportButtonWhenSubmissionsExist() {
            // Given
            viewModel.addSubmission(createSubmission(
                SubmissionType.ANNUAL, "2025/26", SubmissionStatus.ACCEPTED
            ));

            // Then
            assertThat(viewModel.isExportEnabled()).isTrue();
        }

        @Test
        @DisplayName("BTN-U09: should return export tooltip for disabled state")
        void shouldReturnExportTooltipForDisabledState() {
            // Given - empty state
            assertThat(viewModel.isEmptyState()).isTrue();

            // Then
            assertThat(viewModel.getExportTooltip())
                .isEqualTo("No submissions to export");
        }

        @Test
        @DisplayName("BTN-U10: should return export tooltip for enabled state")
        void shouldReturnExportTooltipForEnabledState() {
            // Given
            viewModel.addSubmission(createSubmission(
                SubmissionType.ANNUAL, "2025/26", SubmissionStatus.ACCEPTED
            ));

            // Then
            assertThat(viewModel.getExportTooltip())
                .isEqualTo("Export submission history");
        }
    }

    // === Helper Methods ===

    private SubmissionTableRow createSubmission(
            SubmissionType type,
            String taxYear,
            SubmissionStatus status
    ) {
        return SubmissionTableRow.builder()
            .id(System.nanoTime())
            .submittedAt(LocalDateTime.now())
            .type(type)
            .taxYear(taxYear)
            .status(status)
            .hmrcReference("REF-" + System.nanoTime())
            .totalIncome(new BigDecimal("12500.00"))
            .totalExpenses(new BigDecimal("2500.00"))
            .netProfit(new BigDecimal("10000.00"))
            .taxDue(BigDecimal.ZERO)
            .build();
    }

    private SubmissionTableRow createSubmissionWithTaxYear(String taxYear, SubmissionType type) {
        return SubmissionTableRow.builder()
            .id(System.nanoTime())
            .submittedAt(LocalDateTime.now())
            .type(type)
            .taxYear(taxYear)
            .status(SubmissionStatus.ACCEPTED)
            .hmrcReference("REF-" + System.nanoTime())
            .totalIncome(new BigDecimal("12500.00"))
            .totalExpenses(new BigDecimal("2500.00"))
            .netProfit(new BigDecimal("10000.00"))
            .build();
    }

    private SubmissionTableRow createSubmissionWithDate(LocalDateTime dateTime, String taxYear) {
        return SubmissionTableRow.builder()
            .id(System.nanoTime())
            .submittedAt(dateTime)
            .type(SubmissionType.QUARTERLY_Q1)
            .taxYear(taxYear)
            .status(SubmissionStatus.ACCEPTED)
            .hmrcReference("REF-" + System.nanoTime())
            .totalIncome(new BigDecimal("12500.00"))
            .totalExpenses(new BigDecimal("2500.00"))
            .netProfit(new BigDecimal("10000.00"))
            .build();
    }
}
