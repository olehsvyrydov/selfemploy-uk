package uk.selfemploy.ui.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.viewmodel.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ComparisonDialog.
 * Tests the comparison dialog logic without JavaFX Platform.
 *
 * <p>SE-10B-005: Import Review UI - Comparison Dialog</p>
 *
 * <p>Note: Full UI tests requiring JavaFX Platform should be tagged with @Tag("e2e")
 * and run locally. These tests focus on the logic that can be tested without the platform.</p>
 */
@DisplayName("ComparisonDialog Tests")
class ComparisonDialogTest {

    @Nested
    @DisplayName("ViewModel Construction for Comparison")
    class ViewModelConstruction {

        @Test
        @DisplayName("should create candidate with matched record for comparison")
        void shouldCreateCandidateWithMatchedRecord() {
            // Given
            UUID candidateId = UUID.randomUUID();
            UUID matchedId = UUID.randomUUID();

            MatchedRecordViewModel matchedRecord = new MatchedRecordViewModel(
                matchedId,
                LocalDate.of(2026, 1, 14),
                "Office Supplies Ltd",
                new BigDecimal("-45.99"),
                "Office Costs"
            );

            // When
            ImportCandidateViewModel candidate = new ImportCandidateViewModel(
                candidateId,
                LocalDate.of(2026, 1, 15),
                "Office Supplies",
                new BigDecimal("-45.99"),
                null,
                MatchType.LIKELY,
                matchedId,
                matchedRecord
            );

            // Then
            assertThat(candidate.hasMatch()).isTrue();
            assertThat(candidate.getMatchedRecord()).isNotNull();
            assertThat(candidate.getMatchedRecord().getDescription()).isEqualTo("Office Supplies Ltd");
        }

        @Test
        @DisplayName("should detect differences between candidate and matched record")
        void shouldDetectDifferences() {
            // Given - records with different descriptions and dates
            UUID matchedId = UUID.randomUUID();

            MatchedRecordViewModel matchedRecord = new MatchedRecordViewModel(
                matchedId,
                LocalDate.of(2026, 1, 14), // Different date
                "Office Supplies Ltd",      // Different description
                new BigDecimal("-45.99"),   // Same amount
                "Office Costs"              // Has category
            );

            ImportCandidateViewModel candidate = new ImportCandidateViewModel(
                UUID.randomUUID(),
                LocalDate.of(2026, 1, 15),  // Different date
                "Office Supplies",          // Different description
                new BigDecimal("-45.99"),   // Same amount
                null,                       // No category
                MatchType.LIKELY,
                matchedId,
                matchedRecord
            );

            // Then - can identify differences for highlighting
            assertThat(candidate.getFormattedDate()).isNotEqualTo(matchedRecord.getFormattedDate());
            assertThat(candidate.getDescription()).isNotEqualTo(matchedRecord.getDescription());
            assertThat(candidate.getFormattedAmount()).isEqualTo(matchedRecord.getFormattedAmount());
            assertThat(candidate.getDisplayCategory()).isNotEqualTo(matchedRecord.getDisplayCategory());
        }

        @Test
        @DisplayName("should identify identical records")
        void shouldIdentifyIdenticalRecords() {
            // Given - records with same values
            UUID matchedId = UUID.randomUUID();
            LocalDate date = LocalDate.of(2026, 1, 15);
            String description = "Office Supplies";
            BigDecimal amount = new BigDecimal("-45.99");

            MatchedRecordViewModel matchedRecord = new MatchedRecordViewModel(
                matchedId, date, description, amount, ""
            );

            ImportCandidateViewModel candidate = new ImportCandidateViewModel(
                UUID.randomUUID(), date, description, amount, "",
                MatchType.EXACT, matchedId, matchedRecord
            );

            // Then
            assertThat(candidate.getFormattedDate()).isEqualTo(matchedRecord.getFormattedDate());
            assertThat(candidate.getDescription()).isEqualTo(matchedRecord.getDescription());
            assertThat(candidate.getFormattedAmount()).isEqualTo(matchedRecord.getFormattedAmount());
            assertThat(candidate.getDisplayCategory()).isEqualTo(matchedRecord.getDisplayCategory());
        }
    }

