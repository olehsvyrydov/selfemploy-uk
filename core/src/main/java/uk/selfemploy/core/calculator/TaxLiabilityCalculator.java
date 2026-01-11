package uk.selfemploy.core.calculator;

import java.math.BigDecimal;

/**
 * Calculator for combined tax liability (Income Tax + NI Class 4 + NI Class 2).
 *
 * Calculates all tax components for self-employed individuals:
 * - Income Tax (based on taxable income bands)
 * - NI Class 4 (percentage-based on profits above Lower Profits Limit £12,570)
 * - NI Class 2 (flat rate, mandatory if profits > Small Profits Threshold £6,845)
 */
public class TaxLiabilityCalculator {

    private final int taxYear;
    private final IncomeTaxCalculator incomeTaxCalculator;
    private final NationalInsuranceCalculator niClass4Calculator;
    private final NationalInsuranceClass2Calculator niClass2Calculator;

    public TaxLiabilityCalculator(int taxYear) {
        this.taxYear = taxYear;
        this.incomeTaxCalculator = new IncomeTaxCalculator(taxYear);
        this.niClass4Calculator = new NationalInsuranceCalculator(taxYear);
        this.niClass2Calculator = new NationalInsuranceClass2Calculator(taxYear);
    }

    /**
     * Calculates combined tax liability for the given gross profit.
     * Uses default voluntary Class 2 NI = false.
     *
     * @param grossProfit The gross profit amount
     * @return TaxLiabilityResult containing all tax calculations
     */
    public TaxLiabilityResult calculate(BigDecimal grossProfit) {
        return calculate(grossProfit, false);
    }

    /**
     * Calculates combined tax liability for the given gross profit with voluntary Class 2 NI option.
     *
     * @param grossProfit The gross profit amount
     * @param voluntaryClass2NI Whether to pay Class 2 NI voluntarily (only applies below threshold)
     * @return TaxLiabilityResult containing all tax calculations
     */
    public TaxLiabilityResult calculate(BigDecimal grossProfit, boolean voluntaryClass2NI) {
        TaxCalculationResult incomeTaxResult = incomeTaxCalculator.calculate(grossProfit);
        NICalculationResult niClass4Result = niClass4Calculator.calculate(grossProfit);
        Class2NICalculationResult niClass2Result = niClass2Calculator.calculate(grossProfit, voluntaryClass2NI);

        BigDecimal totalLiability = incomeTaxResult.totalTax()
            .add(niClass4Result.totalNI())
            .add(niClass2Result.totalNI());

        return new TaxLiabilityResult(
            grossProfit != null ? grossProfit : BigDecimal.ZERO,
            incomeTaxResult.totalTax(),
            niClass4Result.totalNI(),
            niClass2Result.totalNI(),
            totalLiability,
            incomeTaxResult,
            niClass4Result,
            niClass2Result
        );
    }

    /**
     * Returns the tax year this calculator is configured for.
     */
    public int getTaxYear() {
        return taxYear;
    }
}
