package uk.selfemploy.ui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.hmrc.client.dto.FinalDeclarationRequest;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Submits a Self Assessment final declaration to HMRC (Individual Calculations
 * API v8), the last step of the annual return: the taxpayer declares a specific
 * calculation is complete and correct.
 *
 * <p>The declaration is gated behind an explicit user confirmation and fails
 * closed without one — nothing is ever sent to HMRC unless the caller supplies a
 * confirmation with {@code confirmedByUser == true}. On success the HMRC
 * calculation id is the reference stored and shown; no reference is ever
 * fabricated.</p>
 */
public final class HmrcFinalDeclarationService {

    private static final Logger LOG = Logger.getLogger(HmrcFinalDeclarationService.class.getName());
    private static final String DEFAULT_BASE_URL = "https://test-api.service.hmrc.gov.uk";
    static final String ACCEPT_HEADER = "application/vnd.hmrc.8.0+json";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final HmrcOAuthService oauthService;
    private final ObjectMapper objectMapper;

    public HmrcFinalDeclarationService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
             OAuthServiceFactory.getOAuthService());
    }

    HmrcFinalDeclarationService(HttpClient httpClient, HmrcOAuthService oauthService) {
        this.httpClient = httpClient;
        this.oauthService = oauthService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ==================== Confirmation + typed result ====================

    /**
     * The taxpayer's explicit confirmation that the reviewed figures are correct.
     * Without one carrying {@code confirmedByUser == true}, no declaration is sent.
     */
    public record DeclarationConfirmation(boolean confirmedByUser, Instant confirmedAt) {
    }

    /** The outcome of a final declaration submission. */
    public sealed interface DeclarationOutcome {

        /** HMRC accepted the declaration; {@code hmrcReference} is the calculation id. */
        record Success(String hmrcReference, Instant declaredAt) implements DeclarationOutcome {
        }

        /** The declaration was not made; {@code reason} classifies why. */
        record Failure(Reason reason, String message, int httpStatus) implements DeclarationOutcome {
        }

        /** Why a declaration was not made. */
        enum Reason {
            NOT_CONFIRMED,
            NOT_CONNECTED,
            SESSION_EXPIRED,
            VALIDATION,
            FORBIDDEN,
            ALREADY_DECLARED,
            RATE_LIMITED,
            TIMEOUT,
            NETWORK,
            SERVER_ERROR,
            UNEXPECTED
        }
    }

    // ==================== Public API ====================

    /**
     * Submits the final declaration for a calculation the user has confirmed.
     *
     * @param nino          the taxpayer's National Insurance number
     * @param taxYear       the tax year being declared
     * @param calculationId the HMRC calculation id being declared final
     * @param confirmation  the explicit user confirmation; must be non-null and
     *                      {@code confirmedByUser}
     * @return a typed outcome; never null, never throws for expected HMRC errors
     */
    public DeclarationOutcome submitFinalDeclaration(String nino, TaxYear taxYear,
                                                     String calculationId,
                                                     DeclarationConfirmation confirmation) {
        if (confirmation == null || !confirmation.confirmedByUser()) {
            return new DeclarationOutcome.Failure(DeclarationOutcome.Reason.NOT_CONFIRMED,
                "The figures must be confirmed before a declaration can be sent to HMRC.", 0);
        }
        if (nino == null || nino.isBlank()) {
            return failure(DeclarationOutcome.Reason.VALIDATION, "A National Insurance number is required", 0);
        }
        if (taxYear == null) {
            return failure(DeclarationOutcome.Reason.VALIDATION, "A tax year is required", 0);
        }
        if (calculationId == null || calculationId.isBlank()) {
            return failure(DeclarationOutcome.Reason.VALIDATION, "A calculation id is required", 0);
        }

        try {
            String token = bearerToken(false);
            return declare(nino, taxYear, calculationId, confirmation, token);
        } catch (DeclarationException e) {
            LOG.log(Level.INFO, "Final declaration failed: " + e.reason + " - " + e.getMessage());
            return new DeclarationOutcome.Failure(e.reason, e.getMessage(), e.httpStatus);
        }
    }

    private DeclarationOutcome declare(String nino, TaxYear taxYear, String calculationId,
                                       DeclarationConfirmation confirmation, String token) {
        String url = buildDeclarationUrl(baseUrl(), nino, taxYear);
        String body = serialize(new FinalDeclarationRequest(calculationId));

        HttpResponse<String> response = send(url, token, body);
        if (response.statusCode() == 401) {
            response = send(url, bearerToken(true), body);
        }

        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            // Real MTD final declarations return 204 with no body: the calculation id
            // is the durable HMRC reference. If a body carries a charge reference or
            // timestamp, prefer those.
            String reference = calculationId;
            Instant declaredAt = confirmation.confirmedAt() != null ? confirmation.confirmedAt() : null;
            String parsedReference = parseReference(response.body());
            if (parsedReference != null && !parsedReference.isBlank()) {
                reference = parsedReference;
            }
            LOG.info("HMRC accepted final declaration for calculation " + calculationId);
            return new DeclarationOutcome.Success(reference, declaredAt);
        }
        throw mapError(status, response.body());
    }

    // ==================== HTTP ====================

    private HttpResponse<String> send(String url, String token, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(REQUEST_TIMEOUT)
            .header("Authorization", "Bearer " + token)
            .header("Accept", ACCEPT_HEADER)
            .header("Content-Type", "application/json");
        addFraudPreventionHeaders(builder);
        builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        try {
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new DeclarationException(DeclarationOutcome.Reason.TIMEOUT,
                "The request to HMRC timed out. Please try again.", 0);
        } catch (java.io.IOException e) {
            throw new DeclarationException(DeclarationOutcome.Reason.NETWORK,
                "Network error contacting HMRC: " + e.getMessage(), 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DeclarationException(DeclarationOutcome.Reason.NETWORK, "The request was interrupted", 0);
        }
    }

    // ==================== URL ====================

    static String baseUrl() {
        return System.getProperty("HMRC_API_BASE_URL", DEFAULT_BASE_URL);
    }

    static String buildDeclarationUrl(String baseUrl, String nino, TaxYear taxYear) {
        return baseUrl + "/individuals/declarations/self-assessment/" + nino + "/" + taxYear.hmrcFormat();
    }

    // ==================== Token ====================

    private String bearerToken(boolean forceRefresh) {
        if (!oauthService.isConnected()) {
            throw new DeclarationException(DeclarationOutcome.Reason.NOT_CONNECTED,
                "Not connected to HMRC. Connect via the HMRC Submission page.", 0);
        }
        OAuthTokens tokens = oauthService.getCurrentTokens();
        if (tokens == null) {
            throw new DeclarationException(DeclarationOutcome.Reason.SESSION_EXPIRED,
                "Your HMRC session has expired. Please reconnect.", 0);
        }
        if (forceRefresh || tokens.isExpired() || tokens.getSecondsUntilExpiry() < 300) {
            try {
                tokens = oauthService.refreshAccessToken().get(30, TimeUnit.SECONDS);
                SqliteDataStore.getInstance().saveOAuthTokens(
                    tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn(),
                    tokens.tokenType(), tokens.scope(), tokens.issuedAt());
            } catch (Exception e) {
                SqliteDataStore.getInstance().clearOAuthTokens();
                throw new DeclarationException(DeclarationOutcome.Reason.SESSION_EXPIRED,
                    "Your HMRC session has expired. Please reconnect.", 0);
            }
        }
        return tokens.accessToken();
    }

    // ==================== Fraud prevention headers ====================

    private void addFraudPreventionHeaders(HttpRequest.Builder builder) {
        HmrcFraudHeaders.apply(builder);
    }

    // ==================== JSON ====================

    private String serialize(FinalDeclarationRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new DeclarationException(DeclarationOutcome.Reason.UNEXPECTED,
                "Failed to build the declaration request", 0);
        }
    }

    private String parseReference(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("chargeReference")) {
                return root.path("chargeReference").asText(null);
            }
        } catch (Exception ignored) {
            // no usable body; caller falls back to the calculation id
        }
        return null;
    }

    private DeclarationException mapError(int status, String body) {
        String message = parseErrorMessage(body, status);
        DeclarationOutcome.Reason reason = switch (status) {
            case 400, 422 -> DeclarationOutcome.Reason.VALIDATION;
            case 403 -> DeclarationOutcome.Reason.FORBIDDEN;
            case 409 -> DeclarationOutcome.Reason.ALREADY_DECLARED;
            case 429 -> DeclarationOutcome.Reason.RATE_LIMITED;
            default -> status >= 500 ? DeclarationOutcome.Reason.SERVER_ERROR
                                     : DeclarationOutcome.Reason.UNEXPECTED;
        };
        return new DeclarationException(reason, message, status);
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

    private static DeclarationOutcome.Failure failure(DeclarationOutcome.Reason reason,
                                                      String message, int httpStatus) {
        return new DeclarationOutcome.Failure(reason, message, httpStatus);
    }

    /** Internal control-flow carrier mapped to a {@link DeclarationOutcome.Failure}. */
    private static final class DeclarationException extends RuntimeException {
        final DeclarationOutcome.Reason reason;
        final int httpStatus;

        DeclarationException(DeclarationOutcome.Reason reason, String message, int httpStatus) {
            super(message);
            this.reason = reason;
            this.httpStatus = httpStatus;
        }
    }
}
