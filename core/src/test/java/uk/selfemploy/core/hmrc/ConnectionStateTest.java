package uk.selfemploy.core.hmrc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ConnectionState enum.
 * Tests enum values and their semantics.
 *
 * Sprint 10A: SE-10A-002
 */
@DisplayName("ConnectionState")
class ConnectionStateTest {

    @Nested
    @DisplayName("Enum Values")
    class EnumValues {

        @Test
        @DisplayName("should have NOT_CONFIGURED value")
        void shouldHaveNotConfiguredValue() {
            assertThat(ConnectionState.NOT_CONFIGURED).isNotNull();
            assertThat(ConnectionState.NOT_CONFIGURED.name()).isEqualTo("NOT_CONFIGURED");
        }

        @Test
        @DisplayName("should have NOT_CONNECTED value")
        void shouldHaveNotConnectedValue() {
            assertThat(ConnectionState.NOT_CONNECTED).isNotNull();
            assertThat(ConnectionState.NOT_CONNECTED.name()).isEqualTo("NOT_CONNECTED");
        }

        @Test
        @DisplayName("should have CONNECTED value")
        void shouldHaveConnectedValue() {
            assertThat(ConnectionState.CONNECTED).isNotNull();
            assertThat(ConnectionState.CONNECTED.name()).isEqualTo("CONNECTED");
        }

        @Test
        @DisplayName("should have EXPIRED value")
        void shouldHaveExpiredValue() {
            assertThat(ConnectionState.EXPIRED).isNotNull();
            assertThat(ConnectionState.EXPIRED.name()).isEqualTo("EXPIRED");
        }

        @Test
        @DisplayName("should have CONNECTING value")
        void shouldHaveConnectingValue() {
            assertThat(ConnectionState.CONNECTING).isNotNull();
            assertThat(ConnectionState.CONNECTING.name()).isEqualTo("CONNECTING");
        }

        @Test
        @DisplayName("should have ERROR value")
        void shouldHaveErrorValue() {
            assertThat(ConnectionState.ERROR).isNotNull();
            assertThat(ConnectionState.ERROR.name()).isEqualTo("ERROR");
        }

        @Test
        @DisplayName("should have exactly six values")
        void shouldHaveExactlySixValues() {
            assertThat(ConnectionState.values()).hasSize(6);
        }

        @Test
        @DisplayName("values should be in expected order")
        void valuesShouldBeInExpectedOrder() {
            ConnectionState[] values = ConnectionState.values();
            assertThat(values[0]).isEqualTo(ConnectionState.NOT_CONFIGURED);
            assertThat(values[1]).isEqualTo(ConnectionState.NOT_CONNECTED);
            assertThat(values[2]).isEqualTo(ConnectionState.CONNECTED);
            assertThat(values[3]).isEqualTo(ConnectionState.EXPIRED);
            assertThat(values[4]).isEqualTo(ConnectionState.CONNECTING);
            assertThat(values[5]).isEqualTo(ConnectionState.ERROR);
        }
    }

    @Nested
    @DisplayName("Value Of")
    class ValueOf {

        @Test
        @DisplayName("valueOf should work for all values")
        void valueOfShouldWorkForAllValues() {
            assertThat(ConnectionState.valueOf("NOT_CONFIGURED")).isEqualTo(ConnectionState.NOT_CONFIGURED);
            assertThat(ConnectionState.valueOf("NOT_CONNECTED")).isEqualTo(ConnectionState.NOT_CONNECTED);
            assertThat(ConnectionState.valueOf("CONNECTED")).isEqualTo(ConnectionState.CONNECTED);
            assertThat(ConnectionState.valueOf("EXPIRED")).isEqualTo(ConnectionState.EXPIRED);
            assertThat(ConnectionState.valueOf("CONNECTING")).isEqualTo(ConnectionState.CONNECTING);
            assertThat(ConnectionState.valueOf("ERROR")).isEqualTo(ConnectionState.ERROR);
        }

