package uk.selfemploy.ui.util;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides user-friendly guidance for HMRC error codes.
 *
 * <p>SE-SH-006: Error Resolution Guidance</p>
 *
 * <p>Maps common HMRC error codes to actionable help text that users can
 * understand without technical knowledge. Also provides a link to official
 * HMRC guidance for corrections.</p>
 *
 * <p>Common HMRC error codes covered:</p>
 * <ul>
 *   <li>INVALID_NINO - National Insurance number format issues</li>
 *   <li>INVALID_TAX_YEAR - Tax year format issues</li>
 *   <li>DUPLICATE_SUBMISSION - Already submitted for this period</li>
 *   <li>CALCULATION_ERROR - HMRC calculation failure</li>
 *   <li>BUSINESS_VALIDATION - Data mismatch with HMRC records</li>
 * </ul>
 */
public class HmrcErrorGuidance {

    /**
     * HMRC guidance URL for Self Assessment corrections.
     */
    private static final String HMRC_CORRECTIONS_URL =
        "https://www.gov.uk/self-assessment-tax-returns/corrections";

    /**
     * Default guidance when error code is unknown.
     */
    private static final String DEFAULT_GUIDANCE =
        "Please review your submission details and try again. " +
        "If the problem persists, contact HMRC for assistance.";

    /**
     * Pattern to match common HMRC error codes in error messages.
     * Matches uppercase codes with underscores like INVALID_NINO, FORMAT_VALUE, etc.
     */
    private static final Pattern ERROR_CODE_PATTERN =
        Pattern.compile("\\b([A-Z][A-Z0-9_]{3,}[A-Z0-9])\\b");

    /**
     * Mapping of HMRC error codes to user-friendly help text.
     */
    private static final Map<String, String> ERROR_GUIDANCE = Map.ofEntries(
        // Direct error codes from requirements
        Map.entry("INVALID_NINO",
            "Check your National Insurance number is correct. " +
            "It should be in the format: AA 12 34 56 B"),

        Map.entry("INVALID_TAX_YEAR",
            "The tax year format should be YYYY-YY (e.g., 2024-25). " +
            "Please check the tax year is valid."),

        Map.entry("DUPLICATE_SUBMISSION",
            "You've already submitted for this period. " +
            "If you need to make corrections, use the amendment process."),

        Map.entry("CALCULATION_ERROR",
            "HMRC could not calculate your tax. Check your figures " +
            "are entered correctly and all required fields are complete."),

        Map.entry("BUSINESS_VALIDATION",
            "Some of your data doesn't match HMRC records. " +
            "Please verify your business registration details are correct."),

        // Format error codes
        Map.entry("FORMAT_NINO",
            "Check your National Insurance number is correct. " +
            "It should be in the format: AA 12 34 56 B"),

        Map.entry("FORMAT_TAX_YEAR",
            "The tax year format should be YYYY-YY (e.g., 2024-25)."),

        Map.entry("FORMAT_VALUE",
            "One or more values are in an invalid format. " +
            "Check all amounts are entered as numbers without symbols."),

        // Rule error codes
        Map.entry("RULE_TAX_YEAR_NOT_SUPPORTED",
            "This tax year is not yet supported for Making Tax Digital. " +
            "Please check the MTD timeline for supported tax years."),

        Map.entry("RULE_ALREADY_EXISTS",
            "This record already exists at HMRC. " +
            "If you need to update it, use the amendment process."),

        Map.entry("RULE_PERIODIC_UPDATE_FOR_PERIOD_SUBMITTED",
            "You've already submitted an update for this period. " +
            "Use the amendment process to make changes."),

        Map.entry("RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED",
            "The submission data appears to be incomplete. " +
            "Please ensure all required fields are filled in."),

        Map.entry("RULE_BUSINESS_VALIDATION_FAILURE",
            "Your business details don't match HMRC records. " +
            "Please verify your Unique Taxpayer Reference (UTR)."),

        Map.entry("RULE_INSOLVENT_TRADER",
            "This business account is marked as insolvent. " +
            "Please contact HMRC directly for assistance."),

        // Auth error codes
        Map.entry("CLIENT_OR_AGENT_NOT_AUTHORISED",
            "Your HMRC authorization has expired. " +
            "Please reconnect to HMRC from the settings page."),

        Map.entry("UNAUTHORISED",
            "Your HMRC session has expired. " +
            "Please reconnect to HMRC."),

        Map.entry("FORBIDDEN",
            "You don't have permission for this action. " +
            "Check your HMRC account access settings."),

        // Resource error codes
        Map.entry("MATCHING_RESOURCE_NOT_FOUND",
            "The record could not be found at HMRC. " +
            "Please verify your submission details."),

        // Server error codes
        Map.entry("SERVER_ERROR",
            "HMRC is experiencing technical difficulties. " +
            "Please try again in a few minutes."),

        Map.entry("SERVICE_UNAVAILABLE",
            "HMRC services are temporarily unavailable. " +
            "Please try again later.")
    );

    /**
     * Set of known error codes for quick lookup.
     */
    private static final Set<String> KNOWN_CODES = ERROR_GUIDANCE.keySet();

    /**
     * Returns user-friendly guidance text for the given HMRC error code.
     *
     * @param errorCode the HMRC error code (case-insensitive)
     * @return guidance text explaining what the user should do
     */
    public String getGuidanceForErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return DEFAULT_GUIDANCE;
        }

        String normalizedCode = errorCode.toUpperCase().trim();
        return ERROR_GUIDANCE.getOrDefault(normalizedCode, DEFAULT_GUIDANCE);
    }

    /**
     * Attempts to extract an HMRC error code from an error message.
     *
     * @param errorMessage the full error message
     * @return the extracted error code, or null if none found
     */
    public String extractErrorCode(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }

        Matcher matcher = ERROR_CODE_PATTERN.matcher(errorMessage);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            // Check if this looks like a real error code (known or follows pattern)
            if (KNOWN_CODES.contains(candidate) ||
                candidate.startsWith("RULE_") ||
                candidate.startsWith("FORMAT_") ||
                candidate.startsWith("INVALID_")) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Returns the HMRC guidance URL for Self Assessment corrections.
     *
     * @return the HMRC corrections guidance URL
     */
    public String getGuidanceUrl() {
        return HMRC_CORRECTIONS_URL;
    }

    /**
     * Returns formatted guidance text based on error code and message.
     *
     * @param errorCode the HMRC error code (may be null)
     * @param errorMessage the original error message (may be null)
     * @return formatted guidance text
     */
    public String getFormattedGuidance(String errorCode, String errorMessage) {
        // First try to use the provided error code
        if (errorCode != null && !errorCode.isBlank()) {
            String guidance = getGuidanceForErrorCode(errorCode);
            if (!guidance.equals(DEFAULT_GUIDANCE)) {
                return guidance;
            }
        }

        // Try to extract error code from message
        if (errorMessage != null && !errorMessage.isBlank()) {
            String extractedCode = extractErrorCode(errorMessage);
            if (extractedCode != null) {
                return getGuidanceForErrorCode(extractedCode);
            }
        }

        return DEFAULT_GUIDANCE;
    }

    /**
     * Checks if the given error code is a known HMRC error code.
     *
     * @param errorCode the error code to check
     * @return true if the code is known, false otherwise
     */
    public boolean isKnownErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return false;
        }
        return KNOWN_CODES.contains(errorCode.toUpperCase().trim());
    }
}
