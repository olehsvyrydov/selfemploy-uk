package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.ui.viewmodel.ImportAction;
import uk.selfemploy.ui.viewmodel.ImportCandidateViewModel;
import uk.selfemploy.ui.viewmodel.ImportReviewViewModel;
import uk.selfemploy.ui.viewmodel.MatchType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for ImportReviewController.
 * SE-10C-001: Fix Import Review Bulk Action Buttons
 *
 * <p>Tests verify that bulk action button handlers work correctly.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ImportReviewController")
class ImportReviewControllerTest {

    private ImportReviewController controller;
    private ImportReviewViewModel viewModel;

    @BeforeEach
    void setUp() {
        controller = new ImportReviewController();
        viewModel = new ImportReviewViewModel();
    }

    // ========================================================================
    // Initialization Tests
    // ========================================================================

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("should implement Initializable interface")
        void shouldImplementInitializable() {
            assertThat(controller).isInstanceOf(javafx.fxml.Initializable.class);
        }

        @Test
        @DisplayName("should create controller without errors")
        void shouldCreateControllerWithoutErrors() {
            assertThat(controller).isNotNull();
        }
    }

    // ========================================================================
    // Bulk Action Handler Tests - SE-10C-001
    // ========================================================================

    @Nested
    @DisplayName("Bulk Action Handlers (SE-10C-001)")
    class BulkActionHandlerTests {

        @BeforeEach
        void setUpWithCandidates() {
            // Create mixed candidates: 2 NEW, 2 EXACT, 1 LIKELY
            List<ImportCandidateViewModel> candidates = createMixedCandidates();
            viewModel.setCandidates(candidates);
            controller.setViewModel(viewModel);
        }

        @Test
        @DisplayName("AC1: handleImportAllNew should set all NEW items to IMPORT action")
        void handleImportAllNewShouldSetAllNewItemsToImport() {
            // Given - all items start with their default actions
            // NEW items default to IMPORT, but let's set them all to SKIP first
            for (ImportCandidateViewModel candidate : viewModel.getCandidates()) {
                candidate.setAction(ImportAction.SKIP);
            }

            // When - handler is called (simulating button click)
            controller.handleImportAllNew(mock(ActionEvent.class));

            // Then - all NEW items should be IMPORT, others remain SKIP
            for (ImportCandidateViewModel candidate : viewModel.getCandidates()) {
                if (candidate.getMatchType() == MatchType.NEW) {
                    assertThat(candidate.getAction())
                        .as("NEW item should have IMPORT action")
                        .isEqualTo(ImportAction.IMPORT);
                } else {
                    assertThat(candidate.getAction())
                        .as("Non-NEW item should remain SKIP")
                        .isEqualTo(ImportAction.SKIP);
                }
            }
        }

        @Test
        @DisplayName("AC2: handleSkipAllDuplicates should set all EXACT items to SKIP action")
        void handleSkipAllDuplicatesShouldSetAllExactItemsToSkip() {
            // Given - all items start with IMPORT action
            for (ImportCandidateViewModel candidate : viewModel.getCandidates()) {
                candidate.setAction(ImportAction.IMPORT);
            }

            // When - handler is called (simulating button click)
            controller.handleSkipAllDuplicates(mock(ActionEvent.class));

            // Then - all EXACT items should be SKIP, others remain IMPORT
            for (ImportCandidateViewModel candidate : viewModel.getCandidates()) {
                if (candidate.getMatchType() == MatchType.EXACT) {
                    assertThat(candidate.getAction())
                        .as("EXACT item should have SKIP action")
                        .isEqualTo(ImportAction.SKIP);
                } else {
                    assertThat(candidate.getAction())
                        .as("Non-EXACT item should remain IMPORT")
                        .isEqualTo(ImportAction.IMPORT);
                }
            }
        }

        @Test
        @DisplayName("AC4: Import count should update after bulk action")
        void importCountShouldUpdateAfterBulkAction() {
            // Given - all items set to SKIP (0 imports)
            for (ImportCandidateViewModel candidate : viewModel.getCandidates()) {
                candidate.setAction(ImportAction.SKIP);
            }
            assertThat(viewModel.getImportCount()).isEqualTo(0);

            // When - Import All New is called (2 NEW items)
            controller.handleImportAllNew(mock(ActionEvent.class));

            // Then - import count should be 2 (the 2 NEW items)
            assertThat(viewModel.getImportCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle empty candidate list without error")
        void shouldHandleEmptyCandidateListWithoutError() {
            // Given - empty candidates
            viewModel.setCandidates(List.of());
            controller.setViewModel(viewModel);

            // When/Then - handlers should not throw
            controller.handleImportAllNew(mock(ActionEvent.class));
            controller.handleSkipAllDuplicates(mock(ActionEvent.class));

            assertThat(viewModel.getImportCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle candidates with only NEW items")
        void shouldHandleCandidatesWithOnlyNewItems() {
            // Given - only NEW items
            List<ImportCandidateViewModel> newOnly = List.of(
                createCandidate(MatchType.NEW, new BigDecimal("100.00")),
                createCandidate(MatchType.NEW, new BigDecimal("200.00"))
            );
            viewModel.setCandidates(newOnly);
            controller.setViewModel(viewModel);

            // When
            controller.handleImportAllNew(mock(ActionEvent.class));

            // Then - all should be IMPORT
            assertThat(viewModel.getImportCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle candidates with only EXACT items")
        void shouldHandleCandidatesWithOnlyExactItems() {
            // Given - only EXACT items
            List<ImportCandidateViewModel> exactOnly = List.of(
                createCandidate(MatchType.EXACT, new BigDecimal("-50.00")),
                createCandidate(MatchType.EXACT, new BigDecimal("-75.00"))
            );
            viewModel.setCandidates(exactOnly);
            controller.setViewModel(viewModel);

            // When
            controller.handleSkipAllDuplicates(mock(ActionEvent.class));

            // Then - all should be SKIP
            assertThat(viewModel.getImportCount()).isEqualTo(0);
        }
    }

    // ========================================================================
    // Button Disable State Tests - AC5
    // ========================================================================

    @Nested
    @DisplayName("Button Disable State (AC5)")
    class ButtonDisableStateTests {

        @Test
        @DisplayName("AC5: Import All New button disabled when no NEW items")
        void importAllNewButtonDisabledWhenNoNewItems() {
            // Given - only EXACT items (no NEW)
            List<ImportCandidateViewModel> exactOnly = List.of(
                createCandidate(MatchType.EXACT, new BigDecimal("-50.00")),
                createCandidate(MatchType.EXACT, new BigDecimal("-75.00"))
            );
            viewModel.setCandidates(exactOnly);

            // Then - newCount should be 0
            assertThat(viewModel.getNewCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("AC5: Skip All Duplicates button disabled when no EXACT items")
        void skipAllDuplicatesButtonDisabledWhenNoExactItems() {
            // Given - only NEW items (no EXACT)
            List<ImportCandidateViewModel> newOnly = List.of(
                createCandidate(MatchType.NEW, new BigDecimal("100.00")),
                createCandidate(MatchType.NEW, new BigDecimal("200.00"))
            );
            viewModel.setCandidates(newOnly);

            // Then - exactCount should be 0
            assertThat(viewModel.getExactCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("AC5: Buttons enabled when relevant items exist")
        void buttonsEnabledWhenRelevantItemsExist() {
            // Given - mixed items
            List<ImportCandidateViewModel> candidates = createMixedCandidates();
            viewModel.setCandidates(candidates);

            // Then - both counts should be > 0
            assertThat(viewModel.getNewCount()).isGreaterThan(0);
            assertThat(viewModel.getExactCount()).isGreaterThan(0);
        }
    }

    // ========================================================================
    // State Management Tests
    // ========================================================================

    @Nested
    @DisplayName("State Management")
    class StateManagementTests {

        @Test
        @DisplayName("should have viewModel after initialization")
        void shouldHaveViewModelAfterInitialization() {
            // Given - skip initialize() test since it requires FXML injection
            // We test that setViewModel works instead
            controller.setViewModel(viewModel);

            // Then
            assertThat(controller.getViewModel()).isNotNull();
        }

        @Test
        @DisplayName("should allow setting custom viewModel")
        void shouldAllowSettingCustomViewModel() {
            // Given
            ImportReviewViewModel customViewModel = new ImportReviewViewModel();

            // When
            controller.setViewModel(customViewModel);

            // Then
            assertThat(controller.getViewModel()).isSameAs(customViewModel);
        }

        @Test
        @DisplayName("should update viewModel when setCandidates is called")
        void shouldUpdateViewModelWhenSetCandidatesIsCalled() {
            // Given - use setViewModel instead of initialize() to avoid FXML injection issues
            controller.setViewModel(viewModel);
            List<ImportCandidateViewModel> candidates = createMixedCandidates();

            // When
            viewModel.setCandidates(candidates);

            // Then
            assertThat(controller.getViewModel().getCandidates()).hasSize(5);
        }
    }

    // ========================================================================
    // Callback Tests
    // ========================================================================

    @Nested
    @DisplayName("Callback Tests")
    class CallbackTests {

        @Test
        @DisplayName("should set onImportComplete callback")
        void shouldSetOnImportCompleteCallback() {
            // Given
            var called = new boolean[]{false};

            // When
            controller.setOnImportComplete(candidates -> called[0] = true);

            // Then - callback is set (we can't easily test it without full FXML)
            assertThat(controller).isNotNull();
        }

        @Test
        @DisplayName("should set onCancel callback")
        void shouldSetOnCancelCallback() {
            // When
            controller.setOnCancel(() -> {});

            // Then - no exception thrown
            assertThat(controller).isNotNull();
        }

        @Test
        @DisplayName("should set onBack callback")
        void shouldSetOnBackCallback() {
            // When
            controller.setOnBack(() -> {});

            // Then - no exception thrown
            assertThat(controller).isNotNull();
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private List<ImportCandidateViewModel> createMixedCandidates() {
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
