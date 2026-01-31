package uk.selfemploy.ui.util;

/**
 * Data carrier for submission error dialog rendering.
 *
 * <p>SE-10E-001: Created as a lightweight record that carries all the information
 * needed to render an actionable submission error dialog. This is a pure data carrier
 * with no business logic - it is created by {@link HmrcErrorGuidance#buildErrorDisplay(Throwable)}
 * and consumed by {@code QuarterlyReviewDialog.showErrorDialog()}.</p>
 *
 * @param title        dialog header title (e.g., "National Insurance Number Not Set")
 * @param message      user-friendly explanation of what went wrong
 * @param guidance     actionable steps the user can take to resolve the error
 * @param errorCode    raw error code for support reference (e.g., "NINO_REQUIRED"), may be null
 * @param retryable    whether a "Try Again" hint should be shown
 * @param settingsError whether an "Open Settings" button should be shown
 */
public record SubmissionErrorDisplay(
    String title,
    String message,
    String guidance,
    String errorCode,
    boolean retryable,
    boolean settingsError
) {}
