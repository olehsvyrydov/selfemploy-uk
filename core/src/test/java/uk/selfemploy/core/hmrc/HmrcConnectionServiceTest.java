package uk.selfemploy.core.hmrc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.hmrc.config.HmrcConfig;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.hmrc.oauth.storage.TokenStorageService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HmrcConnectionService.
 * Tests cover connection status, OAuth flow, and wizard prerequisites.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HmrcConnectionService Tests")
class HmrcConnectionServiceTest {

    @Mock
    private HmrcOAuthService oAuthService;

    @Mock
    private TokenStorageService tokenStorageService;

    @Mock
    private HmrcConfig hmrcConfig;

    private HmrcConnectionService connectionService;

    @BeforeEach
    void setUp() {
        connectionService = new HmrcConnectionService(oAuthService, tokenStorageService, hmrcConfig);
    }

    @Nested
    @DisplayName("Connection Status Tests")
    class ConnectionStatusTests {

        @Test
        @DisplayName("should return CONNECTED when valid tokens exist")
        void shouldReturnConnectedWhenValidTokensExist() {
            // Given
            OAuthTokens validTokens = createValidTokens();
            when(hmrcConfig.isConfigured()).thenReturn(true);
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(validTokens));

            // When
            ConnectionStatus status = connectionService.getConnectionStatus();

            // Then
            assertThat(status.state()).isEqualTo(ConnectionState.CONNECTED);
            assertThat(status.isConnected()).isTrue();
        }

        @Test
        @DisplayName("should return NOT_CONNECTED when no tokens exist")
        void shouldReturnNotConnectedWhenNoTokens() {
            // Given
            when(hmrcConfig.isConfigured()).thenReturn(true);
            when(tokenStorageService.loadTokens()).thenReturn(Optional.empty());

            // When
            ConnectionStatus status = connectionService.getConnectionStatus();

            // Then
            assertThat(status.state()).isEqualTo(ConnectionState.NOT_CONNECTED);
            assertThat(status.isConnected()).isFalse();
        }

        @Test
        @DisplayName("should return EXPIRED when tokens are expired")
        void shouldReturnExpiredWhenTokensExpired() {
            // Given
            OAuthTokens expiredTokens = createExpiredTokens();
            when(hmrcConfig.isConfigured()).thenReturn(true);
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(expiredTokens));

            // When
            ConnectionStatus status = connectionService.getConnectionStatus();

