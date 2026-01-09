package uk.selfemploy.core.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
}
