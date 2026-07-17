package uk.selfemploy.hmrc.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.List;
import java.util.Optional;

/**
 * The server stack's SmallRye binding for the OAuth half of the {@code hmrc.*} prefix.
 *
 * <p>{@link HmrcConfig} carried this {@code @ConfigMapping} itself until the desktop build shed
 * its config-framework dependency. Declaring the same keys here keeps them known to SmallRye's
 * mapping validation — {@code hmrc.} shares its prefix with
 * {@link uk.selfemploy.hmrc.resilience.HmrcResilienceConfig}, and a key under a mapped prefix
 * that no root declares fails a Quarkus application at startup. Defaults mirror the interface's.
 */
@ConfigMapping(prefix = "hmrc")
public interface ServerHmrcConfig {

    /** Base URL for HMRC APIs. */
    @WithDefault("https://test-api.service.hmrc.gov.uk")
    String apiBaseUrl();

    /** OAuth2 authorization URL. */
    @WithDefault("https://test-www.tax.service.gov.uk/oauth/authorize")
    String authorizeUrl();

    /** OAuth2 token endpoint URL. */
    @WithDefault("https://test-api.service.hmrc.gov.uk/oauth/token")
    String tokenUrl();

    /** OAuth2 client ID from HMRC Developer Hub. */
    Optional<String> clientId();

    /** OAuth2 client secret from HMRC Developer Hub. */
    Optional<String> clientSecret();

    /** OAuth2 scopes required for MTD APIs. */
    @WithDefault("read:self-assessment,write:self-assessment")
    List<String> scopes();

    /** Localhost port for OAuth callback server. */
    @WithDefault("8088")
    int callbackPort();

    /** Callback path for OAuth redirect. */
    @WithDefault("/oauth/callback")
    String callbackPath();

    /** Product name for fraud prevention headers. */
    @WithDefault("UK Self-Employment Manager")
    String productName();

    /** Product version for fraud prevention headers. */
    @WithDefault("1.0.0")
    String productVersion();

    /** Timeout in seconds for OAuth flow. */
    @WithDefault("120")
    int authTimeoutSeconds();
}
