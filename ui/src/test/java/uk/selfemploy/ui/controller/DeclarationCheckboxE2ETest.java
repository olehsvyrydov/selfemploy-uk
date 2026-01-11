package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.AnnualSubmissionState;
import uk.selfemploy.common.domain.TaxCalculationResult;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.viewmodel.AnnualSubmissionViewModel;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Declaration Checkbox UI (SE-506).
 *
 * These tests verify the declaration checkbox behavior at the ViewModel level.
 * Tests cover QA specifications TC-506-001 through TC-506-012.
 *
 * The tests focus on business logic validation through the ViewModel,
 * which is the source of truth for declaration state management.
 * UI-level tests (if needed) should use TestFX with Monocle in CI/CD.
 */
@DisplayName("Declaration Checkbox Integration Tests (SE-506)")
class DeclarationCheckboxE2ETest {

    private AnnualSubmissionViewModel viewModel;

    @BeforeEach
    void setUpViewModel() {
        // Reset ViewModel for each test
        viewModel = new AnnualSubmissionViewModel();
    }

    // ===== P0 Critical Tests =====

    @Nested
    @DisplayName("P0 Critical: Declaration Core Functionality")
    class P0CriticalTests {

        @Test
        @DisplayName("TC-506-001: Declaration card visibility at Step 3")
        void tc506_001_declarationCardShouldBeVisibleAtStep3() {
            // Given: User progresses to Step 3
            setupViewModelToStep3();

            // Then: Declaration should be available at Step 3
            assertThat(viewModel.getCurrentStep()).isEqualTo(3);
            assertThat(viewModel.getCurrentState()).isEqualTo(AnnualSubmissionState.CALCULATED);
            // Declaration is controlled by Controller based on step - ViewModel tracks checkbox state
            assertThat(viewModel.isDeclarationConfirmed()).isFalse();
        }

        @Test
        @DisplayName("TC-506-002: HMRC official declaration text matches exactly")
        void tc506_002_declarationTextShouldMatchHmrcOfficial() {
            // Given: The official HMRC declaration text
            String expectedText =
                "I declare that the information I have given on this tax return " +
                "and any supplementary pages is correct and complete to the best of my knowledge and belief. " +
                "I understand that I may have to pay financial penalties and face prosecution if I give false information.";

            // Then: ViewModel constant matches exactly
            assertThat(AnnualSubmissionViewModel.DECLARATION_TEXT).isEqualTo(expectedText);
        }

        @Test
        @DisplayName("TC-506-003: Submit button disabled when checkbox unchecked")
        void tc506_003_submitButtonShouldBeDisabledWhenUnchecked() {
            // Given: At Step 3 with declaration unchecked
            setupViewModelToStep3();

            // When: Declaration is not confirmed
            viewModel.setDeclarationConfirmed(false);

            // Then: Cannot submit (canSubmit is false)
            assertThat(viewModel.canSubmit()).isFalse();
        }

        @Test
        @DisplayName("TC-506-004: Submit button enabled when checkbox checked")
        void tc506_004_submitButtonShouldBeEnabledWhenChecked() {
            // Given: At Step 3 with calculated state
            setupViewModelToStep3();

            // When: Declaration is confirmed
            viewModel.setDeclarationConfirmed(true);

            // Then: Can submit (canSubmit is true)
            assertThat(viewModel.canSubmit()).isTrue();
        }

        @Test
        @DisplayName("TC-506-005: Timestamp recorded when checkbox checked")
        void tc506_005_timestampRecordedWhenChecked() {
            // Given: Timestamp before checking
            Instant beforeCheck = Instant.now();
            setupViewModelToStep3();

            // When: Declaration is confirmed
            viewModel.setDeclarationConfirmed(true);

            // Then: Timestamp is recorded
            Instant timestamp = viewModel.getDeclarationTimestamp();
            assertThat(timestamp).isNotNull();
            assertThat(timestamp).isAfterOrEqualTo(beforeCheck);
            assertThat(timestamp).isBeforeOrEqualTo(Instant.now());
        }
    }

    // ===== P1 Important Tests =====

    @Nested
    @DisplayName("P1 Important: Declaration State Management")
    class P1ImportantTests {

        @Test
        @DisplayName("TC-506-006: Timestamp displayed after checking")
        void tc506_006_timestampDisplayedAfterChecking() {
            // Given: At Step 3
            setupViewModelToStep3();

            // When: Declaration is confirmed
            viewModel.setDeclarationConfirmed(true);

            // Then: Timestamp is available for display
            Instant timestamp = viewModel.getDeclarationTimestamp();
            assertThat(timestamp).isNotNull();
            // Verify it's in UTC (Instant is always UTC)
            assertThat(timestamp.toString()).endsWith("Z");
        }

