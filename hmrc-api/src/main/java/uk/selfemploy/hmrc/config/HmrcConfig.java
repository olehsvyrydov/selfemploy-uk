package uk.selfemploy.hmrc.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.List;
import java.util.Optional;

/**
 * Configuration for HMRC API integration.
 * Uses SmallRye Config for type-safe configuration mapping.
 */
@ConfigMapping(prefix = "hmrc")
public interface HmrcConfig {

    /**
     * Base URL for HMRC APIs.
     * Sandbox: https://test-api.service.hmrc.gov.uk
     * Production: https://api.service.hmrc.gov.uk
     */
    @WithDefault("https://test-api.service.hmrc.gov.uk")
    String apiBaseUrl();

    /**
     * OAuth2 authorization URL.
     * Sandbox: https://test-www.tax.service.gov.uk/oauth/authorize
     * Production: https://www.tax.service.gov.uk/oauth/authorize
     */
    @WithDefault("https://test-www.tax.service.gov.uk/oauth/authorize")
    String authorizeUrl();

    /**
     * OAuth2 token endpoint URL.
     * Sandbox: https://test-api.service.hmrc.gov.uk/oauth/token
     * Production: https://api.service.hmrc.gov.uk/oauth/token
     */
    @WithDefault("https://test-api.service.hmrc.gov.uk/oauth/token")
    String tokenUrl();

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
    @WithDefault("read:self-assessment,write:self-assessment")
    List<String> scopes();

    /**
     * Localhost port for OAuth callback server.
     */
    @WithDefault("8088")
    int callbackPort();

    /**
     * Callback path for OAuth redirect.
     */
    @WithDefault("/oauth/callback")
    String callbackPath();

    /**
     * Product name for fraud prevention headers.
     */
    @WithDefault("UK Self-Employment Manager")
    String productName();

    /**
     * Product version for fraud prevention headers.
     */
    @WithDefault("1.0.0")
    String productVersion();

    /**
     * Timeout in seconds for OAuth flow.
     */
    @WithDefault("300")
    int authTimeoutSeconds();

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
