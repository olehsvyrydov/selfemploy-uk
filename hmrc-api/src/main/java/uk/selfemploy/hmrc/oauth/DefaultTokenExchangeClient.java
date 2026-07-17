package uk.selfemploy.hmrc.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.config.HmrcConfig;
import uk.selfemploy.hmrc.config.HmrcHosts;
import uk.selfemploy.hmrc.exception.HmrcOAuthException;
import uk.selfemploy.hmrc.exception.HmrcOAuthException.OAuthError;
import uk.selfemploy.hmrc.logging.HmrcPiiRedactor;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of TokenExchangeClient using java.net.http.HttpClient.
 */
@ApplicationScoped
public class DefaultTokenExchangeClient implements TokenExchangeClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultTokenExchangeClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HmrcConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public DefaultTokenExchangeClient(HmrcConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    // Constructor for testing
    DefaultTokenExchangeClient(HmrcConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public CompletableFuture<OAuthTokens> exchangeCodeForTokens(String authorizationCode, String codeVerifier) {
        log.debug("Exchanging authorization code for tokens");

        String body = buildTokenRequestBody("authorization_code", "code", authorizationCode)
            + "&code_verifier=" + encode(codeVerifier);

        return sendTokenRequest(body)
            .exceptionally(ex -> {
                log.error("Token exchange failed: {}",
                    HmrcPiiRedactor.redact(String.valueOf(ex.getMessage())), ex);
                HmrcOAuthException typed = asHmrcOAuthException(ex);
                throw typed != null ? typed : new HmrcOAuthException(OAuthError.TOKEN_EXCHANGE_FAILED, ex);
            });
    }

    /**
     * Returns the {@link HmrcOAuthException} carried by a failed stage — whether it is the throwable
     * itself or its cause — or null if the failure is not one. A stage that fails with a plain
     * {@code failedFuture(hmrcException)} surfaces it directly, not wrapped.
     */
    private static HmrcOAuthException asHmrcOAuthException(Throwable ex) {
        if (ex instanceof HmrcOAuthException direct) {
            return direct;
        }
        if (ex.getCause() instanceof HmrcOAuthException cause) {
            return cause;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A rejected refresh token is an expected, user-recoverable outcome — the user reconnects — so
     * it is logged as a single warning. A stack trace is reserved for failures that are unexpected and
     * therefore need diagnosing.
     */
    @Override
    public CompletableFuture<OAuthTokens> refreshTokens(String refreshToken) {
        log.debug("Refreshing access token");

        String body = buildTokenRequestBody("refresh_token", "refresh_token", refreshToken);

        return sendTokenRequest(body)
            .exceptionally(DefaultTokenExchangeClient::logAndRethrowRefreshFailure);
    }

    /**
     * Logs a refresh failure and rethrows it as an {@link HmrcOAuthException}.
     *
     * <p>An {@code invalid_grant} is the one failure that destroys the stored session, so the log
     * must record what HMRC actually said — redacted, and without a stack trace, since a rejection
     * is expected and user-recoverable rather than a fault to diagnose. Every other failure keeps
     * its stack trace.</p>
     *
     * @param ex the failure raised by the token request
     * @return never; the method always throws
     */
    private static OAuthTokens logAndRethrowRefreshFailure(Throwable ex) {
        HmrcOAuthException typed = asHmrcOAuthException(ex);
        HmrcOAuthException failure = typed != null
            ? typed
            : new HmrcOAuthException(OAuthError.TOKEN_EXCHANGE_FAILED, ex);
        if (failure.getError() == OAuthError.INVALID_GRANT) {
            log.warn("HMRC rejected the refresh token; the user must reconnect: {}",
                HmrcPiiRedactor.redact(String.valueOf(failure.getMessage())));
        } else {
            log.error("Token refresh failed: {}",
                HmrcPiiRedactor.redact(String.valueOf(ex.getMessage())), ex);
        }
        throw failure;
    }

    private String buildTokenRequestBody(String grantType, String paramName, String paramValue) {
        StringBuilder body = new StringBuilder();
        body.append("grant_type=").append(grantType);
        body.append("&").append(paramName).append("=").append(encode(paramValue));
        body.append("&client_id=").append(encode(config.clientId().orElse("")));
        body.append("&client_secret=").append(encode(config.clientSecret().orElse("")));
        body.append("&redirect_uri=").append(encode(config.getRedirectUri()));
        return body.toString();
    }

    /**
     * Posts a token request, but only to an official HMRC host. The token URL comes from a system
     * property a {@code .env} file can set, so the destination is checked against {@link HmrcHosts}
     * immediately before the client secret is sent, whatever the configuration claims.
     */
    private CompletableFuture<OAuthTokens> sendTokenRequest(String body) {
        URI tokenUri = URI.create(config.tokenUrl());
        if (!HmrcHosts.isAllowed(tokenUri)) {
            log.error("Refusing to send credentials: token URL host is not an official HMRC host");
            return CompletableFuture.failedFuture(
                new HmrcOAuthException(OAuthError.CONFIGURATION_ERROR,
                    "The configured HMRC token URL is not an official HMRC address."));
        }
        HttpRequest request = HttpRequest.newBuilder()
            .uri(tokenUri)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .timeout(TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> handleTokenResponse(response));
    }

    private OAuthTokens handleTokenResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String responseBody = response.body();

        log.debug("Token response status: {}", statusCode);

        if (statusCode == 200) {
            try {
                return parseTokenResponse(responseBody);
            } catch (Exception e) {
                log.error("Failed to parse token response: {}",
                    HmrcPiiRedactor.redact(String.valueOf(e.getMessage())), e);
                throw new HmrcOAuthException(OAuthError.TOKEN_EXCHANGE_FAILED, "Invalid response format");
            }
        } else if (statusCode == 400) {
            OAuthError error = parseErrorResponse(responseBody);
            throw new HmrcOAuthException(error, HmrcPiiRedactor.redact(responseBody));
        } else if (statusCode == 401) {
            throw new HmrcOAuthException(OAuthError.INVALID_CLIENT, "Invalid client credentials");
        } else {
            throw new HmrcOAuthException(OAuthError.SERVER_ERROR, "HTTP " + statusCode);
        }
    }

    private OAuthTokens parseTokenResponse(String responseBody) throws Exception {
        var node = objectMapper.readTree(responseBody);

        String accessToken = node.get("access_token").asText();
        String refreshToken = node.has("refresh_token") ? node.get("refresh_token").asText() : null;
        long expiresIn = node.get("expires_in").asLong();
        String tokenType = node.has("token_type") ? node.get("token_type").asText() : "Bearer";
        String scope = node.has("scope") ? node.get("scope").asText() : "";

        return OAuthTokens.create(accessToken, refreshToken, expiresIn, tokenType, scope);
    }

    private OAuthError parseErrorResponse(String responseBody) {
        try {
            var node = objectMapper.readTree(responseBody);
            String error = node.has("error") ? node.get("error").asText() : "unknown";

            return switch (error) {
                case "invalid_grant" -> OAuthError.INVALID_GRANT;
                case "invalid_client" -> OAuthError.INVALID_CLIENT;
                case "access_denied" -> OAuthError.ACCESS_DENIED;
                default -> OAuthError.TOKEN_EXCHANGE_FAILED;
            };
        } catch (Exception e) {
            return OAuthError.TOKEN_EXCHANGE_FAILED;
        }
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
