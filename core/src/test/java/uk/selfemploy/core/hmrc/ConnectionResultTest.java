package uk.selfemploy.core.hmrc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConnectionResult record.
 * Tests factory methods and result states.
 *
 * Sprint 10A: SE-10A-002
 */
@DisplayName("ConnectionResult")
class ConnectionResultTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("connected() should create successful connection result")
        void connectedShouldCreateSuccessfulResult() {
            // When
            ConnectionResult result = ConnectionResult.connected();

            // Then
            assertThat(result.successful()).isTrue();
            assertThat(result.success()).isTrue();
            assertThat(result.message()).isEqualTo("Successfully connected to HMRC");
            assertThat(result.state()).isEqualTo(ConnectionState.CONNECTED);
        }

        @Test
        @DisplayName("refreshed() should create successful refresh result")
        void refreshedShouldCreateSuccessfulResult() {
            // When
            ConnectionResult result = ConnectionResult.refreshed();

            // Then
            assertThat(result.successful()).isTrue();
            assertThat(result.success()).isTrue();
            assertThat(result.message()).isEqualTo("Successfully refreshed HMRC connection");
            assertThat(result.state()).isEqualTo(ConnectionState.CONNECTED);
        }

        @Test
        @DisplayName("failure() should create failed result with error message")
        void failureShouldCreateFailedResult() {
            // Given
            String errorMessage = "Authentication failed";

            // When
            ConnectionResult result = ConnectionResult.failure(errorMessage);

            // Then
            assertThat(result.successful()).isFalse();
            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo(errorMessage);
            assertThat(result.state()).isEqualTo(ConnectionState.ERROR);
        }

        @Test
        @DisplayName("cancelled() should create cancelled result")
        void cancelledShouldCreateCancelledResult() {
            // When
            ConnectionResult result = ConnectionResult.cancelled();

            // Then
            assertThat(result.successful()).isFalse();
            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Connection cancelled by user");
            assertThat(result.state()).isEqualTo(ConnectionState.NOT_CONNECTED);
        }
    }

    @Nested
    @DisplayName("Success Method")
    class SuccessMethod {

        @Test
        @DisplayName("success() should return same value as successful()")
        void successShouldReturnSameAsSuccessful() {
            // Given
            ConnectionResult connected = ConnectionResult.connected();
            ConnectionResult refreshed = ConnectionResult.refreshed();
            ConnectionResult failed = ConnectionResult.failure("error");
            ConnectionResult cancelled = ConnectionResult.cancelled();

            // Then
            assertThat(connected.success()).isEqualTo(connected.successful());
            assertThat(refreshed.success()).isEqualTo(refreshed.successful());
            assertThat(failed.success()).isEqualTo(failed.successful());
            assertThat(cancelled.success()).isEqualTo(cancelled.successful());
        }

        @Test
        @DisplayName("success() should be true only for connected and refreshed")
        void successShouldBeTrueOnlyForSuccessfulResults() {
            assertThat(ConnectionResult.connected().success()).isTrue();
            assertThat(ConnectionResult.refreshed().success()).isTrue();
            assertThat(ConnectionResult.failure("error").success()).isFalse();
            assertThat(ConnectionResult.cancelled().success()).isFalse();
        }
    }

    @Nested
    @DisplayName("Connection State")
    class ConnectionStateTests {

        @Test
        @DisplayName("connected() should have CONNECTED state")
        void connectedShouldHaveConnectedState() {
            assertThat(ConnectionResult.connected().state()).isEqualTo(ConnectionState.CONNECTED);
        }

        @Test
        @DisplayName("refreshed() should have CONNECTED state")
        void refreshedShouldHaveConnectedState() {
            assertThat(ConnectionResult.refreshed().state()).isEqualTo(ConnectionState.CONNECTED);
        }

        @Test
        @DisplayName("failure() should have ERROR state")
        void failureShouldHaveErrorState() {
            assertThat(ConnectionResult.failure("error").state()).isEqualTo(ConnectionState.ERROR);
        }

        @Test
        @DisplayName("cancelled() should have NOT_CONNECTED state")
        void cancelledShouldHaveNotConnectedState() {
            assertThat(ConnectionResult.cancelled().state()).isEqualTo(ConnectionState.NOT_CONNECTED);
        }
    }

    @Nested
    @DisplayName("Record Equality")
    class RecordEquality {

        @Test
        @DisplayName("connected results should be equal")
        void connectedResultsShouldBeEqual() {
            assertThat(ConnectionResult.connected()).isEqualTo(ConnectionResult.connected());
            assertThat(ConnectionResult.connected().hashCode())
                .isEqualTo(ConnectionResult.connected().hashCode());
        }

        @Test
        @DisplayName("refreshed results should be equal")
        void refreshedResultsShouldBeEqual() {
            assertThat(ConnectionResult.refreshed()).isEqualTo(ConnectionResult.refreshed());
        }

        @Test
        @DisplayName("cancelled results should be equal")
        void cancelledResultsShouldBeEqual() {
            assertThat(ConnectionResult.cancelled()).isEqualTo(ConnectionResult.cancelled());
        }

        @Test
        @DisplayName("failure results with same message should be equal")
        void failureResultsWithSameMessageShouldBeEqual() {
            assertThat(ConnectionResult.failure("error")).isEqualTo(ConnectionResult.failure("error"));
        }

        @Test
        @DisplayName("failure results with different messages should not be equal")
        void failureResultsWithDifferentMessagesShouldNotBeEqual() {
            assertThat(ConnectionResult.failure("error1"))
                .isNotEqualTo(ConnectionResult.failure("error2"));
        }

        @Test
        @DisplayName("connected and refreshed should not be equal (different messages)")
        void connectedAndRefreshedShouldNotBeEqual() {
            assertThat(ConnectionResult.connected()).isNotEqualTo(ConnectionResult.refreshed());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("failure with null message should be allowed")
        void failureWithNullMessageShouldBeAllowed() {
            // When
            ConnectionResult result = ConnectionResult.failure(null);

            // Then
            assertThat(result.successful()).isFalse();
            assertThat(result.message()).isNull();
            assertThat(result.state()).isEqualTo(ConnectionState.ERROR);
        }

        @Test
        @DisplayName("failure with empty message should be allowed")
        void failureWithEmptyMessageShouldBeAllowed() {
            // When
            ConnectionResult result = ConnectionResult.failure("");

            // Then
            assertThat(result.message()).isEmpty();
        }

        @Test
        @DisplayName("failure with very long message should be allowed")
        void failureWithVeryLongMessageShouldBeAllowed() {
            // Given
            String longMessage = "Network error: " + "x".repeat(10000);

            // When
            ConnectionResult result = ConnectionResult.failure(longMessage);

            // Then
            assertThat(result.message()).startsWith("Network error: ");
        }
    }

    @Nested
    @DisplayName("Usage Patterns")
    class UsagePatterns {

        @Test
        @DisplayName("can use success() in conditional")
        void canUseSuccessInConditional() {
            // Given
            ConnectionResult result = ConnectionResult.connected();
            String action;

            // When
            if (result.success()) {
                action = "show success";
            } else {
                action = "show error";
            }

            // Then
            assertThat(action).isEqualTo("show success");
        }

        @Test
        @DisplayName("can use in pattern matching")
        void canUseInPatternMatching() {
            // Given
            ConnectionResult result = ConnectionResult.cancelled();

            // When
            String stateDescription = switch (result.state()) {
                case CONNECTED -> "connected";
                case NOT_CONNECTED -> "not connected";
                case ERROR -> "error";
                default -> "other";
            };

            // Then
            assertThat(stateDescription).isEqualTo("not connected");
        }
    }
}
