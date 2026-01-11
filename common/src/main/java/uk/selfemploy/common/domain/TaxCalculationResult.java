package uk.selfemploy.common.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents the tax calculation result from HMRC.
 *
 * <p>This is the output of the HMRC Self Assessment Calculation API.
 * Contains the tax liability breakdown for the user to review before final submission.
 */
public record TaxCalculationResult(
    UUID id,
    String calculationId,
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal netProfit,
    BigDecimal incomeTax,
    BigDecimal nationalInsuranceClass2,
    BigDecimal nationalInsuranceClass4,
    BigDecimal totalTaxLiability,
    Instant calculatedAt
) {
    /**
     * Creates a new TaxCalculationResult.
     */
    public static TaxCalculationResult create(
            String calculationId,
            BigDecimal totalIncome,
            BigDecimal totalExpenses,
            BigDecimal netProfit,
            BigDecimal incomeTax,
            BigDecimal nationalInsuranceClass2,
            BigDecimal nationalInsuranceClass4) {

        BigDecimal totalTax = incomeTax
            .add(nationalInsuranceClass2)
            .add(nationalInsuranceClass4);

        return new TaxCalculationResult(
            UUID.randomUUID(),
            calculationId,
            totalIncome,
            totalExpenses,
            netProfit,
            incomeTax,
            nationalInsuranceClass2,
            nationalInsuranceClass4,
            totalTax,
            Instant.now()
        );
    }
}
