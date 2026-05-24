package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Request body for a single state benefit submission against HMRC Individuals
 * State Benefits API v2.
 *
 * <p><strong>New business validation rule:</strong> HMRC's
 * 2026-03-24 v2 deployment rejects any submission where {@code taxPaid} is populated
 * AND {@code benefitType} is one of {@link BenefitType#STATE_PENSION},
 * {@link BenefitType#BEREAVEMENT_ALLOWANCE}, or {@link BenefitType#OTHER_STATE_BENEFITS}.
 * These benefits are paid gross by DWP — tax is collected via the customer's main
 * PAYE coding or Self Assessment liability, never deducted by the payer. The
 * {@link #validate()} method enforces the rule client-side so callers do not need
 * to round-trip to HMRC to learn the request is invalid.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individuals-state-benefits-api">
 *     HMRC Individuals State Benefits API</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StateBenefit(
    @JsonProperty("benefitType") BenefitType benefitType,
    @JsonProperty("startDate") LocalDate startDate,
    @JsonProperty("endDate") LocalDate endDate,
    @JsonProperty("amount") BigDecimal amount,
    @JsonProperty("taxPaid") BigDecimal taxPaid
) {

    /**
     * Benefit types HMRC currently accepts on Individuals State Benefits v2.
     * The {@link #TAX_PAID_PROHIBITED} subset cannot carry a {@code taxPaid} value
     * (DWP pays them gross).
     */
    public enum BenefitType {
        @JsonProperty("statePension") STATE_PENSION("statePension"),
        @JsonProperty("statePensionLumpSum") STATE_PENSION_LUMP_SUM("statePensionLumpSum"),
        @JsonProperty("employmentSupportAllowance") EMPLOYMENT_SUPPORT_ALLOWANCE("employmentSupportAllowance"),
        @JsonProperty("jobSeekersAllowance") JOB_SEEKERS_ALLOWANCE("jobSeekersAllowance"),
        @JsonProperty("incapacityBenefit") INCAPACITY_BENEFIT("incapacityBenefit"),
        @JsonProperty("bereavementAllowance") BEREAVEMENT_ALLOWANCE("bereavementAllowance"),
        @JsonProperty("otherStateBenefits") OTHER_STATE_BENEFITS("otherStateBenefits");

        /**
         * Subset of benefit types HMRC v2 rejects with {@code taxPaid} populated
         * (paid gross by DWP).
         */
        public static final Set<BenefitType> TAX_PAID_PROHIBITED = Set.of(
            STATE_PENSION, BEREAVEMENT_ALLOWANCE, OTHER_STATE_BENEFITS
        );

        private final String hmrcLabel;

        BenefitType(String hmrcLabel) {
            this.hmrcLabel = hmrcLabel;
        }

        @JsonValue
        public String hmrcLabel() {
            return hmrcLabel;
        }

        @JsonCreator
        public static BenefitType fromHmrcLabel(String label) {
            if (label == null) return null;
            for (BenefitType type : values()) {
                if (type.hmrcLabel.equalsIgnoreCase(label)) return type;
            }
            throw new IllegalArgumentException("Unknown benefit type: " + label);
        }
    }

    /**
     * Validates this state benefit against the HMRC v2 business rules.
     *
     * @throws IllegalArgumentException if {@code taxPaid} is populated and the
     *         {@code benefitType} is in {@link BenefitType#TAX_PAID_PROHIBITED}.
     */
    public void validate() {
        if (taxPaid != null && benefitType != null && BenefitType.TAX_PAID_PROHIBITED.contains(benefitType)) {
            throw new IllegalArgumentException(
                "HMRC Individuals State Benefits v2 rejects taxPaid for benefitType="
                    + benefitType.hmrcLabel()
                    + " — this benefit is paid gross by DWP. Leave taxPaid null and report any tax due"
                    + " via the customer's main Self Assessment liability instead.");
        }
    }
}
