package uk.selfemploy.core.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Result of a combined tax liability calculation (Income Tax + NI Class 4 + NI Class 2).
 *
 * This record contains all tax liability components:
 * - Income Tax (based on taxable income bands)
 * - NI Class 4 (percentage-based on profits above Lower Profits Limit £12,570)
 * - NI Class 2 (flat rate based on weeks, mandatory if profits > Small Profits Threshold £6,845)
 */
public record TaxLiabilityResult(
    BigDecimal grossProfit,
    BigDecimal incomeTax,
    BigDecimal niClass4,
    BigDecimal niClass2,
    BigDecimal totalLiability,
    TaxCalculationResult incomeTaxDetails,
    NICalculationResult niClass4Details,
    Class2NICalculationResult niClass2Details
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

    /**
     * Returns total National Insurance (Class 2 + Class 4).
     */
    public BigDecimal totalNI() {
        BigDecimal class2 = niClass2 != null ? niClass2 : BigDecimal.ZERO;
        BigDecimal class4 = niClass4 != null ? niClass4 : BigDecimal.ZERO;
        return class2.add(class4);
    }

    /**
     * Backward compatibility accessor for NI Class 4 details.
     * @deprecated Use {@link #niClass4Details()} instead
     */
    @Deprecated
    public NICalculationResult niDetails() {
        return niClass4Details;
    }
}
