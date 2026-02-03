package uk.selfemploy.plugin.api;

/**
 * Event bus for decoupled communication between plugins.
 *
 * <p>The Plugin Event Bus implements the publish-subscribe pattern, allowing plugins
 * to communicate without direct dependencies on each other. Publishers emit events
 * and subscribers receive them based on the event type.</p>
 *
 * <h2>Publishing Events</h2>
 * <pre>{@code
 * // Create and publish a custom event
 * TransactionImportedEvent event = new TransactionImportedEvent(
 *     context.getPluginId(),
 *     importedTransaction
 * );
 * context.getEventBus().publish(event);
 * }</pre>
 *
 * <h2>Subscribing to Events</h2>
 * <pre>{@code
 * // Subscribe with default BACKGROUND affinity
 * Subscription sub = context.getEventBus().subscribe(
 *     TransactionImportedEvent.class,
 *     event -> processImport(event.getTransaction())
 * );
 *
 * // Subscribe for UI updates
 * Subscription uiSub = context.getEventBus().subscribe(
 *     DataChangedEvent.class,
 *     event -> updateUI(event.getData()),
 *     ThreadAffinity.UI_THREAD
 * );
 *
 * // Unsubscribe when done
 * sub.unsubscribe();
 * }</pre>
 *
 * <h2>Thread Affinity</h2>
 * <p>Handlers can specify which thread they should run on:</p>
 * <ul>
 *   <li>{@link ThreadAffinity#BACKGROUND} (default) - Runs on a background thread pool</li>
 *   <li>{@link ThreadAffinity#UI_THREAD} - Runs on the JavaFX Application Thread</li>
 * </ul>
 *
 * <h2>Exception Handling</h2>
 * <p>If a handler throws an exception, it is logged but does not affect other handlers.
 * Each handler is invoked independently.</p>
 *
 * <h2>Memory Safety</h2>
 * <p>Handlers are stored using weak references. When a plugin is unloaded, its
 * subscriptions are automatically cleaned up to prevent memory leaks.</p>
 *
 * <h2>Event Hierarchy</h2>
 * <p>Subscriptions are type-specific. Subscribing to a base event type does not
 * receive subclass events. Subscribe to the exact event type you want to receive.</p>
 *
 * @see PluginEvent
 * @see EventHandler
 * @see Subscription
 * @see ThreadAffinity
 */
public interface PluginEventBus {

    /**
     * Subscribes to events of the specified type with thread affinity.
     *
     * <p>The handler will be called for each event of the specified type
     * (or its subclasses) published after this subscription.</p>
     *
     * @param <T>       the event type
     * @param eventType the class of events to subscribe to
     * @param handler   the handler to call when events are published
     * @param affinity  the thread on which to invoke the handler
     * @return a subscription that can be used to unsubscribe
     * @throws NullPointerException if any parameter is null
     */
    <T extends PluginEvent> Subscription subscribe(
        Class<T> eventType,
        EventHandler<T> handler,
        ThreadAffinity affinity
    );

    /**
     * Subscribes to events with default BACKGROUND thread affinity.
     *
     * <p>This is a convenience method equivalent to:
     * {@code subscribe(eventType, handler, ThreadAffinity.BACKGROUND)}</p>
     *
     * @param <T>       the event type
     * @param eventType the class of events to subscribe to
     * @param handler   the handler to call when events are published
     * @return a subscription that can be used to unsubscribe
     * @throws NullPointerException if any parameter is null
     */
    default <T extends PluginEvent> Subscription subscribe(
            Class<T> eventType,
            EventHandler<T> handler) {
        return subscribe(eventType, handler, ThreadAffinity.BACKGROUND);
    }

    /**
     * Publishes an event to all subscribers.
     *
     * <p>The event is delivered asynchronously to all handlers subscribed to
     * the event's type. The order of delivery is not guaranteed.</p>
     *
     * <p>This method returns immediately after queuing the event for delivery.
     * It does not wait for handlers to complete.</p>
     *
     * @param event the event to publish
     * @throws NullPointerException if event is null
     */
    void publish(PluginEvent event);

    /**
     * Unsubscribes all handlers registered by the specified plugin.
     *
     * <p>This method is called automatically when a plugin is unloaded.
     * Plugins should not typically need to call this directly.</p>
     *
     * @param pluginId the ID of the plugin whose handlers should be removed
     */
    void unsubscribeAll(String pluginId);

    /**
     * Returns the number of active subscriptions for an event type.
     *
     * <p>This method is primarily for testing and monitoring.</p>
     *
     * @param eventType the event type to check
     * @return the number of active subscriptions
     */
    int getSubscriptionCount(Class<? extends PluginEvent> eventType);
}
