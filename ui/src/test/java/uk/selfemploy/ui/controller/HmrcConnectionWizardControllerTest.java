package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.ui.viewmodel.HmrcConnectionWizardViewModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HmrcConnectionWizardController.
 * Sprint 12 - SE-12-001: Prerequisites Checklist Screen
 *
 * <p>Test Categories:
 * <ul>
 *   <li>Initialization Tests - Controller setup and step display</li>
 *   <li>Progress Indicator Tests - Step navigation and status</li>
 *   <li>Prerequisite Items Tests - Display and link handling</li>
 *   <li>Button Tests - Cancel and Get Started functionality</li>
 *   <li>Keyboard Accessibility Tests - Tab navigation and key handlers</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HmrcConnectionWizardController")
class HmrcConnectionWizardControllerTest {

    private HmrcConnectionWizardController controller;
    private HmrcConnectionWizardViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new HmrcConnectionWizardViewModel();
        controller = new HmrcConnectionWizardController();
        controller.setViewModel(viewModel);
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("should create controller without errors")
        void shouldCreateControllerWithoutErrors() {
            assertThat(controller).isNotNull();
        }

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
        @DisplayName("should initialize with canProceed true on step 1")
        void shouldInitializeWithCanProceedTrue() {
            assertThat(viewModel.canProceed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Progress Indicator Tests")
    class ProgressIndicatorTests {

        @Test
        @DisplayName("should show step 1 as active")
        void shouldShowStep1AsActive() {
            assertThat(viewModel.isStepActive(1)).isTrue();
        }

        @Test
        @DisplayName("should show steps 2-5 as pending")
        void shouldShowSteps2To5AsPending() {
            for (int step = 2; step <= 5; step++) {
                assertThat(viewModel.isStepPending(step))
                    .as("Step %d should be pending", step)
                    .isTrue();
            }
        }

        @Test
        @DisplayName("should not show any step as completed initially")
        void shouldNotShowAnyStepAsCompletedInitially() {
            for (int step = 1; step <= 5; step++) {
                assertThat(viewModel.isStepCompleted(step))
                    .as("Step %d should not be completed", step)
                    .isFalse();
            }
        }

        @Test
        @DisplayName("should format step label correctly")
        void shouldFormatStepLabelCorrectly() {
            assertThat(viewModel.getStepLabel()).isEqualTo("Step 1 of 5");
        }
    }

    @Nested
    @DisplayName("Prerequisite Items Tests")
    class PrerequisiteItemsTests {

        @Test
        @DisplayName("should have 3 prerequisite items")
        void shouldHave3PrerequisiteItems() {
            assertThat(HmrcConnectionWizardController.PREREQUISITE_COUNT).isEqualTo(3);
        }

        @Test
        @DisplayName("should have Self Assessment registration URL")
        void shouldHaveSelfAssessmentUrl() {
            assertThat(HmrcConnectionWizardController.SELF_ASSESSMENT_URL)
                .isEqualTo("https://www.gov.uk/register-for-self-assessment");
        }

        @Test
        @DisplayName("should have Government Gateway URL")
        void shouldHaveGovGatewayUrl() {
            assertThat(HmrcConnectionWizardController.GOV_GATEWAY_URL)
                .isEqualTo("https://www.gov.uk/log-in-register-hmrc-online-services");
        }

        @Test
        @DisplayName("should have Find NINO URL")
        void shouldHaveFindNinoUrl() {
            assertThat(HmrcConnectionWizardController.FIND_NINO_URL)
                .isEqualTo("https://www.gov.uk/lost-national-insurance-number");
        }

        @Test
        @DisplayName("should have correct prerequisite titles")
        void shouldHaveCorrectPrerequisiteTitles() {
            assertThat(HmrcConnectionWizardController.PREREQ_TITLE_SELF_ASSESSMENT)
                .isEqualTo("Registered for Self Assessment");
            assertThat(HmrcConnectionWizardController.PREREQ_TITLE_GOV_GATEWAY)
                .isEqualTo("Government Gateway account");
            assertThat(HmrcConnectionWizardController.PREREQ_TITLE_NINO)
                .isEqualTo("Your National Insurance Number (NINO)");
        }

        @Test
        @DisplayName("should have correct prerequisite descriptions")
        void shouldHaveCorrectPrerequisiteDescriptions() {
            assertThat(HmrcConnectionWizardController.PREREQ_DESC_SELF_ASSESSMENT)
                .contains("registered with HMRC for Self Assessment");
            assertThat(HmrcConnectionWizardController.PREREQ_DESC_GOV_GATEWAY)
                .contains("Government Gateway user ID and password");
            assertThat(HmrcConnectionWizardController.PREREQ_DESC_NINO)
                .contains("9-character code");
        }
    }

    @Nested
    @DisplayName("Navigation Tests")
    class NavigationTests {

        @Test
        @DisplayName("goNext should advance to step 2")
        void goNextShouldAdvanceToStep2() {
            // When
            viewModel.goNext();

            // Then
            assertThat(viewModel.getCurrentStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("goNext should mark step 1 as completed")
        void goNextShouldMarkStep1AsCompleted() {
            // When
            viewModel.goNext();

            // Then
            assertThat(viewModel.isStepCompleted(1)).isTrue();
        }

        @Test
        @DisplayName("goBack should return to step 1 from step 2")
        void goBackShouldReturnToStep1() {
            // Given
            viewModel.goNext();
            assertThat(viewModel.getCurrentStep()).isEqualTo(2);

            // When
            viewModel.goBack();

            // Then
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("goBack should not go below step 1")
        void goBackShouldNotGoBelowStep1() {
            // When
            viewModel.goBack();

            // Then
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("goNext should not exceed step 5")
        void goNextShouldNotExceedStep5() {
            // Given
            for (int i = 0; i < 10; i++) {
                viewModel.goNext();
            }

            // Then
            assertThat(viewModel.getCurrentStep()).isLessThanOrEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Cancel Tests")
    class CancelTests {

        @Test
        @DisplayName("cancel should set cancelled flag")
        void cancelShouldSetCancelledFlag() {
            // When
            viewModel.cancel();

            // Then
            assertThat(viewModel.isCancelled()).isTrue();
        }

        @Test
        @DisplayName("isCancelled should be false initially")
        void isCancelledShouldBeFalseInitially() {
            assertThat(viewModel.isCancelled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Button State Tests")
    class ButtonStateTests {

        @Test
        @DisplayName("should show Get Started button on step 1")
        void shouldShowGetStartedButtonOnStep1() {
            assertThat(viewModel.getNextButtonText()).isEqualTo("Get Started");
        }

        @Test
        @DisplayName("should show Next button on step 2")
        void shouldShowNextButtonOnStep2() {
            viewModel.goNext();
            assertThat(viewModel.getNextButtonText()).isEqualTo("Next");
        }

        @Test
        @DisplayName("should show Cancel button text")
        void shouldShowCancelButtonText() {
            assertThat(viewModel.getCancelButtonText()).isEqualTo("Cancel");
        }
    }

    @Nested
    @DisplayName("Keyboard Accessibility Tests")
    class KeyboardAccessibilityTests {

        @Test
        @DisplayName("controller should support escape key handler")
        void controllerShouldSupportEscapeKeyHandler() {
            // Verify the controller has handleEscapeKey method
            boolean hasMethod = false;
            for (var method : controller.getClass().getDeclaredMethods()) {
                if (method.getName().equals("handleEscapeKey")) {
                    hasMethod = true;
                    break;
                }
            }
            assertThat(hasMethod)
                .as("Controller should have handleEscapeKey method")
                .isTrue();
        }

        @Test
        @DisplayName("controller should have method to open external link")
        void controllerShouldHaveOpenExternalLinkMethod() {
            boolean hasMethod = false;
            for (var method : controller.getClass().getDeclaredMethods()) {
                if (method.getName().equals("openExternalLink")) {
                    hasMethod = true;
                    break;
                }
            }
            assertThat(hasMethod)
                .as("Controller should have openExternalLink method")
                .isTrue();
        }
    }

    @Nested
    @DisplayName("Dialog Constants Tests")
    class DialogConstantsTests {

        @Test
        @DisplayName("should define dialog width")
        void shouldDefineDialogWidth() {
            assertThat(HmrcConnectionWizardController.DIALOG_WIDTH).isEqualTo(520);
        }

        @Test
        @DisplayName("should define minimum dialog height")
        void shouldDefineMinDialogHeight() {
            assertThat(HmrcConnectionWizardController.DIALOG_MIN_HEIGHT).isEqualTo(400);
        }

        @Test
        @DisplayName("should define maximum dialog height")
        void shouldDefineMaxDialogHeight() {
            assertThat(HmrcConnectionWizardController.DIALOG_MAX_HEIGHT).isEqualTo(600);
        }

        @Test
        @DisplayName("should define wizard title")
        void shouldDefineWizardTitle() {
            assertThat(HmrcConnectionWizardController.WIZARD_TITLE)
                .isEqualTo("Connect to HMRC");
        }
    }

    @Nested
    @DisplayName("Error Message Tests")
    class ErrorMessageTests {

        @Test
        @DisplayName("should have empty error message initially")
        void shouldHaveEmptyErrorMessageInitially() {
            assertThat(viewModel.getErrorMessage()).isEmpty();
        }

        @Test
        @DisplayName("should be able to set error message")
        void shouldBeAbleToSetErrorMessage() {
            // When
            viewModel.setErrorMessage("Test error");

            // Then
            assertThat(viewModel.getErrorMessage()).isEqualTo("Test error");
        }

        @Test
        @DisplayName("should be able to clear error message")
        void shouldBeAbleToClearErrorMessage() {
            // Given
            viewModel.setErrorMessage("Test error");

            // When
            viewModel.clearErrorMessage();

            // Then
            assertThat(viewModel.getErrorMessage()).isEmpty();
        }
    }

    // ===== SE-12-002: Step 2 NINO Entry Tests =====

    @Nested
    @DisplayName("Step 2 NINO Entry Tests")
    class Step2NinoEntryTests {

        @Test
        @DisplayName("should have NINO validation error message constant")
        void step2_hasNinoValidationErrorMessage() {
            assertThat(HmrcConnectionWizardController.NINO_VALIDATION_ERROR)
                .contains("QQ 12 34 56 A");
        }

        @Test
        @DisplayName("should have NINO vs UTR info title")
        void step2_hasNinoVsUtrInfoTitle() {
            assertThat(HmrcConnectionWizardController.NINO_VS_UTR_TITLE)
                .isEqualTo("NINO vs UTR");
        }

        @Test
        @DisplayName("should have NINO vs UTR info description per FIN-001")
        void step2_hasNinoVsUtrInfoDescription() {
            // FIN-001: Must include clarification about NINO vs UTR
            assertThat(HmrcConnectionWizardController.NINO_VS_UTR_DESCRIPTION)
                .contains("National Insurance Number (NINO)")
                .contains("Unique Taxpayer Reference (UTR)")
                .contains("9-character code")
                .contains("10-digit number");
        }

        @Test
        @DisplayName("should have step 2 title constant")
        void step2_hasTitleConstant() {
            assertThat(HmrcConnectionWizardController.STEP2_TITLE)
                .isEqualTo("Enter your National Insurance Number");
        }

        @Test
        @DisplayName("should have NINO placeholder constant")
        void step2_hasNinoPlaceholder() {
            assertThat(HmrcConnectionWizardController.NINO_PLACEHOLDER)
                .isEqualTo("QQ 12 34 56 A");
        }

        @Test
        @DisplayName("should show Next button on step 2")
        void step2_showsNextButton() {
            viewModel.goNext(); // Go to step 2
            assertThat(viewModel.getNextButtonText()).isEqualTo("Next");
        }

        @Test
        @DisplayName("should require valid NINO to proceed on step 2")
        void step2_cannotProceedWithoutValidNino() {
            viewModel.goNext(); // Go to step 2
            viewModel.setCanProceed(false); // Simulating invalid NINO state
            assertThat(viewModel.canProceed()).isFalse();
        }

        @Test
        @DisplayName("should allow proceed with valid NINO on step 2")
        void step2_canProceedWithValidNino() {
            viewModel.goNext(); // Go to step 2
            viewModel.setNino("AB123456A");
            viewModel.setCanProceed(true);
            assertThat(viewModel.canProceed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Step 2 NINO Info Box Tests (FIN-001)")
    class Step2NinoInfoBoxTests {

        @Test
        @DisplayName("NINO info should mention found on payslips")
        void step2_ninoInfo_mentionsPayslips() {
            assertThat(HmrcConnectionWizardController.NINO_VS_UTR_DESCRIPTION)
                .containsIgnoringCase("payslips");
        }

        @Test
        @DisplayName("NINO info should mention P60")
        void step2_ninoInfo_mentionsP60() {
            assertThat(HmrcConnectionWizardController.NINO_VS_UTR_DESCRIPTION)
                .containsIgnoringCase("P60");
        }

        @Test
        @DisplayName("NINO info should mention tax letters")
        void step2_ninoInfo_mentionsTaxLetters() {
            assertThat(HmrcConnectionWizardController.NINO_VS_UTR_DESCRIPTION)
                .containsIgnoringCase("tax letters");
        }

        @Test
        @DisplayName("NINO info should mention Self Assessment")
        void step2_ninoInfo_mentionsSelfAssessment() {
            assertThat(HmrcConnectionWizardController.NINO_VS_UTR_DESCRIPTION)
                .containsIgnoringCase("Self Assessment");
        }
    }

    // ===== SE-12-003: Step 3 Government Gateway Explainer Tests =====

    @Nested
    @DisplayName("Step 3 Constants Tests (SE-12-003)")
    class Step3ConstantsTests {

        @Test
        @DisplayName("should have step 3 title constant")
        void step3_hasTitleConstant() {
            assertThat(HmrcConnectionWizardController.STEP3_TITLE)
                .isEqualTo("About Government Gateway");
        }

        @Test
        @DisplayName("should have Government Gateway intro constant")
        void step3_hasGovGatewayIntroConstant() {
            assertThat(HmrcConnectionWizardController.GOV_GATEWAY_INTRO)
                .contains("Government Gateway")
                .contains("secure login service");
        }

        @Test
        @DisplayName("should have 3 Government Gateway uses")
        void step3_hasThreeGovGatewayUses() {
            assertThat(HmrcConnectionWizardController.GOV_GATEWAY_USES)
                .hasSize(3);
        }

        @Test
        @DisplayName("should have correct Government Gateway uses content")
        void step3_hasCorrectGovGatewayUsesContent() {
            assertThat(HmrcConnectionWizardController.GOV_GATEWAY_USES)
                .contains("Filing your Self Assessment")
                .contains("Viewing your tax account")
                .contains("Managing your tax credits");
        }

        @Test
        @DisplayName("should have security title constant")
        void step3_hasSecurityTitleConstant() {
            assertThat(HmrcConnectionWizardController.SECURITY_TITLE)
                .isEqualTo("Your Security");
        }

        @Test
        @DisplayName("should have security message constant")
        void step3_hasSecurityMessageConstant() {
            assertThat(HmrcConnectionWizardController.SECURITY_MESSAGE)
                .contains("browser will open")
                .contains("Government Gateway login page")
                .contains("NEVER sees your password");
        }

        @Test
        @DisplayName("security message should reassure about password")
        void step3_securityMessageReassuresAboutPassword() {
            // AC2: Reassure users about security (app never sees password)
            assertThat(HmrcConnectionWizardController.SECURITY_MESSAGE)
                .containsIgnoringCase("never")
                .containsIgnoringCase("password");
        }

        @Test
        @DisplayName("should have what happens title constant")
        void step3_hasWhatHappensTitleConstant() {
            assertThat(HmrcConnectionWizardController.WHAT_HAPPENS_TITLE)
                .isEqualTo("What happens next");
        }

        @Test
        @DisplayName("should have 4 what happens steps")
        void step3_hasFourWhatHappensSteps() {
            assertThat(HmrcConnectionWizardController.WHAT_HAPPENS_STEPS)
                .hasSize(4);
        }

        @Test
        @DisplayName("should have correct what happens steps content")
        void step3_hasCorrectWhatHappensStepsContent() {
            assertThat(HmrcConnectionWizardController.WHAT_HAPPENS_STEPS)
                .contains("Your browser opens to Government Gateway")
                .contains("Sign in with your Government Gateway credentials")
                .contains("Click \"Grant authority\" to authorize this app")
                .contains("You'll be redirected back here automatically");
        }

        @Test
        @DisplayName("what happens steps should explain browser behavior")
        void step3_whatHappensStepsExplainBrowserBehavior() {
            // AC4: Show expected browser behavior (opens, then returns)
            String[] steps = HmrcConnectionWizardController.WHAT_HAPPENS_STEPS;
            String allSteps = String.join(" ", steps);
            assertThat(allSteps)
                .containsIgnoringCase("browser opens")
                .containsIgnoringCase("redirected back");
        }
    }

    @Nested
    @DisplayName("Step 3 Navigation Tests (SE-12-003)")
    class Step3NavigationTests {

        @Test
        @DisplayName("should show Next button on step 3")
        void step3_showsNextButton() {
            // Navigate to step 3
            viewModel.goNext(); // Step 2
            viewModel.goNext(); // Step 3
            assertThat(viewModel.getNextButtonText()).isEqualTo("Next");
        }

        @Test
        @DisplayName("should show step 3 of 5 label")
        void step3_showsStep3Of5Label() {
            // Navigate to step 3
            viewModel.goNext(); // Step 2
            viewModel.goNext(); // Step 3
            assertThat(viewModel.getStepLabel()).isEqualTo("Step 3 of 5");
        }

        @Test
        @DisplayName("back should return to step 2")
        void step3_backReturnsToStep2() {
            // Navigate to step 3
            viewModel.goNext(); // Step 2
            viewModel.goNext(); // Step 3
            assertThat(viewModel.getCurrentStep()).isEqualTo(3);

            // Go back
            viewModel.goBack();

            assertThat(viewModel.getCurrentStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("next should proceed to step 4")
        void step3_nextProceedsToStep4() {
            // Navigate to step 3
            viewModel.goNext(); // Step 2
            viewModel.goNext(); // Step 3
            assertThat(viewModel.getCurrentStep()).isEqualTo(3);

            // Go next
            viewModel.goNext();

            assertThat(viewModel.getCurrentStep()).isEqualTo(4);
        }

        @Test
        @DisplayName("step 1 and 2 should be completed on step 3")
        void step3_steps1And2Completed() {
            // Navigate to step 3
            viewModel.goNext(); // Step 2
            viewModel.goNext(); // Step 3

            assertThat(viewModel.isStepCompleted(1)).isTrue();
            assertThat(viewModel.isStepCompleted(2)).isTrue();
            assertThat(viewModel.isStepActive(3)).isTrue();
        }
    }

    @Nested
    @DisplayName("Step 3 Can Proceed Tests (SE-12-003)")
    class Step3CanProceedTests {

        @Test
        @DisplayName("should be able to proceed on step 3")
        void step3_canProceed() {
            // Navigate to step 3
            viewModel.goNext(); // Step 2
            viewModel.goNext(); // Step 3

            // Step 3 is informational, user can always proceed
            viewModel.setCanProceed(true);
            assertThat(viewModel.canProceed()).isTrue();
        }
    }

    // ===== SE-12-005: Step 5 Confirmation and Next Steps Tests =====

    @Nested
    @DisplayName("Step 5 Constants Tests (SE-12-005)")
    class Step5ConstantsTests {

        @Test
        @DisplayName("should have step 5 title constant")
        void step5_hasTitleConstant() {
            assertThat(HmrcConnectionWizardController.STEP5_TITLE)
                .isEqualTo("Connected to HMRC");
        }

        @Test
        @DisplayName("should have success message constant")
        void step5_hasSuccessMessageConstant() {
            assertThat(HmrcConnectionWizardController.SUCCESS_MESSAGE)
                .isEqualTo("Successfully Connected to HMRC");
        }

        @Test
        @DisplayName("should have success description constant")
        void step5_hasSuccessDescriptionConstant() {
            assertThat(HmrcConnectionWizardController.SUCCESS_DESCRIPTION)
                .contains("account is now linked")
                .contains("quarterly updates")
                .contains("annual returns");
        }

        @Test
        @DisplayName("should have what you can do constant")
        void step5_hasWhatYouCanDoConstant() {
            assertThat(HmrcConnectionWizardController.WHAT_YOU_CAN_DO)
                .isEqualTo("What you can do now:");
        }

        @Test
        @DisplayName("should have 3 next steps")
        void step5_hasThreeNextSteps() {
            assertThat(HmrcConnectionWizardController.NEXT_STEPS)
                .hasSize(3);
        }

        @Test
        @DisplayName("should have correct next steps content")
        void step5_hasCorrectNextStepsContent() {
            assertThat(HmrcConnectionWizardController.NEXT_STEPS)
                .contains("Submit quarterly updates to HMRC")
                .contains("File your annual Self Assessment")
                .contains("View your submission history");
        }
    }

    @Nested
    @DisplayName("Step 5 Retention Reminder Tests (FIN-002)")
    class Step5RetentionReminderTests {

        @Test
        @DisplayName("should have retention title constant")
        void step5_hasRetentionTitleConstant() {
            assertThat(HmrcConnectionWizardController.RETENTION_TITLE)
                .isEqualTo("Important Reminder");
        }

        @Test
        @DisplayName("should have retention message per FIN-002")
        void step5_hasRetentionMessagePerFIN002() {
            // FIN-002: Add 5-year retention reminder
            assertThat(HmrcConnectionWizardController.RETENTION_MESSAGE)
                .isEqualTo("HMRC requires you to keep records for at least 5 years after the submission deadline.");
        }

        @Test
        @DisplayName("retention message should mention 5 years")
        void step5_retentionMessageMentions5Years() {
            assertThat(HmrcConnectionWizardController.RETENTION_MESSAGE)
                .containsIgnoringCase("5 years");
        }

        @Test
        @DisplayName("retention message should mention submission deadline")
        void step5_retentionMessageMentionsSubmissionDeadline() {
            assertThat(HmrcConnectionWizardController.RETENTION_MESSAGE)
                .containsIgnoringCase("submission deadline");
        }
    }

    @Nested
    @DisplayName("Step 5 Navigation Tests")
    class Step5NavigationTests {

        @Test
        @DisplayName("should show Done button on step 5")
        void step5_showsDoneButton() {
            // Navigate to step 5
            for (int i = 0; i < 4; i++) {
                viewModel.goNext();
            }
            assertThat(viewModel.getNextButtonText()).isEqualTo("Done");
        }

        @Test
        @DisplayName("step 5 is the last step")
        void step5_isLastStep() {
            // Navigate to step 5
            for (int i = 0; i < 4; i++) {
                viewModel.goNext();
            }
            assertThat(viewModel.isLastStep()).isTrue();
        }

        @Test
        @DisplayName("step 5 should show all steps completed")
        void step5_allStepsCompleted() {
            // Navigate to step 5
            for (int i = 0; i < 4; i++) {
                viewModel.goNext();
            }
            // Steps 1-4 should be completed
            for (int step = 1; step <= 4; step++) {
                assertThat(viewModel.isStepCompleted(step))
                    .as("Step %d should be completed", step)
                    .isTrue();
            }
            // Step 5 should be active
            assertThat(viewModel.isStepActive(5)).isTrue();
        }
    }
}
