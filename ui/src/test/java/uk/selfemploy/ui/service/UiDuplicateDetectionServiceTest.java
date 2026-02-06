package uk.selfemploy.ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.ui.viewmodel.ImportCandidateViewModel;
import uk.selfemploy.ui.viewmodel.MatchType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UiDuplicateDetectionService.
 *
 * <p>Sprint 10C: These tests verify the duplicate detection algorithm and
 * would have caught the missing commons-text dependency at build time.</p>
 *
 * <p>Test Cases from /rob's test design:
 * <ul>
 *   <li>TC-DUP-001: Exact match detection</li>
 *   <li>TC-DUP-002: Likely match detection (Levenshtein >= 80%)</li>
 *   <li>TC-DUP-003: New record detection</li>
 *   <li>TC-DUP-004: Case-insensitive matching</li>
 *   <li>TC-DUP-005: Whitespace normalization</li>
 *   <li>TC-DUP-006: Empty import list</li>
 *   <li>TC-DUP-007: Large batch performance</li>
 * </ul>
 * </p>
 */
@DisplayName("UiDuplicateDetectionService Integration Tests")
class UiDuplicateDetectionServiceTest {

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final TaxYear TAX_YEAR = TaxYear.of(2025);
    private static final LocalDate TEST_DATE = LocalDate.of(2025, 6, 15);

    private InMemoryIncomeService incomeService;
    private InMemoryExpenseService expenseService;
    private UiDuplicateDetectionService duplicateDetectionService;

    @BeforeEach
    void setUp() {
        incomeService = new InMemoryIncomeService();
        expenseService = new InMemoryExpenseService();
        // CRITICAL: This instantiation would fail if commons-text dependency is missing
        duplicateDetectionService = new UiDuplicateDetectionService(
            incomeService, expenseService, BUSINESS_ID
        );
    }

    // ========================================================================
    // TC-DUP-001: Exact Match Detection
    // ========================================================================

    @Nested
    @DisplayName("TC-DUP-001: Exact Match Detection")
    class ExactMatchDetectionTests {

        @Test
        @DisplayName("should detect exact match for income with same date, amount, and description")
        void shouldDetectExactMatchForIncome() {
            // Given - existing income in database
            incomeService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("1500.00"),
                "Web development project", IncomeCategory.SALES, "INV-001");

            // When - import income with exact same values
            Income importedIncome = createIncome(TEST_DATE, new BigDecimal("1500.00"),
                "Web development project");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(List.of(importedIncome), TAX_YEAR);

