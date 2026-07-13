package uk.selfemploy.core.dedup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.core.bankimport.ImportedTransaction;
import uk.selfemploy.persistence.entity.ExpenseEntity;
import uk.selfemploy.persistence.entity.IncomeEntity;
import uk.selfemploy.persistence.repository.ExpenseRepository;
import uk.selfemploy.persistence.repository.IncomeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DuplicateDetectionService.
 *
 * <p>Tests the 3-tier duplicate detection system (ADR-10B-003):</p>
 * <ul>
 *   <li>EXACT: Same date + amount + description (case-insensitive)</li>
 *   <li>LIKELY: Same date + amount + Levenshtein distance &lt;= 3</li>
 *   <li>DATE_ONLY: Same date + similar amount (+/- 5%)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DuplicateDetectionService Tests")
class DuplicateDetectionServiceTest {

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    private DuplicateDetectionService service;

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final LocalDate TEST_DATE = LocalDate.of(2025, 6, 15);

    @BeforeEach
    void setUp() {
        service = new DuplicateDetectionService(incomeRepository, expenseRepository);
    }

    // === Helper Methods ===

    private ImportedTransaction createIncome(LocalDate date, BigDecimal amount, String description) {
        return new ImportedTransaction(date, amount, description, null, null);
    }

    private ImportedTransaction createExpense(LocalDate date, BigDecimal amount, String description) {
        // Expenses are negative in imports
        return new ImportedTransaction(date, amount.negate(), description, null, null);
    }

    private IncomeEntity createIncomeEntity(UUID id, LocalDate date, BigDecimal amount, String description) {
        IncomeEntity entity = new IncomeEntity();
        entity.setId(id);
        entity.setBusinessId(BUSINESS_ID);
        entity.setDate(date);
        entity.setAmount(amount);
        entity.setDescription(description);
        return entity;
    }

    private ExpenseEntity createExpenseEntity(UUID id, LocalDate date, BigDecimal amount, String description) {
        ExpenseEntity entity = new ExpenseEntity();
        entity.setId(id);
        entity.setBusinessId(BUSINESS_ID);
        entity.setDate(date);
        entity.setAmount(amount);  // Stored as positive
        entity.setDescription(description);
        return entity;
    }

    // === Exact Match Tests ===

    @Nested
    @DisplayName("Exact Match Detection")
    class ExactMatchDetection {

