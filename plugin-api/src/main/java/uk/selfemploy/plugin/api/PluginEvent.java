package uk.selfemploy.plugin.api;

import java.time.Instant;
import java.util.Objects;

/**
 * Base class for all plugin events.
 *
 * <p>Events are the primary mechanism for decoupled communication between plugins.
 * Plugins can publish events to notify other plugins of significant occurrences,
 * and subscribe to events to react to changes in the system.</p>
 *
 * <h2>Creating Custom Events</h2>
 * <pre>{@code
 * public class TransactionImportedEvent extends PluginEvent {
 *     private final Transaction transaction;
 *
 *     public TransactionImportedEvent(String sourcePluginId, Transaction transaction) {
 *         super(sourcePluginId);
 *         this.transaction = transaction;
 *     }
 *
 *     public Transaction getTransaction() {
 *         return transaction;
 *     }
 * }
 * }</pre>
 *
 * <h2>Publishing Events</h2>
 * <pre>{@code
 * context.getEventBus().publish(new TransactionImportedEvent(
 *     context.getPluginId(),
 *     importedTransaction
 * ));
 * }</pre>
 *
 * <h2>Subscribing to Events</h2>
 * <pre>{@code
 * Subscription sub = context.getEventBus().subscribe(
 *     TransactionImportedEvent.class,
 *     event -> handleImport(event.getTransaction())
 * );
 *
 * // Later, unsubscribe
 * sub.unsubscribe();
 * }</pre>
 *
 * @see PluginEventBus
 * @see EventHandler
 */
public abstract class PluginEvent {

    private final String sourcePluginId;
    private final Instant timestamp;

    /**
     * Creates a new plugin event.
     *
     * @param sourcePluginId the ID of the plugin that created this event
     * @throws NullPointerException if sourcePluginId is null
     */
    protected PluginEvent(String sourcePluginId) {
        this.sourcePluginId = Objects.requireNonNull(
            sourcePluginId,
            "sourcePluginId must not be null"
        );
        this.timestamp = Instant.now();
    }

    /**
     * Returns the ID of the plugin that created this event.
     *
     * @return the source plugin ID, never null
     */
    public String getSourcePluginId() {
        return sourcePluginId;
    }

    /**
     * Returns the timestamp when this event was created.
     *
     * @return the creation timestamp, never null
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format(
            "%s[source=%s, time=%s]",
            getClass().getSimpleName(),
            sourcePluginId,
            timestamp
        );
    }
}
