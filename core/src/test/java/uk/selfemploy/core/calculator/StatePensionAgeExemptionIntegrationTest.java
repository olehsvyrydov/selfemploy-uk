package uk.selfemploy.core.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SE-808: State Pension Age Exemption.
 *
 * Tests the complete exemption flow across all calculator components.
 * Verifies that pensioners are correctly exempted from Class 4 NI while
 * still paying Income Tax and optionally Class 2 NI.
 *
 * Test Cases: TC-SE808-001 through TC-SE808-014
 *
 * @see <a href="https://www.gov.uk/state-pension-age">HMRC State Pension Age</a>
 * @see <a href="https://www.gov.uk/self-employed-national-insurance-rates">HMRC NI Rates</a>
 */
@Tag("integration")
@DisplayName("State Pension Age Exemption Integration Tests (SE-808)")
class StatePensionAgeExemptionIntegrationTest {

    private static final int STATE_PENSION_AGE = 66;

    // Tax year 2025/26 starts 6 April 2025
    // To be exempt, person must be 66 on 6 April 2025
    // So they must be born on or before 6 April 1959

    private NationalInsuranceCalculator niCalculator2024;
    private NationalInsuranceCalculator niCalculator2025;
    private TaxLiabilityCalculator taxCalculator2024;
    private TaxLiabilityCalculator taxCalculator2025;

    @BeforeEach
    void setUp() {
        niCalculator2024 = new NationalInsuranceCalculator(2024);
        niCalculator2025 = new NationalInsuranceCalculator(2025);
        taxCalculator2024 = new TaxLiabilityCalculator(2024);
        taxCalculator2025 = new TaxLiabilityCalculator(2025);
    }

    @Nested
    @DisplayName("TC-SE808-001: Person Aged 65 at Tax Year Start - NOT Exempt")
    class PersonAged65NotExempt {

        @Test
        @DisplayName("person born 6 April 1959 is 65 on 6 April 2024 - NOT exempt")
        void personBorn1959Apr6Is65On2024Apr6NotExempt() {
            // Tax year 2024/25 starts 6 April 2024
            // Person born 6 April 1959 turns 65 on 6 April 2024
            LocalDate dateOfBirth = LocalDate.of(1959, 4, 6);
            BigDecimal profit = new BigDecimal("60000");

            NICalculationResult result = niCalculator2024.calculate(profit, dateOfBirth);

            assertThat(result.isExempt())
                .as("Person aged 65 at tax year start should NOT be exempt")
                .isFalse();
            assertThat(result.totalNI())
                .as("Normal NI should be calculated")
                .isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("person born 7 April 1959 is still 64 on 6 April 2024 - NOT exempt")
        void personBorn1959Apr7Is64On2024Apr6NotExempt() {
            LocalDate dateOfBirth = LocalDate.of(1959, 4, 7);
            BigDecimal profit = new BigDecimal("60000");

            NICalculationResult result = niCalculator2024.calculate(profit, dateOfBirth);

            assertThat(result.isExempt()).isFalse();
            assertThat(result.exemptionReason()).isNull();
        }
    }

    @Nested
    @DisplayName("TC-SE808-002: Person Aged 66 at Tax Year Start - IS Exempt")
    class PersonAged66IsExempt {

