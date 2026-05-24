package uk.selfemploy.common.legal;

import java.time.Instant;
import java.util.Objects;

/**
 * User confirmation that figures in a Self Assessment final declaration are
 * accurate, captured prior to invoking the HMRC declaration endpoint.
 *
 * <p><strong>SLFEMPUK-35 / S17-11:</strong> per /alex (legal) recommendation,
 * a Pre-Submission Confirmation gate must precede any POST to HMRC's final
 * declaration endpoint. This record carries the user's explicit acknowledgement
 * and the timestamp at which it was given. The presence of a valid
 * {@code SubmissionConfirmation} at the call site shifts evidential burden if
 * HMRC opens an FA 2009 Sch.55 enquiry and mirrors the taxpayer's own
 * declaration obligation under TMA 1970 s.8.
 *
 * <p>The accompanying {@link uk.selfemploy.common.legal.Disclaimers} text must
 * be displayed to the user before this confirmation is constructed. Construction
 * without {@code confirmedByUser=true} is rejected at the call site by
 * {@code AnnualSubmissionService.confirmDeclaration}.
 *
 * <p>This is a value object — immutable, equality by field, no behaviour.
 *
 * @param userId       Identifier for the human user who ticked the confirmation
 *                     box. Free-form (display name, UUID, OS username); not used
 *                     for authentication. Required, non-blank.
 * @param confirmedByUser Must be {@code true} — represents the user actively
 *                     ticking the "I confirm these figures are accurate" box.
 *                     A {@code false} value will be rejected at the gate.
 * @param confirmedAt  UTC instant at which the user ticked the confirmation
 *                     box. Required, non-null. Persisted to the local audit log.
 */
public record SubmissionConfirmation(
        String userId,
        boolean confirmedByUser,
        Instant confirmedAt
) {

    public SubmissionConfirmation {
        Objects.requireNonNull(confirmedAt, "confirmedAt must not be null");
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be null or blank");
        }
    }

    /**
     * Convenience factory for the common case of a user actively confirming
     * at the current instant.
     *
     * @param userId identifier of the confirming user
     * @return a confirmation with {@code confirmedByUser=true} at the given instant
     */
    public static SubmissionConfirmation confirmedNow(String userId, Instant now) {
        return new SubmissionConfirmation(userId, true, now);
    }
}
