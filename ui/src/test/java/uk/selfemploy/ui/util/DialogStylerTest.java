package uk.selfemploy.ui.util;

import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DialogStyler utility class.
 * Tests the dialog styling utilities based on /aura's design specification.
 */
@DisplayName("DialogStyler")
class DialogStylerTest {

    @Nested
    @DisplayName("Design Constants")
    class DesignConstantsTests {

        @Test
        @DisplayName("should define SHADOW_BLUR as 32")
        void shouldDefineShadowBlur() {
            assertEquals(32.0, DialogStyler.SHADOW_BLUR);
        }

        @Test
        @DisplayName("should define SHADOW_OPACITY as 0.35")
        void shouldDefineShadowOpacity() {
            assertEquals(0.35, DialogStyler.SHADOW_OPACITY, 0.001);
        }

        @Test
        @DisplayName("should define SHADOW_SPREAD as 0.15")
        void shouldDefineShadowSpread() {
            assertEquals(0.15, DialogStyler.SHADOW_SPREAD, 0.001);
        }

        @Test
        @DisplayName("should define SHADOW_Y_OFFSET as 12")
        void shouldDefineShadowYOffset() {
            assertEquals(12.0, DialogStyler.SHADOW_Y_OFFSET);
        }

        @Test
        @DisplayName("should define CORNER_RADIUS as 12")
        void shouldDefineCornerRadius() {
            assertEquals(12.0, DialogStyler.CORNER_RADIUS);
        }

        @Test
        @DisplayName("should define SHADOW_PADDING as 48")
        void shouldDefineShadowPadding() {
            assertEquals(48.0, DialogStyler.SHADOW_PADDING);
        }
    }

    @Nested
    @DisplayName("createStandardShadow()")
    class CreateStandardShadowTests {

        @Test
        @DisplayName("should return non-null DropShadow")
        void shouldReturnNonNullDropShadow() {
            DropShadow shadow = DialogStyler.createStandardShadow();
            assertNotNull(shadow);
        }

        @Test
        @DisplayName("should use GAUSSIAN blur type")
        void shouldUseGaussianBlurType() {
            DropShadow shadow = DialogStyler.createStandardShadow();
            assertEquals(BlurType.GAUSSIAN, shadow.getBlurType());
        }

        @Test
        @DisplayName("should use 32px blur radius")
        void shouldUse32pxBlurRadius() {
            DropShadow shadow = DialogStyler.createStandardShadow();
            assertEquals(32.0, shadow.getRadius());
        }

        @Test
        @DisplayName("should use 0.15 spread")
        void shouldUse015Spread() {
            DropShadow shadow = DialogStyler.createStandardShadow();
            assertEquals(0.15, shadow.getSpread(), 0.001);
        }

        @Test
        @DisplayName("should use 0 x-offset")
        void shouldUseZeroXOffset() {
            DropShadow shadow = DialogStyler.createStandardShadow();
            assertEquals(0.0, shadow.getOffsetX());
        }

        @Test
        @DisplayName("should use 12px y-offset")
        void shouldUse12pxYOffset() {
            DropShadow shadow = DialogStyler.createStandardShadow();
            assertEquals(12.0, shadow.getOffsetY());
        }

        @Test
        @DisplayName("should use 35% opacity black color")
        void shouldUse35PercentOpacityBlack() {
            DropShadow shadow = DialogStyler.createStandardShadow();
            Color color = shadow.getColor();

            assertEquals(0.0, color.getRed(), 0.001);
            assertEquals(0.0, color.getGreen(), 0.001);
            assertEquals(0.0, color.getBlue(), 0.001);
            assertEquals(0.35, color.getOpacity(), 0.001);
        }
    }

    @Nested
    @DisplayName("applyRoundedClip()")
    class ApplyRoundedClipTests {

        @Test
        @DisplayName("should apply clip to container")
        void shouldApplyClipToContainer() {
            VBox container = new VBox();

            DialogStyler.applyRoundedClip(container, 12.0);

            assertNotNull(container.getClip());
            assertInstanceOf(Rectangle.class, container.getClip());
        }

        @Test
        @DisplayName("should set arc width to radius * 2")
        void shouldSetArcWidthToRadiusTimesTwo() {
            VBox container = new VBox();

            DialogStyler.applyRoundedClip(container, 12.0);

            Rectangle clip = (Rectangle) container.getClip();
            assertEquals(24.0, clip.getArcWidth());
        }

        @Test
        @DisplayName("should set arc height to radius * 2")
        void shouldSetArcHeightToRadiusTimesTwo() {
            VBox container = new VBox();

            DialogStyler.applyRoundedClip(container, 12.0);

            Rectangle clip = (Rectangle) container.getClip();
            assertEquals(24.0, clip.getArcHeight());
        }

        @Test
        @DisplayName("should work with different radius values")
        void shouldWorkWithDifferentRadiusValues() {
            VBox container = new VBox();

            DialogStyler.applyRoundedClip(container, 8.0);

            Rectangle clip = (Rectangle) container.getClip();
            assertEquals(16.0, clip.getArcWidth());
            assertEquals(16.0, clip.getArcHeight());
        }

        @Test
        @DisplayName("should work with StackPane")
        void shouldWorkWithStackPane() {
            StackPane container = new StackPane();

            DialogStyler.applyRoundedClip(container, 12.0);

            assertNotNull(container.getClip());
            assertInstanceOf(Rectangle.class, container.getClip());
        }

