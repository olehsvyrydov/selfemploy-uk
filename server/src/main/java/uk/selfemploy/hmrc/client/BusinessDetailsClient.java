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
import uk.selfemploy.hmrc.client.dto.BusinessDetailsV2Response;

import java.util.concurrent.CompletionStage;

/**
 * REST client for HMRC Business Details API v2.
 *
 * <p>Path: {@code /individuals/business/details/{nino}}. Returns the customer's
 * MTD-registered income sources — self-employment businesses and property businesses —
 * along with the MTD subscription reference (MTDBSA). The {@code incomeSourceId} of
 * each returned business is what every other MTD ITSA API refers to as
 * {@code businessId}.
 *
 * <p>Version 1 retired in production on 2026-03-24; v2 is the current contract.
 *
 * <p>Distinct from {@link SelfEmploymentBusinessClient}, which serves the
 * Self-Employment Business API v5 at {@code /individuals/business/self-employment}.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/business-details-api">
 *     HMRC Business Details API</a>
 */
@Path("/individuals/business/details")
@RegisterRestClient(configKey = "hmrc-business-details-api")
@RegisterClientHeaders(HmrcHeaderFactory.class)
@ClientHeaderParam(name = "Accept", value = "application/vnd.hmrc.2.0+json")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BusinessDetailsClient {

    /**
     * Retrieves all MTD-registered income sources for a National Insurance Number.
     *
     * @param nino National Insurance Number
     * @param authorization Bearer token
     * @return the full {@link BusinessDetailsV2Response}; {@code businessData} and
     *         {@code propertyData} may be empty but the wrapper is always returned.
     */
    @GET
    @Path("/{nino}")
    CompletionStage<BusinessDetailsV2Response> listIncomeSources(
        @PathParam("nino") String nino,
        @HeaderParam("Authorization") String authorization
    );
}
