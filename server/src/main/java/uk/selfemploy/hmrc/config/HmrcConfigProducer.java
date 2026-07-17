package uk.selfemploy.hmrc.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.Config;

import java.util.List;
import java.util.Optional;

/**
 * Produces the {@link HmrcConfig} bean for the server stack from MicroProfile Config.
 *
 * <p>The interface used to carry a SmallRye {@code @ConfigMapping(prefix = "hmrc")} binding, which
 * made it injectable and property-driven under Quarkus. That binding was removed so the desktop
 * build no longer needs a config framework — the desktop implements the interface directly. This
 * producer restores injection and the same {@code hmrc.*} property keys for the server side; every
 * value falls back to the interface's own default, so only overrides need to be configured.
 */
@ApplicationScoped
public class HmrcConfigProducer {

    @Produces
    @ApplicationScoped
    HmrcConfig hmrcConfig(Config config) {
        return new MicroProfileHmrcConfig(config);
    }

    private static final class MicroProfileHmrcConfig implements HmrcConfig {

        private final Config config;

        private MicroProfileHmrcConfig(Config config) {
            this.config = config;
        }

        @Override
        public String apiBaseUrl() {
            return value("hmrc.api-base-url", HmrcConfig.super.apiBaseUrl());
        }

        @Override
        public String authorizeUrl() {
            return value("hmrc.authorize-url", HmrcConfig.super.authorizeUrl());
        }

        @Override
        public String tokenUrl() {
            return value("hmrc.token-url", HmrcConfig.super.tokenUrl());
        }

        @Override
        public Optional<String> clientId() {
            return config.getOptionalValue("hmrc.client-id", String.class);
        }

        @Override
        public Optional<String> clientSecret() {
            return config.getOptionalValue("hmrc.client-secret", String.class);
        }

        @Override
        public List<String> scopes() {
            return config.getOptionalValue("hmrc.scopes", String[].class)
                .map(List::of)
                .orElseGet(HmrcConfig.super::scopes);
        }

        @Override
        public int callbackPort() {
            return config.getOptionalValue("hmrc.callback-port", Integer.class)
                .orElseGet(HmrcConfig.super::callbackPort);
        }

        @Override
        public String callbackPath() {
            return value("hmrc.callback-path", HmrcConfig.super.callbackPath());
        }

        @Override
        public String productName() {
            return value("hmrc.product-name", HmrcConfig.super.productName());
        }

        @Override
        public String productVersion() {
            return value("hmrc.product-version", HmrcConfig.super.productVersion());
        }

        @Override
        public int authTimeoutSeconds() {
            return config.getOptionalValue("hmrc.auth-timeout-seconds", Integer.class)
                .orElseGet(HmrcConfig.super::authTimeoutSeconds);
        }

        private String value(String key, String fallback) {
            return config.getOptionalValue(key, String.class).orElse(fallback);
        }
    }
}
