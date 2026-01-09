package uk.selfemploy.core.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculator for UK National Insurance Class 4.
 *
 * NI Class 4 is payable by self-employed individuals on their profits.
 */
public class NationalInsuranceCalculator {

    private final int taxYear;
    private final NIRates rates;

    public NationalInsuranceCalculator(int taxYear) {
        this.taxYear = taxYear;
        this.rates = NIRates.forYear(taxYear);
    }

    /**
     * Calculates NI Class 4 for the given gross profit.
     */
    public NICalculationResult calculate(BigDecimal grossProfit) {
        if (grossProfit == null || grossProfit.compareTo(BigDecimal.ZERO) <= 0) {
            return zeroResult(grossProfit != null ? grossProfit : BigDecimal.ZERO);
        }

        // Calculate profit subject to NI
        BigDecimal lowerProfitsLimit = rates.lowerProfitsLimit();
        BigDecimal profitSubjectToNI = grossProfit.subtract(lowerProfitsLimit);

        if (profitSubjectToNI.compareTo(BigDecimal.ZERO) <= 0) {
            return zeroResult(grossProfit);
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
            grossProfit,
            lowerProfitsLimit,
            profitSubjectToNI,
            mainRateAmount,
            mainRateNI,
            additionalRateAmount,
            additionalRateNI,
            totalNI
        );
    }

    private NICalculationResult zeroResult(BigDecimal grossProfit) {
        return new NICalculationResult(
            grossProfit,
            rates.lowerProfitsLimit(),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
    }

    /**
     * NI Class 4 rates for different years.
     */
    public record NIRates(
        BigDecimal lowerProfitsLimit,
        BigDecimal upperProfitsLimit,
        BigDecimal mainRate,
        BigDecimal additionalRate
    ) {
        public static NIRates forYear(int year) {
            // 2025/26 rates (reduced from 2024/25)
            if (year >= 2025) {
                return new NIRates(
                    new BigDecimal("12570"),  // Lower Profits Limit
                    new BigDecimal("50270"),  // Upper Profits Limit
                    new BigDecimal("0.06"),   // Main rate 6%
                    new BigDecimal("0.02")    // Additional rate 2%
                );
            }

            // 2024/25 rates
            return new NIRates(
                new BigDecimal("12570"),
                new BigDecimal("50270"),
                new BigDecimal("0.06"),   // Was 9% before April 2024, now 6%
                new BigDecimal("0.02")
            );
        }
    }
}
