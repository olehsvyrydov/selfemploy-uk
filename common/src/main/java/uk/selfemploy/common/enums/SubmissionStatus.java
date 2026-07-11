package uk.selfemploy.common.enums;

/**
 * Status of an HMRC submission.
 *
 * <p>The four HMRC-response states map 1:1 to what HMRC actually returns:</p>
 * <ul>
 *   <li>PENDING - Awaiting HMRC response</li>
 *   <li>SUBMITTED - Successfully sent to HMRC, awaiting processing</li>
 *   <li>ACCEPTED - HMRC has accepted the submission</li>
 *   <li>REJECTED - HMRC has rejected the submission (validation error)</li>
 * </ul>
 *
 * <p>{@link #NOT_SUBMITTED} is orthogonal to the HMRC states: it marks a record
 * that was never sent to HMRC (a local estimate or a legacy test record), so it
 * must never be presented as an HMRC filing.</p>
 */
public enum SubmissionStatus {

    /**
     * Submission is being processed or awaiting response.
     */
    PENDING("Pending", "clock"),

    /**
     * Submission successfully sent to HMRC.
     */
    SUBMITTED("Submitted", "send"),

    /**
     * HMRC has accepted the submission.
     */
    ACCEPTED("Accepted", "check"),

    /**
     * HMRC has rejected the submission due to validation errors.
     */
    REJECTED("Rejected", "x"),

    /**
     * The record was never sent to HMRC - a local estimate or a legacy test
     * record. It carries no HMRC reference and is not an HMRC filing.
     */
    NOT_SUBMITTED("Not submitted", "info");

    private final String displayName;
    private final String icon;

    SubmissionStatus(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    /**
     * Returns the display name for UI presentation.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the icon identifier for UI presentation.
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Checks if this status represents a successful submission.
     */
    public boolean isSuccessful() {
        return this == ACCEPTED || this == SUBMITTED;
    }

    /**
     * Checks if this status is a terminal state (no further changes expected).
     * A record that was never submitted is terminal - HMRC will never process it.
     */
    public boolean isTerminal() {
        return this == ACCEPTED || this == REJECTED || this == NOT_SUBMITTED;
    }

    /**
     * Checks whether this status reflects a record that was actually sent to HMRC.
     * Only the four HMRC-response states qualify; {@link #NOT_SUBMITTED} does not.
     */
    public boolean isSentToHmrc() {
        return this != NOT_SUBMITTED;
    }

    /**
     * Returns the CSS style class suffix for this status.
     * Used for styling status badges (e.g., "status-accepted").
     */
    public String getStyleClass() {
        return "status-" + name().toLowerCase();
    }
}
