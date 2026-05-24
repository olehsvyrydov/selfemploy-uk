package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Request / response body for HMRC Individuals Reliefs API v3.
 *
 * <p>HMRC's 2026-03-24 v3 production deployment <strong>removed all non-UK
 * charitable giving fields</strong> — relief is now only available for gifts to
 * UK charities and Community Amateur Sports Clubs (CASCs) under
 * ITA 2007 s.413 / CTM09000. Existing historical records (TY 2024/25 and 2025/26)
 * remain readable from HMRC but cannot accept new amendments containing the
 * removed fields.
 *
 * <p>This DTO deliberately models only the UK-side fields. Any attempt to
 * deserialize a legacy payload containing {@code nonUkCharitiesCharityNames},
 * {@code giftsToOverseasCharities}, {@code sharesOrSecuritiesGiftedToNonUkCharities},
 * or similar legacy fields will quietly drop them via {@code @JsonIgnoreProperties}
 * — readers see the UK figures; writers cannot accidentally re-introduce the
 * removed fields because they are not part of the type.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individuals-reliefs-api">
 *     HMRC Individuals Reliefs API</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Reliefs(
    @JsonProperty("giftAidPayments") GiftAidPayments giftAidPayments,
    @JsonProperty("gifts") Gifts gifts
) {

    /**
     * Gift Aid relief on cash donations to UK charities.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GiftAidPayments(
        @JsonProperty("currentYearAmount") BigDecimal currentYearAmount,
        @JsonProperty("oneOffCurrentYearAmount") BigDecimal oneOffCurrentYearAmount,
        @JsonProperty("amountTreatedAsPreviousYear") BigDecimal amountTreatedAsPreviousYear,
        @JsonProperty("currentYearTreatedAsPreviousYear") BigDecimal currentYearTreatedAsPreviousYear
    ) {
    }

    /**
     * Relief for gifts of qualifying investments (shares, securities, land) to
     * UK charities and CASCs. {@code investmentsAmount} covers UK-listed
     * investments; {@code landAndBuildings} covers UK real property; other
     * qualifying UK gifts under {@code sharesOrSecurities}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Gifts(
        @JsonProperty("investmentsAmount") BigDecimal investmentsAmount,
        @JsonProperty("landAndBuildings") BigDecimal landAndBuildings,
        @JsonProperty("sharesOrSecurities") BigDecimal sharesOrSecurities
    ) {
    }
}
