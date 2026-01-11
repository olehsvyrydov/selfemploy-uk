package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response from retrieving a Self Assessment tax calculation.
 *
 * <p>API: GET /individuals/calculations/self-assessment/{nino}/{taxYear}/{calculationId}
 *
 * <p>Contains the tax liability breakdown including income tax and National Insurance.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individual-calculations-api">
 *     HMRC Individual Calculations API</a>
 */
public record CalculationResponse(
    @JsonProperty("id")
    String calculationId,

    @JsonProperty("calculationTimestamp")
    LocalDateTime calculationTimestamp,

    @JsonProperty("calculationReason")
    String calculationReason,

    @JsonProperty("totalIncomeTaxAndNicsDue")
    BigDecimal totalIncomeTaxAndNicsDue,

    @JsonProperty("totalIncomeReceived")
    BigDecimal totalIncomeReceived,

    @JsonProperty("totalAllowancesAndDeductions")
    BigDecimal totalAllowancesAndDeductions,

    @JsonProperty("totalTaxableIncome")
    BigDecimal totalTaxableIncome,

    @JsonProperty("incomeTax")
    IncomeTaxBreakdown incomeTax,

    @JsonProperty("nics")
    NationalInsuranceBreakdown nics
) {
    /**
     * Income tax breakdown.
     */
    public record IncomeTaxBreakdown(
        @JsonProperty("totalIncomeTax")
        BigDecimal totalIncomeTax,

        @JsonProperty("incomeTaxCharged")
        BigDecimal incomeTaxCharged
    ) {
    }

    /**
     * National Insurance contributions breakdown.
     */
    public record NationalInsuranceBreakdown(
        @JsonProperty("class2Nics")
        Class2Nics class2Nics,

        @JsonProperty("class4Nics")
        Class4Nics class4Nics
    ) {
        public record Class2Nics(
            @JsonProperty("amount")
            BigDecimal amount
        ) {
        }

        public record Class4Nics(
            @JsonProperty("totalClass4ChargeableProfits")
            BigDecimal totalClass4ChargeableProfits,

            @JsonProperty("totalClass4Nics")
            BigDecimal totalClass4Nics
        ) {
        }
    }
}
