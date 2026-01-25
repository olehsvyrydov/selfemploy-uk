package uk.selfemploy.core.dedup;

import uk.selfemploy.core.bankimport.ImportedTransaction;

import java.util.UUID;

/**
 * Represents a duplicate detection result for an imported transaction.
 *
 * <p>Contains information about the match type, confidence score,
 * and the existing record if a match was found.</p>
 */
public record DuplicateMatch(
    ImportedTransaction imported,
    MatchType matchType,
    double confidence,
    UUID existingRecordId,
    String existingDescription
) {
    /**
     * Compact constructor for validation.
     */
    public DuplicateMatch {
        if (imported == null) {
            throw new IllegalArgumentException("imported cannot be null");
        }
        if (matchType == null) {
            throw new IllegalArgumentException("matchType cannot be null");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
    }

    /**
     * Creates a match result indicating no duplicate found.
     */
    public static DuplicateMatch noMatch(ImportedTransaction imported) {
        return new DuplicateMatch(imported, MatchType.NONE, 0.0, null, null);
    }

    /**
     * Creates an exact match result.
     */
    public static DuplicateMatch exactMatch(ImportedTransaction imported, UUID existingId, String existingDesc) {
        return new DuplicateMatch(imported, MatchType.EXACT, 1.0, existingId, existingDesc);
    }

    /**
     * Creates a likely match result with a specific confidence score.
     */
    public static DuplicateMatch likelyMatch(ImportedTransaction imported, double confidence,
                                             UUID existingId, String existingDesc) {
        return new DuplicateMatch(imported, MatchType.LIKELY, confidence, existingId, existingDesc);
    }

    /**
     * Creates a date-only match result.
     */
    public static DuplicateMatch dateOnlyMatch(ImportedTransaction imported, UUID existingId, String existingDesc) {
        return new DuplicateMatch(imported, MatchType.DATE_ONLY, 0.3, existingId, existingDesc);
    }

    /**
     * Checks if this match has a high confidence (>= 0.8).
     */
    public boolean isHighConfidence() {
        return confidence >= 0.8;
    }

    /**
     * Checks if this match has a medium confidence (0.5 - 0.8).
     */
    public boolean isMediumConfidence() {
        return confidence >= 0.5 && confidence < 0.8;
    }

    /**
     * Checks if this match has a low confidence (< 0.5).
     */
    public boolean isLowConfidence() {
        return confidence < 0.5;
    }

    /**
     * Checks if a match was found (any type except NONE).
     */
    public boolean hasMatch() {
        return matchType != MatchType.NONE;
    }
}
