package uk.selfemploy.plugin.extension;

import java.util.List;

/**
 * Resolves ordering conflicts between multiple extensions of the same type.
 *
 * <p>When multiple plugins register extensions of the same type (e.g., multiple
 * navigation items or dashboard widgets), the conflict resolver determines
 * the final order in which they appear.</p>
 *
 * <h2>Resolution Process</h2>
 * <ol>
 *   <li>Collect all extensions of a type</li>
 *   <li>Apply the conflict resolution policy</li>
 *   <li>Return the sorted list</li>
 * </ol>
 *
 * <h2>Priority Extraction</h2>
 * <p>The resolver extracts priority values from extensions in the following order:</p>
 * <ol>
 *   <li>If the extension implements {@link Prioritizable}, use {@code getPriority()}</li>
 *   <li>If the extension has a method like {@code getNavigationOrder()}, use that</li>
 *   <li>Otherwise, use the default priority (100)</li>
 * </ol>
 *
 * @see ConflictResolutionPolicy
 * @see Prioritizable
 */
public interface ExtensionConflictResolver {

    /**
     * Resolves the order of extensions according to the given policy.
     *
     * <p>The input list is not modified. A new list is returned with the
     * extensions in the resolved order.</p>
     *
     * @param <T>        the extension type
     * @param extensions the extensions to resolve
     * @param policy     the resolution policy to apply
     * @return a new list with extensions in resolved order
     * @throws NullPointerException if any argument is null
     */
    <T extends ExtensionPoint> List<T> resolve(
        List<T> extensions,
        ConflictResolutionPolicy policy
    );

    /**
     * Resolves the order of extensions using the default policy (PRIORITY_ORDER).
     *
     * @param <T>        the extension type
     * @param extensions the extensions to resolve
     * @return a new list with extensions in resolved order
     * @throws NullPointerException if extensions is null
     */
    default <T extends ExtensionPoint> List<T> resolve(List<T> extensions) {
        return resolve(extensions, ConflictResolutionPolicy.PRIORITY_ORDER);
    }

    /**
     * Extracts the priority value from an extension.
     *
     * <p>This method attempts to get a priority value from the extension by:</p>
     * <ol>
     *   <li>Checking if it implements {@link Prioritizable}</li>
     *   <li>Looking for common order methods (getNavigationOrder, getWidgetOrder, etc.)</li>
     *   <li>Returning the default priority if none found</li>
     * </ol>
     *
     * @param extension the extension to get priority from
     * @return the priority value, or default (100) if not prioritizable
     */
    int getPriorityOf(ExtensionPoint extension);
}
