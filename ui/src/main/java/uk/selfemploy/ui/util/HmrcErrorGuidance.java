package uk.selfemploy.ui.util;

import uk.selfemploy.core.exception.SubmissionException;

import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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
        // Pre-validation error codes (SE-10E-002)
        Map.entry("NINO_REQUIRED",
            "Your National Insurance Number is required for MTD submissions. " +
            "Go to Settings > Profile and enter your NINO."),

        Map.entry("BUSINESS_ID_REQUIRED",
            "Your HMRC business profile needs to be synced. " +
            "Go to Settings and reconnect to HMRC to retrieve your business registration details."),

        Map.entry("DECLARATION_REQUIRED",
            "You must tick all confirmation checkboxes before submitting to HMRC."),

        // Auth error codes (SE-10E-002)
        Map.entry("TOKEN_EXPIRED",
            "Your HMRC connection has expired. " +
            "Go to Settings > HMRC Connection and sign in again with your Government Gateway credentials."),

        Map.entry("NOT_CONNECTED",
            "You are not connected to HMRC. " +
            "Go to Settings > HMRC Connection and sign in with your Government Gateway credentials."),

        Map.entry("AUTH_FAILED",
            "Your Government Gateway credentials were rejected by HMRC. " +
            "Please reconnect via Settings > HMRC Connection."),

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
            "Your HMRC business profile has not been synced yet. " +
            "Go to Settings and reconnect to HMRC to fetch your business registration details. " +
            "This is a one-time setup after connecting."),

        Map.entry("HMRC_PROFILE_NOT_SYNCED",
            "Your HMRC business profile needs to be synced before submitting. " +
            "Go to Settings and reconnect to HMRC to fetch your business details."),

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
     * Mapping of error codes to dialog header titles.
     * Falls back to "Submission Failed" for unknown codes.
     */
    private static final Map<String, String> ERROR_TITLES = Map.ofEntries(
        Map.entry("NINO_REQUIRED", "National Insurance Number Not Set"),
        Map.entry("BUSINESS_ID_REQUIRED", "HMRC Business Profile Not Synced"),
        Map.entry("DECLARATION_REQUIRED", "Declaration Not Accepted"),
        Map.entry("TOKEN_EXPIRED", "HMRC Session Expired"),
        Map.entry("NOT_CONNECTED", "Not Connected to HMRC"),
        Map.entry("AUTH_FAILED", "HMRC Authentication Failed"),
        Map.entry("INVALID_NINO", "Invalid National Insurance Number"),
        Map.entry("DUPLICATE_SUBMISSION", "Already Submitted"),
        Map.entry("RULE_ALREADY_EXISTS", "Already Submitted"),
        Map.entry("RULE_PERIODIC_UPDATE_FOR_PERIOD_SUBMITTED", "Already Submitted"),
        Map.entry("MATCHING_RESOURCE_NOT_FOUND", "HMRC Business Profile Not Synced"),
        Map.entry("HMRC_PROFILE_NOT_SYNCED", "HMRC Profile Not Synced"),
        Map.entry("SERVER_ERROR", "HMRC Service Unavailable"),
        Map.entry("SERVICE_UNAVAILABLE", "HMRC Service Unavailable")
    );

    /**
     * Error codes that can be resolved by visiting Settings.
     * When one of these codes is encountered, the dialog shows an "Open Settings" button.
     */
    private static final Set<String> SETTINGS_ERROR_CODES = Set.of(
        "NINO_REQUIRED",
        "BUSINESS_ID_REQUIRED",
        "TOKEN_EXPIRED",
        "NOT_CONNECTED",
        "AUTH_FAILED",
        "CLIENT_OR_AGENT_NOT_AUTHORISED",
        "UNAUTHORISED",
        "MATCHING_RESOURCE_NOT_FOUND",
        "HMRC_PROFILE_NOT_SYNCED"
    );

    /**
     * Network exception types that indicate connection problems.
     */
    private static final Set<Class<? extends Throwable>> NETWORK_EXCEPTION_TYPES = Set.of(
        SSLHandshakeException.class,
        ConnectException.class,
        SocketTimeoutException.class,
        UnknownHostException.class
    );

    /**
     * Patterns in exception messages that indicate network errors.
     */
    private static final Pattern NETWORK_MESSAGE_PATTERN =
        Pattern.compile("(?i)(network\s+error|connection\s+timeout|ssl\s+handshake)");

    /**
     * Title for network-related errors.
     */
    private static final String NETWORK_ERROR_TITLE = "Connection Error";

    /**
     * User-friendly message for network errors.
     */
    private static final String NETWORK_ERROR_MESSAGE =
        "Could not connect to HMRC. The service may be temporarily unavailable.";

    /**
     * Actionable guidance for network errors.
     */
    private static final String NETWORK_ERROR_GUIDANCE =
        "Please check your internet connection and try again. " +
        "If the problem persists, HMRC services may be temporarily unavailable.";

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

    /**
     * Returns a dialog-specific title for the given error code.
     *
     * <p>SE-10E-002: Maps known error codes to descriptive dialog titles.
     * Falls back to "Submission Failed" for unknown or null codes.</p>
     *
     * @param errorCode the error code (case-insensitive)
     * @return a dialog title appropriate for the error
     */
    public String getTitle(String errorCode) {
        if (errorCode == null) {
            return "Submission Failed";
        }
        return ERROR_TITLES.getOrDefault(errorCode.toUpperCase().trim(), "Submission Failed");
    }

    /**
     * Checks if the given error code represents an error fixable via Settings.
     *
     * <p>SE-10E-002: Settings errors include missing NINO, expired tokens,
     * and authentication failures - all resolvable by the user in the Settings page.</p>
     *
     * @param errorCode the error code to check
     * @return true if the error can be fixed via Settings
     */
    public boolean isSettingsError(String errorCode) {
        if (errorCode == null) {
            return false;
        }
        return SETTINGS_ERROR_CODES.contains(errorCode.toUpperCase().trim());
    }

    /**
     * Builds a complete {@link SubmissionErrorDisplay} for rendering the error dialog.
     *
     * <p>SE-10E-002: This is the main entry point for the error dialog redesign.
     * It extracts the error code from the exception, looks up guidance, title,
     * and action hints, and returns a fully populated display record.</p>
     *
     * @param exception the exception from the failed submission (may be null)
     * @return a display record with all information needed to render the error dialog
     */
    public SubmissionErrorDisplay buildErrorDisplay(Throwable exception) {
        String errorMessage = exception != null ? exception.getMessage() : null;
        boolean retryable = exception instanceof SubmissionException se && se.isRetryable();

        // Check for network errors first
        if (isNetworkError(exception)) {
            return new SubmissionErrorDisplay(
                NETWORK_ERROR_TITLE, NETWORK_ERROR_MESSAGE, NETWORK_ERROR_GUIDANCE,
                null, retryable, false);
        }

        String errorCode = extractErrorCode(errorMessage);
        String guidance = getFormattedGuidance(errorCode, errorMessage);
        String title = getTitle(errorCode);
        boolean settingsError = errorCode != null && SETTINGS_ERROR_CODES.contains(errorCode.toUpperCase().trim());

        // Differentiate message from guidance:
        // - message: the original exception message (what went wrong)
        // - guidance: the looked-up actionable steps (what to do about it)
        String message;
        if (errorMessage != null && !errorMessage.isBlank()) {
            message = errorMessage;
        } else {
            message = guidance;
        }

        return new SubmissionErrorDisplay(title, message, guidance, errorCode, retryable, settingsError);
    }

    /**
     * Checks if the given exception represents a network connectivity error.
     *
     * <p>Inspects the exception and its cause chain for known network exception types
     * (SSLHandshakeException, ConnectException, SocketTimeoutException, UnknownHostException)
     * and also checks the exception message for network-related keywords.</p>
     *
     * @param exception the exception to check
     * @return true if this is a network-related error
     */
    boolean isNetworkError(Throwable exception) {
        if (exception == null) {
            return false;
        }

        // Check exception message for network patterns
        String message = exception.getMessage();
        if (message != null && NETWORK_MESSAGE_PATTERN.matcher(message).find()) {
            return true;
        }

        // Check cause chain for network exception types (up to 5 levels deep)
        Throwable cause = exception.getCause();
        int depth = 0;
        while (cause != null && depth < 5) {
            for (Class<? extends Throwable> networkType : NETWORK_EXCEPTION_TYPES) {
                if (networkType.isInstance(cause)) {
                    return true;
                }
            }
            cause = cause.getCause();
            depth++;
        }

        return false;
    }
}
