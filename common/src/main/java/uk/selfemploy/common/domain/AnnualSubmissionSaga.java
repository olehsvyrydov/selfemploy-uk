package uk.selfemploy.common.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model for the Annual Submission Saga.
 *
 * <p>Tracks the state of a multi-step annual Self Assessment submission process.
 * Supports resume capability by persisting state after each successful step.
 */
public record AnnualSubmissionSaga(
    UUID id,
    String nino,
    TaxYear taxYear,
    AnnualSubmissionState state,
    String calculationId,
    TaxCalculationResult calculationResult,
    String hmrcConfirmation,
    String errorMessage,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Creates a new saga in INITIATED state.
     */
    public static AnnualSubmissionSaga create(TaxYear taxYear, String nino) {
        Instant now = Instant.now();
        return new AnnualSubmissionSaga(
            UUID.randomUUID(),
            nino,
            taxYear,
            AnnualSubmissionState.INITIATED,
            null,
            null,
            null,
            null,
            now,
            now
        );
    }

    /**
     * Transitions to CALCULATING state with the calculationId from HMRC.
     */
    public AnnualSubmissionSaga withCalculating(String calculationId) {
        if (state != AnnualSubmissionState.INITIATED) {
            throw new IllegalStateException("Can only transition to CALCULATING from INITIATED state");
        }
        return new AnnualSubmissionSaga(
            id, nino, taxYear,
            AnnualSubmissionState.CALCULATING,
            calculationId,
            null,
            null,
            null,
            createdAt,
            Instant.now()
        );
    }

    /**
     * Transitions to CALCULATED state with the calculation result.
     */
    public AnnualSubmissionSaga withCalculated(TaxCalculationResult result) {
        if (state != AnnualSubmissionState.CALCULATING) {
            throw new IllegalStateException("Can only transition to CALCULATED from CALCULATING state");
        }
        return new AnnualSubmissionSaga(
            id, nino, taxYear,
            AnnualSubmissionState.CALCULATED,
            calculationId,
            result,
            null,
            null,
            createdAt,
            Instant.now()
        );
    }

    /**
     * Transitions to DECLARING state.
     */
    public AnnualSubmissionSaga withDeclaring() {
        if (state != AnnualSubmissionState.CALCULATED) {
            throw new IllegalStateException("Can only transition to DECLARING from CALCULATED state");
        }
        return new AnnualSubmissionSaga(
            id, nino, taxYear,
            AnnualSubmissionState.DECLARING,
            calculationId,
            calculationResult,
            null,
            null,
            createdAt,
            Instant.now()
        );
    }

    /**
     * Transitions to COMPLETED state with HMRC confirmation.
     */
    public AnnualSubmissionSaga withCompleted(String hmrcConfirmation) {
        if (state != AnnualSubmissionState.DECLARING) {
            throw new IllegalStateException("Can only transition to COMPLETED from DECLARING state");
        }
        return new AnnualSubmissionSaga(
            id, nino, taxYear,
            AnnualSubmissionState.COMPLETED,
            calculationId,
            calculationResult,
            hmrcConfirmation,
            null,
            createdAt,
            Instant.now()
        );
    }

    /**
     * Transitions to FAILED state with error message.
     */
    public AnnualSubmissionSaga withFailed(String errorMessage) {
        if (state.isTerminal()) {
            throw new IllegalStateException("Cannot transition to FAILED from terminal state");
        }
        return new AnnualSubmissionSaga(
            id, nino, taxYear,
            AnnualSubmissionState.FAILED,
            calculationId,
            calculationResult,
            hmrcConfirmation,
            errorMessage,
            createdAt,
            Instant.now()
        );
    }

    /**
     * Returns true if the saga is in a terminal state.
     */
    public boolean isTerminal() {
        return state.isTerminal();
    }

    /**
     * Returns true if the saga can be resumed.
     */
    public boolean isResumable() {
        return state.isResumable();
    }
}
