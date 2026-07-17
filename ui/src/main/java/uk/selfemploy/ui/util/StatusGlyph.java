package uk.selfemploy.ui.util;

/**
 * Shared status glyphs used across the UI so a check mark or cross means the same thing everywhere.
 */
public final class StatusGlyph {

    /** A passed / positive / deductible state. */
    public static final String PASS = "✓";

    /** A failed / negative state. */
    public static final String FAIL = "✗";

    /** A neutral / not-applicable / skipped state. */
    public static final String NEUTRAL = "–";

    private StatusGlyph() {
    }
}
