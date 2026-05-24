package uk.selfemploy.hmrc.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import uk.selfemploy.hmrc.client.dto.BsasResponse;

import java.util.concurrent.CompletionStage;

/**
 * REST client for HMRC Business Source Adjustable Summary (BSAS) API v7.
 *
 * <p>Path: {@code /individuals/self-assessment/adjustable-summary/{nino}/self-employment/{bsasId}}.
 *
 * <p>Retrieves an adjustable-summary for a self-employment income source. BSAS
 * is the basis for in-year tax estimates and end-of-period statements: the
 * customer (or their software) submits adjustments on top of the
 * auto-aggregated quarterly totals, and HMRC returns an updated summary.
 *
 * <p>v7 (production 2026-03-24) <strong>removed the {@code averagingAdjustment}
 * field</strong> — averaging relief (ITTOIA 2005 s.221 for farmers, creative
 * artists) is no longer expressible through BSAS. The shared submission DTOs
 * already enforce this; this client never accepts the field on the wire because
 * the {@link BsasResponse.Adjustments} record has no component for it.
 *
 * <p>Scope of this minimum-viable client: a single retrieve-self-employment-BSAS
 * read. Trigger-BSAS and submit-adjustments writes are not modelled here.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/self-assessment-bsas-api">
 *     HMRC BSAS API</a>
 */
@Path("/individuals/self-assessment/adjustable-summary")
@RegisterRestClient(configKey = "hmrc-bsas-api")
@RegisterClientHeaders(HmrcHeaderFactory.class)
@ClientHeaderParam(name = "Accept", value = "application/vnd.hmrc.7.0+json")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BsasClient {

    /**
     * Retrieves a self-employment BSAS by its HMRC-issued identifier.
     *
     * @param nino National Insurance Number
     * @param bsasId UUID-style BSAS identifier returned by the trigger-BSAS endpoint
     * @param authorization Bearer token
     * @return the full {@link BsasResponse}.
     */
    @GET
    @Path("/{nino}/self-employment/{bsasId}")
    CompletionStage<BsasResponse> retrieveSelfEmploymentBsas(
        @PathParam("nino") String nino,
        @PathParam("bsasId") String bsasId,
        @HeaderParam("Authorization") String authorization
    );
}
