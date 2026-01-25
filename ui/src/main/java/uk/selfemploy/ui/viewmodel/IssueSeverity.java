package uk.selfemploy.ui.viewmodel;

/**
 * Severity levels for reconciliation issues.
 */
public enum IssueSeverity {
    /**
     * High priority issues - potential duplicates, data corruption.
     */
    HIGH("HIGH", "!", "#f8d7da", "#721c24", "issue-severity-high"),

    /**
     * Medium priority issues - missing categories, incomplete records.
     */
    MEDIUM("MEDIUM", "*", "#fff3cd", "#856404", "issue-severity-medium"),

    /**
     * Low priority issues - date gaps, suggestions.
     */
    LOW("LOW", "-", "#cce5ff", "#004085", "issue-severity-low");

    private final String displayText;
    private final String icon;
    private final String backgroundColor;
    private final String textColor;
    private final String styleClass;

    IssueSeverity(String displayText, String icon, String backgroundColor, String textColor, String styleClass) {
        this.displayText = displayText;
        this.icon = icon;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.styleClass = styleClass;
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

    public String getStyleClass() {
        return styleClass;
    }
}
