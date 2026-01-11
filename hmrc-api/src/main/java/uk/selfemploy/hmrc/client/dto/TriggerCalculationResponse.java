package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from triggering a Self Assessment tax calculation.
 *
 * <p>Contains the calculationId which is used to retrieve the calculation result.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individual-calculations-api">
 *     HMRC Individual Calculations API</a>
 */
public record TriggerCalculationResponse(
    @JsonProperty("id")
    String calculationId
) {
}
