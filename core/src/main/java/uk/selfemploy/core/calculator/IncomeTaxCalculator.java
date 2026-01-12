package uk.selfemploy.core.calculator;

import uk.selfemploy.core.config.IncomeTaxRates;
import uk.selfemploy.core.config.TaxRateConfiguration;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculator for UK Income Tax.
 *
 * Supports multiple tax years with different rates loaded from YAML configuration.
 * Falls back to default rates if YAML is not available.
 */
public class IncomeTaxCalculator {

    private final int taxYear;
    private final IncomeTaxRates rates;

    public IncomeTaxCalculator(int taxYear) {
        this.taxYear = taxYear;
        this.rates = TaxRateConfiguration.getInstance().getIncomeTaxRates(taxYear);
    }

    /**
     * Calculates income tax for the given gross income.
     */
    public TaxCalculationResult calculate(BigDecimal grossIncome) {
        if (grossIncome == null || grossIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return zeroResult(grossIncome != null ? grossIncome : BigDecimal.ZERO);
        }

        // Calculate personal allowance (may be reduced for high earners)
        BigDecimal personalAllowance = calculatePersonalAllowance(grossIncome);

        // Calculate taxable income
        BigDecimal taxableIncome = grossIncome.subtract(personalAllowance);
        if (taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return zeroResult(grossIncome);
        }

        // Calculate tax at each band
        BigDecimal basicRateAmount = BigDecimal.ZERO;
        BigDecimal basicRateTax = BigDecimal.ZERO;
        BigDecimal higherRateAmount = BigDecimal.ZERO;
        BigDecimal higherRateTax = BigDecimal.ZERO;
        BigDecimal additionalRateAmount = BigDecimal.ZERO;
        BigDecimal additionalRateTax = BigDecimal.ZERO;

        BigDecimal remainingIncome = taxableIncome;

        // Basic rate band
        BigDecimal basicRateBandSize = rates.basicRateUpperLimit().subtract(rates.personalAllowance());
        if (remainingIncome.compareTo(BigDecimal.ZERO) > 0) {
            basicRateAmount = remainingIncome.min(basicRateBandSize);
            basicRateTax = basicRateAmount.multiply(rates.basicRate()).setScale(2, RoundingMode.HALF_UP);
            remainingIncome = remainingIncome.subtract(basicRateAmount);
        }

        // Higher rate band
        BigDecimal higherRateBandSize = rates.higherRateUpperLimit().subtract(rates.basicRateUpperLimit());
        if (remainingIncome.compareTo(BigDecimal.ZERO) > 0) {
            higherRateAmount = remainingIncome.min(higherRateBandSize);
            higherRateTax = higherRateAmount.multiply(rates.higherRate()).setScale(2, RoundingMode.HALF_UP);
            remainingIncome = remainingIncome.subtract(higherRateAmount);
        }

        // Additional rate
        if (remainingIncome.compareTo(BigDecimal.ZERO) > 0) {
            additionalRateAmount = remainingIncome;
            additionalRateTax = additionalRateAmount.multiply(rates.additionalRate()).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalTax = basicRateTax.add(higherRateTax).add(additionalRateTax);

        return new TaxCalculationResult(
            grossIncome,
            personalAllowance,
            taxableIncome,
            basicRateAmount,
            basicRateTax,
            higherRateAmount,
            higherRateTax,
            additionalRateAmount,
            additionalRateTax,
            totalTax
        );
    }

    /**
     * Calculates personal allowance with taper for high earners.
     *
     * For income over £100,000:
     * - Allowance reduces by £1 for every £2 over £100,000
     * - At £125,140, allowance is fully withdrawn
     */
    private BigDecimal calculatePersonalAllowance(BigDecimal grossIncome) {
        BigDecimal taperThreshold = rates.taperThreshold();
        BigDecimal standardAllowance = rates.personalAllowance();

        if (grossIncome.compareTo(taperThreshold) <= 0) {
            return standardAllowance;
        }

        // Reduce by £1 for every £2 over the threshold
        BigDecimal excessIncome = grossIncome.subtract(taperThreshold);
        BigDecimal reduction = excessIncome.divide(new BigDecimal("2"), 0, RoundingMode.DOWN);

        BigDecimal adjustedAllowance = standardAllowance.subtract(reduction);
        return adjustedAllowance.max(BigDecimal.ZERO);
    }

    private TaxCalculationResult zeroResult(BigDecimal grossIncome) {
        return new TaxCalculationResult(
            grossIncome,
            rates.personalAllowance(),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
    }

    /**
     * Returns the tax year this calculator is configured for.
     */
    public int getTaxYear() {
        return taxYear;
    }

    /**
     * Returns the loaded income tax rates.
     */
    public IncomeTaxRates getRates() {
        return rates;
    }
}
