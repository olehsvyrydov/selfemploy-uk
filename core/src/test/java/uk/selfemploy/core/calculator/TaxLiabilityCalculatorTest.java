package uk.selfemploy.core.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for combined Tax Liability calculations (Income Tax + NI Class 4).
 */
@DisplayName("Tax Liability Calculator Tests (2025/26)")
class TaxLiabilityCalculatorTest {

    private TaxLiabilityCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new TaxLiabilityCalculator(2025);
    }

    @Nested
    @DisplayName("Combined Tax Calculations")
    class CombinedTaxCalculations {

        @Test
        @DisplayName("should calculate combined liability for £20,000 profit")
        void shouldCalculateCombinedLiabilityFor20kProfit() {
            // Income Tax: £1,486
            // NI Class 4: £445.80
            // Total: £1,931.80
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("20000"));

            assertThat(result.incomeTax()).isEqualByComparingTo(new BigDecimal("1486.00"));
            assertThat(result.niClass4()).isEqualByComparingTo(new BigDecimal("445.80"));
            assertThat(result.totalLiability()).isEqualByComparingTo(new BigDecimal("1931.80"));
        }

        @Test
        @DisplayName("should calculate combined liability for £60,000 profit")
        void shouldCalculateCombinedLiabilityFor60kProfit() {
            // Income Tax: £11,432
            // NI Class 4: £2,456.60
            // Total: £13,888.60
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("60000"));

            assertThat(result.incomeTax()).isEqualByComparingTo(new BigDecimal("11432.00"));
            assertThat(result.niClass4()).isEqualByComparingTo(new BigDecimal("2456.60"));
            assertThat(result.totalLiability()).isEqualByComparingTo(new BigDecimal("13888.60"));
        }

        @Test
        @DisplayName("should calculate zero liability for income below thresholds")
        void shouldCalculateZeroLiabilityForLowIncome() {
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("10000"));

            assertThat(result.incomeTax()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.niClass4()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.totalLiability()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Net Profit Calculations")
    class NetProfitCalculations {

        @Test
        @DisplayName("should calculate net profit after tax")
        void shouldCalculateNetProfitAfterTax() {
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("60000"));

            // £60,000 - £13,888.60 = £46,111.40
            assertThat(result.netProfitAfterTax()).isEqualByComparingTo(new BigDecimal("46111.40"));
        }

        @Test
        @DisplayName("should calculate effective tax rate")
        void shouldCalculateEffectiveTaxRate() {
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("60000"));

            // £13,888.60 / £60,000 = 23.15% (rounded)
            assertThat(result.effectiveRate())
                .isGreaterThanOrEqualTo(new BigDecimal("23"))
                .isLessThanOrEqualTo(new BigDecimal("24"));
        }
    }

    @Nested
    @DisplayName("Multiple Tax Years")
    class MultipleTaxYears {

        @Test
        @DisplayName("should support different tax years")
        void shouldSupportDifferentTaxYears() {
            TaxLiabilityCalculator calculator2024 = new TaxLiabilityCalculator(2024);
            TaxLiabilityCalculator calculator2025 = new TaxLiabilityCalculator(2025);

            // Both should work with the same API
            TaxLiabilityResult result2024 = calculator2024.calculate(new BigDecimal("50000"));
            TaxLiabilityResult result2025 = calculator2025.calculate(new BigDecimal("50000"));

            assertThat(result2024.totalLiability()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result2025.totalLiability()).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Payment on Account")
    class PaymentOnAccount {

        @Test
        @DisplayName("should not require POA when liability under £1,000")
        void shouldNotRequirePoaWhenLiabilityUnder1000() {
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("15000"));

            assertThat(result.requiresPaymentOnAccount()).isFalse();
            assertThat(result.paymentOnAccountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should require POA when liability over £1,000")
        void shouldRequirePoaWhenLiabilityOver1000() {
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("60000"));

            assertThat(result.requiresPaymentOnAccount()).isTrue();
            // POA is 50% of total liability
            assertThat(result.paymentOnAccountAmount())
                .isEqualByComparingTo(result.totalLiability().divide(new BigDecimal("2")));
        }
    }
}
