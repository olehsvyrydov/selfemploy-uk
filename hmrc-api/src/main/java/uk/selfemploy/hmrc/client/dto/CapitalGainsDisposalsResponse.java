package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response shape for HMRC Individuals Disposals API v3 — retrieve disposals
 * for tax year.
 *
 * <p>Endpoint: {@code GET /individuals/disposals/residential-property/{nino}/{taxYear}}.
 *
 * <p>Each entry represents a single chargeable disposal the customer has
 * submitted, including UK residential property and — from v3 — crypto-asset
 * disposals (HMRC Cryptoassets Manual CRYPTO20250). The {@code assetType}
 * field discriminates: typical values are {@code residential-property},
 * {@code crypto-asset}, and {@code other}.
 *
 * <p>This is the <em>customer-submitted</em> disposal shape and must not be
 * confused with the calculator's {@code CapitalGainsTax} response shape, which
 * expresses computed liability rather than source disposals.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individuals-disposals-income-api">
 *     HMRC Individuals Disposals API</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CapitalGainsDisposalsResponse(
    @JsonProperty("taxYear") String taxYear,
    @JsonProperty("disposals") List<Disposal> disposals
) {

    /**
     * A single chargeable disposal.
     *
     * <p>{@code assetType} discriminates: {@code residential-property},
     * {@code crypto-asset}, or {@code other}. For crypto disposals
     * {@code assetDescription} typically carries the token name
     * (e.g. {@code "BTC"}).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Disposal(
        @JsonProperty("disposalId") String disposalId,
        @JsonProperty("assetType") String assetType,
        @JsonProperty("assetDescription") String assetDescription,
        @JsonProperty("acquisitionDate") LocalDate acquisitionDate,
        @JsonProperty("disposalDate") LocalDate disposalDate,
        @JsonProperty("disposalProceeds") BigDecimal disposalProceeds,
        @JsonProperty("allowableCosts") BigDecimal allowableCosts,
        @JsonProperty("gain") BigDecimal gain,
        @JsonProperty("loss") BigDecimal loss
    ) {
        /** @return {@code true} when this disposal is a crypto-asset under HMRC v3. */
        public boolean isCryptoAsset() {
            return "crypto-asset".equalsIgnoreCase(assetType);
        }
    }

    /** Convenience accessor returning an empty list if {@code disposals} is {@code null}. */
    public List<Disposal> disposalsOrEmpty() {
        return disposals != null ? disposals : List.of();
    }
}
