package uk.selfemploy.common.domain;

/**
 * Represents the state of an annual Self Assessment submission saga.
 *
 * <p>State transitions follow this flow:
 * <pre>
 * INITIATED → CALCULATING → CALCULATED → DECLARING → COMPLETED
 *                ↓             ↓            ↓
 *              FAILED       FAILED       FAILED
 * </pre>
 *
 * <p>The saga can be resumed from any non-terminal state (INITIATED, CALCULATING, CALCULATED, DECLARING).
 */
public enum AnnualSubmissionState {
    /**
     * Initial state - saga has been created but no API calls have been made.
     * Next step: Trigger calculation via HMRC API.
     */
    INITIATED,

    /**
     * Calculation has been triggered, waiting for HMRC to complete calculation.
     * Next step: Poll for calculation result.
     */
    CALCULATING,

    /**
     * Tax calculation has been retrieved from HMRC.
     * Next step: User confirms and triggers final declaration.
     */
    CALCULATED,

    /**
     * Final declaration is being submitted to HMRC.
     * Next step: Receive HMRC confirmation.
     */
    DECLARING,

    /**
     * Terminal state - submission completed successfully.
     * HMRC confirmation received and saved.
     */
    COMPLETED,

    /**
     * Terminal state - submission failed and cannot be automatically retried.
     * User intervention required.
     */
    FAILED;

    /**
     * Returns true if this is a terminal state (COMPLETED or FAILED).
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }

    /**
     * Returns true if the saga can be resumed from this state.
     */
    public boolean isResumable() {
        return !isTerminal();
    }
}
