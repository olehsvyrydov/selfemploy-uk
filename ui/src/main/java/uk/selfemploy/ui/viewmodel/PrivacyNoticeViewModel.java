package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import uk.selfemploy.core.service.PrivacyAcknowledgmentService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * ViewModel for the Privacy Notice dialog.
 * Manages acknowledgment flow, version checking, and mode handling.
 *
 * SE-507: Privacy Notice UI
 *
 * Supports two modes:
 * 1. First Launch Mode - User must acknowledge before using the app
 * 2. Settings Mode - User can view the privacy notice from settings (close button visible)
 */
public class PrivacyNoticeViewModel {

    /**
     * Current privacy notice version.
     * Increment this when the privacy notice content changes.
     */
    public static final String CURRENT_PRIVACY_VERSION = "1.0";

    /**
     * Effective date of the current privacy notice version.
     */
    public static final LocalDate EFFECTIVE_DATE = LocalDate.of(2026, 1, 1);

    /**
     * URL to the full privacy policy (opens in browser).
     */
    private static final String FULL_PRIVACY_POLICY_URL = "https://github.com/selfemploy-uk/self-employment/blob/main/PRIVACY.md";

    /**
     * Application version for tracking which app version acknowledged the notice.
     */
    private static final String APPLICATION_VERSION = "1.0.0";

    private final PrivacyAcknowledgmentService acknowledgmentService;

    // Properties
    private final BooleanProperty acknowledged = new SimpleBooleanProperty(false);
    private final BooleanProperty continueEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty firstLaunchMode = new SimpleBooleanProperty(true);
    private final BooleanProperty closeButtonVisible = new SimpleBooleanProperty(false);

    // Callbacks
    private Runnable onAcknowledgedCallback;
    private Runnable onCloseCallback;
    private Consumer<String> onOpenBrowserCallback;

    /**
     * Creates a new PrivacyNoticeViewModel.
     *
     * @param acknowledgmentService The service for managing acknowledgments
     */
    public PrivacyNoticeViewModel(PrivacyAcknowledgmentService acknowledgmentService) {
        this.acknowledgmentService = acknowledgmentService;
        setupBindings();
    }

    private void setupBindings() {
        // Continue button is enabled when checkbox is checked
        continueEnabled.bind(acknowledged);

        // Close button is visible when not in first launch mode
        closeButtonVisible.bind(firstLaunchMode.not());
    }

    // === Getters ===

    /**
     * Returns the current privacy notice version.
     */
    public String getPrivacyVersion() {
        return CURRENT_PRIVACY_VERSION;
    }

    /**
     * Returns the effective date of the current privacy notice.
     */
    public String getEffectiveDate() {
        return EFFECTIVE_DATE.format(DateTimeFormatter.ofPattern("d MMMM yyyy"));
    }

    /**
     * Returns the URL to the full privacy policy.
     */
    public String getFullPrivacyPolicyUrl() {
        return FULL_PRIVACY_POLICY_URL;
    }

    /**
     * Returns whether the checkbox is acknowledged.
     */
    public boolean isAcknowledged() {
        return acknowledged.get();
    }

    /**
     * Returns whether the continue button is enabled.
     */
    public boolean isContinueEnabled() {
        return continueEnabled.get();
    }

    /**
     * Returns whether in first launch mode.
     */
    public boolean isFirstLaunchMode() {
        return firstLaunchMode.get();
    }

    /**
     * Returns whether the close button should be visible.
     */
    public boolean isCloseButtonVisible() {
        return closeButtonVisible.get();
    }

    /**
     * Returns the dialog title.
     */
    public String getDialogTitle() {
        return "Privacy Notice";
    }

    /**
     * Returns the privacy header title.
     */
    public String getPrivacyTitle() {
        return "Your Privacy Matters";
    }

    /**
     * Returns the acknowledgment checkbox label text.
     */
    public String getAcknowledgmentLabelText() {
        return "I have read and understand this privacy notice";
    }

    /**
     * Returns the continue button text.
     */
    public String getContinueButtonText() {
        return "Continue";
    }

    // === Properties ===

    public BooleanProperty acknowledgedProperty() {
        return acknowledged;
    }

    public ReadOnlyBooleanProperty continueEnabledProperty() {
        return continueEnabled;
    }

    public ReadOnlyBooleanProperty closeButtonVisibleProperty() {
        return closeButtonVisible;
    }

    public BooleanProperty firstLaunchModeProperty() {
        return firstLaunchMode;
    }

    // === Setters ===

    /**
     * Sets the acknowledged state.
     */
    public void setAcknowledged(boolean value) {
        acknowledged.set(value);
    }

    /**
     * Sets whether in settings mode (shows close button, allows closing without acknowledgment).
     */
    public void setSettingsMode(boolean settingsMode) {
        firstLaunchMode.set(!settingsMode);
    }

    /**
     * Sets the callback for successful acknowledgment.
     */
    public void setOnAcknowledgedCallback(Runnable callback) {
        this.onAcknowledgedCallback = callback;
    }

    /**
     * Sets the callback for dialog close (settings mode only).
     */
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    /**
     * Sets the callback for opening external URLs.
     */
    public void setOnOpenBrowserCallback(Consumer<String> callback) {
        this.onOpenBrowserCallback = callback;
    }

    // === Actions ===

    /**
     * Handles the continue button click.
     * Saves the acknowledgment and triggers the callback.
     *
     * @return true if acknowledgment was saved successfully, false otherwise
     */
    public boolean handleContinue() {
        if (!acknowledged.get()) {
            return false;
        }

        boolean saved = acknowledgmentService.saveAcknowledgment(
                CURRENT_PRIVACY_VERSION,
                Instant.now(),
                APPLICATION_VERSION
        );

        if (saved && onAcknowledgedCallback != null) {
            onAcknowledgedCallback.run();
        }

        return saved;
    }

    /**
     * Handles the close button click (settings mode only).
     */
    public void handleClose() {
        if (!firstLaunchMode.get() && onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    /**
     * Handles opening the full privacy policy in browser.
     */
    public void handleOpenFullPolicy() {
        if (onOpenBrowserCallback != null) {
            onOpenBrowserCallback.accept(FULL_PRIVACY_POLICY_URL);
        }
    }

    /**
     * Checks if the user needs to acknowledge the privacy notice.
     * Returns true if:
     * - No previous acknowledgment exists
     * - Previous acknowledgment was for a different version
     *
     * @return true if acknowledgment is required, false otherwise
     */
    public boolean requiresAcknowledgment() {
        Optional<String> acknowledgedVersion = acknowledgmentService.getAcknowledgedVersion();

        if (acknowledgedVersion.isEmpty()) {
            return true;
        }

        return !CURRENT_PRIVACY_VERSION.equals(acknowledgedVersion.get());
    }
}
