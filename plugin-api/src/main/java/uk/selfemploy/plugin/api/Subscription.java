package uk.selfemploy.plugin.api;

/**
 * Represents an active event subscription that can be cancelled.
 *
 * <p>When you subscribe to events, a Subscription is returned that allows you
 * to unsubscribe when you no longer want to receive events. This is important
 * for preventing memory leaks and unnecessary processing.</p>
 *
 * <h2>Manual Unsubscription</h2>
 * <pre>{@code
 * Subscription sub = eventBus.subscribe(MyEvent.class, this::handleEvent);
 *
 * // When no longer needed
 * sub.unsubscribe();
 * }</pre>
 *
 * <h2>Try-With-Resources</h2>
 * <p>Subscription implements {@link AutoCloseable}, so it can be used with
 * try-with-resources for automatic cleanup:</p>
 * <pre>{@code
 * try (Subscription sub = eventBus.subscribe(MyEvent.class, this::handleEvent)) {
 *     // Subscription is active within this block
 *     doSomething();
 * }
 * // Subscription is automatically unsubscribed here
 * }</pre>
 *
 * <h2>Plugin Unload</h2>
 * <p>When a plugin is unloaded, all its subscriptions are automatically
 * cancelled by the event bus. However, it's good practice to explicitly
 * unsubscribe in your plugin's {@code onUnload()} method.</p>
 *
 * @see PluginEventBus
 */
public interface Subscription extends AutoCloseable {

    /**
     * Cancels this subscription.
     *
     * <p>After calling this method, the handler will no longer receive events.
     * Calling this method multiple times has no effect after the first call.</p>
     *
     * <p>This method is thread-safe and can be called from any thread.</p>
     */
    void unsubscribe();

    /**
     * Returns whether this subscription is still active.
     *
     * <p>A subscription is active from creation until {@link #unsubscribe()}
     * is called or the subscribing plugin is unloaded.</p>
     *
     * @return true if still receiving events, false if unsubscribed
     */
    boolean isActive();

    /**
     * Closes this subscription by unsubscribing.
     *
     * <p>This method is equivalent to calling {@link #unsubscribe()} and
     * enables use with try-with-resources.</p>
     */
    @Override
    default void close() {
        unsubscribe();
    }
}
