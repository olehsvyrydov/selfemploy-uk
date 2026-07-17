package uk.selfemploy.hmrc.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.config.HmrcConfig;
import uk.selfemploy.hmrc.exception.HmrcOAuthException;
import uk.selfemploy.hmrc.exception.HmrcOAuthException.OAuthError;

import java.net.http.HttpClient;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DefaultTokenExchangeClient — host gate")
class DefaultTokenExchangeClientTest {

    private HmrcConfig configWithTokenUrl(String tokenUrl) {
        HmrcConfig config = mock(HmrcConfig.class);
        when(config.tokenUrl()).thenReturn(tokenUrl);
        when(config.clientId()).thenReturn(Optional.of("test-client-id-value"));
        when(config.clientSecret()).thenReturn(Optional.of("test-client-secret-value"));
        when(config.getRedirectUri()).thenReturn("http://localhost:8088/oauth/callback");
        return config;
    }

    @Test
    @DisplayName("refuses to send credentials when the token URL is not an official HMRC host")
    void refusesNonHmrcTokenUrl() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HmrcConfig config = configWithTokenUrl("https://attacker.example/collect");
        DefaultTokenExchangeClient client =
            new DefaultTokenExchangeClient(config, httpClient, new ObjectMapper());

        assertThatThrownBy(() -> client.exchangeCodeForTokens("auth-code", "verifier").get())
            .isInstanceOf(ExecutionException.class)
            .cause()
            .isInstanceOf(HmrcOAuthException.class)
            .satisfies(e -> assertThat(((HmrcOAuthException) e).getError())
                .isEqualTo(OAuthError.CONFIGURATION_ERROR));

        // The secret must never have left the machine.
        verify(httpClient, never()).sendAsync(any(), any());
    }

    @Test
    @DisplayName("the same guard protects the refresh path")
    void refusesNonHmrcTokenUrlOnRefresh() {
        HttpClient httpClient = mock(HttpClient.class);
        HmrcConfig config = configWithTokenUrl("http://api.service.hmrc.gov.uk/oauth/token"); // http, not https
        DefaultTokenExchangeClient client =
            new DefaultTokenExchangeClient(config, httpClient, new ObjectMapper());

        assertThatThrownBy(() -> client.refreshTokens("refresh-token").get())
            .cause()
            .isInstanceOf(HmrcOAuthException.class);

        verify(httpClient, never()).sendAsync(any(), any());
    }
}