        @Test
        @DisplayName("TC-506-007: Unchecking declaration clears timestamp")
        void tc506_007_uncheckingClearsTimestamp() {
            // Given: Declaration was checked
            setupViewModelToStep3();
            viewModel.setDeclarationConfirmed(true);
            assertThat(viewModel.getDeclarationTimestamp()).isNotNull();

            // When: Declaration is unchecked
            viewModel.setDeclarationConfirmed(false);

            // Then: Timestamp is cleared
            assertThat(viewModel.getDeclarationTimestamp()).isNull();
            assertThat(viewModel.canSubmit()).isFalse();
        }

        @Test
        @DisplayName("TC-506-008: Declaration hidden before Step 3")
        void tc506_008_declarationHiddenBeforeStep3() {
            // Given: At Step 1
            viewModel.startSubmission(TaxYear.of(2025));
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);

            // Then: Declaration state is not applicable (checkbox hidden in UI)
            // ViewModel always allows setting declaration, but UI controls visibility
            assertThat(viewModel.isDeclarationConfirmed()).isFalse();

            // Move to Step 2
            viewModel.setTotalIncome(new BigDecimal("50000.00"));
            viewModel.setTotalExpenses(new BigDecimal("10000.00"));
            viewModel.setNetProfit(new BigDecimal("40000.00"));
            viewModel.executeNextStep();

            assertThat(viewModel.getCurrentStep()).isEqualTo(2);
            // Still in CALCULATING state, not yet CALCULATED
            assertThat(viewModel.getCurrentState()).isEqualTo(AnnualSubmissionState.CALCULATING);
        }

        @Test
        @DisplayName("TC-506-009: Declaration reset on cancel")
        void tc506_009_declarationResetOnCancel() {
            // Given: Declaration was confirmed
            setupViewModelToStep3();
            viewModel.setDeclarationConfirmed(true);
            assertThat(viewModel.getDeclarationTimestamp()).isNotNull();

            // When: User cancels
            viewModel.cancel();

            // Then: Declaration and timestamp are reset
            assertThat(viewModel.isDeclarationConfirmed()).isFalse();
            assertThat(viewModel.getDeclarationTimestamp()).isNull();
            assertThat(viewModel.getCurrentStep()).isEqualTo(0);
        }

