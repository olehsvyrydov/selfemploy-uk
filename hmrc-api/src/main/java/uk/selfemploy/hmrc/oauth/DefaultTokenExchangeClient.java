package uk.selfemploy.hmrc.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.config.HmrcConfig;
import uk.selfemploy.hmrc.exception.HmrcOAuthException;
import uk.selfemploy.hmrc.exception.HmrcOAuthException.OAuthError;
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
    public CompletableFuture<OAuthTokens> exchangeCodeForTokens(String authorizationCode) {
        log.debug("Exchanging authorization code for tokens");

        String body = buildTokenRequestBody("authorization_code", "code", authorizationCode);

        return sendTokenRequest(body)
            .exceptionally(ex -> {
                log.error("Token exchange failed", ex);
                if (ex.getCause() instanceof HmrcOAuthException) {
                    throw (HmrcOAuthException) ex.getCause();
                }
                throw new HmrcOAuthException(OAuthError.TOKEN_EXCHANGE_FAILED, ex);
            });
    }

    @Override
    public CompletableFuture<OAuthTokens> refreshTokens(String refreshToken) {
        log.debug("Refreshing access token");

        String body = buildTokenRequestBody("refresh_token", "refresh_token", refreshToken);

        return sendTokenRequest(body)
            .exceptionally(ex -> {
                log.error("Token refresh failed", ex);
                if (ex.getCause() instanceof HmrcOAuthException) {
                    throw (HmrcOAuthException) ex.getCause();
                }
                throw new HmrcOAuthException(OAuthError.TOKEN_EXCHANGE_FAILED, ex);
            });
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

    private CompletableFuture<OAuthTokens> sendTokenRequest(String body) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.tokenUrl()))
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
                log.error("Failed to parse token response", e);
                throw new HmrcOAuthException(OAuthError.TOKEN_EXCHANGE_FAILED, "Invalid response format");
            }
        } else if (statusCode == 400) {
            // Parse error response
            OAuthError error = parseErrorResponse(responseBody);
            throw new HmrcOAuthException(error, responseBody);
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
