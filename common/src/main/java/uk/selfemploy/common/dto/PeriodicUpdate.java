package uk.selfemploy.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for HMRC MTD Periodic Update submission.
 *
 * <p>Represents the data structure required by HMRC's Self Assessment MTD API
 * for quarterly periodic updates.</p>
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/self-assessment-api">HMRC MTD API</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PeriodicUpdate(
    @JsonProperty("periodFromDate") LocalDate periodFromDate,
    @JsonProperty("periodToDate") LocalDate periodToDate,
    @JsonProperty("periodIncome") PeriodIncome periodIncome,
    @JsonProperty("periodExpenses") PeriodExpenses periodExpenses
) {

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
     * Calculates the net profit for this period.
     *
     * @return Total income minus total expenses
     */
    public BigDecimal getNetProfit() {
        BigDecimal totalIncome = periodIncome != null ? periodIncome.getTotal() : BigDecimal.ZERO;
        BigDecimal totalExpenses = periodExpenses != null ? periodExpenses.getTotal() : BigDecimal.ZERO;
        return totalIncome.subtract(totalExpenses);
    }

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

        public BigDecimal getTotal() {
            return turnover.add(other);
        }
    }

    /**
     * Expense breakdown for the period, aligned with SA103F categories.
     *
     * <p>Maps to HMRC MTD API periodExpenses structure.</p>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PeriodExpenses(
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
         */
        public BigDecimal getTotal() {
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
         */
        public BigDecimal getAllowableTotal() {
            return getTotal()
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