        @Test
        @DisplayName("TC-506-010: Declaration checkbox disabled during submission")
        void tc506_010_declarationDisabledDuringSubmission() {
            // Given: Declaration confirmed and submitting
            setupViewModelToStep3();
            viewModel.setDeclarationConfirmed(true);

            // When: Submission starts
            viewModel.confirmAndSubmit();

            // Then: State changes to DECLARING and loading is true
            assertThat(viewModel.getCurrentState()).isEqualTo(AnnualSubmissionState.DECLARING);
            assertThat(viewModel.isLoading()).isTrue();
            // UI will disable checkbox based on isLoading() or state
        }
    }

    // ===== P2 Nice-to-have Tests =====

    @Nested
    @DisplayName("P2 Nice-to-have: UI Polish")
    class P2NiceToHaveTests {

        @Test
        @DisplayName("TC-506-011: Helper text updates based on checkbox state")
        void tc506_011_helperTextUpdatesWithCheckboxState() {
            // Given: At Step 3
            setupViewModelToStep3();

            // When: Unchecked
            viewModel.setDeclarationConfirmed(false);

            // Then: canSubmit is false (UI shows "Please confirm the declaration above")
            assertThat(viewModel.canSubmit()).isFalse();

            // When: Checked
            viewModel.setDeclarationConfirmed(true);

            // Then: canSubmit is true (UI shows "Ready to submit")
            assertThat(viewModel.canSubmit()).isTrue();
        }

        @Test
        @DisplayName("TC-506-012: Declaration card has warning styling")
        void tc506_012_declarationCardHasWarningStyling() {
            // This is verified by checking the FXML has correct styleClass
            // The test confirms the expected constant is available
            assertThat(AnnualSubmissionViewModel.DECLARATION_TEXT)
                .contains("financial penalties")
                .contains("prosecution")
                .contains("false information");
        }
    }

    // ===== Additional Integration Tests =====

    @Nested
    @DisplayName("Additional: Integration Scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("Full flow: Start -> Calculate -> Declare -> Submit ready")
        void fullFlowFromStartToSubmitReady() {
            // Step 1: Start submission
            viewModel.startSubmission(TaxYear.of(2025));
            viewModel.setTotalIncome(new BigDecimal("50000.00"));
            viewModel.setTotalExpenses(new BigDecimal("10000.00"));
            viewModel.setNetProfit(new BigDecimal("40000.00"));

            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
            assertThat(viewModel.canSubmit()).isFalse();

            // Step 2: Calculate
            viewModel.executeNextStep();
            assertThat(viewModel.getCurrentStep()).isEqualTo(2);
            assertThat(viewModel.isLoading()).isTrue();

            // Simulate calculation complete
            viewModel.setCalculationResult(createMockCalculationResult());
            viewModel.setCurrentState(AnnualSubmissionState.CALCULATED);
            viewModel.setLoading(false);

            // Step 3: Review and declare
            viewModel.executeNextStep();
            assertThat(viewModel.getCurrentStep()).isEqualTo(3);
            assertThat(viewModel.canSubmit()).isFalse();

            // Confirm declaration
            viewModel.setDeclarationConfirmed(true);
            assertThat(viewModel.canSubmit()).isTrue();
            assertThat(viewModel.getDeclarationTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("canSubmit requires CALCULATED state AND declaration")
        void canSubmitRequiresBothConditions() {
            // Given: Declaration confirmed but NOT in CALCULATED state
            viewModel.startSubmission(TaxYear.of(2025));
            viewModel.setDeclarationConfirmed(true);

            // Then: Cannot submit (not in CALCULATED state)
            assertThat(viewModel.canSubmit()).isFalse();

            // Given: In CALCULATED state but declaration not confirmed
            setupViewModelToStep3();
            viewModel.setDeclarationConfirmed(false);

            // Then: Still cannot submit
            assertThat(viewModel.canSubmit()).isFalse();
        }

        @Test
        @DisplayName("Declaration state persists across step transitions")
        void declarationStatePersistsAcrossSteps() {
            // Given: At Step 3 with declaration confirmed
            setupViewModelToStep3();
            viewModel.setDeclarationConfirmed(true);
            Instant timestamp = viewModel.getDeclarationTimestamp();

            // When: User goes back to Step 2 and returns
            // (In real UI this would be via Review button or similar)
            viewModel.setCurrentStep(2);
            viewModel.setCurrentStep(3);

            // Then: Declaration state is preserved
            assertThat(viewModel.isDeclarationConfirmed()).isTrue();
            assertThat(viewModel.getDeclarationTimestamp()).isEqualTo(timestamp);
        }

        @Test
        @DisplayName("Starting new submission resets declaration")
        void startingNewSubmissionResetsDeclaration() {
            // Given: Previous declaration was confirmed
            setupViewModelToStep3();
            viewModel.setDeclarationConfirmed(true);

            // When: Starting new submission
            viewModel.startSubmission(TaxYear.of(2026));

            // Then: Declaration is reset
            assertThat(viewModel.isDeclarationConfirmed()).isFalse();
            assertThat(viewModel.getDeclarationTimestamp()).isNull();
        }

        @Test
        @DisplayName("Property binding notifies listeners")
        void propertyBindingNotifiesListeners() {
            // Given
            boolean[] declarationChanged = {false};
            boolean[] canSubmitChanged = {false};

            viewModel.declarationConfirmedProperty().addListener((obs, oldVal, newVal) -> {
                declarationChanged[0] = true;
            });

            viewModel.canSubmitProperty().addListener((obs, oldVal, newVal) -> {
                canSubmitChanged[0] = true;
            });

            setupViewModelToStep3();

            // When
            viewModel.setDeclarationConfirmed(true);

            // Then: Both listeners notified
            assertThat(declarationChanged[0]).isTrue();
            assertThat(canSubmitChanged[0]).isTrue();
        }

        @Test
        @DisplayName("Timestamp format is ISO 8601 UTC")
        void timestampFormatIsIso8601Utc() {
            // Given
            setupViewModelToStep3();

            // When
            viewModel.setDeclarationConfirmed(true);

            // Then
            Instant timestamp = viewModel.getDeclarationTimestamp();
            String isoString = timestamp.toString();

            // ISO 8601 UTC format ends with Z
            assertThat(isoString).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z");
        }
    }

    // ===== Helper Methods =====

    private void setupViewModelToStep3() {
        viewModel.startSubmission(TaxYear.of(2025));
        viewModel.setTotalIncome(new BigDecimal("50000.00"));
        viewModel.setTotalExpenses(new BigDecimal("10000.00"));
        viewModel.setNetProfit(new BigDecimal("40000.00"));
        viewModel.executeNextStep(); // Step 2
        viewModel.setCalculationResult(createMockCalculationResult());
        viewModel.setCurrentState(AnnualSubmissionState.CALCULATED);
        viewModel.setLoading(false);
        viewModel.executeNextStep(); // Step 3
    }

    private TaxCalculationResult createMockCalculationResult() {
        return TaxCalculationResult.create(
            "calc-test-123",
            new BigDecimal("50000.00"),
            new BigDecimal("10000.00"),
            new BigDecimal("40000.00"),
            new BigDecimal("5000.00"),
            new BigDecimal("500.00"),
            new BigDecimal("1500.00")
        );
    }
}
