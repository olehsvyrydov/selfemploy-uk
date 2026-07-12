package uk.selfemploy.ui.service;

/**
 * Decides whether an annual Self Assessment declaration may proceed. The check
 * runs BEFORE the declaration step so a taxpayer without HMRC credentials is
 * stopped with an actionable message rather than hitting a failed HMRC call.
 */
public final class SubmissionCredentialGate {

    private SubmissionCredentialGate() {
    }

    /** Why a submission is blocked, or that it may proceed. */
    public enum Status {
        ALLOWED,
        NOT_CONNECTED,
        NO_NINO
    }

    /**
     * The gate decision. {@link #message()} is null when {@link #allowed()}.
     */
    public record Decision(Status status, String message) {
        public boolean allowed() {
            return status == Status.ALLOWED;
        }
    }

    /**
     * Evaluates the gate.
     *
     * @param connectedToHmrc whether an HMRC session is currently connected
     * @param nino            the taxpayer's National Insurance number (may be null/blank)
     * @return an ALLOWED decision, or a blocked decision with an actionable message
     */
    public static Decision evaluate(boolean connectedToHmrc, String nino) {
        if (!connectedToHmrc) {
            return new Decision(Status.NOT_CONNECTED,
                "You are not connected to HMRC. Open the HMRC Submission page and connect your "
                + "HMRC account before you can submit an annual declaration.");
        }
        if (nino == null || nino.isBlank()) {
            return new Decision(Status.NO_NINO,
                "Your National Insurance number is missing. Add it in Settings before submitting "
                + "to HMRC.");
        }
        return new Decision(Status.ALLOWED, null);
    }
}
