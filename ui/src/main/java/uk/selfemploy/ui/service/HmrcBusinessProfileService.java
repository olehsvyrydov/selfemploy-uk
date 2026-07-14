package uk.selfemploy.ui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
     * @param previousNino the NINO the connection was previously verified against, when it changed
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
        String url = apiBaseUrl + "/individuals/business/self-employment/" + nino;
        LOG.info("Fetching business details from: " + HmrcPiiRedactor.redact(url));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(HTTP_TIMEOUT)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.hmrc.2.0+json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.info("Business details response: " + response.statusCode());
            return applyResponse(response.statusCode(), response.body(), nino, sandbox);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to fetch business profile", e);
            // The OAuth connection succeeded; only the profile fetch failed. Keep the NINO so it is
            // not lost, mark it unverified, and let the profile sync on the first submission.
            return persistPending(nino, sandbox);
        }
    }

    /**
     * Decides the outcome for an HMRC response and persists it per the class policy. Package-private
     * so the decision-and-persistence logic can be unit-tested without a live HMRC endpoint.
     */
    Result applyResponse(int statusCode, String body, String nino, boolean sandbox) {
        SqliteDataStore store = SqliteDataStore.getInstance();

        if (statusCode == 200) {
            return handleOkResponse(body, nino, sandbox);
        }

        if (statusCode == 401 || statusCode == 403) {
            LOG.warning("NINO mismatch - HTTP " + statusCode);
            return rejectVerification(Outcome.NINO_MISMATCH, sandbox);
        }

        if (statusCode == 404) {
            if (sandbox) {
                // Sandbox has no real NINOs, so a 404 is expected: use the fallback business ID.
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
                return new Result(Outcome.VERIFIED, SANDBOX_FALLBACK_BUSINESS_ID, nino, true);
            }
            LOG.warning("NINO not found in production - HTTP 404");
            return rejectVerification(Outcome.NINO_NOT_FOUND, false);
        }

        // 5xx and any other status: OAuth worked but the profile could not be fetched. Keep the NINO
        // (unless a different one is already stored) and let the profile sync later.
        LOG.warning("Business profile not available - HTTP " + statusCode);
        return persistPending(nino, sandbox);
    }

    /**
     * Handles a 200 response, distinguishing a genuine "no business registered" (valid body, empty
     * result) from an unreadable body. The former is a definitive rejection; the latter is a
     * transient content problem that must not wipe a previously-verified profile.
     */
    private Result handleOkResponse(String body, String nino, boolean sandbox) {
        JsonNode root;
        try {
            root = MAPPER.readTree(body);
        } catch (Exception e) {
            LOG.warning("200 response body could not be parsed; treating as sync-pending");
            return persistPending(nino, sandbox);
        }

        String businessId = extractBusinessId(root);
        if (businessId != null) {
            SqliteDataStore store = SqliteDataStore.getInstance();
            store.saveHmrcBusinessId(businessId);
            store.saveNinoVerified(true);
            store.saveNino(nino);
            store.saveConnectedNino(nino);
            LOG.info("Stored business ID: " + businessId);
            return new Result(Outcome.VERIFIED, businessId, nino, sandbox);
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

    private Result persistPending(String nino, boolean sandbox) {
        SqliteDataStore store = SqliteDataStore.getInstance();
        store.saveNinoVerified(false);
        String existing = store.loadNino();
        if (nino != null && !nino.isBlank()
                && (existing == null || existing.isBlank() || existing.equalsIgnoreCase(nino))) {
            // First connection or the same NINO: keep the user's input so it is not lost. A different
            // NINO already on file is left in place, since a transient error cannot confirm a change.
            store.saveNino(nino);
        }
        return new Result(Outcome.PROFILE_SYNC_PENDING, null, null, sandbox);
    }

    /**
     * Parses the business ID from an HMRC self-employment response, validating its format. Reads the
     * JSON structurally so a reordered or differently-shaped-but-valid response is not rejected.
     *
     * @param jsonResponse the response body
     * @return the first valid business ID, or {@code null} if absent or malformed
     */
    public static String parseBusinessId(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return null;
        }
        try {
            return extractBusinessId(MAPPER.readTree(jsonResponse));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse business ID from response", e);
            return null;
        }
    }

    private static String extractBusinessId(JsonNode root) {
        JsonNode businesses = root.path("selfEmployments");
        if (businesses.isArray()) {
            for (JsonNode business : businesses) {
                String id = business.path("businessId").asText(null);
                if (id != null && id.matches(BUSINESS_ID_PATTERN)) {
                    return id;
                }
            }
        }
        String direct = root.path("businessId").asText(null);
        if (direct != null && direct.matches(BUSINESS_ID_PATTERN)) {
            return direct;
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
