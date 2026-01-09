package uk.selfemploy.core.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Result of an income tax calculation.
 */
public record TaxCalculationResult(
    BigDecimal grossIncome,
    BigDecimal personalAllowance,
    BigDecimal taxableIncome,
    BigDecimal basicRateAmount,
    BigDecimal basicRateTax,
    BigDecimal higherRateAmount,
    BigDecimal higherRateTax,
    BigDecimal additionalRateAmount,
    BigDecimal additionalRateTax,
    BigDecimal totalTax
) {
    /**
     * Calculates the effective tax rate as a percentage.
     */
    public BigDecimal effectiveRate() {
        if (grossIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalTax
            .multiply(new BigDecimal("100"))
            .divide(grossIncome, 2, RoundingMode.HALF_UP);
    }
}
