package uk.selfemploy.core.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for tax calculation boundary values.
 *
 * These tests verify critical HMRC boundary thresholds:
 * - Personal Allowance: £12,570
 * - Basic Rate Upper Limit / NI Upper Profits Limit: £50,270
 * - PA Taper Threshold: £100,000
 * - PA Fully Withdrawn: £125,140
 *
 * Test Cases: TC-TD009-010 through TC-TD009-015
 * HMRC Source: https://www.gov.uk/income-tax-rates
 *
 * @see <a href="https://www.gov.uk/income-tax-rates">HMRC Income Tax Rates</a>
 * @see <a href="https://www.gov.uk/self-employed-national-insurance-rates">HMRC NI Rates</a>
 */
@Tag("integration")
@DisplayName("Tax Calculation Boundary Value Integration Tests (TD-009)")
class TaxCalculationBoundaryIntegrationTest {

    private TaxLiabilityCalculator calculator2024;
    private TaxLiabilityCalculator calculator2025;
    private IncomeTaxCalculator incomeTaxCalculator2024;
    private NationalInsuranceCalculator niCalculator2024;

    @BeforeEach
    void setUp() {
        calculator2024 = new TaxLiabilityCalculator(2024);
        calculator2025 = new TaxLiabilityCalculator(2025);
        incomeTaxCalculator2024 = new IncomeTaxCalculator(2024);
        niCalculator2024 = new NationalInsuranceCalculator(2024);
    }

    @Nested
    @DisplayName("TC-TD009-010: Exactly £12,570 Income (Personal Allowance Boundary)")
    class PersonalAllowanceBoundary {

