package uk.selfemploy.core.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Result of a National Insurance Class 4 calculation.
 */
public record NICalculationResult(
    BigDecimal grossProfit,
    BigDecimal lowerProfitsLimit,
    BigDecimal profitSubjectToNI,
    BigDecimal mainRateAmount,
    BigDecimal mainRateNI,
    BigDecimal additionalRateAmount,
    BigDecimal additionalRateNI,
    BigDecimal totalNI
) {
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
