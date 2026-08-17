package uk.selfemploy.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * The second column of SA103F: the part of each expense that cannot be claimed for tax purposes.
 *
 * <p>SA103F reports expenses twice. Boxes 17 to 30 carry what was spent, and boxes 32 to 45 carry
 * the disallowable portion of the same spend; HMRC derives the deduction by subtracting the second
 * from the first. Filing only the first column therefore declares the whole spend as deductible.
 *
 * <p>Shared by {@link PeriodicUpdate} and {@link CumulativeSummary} because the
 * {@code periodDisallowableExpenses} object is defined with the same property names in the period
 * and cumulative request schemas alike.
 *
 * <p>The component names are the Java-side SA103F names; the wire names are the
 * {@code @JsonProperty} values, which are the only thing HMRC sees and the only thing the request
 * schemas define.
 *
 * @param costOfGoods disallowed part of goods bought for resale or used
 * @param cisPaymentsToSubcontractors disallowed part of payments to construction industry subcontractors
 * @param staffCosts disallowed part of wages, salaries and other staff costs
 * @param travelCosts disallowed part of car, van and travel expenses
 * @param premisesRunningCosts disallowed part of rent, rates, power and insurance costs
 * @param maintenanceCosts disallowed part of repairs and renewals of property and equipment
 * @param adminCosts disallowed part of phone, stationery and other office costs
 * @param advertisingCosts disallowed part of advertising costs
 * @param businessEntertainmentCosts disallowed part of business entertainment, which is all of it
 * @param interest disallowed part of interest on bank and other loans
 * @param financialCharges disallowed part of bank, credit card and other financial charges
 * @param badDebt disallowed part of irrecoverable debts written off
 * @param professionalFees disallowed part of accountancy, legal and other professional fees
 * @param depreciation disallowed part of depreciation and loss or profit on sale of assets, which is all of it
 * @param other disallowed part of other business expenses
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/self-employment-business-api/5.0">
 *     HMRC Self-Employment Business API v5.0</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DisallowableExpenses(
    @JsonProperty("costOfGoodsDisallowable") BigDecimal costOfGoods,
    @JsonProperty("paymentsToSubcontractorsDisallowable") BigDecimal cisPaymentsToSubcontractors,
    @JsonProperty("wagesAndStaffCostsDisallowable") BigDecimal staffCosts,
    @JsonProperty("carVanTravelExpensesDisallowable") BigDecimal travelCosts,
    @JsonProperty("premisesRunningCostsDisallowable") BigDecimal premisesRunningCosts,
    @JsonProperty("maintenanceCostsDisallowable") BigDecimal maintenanceCosts,
    @JsonProperty("adminCostsDisallowable") BigDecimal adminCosts,
    @JsonProperty("advertisingCostsDisallowable") BigDecimal advertisingCosts,
    @JsonProperty("businessEntertainmentCostsDisallowable") BigDecimal businessEntertainmentCosts,
    @JsonProperty("interestOnBankOtherLoansDisallowable") BigDecimal interest,
    @JsonProperty("financeChargesDisallowable") BigDecimal financialCharges,
    @JsonProperty("irrecoverableDebtsDisallowable") BigDecimal badDebt,
    @JsonProperty("professionalFeesDisallowable") BigDecimal professionalFees,
    @JsonProperty("depreciationDisallowable") BigDecimal depreciation,
    @JsonProperty("otherExpensesDisallowable") BigDecimal other
) {

    public DisallowableExpenses {
        costOfGoods = costOfGoods != null ? costOfGoods : BigDecimal.ZERO;
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

    /** A period in which nothing spent was disallowed. */
    public static DisallowableExpenses none() {
        return builder().build();
    }

    /** The total that HMRC will subtract from the declared spend. */
    public BigDecimal calculateTotal() {
        return costOfGoods
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

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for DisallowableExpenses. */
    public static class Builder {
        private BigDecimal costOfGoods = BigDecimal.ZERO;
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

        public Builder costOfGoods(BigDecimal value) { this.costOfGoods = value; return this; }
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

        public DisallowableExpenses build() {
            return new DisallowableExpenses(
                costOfGoods, cisPaymentsToSubcontractors, staffCosts, travelCosts,
                premisesRunningCosts, maintenanceCosts, adminCosts, advertisingCosts,
                businessEntertainmentCosts, interest, financialCharges, badDebt,
                professionalFees, depreciation, other
            );
        }
    }
}
