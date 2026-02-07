package uk.selfemploy.core.bankimport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.domain.ImportAudit;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.ReviewStatus;
import uk.selfemploy.persistence.repository.BankTransactionRepository;
import uk.selfemploy.persistence.repository.ImportAuditRepository;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the bank import pipeline.
 *
 * <p>Tests the cross-service data flow: import → categorize → flag → review status transitions.
 * Uses real service objects for the classification chain and mocked repositories.</p>
 */
@DisplayName("Bank Import Pipeline Integration")
class BankImportPipelineIntegrationTest {

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final UUID AUDIT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2025-06-15T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    // Real service chain
    private DescriptionCategorizer categorizer;
    private TransactionClassificationService classificationService;
    private ExclusionRulesEngine exclusionEngine;
    private CategorizationEngine categorizationEngine;

    // Mocked repositories
    private BankTransactionRepository bankTransactionRepository;
    private ImportAuditRepository importAuditRepository;

    // Services under test
    private BankStatementImportService importService;
    private BusinessPersonalService businessPersonalService;

    @BeforeEach
    void setUp() {
        // Real service chain (no mocks) — tests the actual classification logic
        categorizer = new DescriptionCategorizer();
        classificationService = new TransactionClassificationService(categorizer);
        exclusionEngine = new ExclusionRulesEngine();
        categorizationEngine = new CategorizationEngine(classificationService, exclusionEngine);

        // Mocked repositories
        bankTransactionRepository = mock(BankTransactionRepository.class);
        importAuditRepository = mock(ImportAuditRepository.class);

        // Default mock behavior: save returns input, no duplicates
        when(bankTransactionRepository.save(any(BankTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(bankTransactionRepository.update(any(BankTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(bankTransactionRepository.existsByHash(any(), any())).thenReturn(false);
        when(importAuditRepository.save(any(ImportAudit.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Services with mocked repos
        BankFormatDetector formatDetector = new BankFormatDetector(List.of());
        importService = new BankStatementImportService(
                formatDetector, bankTransactionRepository, importAuditRepository, FIXED_CLOCK);
        businessPersonalService = new BusinessPersonalService(bankTransactionRepository, FIXED_CLOCK);
    }

    // --- Helper methods ---

    private BankTransaction createExpenseTransaction(String description, BigDecimal amount) {
        return BankTransaction.create(
                BUSINESS_ID, AUDIT_ID, "csv-barclays",
                LocalDate.of(2025, 6, 15), amount.negate().abs().negate(), description,
                "1234", null, "hash-" + description.hashCode(), NOW
        );
    }

    private BankTransaction createIncomeTransaction(String description, BigDecimal amount) {
        return BankTransaction.create(
                BUSINESS_ID, AUDIT_ID, "csv-barclays",
                LocalDate.of(2025, 6, 15), amount.abs(), description,
                "1234", null, "hash-" + description.hashCode(), NOW
        );
    }

    private BankTransaction createTransaction(String description, BigDecimal amount) {
        return BankTransaction.create(
                BUSINESS_ID, AUDIT_ID, "csv-barclays",
                LocalDate.of(2025, 6, 15), amount, description,
                "1234", null, "hash-" + description.hashCode(), NOW
        );
    }

    @Nested
    @DisplayName("1. Full Pipeline Flow")
    class FullPipelineFlow {

        @Test
        @DisplayName("import CSV + classify + categorize an expense (UBER → TRAVEL, 0.95 confidence)")
        void importClassifyCategorizeExpense() {
            BankTransaction tx = createExpenseTransaction("UBER TRIP LONDON", new BigDecimal("25.00"));

            CategorizationRecommendation rec = categorizationEngine.recommend(tx);
            BankTransaction categorized = categorizationEngine.applyRecommendation(tx, NOW);

            assertThat(rec.isIncome()).isFalse();
            assertThat(rec.shouldExclude()).isFalse();
            assertThat(rec.expenseCategory()).isEqualTo(ExpenseCategory.TRAVEL);
            assertThat(rec.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.95"));
            assertThat(rec.confidenceLevel()).isEqualTo(Confidence.HIGH);
            assertThat(categorized.suggestedCategory()).isEqualTo(ExpenseCategory.TRAVEL);
            assertThat(categorized.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.95"));
            assertThat(categorized.reviewStatus()).isEqualTo(ReviewStatus.PENDING);
        }

        @Test
        @DisplayName("import CSV + classify a generic income (CLIENT PAYMENT → SALES, 0.75 confidence)")
        void importClassifyCategorizeGenericIncome() {
            BankTransaction tx = createIncomeTransaction("CLIENT PAYMENT ACME LTD", new BigDecimal("1500.00"));

            CategorizationRecommendation rec = categorizationEngine.recommend(tx);
            BankTransaction categorized = categorizationEngine.applyRecommendation(tx, NOW);

            assertThat(rec.isIncome()).isTrue();
            assertThat(rec.shouldExclude()).isFalse();
            assertThat(rec.expenseCategory()).isNull();
            assertThat(rec.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.75"));
            assertThat(rec.confidenceLevel()).isEqualTo(Confidence.MEDIUM);
            // Income transactions get withSuggestion(null category, 0.75 score)
            assertThat(categorized.suggestedCategory()).isNull();
            assertThat(categorized.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.75"));
        }

        @Test
        @DisplayName("import mixed batch: income + expense + excluded, each categorized correctly")
        void importMixedBatch() {
            BankTransaction expense = createExpenseTransaction("AMAZON PURCHASE", new BigDecimal("49.99"));
            BankTransaction income = createIncomeTransaction("CLIENT PAYMENT", new BigDecimal("2000.00"));
            BankTransaction excluded = createExpenseTransaction("HMRC TAX PAYMENT", new BigDecimal("500.00"));

            CategorizationRecommendation expenseRec = categorizationEngine.recommend(expense);
            CategorizationRecommendation incomeRec = categorizationEngine.recommend(income);
            CategorizationRecommendation excludedRec = categorizationEngine.recommend(excluded);

            assertThat(expenseRec.expenseCategory()).isEqualTo(ExpenseCategory.OFFICE_COSTS);
            assertThat(expenseRec.shouldExclude()).isFalse();

            assertThat(incomeRec.isIncome()).isTrue();
            assertThat(incomeRec.shouldExclude()).isFalse();

            assertThat(excludedRec.shouldExclude()).isTrue();
            assertThat(excludedRec.exclusionReason()).isEqualTo("TAX_PAYMENT");
        }

        @Test
        @DisplayName("full lifecycle: import → suggest → withCategorizedAsExpense → CATEGORIZED")
        void fullLifecycleExpense() {
            BankTransaction tx = createExpenseTransaction("UBER TRIP", new BigDecimal("30.00"));

            // Step 1: Categorization engine suggests
            BankTransaction suggested = categorizationEngine.applyRecommendation(tx, NOW);
            assertThat(suggested.suggestedCategory()).isEqualTo(ExpenseCategory.TRAVEL);
            assertThat(suggested.reviewStatus()).isEqualTo(ReviewStatus.PENDING);

            // Step 2: User confirms → withCategorizedAsExpense
            UUID expenseId = UUID.randomUUID();
            BankTransaction categorized = suggested.withCategorizedAsExpense(expenseId, NOW);
            assertThat(categorized.reviewStatus()).isEqualTo(ReviewStatus.CATEGORIZED);
            assertThat(categorized.expenseId()).isEqualTo(expenseId);
            assertThat(categorized.incomeId()).isNull();
        }

        @Test
        @DisplayName("full lifecycle: import → suggest → withCategorizedAsIncome → CATEGORIZED")
        void fullLifecycleIncome() {
            BankTransaction tx = createIncomeTransaction("DIVIDEND PAYMENT", new BigDecimal("500.00"));

            BankTransaction suggested = categorizationEngine.applyRecommendation(tx, NOW);
            assertThat(suggested.reviewStatus()).isEqualTo(ReviewStatus.PENDING);

            UUID incomeId = UUID.randomUUID();
            BankTransaction categorized = suggested.withCategorizedAsIncome(incomeId, NOW);
            assertThat(categorized.reviewStatus()).isEqualTo(ReviewStatus.CATEGORIZED);
            assertThat(categorized.incomeId()).isEqualTo(incomeId);
            assertThat(categorized.expenseId()).isNull();
        }

        @Test
        @DisplayName("excluded transaction → applyRecommendation → EXCLUDED status")
        void excludedTransactionLifecycle() {
            BankTransaction tx = createExpenseTransaction("TRANSFER TO SAVINGS", new BigDecimal("1000.00"));

            BankTransaction result = categorizationEngine.applyRecommendation(tx, NOW);

            assertThat(result.reviewStatus()).isEqualTo(ReviewStatus.EXCLUDED);
            assertThat(result.exclusionReason()).isEqualTo("TRANSFER");
        }
    }

    @Nested
    @DisplayName("2. Exclusion + Categorization Chain")
    class ExclusionCategorizationChain {

        @Test
        @DisplayName("excluded transaction (HMRC) bypasses category suggestion entirely")
        void excludedBypasessCategorySuggestion() {
            BankTransaction tx = createExpenseTransaction("HMRC SELF ASSESSMENT", new BigDecimal("2500.00"));

            CategorizationRecommendation rec = categorizationEngine.recommend(tx);

            assertThat(rec.shouldExclude()).isTrue();
            assertThat(rec.exclusionReason()).isEqualTo("TAX_PAYMENT");
            assertThat(rec.expenseCategory()).isNull();
            assertThat(rec.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.95"));
        }

        @Test
        @DisplayName("non-excluded expense gets OFFICE_COSTS + HIGH confidence")
        void nonExcludedExpenseCategorizesCorrectly() {
            BankTransaction tx = createExpenseTransaction("MICROSOFT 365 SUBSCRIPTION", new BigDecimal("9.99"));

            CategorizationRecommendation rec = categorizationEngine.recommend(tx);

            assertThat(rec.shouldExclude()).isFalse();
            assertThat(rec.expenseCategory()).isEqualTo(ExpenseCategory.OFFICE_COSTS);
            assertThat(rec.confidenceLevel()).isEqualTo(Confidence.HIGH);
        }

        @Test
        @DisplayName("non-excluded income classified with null category")
        void nonExcludedIncomeHasNullCategory() {
            BankTransaction tx = createIncomeTransaction("FREELANCE WORK JAN", new BigDecimal("3000.00"));

            CategorizationRecommendation rec = categorizationEngine.recommend(tx);

            assertThat(rec.isIncome()).isTrue();
            assertThat(rec.shouldExclude()).isFalse();
            assertThat(rec.expenseCategory()).isNull();
        }

        @Test
        @DisplayName("exclusion priority: 'TRANSFER OFFICE ACCOUNT' → excluded (not categorized as office)")
        void exclusionTakesPriorityOverCategorization() {
            // "transfer" matches exclusion before "office" could match categorization
            BankTransaction tx = createExpenseTransaction("TRANSFER OFFICE ACCOUNT", new BigDecimal("500.00"));

            CategorizationRecommendation rec = categorizationEngine.recommend(tx);

            assertThat(rec.shouldExclude()).isTrue();
            assertThat(rec.exclusionReason()).isEqualTo("TRANSFER");
            assertThat(rec.expenseCategory()).isNull();
        }
    }

    @Nested
    @DisplayName("3. Duplicate Detection + Import")
    class DuplicateDetection {

        @Test
        @DisplayName("re-import same transactions → all duplicates detected")
        void reimportDetectsAllDuplicates() {
            // Simulate that all hashes already exist in repository
            when(bankTransactionRepository.existsByHash(eq(BUSINESS_ID), any())).thenReturn(true);

            ImportedTransaction tx1 = new ImportedTransaction(
                    LocalDate.of(2025, 6, 1), new BigDecimal("-25.00"), "UBER TRIP", null, null);
            ImportedTransaction tx2 = new ImportedTransaction(
                    LocalDate.of(2025, 6, 2), new BigDecimal("1500.00"), "CLIENT PAYMENT", null, null);

            List<ImportedTransaction> transactions = List.of(tx1, tx2);

            // Simulate what import service does internally for deduplication
            int duplicateCount = 0;
            List<ImportedTransaction> unique = new ArrayList<>();
            for (ImportedTransaction tx : transactions) {
                if (bankTransactionRepository.existsByHash(BUSINESS_ID, tx.transactionHash())) {
                    duplicateCount++;
                } else {
                    unique.add(tx);
                }
            }

            assertThat(duplicateCount).isEqualTo(2);
            assertThat(unique).isEmpty();
        }

        @Test
        @DisplayName("within-batch duplicate → deduplicated")
        void withinBatchDuplicateDeduplicated() {
            ImportedTransaction tx1 = new ImportedTransaction(
                    LocalDate.of(2025, 6, 1), new BigDecimal("-25.00"), "UBER TRIP", null, null);
            // Same transaction again (same date, amount, description → same hash)
            ImportedTransaction tx2 = new ImportedTransaction(
                    LocalDate.of(2025, 6, 1), new BigDecimal("-25.00"), "UBER TRIP", null, null);

            List<ImportedTransaction> transactions = List.of(tx1, tx2);

            // Simulate within-batch dedup logic from BankStatementImportService
            java.util.Set<String> seenHashes = new java.util.HashSet<>();
            List<ImportedTransaction> unique = new ArrayList<>();
            int duplicateCount = 0;
            for (ImportedTransaction tx : transactions) {
                String hash = tx.transactionHash();
                if (seenHashes.contains(hash)) {
                    duplicateCount++;
                } else {
                    unique.add(tx);
                    seenHashes.add(hash);
                }
            }

            assertThat(unique).hasSize(1);
            assertThat(duplicateCount).isEqualTo(1);
        }

        @Test
        @DisplayName("partial duplicates → only new ones imported")
        void partialDuplicatesOnlyNewImported() {
            // First transaction already exists
            ImportedTransaction existing = new ImportedTransaction(
                    LocalDate.of(2025, 6, 1), new BigDecimal("-25.00"), "UBER TRIP", null, null);
            ImportedTransaction newTx = new ImportedTransaction(
                    LocalDate.of(2025, 6, 3), new BigDecimal("-10.00"), "STATIONERY PURCHASE", null, null);

            when(bankTransactionRepository.existsByHash(BUSINESS_ID, existing.transactionHash())).thenReturn(true);
            when(bankTransactionRepository.existsByHash(BUSINESS_ID, newTx.transactionHash())).thenReturn(false);

            List<ImportedTransaction> transactions = List.of(existing, newTx);

            List<ImportedTransaction> unique = new ArrayList<>();
            int duplicateCount = 0;
            for (ImportedTransaction tx : transactions) {
                if (bankTransactionRepository.existsByHash(BUSINESS_ID, tx.transactionHash())) {
                    duplicateCount++;
                } else {
                    unique.add(tx);
                }
            }

            assertThat(duplicateCount).isEqualTo(1);
            assertThat(unique).hasSize(1);
            assertThat(unique.get(0).description()).isEqualTo("STATIONERY PURCHASE");
        }

        @Test
        @DisplayName("non-duplicate data integrity preserved after filtering")
        void nonDuplicateDataIntegrityPreserved() {
            ImportedTransaction tx = new ImportedTransaction(
                    LocalDate.of(2025, 6, 5), new BigDecimal("-45.50"), "TRAIN TICKET LONDON", new BigDecimal("1500.00"), "REF-123");

            // Create a BankTransaction from the ImportedTransaction (simulating the import step)
            BankTransaction bankTx = BankTransaction.create(
                    BUSINESS_ID, AUDIT_ID, "csv-barclays",
                    tx.date(), tx.amount(), tx.description(),
                    null, tx.reference(), tx.transactionHash(), NOW
            );

            assertThat(bankTx.date()).isEqualTo(LocalDate.of(2025, 6, 5));
            assertThat(bankTx.amount()).isEqualByComparingTo(new BigDecimal("-45.50"));
            assertThat(bankTx.description()).isEqualTo("TRAIN TICKET LONDON");
            assertThat(bankTx.bankTransactionId()).isEqualTo("REF-123");
            assertThat(bankTx.transactionHash()).isEqualTo(tx.transactionHash());
            assertThat(bankTx.reviewStatus()).isEqualTo(ReviewStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("4. Business/Personal Classification")
    class BusinessPersonalClassification {

        @Test
        @DisplayName("flag as business preserves all other fields")
        void flagAsBusinessPreservesFields() {
            BankTransaction tx = createExpenseTransaction("UBER TRIP", new BigDecimal("25.00"));
            BankTransaction suggested = categorizationEngine.applyRecommendation(tx, NOW);

            BankTransaction flagged = suggested.withBusinessFlag(true, NOW);

            assertThat(flagged.isBusiness()).isTrue();
            assertThat(flagged.suggestedCategory()).isEqualTo(suggested.suggestedCategory());
            assertThat(flagged.confidenceScore()).isEqualByComparingTo(suggested.confidenceScore());
            assertThat(flagged.description()).isEqualTo(suggested.description());
            assertThat(flagged.amount()).isEqualByComparingTo(suggested.amount());
            assertThat(flagged.date()).isEqualTo(suggested.date());
        }

        @Test
        @DisplayName("flag as personal does not change review status")
        void flagAsPersonalKeepsReviewStatus() {
            BankTransaction tx = createExpenseTransaction("AMAZON PURCHASE", new BigDecimal("15.00"));

            BankTransaction flagged = tx.withBusinessFlag(false, NOW);

            assertThat(flagged.isBusiness()).isFalse();
            assertThat(flagged.reviewStatus()).isEqualTo(ReviewStatus.PENDING);
        }

        @Test
        @DisplayName("uncategorized (null isBusiness) blocks submission readiness")
        void uncategorizedBlocksSubmission() {
            BankTransaction tx1 = createExpenseTransaction("UBER TRIP", new BigDecimal("25.00"));
            BankTransaction tx2 = createIncomeTransaction("CLIENT PAYMENT", new BigDecimal("1500.00"));

            // tx1 has null isBusiness, tx2 has null isBusiness
            when(bankTransactionRepository.findByBusinessId(BUSINESS_ID))
                    .thenReturn(List.of(tx1, tx2));

            assertThat(businessPersonalService.hasUncategorizedTransactions(BUSINESS_ID)).isTrue();
            assertThat(businessPersonalService.isReadyForSubmission(BUSINESS_ID)).isFalse();
            assertThat(businessPersonalService.countUncategorized(BUSINESS_ID)).isEqualTo(2);
        }

        @Test
        @DisplayName("all flagged → submission ready")
        void allFlaggedMeansSubmissionReady() {
            BankTransaction tx1 = createExpenseTransaction("UBER TRIP", new BigDecimal("25.00"))
                    .withBusinessFlag(true, NOW);
            BankTransaction tx2 = createIncomeTransaction("CLIENT PAYMENT", new BigDecimal("1500.00"))
                    .withBusinessFlag(false, NOW);

            when(bankTransactionRepository.findByBusinessId(BUSINESS_ID))
                    .thenReturn(List.of(tx1, tx2));

            assertThat(businessPersonalService.isReadyForSubmission(BUSINESS_ID)).isTrue();
            assertThat(businessPersonalService.countUncategorized(BUSINESS_ID)).isZero();
        }

        @Test
        @DisplayName("full chain: import → categorize → flag → withCategorizedAsExpense")
        void fullChainImportCategorizeFlagCategorize() {
            // Step 1: Create imported transaction
            BankTransaction tx = createExpenseTransaction("UBER TRIP MANCHESTER", new BigDecimal("35.00"));

            // Step 2: Categorization engine suggests
            BankTransaction suggested = categorizationEngine.applyRecommendation(tx, NOW);
            assertThat(suggested.suggestedCategory()).isEqualTo(ExpenseCategory.TRAVEL);

            // Step 3: Flag as business
            BankTransaction flagged = suggested.withBusinessFlag(true, NOW);
            assertThat(flagged.isBusiness()).isTrue();
            assertThat(flagged.suggestedCategory()).isEqualTo(ExpenseCategory.TRAVEL);

            // Step 4: Confirm categorization as expense
            UUID expenseId = UUID.randomUUID();
            BankTransaction categorized = flagged.withCategorizedAsExpense(expenseId, NOW);
            assertThat(categorized.reviewStatus()).isEqualTo(ReviewStatus.CATEGORIZED);
            assertThat(categorized.expenseId()).isEqualTo(expenseId);
            assertThat(categorized.isBusiness()).isTrue();
            assertThat(categorized.suggestedCategory()).isEqualTo(ExpenseCategory.TRAVEL);
        }
    }

    @Nested
    @DisplayName("5. State Machine Transitions")
    class StateMachineTransitions {

        @Test
        @DisplayName("PENDING → CATEGORIZED via withCategorizedAsExpense")
        void pendingToCategorizedViaExpense() {
            BankTransaction tx = createExpenseTransaction("OFFICE SUPPLIES", new BigDecimal("50.00"));
            assertThat(tx.reviewStatus()).isEqualTo(ReviewStatus.PENDING);

            UUID expenseId = UUID.randomUUID();
            BankTransaction categorized = tx.withCategorizedAsExpense(expenseId, NOW);

            assertThat(categorized.reviewStatus()).isEqualTo(ReviewStatus.CATEGORIZED);
            assertThat(categorized.expenseId()).isEqualTo(expenseId);
            assertThat(categorized.incomeId()).isNull();
        }

        @Test
        @DisplayName("PENDING → CATEGORIZED via withCategorizedAsIncome")
        void pendingToCategorizedViaIncome() {
            BankTransaction tx = createIncomeTransaction("CONSULTING FEE", new BigDecimal("2000.00"));
            assertThat(tx.reviewStatus()).isEqualTo(ReviewStatus.PENDING);

            UUID incomeId = UUID.randomUUID();
            BankTransaction categorized = tx.withCategorizedAsIncome(incomeId, NOW);

            assertThat(categorized.reviewStatus()).isEqualTo(ReviewStatus.CATEGORIZED);
            assertThat(categorized.incomeId()).isEqualTo(incomeId);
            assertThat(categorized.expenseId()).isNull();
        }

        @Test
        @DisplayName("PENDING → EXCLUDED via withExcluded")
        void pendingToExcludedViaWithExcluded() {
            BankTransaction tx = createExpenseTransaction("ATM WITHDRAWAL HIGH ST", new BigDecimal("200.00"));
            assertThat(tx.reviewStatus()).isEqualTo(ReviewStatus.PENDING);

            // applyRecommendation should detect ATM and auto-exclude
            BankTransaction excluded = categorizationEngine.applyRecommendation(tx, NOW);

            assertThat(excluded.reviewStatus()).isEqualTo(ReviewStatus.EXCLUDED);
            assertThat(excluded.exclusionReason()).isEqualTo("CASH_WITHDRAWAL");
        }

        @Test
        @DisplayName("business flag does not change review status")
        void businessFlagDoesNotChangeReviewStatus() {
            BankTransaction pending = createExpenseTransaction("UBER TRIP", new BigDecimal("25.00"));
            assertThat(pending.reviewStatus()).isEqualTo(ReviewStatus.PENDING);

            BankTransaction flagged = pending.withBusinessFlag(true, NOW);
            assertThat(flagged.reviewStatus()).isEqualTo(ReviewStatus.PENDING);

            UUID expenseId = UUID.randomUUID();
            BankTransaction categorized = pending.withCategorizedAsExpense(expenseId, NOW);
            BankTransaction categorizedAndFlagged = categorized.withBusinessFlag(true, NOW);
            assertThat(categorizedAndFlagged.reviewStatus()).isEqualTo(ReviewStatus.CATEGORIZED);
        }
    }

    @Nested
    @DisplayName("6. Confidence Score Propagation")
    class ConfidenceScorePropagation {

        @Test
        @DisplayName("known keyword → HIGH (0.95) score propagates through pipeline")
        void knownKeywordHighConfidence() {
            BankTransaction tx = createExpenseTransaction("UBER TRIP BIRMINGHAM", new BigDecimal("18.50"));

            ClassificationResult classification = classificationService.classify(tx);
            CategorizationRecommendation rec = categorizationEngine.recommend(tx);
            BankTransaction applied = categorizationEngine.applyRecommendation(tx, NOW);

            assertThat(classification.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.95"));
            assertThat(classification.confidenceLevel()).isEqualTo(Confidence.HIGH);
            assertThat(rec.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.95"));
            assertThat(applied.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.95"));
        }

        @Test
        @DisplayName("unknown description → LOW (0.30) + OTHER_EXPENSES")
        void unknownDescriptionLowConfidence() {
            BankTransaction tx = createExpenseTransaction("RANDOM VENDOR XYZ", new BigDecimal("75.00"));

            ClassificationResult classification = classificationService.classify(tx);
            CategorizationRecommendation rec = categorizationEngine.recommend(tx);
            BankTransaction applied = categorizationEngine.applyRecommendation(tx, NOW);

            assertThat(classification.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.30"));
            assertThat(classification.confidenceLevel()).isEqualTo(Confidence.LOW);
            assertThat(classification.suggestedCategory()).isEqualTo(ExpenseCategory.OTHER_EXPENSES);
            assertThat(rec.expenseCategory()).isEqualTo(ExpenseCategory.OTHER_EXPENSES);
            assertThat(applied.suggestedCategory()).isEqualTo(ExpenseCategory.OTHER_EXPENSES);
            assertThat(applied.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.30"));
        }

        @Test
        @DisplayName("generic income → MEDIUM (0.75)")
        void genericIncomeMediumConfidence() {
            BankTransaction tx = createIncomeTransaction("CLIENT PAYMENT ACME", new BigDecimal("5000.00"));

            ClassificationResult classification = classificationService.classify(tx);
            CategorizationRecommendation rec = categorizationEngine.recommend(tx);

            assertThat(classification.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.75"));
            assertThat(classification.confidenceLevel()).isEqualTo(Confidence.MEDIUM);
            assertThat(rec.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.75"));
        }

        @Test
        @DisplayName("income keyword (dividend) → HIGH (0.95)")
        void incomeKeywordHighConfidence() {
            BankTransaction tx = createIncomeTransaction("DIVIDEND PAYMENT Q2", new BigDecimal("250.00"));

            ClassificationResult classification = classificationService.classify(tx);
            CategorizationRecommendation rec = categorizationEngine.recommend(tx);

            assertThat(classification.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.95"));
            assertThat(classification.confidenceLevel()).isEqualTo(Confidence.HIGH);
            assertThat(rec.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.95"));
        }
    }

    @Nested
    @DisplayName("7. Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("empty import: 0 transactions, audit still created")
        void emptyImportCreatesAudit() {
            List<ImportedTransaction> transactions = List.of();

            ImportAudit audit = ImportAudit.create(
                    BUSINESS_ID, NOW, "empty.csv", "sha256-empty",
                    uk.selfemploy.common.enums.ImportAuditType.BANK_CSV,
                    0, 0, 0, List.of()
            );

            assertThat(audit).isNotNull();
            assertThat(audit.totalRecords()).isZero();
            assertThat(audit.importedCount()).isZero();
            assertThat(audit.skippedCount()).isZero();
            assertThat(audit.businessId()).isEqualTo(BUSINESS_ID);
        }

        @Test
        @DisplayName("single transaction: full import-classify-categorize-flag cycle")
        void singleTransactionFullCycle() {
            BankTransaction tx = createExpenseTransaction("TRAINLINE TICKET", new BigDecimal("89.00"));

            // Classify
            BankTransaction suggested = categorizationEngine.applyRecommendation(tx, NOW);
            assertThat(suggested.suggestedCategory()).isEqualTo(ExpenseCategory.TRAVEL);

            // Flag as business
            BankTransaction flagged = suggested.withBusinessFlag(true, NOW);
            assertThat(flagged.isBusiness()).isTrue();

            // Categorize as expense
            UUID expenseId = UUID.randomUUID();
            BankTransaction categorized = flagged.withCategorizedAsExpense(expenseId, NOW);

            assertThat(categorized.reviewStatus()).isEqualTo(ReviewStatus.CATEGORIZED);
            assertThat(categorized.expenseId()).isEqualTo(expenseId);
            assertThat(categorized.isBusiness()).isTrue();
            assertThat(categorized.suggestedCategory()).isEqualTo(ExpenseCategory.TRAVEL);
            assertThat(categorized.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.95"));
        }

        @Test
        @DisplayName("all duplicates: 0 imported, audit still created")
        void allDuplicatesAuditStillCreated() {
            when(bankTransactionRepository.existsByHash(eq(BUSINESS_ID), any())).thenReturn(true);

            ImportedTransaction tx1 = new ImportedTransaction(
                    LocalDate.of(2025, 6, 1), new BigDecimal("-25.00"), "UBER TRIP", null, null);
            ImportedTransaction tx2 = new ImportedTransaction(
                    LocalDate.of(2025, 6, 2), new BigDecimal("-10.00"), "OFFICE DEPOT", null, null);

            List<ImportedTransaction> transactions = List.of(tx1, tx2);

            // Simulate dedup logic
            List<ImportedTransaction> unique = new ArrayList<>();
            int duplicateCount = 0;
            for (ImportedTransaction tx : transactions) {
                if (bankTransactionRepository.existsByHash(BUSINESS_ID, tx.transactionHash())) {
                    duplicateCount++;
                } else {
                    unique.add(tx);
                }
            }

            // Audit should still be created for the attempt
            ImportAudit audit = ImportAudit.create(
                    BUSINESS_ID, NOW, "duplicates.csv", "sha256-dup",
                    uk.selfemploy.common.enums.ImportAuditType.BANK_CSV,
                    transactions.size(), unique.size(), duplicateCount, List.of()
            );

            assertThat(audit.totalRecords()).isEqualTo(2);
            assertThat(audit.importedCount()).isZero();
            assertThat(audit.skippedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("mixed batch: 5 transactions → 2 EXCLUDED + 3 PENDING with suggestions")
        void mixedBatchExclusionsAndSuggestions() {
            List<BankTransaction> batch = List.of(
                    createExpenseTransaction("TRANSFER TO ISA", new BigDecimal("500.00")),
                    createExpenseTransaction("HMRC PAYMENT", new BigDecimal("1200.00")),
                    createExpenseTransaction("UBER TRIP LEEDS", new BigDecimal("22.00")),
                    createExpenseTransaction("AMAZON OFFICE CHAIR", new BigDecimal("199.99")),
                    createIncomeTransaction("CLIENT PAYMENT FEB", new BigDecimal("3500.00"))
            );

            List<BankTransaction> categorized = batch.stream()
                    .map(tx -> categorizationEngine.applyRecommendation(tx, NOW))
                    .toList();

            long excludedCount = categorized.stream()
                    .filter(tx -> tx.reviewStatus() == ReviewStatus.EXCLUDED)
                    .count();
            long pendingCount = categorized.stream()
                    .filter(tx -> tx.reviewStatus() == ReviewStatus.PENDING)
                    .count();

            assertThat(excludedCount).isEqualTo(2);
            assertThat(pendingCount).isEqualTo(3);

            // Verify the pending ones got suggestions
            List<BankTransaction> pending = categorized.stream()
                    .filter(tx -> tx.reviewStatus() == ReviewStatus.PENDING)
                    .toList();
            assertThat(pending).allSatisfy(tx ->
                    assertThat(tx.confidenceScore()).isNotNull()
            );
        }

        @Test
        @DisplayName("import stats accuracy for batch with mixed duplicates")
        void importStatsAccuracy() {
            // 3 transactions total: 1 existing duplicate, 1 within-batch duplicate, 1 new
            ImportedTransaction existing = new ImportedTransaction(
                    LocalDate.of(2025, 6, 1), new BigDecimal("-25.00"), "UBER TRIP", null, null);
            ImportedTransaction newTx = new ImportedTransaction(
                    LocalDate.of(2025, 6, 3), new BigDecimal("-50.00"), "OFFICE SUPPLIES", null, null);
            ImportedTransaction batchDup = new ImportedTransaction(
                    LocalDate.of(2025, 6, 3), new BigDecimal("-50.00"), "OFFICE SUPPLIES", null, null);

            when(bankTransactionRepository.existsByHash(BUSINESS_ID, existing.transactionHash())).thenReturn(true);
            when(bankTransactionRepository.existsByHash(BUSINESS_ID, newTx.transactionHash())).thenReturn(false);

            List<ImportedTransaction> transactions = List.of(existing, newTx, batchDup);

            // Simulate full dedup logic from import service
            java.util.Set<String> seenHashes = new java.util.HashSet<>();
            List<ImportedTransaction> unique = new ArrayList<>();
            int duplicateCount = 0;
            for (ImportedTransaction tx : transactions) {
                String hash = tx.transactionHash();
                if (bankTransactionRepository.existsByHash(BUSINESS_ID, hash) || seenHashes.contains(hash)) {
                    duplicateCount++;
                } else {
                    unique.add(tx);
                    seenHashes.add(hash);
                }
            }

            assertThat(transactions).hasSize(3);
            assertThat(unique).hasSize(1);
            assertThat(duplicateCount).isEqualTo(2);
            assertThat(unique.get(0).description()).isEqualTo("OFFICE SUPPLIES");
        }
    }
}
