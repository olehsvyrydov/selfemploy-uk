package uk.selfemploy.core.bankimport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.BankTransaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExclusionRulesEngine")
class ExclusionRulesEngineTest {

    private ExclusionRulesEngine engine;

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final UUID AUDIT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2025-06-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        engine = new ExclusionRulesEngine();
    }

    private BankTransaction createTransaction(String description, BigDecimal amount) {
        return BankTransaction.create(
            BUSINESS_ID, AUDIT_ID, "csv-barclays",
            LocalDate.of(2025, 6, 15), amount, description,
            "1234", null, "hash-" + description.hashCode(), NOW
        );
    }

    @Nested
    @DisplayName("Transfer detection")
    class TransferDetection {

        @Test
        @DisplayName("TFR keyword triggers exclusion")
        void tfrKeyword() {
            BankTransaction tx = createTransaction("TFR TO SAVINGS", new BigDecimal("-500.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isTrue();
            assertThat(result.reason()).contains("TRANSFER");
        }

        @Test
        @DisplayName("TRANSFER keyword triggers exclusion")
        void transferKeyword() {
            BankTransaction tx = createTransaction("BANK TRANSFER", new BigDecimal("-200.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isTrue();
        }

        @Test
        @DisplayName("FPO (Faster Payment Out) triggers exclusion")
        void fpoKeyword() {
            BankTransaction tx = createTransaction("FPO TO OTHER ACCOUNT", new BigDecimal("-1000.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isTrue();
        }

        @Test
        @DisplayName("FPI (Faster Payment In) triggers exclusion")
        void fpiKeyword() {
            BankTransaction tx = createTransaction("FPI FROM SAVINGS", new BigDecimal("500.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isTrue();
        }
    }

    @Nested
    @DisplayName("HMRC payment detection")
    class HmrcDetection {

        @Test
        @DisplayName("HMRC keyword triggers exclusion")
        void hmrcKeyword() {
            BankTransaction tx = createTransaction("HMRC SELF ASSESSMENT", new BigDecimal("-5000.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isTrue();
            assertThat(result.reason()).contains("TAX_PAYMENT");
        }

        @Test
        @DisplayName("HMRC payment-on-account triggers exclusion")
        void hmrcPaymentOnAccount() {
            BankTransaction tx = createTransaction("HMRC P/MENT ON ACCT", new BigDecimal("-3000.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isTrue();
        }
    }

    @Nested
    @DisplayName("Loan and credit card detection")
    class LoanDetection {

        @Test
        @DisplayName("LOAN CREDIT triggers exclusion")
        void loanCredit() {
            BankTransaction tx = createTransaction("LOAN CREDIT", new BigDecimal("10000.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isTrue();
            assertThat(result.reason()).contains("LOAN");
        }

        @Test
        @DisplayName("LOAN PAYMENT triggers exclusion")
        void loanPayment() {
            BankTransaction tx = createTransaction("LOAN PAYMENT", new BigDecimal("-250.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isTrue();
        }

        @Test
        @DisplayName("CC PAYMENT triggers exclusion")
        void ccPayment() {
            BankTransaction tx = createTransaction("CC PAYMENT VISA", new BigDecimal("-1500.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isTrue();
            assertThat(result.reason()).contains("CREDIT_CARD");
        }
    }

    @Nested
    @DisplayName("Cash withdrawal detection")
    class CashDetection {

        @Test
        @DisplayName("ATM keyword triggers exclusion")
        void atmKeyword() {
            BankTransaction tx = createTransaction("ATM WITHDRAWAL", new BigDecimal("-200.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isTrue();
            assertThat(result.reason()).contains("CASH");
        }

        @Test
        @DisplayName("CASH keyword triggers exclusion")
        void cashKeyword() {
            BankTransaction tx = createTransaction("CASH WITHDRAWAL", new BigDecimal("-100.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isTrue();
        }
    }

    @Nested
    @DisplayName("Non-excluded transactions")
    class NonExcluded {

        @Test
        @DisplayName("normal business expense not excluded")
        void normalExpenseNotExcluded() {
            BankTransaction tx = createTransaction("OFFICE DEPOT STATIONERY", new BigDecimal("-45.99"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isFalse();
            assertThat(result.reason()).isNull();
        }

        @Test
        @DisplayName("client payment not excluded")
        void clientPaymentNotExcluded() {
            BankTransaction tx = createTransaction("ACME CORP INVOICE 1234", new BigDecimal("5000.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isFalse();
        }

        @Test
        @DisplayName("subscription not excluded")
        void subscriptionNotExcluded() {
            BankTransaction tx = createTransaction("ADOBE CREATIVE CLOUD", new BigDecimal("-52.99"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isFalse();
        }
    }

    @Nested
    @DisplayName("Case insensitivity")
    class CaseInsensitivity {

        @Test
        @DisplayName("lowercase transfer detected")
        void lowercaseTransfer() {
            BankTransaction tx = createTransaction("transfer to savings", new BigDecimal("-500.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isTrue();
        }

        @Test
        @DisplayName("mixed case HMRC detected")
        void mixedCaseHmrc() {
            BankTransaction tx = createTransaction("Hmrc Self Assessment", new BigDecimal("-5000.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.shouldExclude()).isTrue();
        }
    }

    @Nested
    @DisplayName("Confidence scoring")
    class ConfidenceScoring {

        @Test
        @DisplayName("excluded transactions have high confidence")
        void excludedHaveHighConfidence() {
            BankTransaction tx = createTransaction("TFR TO ISA", new BigDecimal("-1000.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
        }

        @Test
        @DisplayName("non-excluded transactions return LOW confidence")
        void nonExcludedLowConfidence() {
            BankTransaction tx = createTransaction("BUSINESS PAYMENT", new BigDecimal("-100.00"));

            ExclusionResult result = engine.evaluate(tx);

            assertThat(result.confidence()).isEqualTo(Confidence.LOW);
        }
    }
}
