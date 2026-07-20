package uk.selfemploy.ui.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import uk.selfemploy.hmrc.client.dto.BusinessDetailsV2Response;
import uk.selfemploy.hmrc.logging.HmrcPiiRedactor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves and persists the HMRC self-employment business profile for a connected session.
 *
 * <p>This service owns the whole "verify the connection" step: it calls HMRC with the freshly
 * obtained access token, decides what the response means, and persists the business ID and NINO
 * according to a single policy. It performs no UI work and returns a {@link Result} so the caller
 * (the connection wizard, or a direct reconnect from Settings) can render the outcome however it
 * likes.
 *
 * <p>Persistence policy:
 * <ul>
 *   <li>The NINO is stored as the verified, connected NINO only when HMRC confirms it (a 200 with a
 *       business, or a sandbox connection that cannot reject it).</li>
 *   <li>A definitive rejection (NINO mismatch, no business, or no record in production) never
 *       overwrites a previously-stored NINO, and clears any previously-resolved business ID so the
 *       app stops treating the user as submission-ready against a business HMRC no longer accepts.</li>
 *   <li>When the connection succeeds but the profile cannot be fetched (a server error or network
 *       failure), the NINO is kept — unverified — so the user's input is not lost, unless a different
 *       NINO is already stored, which a transient error must not overwrite.</li>
 * </ul>
 */
public class HmrcBusinessProfileService {

    private static final Logger LOG = Logger.getLogger(HmrcBusinessProfileService.class.getName());

