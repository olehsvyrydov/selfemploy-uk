package uk.selfemploy.core.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Calculator for Payments on Account (POA) obligations.
 *
 * <p>Payments on Account are advance payments towards your Self Assessment tax bill.
 * HMRC requires POA when:
 * <ul>
 *   <li>Your previous year's tax liability exceeds £1,000</li>
 *   <li>Less than 80% of your income was taxed at source (PAYE)</li>
 *   <li>It's not your first year of self-employment</li>
 * </ul>
 *
 * <p>Each POA is 50% of the previous year's total tax liability (Income Tax + NI Class 4).
 * Payments are due on:
 * <ul>
 *   <li>31 January following the end of the tax year</li>
 *   <li>31 July following the end of the tax year</li>
 * </ul>
 *
 * @see <a href="https://www.gov.uk/understand-self-assessment-bill/payments-on-account">
 *      HMRC Payments on Account guidance</a>
 */
public class PaymentsOnAccountCalculator {

    /**
     * POA threshold: liability must exceed this amount for POA to apply.
     */
    private static final BigDecimal POA_THRESHOLD = new BigDecimal("1000");

    /**
     * PAYE exemption threshold: if PAYE income exceeds this percentage, POA not required.
     */
    private static final BigDecimal PAYE_EXEMPTION_THRESHOLD = new BigDecimal("80");

    /**
     * Divisor for splitting liability into two equal payments.
     */
    private static final BigDecimal TWO = new BigDecimal("2");

    /**
     * Minimum valid PAYE percentage.
     */
    private static final BigDecimal MIN_PAYE_PERCENTAGE = BigDecimal.ZERO;

    /**
     * Maximum valid PAYE percentage.
     */
    private static final BigDecimal MAX_PAYE_PERCENTAGE = new BigDecimal("100");

    /**
     * Calculates POA obligations using the current tax year.
     *
     * @param previousYearLiability Total tax liability from previous year (Income Tax + NI Class 4)
     * @param isFirstYear True if this is the user's first year of self-employment
     * @param payePercentage Percentage of income already taxed at source (0-100)
     * @return Calculated POA result
     * @throws IllegalArgumentException if payePercentage is not between 0 and 100
     */
    public PaymentsOnAccountResult calculate(
            BigDecimal previousYearLiability,
            boolean isFirstYear,
            BigDecimal payePercentage) {
        return calculate(previousYearLiability, isFirstYear, payePercentage, getCurrentTaxYear());
    }

    /**
     * Calculates POA obligations for a specific tax year.
     *
     * @param previousYearLiability Total tax liability from previous year (Income Tax + NI Class 4)
     * @param isFirstYear True if this is the user's first year of self-employment
     * @param payePercentage Percentage of income already taxed at source (0-100)
     * @param taxYear The tax year (e.g., 2025 for tax year 2025/26)
     * @return Calculated POA result
     * @throws IllegalArgumentException if payePercentage is not between 0 and 100
     */
    public PaymentsOnAccountResult calculate(
            BigDecimal previousYearLiability,
            boolean isFirstYear,
            BigDecimal payePercentage,
            int taxYear) {

        // Normalize inputs
        BigDecimal normalizedLiability = normalizeAmount(previousYearLiability);
        BigDecimal normalizedPayePercentage = normalizePayePercentage(payePercentage);

        // Validate PAYE percentage
        validatePayePercentage(normalizedPayePercentage);

        // Check exemptions in priority order
        PaymentsOnAccountResult.ExemptionReason exemption = determineExemption(
            normalizedLiability, isFirstYear, normalizedPayePercentage
        );

        if (exemption != null) {
            return PaymentsOnAccountResult.notRequired(normalizedLiability, exemption, taxYear);
        }

        // Calculate POA amounts (50% each)
        BigDecimal eachPayment = normalizedLiability.divide(TWO, 2, RoundingMode.HALF_UP);

        // Calculate deadlines
        LocalDate firstDeadline = calculateFirstPaymentDeadline(taxYear);
        LocalDate secondDeadline = calculateSecondPaymentDeadline(taxYear);

        return PaymentsOnAccountResult.required(
            normalizedLiability,
            eachPayment,
            eachPayment,
            firstDeadline,
            secondDeadline,
            taxYear
        );
    }

