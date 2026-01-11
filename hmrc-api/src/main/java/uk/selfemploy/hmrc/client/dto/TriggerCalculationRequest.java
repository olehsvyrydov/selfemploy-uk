package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request to trigger a Self Assessment tax calculation.
 *
 * <p>API: POST /individuals/calculations/self-assessment/{nino}/{taxYear}
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individual-calculations-api">
 *     HMRC Individual Calculations API</a>
 */
public record TriggerCalculationRequest(
    @JsonProperty("crystallise")
    boolean crystallise
) {
    /**
     * Creates a request for final annual calculation (crystallisation).
     */
    public static TriggerCalculationRequest forAnnualSubmission() {
        return new TriggerCalculationRequest(true);
    }

    /**
     * Creates a request for in-year calculation (non-crystallised).
     */
    public static TriggerCalculationRequest forInYearEstimate() {
        return new TriggerCalculationRequest(false);
    }
}
