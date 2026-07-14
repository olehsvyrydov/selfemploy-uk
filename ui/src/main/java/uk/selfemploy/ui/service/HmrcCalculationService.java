package uk.selfemploy.ui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.hmrc.client.dto.CalculationResponse;
import uk.selfemploy.hmrc.client.dto.TriggerCalculationRequest;
import uk.selfemploy.hmrc.client.dto.TriggerCalculationResponse;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Retrieves a Self Assessment tax calculation from the HMRC Individual Calculations
 * API (v8) on {@code java.net.http}, mirroring {@link UiQuarterlySubmissionService}.
 *
 * <p>The flow is: trigger a calculation (POST, returns an id), then retrieve the
 * liability breakdown (GET, polling while HMRC computes it). Every outcome is
 * reported as a typed {@link CalculationOutcome} rather than thrown, so callers can
 * distinguish "not connected", "no data", "forbidden", "timed out" and so on.</p>
 */
public final class HmrcCalculationService {

    private static final Logger LOG = Logger.getLogger(HmrcCalculationService.class.getName());
    private static final String DEFAULT_BASE_URL = "https://test-api.service.hmrc.gov.uk";
    /** Individual Calculations API version (v5/6/7 retired in production 2026-03-24). */
    static final String ACCEPT_HEADER = "application/vnd.hmrc.8.0+json";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final HmrcOAuthService oauthService;
    private final ObjectMapper objectMapper;

    private int pollAttempts = 6;
    private long pollDelayMillis = 2000;

