package uk.selfemploy.hmrc.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import uk.selfemploy.hmrc.client.dto.CalculationResponse;
import uk.selfemploy.hmrc.client.dto.TriggerCalculationRequest;
import uk.selfemploy.hmrc.client.dto.TriggerCalculationResponse;

/**
 * REST client for HMRC Self Assessment Calculation API.
 *
 * <p>Handles triggering tax calculations and retrieving calculation results.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individual-calculations-api">
 *     HMRC Individual Calculations API</a>
 */
@RegisterRestClient(configKey = "hmrc-calculation-api")
@Path("/individuals/calculations/self-assessment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SelfAssessmentCalculationClient {

    /**
     * Triggers a Self Assessment tax calculation for a specific tax year.
     *
     * <p>This initiates the calculation process. The calculationId in the response
     * is used to retrieve the calculation result.
     *
     * @param nino National Insurance Number
     * @param taxYear Tax year in format "2024-25"
     * @param authorization Bearer token
     * @param request Calculation request (crystallise flag)
     * @return Response containing calculationId
     */
    @POST
    @Path("/{nino}/{taxYear}")
    TriggerCalculationResponse triggerCalculation(
            @PathParam("nino") String nino,
            @PathParam("taxYear") String taxYear,
            @HeaderParam("Authorization") String authorization,
            TriggerCalculationRequest request
    );

    /**
     * Retrieves a Self Assessment tax calculation result.
     *
     * <p>Returns the full tax liability breakdown including income tax and National Insurance.
     *
     * @param nino National Insurance Number
     * @param taxYear Tax year in format "2024-25"
     * @param calculationId The calculation ID from triggerCalculation
     * @param authorization Bearer token
     * @return The calculation result
     */
    @GET
    @Path("/{nino}/{taxYear}/{calculationId}")
    CalculationResponse getCalculation(
            @PathParam("nino") String nino,
            @PathParam("taxYear") String taxYear,
            @PathParam("calculationId") String calculationId,
            @HeaderParam("Authorization") String authorization
    );
}
