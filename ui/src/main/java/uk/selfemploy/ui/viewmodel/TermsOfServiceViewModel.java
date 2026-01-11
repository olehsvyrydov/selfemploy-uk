package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import uk.selfemploy.core.service.TermsAcceptanceService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * ViewModel for the Terms of Service dialog.
 * Manages scroll tracking, acceptance flow, version checking, and mode handling.
 *
 * SE-508: Terms of Service UI
 *
 * Key Features:
 * - Scroll tracking: Accept button only enabled after scrolling to bottom
 * - Version tracking: Re-show ToS if version changes
 * - Decline flow: User cannot use app without accepting ToS
 * - Settings access: View-only mode from Settings > Legal
 */
public class TermsOfServiceViewModel {

    /**
     * Current Terms of Service version.
     * Increment this when the ToS content changes.
     */
    public static final String CURRENT_TOS_VERSION = "1.0";

    /**
     * Last updated date of the current ToS version.
     */
    public static final LocalDate LAST_UPDATED_DATE = LocalDate.of(2026, 1, 1);

    /**
     * Application version for tracking which app version accepted the ToS.
     */
    private static final String APPLICATION_VERSION = "1.0.0";

    /**
     * Scroll threshold to consider "scrolled to bottom" (98% tolerance).
     */
    private static final double SCROLL_BOTTOM_THRESHOLD = 0.98;

    private final TermsAcceptanceService acceptanceService;

    // Scroll tracking properties
    private final DoubleProperty scrollProgress = new SimpleDoubleProperty(0.0);
    private final BooleanProperty scrolledToBottom = new SimpleBooleanProperty(false);
    private final StringProperty scrollPercentage = new SimpleStringProperty("0%");
    private final StringProperty scrollHintText = new SimpleStringProperty("Scroll to the bottom to enable acceptance");

    // Button state properties
    private final BooleanProperty acceptEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty firstLaunchMode = new SimpleBooleanProperty(true);
    private final BooleanProperty closeButtonVisible = new SimpleBooleanProperty(false);
    private final BooleanProperty actionButtonsVisible = new SimpleBooleanProperty(true);

    // Timestamp tracking
    private Instant scrollCompletedAt;
    private boolean hasReachedBottom = false;

    // Callbacks
    private Runnable onAcceptedCallback;
    private Runnable onDeclinedCallback;
    private Runnable onCloseCallback;
    private Runnable onPrintCallback;
    private Consumer<String> onScrollToSectionCallback;

    /**
     * Creates a new TermsOfServiceViewModel.
     *
     * @param acceptanceService The service for managing ToS acceptances
     */
    public TermsOfServiceViewModel(TermsAcceptanceService acceptanceService) {
        this.acceptanceService = acceptanceService;
        setupBindings();
        setupListeners();
    }

    private void setupBindings() {
        // Close button visible only when not in first launch mode
        closeButtonVisible.bind(firstLaunchMode.not());

        // Action buttons visible only in first launch mode
        actionButtonsVisible.bind(firstLaunchMode);
    }

    private void setupListeners() {
        // Listen to scroll progress changes
        scrollProgress.addListener((obs, oldVal, newVal) -> {
            updateScrollState(newVal.doubleValue());
        });
    }

    private void updateScrollState(double progress) {
        // Clamp progress to valid range
        double clampedProgress = Math.max(0.0, Math.min(1.0, progress));

        // Update percentage display
        int percentage = (int) Math.round(clampedProgress * 100);
        scrollPercentage.set(percentage + "%");

        // Check if scrolled to bottom (with tolerance)
        boolean isAtBottom = clampedProgress >= SCROLL_BOTTOM_THRESHOLD;
        scrolledToBottom.set(isAtBottom);

        if (isAtBottom) {
            // Record first time reaching bottom
            if (!hasReachedBottom) {
                hasReachedBottom = true;
                scrollCompletedAt = Instant.now();
            }
            // Enable accept button and update hint
            acceptEnabled.set(true);
            scrollHintText.set("You can now accept the terms");
        } else if (hasReachedBottom) {
            // Keep accept enabled once scrolled to bottom (UX decision)
            // User has proven they scrolled through
            // scrollHintText stays as completion hint
        } else {
            scrollHintText.set("Scroll to the bottom to enable acceptance");
        }
    }

    // === Getters for display values ===

    /**
     * Returns the current ToS version.
     */
    public String getTosVersion() {
        return CURRENT_TOS_VERSION;
    }

    /**
     * Returns the last updated date formatted for display.
     */
    public String getLastUpdatedDate() {
        return LAST_UPDATED_DATE.format(DateTimeFormatter.ofPattern("d MMMM yyyy"));
    }

    /**
     * Returns the dialog title.
     */
    public String getDialogTitle() {
        return "Terms of Service";
    }

    /**
     * Returns the ToS title.
     */
    public String getTosTitle() {
        return "Terms of Service";
    }

    /**
     * Returns the accept button text.
     */
    public String getAcceptButtonText() {
        return "I Accept";
    }

    /**
     * Returns the decline button text.
     */
    public String getDeclineButtonText() {
        return "I Decline";
    }

    /**
     * Returns the print button text.
     */
    public String getPrintButtonText() {
        return "Print / Export";
    }

    /**
     * Returns the decline confirmation dialog title.
     */
    public String getDeclineConfirmationTitle() {
        return "Cannot Continue Without Accepting Terms";
    }

