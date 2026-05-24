package uk.selfemploy.core.exception;

/**
 * Thrown when an attempt is made to submit a final Self Assessment declaration
 * to HMRC without a valid user confirmation.
 *
 * <p>The Pre-Submission Confirmation gate refuses to proceed when the supplied
 * {@code uk.selfemploy.common.legal.SubmissionConfirmation} is either {@code null}
 * or has {@code confirmedByUser=false}. The user must explicitly acknowledge
 * that figures are accurate before any POST is made to HMRC.
 */
public class DeclarationNotConfirmedException extends RuntimeException {

    public DeclarationNotConfirmedException(String message) {
        super(message);
    }
}
