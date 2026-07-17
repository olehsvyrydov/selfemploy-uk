package uk.selfemploy.ui.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("OAuthServiceFactory.getRedirectUri")
class OAuthServiceFactoryRedirectUriTest {

    @AfterEach
    void clearPort() {
        System.clearProperty("HMRC_CALLBACK_PORT");
    }

    @Test
    @DisplayName("uses the configured callback port")
    void usesConfiguredPort() {
        System.setProperty("HMRC_CALLBACK_PORT", "9000");
        assertThat(OAuthServiceFactory.getRedirectUri()).isEqualTo("http://localhost:9000/oauth/callback");
    }

    @Test
    @DisplayName("falls back to the default port for a non-numeric value rather than throwing")
    void nonNumericPortDoesNotThrow() {
        System.setProperty("HMRC_CALLBACK_PORT", "not-a-port");

        assertThatCode(OAuthServiceFactory::getRedirectUri).doesNotThrowAnyException();
        assertThat(OAuthServiceFactory.getRedirectUri()).isEqualTo("http://localhost:8088/oauth/callback");
    }

    @Test
    @DisplayName("falls back to the default port for an out-of-range value")
    void outOfRangePortFallsBack() {
        System.setProperty("HMRC_CALLBACK_PORT", "808888");
        assertThat(OAuthServiceFactory.getRedirectUri()).isEqualTo("http://localhost:8088/oauth/callback");

        System.setProperty("HMRC_CALLBACK_PORT", "-1");
        assertThat(OAuthServiceFactory.getRedirectUri()).isEqualTo("http://localhost:8088/oauth/callback");
    }
}
