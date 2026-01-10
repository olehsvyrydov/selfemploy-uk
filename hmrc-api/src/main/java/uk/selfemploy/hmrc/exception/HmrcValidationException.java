package uk.selfemploy.hmrc.exception;

import java.util.Collections;
import java.util.List;

/**
 * Exception for HMRC 4xx client/validation errors.
 * These errors are NOT retryable as they indicate issues with the request data.
 */
public class HmrcValidationException extends HmrcApiException {

    private final List<ValidationError> validationErrors;

    public HmrcValidationException(String message, String errorCode, int httpStatus) {
        super(message, errorCode, httpStatus);
        this.validationErrors = Collections.emptyList();
    }

    public HmrcValidationException(String message, String errorCode, int httpStatus, String userMessage) {
        super(message, errorCode, httpStatus, userMessage);
        this.validationErrors = Collections.emptyList();
    }

    public HmrcValidationException(String message, String errorCode, int httpStatus, List<ValidationError> errors) {
        super(message, errorCode, httpStatus);
        this.validationErrors = errors != null ? List.copyOf(errors) : Collections.emptyList();
    }

    public HmrcValidationException(String message, String errorCode, int httpStatus, String userMessage, List<ValidationError> errors) {
        super(message, errorCode, httpStatus, userMessage);
        this.validationErrors = errors != null ? List.copyOf(errors) : Collections.emptyList();
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    @Override
    public boolean isRetryable() {
        return false;
    }
}
