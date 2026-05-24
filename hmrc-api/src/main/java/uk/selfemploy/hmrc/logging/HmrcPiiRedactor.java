package uk.selfemploy.hmrc.logging;

import java.util.regex.Pattern;

/**
 * Removes personally identifiable information from strings before they reach
 * any log appender. Intended to wrap every {@code log.error} / {@code log.warn}
 * site that may include an HMRC response body, request payload, or exception
 * message containing taxpayer data.
 *
 * <h3>Patterns removed</h3>
 * <ul>
 *   <li>National Insurance Number (NINO) — UK statutory format
 *       {@code [A-CEGHJ-PR-TW-Z]{2}\d{6}[A-D]}, with optional spaces.</li>
 *   <li>Unique Taxpayer Reference (UTR) — exactly 10 digits, word-boundary anchored.</li>
 *   <li>Bearer tokens — case-insensitive {@code Bearer <token>}.</li>
 *   <li>UUIDs — calculationId / submissionId / etc.</li>
 * </ul>
 *
 * <p>The redactor is intentionally over-eager: false positives on benign 10-digit
 * numbers are preferable to leaking a UTR. Callers that need the original value
 * must keep it out of log statements entirely.
 */
public final class HmrcPiiRedactor {

    // Statutory NINO format. Tolerates optional whitespace between every pair
    // of characters as written on paperwork (e.g. "AB 12 34 56 C").
    private static final Pattern NINO = Pattern.compile(
        "\\b[A-CEGHJ-PR-TW-Z]{2}\\s?\\d{2}\\s?\\d{2}\\s?\\d{2}\\s?[A-D]\\b");

    // UTR is always 10 digits. Word-boundary prevents matching inside longer
    // numeric strings (timestamps, IDs).
    private static final Pattern UTR = Pattern.compile("\\b\\d{10}\\b");

    // OAuth Bearer token of variable length.
    private static final Pattern BEARER = Pattern.compile(
        "(?i)Bearer\\s+[A-Za-z0-9._\\-]+");

    // RFC 4122 UUID, any case.
    private static final Pattern UUID = Pattern.compile(
        "\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b");

    private HmrcPiiRedactor() {
        // Utility class.
    }

    /**
     * Returns {@code message} with NINO, UTR, Bearer tokens, and UUIDs replaced
     * by opaque placeholder tokens. A {@code null} input maps to the literal
     * string {@code "null"} so callers can safely concatenate the result.
     */
    public static String redact(String message) {
        if (message == null) {
            return "null";
        }
        String out = NINO.matcher(message).replaceAll("<NINO_REDACTED>");
        out = BEARER.matcher(out).replaceAll("Bearer <REDACTED>");
        out = UTR.matcher(out).replaceAll("<UTR_REDACTED>");
        out = UUID.matcher(out).replaceAll("<ID_REDACTED>");
        return out;
    }
}
