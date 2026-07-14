package uk.selfemploy.ui.service;

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
 * <p>Persistence policy: the entered NINO is stored as the verified, connected NINO only when HMRC
 * confirms it (a 200 with a business, or a sandbox connection that cannot reject it). A definitive
 * rejection (NINO mismatch, or no record in production) never overwrites a previously-stored NINO.
 * When the connection succeeds but the profile cannot be fetched yet (a server error or network
 * failure), the NINO is still persisted — unverified — so the user's input is not lost and the
 * profile can sync on the first submission.
 */
public class HmrcBusinessProfileService {

    private static final Logger LOG = Logger.getLogger(HmrcBusinessProfileService.class.getName());

    private static final String DEFAULT_API_BASE_URL = "https://test-api.service.hmrc.gov.uk";
    private static final String SANDBOX_FALLBACK_BUSINESS_ID = "XAIS12345678901";
    private static final String BUSINESS_ID_PATTERN = "^X[A-Z0-9]{1}IS[0-9]{11}$";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

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
     */
    public record Result(Outcome outcome, boolean connected, boolean ninoVerified,
                         String businessId, String previousNino, String currentNino, boolean sandbox) {

        /** True when the connection is usable and the NINO was accepted (verified or sandbox). */
        public boolean isVerifiedSuccess() {
            return outcome == Outcome.VERIFIED || outcome == Outcome.NINO_CHANGED_SANDBOX;
        }

        /** True when the user is connected to HMRC, even if the profile has not synced yet. */
        public boolean isConnected() {
            return connected;
        }
    }

    private final HttpClient httpClient;

    public HmrcBusinessProfileService() {
        this(HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build());
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
            // The OAuth connection succeeded; only the profile fetch failed. Persist the NINO so it
            // is not lost, mark it unverified, and let the profile sync on the first submission.
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
            String businessId = parseBusinessId(body);
            if (businessId != null) {
                store.saveHmrcBusinessId(businessId);
                store.saveNinoVerified(true);
                store.saveNino(nino);
                store.saveConnectedNino(nino);
                LOG.info("Stored business ID: " + businessId);
                return new Result(Outcome.VERIFIED, true, true, businessId, nino, nino, sandbox);
            }
            LOG.warning("No self-employment business found for the connecting NINO");
            store.saveNinoVerified(false);
            return new Result(Outcome.NO_BUSINESS_FOUND, true, false, null, null, nino, sandbox);
        }

        if (statusCode == 401 || statusCode == 403) {
            LOG.warning("NINO mismatch - HTTP " + statusCode);
            store.saveNinoVerified(false);
            return new Result(Outcome.NINO_MISMATCH, true, false, null, null, nino, sandbox);
        }

        if (statusCode == 404) {
            if (sandbox) {
                // Sandbox has no real NINOs, so a 404 is expected: use the fallback business ID.
                String fallbackBusinessId = SANDBOX_FALLBACK_BUSINESS_ID;
                store.saveHmrcBusinessId(fallbackBusinessId);

                String connectedNino = store.loadConnectedNino();
                boolean firstConnection = connectedNino == null || connectedNino.isBlank();
                boolean ninoChanged = !firstConnection && !connectedNino.equalsIgnoreCase(nino);

                store.saveNinoVerified(!ninoChanged);
                store.saveNino(nino);
                store.saveConnectedNino(nino);

                if (ninoChanged) {
                    LOG.warning("NINO changed since the last connection - sandbox cannot verify correctness");
                    return new Result(Outcome.NINO_CHANGED_SANDBOX, true, false,
                            fallbackBusinessId, connectedNino, nino, true);
                }
                return new Result(Outcome.VERIFIED, true, true, fallbackBusinessId, nino, nino, true);
            }
            LOG.warning("NINO not found in production - HTTP 404");
            store.saveNinoVerified(false);
            return new Result(Outcome.NINO_NOT_FOUND, true, false, null, null, nino, false);
        }

        // 5xx and any other status: OAuth worked but the profile could not be fetched. Persist the
        // NINO (unverified) so it survives, and let the profile sync later.
        LOG.warning("Business profile not available - HTTP " + statusCode);
        return persistPending(nino, sandbox);
    }

    private Result persistPending(String nino, boolean sandbox) {
        SqliteDataStore store = SqliteDataStore.getInstance();
        store.saveNinoVerified(false);
        if (nino != null && !nino.isBlank()) {
            store.saveNino(nino);
        }
        return new Result(Outcome.PROFILE_SYNC_PENDING, true, false, null, null, nino, sandbox);
    }

    /**
     * Parses the business ID from an HMRC self-employment response, validating its format.
     *
     * @param jsonResponse the response body
     * @return the business ID, or {@code null} if absent or malformed
     */
    public static String parseBusinessId(String jsonResponse) {
        if (jsonResponse == null) {
            return null;
        }
        try {
            int idx = jsonResponse.indexOf("\"businessId\"");
            if (idx >= 0) {
                int colonIdx = jsonResponse.indexOf(":", idx);
                int quoteStart = jsonResponse.indexOf("\"", colonIdx + 1);
                int quoteEnd = jsonResponse.indexOf("\"", quoteStart + 1);
                if (quoteStart >= 0 && quoteEnd > quoteStart) {
                    String businessId = jsonResponse.substring(quoteStart + 1, quoteEnd);
                    if (businessId.matches(BUSINESS_ID_PATTERN)) {
                        return businessId;
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse business ID from response", e);
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
