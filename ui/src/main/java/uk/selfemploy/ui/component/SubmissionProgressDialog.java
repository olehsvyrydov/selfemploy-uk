package uk.selfemploy.ui.component;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import uk.selfemploy.ui.service.AutoOAuthSubmissionService.SubmissionProgress;
import uk.selfemploy.ui.service.AutoOAuthSubmissionService.SubmissionStage;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Progress dialog shown during HMRC submission with automatic OAuth.
 *
 * <p>Displays the current stage of the submission process:</p>
 * <ul>
 *   <li>VALIDATING - Checking prerequisites</li>
 *   <li>AUTHENTICATING - OAuth flow in progress (browser opened)</li>
 *   <li>SUBMITTING - Sending data to HMRC</li>
 *   <li>COMPLETE - Submission successful</li>
 *   <li>FAILED - Submission failed with error</li>
 * </ul>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Animated progress indicator</li>
 *   <li>Stage-specific messages</li>
 *   <li>Cancel button for OAuth flow</li>
 *   <li>Accessible labels for screen readers</li>
 *   <li>Modal dialog blocking parent window</li>
 * </ul>
 *
 * @since Sprint 13
 */
public class SubmissionProgressDialog {

    private static final Logger LOG = Logger.getLogger(SubmissionProgressDialog.class.getName());

    private static final double DIALOG_WIDTH = 400;
    private static final double DIALOG_HEIGHT = 200;

    private final Stage stage;
    private final Label titleLabel;
    private final Label messageLabel;
    private final ProgressIndicator progressIndicator;
    private final Button cancelButton;
    private final Button closeButton;

    private Consumer<Void> onCancel;
    private SubmissionStage currentStage = SubmissionStage.VALIDATING;

    /**
     * Creates a new submission progress dialog.
     *
     * @param owner the owner window (for modality)
     */
    public SubmissionProgressDialog(Window owner) {
        stage = new Stage();
        stage.initStyle(StageStyle.UTILITY);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("Submitting to HMRC");
        stage.setResizable(false);
        stage.setWidth(DIALOG_WIDTH);
        stage.setHeight(DIALOG_HEIGHT);

        // Create components
        titleLabel = new Label("Submitting to HMRC");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        titleLabel.setAccessibleRole(AccessibleRole.TEXT);

        messageLabel = new Label(SubmissionStage.VALIDATING.getDefaultMessage());
        messageLabel.setStyle("-fx-font-size: 13px;");
        messageLabel.setWrapText(true);
        messageLabel.setAccessibleRole(AccessibleRole.TEXT);

        progressIndicator = new ProgressIndicator(-1); // Indeterminate
        progressIndicator.setPrefSize(50, 50);
        progressIndicator.setAccessibleText("Submission in progress");

        cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> handleCancel());
        cancelButton.setAccessibleText("Cancel submission");

        closeButton = new Button("Close");
        closeButton.setOnAction(e -> stage.close());
        closeButton.setVisible(false);
        closeButton.setManaged(false);

        // Layout
        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));

        HBox progressRow = new HBox(16);
        progressRow.setAlignment(Pos.CENTER);
        progressRow.getChildren().addAll(progressIndicator, messageLabel);
        HBox.setHgrow(messageLabel, javafx.scene.layout.Priority.ALWAYS);

        HBox buttonRow = new HBox(12);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.getChildren().addAll(cancelButton, closeButton);

        content.getChildren().addAll(titleLabel, progressRow, buttonRow);

        Scene scene = new Scene(content);
        stage.setScene(scene);

        // Handle close request
        stage.setOnCloseRequest(e -> {
            if (currentStage != SubmissionStage.COMPLETE && currentStage != SubmissionStage.FAILED) {
                e.consume(); // Prevent close during active submission
                handleCancel();
            }
        });
    }

    /**
     * Shows the dialog.
     */
    public void show() {
        LOG.info("Showing submission progress dialog");
        stage.show();
        stage.centerOnScreen();
    }

    /**
     * Hides the dialog.
     */
    public void hide() {
        LOG.info("Hiding submission progress dialog");
        if (stage.isShowing()) {
            stage.hide();
        }
    }

    /**
     * Updates the progress display.
     *
     * @param progress the current progress
     */
    public void updateProgress(SubmissionProgress progress) {
        runOnFxThread(() -> {
            currentStage = progress.stage();
            messageLabel.setText(progress.message());
            messageLabel.setAccessibleText(progress.message());

            switch (progress.stage()) {
                case AUTHENTICATING -> {
                    titleLabel.setText("Authenticating with HMRC");
                    cancelButton.setDisable(false);
                    cancelButton.setVisible(true);
                    cancelButton.setManaged(true);
                }
                case SUBMITTING -> {
                    titleLabel.setText("Submitting to HMRC");
                    // Can't cancel once submission starts
                    cancelButton.setDisable(true);
                }
                case COMPLETE -> {
                    titleLabel.setText("Submission Complete");
                    progressIndicator.setProgress(1.0);
                    progressIndicator.setStyle("-fx-accent: green;");
                    cancelButton.setVisible(false);
                    cancelButton.setManaged(false);
                    closeButton.setVisible(true);
                    closeButton.setManaged(true);
                }
                case FAILED -> {
                    titleLabel.setText("Submission Failed");
                    progressIndicator.setProgress(0);
                    progressIndicator.setStyle("-fx-accent: red;");
                    cancelButton.setVisible(false);
                    cancelButton.setManaged(false);
                    closeButton.setVisible(true);
                    closeButton.setManaged(true);
                }
                default -> {
                    // VALIDATING
                    titleLabel.setText("Validating...");
                    cancelButton.setDisable(false);
                }
            }
        });
    }

    /**
     * Sets the cancel callback.
     *
     * @param onCancel callback when cancel is clicked
     */
    public void setOnCancel(Consumer<Void> onCancel) {
        this.onCancel = onCancel;
    }

    /**
     * Handles cancel button click.
     */
    private void handleCancel() {
        LOG.info("Cancel requested");
        if (onCancel != null) {
            onCancel.accept(null);
        }
        // Don't close the dialog - let the service handle that
        cancelButton.setDisable(true);
        cancelButton.setText("Cancelling...");
    }

    /**
     * Closes the dialog.
     */
    public void close() {
        runOnFxThread(() -> {
            if (stage.isShowing()) {
                stage.close();
            }
        });
    }

    /**
     * Returns whether the dialog is showing.
     *
     * @return true if showing
     */
    public boolean isShowing() {
        return stage.isShowing();
    }

    /**
     * Runs a task on the FX thread.
     */
    private void runOnFxThread(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    /**
     * Returns the current stage.
     * Package-private for testing.
     *
     * @return the current stage
     */
    SubmissionStage getCurrentStage() {
        return currentStage;
    }

    /**
     * Returns the message label text.
     * Package-private for testing.
     *
     * @return the message text
     */
    String getMessageText() {
        return messageLabel.getText();
    }
}
