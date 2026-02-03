package uk.selfemploy.plugin.runtime;

/**
 * Listener interface for hot-reload notifications.
 *
 * <p>Implementations of this interface can receive notifications when plugins
 * are being reloaded. This is typically used by the UI layer to display
 * toast notifications to the user.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations must be thread-safe as callbacks may be invoked from
 * the hot-reload watcher thread.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * HotReloadListener listener = new HotReloadListener() {
 *     @Override
 *     public void onReloadStarted(String pluginId) {
 *         Platform.runLater(() ->
 *             ToastService.show("Reloading plugin: " + pluginId));
 *     }
 *
 *     @Override
 *     public void onReloadCompleted(String pluginId, boolean success) {
 *         Platform.runLater(() -> {
 *             if (success) {
 *                 ToastService.success("Plugin reloaded: " + pluginId);
 *             } else {
 *                 ToastService.error("Failed to reload: " + pluginId);
 *             }
 *         });
 *     }
 * };
 * }</pre>
 *
 * @see PluginHotReloader
 */
public interface HotReloadListener {

    /**
     * Called when a plugin reload has started.
     *
     * <p>This is called before the plugin is disabled. The reload sequence is:</p>
     * <ol>
     *   <li>onReloadStarted()</li>
     *   <li>disable plugin</li>
     *   <li>unload plugin</li>
     *   <li>load plugin</li>
     *   <li>enable plugin</li>
     *   <li>onReloadCompleted()</li>
     * </ol>
     *
     * @param pluginId the ID of the plugin being reloaded
     */
    void onReloadStarted(String pluginId);

    /**
     * Called when a plugin reload has completed.
     *
     * @param pluginId the ID of the plugin that was reloaded
     * @param success  true if the reload was successful, false if it failed
     */
    void onReloadCompleted(String pluginId, boolean success);

    /**
     * Called when a plugin reload fails with an exception.
     *
     * <p>This method provides more details about the failure. The default
     * implementation delegates to {@link #onReloadCompleted(String, boolean)}
     * with success=false.</p>
     *
     * @param pluginId the ID of the plugin that failed to reload
     * @param error    the exception that caused the failure
     */
    default void onReloadFailed(String pluginId, Throwable error) {
        onReloadCompleted(pluginId, false);
    }

    /**
     * Returns a no-op listener that ignores all notifications.
     *
     * <p>Useful when no UI notifications are needed.</p>
     *
     * @return a no-op listener instance
     */
    static HotReloadListener noOp() {
        return NoOpListener.INSTANCE;
    }

    /**
     * Singleton no-op listener implementation.
     */
    enum NoOpListener implements HotReloadListener {
        INSTANCE;

        @Override
        public void onReloadStarted(String pluginId) {
            // No-op
        }

        @Override
        public void onReloadCompleted(String pluginId, boolean success) {
            // No-op
        }

        @Override
        public void onReloadFailed(String pluginId, Throwable error) {
            // No-op
        }
    }
}