    /**
     * Returns the decline confirmation message.
     */
    public String getDeclineConfirmationMessage() {
        return """
            You must accept the Terms of Service to use UK Self-Employment Manager.

            Without accepting:
            - You cannot record income or expenses
            - You cannot calculate tax liabilities
            - You cannot submit to HMRC

            This is required for legal protection of both you and us.
            """;
    }

    /**
     * Returns the current scroll hint text.
     */
    public String getScrollHintText() {
        return scrollHintText.get();
    }

    /**
     * Returns the scroll percentage as string (e.g., "65%").
     */
    public String getScrollPercentage() {
        return scrollPercentage.get();
    }

    // === State getters ===

    /**
     * Returns the current scroll progress (0.0 to 1.0).
     */
    public double getScrollProgress() {
        return scrollProgress.get();
    }

    /**
     * Returns whether user has scrolled to bottom.
     */
    public boolean isScrolledToBottom() {
        return scrolledToBottom.get();
    }

    /**
     * Returns whether the accept button is enabled.
     */
    public boolean isAcceptEnabled() {
        return acceptEnabled.get();
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
     * Returns whether the action buttons (Accept/Decline) should be visible.
     */
    public boolean areActionButtonsVisible() {
        return actionButtonsVisible.get();
    }

    /**
     * Returns the timestamp when user first scrolled to bottom.
     */
    public Instant getScrollCompletedAt() {
        return scrollCompletedAt;
    }

    // === Property accessors for binding ===

    public DoubleProperty scrollProgressProperty() {
        return scrollProgress;
    }

    public ReadOnlyBooleanProperty scrolledToBottomProperty() {
        return scrolledToBottom;
    }

    public ReadOnlyStringProperty scrollPercentageProperty() {
        return scrollPercentage;
    }

    public ReadOnlyStringProperty scrollHintTextProperty() {
        return scrollHintText;
    }

    public ReadOnlyBooleanProperty acceptEnabledProperty() {
        return acceptEnabled;
    }

    public BooleanProperty firstLaunchModeProperty() {
        return firstLaunchMode;
    }

    public ReadOnlyBooleanProperty closeButtonVisibleProperty() {
        return closeButtonVisible;
    }

    public ReadOnlyBooleanProperty actionButtonsVisibleProperty() {
        return actionButtonsVisible;
    }

    // === Setters ===

    /**
     * Sets the scroll progress (0.0 to 1.0).
     */
    public void setScrollProgress(double progress) {
        scrollProgress.set(progress);
    }

    /**
     * Sets whether in settings mode (shows close button, view-only).
     */
    public void setSettingsMode(boolean settingsMode) {
        firstLaunchMode.set(!settingsMode);
    }

    // === Callback setters ===

    /**
     * Sets the callback for successful acceptance.
     */
    public void setOnAcceptedCallback(Runnable callback) {
        this.onAcceptedCallback = callback;
    }

    /**
     * Sets the callback for decline action.
     */
    public void setOnDeclinedCallback(Runnable callback) {
        this.onDeclinedCallback = callback;
    }

    /**
     * Sets the callback for dialog close (settings mode only).
     */
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    /**
     * Sets the callback for print/export action.
     */
    public void setOnPrintCallback(Runnable callback) {
        this.onPrintCallback = callback;
    }

    /**
     * Sets the callback for scrolling to a section.
     */
    public void setOnScrollToSectionCallback(Consumer<String> callback) {
        this.onScrollToSectionCallback = callback;
    }

    // === Action handlers ===

    /**
     * Handles the accept button click.
     * Saves the acceptance and triggers the callback.
     *
     * @return true if acceptance was saved successfully, false otherwise
     */
    public boolean handleAccept() {
        if (!hasReachedBottom) {
            return false;
        }

        Instant acceptedAt = Instant.now();
        Instant scrolledAt = scrollCompletedAt != null ? scrollCompletedAt : acceptedAt;

        boolean saved = acceptanceService.saveAcceptance(
                CURRENT_TOS_VERSION,
                acceptedAt,
                scrolledAt,
                APPLICATION_VERSION
        );

        if (saved && onAcceptedCallback != null) {
            onAcceptedCallback.run();
        }

        return saved;
    }

    /**
     * Handles the decline button click.
     * Triggers the decline callback.
     */
    public void handleDecline() {
        if (onDeclinedCallback != null) {
            onDeclinedCallback.run();
        }
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
     * Handles the print/export button click.
     */
    public void handlePrint() {
        if (onPrintCallback != null) {
            onPrintCallback.run();
        }
    }

    /**
     * Handles scrolling to a specific section.
     *
     * @param sectionId The ID of the section to scroll to
     */
    public void handleScrollToSection(String sectionId) {
        if (onScrollToSectionCallback != null) {
            onScrollToSectionCallback.accept(sectionId);
        }
    }

    /**
     * Checks if the user needs to accept the Terms of Service.
     * Returns true if:
     * - No previous acceptance exists
     * - Previous acceptance was for a different version
     *
     * @return true if acceptance is required, false otherwise
     */
    public boolean requiresAcceptance() {
        Optional<String> acceptedVersion = acceptanceService.getAcceptedVersion();

        if (acceptedVersion.isEmpty()) {
            return true;
        }

        return !CURRENT_TOS_VERSION.equals(acceptedVersion.get());
    }
}