            // Then
            assertThat(status.state()).isEqualTo(ConnectionState.EXPIRED);
            assertThat(status.isConnected()).isFalse();
            assertThat(status.canRefresh()).isTrue();
        }

        @Test
        @DisplayName("should return NOT_CONFIGURED when credentials missing")
        void shouldReturnNotConfiguredWhenCredentialsMissing() {
            // Given
            when(hmrcConfig.isConfigured()).thenReturn(false);

            // When
            ConnectionStatus status = connectionService.getConnectionStatus();

            // Then
            assertThat(status.state()).isEqualTo(ConnectionState.NOT_CONFIGURED);
        }
    }

    @Nested
    @DisplayName("Prerequisites Tests")
    class PrerequisitesTests {

        @Test
        @DisplayName("should list all prerequisites")
        void shouldListAllPrerequisites() {
            // When
            List<HmrcPrerequisite> prerequisites = connectionService.getPrerequisites();

            // Then
            assertThat(prerequisites).isNotEmpty();
            assertThat(prerequisites).anyMatch(p -> p.id().equals("self_assessment_registration"));
            assertThat(prerequisites).anyMatch(p -> p.id().equals("utr_number"));
            assertThat(prerequisites).anyMatch(p -> p.id().equals("government_gateway"));
        }

        @Test
        @DisplayName("should mark prerequisite as completed")
        void shouldMarkPrerequisiteAsCompleted() {
            // Given
            String prerequisiteId = "self_assessment_registration";

            // When
            connectionService.markPrerequisiteComplete(prerequisiteId, true);

            // Then
            List<HmrcPrerequisite> prerequisites = connectionService.getPrerequisites();
            HmrcPrerequisite registration = prerequisites.stream()
                .filter(p -> p.id().equals(prerequisiteId))
                .findFirst()
                .orElseThrow();

            assertThat(registration.isComplete()).isTrue();
        }

        @Test
        @DisplayName("should check if all prerequisites are met")
        void shouldCheckIfAllPrerequisitesMet() {
            // Given - mark all as complete
            connectionService.markPrerequisiteComplete("self_assessment_registration", true);
            connectionService.markPrerequisiteComplete("utr_number", true);
            connectionService.markPrerequisiteComplete("government_gateway", true);

            // When
            boolean allMet = connectionService.areAllPrerequisitesMet();

            // Then
            assertThat(allMet).isTrue();
        }

        @Test
        @DisplayName("should return false when prerequisites not met")
        void shouldReturnFalseWhenPrerequisitesNotMet() {
            // Given - leave some incomplete
            connectionService.markPrerequisiteComplete("self_assessment_registration", true);

            // When
            boolean allMet = connectionService.areAllPrerequisitesMet();

            // Then
            assertThat(allMet).isFalse();
        }
    }

    @Nested
    @DisplayName("Connect to HMRC Tests")
    class ConnectToHmrcTests {

        @Test
        @DisplayName("should initiate OAuth flow when connecting")
        void shouldInitiateOAuthFlowWhenConnecting() {
            // Given
            OAuthTokens tokens = createValidTokens();
            when(oAuthService.authenticate())
                .thenReturn(CompletableFuture.completedFuture(tokens));

            // When
            CompletableFuture<ConnectionResult> result = connectionService.connect();

            // Then
            assertThat(result).isCompletedWithValueMatching(r -> r.success());
            verify(oAuthService).authenticate();
        }

        @Test
        @DisplayName("should save tokens after successful connection")
        void shouldSaveTokensAfterConnection() throws Exception {
            // Given
            OAuthTokens tokens = createValidTokens();
            when(oAuthService.authenticate())
                .thenReturn(CompletableFuture.completedFuture(tokens));

            // When
            connectionService.connect().get();

            // Then
            verify(tokenStorageService).saveTokens(tokens);
        }

        @Test
        @DisplayName("should return failure result on OAuth error")
        void shouldReturnFailureOnOAuthError() {
            // Given
            when(oAuthService.authenticate())
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Auth failed")));

            // When
            CompletableFuture<ConnectionResult> result = connectionService.connect();

            // Then
            assertThat(result).isCompletedWithValueMatching(r -> !r.success());
        }
    }

    @Nested
    @DisplayName("Disconnect Tests")
    class DisconnectTests {

        @Test
        @DisplayName("should clear tokens on disconnect")
        void shouldClearTokensOnDisconnect() {
            // When
            connectionService.disconnect();

            // Then
            verify(tokenStorageService).deleteTokens();
            verify(oAuthService).disconnect();
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("should refresh tokens when expired")
        void shouldRefreshTokensWhenExpired() {
            // Given
            OAuthTokens newTokens = createValidTokens();
            when(oAuthService.refreshAccessToken())
                .thenReturn(CompletableFuture.completedFuture(newTokens));

            // When
            CompletableFuture<ConnectionResult> result = connectionService.refreshConnection();

            // Then
            assertThat(result).isCompletedWithValueMatching(ConnectionResult::success);
            verify(tokenStorageService).saveTokens(newTokens);
        }
    }

    @Nested
    @DisplayName("User Guide Content Tests")
    class UserGuideContentTests {

        @Test
        @DisplayName("should return registration guide content")
        void shouldReturnRegistrationGuideContent() {
            // When
            HmrcGuideContent content = connectionService.getRegistrationGuide();

            // Then
            assertThat(content).isNotNull();
            assertThat(content.title()).isNotEmpty();
            assertThat(content.steps()).isNotEmpty();
        }

        @Test
        @DisplayName("should return UTR guide content")
        void shouldReturnUtrGuideContent() {
            // When
            HmrcGuideContent content = connectionService.getUtrGuide();

            // Then
            assertThat(content).isNotNull();
            assertThat(content.title()).contains("UTR");
        }

        @Test
        @DisplayName("should return Government Gateway guide content")
        void shouldReturnGovernmentGatewayGuideContent() {
            // When
            HmrcGuideContent content = connectionService.getGovernmentGatewayGuide();

            // Then
            assertThat(content).isNotNull();
            assertThat(content.title()).containsIgnoringCase("Government Gateway");
        }
    }

    // =========================================================================
    // PS11-003: HMRC Registration Guide - Additional Tests
    // =========================================================================
    @Nested
    @DisplayName("PS11-003: Wizard Navigation Tests")
    class Ps11003WizardNavigationTests {

        @Test
        @DisplayName("HMRC-U01: should return prerequisites in correct order")
        void shouldReturnPrerequisitesInCorrectOrder() {
            // When
            List<HmrcPrerequisite> prerequisites = connectionService.getPrerequisites();

            // Then
            assertThat(prerequisites).hasSize(3);
            assertThat(prerequisites.get(0).id()).isEqualTo("self_assessment_registration");
            assertThat(prerequisites.get(1).id()).isEqualTo("utr_number");
            assertThat(prerequisites.get(2).id()).isEqualTo("government_gateway");
        }

        @Test
        @DisplayName("HMRC-U02: should track prerequisite completion state")
        void shouldTrackPrerequisiteCompletionState() {
            // Given
            connectionService.markPrerequisiteComplete("self_assessment_registration", true);

            // When
            List<HmrcPrerequisite> prerequisites = connectionService.getPrerequisites();

            // Then
            HmrcPrerequisite registration = prerequisites.stream()
                .filter(p -> p.id().equals("self_assessment_registration"))
                .findFirst()
                .orElseThrow();
            assertThat(registration.isComplete()).isTrue();
        }

        @Test
        @DisplayName("HMRC-U03: should allow toggling prerequisite completion")
        void shouldAllowTogglingPrerequisiteCompletion() {
            // Given
            connectionService.markPrerequisiteComplete("utr_number", true);

            // When
            connectionService.markPrerequisiteComplete("utr_number", false);

            // Then
            List<HmrcPrerequisite> prerequisites = connectionService.getPrerequisites();
            HmrcPrerequisite utr = prerequisites.stream()
                .filter(p -> p.id().equals("utr_number"))
                .findFirst()
                .orElseThrow();
            assertThat(utr.isComplete()).isFalse();
        }

        @Test
        @DisplayName("HMRC-U04: should report all prerequisites met status")
        void shouldReportAllPrerequisitesMetStatus() {
            // Given - All complete
            connectionService.markPrerequisiteComplete("self_assessment_registration", true);
            connectionService.markPrerequisiteComplete("utr_number", true);
            connectionService.markPrerequisiteComplete("government_gateway", true);

            // When/Then
            assertThat(connectionService.areAllPrerequisitesMet()).isTrue();
        }

        @Test
        @DisplayName("HMRC-U05: should report prerequisites not met when incomplete")
        void shouldReportPrerequisitesNotMetWhenIncomplete() {
            // Given - Only one complete
            connectionService.markPrerequisiteComplete("self_assessment_registration", true);

            // When/Then
            assertThat(connectionService.areAllPrerequisitesMet()).isFalse();
        }
    }

    @Nested
    @DisplayName("PS11-003: Prerequisites Checklist Validation Tests")
    class Ps11003PrerequisitesChecklistTests {

        @Test
        @DisplayName("HMRC-U06: should include Self Assessment registration prerequisite")
        void shouldIncludeSelfAssessmentRegistrationPrerequisite() {
            // When
            List<HmrcPrerequisite> prerequisites = connectionService.getPrerequisites();

            // Then
            assertThat(prerequisites).anyMatch(p ->
                p.id().equals("self_assessment_registration") &&
                p.title() != null &&
                !p.title().isEmpty());
        }

        @Test
        @DisplayName("HMRC-U07: should include UTR number prerequisite")
        void shouldIncludeUtrNumberPrerequisite() {
            // When
            List<HmrcPrerequisite> prerequisites = connectionService.getPrerequisites();

            // Then
            assertThat(prerequisites).anyMatch(p ->
                p.id().equals("utr_number") &&
                p.title() != null &&
                !p.title().isEmpty());
        }

        @Test
        @DisplayName("HMRC-U08: should include Government Gateway prerequisite")
        void shouldIncludeGovernmentGatewayPrerequisite() {
            // When
            List<HmrcPrerequisite> prerequisites = connectionService.getPrerequisites();

            // Then
            assertThat(prerequisites).anyMatch(p ->
                p.id().equals("government_gateway") &&
                p.title() != null &&
                !p.title().isEmpty());
        }

        @Test
        @DisplayName("HMRC-U09: should provide description for each prerequisite")
        void shouldProvideDescriptionForEachPrerequisite() {
            // When
            List<HmrcPrerequisite> prerequisites = connectionService.getPrerequisites();

            // Then
            for (HmrcPrerequisite prerequisite : prerequisites) {
                assertThat(prerequisite.description())
                    .as("Prerequisite %s should have description", prerequisite.id())
                    .isNotNull()
                    .isNotEmpty();
            }
        }

        @Test
        @DisplayName("HMRC-U10: should provide help URL for each prerequisite")
        void shouldProvideHelpUrlForEachPrerequisite() {
            // When
            List<HmrcPrerequisite> prerequisites = connectionService.getPrerequisites();

            // Then
            for (HmrcPrerequisite prerequisite : prerequisites) {
                assertThat(prerequisite.helpUrl())
                    .as("Prerequisite %s should have help URL", prerequisite.id())
                    .isNotNull()
                    .startsWith("https://");
            }
        }
    }

    @Nested
    @DisplayName("PS11-003: OAuth Flow Integration Tests")
    class Ps11003OAuthFlowIntegrationTests {

        @Test
        @DisplayName("HMRC-U11: should initiate OAuth flow on connect")
        void shouldInitiateOAuthFlowOnConnect() {
            // Given
            OAuthTokens tokens = createValidTokens();
            when(oAuthService.authenticate())
                .thenReturn(CompletableFuture.completedFuture(tokens));

            // When
            CompletableFuture<ConnectionResult> result = connectionService.connect();

            // Then
            verify(oAuthService).authenticate();
            assertThat(result).isCompletedWithValueMatching(ConnectionResult::success);
        }

        @Test
        @DisplayName("HMRC-U12: should store tokens after successful OAuth")
        void shouldStoreTokensAfterSuccessfulOAuth() throws Exception {
            // Given
            OAuthTokens tokens = createValidTokens();
            when(oAuthService.authenticate())
                .thenReturn(CompletableFuture.completedFuture(tokens));

            // When
            connectionService.connect().get();

            // Then
            verify(tokenStorageService).saveTokens(tokens);
        }

        @Test
        @DisplayName("HMRC-U13: should handle OAuth cancellation")
        void shouldHandleOAuthCancellation() {
            // Given
            when(oAuthService.authenticate())
                .thenReturn(CompletableFuture.failedFuture(
                    new RuntimeException("User cancelled authentication")));

            // When
            CompletableFuture<ConnectionResult> result = connectionService.connect();

            // Then
            assertThat(result).isCompletedWithValueMatching(r -> !r.success());
        }

        @Test
        @DisplayName("HMRC-U14: should handle OAuth timeout")
        void shouldHandleOAuthTimeout() {
            // Given
            when(oAuthService.authenticate())
                .thenReturn(CompletableFuture.failedFuture(
                    new RuntimeException("Authentication timeout")));

            // When
            CompletableFuture<ConnectionResult> result = connectionService.connect();

            // Then
            assertThat(result).isCompletedWithValueMatching(r -> !r.success());
        }

        @Test
        @DisplayName("HMRC-U15: should handle network error during OAuth")
        void shouldHandleNetworkErrorDuringOAuth() {
            // Given
            when(oAuthService.authenticate())
                .thenReturn(CompletableFuture.failedFuture(
                    new RuntimeException("Network error")));

            // When
            CompletableFuture<ConnectionResult> result = connectionService.connect();

            // Then
            assertThat(result).isCompletedWithValueMatching(r -> !r.success());
        }
    }

    @Nested
    @DisplayName("PS11-003: Connection Status Storage Tests")
    class Ps11003ConnectionStatusStorageTests {

        @Test
        @DisplayName("HMRC-U16: should persist connection status across sessions")
        void shouldPersistConnectionStatusAcrossSessions() {
            // Given
            OAuthTokens validTokens = createValidTokens();
            when(hmrcConfig.isConfigured()).thenReturn(true);
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(validTokens));

            // When
            ConnectionStatus status = connectionService.getConnectionStatus();

            // Then
            assertThat(status.state()).isEqualTo(ConnectionState.CONNECTED);
        }

        @Test
        @DisplayName("HMRC-U17: should clear connection status on disconnect")
        void shouldClearConnectionStatusOnDisconnect() {
            // When
            connectionService.disconnect();

            // Then
            verify(tokenStorageService).deleteTokens();
            verify(oAuthService).disconnect();
        }

        @Test
        @DisplayName("HMRC-U18: should detect expired tokens")
        void shouldDetectExpiredTokens() {
            // Given
            OAuthTokens expiredTokens = createExpiredTokens();
            when(hmrcConfig.isConfigured()).thenReturn(true);
            when(tokenStorageService.loadTokens()).thenReturn(Optional.of(expiredTokens));

            // When
            ConnectionStatus status = connectionService.getConnectionStatus();

            // Then
            assertThat(status.state()).isEqualTo(ConnectionState.EXPIRED);
        }

        @Test
        @DisplayName("HMRC-U19: should refresh expired tokens automatically")
        void shouldRefreshExpiredTokensAutomatically() {
            // Given
            OAuthTokens newTokens = createValidTokens();
            when(oAuthService.refreshAccessToken())
                .thenReturn(CompletableFuture.completedFuture(newTokens));

            // When
            CompletableFuture<ConnectionResult> result = connectionService.refreshConnection();

            // Then
            assertThat(result).isCompletedWithValueMatching(ConnectionResult::success);
            verify(tokenStorageService).saveTokens(newTokens);
        }
    }

    @Nested
    @DisplayName("PS11-003: Error Handling Tests")
    class Ps11003ErrorHandlingTests {

        @Test
        @DisplayName("HMRC-U20: should report NOT_CONFIGURED when credentials missing")
        void shouldReportNotConfiguredWhenCredentialsMissing() {
            // Given
            when(hmrcConfig.isConfigured()).thenReturn(false);

            // When
            ConnectionStatus status = connectionService.getConnectionStatus();

            // Then
            assertThat(status.state()).isEqualTo(ConnectionState.NOT_CONFIGURED);
        }

        @Test
        @DisplayName("HMRC-U21: should report NOT_CONNECTED when no tokens exist")
        void shouldReportNotConnectedWhenNoTokensExist() {
            // Given
            when(hmrcConfig.isConfigured()).thenReturn(true);
            when(tokenStorageService.loadTokens()).thenReturn(Optional.empty());

            // When
            ConnectionStatus status = connectionService.getConnectionStatus();

            // Then
            assertThat(status.state()).isEqualTo(ConnectionState.NOT_CONNECTED);
        }

        @Test
        @DisplayName("HMRC-U22: should handle refresh token failure")
        void shouldHandleRefreshTokenFailure() {
            // Given
            when(oAuthService.refreshAccessToken())
                .thenReturn(CompletableFuture.failedFuture(
                    new RuntimeException("Refresh token expired")));

            // When
            CompletableFuture<ConnectionResult> result = connectionService.refreshConnection();

            // Then
            assertThat(result).isCompletedWithValueMatching(r -> !r.success());
        }
    }

    // Helper methods

    private OAuthTokens createValidTokens() {
        return new OAuthTokens(
            "valid-access-token",
            "valid-refresh-token",
            14400, // 4 hours
            "Bearer",
            "read:self-assessment write:self-assessment",
            Instant.now()
        );
    }

    private OAuthTokens createExpiredTokens() {
        return new OAuthTokens(
            "expired-access-token",
            "valid-refresh-token",
            14400,
            "Bearer",
            "read:self-assessment write:self-assessment",
            Instant.now().minusSeconds(20000) // Expired
        );
    }
}
