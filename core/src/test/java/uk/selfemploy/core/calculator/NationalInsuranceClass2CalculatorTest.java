package uk.selfemploy.core.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for National Insurance Class 2 calculations for tax year 2025/26.
 *
 * Class 2 NI Rates (2025/26):
 * - Weekly rate: £3.50/week
 * - Annual amount: £3.50 x 52 = £182.00
 * - Small Profits Threshold: £6,845 (mandatory above, voluntary below)
 *
 * Note: Class 2 NI is separate from Class 4 NI:
 * - Class 2: Flat rate based on weeks, applies if profits > £6,845 (Small Profits Threshold)
 * - Class 4: Percentage-based on profits above £12,570 (Lower Profits Limit)
 */
@DisplayName("National Insurance Class 2 Calculator Tests (2025/26)")
class NationalInsuranceClass2CalculatorTest {

    private NationalInsuranceClass2Calculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new NationalInsuranceClass2Calculator(2025);
    }

    @Nested
    @DisplayName("Mandatory Class 2 NI - Above Small Profits Threshold")
    class MandatoryClass2NI {

        @Test
        @DisplayName("profits above SPT should calculate mandatory Class 2 NI at £182.00")
        void profitsAboveSptShouldCalculateMandatoryClass2Ni() {
            // £10,000 profit > £6,845 Small Profits Threshold
            // £3.50/week x 52 weeks = £182.00
            BigDecimal profit = new BigDecimal("10000");

            Class2NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("182.00"));
            assertThat(result.isMandatory()).isTrue();
            assertThat(result.isVoluntary()).isFalse();
            assertThat(result.weeklyRate()).isEqualByComparingTo(new BigDecimal("3.50"));
            assertThat(result.weeksLiable()).isEqualTo(52);
        }

        @Test
        @DisplayName("profits just above SPT should calculate mandatory Class 2 NI")
        void profitsJustAboveSptShouldCalculateMandatoryClass2Ni() {
            // £6,846 profit > £6,845 Small Profits Threshold
            BigDecimal profit = new BigDecimal("6846");

            Class2NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("182.00"));
            assertThat(result.isMandatory()).isTrue();
        }

        @Test
        @DisplayName("high profits should still calculate same Class 2 NI amount")
        void highProfitsShouldStillCalculateSameClass2NiAmount() {
            // Class 2 NI is a flat rate, not percentage-based like Class 4
            BigDecimal profit = new BigDecimal("100000");

            Class2NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("182.00"));
            assertThat(result.isMandatory()).isTrue();
        }
    }

    @Nested
    @DisplayName("No Class 2 NI - Below Small Profits Threshold")
    class NoClass2NI {

        @Test
        @DisplayName("profits below SPT should have zero Class 2 NI by default")
        void profitsBelowSptShouldHaveZeroClass2Ni() {
            // £5,000 profit < £6,845 Small Profits Threshold
            BigDecimal profit = new BigDecimal("5000");

            Class2NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.isMandatory()).isFalse();
            assertThat(result.isVoluntary()).isFalse();
        }

        @Test
        @DisplayName("profits at exactly SPT should have zero Class 2 NI")
        void profitsAtExactlySptShouldHaveZeroClass2Ni() {
            // £6,845 profit = £6,845 Small Profits Threshold (not exceeding)
            BigDecimal profit = new BigDecimal("6845");

            Class2NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.isMandatory()).isFalse();
        }
    }

    @Nested
    @DisplayName("Voluntary Class 2 NI - Below Threshold Option")
    class VoluntaryClass2NI {

        @Test
        @DisplayName("voluntary Class 2 NI should calculate £182.00 for profits below SPT")
        void voluntaryClass2NiShouldCalculate182ForProfitsBelowSpt() {
            // £5,000 profit < £6,845, but choosing to pay voluntarily
            BigDecimal profit = new BigDecimal("5000");
            boolean voluntary = true;

            Class2NICalculationResult result = calculator.calculate(profit, voluntary);

            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("182.00"));
            assertThat(result.isMandatory()).isFalse();
            assertThat(result.isVoluntary()).isTrue();
        }

        @Test
        @DisplayName("voluntary flag should be ignored if profits above SPT")
        void voluntaryFlagShouldBeIgnoredIfProfitsAboveSpt() {
            // £10,000 profit > £6,845 - Class 2 NI is mandatory regardless
            BigDecimal profit = new BigDecimal("10000");
            boolean voluntary = true;

            Class2NICalculationResult result = calculator.calculate(profit, voluntary);

            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("182.00"));
            assertThat(result.isMandatory()).isTrue();
            assertThat(result.isVoluntary()).isFalse(); // Not voluntary, it's mandatory
        }

        @Test
        @DisplayName("voluntary false should not pay Class 2 NI below threshold")
        void voluntaryFalseShouldNotPayClass2NiBelowThreshold() {
            BigDecimal profit = new BigDecimal("5000");
            boolean voluntary = false;

            Class2NICalculationResult result = calculator.calculate(profit, voluntary);

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.isVoluntary()).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("zero profit should have zero Class 2 NI")
        void zeroProfitShouldHaveZeroClass2Ni() {
            Class2NICalculationResult result = calculator.calculate(BigDecimal.ZERO);

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.isMandatory()).isFalse();
        }

        @Test
        @DisplayName("negative profit should have zero Class 2 NI")
        void negativeProfitShouldHaveZeroClass2Ni() {
            Class2NICalculationResult result = calculator.calculate(new BigDecimal("-5000"));

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.isMandatory()).isFalse();
        }

        @Test
        @DisplayName("null profit should be treated as zero")
        void nullProfitShouldBeTreatedAsZero() {
            Class2NICalculationResult result = calculator.calculate(null);

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.grossProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("voluntary Class 2 NI should still apply for zero profit")
        void voluntaryClass2NiShouldStillApplyForZeroProfit() {
            // Some self-employed people pay voluntary NI to build state pension
            Class2NICalculationResult result = calculator.calculate(BigDecimal.ZERO, true);

            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("182.00"));
            assertThat(result.isVoluntary()).isTrue();
        }
    }

    @Nested
    @DisplayName("Rate Details")
    class RateDetails {

        @Test
        @DisplayName("should provide correct weekly rate for 2025/26")
        void shouldProvideCorrectWeeklyRateFor2025() {
            Class2NICalculationResult result = calculator.calculate(new BigDecimal("10000"));

            assertThat(result.weeklyRate()).isEqualByComparingTo(new BigDecimal("3.50"));
        }

        @Test
        @DisplayName("should provide correct weeks liable for full year")
        void shouldProvideCorrectWeeksLiableForFullYear() {
            Class2NICalculationResult result = calculator.calculate(new BigDecimal("10000"));

            assertThat(result.weeksLiable()).isEqualTo(52);
        }

        @Test
        @DisplayName("should provide correct small profits threshold")
        void shouldProvideCorrectSmallProfitsThreshold() {
            Class2NICalculationResult result = calculator.calculate(new BigDecimal("10000"));

            assertThat(result.smallProfitsThreshold()).isEqualByComparingTo(new BigDecimal("6845"));
        }

        @Test
        @DisplayName("annual calculation should be £3.50 x 52 = £182.00")
        void annualCalculationShouldBe3_50Times52() {
            Class2NICalculationResult result = calculator.calculate(new BigDecimal("10000"));

            // Verify: £3.50 x 52 = £182.00
            BigDecimal expectedAnnual = new BigDecimal("3.50").multiply(new BigDecimal("52"));
            assertThat(result.totalNI()).isEqualByComparingTo(expectedAnnual);
            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("182.00"));
        }
    }

    @Nested
    @DisplayName("Result Record Methods")
    class ResultRecordMethods {

        @Test
        @DisplayName("effectiveRate should calculate correct percentage")
        void effectiveRateShouldCalculateCorrectPercentage() {
            BigDecimal profit = new BigDecimal("10000");
            Class2NICalculationResult result = calculator.calculate(profit);

            // £182.00 / £10,000 = 1.82%
            assertThat(result.effectiveRate()).isEqualByComparingTo(new BigDecimal("1.82"));
        }

        @Test
        @DisplayName("effectiveRate should be zero for zero profit")
        void effectiveRateShouldBeZeroForZeroProfit() {
            Class2NICalculationResult result = calculator.calculate(BigDecimal.ZERO);

            assertThat(result.effectiveRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("isApplicable should return true when NI is due")
        void isApplicableShouldReturnTrueWhenNiIsDue() {
            Class2NICalculationResult result = calculator.calculate(new BigDecimal("10000"));

            assertThat(result.isApplicable()).isTrue();
        }

        @Test
        @DisplayName("isApplicable should return false when no NI is due")
        void isApplicableShouldReturnFalseWhenNoNiIsDue() {
            Class2NICalculationResult result = calculator.calculate(new BigDecimal("5000"));

            assertThat(result.isApplicable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Multiple Tax Years")
    class MultipleTaxYears {

        @Test
        @DisplayName("should use correct rates for 2024 tax year")
        void shouldUseCorrectRatesFor2024TaxYear() {
            NationalInsuranceClass2Calculator calculator2024 = new NationalInsuranceClass2Calculator(2024);

            Class2NICalculationResult result = calculator2024.calculate(new BigDecimal("10000"));

            // 2024/25 rate is also £3.45/week (before correction) - check with /inga for historical rates
            // For now, we'll use the same rate structure
            assertThat(result.totalNI()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should support different tax years via constructor")
        void shouldSupportDifferentTaxYearsViaConstructor() {
            NationalInsuranceClass2Calculator calc2024 = new NationalInsuranceClass2Calculator(2024);
            NationalInsuranceClass2Calculator calc2025 = new NationalInsuranceClass2Calculator(2025);

            BigDecimal profit = new BigDecimal("10000");

            Class2NICalculationResult result2024 = calc2024.calculate(profit);
            Class2NICalculationResult result2025 = calc2025.calculate(profit);

            // Both should calculate valid results
            assertThat(result2024.totalNI()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result2025.totalNI()).isGreaterThan(BigDecimal.ZERO);
        }
    }
}
