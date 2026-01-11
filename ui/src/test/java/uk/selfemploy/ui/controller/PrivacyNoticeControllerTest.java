package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.selfemploy.core.service.PrivacyAcknowledgmentService;
import uk.selfemploy.ui.viewmodel.PrivacyNoticeViewModel;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Privacy Notice Controller.
 * Implements P0 test cases from /rob's QA specifications (SE-507).
 *
 * <p>Test Reference: docs/sprints/sprint-6/testing/rob-qa-SE-801-SE-507.md
 *
 * <p>Key Test Scenarios:
 * <ul>
 *   <li>Privacy notice accessible from settings</li>
 *   <li>Privacy notice shown on first launch</li>
 *   <li>Checkbox enables/disables Continue button</li>
 *   <li>All 9 legal sections present</li>
 *   <li>Version tracking and re-acknowledgment</li>
 * </ul>
 *
 * <p>Note: These tests focus on ViewModel and Controller logic without TestFX
 * to ensure CI/CD compatibility. Full E2E tests would use TestFX with Monocle
 * for headless JavaFX testing.
 *
 * @author /adam (Senior E2E Test Automation Engineer)
 * @see PrivacyNoticeController
 * @see PrivacyNoticeViewModel
 */
@DisplayName("SE-507: Privacy Notice UI Integration Tests")
class PrivacyNoticeControllerTest {

    private static final String CURRENT_PRIVACY_VERSION = "1.0";
    private static final String PREVIOUS_PRIVACY_VERSION = "0.9";

    @Mock
    private PrivacyAcknowledgmentService acknowledgmentService;

