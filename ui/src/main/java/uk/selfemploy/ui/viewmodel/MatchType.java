package uk.selfemploy.ui.viewmodel;

/**
 * Match type for duplicate detection during import.
 * Indicates whether an imported transaction matches an existing record.
 */
public enum MatchType {
    /**
     * No matching record found - safe to import.
     */
    NEW("NEW", "+", "#d4edda", "#155724"),

    /**
     * Identical record exists - likely duplicate.
     */
    EXACT("EXACT", "!", "#f8d7da", "#721c24"),

    /**
     * Possible duplicate - description ≥80% similar, same date + amount.
     */
    LIKELY("LIKELY", "?", "#fff3cd", "#856404"),

    /**
     * Same date and amount but different description (<80% similar).
     * User should review - could be a duplicate with different description.
     */
    SIMILAR("SIMILAR", "~", "#cce5ff", "#004085");

    private final String displayText;
    private final String icon;
    private final String backgroundColor;
    private final String textColor;

    MatchType(String displayText, String icon, String backgroundColor, String textColor) {
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
     * Returns the CSS style class for this match type.
     */
    public String getStyleClass() {
        return "match-badge-" + name().toLowerCase();
    }

    /**
     * Returns the row style class for this match type.
     */
    public String getRowStyleClass() {
        return "row-" + name().toLowerCase();
    }
}
