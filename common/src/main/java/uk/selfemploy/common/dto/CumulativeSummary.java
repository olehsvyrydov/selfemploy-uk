package uk.selfemploy.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * DTO for HMRC MTD Cumulative Update submission (v5.0 cumulative endpoint).
 *
 * <p>This DTO is used for tax years 2025-26 onwards with the PUT /cumulative endpoint.
 * Unlike {@link PeriodicUpdate}, this DTO does NOT have a periodDates wrapper - the
 * tax year is provided as a query parameter instead.</p>
 *
 * <h3>Endpoint Differences:</h3>
 * <ul>
 *   <li>Tax year 2024-25 and earlier: POST /period + periodDates in body → use {@link PeriodicUpdate}</li>
 *   <li>Tax year 2025-26 onwards: PUT /cumulative?taxYear=YYYY-YY → use this DTO</li>
 * </ul>
 *
 * <p>Both endpoints use the same Accept header: application/vnd.hmrc.5.0+json</p>
 *
 * @see PeriodicUpdate for the periodDates-based DTO used with older tax years
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/self-employment-business-api/5.0">
 *     HMRC Self-Employment Business API v5.0</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CumulativeSummary(
    @JsonProperty("periodIncome") CumulativeIncome periodIncome,
    @JsonProperty("periodExpenses") CumulativeExpenses periodExpenses
) {

    /**
     * Creates a CumulativeSummary from a PeriodicUpdate.
     *
     * <p>Converts the periodDates-based DTO to the flat structure needed for
     * the cumulative endpoint. The period dates are discarded as they are
     * provided via the taxYear query parameter instead.</p>
     *
     * @param periodicUpdate the PeriodicUpdate to convert
     * @return a new CumulativeSummary with the same income and expenses
     */
    public static CumulativeSummary fromPeriodicUpdate(PeriodicUpdate periodicUpdate) {
        if (periodicUpdate == null) {
            return new CumulativeSummary(null, null);
        }

        CumulativeIncome income = null;
        if (periodicUpdate.periodIncome() != null) {
            income = new CumulativeIncome(
                    periodicUpdate.periodIncome().turnover(),
                    periodicUpdate.periodIncome().other()
            );
        }

        CumulativeExpenses expenses = null;
        if (periodicUpdate.periodExpenses() != null) {
            var pe = periodicUpdate.periodExpenses();
            expenses = new CumulativeExpenses(
                    pe.costOfGoodsBought(),
                    pe.cisPaymentsToSubcontractors(),
                    pe.staffCosts(),
                    pe.travelCosts(),
                    pe.premisesRunningCosts(),
                    pe.maintenanceCosts(),
                    pe.adminCosts(),
                    pe.advertisingCosts(),
                    pe.businessEntertainmentCosts(),
                    pe.interest(),
                    pe.financialCharges(),
                    pe.badDebt(),
                    pe.professionalFees(),
                    pe.depreciation(),
                    pe.other()
            );
        }

        return new CumulativeSummary(income, expenses);
    }

    /**
     * Calculates the net profit for this cumulative period.
     * Named 'calculate' instead of 'get' to prevent Jackson from serializing it.
     *
     * @return Total income minus total expenses
     */
    public BigDecimal calculateNetProfit() {
        BigDecimal totalIncome = periodIncome != null ? periodIncome.calculateTotal() : BigDecimal.ZERO;
        BigDecimal totalExpenses = periodExpenses != null ? periodExpenses.calculateTotal() : BigDecimal.ZERO;
        return totalIncome.subtract(totalExpenses);
    }

    /**
     * Income breakdown for the cumulative period.
     *
     * <p>Same structure as {@link PeriodicUpdate.PeriodIncome} but without the
     * periodDates wrapper context.</p>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CumulativeIncome(
        @JsonProperty("turnover") BigDecimal turnover,
        @JsonProperty("other") BigDecimal other
    ) {
        public CumulativeIncome {
            turnover = turnover != null ? turnover : BigDecimal.ZERO;
            other = other != null ? other : BigDecimal.ZERO;
        }

        public static CumulativeIncome of(BigDecimal turnover, BigDecimal other) {
            return new CumulativeIncome(turnover, other);
        }

        public static CumulativeIncome ofTurnover(BigDecimal turnover) {
            return new CumulativeIncome(turnover, BigDecimal.ZERO);
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
     * Expense breakdown for the cumulative period, aligned with SA103F categories.
     *
     * <p>Same structure as {@link PeriodicUpdate.PeriodExpenses} but without the
     * periodDates wrapper context.</p>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CumulativeExpenses(
        @JsonProperty("costOfGoodsBought") BigDecimal costOfGoodsBought,           // SA103F Box 17
        @JsonProperty("cisPaymentsToSubcontractors") BigDecimal cisPaymentsToSubcontractors, // Box 18
        @JsonProperty("staffCosts") BigDecimal staffCosts,                         // Box 19
        @JsonProperty("travelCosts") BigDecimal travelCosts,                       // Box 20
        @JsonProperty("premisesRunningCosts") BigDecimal premisesRunningCosts,     // Box 21
        @JsonProperty("maintenanceCosts") BigDecimal maintenanceCosts,             // Box 22
        @JsonProperty("adminCosts") BigDecimal adminCosts,                         // Box 23
        @JsonProperty("advertisingCosts") BigDecimal advertisingCosts,             // Box 24
        @JsonProperty("businessEntertainmentCosts") BigDecimal businessEntertainmentCosts, // Not allowable
        @JsonProperty("interest") BigDecimal interest,                             // Box 25
        @JsonProperty("financialCharges") BigDecimal financialCharges,             // Box 26
        @JsonProperty("badDebt") BigDecimal badDebt,                               // Box 27
        @JsonProperty("professionalFees") BigDecimal professionalFees,             // Box 28
        @JsonProperty("depreciation") BigDecimal depreciation,                     // Box 29 (not allowable)
        @JsonProperty("other") BigDecimal other                                    // Box 30
    ) {
        public CumulativeExpenses {
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
         * Creates an empty CumulativeExpenses with all zeros.
         */
        public static CumulativeExpenses empty() {
            return new CumulativeExpenses(
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
         * Calculates total allowable expenses (excludes depreciation and business entertainment).
         * Named 'calculate' instead of 'get' to prevent Jackson from serializing it.
         */
        public BigDecimal calculateAllowableTotal() {
            return calculateTotal()
                .subtract(depreciation)
                .subtract(businessEntertainmentCosts);
        }

        /**
         * Builder for CumulativeExpenses.
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

            public CumulativeExpenses build() {
                return new CumulativeExpenses(
                    costOfGoodsBought, cisPaymentsToSubcontractors, staffCosts, travelCosts,
                    premisesRunningCosts, maintenanceCosts, adminCosts, advertisingCosts,
                    businessEntertainmentCosts, interest, financialCharges, badDebt,
                    professionalFees, depreciation, other
                );
            }
        }
    }
}
