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
import uk.selfemploy.hmrc.client.dto.SaIndividualDetailsResponse;

import java.util.concurrent.CompletionStage;

/**
 * REST client for HMRC Self Assessment Individual Details API v2.
 *
 * <p>Path: {@code /individuals/details/{nino}}. Returns the customer's personal
 * details and their per-tax-year ITSA status, including the seven new sign-up
 * reasons added 2026-05-15 and the {@code Digitally Exempt} status renamed from
 * the previous {@code Non-Digital}.
 *
 * <p><strong>New error code 2026-05-15:</strong> {@link #ERROR_CLIENT_NOT_MTD_ENROLLED}
 * is returned when the supplied NINO is not signed up for MTD ITSA. Callers should
 * map this to a user-facing onboarding redirect to
 * {@code https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax}
 * rather than treating it as a generic 404 / 4xx.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/self-assessment-individual-details-api">
 *     HMRC Self Assessment Individual Details API</a>
 */
@Path("/individuals/details")
@RegisterRestClient(configKey = "hmrc-sa-individual-details-api")
@RegisterClientHeaders(HmrcHeaderFactory.class)
@ClientHeaderParam(name = "Accept", value = "application/vnd.hmrc.2.0+json")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SaIndividualDetailsClient {

    /**
     * HMRC error code introduced 2026-05-15 — returned when the supplied NINO is
     * not signed up for Making Tax Digital for Income Tax. The application MUST
     * map this to the gov.uk MTD ITSA sign-up flow rather than presenting a
     * generic error.
     */
    String ERROR_CLIENT_NOT_MTD_ENROLLED = "CLIENT_NOT_MTD_ENROLLED";

    /** Public URL the application should redirect users to when {@link #ERROR_CLIENT_NOT_MTD_ENROLLED} is returned. */
    String GOV_UK_MTD_SIGNUP_URL =
        "https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax";

    /**
     * Retrieves the customer's Self Assessment individual details for a National
     * Insurance Number.
     *
     * @param nino National Insurance Number
     * @param authorization Bearer token
     * @return the full {@link SaIndividualDetailsResponse}; HMRC may return the
     *         {@link #ERROR_CLIENT_NOT_MTD_ENROLLED} error code if the NINO is
     *         not enrolled for MTD ITSA.
     */
    @GET
    @Path("/{nino}")
    CompletionStage<SaIndividualDetailsResponse> getIndividualDetails(
        @PathParam("nino") String nino,
        @HeaderParam("Authorization") String authorization
    );
}
