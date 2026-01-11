package uk.selfemploy.hmrc.oauth;

import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import uk.selfemploy.hmrc.config.HmrcConfig;

/**
 * CDI Producer for OAuth-related beans that require special construction.
 *
 * <p>This producer is needed because some OAuth components have complex
 * construction requirements that cannot be satisfied by standard CDI injection:</p>
 *
 * <ul>
 *   <li>{@link HmrcOAuthService} - requires OAuthCallbackServer which needs Vertx</li>
 *   <li>{@link OAuthCallbackServer} - requires Vertx instance and config values</li>
 * </ul>
 *
 * <p>Other OAuth beans like {@link DefaultTokenExchangeClient} and
 * {@link DefaultBrowserLauncher} are already CDI beans with {@code @ApplicationScoped}.</p>
 */
@ApplicationScoped
public class OAuthBeanProducer {

    @Inject
    HmrcConfig config;

    @Inject
    Vertx vertx;

    /**
     * Produces the OAuth callback server.
     *
     * <p>Requires Vertx for HTTP server and config values for port/path.</p>
     */
    @Produces
    @Singleton
    public OAuthCallbackServer produceCallbackServer() {
        return new OAuthCallbackServer(
                vertx,
                config.callbackPort(),
                config.callbackPath(),
                config.authTimeoutSeconds()
        );
    }

    /**
     * Produces the main OAuth service.
     *
     * <p>Injects existing CDI beans (TokenExchangeClient, BrowserLauncher)
     * and the producer-created OAuthCallbackServer.</p>
     */
    @Produces
    @Singleton
    public HmrcOAuthService produceOAuthService(
            OAuthCallbackServer callbackServer,
            TokenExchangeClient tokenExchangeClient,
            BrowserLauncher browserLauncher) {
        return new HmrcOAuthService(
                config,
                callbackServer,
                tokenExchangeClient,
                browserLauncher
        );
    }
}
