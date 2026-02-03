package uk.selfemploy.plugin.runtime;

import uk.selfemploy.plugin.extension.ConflictResolutionPolicy;
import uk.selfemploy.plugin.extension.DashboardWidget;
import uk.selfemploy.plugin.extension.ExtensionConflictResolver;
import uk.selfemploy.plugin.extension.ExtensionPoint;
import uk.selfemploy.plugin.extension.NavigationExtension;
import uk.selfemploy.plugin.extension.Prioritizable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of {@link ExtensionConflictResolver}.
 *
 * <p>This implementation satisfies all architecture conditions from SE-1105:</p>
 * <ul>
 *   <li>COND-1105-A: Default policy is PRIORITY_ORDER</li>
 *   <li>COND-1105-B: Built-in 0-99, plugins 100+ (enforced by documentation)</li>
 *   <li>COND-1105-C: Equal priority uses registration order (stable sort)</li>
 *   <li>COND-1105-D: Resolver is injectable into ExtensionRegistry</li>
 *   <li>COND-1105-E: Navigation groups support custom ordering via getPriorityOf</li>
 * </ul>
 *
 * @see ExtensionConflictResolver
 * @see ConflictResolutionPolicy
 */
public class DefaultExtensionConflictResolver implements ExtensionConflictResolver {

    /**
     * Default priority for extensions that don't specify one.
     */
    private static final int DEFAULT_PRIORITY = Prioritizable.DEFAULT_PLUGIN_PRIORITY;

    @Override
    public <T extends ExtensionPoint> List<T> resolve(
            List<T> extensions,
            ConflictResolutionPolicy policy) {
        Objects.requireNonNull(extensions, "extensions must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        if (extensions.isEmpty()) {
            return List.of();
        }

        if (extensions.size() == 1) {
            return new ArrayList<>(extensions);
        }

        // Create a mutable copy to sort
        List<T> result = new ArrayList<>(extensions);

        switch (policy) {
            case PRIORITY_ORDER -> {
                // COND-1105-C: Stable sort preserves registration order for equal priorities
                result.sort(Comparator.comparingInt(this::getPriorityOf));
            }
            case REGISTRATION_ORDER -> {
                // Already in registration order, no sorting needed
            }
            case ALPHABETICAL -> {
                result.sort(Comparator.comparing(this::getExtensionId));
            }
        }

        return result;
    }

    @Override
    public int getPriorityOf(ExtensionPoint extension) {
        if (extension == null) {
            return DEFAULT_PRIORITY;
        }

        // 1. Check if it implements Prioritizable directly
        if (extension instanceof Prioritizable prioritizable) {
            return prioritizable.getPriority();
        }

        // 2. Check for known extension types with ordering methods
        if (extension instanceof NavigationExtension nav) {
            return nav.getNavigationOrder();
        }

        if (extension instanceof DashboardWidget widget) {
            return widget.getWidgetOrder();
        }

        // 3. Default priority for unknown extension types
        return DEFAULT_PRIORITY;
    }

    /**
     * Gets the identifier for an extension (for alphabetical sorting).
     *
     * @param extension the extension
     * @return the extension ID, or class name if not identifiable
     */
    private String getExtensionId(ExtensionPoint extension) {
        if (extension instanceof NavigationExtension nav) {
            return nav.getNavigationId();
        }

        if (extension instanceof DashboardWidget widget) {
            return widget.getWidgetId();
        }

        // Fall back to class name
        return extension.getClass().getSimpleName();
    }
}
