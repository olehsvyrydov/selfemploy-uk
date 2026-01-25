package uk.selfemploy.plugin.extension;

import javafx.scene.Node;

/**
 * Extension point for adding widgets to the dashboard.
 *
 * <p>Dashboard widgets are UI components that display information or provide
 * quick access to functionality directly on the application's main dashboard.
 * Plugins implement this interface to contribute widgets that appear alongside
 * the built-in dashboard content.</p>
 *
 * <h2>Widget Lifecycle</h2>
 * <ol>
 *   <li>{@link #createWidget()} is called when the dashboard is first displayed</li>
 *   <li>{@link #refresh()} is called when the dashboard data should be updated</li>
 *   <li>The widget is disposed when the user navigates away from the dashboard</li>
 * </ol>
 *
 * <h2>Implementation Example</h2>
 * <pre>{@code
 * public class ProfitForecastWidget implements DashboardWidget {
 *     @Override
 *     public String getWidgetId() {
 *         return "profit-forecast";
 *     }
 *
 *     @Override
 *     public String getWidgetTitle() {
 *         return "Profit Forecast";
 *     }
 *
 *     @Override
 *     public WidgetSize getWidgetSize() {
 *         return WidgetSize.MEDIUM;
 *     }
 *
 *     @Override
 *     public Node createWidget() {
 *         return new ProfitForecastPane();
 *     }
 *
 *     @Override
 *     public void refresh() {
 *         // Reload forecast data
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this interface are called on the JavaFX Application Thread.</p>
 *
 * @see WidgetSize
 * @see ExtensionPoint
 */
public interface DashboardWidget extends ExtensionPoint {

    /**
     * Returns the unique identifier for this widget.
     *
     * <p>The ID must be unique across all dashboard widgets in the application.
     * It is recommended to use a namespaced format like "plugin-name.widget-id"
     * to avoid conflicts.</p>
     *
     * @return the widget ID, never null or blank
     */
    String getWidgetId();

    /**
     * Returns the display title for this widget.
     *
     * <p>The title is typically shown in a header area above the widget content.
     * Keep it short and descriptive (typically 1-4 words).</p>
     *
     * @return the widget title, never null or blank
     */
    String getWidgetTitle();

    /**
     * Returns the preferred size for this widget.
     *
     * <p>The dashboard layout will attempt to honor this preference, but may
     * adjust based on available space and window dimensions.</p>
     *
     * @return the widget size preference, never null
     * @see WidgetSize
     */
    WidgetSize getWidgetSize();

    /**
     * Returns the sort order for positioning on the dashboard.
     *
     * <p>Lower values appear first (top-left). Built-in widgets typically use
     * values in the range 0-100. Plugin widgets should use values of 100 or
     * higher to appear after built-in widgets.</p>
     *
     * <p>Default value is 100.</p>
     *
     * @return the widget order
     */
    default int getWidgetOrder() {
        return 100;
    }

    /**
     * Creates the widget content.
     *
     * <p>This method is called once when the dashboard is first displayed.
     * The returned Node should be fully initialized and ready to display.</p>
     *
     * <p>This method is called on the JavaFX Application Thread.</p>
     *
     * @return the widget content node, never null
     */
    Node createWidget();

    /**
     * Called when the dashboard data should be refreshed.
     *
     * <p>This method is invoked when the user manually refreshes the dashboard,
     * when the selected tax year changes, or when relevant data is modified.
     * Implementations should update their display to reflect current data.</p>
     *
     * <p>This method is called on the JavaFX Application Thread.</p>
     */
    void refresh();
}
