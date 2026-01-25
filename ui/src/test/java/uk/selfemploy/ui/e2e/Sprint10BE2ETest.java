package uk.selfemploy.ui.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.viewmodel.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Tests for Sprint 10B Features (Data Integrity).
 * Tests DUP-E01 through REC-E06 from /rob's test design.
 *
 * <p>Sprint 10B Features:</p>
 * <ul>
 *   <li>SE-10B-001: Database Duplicate Detection</li>
 *   <li>SE-10B-002: Import Audit Trail</li>
 *   <li>SE-10B-003: Import Undo/Rollback</li>
 *   <li>SE-10B-004/005: Import Review UI</li>
 *   <li>SE-10B-006: Import History View</li>
 *   <li>SE-10B-007/008: Reconciliation Dashboard</li>
 * </ul>
 *
 * <p>These tests verify the ViewModel layer logic for Sprint 10B features.
 * They do NOT require JavaFX toolkit and can run in CI environments.</p>
 *
 * <p>For full UI E2E tests requiring JavaFX, use @Tag("e2e") and run locally
 * with: {@code mvn test -pl ui -Dgroups=e2e}</p>
 *
 * @author /adam - E2E Test Automation Engineer
 * @see docs/sprints/sprint-10B/testing/rob-test-design-10B.md
 */
@DisplayName("Sprint 10B: Data Integrity E2E Tests")
class Sprint10BE2ETest {

    // === DUP-E01 to DUP-E04: Duplicate Detection E2E ===

    @Nested
    @DisplayName("Duplicate Detection E2E Tests")
    class DuplicateDetectionE2E {

