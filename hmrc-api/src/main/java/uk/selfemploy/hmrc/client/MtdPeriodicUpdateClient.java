package uk.selfemploy.hmrc.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import uk.selfemploy.common.dto.CumulativeSummary;
import uk.selfemploy.common.dto.PeriodicUpdate;
import uk.selfemploy.hmrc.client.dto.HmrcSubmissionResponse;

/**
 * REST client for HMRC MTD Self-Employment Business API v5 (periodic + cumulative updates).
 *
 * <p>Submits quarterly periodic updates as required by Making Tax Digital.</p>
 *
 * <p><strong>Deprecated fields (HMRC changelog 2026-04-24 and 2026-05-12):</strong>
 * The {@code averagingAdjustment} field is deprecated across all tax years for the
 * Self-Employment Business v5 response bodies, and {@code adjustments.overlapReliefUsed}
 * is deprecated for tax years 2024-25 and later. This client and its DTOs
 * ({@link uk.selfemploy.common.dto.PeriodicUpdate},
 * {@link uk.selfemploy.common.dto.CumulativeSummary}) intentionally model NEITHER
 * field, so the application can never submit them on the wire. Historical persisted
 * JSON containing these fields is tolerated by {@code @JsonIgnoreProperties} on the
 * DTO records. If HMRC returns {@link #ERROR_OVERLAP_RELIEF_USED_NOT_ALLOWED} the
 * caller must surface the user-facing remediation guidance described on the constant.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/self-employment-business-api">
 *     HMRC Self-Employment Business API</a>
 * @see <a href="https://github.com/hmrc/income-tax-mtd-changelog">HMRC MTD changelog</a>
 */
@RegisterRestClient(configKey = "hmrc-mtd-api")
@RegisterClientHeaders(HmrcHeaderFactory.class)
@ClientHeaderParam(name = "Accept", value = "application/vnd.hmrc.5.0+json")
@Path("/individuals/business/self-employment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface MtdPeriodicUpdateClient {

    /**
     * HMRC error code introduced 2026-05-12 — returned when a Self-Employment
     * Business v5 submission includes {@code adjustments.overlapReliefUsed} for
     * tax year 2024-25 or later. Overlap relief was abolished by basis-period
     * reform; the application MUST present a user-facing explanation pointing
     * the user at their transition-profit figure rather than re-trying the
     * submission with the same value.
     *
     * <p>This constant exists primarily so {@link uk.selfemploy.hmrc.exception
     * HMRC error handlers} can match by symbolic name, but in practice our
     * outgoing DTOs cannot encode {@code overlapReliefUsed} at all — see the
     * class-level Javadoc and the contract tests in
     * {@code MtdPeriodicUpdateClientTest}.
     */
    String ERROR_OVERLAP_RELIEF_USED_NOT_ALLOWED = "RULE_OVERLAP_RELIEF_USED_NOT_ALLOWED";

    /**
     * Submits a periodic update for a self-employment business.
     *
     * @param nino National Insurance Number
     * @param businessId HMRC business ID
     * @param authorization Bearer token
     * @param periodicUpdate The update data
     * @return HMRC submission response with reference
     */
    @POST
    @Path("/{nino}/{businessId}/period")
    HmrcSubmissionResponse submitPeriodicUpdate(
            @PathParam("nino") String nino,
            @PathParam("businessId") String businessId,
            @HeaderParam("Authorization") String authorization,
            PeriodicUpdate periodicUpdate
    );

    /**
     * Amends an existing periodic update.
     *
     * @param nino National Insurance Number
     * @param businessId HMRC business ID
     * @param periodId The period ID to amend
     * @param authorization Bearer token
     * @param periodicUpdate The updated data
     * @return HMRC submission response
     */
    @PUT
    @Path("/{nino}/{businessId}/period/{periodId}")
    HmrcSubmissionResponse amendPeriodicUpdate(
            @PathParam("nino") String nino,
            @PathParam("businessId") String businessId,
            @PathParam("periodId") String periodId,
            @HeaderParam("Authorization") String authorization,
            PeriodicUpdate periodicUpdate
    );

    /**
     * Submits a cumulative update for a self-employment business (tax year 2025-26+).
     *
     * <p>This endpoint is used for tax years 2025-26 onwards. Unlike the period endpoint,
     * the tax year is provided as a query parameter and the request body does NOT contain
     * periodDates - it uses the flat {@link CumulativeSummary} structure.</p>
     *
     * <p>Both this endpoint and the period endpoint use the same API version (v5.0).</p>
     *
     * @param nino National Insurance Number
     * @param businessId HMRC business ID
     * @param taxYear Tax year in format "YYYY-YY" (e.g., "2025-26")
     * @param authorization Bearer token
     * @param cumulativeSummary The cumulative income and expense data
     * @return HMRC submission response with period reference
     * @see CumulativeSummary for the flat DTO structure
     */
    @PUT
    @Path("/{nino}/{businessId}/cumulative")
    HmrcSubmissionResponse submitCumulativeUpdate(
            @PathParam("nino") String nino,
            @PathParam("businessId") String businessId,
            @QueryParam("taxYear") String taxYear,
            @HeaderParam("Authorization") String authorization,
            CumulativeSummary cumulativeSummary
    );
}
