package uk.selfemploy.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import uk.selfemploy.ui.service.HmrcRegistrationGuide;
import uk.selfemploy.ui.util.BrowserUtil;
import uk.selfemploy.ui.util.ClipboardUtil;
import uk.selfemploy.ui.util.DialogStyler;

import java.util.function.Consumer;

/**
 * A guided walkthrough for registering an application on the HMRC Developer Hub to obtain API
 * credentials. Renders the ordered steps from {@link HmrcRegistrationGuide} as a tickable checklist,
 * offers a copy button for each value HMRC asks the user to enter (the suggested app name and the
 * redirect URI), and opens the Developer Hub in the user's browser.
 *
 * <p>Replaces the previous static help text with something the user can act on directly.
 */
public final class HmrcRegistrationGuideDialog {

    private static final String STYLESHEET = "/css/help.css";

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
        stage.setTitle("Register your app with HMRC");
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.getChildren().add(header());
        content.getChildren().add(copyValuesSection());
        content.getChildren().add(openHubButton());
        content.getChildren().add(stepsSection());
        content.getChildren().add(productionNote());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox root = new VBox(scroll, footer(stage));
        DialogStyler.setupStyledDialog(stage, root, STYLESHEET);
        stage.setScene(new Scene(root, 560, 640));
        stage.showAndWait();
    }

    private Label header() {
        Label header = new Label("Each user registers their own free HMRC application to get API "
            + "credentials. Follow these steps, then paste your Client ID and Secret below.");
        header.setWrapText(true);
        return header;
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
        label.setMinWidth(120);

        TextField field = new TextField(value.value());
        field.setEditable(false);
        HBox.setHgrow(field, Priority.ALWAYS);

        Button copy = new Button("Copy");
        copy.setOnAction(e -> ClipboardUtil.copyWithFeedback(copy, value.value()));

        HBox row = new HBox(8, label, field, copy);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button openHubButton() {
        Button open = new Button("Open HMRC Developer Hub");
        open.setOnAction(e -> browserOpener.accept(guide.developerHubUrl()));
        return open;
    }

    private VBox stepsSection() {
        VBox box = new VBox(10);
        for (HmrcRegistrationGuide.Step step : guide.steps()) {
            CheckBox check = new CheckBox(step.number() + ". " + step.title());
            check.setStyle("-fx-font-weight: bold;");

            Label detail = new Label(step.detail());
            detail.setWrapText(true);
            detail.setPadding(new Insets(0, 0, 0, 24));

            box.getChildren().addAll(check, detail);
        }
        return box;
    }

    private Label productionNote() {
        Label note = new Label(guide.productionNote());
        note.setWrapText(true);
        note.setPadding(new Insets(8, 0, 0, 0));
        note.setStyle("-fx-font-style: italic;");
        return note;
    }

    private HBox footer(Stage stage) {
        Button close = new Button("Done");
        close.setDefaultButton(true);
        close.setOnAction(e -> stage.close());
        HBox footer = new HBox(close);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 24, 16, 24));
        return footer;
    }
}