            // Then - should be detected as EXACT match
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.EXACT);
            assertThat(candidates.get(0).hasMatch()).isTrue();
        }

        @Test
        @DisplayName("should detect exact match for expense with same date, amount, and description")
        void shouldDetectExactMatchForExpense() {
            // Given - existing expense in database
            expenseService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("250.00"),
                "Office supplies", ExpenseCategory.OFFICE_COSTS, null, null);

            // When - import expense with exact same values
            Expense importedExpense = createExpense(TEST_DATE, new BigDecimal("250.00"),
                "Office supplies");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeExpenses(List.of(importedExpense), TAX_YEAR);

            // Then - should be detected as EXACT match
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.EXACT);
            assertThat(candidates.get(0).hasMatch()).isTrue();
        }

        @Test
        @DisplayName("should return matched record details for exact match")
        void shouldReturnMatchedRecordDetailsForExactMatch() {
            // Given
            incomeService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("2000.00"),
                "Consulting fee", IncomeCategory.SALES, "REF-123");

            // When
            Income importedIncome = createIncome(TEST_DATE, new BigDecimal("2000.00"),
                "Consulting fee");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(List.of(importedIncome), TAX_YEAR);

            // Then
            assertThat(candidates.get(0).getMatchedRecord()).isNotNull();
            assertThat(candidates.get(0).getMatchedRecordId()).isNotNull();
        }
    }

    // ========================================================================
    // TC-DUP-002: Likely Match Detection (Levenshtein >= 80%)
    // ========================================================================

    @Nested
    @DisplayName("TC-DUP-002: Likely Match Detection")
    class LikelyMatchDetectionTests {

        @Test
        @DisplayName("should detect likely match when description similarity >= 80%")
        void shouldDetectLikelyMatchForSimilarDescription() {
            // Given - existing income
            incomeService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("1500.00"),
                "Web development project", IncomeCategory.SALES, null);

            // When - import with slightly different description (>80% similar)
            // "Web development project" vs "Web development projects" = ~95% similar
            Income importedIncome = createIncome(TEST_DATE, new BigDecimal("1500.00"),
                "Web development projects");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(List.of(importedIncome), TAX_YEAR);

            // Then - should be detected as LIKELY match
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.LIKELY);
        }

        @Test
        @DisplayName("should detect likely match for expense with similar description")
        void shouldDetectLikelyMatchForExpense() {
            // Given
            expenseService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("100.00"),
                "Train ticket London", ExpenseCategory.TRAVEL, null, null);

            // When - similar description
            Expense importedExpense = createExpense(TEST_DATE, new BigDecimal("100.00"),
                "Train ticket to London");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeExpenses(List.of(importedExpense), TAX_YEAR);

            // Then
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.LIKELY);
        }

        @Test
        @DisplayName("should detect SIMILAR when similarity is below 80% but date+amount match")
        void shouldDetectSimilarWhenSimilarityBelowThreshold() {
            // Given
            incomeService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("1000.00"),
                "Design work", IncomeCategory.SALES, null);

            // When - very different description (<80% similar) but same date and amount
            Income importedIncome = createIncome(TEST_DATE, new BigDecimal("1000.00"),
                "Development project");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(List.of(importedIncome), TAX_YEAR);

            // Then - should be SIMILAR (same date+amount, different description)
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.SIMILAR);
            assertThat(candidates.get(0).hasMatch()).isTrue();
        }
    }

    // ========================================================================
    // TC-DUP-003: New Record Detection
    // ========================================================================

    @Nested
    @DisplayName("TC-DUP-003: New Record Detection")
    class NewRecordDetectionTests {

        @Test
        @DisplayName("should detect new income when no match exists")
        void shouldDetectNewIncomeWhenNoMatch() {
            // Given - empty database (no existing incomes)

            // When
            Income importedIncome = createIncome(TEST_DATE, new BigDecimal("500.00"),
                "New client payment");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(List.of(importedIncome), TAX_YEAR);

            // Then
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.NEW);
            assertThat(candidates.get(0).hasMatch()).isFalse();
            assertThat(candidates.get(0).getMatchedRecord()).isNull();
        }

        @Test
        @DisplayName("should detect new expense when no match exists")
        void shouldDetectNewExpenseWhenNoMatch() {
            // Given - empty database

            // When
            Expense importedExpense = createExpense(TEST_DATE, new BigDecimal("75.00"),
                "Software subscription");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeExpenses(List.of(importedExpense), TAX_YEAR);

            // Then
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.NEW);
        }

        @Test
        @DisplayName("should detect new when date differs")
        void shouldDetectNewWhenDateDiffers() {
            // Given - existing income on different date
            incomeService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("1000.00"),
                "Same description", IncomeCategory.SALES, null);

            // When - same amount and description but different date
            Income importedIncome = createIncome(TEST_DATE.plusDays(1), new BigDecimal("1000.00"),
                "Same description");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(List.of(importedIncome), TAX_YEAR);

            // Then - should be NEW (date must match for duplicate detection)
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.NEW);
        }

        @Test
        @DisplayName("should detect new when amount differs")
        void shouldDetectNewWhenAmountDiffers() {
            // Given
            incomeService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("1000.00"),
                "Same description", IncomeCategory.SALES, null);

            // When - same date and description but different amount
            Income importedIncome = createIncome(TEST_DATE, new BigDecimal("1500.00"),
                "Same description");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(List.of(importedIncome), TAX_YEAR);

            // Then
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.NEW);
        }
    }

    // ========================================================================
    // TC-DUP-003b: SIMILAR Match Detection (same date+amount, different description)
    // ========================================================================

    @Nested
    @DisplayName("TC-DUP-003b: SIMILAR Match Detection")
    class SimilarMatchDetectionTests {

        @Test
        @DisplayName("should detect SIMILAR when date and amount match but description is completely different")
        void shouldDetectSimilarForCompletelyDifferentDescription() {
            // Given - existing income
            incomeService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("1500.00"),
                "Web development project", IncomeCategory.SALES, null);

            // When - same date and amount, but totally different description
            Income importedIncome = createIncome(TEST_DATE, new BigDecimal("1500.00"),
                "Bank transfer received");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(List.of(importedIncome), TAX_YEAR);

            // Then - should be SIMILAR (same date+amount, different description)
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.SIMILAR);
            assertThat(candidates.get(0).hasMatch()).isTrue();
            assertThat(candidates.get(0).getMatchedRecordId()).isNotNull();
        }

        @Test
        @DisplayName("should detect SIMILAR for expense with different description")
        void shouldDetectSimilarForExpense() {
            // Given
            expenseService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("100.00"),
                "Train ticket London", ExpenseCategory.TRAVEL, null, null);

            // When - same date and amount, completely different description
            Expense importedExpense = createExpense(TEST_DATE, new BigDecimal("100.00"),
                "Card payment TFL");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeExpenses(List.of(importedExpense), TAX_YEAR);

            // Then
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.SIMILAR);
        }

        @Test
        @DisplayName("should return matched record details for SIMILAR match")
        void shouldReturnMatchedRecordDetailsForSimilarMatch() {
            // Given
            incomeService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("2000.00"),
                "Invoice payment ABC Ltd", IncomeCategory.SALES, null);

            // When
            Income importedIncome = createIncome(TEST_DATE, new BigDecimal("2000.00"),
                "FPS credit reference XYZ");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(List.of(importedIncome), TAX_YEAR);

            // Then
            assertThat(candidates.get(0).getMatchedRecord()).isNotNull();
            assertThat(candidates.get(0).getMatchedRecord().getDescription())
                .isEqualTo("Invoice payment ABC Ltd");
        }
    }

    // ========================================================================
    // TC-DUP-004: Case-Insensitive Matching
    // ========================================================================

    @Nested
    @DisplayName("TC-DUP-004: Case-Insensitive Matching")
    class CaseInsensitiveMatchingTests {

        @Test
        @DisplayName("should match income regardless of description case")
        void shouldMatchIncomeRegardlessOfCase() {
            // Given
            incomeService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("1000.00"),
                "Web Development Project", IncomeCategory.SALES, null);

            // When - same description but different case
            Income importedIncome = createIncome(TEST_DATE, new BigDecimal("1000.00"),
                "WEB DEVELOPMENT PROJECT");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(List.of(importedIncome), TAX_YEAR);

            // Then - should be EXACT match (case-insensitive)
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.EXACT);
        }

        @Test
        @DisplayName("should match expense regardless of description case")
        void shouldMatchExpenseRegardlessOfCase() {
            // Given
            expenseService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("50.00"),
                "office supplies", ExpenseCategory.OFFICE_COSTS, null, null);

            // When
            Expense importedExpense = createExpense(TEST_DATE, new BigDecimal("50.00"),
                "OFFICE SUPPLIES");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeExpenses(List.of(importedExpense), TAX_YEAR);

            // Then
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.EXACT);
        }

        @Test
        @DisplayName("should match with mixed case variations")
        void shouldMatchWithMixedCaseVariations() {
            // Given
            incomeService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("2500.00"),
                "Invoice Payment ABC Ltd", IncomeCategory.SALES, null);

            // When
            Income importedIncome = createIncome(TEST_DATE, new BigDecimal("2500.00"),
                "invoice payment abc ltd");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(List.of(importedIncome), TAX_YEAR);

            // Then
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.EXACT);
        }
    }

    // ========================================================================
    // TC-DUP-005: Whitespace Normalization
    // ========================================================================

    @Nested
    @DisplayName("TC-DUP-005: Whitespace Normalization")
    class WhitespaceNormalizationTests {

        @Test
        @DisplayName("should match with leading/trailing whitespace")
        void shouldMatchWithLeadingTrailingWhitespace() {
            // Given
            incomeService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("1000.00"),
                "Project payment", IncomeCategory.SALES, null);

            // When - description with extra whitespace
            Income importedIncome = createIncome(TEST_DATE, new BigDecimal("1000.00"),
                "  Project payment  ");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(List.of(importedIncome), TAX_YEAR);

            // Then
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.EXACT);
        }

        @Test
        @DisplayName("should match with multiple internal spaces")
        void shouldMatchWithMultipleInternalSpaces() {
            // Given
            incomeService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("750.00"),
                "Client ABC payment", IncomeCategory.SALES, null);

            // When - multiple spaces between words
            Income importedIncome = createIncome(TEST_DATE, new BigDecimal("750.00"),
                "Client   ABC    payment");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(List.of(importedIncome), TAX_YEAR);

            // Then
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.EXACT);
        }

        @Test
        @DisplayName("should match expense with normalized whitespace")
        void shouldMatchExpenseWithNormalizedWhitespace() {
            // Given
            expenseService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("120.00"),
                "Train ticket London", ExpenseCategory.TRAVEL, null, null);

            // When
            Expense importedExpense = createExpense(TEST_DATE, new BigDecimal("120.00"),
                "  Train   ticket   London  ");
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeExpenses(List.of(importedExpense), TAX_YEAR);

            // Then
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.EXACT);
        }
    }

    // ========================================================================
    // TC-DUP-006: Empty Import List
    // ========================================================================

    @Nested
    @DisplayName("TC-DUP-006: Empty Import List")
    class EmptyImportListTests {

        @Test
        @DisplayName("should return empty list for null income list")
        void shouldReturnEmptyForNullIncomeList() {
            // When
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(null, TAX_YEAR);

            // Then
            assertThat(candidates).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for empty income list")
        void shouldReturnEmptyForEmptyIncomeList() {
            // When
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(List.of(), TAX_YEAR);

            // Then
            assertThat(candidates).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for null expense list")
        void shouldReturnEmptyForNullExpenseList() {
            // When
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeExpenses(null, TAX_YEAR);

            // Then
            assertThat(candidates).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for empty expense list")
        void shouldReturnEmptyForEmptyExpenseList() {
            // When
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeExpenses(List.of(), TAX_YEAR);

            // Then
            assertThat(candidates).isEmpty();
        }
    }

    // ========================================================================
    // TC-DUP-007: Large Batch Performance
    // ========================================================================

    @Nested
    @DisplayName("TC-DUP-007: Large Batch Performance")
    class LargeBatchPerformanceTests {

        @Test
        @DisplayName("should process 100 income records in under 2 seconds")
        void shouldProcess100IncomesQuickly() {
            // Given - 50 existing records with longer descriptions for fuzzy matching
            // Using longer descriptions so that small changes stay above 80% similarity
            for (int i = 0; i < 50; i++) {
                incomeService.create(BUSINESS_ID, TEST_DATE.plusDays(i),
                    new BigDecimal(1000 + i), "Web development project payment number " + i,
                    IncomeCategory.SALES, null);
            }

            // Create 100 import candidates (mix of duplicates and new)
            List<Income> importList = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                if (i < 25) {
                    // Exact duplicates (same description)
                    importList.add(createIncome(TEST_DATE.plusDays(i),
                        new BigDecimal(1000 + i), "Web development project payment number " + i));
                } else if (i < 50) {
                    // Similar (likely matches) - small typo keeps similarity >= 80%
                    // "Web development project payment number X" vs
                    // "Web development project payments number X" (~97% similar - 1 char diff in 40+ chars)
                    int matchIndex = i - 25;
                    importList.add(createIncome(TEST_DATE.plusDays(matchIndex),
                        new BigDecimal(1000 + matchIndex), "Web development project payments number " + matchIndex));
                } else {
                    // New records (completely different)
                    importList.add(createIncome(TEST_DATE.plusDays(i),
                        new BigDecimal(2000 + i), "New income " + i));
                }
            }

            // When
            long startTime = System.currentTimeMillis();
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(importList, TAX_YEAR);
            long duration = System.currentTimeMillis() - startTime;

            // Then
            assertThat(candidates).hasSize(100);
            assertThat(duration).as("Processing 100 records should take less than 2 seconds")
                .isLessThan(2000);

            // Verify distribution of match types
            long exactCount = candidates.stream()
                .filter(c -> c.getMatchType() == MatchType.EXACT).count();
            long likelyCount = candidates.stream()
                .filter(c -> c.getMatchType() == MatchType.LIKELY).count();
            long newCount = candidates.stream()
                .filter(c -> c.getMatchType() == MatchType.NEW).count();

            assertThat(exactCount).isEqualTo(25);
            assertThat(likelyCount).isEqualTo(25);
            assertThat(newCount).isEqualTo(50);
        }

        @Test
        @DisplayName("should process 100 expense records in under 2 seconds")
        void shouldProcess100ExpensesQuickly() {
            // Given - 50 existing expenses
            for (int i = 0; i < 50; i++) {
                expenseService.create(BUSINESS_ID, TEST_DATE.plusDays(i),
                    new BigDecimal(100 + i), "Existing expense " + i,
                    ExpenseCategory.OFFICE_COSTS, null, null);
            }

            // Create 100 import candidates
            List<Expense> importList = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                if (i < 25) {
                    // Exact duplicates
                    importList.add(createExpense(TEST_DATE.plusDays(i),
                        new BigDecimal(100 + i), "Existing expense " + i));
                } else {
                    // New records
                    importList.add(createExpense(TEST_DATE.plusDays(i),
                        new BigDecimal(500 + i), "New expense " + i));
                }
            }

            // When
            long startTime = System.currentTimeMillis();
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeExpenses(importList, TAX_YEAR);
            long duration = System.currentTimeMillis() - startTime;

            // Then
            assertThat(candidates).hasSize(100);
            assertThat(duration).as("Processing 100 records should take less than 2 seconds")
                .isLessThan(2000);
        }
    }

    // ========================================================================
    // Dependency Verification Tests (Would catch missing commons-text)
    // ========================================================================

    @Nested
    @DisplayName("Dependency Verification")
    class DependencyVerificationTests {

        @Test
        @DisplayName("should successfully instantiate service (verifies LevenshteinDistance is available)")
        void shouldInstantiateServiceSuccessfully() {
            // This test would fail at class loading time if commons-text is missing
            UiDuplicateDetectionService service = new UiDuplicateDetectionService(
                incomeService, expenseService, BUSINESS_ID
            );
            assertThat(service).isNotNull();
        }

        @Test
        @DisplayName("should use Levenshtein distance for fuzzy matching")
        void shouldUseLevenshteinForFuzzyMatching() {
            // Given - record that requires Levenshtein calculation
            incomeService.create(BUSINESS_ID, TEST_DATE, new BigDecimal("1000.00"),
                "ABCDEFGHIJ", IncomeCategory.SALES, null);

            // When - import with 1 character difference (90% similar)
            Income importedIncome = createIncome(TEST_DATE, new BigDecimal("1000.00"),
                "ABCDEFGHIK"); // J -> K
            List<ImportCandidateViewModel> candidates = duplicateDetectionService
                .analyzeIncomes(List.of(importedIncome), TAX_YEAR);

            // Then - should be LIKELY match (90% > 80% threshold)
            assertThat(candidates).hasSize(1);
            assertThat(candidates.get(0).getMatchType()).isEqualTo(MatchType.LIKELY);
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private Income createIncome(LocalDate date, BigDecimal amount, String description) {
        return new Income(
            UUID.randomUUID(),
            BUSINESS_ID,
            date,
            amount,
            description,
            IncomeCategory.SALES,
            null,
            null,
            null,
            null,
            null
        );
    }

    private Expense createExpense(LocalDate date, BigDecimal amount, String description) {
        return new Expense(
            UUID.randomUUID(),
            BUSINESS_ID,
            date,
            amount,
            description,
            ExpenseCategory.OFFICE_COSTS,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}
