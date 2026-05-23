package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * Response shape for HMRC Self Assessment Individual Details API v2.
 *
 * <p>Endpoint: {@code GET /individuals/details/{nino}}.
 *
 * <p>Returns the customer's personal details and their per-tax-year ITSA status.
 * HMRC's 2026-05-15 deployment added seven new {@link ItsaStatusReason} codes and
 * renamed the previous "Non-Digital" status to "Digitally Exempt" — both modelled
 * here.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/self-assessment-individual-details-api">
 *     HMRC Self Assessment Individual Details API</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SaIndividualDetailsResponse(
    @JsonProperty("nino") String nino,
    @JsonProperty("mtdbsa") String mtdbsa,
    @JsonProperty("firstName") String firstName,
    @JsonProperty("lastName") String lastName,
    @JsonProperty("dateOfBirth") LocalDate dateOfBirth,
    @JsonProperty("itsaStatusByTaxYear") List<ItsaStatusForTaxYear> itsaStatusByTaxYear
) {

    /**
     * One ITSA status record — one per tax year HMRC has assessed the customer for.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItsaStatusForTaxYear(
        @JsonProperty("taxYear") String taxYear,
        @JsonProperty("status") ItsaStatus status,
        @JsonProperty("reason") ItsaStatusReason reason,
        @JsonProperty("statusEffectiveFrom") LocalDate statusEffectiveFrom
    ) {
    }

    /**
     * ITSA status as returned in the HMRC v2 payload.
     *
     * <p>HMRC renamed the previous {@code "Non-Digital"} status to
     * {@link #DIGITALLY_EXEMPT} on 2026-05-15; callers should not rely on the old
     * spelling anywhere.
     */
    public enum ItsaStatus {
        @JsonProperty("Mandated") MANDATED,
        @JsonProperty("Voluntary") VOLUNTARY,
        @JsonProperty("Annual") ANNUAL,
        @JsonProperty("No Status") NO_STATUS,
        @JsonProperty("Dormant") DORMANT,
        @JsonProperty("Digitally Exempt") DIGITALLY_EXEMPT
    }

    /** @return an empty list if {@code itsaStatusByTaxYear} is {@code null}. */
    public List<ItsaStatusForTaxYear> itsaStatusByTaxYearOrEmpty() {
        return itsaStatusByTaxYear != null ? itsaStatusByTaxYear : List.of();
    }
}
