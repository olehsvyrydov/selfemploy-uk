package uk.selfemploy.hmrc.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import uk.selfemploy.hmrc.client.dto.Reliefs;

import java.util.concurrent.CompletionStage;

/**
 * REST client for HMRC Individuals Reliefs API v3.
 *
 * <p>Path: {@code /individuals/reliefs/charitable-giving/{nino}/{taxYear}}.
 *
 * <p>HMRC v3 (deployed in production 2026-03-24) removed all non-UK charitable
 * giving fields — only UK Gift Aid and gifts to UK charities / CASCs are
 * acceptable (ITA 2007 s.413; CTM09000). Historical records from prior tax
 * years remain readable.
 *
 * <p><strong>Migration UX (UI ticket — out of scope here):</strong> users with
 * historical non-UK charitable records should see a one-time notice explaining
 * that non-UK relief is no longer available; their historical data remains
 * preserved read-only for audit. Tracked separately.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individuals-reliefs-api">
 *     HMRC Individuals Reliefs API</a>
 */
@Path("/individuals/reliefs")
@RegisterRestClient(configKey = "hmrc-reliefs-api")
@RegisterClientHeaders(HmrcHeaderFactory.class)
@ClientHeaderParam(name = "Accept", value = "application/vnd.hmrc.3.0+json")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ReliefsClient {

    /**
     * Retrieves the customer's charitable-giving reliefs for the tax year.
     */
    @GET
    @Path("/charitable-giving/{nino}/{taxYear}")
    CompletionStage<Reliefs> getCharitableGivingReliefs(
        @PathParam("nino") String nino,
        @PathParam("taxYear") String taxYear,
        @HeaderParam("Authorization") String authorization
    );

    /**
     * Creates or amends the customer's charitable-giving reliefs for the tax year.
     * The {@link Reliefs} DTO models only UK-permitted fields — there is no way
     * to accidentally submit a non-UK field.
     */
    @PUT
    @Path("/charitable-giving/{nino}/{taxYear}")
    CompletionStage<Response> amendCharitableGivingReliefs(
        @PathParam("nino") String nino,
        @PathParam("taxYear") String taxYear,
        @HeaderParam("Authorization") String authorization,
        Reliefs reliefs
    );

    /**
     * Deletes the customer's charitable-giving reliefs for the tax year.
     */
    @DELETE
    @Path("/charitable-giving/{nino}/{taxYear}")
    CompletionStage<Response> deleteCharitableGivingReliefs(
        @PathParam("nino") String nino,
        @PathParam("taxYear") String taxYear,
        @HeaderParam("Authorization") String authorization
    );
}
