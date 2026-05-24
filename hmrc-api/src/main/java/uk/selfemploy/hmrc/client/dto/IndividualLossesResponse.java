package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response shape for HMRC Individual Losses API v6 — list brought-forward losses.
 *
 * <p>Endpoint: {@code GET /individuals/losses/{nino}/brought-forward-losses}.
 *
 * <p>Only headline fields are modelled (loss id, income source, loss type,
 * tax year claimed for, last-modified timestamp, and the amount). The HMRC
 * payload may carry additional links / metadata; {@code @JsonIgnoreProperties}
 * tolerates them transparently.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individual-losses-api">
 *     HMRC Individual Losses API</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IndividualLossesResponse(
    @JsonProperty("losses") List<BroughtForwardLoss> losses
) {

    /**
     * A single brought-forward loss entry.
     *
     * <p>{@code lossType} values: {@code self-employment},
     * {@code self-employment-class4}, {@code uk-property-non-fhl}, or
     * {@code foreign-property}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BroughtForwardLoss(
        @JsonProperty("lossId") String lossId,
        @JsonProperty("businessId") String businessId,
        @JsonProperty("incomeSourceId") String incomeSourceId,
        @JsonProperty("lossType") String lossType,
        @JsonProperty("taxYearClaimedFor") String taxYearClaimedFor,
        @JsonProperty("lossAmount") BigDecimal lossAmount,
        @JsonProperty("lastModified") OffsetDateTime lastModified
    ) {
    }

    /** Convenience accessor returning an empty list if {@code losses} is {@code null}. */
    public List<BroughtForwardLoss> lossesOrEmpty() {
        return losses != null ? losses : List.of();
    }
}
