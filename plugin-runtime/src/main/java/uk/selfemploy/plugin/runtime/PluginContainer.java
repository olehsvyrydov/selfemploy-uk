package uk.selfemploy.plugin.runtime;

import uk.selfemploy.plugin.api.Plugin;
import uk.selfemploy.plugin.api.PluginContext;
import uk.selfemploy.plugin.api.PluginDescriptor;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Container that holds a plugin instance along with its state and context.
 *
 * <p>The PluginContainer encapsulates all information about a loaded plugin,
 * including its current lifecycle state, runtime context, and any error
 * information if the plugin failed.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. State changes are protected by a read-write lock
 * to allow concurrent reads while ensuring exclusive writes.</p>
 *
 * @see Plugin
 * @see PluginState
 * @see PluginManager
 */
public class PluginContainer {

    private final Plugin plugin;
    private final PluginDescriptor descriptor;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private PluginState state;
    private PluginContext context;
    private Throwable failureCause;

    /**
     * Creates a new PluginContainer for the given plugin.
     *
     * <p>The container is initialized in the DISCOVERED state.</p>
     *
     * @param plugin the plugin instance, must not be null
     * @throws NullPointerException if plugin is null
     */
    public PluginContainer(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin must not be null");
        this.descriptor = Objects.requireNonNull(
            plugin.getDescriptor(),
            "plugin descriptor must not be null"
        );
        this.state = PluginState.DISCOVERED;
    }

    /**
     * Returns the plugin instance.
     *
     * @return the plugin instance, never null
     */
    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * Returns the plugin descriptor.
     *
     * @return the plugin descriptor, never null
     */
    public PluginDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Returns the plugin ID from the descriptor.
     *
     * @return the plugin ID, never null
     */
    public String getId() {
        return descriptor.id();
    }

    /**
     * Returns the current state of the plugin.
     *
     * @return the current state
     */
    public PluginState getState() {
        lock.readLock().lock();
        try {
            return state;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Sets the plugin state.
     *
     * <p>This method validates that the state transition is allowed before
     * applying it. Invalid transitions throw a PluginStateException.</p>
     *
     * @param newState the new state
     * @throws PluginStateException if the transition is not allowed
     */
    public void setState(PluginState newState) {
        lock.writeLock().lock();
        try {
            if (!state.canTransitionTo(newState)) {
                throw new PluginStateException(getId(), state, newState);
            }
            this.state = newState;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Forcefully sets the plugin state without transition validation.
     *
     * <p>This method is intended for error recovery scenarios where
     * normal state transitions don't apply.</p>
     *
     * @param newState the new state
     */
    void forceState(PluginState newState) {
        lock.writeLock().lock();
        try {
            this.state = newState;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the plugin context.
     *
     * @return the context, or null if not yet loaded
     */
    public PluginContext getContext() {
        lock.readLock().lock();
        try {
            return context;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Sets the plugin context.
     *
     * @param context the context to set
     */
    public void setContext(PluginContext context) {
        lock.writeLock().lock();
        try {
            this.context = context;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the cause of failure, if the plugin is in FAILED state.
     *
     * @return the failure cause, or null if not failed
     */
    public Throwable getFailureCause() {
        lock.readLock().lock();
        try {
            return failureCause;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Marks the plugin as failed with the given cause.
     *
     * <p>This method sets the state to FAILED and records the cause
     * of the failure for later inspection.</p>
     *
     * @param cause the cause of the failure
     */
    public void markFailed(Throwable cause) {
        lock.writeLock().lock();
        try {
            this.failureCause = cause;
            this.state = PluginState.FAILED;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Checks if the plugin is in the ENABLED state.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return getState() == PluginState.ENABLED;
    }

    /**
     * Checks if the plugin is in the LOADED state.
     *
     * @return true if loaded
     */
    public boolean isLoaded() {
        PluginState currentState = getState();
        return currentState == PluginState.LOADED
            || currentState == PluginState.ENABLED
            || currentState == PluginState.DISABLED;
    }

    /**
     * Checks if the plugin is in the FAILED state.
     *
     * @return true if failed
     */
    public boolean isFailed() {
        return getState() == PluginState.FAILED;
    }

    @Override
    public String toString() {
        return String.format(
            "PluginContainer[id=%s, name=%s, version=%s, state=%s]",
            getId(),
            descriptor.name(),
            descriptor.version(),
            getState()
        );
    }
}
