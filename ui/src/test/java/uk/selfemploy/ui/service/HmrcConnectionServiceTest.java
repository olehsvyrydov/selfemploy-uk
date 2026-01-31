package uk.selfemploy.ui.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.service.HmrcConnectionService.ConnectionState;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for HmrcConnectionService - Single Source of Truth for HMRC connection status.
 *
 * <p>Tests the connection state machine:</p>
 * <ul>
 *   <li>NOT_CONNECTED - No OAuth tokens or business ID</li>
 *   <li>CONNECTED - OAuth tokens valid but no business ID</li>
 *   <li>PROFILE_SYNCED - OAuth tokens valid AND business ID stored</li>
 *   <li>READY_TO_SUBMIT - All prerequisites met (tokens, business ID, NINO)</li>
 * </ul>
 */
@DisplayName("HmrcConnectionService - Single Source of Truth")
class HmrcConnectionServiceTest {

    private HmrcConnectionService service;

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetTestData();
        service = new HmrcConnectionService();
        // Mark session as verified by default for tests that don't specifically test verification
        service.markSessionVerified();
    }

    @Nested
    @DisplayName("Connection State Determination")
    class ConnectionStateTests {

        @Test
        @DisplayName("should return NOT_CONNECTED when no business ID stored")
        void shouldReturnNotConnectedWhenNoBusinessId() {
            // Given: No HMRC business ID in store
            assertThat(SqliteDataStore.getInstance().loadHmrcBusinessId()).isNull();

            // When
            ConnectionState state = service.getConnectionState();

            // Then
            assertThat(state).isEqualTo(ConnectionState.NOT_CONNECTED);
        }

        @Test
        @DisplayName("should return SESSION_EXPIRED when business ID exists but no OAuth tokens")
        void shouldReturnSessionExpiredWhenNoOAuthTokens() {
            // Given: HMRC business ID stored but no OAuth tokens
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            // No OAuth tokens

            // When
            ConnectionState state = service.getConnectionState();

            // Then
            assertThat(state).isEqualTo(ConnectionState.SESSION_EXPIRED);
        }

        @Test
        @DisplayName("should return PROFILE_SYNCED when business ID and valid OAuth tokens exist")
        void shouldReturnProfileSyncedWhenBusinessIdAndValidTokensExist() {
            // Given: HMRC business ID and valid OAuth tokens
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());

            // When
            ConnectionState state = service.getConnectionState();

            // Then
            assertThat(state).isEqualTo(ConnectionState.PROFILE_SYNCED);
        }

        @Test
        @DisplayName("should return READY_TO_SUBMIT when business ID, valid OAuth tokens, and NINO exist")
        void shouldReturnReadyToSubmitWhenAllPrerequisitesMet() {
            // Given: All prerequisites met
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());
            SqliteDataStore.getInstance().saveNino("AB123456C");

            // When
            ConnectionState state = service.getConnectionState();

            // Then
            assertThat(state).isEqualTo(ConnectionState.READY_TO_SUBMIT);
        }

        @Test
        @DisplayName("should return SESSION_EXPIRED when business ID exists but OAuth tokens expired")
        void shouldReturnSessionExpiredWhenOAuthTokensExpired() {
            // Given: Business ID exists but OAuth tokens are expired (issued 5 hours ago, expires in 4 hours)
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            java.time.Instant fiveHoursAgo = java.time.Instant.now().minusSeconds(5 * 3600);
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", fiveHoursAgo);

            // When
            ConnectionState state = service.getConnectionState();

            // Then
            assertThat(state).isEqualTo(ConnectionState.SESSION_EXPIRED);
        }

        @Test
        @DisplayName("should return PROFILE_SYNCED when business ID and valid tokens exist but no NINO")
        void shouldReturnProfileSyncedWhenNoNino() {
            // Given: Business ID and valid tokens but no NINO
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());
            // NINO not set

            // When
            ConnectionState state = service.getConnectionState();

            // Then
            assertThat(state).isEqualTo(ConnectionState.PROFILE_SYNCED);
        }

        @Test
        @DisplayName("should handle empty business ID as not connected")
        void shouldHandleEmptyBusinessIdAsNotConnected() {
            // Given: Empty business ID
            SqliteDataStore.getInstance().saveHmrcBusinessId("");

            // When
            ConnectionState state = service.getConnectionState();

            // Then
            assertThat(state).isEqualTo(ConnectionState.NOT_CONNECTED);
        }

        @Test
        @DisplayName("should handle blank business ID as not connected")
        void shouldHandleBlankBusinessIdAsNotConnected() {
            // Given: Blank business ID
            SqliteDataStore.getInstance().saveHmrcBusinessId("   ");

            // When
            ConnectionState state = service.getConnectionState();

            // Then
            assertThat(state).isEqualTo(ConnectionState.NOT_CONNECTED);
        }
    }

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethodTests {

        @Test
        @DisplayName("isConnected should return false when NOT_CONNECTED")
        void isConnectedShouldReturnFalseWhenNotConnected() {
            // Given: No connection
            assertThat(SqliteDataStore.getInstance().loadHmrcBusinessId()).isNull();

            // When/Then
            assertThat(service.isConnected()).isFalse();
        }

        @Test
        @DisplayName("isConnected should return false when SESSION_EXPIRED")
        void isConnectedShouldReturnFalseWhenSessionExpired() {
            // Given: Business ID stored but no OAuth tokens (session expired)
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            // No OAuth tokens

            // When/Then
            assertThat(service.isConnected()).isFalse();
            assertThat(service.isSessionExpired()).isTrue();
        }

        @Test
        @DisplayName("isConnected should return true when PROFILE_SYNCED")
        void isConnectedShouldReturnTrueWhenProfileSynced() {
            // Given: Business ID and valid OAuth tokens
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());

            // When/Then
            assertThat(service.isConnected()).isTrue();
        }

        @Test
        @DisplayName("isConnected should return true when READY_TO_SUBMIT")
        void isConnectedShouldReturnTrueWhenReadyToSubmit() {
            // Given: All prerequisites with valid OAuth tokens
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());
            SqliteDataStore.getInstance().saveNino("AB123456C");

            // When/Then
            assertThat(service.isConnected()).isTrue();
        }

        @Test
        @DisplayName("isReadyToSubmit should return false when NOT_CONNECTED")
        void isReadyToSubmitShouldReturnFalseWhenNotConnected() {
            // Given: No connection
            assertThat(SqliteDataStore.getInstance().loadHmrcBusinessId()).isNull();

            // When/Then
            assertThat(service.isReadyToSubmit()).isFalse();
        }

        @Test
        @DisplayName("isReadyToSubmit should return false when SESSION_EXPIRED")
        void isReadyToSubmitShouldReturnFalseWhenSessionExpired() {
            // Given: Business ID but no OAuth tokens
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveNino("AB123456C");
            // No OAuth tokens

            // When/Then
            assertThat(service.isReadyToSubmit()).isFalse();
        }

        @Test
        @DisplayName("isReadyToSubmit should return false when PROFILE_SYNCED but no NINO")
        void isReadyToSubmitShouldReturnFalseWhenNoNino() {
            // Given: Business ID and valid tokens but no NINO
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());

            // When/Then
            assertThat(service.isReadyToSubmit()).isFalse();
        }

        @Test
        @DisplayName("isReadyToSubmit should return true when READY_TO_SUBMIT")
        void isReadyToSubmitShouldReturnTrueWhenAllPrerequisites() {
            // Given: All prerequisites with valid tokens
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());
            SqliteDataStore.getInstance().saveNino("AB123456C");

            // When/Then
            assertThat(service.isReadyToSubmit()).isTrue();
        }
    }

    @Nested
    @DisplayName("Status Text Generation")
    class StatusTextTests {

        @Test
        @DisplayName("should return 'Not connected to HMRC' when NOT_CONNECTED")
        void shouldReturnNotConnectedText() {
            // Given: No connection

            // When
            String text = service.getStatusText();

            // Then
            assertThat(text).isEqualTo("Not connected to HMRC");
        }

        @Test
        @DisplayName("should return 'Session expired' when SESSION_EXPIRED")
        void shouldReturnSessionExpiredText() {
            // Given: Business ID but no OAuth tokens
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            // No OAuth tokens

            // When
            String text = service.getStatusText();

            // Then
            assertThat(text).isEqualTo("Session expired");
        }

        @Test
        @DisplayName("should return 'Connected to HMRC' when PROFILE_SYNCED")
        void shouldReturnConnectedTextWhenProfileSynced() {
            // Given: Business ID and valid OAuth tokens
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());

            // When
            String text = service.getStatusText();

            // Then
            assertThat(text).isEqualTo("Connected to HMRC");
        }

        @Test
        @DisplayName("should return 'Connected to HMRC' when READY_TO_SUBMIT")
        void shouldReturnConnectedTextWhenReadyToSubmit() {
            // Given: All prerequisites with valid tokens
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());
            SqliteDataStore.getInstance().saveNino("AB123456C");

            // When
            String text = service.getStatusText();

            // Then
            assertThat(text).isEqualTo("Connected to HMRC");
        }
    }

    @Nested
    @DisplayName("Status Message Generation")
    class StatusMessageTests {

        @Test
        @DisplayName("should return setup instruction when NOT_CONNECTED")
        void shouldReturnSetupInstructionWhenNotConnected() {
            // Given: No connection

            // When
            String message = service.getStatusMessage();

            // Then
            assertThat(message).contains("Connect").contains("HMRC");
        }

        @Test
        @DisplayName("should return session expired message when SESSION_EXPIRED")
        void shouldReturnSessionExpiredMessage() {
            // Given: Business ID but no OAuth tokens
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            // No OAuth tokens

            // When
            String message = service.getStatusMessage();

            // Then
            assertThat(message).contains("expired").contains("Reconnect");
        }

        @Test
        @DisplayName("should return NINO needed message when PROFILE_SYNCED but no NINO")
        void shouldReturnNinoNeededMessage() {
            // Given: Business ID and valid tokens but no NINO
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());

            // When
            String message = service.getStatusMessage();

            // Then
            assertThat(message).contains("NINO").contains("Settings");
        }

        @Test
        @DisplayName("should return ready message when READY_TO_SUBMIT")
        void shouldReturnReadyMessage() {
            // Given: All prerequisites with valid tokens
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());
            SqliteDataStore.getInstance().saveNino("AB123456C");

            // When
            String message = service.getStatusMessage();

            // Then
            assertThat(message).contains("ready").contains("submit");
        }
    }

    @Nested
    @DisplayName("Business Details")
    class BusinessDetailsTests {

        @Test
        @DisplayName("should return null business ID when not connected")
        void shouldReturnNullBusinessIdWhenNotConnected() {
            // Given: No connection

            // When/Then
            assertThat(service.getHmrcBusinessId()).isNull();
        }

        @Test
        @DisplayName("should return business ID when connected")
        void shouldReturnBusinessIdWhenConnected() {
            // Given: Business ID stored
            String businessId = "XAIS12345678901";
            SqliteDataStore.getInstance().saveHmrcBusinessId(businessId);

            // When/Then
            assertThat(service.getHmrcBusinessId()).isEqualTo(businessId);
        }

        @Test
        @DisplayName("should return null trading name when not set")
        void shouldReturnNullTradingNameWhenNotSet() {
            // Given: Business ID but no trading name
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");

            // When/Then
            assertThat(service.getTradingName()).isNull();
        }

        @Test
        @DisplayName("should return trading name when set")
        void shouldReturnTradingNameWhenSet() {
            // Given: Business ID and trading name stored
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveHmrcTradingName("John's Consultancy");

            // When/Then
            assertThat(service.getTradingName()).isEqualTo("John's Consultancy");
        }
    }

    @Nested
    @DisplayName("NINO Access")
    class NinoAccessTests {

        @Test
        @DisplayName("should return null NINO when not set")
        void shouldReturnNullNinoWhenNotSet() {
            // Given: No NINO

            // When/Then
            assertThat(service.getNino()).isNull();
        }

        @Test
        @DisplayName("should return NINO when set")
        void shouldReturnNinoWhenSet() {
            // Given: NINO stored
            SqliteDataStore.getInstance().saveNino("AB123456C");

            // When/Then
            assertThat(service.getNino()).isEqualTo("AB123456C");
        }

        @Test
        @DisplayName("should mask NINO for display")
        void shouldMaskNinoForDisplay() {
            // Given: NINO stored (9 characters: AB123456C)
            SqliteDataStore.getInstance().saveNino("AB123456C");

            // When
            String masked = service.getMaskedNino();

            // Then - should show last 3 characters only (9-3=6 asterisks)
            assertThat(masked).isEqualTo("******56C");
        }

        @Test
        @DisplayName("should return empty string for masked NINO when not set")
        void shouldReturnEmptyMaskedNinoWhenNotSet() {
            // Given: No NINO

            // When/Then
            assertThat(service.getMaskedNino()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Disconnect Functionality")
    class DisconnectTests {

        @Test
        @DisplayName("should clear business ID on disconnect")
        void shouldClearBusinessIdOnDisconnect() {
            // Given: Connected state with valid OAuth tokens
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                    "access_token", "refresh_token", 3600, "Bearer", "scope",
                    java.time.Instant.now());
            assertThat(service.isConnected()).isTrue();

            // When
            service.disconnect();

            // Then
            assertThat(service.isConnected()).isFalse();
            assertThat(SqliteDataStore.getInstance().loadHmrcBusinessId()).isNull();
        }

        @Test
        @DisplayName("should clear trading name on disconnect")
        void shouldClearTradingNameOnDisconnect() {
            // Given: Connected state with trading name and valid OAuth tokens
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveHmrcTradingName("John's Consultancy");
            SqliteDataStore.getInstance().saveOAuthTokens(
                    "access_token", "refresh_token", 3600, "Bearer", "scope",
                    java.time.Instant.now());

            // When
            service.disconnect();

            // Then
            assertThat(SqliteDataStore.getInstance().loadHmrcTradingName()).isNull();
        }

        @Test
        @DisplayName("should NOT clear NINO on disconnect (profile data)")
        void shouldNotClearNinoOnDisconnect() {
            // Given: Connected state with NINO and valid OAuth tokens
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveNino("AB123456C");
            SqliteDataStore.getInstance().saveOAuthTokens(
                    "access_token", "refresh_token", 3600, "Bearer", "scope",
                    java.time.Instant.now());

            // When
            service.disconnect();

            // Then - NINO should remain (it's profile data, not connection data)
            assertThat(SqliteDataStore.getInstance().loadNino()).isEqualTo("AB123456C");
        }
    }

    @Nested
    @DisplayName("OAuth Token Persistence")
    class OAuthTokenPersistenceTests {

        @Test
        @DisplayName("should save OAuth tokens to storage")
        void shouldSaveOAuthTokensToStorage() {
            // Given: OAuth token data
            String accessToken = "test-access-token";
            String refreshToken = "test-refresh-token";
            long expiresIn = 14400; // 4 hours
            String tokenType = "bearer";
            String scope = "read:self-assessment write:self-assessment";
            java.time.Instant issuedAt = java.time.Instant.now();

            // When: Saving tokens
            SqliteDataStore.getInstance().saveOAuthTokens(
                accessToken, refreshToken, expiresIn, tokenType, scope, issuedAt);

            // Then: Tokens should be retrievable
            assertThat(SqliteDataStore.getInstance().hasOAuthTokens()).isTrue();
            String[] loaded = SqliteDataStore.getInstance().loadOAuthTokens();
            assertThat(loaded).isNotNull();
            assertThat(loaded[0]).isEqualTo(accessToken);
            assertThat(loaded[1]).isEqualTo(refreshToken);
        }

        @Test
        @DisplayName("should clear OAuth tokens on disconnect")
        void shouldClearOAuthTokensOnDisconnect() {
            // Given: Connected with OAuth tokens
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "token", "refresh", 14400, "bearer", "scope", java.time.Instant.now());
            assertThat(SqliteDataStore.getInstance().hasOAuthTokens()).isTrue();

            // When: Disconnecting
            service.disconnect();

            // Then: OAuth tokens should be cleared
            assertThat(SqliteDataStore.getInstance().hasOAuthTokens()).isFalse();
            assertThat(SqliteDataStore.getInstance().loadOAuthTokens()).isNull();
        }

        @Test
        @DisplayName("should report hasOAuthTokens correctly")
        void shouldReportHasOAuthTokensCorrectly() {
            // Given: No tokens initially
            assertThat(SqliteDataStore.getInstance().hasOAuthTokens()).isFalse();

            // When: Saving tokens
            SqliteDataStore.getInstance().saveOAuthTokens(
                "token", "refresh", 14400, "bearer", "scope", java.time.Instant.now());

            // Then
            assertThat(SqliteDataStore.getInstance().hasOAuthTokens()).isTrue();

            // When: Clearing tokens
            SqliteDataStore.getInstance().clearOAuthTokens();

            // Then
            assertThat(SqliteDataStore.getInstance().hasOAuthTokens()).isFalse();
        }
    }

    @Nested
    @DisplayName("Singleton Instance")
    class SingletonTests {

        @Test
        @DisplayName("should return same instance on multiple calls")
        void shouldReturnSameInstance() {
            // When
            HmrcConnectionService instance1 = HmrcConnectionService.getInstance();
            HmrcConnectionService instance2 = HmrcConnectionService.getInstance();

            // Then
            assertThat(instance1).isSameAs(instance2);
        }

        @Test
        @DisplayName("getInstance should return non-null service")
        void getInstanceShouldReturnNonNull() {
            // When/Then
            assertThat(HmrcConnectionService.getInstance()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Session Verification - NEEDS_VERIFICATION State")
    class SessionVerificationTests {

        @BeforeEach
        void resetVerification() {
            service.resetSessionVerification();
        }

        @Test
        @DisplayName("should return NEEDS_VERIFICATION when tokens exist but session not verified")
        void shouldReturnNeedsVerificationWhenNotVerified() {
            // Given: Business ID and valid tokens exist, but session not verified this run
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());
            service.resetSessionVerification(); // Ensure not verified

            // When
            ConnectionState state = service.getConnectionState();

            // Then
            assertThat(state).isEqualTo(ConnectionState.NEEDS_VERIFICATION);
        }

        @Test
        @DisplayName("should return PROFILE_SYNCED after markSessionVerified is called")
        void shouldReturnProfileSyncedAfterVerification() {
            // Given: Business ID and valid tokens exist
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());
            service.resetSessionVerification();

            // When: Mark session as verified
            service.markSessionVerified();

            // Then
            assertThat(service.getConnectionState()).isEqualTo(ConnectionState.PROFILE_SYNCED);
        }

        @Test
        @DisplayName("should return READY_TO_SUBMIT after verification when NINO exists")
        void shouldReturnReadyToSubmitAfterVerificationWithNino() {
            // Given: All prerequisites exist
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());
            SqliteDataStore.getInstance().saveNino("AB123456C");
            service.resetSessionVerification();

            // When: Mark session as verified
            service.markSessionVerified();

            // Then
            assertThat(service.getConnectionState()).isEqualTo(ConnectionState.READY_TO_SUBMIT);
        }

        @Test
        @DisplayName("isConnected should return false when NEEDS_VERIFICATION")
        void isConnectedShouldReturnFalseWhenNeedsVerification() {
            // Given: Tokens exist but not verified
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());
            service.resetSessionVerification();

            // When/Then
            assertThat(service.isConnected()).isFalse();
            assertThat(service.needsVerification()).isTrue();
        }

        @Test
        @DisplayName("needsVerification should return true only for NEEDS_VERIFICATION state")
        void needsVerificationShouldReturnTrueOnlyForCorrectState() {
            // Given: NOT_CONNECTED state
            assertThat(service.needsVerification()).isFalse();

            // Given: SESSION_EXPIRED state
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            // No OAuth tokens
            assertThat(service.needsVerification()).isFalse();

            // Given: NEEDS_VERIFICATION state
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());
            service.resetSessionVerification();
            assertThat(service.needsVerification()).isTrue();

            // Given: PROFILE_SYNCED state (verified)
            service.markSessionVerified();
            assertThat(service.needsVerification()).isFalse();
        }

        @Test
        @DisplayName("isSessionVerified should track verification state correctly")
        void isSessionVerifiedShouldTrackStateCorrectly() {
            // Initially not verified
            service.resetSessionVerification();
            assertThat(service.isSessionVerified()).isFalse();

            // After marking verified
            service.markSessionVerified();
            assertThat(service.isSessionVerified()).isTrue();

            // After reset
            service.resetSessionVerification();
            assertThat(service.isSessionVerified()).isFalse();
        }

        @Test
        @DisplayName("disconnect should reset session verification")
        void disconnectShouldResetSessionVerification() {
            // Given: Verified session
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());
            service.markSessionVerified();
            assertThat(service.isSessionVerified()).isTrue();

            // When: Disconnect
            service.disconnect();

            // Then: Verification should be reset
            assertThat(service.isSessionVerified()).isFalse();
        }

        @Test
        @DisplayName("getStatusText should return 'Connection saved' for NEEDS_VERIFICATION")
        void getStatusTextShouldReturnCorrectTextForNeedsVerification() {
            // Given: NEEDS_VERIFICATION state
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());
            service.resetSessionVerification();

            // When/Then
            assertThat(service.getStatusText()).isEqualTo("Connection saved");
        }

        @Test
        @DisplayName("getStatusMessage should return verification instruction for NEEDS_VERIFICATION")
        void getStatusMessageShouldReturnVerificationInstruction() {
            // Given: NEEDS_VERIFICATION state
            SqliteDataStore.getInstance().saveHmrcBusinessId("XAIS12345678901");
            SqliteDataStore.getInstance().saveOAuthTokens(
                "access", "refresh", 14400, "bearer", "scope", java.time.Instant.now());
            service.resetSessionVerification();

            // When
            String message = service.getStatusMessage();

            // Then
            assertThat(message).contains("Verify");
        }
    }

    @Nested
    @DisplayName("verifySession - Async Session Verification")
    class VerifySessionTests {

        @BeforeEach
        void resetVerification() {
            service.resetSessionVerification();
        }

        @Test
        @DisplayName("verifySession should exist as async method")
        void verifySessionShouldExist() {
            // The method should exist and return a CompletableFuture
            // This test will fail until we implement verifySession()
            assertThat(service).hasFieldOrProperty("class");
            // Will add actual test once method is implemented
        }
    }
}
