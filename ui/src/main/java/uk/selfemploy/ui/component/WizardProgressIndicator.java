package uk.selfemploy.ui.component;

import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * A reusable wizard progress indicator showing step circles and connectors.
 * Sprint 12 - SE-12-001: Prerequisites Checklist Screen
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Visual step circles with numbers</li>
 *   <li>Connector lines between steps</li>
 *   <li>Active, pending, and completed states</li>
 *   <li>Accessible role and text for screen readers</li>
 *   <li>Focus traversable for keyboard navigation</li>
 * </ul>
 *
 * <h2>CSS Classes</h2>
 * <ul>
 *   <li>.wizard-progress-indicator - Container</li>
 *   <li>.wizard-step-circle - Individual step circle</li>
 *   <li>.wizard-step-circle.active - Current step</li>
 *   <li>.wizard-step-circle.pending - Future steps</li>
 *   <li>.wizard-step-circle.completed - Past steps</li>
 *   <li>.wizard-step-connector - Line between steps</li>
 *   <li>.wizard-step-connector.completed - Completed connector</li>
 * </ul>
 */
public class WizardProgressIndicator extends HBox {

    private static final String STYLE_CLASS = "wizard-progress-indicator";
    private static final String STEP_CIRCLE_CLASS = "wizard-step-circle";
    private static final String CONNECTOR_CLASS = "wizard-step-connector";
    private static final String ACTIVE_CLASS = "active";
    private static final String PENDING_CLASS = "pending";
    private static final String COMPLETED_CLASS = "completed";

    private final int totalSteps;
    private int currentStep;
    private final List<Label> stepCircles = new ArrayList<>();
    private final List<Region> connectors = new ArrayList<>();

    /**
     * Creates a new wizard progress indicator.
     *
     * @param totalSteps the total number of steps in the wizard
     * @param currentStep the current step (1-indexed)
     */
    public WizardProgressIndicator(int totalSteps, int currentStep) {
        super(0); // No spacing - connectors handle visual separation
        this.totalSteps = Math.max(1, totalSteps);
        this.currentStep = clampStep(currentStep);

        setAlignment(Pos.CENTER);
        getStyleClass().add(STYLE_CLASS);
        setAccessibleRole(AccessibleRole.TOOL_BAR);
        setFocusTraversable(true);

        buildIndicator();
        updateStates();
        updateAccessibleText();
    }

    /**
     * Builds the step circles and connectors.
     */
    private void buildIndicator() {
        for (int i = 1; i <= totalSteps; i++) {
            // Create step circle
            Label stepCircle = new Label(String.valueOf(i));
            stepCircle.getStyleClass().add(STEP_CIRCLE_CLASS);
            stepCircle.setAccessibleRole(AccessibleRole.TEXT);
            stepCircles.add(stepCircle);
            getChildren().add(stepCircle);

            // Create connector (except after last step)
            if (i < totalSteps) {
                Region connector = new Region();
                connector.getStyleClass().add(CONNECTOR_CLASS);
                connectors.add(connector);
                getChildren().add(connector);
            }
        }
    }

    /**
     * Updates the visual state of all step circles and connectors.
     */
    private void updateStates() {
        for (int i = 0; i < stepCircles.size(); i++) {
            Label circle = stepCircles.get(i);
            int stepNum = i + 1;

            // Remove all state classes
            circle.getStyleClass().removeAll(ACTIVE_CLASS, PENDING_CLASS, COMPLETED_CLASS);

            // Add appropriate state class
            if (stepNum < currentStep) {
                circle.getStyleClass().add(COMPLETED_CLASS);
                circle.setText("\u2713"); // Checkmark for completed
            } else if (stepNum == currentStep) {
                circle.getStyleClass().add(ACTIVE_CLASS);
                circle.setText(String.valueOf(stepNum));
            } else {
                circle.getStyleClass().add(PENDING_CLASS);
                circle.setText(String.valueOf(stepNum));
            }
        }

        // Update connectors
        for (int i = 0; i < connectors.size(); i++) {
            Region connector = connectors.get(i);
            int stepAfterConnector = i + 2;

            connector.getStyleClass().removeAll(COMPLETED_CLASS);
            if (stepAfterConnector <= currentStep) {
                connector.getStyleClass().add(COMPLETED_CLASS);
            }
        }
    }

    /**
     * Updates the accessible text for screen readers.
     */
    private void updateAccessibleText() {
        setAccessibleText(String.format("Step %d of %d", currentStep, totalSteps));
    }

    /**
     * Clamps a step number to valid range.
     */
    private int clampStep(int step) {
        return Math.max(1, Math.min(step, totalSteps));
    }

    // === Public API ===

    /**
     * Returns the total number of steps.
     *
     * @return total steps
     */
    public int getTotalSteps() {
        return totalSteps;
    }

    /**
     * Returns the current step number.
     *
     * @return current step (1-indexed)
     */
    public int getCurrentStep() {
        return currentStep;
    }

    /**
     * Sets the current step and updates the visual state.
     *
     * @param step the new current step
     */
    public void setCurrentStep(int step) {
        this.currentStep = clampStep(step);
        updateStates();
        updateAccessibleText();
    }

    /**
     * Checks if a step is currently active.
     *
     * @param step the step to check
     * @return true if active
     */
    public boolean isStepActive(int step) {
        return step == currentStep;
    }

    /**
     * Checks if a step is pending (future).
     *
     * @param step the step to check
     * @return true if pending
     */
    public boolean isStepPending(int step) {
        return step > currentStep;
    }

    /**
     * Checks if a step is completed.
     *
     * @param step the step to check
     * @return true if completed
     */
    public boolean isStepCompleted(int step) {
        return step < currentStep;
    }

    /**
     * Returns the number of connectors between steps.
     *
     * @return connector count (totalSteps - 1)
     */
    public int getConnectorCount() {
        return connectors.size();
    }
}