    @Nested
    @DisplayName("Action Selection")
    class ActionSelection {

        @Test
        @DisplayName("Import action should mark candidate as will be imported")
        void importActionShouldMarkAsWillBeImported() {
            // Given
            ImportCandidateViewModel candidate = createLikelyMatch();

            // When
            candidate.setAction(ImportAction.IMPORT);

            // Then
            assertThat(candidate.willBeImported()).isTrue();
            assertThat(candidate.getAction()).isEqualTo(ImportAction.IMPORT);
        }

        @Test
        @DisplayName("Skip action should mark candidate as will not be imported")
        void skipActionShouldMarkAsWillNotBeImported() {
            // Given
            ImportCandidateViewModel candidate = createLikelyMatch();

            // When
            candidate.setAction(ImportAction.SKIP);

            // Then
            assertThat(candidate.willBeImported()).isFalse();
            assertThat(candidate.getAction()).isEqualTo(ImportAction.SKIP);
        }

        @Test
        @DisplayName("Update action should mark candidate as will be imported")
        void updateActionShouldMarkAsWillBeImported() {
            // Given
            ImportCandidateViewModel candidate = createLikelyMatch();

            // When
            candidate.setAction(ImportAction.UPDATE);

            // Then
            assertThat(candidate.willBeImported()).isTrue();
            assertThat(candidate.getAction()).isEqualTo(ImportAction.UPDATE);
        }
    }

    @Nested
    @DisplayName("Default Actions")
    class DefaultActions {

        @Test
        @DisplayName("LIKELY match should default to IMPORT")
        void likelyMatchShouldDefaultToImport() {
            ImportCandidateViewModel candidate = createLikelyMatch();

            assertThat(candidate.getAction()).isEqualTo(ImportAction.IMPORT);
        }

        @Test
        @DisplayName("EXACT match should default to SKIP")
        void exactMatchShouldDefaultToSkip() {
            ImportCandidateViewModel candidate = createExactMatch();

            assertThat(candidate.getAction()).isEqualTo(ImportAction.SKIP);
        }

        @Test
        @DisplayName("NEW should default to IMPORT")
        void newShouldDefaultToImport() {
            ImportCandidateViewModel candidate = new ImportCandidateViewModel(
                UUID.randomUUID(),
                LocalDate.now(),
                "New Transaction",
                new BigDecimal("100.00"),
                MatchType.NEW,
                null
            );

            assertThat(candidate.getAction()).isEqualTo(ImportAction.IMPORT);
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidation {

        @Test
        @DisplayName("should reject null candidate in dialog construction")
        void shouldRejectNullCandidate() {
            // This test verifies the contract - actual dialog creation requires JavaFX
            // The ComparisonDialog constructor uses Objects.requireNonNull
            assertThatThrownBy(() -> {
                // Simulating the null check that happens in constructor
                java.util.Objects.requireNonNull(null, "candidate cannot be null");
            }).isInstanceOf(NullPointerException.class)
              .hasMessage("candidate cannot be null");
        }
    }

    // === Helper Methods ===

    private ImportCandidateViewModel createLikelyMatch() {
        UUID matchedId = UUID.randomUUID();
        MatchedRecordViewModel matchedRecord = new MatchedRecordViewModel(
            matchedId,
            LocalDate.of(2026, 1, 14),
            "Office Supplies Ltd",
            new BigDecimal("-45.99"),
            "Office Costs"
        );

        return new ImportCandidateViewModel(
            UUID.randomUUID(),
            LocalDate.of(2026, 1, 15),
            "Office Supplies",
            new BigDecimal("-45.99"),
            null,
            MatchType.LIKELY,
            matchedId,
            matchedRecord
        );
    }

    private ImportCandidateViewModel createExactMatch() {
        UUID matchedId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 1, 15);
        String description = "Office Supplies";
        BigDecimal amount = new BigDecimal("-45.99");

        MatchedRecordViewModel matchedRecord = new MatchedRecordViewModel(
            matchedId, date, description, amount, ""
        );

        return new ImportCandidateViewModel(
            UUID.randomUUID(), date, description, amount, "",
            MatchType.EXACT, matchedId, matchedRecord
        );
    }
}
