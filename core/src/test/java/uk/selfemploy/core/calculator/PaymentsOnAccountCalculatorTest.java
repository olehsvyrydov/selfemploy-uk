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
 * Tests for Payments on Account (POA) calculations.
 *
 * Acceptance Criteria (SE-505):
 * - AC-1: POA calculated when tax liability > £1,000
 * - AC-2: Each POA = 50% of previous year's tax liability
 * - AC-3: POA not required if >80% of income is taxed at source (PAYE)
 * - AC-4: POA displayed in tax summary view (UI concern - not tested here)
 * - AC-5: POA deadlines shown: 31 January, 31 July
 * - AC-6: Balancing payment calculated (total - POAs paid)
 */
@DisplayName("Payments on Account Calculator Tests")
class PaymentsOnAccountCalculatorTest {

    private PaymentsOnAccountCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PaymentsOnAccountCalculator();
    }

    @Nested
    @DisplayName("AC-1: POA Threshold Calculations")
    class PoaThresholdCalculations {

        @Test
        @DisplayName("should NOT require POA when liability is £500 (below £1,000 threshold)")
        void shouldNotRequirePoaWhenLiabilityIs500() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("500");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.firstPayment()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.secondPayment()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should NOT require POA when liability is exactly £1,000")
        void shouldNotRequirePoaWhenLiabilityIsExactly1000() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("1000");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then
            assertThat(result.requiresPoa()).isFalse();
        }

        @Test
        @DisplayName("should require POA when liability is £1,001 (just above threshold)")
        void shouldRequirePoaWhenLiabilityIsJustAboveThreshold() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("1001");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then
            assertThat(result.requiresPoa()).isTrue();
        }
    }

    @Nested
    @DisplayName("AC-2: POA Amount Calculations (50% of previous year)")
    class PoaAmountCalculations {

        @Test
        @DisplayName("should calculate POA as £1,000 each when liability is £2,000")
        void shouldCalculatePoaWhenLiabilityIs2000() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("2000");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then
            assertThat(result.requiresPoa()).isTrue();
            assertThat(result.firstPayment()).isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(result.secondPayment()).isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(result.totalPoaPayments()).isEqualByComparingTo(new BigDecimal("2000"));
        }

        @Test
        @DisplayName("should calculate POA as £2,500 each when liability is £5,000")
        void shouldCalculatePoaWhenLiabilityIs5000() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("5000");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then
            assertThat(result.requiresPoa()).isTrue();
            assertThat(result.firstPayment()).isEqualByComparingTo(new BigDecimal("2500"));
            assertThat(result.secondPayment()).isEqualByComparingTo(new BigDecimal("2500"));
            assertThat(result.totalPoaPayments()).isEqualByComparingTo(new BigDecimal("5000"));
        }

        @Test
        @DisplayName("should round POA to 2 decimal places for odd liability amounts")
        void shouldRoundPoaToTwoDecimalPlaces() {
            // Given: £3,333 liability should give £1,666.50 each
            BigDecimal previousYearLiability = new BigDecimal("3333");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then
            assertThat(result.firstPayment()).isEqualByComparingTo(new BigDecimal("1666.50"));
            assertThat(result.secondPayment()).isEqualByComparingTo(new BigDecimal("1666.50"));
        }
    }

    @Nested
    @DisplayName("AC-3: PAYE Exemption (>80% income taxed at source)")
    class PayeExemption {

        @Test
        @DisplayName("should NOT require POA when 85% of income is PAYE (above 80% threshold)")
        void shouldNotRequirePoaWhen85PercentIsPaye() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("5000");
            boolean isFirstYear = false;
            BigDecimal payePercentage = new BigDecimal("85");

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.exemptionReason()).isEqualTo(PaymentsOnAccountResult.ExemptionReason.PAYE_EXCEEDS_80_PERCENT);
            assertThat(result.firstPayment()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.secondPayment()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should NOT require POA when exactly 81% of income is PAYE")
        void shouldNotRequirePoaWhen81PercentIsPaye() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("5000");
            boolean isFirstYear = false;
            BigDecimal payePercentage = new BigDecimal("81");

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.exemptionReason()).isEqualTo(PaymentsOnAccountResult.ExemptionReason.PAYE_EXCEEDS_80_PERCENT);
        }

        @Test
        @DisplayName("should require POA when exactly 80% of income is PAYE (not above)")
        void shouldRequirePoaWhenExactly80PercentIsPaye() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("5000");
            boolean isFirstYear = false;
            BigDecimal payePercentage = new BigDecimal("80");

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then
            assertThat(result.requiresPoa()).isTrue();
            assertThat(result.exemptionReason()).isNull();
        }

        @Test
        @DisplayName("should require POA when 75% of income is PAYE (below 80% threshold)")
        void shouldRequirePoaWhen75PercentIsPaye() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("5000");
            boolean isFirstYear = false;
            BigDecimal payePercentage = new BigDecimal("75");

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then
            assertThat(result.requiresPoa()).isTrue();
            assertThat(result.exemptionReason()).isNull();
            assertThat(result.firstPayment()).isEqualByComparingTo(new BigDecimal("2500"));
            assertThat(result.secondPayment()).isEqualByComparingTo(new BigDecimal("2500"));
        }

        @Test
        @DisplayName("should require POA when 0% PAYE (fully self-employed)")
        void shouldRequirePoaWhenZeroPaye() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("5000");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then
            assertThat(result.requiresPoa()).isTrue();
        }
    }

    @Nested
    @DisplayName("First Year Exemption")
    class FirstYearExemption {

        @Test
        @DisplayName("should NOT require POA for first year users regardless of liability")
        void shouldNotRequirePoaForFirstYear() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("10000");
            boolean isFirstYear = true;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.exemptionReason()).isEqualTo(PaymentsOnAccountResult.ExemptionReason.FIRST_YEAR);
            assertThat(result.firstPayment()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.secondPayment()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("first year exemption should take precedence over PAYE exemption")
        void firstYearExemptionShouldTakePrecedence() {
            // Given: Both exemptions could apply
            BigDecimal previousYearLiability = new BigDecimal("10000");
            boolean isFirstYear = true;
            BigDecimal payePercentage = new BigDecimal("90"); // Also qualifies for PAYE exemption

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then: Should show first year as the reason
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.exemptionReason()).isEqualTo(PaymentsOnAccountResult.ExemptionReason.FIRST_YEAR);
        }
    }

    @Nested
    @DisplayName("AC-5: POA Deadlines (31 January, 31 July)")
    class PoaDeadlines {

        @Test
        @DisplayName("should return correct POA deadlines for tax year 2025/26")
        void shouldReturnCorrectDeadlinesFor2025TaxYear() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("5000");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, 2025
            );

            // Then: Tax year 2025/26 runs Apr 2025 - Apr 2026
            // POA1 due 31 January 2027 (of the following tax year)
            // POA2 due 31 July 2027
            assertThat(result.firstPaymentDeadline()).isEqualTo(LocalDate.of(2027, 1, 31));
            assertThat(result.secondPaymentDeadline()).isEqualTo(LocalDate.of(2027, 7, 31));
        }

        @Test
        @DisplayName("should return correct POA deadlines for tax year 2024/25")
        void shouldReturnCorrectDeadlinesFor2024TaxYear() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("5000");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, 2024
            );

            // Then: Tax year 2024/25 runs Apr 2024 - Apr 2025
            // POA1 due 31 January 2026
            // POA2 due 31 July 2026
            assertThat(result.firstPaymentDeadline()).isEqualTo(LocalDate.of(2026, 1, 31));
            assertThat(result.secondPaymentDeadline()).isEqualTo(LocalDate.of(2026, 7, 31));
        }

        @Test
        @DisplayName("should return null deadlines when POA not required")
        void shouldReturnNullDeadlinesWhenPoaNotRequired() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("500");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage, 2025
            );

            // Then
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.firstPaymentDeadline()).isNull();
            assertThat(result.secondPaymentDeadline()).isNull();
        }
    }

    @Nested
    @DisplayName("AC-6: Balancing Payment Calculations")
    class BalancingPaymentCalculations {

        @Test
        @DisplayName("should calculate zero balancing payment when current year equals previous")
        void shouldCalculateZeroBalancingPaymentWhenLiabilitySame() {
            // Given: Previous year £4,000, current year £4,000
            BigDecimal previousYearLiability = new BigDecimal("4000");
            BigDecimal currentYearLiability = new BigDecimal("4000");
            BigDecimal poasPaid = new BigDecimal("4000"); // 2 x £2,000

            // When
            BigDecimal balancingPayment = calculator.calculateBalancingPayment(
                currentYearLiability, poasPaid
            );

            // Then: £4,000 - £4,000 = £0
            assertThat(balancingPayment).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should calculate positive balancing payment when current year exceeds POAs")
        void shouldCalculatePositiveBalancingPayment() {
            // Given: Previous year £4,000, current year £6,000
            // POAs paid: £4,000 (based on previous year)
            BigDecimal currentYearLiability = new BigDecimal("6000");
            BigDecimal poasPaid = new BigDecimal("4000");

            // When
            BigDecimal balancingPayment = calculator.calculateBalancingPayment(
                currentYearLiability, poasPaid
            );

            // Then: £6,000 - £4,000 = £2,000 still owed
            assertThat(balancingPayment).isEqualByComparingTo(new BigDecimal("2000"));
        }

        @Test
        @DisplayName("should calculate negative balancing payment (refund) when POAs exceed liability")
        void shouldCalculateNegativeBalancingPaymentAsRefund() {
            // Given: Previous year £6,000, current year £4,000
            // POAs paid: £6,000 (based on previous year)
            BigDecimal currentYearLiability = new BigDecimal("4000");
            BigDecimal poasPaid = new BigDecimal("6000");

            // When
            BigDecimal balancingPayment = calculator.calculateBalancingPayment(
                currentYearLiability, poasPaid
            );

            // Then: £4,000 - £6,000 = -£2,000 (refund due)
            assertThat(balancingPayment).isEqualByComparingTo(new BigDecimal("-2000"));
        }

        @Test
        @DisplayName("should calculate balancing payment when no POAs were paid")
        void shouldCalculateBalancingPaymentWhenNoPoasPaid() {
            // Given: First year user, no POAs paid
            BigDecimal currentYearLiability = new BigDecimal("5000");
            BigDecimal poasPaid = BigDecimal.ZERO;

            // When
            BigDecimal balancingPayment = calculator.calculateBalancingPayment(
                currentYearLiability, poasPaid
            );

            // Then: Full liability due
            assertThat(balancingPayment).isEqualByComparingTo(new BigDecimal("5000"));
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidation {

        @Test
        @DisplayName("should handle null previous year liability as zero")
        void shouldHandleNullPreviousYearLiabilityAsZero() {
            // When
            PaymentsOnAccountResult result = calculator.calculate(
                null, false, BigDecimal.ZERO
            );

            // Then
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.exemptionReason()).isEqualTo(PaymentsOnAccountResult.ExemptionReason.BELOW_THRESHOLD);
        }

        @Test
        @DisplayName("should handle negative liability as zero")
        void shouldHandleNegativeLiabilityAsZero() {
            // When
            PaymentsOnAccountResult result = calculator.calculate(
                new BigDecimal("-500"), false, BigDecimal.ZERO
            );

            // Then
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.exemptionReason()).isEqualTo(PaymentsOnAccountResult.ExemptionReason.BELOW_THRESHOLD);
        }

        @Test
        @DisplayName("should handle null PAYE percentage as zero")
        void shouldHandleNullPayePercentageAsZero() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("5000");

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, false, null
            );

            // Then: Should calculate POA normally (null treated as 0% PAYE)
            assertThat(result.requiresPoa()).isTrue();
            assertThat(result.firstPayment()).isEqualByComparingTo(new BigDecimal("2500"));
        }

        @Test
        @DisplayName("should reject PAYE percentage over 100")
        void shouldRejectPayePercentageOver100() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("5000");
            BigDecimal invalidPayePercentage = new BigDecimal("101");

            // Then
            assertThatThrownBy(() -> calculator.calculate(
                previousYearLiability, false, invalidPayePercentage
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PAYE percentage must be between 0 and 100");
        }

        @Test
        @DisplayName("should reject negative PAYE percentage")
        void shouldRejectNegativePayePercentage() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("5000");
            BigDecimal invalidPayePercentage = new BigDecimal("-5");

            // Then
            assertThatThrownBy(() -> calculator.calculate(
                previousYearLiability, false, invalidPayePercentage
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PAYE percentage must be between 0 and 100");
        }
    }

    @Nested
    @DisplayName("Default Tax Year Behavior")
    class DefaultTaxYearBehavior {

        @Test
        @DisplayName("should use current tax year when not specified")
        void shouldUseCurrentTaxYearWhenNotSpecified() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("5000");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When: Using overload without tax year parameter
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then: Should calculate POA (deadlines will be for current tax year)
            assertThat(result.requiresPoa()).isTrue();
            assertThat(result.firstPaymentDeadline()).isNotNull();
            assertThat(result.secondPaymentDeadline()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Combined Exemption Scenarios")
    class CombinedExemptionScenarios {

        @Test
        @DisplayName("below threshold exemption should show correct reason")
        void belowThresholdExemptionShouldShowCorrectReason() {
            // Given
            BigDecimal previousYearLiability = new BigDecimal("800");
            boolean isFirstYear = false;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.exemptionReason()).isEqualTo(PaymentsOnAccountResult.ExemptionReason.BELOW_THRESHOLD);
        }

        @Test
        @DisplayName("multiple exemptions should prioritize first year over below threshold")
        void multipleExemptionsShouldPrioritizeFirstYear() {
            // Given: First year AND below threshold
            BigDecimal previousYearLiability = new BigDecimal("500"); // Below threshold
            boolean isFirstYear = true;
            BigDecimal payePercentage = BigDecimal.ZERO;

            // When
            PaymentsOnAccountResult result = calculator.calculate(
                previousYearLiability, isFirstYear, payePercentage
            );

            // Then: First year exemption takes precedence
            assertThat(result.requiresPoa()).isFalse();
            assertThat(result.exemptionReason()).isEqualTo(PaymentsOnAccountResult.ExemptionReason.FIRST_YEAR);
        }
    }
}
