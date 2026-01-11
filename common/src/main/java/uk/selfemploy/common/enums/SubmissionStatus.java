package uk.selfemploy.common.enums;

/**
 * Status of an HMRC submission.
 *
 * <p>Submissions can be in one of four states:</p>
 * <ul>
 *   <li>PENDING - Awaiting HMRC response</li>
 *   <li>SUBMITTED - Successfully sent to HMRC, awaiting processing</li>
 *   <li>ACCEPTED - HMRC has accepted the submission</li>
 *   <li>REJECTED - HMRC has rejected the submission (validation error)</li>
 * </ul>
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
    REJECTED("Rejected", "x");

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
     */
    public boolean isTerminal() {
        return this == ACCEPTED || this == REJECTED;
    }

    /**
     * Returns the CSS style class suffix for this status.
     * Used for styling status badges (e.g., "status-accepted").
     */
    public String getStyleClass() {
        return "status-" + name().toLowerCase();
    }
}