    private static final String DEFAULT_API_BASE_URL = "https://test-api.service.hmrc.gov.uk";
    private static final String SANDBOX_FALLBACK_BUSINESS_ID = "XAIS12345678901";
    private static final String BUSINESS_ID_PATTERN = "^X[A-Z0-9]{1}IS[0-9]{11}$";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);
    // JavaTimeModule so the LocalDate fields on BusinessDetailsV2Response deserialise.
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    /** Shared across instances so multiple controllers do not each spawn a connection pool. */
    private static final HttpClient SHARED_CLIENT =
            HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();

    /**
     * The outcome of resolving the business profile, from the app's point of view.
     */
    public enum Outcome {
        /** Connected and the NINO was verified (production 200, or a clean sandbox connection). */
        VERIFIED,
        /** Sandbox connection where the NINO changed since the last connection; cannot be verified. */
        NINO_CHANGED_SANDBOX,
        /** HMRC rejected the NINO (401/403): it does not match the authenticated user. */
        NINO_MISMATCH,
        /** The response was OK but no self-employment business is registered for the NINO. */
        NO_BUSINESS_FOUND,
        /** Production 404: no self-employment record found for the NINO. */
        NINO_NOT_FOUND,
        /** Connected, but the profile could not be fetched yet (server error / network failure). */
        PROFILE_SYNC_PENDING
    }

    /**
     * The result of {@link #fetchAndPersist(String, String)}: the outcome plus the details needed to
     * present it. Persistence has already happened per the class policy by the time this is returned.
     *
     * <p>The user is connected to HMRC (OAuth succeeded) for every outcome; only whether the profile
     * verified and what was stored varies.
     *
     * @param outcome     what the response meant
     * @param businessId  the resolved business ID, or null when none was stored
     * @param previousNino the NINO the previous connection used, set only for
     *                     {@link Outcome#NINO_CHANGED_SANDBOX}; null for every other outcome
     * @param sandbox     whether the connection targeted the sandbox environment
     */
    public record Result(Outcome outcome, String businessId, String previousNino, boolean sandbox) {
    }

    private final HttpClient httpClient;

    public HmrcBusinessProfileService() {
        this(SHARED_CLIENT);
    }

    HmrcBusinessProfileService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Fetches the business profile from HMRC and persists the result per the class policy.
     * Blocking; call from a background thread.
     *
     * @param nino        the NINO the user is connecting with
     * @param accessToken the OAuth access token for the session
     * @return the outcome and the details needed to present it
     */
    public Result fetchAndPersist(String nino, String accessToken) {
        String apiBaseUrl = System.getProperty("HMRC_API_BASE_URL", DEFAULT_API_BASE_URL);
        boolean sandbox = isSandbox(apiBaseUrl);
        // Business Details API v2: lists the customer's MTD income sources. Each self-employment
        // business's incomeSourceId is the businessId every other MTD ITSA API refers to.
        String url = apiBaseUrl + "/individuals/business/details/" + nino;
        LOG.info("Fetching business details from: " + HmrcPiiRedactor.redact(url));

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(HTTP_TIMEOUT)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.hmrc.2.0+json")
                    .GET();
            HmrcFraudHeaders.apply(builder);
            HttpRequest request = builder.build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.info("Business details response: " + response.statusCode());
            return applyResponse(response.statusCode(), response.body(), nino, sandbox);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to fetch business profile", e);
            return persistPending(nino, sandbox);
        }
    }

    /**
     * Decides the outcome for an HMRC response and persists it per the class policy. Package-private
     * so the decision-and-persistence logic can be unit-tested without a live HMRC endpoint.
     *
     * <p>401 and 403 are definitive NINO rejections. A 404 is a rejection in production, but is
     * expected in the sandbox, which holds no real NINOs and therefore resolves to the fallback
     * business ID. Every other status — 5xx included — means OAuth worked but the profile could not
     * be fetched, which is transient and resolves to {@link Outcome#PROFILE_SYNC_PENDING}.
     */
    Result applyResponse(int statusCode, String body, String nino, boolean sandbox) {
        if (statusCode == 200) {
            return handleOkResponse(body, nino, sandbox);
        }

        if (statusCode == 401 || statusCode == 403) {
            LOG.warning("NINO mismatch - HTTP " + statusCode);
            return rejectVerification(Outcome.NINO_MISMATCH, sandbox);
        }

        if (statusCode == 404) {
            if (sandbox) {
                SqliteDataStore store = SqliteDataStore.getInstance();
                store.saveHmrcBusinessId(SANDBOX_FALLBACK_BUSINESS_ID);

                String connectedNino = store.loadConnectedNino();
                boolean firstConnection = connectedNino == null || connectedNino.isBlank();
                boolean ninoChanged = !firstConnection && !connectedNino.equalsIgnoreCase(nino);

                store.saveNinoVerified(!ninoChanged);
                store.saveNino(nino);
                store.saveConnectedNino(nino);

                if (ninoChanged) {
                    LOG.warning("NINO changed since the last connection - sandbox cannot verify correctness");
                    return new Result(Outcome.NINO_CHANGED_SANDBOX,
                            SANDBOX_FALLBACK_BUSINESS_ID, connectedNino, true);
                }
                return new Result(Outcome.VERIFIED, SANDBOX_FALLBACK_BUSINESS_ID, null, true);
            }
            LOG.warning("NINO not found in production - HTTP 404");
            return rejectVerification(Outcome.NINO_NOT_FOUND, false);
        }

        LOG.warning("Business profile not available - HTTP " + statusCode);
        return persistPending(nino, sandbox);
    }

    /**
     * Handles a 200 response, distinguishing a genuine "no business registered" from an unreadable
     * body. The former is a definitive rejection; the latter is a transient content problem that must
     * not wipe a previously-verified profile.
     *
     * <p>Only a Business Details document (a JSON object) is a real business list. An empty body, an
     * unexpected scalar ({@code "OK"}), or any non-object shape fails to deserialise and is treated as
     * sync-pending so a transient content problem does not wipe a verified profile. A well-formed
     * document with no self-employment business is a genuine no-business result and clears the profile.
     */
    private Result handleOkResponse(String body, String nino, boolean sandbox) {
        BusinessDetailsV2Response profile;
        try {
            profile = MAPPER.readValue(body, BusinessDetailsV2Response.class);
        } catch (Exception e) {
            LOG.warning("200 response body was not a Business Details document; treating as sync-pending");
            return persistPending(nino, sandbox);
        }

        if (profile == null) {
            return persistPending(nino, sandbox);
        }

        String businessId = firstSelfEmploymentBusinessId(profile);
        if (businessId != null) {
            SqliteDataStore store = SqliteDataStore.getInstance();
            store.saveHmrcBusinessId(businessId);
            store.saveNinoVerified(true);
            store.saveNino(nino);
            store.saveConnectedNino(nino);
            LOG.info("Stored business ID: " + businessId);
            return new Result(Outcome.VERIFIED, businessId, null, sandbox);
        }
        LOG.warning("No self-employment business found for the connecting NINO");
        return rejectVerification(Outcome.NO_BUSINESS_FOUND, sandbox);
    }

    /**
     * Records a definitive verification failure: marks the NINO unverified and clears any previously
     * resolved business profile, so the app no longer treats the session as submission-ready. The
     * stored NINO is left untouched — a rejection must not overwrite a previously-correct value.
     */
    private Result rejectVerification(Outcome outcome, boolean sandbox) {
        SqliteDataStore store = SqliteDataStore.getInstance();
        store.saveNinoVerified(false);
        store.saveHmrcBusinessId(null);
        store.saveConnectedNino(null);
        return new Result(outcome, null, null, sandbox);
    }

    /**
     * Records a connection whose profile could not be resolved: OAuth succeeded, so the session is
     * kept, but the NINO is marked unverified and the profile syncs on the first submission.
     *
     * <p>The NINO is stored only on a first connection or when it matches the one already on file. A
     * different NINO already stored is left in place, because a transient error cannot confirm that
     * the user really changed it.
     */
    private Result persistPending(String nino, boolean sandbox) {
        SqliteDataStore store = SqliteDataStore.getInstance();
        store.saveNinoVerified(false);
        String existing = store.loadNino();
        if (nino != null && !nino.isBlank()
                && (existing == null || existing.isBlank() || existing.equalsIgnoreCase(nino))) {
            store.saveNino(nino);
        }
        return new Result(Outcome.PROFILE_SYNC_PENDING, null, null, sandbox);
    }

    /**
     * Parses the self-employment business ID from an HMRC Business Details API v2 response, validating
     * its format.
     *
     * @param jsonResponse the response body
     * @return the first valid self-employment business ID, or {@code null} if absent or malformed
     */
    public static String parseBusinessId(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return null;
        }
        try {
            BusinessDetailsV2Response profile = MAPPER.readValue(jsonResponse, BusinessDetailsV2Response.class);
            return profile == null ? null : firstSelfEmploymentBusinessId(profile);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse business ID from response", e);
            return null;
        }
    }

    /**
     * Returns the first self-employment business ID from a Business Details response. Self-employment
     * income sources are carried in {@code businessData} (property businesses are in {@code propertyData}
     * and are ignored); each source's {@code incomeSourceId} is the MTD businessId.
     *
     * @param profile the parsed Business Details document
     * @return the first {@code incomeSourceId} matching the expected format, or {@code null} if none
     */
    private static String firstSelfEmploymentBusinessId(BusinessDetailsV2Response profile) {
        for (BusinessDetailsV2Response.IncomeSource business : profile.businessesOrEmpty()) {
            String id = business.incomeSourceId();
            if (id != null && id.matches(BUSINESS_ID_PATTERN)) {
                return id;
            }
        }
        return null;
    }

    /**
     * Whether the given HMRC API base URL targets the sandbox environment.
     *
     * @param apiBaseUrl the HMRC API base URL
     * @return true for sandbox, false for production (the safer default)
     */
    public static boolean isSandbox(String apiBaseUrl) {
        if (apiBaseUrl == null || apiBaseUrl.isEmpty()) {
            return false;
        }
        return apiBaseUrl.toLowerCase().contains("test-api");
    }

    /**
     * Whether the given HTTP status code is a server error (5xx).
     *
     * @param statusCode the HTTP status code
     * @return true for 500-599
     */
    public static boolean isServerError(int statusCode) {
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * The fallback business ID used when the sandbox API returns 404 (no real NINOs in sandbox).
     *
     * @return the sandbox fallback business ID
     */
    public static String sandboxFallbackBusinessId() {
        return SANDBOX_FALLBACK_BUSINESS_ID;
    }
}
