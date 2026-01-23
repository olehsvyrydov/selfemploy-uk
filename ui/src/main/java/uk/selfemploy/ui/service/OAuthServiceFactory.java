package uk.selfemploy.ui.service;

import io.vertx.core.Vertx;
import uk.selfemploy.hmrc.config.HmrcConfig;
import uk.selfemploy.hmrc.oauth.*;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Factory for creating OAuth service instances in the UI layer.
 * Since the UI doesn't use Quarkus CDI, we manually construct the OAuth components.
 */
public final class OAuthServiceFactory {

    private static final Logger LOG = Logger.getLogger(OAuthServiceFactory.class.getName());

    private static HmrcOAuthService instance;
    private static Vertx vertx;

    private OAuthServiceFactory() {
        // Utility class
    }

    /**
     * Gets or creates the singleton OAuth service instance.
     *
     * @return The HmrcOAuthService instance
     */
    public static synchronized HmrcOAuthService getOAuthService() {
        if (instance == null) {
            instance = createOAuthService();
        }
        return instance;
    }

    /**
     * Creates a new OAuth service with configuration from environment/system properties.
     */
    private static HmrcOAuthService createOAuthService() {
        LOG.info("Creating OAuth service from environment configuration");

        try {
            // Create config from environment variables
            HmrcConfig config = new SimpleHmrcConfig();

            // Log configuration status (without revealing secrets)
            if (config.isConfigured()) {
                LOG.info("HMRC credentials configured: clientId=" +
                    config.clientId().map(id -> id.substring(0, Math.min(8, id.length())) + "...").orElse("MISSING"));
            } else {
                LOG.warning("HMRC credentials NOT configured. Set HMRC_CLIENT_ID and HMRC_CLIENT_SECRET in .env file.");
            }

            // Create Vert.x instance for HTTP server (lazy, only when needed)
            LOG.info("Creating Vert.x instance for OAuth callback server");
            vertx = Vertx.vertx();

            // Create the OAuth components
            LOG.info("Creating OAuth components");
            BrowserLauncher browserLauncher = new DefaultBrowserLauncher();
            TokenExchangeClient tokenExchangeClient = new DefaultTokenExchangeClient(config);
            OAuthCallbackServer callbackServer = new OAuthCallbackServer(
                vertx,
                config.callbackPort(),
                config.callbackPath(),
                config.authTimeoutSeconds()
            );

            LOG.info("OAuth service created successfully");
            return new HmrcOAuthService(config, callbackServer, tokenExchangeClient, browserLauncher);

        } catch (Exception e) {
            LOG.severe("Failed to create OAuth service: " + e.getMessage());
            throw new RuntimeException("Failed to initialize OAuth service", e);
        }
    }

    /**
     * Shuts down the OAuth service and Vert.x instance.
     * Should be called when the application is closing.
     */
    public static synchronized void shutdown() {
        if (vertx != null) {
            vertx.close();
            vertx = null;
        }
        instance = null;
        LOG.info("OAuth service shut down");
    }

    /**
     * Checks if HMRC credentials are configured.
     */
    public static boolean isConfigured() {
        return getOAuthService() != null &&
               new SimpleHmrcConfig().isConfigured();
    }

    /**
     * Simple implementation of HmrcConfig that reads from System properties.
     * System properties are set by EnvLoader from the .env file.
     */
    private static class SimpleHmrcConfig implements HmrcConfig {

        @Override
        public String apiBaseUrl() {
            return System.getProperty("HMRC_API_BASE_URL",
                "https://test-api.service.hmrc.gov.uk");
        }

        @Override
        public String authorizeUrl() {
            return System.getProperty("HMRC_AUTHORIZE_URL",
                "https://test-www.tax.service.gov.uk/oauth/authorize");
        }

        @Override
        public String tokenUrl() {
            return System.getProperty("HMRC_TOKEN_URL",
                "https://test-api.service.hmrc.gov.uk/oauth/token");
        }

        @Override
        public Optional<String> clientId() {
            String value = System.getProperty("HMRC_CLIENT_ID");
            return (value != null && !value.isBlank()) ? Optional.of(value) : Optional.empty();
        }

        @Override
        public Optional<String> clientSecret() {
            String value = System.getProperty("HMRC_CLIENT_SECRET");
            return (value != null && !value.isBlank()) ? Optional.of(value) : Optional.empty();
        }

        @Override
        public List<String> scopes() {
            String scopes = System.getProperty("HMRC_SCOPES",
                "read:self-assessment,write:self-assessment");
            return List.of(scopes.split(","));
        }

        @Override
        public int callbackPort() {
            return Integer.parseInt(System.getProperty("HMRC_CALLBACK_PORT", "8088"));
        }

        @Override
        public String callbackPath() {
            return "/oauth/callback";
        }

        @Override
        public String productName() {
            return "UK Self-Employment Manager";
        }

        @Override
        public String productVersion() {
            return "0.1.0-SNAPSHOT";
        }

        @Override
        public int authTimeoutSeconds() {
            return 300;
        }
    }
}
