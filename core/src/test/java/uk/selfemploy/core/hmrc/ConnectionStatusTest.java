package uk.selfemploy.core.hmrc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConnectionStatus record.
 * Tests factory methods and state handling.
 *
 * Sprint 10A: SE-10A-002
 */
@DisplayName("ConnectionStatus")
class ConnectionStatusTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("connected() should create connected status with expiry time")
        void connectedShouldCreateConnectedStatus() {
            // Given
            Instant expiryTime = Instant.now().plus(1, ChronoUnit.HOURS);

            // When
            ConnectionStatus status = ConnectionStatus.connected(expiryTime);

            // Then
            assertThat(status.state()).isEqualTo(ConnectionState.CONNECTED);
            assertThat(status.message()).isEqualTo("Connected to HMRC");
            assertThat(status.expiryTime()).isEqualTo(expiryTime);
            assertThat(status.canRefresh()).isTrue();
            assertThat(status.isConnected()).isTrue();
        }

        @Test
        @DisplayName("notConnected() should create not connected status")
        void notConnectedShouldCreateNotConnectedStatus() {
            // When
            ConnectionStatus status = ConnectionStatus.notConnected();

            // Then
            assertThat(status.state()).isEqualTo(ConnectionState.NOT_CONNECTED);
            assertThat(status.message()).isEqualTo("Not connected to HMRC. Click 'Connect' to authorize.");
            assertThat(status.expiryTime()).isNull();
            assertThat(status.canRefresh()).isFalse();
            assertThat(status.isConnected()).isFalse();
        }

        @Test
        @DisplayName("expired() should create expired status")
        void expiredShouldCreateExpiredStatus() {
            // When
            ConnectionStatus status = ConnectionStatus.expired();

            // Then
            assertThat(status.state()).isEqualTo(ConnectionState.EXPIRED);
            assertThat(status.message()).isEqualTo("HMRC connection expired. Click 'Refresh' to reconnect.");
            assertThat(status.expiryTime()).isNull();
            assertThat(status.canRefresh()).isTrue();
            assertThat(status.isConnected()).isFalse();
        }

        @Test
        @DisplayName("notConfigured() should create not configured status")
        void notConfiguredShouldCreateNotConfiguredStatus() {
            // When
            ConnectionStatus status = ConnectionStatus.notConfigured();

            // Then
            assertThat(status.state()).isEqualTo(ConnectionState.NOT_CONFIGURED);
            assertThat(status.message()).isEqualTo("HMRC API credentials not configured. Contact support.");
            assertThat(status.expiryTime()).isNull();
            assertThat(status.canRefresh()).isFalse();
            assertThat(status.isConnected()).isFalse();
        }

        @Test
        @DisplayName("error() should create error status with message")
        void errorShouldCreateErrorStatus() {
            // Given
            String errorMessage = "Network timeout";

            // When
            ConnectionStatus status = ConnectionStatus.error(errorMessage);

            // Then
            assertThat(status.state()).isEqualTo(ConnectionState.ERROR);
            assertThat(status.message()).isEqualTo(errorMessage);
            assertThat(status.expiryTime()).isNull();
            assertThat(status.canRefresh()).isFalse();
            assertThat(status.isConnected()).isFalse();
        }

        @Test
        @DisplayName("connecting() should create connecting status")
        void connectingShouldCreateConnectingStatus() {
            // When
            ConnectionStatus status = ConnectionStatus.connecting();

            // Then
            assertThat(status.state()).isEqualTo(ConnectionState.CONNECTING);
            assertThat(status.message()).isEqualTo("Connecting to HMRC...");
            assertThat(status.expiryTime()).isNull();
            assertThat(status.canRefresh()).isFalse();
            assertThat(status.isConnected()).isFalse();
        }
    }

    @Nested
    @DisplayName("Is Connected Method")
    class IsConnectedMethod {

        @Test
        @DisplayName("isConnected() should return true only for CONNECTED state")
        void isConnectedShouldReturnTrueOnlyForConnectedState() {
            assertThat(ConnectionStatus.connected(Instant.now()).isConnected()).isTrue();
            assertThat(ConnectionStatus.notConnected().isConnected()).isFalse();
            assertThat(ConnectionStatus.expired().isConnected()).isFalse();
            assertThat(ConnectionStatus.notConfigured().isConnected()).isFalse();
            assertThat(ConnectionStatus.error("error").isConnected()).isFalse();
            assertThat(ConnectionStatus.connecting().isConnected()).isFalse();
        }
    }

    @Nested
    @DisplayName("Can Refresh Flag")
    class CanRefreshFlag {

        @Test
        @DisplayName("canRefresh should be true for connected and expired states")
        void canRefreshShouldBeTrueForConnectedAndExpired() {
            assertThat(ConnectionStatus.connected(Instant.now()).canRefresh()).isTrue();
            assertThat(ConnectionStatus.expired().canRefresh()).isTrue();
        }

        @Test
        @DisplayName("canRefresh should be false for other states")
        void canRefreshShouldBeFalseForOtherStates() {
            assertThat(ConnectionStatus.notConnected().canRefresh()).isFalse();
            assertThat(ConnectionStatus.notConfigured().canRefresh()).isFalse();
            assertThat(ConnectionStatus.error("error").canRefresh()).isFalse();
            assertThat(ConnectionStatus.connecting().canRefresh()).isFalse();
        }
    }

    @Nested
    @DisplayName("Expiry Time")
    class ExpiryTime {

        @Test
        @DisplayName("only connected status should have expiry time")
        void onlyConnectedShouldHaveExpiryTime() {
            Instant expiry = Instant.now().plus(1, ChronoUnit.HOURS);

            assertThat(ConnectionStatus.connected(expiry).expiryTime()).isNotNull();
            assertThat(ConnectionStatus.notConnected().expiryTime()).isNull();
            assertThat(ConnectionStatus.expired().expiryTime()).isNull();
            assertThat(ConnectionStatus.notConfigured().expiryTime()).isNull();
            assertThat(ConnectionStatus.error("error").expiryTime()).isNull();
            assertThat(ConnectionStatus.connecting().expiryTime()).isNull();
        }

        @Test
        @DisplayName("connected with past expiry time should still be valid")
        void connectedWithPastExpiryShouldStillBeValid() {
            // Given
            Instant pastExpiry = Instant.now().minus(1, ChronoUnit.HOURS);

            // When
            ConnectionStatus status = ConnectionStatus.connected(pastExpiry);

            // Then - The record itself doesn't validate, that's done elsewhere
            assertThat(status.expiryTime()).isEqualTo(pastExpiry);
            assertThat(status.isConnected()).isTrue();
        }

        @Test
        @DisplayName("connected with null expiry time should be allowed")
        void connectedWithNullExpiryShouldBeAllowed() {
            // When
            ConnectionStatus status = ConnectionStatus.connected(null);

            // Then
            assertThat(status.expiryTime()).isNull();
            assertThat(status.isConnected()).isTrue();
        }
    }

    @Nested
    @DisplayName("Record Equality")
    class RecordEquality {

        @Test
        @DisplayName("equal statuses should be equal")
        void equalStatusesShouldBeEqual() {
            // Given
            Instant expiry = Instant.parse("2024-01-15T10:00:00Z");
            ConnectionStatus status1 = ConnectionStatus.connected(expiry);
            ConnectionStatus status2 = ConnectionStatus.connected(expiry);

            // Then
            assertThat(status1).isEqualTo(status2);
            assertThat(status1.hashCode()).isEqualTo(status2.hashCode());
        }

        @Test
        @DisplayName("notConnected statuses should be equal")
        void notConnectedStatusesShouldBeEqual() {
            assertThat(ConnectionStatus.notConnected()).isEqualTo(ConnectionStatus.notConnected());
        }

        @Test
        @DisplayName("different states should not be equal")
        void differentStatesShouldNotBeEqual() {
            assertThat(ConnectionStatus.notConnected()).isNotEqualTo(ConnectionStatus.expired());
        }

        @Test
        @DisplayName("error statuses with different messages should not be equal")
        void errorStatusesWithDifferentMessagesShouldNotBeEqual() {
            assertThat(ConnectionStatus.error("error1")).isNotEqualTo(ConnectionStatus.error("error2"));
        }

        @Test
        @DisplayName("connected with different expiry times should not be equal")
        void connectedWithDifferentExpiryTimesShouldNotBeEqual() {
            // Given
            ConnectionStatus status1 = ConnectionStatus.connected(Instant.parse("2024-01-15T10:00:00Z"));
            ConnectionStatus status2 = ConnectionStatus.connected(Instant.parse("2024-01-15T11:00:00Z"));

            // Then
            assertThat(status1).isNotEqualTo(status2);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("error with null message should be allowed")
        void errorWithNullMessageShouldBeAllowed() {
            // When
            ConnectionStatus status = ConnectionStatus.error(null);

            // Then
            assertThat(status.state()).isEqualTo(ConnectionState.ERROR);
            assertThat(status.message()).isNull();
        }

        @Test
        @DisplayName("error with empty message should be allowed")
        void errorWithEmptyMessageShouldBeAllowed() {
            // When
            ConnectionStatus status = ConnectionStatus.error("");

            // Then
            assertThat(status.message()).isEmpty();
        }

        @Test
        @DisplayName("error with very long message should be allowed")
        void errorWithVeryLongMessageShouldBeAllowed() {
            // Given
            String longMessage = "x".repeat(10000);

            // When
            ConnectionStatus status = ConnectionStatus.error(longMessage);

            // Then
            assertThat(status.message()).hasSize(10000);
        }
    }
}
