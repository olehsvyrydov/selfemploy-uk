package uk.selfemploy.core.error;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Set;

/**
 * Maps HMRC error codes and HTTP status codes to user-friendly messages.
 * Used by the error handling layer to present meaningful messages to users.
 */
@ApplicationScoped
public class ErrorMessageMapper {

    private static final String DEFAULT_ERROR_MESSAGE =
            "An unexpected error occurred. Please try again or contact support.";

    private static final String DEFAULT_HTTP_ERROR_MESSAGE =
            "An error occurred while communicating with HMRC.";

    /**
     * HMRC-specific error codes to user-friendly messages.
     */
    private static final Map<String, String> ERROR_CODE_MESSAGES = Map.ofEntries(
            // Validation errors
            Map.entry("RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED", "Please check your submission data"),
            Map.entry("RULE_PERIODIC_UPDATE_FOR_PERIOD_SUBMITTED", "This period has already been submitted"),
            Map.entry("RULE_TAX_YEAR_NOT_SUPPORTED", "Tax year not supported for MTD"),
            Map.entry("CLIENT_OR_AGENT_NOT_AUTHORISED", "Please reconnect to HMRC"),
            Map.entry("MATCHING_RESOURCE_NOT_FOUND", "Record not found at HMRC"),

            // Format errors
            Map.entry("FORMAT_NINO", "Invalid National Insurance number format"),
            Map.entry("FORMAT_TAX_YEAR", "Invalid tax year format"),
            Map.entry("FORMAT_VALUE", "Invalid value format in submission"),

            // Rule errors
            Map.entry("RULE_ALREADY_EXISTS", "This record already exists at HMRC"),
            Map.entry("RULE_INSOLVENT_TRADER", "Account is insolvent - contact HMRC"),
            Map.entry("RULE_BUSINESS_VALIDATION_FAILURE", "Business validation failed - check your data"),

            // Auth errors
            Map.entry("UNAUTHORISED", "Your HMRC session has expired"),
            Map.entry("FORBIDDEN", "You don't have permission for this action"),

            // Server errors
            Map.entry("SERVER_ERROR", "HMRC is experiencing technical difficulties"),
            Map.entry("SERVICE_UNAVAILABLE", "HMRC services are temporarily unavailable")
    );

    /**
     * HTTP status codes to user-friendly messages.
     */
    private static final Map<Integer, String> HTTP_STATUS_MESSAGES = Map.of(
            400, "The request was invalid. Please check your data and try again.",
            401, "Your HMRC session has expired. Please reconnect to HMRC.",
            403, "You don't have permission to perform this action. Please check your HMRC access.",
            404, "The requested record was not found at HMRC.",
            429, "Too many requests. Please wait a moment before trying again.",
            500, "HMRC is experiencing technical difficulties. Please try again later.",
            502, "HMRC services are temporarily unavailable. Please try again later.",
            503, "HMRC services are temporarily unavailable. Please try again later.",
            504, "HMRC is taking too long to respond. Please try again later."
    );

    /**
     * HTTP status codes that are retryable (temporary failures).
     */
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(
            429, // Too Many Requests
            500, // Internal Server Error
            502, // Bad Gateway
            503, // Service Unavailable
            504  // Gateway Timeout
    );

    /**
     * Maps an HMRC error code to a user-friendly message.
     *
     * @param errorCode the HMRC error code
     * @return user-friendly message
     */
    public String mapErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return DEFAULT_ERROR_MESSAGE;
        }
        return ERROR_CODE_MESSAGES.getOrDefault(errorCode, DEFAULT_ERROR_MESSAGE);
    }

    /**
     * Maps an HTTP status code to a user-friendly message.
     *
     * @param httpStatus the HTTP status code
     * @return user-friendly message
     */
    public String mapHttpStatus(int httpStatus) {
        return HTTP_STATUS_MESSAGES.getOrDefault(httpStatus, DEFAULT_HTTP_ERROR_MESSAGE);
    }

    /**
     * Checks if the HTTP status code indicates a client error (4xx).
     *
     * @param httpStatus the HTTP status code
     * @return true if it's a client error
     */
    public boolean isClientError(int httpStatus) {
        return httpStatus >= 400 && httpStatus < 500;
    }

    /**
     * Checks if the HTTP status code indicates a server error (5xx).
     *
     * @param httpStatus the HTTP status code
     * @return true if it's a server error
     */
    public boolean isServerError(int httpStatus) {
        return httpStatus >= 500 && httpStatus < 600;
    }

    /**
     * Checks if the HTTP status code indicates a retryable error.
     * Retryable errors are temporary failures that may succeed on retry.
     *
     * @param httpStatus the HTTP status code
     * @return true if the error is retryable
     */
    public boolean isRetryable(int httpStatus) {
        return RETRYABLE_STATUS_CODES.contains(httpStatus);
    }
}
