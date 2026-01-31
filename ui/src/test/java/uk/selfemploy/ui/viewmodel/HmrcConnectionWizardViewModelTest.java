package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.service.OAuthConnectionHandler.ConnectionStatus;
import uk.selfemploy.ui.service.OAuthConnectionHandler.OAuthResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HmrcConnectionWizardViewModel.
 * Sprint 12 - SE-12-001: Prerequisites Checklist Screen
 *
 * <p>Tests the view model that manages wizard state including
 * current step, navigation, and observable properties.</p>
 */
@DisplayName("HmrcConnectionWizardViewModel")
class HmrcConnectionWizardViewModelTest {

    private HmrcConnectionWizardViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new HmrcConnectionWizardViewModel();
    }

    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {

        @Test
        @DisplayName("should start at step 1")
        void shouldStartAtStep1() {
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("should have 5 total steps")
        void shouldHave5TotalSteps() {
            assertThat(HmrcConnectionWizardViewModel.TOTAL_STEPS).isEqualTo(5);
        }

        @Test
        @DisplayName("should be able to proceed initially")
        void shouldBeAbleToProceedInitially() {
            assertThat(viewModel.canProceed()).isTrue();
        }

        @Test
        @DisplayName("should not be cancelled initially")
        void shouldNotBeCancelledInitially() {
            assertThat(viewModel.isCancelled()).isFalse();
        }

        @Test
        @DisplayName("should have empty error message initially")
        void shouldHaveEmptyErrorMessageInitially() {
            assertThat(viewModel.getErrorMessage()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Step Navigation Tests")
    class StepNavigationTests {

        @Test
        @DisplayName("goNext should advance to step 2")
        void goNextShouldAdvanceToStep2() {
            viewModel.goNext();
            assertThat(viewModel.getCurrentStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("goNext should advance through all steps")
        void goNextShouldAdvanceThroughAllSteps() {
            for (int expected = 2; expected <= 5; expected++) {
                viewModel.goNext();
                assertThat(viewModel.getCurrentStep()).isEqualTo(expected);
            }
        }

        @Test
        @DisplayName("goNext should not exceed step 5")
        void goNextShouldNotExceedStep5() {
            for (int i = 0; i < 10; i++) {
                viewModel.goNext();
            }
            assertThat(viewModel.getCurrentStep()).isEqualTo(5);
        }

        @Test
        @DisplayName("goBack should return to previous step")
        void goBackShouldReturnToPreviousStep() {
            viewModel.goNext();
            viewModel.goNext();
            assertThat(viewModel.getCurrentStep()).isEqualTo(3);

            viewModel.goBack();
            assertThat(viewModel.getCurrentStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("goBack should not go below step 1")
        void goBackShouldNotGoBelowStep1() {
            viewModel.goBack();
            viewModel.goBack();
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("should navigate back and forth correctly")
        void shouldNavigateBackAndForthCorrectly() {
            viewModel.goNext(); // 2
            viewModel.goNext(); // 3
            viewModel.goBack(); // 2
            viewModel.goNext(); // 3
            viewModel.goBack(); // 2
            viewModel.goBack(); // 1
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Step State Tests")
    class StepStateTests {

        @Test
        @DisplayName("should mark step 1 as active initially")
        void shouldMarkStep1AsActiveInitially() {
            assertThat(viewModel.isStepActive(1)).isTrue();
        }

        @Test
        @DisplayName("should mark steps 2-5 as pending initially")
        void shouldMarkSteps2To5AsPendingInitially() {
            for (int step = 2; step <= 5; step++) {
                assertThat(viewModel.isStepPending(step))
                    .as("Step %d should be pending", step)
                    .isTrue();
            }
        }

        @Test
        @DisplayName("should not mark any step as completed initially")
        void shouldNotMarkAnyStepAsCompletedInitially() {
            for (int step = 1; step <= 5; step++) {
                assertThat(viewModel.isStepCompleted(step))
                    .as("Step %d should not be completed", step)
                    .isFalse();
            }
        }

        @Test
        @DisplayName("should mark previous step as completed when advancing")
        void shouldMarkPreviousStepAsCompletedWhenAdvancing() {
            viewModel.goNext();
            assertThat(viewModel.isStepCompleted(1)).isTrue();
            assertThat(viewModel.isStepActive(2)).isTrue();
        }

        @Test
        @DisplayName("should mark multiple previous steps as completed")
        void shouldMarkMultiplePreviousStepsAsCompleted() {
            viewModel.goNext(); // to 2
            viewModel.goNext(); // to 3
            viewModel.goNext(); // to 4

            assertThat(viewModel.isStepCompleted(1)).isTrue();
            assertThat(viewModel.isStepCompleted(2)).isTrue();
            assertThat(viewModel.isStepCompleted(3)).isTrue();
            assertThat(viewModel.isStepActive(4)).isTrue();
            assertThat(viewModel.isStepPending(5)).isTrue();
        }
    }

    @Nested
    @DisplayName("Cancel Tests")
    class CancelTests {

        @Test
        @DisplayName("cancel should set cancelled flag")
        void cancelShouldSetCancelledFlag() {
            viewModel.cancel();
            assertThat(viewModel.isCancelled()).isTrue();
        }

        @Test
        @DisplayName("cancel should not change current step")
        void cancelShouldNotChangeCurrentStep() {
            viewModel.goNext();
            int stepBefore = viewModel.getCurrentStep();
            viewModel.cancel();
            assertThat(viewModel.getCurrentStep()).isEqualTo(stepBefore);
        }
    }

    @Nested
    @DisplayName("Step Label Tests")
    class StepLabelTests {

        @Test
        @DisplayName("should format step 1 label correctly")
        void shouldFormatStep1LabelCorrectly() {
            assertThat(viewModel.getStepLabel()).isEqualTo("Step 1 of 5");
        }

        @Test
        @DisplayName("should format step 3 label correctly")
        void shouldFormatStep3LabelCorrectly() {
            viewModel.goNext();
            viewModel.goNext();
            assertThat(viewModel.getStepLabel()).isEqualTo("Step 3 of 5");
        }

        @Test
        @DisplayName("should format step 5 label correctly")
        void shouldFormatStep5LabelCorrectly() {
            for (int i = 0; i < 4; i++) viewModel.goNext();
            assertThat(viewModel.getStepLabel()).isEqualTo("Step 5 of 5");
        }
    }

    @Nested
    @DisplayName("Button Text Tests")
    class ButtonTextTests {

        @Test
        @DisplayName("should show Get Started on step 1")
        void shouldShowGetStartedOnStep1() {
            assertThat(viewModel.getNextButtonText()).isEqualTo("Get Started");
        }

        @Test
        @DisplayName("should show Next on step 2")
        void shouldShowNextOnStep2() {
            viewModel.goNext();
            assertThat(viewModel.getNextButtonText()).isEqualTo("Next");
        }

        @Test
        @DisplayName("should show Next on step 3")
        void shouldShowNextOnStep3() {
            viewModel.goNext();
            viewModel.goNext();
            assertThat(viewModel.getNextButtonText()).isEqualTo("Next");
        }

        @Test
        @DisplayName("should show Done on step 5")
        void shouldShowDoneOnStep5() {
            for (int i = 0; i < 4; i++) viewModel.goNext();
            assertThat(viewModel.getNextButtonText()).isEqualTo("Done");
        }

        @Test
        @DisplayName("should always show Cancel for cancel button")
        void shouldAlwaysShowCancelForCancelButton() {
            for (int step = 1; step <= 5; step++) {
                assertThat(viewModel.getCancelButtonText())
                    .as("Cancel button text on step %d", step)
                    .isEqualTo("Cancel");
                if (step < 5) viewModel.goNext();
            }
        }
    }

    @Nested
    @DisplayName("Error Message Tests")
    class ErrorMessageTests {

        @Test
        @DisplayName("should set error message")
        void shouldSetErrorMessage() {
            viewModel.setErrorMessage("Test error");
            assertThat(viewModel.getErrorMessage()).isEqualTo("Test error");
        }

        @Test
        @DisplayName("should clear error message")
        void shouldClearErrorMessage() {
            viewModel.setErrorMessage("Test error");
            viewModel.clearErrorMessage();
            assertThat(viewModel.getErrorMessage()).isEmpty();
        }

        @Test
        @DisplayName("should handle null error message")
        void shouldHandleNullErrorMessage() {
            viewModel.setErrorMessage(null);
            assertThat(viewModel.getErrorMessage()).isEmpty();
        }
    }

    @Nested
    @DisplayName("CanProceed Tests")
    class CanProceedTests {

        @Test
        @DisplayName("should be able to set canProceed to false")
        void shouldBeAbleToSetCanProceedToFalse() {
            viewModel.setCanProceed(false);
            assertThat(viewModel.canProceed()).isFalse();
        }

        @Test
        @DisplayName("should be able to set canProceed to true")
        void shouldBeAbleToSetCanProceedToTrue() {
            viewModel.setCanProceed(false);
            viewModel.setCanProceed(true);
            assertThat(viewModel.canProceed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Observable Property Tests")
    class ObservablePropertyTests {

        @Test
        @DisplayName("should have currentStep property")
        void shouldHaveCurrentStepProperty() {
            assertThat(viewModel.currentStepProperty()).isNotNull();
            assertThat(viewModel.currentStepProperty().get()).isEqualTo(1);
        }

        @Test
        @DisplayName("should have canProceed property")
        void shouldHaveCanProceedProperty() {
            assertThat(viewModel.canProceedProperty()).isNotNull();
            assertThat(viewModel.canProceedProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should have errorMessage property")
        void shouldHaveErrorMessageProperty() {
            assertThat(viewModel.errorMessageProperty()).isNotNull();
            assertThat(viewModel.errorMessageProperty().get()).isEmpty();
        }

        @Test
        @DisplayName("should have cancelled property")
        void shouldHaveCancelledProperty() {
            assertThat(viewModel.cancelledProperty()).isNotNull();
            assertThat(viewModel.cancelledProperty().get()).isFalse();
        }

        @Test
        @DisplayName("currentStep property should update when goNext called")
        void currentStepPropertyShouldUpdateWhenGoNextCalled() {
            int[] observed = {0};
            viewModel.currentStepProperty().addListener((obs, old, newVal) -> {
                observed[0] = newVal.intValue();
            });

            viewModel.goNext();
            assertThat(observed[0]).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("First and Last Step Tests")
    class FirstAndLastStepTests {

        @Test
        @DisplayName("should identify first step")
        void shouldIdentifyFirstStep() {
            assertThat(viewModel.isFirstStep()).isTrue();
        }

        @Test
        @DisplayName("should not identify first step on step 2")
        void shouldNotIdentifyFirstStepOnStep2() {
            viewModel.goNext();
            assertThat(viewModel.isFirstStep()).isFalse();
        }

        @Test
        @DisplayName("should not identify last step on step 1")
        void shouldNotIdentifyLastStepOnStep1() {
            assertThat(viewModel.isLastStep()).isFalse();
        }

        @Test
        @DisplayName("should identify last step on step 5")
        void shouldIdentifyLastStepOnStep5() {
            for (int i = 0; i < 4; i++) viewModel.goNext();
            assertThat(viewModel.isLastStep()).isTrue();
        }
    }

    // ===== SE-12-002: NINO Entry Tests =====

    @Nested
    @DisplayName("Step 2 NINO Property Tests")
    class Step2NinoPropertyTests {

        @Test
        @DisplayName("should have nino property")
        void step2_ninoProperty_exists() {
            assertThat(viewModel.ninoProperty()).isNotNull();
        }

        @Test
        @DisplayName("should have empty NINO initially")
        void step2_ninoProperty_emptyInitially() {
            assertThat(viewModel.getNino()).isEmpty();
        }

        @Test
        @DisplayName("should store NINO value when set")
        void step2_setNino_storesValue() {
            viewModel.setNino("AB123456A");
            assertThat(viewModel.getNino()).isEqualTo("AB123456A");
        }

        @Test
        @DisplayName("should handle null NINO")
        void step2_setNino_handlesNull() {
            viewModel.setNino("AB123456A");
            viewModel.setNino(null);
            assertThat(viewModel.getNino()).isEmpty();
        }

        @Test
        @DisplayName("nino property should update when setNino called")
        void step2_ninoProperty_updatesOnSet() {
            String[] observed = {""};
            viewModel.ninoProperty().addListener((obs, old, newVal) -> observed[0] = newVal);

            viewModel.setNino("AB123456A");
            assertThat(observed[0]).isEqualTo("AB123456A");
        }
    }

    // ===== SE-12-004: OAuth Connection Tests =====

    @Nested
    @DisplayName("Step 4 OAuth Connection State Tests")
    class Step4OAuthConnectionStateTests {

        @Test
        @DisplayName("should have null connection status initially")
        void step4_connectionStatus_nullInitially() {
            assertThat(viewModel.getConnectionStatus()).isNull();
        }

        @Test
        @DisplayName("should have empty status message initially")
        void step4_statusMessage_emptyInitially() {
            assertThat(viewModel.getStatusMessage()).isEmpty();
        }

        @Test
        @DisplayName("should not be connecting initially")
        void step4_notConnectingInitially() {
            assertThat(viewModel.isConnecting()).isFalse();
        }

        @Test
        @DisplayName("should not be connection successful initially")
        void step4_notConnectionSuccessfulInitially() {
            assertThat(viewModel.isConnectionSuccessful()).isFalse();
        }

        @Test
        @DisplayName("should set connection status")
        void step4_setConnectionStatus_updatesStatus() {
            viewModel.setConnectionStatus(ConnectionStatus.OPENING_BROWSER);
            assertThat(viewModel.getConnectionStatus()).isEqualTo(ConnectionStatus.OPENING_BROWSER);
        }

        @Test
        @DisplayName("should update status message when setting connection status")
        void step4_setConnectionStatus_updatesMessage() {
            viewModel.setConnectionStatus(ConnectionStatus.WAITING_FOR_AUTH);
            assertThat(viewModel.getStatusMessage()).isEqualTo("Waiting for HMRC authorization...");
        }

        @Test
        @DisplayName("should set connecting flag")
        void step4_setConnecting_updatesFlag() {
            viewModel.setConnecting(true);
            assertThat(viewModel.isConnecting()).isTrue();
        }

        @Test
        @DisplayName("should set connection successful flag")
        void step4_setConnectionSuccessful_updatesFlag() {
            viewModel.setConnectionSuccessful(true);
            assertThat(viewModel.isConnectionSuccessful()).isTrue();
        }
    }

    @Nested
    @DisplayName("Step 4 OAuth Result Handling Tests")
    class Step4OAuthResultHandlingTests {

        @Test
        @DisplayName("should handle success result")
        void step4_handleOAuthResult_success() {
            viewModel.setConnecting(true);

            viewModel.handleOAuthResult(OAuthResult.ofSuccess());

            assertThat(viewModel.isConnecting()).isFalse();
            assertThat(viewModel.isConnectionSuccessful()).isTrue();
            assertThat(viewModel.getConnectionErrorCode()).isNull();
            assertThat(viewModel.getConnectionErrorMessage()).isNull();
        }

        @Test
        @DisplayName("should handle error result")
        void step4_handleOAuthResult_error() {
            viewModel.setConnecting(true);

            viewModel.handleOAuthResult(OAuthResult.ofError("ACCESS_DENIED", "User denied access"));

            assertThat(viewModel.isConnecting()).isFalse();
            assertThat(viewModel.isConnectionSuccessful()).isFalse();
            assertThat(viewModel.getConnectionErrorCode()).isEqualTo("ACCESS_DENIED");
            assertThat(viewModel.getConnectionErrorMessage()).isEqualTo("User denied access");
        }

        @Test
        @DisplayName("should handle timeout result")
        void step4_handleOAuthResult_timeout() {
            viewModel.setConnecting(true);

            viewModel.handleOAuthResult(OAuthResult.ofTimeout());

            assertThat(viewModel.isConnecting()).isFalse();
            assertThat(viewModel.isConnectionSuccessful()).isFalse();
            assertThat(viewModel.getConnectionErrorCode()).isEqualTo("TIMEOUT");
        }

        @Test
        @DisplayName("should handle cancelled result")
        void step4_handleOAuthResult_cancelled() {
            viewModel.setConnecting(true);

            viewModel.handleOAuthResult(OAuthResult.ofCancelled());

            assertThat(viewModel.isConnecting()).isFalse();
            assertThat(viewModel.isConnectionSuccessful()).isFalse();
            assertThat(viewModel.getConnectionErrorCode()).isEqualTo("USER_CANCELLED");
        }
    }

    @Nested
    @DisplayName("Step 4 Connection State Reset Tests")
    class Step4ConnectionStateResetTests {

        @Test
        @DisplayName("should reset all connection state")
        void step4_resetConnectionState_clearsAll() {
            // Set up some state
            viewModel.setConnectionStatus(ConnectionStatus.ERROR);
            viewModel.setConnecting(true);
            viewModel.setConnectionSuccessful(false);
            viewModel.handleOAuthResult(OAuthResult.ofError("TEST", "Test error"));

            // Reset
            viewModel.resetConnectionState();

            // Verify all reset
            assertThat(viewModel.getConnectionStatus()).isNull();
            assertThat(viewModel.getStatusMessage()).isEmpty();
            assertThat(viewModel.isConnecting()).isFalse();
            assertThat(viewModel.isConnectionSuccessful()).isFalse();
            assertThat(viewModel.getConnectionErrorCode()).isNull();
            assertThat(viewModel.getConnectionErrorMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("Step 4 Error State Query Tests")
    class Step4ErrorStateQueryTests {

        @Test
        @DisplayName("should identify error state")
        void step4_hasConnectionError_forErrorStatus() {
            viewModel.setConnectionStatus(ConnectionStatus.ERROR);
            assertThat(viewModel.hasConnectionError()).isTrue();
        }

        @Test
        @DisplayName("should identify timeout as error")
        void step4_hasConnectionError_forTimeoutStatus() {
            viewModel.setConnectionStatus(ConnectionStatus.TIMEOUT);
            assertThat(viewModel.hasConnectionError()).isTrue();
        }

        @Test
        @DisplayName("should not identify success as error")
        void step4_hasConnectionError_falseForSuccess() {
            viewModel.setConnectionStatus(ConnectionStatus.SUCCESS);
            assertThat(viewModel.hasConnectionError()).isFalse();
        }

        @Test
        @DisplayName("should identify cancelled state")
        void step4_isConnectionCancelled_forCancelledStatus() {
            viewModel.setConnectionStatus(ConnectionStatus.CANCELLED);
            assertThat(viewModel.isConnectionCancelled()).isTrue();
        }

        @Test
        @DisplayName("should not identify error as cancelled")
        void step4_isConnectionCancelled_falseForError() {
            viewModel.setConnectionStatus(ConnectionStatus.ERROR);
            assertThat(viewModel.isConnectionCancelled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Step 4 Observable Property Tests")
    class Step4ObservablePropertyTests {

        @Test
        @DisplayName("should have connectionStatus property")
        void step4_connectionStatusProperty_exists() {
            assertThat(viewModel.connectionStatusProperty()).isNotNull();
        }

        @Test
        @DisplayName("should have statusMessage property")
        void step4_statusMessageProperty_exists() {
            assertThat(viewModel.statusMessageProperty()).isNotNull();
        }

        @Test
        @DisplayName("should have connecting property")
        void step4_connectingProperty_exists() {
            assertThat(viewModel.connectingProperty()).isNotNull();
        }

        @Test
        @DisplayName("should have connectionSuccessful property")
        void step4_connectionSuccessfulProperty_exists() {
            assertThat(viewModel.connectionSuccessfulProperty()).isNotNull();
        }

        @Test
        @DisplayName("should have connectionErrorCode property")
        void step4_connectionErrorCodeProperty_exists() {
            assertThat(viewModel.connectionErrorCodeProperty()).isNotNull();
        }

        @Test
        @DisplayName("should have connectionErrorMessage property")
        void step4_connectionErrorMessageProperty_exists() {
            assertThat(viewModel.connectionErrorMessageProperty()).isNotNull();
        }

        @Test
        @DisplayName("connectionStatus property should update when set")
        void step4_connectionStatusProperty_updatesOnSet() {
            ConnectionStatus[] observed = {null};
            viewModel.connectionStatusProperty().addListener(
                (obs, old, newVal) -> observed[0] = newVal
            );

            viewModel.setConnectionStatus(ConnectionStatus.SUCCESS);
            assertThat(observed[0]).isEqualTo(ConnectionStatus.SUCCESS);
        }
    }

    // ===== SE-12-005: Step 5 Confirmation and Next Steps Tests =====

    @Nested
    @DisplayName("Step 5 Confirmation Tests (SE-12-005)")
    class Step5ConfirmationTests {

        @Test
        @DisplayName("should identify step 5 as last step")
        void step5_isLastStep_returnsTrue() {
            // Navigate to step 5
            for (int i = 0; i < 4; i++) {
                viewModel.goNext();
            }
            assertThat(viewModel.isLastStep()).isTrue();
            assertThat(viewModel.getCurrentStep()).isEqualTo(5);
        }

        @Test
        @DisplayName("should return Done for next button text on step 5")
        void step5_nextButtonText_returnsDone() {
            // Navigate to step 5
            for (int i = 0; i < 4; i++) {
                viewModel.goNext();
            }
            assertThat(viewModel.getNextButtonText()).isEqualTo("Done");
        }

        @Test
        @DisplayName("should not advance past step 5")
        void step5_goNext_staysAtStep5() {
            // Navigate to step 5
            for (int i = 0; i < 4; i++) {
                viewModel.goNext();
            }
            assertThat(viewModel.getCurrentStep()).isEqualTo(5);

            // Try to go further
            viewModel.goNext();
            assertThat(viewModel.getCurrentStep()).isEqualTo(5);
        }

        @Test
        @DisplayName("should show step 5 of 5 label")
        void step5_stepLabel_showsStep5of5() {
            // Navigate to step 5
            for (int i = 0; i < 4; i++) {
                viewModel.goNext();
            }
            assertThat(viewModel.getStepLabel()).isEqualTo("Step 5 of 5");
        }

        @Test
        @DisplayName("steps 1-4 should be completed on step 5")
        void step5_allPreviousStepsCompleted() {
            // Navigate to step 5
            for (int i = 0; i < 4; i++) {
                viewModel.goNext();
            }

            // All previous steps should be completed
            assertThat(viewModel.isStepCompleted(1)).isTrue();
            assertThat(viewModel.isStepCompleted(2)).isTrue();
            assertThat(viewModel.isStepCompleted(3)).isTrue();
            assertThat(viewModel.isStepCompleted(4)).isTrue();
        }

        @Test
        @DisplayName("step 5 should be active not completed")
        void step5_shouldBeActiveNotCompleted() {
            // Navigate to step 5
            for (int i = 0; i < 4; i++) {
                viewModel.goNext();
            }

            assertThat(viewModel.isStepActive(5)).isTrue();
            assertThat(viewModel.isStepCompleted(5)).isFalse();
        }
    }
}
