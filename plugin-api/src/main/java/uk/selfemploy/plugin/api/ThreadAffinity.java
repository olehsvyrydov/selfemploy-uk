package uk.selfemploy.plugin.api;

/**
 * Specifies which thread an event handler should run on.
 *
 * <p>Event handlers can specify their thread affinity when subscribing to events.
 * This allows handlers to safely update UI components or perform background work.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Handler that updates UI - runs on JavaFX Application Thread
 * eventBus.subscribe(
 *     DataChangedEvent.class,
 *     event -> updateUI(event.getData()),
 *     ThreadAffinity.UI_THREAD
 * );
 *
 * // Handler that does background work (default)
 * eventBus.subscribe(
 *     FileImportEvent.class,
 *     event -> processFile(event.getFile()),
 *     ThreadAffinity.BACKGROUND
 * );
 * }</pre>
 *
 * @see PluginEventBus
 */
public enum ThreadAffinity {

    /**
     * Handler runs on the JavaFX Application Thread.
     *
     * <p>Use this when the handler needs to update UI components.
     * The event bus will use {@code Platform.runLater()} to dispatch
     * the event to the JavaFX thread.</p>
     *
     * <p><strong>Warning:</strong> Long-running operations on the UI thread
     * will freeze the application. Only use this for quick UI updates.</p>
     */
    UI_THREAD,

    /**
     * Handler runs on a background executor thread.
     *
     * <p>This is the default affinity. Use this for:
     * <ul>
     *   <li>Computationally intensive operations</li>
     *   <li>I/O operations (file, network)</li>
     *   <li>Any work that doesn't require UI access</li>
     * </ul>
     * </p>
     *
     * <p>If the handler needs to update UI after background work, it should
     * explicitly call {@code Platform.runLater()} for UI operations.</p>
     */
    BACKGROUND
}
