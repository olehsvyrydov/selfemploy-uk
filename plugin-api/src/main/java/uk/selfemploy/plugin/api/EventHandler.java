package uk.selfemploy.plugin.api;

/**
 * Functional interface for handling plugin events.
 *
 * <p>Event handlers receive events published to the event bus and process them.
 * Handlers are invoked asynchronously on either the UI thread or a background
 * thread, depending on the specified {@link ThreadAffinity}.</p>
 *
 * <h2>Exception Handling</h2>
 * <p>If a handler throws an exception, it will be logged but will not prevent
 * other handlers from receiving the event. Each handler is invoked independently.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>Handlers may be invoked concurrently if multiple events are published
 * simultaneously. Ensure your handler is thread-safe if it modifies shared state.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * EventHandler<TransactionCreatedEvent> handler = event -> {
 *     Transaction tx = event.getTransaction();
 *     log.info("New transaction: {}", tx.getDescription());
 *     updateStatistics(tx);
 * };
 *
 * eventBus.subscribe(TransactionCreatedEvent.class, handler);
 * }</pre>
 *
 * @param <T> the type of event this handler processes
 * @see PluginEventBus
 * @see PluginEvent
 */
@FunctionalInterface
public interface EventHandler<T extends PluginEvent> {

    /**
     * Handles the specified event.
     *
     * <p>This method is called when an event of the subscribed type is published.
     * The method is invoked on the thread specified by the subscription's
     * {@link ThreadAffinity}.</p>
     *
     * @param event the event to handle, never null
     */
    void handle(T event);
}