    public HmrcCalculationService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
             OAuthServiceFactory.getOAuthService());
    }

    HmrcCalculationService(HttpClient httpClient, HmrcOAuthService oauthService) {
        this.httpClient = httpClient;
        this.oauthService = oauthService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Visible for testing: shortens polling so a "not ready" path resolves quickly.
     */
    void configurePolling(int attempts, long delayMillis) {
        this.pollAttempts = attempts;
        this.pollDelayMillis = delayMillis;
    }

    /** The outcome of a calculation request. */
    public sealed interface CalculationOutcome {

        /**
         * HMRC returned a liability breakdown. {@code calculationId} is the id from
         * the trigger response (the authoritative reference for the declaration),
         * which is not re-derived from the retrieve body.
         */
        record Success(CalculationResponse calculation, String calculationId) implements CalculationOutcome {
        }

        /** The request could not be completed; {@code reason} classifies why. */
        record Failure(Reason reason, String message, int httpStatus) implements CalculationOutcome {
        }

        /** Why a calculation could not be produced. */
        enum Reason {
            NOT_CONNECTED,
            SESSION_EXPIRED,
            NO_DATA,
            NOT_READY,
            FORBIDDEN,
            VALIDATION,
            RATE_LIMITED,
            TIMEOUT,
            NETWORK,
            SERVER_ERROR,
            UNEXPECTED
        }
    }

    /**
     * Triggers and retrieves a Self Assessment calculation for the given taxpayer.
     *
     * @param nino        the taxpayer's National Insurance number
     * @param taxYear     the tax year to calculate
     * @param crystallise {@code true} for a final (crystallisation) calculation,
     *                    {@code false} for an in-year estimate
     * @return a typed outcome; never null, never throws for expected HMRC errors
     */
    public CalculationOutcome calculate(String nino, TaxYear taxYear, boolean crystallise) {
        if (nino == null || nino.isBlank()) {
            return failure(CalculationOutcome.Reason.VALIDATION, "A National Insurance number is required", 0);
        }
        if (taxYear == null) {
            return failure(CalculationOutcome.Reason.VALIDATION, "A tax year is required", 0);
        }
        try {
            String token = bearerToken(false);
            String calculationId = triggerCalculation(nino, taxYear, crystallise, token);
            CalculationResponse calculation = retrieveWithPolling(nino, taxYear, calculationId, token);
            return new CalculationOutcome.Success(calculation, calculationId);
        } catch (CalcException e) {
            LOG.log(Level.INFO, "Calculation failed: " + e.reason + " - " + e.getMessage());
            return new CalculationOutcome.Failure(e.reason, e.getMessage(), e.httpStatus);
        }
    }

    /**
     * POSTs a trigger request and returns the calculation id from a 202 response.
     * Package-private for testing.
     */
    String triggerCalculation(String nino, TaxYear taxYear, boolean crystallise, String token) {
        String url = buildTriggerUrl(baseUrl(), nino, taxYear);
        String body = serialize(crystallise
            ? TriggerCalculationRequest.forAnnualSubmission()
            : TriggerCalculationRequest.forInYearEstimate());

        HttpResponse<String> response = send(HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/json"), token, "POST", body);

        if (response.statusCode() == 401) {
            response = send(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json"),
                bearerToken(true), "POST", body);
        }

        int status = response.statusCode();
        if (status == 200 || status == 202) {
            TriggerCalculationResponse parsed = parse(response.body(), TriggerCalculationResponse.class);
            if (parsed == null || parsed.calculationId() == null || parsed.calculationId().isBlank()) {
                throw new CalcException(CalculationOutcome.Reason.UNEXPECTED,
                    "HMRC accepted the calculation but returned no calculation id", status);
            }
            return parsed.calculationId();
        }
        throw mapError(status, response.body());
    }

    private CalculationResponse retrieveWithPolling(String nino, TaxYear taxYear,
                                                    String calculationId, String token) {
        CalcException lastNotReady = null;
        for (int attempt = 1; attempt <= Math.max(1, pollAttempts); attempt++) {
            try {
                return retrieveCalculation(nino, taxYear, calculationId, token);
            } catch (CalcException e) {
                if (e.reason != CalculationOutcome.Reason.NOT_READY) {
                    throw e;
                }
                lastNotReady = e;
                if (attempt < pollAttempts) {
                    sleep(pollDelayMillis);
                }
            }
        }
        throw lastNotReady != null ? lastNotReady
            : new CalcException(CalculationOutcome.Reason.NOT_READY,
                "HMRC has not finished the calculation yet. Please try again shortly.", 0);
    }

    /**
     * GETs a calculation result. A 404 means HMRC is still computing (retryable).
     * Package-private for testing.
     */
    CalculationResponse retrieveCalculation(String nino, TaxYear taxYear,
                                            String calculationId, String token) {
        String url = buildRetrieveUrl(baseUrl(), nino, taxYear, calculationId);

        HttpResponse<String> response = send(HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(REQUEST_TIMEOUT), token, "GET", null);

        if (response.statusCode() == 401) {
            response = send(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT), bearerToken(true), "GET", null);
        }

        int status = response.statusCode();
        if (status == 200) {
            CalculationResponse parsed = parse(response.body(), CalculationResponse.class);
            if (parsed == null) {
                throw new CalcException(CalculationOutcome.Reason.UNEXPECTED,
                    "HMRC returned an empty calculation", status);
            }
            return parsed;
        }
        if (status == 404) {
            // MATCHING_RESOURCE_NOT_FOUND while HMRC is still computing the result.
            throw new CalcException(CalculationOutcome.Reason.NOT_READY,
                "HMRC has not finished the calculation yet. Please try again shortly.", status);
        }
        throw mapError(status, response.body());
    }

    private HttpResponse<String> send(HttpRequest.Builder builder, String token,
                                      String method, String body) {
        builder.header("Authorization", "Bearer " + token)
               .header("Accept", ACCEPT_HEADER);
        addFraudPreventionHeaders(builder);
        if ("POST".equals(method)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        } else {
            builder.GET();
        }
        try {
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new CalcException(CalculationOutcome.Reason.TIMEOUT,
                "The request to HMRC timed out. Please try again.", 0);
        } catch (java.io.IOException e) {
            throw new CalcException(CalculationOutcome.Reason.NETWORK,
                "Network error contacting HMRC: " + e.getMessage(), 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CalcException(CalculationOutcome.Reason.NETWORK, "The request was interrupted", 0);
        }
    }

    static String baseUrl() {
        return System.getProperty("HMRC_API_BASE_URL", DEFAULT_BASE_URL);
    }

    static String buildTriggerUrl(String baseUrl, String nino, TaxYear taxYear) {
        return baseUrl + "/individuals/calculations/self-assessment/" + nino + "/" + taxYear.hmrcFormat();
    }

    static String buildRetrieveUrl(String baseUrl, String nino, TaxYear taxYear, String calculationId) {
        return buildTriggerUrl(baseUrl, nino, taxYear) + "/" + calculationId;
    }

    private String bearerToken(boolean forceRefresh) {
        if (!oauthService.isConnected()) {
            throw new CalcException(CalculationOutcome.Reason.NOT_CONNECTED,
                "Not connected to HMRC. Connect via the HMRC Submission page.", 0);
        }
        OAuthTokens tokens = oauthService.getCurrentTokens();
        if (tokens == null) {
            throw new CalcException(CalculationOutcome.Reason.SESSION_EXPIRED,
                "Your HMRC session has expired. Please reconnect.", 0);
        }
        if (forceRefresh || tokens.isExpired() || tokens.getSecondsUntilExpiry() < 300) {
            try {
                tokens = oauthService.refreshAccessToken().get(30, TimeUnit.SECONDS);
                SqliteDataStore.getInstance().saveOAuthTokens(
                    tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn(),
                    tokens.tokenType(), tokens.scope(), tokens.issuedAt());
            } catch (Exception e) {
                HmrcSessionPolicy.onRefreshFailure(e, oauthService);
                throw new CalcException(CalculationOutcome.Reason.SESSION_EXPIRED,
                    "Your HMRC session has expired. Please reconnect.", 0);
            }
        }
        return tokens.accessToken();
    }

    private void addFraudPreventionHeaders(HttpRequest.Builder builder) {
        HmrcFraudHeaders.apply(builder);
    }

    private String serialize(TriggerCalculationRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new CalcException(CalculationOutcome.Reason.UNEXPECTED,
                "Failed to build the calculation request", 0);
        }
    }

    private <T> T parse(String body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (Exception e) {
            throw new CalcException(CalculationOutcome.Reason.UNEXPECTED,
                "Could not read HMRC's response", 0);
        }
    }

    private CalcException mapError(int status, String body) {
        String message = parseErrorMessage(body, status);
        CalculationOutcome.Reason reason = switch (status) {
            case 400, 422 -> CalculationOutcome.Reason.VALIDATION;
            case 403 -> CalculationOutcome.Reason.FORBIDDEN;
            case 404 -> CalculationOutcome.Reason.NO_DATA;
            case 429 -> CalculationOutcome.Reason.RATE_LIMITED;
            default -> status >= 500 ? CalculationOutcome.Reason.SERVER_ERROR
                                     : CalculationOutcome.Reason.UNEXPECTED;
        };
        return new CalcException(reason, message, status);
    }

    private String parseErrorMessage(String body, int status) {
        if (body == null || body.isBlank()) {
            return "HMRC returned an error (HTTP " + status + ")";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("message")) {
                return root.path("message").asText();
            }
            if (root.has("errors") && root.path("errors").isArray() && !root.path("errors").isEmpty()) {
                JsonNode first = root.path("errors").get(0);
                return first.path("message").asText(first.path("code").asText("HMRC error"));
            }
            if (root.has("code")) {
                return root.path("code").asText();
            }
        } catch (Exception ignored) {
            // fall through to generic message
        }
        return "HMRC returned an error (HTTP " + status + ")";
    }

    private static CalculationOutcome.Failure failure(CalculationOutcome.Reason reason,
                                                      String message, int httpStatus) {
        return new CalculationOutcome.Failure(reason, message, httpStatus);
    }

    private static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Internal control-flow carrier mapped to a {@link CalculationOutcome.Failure}. */
    private static final class CalcException extends RuntimeException {
        final CalculationOutcome.Reason reason;
        final int httpStatus;

        CalcException(CalculationOutcome.Reason reason, String message, int httpStatus) {
            super(message);
            this.reason = reason;
            this.httpStatus = httpStatus;
        }
    }
}
