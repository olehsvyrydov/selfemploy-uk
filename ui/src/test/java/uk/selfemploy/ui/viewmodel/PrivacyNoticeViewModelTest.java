package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.selfemploy.core.service.PrivacyAcknowledgmentService;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD tests for PrivacyNoticeViewModel.
 * Tests acknowledgment flow, version tracking, and mode handling.
 *
 * SE-507: Privacy Notice UI
 */
@DisplayName("PrivacyNoticeViewModel")
class PrivacyNoticeViewModelTest {

    @Mock
    private PrivacyAcknowledgmentService acknowledgmentService;

    private PrivacyNoticeViewModel viewModel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new PrivacyNoticeViewModel(acknowledgmentService);
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("should have acknowledgment checkbox unchecked initially")
        void shouldHaveAcknowledgmentCheckboxUncheckedInitially() {
            assertThat(viewModel.isAcknowledged()).isFalse();
        }

        @Test
        @DisplayName("should have continue button disabled initially")
        void shouldHaveContinueButtonDisabledInitially() {
            assertThat(viewModel.isContinueEnabled()).isFalse();
        }

        @Test
        @DisplayName("should have current privacy notice version")
        void shouldHaveCurrentPrivacyNoticeVersion() {
            assertThat(viewModel.getPrivacyVersion()).isEqualTo("1.0");
        }

        @Test
        @DisplayName("should have effective date")
        void shouldHaveEffectiveDate() {
            assertThat(viewModel.getEffectiveDate()).isNotNull();
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
    }

    @Nested
    @DisplayName("Acknowledgment Flow")
    class AcknowledgmentFlow {

        @Test
        @DisplayName("should enable continue button when checkbox is checked")
        void shouldEnableContinueButtonWhenCheckboxIsChecked() {
            // When
            viewModel.setAcknowledged(true);

            // Then
            assertThat(viewModel.isContinueEnabled()).isTrue();
        }

        @Test
        @DisplayName("should disable continue button when checkbox is unchecked")
        void shouldDisableContinueButtonWhenCheckboxIsUnchecked() {
            // Given
            viewModel.setAcknowledged(true);

            // When
            viewModel.setAcknowledged(false);

            // Then
            assertThat(viewModel.isContinueEnabled()).isFalse();
        }

        @Test
        @DisplayName("should save acknowledgment when continue is clicked")
        void shouldSaveAcknowledgmentWhenContinueIsClicked() {
            // Given
            viewModel.setAcknowledged(true);
            when(acknowledgmentService.saveAcknowledgment(any(), any(), any())).thenReturn(true);

            // When
            boolean result = viewModel.handleContinue();

            // Then
            assertThat(result).isTrue();
            verify(acknowledgmentService).saveAcknowledgment(
                eq("1.0"),
                any(Instant.class),
                any(String.class)
            );
        }

        @Test
        @DisplayName("should not save acknowledgment when continue is clicked without acknowledgment")
        void shouldNotSaveAcknowledgmentWhenContinueIsClickedWithoutAcknowledgment() {
            // When
            boolean result = viewModel.handleContinue();

            // Then
            assertThat(result).isFalse();
            verify(acknowledgmentService, never()).saveAcknowledgment(any(), any(), any());
        }

        @Test
        @DisplayName("should trigger callback on successful acknowledgment")
        void shouldTriggerCallbackOnSuccessfulAcknowledgment() {
            // Given
            AtomicBoolean callbackCalled = new AtomicBoolean(false);
            viewModel.setOnAcknowledgedCallback(() -> callbackCalled.set(true));
            viewModel.setAcknowledged(true);
            when(acknowledgmentService.saveAcknowledgment(any(), any(), any())).thenReturn(true);

            // When
            viewModel.handleContinue();

            // Then
            assertThat(callbackCalled.get()).isTrue();
        }

