package uk.selfemploy.ui.viewmodel;

/**
 * Status of a quarterly MTD submission.
 * Sprint 10D: SE-10D-001 - Quarterly Dashboard View
 */
public enum QuarterStatus {

    /**
     * Successfully submitted to HMRC.
     */
    SUBMITTED("Submitted", "check-circle", "#d4edda", "#155724"),

    /**
     * Deadline passed but not yet submitted.
     */
    OVERDUE("Overdue", "exclamation-triangle", "#f8d7da", "#721c24"),

    /**
     * Has data and ready to submit (or in progress).
     */
    DRAFT("Draft", "edit", "#fff3cd", "#856404"),

    /**
     * Future quarter - no action needed yet.
     */
    FUTURE("Future", "clock", "#e9ecef", "#6c757d");

    private final String displayText;
    private final String icon;
    private final String backgroundColor;
    private final String textColor;

    QuarterStatus(String displayText, String icon, String backgroundColor, String textColor) {
        this.displayText = displayText;
        this.icon = icon;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getIcon() {
        return icon;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getTextColor() {
        return textColor;
    }

    /**
     * Returns the CSS style class for this status badge.
     */
    public String getStyleClass() {
        return "status-badge-" + name().toLowerCase();
    }

    /**
     * Returns the CSS style class for the quarter card.
     */
    public String getCardStyleClass() {
        return "quarter-card-" + name().toLowerCase();
    }

    /**
     * Returns the icon character for display.
     */
    public String getIconChar() {
        return switch (this) {
            case SUBMITTED -> "\u2713"; // ✓
            case OVERDUE -> "\u26A0";   // ⚠
            case DRAFT -> "\u25CB";     // ○
            case FUTURE -> "\u25CB";    // ○
        };
    }
}
