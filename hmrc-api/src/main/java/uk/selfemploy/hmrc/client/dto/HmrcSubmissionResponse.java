package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from HMRC MTD Periodic Update API.
 *
 * @param hmrcReference The unique reference assigned by HMRC
 * @param status The status of the submission (ACCEPTED, REJECTED, etc.)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HmrcSubmissionResponse(
    @JsonProperty("id") String hmrcReference,
    @JsonProperty("status") String status
) {
    /**
     * Checks if the submission was accepted by HMRC.
     */
    public boolean isAccepted() {
        return "ACCEPTED".equalsIgnoreCase(status);
    }
}
