package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PluginState")
class PluginStateTest {

    @Nested
    @DisplayName("State transitions")
    class StateTransitions {

        @Test
        @DisplayName("DISCOVERED can transition to LOADED")
        void discoveredCanTransitionToLoaded() {
            assertThat(PluginState.DISCOVERED.canTransitionTo(PluginState.LOADED)).isTrue();
        }

        @Test
        @DisplayName("DISCOVERED can transition to FAILED")
        void discoveredCanTransitionToFailed() {
            assertThat(PluginState.DISCOVERED.canTransitionTo(PluginState.FAILED)).isTrue();
        }

        @Test
        @DisplayName("DISCOVERED cannot transition to ENABLED")
        void discoveredCannotTransitionToEnabled() {
            assertThat(PluginState.DISCOVERED.canTransitionTo(PluginState.ENABLED)).isFalse();
        }

        @Test
        @DisplayName("LOADED can transition to ENABLED")
        void loadedCanTransitionToEnabled() {
            assertThat(PluginState.LOADED.canTransitionTo(PluginState.ENABLED)).isTrue();
        }

        @Test
        @DisplayName("LOADED can transition to UNLOADED")
        void loadedCanTransitionToUnloaded() {
            assertThat(PluginState.LOADED.canTransitionTo(PluginState.UNLOADED)).isTrue();
        }

        @Test
        @DisplayName("LOADED can transition to FAILED")
        void loadedCanTransitionToFailed() {
            assertThat(PluginState.LOADED.canTransitionTo(PluginState.FAILED)).isTrue();
        }

        @Test
        @DisplayName("ENABLED can transition to DISABLED")
        void enabledCanTransitionToDisabled() {
            assertThat(PluginState.ENABLED.canTransitionTo(PluginState.DISABLED)).isTrue();
        }

        @Test
        @DisplayName("ENABLED cannot transition directly to UNLOADED")
        void enabledCannotTransitionToUnloaded() {
            assertThat(PluginState.ENABLED.canTransitionTo(PluginState.UNLOADED)).isFalse();
        }

        @Test
        @DisplayName("DISABLED can transition to ENABLED")
        void disabledCanTransitionToEnabled() {
            assertThat(PluginState.DISABLED.canTransitionTo(PluginState.ENABLED)).isTrue();
        }

        @Test
        @DisplayName("DISABLED can transition to UNLOADED")
        void disabledCanTransitionToUnloaded() {
            assertThat(PluginState.DISABLED.canTransitionTo(PluginState.UNLOADED)).isTrue();
        }

        @Test
        @DisplayName("FAILED can transition to UNLOADED")
        void failedCanTransitionToUnloaded() {
            assertThat(PluginState.FAILED.canTransitionTo(PluginState.UNLOADED)).isTrue();
        }

        @Test
        @DisplayName("UNLOADED cannot transition to any state")
        void unloadedCannotTransition() {
            for (PluginState state : PluginState.values()) {
                assertThat(PluginState.UNLOADED.canTransitionTo(state)).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("State properties")
    class StateProperties {

        @Test
        @DisplayName("LOADED is active")
        void loadedIsActive() {
            assertThat(PluginState.LOADED.isActive()).isTrue();
        }

        @Test
        @DisplayName("ENABLED is active")
        void enabledIsActive() {
            assertThat(PluginState.ENABLED.isActive()).isTrue();
        }

        @Test
        @DisplayName("DISABLED is active")
        void disabledIsActive() {
            assertThat(PluginState.DISABLED.isActive()).isTrue();
        }

        @Test
        @DisplayName("DISCOVERED is not active")
        void discoveredIsNotActive() {
            assertThat(PluginState.DISCOVERED.isActive()).isFalse();
        }

        @Test
        @DisplayName("FAILED is not active")
        void failedIsNotActive() {
            assertThat(PluginState.FAILED.isActive()).isFalse();
        }

        @Test
        @DisplayName("UNLOADED is terminal")
        void unloadedIsTerminal() {
            assertThat(PluginState.UNLOADED.isTerminal()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = PluginState.class, names = {"DISCOVERED", "LOADED", "ENABLED", "DISABLED", "FAILED"})
        @DisplayName("Non-terminal states are not terminal")
        void nonTerminalStatesAreNotTerminal(PluginState state) {
            assertThat(state.isTerminal()).isFalse();
        }
    }

    @Test
    @DisplayName("Display names are set correctly")
    void displayNamesAreSet() {
        assertThat(PluginState.DISCOVERED.getDisplayName()).isEqualTo("Discovered");
        assertThat(PluginState.LOADED.getDisplayName()).isEqualTo("Loaded");
        assertThat(PluginState.ENABLED.getDisplayName()).isEqualTo("Enabled");
        assertThat(PluginState.DISABLED.getDisplayName()).isEqualTo("Disabled");
        assertThat(PluginState.UNLOADED.getDisplayName()).isEqualTo("Unloaded");
        assertThat(PluginState.FAILED.getDisplayName()).isEqualTo("Failed");
    }
}
