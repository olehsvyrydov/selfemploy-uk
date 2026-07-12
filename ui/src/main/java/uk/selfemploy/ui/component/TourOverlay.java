package uk.selfemploy.ui.component;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import uk.selfemploy.ui.viewmodel.TourStep;
import uk.selfemploy.ui.viewmodel.TourViewModel;

/**
 * A modal overlay that presents the guided tour: a dimmed backdrop with a spotlight cut out over the
 * highlighted control, and a coach-mark card with the step text and Back / Next / Skip controls.
 *
 * <p>The overlay is added on top of the application's root {@link StackPane} while the tour runs and
 * removed when it finishes. It consumes background mouse input so the app is not interactable during
 * the tour, and Escape (or Skip) ends it. Targets are resolved by {@code fx:id} against a supplied
 * root node, so a step whose target is missing simply shows a centred card with no spotlight.</p>
 */
public class TourOverlay extends StackPane {

    private static final double SPOTLIGHT_PADDING = 6;

    private final TourViewModel viewModel;
    private final Node lookupRoot;
    private final Runnable onFinished;

    private final Pane backdrop = new Pane();
    private final VBox card = new VBox();
    private final Label titleLabel = new Label();
    private final Label bodyLabel = new Label();
    private final Label progressLabel = new Label();
    private final Button backButton = new Button("Back");
    private final Button nextButton = new Button("Next");
    private final Button skipButton = new Button("Skip");

    /**
     * @param viewModel  the tour state machine, already started
     * @param lookupRoot the node whose subtree is searched for step targets by {@code fx:id}
     * @param onFinished run once when the tour ends (via finishing, Skip or Escape); typically
     *                   removes this overlay from its parent
     */
    public TourOverlay(TourViewModel viewModel, Node lookupRoot, Runnable onFinished) {
        this.viewModel = viewModel;
        this.lookupRoot = lookupRoot;
        this.onFinished = onFinished;

        getStyleClass().add("tour-overlay");
        setPickOnBounds(true);
        setAlignment(Pos.CENTER);

        backdrop.setMouseTransparent(false);
        backdrop.setOnMousePressed(e -> e.consume());
        backdrop.setOnMouseClicked(e -> e.consume());

        buildCard();
        getChildren().addAll(backdrop, card);

        // Re-render when the overlay is resized so the spotlight tracks the target.
        widthProperty().addListener((obs, o, n) -> render());
        heightProperty().addListener((obs, o, n) -> render());

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                finish();
                e.consume();
            }
        });

        render();
    }

    private void buildCard() {
        titleLabel.getStyleClass().add("tour-card-title");
        bodyLabel.getStyleClass().add("tour-card-body");
        bodyLabel.setWrapText(true);
        bodyLabel.setMaxWidth(360);
        progressLabel.getStyleClass().add("tour-card-progress");

        backButton.getStyleClass().addAll("button-secondary", "tour-card-button");
        skipButton.getStyleClass().addAll("button-secondary", "tour-card-button");
        nextButton.getStyleClass().addAll("button-success", "tour-card-button");

        backButton.setOnAction(e -> {
            viewModel.back();
            render();
        });
        nextButton.setOnAction(e -> {
            viewModel.next();
            if (viewModel.isActive()) {
                render();
            } else {
                finish();
            }
        });
        skipButton.setOnAction(e -> finish());

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox buttons = new HBox(10, skipButton, spacer, backButton, nextButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        card.getStyleClass().add("tour-card");
        card.setSpacing(12);
        card.setMaxWidth(400);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.getChildren().addAll(progressLabel, titleLabel, bodyLabel, buttons);
    }

    /** Redraws the backdrop spotlight and updates the card for the current step. */
    private void render() {
        if (!viewModel.isActive()) {
            return;
        }
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return; // not laid out yet; a size listener will call render() again
        }

        TourStep step = viewModel.currentStep();

        // Card content
        progressLabel.setText(viewModel.progressText());
        titleLabel.setText(step.title());
        bodyLabel.setText(step.body());
        backButton.setVisible(viewModel.hasPrevious());
        backButton.setManaged(viewModel.hasPrevious());
        nextButton.setText(viewModel.isLastStep() ? "Finish" : "Next");

        // Backdrop with an optional spotlight hole over the target control
        backdrop.getChildren().clear();
        Bounds target = resolveTargetBounds(step);
        Shape dim = new Rectangle(0, 0, w, h);
        if (target != null) {
            Rectangle hole = new Rectangle(
                target.getMinX() - SPOTLIGHT_PADDING,
                target.getMinY() - SPOTLIGHT_PADDING,
                target.getWidth() + 2 * SPOTLIGHT_PADDING,
                target.getHeight() + 2 * SPOTLIGHT_PADDING);
            hole.setArcWidth(12);
            hole.setArcHeight(12);
            dim = Shape.subtract(dim, hole);

            Rectangle highlight = new Rectangle(
                target.getMinX() - SPOTLIGHT_PADDING,
                target.getMinY() - SPOTLIGHT_PADDING,
                target.getWidth() + 2 * SPOTLIGHT_PADDING,
                target.getHeight() + 2 * SPOTLIGHT_PADDING);
            highlight.setArcWidth(12);
            highlight.setArcHeight(12);
            highlight.setFill(Color.TRANSPARENT);
            highlight.getStyleClass().add("tour-spotlight-border");
            highlight.setStroke(Color.web("#22c55e"));
            highlight.setStrokeWidth(2);
            highlight.setMouseTransparent(true);
            dim.setFill(Color.rgb(0, 0, 0, 0.55));
            dim.setMouseTransparent(false);
            backdrop.getChildren().addAll(dim, highlight);
        } else {
            dim.setFill(Color.rgb(0, 0, 0, 0.55));
            backdrop.getChildren().add(dim);
        }
        // The backdrop still consumes clicks even where the dim shape has a hole.
        backdrop.resizeRelocate(0, 0, w, h);
    }

    private Bounds resolveTargetBounds(TourStep step) {
        if (step.targetNodeId() == null || lookupRoot == null) {
            return null;
        }
        Node target = lookupRoot.lookup("#" + step.targetNodeId());
        if (target == null || target.getScene() == null) {
            return null;
        }
        Bounds inScene = target.localToScene(target.getBoundsInLocal());
        return sceneToLocal(inScene);
    }

    private void finish() {
        if (viewModel.isActive()) {
            viewModel.skip();
        }
        if (onFinished != null) {
            onFinished.run();
        }
    }
}
