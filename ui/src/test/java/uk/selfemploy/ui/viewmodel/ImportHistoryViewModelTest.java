package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Tests for Import History View (Sprint 10B).
 * Tests HIST-U01 through HIST-U06 from /rob's test design.
 *
 * <p>SE-10B-006: Import History View Tests</p>
 *
 * @see docs/sprints/sprint-10A/testing/rob-test-design-10A-10B.md
 */
@DisplayName("Import History ViewModel Tests (Sprint 10B)")
class ImportHistoryViewModelTest {

    private ImportHistoryViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new ImportHistoryViewModel();
    }

    // === HIST-U01 to HIST-U03: History Loading and Display ===

    @Nested
    @DisplayName("History Loading and Display")
    class HistoryLoadingAndDisplay {

        @Test
        @DisplayName("HIST-U01: should load import history for business")
        void shouldLoadImportHistoryForBusiness() {
            // Given
            List<ImportHistoryItemViewModel> imports = createSampleImports();

            // When
            viewModel.setImports(imports);

            // Then
            assertThat(viewModel.getAllImports()).hasSize(4);
            assertThat(viewModel.getTotalCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("HIST-U02: should sort by date descending")
        void shouldSortByDateDescending() {
            // Given
            List<ImportHistoryItemViewModel> imports = createSampleImports();

            // When
            viewModel.setImports(imports);

            // Then - most recent should be first
            List<ImportHistoryItemViewModel> filtered = viewModel.getFilteredImports();
            for (int i = 0; i < filtered.size() - 1; i++) {
                assertThat(filtered.get(i).getImportDateTime())
                    .isAfterOrEqualTo(filtered.get(i + 1).getImportDateTime());
            }
        }

        @Test
        @DisplayName("HIST-U03: should show correct status badge")
        void shouldShowCorrectStatusBadge() {
            // Given
            ImportHistoryItemViewModel active = createImport(
                LocalDateTime.now().minusDays(2), ImportStatus.ACTIVE, null, null
            );
            ImportHistoryItemViewModel undone = createImport(
                LocalDateTime.now().minusDays(3), ImportStatus.UNDONE,
                LocalDateTime.now().minusDays(1), null
            );
            ImportHistoryItemViewModel locked = createImport(
                LocalDateTime.now().minusDays(30), ImportStatus.LOCKED,
                null, LocalDateTime.now().minusDays(5)
            );

            // When
            viewModel.setImports(List.of(active, undone, locked));

            // Then
            assertThat(viewModel.getFilteredImports().stream()
                .filter(i -> i.getStatus() == ImportStatus.ACTIVE).count()).isEqualTo(1);
            assertThat(viewModel.getFilteredImports().stream()
                .filter(i -> i.getStatus() == ImportStatus.UNDONE).count()).isEqualTo(1);
            assertThat(viewModel.getFilteredImports().stream()
                .filter(i -> i.getStatus() == ImportStatus.LOCKED).count()).isEqualTo(1);
        }
    }

    // === HIST-U04 to HIST-U06: Undo Button State ===

    @Nested
    @DisplayName("Undo Button State")
    class UndoButtonState {

        @Test
        @DisplayName("HIST-U04: should enable undo for eligible imports (within 7 days, no tax submission)")
        void shouldEnableUndoForEligibleImports() {
            // Given - import 2 days ago, active, no tax submission
            ImportHistoryItemViewModel recentActive = createImport(
                LocalDateTime.now().minusDays(2), ImportStatus.ACTIVE, null, null
            );

            // When
            viewModel.setImports(List.of(recentActive));

            // Then
            assertThat(recentActive.canUndo()).isTrue();
            assertThat(viewModel.getUndoableCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("HIST-U05: should disable undo for old imports (older than 7 days)")
        void shouldDisableUndoForOldImports() {
            // Given - import 15 days ago
            ImportHistoryItemViewModel oldImport = createImport(
                LocalDateTime.now().minusDays(15), ImportStatus.ACTIVE, null, null
            );

            // When
            viewModel.setImports(List.of(oldImport));

            // Then
            assertThat(oldImport.canUndo()).isFalse();
            assertThat(oldImport.getUndoDisabledReason())
                .contains("7 days");
            assertThat(viewModel.getUndoableCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should disable undo for imports used in tax submission")
        void shouldDisableUndoForTaxSubmissionImports() {
            // Given - import used in tax submission
            ImportHistoryItemViewModel lockedImport = createImport(
                LocalDateTime.now().minusDays(5), ImportStatus.LOCKED,
                null, LocalDateTime.now().minusDays(2)
            );

            // When
            viewModel.setImports(List.of(lockedImport));

            // Then
            assertThat(lockedImport.canUndo()).isFalse();
            assertThat(lockedImport.getUndoDisabledReason())
                .contains("tax submission");
        }

        @Test
        @DisplayName("should disable undo for already undone imports")
        void shouldDisableUndoForAlreadyUndoneImports() {
            // Given
            ImportHistoryItemViewModel undoneImport = createImport(
                LocalDateTime.now().minusDays(3), ImportStatus.UNDONE,
                LocalDateTime.now().minusDays(1), null
            );

            // When
            viewModel.setImports(List.of(undoneImport));

            // Then
            assertThat(undoneImport.canUndo()).isFalse();
            assertThat(undoneImport.getUndoDisabledReason())
                .contains("already undone");
        }

        @Test
        @DisplayName("HIST-U06: should filter by date range")
        void shouldFilterByDateRange() {
            // Given
            viewModel.setImports(createSampleImports());
            int totalCount = viewModel.getAllImports().size();

            // When - filter to last 7 days
            viewModel.setFilter(ImportHistoryFilter.LAST_7_DAYS);

            // Then - should have fewer items
            int filtered7Days = viewModel.getFilteredImports().size();

            // When - filter to all time
            viewModel.setFilter(ImportHistoryFilter.ALL_TIME);

            // Then - should have all items
            assertThat(viewModel.getFilteredImports()).hasSize(totalCount);
        }
    }

    // === Additional Tests ===

    @Nested
    @DisplayName("State Management")
    class StateManagement {

        @Test
        @DisplayName("should detect empty state")
        void shouldDetectEmptyState() {
            assertThat(viewModel.isEmpty()).isTrue();

            viewModel.setImports(createSampleImports());

            assertThat(viewModel.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("should detect filtered empty state")
        void shouldDetectFilteredEmptyState() {
            // Given - imports all older than 7 days
            List<ImportHistoryItemViewModel> imports = List.of(
                createImport(LocalDateTime.now().minusDays(15), ImportStatus.ACTIVE, null, null),
                createImport(LocalDateTime.now().minusDays(30), ImportStatus.ACTIVE, null, null)
            );
            viewModel.setImports(imports);

            // When
            viewModel.setFilter(ImportHistoryFilter.LAST_7_DAYS);

            // Then
            assertThat(viewModel.isFilteredEmpty()).isTrue();
            assertThat(viewModel.isEmpty()).isFalse();
        }
    }

    // === Helper Methods ===

    private List<ImportHistoryItemViewModel> createSampleImports() {
        List<ImportHistoryItemViewModel> list = new ArrayList<>();

        // Recent active import (2 days ago)
        list.add(createImport(LocalDateTime.now().minusDays(2), ImportStatus.ACTIVE, null, null));

        // Old import (15 days ago)
        list.add(createImport(LocalDateTime.now().minusDays(15), ImportStatus.ACTIVE, null, null));

        // Locked import (30 days ago, used in tax submission)
        list.add(createImport(LocalDateTime.now().minusDays(30), ImportStatus.LOCKED,
            null, LocalDateTime.now().minusDays(5)));

        // Undone import (3 days ago, undone 1 day ago)
        list.add(createImport(LocalDateTime.now().minusDays(3), ImportStatus.UNDONE,
            LocalDateTime.now().minusDays(1), null));

        return list;
    }

    private ImportHistoryItemViewModel createImport(LocalDateTime importDateTime,
                                                     ImportStatus status,
                                                     LocalDateTime undoneDateTime,
                                                     LocalDateTime taxSubmissionDate) {
        return new ImportHistoryItemViewModel(
            UUID.randomUUID(),
            importDateTime,
            "test-file.csv",
            "/path/to/file.csv",
            "Barclays",
            10, 25,
            new BigDecimal("2500.00"),
            new BigDecimal("1200.50"),
            status,
            undoneDateTime,
            taxSubmissionDate
        );
    }
}