        @Test
        @DisplayName("should detect exact match for income with same date, amount, and description")
        void shouldDetectExactMatchForIncome() {
            // Given
            UUID existingId = UUID.randomUUID();
            IncomeEntity existing = createIncomeEntity(existingId, TEST_DATE,
                new BigDecimal("1500.00"), "Web development project");

            when(incomeRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existing));
            when(expenseRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            ImportedTransaction imported = createIncome(TEST_DATE,
                new BigDecimal("1500.00"), "Web development project");

            // When
            List<DuplicateMatch> results = service.detectDuplicates(
                List.of(imported), BUSINESS_ID);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).matchType()).isEqualTo(MatchType.EXACT);
            assertThat(results.get(0).confidence()).isEqualTo(1.0);
            assertThat(results.get(0).existingRecordId()).isEqualTo(existingId);
        }

        @Test
        @DisplayName("should detect exact match with case-insensitive description")
        void shouldDetectExactMatchCaseInsensitive() {
            // Given
            UUID existingId = UUID.randomUUID();
            IncomeEntity existing = createIncomeEntity(existingId, TEST_DATE,
                new BigDecimal("1500.00"), "WEB DEVELOPMENT PROJECT");

            when(incomeRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existing));
            when(expenseRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            ImportedTransaction imported = createIncome(TEST_DATE,
                new BigDecimal("1500.00"), "web development project");

            // When
            List<DuplicateMatch> results = service.detectDuplicates(
                List.of(imported), BUSINESS_ID);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).matchType()).isEqualTo(MatchType.EXACT);
        }

        @Test
        @DisplayName("should detect exact match with normalized whitespace")
        void shouldDetectExactMatchNormalizedWhitespace() {
            // Given
            UUID existingId = UUID.randomUUID();
            IncomeEntity existing = createIncomeEntity(existingId, TEST_DATE,
                new BigDecimal("1500.00"), "Web   Development   Project");

            when(incomeRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existing));
            when(expenseRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            ImportedTransaction imported = createIncome(TEST_DATE,
                new BigDecimal("1500.00"), "Web Development Project");

            // When
            List<DuplicateMatch> results = service.detectDuplicates(
                List.of(imported), BUSINESS_ID);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).matchType()).isEqualTo(MatchType.EXACT);
        }
    }

    // === Likely Match Tests ===

    @Nested
    @DisplayName("Likely Match Detection (Fuzzy)")
    class LikelyMatchDetection {

        @Test
        @DisplayName("should detect likely match with similar description (Levenshtein <= 3)")
        void shouldDetectLikelyMatchWithSimilarDescription() {
            // Given
            UUID existingId = UUID.randomUUID();
            // "Web development" vs "Web developmnt" - distance 1
            IncomeEntity existing = createIncomeEntity(existingId, TEST_DATE,
                new BigDecimal("1500.00"), "Web developmnt project");

            when(incomeRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existing));
            when(expenseRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            ImportedTransaction imported = createIncome(TEST_DATE,
                new BigDecimal("1500.00"), "Web development project");

            // When
            List<DuplicateMatch> results = service.detectDuplicates(
                List.of(imported), BUSINESS_ID);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).matchType()).isEqualTo(MatchType.LIKELY);
            assertThat(results.get(0).confidence()).isGreaterThan(0.8);
            assertThat(results.get(0).existingRecordId()).isEqualTo(existingId);
        }

        @Test
        @DisplayName("should not detect likely match when description too different")
        void shouldNotDetectLikelyMatchWhenDescriptionTooDifferent() {
            // Given
            UUID existingId = UUID.randomUUID();
            // Completely different descriptions
            IncomeEntity existing = createIncomeEntity(existingId, TEST_DATE,
                new BigDecimal("1500.00"), "Marketing campaign");

            when(incomeRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existing));
            when(expenseRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            ImportedTransaction imported = createIncome(TEST_DATE,
                new BigDecimal("1500.00"), "Web development project");

            // When
            List<DuplicateMatch> results = service.detectDuplicates(
                List.of(imported), BUSINESS_ID);

            // Then
            assertThat(results).hasSize(1);
            // Different description should result in DATE_ONLY match (same date and amount)
            assertThat(results.get(0).matchType()).isNotEqualTo(MatchType.EXACT);
        }
    }

    // === Date Only Match Tests ===

    @Nested
    @DisplayName("Date-Only Match Detection")
    class DateOnlyMatchDetection {

        @Test
        @DisplayName("should detect date-only match with same date and amount within 5%")
        void shouldDetectDateOnlyMatchDifferentAmount() {
            // Given
            UUID existingId = UUID.randomUUID();
            IncomeEntity existing = createIncomeEntity(existingId, TEST_DATE,
                new BigDecimal("1500.00"), "Project payment");

            when(incomeRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existing));
            when(expenseRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            // Amount within 5% (1520 is ~1.3% of 1500), completely different description
            ImportedTransaction imported = createIncome(TEST_DATE,
                new BigDecimal("1520.00"), "Consulting fee");

            // When
            List<DuplicateMatch> results = service.detectDuplicates(
                List.of(imported), BUSINESS_ID);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).matchType()).isEqualTo(MatchType.DATE_ONLY);
            assertThat(results.get(0).confidence()).isLessThan(0.5);
        }

        @Test
        @DisplayName("should detect date-only match when amount within 5% but different description")
        void shouldDetectDateOnlyMatchAmountWithin5Percent() {
            // Given
            UUID existingId = UUID.randomUUID();
            IncomeEntity existing = createIncomeEntity(existingId, TEST_DATE,
                new BigDecimal("1500.00"), "Project Alpha");

            when(incomeRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existing));
            when(expenseRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            // Amount within 5% (1540 is ~2.7% of 1500), completely different description
            ImportedTransaction imported = createIncome(TEST_DATE,
                new BigDecimal("1540.00"), "Completely Different Description");

            // When
            List<DuplicateMatch> results = service.detectDuplicates(
                List.of(imported), BUSINESS_ID);

            // Then
            assertThat(results).hasSize(1);
            // Should be DATE_ONLY since description is completely different
            assertThat(results.get(0).matchType()).isEqualTo(MatchType.DATE_ONLY);
        }
    }

    // === No Match Tests ===

    @Nested
    @DisplayName("No Match Detection")
    class NoMatchDetection {

        @Test
        @DisplayName("should return no match when no existing records")
        void shouldReturnNoMatchWhenNoExistingRecords() {
            // Given
            when(incomeRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());
            when(expenseRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            ImportedTransaction imported = createIncome(TEST_DATE,
                new BigDecimal("1500.00"), "Web development project");

            // When
            List<DuplicateMatch> results = service.detectDuplicates(
                List.of(imported), BUSINESS_ID);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).matchType()).isEqualTo(MatchType.NONE);
            assertThat(results.get(0).confidence()).isEqualTo(0.0);
            assertThat(results.get(0).existingRecordId()).isNull();
        }

        @Test
        @DisplayName("should return no match when date is different")
        void shouldReturnNoMatchWhenDateIsDifferent() {
            // Given
            UUID existingId = UUID.randomUUID();
            IncomeEntity existing = createIncomeEntity(existingId,
                TEST_DATE.minusDays(10),  // Different date
                new BigDecimal("1500.00"), "Web development project");

            when(incomeRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existing));
            when(expenseRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            ImportedTransaction imported = createIncome(TEST_DATE,
                new BigDecimal("1500.00"), "Web development project");

            // When
            List<DuplicateMatch> results = service.detectDuplicates(
                List.of(imported), BUSINESS_ID);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).matchType()).isEqualTo(MatchType.NONE);
        }
    }

    // === Expense Matching Tests ===

    @Nested
    @DisplayName("Expense Duplicate Detection")
    class ExpenseDuplicateDetection {

        @Test
        @DisplayName("should detect exact match for expense")
        void shouldDetectExactMatchForExpense() {
            // Given
            UUID existingId = UUID.randomUUID();
            ExpenseEntity existing = createExpenseEntity(existingId, TEST_DATE,
                new BigDecimal("500.00"), "Office supplies");

            when(incomeRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());
            when(expenseRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existing));

            // Expense imports are negative
            ImportedTransaction imported = createExpense(TEST_DATE,
                new BigDecimal("500.00"), "Office supplies");

            // When
            List<DuplicateMatch> results = service.detectDuplicates(
                List.of(imported), BUSINESS_ID);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).matchType()).isEqualTo(MatchType.EXACT);
            assertThat(results.get(0).existingRecordId()).isEqualTo(existingId);
        }
    }

    // === Multiple Transactions Tests ===

    @Nested
    @DisplayName("Multiple Transaction Detection")
    class MultipleTransactionDetection {

        @Test
        @DisplayName("should handle multiple imports with mixed match types")
        void shouldHandleMultipleImportsWithMixedMatchTypes() {
            // Given
            UUID exactMatchId = UUID.randomUUID();
            UUID likelyMatchId = UUID.randomUUID();

            IncomeEntity exactMatch = createIncomeEntity(exactMatchId, TEST_DATE,
                new BigDecimal("1500.00"), "Project Alpha");
            IncomeEntity likelyMatch = createIncomeEntity(likelyMatchId, TEST_DATE.plusDays(1),
                new BigDecimal("2000.00"), "Project Beta");

            when(incomeRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(exactMatch, likelyMatch));
            when(expenseRepository.findEntitiesByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            ImportedTransaction import1 = createIncome(TEST_DATE,
                new BigDecimal("1500.00"), "Project Alpha");  // Exact match
            ImportedTransaction import2 = createIncome(TEST_DATE.plusDays(1),
                new BigDecimal("2000.00"), "Project Beta");   // Exact match
            ImportedTransaction import3 = createIncome(TEST_DATE.plusDays(5),
                new BigDecimal("3000.00"), "New Project");    // No match

            // When
            List<DuplicateMatch> results = service.detectDuplicates(
                List.of(import1, import2, import3), BUSINESS_ID);

            // Then
            assertThat(results).hasSize(3);

            DuplicateMatch result1 = results.stream()
                .filter(r -> r.imported().equals(import1))
                .findFirst().orElseThrow();
            assertThat(result1.matchType()).isEqualTo(MatchType.EXACT);

            DuplicateMatch result2 = results.stream()
                .filter(r -> r.imported().equals(import2))
                .findFirst().orElseThrow();
            assertThat(result2.matchType()).isEqualTo(MatchType.EXACT);

            DuplicateMatch result3 = results.stream()
                .filter(r -> r.imported().equals(import3))
                .findFirst().orElseThrow();
            assertThat(result3.matchType()).isEqualTo(MatchType.NONE);
        }

        @Test
        @DisplayName("should return empty list for empty input")
        void shouldReturnEmptyListForEmptyInput() {
            // When
            List<DuplicateMatch> results = service.detectDuplicates(
                Collections.emptyList(), BUSINESS_ID);

            // Then
            assertThat(results).isEmpty();
        }
    }
}