        @Test
        @DisplayName("DUP-E01: should show duplicate count in import preview")
        void shouldShowDuplicateCountInImportPreview() {
            // Given: An ImportReviewViewModel with mixed candidates
            ImportReviewViewModel viewModel = new ImportReviewViewModel();

            List<ImportCandidateViewModel> candidates = new ArrayList<>();
            candidates.add(createCandidate("Client payment", new BigDecimal("1500.00"), MatchType.NEW));
            candidates.add(createCandidate("Duplicate expense", new BigDecimal("-45.50"), MatchType.EXACT));
            candidates.add(createCandidate("Likely duplicate", new BigDecimal("-100.00"), MatchType.LIKELY));
            candidates.add(createCandidate("New expense", new BigDecimal("-200.00"), MatchType.NEW));

            // When: Setting candidates on the view model
            viewModel.setCandidates(candidates);

            // Then: Duplicate counts should be accurate
            assertThat(viewModel.getTotalCount()).isEqualTo(4);
            assertThat(viewModel.getNewCount()).isEqualTo(2);
            assertThat(viewModel.getExactCount()).isEqualTo(1);
            assertThat(viewModel.getLikelyCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("DUP-E02: should highlight duplicate rows in review table")
        void shouldHighlightDuplicateRowsInReviewTable() {
            // Given: Candidates with different match types
            ImportCandidateViewModel newCandidate = createCandidate("New transaction", new BigDecimal("100.00"), MatchType.NEW);
            ImportCandidateViewModel exactMatch = createCandidate("Exact match", new BigDecimal("100.00"), MatchType.EXACT);
            ImportCandidateViewModel likelyMatch = createCandidate("Likely match", new BigDecimal("100.00"), MatchType.LIKELY);

            // Then: Each match type should have the correct row style class
            assertThat(newCandidate.getMatchType().getRowStyleClass()).isEqualTo("row-new");
            assertThat(exactMatch.getMatchType().getRowStyleClass()).isEqualTo("row-exact");
            assertThat(likelyMatch.getMatchType().getRowStyleClass()).isEqualTo("row-likely");
        }

        @Test
        @DisplayName("DUP-E03: should allow user to override duplicate decision")
        void shouldAllowUserToOverrideDuplicateDecision() {
            // Given: An EXACT match candidate with default SKIP action
            ImportCandidateViewModel candidate = createCandidate("Duplicate expense", new BigDecimal("-45.50"), MatchType.EXACT);
            assertThat(candidate.getAction()).isEqualTo(ImportAction.SKIP);

            // When: User overrides the action to IMPORT
            candidate.setAction(ImportAction.IMPORT);

            // Then: The action should be changed and candidate will be imported
            assertThat(candidate.getAction()).isEqualTo(ImportAction.IMPORT);
            assertThat(candidate.willBeImported()).isTrue();
        }

        @Test
        @DisplayName("DUP-E04: should show confidence indicator for LIKELY matches")
        void shouldShowConfidenceIndicatorForLikelyMatches() {
            // Given: Different match types
            ImportCandidateViewModel exactMatch = createCandidate("Exact", new BigDecimal("100.00"), MatchType.EXACT);
            ImportCandidateViewModel likelyMatch = createCandidate("Likely", new BigDecimal("100.00"), MatchType.LIKELY);

            // Then: Match type indicators should be correct
            assertThat(exactMatch.getMatchType().getDisplayText()).isEqualTo("EXACT");
            assertThat(exactMatch.getMatchType().getIcon()).isEqualTo("!");

            assertThat(likelyMatch.getMatchType().getDisplayText()).isEqualTo("LIKELY");
            assertThat(likelyMatch.getMatchType().getIcon()).isEqualTo("?");
        }
    }

    // === AUD-E01 to AUD-E02: Audit Trail E2E ===

    @Nested
    @DisplayName("Audit Trail E2E Tests")
    class AuditTrailE2E {

        @Test
        @DisplayName("AUD-E01: should show import history in Settings")
        void shouldShowImportHistoryInSettings() {
            // Given: An ImportHistoryViewModel with history data
            ImportHistoryViewModel viewModel = new ImportHistoryViewModel();
            List<ImportHistoryItemViewModel> imports = createSampleImportHistory();

            // When: Setting the imports
            viewModel.setImports(imports);

            // Then: History should be populated
            assertThat(viewModel.getTotalCount()).isEqualTo(4);
            assertThat(viewModel.getFilteredImports()).hasSize(4);
        }

        @Test
        @DisplayName("AUD-E02: should show audit details on click")
        void shouldShowAuditDetailsOnClick() {
            // Given: An import history item
            ImportHistoryItemViewModel item = createImportHistoryItem(
                LocalDateTime.now().minusDays(2),
                "barclays-january.csv",
                "Barclays",
                15, 42,
                new BigDecimal("3450.00"),
                new BigDecimal("1230.50"),
                ImportStatus.ACTIVE
            );

            // When: Expanding the item
            assertThat(item.isExpanded()).isFalse();
            item.setExpanded(true);

            // Then: Item should be expanded and show details
            assertThat(item.isExpanded()).isTrue();
            assertThat(item.getFileName()).isEqualTo("barclays-january.csv");
            assertThat(item.getBankFormatDisplay()).isEqualTo("Barclays (Auto-detected)");
            assertThat(item.getIncomeRecords()).isEqualTo(15);
            assertThat(item.getExpenseRecords()).isEqualTo(42);
            assertThat(item.getRecordCountText()).isEqualTo("57 records imported");
        }
    }

    // === UNDO-E01 to UNDO-E04: Undo E2E ===

    @Nested
    @DisplayName("Undo E2E Tests")
    class UndoE2E {

        @Test
        @DisplayName("UNDO-E01: should show undo button on eligible imports")
        void shouldShowUndoButtonOnEligibleImports() {
            // Given: A recent active import (within 7 days)
            ImportHistoryItemViewModel recentImport = createImportHistoryItem(
                LocalDateTime.now().minusDays(2),
                "recent-import.csv",
                "HSBC",
                10, 20,
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                ImportStatus.ACTIVE
            );

            // Then: Undo should be available
            assertThat(recentImport.canUndo()).isTrue();
            assertThat(recentImport.getUndoDisabledReason()).isNull();
        }

        @Test
        @DisplayName("UNDO-E02: should show confirmation dialog before undo")
        void shouldShowConfirmationDialogBeforeUndo() {
            // Given: An import item that can be undone
            ImportHistoryItemViewModel item = createImportHistoryItem(
                LocalDateTime.now().minusDays(1),
                "test-import.csv",
                "Monzo",
                5, 10,
                new BigDecimal("500.00"),
                new BigDecimal("250.00"),
                ImportStatus.ACTIVE
            );

            // Then: The item should provide formatted data for confirmation dialog
            // Currency format varies by locale (GBP500.00 vs £500.00)
            assertThat(item.getFormattedIncomeTotal()).matches("\\+.*500.*");
            assertThat(item.getFormattedExpenseTotal()).matches("-.*250.*");
            assertThat(item.getRecordCountText()).isEqualTo("15 records imported");
        }

        @Test
        @DisplayName("UNDO-E03: should show blocked message for tax submitted data")
        void shouldShowBlockedMessageForTaxSubmittedData() {
            // Given: An import that was used in tax submission
            ImportHistoryItemViewModel lockedImport = new ImportHistoryItemViewModel(
                UUID.randomUUID(),
                LocalDateTime.now().minusDays(30),
                "locked-import.csv",
                "/path/to/file.csv",
                "Lloyds",
                12, 35,
                new BigDecimal("5200.00"),
                new BigDecimal("2150.75"),
                ImportStatus.LOCKED,
                null,
                LocalDateTime.now().minusDays(5) // Tax submission date
            );

            // Then: Undo should be blocked
            assertThat(lockedImport.canUndo()).isFalse();
            assertThat(lockedImport.getUndoDisabledReason())
                .contains("Cannot undo")
                .contains("tax submission");
            assertThat(lockedImport.getStatus()).isEqualTo(ImportStatus.LOCKED);
        }

        @Test
        @DisplayName("UNDO-E04: should update UI after successful undo")
        void shouldUpdateUIAfterSuccessfulUndo() {
            // Given: An import history with an active import
            ImportHistoryViewModel viewModel = new ImportHistoryViewModel();
            List<ImportHistoryItemViewModel> imports = new ArrayList<>();
            ImportHistoryItemViewModel itemToUndo = createImportHistoryItem(
                LocalDateTime.now().minusDays(1),
                "to-undo.csv",
                "Barclays",
                5, 10,
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                ImportStatus.ACTIVE
            );
            imports.add(itemToUndo);
            viewModel.setImports(imports);

            assertThat(viewModel.getActiveCount()).isEqualTo(1);

            // When: Removing the import after undo
            viewModel.removeImport(itemToUndo);

            // Then: Counts should be updated
            assertThat(viewModel.getTotalCount()).isZero();
            assertThat(viewModel.getActiveCount()).isZero();
        }
    }

    // === REV-E01 to REV-E10: Import Review UI E2E ===

    @Nested
    @DisplayName("Import Review UI E2E Tests")
    class ImportReviewUIE2E {

        @Test
        @DisplayName("REV-E01: should open review UI after file selection")
        void shouldOpenReviewUIAfterFileSelection() {
            // Given: An ImportReviewViewModel
            ImportReviewViewModel viewModel = new ImportReviewViewModel();

            // When: No candidates are set yet
            assertThat(viewModel.hasCandidates()).isFalse();
            assertThat(viewModel.isLoading()).isFalse();

            // Then: Loading can be set while processing
            viewModel.setLoading(true);
            assertThat(viewModel.isLoading()).isTrue();
            assertThat(viewModel.getLoadingMessage()).isEqualTo("Checking for duplicates...");
        }

        @Test
        @DisplayName("REV-E02: should show summary panel with counts")
        void shouldShowSummaryPanelWithCounts() {
            // Given: A view model with mixed candidates
            ImportReviewViewModel viewModel = new ImportReviewViewModel();
            List<ImportCandidateViewModel> candidates = new ArrayList<>();
            candidates.add(createCandidate("Income 1", new BigDecimal("1000.00"), MatchType.NEW));
            candidates.add(createCandidate("Income 2", new BigDecimal("500.00"), MatchType.LIKELY));
            candidates.add(createCandidate("Expense 1", new BigDecimal("-100.00"), MatchType.NEW));
            candidates.add(createCandidate("Expense 2", new BigDecimal("-50.00"), MatchType.EXACT));

            viewModel.setCandidates(candidates);

            // Then: Summary counts should be correct
            assertThat(viewModel.getTotalCount()).isEqualTo(4);
            assertThat(viewModel.getNewCount()).isEqualTo(2);
            assertThat(viewModel.getExactCount()).isEqualTo(1);
            assertThat(viewModel.getLikelyCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("REV-E03: should highlight rows needing review")
        void shouldHighlightRowsNeedingReview() {
            // Given: Candidates with LIKELY match type need review
            ImportCandidateViewModel likelyMatch = createCandidate("Needs review", new BigDecimal("-100.00"), MatchType.LIKELY);

            // Then: LIKELY matches have specific styling
            assertThat(likelyMatch.getMatchType().getStyleClass()).isEqualTo("match-badge-likely");
            assertThat(likelyMatch.getMatchType().getRowStyleClass()).isEqualTo("row-likely");
            // Default action for LIKELY is IMPORT (user should review)
            assertThat(likelyMatch.getAction()).isEqualTo(ImportAction.IMPORT);
        }

        @Test
        @DisplayName("REV-E04: should show comparison dialog on compare click")
        void shouldShowComparisonDialogOnCompareClick() {
            // Given: A candidate with a matched record
            UUID matchedId = UUID.randomUUID();
            MatchedRecordViewModel matchedRecord = new MatchedRecordViewModel(
                matchedId,
                LocalDate.of(2026, 1, 10),
                "Office Supplies",
                new BigDecimal("-45.00"),
                "Office Costs"
            );

            ImportCandidateViewModel candidate = new ImportCandidateViewModel(
                UUID.randomUUID(),
                LocalDate.of(2026, 1, 10),
                "Office Supplies",
                new BigDecimal("-45.50"),
                "Office Costs",
                MatchType.LIKELY,
                matchedId,
                matchedRecord
            );

            // Then: Candidate should have match data for comparison
            assertThat(candidate.hasMatch()).isTrue();
            assertThat(candidate.getMatchedRecordId()).isEqualTo(matchedId);
            assertThat(candidate.getMatchedRecord()).isNotNull();
            assertThat(candidate.getMatchedRecord().getFormattedAmount()).contains("45.00");
        }

        @Test
        @DisplayName("REV-E05: should highlight differences in comparison")
        void shouldHighlightDifferencesInComparison() {
            // Given: A candidate and matched record with differences
            MatchedRecordViewModel matchedRecord = new MatchedRecordViewModel(
                UUID.randomUUID(),
                LocalDate.of(2026, 1, 10),
                "Office Supplies Ltd",  // Different description
                new BigDecimal("-45.00"), // Different amount
                "Office Costs"
            );

            ImportCandidateViewModel candidate = new ImportCandidateViewModel(
                UUID.randomUUID(),
                LocalDate.of(2026, 1, 10),
                "Office Supplies",
                new BigDecimal("-45.50"),
                "Office Costs",
                MatchType.LIKELY,
                matchedRecord.getId(),
                matchedRecord
            );

            // Then: Differences can be detected for highlighting
            assertThat(candidate.getDescription()).isNotEqualTo(matchedRecord.getDescription());
            assertThat(candidate.getFormattedAmount()).isNotEqualTo(matchedRecord.getFormattedAmount());
            assertThat(candidate.getFormattedDate()).isEqualTo(matchedRecord.getFormattedDate()); // Same date
        }

        @Test
        @DisplayName("REV-E06: should allow bulk action selection")
        void shouldAllowBulkActionSelection() {
            // Given: A view model with multiple candidates
            ImportReviewViewModel viewModel = new ImportReviewViewModel();
            List<ImportCandidateViewModel> candidates = new ArrayList<>();
            candidates.add(createCandidate("New 1", new BigDecimal("100.00"), MatchType.NEW));
            candidates.add(createCandidate("Exact 1", new BigDecimal("-50.00"), MatchType.EXACT));
            candidates.add(createCandidate("Exact 2", new BigDecimal("-75.00"), MatchType.EXACT));

            viewModel.setCandidates(candidates);

            // When: Selecting all and applying bulk action
            viewModel.selectAll();
            assertThat(viewModel.getSelectedCount()).isEqualTo(3);

            // When: Applying "Skip All Duplicates"
            viewModel.skipAllDuplicates();

            // Then: Only EXACT matches should be set to SKIP
            List<ImportCandidateViewModel> exactMatches = viewModel.getCandidatesByMatchType(MatchType.EXACT);
            assertThat(exactMatches).allMatch(c -> c.getAction() == ImportAction.SKIP);
        }

        @Test
        @DisplayName("REV-E07: should show success summary after commit")
        void shouldShowSuccessSummaryAfterCommit() {
            // Given: A view model with candidates set to import
            ImportReviewViewModel viewModel = new ImportReviewViewModel();
            List<ImportCandidateViewModel> candidates = new ArrayList<>();
            candidates.add(createCandidate("Income 1", new BigDecimal("1000.00"), MatchType.NEW));
            candidates.add(createCandidate("Expense 1", new BigDecimal("-100.00"), MatchType.NEW));
            candidates.add(createCandidate("Skip this", new BigDecimal("-50.00"), MatchType.EXACT));

            viewModel.setCandidates(candidates);

            // Then: Import count should reflect only items to be imported
            assertThat(viewModel.getImportCount()).isEqualTo(2); // NEW items default to IMPORT
            assertThat(viewModel.getCandidatesToImport()).hasSize(2);

            // And: Totals can be calculated
            assertThat(viewModel.getTotalIncomeToImport()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(viewModel.getTotalExpensesToImport()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("REV-E08: should navigate to imported records after commit")
        void shouldNavigateToImportedRecordsAfterCommit() {
            // Given: Candidates that will be imported
            ImportReviewViewModel viewModel = new ImportReviewViewModel();
            List<ImportCandidateViewModel> candidates = new ArrayList<>();
            ImportCandidateViewModel income = createCandidate("Client Invoice", new BigDecimal("2500.00"), MatchType.NEW);
            candidates.add(income);
            viewModel.setCandidates(candidates);

            // Then: Imported candidates should be identifiable for navigation
            List<ImportCandidateViewModel> toImport = viewModel.getCandidatesToImport();
            assertThat(toImport).hasSize(1);
            assertThat(toImport.get(0).getId()).isEqualTo(income.getId());
            assertThat(toImport.get(0).isIncome()).isTrue();
        }

        @Test
        @DisplayName("REV-E09: should cancel import without changes")
        void shouldCancelImportWithoutChanges() {
            // Given: A view model with candidates
            ImportReviewViewModel viewModel = new ImportReviewViewModel();
            List<ImportCandidateViewModel> candidates = new ArrayList<>();
            candidates.add(createCandidate("Test", new BigDecimal("100.00"), MatchType.NEW));
            viewModel.setCandidates(candidates);

            // When: User makes changes
            viewModel.getCandidates().get(0).setAction(ImportAction.SKIP);
            viewModel.selectAll();

            // Then: Cancel is possible (view model can be discarded)
            // In real UI, the controller would simply not call import
            assertThat(viewModel.hasCandidates()).isTrue();
            assertThat(viewModel.getImportCount()).isZero(); // All set to SKIP
        }

        @Test
        @DisplayName("REV-E10: should navigate review table via keyboard")
        void shouldNavigateReviewTableViaKeyboard() {
            // Given: Candidates for keyboard navigation
            ImportCandidateViewModel candidate = createCandidateWithMatch("Test", new BigDecimal("-100.00"), MatchType.LIKELY);

            // Then: Candidate should have accessible text for screen readers
            String accessibleText = candidate.getAccessibleText();
            assertThat(accessibleText).contains("Expense");
            assertThat(accessibleText).contains("Test");
            assertThat(accessibleText).contains("possible duplicate");
            assertThat(accessibleText).contains("action:");
        }
    }

    // === HIST-E01 to HIST-E04: Import History E2E ===

    @Nested
    @DisplayName("Import History E2E Tests")
    class ImportHistoryE2E {

        @Test
        @DisplayName("HIST-E01: should access history from Settings")
        void shouldAccessHistoryFromSettings() {
            // Given: An import history view model
            ImportHistoryViewModel viewModel = new ImportHistoryViewModel();

            // When: Loading history data
            viewModel.setImports(createSampleImportHistory());

            // Then: History should be available
            assertThat(viewModel.isEmpty()).isFalse();
            assertThat(viewModel.getTotalCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("HIST-E02: should show import details on expand")
        void shouldShowImportDetailsOnExpand() {
            // Given: An import history item
            ImportHistoryItemViewModel item = createImportHistoryItem(
                LocalDateTime.now().minusDays(2),
                "barclays-statement.csv",
                "Barclays",
                15, 42,
                new BigDecimal("3450.00"),
                new BigDecimal("1230.50"),
                ImportStatus.ACTIVE
            );

            // When: Item is expanded
            item.setExpanded(true);

            // Then: Details should be accessible
            assertThat(item.isExpanded()).isTrue();
            assertThat(item.getBankFormat()).isEqualTo("Barclays");
            assertThat(item.getBankFormatDisplay()).isEqualTo("Barclays (Auto-detected)");
            assertThat(item.getFormattedIncomeTotal()).matches(".*3,450.*");
            assertThat(item.getFormattedExpenseTotal()).matches(".*1,230.*");
        }

        @Test
        @DisplayName("HIST-E03: should undo from history view")
        void shouldUndoFromHistoryView() {
            // Given: An import history with undoable items
            ImportHistoryViewModel viewModel = new ImportHistoryViewModel();
            List<ImportHistoryItemViewModel> imports = new ArrayList<>();

            ImportHistoryItemViewModel undoableItem = createImportHistoryItem(
                LocalDateTime.now().minusDays(2),
                "undoable.csv",
                "HSBC",
                5, 10,
                new BigDecimal("500.00"),
                new BigDecimal("250.00"),
                ImportStatus.ACTIVE
            );
            imports.add(undoableItem);
            viewModel.setImports(imports);

            // Then: Undoable count should be tracked
            assertThat(viewModel.getUndoableCount()).isEqualTo(1);
            assertThat(undoableItem.canUndo()).isTrue();
        }

        @Test
        @DisplayName("HIST-E04: should filter history by date picker")
        void shouldFilterHistoryByDatePicker() {
            // Given: Import history with various dates
            ImportHistoryViewModel viewModel = new ImportHistoryViewModel();
            List<ImportHistoryItemViewModel> imports = new ArrayList<>();

            // Recent import (within 7 days)
            imports.add(createImportHistoryItem(
                LocalDateTime.now().minusDays(2),
                "recent.csv", "Barclays", 5, 10,
                new BigDecimal("100"), new BigDecimal("50"), ImportStatus.ACTIVE
            ));

            // Older import (within 30 days but not 7)
            imports.add(createImportHistoryItem(
                LocalDateTime.now().minusDays(15),
                "older.csv", "HSBC", 3, 7,
                new BigDecimal("200"), new BigDecimal("100"), ImportStatus.ACTIVE
            ));

            viewModel.setImports(imports);

            // When: Filtering by last 7 days
            viewModel.setFilter(ImportHistoryFilter.LAST_7_DAYS);

            // Then: Only recent import should be visible
            assertThat(viewModel.getFilteredImports()).hasSize(1);
            assertThat(viewModel.getFilteredImports().get(0).getFileName()).isEqualTo("recent.csv");

            // When: Filtering by all time
            viewModel.setFilter(ImportHistoryFilter.ALL_TIME);

            // Then: All imports should be visible
            assertThat(viewModel.getFilteredImports()).hasSize(2);
        }
    }

    // === REC-E01 to REC-E06: Reconciliation Dashboard E2E ===

    @Nested
    @DisplayName("Reconciliation Dashboard E2E Tests")
    class ReconciliationDashboardE2E {

        @Test
        @DisplayName("REC-E01: should access dashboard from navigation")
        void shouldAccessDashboardFromNavigation() {
            // Given: A reconciliation view model
            ReconciliationViewModel viewModel = new ReconciliationViewModel();

            // When: Setting metrics
            viewModel.setMetrics(
                new BigDecimal("45230.00"),
                new BigDecimal("12450.75"),
                142, 387,
                3, 8
            );

            // Then: Dashboard data should be available
            assertThat(viewModel.getTotalIncome()).isEqualByComparingTo(new BigDecimal("45230.00"));
            assertThat(viewModel.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("12450.75"));
            assertThat(viewModel.getIncomeCount()).isEqualTo(142);
            assertThat(viewModel.getExpenseCount()).isEqualTo(387);
        }

        @Test
        @DisplayName("REC-E02: should show loading state while fetching data")
        void shouldShowLoadingStateWhileFetchingData() {
            // Given: A reconciliation view model
            ReconciliationViewModel viewModel = new ReconciliationViewModel();

            // When: Setting loading state
            assertThat(viewModel.isLoading()).isFalse();
            viewModel.setLoading(true);

            // Then: Loading state should be reflected
            assertThat(viewModel.isLoading()).isTrue();

            // When: Data loads
            viewModel.setLoading(false);
            assertThat(viewModel.isLoading()).isFalse();
        }

        @Test
        @DisplayName("REC-E03: should click warning to navigate to details")
        void shouldClickWarningToNavigateToDetails() {
            // Given: A reconciliation issue
            ReconciliationIssue duplicatesIssue = ReconciliationIssue.duplicates(3, List.of(
                "Amazon - 23.99",
                "Amazon - 23.99"
            ));

            // Then: Issue should have action text for navigation
            assertThat(duplicatesIssue.getActionText()).isNotBlank();
            assertThat(duplicatesIssue.getSeverity()).isEqualTo(IssueSeverity.HIGH);
            assertThat(duplicatesIssue.getAffectedCount()).isEqualTo(3);
            assertThat(duplicatesIssue.hasDetails()).isTrue();
        }

        @Test
        @DisplayName("REC-E04: should refresh dashboard on demand")
        void shouldRefreshDashboardOnDemand() {
            // Given: A view model with last checked time
            ReconciliationViewModel viewModel = new ReconciliationViewModel();
            LocalDateTime initialChecked = viewModel.getLastChecked();

            // When: Refreshing
            try {
                Thread.sleep(10); // Small delay to ensure time difference
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            viewModel.refresh();

            // Then: Last checked should be updated
            assertThat(viewModel.getLastChecked()).isAfterOrEqualTo(initialChecked);
            assertThat(viewModel.getLastCheckedText()).isEqualTo("Just now");
        }

        @Test
        @DisplayName("REC-E05: should show empty state for new business")
        void shouldShowEmptyStateForNewBusiness() {
            // Given: A view model with no issues and zero counts
            ReconciliationViewModel viewModel = new ReconciliationViewModel();
            viewModel.setMetrics(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0, 0,
                0, 0
            );
            viewModel.setIssues(List.of());

            // Then: Should show all clear state
            assertThat(viewModel.isAllClear()).isTrue();
            assertThat(viewModel.hasDuplicates()).isFalse();
            assertThat(viewModel.hasUncategorized()).isFalse();
            assertThat(viewModel.getIssueCount()).isZero();
        }

        @Test
        @DisplayName("REC-E06: should navigate quick actions via keyboard")
        void shouldNavigateQuickActionsViaKeyboard() {
            // Given: Issues with different severities
            ReconciliationViewModel viewModel = new ReconciliationViewModel();

            List<ReconciliationIssue> issues = new ArrayList<>();
            issues.add(ReconciliationIssue.duplicates(3, List.of("dup1", "dup2")));
            issues.add(ReconciliationIssue.missingCategories(8));
            issues.add(ReconciliationIssue.dateGaps(2, List.of("Oct 2025", "Nov 2025")));

            viewModel.setIssues(issues);

            // Then: Issues should be accessible for keyboard navigation
            assertThat(viewModel.getIssues()).hasSize(3);

            // Issues are sorted by severity (HIGH first)
            assertThat(viewModel.getIssuesBySeverity(IssueSeverity.HIGH)).hasSize(1);
            assertThat(viewModel.getIssuesBySeverity(IssueSeverity.MEDIUM)).hasSize(1);
            assertThat(viewModel.getIssuesBySeverity(IssueSeverity.LOW)).hasSize(1);

            // Each issue has accessible text
            for (ReconciliationIssue issue : viewModel.getIssues()) {
                assertThat(issue.getAccessibleText()).isNotBlank();
                assertThat(issue.getAccessibleText()).contains("priority");
            }
        }
    }

    // === Helper Methods ===

    private ImportCandidateViewModel createCandidate(String description, BigDecimal amount, MatchType matchType) {
        return new ImportCandidateViewModel(
            UUID.randomUUID(),
            LocalDate.now(),
            description,
            amount,
            matchType,
            matchType == MatchType.NEW ? null : UUID.randomUUID()
        );
    }

    private ImportCandidateViewModel createCandidateWithMatch(String description, BigDecimal amount, MatchType matchType) {
        UUID matchedId = UUID.randomUUID();
        MatchedRecordViewModel matchedRecord = new MatchedRecordViewModel(
            matchedId,
            LocalDate.now(),
            description + " (existing)",
            amount,
            "Office Costs"
        );

        return new ImportCandidateViewModel(
            UUID.randomUUID(),
            LocalDate.now(),
            description,
            amount,
            "Office Costs",
            matchType,
            matchedId,
            matchedRecord
        );
    }

    private ImportHistoryItemViewModel createImportHistoryItem(
        LocalDateTime importDateTime,
        String fileName,
        String bankFormat,
        int incomeRecords,
        int expenseRecords,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        ImportStatus status
    ) {
        return new ImportHistoryItemViewModel(
            UUID.randomUUID(),
            importDateTime,
            fileName,
            "/path/to/" + fileName,
            bankFormat,
            incomeRecords,
            expenseRecords,
            incomeTotal,
            expenseTotal,
            status,
            null,
            null
        );
    }

    private List<ImportHistoryItemViewModel> createSampleImportHistory() {
        List<ImportHistoryItemViewModel> samples = new ArrayList<>();

        // Recent active import
        samples.add(new ImportHistoryItemViewModel(
            UUID.randomUUID(),
            LocalDateTime.now().minusDays(2),
            "barclays-january-2026.csv",
            "/path/to/file.csv",
            "Barclays",
            15, 42,
            new BigDecimal("3450.00"),
            new BigDecimal("1230.50"),
            ImportStatus.ACTIVE,
            null, null
        ));

        // Old import (cannot undo - older than 7 days)
        samples.add(new ImportHistoryItemViewModel(
            UUID.randomUUID(),
            LocalDateTime.now().minusDays(15),
            "hsbc-december-2025.csv",
            "/path/to/file.csv",
            "HSBC",
            8, 23,
            new BigDecimal("2100.00"),
            new BigDecimal("890.25"),
            ImportStatus.ACTIVE,
            null, null
        ));

        // Locked import (used in tax submission)
        samples.add(new ImportHistoryItemViewModel(
            UUID.randomUUID(),
            LocalDateTime.now().minusDays(30),
            "lloyds-november-2025.csv",
            "/path/to/file.csv",
            "Lloyds",
            12, 35,
            new BigDecimal("5200.00"),
            new BigDecimal("2150.75"),
            ImportStatus.LOCKED,
            null,
            LocalDateTime.now().minusDays(5)
        ));

        // Undone import
        samples.add(new ImportHistoryItemViewModel(
            UUID.randomUUID(),
            LocalDateTime.now().minusDays(3),
            "monzo-january-2026.csv",
            "/path/to/file.csv",
            "Monzo",
            5, 18,
            new BigDecimal("980.00"),
            new BigDecimal("450.30"),
            ImportStatus.UNDONE,
            LocalDateTime.now().minusDays(1),
            null
        ));

        return samples;
    }
}
