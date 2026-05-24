package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * Response shape for HMRC Property Business API v6 — list income sources.
 *
 * <p>Endpoint: {@code GET /individuals/business/property/{nino}}.
 *
 * <p>Under v6 the response is a flat list of income-sources. Foreign-property
 * income sources are represented <strong>one record per property</strong>
 * (carrying an ISO-3166-1 alpha-3 {@code countryCode}), rather than rolled up
 * into a single foreign-property block. UK income sources are
 * {@code uk-property} (covers both UK-FHL and UK non-FHL under v6's revised
 * model; the {@code uk-property-fhl} sub-type is retained where the customer
 * has elected to keep an FHL designation grandfathered after FA 2024 abolition).
 *
 * <p>Per-property income figures (rent, premiums, expenses) are not modelled
 * by this DTO — only the structural address-shape and accounting-period
 * boundaries are surfaced.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/property-business-api">
 *     HMRC Property Business API</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PropertyBusinessResponse(
    @JsonProperty("incomeSources") List<IncomeSource> incomeSources
) {

    /**
     * A single property income-source. For foreign properties the
     * {@code countryCode} is populated; for UK properties it is {@code null}
     * and {@code address} is the UK postcode-bearing form.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IncomeSource(
        @JsonProperty("incomeSourceId") String incomeSourceId,
        @JsonProperty("incomeSourceType") String incomeSourceType,
        @JsonProperty("countryCode") String countryCode,
        @JsonProperty("commencementDate") LocalDate commencementDate,
        @JsonProperty("cessationDate") LocalDate cessationDate,
        @JsonProperty("accountingPeriod") AccountingPeriod accountingPeriod,
        @JsonProperty("address") PropertyAddress address
    ) {
        /** @return {@code true} if this is a foreign-property income source. */
        public boolean isForeign() {
            return "foreign-property".equalsIgnoreCase(incomeSourceType);
        }
    }

    /** Accounting period boundaries for a single income source. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AccountingPeriod(
        @JsonProperty("startDate") LocalDate startDate,
        @JsonProperty("endDate") LocalDate endDate
    ) {
    }

    /**
     * Per-property address. UK fields ({@code postcode}) and foreign fields
     * ({@code countryName}) are both modelled — unused fields are {@code null}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PropertyAddress(
        @JsonProperty("addressLine1") String addressLine1,
        @JsonProperty("addressLine2") String addressLine2,
        @JsonProperty("townOrCity") String townOrCity,
        @JsonProperty("county") String county,
        @JsonProperty("postcode") String postcode,
        @JsonProperty("countryName") String countryName
    ) {
    }

    /** Convenience accessor returning an empty list if {@code incomeSources} is {@code null}. */
    public List<IncomeSource> incomeSourcesOrEmpty() {
        return incomeSources != null ? incomeSources : List.of();
    }
}
