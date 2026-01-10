package uk.selfemploy.hmrc.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.selfemploy.hmrc.client.dto.BusinessDetails;
import uk.selfemploy.hmrc.exception.HmrcApiException;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.hmrc.oauth.storage.TokenStorageService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for BusinessDetailsService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BusinessDetailsService")
class BusinessDetailsServiceTest {

    @Mock
    private BusinessDetailsClient businessDetailsClient;

    @Mock
    private TokenStorageService tokenStorageService;

    private BusinessDetailsService service;

    @BeforeEach
    void setup() {
        service = new BusinessDetailsService(businessDetailsClient, tokenStorageService);
    }

    @Nested
    @DisplayName("List Businesses")
    class ListBusinesses {

        @Test
        @DisplayName("should return list of businesses for NINO")
        void shouldReturnListOfBusinessesForNino() throws Exception {
            // Given
            String nino = "AA123456A";
            OAuthTokens tokens = createValidTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(tokens));

            List<BusinessDetails> businesses = List.of(
                createBusinessDetails("XAIS12345678901", "Software Development"),
                createBusinessDetails("XAIS98765432101", "Consulting")
            );
            BusinessDetailsClient.BusinessListResponse response =
                new BusinessDetailsClient.BusinessListResponse(businesses);

            when(businessDetailsClient.listBusinesses(eq(nino), anyString()))
                .thenReturn(CompletableFuture.completedFuture(response));