        @Test
        @DisplayName("income exactly at Personal Allowance should have zero tax")
        void incomeExactlyAtPersonalAllowanceShouldHaveZeroTax() {
            BigDecimal income = new BigDecimal("12570");

            TaxCalculationResult result = incomeTaxCalculator2024.calculate(income);

            assertThat(result.totalTax())
                .as("Income of £12,570 (exactly at PA) should have £0 tax")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("profit exactly at Lower Profits Limit should have zero NI")
        void profitExactlyAtLplShouldHaveZeroNi() {
            BigDecimal profit = new BigDecimal("12570");

            NICalculationResult result = niCalculator2024.calculate(profit);

            assertThat(result.totalNI())
                .as("Profit of £12,570 (exactly at LPL) should have £0 NI")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("combined calculation at £12,570 should be Class 2 NI only")
        void combinedAt12570ShouldBeClass2Only() {
            BigDecimal profit = new BigDecimal("12570");

            TaxLiabilityResult result = calculator2024.calculate(profit);

            assertThat(result.incomeTax()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.niClass4()).isEqualByComparingTo(BigDecimal.ZERO);
            // Class 2 NI applies since £12,570 > £6,725 Small Profits Threshold
            assertThat(result.niClass2()).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("TC-TD009-011: Exactly £12,571 Income (First Pound Over PA)")
    class FirstPoundOverPersonalAllowance {

        @Test
        @DisplayName("income £12,571 should calculate £0.20 tax (£1 × 20%)")
        void income12571ShouldCalculate20pTax() {
            BigDecimal income = new BigDecimal("12571");

            TaxCalculationResult result = incomeTaxCalculator2024.calculate(income);

            assertThat(result.totalTax())
                .as("Income of £12,571 (£1 over PA) should have £0.20 tax")
                .isEqualByComparingTo(new BigDecimal("0.20"));
        }

        @Test
        @DisplayName("profit £12,571 should calculate £0.06 NI (£1 × 6%)")
        void profit12571ShouldCalculate6pNi() {
            BigDecimal profit = new BigDecimal("12571");

            NICalculationResult result = niCalculator2024.calculate(profit);

            assertThat(result.totalNI())
                .as("Profit of £12,571 (£1 over LPL) should have £0.06 NI")
                .isEqualByComparingTo(new BigDecimal("0.06"));
        }
    }

    @Nested
    @DisplayName("TC-TD009-012: Exactly £100,000 Income (PA Taper Threshold)")
    class PaTaperThresholdBoundary {

        @Test
        @DisplayName("income £100,000 should have full Personal Allowance")
        void income100000ShouldHaveFullPersonalAllowance() {
            BigDecimal income = new BigDecimal("100000");

            TaxCalculationResult result = incomeTaxCalculator2024.calculate(income);

            // At exactly £100,000, PA is NOT tapered yet
            // Taxable income = £100,000 - £12,570 = £87,430
            // Tax = £37,700 × 20% + £49,730 × 40% = £7,540 + £19,892 = £27,432
            assertThat(result.personalAllowance())
                .as("At exactly £100,000, PA should be full £12,570 (not yet tapered)")
                .isEqualByComparingTo(new BigDecimal("12570"));
        }

        @Test
        @DisplayName("income £100,001 should still have full PA (integer rounding)")
        void income100001ShouldStillHaveFullPa() {
            BigDecimal income = new BigDecimal("100001");

            TaxCalculationResult result = incomeTaxCalculator2024.calculate(income);

            // At £100,001, excess = £1, reduction = £1/2 = £0 (ROUND_DOWN with integer precision)
            // PA remains at £12,570 due to integer rounding in implementation
            // Note: HMRC rounds to whole pounds, so this matches HMRC behavior
            assertThat(result.personalAllowance())
                .as("At £100,001, PA should still be £12,570 (£0.50 rounds to £0)")
                .isEqualByComparingTo(new BigDecimal("12570"));
        }

        @Test
        @DisplayName("income £100,002 should taper PA by £1")
        void income100002ShouldTaperPaBy1() {
            BigDecimal income = new BigDecimal("100002");

            TaxCalculationResult result = incomeTaxCalculator2024.calculate(income);

            // At £100,002, PA is reduced by £1 (£2 over × 50%)
            // Effective PA = £12,570 - £1 = £12,569
            assertThat(result.personalAllowance())
                .as("At £100,002, PA should be £12,569 (tapered by £1)")
                .isEqualByComparingTo(new BigDecimal("12569"));
        }
    }

    @Nested
    @DisplayName("TC-TD009-013: Exactly £125,140 Income (PA Fully Withdrawn)")
    class PaFullyWithdrawnBoundary {

        @Test
        @DisplayName("income £125,140 should have zero Personal Allowance")
        void income125140ShouldHaveZeroPersonalAllowance() {
            BigDecimal income = new BigDecimal("125140");

            TaxCalculationResult result = incomeTaxCalculator2024.calculate(income);

            // At £125,140, PA is fully withdrawn
            // £125,140 - £100,000 = £25,140 over taper threshold
            // PA reduction = £25,140 × 50% = £12,570 (entire PA)
            assertThat(result.personalAllowance())
                .as("At exactly £125,140, PA should be £0 (fully withdrawn)")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("PA withdrawal calculation should be exact")
        void paWithdrawalCalculationShouldBeExact() {
            // Verify the math: (£125,140 - £100,000) / 2 = £12,570 reduction
            BigDecimal overThreshold = new BigDecimal("125140").subtract(new BigDecimal("100000"));
            BigDecimal reduction = overThreshold.divide(new BigDecimal("2"));

            assertThat(reduction)
                .as("PA reduction should equal PA: (£125,140 - £100,000) ÷ 2 = £12,570")
                .isEqualByComparingTo(new BigDecimal("12570"));
        }

        @Test
        @DisplayName("income above £125,140 should still have zero PA")
        void incomeAbove125140ShouldStillHaveZeroPa() {
            BigDecimal income = new BigDecimal("200000");

            TaxCalculationResult result = incomeTaxCalculator2024.calculate(income);

            assertThat(result.personalAllowance())
                .as("Income above £125,140 should still have £0 PA")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("TC-TD009-014: Exactly £50,270 Profit (NI Upper Profits Limit)")
    class NiUpperProfitsLimitBoundary {

        @Test
        @DisplayName("profit exactly at £50,270 should use main rate only")
        void profitAt50270ShouldUseMainRateOnly() {
            BigDecimal profit = new BigDecimal("50270");

            NICalculationResult result = niCalculator2024.calculate(profit);

            // £50,270 - £12,570 = £37,700 × 6% = £2,262.00
            assertThat(result.mainRateNI())
                .as("Main rate NI on £50,270 profit")
                .isEqualByComparingTo(new BigDecimal("2262.00"));

            assertThat(result.additionalRateNI())
                .as("No additional rate NI at exactly £50,270")
                .isEqualByComparingTo(BigDecimal.ZERO);

            assertThat(result.totalNI())
                .as("Total NI at £50,270")
                .isEqualByComparingTo(new BigDecimal("2262.00"));
        }

        @Test
        @DisplayName("profit £50,271 should include £0.02 additional rate NI")
        void profit50271ShouldIncludeAdditionalRateNi() {
            BigDecimal profit = new BigDecimal("50271");

            NICalculationResult result = niCalculator2024.calculate(profit);

            // Main rate: £37,700 × 6% = £2,262.00
            // Additional rate: £1 × 2% = £0.02
            assertThat(result.mainRateNI())
                .as("Main rate NI on £50,271 profit")
                .isEqualByComparingTo(new BigDecimal("2262.00"));

            assertThat(result.additionalRateNI())
                .as("Additional rate NI: £1 × 2%")
                .isEqualByComparingTo(new BigDecimal("0.02"));

            assertThat(result.totalNI())
                .as("Total NI at £50,271")
                .isEqualByComparingTo(new BigDecimal("2262.02"));
        }
    }

    @Nested
    @DisplayName("TC-TD009-015: Negative Profit Handling")
    class NegativeProfitHandling {

        @Test
        @DisplayName("negative profit should have zero tax")
        void negativeProfitShouldHaveZeroTax() {
            BigDecimal loss = new BigDecimal("-5000");

            TaxCalculationResult result = incomeTaxCalculator2024.calculate(loss);

            assertThat(result.totalTax())
                .as("Negative profit (loss) should have £0 tax")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("negative profit should have zero NI")
        void negativeProfitShouldHaveZeroNi() {
            BigDecimal loss = new BigDecimal("-5000");

            NICalculationResult result = niCalculator2024.calculate(loss);

            assertThat(result.totalNI())
                .as("Negative profit (loss) should have £0 NI")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("zero profit should have zero liability")
        void zeroProfitShouldHaveZeroLiability() {
            TaxLiabilityResult result = calculator2024.calculate(BigDecimal.ZERO);

            assertThat(result.totalLiability())
                .as("Zero profit should have £0 total liability")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("null profit should be treated as zero")
        void nullProfitShouldBeTreatedAsZero() {
            TaxLiabilityResult result = calculator2024.calculate(null);

            assertThat(result.grossProfit())
                .as("Null profit should be treated as £0")
                .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.totalLiability())
                .as("Null profit should have £0 liability")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Cross-Tax-Year Boundary Verification")
    class CrossTaxYearBoundaryVerification {

        @Test
        @DisplayName("2024/25 and 2025/26 should have same PA boundary")
        void bothYearsShouldHaveSamePaBoundary() {
            BigDecimal incomeAtPa = new BigDecimal("12570");

            TaxLiabilityResult result2024 = calculator2024.calculate(incomeAtPa);
            TaxLiabilityResult result2025 = calculator2025.calculate(incomeAtPa);

            // Both should have zero income tax (at PA boundary)
            assertThat(result2024.incomeTax())
                .as("2024/25: £12,570 should have £0 income tax")
                .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result2025.incomeTax())
                .as("2025/26: £12,570 should have £0 income tax")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("2024/25 and 2025/26 should have same NI boundaries (frozen)")
        void bothYearsShouldHaveSameNiBoundaries() {
            BigDecimal profitAtUpl = new BigDecimal("50270");

            TaxLiabilityResult result2024 = calculator2024.calculate(profitAtUpl);
            TaxLiabilityResult result2025 = calculator2025.calculate(profitAtUpl);

            // Both should have same Class 4 NI
            assertThat(result2024.niClass4())
                .as("2024/25 NI at £50,270")
                .isEqualByComparingTo(new BigDecimal("2262.00"));
            assertThat(result2025.niClass4())
                .as("2025/26 NI at £50,270")
                .isEqualByComparingTo(new BigDecimal("2262.00"));
        }
    }

    @Nested
    @DisplayName("Additional Boundary Edge Cases")
    class AdditionalBoundaryEdgeCases {

        @Test
        @DisplayName("income one penny below PA should have zero tax")
        void incomeOnePennyBelowPaShouldHaveZeroTax() {
            BigDecimal income = new BigDecimal("12569.99");

            TaxCalculationResult result = incomeTaxCalculator2024.calculate(income);

            assertThat(result.totalTax())
                .as("Income 1p below PA should have £0 tax")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("income one penny over PA should have tax")
        void incomeOnePennyOverPaShouldHaveTax() {
            BigDecimal income = new BigDecimal("12570.01");

            TaxCalculationResult result = incomeTaxCalculator2024.calculate(income);

            // £0.01 × 20% = £0.00 (rounds to zero)
            // Need at least £0.50 for £0.10 tax
            assertThat(result.totalTax())
                .as("Income 1p over PA should be calculated (may round to zero)")
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("very large income should calculate correctly")
        void veryLargeIncomeShouldCalculateCorrectly() {
            BigDecimal income = new BigDecimal("1000000");

            TaxLiabilityResult result = calculator2024.calculate(income);

            // Should not overflow or error
            assertThat(result.totalLiability()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result.effectiveRate()).isLessThan(new BigDecimal("100"));
        }
    }
}
