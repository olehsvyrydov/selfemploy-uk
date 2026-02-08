package uk.selfemploy.core.reconciliation;

import org.apache.commons.text.similarity.LevenshteinDistance;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;

/**
 * Shared utility methods for transaction matching, extracted from the
 * duplicate detection system for reuse across import-time detection
 * and post-import reconciliation.
 *
 * <p>All methods are static and stateless for easy reuse.</p>
 */
public final class MatchingUtils {

    private static final LevenshteinDistance LEVENSHTEIN = new LevenshteinDistance();

    /**
     * Minimum Levenshtein similarity for a "likely" match.
     */
    public static final double LIKELY_THRESHOLD = 0.80;

    /**
     * Tier 3 relative tolerance: 1% of the amount.
     */
    static final BigDecimal RELATIVE_TOLERANCE = new BigDecimal("0.01");

    /**
     * Tier 3 absolute tolerance: GBP 1.00.
     */
    static final BigDecimal ABSOLUTE_TOLERANCE = new BigDecimal("1.00");

    private MatchingUtils() {
        // Utility class
    }

    /**
     * Normalizes a description for comparison.
     * Lowercases, trims, and collapses whitespace.
     *
     * @param description the raw description
     * @return normalized description, empty string if null
     */
    public static String normalizeDescription(String description) {
        if (description == null) {
            return "";
        }
        return description.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /**
     * Calculates string similarity using Levenshtein distance.
     *
     * @param s1 first normalized string
     * @param s2 second normalized string
     * @return similarity score between 0.0 (completely different) and 1.0 (identical)
     */
    public static double calculateSimilarity(String s1, String s2) {
        if (s1 == null) s1 = "";
        if (s2 == null) s2 = "";

        if (s1.equals(s2)) {
            return 1.0;
        }

        int distance = LEVENSHTEIN.apply(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());

        if (maxLength == 0) {
            return 1.0;
        }

        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Creates an exact match key from date, absolute amount, and normalized description.
     *
     * @param date        transaction date
     * @param absAmount   absolute amount (always positive)
     * @param description raw description (will be normalized)
     * @return pipe-delimited key string
     */
    public static String createExactKey(LocalDate date, BigDecimal absAmount, String description) {
        String normalizedDesc = normalizeDescription(description);
        return String.format("%s|%s|%s",
            date.toString(),
            absAmount.stripTrailingZeros().toPlainString(),
            normalizedDesc);
    }

    /**
     * Checks whether two amounts are within the Tier 3 tolerance.
     * Tolerance is 1% of the amount OR GBP 1.00, whichever is GREATER.
     *
     * @param amount1 first absolute amount
     * @param amount2 second absolute amount
     * @return true if amounts are within tolerance
     */
    public static boolean isWithinTolerance(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null || amount2 == null) {
            return false;
        }

        BigDecimal diff = amount1.subtract(amount2).abs();

        // Calculate 1% of the smaller amount (conservative tolerance)
        BigDecimal referenceAmount = amount1.min(amount2);
        BigDecimal relativeTolerance = referenceAmount.multiply(RELATIVE_TOLERANCE);

        // Use whichever tolerance is GREATER
        BigDecimal effectiveTolerance = relativeTolerance.max(ABSOLUTE_TOLERANCE);

        return diff.compareTo(effectiveTolerance) <= 0;
    }

    /**
     * Checks whether two absolute amounts are exactly equal (penny-for-penny).
     *
     * @param amount1 first absolute amount
     * @param amount2 second absolute amount
     * @return true if amounts are exactly equal
     */
    public static boolean isExactAmount(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null || amount2 == null) {
            return false;
        }
        return amount1.compareTo(amount2) == 0;
    }
}
