package uk.selfemploy.core.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for combined Tax Liability calculations (Income Tax + NI Class 4 + NI Class 2).
 *
 * Tax components for 2025/26:
 * - Income Tax: Based on taxable income bands (20% basic, 40% higher, 45% additional)
 * - NI Class 4: 6% on profits £12,570-£50,270, 2% above £50,270
 * - NI Class 2: £3.50/week = £182.00/year if profits > £6,845 (Small Profits Threshold)
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
            // Income Tax: £1,486.00
            // NI Class 4: £445.80
            // NI Class 2: £182.00 (profits > £6,845 threshold)
            // Total: £2,113.80
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("20000"));

            assertThat(result.incomeTax()).isEqualByComparingTo(new BigDecimal("1486.00"));
            assertThat(result.niClass4()).isEqualByComparingTo(new BigDecimal("445.80"));
            assertThat(result.niClass2()).isEqualByComparingTo(new BigDecimal("182.00"));
            assertThat(result.totalLiability()).isEqualByComparingTo(new BigDecimal("2113.80"));
        }

        @Test
        @DisplayName("should calculate combined liability for £60,000 profit")
        void shouldCalculateCombinedLiabilityFor60kProfit() {
            // Income Tax: £11,432.00
            // NI Class 4: £2,456.60
            // NI Class 2: £182.00 (profits > £6,845 threshold)
            // Total: £14,070.60
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("60000"));

            assertThat(result.incomeTax()).isEqualByComparingTo(new BigDecimal("11432.00"));
            assertThat(result.niClass4()).isEqualByComparingTo(new BigDecimal("2456.60"));
            assertThat(result.niClass2()).isEqualByComparingTo(new BigDecimal("182.00"));
            assertThat(result.totalLiability()).isEqualByComparingTo(new BigDecimal("14070.60"));
        }

        @Test
        @DisplayName("should calculate zero liability for income below all thresholds")
        void shouldCalculateZeroLiabilityForLowIncome() {
            // £5,000 profit < £6,845 Small Profits Threshold
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("5000"));

            assertThat(result.incomeTax()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.niClass4()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.niClass2()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.totalLiability()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should calculate Class 2 NI only for profits between £6,845 and £12,570")
        void shouldCalculateClass2NiOnlyBetweenThresholds() {
            // £10,000 profit > £6,845 SPT but < £12,570 LPL
            // Income Tax: £0 (below personal allowance)
            // NI Class 4: £0 (below Lower Profits Limit)
            // NI Class 2: £182.00 (above Small Profits Threshold)
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("10000"));

            assertThat(result.incomeTax()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.niClass4()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.niClass2()).isEqualByComparingTo(new BigDecimal("182.00"));
            assertThat(result.totalLiability()).isEqualByComparingTo(new BigDecimal("182.00"));
        }
    }

    @Nested
    @DisplayName("Class 2 NI Voluntary Payment")
    class Class2NIVoluntaryPayment {

        @Test
        @DisplayName("should allow voluntary Class 2 NI for profits below threshold")
        void shouldAllowVoluntaryClass2NiForProfitsBelowThreshold() {
            // £5,000 profit < £6,845 SPT but paying voluntarily
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("5000"), true);

            assertThat(result.niClass2()).isEqualByComparingTo(new BigDecimal("182.00"));
            assertThat(result.niClass2Details().isVoluntary()).isTrue();
            assertThat(result.niClass2Details().isMandatory()).isFalse();
        }

        @Test
        @DisplayName("should mark as mandatory when profits above threshold")
        void shouldMarkAsMandatoryWhenProfitsAboveThreshold() {
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("10000"));

            assertThat(result.niClass2Details().isMandatory()).isTrue();
            assertThat(result.niClass2Details().isVoluntary()).isFalse();
        }
    }

    @Nested
    @DisplayName("Total NI Calculation")
    class TotalNICalculation {

        @Test
        @DisplayName("should calculate total NI as Class 2 + Class 4")
        void shouldCalculateTotalNiAsClass2PlusClass4() {
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("60000"));

            // Total NI = £182.00 (Class 2) + £2,456.60 (Class 4) = £2,638.60
            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("2638.60"));
        }
    }

    @Nested
    @DisplayName("Net Profit Calculations")
    class NetProfitCalculations {

        @Test
        @DisplayName("should calculate net profit after tax including Class 2 NI")
        void shouldCalculateNetProfitAfterTax() {
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("60000"));

            // £60,000 - £14,070.60 = £45,929.40
            assertThat(result.netProfitAfterTax()).isEqualByComparingTo(new BigDecimal("45929.40"));
        }

        @Test
        @DisplayName("should calculate effective tax rate including Class 2 NI")
        void shouldCalculateEffectiveTaxRate() {
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("60000"));

            // £14,070.60 / £60,000 = 23.45% (rounded)
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

            // Both should include Class 2 NI
            assertThat(result2024.niClass2()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result2025.niClass2()).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Payment on Account")
    class PaymentOnAccount {

        @Test
        @DisplayName("should not require POA when liability under £1,000")
        void shouldNotRequirePoaWhenLiabilityUnder1000() {
            // £5,000 profit has zero liability (below all thresholds)
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("5000"));

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

    @Nested
    @DisplayName("Backward Compatibility")
    class BackwardCompatibility {

        @Test
        @DisplayName("should provide niDetails as alias for niClass4Details")
        void shouldProvideNiDetailsAsAliasForNiClass4Details() {
            TaxLiabilityResult result = calculator.calculate(new BigDecimal("60000"));

            // Deprecated niDetails() should return same as niClass4Details()
            assertThat(result.niDetails()).isEqualTo(result.niClass4Details());
        }
    }
}
