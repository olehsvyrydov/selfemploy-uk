package uk.selfemploy.hmrc.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.util.List;
import java.util.Optional;

/**
 * Produces the {@link HmrcConfig} bean for the server stack by adapting the SmallRye-mapped
 * {@link ServerHmrcConfig}.
 *
 * <p>{@code HmrcConfig} itself is no longer a config mapping — the desktop build implements it
 * directly without a config framework — so the server side binds properties through
 * {@code ServerHmrcConfig} and exposes them under the interface the HMRC beans inject.
 */
@ApplicationScoped
public class HmrcConfigProducer {

    @Produces
    @ApplicationScoped
    HmrcConfig hmrcConfig(ServerHmrcConfig mapped) {
        return new MappedHmrcConfig(mapped);
    }

    private record MappedHmrcConfig(ServerHmrcConfig mapped) implements HmrcConfig {

        @Override
        public String apiBaseUrl() {
            return mapped.apiBaseUrl();
        }

        @Override
        public String authorizeUrl() {
            return mapped.authorizeUrl();
        }

        @Override
        public String tokenUrl() {
            return mapped.tokenUrl();
        }

        @Override
        public Optional<String> clientId() {
            return mapped.clientId();
        }

        @Override
        public Optional<String> clientSecret() {
            return mapped.clientSecret();
        }

        @Override
        public List<String> scopes() {
            return mapped.scopes();
        }

        @Override
        public int callbackPort() {
            return mapped.callbackPort();
        }

        @Override
        public String callbackPath() {
            return mapped.callbackPath();
        }

        @Override
        public String productName() {
            return mapped.productName();
        }

        @Override
        public String productVersion() {
            return mapped.productVersion();
        }

        @Override
        public int authTimeoutSeconds() {
            return mapped.authTimeoutSeconds();
        }
    }
}
