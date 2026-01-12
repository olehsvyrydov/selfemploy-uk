package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.enums.BusinessType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for OnboardingViewModel.
 * Tests the 4-step onboarding wizard flow: Welcome, Details, Tax Year, Business Type.
 *
 * SE-702: User Onboarding Wizard
 */
@DisplayName("OnboardingViewModel")
class OnboardingViewModelTest {

    private OnboardingViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new OnboardingViewModel();
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("should initialize on step 1 (Welcome)")
        void shouldInitializeOnStepOne() {
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("should have total 4 steps")
        void shouldHaveTotalFourSteps() {
            assertThat(viewModel.getTotalSteps()).isEqualTo(4);
        }

        @Test
        @DisplayName("should initialize with empty user name")
        void shouldInitializeWithEmptyUserName() {
            assertThat(viewModel.getUserName()).isEmpty();
        }

        @Test
        @DisplayName("should initialize with empty UTR")
        void shouldInitializeWithEmptyUtr() {
            assertThat(viewModel.getUtr()).isEmpty();
        }

        @Test
        @DisplayName("should initialize with empty NI Number")
        void shouldInitializeWithEmptyNiNumber() {
            assertThat(viewModel.getNiNumber()).isEmpty();
        }

        @Test
        @DisplayName("should initialize with no tax year selected")
        void shouldInitializeWithNoTaxYearSelected() {
            assertThat(viewModel.getSelectedTaxYear()).isNull();
        }

        @Test
        @DisplayName("should initialize with no business type selected")
        void shouldInitializeWithNoBusinessTypeSelected() {
            assertThat(viewModel.getSelectedBusinessType()).isNull();
        }

        @Test
        @DisplayName("should not be completed initially")
        void shouldNotBeCompletedInitially() {
            assertThat(viewModel.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("should allow next on Welcome step (no validation required)")
        void shouldAllowNextOnWelcomeStep() {
            assertThat(viewModel.canGoNext()).isTrue();
        }

        @Test
        @DisplayName("should not allow previous on step 1")
        void shouldNotAllowPreviousOnStepOne() {
            assertThat(viewModel.canGoPrevious()).isFalse();
        }
    }

    @Nested
    @DisplayName("Step Navigation")
    class StepNavigation {

        @Test
        @DisplayName("should navigate from step 1 to step 2")
        void shouldNavigateFromStepOneToStepTwo() {
            viewModel.goToNextStep();

            assertThat(viewModel.getCurrentStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("should allow previous on step 2")
        void shouldAllowPreviousOnStepTwo() {
            viewModel.goToNextStep();

            assertThat(viewModel.canGoPrevious()).isTrue();
        }

        @Test
        @DisplayName("should navigate back from step 2 to step 1")
        void shouldNavigateBackFromStepTwoToStepOne() {
            viewModel.goToNextStep();
            viewModel.goToPreviousStep();

            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not go below step 1")
        void shouldNotGoBelowStepOne() {
            viewModel.goToPreviousStep();
            viewModel.goToPreviousStep();

            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("should navigate through all 4 steps with valid data")
        void shouldNavigateThroughAllFourSteps() {
            // Step 1 -> 2
            viewModel.goToNextStep();
            assertThat(viewModel.getCurrentStep()).isEqualTo(2);

            // Step 2 requires name -> set name and go to 3
            viewModel.setUserName("John Doe");
            viewModel.goToNextStep();
            assertThat(viewModel.getCurrentStep()).isEqualTo(3);

            // Step 3 requires tax year -> set and go to 4
            viewModel.setSelectedTaxYear("2025/26");
            viewModel.goToNextStep();
            assertThat(viewModel.getCurrentStep()).isEqualTo(4);
        }

        @Test
        @DisplayName("should not go beyond step 4")
        void shouldNotGoBeyondStepFour() {
            // Navigate to step 4
            viewModel.goToNextStep(); // to 2
            viewModel.setUserName("John");
            viewModel.goToNextStep(); // to 3
            viewModel.setSelectedTaxYear("2025/26");
            viewModel.goToNextStep(); // to 4

            // Try to go beyond
            viewModel.goToNextStep();

            assertThat(viewModel.getCurrentStep()).isEqualTo(4);
        }

        @Test
        @DisplayName("should return correct step labels")
        void shouldReturnCorrectStepLabels() {
            assertThat(viewModel.getStepLabel(1)).isEqualTo("Welcome");
            assertThat(viewModel.getStepLabel(2)).isEqualTo("Your Details");
            assertThat(viewModel.getStepLabel(3)).isEqualTo("Tax Year");
            assertThat(viewModel.getStepLabel(4)).isEqualTo("Business Type");
        }

        @Test
        @DisplayName("should check if step is completed")
        void shouldCheckIfStepIsCompleted() {
            // Step 1 always completed (no validation)
            assertThat(viewModel.isStepCompleted(1)).isTrue();

            // Step 2 requires name
            assertThat(viewModel.isStepCompleted(2)).isFalse();
            viewModel.setUserName("John");
            assertThat(viewModel.isStepCompleted(2)).isTrue();

            // Step 3 requires tax year
            assertThat(viewModel.isStepCompleted(3)).isFalse();
            viewModel.setSelectedTaxYear("2025/26");
            assertThat(viewModel.isStepCompleted(3)).isTrue();
        }

        @Test
        @DisplayName("should check if step is active")
        void shouldCheckIfStepIsActive() {
            assertThat(viewModel.isStepActive(1)).isTrue();
            assertThat(viewModel.isStepActive(2)).isFalse();

            viewModel.goToNextStep();

            assertThat(viewModel.isStepActive(1)).isFalse();
            assertThat(viewModel.isStepActive(2)).isTrue();
        }
    }

    @Nested
    @DisplayName("Step 2: Your Details")
    class Step2YourDetails {

        @BeforeEach
        void navigateToStep2() {
            viewModel.goToNextStep();
        }

        @Test
        @DisplayName("should set user name")
        void shouldSetUserName() {
            viewModel.setUserName("Jane Smith");

            assertThat(viewModel.getUserName()).isEqualTo("Jane Smith");
        }

        @Test
        @DisplayName("should require name for step 2 validation")
        void shouldRequireNameForStep2Validation() {
            assertThat(viewModel.canGoNext()).isFalse();

            viewModel.setUserName("John");

            assertThat(viewModel.canGoNext()).isTrue();
        }

        @Test
        @DisplayName("should require minimum 2 characters for name")
        void shouldRequireMinimumTwoCharactersForName() {
            viewModel.setUserName("J");
            assertThat(viewModel.canGoNext()).isFalse();
            assertThat(viewModel.isNameValid()).isFalse();

            viewModel.setUserName("Jo");
            assertThat(viewModel.canGoNext()).isTrue();
            assertThat(viewModel.isNameValid()).isTrue();
        }

        @Test
        @DisplayName("should set UTR (optional)")
        void shouldSetUtr() {
            viewModel.setUtr("1234567890");

            assertThat(viewModel.getUtr()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("should validate UTR format - exactly 10 digits")
        void shouldValidateUtrFormat() {
            assertThat(viewModel.isUtrValid()).isTrue(); // Empty is valid (optional)

            viewModel.setUtr("123456789"); // 9 digits - invalid
            assertThat(viewModel.isUtrValid()).isFalse();

            viewModel.setUtr("12345678901"); // 11 digits - invalid
            assertThat(viewModel.isUtrValid()).isFalse();

            viewModel.setUtr("123456789A"); // contains letter - invalid
            assertThat(viewModel.isUtrValid()).isFalse();

            viewModel.setUtr("1234567890"); // exactly 10 digits - valid
            assertThat(viewModel.isUtrValid()).isTrue();
        }

        @Test
        @DisplayName("should set NI Number (optional)")
        void shouldSetNiNumber() {
            viewModel.setNiNumber("AB123456C");

            assertThat(viewModel.getNiNumber()).isEqualTo("AB123456C");
        }

        @Test
        @DisplayName("should validate NI Number format")
        void shouldValidateNiNumberFormat() {
            assertThat(viewModel.isNiNumberValid()).isTrue(); // Empty is valid (optional)

            viewModel.setNiNumber("INVALID");
            assertThat(viewModel.isNiNumberValid()).isFalse();

            viewModel.setNiNumber("AB123456C");
            assertThat(viewModel.isNiNumberValid()).isTrue();

            viewModel.setNiNumber("AB 12 34 56 C"); // with spaces
            assertThat(viewModel.isNiNumberValid()).isTrue();
        }

        @Test
        @DisplayName("should allow navigation to step 3 even with invalid UTR if name is valid")
        void shouldAllowNavigationWithInvalidOptionalFields() {
            viewModel.setUserName("John");
            viewModel.setUtr(""); // Empty UTR is fine

            assertThat(viewModel.canGoNext()).isTrue();
        }

        @Test
        @DisplayName("should block navigation if UTR is partially filled and invalid")
        void shouldBlockNavigationWithInvalidUtr() {
            viewModel.setUserName("John");
            viewModel.setUtr("12345"); // Partial UTR - invalid

            assertThat(viewModel.canGoNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("Step 3: Tax Year Selection")
    class Step3TaxYear {

        @BeforeEach
        void navigateToStep3() {
            viewModel.goToNextStep(); // to 2
            viewModel.setUserName("John");
            viewModel.goToNextStep(); // to 3
        }

        @Test
        @DisplayName("should provide available tax years")
        void shouldProvideAvailableTaxYears() {
            var taxYears = viewModel.getAvailableTaxYears();

            assertThat(taxYears).isNotEmpty();
            assertThat(taxYears).contains("2024/25", "2025/26");
        }

        @Test
        @DisplayName("should select tax year")
        void shouldSelectTaxYear() {
            viewModel.setSelectedTaxYear("2025/26");

            assertThat(viewModel.getSelectedTaxYear()).isEqualTo("2025/26");
        }

        @Test
        @DisplayName("should require tax year selection for step 3 validation")
        void shouldRequireTaxYearForStep3Validation() {
            assertThat(viewModel.canGoNext()).isFalse();

            viewModel.setSelectedTaxYear("2025/26");

            assertThat(viewModel.canGoNext()).isTrue();
        }

        @Test
        @DisplayName("should have current tax year as recommended")
        void shouldHaveCurrentTaxYearAsRecommended() {
            assertThat(viewModel.getRecommendedTaxYear()).isEqualTo("2025/26");
        }

        @Test
        @DisplayName("should get tax year date range")
        void shouldGetTaxYearDateRange() {
            assertThat(viewModel.getTaxYearDateRange("2025/26"))
                    .isEqualTo("6 Apr 2025 - 5 Apr 2026");
            assertThat(viewModel.getTaxYearDateRange("2024/25"))
                    .isEqualTo("6 Apr 2024 - 5 Apr 2025");
        }

        @Test
        @DisplayName("should check if tax year is recommended")
        void shouldCheckIfTaxYearIsRecommended() {
            assertThat(viewModel.isTaxYearRecommended("2025/26")).isTrue();
            assertThat(viewModel.isTaxYearRecommended("2024/25")).isFalse();
        }
    }

    @Nested
    @DisplayName("Step 4: Business Type")
    class Step4BusinessType {

        @BeforeEach
        void navigateToStep4() {
            viewModel.goToNextStep(); // to 2
            viewModel.setUserName("John");
            viewModel.goToNextStep(); // to 3
            viewModel.setSelectedTaxYear("2025/26");
            viewModel.goToNextStep(); // to 4
        }

        @Test
        @DisplayName("should provide available business types")
        void shouldProvideAvailableBusinessTypes() {
            var businessTypes = viewModel.getAvailableBusinessTypes();

            assertThat(businessTypes).contains(
                    BusinessType.SOLE_TRADER,
                    BusinessType.FREELANCER,
                    BusinessType.CONTRACTOR
            );
        }

        @Test
        @DisplayName("should select business type")
        void shouldSelectBusinessType() {
            viewModel.setSelectedBusinessType(BusinessType.FREELANCER);

            assertThat(viewModel.getSelectedBusinessType()).isEqualTo(BusinessType.FREELANCER);
        }

        @Test
        @DisplayName("should get business type display name")
        void shouldGetBusinessTypeDisplayName() {
            assertThat(viewModel.getBusinessTypeDisplayName(BusinessType.SOLE_TRADER))
                    .isEqualTo("Sole Trader / Freelancer");
            assertThat(viewModel.getBusinessTypeDisplayName(BusinessType.CONTRACTOR))
                    .isEqualTo("Contractor");
        }

        @Test
        @DisplayName("should get business type description")
        void shouldGetBusinessTypeDescription() {
            assertThat(viewModel.getBusinessTypeDescription(BusinessType.SOLE_TRADER))
                    .isEqualTo("Working independently for clients");
            assertThat(viewModel.getBusinessTypeDescription(BusinessType.CONTRACTOR))
                    .isEqualTo("Providing services to businesses");
        }

        @Test
        @DisplayName("should check if business type is enabled")
        void shouldCheckIfBusinessTypeIsEnabled() {
            assertThat(viewModel.isBusinessTypeEnabled(BusinessType.SOLE_TRADER)).isTrue();
            assertThat(viewModel.isBusinessTypeEnabled(BusinessType.FREELANCER)).isTrue();
            assertThat(viewModel.isBusinessTypeEnabled(BusinessType.CONTRACTOR)).isTrue();
            assertThat(viewModel.isBusinessTypeEnabled(BusinessType.PARTNERSHIP)).isFalse(); // Coming soon
        }

        @Test
        @DisplayName("should not block navigation from step 4 - business type is optional")
        void shouldNotBlockNavigationFromStep4() {
            // Business type selection is optional for completing onboarding
            assertThat(viewModel.canComplete()).isTrue();
        }
    }

    @Nested
    @DisplayName("Wizard Completion")
    class WizardCompletion {

        @BeforeEach
        void navigateToFinalStep() {
            viewModel.goToNextStep(); // to 2
            viewModel.setUserName("John Doe");
            viewModel.goToNextStep(); // to 3
            viewModel.setSelectedTaxYear("2025/26");
            viewModel.goToNextStep(); // to 4
            viewModel.setSelectedBusinessType(BusinessType.FREELANCER);
        }

        @Test
        @DisplayName("should be completable on step 4")
        void shouldBeCompletableOnStep4() {
            assertThat(viewModel.canComplete()).isTrue();
        }

        @Test
        @DisplayName("should mark wizard as completed")
        void shouldMarkWizardAsCompleted() {
            viewModel.complete();

            assertThat(viewModel.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("should provide completion summary")
        void shouldProvideCompletionSummary() {
            viewModel.complete();

            assertThat(viewModel.getCompletionSummary().userName()).isEqualTo("John Doe");
            assertThat(viewModel.getCompletionSummary().taxYear()).isEqualTo("2025/26");
            assertThat(viewModel.getCompletionSummary().businessType()).isEqualTo(BusinessType.FREELANCER);
        }

        @Test
        @DisplayName("should provide personalized welcome message")
        void shouldProvidePersonalizedWelcomeMessage() {
            assertThat(viewModel.getPersonalizedWelcome()).isEqualTo("You're all set, John Doe!");
        }
    }

    @Nested
    @DisplayName("Skip Functionality")
    class SkipFunctionality {

        @Test
        @DisplayName("should not allow skip on step 1")
        void shouldNotAllowSkipOnStep1() {
            assertThat(viewModel.canSkip()).isFalse();
        }

        @Test
        @DisplayName("should allow skip from step 2")
        void shouldAllowSkipFromStep2() {
            viewModel.goToNextStep();

            assertThat(viewModel.canSkip()).isTrue();
        }

        @Test
        @DisplayName("should set default values when skipping")
        void shouldSetDefaultValuesWhenSkipping() {
            viewModel.goToNextStep();

            viewModel.skipSetup();

            assertThat(viewModel.isCompleted()).isTrue();
            assertThat(viewModel.getSelectedTaxYear()).isEqualTo("2025/26"); // Default to current
        }
    }

    @Nested
    @DisplayName("Wizard Reset")
    class WizardReset {

        @Test
        @DisplayName("should reset wizard to initial state")
        void shouldResetWizardToInitialState() {
            // Setup wizard in mid-state
            viewModel.goToNextStep();
            viewModel.setUserName("John");
            viewModel.setUtr("1234567890");
            viewModel.goToNextStep();
            viewModel.setSelectedTaxYear("2024/25");

            // Reset
            viewModel.reset();

            // Verify initial state
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
            assertThat(viewModel.getUserName()).isEmpty();
            assertThat(viewModel.getUtr()).isEmpty();
            assertThat(viewModel.getNiNumber()).isEmpty();
            assertThat(viewModel.getSelectedTaxYear()).isNull();
            assertThat(viewModel.getSelectedBusinessType()).isNull();
            assertThat(viewModel.isCompleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("Property Bindings")
    class PropertyBindings {

        @Test
        @DisplayName("should expose currentStep property")
        void shouldExposeCurrentStepProperty() {
            assertThat(viewModel.currentStepProperty()).isNotNull();
            assertThat(viewModel.currentStepProperty().get()).isEqualTo(1);
        }

        @Test
        @DisplayName("should expose userName property")
        void shouldExposeUserNameProperty() {
            assertThat(viewModel.userNameProperty()).isNotNull();

            viewModel.userNameProperty().set("Test");
            assertThat(viewModel.getUserName()).isEqualTo("Test");
        }

        @Test
        @DisplayName("should expose utr property")
        void shouldExposeUtrProperty() {
            assertThat(viewModel.utrProperty()).isNotNull();

            viewModel.utrProperty().set("1234567890");
            assertThat(viewModel.getUtr()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("should expose completed property")
        void shouldExposeCompletedProperty() {
            assertThat(viewModel.completedProperty()).isNotNull();
            assertThat(viewModel.completedProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should notify when step changes")
        void shouldNotifyWhenStepChanges() {
            final int[] notifiedValue = {0};
            viewModel.currentStepProperty().addListener((obs, oldVal, newVal) -> {
                notifiedValue[0] = newVal.intValue();
            });

            viewModel.goToNextStep();

            assertThat(notifiedValue[0]).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("UTR Segmented Input")
    class UtrSegmentedInput {

        @Test
        @DisplayName("should get UTR segment 1 (first 4 digits)")
        void shouldGetUtrSegment1() {
            viewModel.setUtr("1234567890");

            assertThat(viewModel.getUtrSegment1()).isEqualTo("1234");
        }

        @Test
        @DisplayName("should get UTR segment 2 (middle 3 digits)")
        void shouldGetUtrSegment2() {
            viewModel.setUtr("1234567890");

            assertThat(viewModel.getUtrSegment2()).isEqualTo("567");
        }

        @Test
        @DisplayName("should get UTR segment 3 (last 3 digits)")
        void shouldGetUtrSegment3() {
            viewModel.setUtr("1234567890");

            assertThat(viewModel.getUtrSegment3()).isEqualTo("890");
        }

        @Test
        @DisplayName("should set UTR from segments")
        void shouldSetUtrFromSegments() {
            viewModel.setUtrFromSegments("1234", "567", "890");

            assertThat(viewModel.getUtr()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("should handle partial segments")
        void shouldHandlePartialSegments() {
            viewModel.setUtrFromSegments("12", "", "");

            assertThat(viewModel.getUtr()).isEqualTo("12");
        }
    }
}
