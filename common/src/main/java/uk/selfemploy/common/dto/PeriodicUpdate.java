package uk.selfemploy.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for HMRC MTD Periodic Update submission.
 *
 * <p>Represents the data structure required by HMRC's Self Assessment MTD API
 * for quarterly periodic updates. For API v5.0, dates must be wrapped in a
 * periodDates object.</p>
 *
 * <p><strong>Deprecated HMRC fields intentionally absent</strong> (changelog
 * 2026-04-24, 2026-05-12):
 * neither {@code averagingAdjustment} nor {@code adjustments.overlapReliefUsed}
 * is modelled here. Submitting {@code overlapReliefUsed} for tax year 2024-25 or
 * later triggers {@code RULE_OVERLAP_RELIEF_USED_NOT_ALLOWED}. The
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} declaration below ensures
 * historical persisted JSON containing these fields still deserialises without
 * loss; the contract is enforced by reflection tests in
 * {@code MtdPeriodicUpdateClientTest} that fail if either component is
 * re-introduced.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/self-employment-business-api/5.0">HMRC MTD API</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PeriodicUpdate(
    @JsonProperty("periodDates") PeriodDates periodDates,
    @JsonProperty("periodIncome") PeriodIncome periodIncome,
    @JsonProperty("periodExpenses") PeriodExpenses periodExpenses,
    // Omitted rather than serialized as null: the request schema types this as an object, so a
    // literal null is a payload HMRC rejects outright.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("periodDisallowableExpenses") DisallowableExpenses periodDisallowableExpenses
) {

    /**
     * Constructor that takes individual dates (for backward compatibility).
     * Creates the PeriodDates wrapper automatically.
     */
    public PeriodicUpdate(LocalDate periodFromDate, LocalDate periodToDate,
                          PeriodIncome periodIncome, PeriodExpenses periodExpenses) {
        this(periodFromDate, periodToDate, periodIncome, periodExpenses, null);
    }

    /**
     * Constructor that takes individual dates and both expense columns.
     * Creates the PeriodDates wrapper automatically.
     */
    public PeriodicUpdate(LocalDate periodFromDate, LocalDate periodToDate,
                          PeriodIncome periodIncome, PeriodExpenses periodExpenses,
                          DisallowableExpenses periodDisallowableExpenses) {
        this(new PeriodDates(periodFromDate, periodToDate), periodIncome, periodExpenses,
             periodDisallowableExpenses);
    }

    /** Three-argument form, for callers that carry no disallowable spend. */
    public PeriodicUpdate(PeriodDates periodDates, PeriodIncome periodIncome,
                          PeriodExpenses periodExpenses) {
        this(periodDates, periodIncome, periodExpenses, null);
    }

    /**
     * Creates a PeriodicUpdate for a specific quarter.
     *
     * @param taxYear The tax year
     * @param quarter The quarter
     * @param income The income data
     * @param expenses The expenses data
     * @return A new PeriodicUpdate instance
     */
    public static PeriodicUpdate forQuarter(TaxYear taxYear, Quarter quarter,
                                            PeriodIncome income, PeriodExpenses expenses) {
        return new PeriodicUpdate(
            quarter.getStartDate(taxYear),
            quarter.getEndDate(taxYear),
            income,
            expenses
        );
    }

    /**
     * Returns the period start date.
     */
    public LocalDate periodFromDate() {
        return periodDates != null ? periodDates.periodStartDate() : null;
    }

    /**
     * Returns the period end date.
     */
    public LocalDate periodToDate() {
        return periodDates != null ? periodDates.periodEndDate() : null;
    }

    /**
     * The profit this payload declares, derived the way HMRC derives it from the two columns.
     * Named 'calculate' instead of 'get' to prevent Jackson from serializing it.
     *
     * @return income, less the declared spend, plus back the part of it that cannot be claimed
     */
    public BigDecimal calculateNetProfit() {
        BigDecimal totalIncome = periodIncome != null ? periodIncome.calculateTotal() : BigDecimal.ZERO;
        BigDecimal totalExpenses = periodExpenses != null ? periodExpenses.calculateTotal() : BigDecimal.ZERO;
        BigDecimal disallowed = periodDisallowableExpenses != null
                ? periodDisallowableExpenses.calculateTotal() : BigDecimal.ZERO;
        return totalIncome.subtract(totalExpenses).add(disallowed);
    }

    /**
     * Period dates wrapper for HMRC API v5.0.
     * HMRC expects dates to be nested in a periodDates object.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PeriodDates(
        @JsonProperty("periodStartDate") LocalDate periodStartDate,
        @JsonProperty("periodEndDate") LocalDate periodEndDate
    ) {}

    /**
     * Income breakdown for the period.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PeriodIncome(
        @JsonProperty("turnover") BigDecimal turnover,
        @JsonProperty("other") BigDecimal other
    ) {
        public PeriodIncome {
            turnover = turnover != null ? turnover : BigDecimal.ZERO;
            other = other != null ? other : BigDecimal.ZERO;
        }

        public static PeriodIncome of(BigDecimal turnover, BigDecimal other) {
            return new PeriodIncome(turnover, other);
        }

        public static PeriodIncome ofTurnover(BigDecimal turnover) {
            return new PeriodIncome(turnover, BigDecimal.ZERO);
        }

        /**
         * Returns total income.
         * Named 'calculate' instead of 'get' to prevent Jackson from serializing it.
         */
        public BigDecimal calculateTotal() {
            return turnover.add(other);
        }
    }

    /**
     * The first column of SA103F: what was spent in each category, boxes 17 to 30.
     *
     * <p>This is the whole spend, not the claimable part of it. The part that cannot be claimed is
     * declared alongside in {@link DisallowableExpenses}, and HMRC subtracts one from the other to
     * reach the deduction — so narrowing this column to allowable spend without emptying that one
     * files a smaller deduction than the records support.
     *
     * <p>The component names are the Java-side SA103F names and the {@code @JsonProperty} values
     * are the wire names the v5 request schemas define. Where the two differ, the wire name is the
     * only one HMRC sees; the pairing is asserted against HMRC's own schemas by
     * {@code HmrcPayloadContractTest}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PeriodExpenses(
        @JsonProperty("costOfGoods") BigDecimal costOfGoodsBought,
        @JsonProperty("paymentsToSubcontractors") BigDecimal cisPaymentsToSubcontractors,
        @JsonProperty("wagesAndStaffCosts") BigDecimal staffCosts,
        @JsonProperty("carVanTravelExpenses") BigDecimal travelCosts,
        @JsonProperty("premisesRunningCosts") BigDecimal premisesRunningCosts,
        @JsonProperty("maintenanceCosts") BigDecimal maintenanceCosts,
        @JsonProperty("adminCosts") BigDecimal adminCosts,
        @JsonProperty("advertisingCosts") BigDecimal advertisingCosts,
        @JsonProperty("businessEntertainmentCosts") BigDecimal businessEntertainmentCosts,
        @JsonProperty("interestOnBankOtherLoans") BigDecimal interest,
        @JsonProperty("financeCharges") BigDecimal financialCharges,
        @JsonProperty("irrecoverableDebts") BigDecimal badDebt,
        @JsonProperty("professionalFees") BigDecimal professionalFees,
        @JsonProperty("depreciation") BigDecimal depreciation,
        @JsonProperty("otherExpenses") BigDecimal other
    ) {
        public PeriodExpenses {
            // Normalize nulls to zero
            costOfGoodsBought = costOfGoodsBought != null ? costOfGoodsBought : BigDecimal.ZERO;
            cisPaymentsToSubcontractors = cisPaymentsToSubcontractors != null ? cisPaymentsToSubcontractors : BigDecimal.ZERO;
            staffCosts = staffCosts != null ? staffCosts : BigDecimal.ZERO;
            travelCosts = travelCosts != null ? travelCosts : BigDecimal.ZERO;
            premisesRunningCosts = premisesRunningCosts != null ? premisesRunningCosts : BigDecimal.ZERO;
            maintenanceCosts = maintenanceCosts != null ? maintenanceCosts : BigDecimal.ZERO;
            adminCosts = adminCosts != null ? adminCosts : BigDecimal.ZERO;
            advertisingCosts = advertisingCosts != null ? advertisingCosts : BigDecimal.ZERO;
            businessEntertainmentCosts = businessEntertainmentCosts != null ? businessEntertainmentCosts : BigDecimal.ZERO;
            interest = interest != null ? interest : BigDecimal.ZERO;
            financialCharges = financialCharges != null ? financialCharges : BigDecimal.ZERO;
            badDebt = badDebt != null ? badDebt : BigDecimal.ZERO;
            professionalFees = professionalFees != null ? professionalFees : BigDecimal.ZERO;
            depreciation = depreciation != null ? depreciation : BigDecimal.ZERO;
            other = other != null ? other : BigDecimal.ZERO;
        }

        /**
         * Creates an empty PeriodExpenses with all zeros.
         */
        public static PeriodExpenses empty() {
            return new PeriodExpenses(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            );
        }

        /**
         * Calculates total expenses.
         * Named 'calculate' instead of 'get' to prevent Jackson from serializing it.
         */
        public BigDecimal calculateTotal() {
            return costOfGoodsBought
                .add(cisPaymentsToSubcontractors)
                .add(staffCosts)
                .add(travelCosts)
                .add(premisesRunningCosts)
                .add(maintenanceCosts)
                .add(adminCosts)
                .add(advertisingCosts)
                .add(businessEntertainmentCosts)
                .add(interest)
                .add(financialCharges)
                .add(badDebt)
                .add(professionalFees)
                .add(depreciation)
                .add(other);
        }

        /**
         * The declared spend less the two categories that are disallowable in full.
         *
         * <p>Not the claim: a category disallowed only in part — a phone bill used 60% for business
         * — is counted here in full. The claim is this column less
         * {@link PeriodicUpdate#periodDisallowableExpenses()}, which is the only figure that
         * accounts for a partial share. Named 'calculate' instead of 'get' to prevent Jackson from
         * serializing it.
         */
        public BigDecimal calculateAllowableTotal() {
            return calculateTotal()
                .subtract(depreciation)
                .subtract(businessEntertainmentCosts);
        }

        /**
         * Builder for PeriodExpenses.
         */
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private BigDecimal costOfGoodsBought = BigDecimal.ZERO;
            private BigDecimal cisPaymentsToSubcontractors = BigDecimal.ZERO;
            private BigDecimal staffCosts = BigDecimal.ZERO;
            private BigDecimal travelCosts = BigDecimal.ZERO;
            private BigDecimal premisesRunningCosts = BigDecimal.ZERO;
            private BigDecimal maintenanceCosts = BigDecimal.ZERO;
            private BigDecimal adminCosts = BigDecimal.ZERO;
            private BigDecimal advertisingCosts = BigDecimal.ZERO;
            private BigDecimal businessEntertainmentCosts = BigDecimal.ZERO;
            private BigDecimal interest = BigDecimal.ZERO;
            private BigDecimal financialCharges = BigDecimal.ZERO;
            private BigDecimal badDebt = BigDecimal.ZERO;
            private BigDecimal professionalFees = BigDecimal.ZERO;
            private BigDecimal depreciation = BigDecimal.ZERO;
            private BigDecimal other = BigDecimal.ZERO;

            public Builder costOfGoodsBought(BigDecimal value) { this.costOfGoodsBought = value; return this; }
            public Builder cisPaymentsToSubcontractors(BigDecimal value) { this.cisPaymentsToSubcontractors = value; return this; }
            public Builder staffCosts(BigDecimal value) { this.staffCosts = value; return this; }
            public Builder travelCosts(BigDecimal value) { this.travelCosts = value; return this; }
            public Builder premisesRunningCosts(BigDecimal value) { this.premisesRunningCosts = value; return this; }
            public Builder maintenanceCosts(BigDecimal value) { this.maintenanceCosts = value; return this; }
            public Builder adminCosts(BigDecimal value) { this.adminCosts = value; return this; }
            public Builder advertisingCosts(BigDecimal value) { this.advertisingCosts = value; return this; }
            public Builder businessEntertainmentCosts(BigDecimal value) { this.businessEntertainmentCosts = value; return this; }
            public Builder interest(BigDecimal value) { this.interest = value; return this; }
            public Builder financialCharges(BigDecimal value) { this.financialCharges = value; return this; }
            public Builder badDebt(BigDecimal value) { this.badDebt = value; return this; }
            public Builder professionalFees(BigDecimal value) { this.professionalFees = value; return this; }
            public Builder depreciation(BigDecimal value) { this.depreciation = value; return this; }
            public Builder other(BigDecimal value) { this.other = value; return this; }

            public PeriodExpenses build() {
                return new PeriodExpenses(
                    costOfGoodsBought, cisPaymentsToSubcontractors, staffCosts, travelCosts,
                    premisesRunningCosts, maintenanceCosts, adminCosts, advertisingCosts,
                    businessEntertainmentCosts, interest, financialCharges, badDebt,
                    professionalFees, depreciation, other
                );
            }
        }
    }
}
