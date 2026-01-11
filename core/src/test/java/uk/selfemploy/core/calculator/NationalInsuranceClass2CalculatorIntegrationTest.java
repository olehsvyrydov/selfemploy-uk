package uk.selfemploy.core.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for National Insurance Class 2 Calculator.
 * Implements P0 test cases from /rob's QA specifications (SE-801).
 *
 * <p>Test Reference: docs/sprints/sprint-6/testing/rob-qa-SE-801-SE-507.md
 *
 * <p>Key Test Data (2025/26):
 * <ul>
 *   <li>Weekly Rate: GBP 3.50</li>
 *   <li>Annual Amount: GBP 182.00 (52 weeks)</li>
 *   <li>Small Profits Threshold: GBP 6,845</li>
 *   <li>Class 4 Lower Profits Limit: GBP 12,570 (different from Class 2 threshold)</li>
 * </ul>
 *
 * @author /adam (Senior E2E Test Automation Engineer)
 * @see NationalInsuranceClass2Calculator
 * @see Class2NICalculationResult
 */
@DisplayName("SE-801: Class 2 NI Calculator Integration Tests")
class NationalInsuranceClass2CalculatorIntegrationTest {

    private static final int TAX_YEAR_2025 = 2025;
    private static final int TAX_YEAR_2024 = 2024;

    // 2025/26 rates
    private static final BigDecimal WEEKLY_RATE_2025 = new BigDecimal("3.50");
    private static final BigDecimal ANNUAL_AMOUNT_2025 = new BigDecimal("182.00");
    private static final BigDecimal SMALL_PROFITS_THRESHOLD_2025 = new BigDecimal("6845");

    // 2024/25 rates
    private static final BigDecimal WEEKLY_RATE_2024 = new BigDecimal("3.45");
    private static final BigDecimal ANNUAL_AMOUNT_2024 = new BigDecimal("179.40");
    private static final BigDecimal SMALL_PROFITS_THRESHOLD_2024 = new BigDecimal("6725");

    private NationalInsuranceClass2Calculator calculator;
    private TaxLiabilityCalculator taxLiabilityCalculator;

    @BeforeEach
    void setUp() {
        calculator = new NationalInsuranceClass2Calculator(TAX_YEAR_2025);
        taxLiabilityCalculator = new TaxLiabilityCalculator(TAX_YEAR_2025);
    }

    // =========================================================================
    // TC-801-010: Mandatory Class 2 NI - Above Threshold
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-801-010: Mandatory Class 2 NI - Above Threshold")
    class MandatoryClass2NIAboveThreshold {

        @Test
        @DisplayName("profits of GBP 10,000 (above threshold GBP 6,845) should calculate mandatory Class 2 NI = GBP 182.00")
        void profitsAboveThresholdShouldCalculateMandatoryClass2NI() {
            // Given: Gross profit GBP 10,000 (above Small Profits Threshold of GBP 6,845)
            BigDecimal grossProfit = new BigDecimal("10000");

            // When: Calculate Class 2 NI
            Class2NICalculationResult result = calculator.calculate(grossProfit);

            // Then: Mandatory Class 2 NI should be GBP 182.00
            assertThat(result.totalNI())
                .as("Class 2 NI for profits above threshold should be GBP 182.00")
                .isEqualByComparingTo(ANNUAL_AMOUNT_2025);

            assertThat(result.isMandatory())
                .as("Class 2 NI should be mandatory when profits exceed threshold")
                .isTrue();

            assertThat(result.isVoluntary())
                .as("Class 2 NI should NOT be voluntary when mandatory")
                .isFalse();

            assertThat(result.weeklyRate())
                .as("Weekly rate should be GBP 3.50 for 2025/26")
                .isEqualByComparingTo(WEEKLY_RATE_2025);

            assertThat(result.weeksLiable())
                .as("Weeks liable should be 52 for full year")
                .isEqualTo(52);
        }
    }

    // =========================================================================
    // TC-801-011: No Class 2 NI - Below Threshold (Not Voluntary)
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-801-011: No Class 2 NI - Below Threshold")
    class NoClass2NIBelowThreshold {

