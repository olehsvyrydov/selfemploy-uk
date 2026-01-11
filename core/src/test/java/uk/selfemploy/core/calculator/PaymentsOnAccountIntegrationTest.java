package uk.selfemploy.core.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Payments on Account (POA) calculations.
 *
 * These tests cover the QA test cases from /rob's specifications (SE-505).
 * Tests are organized by priority (P0 Critical first, then P1 Important, then P2).
 *
 * Test Case Coverage:
 * - TC-505-001 to TC-505-015 from rob-qa-SE-505-SE-506-SE-501.md
 *
 * @see uk.selfemploy.core.calculator.PaymentsOnAccountCalculator
 */
@DisplayName("Payments on Account Integration Tests (SE-505)")
class PaymentsOnAccountIntegrationTest {

    private PaymentsOnAccountCalculator calculator;

    // Test constants
    private static final int TAX_YEAR_2025 = 2025;

    @BeforeEach
    void setUp() {
        calculator = new PaymentsOnAccountCalculator();
    }

    // ===== P0 Critical Tests =====

    @Nested
    @DisplayName("P0 Critical: Core POA Logic")
    class P0CriticalTests {

        @Test
        @DisplayName("TC-505-001: POA calculated when liability exceeds 1,000 threshold")
        void tc505_001_shouldCalculatePoaWhenLiabilityExceedsThreshold() {
            // Given: Previous year tax liability of 2,500 (above threshold)
            BigDecimal previousYearLiability = new BigDecimal("2500.00");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            );

            // Then: POA is required with 1,250.00 for each payment (50% of 2,500)
            assertThat(result.requiresPoa()).isTrue();
            assertThat(result.firstPayment()).isEqualByComparingTo(new BigDecimal("1250.00"));
            assertThat(result.secondPayment()).isEqualByComparingTo(new BigDecimal("1250.00"));
            assertThat(result.totalPoaPayments()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(result.exemptionReason()).isNull();
        }

        @Test
        @DisplayName("TC-505-002: POA NOT required when liability is below 1,000 threshold")
        void tc505_002_shouldNotRequirePoaWhenBelowThreshold() {
            // Given: Previous year tax liability of 950 (below threshold)
            BigDecimal previousYearLiability = new BigDecimal("950.00");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            );

