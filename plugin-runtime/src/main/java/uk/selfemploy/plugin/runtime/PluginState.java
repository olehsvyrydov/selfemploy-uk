package uk.selfemploy.plugin.runtime;

/**
 * Represents the possible states of a plugin throughout its lifecycle.
 *
 * <h2>State Diagram</h2>
 * <pre>
 *              +---> BLOCKED ---+
 *              |                |
 * DISCOVERED --+---> LOADED --> ENABLED
 *              |       |           |
 *              |       v           v
 *              +---> FAILED     DISABLED
 *                      |           |
 *                      v           v
 *                   UNLOADED <-----+
 * </pre>
 *
 * <h2>State Transitions</h2>
 * <ul>
 *   <li>{@code DISCOVERED -> BLOCKED}: Plugin has unresolved dependencies</li>
 *   <li>{@code BLOCKED -> LOADED}: Dependencies resolved, plugin can load</li>
 *   <li>{@code DISCOVERED -> LOADED}: Plugin loaded successfully</li>
 *   <li>{@code DISCOVERED -> FAILED}: Plugin load failed</li>
 *   <li>{@code LOADED -> ENABLED}: Plugin enabled</li>
 *   <li>{@code LOADED -> UNLOADED}: Plugin unloaded without enabling</li>
 *   <li>{@code ENABLED -> DISABLED}: Plugin disabled</li>
 *   <li>{@code DISABLED -> ENABLED}: Plugin re-enabled</li>
 *   <li>{@code DISABLED -> UNLOADED}: Plugin unloaded</li>
 *   <li>{@code FAILED -> UNLOADED}: Cleanup after failure</li>
 * </ul>
 *
 * @see PluginContainer
 * @see PluginManager
 */
public enum PluginState {

    /**
     * Plugin JAR has been discovered but not yet loaded.
     * This is the initial state when a plugin is first found.
     */
    DISCOVERED("Discovered"),

    /**
     * Plugin is blocked from loading due to unresolved dependencies.
     * The plugin will transition to LOADED when dependencies become available.
     */
    BLOCKED("Blocked"),

    /**
     * Plugin has been loaded and onLoad() called successfully.
     * The plugin is ready to be enabled.
     */
    LOADED("Loaded"),

    /**
     * Plugin is active and extensions are registered.
     * This is the normal operating state.
     */
    ENABLED("Enabled"),

    /**
     * Plugin has been disabled but not unloaded.
     * Can be re-enabled without reloading.
     */
    DISABLED("Disabled"),

    /**
     * Plugin has been unloaded and resources released.
     * The plugin instance is no longer usable.
     */
    UNLOADED("Unloaded"),

    /**
     * Plugin failed during loading or lifecycle methods.
     * The plugin cannot be used until the issue is resolved.
     */
    FAILED("Failed");

    private final String displayName;

    PluginState(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable display name for this state.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if a transition from this state to the target state is valid.
     *
     * @param target the target state
     * @return true if the transition is allowed, false otherwise
     */
    public boolean canTransitionTo(PluginState target) {
        return switch (this) {
            case DISCOVERED -> target == LOADED || target == FAILED || target == BLOCKED;
            case BLOCKED -> target == LOADED || target == UNLOADED; // When dependency becomes available
            case LOADED -> target == ENABLED || target == UNLOADED || target == FAILED;
            case ENABLED -> target == DISABLED;
            case DISABLED -> target == ENABLED || target == UNLOADED;
            case FAILED -> target == UNLOADED;
            case UNLOADED -> false; // Terminal state
        };
    }

    /**
     * Checks if the plugin is in an active state (loaded or enabled).
     *
     * @return true if the plugin is active
     */
    public boolean isActive() {
        return this == LOADED || this == ENABLED || this == DISABLED;
    }

    /**
     * Checks if the plugin is in a terminal state.
     *
     * @return true if the plugin is in a terminal state
     */
    public boolean isTerminal() {
        return this == UNLOADED;
    }
}
