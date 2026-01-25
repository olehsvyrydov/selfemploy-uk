package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ImportCandidateViewModel.
 * SE-10B-005: Import Review UI
 */
@DisplayName("ImportCandidateViewModel")
class ImportCandidateViewModelTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            UUID id = UUID.randomUUID();
            LocalDate date = LocalDate.of(2026, 1, 15);
            UUID matchedId = UUID.randomUUID();

            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                id, date, "Amazon Purchase", new BigDecimal("-25.99"),
                MatchType.EXACT, matchedId
            );

            assertThat(vm.getId()).isEqualTo(id);
            assertThat(vm.getDate()).isEqualTo(date);
            assertThat(vm.getDescription()).isEqualTo("Amazon Purchase");
            assertThat(vm.getAmount()).isEqualByComparingTo(new BigDecimal("-25.99"));
            assertThat(vm.getMatchType()).isEqualTo(MatchType.EXACT);
            assertThat(vm.getMatchedRecordId()).isEqualTo(matchedId);
        }

        @Test
        @DisplayName("should set default action based on NEW match type")
        void shouldSetDefaultActionForNew() {
            ImportCandidateViewModel vm = createCandidate(MatchType.NEW);

            assertThat(vm.getAction()).isEqualTo(ImportAction.IMPORT);
        }

        @Test
        @DisplayName("should set default action based on EXACT match type")
        void shouldSetDefaultActionForExact() {
            ImportCandidateViewModel vm = createCandidate(MatchType.EXACT);

            assertThat(vm.getAction()).isEqualTo(ImportAction.SKIP);
        }

        @Test
        @DisplayName("should set default action based on LIKELY match type")
        void shouldSetDefaultActionForLikely() {
            ImportCandidateViewModel vm = createCandidate(MatchType.LIKELY);

            assertThat(vm.getAction()).isEqualTo(ImportAction.IMPORT);
        }
    }

    @Nested
    @DisplayName("Formatting")
    class Formatting {

        @Test
        @DisplayName("should format date correctly")
        void shouldFormatDate() {
            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                UUID.randomUUID(), LocalDate.of(2026, 1, 15),
                "Test", BigDecimal.TEN, MatchType.NEW, null
            );

            assertThat(vm.getFormattedDate()).isEqualTo("15 Jan 2026");
        }

        @Test
        @DisplayName("should format positive amount with plus sign")
        void shouldFormatPositiveAmount() {
            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                UUID.randomUUID(), LocalDate.now(),
                "Income", new BigDecimal("1250.50"), MatchType.NEW, null
            );

            assertThat(vm.getFormattedAmount()).startsWith("+");
            assertThat(vm.getFormattedAmount()).contains("1,250.50");
        }

        @Test
        @DisplayName("should format negative amount with minus sign")
        void shouldFormatNegativeAmount() {
            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                UUID.randomUUID(), LocalDate.now(),
                "Expense", new BigDecimal("-99.99"), MatchType.NEW, null
            );

            assertThat(vm.getFormattedAmount()).startsWith("-");
            assertThat(vm.getFormattedAmount()).contains("99.99");
        }
    }

    @Nested
    @DisplayName("Type Detection")
    class TypeDetection {

        @Test
        @DisplayName("should detect income (positive amount)")
        void shouldDetectIncome() {
            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                UUID.randomUUID(), LocalDate.now(),
                "Payment", new BigDecimal("500.00"), MatchType.NEW, null
            );

            assertThat(vm.isIncome()).isTrue();
            assertThat(vm.isExpense()).isFalse();
        }

        @Test
        @DisplayName("should detect expense (negative amount)")
        void shouldDetectExpense() {
            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                UUID.randomUUID(), LocalDate.now(),
                "Purchase", new BigDecimal("-50.00"), MatchType.NEW, null
            );

            assertThat(vm.isIncome()).isFalse();
            assertThat(vm.isExpense()).isTrue();
        }

        @Test
        @DisplayName("should treat zero as income")
        void shouldTreatZeroAsIncome() {
            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                UUID.randomUUID(), LocalDate.now(),
                "Zero", BigDecimal.ZERO, MatchType.NEW, null
            );

            assertThat(vm.isIncome()).isFalse(); // Zero is not positive
            assertThat(vm.isExpense()).isFalse(); // Zero is not negative
        }
    }

    @Nested
    @DisplayName("Import Status")
    class ImportStatus {

        @Test
        @DisplayName("should report will be imported for IMPORT action")
        void shouldReportWillBeImportedForImport() {
            ImportCandidateViewModel vm = createCandidate(MatchType.NEW);
            vm.setAction(ImportAction.IMPORT);

            assertThat(vm.willBeImported()).isTrue();
        }

        @Test
        @DisplayName("should report will be imported for UPDATE action")
        void shouldReportWillBeImportedForUpdate() {
            ImportCandidateViewModel vm = createCandidate(MatchType.EXACT);
            vm.setAction(ImportAction.UPDATE);

            assertThat(vm.willBeImported()).isTrue();
        }

        @Test
        @DisplayName("should report will not be imported for SKIP action")
        void shouldReportWillNotBeImportedForSkip() {
            ImportCandidateViewModel vm = createCandidate(MatchType.EXACT);
            vm.setAction(ImportAction.SKIP);

            assertThat(vm.willBeImported()).isFalse();
        }
    }

    @Nested
    @DisplayName("Match Detection")
    class MatchDetection {

        @Test
        @DisplayName("should detect has match when matched ID present")
        void shouldDetectHasMatch() {
            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                UUID.randomUUID(), LocalDate.now(),
                "Test", BigDecimal.TEN, MatchType.EXACT, UUID.randomUUID()
            );

            assertThat(vm.hasMatch()).isTrue();
        }

        @Test
        @DisplayName("should detect no match when matched ID is null")
        void shouldDetectNoMatch() {
            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                UUID.randomUUID(), LocalDate.now(),
                "Test", BigDecimal.TEN, MatchType.NEW, null
            );

            assertThat(vm.hasMatch()).isFalse();
        }
    }

    @Nested
    @DisplayName("Search Matching")
    class SearchMatching {

        @Test
        @DisplayName("should match search by description")
        void shouldMatchByDescription() {
            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                UUID.randomUUID(), LocalDate.now(),
                "Amazon Prime Subscription", BigDecimal.TEN, MatchType.NEW, null
            );

            assertThat(vm.matchesSearch("amazon")).isTrue();
            assertThat(vm.matchesSearch("PRIME")).isTrue();
            assertThat(vm.matchesSearch("netflix")).isFalse();
        }

        @Test
        @DisplayName("should match search by amount")
        void shouldMatchByAmount() {
            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                UUID.randomUUID(), LocalDate.now(),
                "Purchase", new BigDecimal("125.50"), MatchType.NEW, null
            );

            assertThat(vm.matchesSearch("125")).isTrue();
        }

        @Test
        @DisplayName("should match empty search")
        void shouldMatchEmptySearch() {
            ImportCandidateViewModel vm = createCandidate(MatchType.NEW);

            assertThat(vm.matchesSearch(null)).isTrue();
            assertThat(vm.matchesSearch("")).isTrue();
            assertThat(vm.matchesSearch("   ")).isTrue();
        }
    }

    @Nested
    @DisplayName("Selection")
    class Selection {

        @Test
        @DisplayName("should be unselected by default")
        void shouldBeUnselectedByDefault() {
            ImportCandidateViewModel vm = createCandidate(MatchType.NEW);

            assertThat(vm.isSelected()).isFalse();
        }

        @Test
        @DisplayName("should update selection state")
        void shouldUpdateSelectionState() {
            ImportCandidateViewModel vm = createCandidate(MatchType.NEW);

            vm.setSelected(true);
            assertThat(vm.isSelected()).isTrue();

            vm.setSelected(false);
            assertThat(vm.isSelected()).isFalse();
        }
    }

    @Nested
    @DisplayName("Category and Matched Record")
    class CategoryAndMatchedRecord {

        @Test
        @DisplayName("should create with category and matched record")
        void shouldCreateWithCategoryAndMatchedRecord() {
            // Given
            UUID id = UUID.randomUUID();
            UUID matchedId = UUID.randomUUID();
            MatchedRecordViewModel matchedRecord = new MatchedRecordViewModel(
                matchedId,
                LocalDate.of(2026, 1, 14),
                "Existing Record",
                new BigDecimal("-45.00"),
                "Office Costs"
            );

            // When
            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                id,
                LocalDate.of(2026, 1, 15),
                "Import Candidate",
                new BigDecimal("-45.00"),
                "Travel",
                MatchType.LIKELY,
                matchedId,
                matchedRecord
            );

            // Then
            assertThat(vm.getCategory()).isEqualTo("Travel");
            assertThat(vm.getDisplayCategory()).isEqualTo("Travel");
            assertThat(vm.getMatchedRecord()).isNotNull();
            assertThat(vm.getMatchedRecord().getId()).isEqualTo(matchedId);
            assertThat(vm.getMatchedRecord().getDescription()).isEqualTo("Existing Record");
        }

        @Test
        @DisplayName("should handle null category")
        void shouldHandleNullCategory() {
            // When
            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                UUID.randomUUID(),
                LocalDate.now(),
                "Test",
                BigDecimal.TEN,
                null,
                MatchType.NEW,
                null,
                null
            );

            // Then
            assertThat(vm.getCategory()).isEmpty();
            assertThat(vm.getDisplayCategory()).isEqualTo("-");
        }

        @Test
        @DisplayName("should handle null matched record")
        void shouldHandleNullMatchedRecord() {
            // When
            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                UUID.randomUUID(),
                LocalDate.now(),
                "Test",
                BigDecimal.TEN,
                "Travel",
                MatchType.NEW,
                null,
                null
            );

            // Then
            assertThat(vm.getMatchedRecord()).isNull();
            assertThat(vm.hasMatch()).isFalse();
        }

        @Test
        @DisplayName("backward compatible constructor should work")
        void backwardCompatibleConstructorShouldWork() {
            // When - using old constructor signature
            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                UUID.randomUUID(),
                LocalDate.now(),
                "Test",
                BigDecimal.TEN,
                MatchType.NEW,
                null
            );

            // Then
            assertThat(vm.getCategory()).isEmpty();
            assertThat(vm.getMatchedRecord()).isNull();
        }
    }

    @Nested
    @DisplayName("Accessibility")
    class Accessibility {

        @Test
        @DisplayName("should provide accessible text")
        void shouldProvideAccessibleText() {
            ImportCandidateViewModel vm = new ImportCandidateViewModel(
                UUID.randomUUID(), LocalDate.of(2026, 1, 15),
                "Office Supplies", new BigDecimal("-50.00"),
                MatchType.EXACT, UUID.randomUUID()
            );
            vm.setAction(ImportAction.SKIP);

            String text = vm.getAccessibleText();

            assertThat(text).contains("Expense");
            assertThat(text).contains("Office Supplies");
            assertThat(text).contains("15 Jan 2026");
            assertThat(text).contains("exact duplicate");
            assertThat(text).contains("Skip");
        }
    }

    // Helper method
    private ImportCandidateViewModel createCandidate(MatchType matchType) {
        UUID matchedId = matchType != MatchType.NEW ? UUID.randomUUID() : null;
        return new ImportCandidateViewModel(
            UUID.randomUUID(),
            LocalDate.now(),
            "Test Transaction",
            new BigDecimal("100.00"),
            matchType,
            matchedId
        );
    }
}
