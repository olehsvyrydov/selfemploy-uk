package uk.selfemploy.core.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for National Insurance Class 4 calculations for tax year 2025/26.
 *
 * NI Class 4 Rates (2025/26):
 * - Below Lower Profits Limit: £0 - £12,570 (0%)
 * - Main Rate: £12,570 - £50,270 (6%)
 * - Additional Rate: Over £50,270 (2%)
 */
@DisplayName("National Insurance Class 4 Calculator Tests (2025/26)")
class NationalInsuranceCalculatorTest {

    private NationalInsuranceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new NationalInsuranceCalculator(2025);
    }

    @Nested
    @DisplayName("Below Lower Profits Limit")
    class BelowLowerProfitsLimit {

        @Test
        @DisplayName("profit below LPL should have zero NI")
        void profitBelowLplShouldHaveZeroNi() {
            BigDecimal profit = new BigDecimal("10000");

            NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("profit at LPL should have zero NI")
        void profitAtLplShouldHaveZeroNi() {
            BigDecimal profit = new BigDecimal("12570");

            NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Main Rate Tests")
    class MainRateTests {

        @Test
        @DisplayName("profit £20,000 should calculate £445.80 NI")
        void profit20kShouldCalculate445_80Ni() {
            // £20,000 - £12,570 = £7,430 @ 6%
            // £7,430 * 0.06 = £445.80
            BigDecimal profit = new BigDecimal("20000");

            NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("445.80"));
            assertThat(result.mainRateNI()).isEqualByComparingTo(new BigDecimal("445.80"));
            assertThat(result.additionalRateNI()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("profit £50,270 should use full main rate band")
        void profit50270ShouldUseFullMainRateBand() {
            // £50,270 - £12,570 = £37,700 @ 6%
            // £37,700 * 0.06 = £2,262
            BigDecimal profit = new BigDecimal("50270");

            NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("2262.00"));
            assertThat(result.mainRateNI()).isEqualByComparingTo(new BigDecimal("2262.00"));
            assertThat(result.additionalRateNI()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Additional Rate Tests")
    class AdditionalRateTests {

        @Test
        @DisplayName("profit £60,000 should calculate £2,456.60 NI")
        void profit60kShouldCalculate2456_60Ni() {
            // Below LPL: £12,570 (0%)      = £0
            // Main Rate: £37,700 (6%)      = £2,262
            // Additional: £9,730 (2%)       = £194.60
            // Total:                        = £2,456.60
            BigDecimal profit = new BigDecimal("60000");

            NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("2456.60"));
            assertThat(result.mainRateNI()).isEqualByComparingTo(new BigDecimal("2262.00"));
            assertThat(result.additionalRateNI()).isEqualByComparingTo(new BigDecimal("194.60"));
        }

        @Test
        @DisplayName("profit £100,000 should calculate correct NI")
        void profit100kShouldCalculateCorrectNi() {
            // Main Rate: £37,700 (6%)       = £2,262
            // Additional: £49,730 (2%)      = £994.60
            // Total:                        = £3,256.60
            BigDecimal profit = new BigDecimal("100000");

            NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("3256.60"));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("zero profit should have zero NI")
        void zeroProfitShouldHaveZeroNi() {
            NICalculationResult result = calculator.calculate(BigDecimal.ZERO);

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("negative profit should have zero NI")
        void negativeProfitShouldHaveZeroNi() {
            NICalculationResult result = calculator.calculate(new BigDecimal("-5000"));

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("first pound over LPL should calculate 6p NI")
        void firstPoundOverLplShouldCalculate6pNi() {
            BigDecimal profit = new BigDecimal("12571");

            NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("0.06"));
        }
    }

    @Nested
    @DisplayName("Breakdown Details")
    class BreakdownDetails {

        @Test
        @DisplayName("should provide detailed breakdown for £60,000 profit")
        void shouldProvideDetailedBreakdownFor60kProfit() {
            NICalculationResult result = calculator.calculate(new BigDecimal("60000"));

            assertThat(result.profitSubjectToNI()).isEqualByComparingTo(new BigDecimal("47430.00")); // 60000 - 12570
            assertThat(result.mainRateAmount()).isEqualByComparingTo(new BigDecimal("37700.00"));
            assertThat(result.additionalRateAmount()).isEqualByComparingTo(new BigDecimal("9730.00"));
        }
    }

    /**
     * SE-808: State Pension Age Exemption Tests
     *
     * People above State Pension Age (currently 66) are exempt from Class 4 NI contributions.
     * The exemption applies if the person reaches pension age BEFORE the start of the tax year.
     *
     * Tax year 2025/26 starts on 6 April 2025.
     * To be exempt, the person must be 66 or older on 6 April 2025.
     * So they must be born on or before 6 April 1959.
     */
    @Nested
    @DisplayName("State Pension Age Exemption (SE-808)")
    class StatePensionAgeExemption {

        @Test
        @DisplayName("person aged 65 at tax year start should NOT be exempt")
        void personAged65AtTaxYearStartShouldNotBeExempt() {
            LocalDate dateOfBirth = LocalDate.of(1959, 4, 7);
            BigDecimal profit = new BigDecimal("60000");

            NICalculationResult result = calculator.calculate(profit, dateOfBirth);

            assertThat(result.isExempt()).isFalse();
            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("2456.60"));
            assertThat(result.exemptionReason()).isNull();
        }

        @Test
        @DisplayName("person aged 66 at tax year start should be exempt")
        void personAged66AtTaxYearStartShouldBeExempt() {
            LocalDate dateOfBirth = LocalDate.of(1959, 4, 6);
            BigDecimal profit = new BigDecimal("60000");

            NICalculationResult result = calculator.calculate(profit, dateOfBirth);

            assertThat(result.isExempt()).isTrue();
            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.exemptionReason()).isEqualTo("State Pension Age reached before tax year start");
        }

        @Test
        @DisplayName("person aged 67 at tax year start should be exempt")
        void personAged67AtTaxYearStartShouldBeExempt() {
            LocalDate dateOfBirth = LocalDate.of(1958, 1, 1);
            BigDecimal profit = new BigDecimal("100000");

            NICalculationResult result = calculator.calculate(profit, dateOfBirth);

            assertThat(result.isExempt()).isTrue();
            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.exemptionReason()).isEqualTo("State Pension Age reached before tax year start");
        }

        @Test
        @DisplayName("person turning 66 during tax year should NOT be exempt")
        void personTurning66DuringTaxYearShouldNotBeExempt() {
            LocalDate dateOfBirth = LocalDate.of(1959, 7, 1);
            BigDecimal profit = new BigDecimal("30000");

            NICalculationResult result = calculator.calculate(profit, dateOfBirth);

            assertThat(result.isExempt()).isFalse();
            assertThat(result.totalNI()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("null date of birth should NOT be exempt (default behavior)")
        void nullDateOfBirthShouldNotBeExempt() {
            BigDecimal profit = new BigDecimal("60000");

            NICalculationResult result = calculator.calculate(profit, null);

            assertThat(result.isExempt()).isFalse();
            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("2456.60"));
        }

        @Test
        @DisplayName("exempt person should have zero NI for all profit levels")
        void exemptPersonShouldHaveZeroNiForAllProfitLevels() {
            LocalDate dateOfBirth = LocalDate.of(1950, 1, 1);

            assertThat(calculator.calculate(new BigDecimal("20000"), dateOfBirth).totalNI())
                .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(calculator.calculate(new BigDecimal("50000"), dateOfBirth).totalNI())
                .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(calculator.calculate(new BigDecimal("100000"), dateOfBirth).totalNI())
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("exempt result should include breakdown showing exemption")
        void exemptResultShouldIncludeBreakdownShowingExemption() {
            LocalDate dateOfBirth = LocalDate.of(1958, 6, 15);
            BigDecimal profit = new BigDecimal("60000");

            NICalculationResult result = calculator.calculate(profit, dateOfBirth);

            assertThat(result.isExempt()).isTrue();
            assertThat(result.grossProfit()).isEqualByComparingTo(profit);
            assertThat(result.profitSubjectToNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.mainRateNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.additionalRateNI()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("young person should calculate normal NI")
        void youngPersonShouldCalculateNormalNi() {
            LocalDate dateOfBirth = LocalDate.of(1990, 6, 15);
            BigDecimal profit = new BigDecimal("60000");

            NICalculationResult result = calculator.calculate(profit, dateOfBirth);

            assertThat(result.isExempt()).isFalse();
            assertThat(result.totalNI()).isEqualByComparingTo(new BigDecimal("2456.60"));
        }

        @Test
        @DisplayName("person born exactly on tax year start boundary should be exempt")
        void personBornExactlyOnTaxYearStartBoundaryShouldBeExempt() {
            LocalDate dateOfBirth = LocalDate.of(1959, 4, 6);
            BigDecimal profit = new BigDecimal("50000");

            NICalculationResult result = calculator.calculate(profit, dateOfBirth);

            assertThat(result.isExempt()).isTrue();
            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("person born one day after boundary should NOT be exempt")
        void personBornOneDayAfterBoundaryShouldNotBeExempt() {
            LocalDate dateOfBirth = LocalDate.of(1959, 4, 7);
            BigDecimal profit = new BigDecimal("50000");

            NICalculationResult result = calculator.calculate(profit, dateOfBirth);

            assertThat(result.isExempt()).isFalse();
            assertThat(result.totalNI()).isGreaterThan(BigDecimal.ZERO);
        }
    }
}
