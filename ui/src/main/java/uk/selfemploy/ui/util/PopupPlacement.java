package uk.selfemploy.ui.util;

import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.stage.PopupWindow;
import javafx.stage.Screen;

/**
 * Positions a transient popup below its anchor, flipping it above when there is not enough room near
 * the bottom edge of the screen, so the popup never renders off-screen.
 *
 * <p>The flip decision is a pure function of screen geometry (the anchor's position and height, the
 * popup's height, and the screen's bottom edge). It is deliberately separated from the JavaFX
 * {@code show} call so it can be unit-tested without a display.</p>
 */
public final class PopupPlacement {

    private PopupPlacement() {
    }

    /**
     * Whether a popup of {@code popupHeight} should open above its anchor because the space below it
     * (down to the screen's bottom edge) is too small to hold it.
     *
     * @param spaceBelow  the vertical gap between the anchor's bottom edge and the screen's bottom edge
     * @param popupHeight the popup's height
     * @return {@code true} when the popup would overflow the bottom edge and should flip upward
     */
    public static boolean shouldFlipUp(double spaceBelow, double popupHeight) {
        return spaceBelow < popupHeight;
    }

    /**
     * The screen Y coordinate for the popup's top edge: directly below the anchor when it fits,
     * otherwise flipped so the popup sits just above the anchor.
     *
     * @param anchorMinY   the anchor's top edge, in screen coordinates
     * @param anchorHeight the anchor's height
     * @param popupHeight  the popup's height
     * @param screenMaxY   the bottom edge of the usable screen area, in screen coordinates
     * @return the Y coordinate at which to place the popup's top edge
     */
    public static double resolveTopY(double anchorMinY, double anchorHeight, double popupHeight,
                                     double screenMaxY) {
        double belowY = anchorMinY + anchorHeight;
        double spaceBelow = screenMaxY - belowY;
        if (shouldFlipUp(spaceBelow, popupHeight)) {
            return anchorMinY - popupHeight;
        }
        return belowY;
    }

    /**
     * Shows {@code popup} left-aligned with {@code anchor}, directly below it, or flipped above it when
     * the space down to the screen's bottom edge is smaller than the popup. The popup is shown first so
     * its content has a measured height, then repositioned upward if it would overflow.
     *
     * @param popup  the popup to show; its content must already be built so its height is meaningful
     * @param anchor a node attached to a shown scene, used as the anchor and to locate the screen
     */
    public static void showBelowOrAbove(PopupWindow popup, Node anchor) {
        Bounds anchorBounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (anchorBounds == null) {
            // Anchor is not on a shown scene; fall back to the platform's default placement.
            popup.show(anchor.getScene() != null ? anchor.getScene().getWindow() : null);
            return;
        }
        double screenMaxY = screenMaxYFor(anchorBounds);
        // Show below first so the popup lays out and reports a real height, then flip up if needed.
        popup.show(anchor, anchorBounds.getMinX(), anchorBounds.getMaxY());
        double y = resolveTopY(anchorBounds.getMinY(), anchorBounds.getHeight(), popup.getHeight(),
                screenMaxY);
        popup.setX(anchorBounds.getMinX());
        popup.setY(y);
    }

    /** The bottom edge of the screen's visible area that hosts the given anchor bounds. */
    private static double screenMaxYFor(Bounds anchorBounds) {
        List<Screen> screens = Screen.getScreensForRectangle(
                anchorBounds.getMinX(), anchorBounds.getMinY(),
                Math.max(1, anchorBounds.getWidth()), Math.max(1, anchorBounds.getHeight()));
        Rectangle2D visual = screens.isEmpty()
                ? Screen.getPrimary().getVisualBounds()
                : screens.get(0).getVisualBounds();
        return visual.getMaxY();
    }
}
