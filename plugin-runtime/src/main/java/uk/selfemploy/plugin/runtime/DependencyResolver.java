package uk.selfemploy.plugin.runtime;

import uk.selfemploy.plugin.api.PluginDependency;
import uk.selfemploy.plugin.api.PluginDescriptor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves plugin dependencies and determines load order.
 *
 * <p>Uses Kahn's algorithm for topological sorting with O(V+E) complexity.
 * Detects circular dependencies before loading any plugins.</p>
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>Build dependency graph from all discovered plugins</li>
 *   <li>Detect circular dependencies (reject if found)</li>
 *   <li>Topological sort for load order</li>
 *   <li>Return plugins in dependency-first order</li>
 * </ol>
 *
 * @see PluginDependency
 * @see PluginManager
 */
public final class DependencyResolver {

    /**
     * Result of dependency resolution.
     *
     * @param loadOrder   plugins in dependency-first order (dependencies before dependents)
     * @param blocked     plugins that cannot be loaded due to missing/incompatible dependencies
     * @param warnings    warnings for optional dependencies that are missing
     */
    public record ResolutionResult(
        List<String> loadOrder,
        Map<String, String> blocked,
        List<String> warnings
    ) {
        public ResolutionResult {
            loadOrder = List.copyOf(loadOrder);
            blocked = Map.copyOf(blocked);
            warnings = List.copyOf(warnings);
        }

        public boolean hasBlockedPlugins() {
            return !blocked.isEmpty();
        }

        public boolean isPluginBlocked(String pluginId) {
            return blocked.containsKey(pluginId);
        }
    }

    /**
     * Resolves dependencies and determines load order.
     *
     * @param plugins map of plugin ID to descriptor
     * @param dependencies map of plugin ID to its dependencies
     * @return resolution result with load order, blocked plugins, and warnings
     * @throws CircularDependencyException if circular dependencies are detected
     */
    public ResolutionResult resolve(
        Map<String, PluginDescriptor> plugins,
        Map<String, List<PluginDependency>> dependencies
    ) {
        if (plugins == null || plugins.isEmpty()) {
            return new ResolutionResult(List.of(), Map.of(), List.of());
        }

        // Build adjacency list and in-degree count
        Map<String, Set<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, String> blocked = new HashMap<>();
        List<String> warnings = new ArrayList<>();

        // Initialize all plugins with 0 in-degree
        for (String pluginId : plugins.keySet()) {
            graph.put(pluginId, new HashSet<>());
            inDegree.put(pluginId, 0);
        }

        // Build graph edges from dependencies
        for (Map.Entry<String, List<PluginDependency>> entry : dependencies.entrySet()) {
            String dependentId = entry.getKey();
            if (!plugins.containsKey(dependentId)) {
                continue;
            }

            for (PluginDependency dep : entry.getValue()) {
                String dependencyId = dep.pluginId();

                // Check if dependency exists
                if (!plugins.containsKey(dependencyId)) {
                    if (dep.isRequired()) {
                        blocked.put(dependentId, "Missing required dependency: " + dependencyId);
                    } else {
                        warnings.add("Optional dependency missing for " + dependentId + ": " + dependencyId);
                    }
                    continue;
                }

                // Check version compatibility
                PluginDescriptor depDescriptor = plugins.get(dependencyId);
                VersionRange range = new VersionRange(dep.versionRange());
                if (!range.matches(depDescriptor.version())) {
                    if (dep.isRequired()) {
                        blocked.put(dependentId,
                            "Dependency version mismatch for " + dependencyId +
                            ": required " + dep.versionRange() +
                            ", found " + depDescriptor.version());
                    } else {
                        warnings.add("Optional dependency version mismatch for " + dependentId +
                            ": " + dependencyId + " requires " + dep.versionRange() +
                            " but found " + depDescriptor.version());
                    }
                    continue;
                }

                // Add edge: dependency -> dependent (dependency must load first)
                graph.computeIfAbsent(dependencyId, k -> new HashSet<>()).add(dependentId);
                inDegree.merge(dependentId, 1, Integer::sum);
            }
        }

        // Remove blocked plugins from graph
        for (String blockedId : blocked.keySet()) {
            graph.remove(blockedId);
            inDegree.remove(blockedId);
            // Remove edges pointing to blocked plugins
            for (Set<String> edges : graph.values()) {
                edges.remove(blockedId);
            }
        }

        // Kahn's algorithm for topological sort
        List<String> loadOrder = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();

        // Start with plugins that have no dependencies (in-degree = 0)
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            loadOrder.add(current);

            // Reduce in-degree for all dependents
            for (String dependent : graph.getOrDefault(current, Set.of())) {
                int newDegree = inDegree.merge(dependent, -1, Integer::sum);
                if (newDegree == 0) {
                    queue.add(dependent);
                }
            }
        }

        // Check for circular dependencies
        if (loadOrder.size() < inDegree.size()) {
            Set<String> remaining = inDegree.keySet().stream()
                .filter(id -> !loadOrder.contains(id))
                .collect(Collectors.toSet());
            throw new CircularDependencyException(
                "Circular dependency detected among: " + remaining
            );
        }

        return new ResolutionResult(loadOrder, blocked, warnings);
    }

    /**
     * Detects if there are any circular dependencies without performing full resolution.
     *
     * @param plugins map of plugin ID to descriptor
     * @param dependencies map of plugin ID to its dependencies
     * @return list of plugin IDs involved in circular dependencies, or empty if none
     */
    public List<String> detectCycles(
        Map<String, PluginDescriptor> plugins,
        Map<String, List<PluginDependency>> dependencies
    ) {
        try {
            resolve(plugins, dependencies);
            return List.of();
        } catch (CircularDependencyException e) {
            // Extract plugin IDs from exception message
            String msg = e.getMessage();
            int start = msg.indexOf('[');
            int end = msg.indexOf(']');
            if (start >= 0 && end > start) {
                String ids = msg.substring(start + 1, end);
                return Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .toList();
            }
            return List.of();
        }
    }

    /**
     * Exception thrown when circular dependencies are detected.
     */
    public static class CircularDependencyException extends RuntimeException {
        public CircularDependencyException(String message) {
            super(message);
        }
    }
}
