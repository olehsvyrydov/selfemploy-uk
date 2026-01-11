package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request to submit a final Self Assessment declaration.
 *
 * <p>API: POST /individuals/declarations/self-assessment/{nino}/{taxYear}
 *
 * <p>This is the final step in the annual submission process after the user
 * has reviewed and confirmed the tax calculation.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individual-calculations-api">
 *     HMRC Individual Calculations API</a>
 */
public record FinalDeclarationRequest(
    @JsonProperty("calculationId")
    String calculationId
) {
}
