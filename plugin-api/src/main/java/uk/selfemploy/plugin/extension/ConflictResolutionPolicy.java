package uk.selfemploy.plugin.extension;

/**
 * Policy for resolving conflicts when multiple extensions compete for the same position.
 *
 * <p>When multiple plugins register extensions of the same type, the conflict resolver
 * uses one of these policies to determine the final ordering.</p>
 *
 * @see ExtensionConflictResolver
 */
public enum ConflictResolutionPolicy {

    /**
     * Order extensions by priority value (lowest first).
     *
     * <p>This is the default policy. Extensions with lower priority values appear
     * before those with higher values. When priorities are equal, registration
     * order is preserved (stable sort).</p>
     *
     * <p>Priority ranges:</p>
     * <ul>
     *   <li>0-99: Reserved for built-in (core) extensions</li>
     *   <li>100+: Plugin extensions (default priority is 100)</li>
     * </ul>
     */
    PRIORITY_ORDER,

    /**
     * Preserve the order in which extensions were registered.
     *
     * <p>Extensions appear in the order they were registered, ignoring any
     * priority values. This is useful when the application wants explicit
     * control over ordering.</p>
     */
    REGISTRATION_ORDER,

    /**
     * Order extensions alphabetically by their ID.
     *
     * <p>Extensions are sorted by their unique identifier (e.g., navigation ID,
     * widget ID). This provides a predictable ordering independent of
     * registration timing or priority values.</p>
     */
    ALPHABETICAL
}