            // When
            List<BusinessDetails> result = service.listBusinesses(nino).toCompletableFuture().get();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).businessId()).isEqualTo("XAIS12345678901");
            assertThat(result.get(1).businessId()).isEqualTo("XAIS98765432101");
        }

        @Test
        @DisplayName("should pass Bearer token in authorization header")
        void shouldPassBearerTokenInAuthorizationHeader() throws Exception {
            // Given
            String nino = "AA123456A";
            OAuthTokens tokens = createValidTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(tokens));

            BusinessDetailsClient.BusinessListResponse response =
                new BusinessDetailsClient.BusinessListResponse(List.of());
            when(businessDetailsClient.listBusinesses(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(response));

            // When
            service.listBusinesses(nino).toCompletableFuture().get();

            // Then
            verify(businessDetailsClient).listBusinesses(eq(nino), eq("Bearer test_access_token"));
        }

        @Test
        @DisplayName("should throw exception when not authenticated")
        void shouldThrowExceptionWhenNotAuthenticated() {
            // Given
            String nino = "AA123456A";
            when(tokenStorageService.loadTokens()).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.listBusinesses(nino).toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(HmrcApiException.class)
                .hasMessageContaining("Not authenticated");
        }

        @Test
        @DisplayName("should return empty list when no businesses exist")
        void shouldReturnEmptyListWhenNoBusinessesExist() throws Exception {
            // Given
            String nino = "AA123456A";
            OAuthTokens tokens = createValidTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(tokens));

            BusinessDetailsClient.BusinessListResponse response =
                new BusinessDetailsClient.BusinessListResponse(List.of());
            when(businessDetailsClient.listBusinesses(eq(nino), anyString()))
                .thenReturn(CompletableFuture.completedFuture(response));

            // When
            List<BusinessDetails> result = service.listBusinesses(nino).toCompletableFuture().get();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Business Details")
    class GetBusinessDetails {

        @Test
        @DisplayName("should return business details for specific business")
        void shouldReturnBusinessDetailsForSpecificBusiness() throws Exception {
            // Given
            String nino = "AA123456A";
            String businessId = "XAIS12345678901";
            OAuthTokens tokens = createValidTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(tokens));

            BusinessDetails expectedDetails = createBusinessDetails(businessId, "Software Development");
            when(businessDetailsClient.getBusinessDetails(eq(nino), eq(businessId), anyString()))
                .thenReturn(CompletableFuture.completedFuture(expectedDetails));

            // When
            BusinessDetails result = service.getBusinessDetails(nino, businessId)
                .toCompletableFuture().get();

            // Then
            assertThat(result.businessId()).isEqualTo(businessId);
            assertThat(result.tradingName()).isEqualTo("Software Development");
        }

        @Test
        @DisplayName("should throw exception when not authenticated")
        void shouldThrowExceptionWhenNotAuthenticated() {
            // Given
            String nino = "AA123456A";
            String businessId = "XAIS12345678901";
            when(tokenStorageService.loadTokens()).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() ->
                service.getBusinessDetails(nino, businessId).toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(HmrcApiException.class)
                .hasMessageContaining("Not authenticated");
        }
    }

    @Nested
    @DisplayName("Connection Status")
    class ConnectionStatus {

        @Test
        @DisplayName("should return true when tokens are available and not expired")
        void shouldReturnTrueWhenTokensAvailableAndNotExpired() {
            // Given
            OAuthTokens tokens = createValidTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(tokens));

            // When
            boolean connected = service.isConnected();

            // Then
            assertThat(connected).isTrue();
        }

        @Test
        @DisplayName("should return false when no tokens available")
        void shouldReturnFalseWhenNoTokensAvailable() {
            // Given
            when(tokenStorageService.loadTokens()).thenReturn(Optional.empty());

            // When
            boolean connected = service.isConnected();

            // Then
            assertThat(connected).isFalse();
        }

        @Test
        @DisplayName("should return false when tokens are expired")
        void shouldReturnFalseWhenTokensExpired() {
            // Given
            OAuthTokens expiredTokens = createExpiredTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(expiredTokens));

            // When
            boolean connected = service.isConnected();

            // Then
            assertThat(connected).isFalse();
        }
    }

    @Nested
    @DisplayName("NINO Validation")
    class NinoValidation {

        @Test
        @DisplayName("should accept valid NINO format")
        void shouldAcceptValidNinoFormat() throws Exception {
            // Given
            String nino = "AA123456A";
            OAuthTokens tokens = createValidTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(tokens));

            BusinessDetailsClient.BusinessListResponse response =
                new BusinessDetailsClient.BusinessListResponse(List.of());
            when(businessDetailsClient.listBusinesses(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(response));

            // When
            List<BusinessDetails> result = service.listBusinesses(nino).toCompletableFuture().get();

            // Then
            assertThat(result).isNotNull();
            verify(businessDetailsClient).listBusinesses(eq("AA123456A"), anyString());
        }

        @Test
        @DisplayName("should accept NINO with spaces")
        void shouldAcceptNinoWithSpaces() throws Exception {
            // Given
            String nino = "AA 12 34 56 A";
            OAuthTokens tokens = createValidTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(tokens));

            BusinessDetailsClient.BusinessListResponse response =
                new BusinessDetailsClient.BusinessListResponse(List.of());
            when(businessDetailsClient.listBusinesses(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(response));

            // When
            service.listBusinesses(nino).toCompletableFuture().get();

            // Then - spaces should be removed
            verify(businessDetailsClient).listBusinesses(eq("AA123456A"), anyString());
        }

        @Test
        @DisplayName("should reject invalid NINO format")
        void shouldRejectInvalidNinoFormat() {
            // Given
            String invalidNino = "INVALID";
            OAuthTokens tokens = createValidTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(tokens));

            // When/Then
            assertThatThrownBy(() ->
                service.listBusinesses(invalidNino).toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid NINO format");
        }

        @Test
        @DisplayName("should reject null NINO")
        void shouldRejectNullNino() {
            // Given
            OAuthTokens tokens = createValidTokens();
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(tokens));

            // When/Then
            assertThatThrownBy(() ->
                service.listBusinesses(null).toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NINO cannot be null or empty");
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

    private OAuthTokens createExpiredTokens() {
        return new OAuthTokens(
            "expired_access_token",
            "test_refresh_token",
            3600L,
            "Bearer",
            "read:self-assessment write:self-assessment",
            Instant.now().minusSeconds(7200) // Expired 2 hours ago
        );
    }

    private BusinessDetails createBusinessDetails(String businessId, String tradingName) {
        return new BusinessDetails(
            businessId,
            "self-employment",
            tradingName,
            LocalDate.of(2020, 4, 6),
            List.of(new BusinessDetails.AccountingPeriod(
                LocalDate.of(2023, 4, 6),
                LocalDate.of(2024, 4, 5)
            )),
            null, // cessationDate
            "123 Business Street",
            "London",
            null,
            null,
            "SW1A 1AA",
            "GB"
        );
    }
}
