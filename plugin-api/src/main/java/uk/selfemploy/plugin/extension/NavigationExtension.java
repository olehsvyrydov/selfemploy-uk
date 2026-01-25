package uk.selfemploy.plugin.extension;

import javafx.scene.Node;
import org.kordamp.ikonli.Ikon;

/**
 * Extension point for adding navigation items and views to the application.
 *
 * <p>Plugins implement this interface to add new pages to the application's
 * navigation sidebar. Each navigation extension appears as a clickable item
 * in the sidebar, and when selected, displays the content returned by
 * {@link #createView()}.</p>
 *
 * <h2>Navigation Groups</h2>
 * <p>Navigation items are organized into groups:</p>
 * <ul>
 *   <li>{@link NavigationGroup#MAIN} - Primary features (Dashboard, Income, etc.)</li>
 *   <li>{@link NavigationGroup#REPORTS} - Report generation and viewing</li>
 *   <li>{@link NavigationGroup#INTEGRATIONS} - Third-party service connections</li>
 *   <li>{@link NavigationGroup#SETTINGS} - Configuration and settings pages</li>
 * </ul>
 *
 * <h2>Implementation Example</h2>
 * <pre>{@code
 * public class BankSettingsNavigation implements NavigationExtension {
 *     @Override
 *     public String getNavigationId() {
 *         return "bank-settings";
 *     }
 *
 *     @Override
 *     public String getNavigationLabel() {
 *         return "Bank Settings";
 *     }
 *
 *     @Override
 *     public Ikon getNavigationIcon() {
 *         return FontAwesomeSolid.UNIVERSITY;
 *     }
 *
 *     @Override
 *     public NavigationGroup getNavigationGroup() {
 *         return NavigationGroup.INTEGRATIONS;
 *     }
 *
 *     @Override
 *     public Node createView() {
 *         return new BankSettingsPane();
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>The {@link #createView()} method is always called on the JavaFX Application
 * Thread. Other methods may be called from any thread.</p>
 *
 * @see NavigationGroup
 * @see ExtensionPoint
 */
public interface NavigationExtension extends ExtensionPoint {

    /**
     * Returns the unique identifier for this navigation item.
     *
     * <p>The ID must be unique across all navigation extensions in the
     * application. It is recommended to use a namespaced format like
     * "plugin-name.navigation-id" to avoid conflicts.</p>
     *
     * @return the navigation ID, never null or blank
     */
    String getNavigationId();

    /**
     * Returns the display label for this navigation item.
     *
     * <p>This label is shown in the navigation sidebar. Keep it short
     * and descriptive (typically 1-3 words).</p>
     *
     * @return the navigation label, never null or blank
     */
    String getNavigationLabel();

    /**
     * Returns the icon for this navigation item.
     *
     * <p>The icon is displayed next to the label in the navigation sidebar.
     * Use Ikonli icon packs (e.g., FontAwesome5) for consistency with the
     * application's visual style.</p>
     *
     * <p>Return {@code null} to use a default icon.</p>
     *
     * @return the navigation icon, or null for default
     */
    Ikon getNavigationIcon();

    /**
     * Returns the navigation group where this item should appear.
     *
     * @return the navigation group, never null
     * @see NavigationGroup
     */
    NavigationGroup getNavigationGroup();

    /**
     * Returns the sort order for positioning within the navigation group.
     *
     * <p>Lower values appear higher in the list. Built-in navigation items
     * typically use values in the range 0-100. Plugin navigation items
     * should use values of 100 or higher to appear below built-in items.</p>
     *
     * <p>Default value is 100.</p>
     *
     * @return the navigation order
     */
    default int getNavigationOrder() {
        return 100;
    }

    /**
     * Creates the view content to display when this navigation item is selected.
     *
     * <p>This method is called on the JavaFX Application Thread each time
     * the navigation item is selected. Implementations may choose to cache
     * the view or create a new instance each time.</p>
     *
     * <p>The returned Node is placed in the main content area of the
     * application window.</p>
     *
     * @return the view content, never null
     */
    Node createView();
}
