package uk.selfemploy.ui.service;

import io.vertx.core.Vertx;
import uk.selfemploy.hmrc.config.HmrcConfig;
import uk.selfemploy.hmrc.oauth.*;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
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

            HmrcOAuthService service = new HmrcOAuthService(config, callbackServer, tokenExchangeClient, browserLauncher);

            // Sprint 12: Restore tokens from persistent storage
            restoreTokensFromStorage(service);

            LOG.info("OAuth service created successfully");
            return service;

        } catch (Exception e) {
            LOG.severe("Failed to create OAuth service: " + e.getMessage());
            throw new RuntimeException("Failed to initialize OAuth service", e);
        }
    }

    /**
     * Restores OAuth tokens from persistent storage.
     * Sprint 12: Enables session survival after app restart.
     *
     * <p>On restore, if tokens have less than 50% of their lifetime remaining,
     * we proactively refresh them to avoid mid-session expiry.</p>
     */
    private static void restoreTokensFromStorage(HmrcOAuthService service) {
        try {
            String[] tokenData = SqliteDataStore.getInstance().loadOAuthTokens();
            if (tokenData == null) {
                LOG.fine("No stored OAuth tokens found");
                return;
            }

            String accessToken = tokenData[0];
            String refreshToken = tokenData[1];
            long expiresIn = Long.parseLong(tokenData[2]);
            String tokenType = tokenData[3];
            String scope = tokenData[4];
            Instant issuedAt = tokenData[5] != null ? Instant.parse(tokenData[5]) : Instant.now();

            OAuthTokens tokens = new OAuthTokens(accessToken, refreshToken, expiresIn, tokenType, scope, issuedAt);

            if (tokens.isExpired()) {
                LOG.info("Stored OAuth tokens have expired - attempting refresh");
                if (refreshToken != null && !refreshToken.isBlank()) {
                    tryRefreshTokens(service, tokens);
                } else {
                    LOG.info("No refresh token available - clearing expired tokens");
                    SqliteDataStore.getInstance().clearOAuthTokens();
                }
                return;
            }

            // Proactive refresh: if less than 50% of lifetime remaining, refresh now
            long halfLifetime = expiresIn / 2;
            long secondsRemaining = tokens.getSecondsUntilExpiry();
            if (secondsRemaining < halfLifetime && refreshToken != null && !refreshToken.isBlank()) {
                LOG.info("Tokens at " + (secondsRemaining * 100 / expiresIn) + "% lifetime - proactively refreshing");
                service.setTokens(tokens); // Set first so refresh has the refresh token
                tryRefreshTokens(service, tokens);
                return;
            }

            service.setTokens(tokens);
            LOG.info("OAuth tokens restored from storage (expires in " + tokens.getSecondsUntilExpiry() + "s)");

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to restore OAuth tokens from storage", e);
        }
    }

    /**
     * Attempts to refresh tokens. If successful, persists the new tokens.
     * If refresh fails, clears the stored tokens.
     */
    private static void tryRefreshTokens(HmrcOAuthService service, OAuthTokens oldTokens) {
        try {
            service.setTokens(oldTokens); // Ensure refresh token is available
            OAuthTokens newTokens = service.refreshAccessToken().get(30, java.util.concurrent.TimeUnit.SECONDS);

            // Persist new tokens
            SqliteDataStore.getInstance().saveOAuthTokens(
                newTokens.accessToken(),
                newTokens.refreshToken(),
                newTokens.expiresIn(),
                newTokens.tokenType(),
                newTokens.scope(),
                newTokens.issuedAt()
            );
            LOG.info("OAuth tokens refreshed and persisted (expires in " + newTokens.getSecondsUntilExpiry() + "s)");

        } catch (Exception e) {
            LOG.warning("Failed to refresh OAuth tokens: " + e.getMessage() + " - clearing stored tokens");
            SqliteDataStore.getInstance().clearOAuthTokens();
            service.setTokens(null);
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
    /**
     * Returns the configured OAuth authentication timeout in seconds.
     *
     * @return timeout in seconds
     */
    public static int getAuthTimeoutSeconds() {
        return new SimpleHmrcConfig().authTimeoutSeconds();
    }

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
            return 120;
        }
    }
}
