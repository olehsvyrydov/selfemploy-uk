package uk.selfemploy.plugin.extension;

/**
 * Interface for extensions that support priority-based ordering.
 *
 * <p>Extensions implementing this interface can specify a priority value
 * that determines their position relative to other extensions of the same type.
 * Lower priority values appear first.</p>
 *
 * <h2>Priority Ranges</h2>
 * <ul>
 *   <li><b>0-99:</b> Reserved for built-in (core) extensions</li>
 *   <li><b>100+:</b> Plugin-provided extensions (default is 100)</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public class MyNavExtension implements NavigationExtension, Prioritizable {
 *     @Override
 *     public int getPriority() {
 *         return 150; // Appears after default priority (100)
 *     }
 *     // ... other methods
 * }
 * }</pre>
 *
 * <p>Note: Many extension interfaces (like {@link NavigationExtension} and
 * {@link DashboardWidget}) already provide ordering methods. This interface
 * provides a standardized way to get priority when it's available.</p>
 *
 * @see ConflictResolutionPolicy#PRIORITY_ORDER
 * @see ExtensionConflictResolver
 */
public interface Prioritizable {

    /**
     * Default priority for plugin extensions.
     *
     * <p>Plugin extensions should use this value or higher. Built-in
     * extensions use values below this.</p>
     */
    int DEFAULT_PLUGIN_PRIORITY = 100;

    /**
     * Maximum priority value reserved for built-in extensions.
     *
     * <p>Built-in extensions should use values from 0 to this value.
     * Plugin extensions should use values greater than this.</p>
     */
    int MAX_BUILTIN_PRIORITY = 99;

    /**
     * Returns the priority value for this extension.
     *
     * <p>Lower values indicate higher priority (appear first).
     * The default implementation returns {@link #DEFAULT_PLUGIN_PRIORITY}.</p>
     *
     * @return the priority value
     */
    default int getPriority() {
        return DEFAULT_PLUGIN_PRIORITY;
    }
}
