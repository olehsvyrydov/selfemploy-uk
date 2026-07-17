package uk.selfemploy.hmrc.config;

import java.util.List;
import java.util.Optional;

/**
 * Configuration for HMRC API integration.
 *
 * <p>Defaults target the HMRC sandbox; an implementation switches to production by overriding the
 * URL methods. The desktop app implements this directly from environment configuration, so no
 * config-binding framework is involved.
 */
public interface HmrcConfig {

    /**
     * Base URL for HMRC APIs.
     * Sandbox: https://test-api.service.hmrc.gov.uk
     * Production: https://api.service.hmrc.gov.uk
     */
    default String apiBaseUrl() {
        return "https://test-api.service.hmrc.gov.uk";
    }

    /**
     * OAuth2 authorization URL.
     * Sandbox: https://test-www.tax.service.gov.uk/oauth/authorize
     * Production: https://www.tax.service.gov.uk/oauth/authorize
     */
    default String authorizeUrl() {
        return "https://test-www.tax.service.gov.uk/oauth/authorize";
    }

    /**
     * OAuth2 token endpoint URL.
     * Sandbox: https://test-api.service.hmrc.gov.uk/oauth/token
     * Production: https://api.service.hmrc.gov.uk/oauth/token
     */
    default String tokenUrl() {
        return "https://test-api.service.hmrc.gov.uk/oauth/token";
    }

    /**
     * OAuth2 client ID from HMRC Developer Hub.
     * Must be set via environment variable HMRC_CLIENT_ID.
     */
    Optional<String> clientId();

    /**
     * OAuth2 client secret from HMRC Developer Hub.
     * Must be set via environment variable HMRC_CLIENT_SECRET.
     */
    Optional<String> clientSecret();

    /**
     * OAuth2 scopes required for MTD APIs.
     */
    default List<String> scopes() {
        return List.of("read:self-assessment", "write:self-assessment");
    }

    /**
     * Localhost port for OAuth callback server.
     */
    default int callbackPort() {
        return 8088;
    }

    /**
     * Callback path for OAuth redirect.
     */
    default String callbackPath() {
        return "/oauth/callback";
    }

    /**
     * Product name for fraud prevention headers.
     */
    default String productName() {
        return "UK Self-Employment Manager";
    }

    /**
     * Product version for fraud prevention headers.
     */
    default String productVersion() {
        return "1.0.0";
    }

    /**
     * Timeout in seconds for OAuth flow.
     */
    default int authTimeoutSeconds() {
        return 120;
    }

    /**
     * Returns the full redirect URI for OAuth callback.
     */
    default String getRedirectUri() {
        return "http://localhost:" + callbackPort() + callbackPath();
    }

    /**
     * Checks if credentials are configured.
     */
    default boolean isConfigured() {
        return clientId().isPresent() && clientSecret().isPresent() &&
               !clientId().get().isBlank() && !clientSecret().get().isBlank();
    }
}
