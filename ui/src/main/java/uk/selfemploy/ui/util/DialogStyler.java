package uk.selfemploy.ui.util;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.logging.Logger;

/**
 * Static utility class for applying consistent dialog styling across the application.
 *
 * <p>This class provides reusable methods for creating professional-looking dialogs
 * with rounded corners, drop shadows, and proper centering - based on /aura's design
 * specification.</p>
 *
 * <h2>Design Constants</h2>
 * <ul>
 *   <li><b>Shadow:</b> 35% opacity, 32px blur, 0.15 spread, 12px y-offset</li>
 *   <li><b>Corner Radius:</b> 12px</li>
 *   <li><b>Shadow Padding:</b> 48px (space for shadow to render)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * VBox container = new VBox();
 * container.getStyleClass().add("my-dialog-container");
 *
 * // Apply rounded corners
 * DialogStyler.applyRoundedClip(container, DialogStyler.CORNER_RADIUS);
 *
 * // Create shadow wrapper
 * StackPane wrapper = DialogStyler.createShadowWrapper(container);
 *
 * // Setup the stage
 * Stage stage = new Stage();
 * DialogStyler.setupStyledDialog(stage, wrapper, "/css/my-dialog.css");
 * DialogStyler.centerOnOwner(stage);
 * }</pre>
 *
 * @author /james
 * @see <a href="docs/sprints/sprint-8/investigations/popup-factory-investigation.md">Investigation Report</a>
 * @since SE-830
 */
public final class DialogStyler {

    private static final Logger LOG = Logger.getLogger(DialogStyler.class.getName());

    // ==================== Design Constants (per /aura's specification) ====================

    /**
     * Shadow blur radius in pixels.
     * Creates a soft, diffused shadow edge.
     */
    public static final double SHADOW_BLUR = 32.0;

    /**
     * Shadow color opacity (0.0 to 1.0).
     * 35% opacity provides subtle depth without being heavy.
     */
    public static final double SHADOW_OPACITY = 0.35;

    /**
     * Shadow spread factor (0.0 to 1.0).
     * Controls how much the shadow extends beyond the blur radius.
     */
    public static final double SHADOW_SPREAD = 0.15;

    /**
     * Shadow vertical offset in pixels.
     * Positive value creates shadow below the element (natural light from above).
     */
    public static final double SHADOW_Y_OFFSET = 12.0;

    /**
     * Default corner radius for rounded dialogs in pixels.
     * Matches the application's design system.
     */
    public static final double CORNER_RADIUS = 12.0;

    /**
     * Padding around the container to allow shadow to render.
     * Must be larger than SHADOW_BLUR + SHADOW_Y_OFFSET.
     */
    public static final double SHADOW_PADDING = 48.0;

