package uk.selfemploy.core.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculator for UK National Insurance Class 2.
 *
 * Class 2 NI is a flat-rate weekly contribution paid by self-employed individuals.
 * It differs from Class 4 (which is percentage-based on profits).
 *
 * For 2025/26:
 * - Weekly rate: £3.50
 * - Annual amount: £182.00 (£3.50 x 52 weeks)
 * - Small Profits Threshold: £6,845 (mandatory above, voluntary below)
 *
 * Key differences from Class 4 NI:
 * - Class 2: Uses Small Profits Threshold (£6,845)
 * - Class 4: Uses Lower Profits Limit (£12,570)
 */
public class NationalInsuranceClass2Calculator {

    private static final int WEEKS_IN_YEAR = 52;

    private final int taxYear;
    private final Class2NIRates rates;

    public NationalInsuranceClass2Calculator(int taxYear) {
        this.taxYear = taxYear;
        this.rates = Class2NIRates.forYear(taxYear);
    }

    /**
     * Calculates Class 2 NI for the given gross profit.
     * Uses default voluntary = false.
     *
     * @param grossProfit The gross profit amount
     * @return Class2NICalculationResult containing the calculation details
     */
    public Class2NICalculationResult calculate(BigDecimal grossProfit) {
        return calculate(grossProfit, false);
    }

    /**
     * Calculates Class 2 NI for the given gross profit with voluntary option.
     *
     * @param grossProfit The gross profit amount
     * @param voluntary Whether to pay Class 2 NI voluntarily (only applies below threshold)
     * @return Class2NICalculationResult containing the calculation details
     */
    public Class2NICalculationResult calculate(BigDecimal grossProfit, boolean voluntary) {
        // Handle null or negative profit
        if (grossProfit == null) {
            grossProfit = BigDecimal.ZERO;
        }

        // Check if profits exceed Small Profits Threshold
        boolean exceedsThreshold = grossProfit.compareTo(rates.smallProfitsThreshold()) > 0;

        boolean isMandatory = false;
        boolean isVoluntary = false;
        BigDecimal totalNI = BigDecimal.ZERO;
        int weeksLiable = 0;

        if (exceedsThreshold) {
            // Mandatory Class 2 NI - profits above Small Profits Threshold
            isMandatory = true;
            isVoluntary = false;
            weeksLiable = WEEKS_IN_YEAR;
            totalNI = calculateAnnualNI();
        } else if (voluntary) {
            // Voluntary Class 2 NI - profits below threshold but choosing to pay
            isMandatory = false;
            isVoluntary = true;
            weeksLiable = WEEKS_IN_YEAR;
            totalNI = calculateAnnualNI();
        }
        // else: No Class 2 NI due (below threshold and not voluntary)

        return new Class2NICalculationResult(
            grossProfit,
            rates.smallProfitsThreshold(),
            rates.weeklyRate(),
            weeksLiable,
            totalNI,
            isMandatory,
            isVoluntary
        );
    }

    /**
     * Calculates the annual Class 2 NI amount.
     * £3.50 x 52 weeks = £182.00 for 2025/26
     */
    private BigDecimal calculateAnnualNI() {
        return rates.weeklyRate()
            .multiply(new BigDecimal(WEEKS_IN_YEAR))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns the tax year this calculator is configured for.
     */
    public int getTaxYear() {
        return taxYear;
    }

    /**
     * Class 2 NI rates for different tax years.
     */
    public record Class2NIRates(
        BigDecimal weeklyRate,
        BigDecimal smallProfitsThreshold
    ) {
        /**
         * Returns the Class 2 NI rates for the specified tax year.
         *
         * @param year The tax year (e.g., 2025 for 2025/26)
         * @return Class2NIRates for the specified year
         */
        public static Class2NIRates forYear(int year) {
            // 2025/26 rates (confirmed by /inga)
            if (year >= 2025) {
                return new Class2NIRates(
                    new BigDecimal("3.50"),    // Weekly rate
                    new BigDecimal("6845")     // Small Profits Threshold
                );
            }

            // 2024/25 rates
            if (year == 2024) {
                return new Class2NIRates(
                    new BigDecimal("3.45"),    // Weekly rate for 2024/25
                    new BigDecimal("6725")     // Small Profits Threshold for 2024/25
                );
            }

            // Default to 2023/24 rates for older years
            return new Class2NIRates(
                new BigDecimal("3.45"),
                new BigDecimal("6725")
            );
        }
    }
}
