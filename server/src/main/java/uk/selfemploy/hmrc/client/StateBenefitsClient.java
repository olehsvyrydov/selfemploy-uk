package uk.selfemploy.hmrc.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import uk.selfemploy.hmrc.client.dto.StateBenefit;

import java.util.concurrent.CompletionStage;

/**
 * REST client for HMRC Individuals State Benefits API v2.
 *
 * <p>Path: {@code /individuals/state-benefits/{nino}/{taxYear}}.
 *
 * <p>HMRC v2 (deployed in production 2026-03-24) adds a business validation rule
 * that rejects submissions where {@code taxPaid} is populated AND {@code benefitType}
 * is one of {@code statePension}, {@code bereavementAllowance}, or
 * {@code otherStateBenefits} (paid gross by DWP). The validator on
 * {@link StateBenefit#validate()} enforces this client-side so callers fail fast.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individuals-state-benefits-api">
 *     HMRC Individuals State Benefits API</a>
 */
@Path("/individuals/state-benefits")
@RegisterRestClient(configKey = "hmrc-state-benefits-api")
@RegisterClientHeaders(HmrcHeaderFactory.class)
@ClientHeaderParam(name = "Accept", value = "application/vnd.hmrc.2.0+json")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface StateBenefitsClient {

    /**
     * Lists all state benefits HMRC holds for the customer in the given tax year.
     */
    @GET
    @Path("/{nino}/{taxYear}")
    CompletionStage<Response> listStateBenefits(
        @PathParam("nino") String nino,
        @PathParam("taxYear") String taxYear,
        @HeaderParam("Authorization") String authorization
    );

    /**
     * Submits a new state benefit entry for the customer. Callers MUST invoke
     * {@link StateBenefit#validate()} before calling this method to fail-fast on
     * the {@code taxPaid}-prohibited benefit types.
     */
    @POST
    @Path("/{nino}/{taxYear}")
    CompletionStage<Response> submitStateBenefit(
        @PathParam("nino") String nino,
        @PathParam("taxYear") String taxYear,
        @HeaderParam("Authorization") String authorization,
        StateBenefit benefit
    );
}
