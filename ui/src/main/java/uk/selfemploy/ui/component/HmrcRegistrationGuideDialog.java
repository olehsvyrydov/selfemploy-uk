package uk.selfemploy.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import uk.selfemploy.ui.service.HmrcRegistrationGuide;
import uk.selfemploy.ui.util.BrowserUtil;
import uk.selfemploy.ui.util.ClipboardUtil;
import uk.selfemploy.ui.util.DialogBounds;
import uk.selfemploy.ui.util.DialogStyler;

import java.util.function.Consumer;

/**
 * A guided walkthrough for registering an application on the HMRC Developer Hub to obtain API
 * credentials. Renders the ordered steps from {@link HmrcRegistrationGuide} as a tickable checklist,
 * offers a copy button for each value HMRC asks the user to enter (the suggested app name and the
 * redirect URI), and opens the Developer Hub in the user's browser.
 *
 * <p>Built with the app's shared dialog recipe (transparent stage, gradient header, rounded corners
 * and drop shadow via {@link DialogStyler}) so it matches the other dialogs rather than rendering as
 * a bare OS window.
 */
public final class HmrcRegistrationGuideDialog {

    private static final String STYLESHEET = "/css/help.css";
    private static final int DIALOG_WIDTH = 600;

    private final HmrcRegistrationGuide guide;
    private final Consumer<String> browserOpener;

    HmrcRegistrationGuideDialog(HmrcRegistrationGuide guide, Consumer<String> browserOpener) {
        this.guide = guide;
        this.browserOpener = browserOpener;
    }

    /**
     * Shows the guide modally.
     *
     * @param owner the owning window, or null
     * @param guide the walkthrough content
     */
    public static void show(Window owner, HmrcRegistrationGuide guide) {
        new HmrcRegistrationGuideDialog(guide, BrowserUtil::openUrl).showModal(owner);
    }

    private void showModal(Window owner) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT); // must be first, before modality/owner
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Register your app with HMRC");
        if (owner != null) {
            stage.initOwner(owner);
        }

        VBox container = new VBox(0);
        container.getStyleClass().add("help-dialog-container");
        container.setMinWidth(DIALOG_WIDTH);
        container.setMaxWidth(DIALOG_WIDTH);
        container.getChildren().addAll(createHeader(stage), createBody(owner), createFooter(stage));

        DialogStyler.setupFullyStyledDialog(stage, container, STYLESHEET, DialogStyler.CORNER_RADIUS);

        stage.getScene().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });
        stage.showAndWait();
    }

    private HBox createHeader(Stage stage) {
        HBox header = new HBox(12);
        header.getStyleClass().addAll("help-dialog-header", "shell-dialog-header", "shell-dialog-header-info");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));

        StackPane iconWrapper = new StackPane();
        iconWrapper.getStyleClass().add("shell-dialog-icon-wrapper");
        iconWrapper.setMinSize(40, 40);
        iconWrapper.setMaxSize(40, 40);
        iconWrapper.setAlignment(Pos.CENTER);
        FontIcon icon = FontIcon.of(FontAwesomeSolid.KEY, 18);
        icon.setIconColor(Color.WHITE);
        iconWrapper.getChildren().add(icon);

        Label title = new Label("Register your app with HMRC");
        title.getStyleClass().add("shell-dialog-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button("✕");
        close.getStyleClass().add("help-dialog-close");
        close.setOnAction(e -> stage.close());

        header.getChildren().addAll(iconWrapper, title, spacer, close);
        return header;
    }

    private ScrollPane createBody(Window owner) {
        VBox body = new VBox(16);
        body.getStyleClass().addAll("help-dialog-body", "shell-dialog-body");
        body.setPadding(new Insets(20, 24, 20, 24));
        body.getChildren().add(intro());
        body.getChildren().add(copyValuesSection());
        body.getChildren().add(openHubButton());
        body.getChildren().add(stepsSection());
        body.getChildren().add(productionNote());

        ScrollPane scroll = new ScrollPane(body);
        scroll.getStyleClass().add("help-scroll-pane");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // Cap against the screen the dialog opens on (not always the primary), so the footer stays
        // on-screen on a multi-monitor setup.
        double screenHeight = DialogBounds.visualBoundsForOwner(owner).getHeight();
        scroll.setMaxHeight(Math.min(520, screenHeight * 0.65));
        return scroll;
    }

    private Label intro() {
        Label intro = new Label("Each user registers their own free HMRC application to get API "
            + "credentials. Follow these steps, then paste your Client ID and Secret below.");
        intro.setWrapText(true);
        intro.setMaxWidth(Double.MAX_VALUE);
        intro.getStyleClass().add("shell-guide-intro");
        return intro;
    }

    private VBox copyValuesSection() {
        VBox box = new VBox(8);
        for (HmrcRegistrationGuide.CopyValue value : guide.copyValues()) {
            box.getChildren().add(copyRow(value));
        }
        return box;
    }

    private HBox copyRow(HmrcRegistrationGuide.CopyValue value) {
        Label label = new Label(value.label());
        label.getStyleClass().add("guide-copy-label");
        label.setMinWidth(120);

        TextField field = new TextField(value.value());
        field.getStyleClass().add("guide-copy-field");
        field.setEditable(false);
        field.setAccessibleText(value.label() + ": " + value.value());
        HBox.setHgrow(field, Priority.ALWAYS);

        Button copy = new Button("Copy");
        copy.getStyleClass().add("help-btn-secondary");
        copy.setMinWidth(72);
        copy.setOnAction(e -> ClipboardUtil.copyWithFeedback(copy, value.value()));

        HBox row = new HBox(8, label, field, copy);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button openHubButton() {
        Button open = new Button("Open HMRC Developer Hub");
        open.getStyleClass().add("help-btn-secondary");
        FontIcon icon = FontIcon.of(FontAwesomeSolid.EXTERNAL_LINK_ALT, 12);
        icon.setIconColor(Color.web("#495057"));
        open.setGraphic(icon);
        open.setGraphicTextGap(8);
        open.setOnAction(e -> browserOpener.accept(guide.developerHubUrl()));
        return open;
    }

    private VBox stepsSection() {
        VBox box = new VBox(0);
        for (HmrcRegistrationGuide.Step step : guide.steps()) {
            CheckBox check = new CheckBox(step.number() + ". " + step.title());
            check.getStyleClass().add("guide-step-check");
            check.setAccessibleHelp(step.detail());

            Label detail = new Label(step.detail());
            detail.getStyleClass().add("guide-step-detail");
            detail.setWrapText(true);
            detail.setMaxWidth(Double.MAX_VALUE);
            detail.setPadding(new Insets(0, 0, 0, 26));

            VBox item = new VBox(4, check, detail);
            item.getStyleClass().add("guide-step");
            box.getChildren().add(item);
        }
        return box;
    }

    private Label productionNote() {
        Label note = new Label(guide.productionNote());
        note.getStyleClass().add("guide-production-note");
        note.setWrapText(true);
        note.setMaxWidth(Double.MAX_VALUE);
        return note;
    }

    private HBox createFooter(Stage stage) {
        Button done = new Button("Done");
        done.getStyleClass().add("help-btn-primary");
        done.setDefaultButton(true);
        done.setOnAction(e -> stage.close());

        HBox footer = new HBox(12, done);
        footer.getStyleClass().add("help-dialog-buttons");
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 20, 20, 20));
        return footer;
    }
}