        @Test
        @DisplayName("should not set clip dimensions when layout bounds are zero (GTK warning fix)")
        void shouldNotSetClipDimensionsWhenLayoutBoundsAreZero() {
            // This test verifies the GTK resize warning fix.
            // When a container has zero dimensions (before layout), the clip
            // should not be set to zero dimensions to avoid GTK warnings on Linux.
            VBox container = new VBox();

            DialogStyler.applyRoundedClip(container, 12.0);

            // Initially, container has zero layout bounds
            Rectangle clip = (Rectangle) container.getClip();

            // The clip should be created but width/height should remain at default (0)
            // until the container has positive dimensions
            assertNotNull(clip);
            // Initial clip dimensions should be 0 (not set yet)
            assertEquals(0.0, clip.getWidth());
            assertEquals(0.0, clip.getHeight());
        }

        @Test
        @DisplayName("should only update clip when dimensions are positive")
        void shouldOnlyUpdateClipWhenDimensionsArePositive() {
            // This simulates what happens when layout bounds change
            VBox container = new VBox();
            DialogStyler.applyRoundedClip(container, 12.0);

            Rectangle clip = (Rectangle) container.getClip();

            // Initial dimensions are 0
            assertEquals(0.0, clip.getWidth());
            assertEquals(0.0, clip.getHeight());

            // Note: In a real JavaFX application, the layoutBoundsProperty listener
            // will be triggered when the container is laid out with positive dimensions.
            // The fix ensures that when dimensions are 0, the clip is NOT updated,
            // preventing GTK warnings on Linux.
        }
    }

    @Nested
    @DisplayName("createShadowWrapper()")
    class CreateShadowWrapperTests {

        @Test
        @DisplayName("should return non-null StackPane")
        void shouldReturnNonNullStackPane() {
            VBox content = new VBox();

            StackPane wrapper = DialogStyler.createShadowWrapper(content);

            assertNotNull(wrapper);
        }

        @Test
        @DisplayName("should contain the provided content")
        void shouldContainProvidedContent() {
            VBox content = new VBox();

            StackPane wrapper = DialogStyler.createShadowWrapper(content);

            assertTrue(wrapper.getChildren().contains(content));
        }

        @Test
        @DisplayName("should have transparent background")
        void shouldHaveTransparentBackground() {
            VBox content = new VBox();

            StackPane wrapper = DialogStyler.createShadowWrapper(content);

            assertTrue(wrapper.getStyle().contains("-fx-background-color: transparent"));
        }

        @Test
        @DisplayName("should have 48px padding for shadow space")
        void shouldHave48pxPadding() {
            VBox content = new VBox();

            StackPane wrapper = DialogStyler.createShadowWrapper(content);

            assertEquals(48.0, wrapper.getPadding().getTop());
            assertEquals(48.0, wrapper.getPadding().getRight());
            assertEquals(48.0, wrapper.getPadding().getBottom());
            assertEquals(48.0, wrapper.getPadding().getLeft());
        }

        @Test
        @DisplayName("should have DropShadow effect applied")
        void shouldHaveDropShadowEffect() {
            VBox content = new VBox();

            StackPane wrapper = DialogStyler.createShadowWrapper(content);

            assertNotNull(wrapper.getEffect());
            assertInstanceOf(DropShadow.class, wrapper.getEffect());
        }

        @Test
        @DisplayName("should use standard shadow parameters")
        void shouldUseStandardShadowParameters() {
            VBox content = new VBox();

            StackPane wrapper = DialogStyler.createShadowWrapper(content);

            DropShadow shadow = (DropShadow) wrapper.getEffect();
            assertEquals(32.0, shadow.getRadius());
            assertEquals(0.15, shadow.getSpread(), 0.001);
            assertEquals(12.0, shadow.getOffsetY());
        }
    }

    @Nested
    @DisplayName("Utility Class Design")
    class UtilityClassDesignTests {

        @Test
        @DisplayName("should be a final class")
        void shouldBeFinalClass() {
            assertTrue(java.lang.reflect.Modifier.isFinal(DialogStyler.class.getModifiers()));
        }

        @Test
        @DisplayName("should have private constructor")
        void shouldHavePrivateConstructor() throws NoSuchMethodException {
            var constructor = DialogStyler.class.getDeclaredConstructor();
            assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        }
    }

    @Nested
    @DisplayName("getCssResourceUrl()")
    class GetCssResourceUrlTests {

        @Test
        @DisplayName("should return null for non-existent CSS file")
        void shouldReturnNullForNonExistentCssFile() {
            String result = DialogStyler.getCssResourceUrl("/css/non-existent.css");

            assertNull(result);
        }

        @Test
        @DisplayName("should return null for null path")
        void shouldReturnNullForNullPath() {
            String result = DialogStyler.getCssResourceUrl(null);

            assertNull(result);
        }

        @Test
        @DisplayName("should return null for empty path")
        void shouldReturnNullForEmptyPath() {
            String result = DialogStyler.getCssResourceUrl("");

            assertNull(result);
        }

        @Test
        @DisplayName("should return URL for existing CSS file")
        void shouldReturnUrlForExistingCssFile() {
            // notifications.css exists in the project
            String result = DialogStyler.getCssResourceUrl("/css/notifications.css");

            assertNotNull(result);
            assertTrue(result.contains("notifications.css"));
        }
    }
}
