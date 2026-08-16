package uk.selfemploy.core.calculator;

import uk.selfemploy.core.config.NIClass2Rates;
import uk.selfemploy.core.config.TaxRateConfiguration;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculator for UK National Insurance Class 2.
 *
 * <p>Class 2 NI is a flat-rate weekly contribution for self-employed individuals. It differs
 * from Class 4 (which is percentage-based on profits): Class 2 uses the Small Profits
 * Threshold, Class 4 the Lower Profits Limit.
 *
 * <p>The requirement to pay Class 2 was removed from 6 April 2024. From tax year 2024/25
 * onwards, profits above the Small Profits Threshold are treated as having paid it, so nothing
 * is charged; below the threshold it may still be paid voluntarily to protect a State Pension
 * qualifying year. Tax years before 2024/25 keep the mandatory weekly charge.
 *
 * <p>Rates are loaded from YAML configuration, with fallback to default rates.
 *
 * @see <a href="https://www.gov.uk/self-employed-national-insurance-rates">HMRC: self-employed
 *      National Insurance rates</a>
 */
public class NationalInsuranceClass2Calculator {

    private static final int WEEKS_IN_YEAR = 52;

    /**
     * First tax year in which profits above the Small Profits Threshold are treated as having
     * paid Class 2 rather than being charged for it (6 April 2024).
     */
    private static final int TREATED_AS_PAID_FROM_TAX_YEAR = 2024;

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
     * <p>Above the Small Profits Threshold the result depends on the tax year: from 2024/25
     * nothing is charged and the year is treated as paid; earlier years charge 52 weeks.
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

        boolean exceedsThreshold = grossProfit.compareTo(rates.smallProfitsThreshold()) > 0;
        boolean treatedAsPaid = exceedsThreshold && taxYear >= TREATED_AS_PAID_FROM_TAX_YEAR;

        boolean isMandatory = false;
        boolean isVoluntary = false;
        BigDecimal totalNI = BigDecimal.ZERO;
        int weeksLiable = 0;

        if (exceedsThreshold && !treatedAsPaid) {
            // Pre-2024/25: mandatory Class 2 NI above the Small Profits Threshold
            isMandatory = true;
            weeksLiable = WEEKS_IN_YEAR;
            totalNI = calculateAnnualNI();
        } else if (!exceedsThreshold && voluntary) {
            // Voluntary Class 2 NI - profits below threshold but choosing to pay
            isVoluntary = true;
            weeksLiable = WEEKS_IN_YEAR;
            totalNI = calculateAnnualNI();
        }
        // else: nothing due — either treated as paid above the threshold, or below it and
        // not opting in.

        return new Class2NICalculationResult(
            grossProfit,
            rates.smallProfitsThreshold(),
            rates.weeklyRate(),
            weeksLiable,
            totalNI,
            isMandatory,
            isVoluntary,
            treatedAsPaid
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
