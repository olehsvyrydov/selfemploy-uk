package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.selfemploy.core.service.TermsAcceptanceService;
import uk.selfemploy.ui.viewmodel.TermsOfServiceViewModel;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * E2E Integration tests for Terms of Service Controller and ViewModel.
 * Implements P0 test cases from /rob's QA specifications (SE-508).
 *
 * <p>Test Reference: docs/sprints/sprint-6/testing/rob-qa-SE-508-SE-803.md
 *
 * <p>P0 Test Cases Implemented:
 * <ul>
 *   <li>TC-508-001: ToS displayed after Privacy Notice on first launch</li>
 *   <li>TC-508-002: Accept button disabled until scroll complete (98% threshold)</li>
 *   <li>TC-508-012: Re-show ToS when version changes</li>
 *   <li>TC-508-013: No ToS dialog when same version accepted</li>
 *   <li>TC-508-017: Decline confirmation shows exit dialog</li>
 *   <li>TC-508-018: Decline confirmed exits application</li>
 *   <li>TC-508-021: Scroll completion timestamp recorded</li>
 *   <li>TC-508-024: Required legal sections present</li>
 * </ul>
 *
 * <p>Note: These tests focus on ViewModel and Controller logic without TestFX
 * to ensure CI/CD compatibility (headless execution). Full E2E tests with
 * actual JavaFX UI would require TestFX with Monocle for headless rendering.
 *
 * @author /adam (Senior E2E Test Automation Engineer)
 * @see TermsOfServiceController
 * @see TermsOfServiceViewModel
 */
@DisplayName("SE-508: Terms of Service E2E Integration Tests")
class TermsOfServiceControllerTest {

    private static final String CURRENT_TOS_VERSION = "1.0";
    private static final String PREVIOUS_TOS_VERSION = "0.9";
    private static final String NEW_TOS_VERSION = "1.1";
    private static final double SCROLL_THRESHOLD = 0.98;
    private static final String EXPECTED_LAST_UPDATED = "1 January 2026";
    private static final String APPLICATION_VERSION = "1.0.0";

    @Mock
    private TermsAcceptanceService acceptanceService;

