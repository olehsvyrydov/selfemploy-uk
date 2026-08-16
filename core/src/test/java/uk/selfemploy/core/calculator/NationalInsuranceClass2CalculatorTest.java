package uk.selfemploy.core.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.selfemploy.core.config.TaxRateConfiguration;

import java.math.BigDecimal;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for National Insurance Class 2 calculations for tax year 2025/26.
 *
 * Class 2 NI Rates (2025/26):
 * - Weekly rate: £3.50/week
 * - Annual amount: £3.50 x 52 = £182.00, charged only on the voluntary path
 * - Small Profits Threshold: £6,845 (treated as paid above, voluntary below)
 *
 * Note: Class 2 NI is separate from Class 4 NI:
 * - Class 2: Flat weekly rate, tied to the Small Profits Threshold (£6,845)
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
    @DisplayName("Above Small Profits Threshold - treated as paid, nothing charged")
    class AboveSmallProfitsThreshold {

        @Test
        @DisplayName("profits above SPT are treated as paid and cost nothing")
        void profitsAboveSptAreTreatedAsPaid() {
            // £10,000 profit > £6,845 Small Profits Threshold
            BigDecimal profit = new BigDecimal("10000");

            Class2NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.isTreatedAsPaid()).isTrue();
            assertThat(result.isMandatory()).isFalse();
            assertThat(result.isVoluntary()).isFalse();
            assertThat(result.weeklyRate()).isEqualByComparingTo(new BigDecimal("3.50"));
            assertThat(result.weeksLiable()).isZero();
        }

        @Test
        @DisplayName("profits just above SPT are treated as paid")
        void profitsJustAboveSptAreTreatedAsPaid() {
            // £6,846 profit > £6,845 Small Profits Threshold
            BigDecimal profit = new BigDecimal("6846");

            Class2NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.isTreatedAsPaid()).isTrue();
        }

        @Test
        @DisplayName("high profits are treated as paid too - the charge does not return")
        void highProfitsAreAlsoTreatedAsPaid() {
            BigDecimal profit = new BigDecimal("100000");

            Class2NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.isTreatedAsPaid()).isTrue();
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
            // £10,000 profit > £6,845 - there is nothing to volunteer for, the year is already paid
            BigDecimal profit = new BigDecimal("10000");
            boolean voluntary = true;

            Class2NICalculationResult result = calculator.calculate(profit, voluntary);

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.isTreatedAsPaid()).isTrue();
            assertThat(result.isVoluntary()).isFalse();
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
        @DisplayName("should provide correct weeks liable for a full voluntary year")
        void shouldProvideCorrectWeeksLiableForFullYear() {
            Class2NICalculationResult result = calculator.calculate(new BigDecimal("5000"), true);

            assertThat(result.weeksLiable()).isEqualTo(52);
        }

        @Test
        @DisplayName("should provide correct small profits threshold")
        void shouldProvideCorrectSmallProfitsThreshold() {
            Class2NICalculationResult result = calculator.calculate(new BigDecimal("10000"));

            assertThat(result.smallProfitsThreshold()).isEqualByComparingTo(new BigDecimal("6845"));
        }

        @Test
        @DisplayName("voluntary annual calculation should be £3.50 x 52 = £182.00")
        void annualCalculationShouldBe3_50Times52() {
            Class2NICalculationResult result = calculator.calculate(new BigDecimal("5000"), true);

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
            BigDecimal profit = new BigDecimal("5000");
            Class2NICalculationResult result = calculator.calculate(profit, true);

            // £182.00 / £5,000 = 3.64%
            assertThat(result.effectiveRate()).isEqualByComparingTo(new BigDecimal("3.64"));
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
            Class2NICalculationResult result = calculator.calculate(new BigDecimal("5000"), true);

            assertThat(result.isApplicable()).isTrue();
        }

        @Test
        @DisplayName("isApplicable should return false when no NI is due")
        void isApplicableShouldReturnFalseWhenNoNiIsDue() {
            Class2NICalculationResult result = calculator.calculate(new BigDecimal("5000"));

            assertThat(result.isApplicable()).isFalse();
        }

        @Test
        @DisplayName("isApplicable should be false above the threshold, where nothing is charged")
        void isApplicableShouldBeFalseWhenTreatedAsPaid() {
            Class2NICalculationResult result = calculator.calculate(new BigDecimal("10000"));

            assertThat(result.isApplicable()).isFalse();
            assertThat(result.isTreatedAsPaid()).isTrue();
        }
    }

    @Nested
    @DisplayName("Class 2 abolition — no weekly charge above the SPT from 2024/25")
    class AbolishedFrom2024 {

        /**
         * Every configured tax year from 2024/25 onwards, so a new rate file is covered the day it
         * is added rather than the day someone remembers to extend this list.
         */
        static IntStream abolishedYears() {
            return TaxRateConfiguration.getInstance().getSupportedTaxYears().stream()
                .mapToInt(Integer::intValue)
                .filter(year -> year >= 2024);
        }

        @Test
        @DisplayName("the years under test are the configured ones from 2024/25 onwards")
        void abolishedYearsCoversEveryConfiguredYearFrom2024() {
            assertThat(abolishedYears().boxed()).contains(2024, 2025, 2026);
        }

        @ParameterizedTest(name = "{0}/{1}")
        @MethodSource("abolishedYears")
        @DisplayName("above-SPT profit is treated as paid, adding nothing to Class 2")
        void aboveSptAddsNoClass2Charge(int taxYear) {
            NationalInsuranceClass2Calculator calc = new NationalInsuranceClass2Calculator(taxYear);
            BigDecimal aboveSpt = calc.getRates().smallProfitsThreshold().add(new BigDecimal("10000"));

            Class2NICalculationResult result = calc.calculate(aboveSpt);

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.weeksLiable()).isZero();
            assertThat(result.isMandatory()).isFalse();
            assertThat(result.isTreatedAsPaid()).isTrue();
        }

        @ParameterizedTest(name = "{0}/{1}")
        @MethodSource("abolishedYears")
        @DisplayName("above-SPT profit adds no weekly charge to the total liability")
        void aboveSptAddsNoClass2ChargeToTotalLiability(int taxYear) {
            BigDecimal profit = new BigDecimal("30000");

            TaxLiabilityResult result = new TaxLiabilityCalculator(taxYear).calculate(profit);

            assertThat(result.niClass2()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.totalLiability())
                .isEqualByComparingTo(result.incomeTax().add(result.niClass4()));
        }

        @ParameterizedTest(name = "{0}/{1}")
        @MethodSource("abolishedYears")
        @DisplayName("the voluntary path below the SPT survives abolition")
        void belowSptRemainsVoluntary(int taxYear) {
            NationalInsuranceClass2Calculator calc = new NationalInsuranceClass2Calculator(taxYear);
            BigDecimal belowSpt = calc.getRates().smallProfitsThreshold().subtract(BigDecimal.ONE);
            BigDecimal expected = calc.getRates().weeklyRate().multiply(new BigDecimal("52"));

            Class2NICalculationResult result = calc.calculate(belowSpt, true);

            assertThat(result.totalNI()).isEqualByComparingTo(expected);
            assertThat(result.weeksLiable()).isEqualTo(52);
            assertThat(result.isVoluntary()).isTrue();
            assertThat(result.isTreatedAsPaid()).isFalse();
        }
    }

    @Nested
    @DisplayName("Multiple Tax Years")
    class MultipleTaxYears {

        @Test
        @DisplayName("should use correct rates for 2024 tax year")
        void shouldUseCorrectRatesFor2024TaxYear() {
            NationalInsuranceClass2Calculator calculator2024 = new NationalInsuranceClass2Calculator(2024);

            Class2NICalculationResult result = calculator2024.calculate(new BigDecimal("5000"), true);

            assertThat(result.weeklyRate()).isEqualByComparingTo(new BigDecimal("3.45"));
            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("179.40"));
        }

        @Test
        @DisplayName("should support different tax years via constructor")
        void shouldSupportDifferentTaxYearsViaConstructor() {
            NationalInsuranceClass2Calculator calc2024 = new NationalInsuranceClass2Calculator(2024);
            NationalInsuranceClass2Calculator calc2025 = new NationalInsuranceClass2Calculator(2025);

            BigDecimal profit = new BigDecimal("5000");

            Class2NICalculationResult result2024 = calc2024.calculate(profit, true);
            Class2NICalculationResult result2025 = calc2025.calculate(profit, true);

            // Each year charges its own weekly rate on the voluntary path
            assertThat(result2024.totalNI()).isEqualByComparingTo(new BigDecimal("179.40"));
            assertThat(result2025.totalNI()).isEqualByComparingTo(new BigDecimal("182.00"));
        }
    }
}
