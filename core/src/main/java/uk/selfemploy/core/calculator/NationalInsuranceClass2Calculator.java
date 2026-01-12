package uk.selfemploy.core.calculator;

import uk.selfemploy.core.config.NIClass2Rates;
import uk.selfemploy.core.config.TaxRateConfiguration;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculator for UK National Insurance Class 2.
 *
 * Class 2 NI is a flat-rate weekly contribution paid by self-employed individuals.
 * It differs from Class 4 (which is percentage-based on profits).
 *
 * Rates are loaded from YAML configuration, with fallback to default rates.
 *
 * Key differences from Class 4 NI:
 * - Class 2: Uses Small Profits Threshold
 * - Class 4: Uses Lower Profits Limit
 */
public class NationalInsuranceClass2Calculator {

    private static final int WEEKS_IN_YEAR = 52;

    private final int taxYear;
    private final NIClass2Rates rates;

    public NationalInsuranceClass2Calculator(int taxYear) {
        this.taxYear = taxYear;
        this.rates = TaxRateConfiguration.getInstance().getNIClass2Rates(taxYear);
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
     * Returns the loaded NI Class 2 rates.
     */
    public NIClass2Rates getRates() {
        return rates;
    }
}