    // ==================== Private Constructor ====================

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private DialogStyler() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }

    // ==================== Shadow Methods ====================

    /**
     * Creates the standard drop shadow effect per /aura's design specification.
     *
     * <p>Shadow parameters:</p>
     * <ul>
     *   <li>Blur type: GAUSSIAN</li>
     *   <li>Color: Black at 35% opacity</li>
     *   <li>Radius: 32px</li>
     *   <li>Spread: 0.15</li>
     *   <li>X-offset: 0</li>
     *   <li>Y-offset: 12px</li>
     * </ul>
     *
     * @return a configured DropShadow effect
     */
    public static DropShadow createStandardShadow() {
        return new DropShadow(
            BlurType.GAUSSIAN,
            Color.rgb(0, 0, 0, SHADOW_OPACITY),
            SHADOW_BLUR,
            SHADOW_SPREAD,
            0,              // offsetX
            SHADOW_Y_OFFSET
        );
    }

    // ==================== Rounded Corners Methods ====================

    /**
     * Applies a rounded corner clip to a container.
     *
     * <p>This method creates a {@link Rectangle} clip with rounded corners
     * and binds it to the container's layout bounds so it automatically
     * resizes with the container.</p>
     *
     * <p><b>Note:</b> When using a clip, any drop shadow effect must be
     * applied to a parent wrapper, not the clipped container itself,
     * as the clip will cut off the shadow.</p>
     *
     * @param container the region to apply the rounded clip to
     * @param radius the corner radius in pixels
     */
    public static void applyRoundedClip(Region container, double radius) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        container.setClip(clip);

        // Bind clip size to container size
        container.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            clip.setWidth(newBounds.getWidth());
            clip.setHeight(newBounds.getHeight());
        });
    }

    // ==================== Shadow Wrapper Methods ====================

    /**
     * Creates a shadow wrapper for the given content.
     *
     * <p>The wrapper is a transparent {@link StackPane} with padding to allow
     * the shadow to render outside the content bounds. The standard drop shadow
     * effect is automatically applied.</p>
     *
     * <p>This should be used in combination with {@link #applyRoundedClip} on
     * the content container:</p>
     * <pre>{@code
     * VBox container = new VBox();
     * DialogStyler.applyRoundedClip(container, DialogStyler.CORNER_RADIUS);
     * StackPane wrapper = DialogStyler.createShadowWrapper(container);
     * }</pre>
     *
     * @param content the content node to wrap (typically a VBox with rounded clip)
     * @return a StackPane wrapper with shadow effect and proper padding
     */
    public static StackPane createShadowWrapper(Region content) {
        StackPane wrapper = new StackPane(content);
        wrapper.setStyle("-fx-background-color: transparent;");
        wrapper.setPadding(new Insets(SHADOW_PADDING));
        wrapper.setEffect(createStandardShadow());
        return wrapper;
    }

    // ==================== CSS Resource Methods ====================

    /**
     * Gets the URL string for a CSS resource, with null-safety.
     *
     * <p>This method safely resolves a CSS resource path and returns its
     * external form URL. If the resource doesn't exist, it returns null
     * instead of throwing a NullPointerException.</p>
     *
     * @param cssPath the classpath path to the CSS file (e.g., "/css/notifications.css")
     * @return the external form URL string, or null if the resource doesn't exist
     *         or the path is null/empty
     */
    public static String getCssResourceUrl(String cssPath) {
        if (cssPath == null || cssPath.isEmpty()) {
            return null;
        }
        var resource = DialogStyler.class.getResource(cssPath);
        return resource != null ? resource.toExternalForm() : null;
    }

    // ==================== Stage Setup Methods ====================

    /**
     * Centers a stage on its owner window when shown.
     *
     * <p>The centering is performed in the stage's {@code onShown} handler
     * to ensure the stage dimensions are known.</p>
     *
     * <p><b>Note:</b> The stage must have an owner set via {@code initOwner()}
     * before calling this method, otherwise no centering will occur.</p>
     *
     * @param stage the stage to center
     */
    public static void centerOnOwner(Stage stage) {
        Window owner = stage.getOwner();
        if (owner != null) {
            stage.setOnShown(e -> {
                stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2);
                stage.setY(owner.getY() + (owner.getHeight() - stage.getHeight()) / 2);
            });
        }
    }

    /**
     * Performs styled dialog scene setup.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Creates a scene with the provided root node</li>
     *   <li>Sets the scene fill to transparent</li>
     *   <li>Loads the specified CSS stylesheet</li>
     *   <li>Attaches the scene to the stage</li>
     * </ol>
     *
     * <p><b>Important:</b> The caller must set {@code stage.initStyle(StageStyle.TRANSPARENT)}
     * BEFORE calling {@code initModality()} or {@code initOwner()}, as JavaFX requires
     * the stage style to be set first.</p>
     *
     * <p><b>Typical usage:</b></p>
     * <pre>{@code
     * Stage stage = new Stage();
     * stage.initStyle(StageStyle.TRANSPARENT);  // Must be first!
     * stage.initOwner(ownerWindow);
     * stage.initModality(Modality.APPLICATION_MODAL);
     *
     * VBox container = new VBox();
     * DialogStyler.applyRoundedClip(container, DialogStyler.CORNER_RADIUS);
     * StackPane wrapper = DialogStyler.createShadowWrapper(container);
     *
     * DialogStyler.setupStyledDialog(stage, wrapper, "/css/my-dialog.css");
     * DialogStyler.centerOnOwner(stage);
     * }</pre>
     *
     * @param stage the stage to configure (must have initStyle already set)
     * @param root the root node for the scene (typically a shadow wrapper)
     * @param cssPath the path to the CSS stylesheet (e.g., "/css/notifications.css")
     */
    public static void setupStyledDialog(Stage stage, Region root, String cssPath) {
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        if (cssPath != null && !cssPath.isEmpty()) {
            String cssUrl = getCssResourceUrl(cssPath);
            if (cssUrl == null) {
                LOG.warning("CSS file not found: " + cssPath + ". Dialog will use default styling.");
            } else {
                scene.getStylesheets().add(cssUrl);
            }
        }

        stage.setScene(scene);
    }

    /**
     * Convenience method to fully setup a styled dialog with rounded corners and shadow.
     *
     * <p>This method combines all styling steps:</p>
     * <ol>
     *   <li>Applies rounded clip to the container</li>
     *   <li>Creates a shadow wrapper</li>
     *   <li>Sets up the stage with transparent scene</li>
     *   <li>Centers the dialog on the owner window</li>
     * </ol>
     *
     * <p><b>Important:</b> The caller must set {@code stage.initStyle(StageStyle.TRANSPARENT)}
     * BEFORE calling {@code initModality()} or {@code initOwner()}, as JavaFX requires
     * the stage style to be set first.</p>
     *
     * @param stage the stage to configure (must have initStyle already set)
     * @param container the dialog content container (will have clip applied)
     * @param cssPath the path to the CSS stylesheet
     * @param cornerRadius the corner radius for rounded corners
     * @return the shadow wrapper (for adding to scene)
     */
    public static StackPane setupFullyStyledDialog(Stage stage, VBox container,
                                                    String cssPath, double cornerRadius) {
        applyRoundedClip(container, cornerRadius);
        StackPane wrapper = createShadowWrapper(container);
        setupStyledDialog(stage, wrapper, cssPath);
        centerOnOwner(stage);
        return wrapper;
    }
}