        @Test
        @DisplayName("person born 5 April 1958 is 66 on 6 April 2024 - IS exempt")
        void personBorn1958Apr5Is66On2024Apr6IsExempt() {
            LocalDate dateOfBirth = LocalDate.of(1958, 4, 5);
            BigDecimal profit = new BigDecimal("60000");

            NICalculationResult result = niCalculator2024.calculate(profit, dateOfBirth);

            assertThat(result.isExempt())
                .as("Person aged 66 at tax year start should be exempt")
                .isTrue();
            assertThat(result.exemptionReason())
                .as("Should have correct exemption reason")
                .isEqualTo("State Pension Age reached before tax year start");
            assertThat(result.totalNI())
                .as("NI should be zero for exempt person")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("TC-SE808-003: Person Aged 67+ at Tax Year Start - IS Exempt")
    class PersonAged67PlusIsExempt {

        @Test
        @DisplayName("person born 1 January 1950 is 74+ - IS exempt")
        void personBorn1950Jan1IsExempt() {
            LocalDate dateOfBirth = LocalDate.of(1950, 1, 1);
            BigDecimal profit = new BigDecimal("100000");

            NICalculationResult result = niCalculator2024.calculate(profit, dateOfBirth);

            assertThat(result.isExempt()).isTrue();
            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @ParameterizedTest
        @ValueSource(ints = {1940, 1945, 1950, 1955})
        @DisplayName("should be exempt for various older birth years")
        void shouldBeExemptForVariousOlderBirthYears(int birthYear) {
            LocalDate dateOfBirth = LocalDate.of(birthYear, 6, 15);
            BigDecimal profit = new BigDecimal("50000");

            NICalculationResult result = niCalculator2024.calculate(profit, dateOfBirth);

            assertThat(result.isExempt())
                .as("Person born in %d should be exempt", birthYear)
                .isTrue();
        }
    }

    @Nested
    @DisplayName("TC-SE808-004: Person Turning 66 DURING Tax Year - NOT Exempt")
    class PersonTurning66DuringTaxYearNotExempt {

        @Test
        @DisplayName("person born 7 April 1958 turns 66 one day after tax year start - NOT exempt")
        void personTurning66OneDayAfterTaxYearStartNotExempt() {
            // Tax year 2024/25 starts 6 April 2024
            // Person born 7 April 1958 turns 66 on 7 April 2024 (one day late)
            LocalDate dateOfBirth = LocalDate.of(1958, 4, 7);
            BigDecimal profit = new BigDecimal("30000");

            NICalculationResult result = niCalculator2024.calculate(profit, dateOfBirth);

            assertThat(result.isExempt())
                .as("Person turning 66 during tax year should NOT be exempt")
                .isFalse();
        }

        @Test
        @DisplayName("person turning 66 in December of tax year - NOT exempt")
        void personTurning66InDecemberNotExempt() {
            // Person born December 1958 - turns 66 in December 2024 (during tax year)
            LocalDate dateOfBirth = LocalDate.of(1958, 12, 15);
            BigDecimal profit = new BigDecimal("50000");

            NICalculationResult result = niCalculator2024.calculate(profit, dateOfBirth);

            assertThat(result.isExempt()).isFalse();
        }
    }

    @Nested
    @DisplayName("TC-SE808-005: Birthday Exactly on Tax Year Start - IS Exempt")
    class BirthdayExactlyOnTaxYearStart {

        @Test
        @DisplayName("person born 6 April 1958 turns 66 exactly on 6 April 2024 - IS exempt")
        void personTurning66ExactlyOnTaxYearStartIsExempt() {
            // Person turns 66 on the exact day tax year starts
            LocalDate dateOfBirth = LocalDate.of(1958, 4, 6);
            BigDecimal profit = new BigDecimal("50000");

            NICalculationResult result = niCalculator2024.calculate(profit, dateOfBirth);

            assertThat(result.isExempt())
                .as("Person turning 66 on tax year start date should be exempt")
                .isTrue();
            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("TC-SE808-006: Null Date of Birth - NOT Exempt")
    class NullDateOfBirthNotExempt {

        @Test
        @DisplayName("null date of birth should NOT be exempt")
        void nullDateOfBirthShouldNotBeExempt() {
            BigDecimal profit = new BigDecimal("60000");

            NICalculationResult result = niCalculator2024.calculate(profit, null);

            assertThat(result.isExempt())
                .as("Null DOB should default to not exempt")
                .isFalse();
            assertThat(result.totalNI())
                .as("Normal NI should be calculated")
                .isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("backward compatible calculate(profit) without DOB should NOT be exempt")
        void calculateWithoutDobShouldNotBeExempt() {
            BigDecimal profit = new BigDecimal("60000");

            NICalculationResult result = niCalculator2024.calculate(profit);

            assertThat(result.isExempt()).isFalse();
        }
    }

    @Nested
    @DisplayName("TC-SE808-007: Exempt Person Has Zero NI for All Profit Levels")
    class ExemptPersonZeroNiAllProfitLevels {

        private static final LocalDate PENSIONER_DOB = LocalDate.of(1950, 1, 1);

        @ParameterizedTest
        @CsvSource({
            "0",
            "12570",
            "50000",
            "100000",
            "1000000"
        })
        @DisplayName("exempt person should have zero NI at all profit levels")
        void exemptPersonShouldHaveZeroNiAtAllProfitLevels(String profitStr) {
            BigDecimal profit = new BigDecimal(profitStr);

            NICalculationResult result = niCalculator2024.calculate(profit, PENSIONER_DOB);

            assertThat(result.isExempt()).isTrue();
            assertThat(result.totalNI())
                .as("Exempt person with £%s profit should have £0 NI", profitStr)
                .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("TC-SE808-008: Non-Exempt Young Person - Normal NI Calculation")
    class NonExemptYoungPersonNormalNi {

        @Test
        @DisplayName("young person born 1990 should calculate normal NI")
        void youngPersonShouldCalculateNormalNi() {
            LocalDate dateOfBirth = LocalDate.of(1990, 1, 1);
            BigDecimal profit = new BigDecimal("30000");

            NICalculationResult result = niCalculator2024.calculate(profit, dateOfBirth);

            assertThat(result.isExempt()).isFalse();
            assertThat(result.totalNI())
                .as("Young person should have normal NI calculated")
                .isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("TC-SE808-009: Exemption Result Contains Breakdown")
    class ExemptionResultContainsBreakdown {

        @Test
        @DisplayName("exempt result should include full breakdown with zero values")
        void exemptResultShouldIncludeFullBreakdownWithZeroValues() {
            // DOB 5 April 1958 means person is exactly 66 years old on 6 April 2024
            LocalDate pensionerDob = LocalDate.of(1958, 4, 5);
            BigDecimal profit = new BigDecimal("60000");

            NICalculationResult result = niCalculator2024.calculate(profit, pensionerDob);

            assertThat(result.isExempt()).isTrue();
            assertThat(result.grossProfit())
                .as("Gross profit should be preserved")
                .isEqualByComparingTo(profit);
            assertThat(result.profitSubjectToNI())
                .as("Profit subject to NI should be zero for exempt person")
                .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.mainRateNI())
                .as("Main rate NI should be zero")
                .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.additionalRateNI())
                .as("Additional rate NI should be zero")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("TC-SE808-010: State Pension Age Constant")
    class StatePensionAgeConstant {

        @Test
        @DisplayName("STATE_PENSION_AGE constant should be 66")
        void statePensionAgeConstantShouldBe66() {
            assertThat(NationalInsuranceCalculator.STATE_PENSION_AGE)
                .as("State Pension Age should be 66")
                .isEqualTo(66);
        }

        @Test
        @DisplayName("exemption reason constant should be accessible")
        void exemptionReasonConstantShouldBeAccessible() {
            assertThat(NationalInsuranceCalculator.PENSION_AGE_EXEMPTION_REASON)
                .as("Exemption reason should be defined")
                .isNotNull()
                .isNotEmpty();
        }
    }

    @Nested
    @DisplayName("TC-SE808-011: Backward Compatibility - calculate(BigDecimal) Still Works")
    class BackwardCompatibility {

        @Test
        @DisplayName("calculate(grossProfit) without DOB should return valid result")
        void calculateWithoutDobShouldReturnValidResult() {
            BigDecimal profit = new BigDecimal("50000");

            NICalculationResult result = niCalculator2024.calculate(profit);

            assertThat(result).isNotNull();
            assertThat(result.isExempt()).isFalse();
            assertThat(result.totalNI()).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("TC-SE808-012: TaxLiabilityCalculator Integration")
    class TaxLiabilityCalculatorIntegration {

        @Test
        @DisplayName("TaxLiabilityCalculator should expose exemption status")
        void taxLiabilityCalculatorShouldExposeExemptionStatus() {
            LocalDate pensionerDob = LocalDate.of(1958, 1, 1);
            BigDecimal profit = new BigDecimal("60000");

            TaxLiabilityResult result = taxCalculator2024.calculate(profit, pensionerDob);

            assertThat(result.isClass4NIExempt())
                .as("TaxLiabilityResult should indicate Class 4 NI exemption")
                .isTrue();
            assertThat(result.class4ExemptionReason())
                .as("Should provide exemption reason")
                .isEqualTo("State Pension Age reached before tax year start");
        }

        @Test
        @DisplayName("TaxLiabilityCalculator should return zero Class 4 NI for pensioner")
        void taxLiabilityCalculatorShouldReturnZeroClass4NiForPensioner() {
            LocalDate pensionerDob = LocalDate.of(1958, 1, 1);
            BigDecimal profit = new BigDecimal("60000");

            TaxLiabilityResult result = taxCalculator2024.calculate(profit, pensionerDob);

            assertThat(result.niClass4())
                .as("Class 4 NI should be zero for pensioner")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("TaxLiabilityCalculator should NOT exempt young person")
        void taxLiabilityCalculatorShouldNotExemptYoungPerson() {
            LocalDate youngDob = LocalDate.of(1990, 6, 15);
            BigDecimal profit = new BigDecimal("60000");

            TaxLiabilityResult result = taxCalculator2024.calculate(profit, youngDob);

            assertThat(result.isClass4NIExempt()).isFalse();
            assertThat(result.class4ExemptionReason()).isNull();
            assertThat(result.niClass4()).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("TC-SE808-013: Pensioner Still Pays Income Tax")
    class PensionerStillPaysIncomeTax {

        @Test
        @DisplayName("pensioner should pay Income Tax but not Class 4 NI")
        void pensionerShouldPayIncomeTaxButNotClass4Ni() {
            LocalDate pensionerDob = LocalDate.of(1958, 1, 1);
            BigDecimal profit = new BigDecimal("50000");

            TaxLiabilityResult result = taxCalculator2024.calculate(profit, pensionerDob);

            assertThat(result.incomeTax())
                .as("Pensioner should still pay Income Tax")
                .isGreaterThan(BigDecimal.ZERO);
            assertThat(result.niClass4())
                .as("Pensioner should be exempt from Class 4 NI")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("pensioner Class 2 NI should follow normal rules")
        void pensionerClass2NiShouldFollowNormalRules() {
            LocalDate pensionerDob = LocalDate.of(1958, 1, 1);
            BigDecimal profit = new BigDecimal("50000"); // Above SPT

            TaxLiabilityResult result = taxCalculator2024.calculate(profit, pensionerDob);

            // Class 2 NI is still mandatory if above Small Profits Threshold
            assertThat(result.niClass2())
                .as("Pensioner should still pay Class 2 NI if above threshold")
                .isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("TC-SE808-014: Higher Net Profit for Pensioners")
    class HigherNetProfitForPensioners {

        @Test
        @DisplayName("pensioner should have higher net profit than non-pensioner")
        void pensionerShouldHaveHigherNetProfitThanNonPensioner() {
            BigDecimal profit = new BigDecimal("60000");
            LocalDate youngDob = LocalDate.of(1990, 6, 15);
            LocalDate pensionerDob = LocalDate.of(1958, 1, 1);

            TaxLiabilityResult youngResult = taxCalculator2024.calculate(profit, youngDob);
            TaxLiabilityResult pensionerResult = taxCalculator2024.calculate(profit, pensionerDob);

            assertThat(pensionerResult.netProfitAfterTax())
                .as("Pensioner should keep more money (no Class 4 NI)")
                .isGreaterThan(youngResult.netProfitAfterTax());

            // The savings should equal the Class 4 NI that the young person pays
            BigDecimal savings = pensionerResult.netProfitAfterTax()
                .subtract(youngResult.netProfitAfterTax());
            assertThat(savings)
                .as("Savings should equal Class 4 NI exempted")
                .isEqualByComparingTo(youngResult.niClass4());
        }

        @Test
        @DisplayName("pensioner should have lower effective tax rate")
        void pensionerShouldHaveLowerEffectiveTaxRate() {
            BigDecimal profit = new BigDecimal("60000");
            LocalDate youngDob = LocalDate.of(1990, 6, 15);
            LocalDate pensionerDob = LocalDate.of(1958, 1, 1);

            TaxLiabilityResult youngResult = taxCalculator2024.calculate(profit, youngDob);
            TaxLiabilityResult pensionerResult = taxCalculator2024.calculate(profit, pensionerDob);

            assertThat(pensionerResult.effectiveRate())
                .as("Pensioner should have lower effective rate")
                .isLessThan(youngResult.effectiveRate());
        }
    }

    @Nested
    @DisplayName("Cross-Tax-Year Exemption Consistency")
    class CrossTaxYearExemptionConsistency {

        @Test
        @DisplayName("exemption should be consistent across tax years")
        void exemptionShouldBeConsistentAcrossTaxYears() {
            // Person born 1950 should be exempt in both 2024 and 2025
            LocalDate pensionerDob = LocalDate.of(1950, 1, 1);
            BigDecimal profit = new BigDecimal("50000");

            NICalculationResult result2024 = niCalculator2024.calculate(profit, pensionerDob);
            NICalculationResult result2025 = niCalculator2025.calculate(profit, pensionerDob);

            assertThat(result2024.isExempt()).isTrue();
            assertThat(result2025.isExempt()).isTrue();
        }

        @Test
        @DisplayName("boundary dates should shift with tax year")
        void boundaryDatesShouldShiftWithTaxYear() {
            // Person born 6 April 1959:
            // - For 2024/25 (start 6 April 2024): turns 65 - NOT exempt
            // - For 2025/26 (start 6 April 2025): turns 66 - IS exempt
            LocalDate dateOfBirth = LocalDate.of(1959, 4, 6);
            BigDecimal profit = new BigDecimal("50000");

            NICalculationResult result2024 = niCalculator2024.calculate(profit, dateOfBirth);
            NICalculationResult result2025 = niCalculator2025.calculate(profit, dateOfBirth);

            assertThat(result2024.isExempt())
                .as("In 2024/25, person is 65 - NOT exempt")
                .isFalse();
            assertThat(result2025.isExempt())
                .as("In 2025/26, person is 66 - IS exempt")
                .isTrue();
        }
    }
}
