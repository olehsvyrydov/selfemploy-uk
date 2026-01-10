package uk.selfemploy.hmrc.client;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.selfemploy.hmrc.fraud.FraudPreventionHeaders;
import uk.selfemploy.hmrc.fraud.FraudPreventionService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.hmrc.oauth.storage.TokenStorageService;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for HmrcHeaderFactory.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("HmrcHeaderFactory")
class HmrcHeaderFactoryTest {

    @Mock
    private TokenStorageService tokenStorageService;

    @Mock
    private FraudPreventionService fraudPreventionService;

    private HmrcHeaderFactory headerFactory;

    @BeforeEach
    void setup() {
        headerFactory = new HmrcHeaderFactory(tokenStorageService, fraudPreventionService);
    }

    @Nested
    @DisplayName("Update Headers")
    class UpdateHeaders {

        @Test
        @DisplayName("should add Authorization header when tokens are available")
        void shouldAddAuthorizationHeaderWhenTokensAvailable() {
            // Given
            OAuthTokens tokens = createValidTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(tokens));
            when(fraudPreventionService.generateHeaders()).thenReturn(new LinkedHashMap<>());

            MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
            MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

            // When
            MultivaluedMap<String, String> result = headerFactory.update(incomingHeaders, outgoingHeaders);

            // Then
            assertThat(result.getFirst("Authorization"))
                .isEqualTo("Bearer test_access_token");
        }

        @Test
        @DisplayName("should not add Authorization header when no tokens available")
        void shouldNotAddAuthorizationHeaderWhenNoTokens() {
            // Given
            when(tokenStorageService.loadTokens()).thenReturn(Optional.empty());
            when(fraudPreventionService.generateHeaders()).thenReturn(new LinkedHashMap<>());

            MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
            MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

            // When
            MultivaluedMap<String, String> result = headerFactory.update(incomingHeaders, outgoingHeaders);

            // Then
            assertThat(result.containsKey("Authorization")).isFalse();
        }

        @Test
        @DisplayName("should add fraud prevention headers")
        void shouldAddFraudPreventionHeaders() {
            // Given
            when(tokenStorageService.loadTokens()).thenReturn(Optional.empty());

            Map<String, String> fraudHeaders = new LinkedHashMap<>();
            fraudHeaders.put(FraudPreventionHeaders.Headers.CONNECTION_METHOD, "DESKTOP_APP_DIRECT");
            fraudHeaders.put(FraudPreventionHeaders.Headers.DEVICE_ID, "device-123");
            fraudHeaders.put(FraudPreventionHeaders.Headers.TIMEZONE, "UTC+00:00");
            when(fraudPreventionService.generateHeaders()).thenReturn(fraudHeaders);

            MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
            MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

            // When
            MultivaluedMap<String, String> result = headerFactory.update(incomingHeaders, outgoingHeaders);

            // Then
            assertThat(result.getFirst(FraudPreventionHeaders.Headers.CONNECTION_METHOD))
                .isEqualTo("DESKTOP_APP_DIRECT");
            assertThat(result.getFirst(FraudPreventionHeaders.Headers.DEVICE_ID))
                .isEqualTo("device-123");
            assertThat(result.getFirst(FraudPreventionHeaders.Headers.TIMEZONE))
                .isEqualTo("UTC+00:00");
        }

        @Test
        @DisplayName("should preserve incoming headers")
        void shouldPreserveIncomingHeaders() {
            // Given
            when(tokenStorageService.loadTokens()).thenReturn(Optional.empty());
            when(fraudPreventionService.generateHeaders()).thenReturn(new LinkedHashMap<>());

            MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
            incomingHeaders.add("X-Custom-Header", "custom-value");

            MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

            // When
            MultivaluedMap<String, String> result = headerFactory.update(incomingHeaders, outgoingHeaders);

            // Then
            assertThat(result.getFirst("X-Custom-Header")).isEqualTo("custom-value");
        }

        @Test
        @DisplayName("should add Accept header for JSON")
        void shouldAddAcceptHeaderForJson() {
            // Given
            when(tokenStorageService.loadTokens()).thenReturn(Optional.empty());
            when(fraudPreventionService.generateHeaders()).thenReturn(new LinkedHashMap<>());

            MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
            MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

            // When
            MultivaluedMap<String, String> result = headerFactory.update(incomingHeaders, outgoingHeaders);

            // Then
            assertThat(result.getFirst("Accept")).isEqualTo("application/vnd.hmrc.1.0+json");
        }

        @Test
        @DisplayName("should combine authorization and fraud headers")
        void shouldCombineAuthorizationAndFraudHeaders() {
            // Given
            OAuthTokens tokens = createValidTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(tokens));

            Map<String, String> fraudHeaders = new LinkedHashMap<>();
            fraudHeaders.put(FraudPreventionHeaders.Headers.CONNECTION_METHOD, "DESKTOP_APP_DIRECT");
            fraudHeaders.put(FraudPreventionHeaders.Headers.DEVICE_ID, "device-456");
            when(fraudPreventionService.generateHeaders()).thenReturn(fraudHeaders);

            MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
            MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

            // When
            MultivaluedMap<String, String> result = headerFactory.update(incomingHeaders, outgoingHeaders);

            // Then
            assertThat(result.getFirst("Authorization"))
                .isEqualTo("Bearer test_access_token");
            assertThat(result.getFirst(FraudPreventionHeaders.Headers.CONNECTION_METHOD))
                .isEqualTo("DESKTOP_APP_DIRECT");
            assertThat(result.getFirst(FraudPreventionHeaders.Headers.DEVICE_ID))
                .isEqualTo("device-456");
        }

        @Test
        @DisplayName("should not overwrite existing Authorization header")
        void shouldNotOverwriteExistingAuthorizationHeader() {
            // Given
            OAuthTokens tokens = createValidTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(tokens));
            when(fraudPreventionService.generateHeaders()).thenReturn(new LinkedHashMap<>());

            MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
            incomingHeaders.add("Authorization", "Bearer existing_token");

            MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

            // When
            MultivaluedMap<String, String> result = headerFactory.update(incomingHeaders, outgoingHeaders);

            // Then
            assertThat(result.getFirst("Authorization"))
                .isEqualTo("Bearer existing_token");
        }
    }

    private OAuthTokens createValidTokens() {
        return new OAuthTokens(
            "test_access_token",
            "test_refresh_token",
            3600L,
            "Bearer",
            "read:self-assessment write:self-assessment",
            Instant.now()
        );
    }
}
