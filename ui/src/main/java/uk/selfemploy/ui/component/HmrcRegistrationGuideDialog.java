package uk.selfemploy.ui.component;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import uk.selfemploy.ui.service.HmrcRegistrationGuide;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A guided walkthrough for registering an application on the HMRC Developer Hub to obtain API
 * credentials. Renders the ordered steps from {@link HmrcRegistrationGuide} as a tickable checklist,
 * offers a copy button for each value HMRC asks the user to enter (the suggested app name and the
 * redirect URI), and opens the Developer Hub in the user's browser.
 *
 * <p>Replaces the previous static help text with something the user can act on directly.
 */
public final class HmrcRegistrationGuideDialog {

    private static final Logger LOG = Logger.getLogger(HmrcRegistrationGuideDialog.class.getName());

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
        new HmrcRegistrationGuideDialog(guide, HmrcRegistrationGuideDialog::openInBrowser).showModal(owner);
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

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);

        VBox root = new VBox(scroll, footer(stage));
        VBox.setVgrow(scroll, Priority.ALWAYS);

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
        copy.setOnAction(e -> copyToClipboard(value.value(), copy));

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

    private HBox footer(Stage stage) {
        Button close = new Button("Done");
        close.setDefaultButton(true);
        close.setOnAction(e -> stage.close());
        HBox footer = new HBox(close);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 24, 16, 24));
        return footer;
    }

    private void copyToClipboard(String value, Button button) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);

        String original = button.getText();
        button.setText("Copied!");
        button.setDisable(true);
        PauseTransition pause = new PauseTransition(Duration.millis(1500));
        pause.setOnFinished(ev -> {
            button.setText(original);
            button.setDisable(false);
        });
        pause.play();
    }

    private static void openInBrowser(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported()
                && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            } else {
                LOG.warning("Cannot open a browser on this platform; the Developer Hub URL is " + url);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to open the HMRC Developer Hub in a browser", e);
        }
    }
}
