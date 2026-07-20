package uk.selfemploy.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import uk.selfemploy.ui.util.DialogStyler;

import java.util.Objects;

/**
 * A styled, always-visible replacement for {@link javafx.scene.control.Alert}.
 *
 * <p>The standard JavaFX {@code Alert} renders invisible while modally blocking the application
 * under XWayland/mutter (the FX thread parks in {@code showAndWait()} with nothing on screen).
 * This component reuses the same {@link Stage}-based recipe as the app's other working dialogs
 * (transparent style, application-modal, owned by the focused window, styled via
 * {@link DialogStyler}), which the window manager treats as a normal top-level window and renders
 * reliably.</p>
 *
 * <p>Use the static entry points: {@link #info}, {@link #warning}, {@link #error} for
 * message-only dialogs, and {@link #confirm} for a yes/no decision. All must be called on the
 * JavaFX Application Thread (as the {@code Alert} calls they replace already were).</p>
 */
public final class AppDialog {

    private static final String STYLESHEET = "/css/help.css";
    private static final int DIALOG_WIDTH = 440;
    private static final double CORNER_RADIUS = 12.0;

    /** Dialog kind — drives the header colour, icon and default buttons. */
    public enum Kind { INFO, WARNING, ERROR, CONFIRM }

    private static final String[] INFO_GRADIENT = {"#0066cc", "#3385d6"};
    private static final String[] WARNING_GRADIENT = {"#d97706", "#f59e0b"};
    private static final String[] ERROR_GRADIENT = {"#dc3545", "#e4606d"};
    private static final String[] CONFIRM_GRADIENT = {"#0066cc", "#3385d6"};

    private final Stage stage;
    private boolean confirmed;