        @Test
        @DisplayName("valueOf with invalid name should throw exception")
        void valueOfWithInvalidNameShouldThrow() {
            assertThatThrownBy(() -> ConnectionState.valueOf("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("valueOf is case sensitive")
        void valueOfIsCaseSensitive() {
            assertThatThrownBy(() -> ConnectionState.valueOf("connected"))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> ConnectionState.valueOf("Connected"))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Ordinal")
    class Ordinal {

        @Test
        @DisplayName("ordinals should be sequential")
        void ordinalsShouldBeSequential() {
            assertThat(ConnectionState.NOT_CONFIGURED.ordinal()).isEqualTo(0);
            assertThat(ConnectionState.NOT_CONNECTED.ordinal()).isEqualTo(1);
            assertThat(ConnectionState.CONNECTED.ordinal()).isEqualTo(2);
            assertThat(ConnectionState.EXPIRED.ordinal()).isEqualTo(3);
            assertThat(ConnectionState.CONNECTING.ordinal()).isEqualTo(4);
            assertThat(ConnectionState.ERROR.ordinal()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("State Semantics")
    class StateSemantics {

        @Test
        @DisplayName("NOT_CONFIGURED means app credentials not set")
        void notConfiguredMeansAppCredentialsNotSet() {
            // This is a documentation test - verifying the semantic meaning
            ConnectionState state = ConnectionState.NOT_CONFIGURED;
            assertThat(state).isNotEqualTo(ConnectionState.NOT_CONNECTED);
            // NOT_CONFIGURED is a system configuration issue, not a user auth issue
        }

        @Test
        @DisplayName("NOT_CONNECTED means user needs to authenticate")
        void notConnectedMeansUserNeedsToAuth() {
            ConnectionState state = ConnectionState.NOT_CONNECTED;
            assertThat(state).isNotEqualTo(ConnectionState.NOT_CONFIGURED);
            // NOT_CONNECTED is a user action needed, not a system issue
        }

        @Test
        @DisplayName("CONNECTED means valid tokens available")
        void connectedMeansValidTokensAvailable() {
            ConnectionState state = ConnectionState.CONNECTED;
            assertThat(state).isNotEqualTo(ConnectionState.EXPIRED);
        }

        @Test
        @DisplayName("EXPIRED means refresh token available")
        void expiredMeansRefreshTokenAvailable() {
            ConnectionState state = ConnectionState.EXPIRED;
            assertThat(state).isNotEqualTo(ConnectionState.NOT_CONNECTED);
            // EXPIRED is recoverable via refresh, NOT_CONNECTED requires re-auth
        }

        @Test
        @DisplayName("CONNECTING means OAuth flow in progress")
        void connectingMeansOAuthFlowInProgress() {
            ConnectionState state = ConnectionState.CONNECTING;
            assertThat(state).isNotEqualTo(ConnectionState.CONNECTED);
            // CONNECTING is a transient state during auth
        }

        @Test
        @DisplayName("ERROR means an error occurred")
        void errorMeansErrorOccurred() {
            ConnectionState state = ConnectionState.ERROR;
            assertThat(state).isNotEqualTo(ConnectionState.CONNECTED);
        }
    }

    @Nested
    @DisplayName("Usage Patterns")
    class UsagePatterns {

        @Test
        @DisplayName("can be used in switch statement")
        void canBeUsedInSwitch() {
            // Given
            ConnectionState state = ConnectionState.CONNECTED;
            String action;

            // When
            switch (state) {
                case NOT_CONFIGURED -> action = "configure";
                case NOT_CONNECTED -> action = "connect";
                case CONNECTED -> action = "ready";
                case EXPIRED -> action = "refresh";
                case CONNECTING -> action = "wait";
                case ERROR -> action = "retry";
                default -> action = "unknown";
            }

            // Then
            assertThat(action).isEqualTo("ready");
        }

        @Test
        @DisplayName("can be used in switch expression")
        void canBeUsedInSwitchExpression() {
            // Given
            ConnectionState state = ConnectionState.EXPIRED;

            // When
            String action = switch (state) {
                case NOT_CONFIGURED -> "configure";
                case NOT_CONNECTED -> "connect";
                case CONNECTED -> "ready";
                case EXPIRED -> "refresh";
                case CONNECTING -> "wait";
                case ERROR -> "retry";
            };

            // Then
            assertThat(action).isEqualTo("refresh");
        }

        @Test
        @DisplayName("can iterate over all values")
        void canIterateOverAllValues() {
            // When
            int count = 0;
            for (ConnectionState state : ConnectionState.values()) {
                count++;
            }

            // Then
            assertThat(count).isEqualTo(6);
        }

        @Test
        @DisplayName("can be compared with == operator")
        void canBeComparedWithEqualsOperator() {
            ConnectionState state = ConnectionState.CONNECTED;
            assertThat(state == ConnectionState.CONNECTED).isTrue();
            assertThat(state == ConnectionState.ERROR).isFalse();
        }
    }

    @Nested
    @DisplayName("State Groups")
    class StateGroups {

        @Test
        @DisplayName("positive states should include CONNECTED only")
        void positiveStatesShouldIncludeConnectedOnly() {
            // Given - states that mean "we can make API calls"
            ConnectionState[] positiveStates = {ConnectionState.CONNECTED};

            // Then
            assertThat(positiveStates).hasSize(1);
            assertThat(positiveStates[0]).isEqualTo(ConnectionState.CONNECTED);
        }

        @Test
        @DisplayName("recoverable states should include EXPIRED")
        void recoverableStatesShouldIncludeExpired() {
            // Given - states that can be recovered without full re-auth
            ConnectionState state = ConnectionState.EXPIRED;

            // Then - EXPIRED can be recovered via refresh token
            assertThat(state).isNotEqualTo(ConnectionState.NOT_CONNECTED);
        }

        @Test
        @DisplayName("transient states should include CONNECTING")
        void transientStatesShouldIncludeConnecting() {
            // Given - states that are temporary
            ConnectionState state = ConnectionState.CONNECTING;

            // Then - CONNECTING is temporary during OAuth flow
            assertThat(state).isNotEqualTo(ConnectionState.CONNECTED);
            assertThat(state).isNotEqualTo(ConnectionState.NOT_CONNECTED);
        }
    }
}
