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
import uk.selfemploy.hmrc.client.dto.ObligationsResponse;

import java.util.concurrent.CompletionStage;

/**
 * REST client for HMRC Obligations API v3 (income and expenditure).
 *
 * <p>Path: {@code /obligations/details/{nino}/income-and-expenditure}.
 *
 * <p>Returns the customer's MTD-quarterly obligations and their due dates. Under
 * the v3 cadence (deployed in production 2026-03-24), due dates fall on the
 * <strong>7th of the month following quarter end</strong>, aligned with VAT MTD.
 * Earlier hardcoded 5th-of-month deadlines in the app were corrected in
 * {@link uk.selfemploy.common.domain.Quarter} is now the
 * single source of truth.
 *
 * <p>Late submission incurs penalty points under FA 2021 Sch 24 (4 points = £200).
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/obligations-api">
 *     HMRC Obligations API</a>
 */
@Path("/obligations/details")
@RegisterRestClient(configKey = "hmrc-obligations-api")
@RegisterClientHeaders(HmrcHeaderFactory.class)
@ClientHeaderParam(name = "Accept", value = "application/vnd.hmrc.3.0+json")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ObligationsClient {

    /**
     * Retrieves all income and expenditure obligations for a National Insurance Number.
     *
     * <p>Optional filters narrow the result to a specific date range and/or status
     * (e.g. {@code Open}, {@code Fulfilled}); when omitted, HMRC returns the
     * default window (typically the current and prior tax year).
     *
     * @param nino National Insurance Number
     * @param fromDate optional ISO-8601 start of the obligation window (inclusive)
     * @param toDate optional ISO-8601 end of the obligation window (inclusive)
     * @param status optional filter — typically {@code Open} or {@code Fulfilled}
     * @param authorization Bearer token
     * @return the full {@link ObligationsResponse} — {@code obligations} may be empty
     *         but the wrapper is always returned.
     */
    @GET
    @Path("/{nino}/income-and-expenditure")
    CompletionStage<ObligationsResponse> listObligations(
        @PathParam("nino") String nino,
        @QueryParam("from") String fromDate,
        @QueryParam("to") String toDate,
        @QueryParam("status") String status,
        @HeaderParam("Authorization") String authorization
    );
}
