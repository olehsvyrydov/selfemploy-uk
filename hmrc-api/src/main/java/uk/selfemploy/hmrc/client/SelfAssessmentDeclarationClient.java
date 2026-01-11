package uk.selfemploy.hmrc.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import uk.selfemploy.hmrc.client.dto.FinalDeclarationRequest;
import uk.selfemploy.hmrc.client.dto.FinalDeclarationResponse;

/**
 * REST client for HMRC Self Assessment Declaration API.
 *
 * <p>Handles submission of final annual declarations.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individual-calculations-api">
 *     HMRC Individual Calculations API</a>
 */
@RegisterRestClient(configKey = "hmrc-declaration-api")
@Path("/individuals/declarations/self-assessment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SelfAssessmentDeclarationClient {

    /**
     * Submits a final Self Assessment declaration.
     *
     * <p>This is the final step in the annual submission process.
     * Must be called after the user has reviewed and confirmed the tax calculation.
     *
     * @param nino National Insurance Number
     * @param taxYear Tax year in format "2024-25"
     * @param authorization Bearer token
     * @param request Declaration request containing calculationId
     * @return Response with charge reference and timestamp
     */
    @POST
    @Path("/{nino}/{taxYear}")
    FinalDeclarationResponse submitDeclaration(
            @PathParam("nino") String nino,
            @PathParam("taxYear") String taxYear,
            @HeaderParam("Authorization") String authorization,
            FinalDeclarationRequest request
    );
}
