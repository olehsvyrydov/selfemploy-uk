package uk.selfemploy.common.validation;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator for HMRC identifiers (UTR and NINO).
 *
 * <p>Provides validation and normalization utilities for:
 * <ul>
 *   <li>UTR (Unique Taxpayer Reference) - 10 digits</li>
 *   <li>NINO (National Insurance Number) - 2 letters, 6 digits, 1 letter suffix</li>
 * </ul>
 *
 * <p>NINO validation follows HMRC's official format specification:
 * <ul>
 *   <li>First character: A-Z excluding D, F, I, Q, U, V</li>
 *   <li>Second character: A-Z excluding D, F, I, O, Q, U, V</li>
 *   <li>Disallowed prefixes: BG, GB, KN, NK, NT, TN, ZZ</li>
 *   <li>Six digits (000000-999999)</li>
 *   <li>Suffix: A, B, C, or D only</li>
 * </ul>
 */
public final class HmrcIdentifierValidator {

    /**
     * UTR pattern: exactly 10 digits.
     */
    private static final Pattern UTR_PATTERN = Pattern.compile("^\\d{10}$");

    /**
     * Full HMRC NINO validation regex.
     * Format: 2 letters + 6 digits + 1 letter (A-D suffix)
     * Excludes:
     * - Prefixes: BG, GB, KN, NK, NT, TN, ZZ
     * - First letter: D, F, I, Q, U, V
     * - Second letter: D, F, I, O, Q, U, V
     */
    private static final Pattern NINO_PATTERN = Pattern.compile(
        "^(?!BG|GB|KN|NK|NT|TN|ZZ)[A-CEGHJ-PR-TW-Z][A-CEGHJ-NPR-TW-Z]\\d{6}[A-D]$"
    );

    /**
     * Disallowed NINO prefixes per HMRC rules.
     */
    private static final Set<String> DISALLOWED_NINO_PREFIXES = Set.of(
        "BG", "GB", "KN", "NK", "NT", "TN", "ZZ"
    );

    private HmrcIdentifierValidator() {
        // Utility class
    }

    /**
     * Validates a UTR (Unique Taxpayer Reference).
     *
     * @param utr the UTR to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidUtr(String utr) {
        if (utr == null || utr.isBlank()) {
            return false;
        }
        return UTR_PATTERN.matcher(utr).matches();
    }

    /**
     * Validates a NINO (National Insurance Number).
     *
     * @param nino the NINO to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidNino(String nino) {
        if (nino == null || nino.isBlank()) {
            return false;
        }
        return NINO_PATTERN.matcher(nino).matches();
    }

    /**
     * Normalizes a UTR by removing spaces and hyphens.
     *
     * @param utr the UTR to normalize
     * @return normalized UTR, or null if input is null
     */
    public static String normalizeUtr(String utr) {
        if (utr == null) {
            return null;
        }
        return utr.replaceAll("[\\s-]", "");
    }

    /**
     * Normalizes a NINO by removing spaces and converting to uppercase.
     *
     * @param nino the NINO to normalize
     * @return normalized NINO, or null if input is null
     */
    public static String normalizeNino(String nino) {
        if (nino == null) {
            return null;
        }
        return nino.replaceAll("\\s", "").toUpperCase();
    }

    /**
     * Validates UTR and throws exception if invalid.
     *
     * @param utr the UTR to validate
     * @throws IllegalArgumentException if UTR is invalid
     */
    public static void validateUtr(String utr) {
        if (!isValidUtr(utr)) {
            throw new IllegalArgumentException(
                "Invalid UTR format. UTR must be exactly 10 digits."
            );
        }
    }

    /**
     * Validates NINO and throws exception if invalid.
     *
     * @param nino the NINO to validate
     * @throws IllegalArgumentException if NINO is invalid
     */
    public static void validateNino(String nino) {
        if (!isValidNino(nino)) {
            throw new IllegalArgumentException(
                "Invalid NINO format. NINO must be 2 letters + 6 digits + 1 letter (A-D). " +
                "First letter cannot be D, F, I, Q, U, V. " +
                "Second letter cannot be D, F, I, O, Q, U, V. " +
                "Prefixes BG, GB, KN, NK, NT, TN, ZZ are not allowed."
            );
        }
    }

    /**
     * Normalizes and validates a UTR.
     *
     * @param utr the UTR to normalize and validate
     * @return the normalized UTR
     * @throws IllegalArgumentException if UTR is invalid after normalization
     */
    public static String normalizeAndValidateUtr(String utr) {
        String normalized = normalizeUtr(utr);
        validateUtr(normalized);
        return normalized;
    }

    /**
     * Normalizes and validates a NINO.
     *
     * @param nino the NINO to normalize and validate
     * @return the normalized NINO
     * @throws IllegalArgumentException if NINO is invalid after normalization
     */
    public static String normalizeAndValidateNino(String nino) {
        String normalized = normalizeNino(nino);
        validateNino(normalized);
        return normalized;
    }
}
