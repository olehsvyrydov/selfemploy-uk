package uk.selfemploy.hmrc.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import uk.selfemploy.common.dto.CumulativeSummary;
import uk.selfemploy.common.dto.PeriodicUpdate;
import uk.selfemploy.hmrc.client.dto.HmrcSubmissionResponse;

/**
 * REST client for HMRC MTD Self-Employment Periodic Updates API.
 *
 * <p>Submits quarterly periodic updates as required by Making Tax Digital.</p>
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/self-employment-business-api">
 *     HMRC Self-Employment Business API</a>
 */
@RegisterRestClient(configKey = "hmrc-mtd-api")
@Path("/individuals/business/self-employment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface MtdPeriodicUpdateClient {

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
