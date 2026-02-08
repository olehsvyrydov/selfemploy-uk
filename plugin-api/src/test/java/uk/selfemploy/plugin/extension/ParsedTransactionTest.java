package uk.selfemploy.plugin.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ParsedTransaction}.
 * Tests the SPI output record for bank statement parsers.
 */
@DisplayName("ParsedTransaction")
class ParsedTransactionTest {

    private static final LocalDate SAMPLE_DATE = LocalDate.of(2025, 6, 15);

    @Nested
    @DisplayName("when creating a transaction")
    class Creation {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            ParsedTransaction tx = new ParsedTransaction(
                SAMPLE_DATE,
                "Client Payment",
                new BigDecimal("1500.00"),
                "REF123",
                "Sales",
                "1234"
            );

            assertThat(tx.date()).isEqualTo(SAMPLE_DATE);
            assertThat(tx.description()).isEqualTo("Client Payment");
            assertThat(tx.amount()).isEqualByComparingTo("1500.00");
            assertThat(tx.reference()).isEqualTo("REF123");
            assertThat(tx.category()).isEqualTo("Sales");
            assertThat(tx.accountInfo()).isEqualTo("1234");
        }

        @Test
        @DisplayName("should create with nullable optional fields")
        void shouldCreateWithNullOptionalFields() {
            ParsedTransaction tx = new ParsedTransaction(
                SAMPLE_DATE,
                "Payment",
                new BigDecimal("100.00"),
                null,
                null,
                null
            );

            assertThat(tx.reference()).isNull();
            assertThat(tx.category()).isNull();
            assertThat(tx.accountInfo()).isNull();
        }

        @Test
        @DisplayName("should reject null date")
        void shouldRejectNullDate() {
            assertThatThrownBy(() -> new ParsedTransaction(
                null, "Payment", new BigDecimal("100"), null, null, null
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("date");
        }

        @Test
        @DisplayName("should reject null description")
        void shouldRejectNullDescription() {
            assertThatThrownBy(() -> new ParsedTransaction(
                SAMPLE_DATE, null, new BigDecimal("100"), null, null, null
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description");
        }

        @Test
        @DisplayName("should reject blank description")
        void shouldRejectBlankDescription() {
            assertThatThrownBy(() -> new ParsedTransaction(
                SAMPLE_DATE, "   ", new BigDecimal("100"), null, null, null
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description");
        }

        @Test
        @DisplayName("should reject null amount")
        void shouldRejectNullAmount() {
            assertThatThrownBy(() -> new ParsedTransaction(
                SAMPLE_DATE, "Payment", null, null, null, null
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
        }
    }

    @Nested
    @DisplayName("income/expense classification")
    class Classification {

        @Test
        @DisplayName("positive amount is income")
        void positiveAmountIsIncome() {
            ParsedTransaction tx = new ParsedTransaction(
                SAMPLE_DATE, "Payment", new BigDecimal("500.00"), null, null, null
            );

            assertThat(tx.isIncome()).isTrue();
            assertThat(tx.isExpense()).isFalse();
        }

        @Test
        @DisplayName("negative amount is expense")
        void negativeAmountIsExpense() {
            ParsedTransaction tx = new ParsedTransaction(
                SAMPLE_DATE, "Office Supplies", new BigDecimal("-45.99"), null, null, null
            );

            assertThat(tx.isIncome()).isFalse();
            assertThat(tx.isExpense()).isTrue();
        }

        @Test
        @DisplayName("zero amount is neither income nor expense")
        void zeroAmountIsNeither() {
            ParsedTransaction tx = new ParsedTransaction(
                SAMPLE_DATE, "Zero Transaction", BigDecimal.ZERO, null, null, null
            );

            assertThat(tx.isIncome()).isFalse();
            assertThat(tx.isExpense()).isFalse();
        }
    }

    @Nested
    @DisplayName("absolute amount")
    class AbsoluteAmount {

        @Test
        @DisplayName("returns positive for positive amount")
        void positiveRemains() {
            ParsedTransaction tx = new ParsedTransaction(
                SAMPLE_DATE, "Payment", new BigDecimal("500.00"), null, null, null
            );

            assertThat(tx.absoluteAmount()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("returns positive for negative amount")
        void negativeBecomesPositive() {
            ParsedTransaction tx = new ParsedTransaction(
                SAMPLE_DATE, "Expense", new BigDecimal("-45.99"), null, null, null
            );

            assertThat(tx.absoluteAmount()).isEqualByComparingTo("45.99");
        }
    }
}
