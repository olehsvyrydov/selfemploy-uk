package uk.selfemploy.core.calculator;

import uk.selfemploy.core.config.NIClass4Rates;
import uk.selfemploy.core.config.TaxRateConfiguration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

/**
 * Calculator for UK National Insurance Class 4.
 *
 * <p>NI Class 4 is payable by self-employed individuals on their profits.
 * Rates are loaded from YAML configuration, with fallback to default rates.</p>
 *
 * <h3>SE-808: State Pension Age Exemption</h3>
 * <p>People who have reached State Pension Age before the start of the tax year
 * are exempt from Class 4 NI contributions.</p>
 *
 * <h3>Current State Pension Age: 66 years</h3>
 *
 * <h3>Future State Pension Age Changes</h3>
 * <p>HMRC Reference: <a href="https://www.gov.uk/state-pension-age">https://www.gov.uk/state-pension-age</a></p>
 *
 * <p>TODO (2028 Release): Implement pension age increase to 67</p>
 * <ul>
 *   <li>From 6 April 2028: State Pension Age rises to 67 for those born on or after 6 March 1961</li>
 *   <li>Transitional period (2026-2028): Those born 6 April 1960 - 5 March 1961 have a gradual
 *       increase from age 66 to 67</li>
 *   <li>Use gov.uk State Pension Age calculator for exact dates</li>
 * </ul>
 *
 * <p>TODO (2044 Release): Implement pension age increase to 68</p>
 * <ul>
 *   <li>Currently planned for 2044-2046 (subject to government review)</li>
 * </ul>
 *
 * @see <a href="https://www.gov.uk/self-employed-national-insurance-rates">HMRC NI Rates</a>
 * @see <a href="https://www.gov.uk/state-pension-age">HMRC State Pension Age</a>
 */
public class NationalInsuranceCalculator {

    /**
     * Current State Pension Age threshold.
     * For MVP, using fixed age of 66.
     */
    public static final int STATE_PENSION_AGE = 66;

    /**
     * Exemption reason message for State Pension Age exemption.
     */
    public static final String PENSION_AGE_EXEMPTION_REASON =
        "State Pension Age reached before tax year start";

    private final int taxYear;
    private final NIClass4Rates rates;

    public NationalInsuranceCalculator(int taxYear) {
        this.taxYear = taxYear;
        this.rates = TaxRateConfiguration.getInstance().getNIClass4Rates(taxYear);
    }

    /**
     * Calculates NI Class 4 for the given gross profit.
     * Does not consider pension age exemption (date of birth not provided).
     *
     * @param grossProfit The gross profit amount
     * @return NICalculationResult with calculated NI (never exempt without date of birth)
     */
    public NICalculationResult calculate(BigDecimal grossProfit) {
        return calculate(grossProfit, null);
    }

    /**
     * Calculates NI Class 4 for the given gross profit, considering State Pension Age exemption.
     *
     * SE-808: People who have reached State Pension Age (currently 66) before the start
     * of the tax year are exempt from Class 4 NI contributions.
     *
     * @param grossProfit The gross profit amount
     * @param dateOfBirth The person's date of birth, or null if not known
     * @return NICalculationResult with calculated NI or exemption details
     */
    public NICalculationResult calculate(BigDecimal grossProfit, LocalDate dateOfBirth) {
        BigDecimal effectiveProfit = grossProfit != null ? grossProfit : BigDecimal.ZERO;

        // Check for pension age exemption
        if (isExemptFromClass4NI(dateOfBirth)) {
            return exemptResult(effectiveProfit);
        }

        if (effectiveProfit.compareTo(BigDecimal.ZERO) <= 0) {
            return zeroResult(effectiveProfit);
        }

        // Calculate profit subject to NI
        BigDecimal lowerProfitsLimit = rates.lowerProfitsLimit();
        BigDecimal profitSubjectToNI = effectiveProfit.subtract(lowerProfitsLimit);

        if (profitSubjectToNI.compareTo(BigDecimal.ZERO) <= 0) {
            return zeroResult(effectiveProfit);
        }

        // Calculate NI at each band
        BigDecimal mainRateAmount = BigDecimal.ZERO;
        BigDecimal mainRateNI = BigDecimal.ZERO;
        BigDecimal additionalRateAmount = BigDecimal.ZERO;
        BigDecimal additionalRateNI = BigDecimal.ZERO;

        BigDecimal remainingProfit = profitSubjectToNI;

        // Main rate band
        BigDecimal mainRateBandSize = rates.upperProfitsLimit().subtract(rates.lowerProfitsLimit());
        if (remainingProfit.compareTo(BigDecimal.ZERO) > 0) {
            mainRateAmount = remainingProfit.min(mainRateBandSize);
            mainRateNI = mainRateAmount.multiply(rates.mainRate()).setScale(2, RoundingMode.HALF_UP);
            remainingProfit = remainingProfit.subtract(mainRateAmount);
        }

        // Additional rate
        if (remainingProfit.compareTo(BigDecimal.ZERO) > 0) {
            additionalRateAmount = remainingProfit;
            additionalRateNI = additionalRateAmount.multiply(rates.additionalRate()).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalNI = mainRateNI.add(additionalRateNI);

        return new NICalculationResult(
            effectiveProfit,
            lowerProfitsLimit,
            profitSubjectToNI,
            mainRateAmount,
            mainRateNI,
            additionalRateAmount,
            additionalRateNI,
            totalNI,
            false,
            null
        );
    }

    /**
     * Checks if a person is exempt from Class 4 NI based on State Pension Age.
     *
     * The exemption applies if the person reaches State Pension Age BEFORE
     * the start of the tax year (6 April).
     *
     * @param dateOfBirth The person's date of birth, or null if not known
     * @return true if exempt from Class 4 NI, false otherwise
     */
    public boolean isExemptFromClass4NI(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return false;
        }

        // Tax year starts on 6 April
        LocalDate taxYearStart = LocalDate.of(taxYear, 4, 6);

        // Calculate age at tax year start
        int ageAtTaxYearStart = Period.between(dateOfBirth, taxYearStart).getYears();

        return ageAtTaxYearStart >= STATE_PENSION_AGE;
    }

    /**
     * Creates a result for a person exempt from Class 4 NI due to State Pension Age.
     */
    private NICalculationResult exemptResult(BigDecimal grossProfit) {
        return new NICalculationResult(
            grossProfit,
            rates.lowerProfitsLimit(),
            BigDecimal.ZERO,  // No profit subject to NI when exempt
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            true,
            PENSION_AGE_EXEMPTION_REASON
        );
    }

    /**
     * Creates a zero result for profits below the threshold (not exempt, just no NI due).
     */
    private NICalculationResult zeroResult(BigDecimal grossProfit) {
        return new NICalculationResult(
            grossProfit,
            rates.lowerProfitsLimit(),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            false,
            null
        );
    }

    /**
     * Returns the tax year this calculator is configured for.
     */
    public int getTaxYear() {
        return taxYear;
    }

    /**
     * Returns the loaded NI Class 4 rates.
     */
    public NIClass4Rates getRates() {
        return rates;
    }
}
