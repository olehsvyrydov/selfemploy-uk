package uk.selfemploy.core.bankimport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.ReviewStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CategorizationEngine")
class CategorizationEngineTest {

    private CategorizationEngine engine;

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final UUID AUDIT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2025-06-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        DescriptionCategorizer categorizer = new DescriptionCategorizer();
        TransactionClassificationService classificationService = new TransactionClassificationService(categorizer);
        ExclusionRulesEngine exclusionEngine = new ExclusionRulesEngine();
        engine = new CategorizationEngine(classificationService, exclusionEngine);
    }

    private BankTransaction createTransaction(String description, BigDecimal amount) {
        return BankTransaction.create(
            BUSINESS_ID, AUDIT_ID, "csv-barclays",
            LocalDate.of(2025, 6, 15), amount, description,
            "1234", null, "hash-" + description.hashCode(), NOW
        );
    }

    @Nested
    @DisplayName("Full categorization flow")
    class FullFlow {

        @Test
        @DisplayName("categorizes business expense with SA103 box mapping")
        void categorizeBusinessExpense() {
            BankTransaction tx = createTransaction("UBER TRIP TO CLIENT", new BigDecimal("-25.00"));

            CategorizationRecommendation rec = engine.recommend(tx);

            assertThat(rec.isIncome()).isFalse();
            assertThat(rec.shouldExclude()).isFalse();
            assertThat(rec.expenseCategory()).isEqualTo(ExpenseCategory.TRAVEL);
            assertThat(rec.sa103Box()).isNotNull();
            assertThat(rec.confidenceScore()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("categorizes income transaction")
        void categorizeIncome() {
            BankTransaction tx = createTransaction("CLIENT PAYMENT LTD", new BigDecimal("5000.00"));

            CategorizationRecommendation rec = engine.recommend(tx);

            assertThat(rec.isIncome()).isTrue();
            assertThat(rec.shouldExclude()).isFalse();
            assertThat(rec.expenseCategory()).isNull();
        }

        @Test
        @DisplayName("recommends exclusion for transfer")
        void recommendExclusionForTransfer() {
            BankTransaction tx = createTransaction("TFR TO SAVINGS", new BigDecimal("-1000.00"));

            CategorizationRecommendation rec = engine.recommend(tx);

            assertThat(rec.shouldExclude()).isTrue();
            assertThat(rec.exclusionReason()).isEqualTo("TRANSFER");
        }

        @Test
        @DisplayName("recommends exclusion for HMRC payment")
        void recommendExclusionForHmrc() {
            BankTransaction tx = createTransaction("HMRC SELF ASSESSMENT", new BigDecimal("-5000.00"));

            CategorizationRecommendation rec = engine.recommend(tx);

            assertThat(rec.shouldExclude()).isTrue();
            assertThat(rec.exclusionReason()).isEqualTo("TAX_PAYMENT");
        }
    }

    @Nested
    @DisplayName("SA103 box mapping")
    class Sa103BoxMapping {

        @Test
        @DisplayName("OFFICE_COSTS maps to Box 23")
        void officeCostsBox23() {
            BankTransaction tx = createTransaction("AMAZON OFFICE SUPPLIES", new BigDecimal("-30.00"));

            CategorizationRecommendation rec = engine.recommend(tx);

            assertThat(rec.expenseCategory()).isEqualTo(ExpenseCategory.OFFICE_COSTS);
            assertThat(rec.sa103Box()).isEqualTo("Box 23");
        }

        @Test
        @DisplayName("TRAVEL maps to Box 20")
        void travelBox20() {
            BankTransaction tx = createTransaction("TRAINLINE TICKET", new BigDecimal("-45.00"));

            CategorizationRecommendation rec = engine.recommend(tx);

            assertThat(rec.expenseCategory()).isEqualTo(ExpenseCategory.TRAVEL);
            assertThat(rec.sa103Box()).isEqualTo("Box 20");
        }

        @Test
        @DisplayName("PREMISES maps to Box 21")
        void premisesBox21() {
            BankTransaction tx = createTransaction("BRITISH GAS BILL", new BigDecimal("-150.00"));

            CategorizationRecommendation rec = engine.recommend(tx);

            assertThat(rec.expenseCategory()).isEqualTo(ExpenseCategory.PREMISES);
            assertThat(rec.sa103Box()).isEqualTo("Box 21");
        }

        @Test
        @DisplayName("STAFF_COSTS maps to Box 19")
        void staffCostsBox19() {
            BankTransaction tx = createTransaction("PAYROLL PROCESSING", new BigDecimal("-2000.00"));

            CategorizationRecommendation rec = engine.recommend(tx);

            assertThat(rec.expenseCategory()).isEqualTo(ExpenseCategory.STAFF_COSTS);
            assertThat(rec.sa103Box()).isEqualTo("Box 19");
        }

        @Test
        @DisplayName("PROFESSIONAL_FEES maps to Box 28")
        void professionalFeesBox28() {
            BankTransaction tx = createTransaction("ACCOUNTANT FEE", new BigDecimal("-500.00"));

            CategorizationRecommendation rec = engine.recommend(tx);

            assertThat(rec.expenseCategory()).isEqualTo(ExpenseCategory.PROFESSIONAL_FEES);
            assertThat(rec.sa103Box()).isEqualTo("Box 28");
        }

        @Test
        @DisplayName("OTHER_EXPENSES maps to Box 30")
        void otherExpensesBox30() {
            BankTransaction tx = createTransaction("RANDOM VENDOR", new BigDecimal("-15.00"));

            CategorizationRecommendation rec = engine.recommend(tx);

            assertThat(rec.expenseCategory()).isEqualTo(ExpenseCategory.OTHER_EXPENSES);
            assertThat(rec.sa103Box()).isEqualTo("Box 30");
        }

        @Test
        @DisplayName("income has null box")
        void incomeNullBox() {
            BankTransaction tx = createTransaction("CLIENT PAYMENT", new BigDecimal("3000.00"));

            CategorizationRecommendation rec = engine.recommend(tx);

            assertThat(rec.sa103Box()).isNull();
        }

        @Test
        @DisplayName("excluded transaction has null box")
        void excludedNullBox() {
            BankTransaction tx = createTransaction("TRANSFER OUT", new BigDecimal("-500.00"));

            CategorizationRecommendation rec = engine.recommend(tx);

            assertThat(rec.sa103Box()).isNull();
        }
    }

    @Nested
    @DisplayName("applyRecommendation()")
    class ApplyRecommendation {

        @Test
        @DisplayName("applies expense categorization to transaction")
        void appliesExpenseCategorization() {
            BankTransaction tx = createTransaction("UBER LONDON", new BigDecimal("-20.00"));
            Instant later = NOW.plusSeconds(60);

            BankTransaction updated = engine.applyRecommendation(tx, later);

            assertThat(updated.suggestedCategory()).isEqualTo(ExpenseCategory.TRAVEL);
            assertThat(updated.confidenceScore()).isNotNull();
        }

        @Test
        @DisplayName("applies exclusion to transaction")
        void appliesExclusion() {
            BankTransaction tx = createTransaction("ATM WITHDRAWAL", new BigDecimal("-200.00"));
            Instant later = NOW.plusSeconds(60);

            BankTransaction updated = engine.applyRecommendation(tx, later);

            assertThat(updated.reviewStatus()).isEqualTo(ReviewStatus.EXCLUDED);
            assertThat(updated.exclusionReason()).isEqualTo("CASH_WITHDRAWAL");
        }
    }
}
