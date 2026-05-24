package uk.selfemploy.core.exception;

/**
 * Thrown when an attempt is made to submit a final Self Assessment declaration
 * to HMRC without a valid user confirmation.
 *
 * <p><strong>SLFEMPUK-35 / S17-11:</strong> the Pre-Submission Confirmation
 * gate refuses to proceed when the supplied
 * {@code uk.selfemploy.common.legal.SubmissionConfirmation} is either {@code null}
 * or has {@code confirmedByUser=false}. The user must explicitly acknowledge
 * that figures are accurate before any POST is made to HMRC. See AC-1 of
 * SLFEMPUK-35.
 */
public class DeclarationNotConfirmedException extends RuntimeException {

    public DeclarationNotConfirmedException(String message) {
        super(message);
    }
}
