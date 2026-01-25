package uk.selfemploy.plugin.runtime;

/**
 * Exception thrown when a plugin operation is invalid for the current state.
 *
 * <p>This exception is thrown when attempting state transitions that are not
 * allowed, such as:</p>
 * <ul>
 *   <li>Enabling a plugin that is not loaded</li>
 *   <li>Disabling a plugin that is not enabled</li>
 *   <li>Loading a plugin that is already loaded</li>
 * </ul>
 *
 * @see PluginState
 * @see PluginManager
 */
public class PluginStateException extends PluginException {

    private final String pluginId;
    private final PluginState currentState;
    private final PluginState targetState;

    /**
     * Constructs a new PluginStateException.
     *
     * @param pluginId     the ID of the plugin
     * @param currentState the current state of the plugin
     * @param targetState  the state transition that was attempted
     */
    public PluginStateException(String pluginId, PluginState currentState, PluginState targetState) {
        super(String.format(
            "Cannot transition plugin '%s' from %s to %s",
            pluginId, currentState, targetState
        ));
        this.pluginId = pluginId;
        this.currentState = currentState;
        this.targetState = targetState;
    }

    /**
     * Returns the ID of the plugin.
     *
     * @return the plugin ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Returns the current state of the plugin.
     *
     * @return the current state
     */
    public PluginState getCurrentState() {
        return currentState;
    }

    /**
     * Returns the target state that was attempted.
     *
     * @return the target state
     */
    public PluginState getTargetState() {
        return targetState;
    }
}
