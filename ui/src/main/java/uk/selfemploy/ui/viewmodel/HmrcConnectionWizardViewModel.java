package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import uk.selfemploy.common.validation.HmrcIdentifierValidator;
import uk.selfemploy.ui.service.OAuthConnectionHandler.ConnectionStatus;
import uk.selfemploy.ui.service.OAuthConnectionHandler.OAuthResult;

/**
 * ViewModel for the HMRC Connection Wizard.
 * Sprint 12 - SE-12-001: Prerequisites Checklist Screen
 *
 * <p>Manages the wizard state including current step, navigation,
 * validation, and observable properties for UI binding.</p>
 *
 * <h2>Wizard Steps</h2>
 * <ol>
 *   <li>Prerequisites Checklist (SE-12-001)</li>
 *   <li>Enter Credentials (future)</li>
 *   <li>OAuth Authorization (future)</li>
 *   <li>Confirmation (future)</li>
 *   <li>Complete (future)</li>
 * </ol>
 */
public class HmrcConnectionWizardViewModel {

    /** Total number of wizard steps */
    public static final int TOTAL_STEPS = 5;

    private final IntegerProperty currentStep = new SimpleIntegerProperty(1);
    private final BooleanProperty canProceed = new SimpleBooleanProperty(true);
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final BooleanProperty cancelled = new SimpleBooleanProperty(false);

    // Step 2: NINO entry (SE-12-002)
    private final StringProperty nino = new SimpleStringProperty("");

    // Step 4: OAuth Connection (SE-12-004)
    private final ObjectProperty<ConnectionStatus> connectionStatus = new SimpleObjectProperty<>(null);
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final BooleanProperty connecting = new SimpleBooleanProperty(false);
    private final BooleanProperty connectionSuccessful = new SimpleBooleanProperty(false);
    private final StringProperty connectionErrorCode = new SimpleStringProperty(null);
    private final StringProperty connectionErrorMessage = new SimpleStringProperty(null);

    /**
     * Creates a new wizard view model starting at step 1.
     */
    public HmrcConnectionWizardViewModel() {
        // Initial state set by property defaults
    }

    // === Navigation Methods ===

    /**
     * Advances to the next step if not at the last step.
     */
    public void goNext() {
        if (currentStep.get() < TOTAL_STEPS) {
            currentStep.set(currentStep.get() + 1);
        }
    }

    /**
     * Returns to the previous step if not at the first step.
     */
    public void goBack() {
        if (currentStep.get() > 1) {
            currentStep.set(currentStep.get() - 1);
        }
    }

    /**
     * Marks the wizard as cancelled.
     */
    public void cancel() {
        cancelled.set(true);
    }

    // === Step State Methods ===

    /**
     * Returns the current step number (1-5).
     */
    public int getCurrentStep() {
        return currentStep.get();
    }

    /**
     * Checks if a step is the currently active step.
     *
     * @param step the step number to check
     * @return true if this is the current step
     */
    public boolean isStepActive(int step) {
        return step == currentStep.get();
    }

    /**
     * Checks if a step is pending (not yet reached).
     *
     * @param step the step number to check
     * @return true if this step is ahead of current step
     */
    public boolean isStepPending(int step) {
        return step > currentStep.get();
    }

    /**
     * Checks if a step has been completed.
     *
     * @param step the step number to check
     * @return true if this step is before the current step
     */
    public boolean isStepCompleted(int step) {
        return step < currentStep.get();
    }

    /**
     * Checks if at the first step.
     *
     * @return true if current step is 1
     */
    public boolean isFirstStep() {
        return currentStep.get() == 1;
    }

    /**
     * Checks if at the last step.
     *
     * @return true if current step is TOTAL_STEPS
     */
    public boolean isLastStep() {
        return currentStep.get() == TOTAL_STEPS;
    }

    // === Display Methods ===

    /**
     * Returns the formatted step label (e.g., "Step 1 of 5").
     *
     * @return the step label string
     */
    public String getStepLabel() {
        return String.format("Step %d of %d", currentStep.get(), TOTAL_STEPS);
    }

    /**
     * Returns the text for the next/proceed button based on current step.
     *
     * @return the button text
     */
    public String getNextButtonText() {
        if (currentStep.get() == 1) {
            return "Get Started";
        } else if (currentStep.get() == TOTAL_STEPS) {
            return "Done";
        }
        return "Next";
    }

    /**
     * Returns the text for the cancel button.
     *
     * @return the cancel button text
     */
    public String getCancelButtonText() {
        return "Cancel";
    }

    // === Validation Methods ===

    /**
     * Checks if the user can proceed to the next step.
     *
     * @return true if all validations pass for current step
     */
    public boolean canProceed() {
        return canProceed.get();
    }

    /**
     * Sets whether the user can proceed to the next step.
     *
     * @param value true to enable proceed, false to disable
     */
    public void setCanProceed(boolean value) {
        canProceed.set(value);
    }

    /**
     * Checks if the wizard has been cancelled.
     *
     * @return true if cancelled
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    // === Error Message Methods ===

    /**
     * Returns the current error message.
     *
     * @return the error message, or empty string if none
     */
    public String getErrorMessage() {
        String msg = errorMessage.get();
        return msg == null ? "" : msg;
    }

    /**
     * Sets an error message to display.
     *
     * @param message the error message, or null to clear
     */
    public void setErrorMessage(String message) {
        errorMessage.set(message == null ? "" : message);
    }

    /**
     * Clears any displayed error message.
     */
    public void clearErrorMessage() {
        errorMessage.set("");
    }

    // === Observable Properties ===

    /**
     * Returns the current step property for binding.
     *
     * @return the currentStep property
     */
    public IntegerProperty currentStepProperty() {
        return currentStep;
    }