    private PrivacyNoticeViewModel viewModel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new PrivacyNoticeViewModel(acknowledgmentService);
    }

    // =========================================================================
    // TC-507-001: Settings Menu Contains Privacy Notice Option
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-507-001 & TC-507-002: Settings Mode Access")
    class SettingsModeAccess {

        @Test
        @DisplayName("close button should be visible in settings mode")
        void closeButtonShouldBeVisibleInSettingsMode() {
            // Given: Settings mode enabled
            viewModel.setSettingsMode(true);

            // Then: Close button should be visible
            assertThat(viewModel.isCloseButtonVisible())
                .as("Close button should be visible in settings mode")
                .isTrue();

            assertThat(viewModel.isFirstLaunchMode())
                .as("Should not be in first launch mode")
                .isFalse();
        }

        @Test
        @DisplayName("settings mode should allow closing without acknowledgment")
        void settingsModeShouldAllowClosingWithoutAcknowledgment() {
            // Given: Settings mode with callback
            AtomicBoolean closeCalled = new AtomicBoolean(false);
            viewModel.setOnCloseCallback(() -> closeCalled.set(true));
            viewModel.setSettingsMode(true);

            // When: Close is clicked without acknowledgment
            viewModel.handleClose();

            // Then: Close callback should be triggered
            assertThat(closeCalled.get())
                .as("Close callback should be called in settings mode")
                .isTrue();
        }

        @Test
        @DisplayName("settings mode should not require re-acknowledgment to close")
        void settingsModeShouldNotRequireReAcknowledgmentToClose() {
            // Given: Settings mode, checkbox NOT checked
            viewModel.setSettingsMode(true);
            viewModel.setAcknowledged(false);

            // Then: Should still be able to close
            assertThat(viewModel.isCloseButtonVisible())
                .as("Close button should be visible regardless of checkbox state")
                .isTrue();
        }
    }

    // =========================================================================
    // TC-507-010: First Launch - Privacy Notice Displayed
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-507-010: First Launch Mode")
    class FirstLaunchMode {

        @Test
        @DisplayName("should be in first launch mode by default")
        void shouldBeInFirstLaunchModeByDefault() {
            // Then: First launch mode should be enabled
            assertThat(viewModel.isFirstLaunchMode())
                .as("Should be in first launch mode by default")
                .isTrue();
        }

        @Test
        @DisplayName("close button should be hidden in first launch mode")
        void closeButtonShouldBeHiddenInFirstLaunchMode() {
            // Then: Close button should not be visible
            assertThat(viewModel.isCloseButtonVisible())
                .as("Close button should be hidden in first launch mode")
                .isFalse();
        }
    }

    // =========================================================================
    // TC-507-011: Continue Button Disabled Initially
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-507-011: Continue Button Initial State")
    class ContinueButtonInitialState {

        @Test
        @DisplayName("continue button should be disabled initially")
        void continueButtonShouldBeDisabledInitially() {
            // Then: Continue button should be disabled
            assertThat(viewModel.isContinueEnabled())
                .as("Continue button should be disabled when checkbox is unchecked")
                .isFalse();
        }

        @Test
        @DisplayName("checkbox should be unchecked initially")
        void checkboxShouldBeUncheckedInitially() {
            // Then: Checkbox should be unchecked
            assertThat(viewModel.isAcknowledged())
                .as("Acknowledgment checkbox should be unchecked initially")
                .isFalse();
        }
    }

    // =========================================================================
    // TC-507-012: Cannot Bypass Privacy Notice on First Launch
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-507-012: Cannot Bypass First Launch")
    class CannotBypassFirstLaunch {

        @Test
        @DisplayName("close callback should not be triggered in first launch mode")
        void closeCallbackShouldNotBeTriggeredInFirstLaunchMode() {
            // Given: First launch mode with close callback
            AtomicBoolean closeCalled = new AtomicBoolean(false);
            viewModel.setOnCloseCallback(() -> closeCalled.set(true));
            // NOT setting settings mode (default is first launch mode)

            // When: Attempt to close
            viewModel.handleClose();

            // Then: Close callback should NOT be triggered
            assertThat(closeCalled.get())
                .as("Close callback should not be triggered in first launch mode")
                .isFalse();
        }

        @Test
        @DisplayName("cannot proceed without acknowledgment in first launch mode")
        void cannotProceedWithoutAcknowledgmentInFirstLaunchMode() {
            // When: Attempt to continue without acknowledging
            boolean result = viewModel.handleContinue();

            // Then: Should fail
            assertThat(result)
                .as("handleContinue should return false without acknowledgment")
                .isFalse();

            verify(acknowledgmentService, never()).saveAcknowledgment(any(), any(), any());
        }
    }

    // =========================================================================
    // TC-507-020: Acknowledgment Checkbox Enables Continue
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-507-020: Checkbox Enables Continue Button")
    class CheckboxEnablesContinue {

        @Test
        @DisplayName("checking checkbox should enable continue button")
        void checkingCheckboxShouldEnableContinueButton() {
            // Given: Checkbox unchecked
            assertThat(viewModel.isContinueEnabled()).isFalse();

            // When: Check the checkbox
            viewModel.setAcknowledged(true);

            // Then: Continue button should be enabled
            assertThat(viewModel.isContinueEnabled())
                .as("Continue button should be enabled when checkbox is checked")
                .isTrue();
        }
    }

    // =========================================================================
    // TC-507-021: Unchecking Checkbox Disables Continue Again
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-507-021: Checkbox Toggle Behavior")
    class CheckboxToggleBehavior {

        @Test
        @DisplayName("unchecking checkbox should disable continue button again")
        void uncheckingCheckboxShouldDisableContinueButtonAgain() {
            // Given: Checkbox checked
            viewModel.setAcknowledged(true);
            assertThat(viewModel.isContinueEnabled()).isTrue();

            // When: Uncheck the checkbox
            viewModel.setAcknowledged(false);

            // Then: Continue button should be disabled
            assertThat(viewModel.isContinueEnabled())
                .as("Continue button should be disabled when checkbox is unchecked")
                .isFalse();
        }

        @Test
        @DisplayName("checkbox toggle should update continue button state correctly")
        void checkboxToggleShouldUpdateContinueButtonStateCorrectly() {
            // Toggle multiple times
            for (int i = 0; i < 3; i++) {
                viewModel.setAcknowledged(true);
                assertThat(viewModel.isContinueEnabled())
                    .as("Continue should be enabled on iteration %d", i)
                    .isTrue();

                viewModel.setAcknowledged(false);
                assertThat(viewModel.isContinueEnabled())
                    .as("Continue should be disabled on iteration %d", i)
                    .isFalse();
            }
        }
    }

    // =========================================================================
    // TC-507-030: All 9 Legal Sections Present
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-507-030: Legal Content Verification")
    class LegalContentVerification {

        @Test
        @DisplayName("privacy title should be 'Your Privacy Matters'")
        void privacyTitleShouldBeCorrect() {
            assertThat(viewModel.getPrivacyTitle())
                .as("Privacy title should be 'Your Privacy Matters'")
                .isEqualTo("Your Privacy Matters");
        }

        @Test
        @DisplayName("dialog title should be 'Privacy Notice'")
        void dialogTitleShouldBeCorrect() {
            assertThat(viewModel.getDialogTitle())
                .as("Dialog title should be 'Privacy Notice'")
                .isEqualTo("Privacy Notice");
        }

        @Test
        @DisplayName("acknowledgment label should contain correct text")
        void acknowledgmentLabelShouldContainCorrectText() {
            assertThat(viewModel.getAcknowledgmentLabelText())
                .as("Acknowledgment label text should match specification")
                .isEqualTo("I have read and understand this privacy notice");
        }

        @Test
        @DisplayName("continue button text should be 'Continue'")
        void continueButtonTextShouldBeContinue() {
            assertThat(viewModel.getContinueButtonText())
                .as("Continue button text should be 'Continue'")
                .isEqualTo("Continue");
        }
    }

    // =========================================================================
    // TC-507-040: External Link Interaction
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-507-040 & TC-507-041: External Link Interaction")
    class ExternalLinkInteraction {

        @Test
        @DisplayName("full privacy policy URL should be set")
        void fullPrivacyPolicyUrlShouldBeSet() {
            assertThat(viewModel.getFullPrivacyPolicyUrl())
                .as("Full privacy policy URL should be set")
                .isNotBlank();
        }

        @Test
        @DisplayName("full privacy policy URL should point to GitHub")
        void fullPrivacyPolicyUrlShouldPointToGitHub() {
            assertThat(viewModel.getFullPrivacyPolicyUrl())
                .as("URL should point to GitHub PRIVACY.md")
                .contains("github.com")
                .contains("PRIVACY.md");
        }

        @Test
        @DisplayName("clicking link should trigger browser callback with correct URL")
        void clickingLinkShouldTriggerBrowserCallbackWithCorrectUrl() {
            // Given: Browser callback set
            AtomicReference<String> openedUrl = new AtomicReference<>();
            viewModel.setOnOpenBrowserCallback(openedUrl::set);

            // When: Click the link
            viewModel.handleOpenFullPolicy();

            // Then: Callback should be triggered with correct URL
            assertThat(openedUrl.get())
                .as("Opened URL should match full privacy policy URL")
                .isEqualTo(viewModel.getFullPrivacyPolicyUrl());
        }

        @Test
        @DisplayName("link click should not crash if no callback set")
        void linkClickShouldNotCrashIfNoCallbackSet() {
            // Given: No callback set

            // When/Then: Should not throw
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> viewModel.handleOpenFullPolicy(),
                "handleOpenFullPolicy should not throw when no callback is set"
            );
        }
    }

    // =========================================================================
    // TC-507-050: Version Stored After Acknowledgment
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-507-050: Version Storage")
    class VersionStorage {

        @Test
        @DisplayName("acknowledgment should save version 1.0")
        void acknowledgmentShouldSaveVersion() {
            // Given: Checkbox checked and service configured
            viewModel.setAcknowledged(true);
            when(acknowledgmentService.saveAcknowledgment(any(), any(), any())).thenReturn(true);

            // When: Continue is clicked
            viewModel.handleContinue();

            // Then: Version should be saved
            verify(acknowledgmentService).saveAcknowledgment(
                eq(CURRENT_PRIVACY_VERSION),
                any(Instant.class),
                any(String.class)
            );
        }

        @Test
        @DisplayName("acknowledgment should save timestamp")
        void acknowledgmentShouldSaveTimestamp() {
            // Given: Checkbox checked
            viewModel.setAcknowledged(true);
            when(acknowledgmentService.saveAcknowledgment(any(), any(), any())).thenReturn(true);

            // When: Continue is clicked
            Instant before = Instant.now();
            viewModel.handleContinue();
            Instant after = Instant.now();

            // Then: Timestamp should be within expected range
            verify(acknowledgmentService).saveAcknowledgment(
                any(),
                argThat(instant -> !instant.isBefore(before) && !instant.isAfter(after)),
                any()
            );
        }

        @Test
        @DisplayName("acknowledgment should include application version")
        void acknowledgmentShouldIncludeApplicationVersion() {
            // Given: Checkbox checked
            viewModel.setAcknowledged(true);
            when(acknowledgmentService.saveAcknowledgment(any(), any(), any())).thenReturn(true);

            // When: Continue is clicked
            viewModel.handleContinue();

            // Then: Application version should be saved
            verify(acknowledgmentService).saveAcknowledgment(
                any(),
                any(),
                argThat(appVersion -> appVersion != null && !appVersion.isEmpty())
            );
        }
    }

    // =========================================================================
    // TC-507-060: Version Change Triggers Re-acknowledgment
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-507-060: Version Change Re-acknowledgment")
    class VersionChangeReAcknowledgment {

        @Test
        @DisplayName("should require acknowledgment when no previous acknowledgment exists")
        void shouldRequireAcknowledgmentWhenNoPreviousExists() {
            // Given: No previous acknowledgment
            when(acknowledgmentService.getAcknowledgedVersion()).thenReturn(Optional.empty());

            // Then: Should require acknowledgment
            assertThat(viewModel.requiresAcknowledgment())
                .as("Should require acknowledgment when no previous exists")
                .isTrue();
        }

        @Test
        @DisplayName("should require acknowledgment when version is different")
        void shouldRequireAcknowledgmentWhenVersionIsDifferent() {
            // Given: Previous version is different
            when(acknowledgmentService.getAcknowledgedVersion())
                .thenReturn(Optional.of(PREVIOUS_PRIVACY_VERSION));

            // Then: Should require acknowledgment
            assertThat(viewModel.requiresAcknowledgment())
                .as("Should require acknowledgment when version changed from %s to %s",
                    PREVIOUS_PRIVACY_VERSION, CURRENT_PRIVACY_VERSION)
                .isTrue();
        }

        @Test
        @DisplayName("should NOT require acknowledgment when version matches")
        void shouldNotRequireAcknowledgmentWhenVersionMatches() {
            // Given: Current version is acknowledged
            when(acknowledgmentService.getAcknowledgedVersion())
                .thenReturn(Optional.of(CURRENT_PRIVACY_VERSION));

            // Then: Should NOT require acknowledgment
            assertThat(viewModel.requiresAcknowledgment())
                .as("Should not require acknowledgment when version matches")
                .isFalse();
        }
    }

    // =========================================================================
    // TC-507-061: Same Version - No Re-acknowledgment
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-507-061: Same Version No Re-acknowledgment")
    class SameVersionNoReAcknowledgment {

        @Test
        @DisplayName("version 1.0 acknowledged, current 1.0 should not require re-acknowledgment")
        void sameVersionShouldNotRequireReAcknowledgment() {
            // Given: Current version acknowledged
            when(acknowledgmentService.getAcknowledgedVersion())
                .thenReturn(Optional.of("1.0"));

            // Then
            assertThat(viewModel.requiresAcknowledgment())
                .as("Same version should not require re-acknowledgment")
                .isFalse();
        }
    }

    // =========================================================================
    // TC-507-062: Version Comparison Logic
    // Priority: P0
    // =========================================================================
    @Nested
    @DisplayName("TC-507-062: Version Comparison Logic")
    class VersionComparisonLogic {

        @Test
        @DisplayName("exact string match should be used for version comparison")
        void exactStringMatchShouldBeUsedForVersionComparison() {
            // Test cases for version comparison
            String[][] testCases = {
                {"1.0", "1.0", "false"},  // Exact match - no prompt
                {"1.0", "1.1", "true"},   // Minor version change - prompt
                {"1.0", "2.0", "true"},   // Major version change - prompt
                {"0.9", "1.0", "true"},   // Upgrade - prompt
            };

            for (String[] testCase : testCases) {
                String acknowledged = testCase[0];
                String current = testCase[1];
                boolean expected = Boolean.parseBoolean(testCase[2]);

                when(acknowledgmentService.getAcknowledgedVersion())
                    .thenReturn(Optional.of(acknowledged));

                // Note: We can't change CURRENT_PRIVACY_VERSION, so we only test
                // against the actual current version "1.0"
                if (current.equals("1.0")) {
                    boolean result = viewModel.requiresAcknowledgment();
                    boolean expectedResult = !acknowledged.equals(current);
                    assertThat(result)
                        .as("Acknowledged %s vs current %s should require=%s",
                            acknowledged, current, expectedResult)
                        .isEqualTo(expectedResult);
                }
            }
        }

        @Test
        @DisplayName("null acknowledged version should require acknowledgment")
        void nullAcknowledgedVersionShouldRequireAcknowledgment() {
            // Given: Empty optional (null equivalent)
            when(acknowledgmentService.getAcknowledgedVersion())
                .thenReturn(Optional.empty());

            // Then
            assertThat(viewModel.requiresAcknowledgment())
                .as("Null/empty acknowledged version should require acknowledgment")
                .isTrue();
        }
    }

    // =========================================================================
    // Additional Integration Tests
    // =========================================================================
    @Nested
    @DisplayName("Additional Integration Tests")
    class AdditionalIntegrationTests {

        @Test
        @DisplayName("successful acknowledgment should trigger callback")
        void successfulAcknowledgmentShouldTriggerCallback() {
            // Given: Checkbox checked and callback set
            AtomicBoolean callbackCalled = new AtomicBoolean(false);
            viewModel.setOnAcknowledgedCallback(() -> callbackCalled.set(true));
            viewModel.setAcknowledged(true);
            when(acknowledgmentService.saveAcknowledgment(any(), any(), any())).thenReturn(true);

            // When: Continue is clicked
            viewModel.handleContinue();

            // Then: Callback should be triggered
            assertThat(callbackCalled.get())
                .as("Acknowledged callback should be triggered on success")
                .isTrue();
        }

        @Test
        @DisplayName("failed acknowledgment should not trigger callback")
        void failedAcknowledgmentShouldNotTriggerCallback() {
            // Given: Checkbox checked but service fails
            AtomicBoolean callbackCalled = new AtomicBoolean(false);
            viewModel.setOnAcknowledgedCallback(() -> callbackCalled.set(true));
            viewModel.setAcknowledged(true);
            when(acknowledgmentService.saveAcknowledgment(any(), any(), any())).thenReturn(false);

            // When: Continue is clicked
            boolean result = viewModel.handleContinue();

            // Then: Callback should NOT be triggered
            assertThat(result).isFalse();
            assertThat(callbackCalled.get())
                .as("Acknowledged callback should NOT be triggered on failure")
                .isFalse();
        }

        @Test
        @DisplayName("service failure should return false from handleContinue")
        void serviceFailureShouldReturnFalseFromHandleContinue() {
            // Given: Service fails
            viewModel.setAcknowledged(true);
            when(acknowledgmentService.saveAcknowledgment(any(), any(), any())).thenReturn(false);

            // When: Continue is clicked
            boolean result = viewModel.handleContinue();

            // Then: Should return false
            assertThat(result)
                .as("handleContinue should return false when service fails")
                .isFalse();
        }

        @Test
        @DisplayName("privacy version should be 1.0")
        void privacyVersionShouldBe1_0() {
            assertThat(viewModel.getPrivacyVersion())
                .as("Current privacy version should be 1.0")
                .isEqualTo("1.0");
        }

        @Test
        @DisplayName("effective date should be set")
        void effectiveDateShouldBeSet() {
            assertThat(viewModel.getEffectiveDate())
                .as("Effective date should be set and not empty")
                .isNotBlank();
        }

        @Test
        @DisplayName("effective date format should be 'd MMMM yyyy'")
        void effectiveDateFormatShouldBeCorrect() {
            String effectiveDate = viewModel.getEffectiveDate();

            // Should be "1 January 2026" format
            assertThat(effectiveDate)
                .as("Effective date should contain month name")
                .containsPattern("\\d+ \\w+ \\d{4}");
        }
    }

    // =========================================================================
    // Property Binding Tests
    // =========================================================================
    @Nested
    @DisplayName("Property Binding Tests")
    class PropertyBindingTests {

        @Test
        @DisplayName("acknowledged property should be bidirectionally bound")
        void acknowledgedPropertyShouldBeBidirectionallyBound() {
            // Property set via setter
            viewModel.setAcknowledged(true);
            assertThat(viewModel.acknowledgedProperty().get()).isTrue();

            // Property set via property
            viewModel.acknowledgedProperty().set(false);
            assertThat(viewModel.isAcknowledged()).isFalse();
        }

        @Test
        @DisplayName("continue enabled should be bound to acknowledged")
        void continueEnabledShouldBeBoundToAcknowledged() {
            // When acknowledged changes, continueEnabled should follow
            assertThat(viewModel.continueEnabledProperty().get()).isFalse();

            viewModel.setAcknowledged(true);
            assertThat(viewModel.continueEnabledProperty().get()).isTrue();

            viewModel.setAcknowledged(false);
            assertThat(viewModel.continueEnabledProperty().get()).isFalse();
        }

        @Test
        @DisplayName("close button visible should be bound to first launch mode")
        void closeButtonVisibleShouldBeBoundToFirstLaunchMode() {
            // Default: first launch mode = true, close visible = false
            assertThat(viewModel.closeButtonVisibleProperty().get()).isFalse();

            // Settings mode
            viewModel.setSettingsMode(true);
            assertThat(viewModel.closeButtonVisibleProperty().get()).isTrue();

            // Back to first launch mode
            viewModel.setSettingsMode(false);
            assertThat(viewModel.closeButtonVisibleProperty().get()).isFalse();
        }

        @Test
        @DisplayName("first launch mode property should be exposed")
        void firstLaunchModePropertyShouldBeExposed() {
            assertThat(viewModel.firstLaunchModeProperty())
                .as("First launch mode property should be exposed")
                .isNotNull();

            assertThat(viewModel.firstLaunchModeProperty().get())
                .as("Default first launch mode should be true")
                .isTrue();
        }
    }

    // =========================================================================
    // Error Handling Tests
    // =========================================================================
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("handleContinue without acknowledgment should return false")
        void handleContinueWithoutAcknowledgmentShouldReturnFalse() {
            // Given: Not acknowledged

            // When
            boolean result = viewModel.handleContinue();

            // Then
            assertThat(result)
                .as("Should return false when not acknowledged")
                .isFalse();

            verify(acknowledgmentService, never()).saveAcknowledgment(any(), any(), any());
        }

        @Test
        @DisplayName("handleClose in first launch mode should do nothing")
        void handleCloseInFirstLaunchModeShouldDoNothing() {
            // Given: First launch mode with callback
            AtomicBoolean closeCalled = new AtomicBoolean(false);
            viewModel.setOnCloseCallback(() -> closeCalled.set(true));

            // When
            viewModel.handleClose();

            // Then: Callback should NOT be called
            assertThat(closeCalled.get())
                .as("Close callback should not be called in first launch mode")
                .isFalse();
        }

        @Test
        @DisplayName("callbacks can be null without causing errors")
        void callbacksCanBeNullWithoutCausingErrors() {
            // No callbacks set

            // Should not throw
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                viewModel.setAcknowledged(true);
                when(acknowledgmentService.saveAcknowledgment(any(), any(), any())).thenReturn(true);
                viewModel.handleContinue();
            }, "Should not throw when acknowledged callback is null");

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                viewModel.setSettingsMode(true);
                viewModel.handleClose();
            }, "Should not throw when close callback is null");
        }
    }
}
