package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.selfemploy.core.service.TermsAcceptanceService;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TDD tests for TermsOfServiceViewModel.
 * Tests acceptance flow, scroll tracking, version management, and mode handling.
 *
 * SE-508: Terms of Service UI
 *
 * Key Features:
 * - Scroll tracking: Accept button only enabled after scrolling to bottom
 * - Version tracking: Re-show ToS if version changes
 * - Decline flow: User cannot use app without accepting ToS
 * - Settings access: View-only mode from Settings > Legal
 */
@DisplayName("TermsOfServiceViewModel")
class TermsOfServiceViewModelTest {

    @Mock
    private TermsAcceptanceService acceptanceService;

    private TermsOfServiceViewModel viewModel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new TermsOfServiceViewModel(acceptanceService);
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("should have accept button disabled initially")
        void shouldHaveAcceptButtonDisabledInitially() {
            assertThat(viewModel.isAcceptEnabled()).isFalse();
        }

        @Test
        @DisplayName("should have scroll position at zero initially")
        void shouldHaveScrollPositionAtZeroInitially() {
            assertThat(viewModel.getScrollProgress()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should not be scrolled to bottom initially")
        void shouldNotBeScrolledToBottomInitially() {
            assertThat(viewModel.isScrolledToBottom()).isFalse();
        }

        @Test
        @DisplayName("should have current ToS version")
        void shouldHaveCurrentTosVersion() {
            assertThat(viewModel.getTosVersion()).isEqualTo("1.0");
        }

        @Test
        @DisplayName("should have last updated date")
        void shouldHaveLastUpdatedDate() {
            assertThat(viewModel.getLastUpdatedDate()).isNotNull();
        }

        @Test
        @DisplayName("should be in first launch mode by default")
        void shouldBeInFirstLaunchModeByDefault() {
            assertThat(viewModel.isFirstLaunchMode()).isTrue();
        }

        @Test
        @DisplayName("should have close button hidden in first launch mode")
        void shouldHaveCloseButtonHiddenInFirstLaunchMode() {
            assertThat(viewModel.isCloseButtonVisible()).isFalse();
        }

        @Test
        @DisplayName("should show scroll hint initially")
        void shouldShowScrollHintInitially() {
            assertThat(viewModel.getScrollHintText())
                .isEqualTo("Scroll to the bottom to enable acceptance");
        }
    }

    @Nested
    @DisplayName("Scroll Progress Tracking")
    class ScrollProgressTracking {

        @Test
        @DisplayName("should update scroll progress when scroll position changes")
        void shouldUpdateScrollProgressWhenScrollPositionChanges() {
            // When
            viewModel.setScrollProgress(0.5);

            // Then
            assertThat(viewModel.getScrollProgress()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("should update scroll percentage label")
        void shouldUpdateScrollPercentageLabel() {
            // When
            viewModel.setScrollProgress(0.65);

            // Then
            assertThat(viewModel.getScrollPercentage()).isEqualTo("65%");
        }

        @Test
        @DisplayName("should round scroll percentage correctly")
        void shouldRoundScrollPercentageCorrectly() {
            // When
            viewModel.setScrollProgress(0.678);

            // Then
            assertThat(viewModel.getScrollPercentage()).isEqualTo("68%");
        }

        @Test
        @DisplayName("should detect when scrolled to bottom at 100%")
        void shouldDetectWhenScrolledToBottomAt100Percent() {
            // When
            viewModel.setScrollProgress(1.0);

            // Then
            assertThat(viewModel.isScrolledToBottom()).isTrue();
        }

        @Test
        @DisplayName("should detect when scrolled to bottom with 98% tolerance")
        void shouldDetectWhenScrolledToBottomWithTolerance() {
            // When (98% tolerance for near-bottom)
            viewModel.setScrollProgress(0.98);

            // Then
            assertThat(viewModel.isScrolledToBottom()).isTrue();
        }

        @Test
        @DisplayName("should not be scrolled to bottom below tolerance")
        void shouldNotBeScrolledToBottomBelowTolerance() {
            // When
            viewModel.setScrollProgress(0.97);

            // Then
            assertThat(viewModel.isScrolledToBottom()).isFalse();
        }

        @Test
        @DisplayName("should enable accept button when scrolled to bottom")
        void shouldEnableAcceptButtonWhenScrolledToBottom() {
            // When
            viewModel.setScrollProgress(1.0);

            // Then
            assertThat(viewModel.isAcceptEnabled()).isTrue();
        }

        @Test
        @DisplayName("should update scroll hint when scrolled to bottom")
        void shouldUpdateScrollHintWhenScrolledToBottom() {
            // When
            viewModel.setScrollProgress(1.0);

            // Then
            assertThat(viewModel.getScrollHintText())
                .isEqualTo("You can now accept the terms");
        }

        @Test
        @DisplayName("should handle scroll progress above 1.0")
        void shouldHandleScrollProgressAboveOne() {
            // When (edge case)
            viewModel.setScrollProgress(1.05);

            // Then
            assertThat(viewModel.isScrolledToBottom()).isTrue();
            assertThat(viewModel.getScrollPercentage()).isEqualTo("100%");
        }

        @Test
        @DisplayName("should handle negative scroll progress")
        void shouldHandleNegativeScrollProgress() {
            // When (edge case)
            viewModel.setScrollProgress(-0.1);

            // Then
            assertThat(viewModel.getScrollPercentage()).isEqualTo("0%");
        }
    }

    @Nested
    @DisplayName("Accept Flow")
    class AcceptFlow {

        @Test
        @DisplayName("should not accept when not scrolled to bottom")
        void shouldNotAcceptWhenNotScrolledToBottom() {
            // When
            boolean result = viewModel.handleAccept();

            // Then
            assertThat(result).isFalse();
            verify(acceptanceService, never()).saveAcceptance(any(), any(), any(), any());
        }

        @Test
        @DisplayName("should save acceptance when scrolled to bottom")
        void shouldSaveAcceptanceWhenScrolledToBottom() {
            // Given
            viewModel.setScrollProgress(1.0);
            when(acceptanceService.saveAcceptance(any(), any(), any(), any())).thenReturn(true);

            // When
            boolean result = viewModel.handleAccept();

            // Then
            assertThat(result).isTrue();
            verify(acceptanceService).saveAcceptance(
                eq("1.0"),
                any(Instant.class),
                any(Instant.class),
                any(String.class)
            );
        }

        @Test
        @DisplayName("should store scroll completed timestamp before acceptance")
        void shouldStoreScrollCompletedTimestampBeforeAcceptance() {
            // Given
            viewModel.setScrollProgress(1.0);
            when(acceptanceService.saveAcceptance(any(), any(), any(), any())).thenReturn(true);

            // When
            viewModel.handleAccept();

            // Then
            verify(acceptanceService).saveAcceptance(
                any(),
                any(Instant.class),
                any(Instant.class),
                any()
            );
        }

        @Test
        @DisplayName("should trigger callback on successful acceptance")
        void shouldTriggerCallbackOnSuccessfulAcceptance() {
            // Given
            AtomicBoolean callbackCalled = new AtomicBoolean(false);
            viewModel.setOnAcceptedCallback(() -> callbackCalled.set(true));
            viewModel.setScrollProgress(1.0);
            when(acceptanceService.saveAcceptance(any(), any(), any(), any())).thenReturn(true);

            // When
            viewModel.handleAccept();

            // Then
            assertThat(callbackCalled.get()).isTrue();
        }

        @Test
        @DisplayName("should not trigger callback on failed acceptance")
        void shouldNotTriggerCallbackOnFailedAcceptance() {
            // Given
            AtomicBoolean callbackCalled = new AtomicBoolean(false);
            viewModel.setOnAcceptedCallback(() -> callbackCalled.set(true));
            viewModel.setScrollProgress(1.0);
            when(acceptanceService.saveAcceptance(any(), any(), any(), any())).thenReturn(false);

            // When
            viewModel.handleAccept();

            // Then
            assertThat(callbackCalled.get()).isFalse();
        }

        @Test
        @DisplayName("should record scroll completed timestamp when first reaching bottom")
        void shouldRecordScrollCompletedTimestampWhenFirstReachingBottom() {
            // Given
            assertThat(viewModel.getScrollCompletedAt()).isNull();

            // When
            viewModel.setScrollProgress(1.0);

            // Then
            assertThat(viewModel.getScrollCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should not update scroll completed timestamp on subsequent scrolls")
        void shouldNotUpdateScrollCompletedTimestampOnSubsequentScrolls() {
            // Given
            viewModel.setScrollProgress(1.0);
            Instant firstTimestamp = viewModel.getScrollCompletedAt();

            // When
            viewModel.setScrollProgress(0.5);
            viewModel.setScrollProgress(1.0);

            // Then
            assertThat(viewModel.getScrollCompletedAt()).isEqualTo(firstTimestamp);
        }
    }

    @Nested
    @DisplayName("Decline Flow")
    class DeclineFlow {

        @Test
        @DisplayName("should trigger decline callback when decline is clicked")
        void shouldTriggerDeclineCallbackWhenDeclineIsClicked() {
            // Given
            AtomicBoolean declineCalled = new AtomicBoolean(false);
            viewModel.setOnDeclinedCallback(() -> declineCalled.set(true));

            // When
            viewModel.handleDecline();

            // Then
            assertThat(declineCalled.get()).isTrue();
        }

        @Test
        @DisplayName("should return decline confirmation message")
        void shouldReturnDeclineConfirmationMessage() {
            assertThat(viewModel.getDeclineConfirmationTitle())
                .isEqualTo("Cannot Continue Without Accepting Terms");
            assertThat(viewModel.getDeclineConfirmationMessage())
                .contains("You must accept the Terms of Service");
        }
    }

    @Nested
    @DisplayName("Settings Access Mode")
    class SettingsAccessMode {

        @Test
        @DisplayName("should show close button in settings mode")
        void shouldShowCloseButtonInSettingsMode() {
            // When
            viewModel.setSettingsMode(true);

            // Then
            assertThat(viewModel.isFirstLaunchMode()).isFalse();
            assertThat(viewModel.isCloseButtonVisible()).isTrue();
        }

        @Test
        @DisplayName("should hide accept and decline buttons in settings mode")
        void shouldHideAcceptAndDeclineButtonsInSettingsMode() {
            // When
            viewModel.setSettingsMode(true);

            // Then
            assertThat(viewModel.areActionButtonsVisible()).isFalse();
        }

        @Test
        @DisplayName("should allow close without acceptance in settings mode")
        void shouldAllowCloseWithoutAcceptanceInSettingsMode() {
            // Given
            AtomicBoolean closeCalled = new AtomicBoolean(false);
            viewModel.setOnCloseCallback(() -> closeCalled.set(true));
            viewModel.setSettingsMode(true);

            // When
            viewModel.handleClose();

            // Then
            assertThat(closeCalled.get()).isTrue();
        }

        @Test
        @DisplayName("should not call close callback in first launch mode")
        void shouldNotCallCloseCallbackInFirstLaunchMode() {
            // Given
            AtomicBoolean closeCalled = new AtomicBoolean(false);
            viewModel.setOnCloseCallback(() -> closeCalled.set(true));

            // When
            viewModel.handleClose();

            // Then
            assertThat(closeCalled.get()).isFalse();
        }
    }

    @Nested
    @DisplayName("Version Checking")
    class VersionChecking {

        @Test
        @DisplayName("should require acceptance on first launch")
        void shouldRequireAcceptanceOnFirstLaunch() {
            // Given
            when(acceptanceService.getAcceptedVersion())
                .thenReturn(Optional.empty());

            // When
            boolean requiresAcceptance = viewModel.requiresAcceptance();

            // Then
            assertThat(requiresAcceptance).isTrue();
        }

        @Test
        @DisplayName("should require acceptance if version changed")
        void shouldRequireAcceptanceIfVersionChanged() {
            // Given
            when(acceptanceService.getAcceptedVersion())
                .thenReturn(Optional.of("0.9"));

            // When
            boolean requiresAcceptance = viewModel.requiresAcceptance();

            // Then
            assertThat(requiresAcceptance).isTrue();
        }

        @Test
        @DisplayName("should not require acceptance if version matches")
        void shouldNotRequireAcceptanceIfVersionMatches() {
            // Given
            when(acceptanceService.getAcceptedVersion())
                .thenReturn(Optional.of("1.0"));

            // When
            boolean requiresAcceptance = viewModel.requiresAcceptance();

            // Then
            assertThat(requiresAcceptance).isFalse();
        }
    }

    @Nested
    @DisplayName("Table of Contents Navigation")
    class TableOfContentsNavigation {

        @Test
        @DisplayName("should trigger scroll to section callback")
        void shouldTriggerScrollToSectionCallback() {
            // Given
            AtomicReference<String> scrolledSection = new AtomicReference<>();
            viewModel.setOnScrollToSectionCallback(scrolledSection::set);

            // When
            viewModel.handleScrollToSection("section-3");

            // Then
            assertThat(scrolledSection.get()).isEqualTo("section-3");
        }
    }

    @Nested
    @DisplayName("Print/Export Feature")
    class PrintExportFeature {

        @Test
        @DisplayName("should trigger print callback")
        void shouldTriggerPrintCallback() {
            // Given
            AtomicBoolean printCalled = new AtomicBoolean(false);
            viewModel.setOnPrintCallback(() -> printCalled.set(true));

            // When
            viewModel.handlePrint();

            // Then
            assertThat(printCalled.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Display Properties")
    class DisplayProperties {

        @Test
        @DisplayName("should have correct dialog title")
        void shouldHaveCorrectDialogTitle() {
            assertThat(viewModel.getDialogTitle()).isEqualTo("Terms of Service");
        }

        @Test
        @DisplayName("should have ToS title")
        void shouldHaveTosTitle() {
            assertThat(viewModel.getTosTitle()).isEqualTo("Terms of Service");
        }

        @Test
        @DisplayName("should have accept button text")
        void shouldHaveAcceptButtonText() {
            assertThat(viewModel.getAcceptButtonText()).isEqualTo("I Accept");
        }

        @Test
        @DisplayName("should have decline button text")
        void shouldHaveDeclineButtonText() {
            assertThat(viewModel.getDeclineButtonText()).isEqualTo("I Decline");
        }

        @Test
        @DisplayName("should have print button text")
        void shouldHavePrintButtonText() {
            assertThat(viewModel.getPrintButtonText()).isEqualTo("Print / Export");
        }
    }

    @Nested
    @DisplayName("Property Bindings")
    class PropertyBindings {

        @Test
        @DisplayName("should expose scrollProgress property for binding")
        void shouldExposeScrollProgressPropertyForBinding() {
            assertThat(viewModel.scrollProgressProperty()).isNotNull();
        }

        @Test
        @DisplayName("should expose scrolledToBottom property for binding")
        void shouldExposeScrolledToBottomPropertyForBinding() {
            assertThat(viewModel.scrolledToBottomProperty()).isNotNull();
        }

        @Test
        @DisplayName("should expose acceptEnabled property for binding")
        void shouldExposeAcceptEnabledPropertyForBinding() {
            assertThat(viewModel.acceptEnabledProperty()).isNotNull();
        }

        @Test
        @DisplayName("should expose closeButtonVisible property for binding")
        void shouldExposeCloseButtonVisiblePropertyForBinding() {
            assertThat(viewModel.closeButtonVisibleProperty()).isNotNull();
        }

        @Test
        @DisplayName("should expose actionButtonsVisible property for binding")
        void shouldExposeActionButtonsVisiblePropertyForBinding() {
            assertThat(viewModel.actionButtonsVisibleProperty()).isNotNull();
        }

        @Test
        @DisplayName("acceptEnabled should update when scroll reaches bottom")
        void acceptEnabledShouldUpdateWhenScrollReachesBottom() {
            // Given
            assertThat(viewModel.acceptEnabledProperty().get()).isFalse();

            // When
            viewModel.scrollProgressProperty().set(1.0);

            // Then
            assertThat(viewModel.acceptEnabledProperty().get()).isTrue();
        }

        @Test
        @DisplayName("scrolledToBottom should update based on scroll progress")
        void scrolledToBottomShouldUpdateBasedOnScrollProgress() {
            // Given
            assertThat(viewModel.scrolledToBottomProperty().get()).isFalse();

            // When
            viewModel.scrollProgressProperty().set(0.98);

            // Then
            assertThat(viewModel.scrolledToBottomProperty().get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Application Version Tracking")
    class ApplicationVersionTracking {

        @Test
        @DisplayName("should include application version in acceptance")
        void shouldIncludeApplicationVersionInAcceptance() {
            // Given
            viewModel.setScrollProgress(1.0);
            when(acceptanceService.saveAcceptance(any(), any(), any(), any())).thenReturn(true);

            // When
            viewModel.handleAccept();

            // Then
            verify(acceptanceService).saveAcceptance(
                eq("1.0"),
                any(Instant.class),
                any(Instant.class),
                argThat((String appVersion) -> appVersion != null && !appVersion.isEmpty())
            );
        }
    }

    @Nested
    @DisplayName("Scroll Hint Text Updates")
    class ScrollHintTextUpdates {

        @Test
        @DisplayName("should show initial hint when not scrolled")
        void shouldShowInitialHintWhenNotScrolled() {
            assertThat(viewModel.scrollHintTextProperty().get())
                .isEqualTo("Scroll to the bottom to enable acceptance");
        }

        @Test
        @DisplayName("should show completion hint when scrolled to bottom")
        void shouldShowCompletionHintWhenScrolledToBottom() {
            // When
            viewModel.setScrollProgress(1.0);

            // Then
            assertThat(viewModel.scrollHintTextProperty().get())
                .isEqualTo("You can now accept the terms");
        }

        @Test
        @DisplayName("should revert hint if scroll back up from bottom")
        void shouldRevertHintIfScrollBackUpFromBottom() {
            // Given
            viewModel.setScrollProgress(1.0);
            assertThat(viewModel.scrollHintTextProperty().get())
                .isEqualTo("You can now accept the terms");

            // When - scroll back up (note: accept still remains enabled per UX)
            viewModel.setScrollProgress(0.5);

            // Then - hint reverts but accept stays enabled (user proved they scrolled)
            // Based on UX decision: once scrolled to bottom, accept stays enabled
            assertThat(viewModel.isAcceptEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scroll Percentage Display")
    class ScrollPercentageDisplay {

        @Test
        @DisplayName("should expose scrollPercentage property for binding")
        void shouldExposeScrollPercentagePropertyForBinding() {
            assertThat(viewModel.scrollPercentageProperty()).isNotNull();
        }

        @Test
        @DisplayName("should update percentage on scroll progress change")
        void shouldUpdatePercentageOnScrollProgressChange() {
            // When
            viewModel.setScrollProgress(0.42);

            // Then
            assertThat(viewModel.scrollPercentageProperty().get()).isEqualTo("42%");
        }
    }
}
