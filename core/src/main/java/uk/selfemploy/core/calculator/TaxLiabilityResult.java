package uk.selfemploy.core.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Result of a combined tax liability calculation (Income Tax + NI Class 4).
 */
public record TaxLiabilityResult(
    BigDecimal grossProfit,
    BigDecimal incomeTax,
    BigDecimal niClass4,
    BigDecimal totalLiability,
    TaxCalculationResult incomeTaxDetails,
    NICalculationResult niDetails
) {
    private static final BigDecimal POA_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal POA_DIVISOR = new BigDecimal("2");

    /**
     * Calculates net profit after all taxes.
     */
    public BigDecimal netProfitAfterTax() {
        return grossProfit.subtract(totalLiability);
    }

    /**
     * Calculates the effective combined tax rate as a percentage.
     */
    public BigDecimal effectiveRate() {
        if (grossProfit.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalLiability
            .multiply(new BigDecimal("100"))
            .divide(grossProfit, 2, RoundingMode.HALF_UP);
    }

    /**
     * Returns true if Payment on Account is required.
     * POA is required if total tax liability exceeds £1,000.
     */
    public boolean requiresPaymentOnAccount() {
        return totalLiability.compareTo(POA_THRESHOLD) > 0;
    }

    /**
     * Calculates the Payment on Account amount.
     * POA is 50% of total liability.
     */
    public BigDecimal paymentOnAccountAmount() {
        if (!requiresPaymentOnAccount()) {
            return BigDecimal.ZERO;
        }
        return totalLiability.divide(POA_DIVISOR, 2, RoundingMode.HALF_UP);
    }
}
