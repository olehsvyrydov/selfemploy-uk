package uk.selfemploy.ui.util;

import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.stage.PopupWindow;
import javafx.stage.Screen;
import javafx.stage.Window;

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
     * otherwise flipped so the popup sits just above the anchor. The result is clamped to the usable
     * screen area so a flipped popup taller than the space above the anchor never runs off the top
     * edge (a popup taller than the whole screen is pinned to the top, its overflow clipped at the
     * bottom rather than hidden above).
     *
     * @param anchorMinY   the anchor's top edge, in screen coordinates
     * @param anchorHeight the anchor's height
     * @param popupHeight  the popup's height
     * @param screenMinY   the top edge of the usable screen area, in screen coordinates
     * @param screenMaxY   the bottom edge of the usable screen area, in screen coordinates
     * @return the Y coordinate at which to place the popup's top edge
     */
    public static double resolveTopY(double anchorMinY, double anchorHeight, double popupHeight,
                                     double screenMinY, double screenMaxY) {
        double belowY = anchorMinY + anchorHeight;
        double spaceBelow = screenMaxY - belowY;
        double y = shouldFlipUp(spaceBelow, popupHeight) ? anchorMinY - popupHeight : belowY;
        double highestTop = screenMaxY - popupHeight; // top position that still keeps the bottom on-screen
        if (highestTop < screenMinY) {
            return screenMinY; // popup taller than the screen: pin its top to the top edge
        }
        return Math.max(screenMinY, Math.min(y, highestTop));
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
            // Anchor is not on a shown scene: without an owner window the popup can be neither placed
            // nor safely shown, so skip it rather than call show(null) (which throws).
            Window owner = anchor.getScene() != null ? anchor.getScene().getWindow() : null;
            if (owner != null) {
                popup.show(owner);
            }
            return;
        }
        Rectangle2D visual = visualBoundsFor(anchorBounds);
        // Show below first so the popup lays out and reports a real height, then flip up if needed.
        popup.show(anchor, anchorBounds.getMinX(), anchorBounds.getMaxY());
        double y = resolveTopY(anchorBounds.getMinY(), anchorBounds.getHeight(), popup.getHeight(),
                visual.getMinY(), visual.getMaxY());
        popup.setX(anchorBounds.getMinX());
        popup.setY(y);
    }

    /** The visible bounds of the screen that hosts the given anchor bounds. */
    private static Rectangle2D visualBoundsFor(Bounds anchorBounds) {
        List<Screen> screens = Screen.getScreensForRectangle(
                anchorBounds.getMinX(), anchorBounds.getMinY(),
                Math.max(1, anchorBounds.getWidth()), Math.max(1, anchorBounds.getHeight()));
        return screens.isEmpty()
                ? Screen.getPrimary().getVisualBounds()
                : screens.get(0).getVisualBounds();
    }
}
