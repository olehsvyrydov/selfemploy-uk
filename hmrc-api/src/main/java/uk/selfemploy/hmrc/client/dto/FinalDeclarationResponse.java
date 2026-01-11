package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Response from submitting a final Self Assessment declaration.
 *
 * <p>Contains the confirmation reference and timestamp from HMRC.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individual-calculations-api">
 *     HMRC Individual Calculations API</a>
 */
public record FinalDeclarationResponse(
    @JsonProperty("chargeReference")
    String chargeReference,

    @JsonProperty("declarationTimestamp")
    LocalDateTime declarationTimestamp
) {
}
