package uk.selfemploy.ui.controller;

import javafx.stage.Stage;

/**
 * A controller that needs a handle on the window it was loaded into, so it can close itself.
 *
 * <p>FXML controllers are instantiated by the loader, which cannot pass the stage — the caller creates
 * it afterwards. This lets a caller wire up any such controller without knowing its concrete type.
 */
public interface DialogStageAware {

    /** Gives the controller the stage it is displayed in. */
    void setDialogStage(Stage dialogStage);
}