        @Test
        @DisplayName("should not trigger callback on failed acknowledgment")
        void shouldNotTriggerCallbackOnFailedAcknowledgment() {
            // Given
            AtomicBoolean callbackCalled = new AtomicBoolean(false);
            viewModel.setOnAcknowledgedCallback(() -> callbackCalled.set(true));
            viewModel.setAcknowledged(true);
            when(acknowledgmentService.saveAcknowledgment(any(), any(), any())).thenReturn(false);

            // When
            viewModel.handleContinue();

            // Then
            assertThat(callbackCalled.get()).isFalse();
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
        @DisplayName("should allow close without acknowledgment in settings mode")
        void shouldAllowCloseWithoutAcknowledgmentInSettingsMode() {
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
        @DisplayName("should require re-acknowledgment if version changed")
        void shouldRequireReAcknowledgmentIfVersionChanged() {
            // Given
            when(acknowledgmentService.getAcknowledgedVersion())
                .thenReturn(Optional.of("0.9"));

            // When
            boolean requiresAcknowledgment = viewModel.requiresAcknowledgment();

            // Then
            assertThat(requiresAcknowledgment).isTrue();
        }

        @Test
        @DisplayName("should not require re-acknowledgment if version matches")
        void shouldNotRequireReAcknowledgmentIfVersionMatches() {
            // Given
            when(acknowledgmentService.getAcknowledgedVersion())
                .thenReturn(Optional.of("1.0"));

            // When
            boolean requiresAcknowledgment = viewModel.requiresAcknowledgment();

            // Then
            assertThat(requiresAcknowledgment).isFalse();
        }

        @Test
        @DisplayName("should require acknowledgment if no previous acknowledgment exists")
        void shouldRequireAcknowledgmentIfNoPreviousAcknowledgmentExists() {
            // Given
            when(acknowledgmentService.getAcknowledgedVersion())
                .thenReturn(Optional.empty());

            // When
            boolean requiresAcknowledgment = viewModel.requiresAcknowledgment();

            // Then
            assertThat(requiresAcknowledgment).isTrue();
        }
    }

    @Nested
    @DisplayName("Privacy Policy Link")
    class PrivacyPolicyLink {

        @Test
        @DisplayName("should have full privacy policy URL")
        void shouldHaveFullPrivacyPolicyUrl() {
            assertThat(viewModel.getFullPrivacyPolicyUrl()).isNotBlank();
        }

        @Test
        @DisplayName("should trigger open browser callback when link clicked")
        void shouldTriggerOpenBrowserCallbackWhenLinkClicked() {
            // Given
            AtomicBoolean linkClicked = new AtomicBoolean(false);
            String[] clickedUrl = new String[1];
            viewModel.setOnOpenBrowserCallback(url -> {
                linkClicked.set(true);
                clickedUrl[0] = url;
            });

            // When
            viewModel.handleOpenFullPolicy();

            // Then
            assertThat(linkClicked.get()).isTrue();
            assertThat(clickedUrl[0]).isEqualTo(viewModel.getFullPrivacyPolicyUrl());
        }
    }

    @Nested
    @DisplayName("Display Title")
    class DisplayTitle {

        @Test
        @DisplayName("should have correct dialog title")
        void shouldHaveCorrectDialogTitle() {
            assertThat(viewModel.getDialogTitle()).isEqualTo("Privacy Notice");
        }

        @Test
        @DisplayName("should have correct privacy header title")
        void shouldHaveCorrectPrivacyHeaderTitle() {
            assertThat(viewModel.getPrivacyTitle()).isEqualTo("Your Privacy Matters");
        }

        @Test
        @DisplayName("should have acknowledgment label text")
        void shouldHaveAcknowledgmentLabelText() {
            assertThat(viewModel.getAcknowledgmentLabelText())
                .isEqualTo("I have read and understand this privacy notice");
        }

        @Test
        @DisplayName("should have continue button text")
        void shouldHaveContinueButtonText() {
            assertThat(viewModel.getContinueButtonText()).isEqualTo("Continue");
        }
    }

    @Nested
    @DisplayName("Property Bindings")
    class PropertyBindings {

        @Test
        @DisplayName("should expose acknowledged property for binding")
        void shouldExposeAcknowledgedPropertyForBinding() {
            assertThat(viewModel.acknowledgedProperty()).isNotNull();
        }

        @Test
        @DisplayName("should expose continueEnabled property for binding")
        void shouldExposeContinueEnabledPropertyForBinding() {
            assertThat(viewModel.continueEnabledProperty()).isNotNull();
        }

        @Test
        @DisplayName("should expose closeButtonVisible property for binding")
        void shouldExposeCloseButtonVisiblePropertyForBinding() {
            assertThat(viewModel.closeButtonVisibleProperty()).isNotNull();
        }

        @Test
        @DisplayName("continueEnabled should update when acknowledged changes")
        void continueEnabledShouldUpdateWhenAcknowledgedChanges() {
            // Given
            assertThat(viewModel.continueEnabledProperty().get()).isFalse();

            // When
            viewModel.acknowledgedProperty().set(true);

            // Then
            assertThat(viewModel.continueEnabledProperty().get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Application Version Tracking")
    class ApplicationVersionTracking {

        @Test
        @DisplayName("should include application version in acknowledgment")
        void shouldIncludeApplicationVersionInAcknowledgment() {
            // Given
            viewModel.setAcknowledged(true);
            when(acknowledgmentService.saveAcknowledgment(any(), any(), any())).thenReturn(true);

            // When
            viewModel.handleContinue();

            // Then
            verify(acknowledgmentService).saveAcknowledgment(
                eq("1.0"),
                any(Instant.class),
                argThat((String appVersion) -> appVersion != null && !appVersion.isEmpty())
            );
        }
    }

}