        @Test
        @DisplayName("profits of GBP 5,000 (below threshold GBP 6,845) should have no Class 2 NI")
        void profitsBelowThresholdShouldHaveNoClass2NI() {
            // Given: Gross profit GBP 5,000 (below Small Profits Threshold of GBP 6,845)
            BigDecimal grossProfit = new BigDecimal("5000");

            // When: Calculate Class 2 NI without voluntary option
            Class2NICalculationResult result = calculator.calculate(grossProfit, false);

            // Then: No Class 2 NI should be due
            assertThat(result.totalNI())
                .as("Class 2 NI should be zero for profits below threshold")
                .isEqualByComparingTo(BigDecimal.ZERO);

            assertThat(result.isMandatory())
                .as("Class 2 NI should NOT be mandatory below threshold")
                .isFalse();

            assertThat(result.isVoluntary())
                .as("Class 2 NI should NOT be voluntary when not opted in")
                .isFalse();

            assertThat(result.weeksLiable())
                .as("Weeks liable should be 0 when no Class 2 NI due")
                .isEqualTo(0);
        }
    }

    // =========================================================================
    // TC-801-012: Boundary Test - Exactly at Threshold
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-801-012: Boundary Test - Exactly at Threshold")
    class BoundaryTestAtThreshold {

        @Test
        @DisplayName("profits of exactly GBP 6,845 (at threshold) should have NO Class 2 NI")
        void profitsAtExactlyThresholdShouldHaveNoClass2NI() {
            // Given: Gross profit exactly GBP 6,845 (equals Small Profits Threshold)
            BigDecimal grossProfit = new BigDecimal("6845");

            // When: Calculate Class 2 NI
            Class2NICalculationResult result = calculator.calculate(grossProfit);

            // Then: No Class 2 NI (threshold is NOT exceeded, condition is "greater than")
            assertThat(result.totalNI())
                .as("Class 2 NI should be zero when profit equals threshold (> not >=)")
                .isEqualByComparingTo(BigDecimal.ZERO);

            assertThat(result.isMandatory())
                .as("Class 2 NI should NOT be mandatory when profit equals threshold")
                .isFalse();
        }

