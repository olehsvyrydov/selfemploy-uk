package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Tests for Import Review UI (Sprint 10B).
 * Tests REV-U01 through REV-U08 from /rob's test design.
 *
 * <p>SE-10B-004/005: Import Review UI Tests</p>
 *
 * @see docs/sprints/sprint-10A/testing/rob-test-design-10A-10B.md
 */
@DisplayName("Import Review ViewModel Tests (Sprint 10B)")
class ImportReviewViewModelTest {

    private ImportReviewViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new ImportReviewViewModel();
    }

    // === REV-U01 to REV-U04: Loading and Display ===

    @Nested
    @DisplayName("Loading and Display")
    class LoadingAndDisplay {

        @Test
        @DisplayName("REV-U01: should load import analysis into table")
        void shouldLoadImportAnalysisIntoTable() {
            // Given
            List<ImportCandidateViewModel> candidates = createSampleCandidates();

            // When
            viewModel.setCandidates(candidates);

            // Then
            assertThat(viewModel.getCandidates()).hasSize(5);
            assertThat(viewModel.getTotalCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("REV-U02: should show correct action for EXACT match")
        void shouldShowCorrectActionForExactMatch() {
            // Given
            ImportCandidateViewModel exact = createCandidate(MatchType.EXACT, new BigDecimal("-50.00"));

            // When
            viewModel.setCandidates(List.of(exact));

            // Then - EXACT matches should default to SKIP
            assertThat(viewModel.getCandidates().get(0).getAction()).isEqualTo(ImportAction.SKIP);
        }

        @Test
        @DisplayName("REV-U03: should show correct action for LIKELY match")
        void shouldShowCorrectActionForLikelyMatch() {
            // Given
            ImportCandidateViewModel likely = createCandidate(MatchType.LIKELY, new BigDecimal("100.00"));

            // When
            viewModel.setCandidates(List.of(likely));

            // Then - LIKELY matches should default to IMPORT (user reviews)
            assertThat(viewModel.getCandidates().get(0).getAction()).isEqualTo(ImportAction.IMPORT);
        }

        @Test
        @DisplayName("REV-U04: should show correct action for no match")
        void shouldShowCorrectActionForNoMatch() {
            // Given
            ImportCandidateViewModel newItem = createCandidate(MatchType.NEW, new BigDecimal("200.00"));

            // When
            viewModel.setCandidates(List.of(newItem));

            // Then - NEW items should default to IMPORT
            assertThat(viewModel.getCandidates().get(0).getAction()).isEqualTo(ImportAction.IMPORT);
        }
    }

    // === REV-U05 to REV-U08: Action Selection and Commit ===

    @Nested
    @DisplayName("Action Selection and Commit")
    class ActionSelectionAndCommit {

        @Test
        @DisplayName("REV-U05: should allow action change via dropdown")
        void shouldAllowActionChangeViaDropdown() {
            // Given
            ImportCandidateViewModel candidate = createCandidate(MatchType.EXACT, new BigDecimal("-50.00"));
            viewModel.setCandidates(List.of(candidate));
            assertThat(candidate.getAction()).isEqualTo(ImportAction.SKIP);

            // When
            candidate.setAction(ImportAction.UPDATE);

            // Then
            assertThat(viewModel.getCandidates().get(0).getAction()).isEqualTo(ImportAction.UPDATE);
        }

        @Test
        @DisplayName("REV-U06: should calculate summary counts correctly")
        void shouldCalculateSummaryCountsCorrectly() {
            // Given
            List<ImportCandidateViewModel> candidates = createSampleCandidates();

            // When
            viewModel.setCandidates(candidates);

            // Then
            assertThat(viewModel.getNewCount()).isEqualTo(2);
            assertThat(viewModel.getExactCount()).isEqualTo(2);
            assertThat(viewModel.getLikelyCount()).isEqualTo(1);
            assertThat(viewModel.getTotalCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("REV-U07: should enable commit when no review pending")
        void shouldEnableCommitWhenNoReviewPending() {
            // Given - only NEW items (no duplicates to review)
            List<ImportCandidateViewModel> candidates = List.of(
                createCandidate(MatchType.NEW, new BigDecimal("100.00")),
                createCandidate(MatchType.NEW, new BigDecimal("200.00"))
            );

            // When
            viewModel.setCandidates(candidates);

            // Then - all clear, can import
            assertThat(viewModel.isAllNew()).isTrue();
            assertThat(viewModel.hasNoDuplicates()).isTrue();
            assertThat(viewModel.hasItemsToImport()).isTrue();
        }

        @Test
        @DisplayName("REV-U08: should disable commit when reviews pending")
        void shouldDisableCommitWhenReviewsPending() {
            // Given - all items set to SKIP
            List<ImportCandidateViewModel> candidates = createSampleCandidates();
            candidates.forEach(c -> c.setAction(ImportAction.SKIP));

            // When
            viewModel.setCandidates(candidates);

            // Then - nothing to import
            assertThat(viewModel.hasItemsToImport()).isFalse();
            assertThat(viewModel.getImportCount()).isEqualTo(0);
        }
    }

    // === Additional Tests ===

    @Nested
    @DisplayName("Bulk Actions")
    class BulkActions {

        @Test
        @DisplayName("should import all new items")
        void shouldImportAllNew() {
            // Given
            List<ImportCandidateViewModel> candidates = createSampleCandidates();
            candidates.forEach(c -> c.setAction(ImportAction.SKIP));
            viewModel.setCandidates(candidates);

            // When
            viewModel.importAllNew();

            // Then - only NEW items should be IMPORT
            for (ImportCandidateViewModel c : viewModel.getCandidates()) {
                if (c.getMatchType() == MatchType.NEW) {
                    assertThat(c.getAction()).isEqualTo(ImportAction.IMPORT);
                } else {
                    assertThat(c.getAction()).isEqualTo(ImportAction.SKIP);
                }
            }
        }

        @Test
        @DisplayName("should skip all duplicates")
        void shouldSkipAllDuplicates() {
            // Given
            List<ImportCandidateViewModel> candidates = createSampleCandidates();
            candidates.forEach(c -> c.setAction(ImportAction.IMPORT));
            viewModel.setCandidates(candidates);

            // When
            viewModel.skipAllDuplicates();

            // Then - only EXACT items should be SKIP
            for (ImportCandidateViewModel c : viewModel.getCandidates()) {
                if (c.getMatchType() == MatchType.EXACT) {
                    assertThat(c.getAction()).isEqualTo(ImportAction.SKIP);
                } else {
                    assertThat(c.getAction()).isEqualTo(ImportAction.IMPORT);
                }
            }
        }
    }

    @Nested
    @DisplayName("Import Totals")
    class ImportTotals {

        @Test
        @DisplayName("should calculate total income to import")
        void shouldCalculateTotalIncomeToImport() {
            // Given
            List<ImportCandidateViewModel> candidates = List.of(
                createCandidate(MatchType.NEW, new BigDecimal("100.00")),
                createCandidate(MatchType.NEW, new BigDecimal("200.00")),
                createCandidate(MatchType.EXACT, new BigDecimal("-50.00"))
            );
            viewModel.setCandidates(candidates);

            // When
            BigDecimal totalIncome = viewModel.getTotalIncomeToImport();

            // Then - only NEW items (income) are imported
            assertThat(totalIncome).isEqualByComparingTo(new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("should calculate total expenses to import")
        void shouldCalculateTotalExpensesToImport() {
            // Given
            List<ImportCandidateViewModel> candidates = List.of(
                createCandidate(MatchType.NEW, new BigDecimal("-100.00")),
                createCandidate(MatchType.NEW, new BigDecimal("-50.00")),
                createCandidate(MatchType.EXACT, new BigDecimal("200.00"))
            );
            viewModel.setCandidates(candidates);

            // When
            BigDecimal totalExpenses = viewModel.getTotalExpensesToImport();

            // Then
            assertThat(totalExpenses).isEqualByComparingTo(new BigDecimal("150.00"));
        }
    }

    // === Helper Methods ===

    private List<ImportCandidateViewModel> createSampleCandidates() {
        List<ImportCandidateViewModel> list = new ArrayList<>();
        list.add(createCandidate(MatchType.NEW, new BigDecimal("100.00")));
        list.add(createCandidate(MatchType.NEW, new BigDecimal("200.00")));
        list.add(createCandidate(MatchType.EXACT, new BigDecimal("-50.00")));
        list.add(createCandidate(MatchType.EXACT, new BigDecimal("-75.00")));
        list.add(createCandidate(MatchType.LIKELY, new BigDecimal("150.00")));
        return list;
    }

    private ImportCandidateViewModel createCandidate(MatchType matchType, BigDecimal amount) {
        UUID matchedId = matchType != MatchType.NEW ? UUID.randomUUID() : null;
        return new ImportCandidateViewModel(
            UUID.randomUUID(),
            LocalDate.now(),
            "Test Transaction " + matchType,
            amount,
            matchType,
            matchedId
        );
    }
}
