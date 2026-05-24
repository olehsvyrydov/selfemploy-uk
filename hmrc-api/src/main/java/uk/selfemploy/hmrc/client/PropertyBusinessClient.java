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
import uk.selfemploy.hmrc.client.dto.PropertyBusinessResponse;

import java.util.concurrent.CompletionStage;

/**
 * REST client for HMRC Property Business API v6.
 *
 * <p>Path: {@code /individuals/business/property/{nino}}.
 *
 * <p>Returns the customer's property income-sources. Under v6 (production
 * 2026-03-24) each foreign-property income-source is modelled
 * <strong>per-property</strong> rather than aggregated under a single foreign
 * record — every foreign property carries its own {@code incomeSourceId},
 * {@code countryCode}, and accounting-period boundaries. Aggregation across
 * properties is the consumer's responsibility.
 *
 * <p>Scope of this minimum-viable client: a single list income-sources read.
 * Per-property submissions (UK-FHL annual / non-FHL / foreign annual) are not
 * yet modelled — they sit downstream of the calculator round-trip.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/property-business-api">
 *     HMRC Property Business API</a>
 */
@Path("/individuals/business/property")
@RegisterRestClient(configKey = "hmrc-property-business-api")
@RegisterClientHeaders(HmrcHeaderFactory.class)
@ClientHeaderParam(name = "Accept", value = "application/vnd.hmrc.6.0+json")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PropertyBusinessClient {

    /**
     * Lists all property income-sources HMRC holds for the customer (UK-FHL,
     * UK non-FHL, and one record per individual foreign property).
     *
     * @param nino National Insurance Number
     * @param authorization Bearer token
     * @return the full {@link PropertyBusinessResponse} — {@code incomeSources}
     *         may be empty.
     */
    @GET
    @Path("/{nino}")
    CompletionStage<PropertyBusinessResponse> listIncomeSources(
        @PathParam("nino") String nino,
        @HeaderParam("Authorization") String authorization
    );
}
