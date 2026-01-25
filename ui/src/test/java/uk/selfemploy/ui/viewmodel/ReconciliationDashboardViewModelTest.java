package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Tests for Reconciliation Dashboard (Sprint 10B).
 * Tests REC-U01 through REC-U08 from /rob's test design.
 *
 * <p>SE-10B-007/008: Reconciliation Dashboard Tests</p>
 *
 * @see docs/sprints/sprint-10A/testing/rob-test-design-10A-10B.md
 */
@DisplayName("Reconciliation Dashboard ViewModel Tests (Sprint 10B)")
class ReconciliationDashboardViewModelTest {

    private ReconciliationViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new ReconciliationViewModel();
    }

    // === REC-U01 to REC-U04: Metrics Calculation ===

    @Nested
    @DisplayName("Metrics Calculation")
    class MetricsCalculation {

        @Test
        @DisplayName("REC-U01: should calculate total income records")
        void shouldCalculateTotalIncomeRecords() {
            // Given & When
            viewModel.setMetrics(
                new BigDecimal("45230.00"),
                new BigDecimal("12450.75"),
                142, 387, 3, 8
            );

            // Then
            assertThat(viewModel.getIncomeCount()).isEqualTo(142);
            assertThat(viewModel.getIncomeCountText()).isEqualTo("142 records");
        }

        @Test
        @DisplayName("REC-U02: should calculate total expense records")
        void shouldCalculateTotalExpenseRecords() {
            // Given & When
            viewModel.setMetrics(
                new BigDecimal("45230.00"),
                new BigDecimal("12450.75"),
                142, 387, 3, 8
            );

            // Then
            assertThat(viewModel.getExpenseCount()).isEqualTo(387);
            assertThat(viewModel.getExpenseCountText()).isEqualTo("387 records");
        }

        @Test
        @DisplayName("REC-U03: should calculate total amounts")
        void shouldCalculateTotalAmounts() {
            // Given & When
            viewModel.setMetrics(
                new BigDecimal("45230.00"),
                new BigDecimal("12450.75"),
                142, 387, 3, 8
            );

            // Then
            assertThat(viewModel.getTotalIncome()).isEqualByComparingTo(new BigDecimal("45230.00"));
            assertThat(viewModel.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("12450.75"));
        }

        @Test
        @DisplayName("REC-U04: should show last checked time")
        void shouldShowLastCheckedTime() {
            // Given - ViewModel initialized (last checked set to now)

            // Then
            assertThat(viewModel.getLastChecked()).isNotNull();
            assertThat(viewModel.getLastCheckedText()).isIn("Just now", "1 minute ago");
        }
    }

    // === REC-U05 to REC-U08: Warning Detection ===

    @Nested
    @DisplayName("Warning Detection")
    class WarningDetection {

        @Test
        @DisplayName("REC-U05: should detect potential duplicates")
        void shouldDetectPotentialDuplicates() {
            // Given & When
            viewModel.setMetrics(
                new BigDecimal("45230.00"),
                new BigDecimal("12450.75"),
                142, 387, 5, 8
            );

            // Then
            assertThat(viewModel.hasDuplicates()).isTrue();
            assertThat(viewModel.getDuplicateCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("REC-U06: should detect missing categories")
        void shouldDetectMissingCategories() {
            // Given & When
            viewModel.setMetrics(
                new BigDecimal("45230.00"),
                new BigDecimal("12450.75"),
                142, 387, 3, 12
            );

            // Then
            assertThat(viewModel.hasUncategorized()).isTrue();
            assertThat(viewModel.getUncategorizedCount()).isEqualTo(12);
        }

        @Test
        @DisplayName("REC-U07: should detect date gaps as issue")
        void shouldDetectDateGaps() {
            // Given
            ReconciliationIssue gapsIssue = ReconciliationIssue.dateGaps(2,
                List.of("October 2025", "November 2025"));

            // When
            viewModel.setIssues(List.of(gapsIssue));

            // Then
            assertThat(viewModel.getIssueCount()).isEqualTo(1);
            assertThat(viewModel.getIssues().get(0).getType())
                .isEqualTo(ReconciliationIssueType.DATE_GAPS);
            assertThat(viewModel.getIssues().get(0).getSeverity())
                .isEqualTo(IssueSeverity.LOW);
        }

        @Test
        @DisplayName("REC-U08: should flag duplicates as high priority")
        void shouldFlagDuplicatesAsHighPriority() {
            // Given
            ReconciliationIssue duplicatesIssue = ReconciliationIssue.duplicates(3,
                List.of("Amazon - 23.99", "Amazon - 23.99"));

            // When
            viewModel.setIssues(List.of(duplicatesIssue));

            // Then
            assertThat(viewModel.getIssues().get(0).getSeverity())
                .isEqualTo(IssueSeverity.HIGH);
            assertThat(viewModel.getIssues().get(0).getType())
                .isEqualTo(ReconciliationIssueType.POTENTIAL_DUPLICATES);
        }
    }

    // === Additional Tests ===

    @Nested
    @DisplayName("All Clear State")
    class AllClearState {

        @Test
        @DisplayName("should show all clear when no issues")
        void shouldShowAllClearWhenNoIssues() {
            // Given & When
            viewModel.setMetrics(
                new BigDecimal("45230.00"),
                new BigDecimal("12450.75"),
                142, 387, 0, 0  // No duplicates, no uncategorized
            );
            viewModel.setIssues(List.of());  // No issues

            // Then
            assertThat(viewModel.isAllClear()).isTrue();
            assertThat(viewModel.hasDuplicates()).isFalse();
            assertThat(viewModel.hasUncategorized()).isFalse();
        }

        @Test
        @DisplayName("should not be all clear when issues exist")
        void shouldNotBeAllClearWhenIssuesExist() {
            // Given
            ReconciliationIssue issue = ReconciliationIssue.missingCategories(5);

            // When
            viewModel.setIssues(List.of(issue));

            // Then
            assertThat(viewModel.isAllClear()).isFalse();
        }
    }

    @Nested
    @DisplayName("Issue Management")
    class IssueManagement {

        @Test
        @DisplayName("should sort issues by severity")
        void shouldSortIssuesBySeverity() {
            // Given - issues in wrong order
            List<ReconciliationIssue> issues = new ArrayList<>();
            issues.add(ReconciliationIssue.dateGaps(2, List.of("Oct", "Nov")));  // LOW
            issues.add(ReconciliationIssue.duplicates(3, List.of("A", "B")));    // HIGH
            issues.add(ReconciliationIssue.missingCategories(5));                 // MEDIUM

            // When
            viewModel.setIssues(issues);

            // Then - sorted by severity (HIGH, MEDIUM, LOW)
            List<ReconciliationIssue> sorted = viewModel.getIssues();
            assertThat(sorted.get(0).getSeverity()).isEqualTo(IssueSeverity.HIGH);
            assertThat(sorted.get(1).getSeverity()).isEqualTo(IssueSeverity.MEDIUM);
            assertThat(sorted.get(2).getSeverity()).isEqualTo(IssueSeverity.LOW);
        }

        @Test
        @DisplayName("should filter issues by severity")
        void shouldFilterIssuesBySeverity() {
            // Given
            List<ReconciliationIssue> issues = List.of(
                ReconciliationIssue.duplicates(3, List.of("A")),
                ReconciliationIssue.missingCategories(5),
                ReconciliationIssue.dateGaps(2, List.of("Oct"))
            );
            viewModel.setIssues(issues);

            // When
            List<ReconciliationIssue> highIssues = viewModel.getIssuesBySeverity(IssueSeverity.HIGH);

            // Then
            assertThat(highIssues).hasSize(1);
            assertThat(highIssues.get(0).getSeverity()).isEqualTo(IssueSeverity.HIGH);
        }

        @Test
        @DisplayName("should remove resolved issue")
        void shouldRemoveResolvedIssue() {
            // Given
            ReconciliationIssue issue = ReconciliationIssue.missingCategories(5);
            viewModel.setIssues(List.of(issue));
            assertThat(viewModel.getIssueCount()).isEqualTo(1);

            // When
            viewModel.removeIssue(issue);

            // Then
            assertThat(viewModel.getIssueCount()).isEqualTo(0);
            assertThat(viewModel.isAllClear()).isTrue();
        }
    }

    @Nested
    @DisplayName("Formatting")
    class Formatting {

        @Test
        @DisplayName("should format currency values")
        void shouldFormatCurrencyValues() {
            // Given & When
            viewModel.setMetrics(
                new BigDecimal("45230.00"),
                new BigDecimal("12450.75"),
                142, 387, 3, 8
            );

            // Then
            assertThat(viewModel.getFormattedTotalIncome()).contains("45,230.00");
            assertThat(viewModel.getFormattedTotalExpenses()).contains("12,450.75");
        }

        @Test
        @DisplayName("should handle single record text")
        void shouldHandleSingleRecordText() {
            // Given & When
            viewModel.setMetrics(
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                1, 1, 0, 0
            );

            // Then
            assertThat(viewModel.getIncomeCountText()).isEqualTo("1 record");
            assertThat(viewModel.getExpenseCountText()).isEqualTo("1 record");
        }
    }
}
