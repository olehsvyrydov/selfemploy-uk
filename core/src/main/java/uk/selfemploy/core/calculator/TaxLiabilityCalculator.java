package uk.selfemploy.core.calculator;

import java.math.BigDecimal;

/**
 * Calculator for combined tax liability (Income Tax + NI Class 4).
 */
public class TaxLiabilityCalculator {

    private final int taxYear;
    private final IncomeTaxCalculator incomeTaxCalculator;
    private final NationalInsuranceCalculator niCalculator;

    public TaxLiabilityCalculator(int taxYear) {
        this.taxYear = taxYear;
        this.incomeTaxCalculator = new IncomeTaxCalculator(taxYear);
        this.niCalculator = new NationalInsuranceCalculator(taxYear);
    }

    /**
     * Calculates combined tax liability for the given gross profit.
     */
    public TaxLiabilityResult calculate(BigDecimal grossProfit) {
        TaxCalculationResult incomeTaxResult = incomeTaxCalculator.calculate(grossProfit);
        NICalculationResult niResult = niCalculator.calculate(grossProfit);

        BigDecimal totalLiability = incomeTaxResult.totalTax().add(niResult.totalNI());

        return new TaxLiabilityResult(
            grossProfit != null ? grossProfit : BigDecimal.ZERO,
            incomeTaxResult.totalTax(),
            niResult.totalNI(),
            totalLiability,
            incomeTaxResult,
            niResult
        );
    }

    /**
     * Returns the tax year this calculator is configured for.
     */
    public int getTaxYear() {
        return taxYear;
    }
}
