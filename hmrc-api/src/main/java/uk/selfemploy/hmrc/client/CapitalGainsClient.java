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
import uk.selfemploy.hmrc.client.dto.CapitalGainsDisposalsResponse;

import java.util.concurrent.CompletionStage;

/**
 * REST client for HMRC Individuals Disposals (residential property and other
 * disposals including crypto) API v3.
 *
 * <p>Path: {@code /individuals/disposals/residential-property/{nino}/{taxYear}}.
 *
 * <p>Returns the customer-submitted disposals HMRC holds for the tax year.
 * Under v3 (production 2026-03-24) the API explicitly accepts <strong>crypto
 * disposals</strong> (HMRC categorises exchange tokens as chargeable assets per
 * the Cryptoassets Manual CRYPTO20250); disposals are surfaced through the
 * same per-disposal record, distinguished by {@code assetType =
 * "crypto-asset"}.
 *
 * <p>The shape returned here is the <em>customer-submitted disposal record</em>
 * and is intentionally different from the calculator's {@code CapitalGainsTax}
 * response (under {@code CalculationResponse}), which expresses the computed
 * CGT liability. The two DTOs serve different sides of the round-trip and are
 * deliberately not unified.
 *
 * <p>Scope of this minimum-viable client: a single retrieve-by-tax-year read.
 * Create / amend / delete disposals are not modelled here.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individuals-disposals-income-api">
 *     HMRC Individuals Disposals API</a>
 */
@Path("/individuals/disposals/residential-property")
@RegisterRestClient(configKey = "hmrc-capital-gains-api")
@RegisterClientHeaders(HmrcHeaderFactory.class)
@ClientHeaderParam(name = "Accept", value = "application/vnd.hmrc.3.0+json")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface CapitalGainsClient {

    /**
     * Retrieves all customer-submitted disposals for the tax year.
     *
     * @param nino National Insurance Number
     * @param taxYear ISO tax year (e.g. {@code 2026-27})
     * @param authorization Bearer token
     * @return the full {@link CapitalGainsDisposalsResponse} — {@code disposals}
     *         may be empty.
     */
    @GET
    @Path("/{nino}/{taxYear}")
    CompletionStage<CapitalGainsDisposalsResponse> retrieveDisposals(
        @PathParam("nino") String nino,
        @PathParam("taxYear") String taxYear,
        @HeaderParam("Authorization") String authorization
    );
}
