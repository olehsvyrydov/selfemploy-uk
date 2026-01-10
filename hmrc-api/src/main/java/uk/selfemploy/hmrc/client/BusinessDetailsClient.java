package uk.selfemploy.hmrc.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import uk.selfemploy.hmrc.client.dto.BusinessDetails;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * REST client for HMRC Self-Employment Business Details API.
 */
@Path("/individuals/business/self-employment")
@RegisterRestClient(configKey = "hmrc-api")
@RegisterClientHeaders(HmrcHeaderFactory.class)
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
