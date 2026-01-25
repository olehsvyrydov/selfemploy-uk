package uk.selfemploy.plugin.extension;

/**
 * Defines the navigation groups where plugin pages can be added.
 *
 * <p>Each group represents a section in the application's navigation sidebar.
 * Plugins can add their navigation items to any of these groups based on
 * the nature of their functionality.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class MyNavigationExtension implements NavigationExtension {
 *     @Override
 *     public NavigationGroup getNavigationGroup() {
 *         return NavigationGroup.INTEGRATIONS;
 *     }
 *     // ... other methods
 * }
 * }</pre>
 *
 * @see NavigationExtension
 */
public enum NavigationGroup {

    /**
     * Main navigation area containing primary features.
     * <p>Examples: Dashboard, Income, Expenses, Tax Summary</p>
     */
    MAIN("Main"),

    /**
     * Reports section for generating and viewing reports.
     * <p>Examples: Tax reports, profit/loss statements, annual summaries</p>
     */
    REPORTS("Reports"),

    /**
     * Integrations section for third-party service connections.
     * <p>Examples: Bank connections, payment processors, invoice services</p>
     */
    INTEGRATIONS("Integrations"),

    /**
     * Settings section for configuration pages.
     * <p>Examples: Plugin settings, preferences, account configuration</p>
     */
    SETTINGS("Settings");

    private final String displayName;

    NavigationGroup(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable display name for this group.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
