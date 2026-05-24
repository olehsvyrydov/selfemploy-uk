package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response from retrieving a Self Assessment tax calculation (HMRC Individual Calculations API v8).
 *
 * <p>API: {@code GET /individuals/calculations/self-assessment/{nino}/{taxYear}/{calculationId}}.
 *
 * <p>Modelled against API v8 (versions v5/v6/v7 retired in production on 2026-03-24).
 * Only fields the application currently consumes are explicit; everything else is preserved
 * forward-compatibly via {@code @JsonIgnoreProperties(ignoreUnknown = true)}.
 *
 * <p>V8 additions over earlier versions:
 * <ul>
 *   <li>{@code transitionProfit} — basis period reform (ITTOIA 2005 s.220+; TY 2026/27 is year 4 of 5 of the 5-year spread)</li>
 *   <li>{@code studentLoansAndPostgraduateLoan} — supports Plan Type 5 (new in v8)</li>
 *   <li>{@code capitalGainsTax} — Crypto disposals, Claim/Election Codes, RTT Tax Paid, Unlisted Shares, BADR multi-asset</li>
 * </ul>
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individual-calculations-api">
 *     HMRC Individual Calculations API</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
    NationalInsuranceBreakdown nics,

    /**
     * Basis-period-reform transition profit recognised in this tax year.
     * {@code null} when the customer is not within the 2023/24–2027/28 spread window
     * (or has no transition profit).
     */
    @JsonProperty("transitionProfit")
    BigDecimal transitionProfit,

    /**
     * Additional transition-profit acceleration the customer has elected to bring into
     * charge this year (ITTOIA 2005 s.220+). {@code null} when no acceleration election
     * has been made.
     */
    @JsonProperty("transitionProfitAcceleratedAmount")
    BigDecimal transitionProfitAcceleratedAmount,

    /**
     * Student loan + postgraduate loan repayments. v8 introduces Plan Type 5.
     */
    @JsonProperty("studentLoansAndPostgraduateLoan")
    StudentLoansAndPostgraduateLoan studentLoansAndPostgraduateLoan,

    /**
     * Capital Gains Tax breakdown — v8 surface includes crypto disposals, claim/election
     * codes, RTT Tax Paid, unlisted shares, and BADR multi-asset disposal.
     */
    @JsonProperty("capitalGainsTax")
    CapitalGainsTax capitalGainsTax
) {
    /**
     * Income tax breakdown.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NationalInsuranceBreakdown(
        @JsonProperty("class2Nics")
        Class2Nics class2Nics,

        @JsonProperty("class4Nics")
        Class4Nics class4Nics
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Class2Nics(
            @JsonProperty("amount")
            BigDecimal amount
        ) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Class4Nics(
            @JsonProperty("totalClass4ChargeableProfits")
            BigDecimal totalClass4ChargeableProfits,

            @JsonProperty("totalClass4Nics")
            BigDecimal totalClass4Nics
        ) {
        }
    }

    /**
     * Student loan + postgraduate loan repayments. Plan Type 5 is new in HMRC Calc v8.
     *
     * <p>Plan types HMRC currently recognises: {@code 01}, {@code 02}, {@code 04}, {@code 05}
     * (plus postgraduate). The application surfaces the type and the amount due; HMRC
     * computes the figures.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StudentLoansAndPostgraduateLoan(
        @JsonProperty("planType")
        String planType,

        @JsonProperty("studentLoanRepaymentAmount")
        BigDecimal studentLoanRepaymentAmount,

        @JsonProperty("postgraduateLoanRepaymentAmount")
        BigDecimal postgraduateLoanRepaymentAmount
    ) {
        /**
         * @return {@code true} when this represents the Plan Type 5 cohort newly supported in v8.
         */
        public boolean isPlanType5() {
            return "05".equals(planType);
        }
    }

    /**
     * Capital Gains Tax breakdown. v8 adds explicit support for cryptocurrency assets,
     * Real Time Transaction (RTT) tax paid, unlisted shares & securities, BADR multi-asset
     * disposals, and Claim/Election Codes within the journey.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CapitalGainsTax(
        @JsonProperty("totalTaxableGains")
        BigDecimal totalTaxableGains,

        @JsonProperty("totalCapitalGainsTax")
        BigDecimal totalCapitalGainsTax,

        @JsonProperty("realTimeTransactionTaxPaid")
        BigDecimal realTimeTransactionTaxPaid,

        @JsonProperty("claimOrElectionCodes")
        List<String> claimOrElectionCodes,

        @JsonProperty("cryptoassetsDisposals")
        CryptoassetsDisposals cryptoassetsDisposals,

        @JsonProperty("unlistedSharesAndSecurities")
        UnlistedSharesAndSecurities unlistedSharesAndSecurities,

        @JsonProperty("businessAssetDisposalRelief")
        BusinessAssetDisposalRelief businessAssetDisposalRelief
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record CryptoassetsDisposals(
            @JsonProperty("totalProceeds")
            BigDecimal totalProceeds,

            @JsonProperty("totalAllowableCosts")
            BigDecimal totalAllowableCosts,

            @JsonProperty("totalGains")
            BigDecimal totalGains,

            @JsonProperty("totalLosses")
            BigDecimal totalLosses
        ) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record UnlistedSharesAndSecurities(
            @JsonProperty("totalProceeds")
            BigDecimal totalProceeds,

            @JsonProperty("totalAllowableCosts")
            BigDecimal totalAllowableCosts,

            @JsonProperty("totalGains")
            BigDecimal totalGains
        ) {
        }

        /**
         * BADR (Business Asset Disposal Relief) multi-asset disposal — v8 lets a single
         * BADR claim cover multiple qualifying assets in one disposal.
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record BusinessAssetDisposalRelief(
            @JsonProperty("totalGainsQualifyingForBadr")
            BigDecimal totalGainsQualifyingForBadr,

            @JsonProperty("badrRate")
            BigDecimal badrRate,

            @JsonProperty("badrTaxDue")
            BigDecimal badrTaxDue,

            @JsonProperty("disposals")
            List<BadrAssetDisposal> disposals
        ) {

            @JsonIgnoreProperties(ignoreUnknown = true)
            public record BadrAssetDisposal(
                @JsonProperty("assetDescription")
                String assetDescription,

                @JsonProperty("disposalProceeds")
                BigDecimal disposalProceeds,

                @JsonProperty("gainQualifyingForBadr")
                BigDecimal gainQualifyingForBadr
            ) {
            }

            /**
             * @return number of disposals in this BADR claim (HMRC v8 supports multi-asset
             *         disposals per claim).
             */
            public int assetCount() {
                return disposals != null ? disposals.size() : 0;
            }
        }
    }
}
