package uk.selfemploy.ui.service;

import java.time.Instant;

/**
 * Represents the persisted state of a wizard for resuming across sessions.
 *
 * <p>This record stores the progress of multi-step wizards (like the HMRC Connection Wizard)
 * so that users can resume from where they left off after closing the application.
 *
 * @param wizardType     The type of wizard (e.g., "hmrc_connection")
 * @param currentStep    The current step number (0-indexed)
 * @param checklistState JSON string containing checklist state, or null if not applicable
 * @param ninoEntered    The National Insurance Number entered, encrypted at rest
 * @param createdAt      When the wizard progress was first created
 * @param updatedAt      When the wizard progress was last updated
 */
public record WizardProgress(
    String wizardType,
    int currentStep,
    String checklistState,
    String ninoEntered,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Wizard type constant for the HMRC Connection Wizard.
     */
    public static final String HMRC_CONNECTION = "hmrc_connection";

    /**
     * Creates a new WizardProgress with validation.
     */
    public WizardProgress {
        if (wizardType == null || wizardType.isBlank()) {
            throw new IllegalArgumentException("Wizard type cannot be null or blank");
        }
        if (currentStep < 0) {
            throw new IllegalArgumentException("Current step cannot be negative");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created timestamp cannot be null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("Updated timestamp cannot be null");
        }
    }

    /**
     * Creates a new WizardProgress for starting a wizard.
     *
     * @param wizardType The type of wizard
     * @param now        The current time
     * @return A new WizardProgress at step 0
     */
    public static WizardProgress start(String wizardType, Instant now) {
        return new WizardProgress(wizardType, 0, null, null, now, now);
    }

    /**
     * Creates a copy of this progress at a different step.
     *
     * @param step The new step
     * @param now  The current time
     * @return A new WizardProgress at the specified step
     */
    public WizardProgress withStep(int step, Instant now) {
        return new WizardProgress(wizardType, step, checklistState, ninoEntered, createdAt, now);
    }

    /**
     * Creates a copy of this progress with updated checklist state.
     *
     * @param state The new checklist state JSON
     * @param now   The current time
     * @return A new WizardProgress with the updated state
     */
    public WizardProgress withChecklistState(String state, Instant now) {
        return new WizardProgress(wizardType, currentStep, state, ninoEntered, createdAt, now);
    }

    /**
     * Creates a copy of this progress with the NINO set.
     *
     * @param nino The National Insurance Number
     * @param now  The current time
     * @return A new WizardProgress with the NINO set
     */
    public WizardProgress withNino(String nino, Instant now) {
        return new WizardProgress(wizardType, currentStep, checklistState, nino, createdAt, now);
    }
}
