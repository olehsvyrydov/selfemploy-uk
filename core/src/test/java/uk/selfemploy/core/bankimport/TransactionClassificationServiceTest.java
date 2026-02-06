package uk.selfemploy.core.bankimport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionClassificationService")
class TransactionClassificationServiceTest {

    private TransactionClassificationService service;
    private DescriptionCategorizer categorizer;

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final UUID AUDIT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2025-06-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        categorizer = new DescriptionCategorizer();
        service = new TransactionClassificationService(categorizer);
    }

    private BankTransaction createTransaction(String description, BigDecimal amount) {
        return BankTransaction.create(
            BUSINESS_ID, AUDIT_ID, "csv-barclays",
            LocalDate.of(2025, 6, 15), amount, description,
            "1234", null, "hash-" + description.hashCode(), NOW
        );
    }

    @Nested
    @DisplayName("Direction classification")
    class DirectionClassification {

        @Test
        @DisplayName("positive amount classified as income")
        void positiveAmountIsIncome() {
            BankTransaction tx = createTransaction("CLIENT PAYMENT", new BigDecimal("1500.00"));

            ClassificationResult result = service.classify(tx);

            assertThat(result.isIncome()).isTrue();
        }

        @Test
        @DisplayName("negative amount classified as expense")
        void negativeAmountIsExpense() {
            BankTransaction tx = createTransaction("OFFICE SUPPLIES", new BigDecimal("-45.00"));

            ClassificationResult result = service.classify(tx);

            assertThat(result.isIncome()).isFalse();
        }

        @Test
        @DisplayName("zero amount classified as expense")
        void zeroAmountIsExpense() {
            BankTransaction tx = createTransaction("ZERO ENTRY", BigDecimal.ZERO);

            ClassificationResult result = service.classify(tx);

            assertThat(result.isIncome()).isFalse();
        }
    }

    @Nested
    @DisplayName("Expense category suggestions")
    class ExpenseCategorySuggestions {

        @Test
        @DisplayName("office keyword triggers OFFICE_COSTS with high confidence")
        void officeKeywordHighConfidence() {
            BankTransaction tx = createTransaction("AMAZON OFFICE SUPPLIES", new BigDecimal("-25.99"));

            ClassificationResult result = service.classify(tx);

            assertThat(result.suggestedCategory()).isEqualTo(ExpenseCategory.OFFICE_COSTS);
            assertThat(result.isHighConfidence()).isTrue();
        }

        @Test
        @DisplayName("travel keyword triggers TRAVEL category")
        void travelKeyword() {
            BankTransaction tx = createTransaction("UBER TRIP LONDON", new BigDecimal("-15.00"));

            ClassificationResult result = service.classify(tx);

            assertThat(result.suggestedCategory()).isEqualTo(ExpenseCategory.TRAVEL);
            assertThat(result.confidenceLevel()).isEqualTo(Confidence.HIGH);
        }

        @Test
        @DisplayName("fuel keyword triggers TRAVEL_MILEAGE category")
        void fuelKeyword() {
            BankTransaction tx = createTransaction("SHELL PETROL STATION", new BigDecimal("-55.00"));

            ClassificationResult result = service.classify(tx);

            assertThat(result.suggestedCategory()).isEqualTo(ExpenseCategory.TRAVEL_MILEAGE);
        }

        @Test
        @DisplayName("accountant keyword triggers PROFESSIONAL_FEES category")
        void accountantKeyword() {
            BankTransaction tx = createTransaction("MY ACCOUNTANT LTD", new BigDecimal("-500.00"));

            ClassificationResult result = service.classify(tx);

            assertThat(result.suggestedCategory()).isEqualTo(ExpenseCategory.PROFESSIONAL_FEES);
        }

        @Test
        @DisplayName("no keyword match defaults to OTHER_EXPENSES with low confidence")
        void noMatchDefaultsToOther() {
            BankTransaction tx = createTransaction("RANDOM VENDOR XYZ", new BigDecimal("-10.00"));

            ClassificationResult result = service.classify(tx);

            assertThat(result.suggestedCategory()).isEqualTo(ExpenseCategory.OTHER_EXPENSES);
            assertThat(result.confidenceLevel()).isEqualTo(Confidence.LOW);
            assertThat(result.requiresManualReview()).isTrue();
        }
    }

    @Nested
    @DisplayName("Income classification")
    class IncomeClassification {

        @Test
        @DisplayName("generic income gets null expense category")
        void genericIncomeNullCategory() {
            BankTransaction tx = createTransaction("CLIENT PAYMENT", new BigDecimal("5000.00"));

            ClassificationResult result = service.classify(tx);

            assertThat(result.isIncome()).isTrue();
            // Income doesn't use ExpenseCategory
            assertThat(result.suggestedCategory()).isNull();
        }

        @Test
        @DisplayName("income classified with medium confidence by default")
        void incomeDefaultMediumConfidence() {
            BankTransaction tx = createTransaction("SOME CLIENT PAYMENT", new BigDecimal("1200.00"));

            ClassificationResult result = service.classify(tx);

            assertThat(result.isSuggestionWorthy()).isTrue();
        }
    }

    @Nested
    @DisplayName("Confidence scores")
    class ConfidenceScores {

        @Test
        @DisplayName("HIGH confidence maps to score > 0.90")
        void highConfidenceScore() {
            BankTransaction tx = createTransaction("AMAZON PURCHASE", new BigDecimal("-30.00"));

            ClassificationResult result = service.classify(tx);

            assertThat(result.confidenceScore()).isGreaterThan(ClassificationResult.HIGH_THRESHOLD);
            assertThat(result.isHighConfidence()).isTrue();
        }

        @Test
        @DisplayName("LOW confidence maps to score < 0.60")
        void lowConfidenceScore() {
            BankTransaction tx = createTransaction("UNKNOWN VENDOR XYZ", new BigDecimal("-30.00"));

            ClassificationResult result = service.classify(tx);

            assertThat(result.confidenceScore()).isLessThan(ClassificationResult.MEDIUM_THRESHOLD);
            assertThat(result.requiresManualReview()).isTrue();
        }
    }

    @Nested
    @DisplayName("classifyAndApply()")
    class ClassifyAndApply {

        @Test
        @DisplayName("applies suggestion to BankTransaction")
        void appliesSuggestionToTransaction() {
            BankTransaction tx = createTransaction("UBER LONDON", new BigDecimal("-12.00"));

            BankTransaction classified = service.classifyAndApply(tx, NOW.plusSeconds(60));

            assertThat(classified.suggestedCategory()).isEqualTo(ExpenseCategory.TRAVEL);
            assertThat(classified.confidenceScore()).isNotNull();
            assertThat(classified.confidenceScore()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("preserves original transaction fields")
        void preservesOriginalFields() {
            BankTransaction tx = createTransaction("SHELL FUEL", new BigDecimal("-40.00"));

            BankTransaction classified = service.classifyAndApply(tx, NOW.plusSeconds(60));

            assertThat(classified.id()).isEqualTo(tx.id());
            assertThat(classified.businessId()).isEqualTo(tx.businessId());
            assertThat(classified.description()).isEqualTo(tx.description());
            assertThat(classified.amount()).isEqualByComparingTo(tx.amount());
        }
    }
}