            // Then: POA is NOT required
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.exemptionReason())
                .isEqualTo(PaymentsOnAccountResult.ExemptionReason.BELOW_THRESHOLD);
            assertThat(result.firstPayment()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.secondPayment()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("TC-505-005: PAYE Exemption when >80% income taxed at source")
        void tc505_005_shouldExemptWhenPayeExceeds80Percent() {
            // Given: Liability 5,000 with 85% PAYE income
            BigDecimal previousYearLiability = new BigDecimal("5000.00");
            boolean isFirstYear = false;
            BigDecimal payePercentage = new BigDecimal("85");

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            );

            // Then: POA NOT required due to PAYE exemption
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.exemptionReason())
                .isEqualTo(PaymentsOnAccountResult.ExemptionReason.PAYE_EXCEEDS_80_PERCENT);
            assertThat(result.exemptionDescription())
                .isEqualTo("More than 80% of income taxed at source");
        }

        @Test
        @DisplayName("TC-505-007: First year exemption - no previous year liability")
        void tc505_007_shouldExemptFirstYearUsers() {
            // Given: First year user with high liability
            BigDecimal previousYearLiability = new BigDecimal("10000.00");
            boolean isFirstYear = true;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            );

            // Then: POA NOT required for first year
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.exemptionReason())
                .isEqualTo(PaymentsOnAccountResult.ExemptionReason.FIRST_YEAR);
            assertThat(result.exemptionDescription())
                .isEqualTo("First year of self-employment");
        }

        @Test
        @DisplayName("TC-505-008: POA deadlines are 31 January and 31 July of correct year")
        void tc505_008_shouldCalculateCorrectDeadlines() {
            // Given: Tax year 2025/26, POA required
            BigDecimal previousYearLiability = new BigDecimal("5000.00");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            );

            // Then: For tax year 2025/26:
            // - First deadline: 31 January 2027
            // - Second deadline: 31 July 2027
            assertThat(result.requiresPoa()).isTrue();
            assertThat(result.firstPaymentDeadline())
                .isEqualTo(LocalDate.of(2027, 1, 31));
            assertThat(result.secondPaymentDeadline())
                .isEqualTo(LocalDate.of(2027, 7, 31));
        }

        @Test
        @DisplayName("TC-505-009: Positive balancing payment when current year exceeds POAs paid")
        void tc505_009_shouldCalculatePositiveBalancingPayment() {
            // Given: Current year liability 6,000; POAs paid 4,000
            BigDecimal currentYearLiability = new BigDecimal("6000.00");
            BigDecimal poasPaid = new BigDecimal("4000.00");

            // When
            BigDecimal balancingPayment = calculator.calculateBalancingPayment(
                currentYearLiability, poasPaid
            );

            // Then: Balancing payment = 2,000.00 (user owes more)
            assertThat(balancingPayment).isEqualByComparingTo(new BigDecimal("2000.00"));
        }

        @Test
        @DisplayName("TC-505-010: Negative balancing payment (refund) when POAs exceed liability")
        void tc505_010_shouldCalculateRefundWhenPoasExceedLiability() {
            // Given: Current year liability 4,000; POAs paid 6,000
            BigDecimal currentYearLiability = new BigDecimal("4000.00");
            BigDecimal poasPaid = new BigDecimal("6000.00");

            // When
            BigDecimal balancingPayment = calculator.calculateBalancingPayment(
                currentYearLiability, poasPaid
            );

            // Then: Balancing payment = -2,000.00 (refund due)
            assertThat(balancingPayment).isEqualByComparingTo(new BigDecimal("-2000.00"));
        }
    }

    // ===== P1 Important Tests =====

    @Nested
    @DisplayName("P1 Important: Boundary and Edge Cases")
    class P1ImportantTests {

        @Test
        @DisplayName("TC-505-003: Exactly 1,000 threshold - POA NOT required")
        void tc505_003_shouldNotRequirePoaAtExactThreshold() {
            // Given: Liability exactly at threshold
            BigDecimal previousYearLiability = new BigDecimal("1000.00");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            );

            // Then: POA NOT required (must EXCEED threshold, not equal)
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.exemptionReason())
                .isEqualTo(PaymentsOnAccountResult.ExemptionReason.BELOW_THRESHOLD);
        }

        @Test
        @DisplayName("TC-505-004: Just above threshold (1,001) - POA IS required")
        void tc505_004_shouldRequirePoaJustAboveThreshold() {
            // Given: Liability 1 pound above threshold
            BigDecimal previousYearLiability = new BigDecimal("1001.00");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            );

            // Then: POA IS required; each payment = 500.50
            assertThat(result.requiresPoa()).isTrue();
            assertThat(result.firstPayment()).isEqualByComparingTo(new BigDecimal("500.50"));
            assertThat(result.secondPayment()).isEqualByComparingTo(new BigDecimal("500.50"));
        }

        @Test
        @DisplayName("TC-505-006: Exactly 80% PAYE - POA IS required (not above)")
        void tc505_006_shouldRequirePoaAtExactly80PercentPaye() {
            // Given: Exactly 80% PAYE (not above)
            BigDecimal previousYearLiability = new BigDecimal("5000.00");
            boolean isFirstYear = false;
            BigDecimal payePercentage = new BigDecimal("80");

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            );

            // Then: POA IS required (exemption only for >80%, not >=80%)
            assertThat(result.requiresPoa()).isTrue();
            assertThat(result.exemptionReason()).isNull();
        }

        @Test
        @DisplayName("TC-505-011: Zero balancing payment when POAs exactly match liability")
        void tc505_011_shouldCalculateZeroBalancingPaymentWhenExactMatch() {
            // Given: Current year liability equals POAs paid
            BigDecimal currentYearLiability = new BigDecimal("4000.00");
            BigDecimal poasPaid = new BigDecimal("4000.00");

            // When
            BigDecimal balancingPayment = calculator.calculateBalancingPayment(
                currentYearLiability, poasPaid
            );

            // Then: Balancing payment = 0.00
            assertThat(balancingPayment).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("TC-505-013: POA amounts rounded to 2 decimal places (HALF_UP)")
        void tc505_013_shouldRoundPoaAmountsCorrectly() {
            // Given: Odd liability amount that requires rounding
            BigDecimal previousYearLiability = new BigDecimal("3333.33");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            );

            // Then: Each POA = 1,666.67 (properly rounded with HALF_UP)
            assertThat(result.requiresPoa()).isTrue();
            assertThat(result.firstPayment()).isEqualByComparingTo(new BigDecimal("1666.67"));
            assertThat(result.secondPayment()).isEqualByComparingTo(new BigDecimal("1666.67"));

            // Verify scale is 2 decimal places
            assertThat(result.firstPayment().scale()).isLessThanOrEqualTo(2);
            assertThat(result.secondPayment().scale()).isLessThanOrEqualTo(2);
        }
    }

    // ===== P2 Nice-to-have Tests =====

    @Nested
    @DisplayName("P2 Nice-to-have: Exemption Priority and Input Validation")
    class P2NiceToHaveTests {

        @Test
        @DisplayName("TC-505-014: Exemption priority - First Year > PAYE > Below Threshold")
        void tc505_014_shouldPrioritizeExemptionReasons() {
            // Given: All three exemption conditions apply
            BigDecimal previousYearLiability = new BigDecimal("500.00"); // Below threshold
            boolean isFirstYear = true; // First year
            BigDecimal payePercentage = new BigDecimal("90"); // PAYE > 80%

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            );

            // Then: First Year takes priority (highest priority reason)
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.exemptionReason())
                .isEqualTo(PaymentsOnAccountResult.ExemptionReason.FIRST_YEAR);
        }

        @Test
        @DisplayName("TC-505-015a: Null liability treated as zero")
        void tc505_015a_shouldTreatNullLiabilityAsZero() {
            // Given: Null liability
            BigDecimal previousYearLiability = null;
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            );

            // Then: Treated as 0, so below threshold
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.exemptionReason())
                .isEqualTo(PaymentsOnAccountResult.ExemptionReason.BELOW_THRESHOLD);
        }

        @Test
        @DisplayName("TC-505-015b: Negative liability treated as zero")
        void tc505_015b_shouldTreatNegativeLiabilityAsZero() {
            // Given: Negative liability
            BigDecimal previousYearLiability = new BigDecimal("-500.00");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            );

            // Then: Treated as 0, so below threshold
            assertThat(result.requiresPoa()).isFalse();
        }

        @Test
        @DisplayName("TC-505-015c: PAYE > 100 throws IllegalArgumentException")
        void tc505_015c_shouldRejectInvalidPayePercentage() {
            // Given: Invalid PAYE percentage
            BigDecimal previousYearLiability = new BigDecimal("5000.00");
            boolean isFirstYear = false;
            BigDecimal payePercentage = new BigDecimal("101");

            // Then: Should throw exception
            assertThatThrownBy(() -> calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PAYE percentage must be between 0 and 100");
        }
    }

    // ===== Additional Integration Scenarios =====

    @Nested
    @DisplayName("Integration Scenarios: Real-world Use Cases")
    class IntegrationScenarios {

        @Test
        @DisplayName("Typical freelancer with 5,000 liability")
        void typicalFreelancerScenario() {
            // Given: Typical freelancer, not first year, no PAYE
            BigDecimal previousYearLiability = new BigDecimal("5000.00");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            );

            // Then
            assertThat(result.requiresPoa()).isTrue();
            assertThat(result.firstPayment()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(result.secondPayment()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(result.taxYear()).isEqualTo(TAX_YEAR_2025);
        }

        @Test
        @DisplayName("Side-hustle with 50% PAYE income")
        void sideHustleWithPayeScenario() {
            // Given: Person with day job (50% PAYE) and side business
            BigDecimal previousYearLiability = new BigDecimal("3000.00");
            boolean isFirstYear = false;
            BigDecimal payePercentage = new BigDecimal("50");

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            );

            // Then: POA still required (PAYE < 80%)
            assertThat(result.requiresPoa()).isTrue();
            assertThat(result.firstPayment()).isEqualByComparingTo(new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("High earner with significant liability")
        void highEarnerScenario() {
            // Given: High earner with 50,000 liability
            BigDecimal previousYearLiability = new BigDecimal("50000.00");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, TAX_YEAR_2025
            );

            // Then
            assertThat(result.requiresPoa()).isTrue();
            assertThat(result.firstPayment()).isEqualByComparingTo(new BigDecimal("25000.00"));
            assertThat(result.secondPayment()).isEqualByComparingTo(new BigDecimal("25000.00"));
            assertThat(result.totalPoaPayments()).isEqualByComparingTo(new BigDecimal("50000.00"));
        }

        @Test
        @DisplayName("Previous year 2024/25 has correct 2026 deadlines")
        void previousTaxYearDeadlinesScenario() {
            // Given: Tax year 2024/25
            BigDecimal previousYearLiability = new BigDecimal("5000.00");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;
            int taxYear2024 = 2024;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, taxYear2024
            );

            // Then: For 2024/25, deadlines are 31 Jan 2026 and 31 Jul 2026
            assertThat(result.firstPaymentDeadline())
                .isEqualTo(LocalDate.of(2026, 1, 31));
            assertThat(result.secondPaymentDeadline())
                .isEqualTo(LocalDate.of(2026, 7, 31));
        }

        @Test
        @DisplayName("Balancing payment calculation end-to-end")
        void balancingPaymentEndToEndScenario() {
            // Scenario: User had 4,000 liability last year, paid 4,000 in POAs
            // This year liability is 5,500

            // Step 1: Calculate this year's POA for next year
            BigDecimal thisYearLiability = new BigDecimal("5500.00");
            PaymentsOnAccountResult nextYearPoa = calculator.calculate(
                thisYearLiability, false, BigDecimal.ZERO, TAX_YEAR_2025
            );

            // Step 2: Calculate balancing payment for this year
            BigDecimal poasPaidThisYear = new BigDecimal("4000.00"); // Based on last year's POA
            BigDecimal balancingPayment = calculator.calculateBalancingPayment(
                thisYearLiability, poasPaidThisYear
            );

            // Then
            assertThat(balancingPayment).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(nextYearPoa.firstPayment()).isEqualByComparingTo(new BigDecimal("2750.00"));
        }
    }
}
