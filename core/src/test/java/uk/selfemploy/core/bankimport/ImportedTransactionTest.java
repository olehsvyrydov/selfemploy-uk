package uk.selfemploy.core.bankimport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ImportedTransaction DTO.
 */
@DisplayName("ImportedTransaction Tests")
class ImportedTransactionTest {

    private static final LocalDate VALID_DATE = LocalDate.of(2025, 6, 15);
    private static final BigDecimal VALID_AMOUNT = new BigDecimal("500.00");
    private static final String VALID_DESCRIPTION = "AMAZON MARKETPLACE";

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("should create income transaction with positive amount")
        void shouldCreateIncomeWithPositiveAmount() {
            ImportedTransaction tx = new ImportedTransaction(
                VALID_DATE,
                new BigDecimal("100.00"),
                VALID_DESCRIPTION,
                null, // balance
                "REF123" // reference
            );

            assertThat(tx.date()).isEqualTo(VALID_DATE);
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(tx.description()).isEqualTo(VALID_DESCRIPTION);
            assertThat(tx.isIncome()).isTrue();
            assertThat(tx.isExpense()).isFalse();
        }

        @Test
        @DisplayName("should create expense transaction with negative amount")
        void shouldCreateExpenseWithNegativeAmount() {
            ImportedTransaction tx = new ImportedTransaction(
                VALID_DATE,
                new BigDecimal("-50.00"),
                VALID_DESCRIPTION,
                null,
                null
            );

            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("-50.00"));
            assertThat(tx.isIncome()).isFalse();
            assertThat(tx.isExpense()).isTrue();
        }

        @Test
        @DisplayName("should treat zero amount as expense")
        void shouldTreatZeroAsExpense() {
            ImportedTransaction tx = new ImportedTransaction(
                VALID_DATE,
                BigDecimal.ZERO,
                VALID_DESCRIPTION,
                null,
                null
            );

            assertThat(tx.isIncome()).isFalse();
            assertThat(tx.isExpense()).isTrue();
        }

        @Test
        @DisplayName("should store optional balance")
        void shouldStoreOptionalBalance() {
            BigDecimal balance = new BigDecimal("1234.56");
            ImportedTransaction tx = new ImportedTransaction(
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                balance,
                null
            );

            assertThat(tx.balance()).isEqualTo(balance);
        }

        @Test
        @DisplayName("should store optional reference")
        void shouldStoreOptionalReference() {
            String reference = "TXN-123456";
            ImportedTransaction tx = new ImportedTransaction(
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                null,
                reference
            );

            assertThat(tx.reference()).isEqualTo(reference);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should throw when date is null")
        void shouldThrowWhenDateNull() {
            assertThatThrownBy(() -> new ImportedTransaction(
                null,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                null,
                null
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("date");
        }

        @Test
        @DisplayName("should throw when amount is null")
        void shouldThrowWhenAmountNull() {
            assertThatThrownBy(() -> new ImportedTransaction(
                VALID_DATE,
                null,
                VALID_DESCRIPTION,
                null,
                null
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("should throw when description is null or blank")
        void shouldThrowWhenDescriptionNullOrBlank() {
            assertThatThrownBy(() -> new ImportedTransaction(
                VALID_DATE,
                VALID_AMOUNT,
                null,
                null,
                null
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description");

            assertThatThrownBy(() -> new ImportedTransaction(
                VALID_DATE,
                VALID_AMOUNT,
                "  ",
                null,
                null
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description");
        }
    }

    @Nested
    @DisplayName("Absolute Amount Tests")
    class AbsoluteAmountTests {

        @Test
        @DisplayName("should return absolute amount for positive value")
        void shouldReturnAbsoluteForPositive() {
            ImportedTransaction tx = new ImportedTransaction(
                VALID_DATE,
                new BigDecimal("100.50"),
                VALID_DESCRIPTION,
                null,
                null
            );

            assertThat(tx.absoluteAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        }

        @Test
        @DisplayName("should return absolute amount for negative value")
        void shouldReturnAbsoluteForNegative() {
            ImportedTransaction tx = new ImportedTransaction(
                VALID_DATE,
                new BigDecimal("-75.25"),
                VALID_DESCRIPTION,
                null,
                null
            );

            assertThat(tx.absoluteAmount()).isEqualByComparingTo(new BigDecimal("75.25"));
        }
    }

    @Nested
    @DisplayName("Hash Code Tests")
    class HashCodeTests {

        @Test
        @DisplayName("should generate consistent transaction hash for duplicate detection")
        void shouldGenerateConsistentHash() {
            ImportedTransaction tx1 = new ImportedTransaction(
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                null,
                null
            );
            ImportedTransaction tx2 = new ImportedTransaction(
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                new BigDecimal("999.99"), // different balance
                "DIFFERENT_REF" // different reference
            );

            // Transaction hash should be same for same date/amount/description
            assertThat(tx1.transactionHash()).isEqualTo(tx2.transactionHash());
        }

        @Test
        @DisplayName("should generate different hash for different transactions")
        void shouldGenerateDifferentHash() {
            ImportedTransaction tx1 = new ImportedTransaction(
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                null,
                null
            );
            ImportedTransaction tx2 = new ImportedTransaction(
                VALID_DATE.plusDays(1), // different date
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                null,
                null
            );

            assertThat(tx1.transactionHash()).isNotEqualTo(tx2.transactionHash());
        }

        @Test
        @DisplayName("should normalize description for hash")
        void shouldNormalizeDescriptionForHash() {
            ImportedTransaction tx1 = new ImportedTransaction(
                VALID_DATE,
                VALID_AMOUNT,
                "  AMAZON   MARKETPLACE  ",
                null,
                null
            );
            ImportedTransaction tx2 = new ImportedTransaction(
                VALID_DATE,
                VALID_AMOUNT,
                "amazon marketplace",
                null,
                null
            );

            assertThat(tx1.transactionHash()).isEqualTo(tx2.transactionHash());
        }
    }
}