    /**
     * Calculates the balancing payment (or refund) due after accounting for POAs paid.
     *
     * <p>The balancing payment is calculated as:
     * <pre>
     *   Balancing Payment = Current Year Liability - POAs Already Paid
     * </pre>
     *
     * <p>A positive result means additional tax is owed. A negative result means a refund is due.
     *
     * @param currentYearLiability The actual tax liability for the current year
     * @param poasPaid Total POA payments already made for this year
     * @return The balancing payment amount (positive = owed, negative = refund)
     */
    public BigDecimal calculateBalancingPayment(BigDecimal currentYearLiability, BigDecimal poasPaid) {
        BigDecimal normalizedLiability = normalizeAmount(currentYearLiability);
        BigDecimal normalizedPoasPaid = normalizeAmount(poasPaid);

        return normalizedLiability.subtract(normalizedPoasPaid);
    }

    /**
     * Determines if any exemption applies and returns the highest priority exemption reason.
     *
     * <p>Exemption priority order:
     * <ol>
     *   <li>First year (no previous liability to base POA on)</li>
     *   <li>PAYE exceeds 80% (most income already taxed)</li>
     *   <li>Below threshold (liability £1,000 or less)</li>
     * </ol>
     */
    private PaymentsOnAccountResult.ExemptionReason determineExemption(
            BigDecimal liability,
            boolean isFirstYear,
            BigDecimal payePercentage) {

        // Priority 1: First year exemption
        if (isFirstYear) {
            return PaymentsOnAccountResult.ExemptionReason.FIRST_YEAR;
        }

        // Priority 2: PAYE exceeds 80%
        if (payePercentage.compareTo(PAYE_EXEMPTION_THRESHOLD) > 0) {
            return PaymentsOnAccountResult.ExemptionReason.PAYE_EXCEEDS_80_PERCENT;
        }

        // Priority 3: Below threshold (liability must EXCEED £1,000)
        if (liability.compareTo(POA_THRESHOLD) <= 0) {
            return PaymentsOnAccountResult.ExemptionReason.BELOW_THRESHOLD;
        }

        // No exemption applies
        return null;
    }

    /**
     * Normalizes an amount by treating null and negative values as zero.
     */
    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return amount;
    }

    /**
     * Normalizes PAYE percentage by treating null as zero.
     */
    private BigDecimal normalizePayePercentage(BigDecimal payePercentage) {
        return payePercentage != null ? payePercentage : BigDecimal.ZERO;
    }

    /**
     * Validates that PAYE percentage is between 0 and 100 (inclusive).
     */
    private void validatePayePercentage(BigDecimal payePercentage) {
        if (payePercentage.compareTo(MIN_PAYE_PERCENTAGE) < 0 ||
            payePercentage.compareTo(MAX_PAYE_PERCENTAGE) > 0) {
            throw new IllegalArgumentException(
                "PAYE percentage must be between 0 and 100, got: " + payePercentage
            );
        }
    }

    /**
     * Calculates the first POA deadline (31 January) for a tax year.
     *
     * <p>For tax year 2025/26 (April 2025 - April 2026):
     * <ul>
     *   <li>Tax return due: 31 January 2027</li>
     *   <li>First POA for NEXT year due: 31 January 2027</li>
     * </ul>
     */
    private LocalDate calculateFirstPaymentDeadline(int taxYear) {
        // Tax year 2025 = 2025/26, ends April 2026
        // POA1 due 31 January following year end = 31 Jan 2027
        return LocalDate.of(taxYear + 2, 1, 31);
    }

    /**
     * Calculates the second POA deadline (31 July) for a tax year.
     *
     * <p>For tax year 2025/26 (April 2025 - April 2026):
     * <ul>
     *   <li>Second POA for NEXT year due: 31 July 2027</li>
     * </ul>
     */
    private LocalDate calculateSecondPaymentDeadline(int taxYear) {
        // Tax year 2025 = 2025/26, ends April 2026
        // POA2 due 31 July following year end = 31 July 2027
        return LocalDate.of(taxYear + 2, 7, 31);
    }

    /**
     * Determines the current tax year based on today's date.
     *
     * <p>UK tax year runs from 6 April to 5 April.
     * <ul>
     *   <li>Before 6 April: Previous calendar year's tax year (e.g., Jan 2026 = 2025/26 = taxYear 2025)</li>
     *   <li>From 6 April: Current calendar year's tax year (e.g., May 2026 = 2026/27 = taxYear 2026)</li>
     * </ul>
     */
    private int getCurrentTaxYear() {
        LocalDate today = LocalDate.now();
        LocalDate taxYearStart = LocalDate.of(today.getYear(), 4, 6);

        if (today.isBefore(taxYearStart)) {
            return today.getYear() - 1;
        }
        return today.getYear();
    }
}