    // Package-private (not private) so component tests can construct a dialog and inspect or
    // drive its stage without the blocking showAndWait() the public API uses.
    AppDialog(Kind kind, String title, String message, String confirmLabel, String cancelLabel) {
        this.stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT); // must be set before modality/owner
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title == null ? "" : title);

        Window owner = ownerWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }

        VBox container = new VBox(0);
        container.getStyleClass().add("help-dialog-container");
        container.setMinWidth(DIALOG_WIDTH);
        container.setMaxWidth(DIALOG_WIDTH);
        container.getChildren().addAll(
                createHeader(kind, title),
                createBody(message),
                createFooter(kind, confirmLabel, cancelLabel));

        DialogStyler.applyRoundedClip(container, CORNER_RADIUS);
        StackPane wrapper = DialogStyler.createShadowWrapper(container);
        DialogStyler.setupStyledDialog(stage, wrapper, STYLESHEET);
        DialogStyler.centerOnOwner(stage);

        // Escape always closes (cancels, for confirm dialogs).
        stage.getScene().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                confirmed = false;
                stage.close();
            }
        });
    }

    // ==================== Public API ====================

    /** Shows an information dialog and blocks until dismissed. */
    public static void info(String title, String message) {
        new AppDialog(Kind.INFO, title, message, "OK", null).stage.showAndWait();
    }

    /** Shows a warning dialog and blocks until dismissed. */
    public static void warning(String title, String message) {
        new AppDialog(Kind.WARNING, title, message, "OK", null).stage.showAndWait();
    }

    /** Shows an error dialog and blocks until dismissed. */
    public static void error(String title, String message) {
        new AppDialog(Kind.ERROR, title, message, "OK", null).stage.showAndWait();
    }

    /**
     * Shows a confirmation dialog with OK / Cancel and blocks until dismissed.
     *
     * @return {@code true} if the user confirmed, {@code false} if cancelled or dismissed
     */
    public static boolean confirm(String title, String message) {
        return confirm(title, message, "OK", "Cancel");
    }

    /**
     * Shows a confirmation dialog with custom button labels and blocks until dismissed.
     *
     * @param confirmLabel label for the affirmative button
     * @param cancelLabel  label for the dismissive button
     * @return {@code true} if the user chose the affirmative button, {@code false} otherwise
     */
    public static boolean confirm(String title, String message, String confirmLabel, String cancelLabel) {
        AppDialog dialog = new AppDialog(Kind.CONFIRM, title, message,
                Objects.requireNonNull(confirmLabel), Objects.requireNonNull(cancelLabel));
        dialog.stage.showAndWait();
        return dialog.confirmed;
    }

    /** Package-private accessor for tests to show/inspect the stage without blocking. */
    Stage stageForTest() {
        return stage;
    }

    /** Package-private accessor for tests to read the confirmation result. */
    boolean isConfirmed() {
        return confirmed;
    }

    // ==================== Testable pure helpers ====================

    /** Header gradient [start, end] hex colours for a kind. */
    public static String[] gradientFor(Kind kind) {
        return switch (kind) {
            case INFO -> INFO_GRADIENT;
            case WARNING -> WARNING_GRADIENT;
            case ERROR -> ERROR_GRADIENT;
            case CONFIRM -> CONFIRM_GRADIENT;
        };
    }

    /** Header icon for a kind. */
    public static Ikon iconFor(Kind kind) {
        return switch (kind) {
            case INFO -> FontAwesomeSolid.INFO_CIRCLE;
            case WARNING -> FontAwesomeSolid.EXCLAMATION_TRIANGLE;
            case ERROR -> FontAwesomeSolid.TIMES_CIRCLE;
            case CONFIRM -> FontAwesomeSolid.QUESTION_CIRCLE;
        };
    }

    /**
     * SCSS style class carrying the header gradient for a kind. {@code CONFIRM} shares the
     * {@code INFO} gradient.
     *
     * @param kind the dialog kind
     * @return the {@code shell-dialog-header-*} style class name
     */
    public static String headerStyleClass(Kind kind) {
        return switch (kind) {
            case INFO, CONFIRM -> "shell-dialog-header-info";
            case WARNING -> "shell-dialog-header-warning";
            case ERROR -> "shell-dialog-header-error";
        };
    }

    // ==================== Private UI construction ====================

    private static Window ownerWindow() {
        Window focused = Window.getWindows().stream()
                .filter(Window::isFocused)
                .findFirst()
                .orElse(null);
        if (focused != null) {
            return focused;
        }
        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .findFirst()
                .orElse(null);
    }

    private HBox createHeader(Kind kind, String title) {
        HBox header = new HBox(12);
        header.getStyleClass().addAll("help-dialog-header", "shell-dialog-header", headerStyleClass(kind));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));

        StackPane iconWrapper = new StackPane();
        iconWrapper.getStyleClass().add("shell-dialog-icon-wrapper");
        iconWrapper.setMinSize(40, 40);
        iconWrapper.setMaxSize(40, 40);
        iconWrapper.setAlignment(Pos.CENTER);
        FontIcon icon = FontIcon.of(iconFor(kind), 18);
        icon.setIconColor(Color.WHITE);
        iconWrapper.getChildren().add(icon);

        Label titleLabel = new Label(title == null ? "" : title);
        titleLabel.getStyleClass().add("shell-dialog-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("help-dialog-close");
        closeBtn.setOnAction(e -> {
            confirmed = false;
            stage.close();
        });

        header.getChildren().addAll(iconWrapper, titleLabel, spacer, closeBtn);
        return header;
    }

    private VBox createBody(String message) {
        VBox body = new VBox(0);
        body.getStyleClass().addAll("help-dialog-body", "shell-dialog-body");
        body.setPadding(new Insets(20));

        Label messageLabel = new Label(message == null ? "" : message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("shell-dialog-message");
        body.getChildren().add(messageLabel);
        return body;
    }

    private HBox createFooter(Kind kind, String confirmLabel, String cancelLabel) {
        HBox footer = new HBox(12);
        footer.getStyleClass().add("help-dialog-buttons");
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 20, 20, 20));

        if (kind == Kind.CONFIRM) {
            Button cancelBtn = new Button(cancelLabel);
            cancelBtn.getStyleClass().add("help-btn-secondary");
            cancelBtn.setCancelButton(true);
            cancelBtn.setOnAction(e -> {
                confirmed = false;
                stage.close();
            });
            footer.getChildren().add(cancelBtn);
        }

        Button confirmBtn = new Button(kind == Kind.CONFIRM ? confirmLabel : "OK");
        confirmBtn.getStyleClass().add("help-btn-primary");
        confirmBtn.setDefaultButton(true);
        confirmBtn.setOnAction(e -> {
            confirmed = true;
            stage.close();
        });
        footer.getChildren().add(confirmBtn);
        return footer;
    }
}
