package uk.selfemploy.core.config;

import java.math.BigDecimal;

/**
 * National Insurance Class 4 rates configuration for a specific tax year.
 *
 * Class 4 NI is payable by self-employed individuals on their profits
 * between the Lower Profits Limit and Upper Profits Limit.
 */
public record NIClass4Rates(
    BigDecimal lowerProfitsLimit,
    BigDecimal upperProfitsLimit,
    BigDecimal mainRate,
    BigDecimal additionalRate
) {
    /**
     * Default fallback rates (2024/25 rates).
     * Used when YAML configuration is not available.
     */
    public static NIClass4Rates defaultRates() {
        return new NIClass4Rates(
            new BigDecimal("12570"),   // Lower Profits Limit
            new BigDecimal("50270"),   // Upper Profits Limit
            new BigDecimal("0.06"),    // Main rate 6%
            new BigDecimal("0.02")     // Additional rate 2%
        );
    }
}
