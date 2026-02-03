package uk.selfemploy.plugin.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.plugin.api.EventHandler;
import uk.selfemploy.plugin.api.PluginEvent;
import uk.selfemploy.plugin.api.PluginEventBus;
import uk.selfemploy.plugin.api.Subscription;
import uk.selfemploy.plugin.api.ThreadAffinity;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default thread-safe implementation of {@link PluginEventBus}.
 *
 * <p>This implementation satisfies all architecture conditions from ADR-012:</p>
 * <ul>
 *   <li>COND-1101-A: WeakReference handlers for memory safety</li>
 *   <li>COND-1101-B: Async dispatch via ExecutorService</li>
 *   <li>COND-1101-C: Exception isolation between handlers</li>
 *   <li>COND-1101-D: Auto-unsubscribe tracked by pluginId</li>
 *   <li>COND-1101-E: Thread-safe with ConcurrentHashMap</li>
 *   <li>COND-1101-F: ThreadAffinity (UI_THREAD, BACKGROUND)</li>
 * </ul>
 *
 * @see PluginEventBus
 */
public class DefaultPluginEventBus implements PluginEventBus, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultPluginEventBus.class);

    /**
     * Map of event type to list of subscriptions.
     */
    private final Map<Class<? extends PluginEvent>, List<SubscriptionEntry<?>>> subscriptions;

    /**
     * Map of plugin ID to set of its subscriptions (for bulk unsubscribe).
     */
    private final Map<String, Set<SubscriptionEntry<?>>> pluginSubscriptions;

    /**
     * Executor service for background async event dispatch.
     */
    private final ExecutorService backgroundExecutor;

    /**
     * Executor for UI thread dispatch (e.g., Platform::runLater in JavaFX).
     */
    private final Executor uiThreadExecutor;

    /**
     * Flag indicating if the event bus has been shut down.
     */
    private final AtomicBoolean shutdown;

    /**
     * Creates a new DefaultPluginEventBus with a cached thread pool.
     *
     * <p>UI thread handlers will run on the background executor since no
     * UI executor is specified. Use {@link #DefaultPluginEventBus(ExecutorService, Executor)}
     * to specify a UI thread executor.</p>
     */
    public DefaultPluginEventBus() {
        this(createDefaultExecutor(), null);
    }

    /**
     * Creates a new DefaultPluginEventBus with the specified background executor.
     *
     * <p>This constructor is primarily for testing. UI thread handlers will
     * run on the background executor.</p>
     *
     * @param backgroundExecutor the executor service for async dispatch
     */
    public DefaultPluginEventBus(ExecutorService backgroundExecutor) {
        this(backgroundExecutor, null);
    }

    /**
     * Creates a new DefaultPluginEventBus with separate background and UI executors.
     *
     * <p>For JavaFX applications, pass {@code Platform::runLater} as the uiThreadExecutor.</p>
     *
     * @param backgroundExecutor the executor service for background dispatch
     * @param uiThreadExecutor   the executor for UI thread dispatch, or null to use background
     */
    public DefaultPluginEventBus(ExecutorService backgroundExecutor, Executor uiThreadExecutor) {
        this.subscriptions = new ConcurrentHashMap<>();
        this.pluginSubscriptions = new ConcurrentHashMap<>();
        this.backgroundExecutor = Objects.requireNonNull(backgroundExecutor, "backgroundExecutor must not be null");
        // If no UI executor provided, use background executor
        this.uiThreadExecutor = uiThreadExecutor != null ? uiThreadExecutor : backgroundExecutor;
        this.shutdown = new AtomicBoolean(false);
    }

    private static ExecutorService createDefaultExecutor() {
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "plugin-event-bus");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public <T extends PluginEvent> Subscription subscribe(
            Class<T> eventType,
            EventHandler<T> handler,
            ThreadAffinity affinity) {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        Objects.requireNonNull(affinity, "affinity must not be null");

        if (shutdown.get()) {
            throw new IllegalStateException("Event bus has been shut down");
        }

        SubscriptionEntry<T> entry = new SubscriptionEntry<>(
            eventType,
            handler,
            affinity,
            this
        );

        // Add to event type subscriptions
        subscriptions.computeIfAbsent(
            eventType,
            k -> new CopyOnWriteArrayList<>()
        ).add(entry);

        LOG.debug("Subscribed handler to {} (affinity: {})", eventType.getSimpleName(), affinity);

        return entry;
    }

    /**
     * Subscribes with plugin tracking for automatic cleanup.
     *
     * @param <T>       the event type
     * @param eventType the class of events to subscribe to
     * @param handler   the handler to call when events are published
     * @param affinity  the thread on which to invoke the handler
     * @param pluginId  the plugin ID for tracking
     * @return a subscription that can be used to unsubscribe
     */
    public <T extends PluginEvent> Subscription subscribe(
            Class<T> eventType,
            EventHandler<T> handler,
            ThreadAffinity affinity,
            String pluginId) {
        Objects.requireNonNull(pluginId, "pluginId must not be null");

        SubscriptionEntry<T> entry = (SubscriptionEntry<T>) subscribe(eventType, handler, affinity);
        entry.setPluginId(pluginId);

        // Track by plugin for bulk unsubscribe
        pluginSubscriptions.computeIfAbsent(
            pluginId,
            k -> ConcurrentHashMap.newKeySet()
        ).add(entry);

        return entry;
    }

    @Override
    public void publish(PluginEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        if (shutdown.get()) {
            LOG.warn("Attempted to publish event after shutdown: {}", event);
            return;
        }

        Class<?> eventType = event.getClass();
        List<SubscriptionEntry<?>> handlers = subscriptions.get(eventType);

        if (handlers == null || handlers.isEmpty()) {
            LOG.trace("No handlers for event type: {}", eventType.getSimpleName());
            return;
        }

        LOG.debug("Publishing {} to {} handler(s)", eventType.getSimpleName(), handlers.size());

        // Clean up dead references and dispatch to live handlers
        Iterator<SubscriptionEntry<?>> iterator = handlers.iterator();
        while (iterator.hasNext()) {
            SubscriptionEntry<?> entry = iterator.next();

            if (!entry.isActive()) {
                // Handler was garbage collected or unsubscribed
                continue;
            }

            dispatchToHandler(entry, event);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends PluginEvent> void dispatchToHandler(
            SubscriptionEntry<?> entry,
            PluginEvent event) {

        SubscriptionEntry<T> typedEntry = (SubscriptionEntry<T>) entry;
        EventHandler<T> handler = typedEntry.getHandler();

        if (handler == null) {
            // Handler was garbage collected
            entry.unsubscribe();
            return;
        }

        Runnable task = () -> {
            try {
                handler.handle((T) event);
            } catch (Exception e) {
                // COND-1101-C: Exception in one handler doesn't affect others
                LOG.error(
                    "Exception in event handler for {}: {}",
                    event.getClass().getSimpleName(),
                    e.getMessage(),
                    e
                );
            }
        };

        // COND-1101-F: Dispatch based on thread affinity
        if (entry.getAffinity() == ThreadAffinity.UI_THREAD) {
            uiThreadExecutor.execute(task);
        } else {
            // COND-1101-B: Async dispatch via ExecutorService
            backgroundExecutor.execute(task);
        }
    }

    @Override
    public void unsubscribeAll(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            return;
        }

        Set<SubscriptionEntry<?>> entries = pluginSubscriptions.remove(pluginId);
        if (entries == null || entries.isEmpty()) {
            return;
        }

        LOG.debug("Unsubscribing {} handlers for plugin: {}", entries.size(), pluginId);

        for (SubscriptionEntry<?> entry : entries) {
            entry.unsubscribe();
        }
    }

    @Override
    public int getSubscriptionCount(Class<? extends PluginEvent> eventType) {
        List<SubscriptionEntry<?>> handlers = subscriptions.get(eventType);
        if (handlers == null) {
            return 0;
        }

        // Count only active subscriptions
        return (int) handlers.stream()
            .filter(SubscriptionEntry::isActive)
            .count();
    }

    /**
     * Removes a subscription from tracking.
     */
    void removeSubscription(SubscriptionEntry<?> entry) {
        Class<?> eventType = entry.getEventType();
        List<SubscriptionEntry<?>> handlers = subscriptions.get(eventType);
        if (handlers != null) {
            handlers.remove(entry);
        }

        String pluginId = entry.getPluginId();
        if (pluginId != null) {
            Set<SubscriptionEntry<?>> pluginEntries = pluginSubscriptions.get(pluginId);
            if (pluginEntries != null) {
                pluginEntries.remove(entry);
            }
        }
    }

    /**
     * Shuts down the event bus and releases resources.
     *
     * <p>After shutdown, no new subscriptions or publications will be accepted.</p>
     */
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            LOG.info("Shutting down event bus");
            backgroundExecutor.shutdown();
            subscriptions.clear();
            pluginSubscriptions.clear();
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    /**
     * Returns the total number of subscriptions across all event types.
     *
     * <p>This method is primarily for testing.</p>
     *
     * @return the total subscription count
     */
    public int getTotalSubscriptionCount() {
        return subscriptions.values().stream()
            .mapToInt(list -> (int) list.stream().filter(SubscriptionEntry::isActive).count())
            .sum();
    }

    /**
     * Internal subscription entry that tracks handler with weak reference.
     */
    private static class SubscriptionEntry<T extends PluginEvent> implements Subscription {

        private final Class<T> eventType;
        private final WeakReference<EventHandler<T>> handlerRef;
        private final ThreadAffinity affinity;
        private final DefaultPluginEventBus eventBus;
        private final AtomicBoolean active;
        private volatile String pluginId;

        SubscriptionEntry(
                Class<T> eventType,
                EventHandler<T> handler,
                ThreadAffinity affinity,
                DefaultPluginEventBus eventBus) {
            this.eventType = eventType;
            // COND-1101-A: WeakReference to prevent memory leaks
            this.handlerRef = new WeakReference<>(handler);
            this.affinity = affinity;
            this.eventBus = eventBus;
            this.active = new AtomicBoolean(true);
        }

        @Override
        public void unsubscribe() {
            if (active.compareAndSet(true, false)) {
                eventBus.removeSubscription(this);
                handlerRef.clear();
                LOG.trace("Unsubscribed from {}", eventType.getSimpleName());
            }
        }

        @Override
        public boolean isActive() {
            return active.get() && handlerRef.get() != null;
        }

        EventHandler<T> getHandler() {
            return handlerRef.get();
        }

        Class<T> getEventType() {
            return eventType;
        }

        ThreadAffinity getAffinity() {
            return affinity;
        }

        String getPluginId() {
            return pluginId;
        }

        void setPluginId(String pluginId) {
            this.pluginId = pluginId;
        }
    }
}
