package uk.selfemploy.ui.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link PopupPlacement}'s flip decision and Y resolution — no display required.
 */
@DisplayName("PopupPlacement")
class PopupPlacementTest {

    @Nested
    @DisplayName("shouldFlipUp")
    class ShouldFlipUp {

        @Test
        @DisplayName("flips when the space below is smaller than the popup")
        void flipsWhenNotEnoughRoom() {
            assertThat(PopupPlacement.shouldFlipUp(50, 200)).isTrue();
        }

        @Test
        @DisplayName("does not flip when the space below fits the popup")
        void staysWhenRoom() {
            assertThat(PopupPlacement.shouldFlipUp(300, 200)).isFalse();
        }

        @Test
        @DisplayName("does not flip at the exact boundary (space equals height)")
        void boundaryDoesNotFlip() {
            assertThat(PopupPlacement.shouldFlipUp(200, 200)).isFalse();
        }
    }

    @Nested
    @DisplayName("resolveTopY")
    class ResolveTopY {

        @Test
        @DisplayName("places the popup directly below the anchor when there is room")
        void belowWhenRoom() {
            // anchor top=100, height=20 -> bottom=120; screen 0..800; popup=200 fits (680 space).
            double y = PopupPlacement.resolveTopY(100, 20, 200, 0, 800);
            assertThat(y).isEqualTo(120);
        }

        @Test
        @DisplayName("flips the popup above the anchor when it would overflow the bottom")
        void aboveWhenNoRoom() {
            // anchor top=700, height=20 -> bottom=720; screen 0..800; popup=200 -> only 80 below.
            double y = PopupPlacement.resolveTopY(700, 20, 200, 0, 800);
            // Flipped: popup top sits popupHeight above the anchor top -> 700 - 200 = 500.
            assertThat(y).isEqualTo(500);
        }

        @Test
        @DisplayName("flipped popup's bottom rests on the anchor's top edge")
        void flippedBottomMeetsAnchorTop() {
            double anchorTop = 700;
            double popupHeight = 200;
            double y = PopupPlacement.resolveTopY(anchorTop, 20, popupHeight, 0, 800);
            assertThat(y + popupHeight).isEqualTo(anchorTop);
        }

        @Test
        @DisplayName("clamps a tall flipped popup to the screen top instead of going off the top edge")
        void clampsToScreenTop() {
            // anchor near the bottom (top=760, height=20, bottom=780); screen 0..800; popup=500.
            // Naive flip = 760 - 500 = 260 (fine here) — but with a small screen top offset it must clamp.
            // screen top = 100: naive flip 260 is above nothing; use an anchor+popup that would overflow.
            double y = PopupPlacement.resolveTopY(300, 20, 500, 100, 400);
            // spaceBelow = 400-320 = 80 < 500 -> flip; naive = 300-500 = -200; screen top = 100.
            assertThat(y).isEqualTo(100);
        }

        @Test
        @DisplayName("pins a popup taller than the screen to the top edge")
        void pinsOversizedPopupToTop() {
            // popup (1000) taller than the screen (0..800) -> top pinned to screen top.
            double y = PopupPlacement.resolveTopY(400, 20, 1000, 0, 800);
            assertThat(y).isEqualTo(0);
        }
    }
}
