package uk.selfemploy.core.config;

import java.math.BigDecimal;

/**
 * Income Tax rates configuration for a specific tax year.
 *
 * Contains all the thresholds and percentages needed to calculate Income Tax.
 * Values are loaded from YAML configuration files.
 */
public record IncomeTaxRates(
    BigDecimal personalAllowance,
    BigDecimal basicRateUpperLimit,
    BigDecimal higherRateUpperLimit,
    BigDecimal taperThreshold,
    BigDecimal basicRate,
    BigDecimal higherRate,
    BigDecimal additionalRate
) {
    /**
     * Default fallback rates (2024/25 frozen rates).
     * Used when YAML configuration is not available.
     */
    public static IncomeTaxRates defaultRates() {
        return new IncomeTaxRates(
            new BigDecimal("12570"),   // Personal Allowance
            new BigDecimal("50270"),   // Basic rate upper limit
            new BigDecimal("125140"),  // Higher rate upper limit
            new BigDecimal("100000"),  // Taper threshold
            new BigDecimal("0.20"),    // Basic rate 20%
            new BigDecimal("0.40"),    // Higher rate 40%
            new BigDecimal("0.45")     // Additional rate 45%
        );
    }
}
