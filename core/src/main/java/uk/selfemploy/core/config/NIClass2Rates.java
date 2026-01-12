package uk.selfemploy.core.config;

import java.math.BigDecimal;

/**
 * National Insurance Class 2 rates configuration for a specific tax year.
 *
 * Class 2 NI is a flat-rate weekly contribution paid by self-employed individuals.
 * It's mandatory if profits exceed the Small Profits Threshold, voluntary otherwise.
 */
public record NIClass2Rates(
    BigDecimal weeklyRate,
    BigDecimal smallProfitsThreshold
) {
    /**
     * Default fallback rates (2024/25 rates).
     * Used when YAML configuration is not available.
     */
    public static NIClass2Rates defaultRates() {
        return new NIClass2Rates(
            new BigDecimal("3.45"),    // Weekly rate
            new BigDecimal("6725")     // Small Profits Threshold
        );
    }
}
