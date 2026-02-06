package uk.selfemploy.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.ReviewStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BankTransaction")
class BankTransactionTest {

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final UUID IMPORT_AUDIT_ID = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2025, 6, 15);
    private static final BigDecimal AMOUNT = new BigDecimal("150.00");
    private static final String DESCRIPTION = "ACME CORP PAYMENT";
    private static final String ACCOUNT_LAST_FOUR = "1234";
    private static final String BANK_TX_ID = "TXN-001";
    private static final String TX_HASH = "abc123def456";
    private static final Instant NOW = Instant.parse("2025-06-15T10:00:00Z");

    private BankTransaction createPending() {
        return BankTransaction.create(
            BUSINESS_ID, IMPORT_AUDIT_ID, "csv-barclays",
            DATE, AMOUNT, DESCRIPTION, ACCOUNT_LAST_FOUR,
            BANK_TX_ID, TX_HASH, NOW
        );
    }

    private BankTransaction createPendingWithAmount(BigDecimal amount) {
        return BankTransaction.create(
            BUSINESS_ID, IMPORT_AUDIT_ID, "csv-barclays",
            DATE, amount, DESCRIPTION, ACCOUNT_LAST_FOUR,
            BANK_TX_ID, TX_HASH, NOW
        );
    }

    @Nested
    @DisplayName("Factory method create()")
    class Create {

        @Test
        @DisplayName("creates pending transaction with generated ID")
        void createsPendingTransaction() {
            BankTransaction tx = createPending();

            assertThat(tx.id()).isNotNull();
            assertThat(tx.businessId()).isEqualTo(BUSINESS_ID);
            assertThat(tx.importAuditId()).isEqualTo(IMPORT_AUDIT_ID);
            assertThat(tx.sourceFormatId()).isEqualTo("csv-barclays");
            assertThat(tx.date()).isEqualTo(DATE);
            assertThat(tx.amount()).isEqualByComparingTo(AMOUNT);
            assertThat(tx.description()).isEqualTo(DESCRIPTION);
            assertThat(tx.accountLastFour()).isEqualTo(ACCOUNT_LAST_FOUR);
            assertThat(tx.bankTransactionId()).isEqualTo(BANK_TX_ID);
            assertThat(tx.transactionHash()).isEqualTo(TX_HASH);
            assertThat(tx.reviewStatus()).isEqualTo(ReviewStatus.PENDING);
            assertThat(tx.createdAt()).isEqualTo(NOW);
            assertThat(tx.updatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("starts with null categorization fields")
        void startsWithNullCategorization() {
            BankTransaction tx = createPending();

            assertThat(tx.incomeId()).isNull();
            assertThat(tx.expenseId()).isNull();
            assertThat(tx.exclusionReason()).isNull();
            assertThat(tx.isBusiness()).isNull();
            assertThat(tx.confidenceScore()).isNull();
            assertThat(tx.suggestedCategory()).isNull();
        }

        @Test
        @DisplayName("starts with null soft-delete fields")
        void startsWithNullSoftDelete() {
            BankTransaction tx = createPending();

            assertThat(tx.deletedAt()).isNull();
            assertThat(tx.deletedBy()).isNull();
            assertThat(tx.deletionReason()).isNull();
            assertThat(tx.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("allows null sourceFormatId for unknown formats")
        void allowsNullSourceFormatId() {
            BankTransaction tx = BankTransaction.create(
                BUSINESS_ID, IMPORT_AUDIT_ID, null,
                DATE, AMOUNT, DESCRIPTION, ACCOUNT_LAST_FOUR,
                BANK_TX_ID, TX_HASH, NOW
            );

            assertThat(tx.sourceFormatId()).isNull();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("rejects null businessId")
        void rejectsNullBusinessId() {
            assertThatThrownBy(() -> BankTransaction.create(
                null, IMPORT_AUDIT_ID, "csv-barclays",
                DATE, AMOUNT, DESCRIPTION, ACCOUNT_LAST_FOUR,
                BANK_TX_ID, TX_HASH, NOW
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("businessId");
        }

        @Test
        @DisplayName("rejects null importAuditId")
        void rejectsNullImportAuditId() {
            assertThatThrownBy(() -> BankTransaction.create(
                BUSINESS_ID, null, "csv-barclays",
                DATE, AMOUNT, DESCRIPTION, ACCOUNT_LAST_FOUR,
                BANK_TX_ID, TX_HASH, NOW
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("importAuditId");
        }

        @Test
        @DisplayName("rejects null date")
        void rejectsNullDate() {
            assertThatThrownBy(() -> BankTransaction.create(
                BUSINESS_ID, IMPORT_AUDIT_ID, "csv-barclays",
                null, AMOUNT, DESCRIPTION, ACCOUNT_LAST_FOUR,
                BANK_TX_ID, TX_HASH, NOW
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("date");
        }

        @Test
        @DisplayName("rejects null amount")
        void rejectsNullAmount() {
            assertThatThrownBy(() -> BankTransaction.create(
                BUSINESS_ID, IMPORT_AUDIT_ID, "csv-barclays",
                DATE, null, DESCRIPTION, ACCOUNT_LAST_FOUR,
                BANK_TX_ID, TX_HASH, NOW
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("rejects null description")
        void rejectsNullDescription() {
            assertThatThrownBy(() -> BankTransaction.create(
                BUSINESS_ID, IMPORT_AUDIT_ID, "csv-barclays",
                DATE, AMOUNT, null, ACCOUNT_LAST_FOUR,
                BANK_TX_ID, TX_HASH, NOW
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("description");
        }

        @Test
        @DisplayName("rejects blank description")
        void rejectsBlankDescription() {
            assertThatThrownBy(() -> BankTransaction.create(
                BUSINESS_ID, IMPORT_AUDIT_ID, "csv-barclays",
                DATE, AMOUNT, "  ", ACCOUNT_LAST_FOUR,
                BANK_TX_ID, TX_HASH, NOW
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("description");
        }

        @Test
        @DisplayName("rejects null transactionHash")
        void rejectsNullTransactionHash() {
            assertThatThrownBy(() -> BankTransaction.create(
                BUSINESS_ID, IMPORT_AUDIT_ID, "csv-barclays",
                DATE, AMOUNT, DESCRIPTION, ACCOUNT_LAST_FOUR,
                BANK_TX_ID, null, NOW
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("transactionHash");
        }

        @Test
        @DisplayName("rejects null createdAt")
        void rejectsNullCreatedAt() {
            assertThatThrownBy(() -> BankTransaction.create(
                BUSINESS_ID, IMPORT_AUDIT_ID, "csv-barclays",
                DATE, AMOUNT, DESCRIPTION, ACCOUNT_LAST_FOUR,
                BANK_TX_ID, TX_HASH, null
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("createdAt");
        }
    }

    @Nested
    @DisplayName("Income/Expense direction classification")
    class DirectionClassification {

        @Test
        @DisplayName("positive amount is income")
        void positiveAmountIsIncome() {
            BankTransaction tx = createPendingWithAmount(new BigDecimal("100.00"));

            assertThat(tx.isIncome()).isTrue();
            assertThat(tx.isExpense()).isFalse();
        }

        @Test
        @DisplayName("negative amount is expense")
        void negativeAmountIsExpense() {
            BankTransaction tx = createPendingWithAmount(new BigDecimal("-50.00"));

            assertThat(tx.isIncome()).isFalse();
            assertThat(tx.isExpense()).isTrue();
        }

        @Test
        @DisplayName("zero amount is neither income nor expense")
        void zeroIsNeither() {
            BankTransaction tx = createPendingWithAmount(BigDecimal.ZERO);

            assertThat(tx.isIncome()).isFalse();
            assertThat(tx.isExpense()).isFalse();
        }

        @Test
        @DisplayName("absoluteAmount returns positive value for negative amounts")
        void absoluteAmountForNegative() {
            BankTransaction tx = createPendingWithAmount(new BigDecimal("-75.50"));

            assertThat(tx.absoluteAmount()).isEqualByComparingTo(new BigDecimal("75.50"));
        }
    }

    @Nested
    @DisplayName("State transitions")
    class StateTransitions {

        @Test
        @DisplayName("categorize as income links incomeId")
        void categorizeAsIncome() {
            BankTransaction tx = createPending();
            UUID incomeId = UUID.randomUUID();
            Instant later = NOW.plusSeconds(60);

            BankTransaction categorized = tx.withCategorizedAsIncome(incomeId, later);

            assertThat(categorized.reviewStatus()).isEqualTo(ReviewStatus.CATEGORIZED);
            assertThat(categorized.incomeId()).isEqualTo(incomeId);
            assertThat(categorized.expenseId()).isNull();
            assertThat(categorized.updatedAt()).isEqualTo(later);
            assertThat(categorized.isReviewed()).isTrue();
        }

        @Test
        @DisplayName("categorize as expense links expenseId")
        void categorizeAsExpense() {
            BankTransaction tx = createPendingWithAmount(new BigDecimal("-30.00"));
            UUID expenseId = UUID.randomUUID();
            Instant later = NOW.plusSeconds(60);

            BankTransaction categorized = tx.withCategorizedAsExpense(expenseId, later);

            assertThat(categorized.reviewStatus()).isEqualTo(ReviewStatus.CATEGORIZED);
            assertThat(categorized.expenseId()).isEqualTo(expenseId);
            assertThat(categorized.incomeId()).isNull();
            assertThat(categorized.updatedAt()).isEqualTo(later);
        }

        @Test
        @DisplayName("exclude with reason sets status and reason")
        void excludeWithReason() {
            BankTransaction tx = createPending();
            Instant later = NOW.plusSeconds(60);

            BankTransaction excluded = tx.withExcluded("TRANSFER", later);

            assertThat(excluded.reviewStatus()).isEqualTo(ReviewStatus.EXCLUDED);
            assertThat(excluded.exclusionReason()).isEqualTo("TRANSFER");
            assertThat(excluded.incomeId()).isNull();
            assertThat(excluded.expenseId()).isNull();
            assertThat(excluded.updatedAt()).isEqualTo(later);
        }

        @Test
        @DisplayName("set business flag")
        void setBusinessFlag() {
            BankTransaction tx = createPending();
            Instant later = NOW.plusSeconds(60);

            BankTransaction business = tx.withBusinessFlag(true, later);

            assertThat(business.isBusiness()).isTrue();
            assertThat(business.updatedAt()).isEqualTo(later);
        }

        @Test
        @DisplayName("set personal flag")
        void setPersonalFlag() {
            BankTransaction tx = createPending();
            Instant later = NOW.plusSeconds(60);

            BankTransaction personal = tx.withBusinessFlag(false, later);

            assertThat(personal.isBusiness()).isFalse();
        }

        @Test
        @DisplayName("null isBusiness means uncategorized")
        void nullBusinessMeansUncategorized() {
            BankTransaction tx = createPending();

            assertThat(tx.isBusiness()).isNull();
        }

        @Test
        @DisplayName("add classification suggestion")
        void addSuggestion() {
            BankTransaction tx = createPending();
            Instant later = NOW.plusSeconds(60);

            BankTransaction suggested = tx.withSuggestion(
                ExpenseCategory.TRAVEL, new BigDecimal("0.85"), later
            );

            assertThat(suggested.suggestedCategory()).isEqualTo(ExpenseCategory.TRAVEL);
            assertThat(suggested.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.85"));
            assertThat(suggested.updatedAt()).isEqualTo(later);
        }
    }

    @Nested
    @DisplayName("Soft delete")
    class SoftDelete {

        @Test
        @DisplayName("soft delete sets all fields")
        void softDeleteSetsFields() {
            BankTransaction tx = createPending();
            Instant deleteTime = NOW.plusSeconds(300);

            BankTransaction deleted = tx.withSoftDelete(deleteTime, "system", "undo import");

            assertThat(deleted.isDeleted()).isTrue();
            assertThat(deleted.deletedAt()).isEqualTo(deleteTime);
            assertThat(deleted.deletedBy()).isEqualTo("system");
            assertThat(deleted.deletionReason()).isEqualTo("undo import");
        }

        @Test
        @DisplayName("non-deleted transaction reports isDeleted false")
        void nonDeletedReportsFalse() {
            BankTransaction tx = createPending();

            assertThat(tx.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("state transitions return new instances")
        void stateTransitionsReturnNewInstances() {
            BankTransaction original = createPending();
            Instant later = NOW.plusSeconds(60);

            BankTransaction categorized = original.withCategorizedAsIncome(UUID.randomUUID(), later);

            assertThat(categorized).isNotSameAs(original);
            assertThat(original.reviewStatus()).isEqualTo(ReviewStatus.PENDING);
            assertThat(categorized.reviewStatus()).isEqualTo(ReviewStatus.CATEGORIZED);
        }

        @Test
        @DisplayName("original timestamp preserved through transitions")
        void createdAtPreserved() {
            BankTransaction original = createPending();
            Instant later = NOW.plusSeconds(60);

            BankTransaction categorized = original.withCategorizedAsIncome(UUID.randomUUID(), later);

            assertThat(categorized.createdAt()).isEqualTo(NOW);
        }
    }
}
