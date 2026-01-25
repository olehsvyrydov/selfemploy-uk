package uk.selfemploy.core.export;

/**
 * Options for customizing data import behavior.
 */
public record ImportOptions(
    /**
     * Whether to merge with existing data (true) or fail on duplicates (false).
     */
    boolean mergeExisting,

    /**
     * Whether to skip duplicate records (true) or fail on duplicates (false).
     */
    boolean skipDuplicatesEnabled
) {
    /**
     * Creates default import options (no merge, no skip).
     */
    public static ImportOptions defaults() {
        return new ImportOptions(false, false);
    }

    /**
     * Creates import options that skip duplicates.
     */
    public static ImportOptions withSkipDuplicates() {
        return new ImportOptions(false, true);
    }
}