        @Test
        @DisplayName("profits of exactly GBP 6,845.00 with decimal precision should have NO Class 2 NI")
        void profitsAtExactlyThresholdWithDecimalsShouldHaveNoClass2NI() {
            // Given: Gross profit GBP 6,845.00 (with decimal places)
            BigDecimal grossProfit = new BigDecimal("6845.00");

            // When: Calculate Class 2 NI
            Class2NICalculationResult result = calculator.calculate(grossProfit);

            // Then: No Class 2 NI
            assertThat(result.totalNI())
                .as("Class 2 NI should be zero when profit equals threshold with decimals")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // =========================================================================
    // TC-801-013: Boundary Test - Just Above Threshold
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-801-013: Just Above Threshold")
    class JustAboveThreshold {

        @Test
        @DisplayName("profits of GBP 6,846 (just above GBP 6,845) should apply Class 2 NI")
        void profitsJustAboveThresholdShouldApplyClass2NI() {
            // Given: Gross profit GBP 6,846 (GBP 1 above threshold)
            BigDecimal grossProfit = new BigDecimal("6846");

            // When: Calculate Class 2 NI
            Class2NICalculationResult result = calculator.calculate(grossProfit);

            // Then: Class 2 NI should apply
            assertThat(result.totalNI())
                .as("Class 2 NI should be GBP 182.00 when profit exceeds threshold")
                .isEqualByComparingTo(ANNUAL_AMOUNT_2025);

            assertThat(result.isMandatory())
                .as("Class 2 NI should be mandatory when profit exceeds threshold")
                .isTrue();
        }

        @Test
        @DisplayName("profits of GBP 6,845.01 (pennies above threshold) should apply Class 2 NI")
        void profitsPenniesAboveThresholdShouldApplyClass2NI() {
            // Given: Gross profit GBP 6,845.01 (1 penny above threshold)
            BigDecimal grossProfit = new BigDecimal("6845.01");

            // When: Calculate Class 2 NI
            Class2NICalculationResult result = calculator.calculate(grossProfit);

            // Then: Class 2 NI should apply
            assertThat(result.totalNI())
                .as("Class 2 NI should apply even with 1 penny above threshold")
                .isEqualByComparingTo(ANNUAL_AMOUNT_2025);

            assertThat(result.isMandatory())
                .as("Class 2 NI should be mandatory for GBP 6,845.01")
                .isTrue();
        }
    }

    // =========================================================================
    // TC-801-020: Separate Display of Class 2 and Class 4 NI
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-801-020: Separate Display of Class 2 and Class 4 NI")
    class SeparateDisplayClass2AndClass4 {

        @Test
        @DisplayName("profits of GBP 20,000 should show Class 2 and Class 4 separately")
        void profitsAboveBothThresholdsShouldShowBothNITypesSeparately() {
            // Given: Gross profit GBP 20,000 (above both Class 2 and Class 4 thresholds)
            BigDecimal grossProfit = new BigDecimal("20000");

            // When: Calculate combined tax liability
            TaxLiabilityResult result = taxLiabilityCalculator.calculate(grossProfit);

            // Then: Class 2 and Class 4 should be separate
            assertThat(result.niClass2())
                .as("Class 2 NI should be GBP 182.00 (flat rate)")
                .isEqualByComparingTo(ANNUAL_AMOUNT_2025);

            assertThat(result.niClass4())
                .as("Class 4 NI should be calculated separately based on profits above LPL")
                .isGreaterThan(BigDecimal.ZERO);

            // Verify Class 2 details are available
            assertThat(result.niClass2Details())
                .as("Class 2 details should be present in result")
                .isNotNull();

            // Verify Class 4 details are available
            assertThat(result.niClass4Details())
                .as("Class 4 details should be present in result")
                .isNotNull();

            // Verify they are different values
            assertThat(result.niClass2())
                .as("Class 2 and Class 4 should be different amounts")
                .isNotEqualByComparingTo(result.niClass4());
        }

        @Test
        @DisplayName("profits of GBP 10,000 (above Class 2, below Class 4 threshold) should show only Class 2")
        void profitsAboveClass2OnlyShouldShowOnlyClass2() {
            // Given: Profit GBP 10,000 (> GBP 6,845 Class 2 threshold, < GBP 12,570 Class 4 LPL)
            BigDecimal grossProfit = new BigDecimal("10000");

            // When: Calculate combined tax liability
            TaxLiabilityResult result = taxLiabilityCalculator.calculate(grossProfit);

            // Then: Only Class 2 NI should apply
            assertThat(result.niClass2())
                .as("Class 2 NI should be GBP 182.00")
                .isEqualByComparingTo(ANNUAL_AMOUNT_2025);

            assertThat(result.niClass4())
                .as("Class 4 NI should be zero (below Lower Profits Limit)")
                .isEqualByComparingTo(BigDecimal.ZERO);

            assertThat(result.totalNI())
                .as("Total NI should equal Class 2 only")
                .isEqualByComparingTo(ANNUAL_AMOUNT_2025);
        }
    }

    // =========================================================================
    // TC-801-030: Total Tax Includes Class 2 NI
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-801-030: Total Tax Includes Class 2 NI")
    class TotalTaxIncludesClass2NI {

        @Test
        @DisplayName("total tax liability should include Class 2 NI")
        void totalTaxLiabilityShouldIncludeClass2NI() {
            // Given: Gross profit GBP 30,000
            BigDecimal grossProfit = new BigDecimal("30000");

            // When: Calculate combined tax liability
            TaxLiabilityResult result = taxLiabilityCalculator.calculate(grossProfit);

            // Then: Total should equal Income Tax + Class 4 NI + Class 2 NI
            BigDecimal expectedTotal = result.incomeTax()
                .add(result.niClass4())
                .add(result.niClass2());

            assertThat(result.totalLiability())
                .as("Total liability should include Class 2 NI (GBP 182.00)")
                .isEqualByComparingTo(expectedTotal);

            // Verify Class 2 NI is specifically included
            assertThat(result.niClass2())
                .as("Class 2 NI component should be GBP 182.00")
                .isEqualByComparingTo(ANNUAL_AMOUNT_2025);
        }
    }

    // =========================================================================
    // TC-801-031: Total NI Property (Class 2 + Class 4)
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-801-031: Total NI Property (Class 2 + Class 4)")
    class TotalNIProperty {

        @Test
        @DisplayName("totalNI should correctly sum Class 2 and Class 4")
        void totalNIShouldSumClass2AndClass4() {
            // Given: Profit generating both NI types
            BigDecimal grossProfit = new BigDecimal("60000");

            // When: Calculate combined tax liability
            TaxLiabilityResult result = taxLiabilityCalculator.calculate(grossProfit);

            // Then: totalNI = niClass2 + niClass4
            BigDecimal expectedTotalNI = result.niClass2().add(result.niClass4());

            assertThat(result.totalNI())
                .as("Total NI should equal Class 2 + Class 4")
                .isEqualByComparingTo(expectedTotalNI);
        }

        @Test
        @DisplayName("totalNI should handle null Class 2 gracefully")
        void totalNIShouldHandleNullGracefully() {
            // Given: Low profit that might not trigger Class 2
            BigDecimal grossProfit = new BigDecimal("5000");

            // When: Calculate combined tax liability
            TaxLiabilityResult result = taxLiabilityCalculator.calculate(grossProfit);

            // Then: totalNI should be zero (no NI due)
            assertThat(result.totalNI())
                .as("Total NI should be zero when neither Class applies")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // =========================================================================
    // TC-801-040: Voluntary Class 2 NI Below Threshold
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-801-040: Voluntary Class 2 NI Below Threshold")
    class VoluntaryClass2NIBelowThreshold {

        @Test
        @DisplayName("user can opt into Class 2 NI when below threshold")
        void userCanOptIntoClass2NIBelowThreshold() {
            // Given: Gross profit GBP 5,000 (below threshold GBP 6,845)
            BigDecimal grossProfit = new BigDecimal("5000");
            boolean voluntary = true;

            // When: Calculate with voluntary option enabled
            Class2NICalculationResult result = calculator.calculate(grossProfit, voluntary);

            // Then: Class 2 NI should apply
            assertThat(result.totalNI())
                .as("Voluntary Class 2 NI should be GBP 182.00")
                .isEqualByComparingTo(ANNUAL_AMOUNT_2025);

            assertThat(result.isMandatory())
                .as("Class 2 NI should NOT be mandatory when voluntary")
                .isFalse();

            assertThat(result.isVoluntary())
                .as("Class 2 NI should be marked as voluntary")
                .isTrue();

            assertThat(result.weeksLiable())
                .as("Weeks liable should be 52 for voluntary contribution")
                .isEqualTo(52);
        }

        @ParameterizedTest
        @DisplayName("voluntary Class 2 NI should work for various low profit levels")
        @CsvSource({
            "0, 182.00",
            "1000, 182.00",
            "3000, 182.00",
            "5000, 182.00",
            "6000, 182.00",
            "6844, 182.00"
        })
        void voluntaryClass2NIShouldWorkForVariousProfitLevels(String profit, String expectedNI) {
            // Given: Various profit levels below threshold
            BigDecimal grossProfit = new BigDecimal(profit);

            // When: Calculate with voluntary option
            Class2NICalculationResult result = calculator.calculate(grossProfit, true);

            // Then: Class 2 NI should be GBP 182.00
            assertThat(result.totalNI())
                .as("Voluntary Class 2 NI for profit GBP %s should be GBP %s", profit, expectedNI)
                .isEqualByComparingTo(new BigDecimal(expectedNI));

            assertThat(result.isVoluntary())
                .as("Should be marked as voluntary")
                .isTrue();
        }
    }

    // =========================================================================
    // TC-801-050: Rate Verification (GBP 3.50/week x 52 = GBP 182.00)
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-801-050: Rate Verification")
    class RateVerification {

        @Test
        @DisplayName("annual amount should equal weekly rate times 52 weeks")
        void annualAmountShouldEqualWeeklyRateTimes52() {
            // Given: Profit above threshold
            BigDecimal grossProfit = new BigDecimal("10000");

            // When: Calculate Class 2 NI
            Class2NICalculationResult result = calculator.calculate(grossProfit);

            // Then: Verify rate calculation
            BigDecimal calculatedAnnual = result.weeklyRate()
                .multiply(new BigDecimal("52"));

            assertThat(result.totalNI())
                .as("Total NI should equal GBP 3.50 x 52 = GBP 182.00")
                .isEqualByComparingTo(calculatedAnnual);

            assertThat(result.weeklyRate())
                .as("Weekly rate should be GBP 3.50")
                .isEqualByComparingTo(WEEKLY_RATE_2025);

            assertThat(result.totalNI())
                .as("Annual amount should be GBP 182.00")
                .isEqualByComparingTo(ANNUAL_AMOUNT_2025);
        }

        @Test
        @DisplayName("2024/25 rates should use GBP 3.45/week = GBP 179.40/year")
        void previousYearRatesShouldBeDifferent() {
            // Given: Calculator for 2024/25
            NationalInsuranceClass2Calculator calc2024 = new NationalInsuranceClass2Calculator(TAX_YEAR_2024);
            BigDecimal grossProfit = new BigDecimal("10000");

            // When: Calculate Class 2 NI for 2024/25
            Class2NICalculationResult result = calc2024.calculate(grossProfit);

            // Then: Should use 2024/25 rates
            assertThat(result.weeklyRate())
                .as("2024/25 weekly rate should be GBP 3.45")
                .isEqualByComparingTo(WEEKLY_RATE_2024);

            assertThat(result.totalNI())
                .as("2024/25 annual amount should be GBP 179.40")
                .isEqualByComparingTo(ANNUAL_AMOUNT_2024);
        }
    }

    // =========================================================================
    // TC-801-060: TaxSummaryViewModel Properties for Class 2
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-801-060: TaxLiabilityResult Contains Class 2 Details")
    class TaxLiabilityResultClass2Details {

        @Test
        @DisplayName("TaxLiabilityResult should contain all Class 2 details")
        void taxLiabilityResultShouldContainAllClass2Details() {
            // Given: Profit above threshold
            BigDecimal grossProfit = new BigDecimal("10000");

            // When: Calculate combined tax liability
            TaxLiabilityResult result = taxLiabilityCalculator.calculate(grossProfit);

            // Then: All Class 2 details should be populated
            Class2NICalculationResult details = result.niClass2Details();

            assertThat(details.grossProfit())
                .as("Gross profit should be captured")
                .isEqualByComparingTo(grossProfit);

            assertThat(details.smallProfitsThreshold())
                .as("Small Profits Threshold should be GBP 6,845")
                .isEqualByComparingTo(SMALL_PROFITS_THRESHOLD_2025);

            assertThat(details.weeklyRate())
                .as("Weekly rate should be GBP 3.50")
                .isEqualByComparingTo(WEEKLY_RATE_2025);

            assertThat(details.weeksLiable())
                .as("Weeks liable should be 52")
                .isEqualTo(52);

            assertThat(details.totalNI())
                .as("Total NI should be GBP 182.00")
                .isEqualByComparingTo(ANNUAL_AMOUNT_2025);

            assertThat(details.isMandatory())
                .as("isMandatory should be true")
                .isTrue();

            assertThat(details.isVoluntary())
                .as("isVoluntary should be false")
                .isFalse();
        }
    }

    // =========================================================================
    // Additional Integration Scenarios
    // =========================================================================
    @Nested
    @DisplayName("Additional Integration Scenarios")
    class AdditionalIntegrationScenarios {

        @Test
        @DisplayName("Class 2 NI should remain flat rate regardless of income level")
        void class2NIShouldRemainFlatRateRegardlessOfIncome() {
            // Given: Very high income scenarios
            BigDecimal[] highProfits = {
                new BigDecimal("100000"),
                new BigDecimal("500000"),
                new BigDecimal("1000000")
            };

            for (BigDecimal profit : highProfits) {
                // When: Calculate Class 2 NI
                Class2NICalculationResult result = calculator.calculate(profit);

                // Then: Class 2 NI should always be GBP 182.00
                assertThat(result.totalNI())
                    .as("Class 2 NI for profit GBP %s should still be GBP 182.00", profit)
                    .isEqualByComparingTo(ANNUAL_AMOUNT_2025);
            }
        }

        @Test
        @DisplayName("effective rate should decrease as profit increases")
        void effectiveRateShouldDecreaseAsProfitIncreases() {
            // Given: Different profit levels
            BigDecimal profit10k = new BigDecimal("10000");
            BigDecimal profit20k = new BigDecimal("20000");

            // When: Calculate effective rates
            Class2NICalculationResult result10k = calculator.calculate(profit10k);
            Class2NICalculationResult result20k = calculator.calculate(profit20k);

            // Then: Effective rate should be lower for higher profits
            assertThat(result10k.effectiveRate())
                .as("Effective rate for GBP 10,000 should be 1.82%")
                .isEqualByComparingTo(new BigDecimal("1.82"));

            assertThat(result20k.effectiveRate())
                .as("Effective rate for GBP 20,000 should be 0.91%")
                .isEqualByComparingTo(new BigDecimal("0.91"));

            assertThat(result10k.effectiveRate())
                .as("Higher profit should have lower effective rate")
                .isGreaterThan(result20k.effectiveRate());
        }

        @Test
        @DisplayName("isApplicable should return true when NI is due")
        void isApplicableShouldReturnTrueWhenNIIsDue() {
            // Given/When: Calculate for various scenarios
            Class2NICalculationResult mandatory = calculator.calculate(new BigDecimal("10000"));
            Class2NICalculationResult voluntary = calculator.calculate(new BigDecimal("5000"), true);
            Class2NICalculationResult neither = calculator.calculate(new BigDecimal("5000"), false);

            // Then
            assertThat(mandatory.isApplicable())
                .as("Mandatory Class 2 NI should be applicable")
                .isTrue();

            assertThat(voluntary.isApplicable())
                .as("Voluntary Class 2 NI should be applicable")
                .isTrue();

            assertThat(neither.isApplicable())
                .as("No Class 2 NI should not be applicable")
                .isFalse();
        }

        @ParameterizedTest
        @DisplayName("boundary tests for various threshold scenarios")
        @MethodSource("uk.selfemploy.core.calculator.NationalInsuranceClass2CalculatorIntegrationTest#boundaryTestCases")
        void boundaryTestsForVariousThresholdScenarios(
                String scenario,
                BigDecimal profit,
                BigDecimal expectedNI,
                boolean expectedMandatory
        ) {
            // When
            Class2NICalculationResult result = calculator.calculate(profit);

            // Then
            assertThat(result.totalNI())
                .as("Scenario: %s - Expected NI: GBP %s", scenario, expectedNI)
                .isEqualByComparingTo(expectedNI);

            assertThat(result.isMandatory())
                .as("Scenario: %s - Expected mandatory: %s", scenario, expectedMandatory)
                .isEqualTo(expectedMandatory);
        }
    }

    // Method source for parameterized boundary tests
    static Stream<Arguments> boundaryTestCases() {
        return Stream.of(
            Arguments.of("Below threshold (GBP 6,844)", new BigDecimal("6844"), BigDecimal.ZERO, false),
            Arguments.of("At threshold (GBP 6,845)", new BigDecimal("6845"), BigDecimal.ZERO, false),
            Arguments.of("Just above threshold (GBP 6,846)", new BigDecimal("6846"), new BigDecimal("182.00"), true),
            Arguments.of("One penny above (GBP 6,845.01)", new BigDecimal("6845.01"), new BigDecimal("182.00"), true),
            Arguments.of("Zero profit", BigDecimal.ZERO, BigDecimal.ZERO, false),
            Arguments.of("Negative profit (loss)", new BigDecimal("-5000"), BigDecimal.ZERO, false)
        );
    }

    // =========================================================================
    // Edge Cases and Error Handling
    // =========================================================================
    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("null profit should be treated as zero")
        void nullProfitShouldBeTreatedAsZero() {
            // When
            Class2NICalculationResult result = calculator.calculate(null);

            // Then
            assertThat(result.totalNI())
                .as("Null profit should result in zero NI")
                .isEqualByComparingTo(BigDecimal.ZERO);

            assertThat(result.grossProfit())
                .as("Gross profit should be normalized to zero")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("negative profit should not trigger Class 2 NI")
        void negativeProfitShouldNotTriggerClass2NI() {
            // When
            Class2NICalculationResult result = calculator.calculate(new BigDecimal("-10000"));

            // Then
            assertThat(result.totalNI())
                .as("Negative profit should result in zero NI")
                .isEqualByComparingTo(BigDecimal.ZERO);

            assertThat(result.isMandatory())
                .as("Class 2 NI should not be mandatory for losses")
                .isFalse();
        }

        @Test
        @DisplayName("voluntary flag should be ignored when profits above threshold")
        void voluntaryFlagShouldBeIgnoredAboveThreshold() {
            // Given: Profit above threshold with voluntary flag
            BigDecimal grossProfit = new BigDecimal("10000");

            // When: Calculate with voluntary flag
            Class2NICalculationResult result = calculator.calculate(grossProfit, true);

            // Then: Should be mandatory, not voluntary
            assertThat(result.isMandatory())
                .as("Class 2 NI should be mandatory above threshold regardless of voluntary flag")
                .isTrue();

            assertThat(result.isVoluntary())
                .as("Voluntary flag should be ignored when mandatory")
                .isFalse();
        }

        @Test
        @DisplayName("effective rate should be zero for zero profit")
        void effectiveRateShouldBeZeroForZeroProfit() {
            // When
            Class2NICalculationResult result = calculator.calculate(BigDecimal.ZERO);

            // Then
            assertThat(result.effectiveRate())
                .as("Effective rate should be zero when profit is zero")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
