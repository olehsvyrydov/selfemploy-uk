package uk.selfemploy.hmrc.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import uk.selfemploy.hmrc.client.dto.BusinessDetails;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * REST client for HMRC Self-Employment Business API v5.
 *
 * <p><strong>Note (SLFEMPUK-26 / S17-02):</strong> this client is misnamed — its path
 * is {@code /individuals/business/self-employment}, which is the Self-Employment Business
 * API (v5), not the Business Details API (which lives at {@code /individuals/business/details}).
 * Rename pending in S17-02; a real Business Details v2 client follows in S17-03.
 */
@Path("/individuals/business/self-employment")
@RegisterRestClient(configKey = "hmrc-api")
@RegisterClientHeaders(HmrcHeaderFactory.class)
@ClientHeaderParam(name = "Accept", value = "application/vnd.hmrc.5.0+json")
public interface BusinessDetailsClient {

    /**
     * Retrieves all self-employment businesses for a National Insurance Number.
     *
     * @param nino National Insurance Number
     * @param authorization Bearer token
     * @return List of business details
     */
    @GET
    @Path("/{nino}")
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<BusinessListResponse> listBusinesses(
        @PathParam("nino") String nino,
        @HeaderParam("Authorization") String authorization
    );

    /**
     * Retrieves details for a specific business.
     *
     * @param nino National Insurance Number
     * @param businessId Business ID
     * @param authorization Bearer token
     * @return Business details
     */
    @GET
    @Path("/{nino}/{businessId}")
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<BusinessDetails> getBusinessDetails(
        @PathParam("nino") String nino,
        @PathParam("businessId") String businessId,
        @HeaderParam("Authorization") String authorization
    );

    /**
     * Response wrapper for list of businesses.
     */
    record BusinessListResponse(List<BusinessDetails> selfEmployments) {}
}