    /**
     * Returns the canProceed property for binding.
     *
     * @return the canProceed property
     */
    public BooleanProperty canProceedProperty() {
        return canProceed;
    }

    /**
     * Returns the error message property for binding.
     *
     * @return the errorMessage property
     */
    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    /**
     * Returns the cancelled property for binding.
     *
     * @return the cancelled property
     */
    public BooleanProperty cancelledProperty() {
        return cancelled;
    }

    // === Step 2: NINO Entry Methods (SE-12-002) ===

    /**
     * Returns the NINO value.
     *
     * @return the NINO, or empty string if not set
     */
    public String getNino() {
        String value = nino.get();
        return value == null ? "" : value;
    }

    /**
     * Sets the NINO value.
     *
     * @param value the NINO to set, or null to clear
     */
    public void setNino(String value) {
        nino.set(value == null ? "" : value);
    }

    /**
     * Returns the NINO property for binding.
     *
     * @return the nino property
     */
    public StringProperty ninoProperty() {
        return nino;
    }

    /**
     * Checks if the current NINO is valid according to HMRC rules.
     *
     * @return true if valid, false otherwise
     */
    public boolean isNinoValid() {
        String value = getNino();
        if (value.isEmpty()) {
            return false;
        }
        return HmrcIdentifierValidator.isValidNino(value);
    }

    // === Step 4: OAuth Connection Methods (SE-12-004) ===

    /**
     * Returns the current connection status.
     *
     * @return the connection status, or null if no connection attempted
     */
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus.get();
    }

    /**
     * Updates the connection status and status message.
     *
     * @param status the new connection status
     */
    public void setConnectionStatus(ConnectionStatus status) {
        connectionStatus.set(status);
        if (status != null) {
            statusMessage.set(status.getDisplayMessage());
        } else {
            statusMessage.set("");
        }
    }

    /**
     * Returns the connection status property for binding.
     *
     * @return the connectionStatus property
     */
    public ObjectProperty<ConnectionStatus> connectionStatusProperty() {
        return connectionStatus;
    }

    /**
     * Returns the current status message for display.
     *
     * @return the status message, or empty string if none
     */
    public String getStatusMessage() {
        String msg = statusMessage.get();
        return msg == null ? "" : msg;
    }

    /**
     * Sets the status message directly.
     *
     * @param message the status message to display
     */
    public void setStatusMessage(String message) {
        statusMessage.set(message == null ? "" : message);
    }

    /**
     * Returns the status message property for binding.
     *
     * @return the statusMessage property
     */
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    /**
     * Returns whether a connection is currently in progress.
     *
     * @return true if connecting
     */
    public boolean isConnecting() {
        return connecting.get();
    }

    /**
     * Sets whether a connection is in progress.
     *
     * @param value true if connecting
     */
    public void setConnecting(boolean value) {
        connecting.set(value);
    }

    /**
     * Returns the connecting property for binding.
     *
     * @return the connecting property
     */
    public BooleanProperty connectingProperty() {
        return connecting;
    }

    /**
     * Returns whether the connection was successful.
     *
     * @return true if connection succeeded
     */
    public boolean isConnectionSuccessful() {
        return connectionSuccessful.get();
    }

    /**
     * Sets whether the connection was successful.
     *
     * @param value true if connection succeeded
     */
    public void setConnectionSuccessful(boolean value) {
        connectionSuccessful.set(value);
    }

    /**
     * Returns the connectionSuccessful property for binding.
     *
     * @return the connectionSuccessful property
     */
    public BooleanProperty connectionSuccessfulProperty() {
        return connectionSuccessful;
    }

    /**
     * Returns the connection error code if the connection failed.
     *
     * @return the error code, or null if no error
     */
    public String getConnectionErrorCode() {
        return connectionErrorCode.get();
    }

    /**
     * Returns the connection error message if the connection failed.
     *
     * @return the error message, or null if no error
     */
    public String getConnectionErrorMessage() {
        return connectionErrorMessage.get();
    }

    /**
     * Returns the connectionErrorCode property for binding.
     *
     * @return the connectionErrorCode property
     */
    public StringProperty connectionErrorCodeProperty() {
        return connectionErrorCode;
    }

    /**
     * Returns the connectionErrorMessage property for binding.
     *
     * @return the connectionErrorMessage property
     */
    public StringProperty connectionErrorMessageProperty() {
        return connectionErrorMessage;
    }

    /**
     * Handles an OAuth connection result.
     *
     * @param result the result from the OAuth handler
     */
    public void handleOAuthResult(OAuthResult result) {
        setConnecting(false);

        if (result.success()) {
            connectionSuccessful.set(true);
            connectionErrorCode.set(null);
            connectionErrorMessage.set(null);
        } else {
            connectionSuccessful.set(false);
            connectionErrorCode.set(result.errorCode());
            connectionErrorMessage.set(result.errorMessage());
        }
    }

    /**
     * Resets the connection state for retry.
     */
    public void resetConnectionState() {
        connectionStatus.set(null);
        statusMessage.set("");
        connecting.set(false);
        connectionSuccessful.set(false);
        connectionErrorCode.set(null);
        connectionErrorMessage.set(null);
    }

    /**
     * Checks if the connection is in an error state.
     *
     * @return true if connection failed with error
     */
    public boolean hasConnectionError() {
        ConnectionStatus status = connectionStatus.get();
        return status == ConnectionStatus.ERROR || status == ConnectionStatus.TIMEOUT;
    }

    /**
     * Checks if the connection was cancelled.
     *
     * @return true if connection was cancelled
     */
    public boolean isConnectionCancelled() {
        return connectionStatus.get() == ConnectionStatus.CANCELLED;
    }
}
