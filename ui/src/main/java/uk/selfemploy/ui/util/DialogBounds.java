package uk.selfemploy.ui.util;

import java.util.List;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Window;

/**
 * Screen-geometry helpers for modal dialogs.
 *
 * <p>Bounds are resolved from the screen that hosts the owner window rather than the primary
 * screen, so on multi-monitor setups a dialog opens and stays on the same display as the
 * application instead of jumping to the primary monitor (where it may be sized for a larger
 * screen and push its footer off the bottom edge).
 */
public final class DialogBounds {

    private DialogBounds() {
    }

    /**
     * Returns the visible bounds of the screen containing {@code owner}, falling back to the
     * primary screen's visible bounds when the owner is {@code null}, not yet positioned, or
     * off every known screen.
     *
     * @param owner the window a dialog is owned by, may be {@code null}
     * @return the visual bounds to size and position the dialog against
     */
    public static Rectangle2D visualBoundsForOwner(Window owner) {
        if (owner != null && !Double.isNaN(owner.getX()) && !Double.isNaN(owner.getY())) {
            double width = Double.isNaN(owner.getWidth()) ? 1 : Math.max(1, owner.getWidth());
            double height = Double.isNaN(owner.getHeight()) ? 1 : Math.max(1, owner.getHeight());
            List<Screen> screens = Screen.getScreensForRectangle(owner.getX(), owner.getY(), width, height);
            if (!screens.isEmpty()) {
                return screens.get(0).getVisualBounds();
            }
        }
        return Screen.getPrimary().getVisualBounds();
    }
}
