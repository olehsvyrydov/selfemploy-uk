package uk.selfemploy.core.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Result of a National Insurance Class 4 calculation.
 *
 * SE-808: Now includes State Pension Age exemption status.
 * People who have reached State Pension Age (currently 66) before the start
 * of the tax year are exempt from Class 4 NI contributions.
 */
public record NICalculationResult(
    BigDecimal grossProfit,
    BigDecimal lowerProfitsLimit,
    BigDecimal profitSubjectToNI,
    BigDecimal mainRateAmount,
    BigDecimal mainRateNI,
    BigDecimal additionalRateAmount,
    BigDecimal additionalRateNI,
    BigDecimal totalNI,
    boolean isExempt,
    String exemptionReason
) {
    /**
     * Backward-compatible constructor without exemption fields.
     * Creates a non-exempt result.
     */
    public NICalculationResult(
            BigDecimal grossProfit,
            BigDecimal lowerProfitsLimit,
            BigDecimal profitSubjectToNI,
            BigDecimal mainRateAmount,
            BigDecimal mainRateNI,
            BigDecimal additionalRateAmount,
            BigDecimal additionalRateNI,
            BigDecimal totalNI
    ) {
        this(grossProfit, lowerProfitsLimit, profitSubjectToNI, mainRateAmount,
             mainRateNI, additionalRateAmount, additionalRateNI, totalNI,
             false, null);
    }
    /**
     * Calculates the effective NI rate as a percentage.
     */
    public BigDecimal effectiveRate() {
        if (grossProfit.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalNI
            .multiply(new BigDecimal("100"))
            .divide(grossProfit, 2, RoundingMode.HALF_UP);
    }
}
