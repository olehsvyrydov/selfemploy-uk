package uk.selfemploy.hmrc.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import uk.selfemploy.hmrc.client.dto.IndividualLossesResponse;

import java.util.concurrent.CompletionStage;

/**
 * REST client for HMRC Individual Losses API v6.
 *
 * <p>Path: {@code /individuals/losses/{nino}/brought-forward-losses}.
 *
 * <p>Returns the brought-forward losses HMRC holds for the customer (one entry
 * per income-source / loss-type / tax-year-claimed-for tuple). Losses arise from
 * self-employment trading losses (ITTOIA 2005 s.83), UK / foreign property
 * losses, and trade losses surrendered into the year.
 *
 * <p>HMRC's published v7 of this API is sandbox-only as of 2026-03; v6 is the
 * latest production-ready version and the one this client targets. Re-pin to v7
 * once HMRC promotes it to production.
 *
 * <p>Scope of this minimum-viable client: a single list-all read endpoint. Write
 * endpoints (create / amend / delete loss-claim, loss-claim-orders) are not
 * modelled here; the calculator does not yet originate losses.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individual-losses-api">
 *     HMRC Individual Losses API</a>
 */
@Path("/individuals/losses")
@RegisterRestClient(configKey = "hmrc-individual-losses-api")
@RegisterClientHeaders(HmrcHeaderFactory.class)
@ClientHeaderParam(name = "Accept", value = "application/vnd.hmrc.6.0+json")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface IndividualLossesClient {

    /**
     * Lists all brought-forward losses HMRC holds for the customer, optionally
     * filtered by tax-year-claimed-for, income-source-id, and/or loss-type
     * ({@code self-employment}, {@code self-employment-class4},
     * {@code uk-property-non-fhl}, {@code foreign-property}).
     *
     * @param nino National Insurance Number
     * @param taxYearClaimedFor optional ISO tax year filter (e.g. {@code 2026-27})
     * @param incomeSourceId optional MTD business or property income-source id
     * @param lossType optional loss-type filter
     * @param authorization Bearer token
     * @return the full {@link IndividualLossesResponse} — {@code losses} may be empty.
     */
    @GET
    @Path("/{nino}/brought-forward-losses")
    CompletionStage<IndividualLossesResponse> listBroughtForwardLosses(
        @PathParam("nino") String nino,
        @QueryParam("taxYearClaimedFor") String taxYearClaimedFor,
        @QueryParam("incomeSourceId") String incomeSourceId,
        @QueryParam("lossType") String lossType,
        @HeaderParam("Authorization") String authorization
    );
}
