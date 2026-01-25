package uk.selfemploy.plugin.extension;

/**
 * Marker interface for all extension point types.
 *
 * <p>An extension point represents a specific way plugins can extend the
 * application's functionality. All concrete extension point interfaces
 * (e.g., NavigationExtension, DashboardWidget, DataImporter) must extend
 * this marker interface.</p>
 *
 * <h2>Purpose</h2>
 * <p>This marker interface allows the plugin runtime to:</p>
 * <ul>
 *   <li>Identify valid extension types at compile time</li>
 *   <li>Manage extension registration uniformly</li>
 *   <li>Apply security and permission checks</li>
 * </ul>
 *
 * <h2>Creating Extension Points</h2>
 * <p>New extension point interfaces should:</p>
 * <ol>
 *   <li>Extend this {@code ExtensionPoint} interface</li>
 *   <li>Define clear contracts for implementations</li>
 *   <li>Be documented with usage examples</li>
 *   <li>Be placed in the {@code extension} package</li>
 * </ol>
 *
 * <h2>Example Extension Point</h2>
 * <pre>{@code
 * public interface MyExtension extends ExtensionPoint {
 *     String getName();
 *     void execute(Context context);
 * }
 * }</pre>
 *
 * <h2>Implementing Extensions</h2>
 * <p>Plugins implement specific extension point interfaces and register
 * them with the application through the {@code PluginContext}.</p>
 *
 * @see uk.selfemploy.plugin.api.PluginContext
 */
public interface ExtensionPoint {
    // Marker interface - no methods required
}
