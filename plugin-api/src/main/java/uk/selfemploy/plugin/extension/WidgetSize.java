package uk.selfemploy.plugin.extension;

/**
 * Defines the size preferences for dashboard widgets.
 *
 * <p>Widget sizes determine how much horizontal space a widget occupies
 * on the dashboard grid. The actual dimensions may vary based on the
 * window size and responsive layout.</p>
 *
 * @see DashboardWidget
 */
public enum WidgetSize {

    /**
     * Small widget occupying approximately 1/4 of the dashboard width.
     * <p>Suitable for simple metrics, counts, or status indicators.</p>
     */
    SMALL(1),

    /**
     * Medium widget occupying approximately 1/2 of the dashboard width.
     * <p>Suitable for charts, lists, or moderate detail displays.</p>
     */
    MEDIUM(2),

    /**
     * Large widget occupying the full dashboard width.
     * <p>Suitable for tables, detailed charts, or complex displays.</p>
     */
    LARGE(4),

    /**
     * Custom size where the plugin specifies exact dimensions.
     * <p>Use when standard sizes don't fit the widget's requirements.</p>
     */
    CUSTOM(0);

    private final int gridSpan;

    WidgetSize(int gridSpan) {
        this.gridSpan = gridSpan;
    }

    /**
     * Returns the number of grid columns this size spans.
     *
     * <p>Returns 0 for {@link #CUSTOM} size, indicating the plugin
     * should specify dimensions explicitly.</p>
     *
     * @return the grid span (0 for custom)
     */
    public int getGridSpan() {
        return gridSpan;
    }
}
