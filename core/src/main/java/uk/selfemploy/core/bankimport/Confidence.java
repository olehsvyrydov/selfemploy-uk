package uk.selfemploy.core.bankimport;

/**
 * Confidence level for category suggestions.
 */
public enum Confidence {
    /**
     * High confidence - strong keyword match.
     */
    HIGH,

    /**
     * Medium confidence - partial match or common pattern.
     */
    MEDIUM,

    /**
     * Low confidence - default or no match.
     */
    LOW
}