    private TermsOfServiceViewModel viewModel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new TermsOfServiceViewModel(acceptanceService);
    }

    // =========================================================================
    // TC-508-001: ToS Displayed After Privacy Notice on First Launch
    // Priority: P0 (Critical)
    // AC Reference: AC-1
    // =========================================================================
    @Nested
    @DisplayName("TC-508-001: ToS Displayed After Privacy Notice on First Launch")
    class TosDisplayedOnFirstLaunch {

        @Test
        @DisplayName("should require acceptance on first launch when no previous acceptance exists")
        void shouldRequireAcceptanceOnFirstLaunch() {
            // Given: Fresh installation - no existing acceptance data
            when(acceptanceService.getAcceptedVersion()).thenReturn(Optional.empty());

            // Then: Should require ToS acceptance
            assertThat(viewModel.requiresAcceptance())
                .as("ToS should be required on first launch with no prior acceptance")
                .isTrue();
        }

        @Test
        @DisplayName("dialog title should be 'Terms of Service'")
        void dialogTitleShouldBeTermsOfService() {
            assertThat(viewModel.getDialogTitle())
                .as("Dialog title should be 'Terms of Service'")
                .isEqualTo("Terms of Service");
        }

        @Test
        @DisplayName("version label should display 'Version 1.0'")
        void versionLabelShouldDisplayCorrectVersion() {
            assertThat(viewModel.getTosVersion())
                .as("ToS version should be '1.0'")
                .isEqualTo(CURRENT_TOS_VERSION);
        }

        @Test
        @DisplayName("last updated date should be displayed as '1 January 2026'")
        void lastUpdatedDateShouldBeDisplayed() {
            assertThat(viewModel.getLastUpdatedDate())
                .as("Last updated date should be '1 January 2026'")
                .isEqualTo(EXPECTED_LAST_UPDATED);
        }

        @Test
        @DisplayName("accept button should be disabled initially")
        void acceptButtonShouldBeDisabledInitially() {
            assertThat(viewModel.isAcceptEnabled())
                .as("Accept button should be disabled before scrolling")
                .isFalse();
        }

        @Test
        @DisplayName("decline button text should be 'I Decline'")
        void declineButtonTextShouldBeCorrect() {
            assertThat(viewModel.getDeclineButtonText())
                .as("Decline button text should be 'I Decline'")
                .isEqualTo("I Decline");
        }

        @Test
        @DisplayName("close button should NOT be visible in first launch mode")
        void closeButtonShouldNotBeVisibleInFirstLaunchMode() {
            assertThat(viewModel.isCloseButtonVisible())
                .as("Close button should NOT be visible in first launch mode")
                .isFalse();
        }

        @Test
        @DisplayName("should be in first launch mode by default")
        void shouldBeInFirstLaunchModeByDefault() {
            assertThat(viewModel.isFirstLaunchMode())
                .as("Should be in first launch mode by default")
                .isTrue();
        }
    }

    // =========================================================================
    // TC-508-002: Accept Button Disabled Until Scroll Complete (98% threshold)
    // Priority: P0 (Critical)
    // AC Reference: AC-2, AC-3
    // =========================================================================
    @Nested
    @DisplayName("TC-508-002: Accept Button Disabled Until Scroll Complete")
    class AcceptButtonScrollThreshold {

        @Test
        @DisplayName("accept button should remain disabled at 0% scroll")
        void acceptButtonShouldRemainDisabledAtZeroPercent() {
            // Given: Initial state (0% scroll)
            viewModel.setScrollProgress(0.0);

            // Then: Accept button should be disabled
            assertThat(viewModel.isAcceptEnabled())
                .as("Accept button should be disabled at 0% scroll")
                .isFalse();
            assertThat(viewModel.getScrollPercentage())
                .as("Scroll percentage should show 0%")
                .isEqualTo("0%");
        }

        @Test
        @DisplayName("accept button should remain disabled at 25% scroll")
        void acceptButtonShouldRemainDisabledAt25Percent() {
            // When: Scroll to 25%
            viewModel.setScrollProgress(0.25);

            // Then: Accept button should be disabled
            assertThat(viewModel.isAcceptEnabled())
                .as("Accept button should be disabled at 25% scroll")
                .isFalse();
            assertThat(viewModel.getScrollPercentage())
                .as("Scroll percentage should show 25%")
                .isEqualTo("25%");
        }

        @Test
        @DisplayName("accept button should remain disabled at 50% scroll")
        void acceptButtonShouldRemainDisabledAt50Percent() {
            // When: Scroll to 50%
            viewModel.setScrollProgress(0.50);

            // Then: Accept button should be disabled
            assertThat(viewModel.isAcceptEnabled())
                .as("Accept button should be disabled at 50% scroll")
                .isFalse();
            assertThat(viewModel.getScrollPercentage())
                .as("Scroll percentage should show 50%")
                .isEqualTo("50%");
        }

        @Test
        @DisplayName("accept button should remain disabled at 97% scroll (below threshold)")
        void acceptButtonShouldRemainDisabledAt97Percent() {
            // When: Scroll to 97% (below 98% threshold)
            viewModel.setScrollProgress(0.97);

            // Then: Accept button should be disabled
            assertThat(viewModel.isAcceptEnabled())
                .as("Accept button should be disabled at 97% (below 98% threshold)")
                .isFalse();
            assertThat(viewModel.isScrolledToBottom())
                .as("Should NOT be considered scrolled to bottom at 97%")
                .isFalse();
        }

        @Test
        @DisplayName("accept button should become enabled at exactly 98% scroll (threshold)")
        void acceptButtonShouldBecomeEnabledAt98Percent() {
            // When: Scroll to 98% (threshold)
            viewModel.setScrollProgress(0.98);

            // Then: Accept button should be enabled
            assertThat(viewModel.isAcceptEnabled())
                .as("Accept button should be enabled at 98% threshold")
                .isTrue();
            assertThat(viewModel.isScrolledToBottom())
                .as("Should be considered scrolled to bottom at 98%")
                .isTrue();
        }

        @Test
        @DisplayName("accept button should be enabled at 100% scroll")
        void acceptButtonShouldBeEnabledAt100Percent() {
            // When: Scroll to 100%
            viewModel.setScrollProgress(1.0);

            // Then: Accept button should be enabled
            assertThat(viewModel.isAcceptEnabled())
                .as("Accept button should be enabled at 100% scroll")
                .isTrue();
            assertThat(viewModel.getScrollPercentage())
                .as("Scroll percentage should show 100%")
                .isEqualTo("100%");
        }

        @Test
        @DisplayName("scroll hint should change from 'Scroll to bottom' to 'You can now accept' at threshold")
        void scrollHintShouldChangeAtThreshold() {
            // Initial state
            assertThat(viewModel.getScrollHintText())
                .as("Initial hint should prompt scrolling")
                .isEqualTo("Scroll to the bottom to enable acceptance");

            // Scroll to below threshold
            viewModel.setScrollProgress(0.50);
            assertThat(viewModel.getScrollHintText())
                .as("Hint should still prompt scrolling at 50%")
                .isEqualTo("Scroll to the bottom to enable acceptance");

            // Scroll to threshold
            viewModel.setScrollProgress(0.98);
            assertThat(viewModel.getScrollHintText())
                .as("Hint should change to completion message at threshold")
                .isEqualTo("You can now accept the terms");
        }

        @Test
        @DisplayName("accept button should stay enabled after scrolling back up")
        void acceptButtonShouldStayEnabledAfterScrollingBackUp() {
            // Given: Scrolled to bottom
            viewModel.setScrollProgress(1.0);
            assertThat(viewModel.isAcceptEnabled()).isTrue();

            // When: Scroll back to 50%
            viewModel.setScrollProgress(0.50);

            // Then: Accept button should remain enabled (UX decision)
            assertThat(viewModel.isAcceptEnabled())
                .as("Accept button should remain enabled after scrolling back up")
                .isTrue();

            // When: Scroll back to top (0%)
            viewModel.setScrollProgress(0.0);

            // Then: Accept button should still remain enabled
            assertThat(viewModel.isAcceptEnabled())
                .as("Accept button should remain enabled even after scrolling to top")
                .isTrue();
        }
    }

    // =========================================================================
    // TC-508-012: Re-show ToS When Version Changes
    // Priority: P0 (Critical)
    // AC Reference: AC-7
    // =========================================================================
    @Nested
    @DisplayName("TC-508-012: Re-show ToS When Version Changes")
    class ReshowTosOnVersionChange {

        @Test
        @DisplayName("should require acceptance when previously accepted version differs from current")
        void shouldRequireAcceptanceWhenVersionChanged() {
            // Given: User previously accepted version 0.9, but current is 1.0
            when(acceptanceService.getAcceptedVersion())
                .thenReturn(Optional.of(PREVIOUS_TOS_VERSION));

            // Then: Should require ToS acceptance
            assertThat(viewModel.requiresAcceptance())
                .as("ToS should be required when version changed from %s to %s",
                    PREVIOUS_TOS_VERSION, CURRENT_TOS_VERSION)
                .isTrue();
        }

        @Test
        @DisplayName("should require acceptance for major version upgrade (0.9 -> 1.0)")
        void shouldRequireAcceptanceForMajorVersionUpgrade() {
            // Given: Upgrade from 0.9 to 1.0
            when(acceptanceService.getAcceptedVersion())
                .thenReturn(Optional.of("0.9"));

            // Then
            assertThat(viewModel.requiresAcceptance())
                .as("Major version upgrade should require re-acceptance")
                .isTrue();
        }

        @Test
        @DisplayName("should require acceptance for minor version upgrade (1.0 -> 1.1 scenario)")
        void shouldRequireAcceptanceForMinorVersionUpgrade() {
            // Given: User accepted older version
            when(acceptanceService.getAcceptedVersion())
                .thenReturn(Optional.of("0.8"));

            // Then: Any non-matching version should require acceptance
            assertThat(viewModel.requiresAcceptance())
                .as("Non-matching version should require re-acceptance")
                .isTrue();
        }

        @Test
        @DisplayName("accept button should be disabled until scroll complete on re-show")
        void acceptButtonShouldBeDisabledUntilScrollOnReshow() {
            // Given: Version changed requiring re-acceptance
            when(acceptanceService.getAcceptedVersion())
                .thenReturn(Optional.of(PREVIOUS_TOS_VERSION));

            // Then: New ViewModel should have disabled accept button
            TermsOfServiceViewModel newViewModel = new TermsOfServiceViewModel(acceptanceService);

            assertThat(newViewModel.isAcceptEnabled())
                .as("Accept button should be disabled (must scroll again)")
                .isFalse();
        }
    }

    // =========================================================================
    // TC-508-013: No ToS Dialog When Same Version Already Accepted
    // Priority: P0 (Critical)
    // AC Reference: AC-7
    // =========================================================================
    @Nested
    @DisplayName("TC-508-013: No ToS Dialog When Same Version Already Accepted")
    class NoDialogWhenSameVersionAccepted {

        @Test
        @DisplayName("should NOT require acceptance when current version already accepted")
        void shouldNotRequireAcceptanceWhenCurrentVersionAccepted() {
            // Given: User already accepted current version 1.0
            when(acceptanceService.getAcceptedVersion())
                .thenReturn(Optional.of(CURRENT_TOS_VERSION));

            // Then: Should NOT require ToS acceptance
            assertThat(viewModel.requiresAcceptance())
                .as("ToS should NOT be required when current version already accepted")
                .isFalse();
        }

        @Test
        @DisplayName("requiresAcceptance() should return false for matching version")
        void requiresAcceptanceShouldReturnFalseForMatchingVersion() {
            // Given: Exact version match
            when(acceptanceService.getAcceptedVersion())
                .thenReturn(Optional.of("1.0"));

            // When/Then
            assertThat(viewModel.requiresAcceptance())
                .as("requiresAcceptance() should return false for version 1.0 = 1.0")
                .isFalse();
        }
    }

    // =========================================================================
    // TC-508-017 & TC-508-018: Decline Confirmation Flow
    // Priority: P0 (Critical) - Mapped to TC-508-006 and TC-508-008 in test data
    // AC Reference: AC-6
    // =========================================================================
    @Nested
    @DisplayName("TC-508-006/017/018: Decline Confirmation and Exit Flow")
    class DeclineConfirmationFlow {

        @Test
        @DisplayName("decline should trigger confirmation callback")
        void declineShouldTriggerConfirmationCallback() {
            // Given: Decline callback set
            AtomicBoolean declineCallbackCalled = new AtomicBoolean(false);
            viewModel.setOnDeclinedCallback(() -> declineCallbackCalled.set(true));

            // When: Decline is clicked
            viewModel.handleDecline();

            // Then: Callback should be triggered
            assertThat(declineCallbackCalled.get())
                .as("Decline callback should be triggered")
                .isTrue();
        }

        @Test
        @DisplayName("decline confirmation title should be 'Cannot Continue Without Accepting Terms'")
        void declineConfirmationTitleShouldBeCorrect() {
            assertThat(viewModel.getDeclineConfirmationTitle())
                .as("Decline confirmation title should match specification")
                .isEqualTo("Cannot Continue Without Accepting Terms");
        }

        @Test
        @DisplayName("decline confirmation message should explain consequences")
        void declineConfirmationMessageShouldExplainConsequences() {
            String message = viewModel.getDeclineConfirmationMessage();

            assertThat(message)
                .as("Decline message should mention income/expenses restriction")
                .contains("cannot record income or expenses");
            assertThat(message)
                .as("Decline message should mention tax calculation restriction")
                .contains("cannot calculate tax liabilities");
            assertThat(message)
                .as("Decline message should mention HMRC submission restriction")
                .contains("cannot submit to HMRC");
            assertThat(message)
                .as("Decline message should mention legal protection")
                .contains("legal protection");
        }

        @Test
        @DisplayName("no acceptance record should be stored on decline")
        void noAcceptanceRecordShouldBeStoredOnDecline() {
            // Given: Decline callback set
            viewModel.setOnDeclinedCallback(() -> {});

            // When: Decline is clicked
            viewModel.handleDecline();

            // Then: No acceptance should be saved
            verify(acceptanceService, never()).saveAcceptance(any(), any(), any(), any());
        }
    }

    // =========================================================================
    // TC-508-021: Scroll Completion Timestamp Recorded
    // Priority: P0 (Critical)
    // AC Reference: AC-5
    // =========================================================================
    @Nested
    @DisplayName("TC-508-021: Scroll Completion Timestamp Recorded")
    class ScrollCompletionTimestamp {

        @Test
        @DisplayName("scroll completion timestamp should be null before reaching bottom")
        void scrollCompletionTimestampShouldBeNullBeforeBottom() {
            // Given: Initial state
            assertThat(viewModel.getScrollCompletedAt())
                .as("Scroll completion timestamp should be null initially")
                .isNull();

            // When: Scroll to 50% (not at bottom)
            viewModel.setScrollProgress(0.50);

            // Then: Still null
            assertThat(viewModel.getScrollCompletedAt())
                .as("Scroll completion timestamp should be null at 50%")
                .isNull();
        }

        @Test
        @DisplayName("scroll completion timestamp should be recorded when first reaching bottom")
        void scrollCompletionTimestampShouldBeRecordedWhenReachingBottom() {
            // Given: Note time before scroll
            Instant beforeScroll = Instant.now();

            // When: Scroll to bottom
            viewModel.setScrollProgress(1.0);

            // Then: Timestamp should be recorded
            Instant scrolledAt = viewModel.getScrollCompletedAt();
            assertThat(scrolledAt)
                .as("Scroll completion timestamp should be recorded")
                .isNotNull();
            assertThat(scrolledAt)
                .as("Scroll timestamp should be after test start time")
                .isAfterOrEqualTo(beforeScroll);
            assertThat(scrolledAt)
                .as("Scroll timestamp should be before current time")
                .isBeforeOrEqualTo(Instant.now());
        }

        @Test
        @DisplayName("scroll completion timestamp should NOT be updated on subsequent scrolls to bottom")
        void scrollCompletionTimestampShouldNotBeUpdatedOnSubsequentScrolls() {
            // Given: First scroll to bottom
            viewModel.setScrollProgress(1.0);
            Instant firstTimestamp = viewModel.getScrollCompletedAt();

            // When: Scroll back up and down again
            viewModel.setScrollProgress(0.50);
            // Small delay to ensure different timestamp if updated
            viewModel.setScrollProgress(1.0);

            // Then: Timestamp should be the original one
            assertThat(viewModel.getScrollCompletedAt())
                .as("Scroll timestamp should be immutable after first recording")
                .isEqualTo(firstTimestamp);
        }

        @Test
        @DisplayName("scroll completion timestamp should be stored in database on acceptance")
        void scrollCompletionTimestampShouldBeStoredInDatabaseOnAcceptance() {
            // Given: Scrolled to bottom
            viewModel.setScrollProgress(1.0);
            Instant scrolledAt = viewModel.getScrollCompletedAt();
            when(acceptanceService.saveAcceptance(any(), any(), any(), any())).thenReturn(true);

            // When: Accept
            viewModel.handleAccept();

            // Then: Both timestamps should be stored
            ArgumentCaptor<Instant> acceptedAtCaptor = ArgumentCaptor.forClass(Instant.class);
            ArgumentCaptor<Instant> scrolledAtCaptor = ArgumentCaptor.forClass(Instant.class);

            verify(acceptanceService).saveAcceptance(
                eq(CURRENT_TOS_VERSION),
                acceptedAtCaptor.capture(),
                scrolledAtCaptor.capture(),
                any(String.class)
            );

            assertThat(scrolledAtCaptor.getValue())
                .as("scroll_completed_at should be stored")
                .isNotNull();
            assertThat(acceptedAtCaptor.getValue())
                .as("accepted_at should be stored")
                .isNotNull();
            assertThat(scrolledAtCaptor.getValue())
                .as("scroll_completed_at should be before or equal to accepted_at")
                .isBeforeOrEqualTo(acceptedAtCaptor.getValue());
        }

        @Test
        @DisplayName("timestamps should be in UTC")
        void timestampsShouldBeInUtc() {
            // Given: Scroll to bottom
            viewModel.setScrollProgress(1.0);

            // Then: Timestamp is Instant (always UTC)
            Instant scrolledAt = viewModel.getScrollCompletedAt();
            assertThat(scrolledAt)
                .as("Scroll timestamp should be an Instant (UTC)")
                .isNotNull();

            // Instant.toString() always ends with 'Z' for UTC
            assertThat(scrolledAt.toString())
                .as("Timestamp should be in UTC format")
                .matches(".*Z$");
        }
    }

    // =========================================================================
    // TC-508-009/024: Required Legal Sections Present
    // Priority: P0 (Critical)
    // AC Reference: AC-4
    // Note: Full content verification requires FXML parsing; this tests constants
    // =========================================================================
    @Nested
    @DisplayName("TC-508-009/024: Required Legal Sections Present")
    class RequiredLegalSections {

        @Test
        @DisplayName("ToS title should be 'Terms of Service'")
        void tosTitleShouldBeCorrect() {
            assertThat(viewModel.getTosTitle())
                .as("ToS title should be 'Terms of Service'")
                .isEqualTo("Terms of Service");
        }

        @Test
        @DisplayName("accept button text should be 'I Accept'")
        void acceptButtonTextShouldBeCorrect() {
            assertThat(viewModel.getAcceptButtonText())
                .as("Accept button text should be 'I Accept'")
                .isEqualTo("I Accept");
        }

        @Test
        @DisplayName("print button text should be 'Print / Export'")
        void printButtonTextShouldBeCorrect() {
            assertThat(viewModel.getPrintButtonText())
                .as("Print button text should be 'Print / Export'")
                .isEqualTo("Print / Export");
        }

        @Test
        @DisplayName("print callback should be triggered when print clicked")
        void printCallbackShouldBeTriggeredWhenPrintClicked() {
            // Given: Print callback set
            AtomicBoolean printCalled = new AtomicBoolean(false);
            viewModel.setOnPrintCallback(() -> printCalled.set(true));

            // When: Print is clicked
            viewModel.handlePrint();

            // Then: Callback should be triggered
            assertThat(printCalled.get())
                .as("Print callback should be triggered")
                .isTrue();
        }
    }

    // =========================================================================
    // TC-508-005: Successful ToS Acceptance Flow
    // Priority: P0 (Critical)
    // AC Reference: AC-5
    // =========================================================================
    @Nested
    @DisplayName("TC-508-005: Successful ToS Acceptance Flow")
    class SuccessfulAcceptanceFlow {

        @Test
        @DisplayName("acceptance should save version, timestamps, and app version to database")
        void acceptanceShouldSaveAllDataToDatabase() {
            // Given: Scrolled to bottom
            viewModel.setScrollProgress(1.0);
            when(acceptanceService.saveAcceptance(any(), any(), any(), any())).thenReturn(true);

            // When: Accept
            boolean result = viewModel.handleAccept();

            // Then: Should save to database with all required fields
            assertThat(result)
                .as("handleAccept should return true on success")
                .isTrue();

            verify(acceptanceService).saveAcceptance(
                eq("1.0"),                    // tos_version
                any(Instant.class),           // accepted_at
                any(Instant.class),           // scroll_completed_at
                argThat((String v) -> v != null && !v.isEmpty())  // application_version
            );
        }

        @Test
        @DisplayName("acceptance should trigger callback on success")
        void acceptanceShouldTriggerCallbackOnSuccess() {
            // Given: Scrolled to bottom with callback
            AtomicBoolean acceptedCallbackCalled = new AtomicBoolean(false);
            viewModel.setOnAcceptedCallback(() -> acceptedCallbackCalled.set(true));
            viewModel.setScrollProgress(1.0);
            when(acceptanceService.saveAcceptance(any(), any(), any(), any())).thenReturn(true);

            // When: Accept
            viewModel.handleAccept();

            // Then: Callback should be triggered
            assertThat(acceptedCallbackCalled.get())
                .as("Accepted callback should be triggered on success")
                .isTrue();
        }

        @Test
        @DisplayName("acceptance should NOT trigger callback on failure")
        void acceptanceShouldNotTriggerCallbackOnFailure() {
            // Given: Scrolled to bottom but service fails
            AtomicBoolean acceptedCallbackCalled = new AtomicBoolean(false);
            viewModel.setOnAcceptedCallback(() -> acceptedCallbackCalled.set(true));
            viewModel.setScrollProgress(1.0);
            when(acceptanceService.saveAcceptance(any(), any(), any(), any())).thenReturn(false);

            // When: Accept
            boolean result = viewModel.handleAccept();

            // Then: Should return false and NOT trigger callback
            assertThat(result).isFalse();
            assertThat(acceptedCallbackCalled.get())
                .as("Callback should NOT be triggered on failure")
                .isFalse();
        }

        @Test
        @DisplayName("cannot accept without scrolling to bottom first")
        void cannotAcceptWithoutScrollingToBottom() {
            // Given: NOT scrolled to bottom
            viewModel.setScrollProgress(0.50);

            // When: Attempt to accept
            boolean result = viewModel.handleAccept();

            // Then: Should fail
            assertThat(result)
                .as("handleAccept should return false when not scrolled to bottom")
                .isFalse();
            verify(acceptanceService, never()).saveAcceptance(any(), any(), any(), any());
        }
    }

    // =========================================================================
    // Settings Access Mode Tests (TC-508-014, TC-508-015, TC-508-016)
    // Priority: P1 (Important)
    // AC Reference: AC-8
    // =========================================================================
    @Nested
    @DisplayName("Settings Access Mode (View-Only)")
    class SettingsAccessMode {

        @Test
        @DisplayName("close button should be visible in settings mode")
        void closeButtonShouldBeVisibleInSettingsMode() {
            // When: Settings mode enabled
            viewModel.setSettingsMode(true);

            // Then: Close button should be visible
            assertThat(viewModel.isCloseButtonVisible())
                .as("Close button should be visible in settings mode")
                .isTrue();
        }

        @Test
        @DisplayName("action buttons (Accept/Decline) should be hidden in settings mode")
        void actionButtonsShouldBeHiddenInSettingsMode() {
            // When: Settings mode enabled
            viewModel.setSettingsMode(true);

            // Then: Action buttons should be hidden
            assertThat(viewModel.areActionButtonsVisible())
                .as("Action buttons should be hidden in settings mode")
                .isFalse();
        }

        @Test
        @DisplayName("close callback should be triggered in settings mode")
        void closeCallbackShouldBeTriggeredInSettingsMode() {
            // Given: Settings mode with callback
            AtomicBoolean closeCalled = new AtomicBoolean(false);
            viewModel.setOnCloseCallback(() -> closeCalled.set(true));
            viewModel.setSettingsMode(true);

            // When: Close is clicked
            viewModel.handleClose();

            // Then: Callback should be triggered
            assertThat(closeCalled.get())
                .as("Close callback should be triggered in settings mode")
                .isTrue();
        }

        @Test
        @DisplayName("close callback should NOT be triggered in first launch mode")
        void closeCallbackShouldNotBeTriggeredInFirstLaunchMode() {
            // Given: First launch mode (default) with callback
            AtomicBoolean closeCalled = new AtomicBoolean(false);
            viewModel.setOnCloseCallback(() -> closeCalled.set(true));

            // When: Attempt to close
            viewModel.handleClose();

            // Then: Callback should NOT be triggered
            assertThat(closeCalled.get())
                .as("Close callback should NOT be triggered in first launch mode")
                .isFalse();
        }

        @Test
        @DisplayName("settings mode should not require scrolling to close")
        void settingsModeShouldNotRequireScrollingToClose() {
            // Given: Settings mode, NOT scrolled
            viewModel.setSettingsMode(true);

            // Then: Can close without scrolling
            assertThat(viewModel.isCloseButtonVisible())
                .as("Close button visible regardless of scroll state")
                .isTrue();
        }
    }

    // =========================================================================
    // Table of Contents Navigation (TC-508-010)
    // Priority: P1 (Important)
    // AC Reference: AC-4
    // =========================================================================
    @Nested
    @DisplayName("Table of Contents Navigation")
    class TableOfContentsNavigation {

        @Test
        @DisplayName("scroll to section callback should be triggered with correct section ID")
        void scrollToSectionCallbackShouldBeTriggeredWithCorrectId() {
            // Given: Scroll to section callback set
            AtomicReference<String> scrolledToSection = new AtomicReference<>();
            viewModel.setOnScrollToSectionCallback(scrolledToSection::set);

            // When: Navigate to section 3
            viewModel.handleScrollToSection("section-3");

            // Then: Callback should receive correct section ID
            assertThat(scrolledToSection.get())
                .as("Should scroll to section-3")
                .isEqualTo("section-3");
        }

        @Test
        @DisplayName("scroll to section callback should handle multiple navigations")
        void scrollToSectionCallbackShouldHandleMultipleNavigations() {
            // Given: Callback set
            AtomicReference<String> lastSection = new AtomicReference<>();
            viewModel.setOnScrollToSectionCallback(lastSection::set);

            // When: Navigate to multiple sections
            viewModel.handleScrollToSection("section-7");
            assertThat(lastSection.get()).isEqualTo("section-7");

            viewModel.handleScrollToSection("section-1");
            assertThat(lastSection.get()).isEqualTo("section-1");

            viewModel.handleScrollToSection("section-14");
            assertThat(lastSection.get()).isEqualTo("section-14");
        }

        @Test
        @DisplayName("scroll to section should not crash if no callback set")
        void scrollToSectionShouldNotCrashIfNoCallbackSet() {
            // Given: No callback set

            // When/Then: Should not throw
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> viewModel.handleScrollToSection("section-1"),
                "Should not throw when no callback is set"
            );
        }
    }

    // =========================================================================
    // Property Binding Tests
    // =========================================================================
    @Nested
    @DisplayName("Property Binding Tests")
    class PropertyBindingTests {

        @Test
        @DisplayName("scrollProgress property should be bidirectionally bound")
        void scrollProgressPropertyShouldBeBidirectionallyBound() {
            // Via setter
            viewModel.setScrollProgress(0.75);
            assertThat(viewModel.scrollProgressProperty().get())
                .as("Property should match setter value")
                .isEqualTo(0.75);

            // Via property
            viewModel.scrollProgressProperty().set(0.25);
            assertThat(viewModel.getScrollProgress())
                .as("Getter should match property value")
                .isEqualTo(0.25);
        }

        @Test
        @DisplayName("acceptEnabled property should reflect scroll state")
        void acceptEnabledPropertyShouldReflectScrollState() {
            // Initial
            assertThat(viewModel.acceptEnabledProperty().get())
                .as("Should be disabled initially")
                .isFalse();

            // After scrolling to bottom
            viewModel.setScrollProgress(1.0);
            assertThat(viewModel.acceptEnabledProperty().get())
                .as("Should be enabled after scrolling to bottom")
                .isTrue();
        }

        @Test
        @DisplayName("scrolledToBottom property should update with scroll progress")
        void scrolledToBottomPropertyShouldUpdateWithScrollProgress() {
            // Below threshold
            viewModel.setScrollProgress(0.97);
            assertThat(viewModel.scrolledToBottomProperty().get())
                .as("Should be false below threshold")
                .isFalse();

            // At threshold
            viewModel.setScrollProgress(0.98);
            assertThat(viewModel.scrolledToBottomProperty().get())
                .as("Should be true at threshold")
                .isTrue();
        }

        @Test
        @DisplayName("closeButtonVisible should be bound to firstLaunchMode")
        void closeButtonVisibleShouldBeBoundToFirstLaunchMode() {
            // Default: first launch mode
            assertThat(viewModel.closeButtonVisibleProperty().get())
                .as("Close button hidden in first launch mode")
                .isFalse();

            // Settings mode
            viewModel.setSettingsMode(true);
            assertThat(viewModel.closeButtonVisibleProperty().get())
                .as("Close button visible in settings mode")
                .isTrue();
        }

        @Test
        @DisplayName("actionButtonsVisible should be bound to firstLaunchMode")
        void actionButtonsVisibleShouldBeBoundToFirstLaunchMode() {
            // Default: first launch mode
            assertThat(viewModel.actionButtonsVisibleProperty().get())
                .as("Action buttons visible in first launch mode")
                .isTrue();

            // Settings mode
            viewModel.setSettingsMode(true);
            assertThat(viewModel.actionButtonsVisibleProperty().get())
                .as("Action buttons hidden in settings mode")
                .isFalse();
        }
    }

    // =========================================================================
    // Edge Cases and Error Handling
    // =========================================================================
    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("scroll progress above 1.0 should be clamped")
        void scrollProgressAbove1ShouldBeClamped() {
            // When: Set scroll progress above 1.0
            viewModel.setScrollProgress(1.5);

            // Then: Should be clamped to 100%
            assertThat(viewModel.getScrollPercentage())
                .as("Percentage should be clamped to 100%")
                .isEqualTo("100%");
            assertThat(viewModel.isScrolledToBottom())
                .as("Should be considered at bottom")
                .isTrue();
        }

        @Test
        @DisplayName("negative scroll progress should be clamped to 0")
        void negativeScrollProgressShouldBeClampedToZero() {
            // When: Set negative scroll progress
            viewModel.setScrollProgress(-0.5);

            // Then: Should be clamped to 0%
            assertThat(viewModel.getScrollPercentage())
                .as("Percentage should be clamped to 0%")
                .isEqualTo("0%");
        }

        @Test
        @DisplayName("callbacks can be null without causing NullPointerException")
        void callbacksCanBeNullWithoutNpe() {
            // No callbacks set

            // Should not throw
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                viewModel.setScrollProgress(1.0);
                when(acceptanceService.saveAcceptance(any(), any(), any(), any())).thenReturn(true);
                viewModel.handleAccept();
            }, "handleAccept should not throw with null callback");

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                viewModel.handleDecline();
            }, "handleDecline should not throw with null callback");

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                viewModel.handlePrint();
            }, "handlePrint should not throw with null callback");

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                viewModel.setSettingsMode(true);
                viewModel.handleClose();
            }, "handleClose should not throw with null callback");
        }

        @Test
        @DisplayName("rapid scrolling should be handled correctly")
        void rapidScrollingShouldBeHandledCorrectly() {
            // Simulate rapid scrolling
            for (double i = 0; i <= 1.0; i += 0.1) {
                viewModel.setScrollProgress(i);
            }

            // Should reach enabled state
            assertThat(viewModel.isAcceptEnabled())
                .as("Accept should be enabled after rapid scrolling to bottom")
                .isTrue();
            assertThat(viewModel.getScrollCompletedAt())
                .as("Scroll timestamp should be recorded")
                .isNotNull();
        }
    }
}
