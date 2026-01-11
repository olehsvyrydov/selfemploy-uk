package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.AnnualSubmissionState;
import uk.selfemploy.common.domain.TaxCalculationResult;
import uk.selfemploy.common.domain.TaxYear;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for AnnualSubmissionViewModel.
 *
 * Tests cover:
 * - Initial state management
 * - Step progression (1-4)
 * - State transitions (INITIATED -> CALCULATING -> CALCULATED -> DECLARING -> COMPLETED)
 * - Error handling and recovery
 * - Observable properties binding
 */
class AnnualSubmissionViewModelTest {

    private AnnualSubmissionViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new AnnualSubmissionViewModel();
    }

    // === Initial State Tests ===

    @Test
    void shouldInitializeWithDefaultState() {
        // Initial state should be null
        assertNull(viewModel.getCurrentState());
        assertNull(viewModel.getCalculationResult());
        assertFalse(viewModel.isLoading());
        assertNull(viewModel.getErrorMessage());
        assertFalse(viewModel.canConfirm());

        // Step should be 0 (not started)
        assertEquals(0, viewModel.getCurrentStep());
        assertEquals("", viewModel.getStepDescription());
    }

    @Test
    void shouldHaveNullFinancialDataInitially() {
        assertNull(viewModel.getTotalIncome());
        assertNull(viewModel.getTotalExpenses());
        assertNull(viewModel.getNetProfit());
        assertNull(viewModel.getTaxDue());
        assertNull(viewModel.getNationalInsurance());
    }

    // === Step Progression Tests ===

    @Test
    void shouldStartAtStep1WhenSubmissionInitiated() {
        TaxYear taxYear = TaxYear.of(2025);

        viewModel.startSubmission(taxYear);

        assertEquals(1, viewModel.getCurrentStep());
        assertEquals("Review Summary", viewModel.getStepDescription());
        assertEquals(AnnualSubmissionState.INITIATED, viewModel.getCurrentState());
        assertFalse(viewModel.canConfirm());
    }

    @Test
    void shouldProgressToStep2WhenCalculating() {
        TaxYear taxYear = TaxYear.of(2025);
        viewModel.startSubmission(taxYear);

        // Set up summary data
        viewModel.setTotalIncome(new BigDecimal("50000.00"));
        viewModel.setTotalExpenses(new BigDecimal("10000.00"));
        viewModel.setNetProfit(new BigDecimal("40000.00"));

        viewModel.executeNextStep();

        assertEquals(2, viewModel.getCurrentStep());
        assertEquals("Calculate Tax", viewModel.getStepDescription());
        assertEquals(AnnualSubmissionState.CALCULATING, viewModel.getCurrentState());
        assertTrue(viewModel.isLoading());
    }

    @Test
    void shouldProgressToStep3WhenCalculationComplete() {
        TaxYear taxYear = TaxYear.of(2025);
        viewModel.startSubmission(taxYear);
        viewModel.setTotalIncome(new BigDecimal("50000.00"));
        viewModel.setTotalExpenses(new BigDecimal("10000.00"));
        viewModel.setNetProfit(new BigDecimal("40000.00"));

        // Simulate calculation completion
        viewModel.executeNextStep(); // Step 2 - Calculating
        viewModel.setCalculationResult(createMockCalculationResult());
        viewModel.setCurrentState(AnnualSubmissionState.CALCULATED);
        viewModel.setLoading(false);

        viewModel.executeNextStep();

        assertEquals(3, viewModel.getCurrentStep());
        assertEquals("Review Calculation", viewModel.getStepDescription());
        assertEquals(AnnualSubmissionState.CALCULATED, viewModel.getCurrentState());
        assertFalse(viewModel.isLoading());
        assertTrue(viewModel.canConfirm());
    }

    @Test
    void shouldProgressToStep4WhenSubmitting() {
        setupToStep3();

        viewModel.confirmAndSubmit();

        assertEquals(4, viewModel.getCurrentStep());
        assertEquals("Submit Declaration", viewModel.getStepDescription());
        assertEquals(AnnualSubmissionState.DECLARING, viewModel.getCurrentState());
        assertTrue(viewModel.isLoading());
    }

    // === State Transition Tests ===

    @Test
    void shouldTransitionFromInitiatedToCalculating() {
        viewModel.setCurrentState(AnnualSubmissionState.INITIATED);
        // Set up required summary data
        viewModel.setTotalIncome(new java.math.BigDecimal("50000.00"));
        viewModel.setTotalExpenses(new java.math.BigDecimal("15000.00"));
        viewModel.setNetProfit(new java.math.BigDecimal("35000.00"));

        viewModel.executeNextStep();

        assertEquals(AnnualSubmissionState.CALCULATING, viewModel.getCurrentState());
    }

    @Test
    void shouldTransitionFromCalculatedToDeclaring() {
        viewModel.setCurrentState(AnnualSubmissionState.CALCULATED);
        viewModel.setCurrentStep(3);

        viewModel.confirmAndSubmit();

        assertEquals(AnnualSubmissionState.DECLARING, viewModel.getCurrentState());
    }

    @Test
    void shouldTransitionToCompletedOnSuccess() {
        setupToStep4();

        viewModel.setCurrentState(AnnualSubmissionState.COMPLETED);
        viewModel.setLoading(false);

        assertEquals(AnnualSubmissionState.COMPLETED, viewModel.getCurrentState());
        assertFalse(viewModel.isLoading());
        assertFalse(viewModel.canConfirm());
    }

    @Test
    void shouldTransitionToFailedOnError() {
        setupToStep4();

        viewModel.setCurrentState(AnnualSubmissionState.FAILED);
        viewModel.setErrorMessage("Network error occurred");
        viewModel.setLoading(false);

        assertEquals(AnnualSubmissionState.FAILED, viewModel.getCurrentState());
        assertEquals("Network error occurred", viewModel.getErrorMessage());
        assertFalse(viewModel.isLoading());
    }

    // === Error Handling Tests ===

    @Test
    void shouldAllowResumeFromFailedState() {
        viewModel.setCurrentState(AnnualSubmissionState.FAILED);
        viewModel.setErrorMessage("Previous error");
        UUID sagaId = UUID.randomUUID();
        viewModel.setSagaId(sagaId);

        viewModel.resumeSubmission(sagaId);

        assertNull(viewModel.getErrorMessage());
        assertNotEquals(AnnualSubmissionState.FAILED, viewModel.getCurrentState());
    }

    @Test
    void shouldClearErrorMessageOnRetry() {
        viewModel.setErrorMessage("Previous error");

        viewModel.clearError();

        assertNull(viewModel.getErrorMessage());
    }

    @Test
    void shouldPreventProgressIfNoSummaryData() {
        viewModel.startSubmission(TaxYear.of(2025));

        // No financial data set
        assertThrows(IllegalStateException.class, () -> viewModel.executeNextStep());
    }

    // === Cancel Operation Tests ===

    @Test
    void shouldResetStateOnCancel() {
        setupToStep3();

        viewModel.cancel();

        assertNull(viewModel.getCurrentState());
        assertEquals(0, viewModel.getCurrentStep());
        assertEquals("", viewModel.getStepDescription());
        assertFalse(viewModel.canConfirm());
    }

    // === Property Binding Tests ===

    @Test
    void shouldUpdateFinancialPropertiesCorrectly() {
        BigDecimal income = new BigDecimal("50000.00");
        BigDecimal expenses = new BigDecimal("10000.00");
        BigDecimal profit = new BigDecimal("40000.00");

        viewModel.setTotalIncome(income);
        viewModel.setTotalExpenses(expenses);
        viewModel.setNetProfit(profit);

        assertEquals(income, viewModel.getTotalIncome());
        assertEquals(expenses, viewModel.getTotalExpenses());
        assertEquals(profit, viewModel.getNetProfit());
    }

    @Test
    void shouldUpdateTaxCalculationProperties() {
        TaxCalculationResult result = createMockCalculationResult();

        viewModel.setCalculationResult(result);

        assertNotNull(viewModel.getCalculationResult());
        // incomeTax from common module's TaxCalculationResult
        assertEquals(new BigDecimal("5000.00"), viewModel.getTaxDue());
        // NI Class 2 (500) + NI Class 4 (1500) = 2000
        assertEquals(new BigDecimal("2000.00"), viewModel.getNationalInsurance());
    }

    @Test
    void shouldNotifyPropertyChanges() {
        boolean[] changed = {false};

        viewModel.currentStateProperty().addListener((obs, oldVal, newVal) -> {
            changed[0] = true;
        });

        viewModel.setCurrentState(AnnualSubmissionState.INITIATED);

        assertTrue(changed[0]);
    }

    @Test
    void shouldUpdateStepDescriptionBasedOnStep() {
        viewModel.setCurrentStep(1);
        assertEquals("Review Summary", viewModel.getStepDescription());

        viewModel.setCurrentStep(2);
        assertEquals("Calculate Tax", viewModel.getStepDescription());

        viewModel.setCurrentStep(3);
        assertEquals("Review Calculation", viewModel.getStepDescription());

        viewModel.setCurrentStep(4);
        assertEquals("Submit Declaration", viewModel.getStepDescription());
    }

    // === Saga ID Management Tests ===

    @Test
    void shouldGenerateSagaIdOnStart() {
        TaxYear taxYear = TaxYear.of(2025);

        viewModel.startSubmission(taxYear);

        // startSubmission generates a new saga ID
        assertNotNull(viewModel.getSagaId());
    }

    @Test
    void shouldAllowResumingWithExistingSagaId() {
        UUID sagaId = UUID.randomUUID();

        viewModel.resumeSubmission(sagaId);

        assertEquals(sagaId, viewModel.getSagaId());
        assertNotNull(viewModel.getCurrentState());
    }

    // === Declaration Checkbox Tests (SE-506) ===

    @Nested
    @DisplayName("Declaration Checkbox - SE-506")
    class DeclarationCheckboxTests {

        @Test
        @DisplayName("AC-2: should have declaration unchecked by default")
        void shouldHaveDeclarationUncheckedByDefault() {
            assertFalse(viewModel.isDeclarationConfirmed());
        }

        @Test
        @DisplayName("AC-4: should record timestamp when declaration is confirmed")
        void shouldRecordTimestampWhenDeclarationConfirmed() {
            // Given
            Instant beforeConfirm = Instant.now();

            // When
            viewModel.setDeclarationConfirmed(true);

            // Then
            assertTrue(viewModel.isDeclarationConfirmed());
            assertNotNull(viewModel.getDeclarationTimestamp());
            Instant timestamp = viewModel.getDeclarationTimestamp();
            assertTrue(timestamp.isAfter(beforeConfirm) || timestamp.equals(beforeConfirm));
            assertTrue(timestamp.isBefore(Instant.now().plusSeconds(1)));
        }

        @Test
        @DisplayName("AC-4: should clear timestamp when declaration is unchecked")
        void shouldClearTimestampWhenDeclarationUnchecked() {
            // Given
            viewModel.setDeclarationConfirmed(true);
            assertNotNull(viewModel.getDeclarationTimestamp());

            // When
            viewModel.setDeclarationConfirmed(false);

            // Then
            assertFalse(viewModel.isDeclarationConfirmed());
            assertNull(viewModel.getDeclarationTimestamp());
        }

        @Test
        @DisplayName("AC-4: should store timestamp in UTC ISO 8601 format")
        void shouldStoreTimestampInUtcIso8601Format() {
            // When
            viewModel.setDeclarationConfirmed(true);

            // Then
            Instant timestamp = viewModel.getDeclarationTimestamp();
            assertNotNull(timestamp);
            // Verify it's a valid Instant (UTC by definition)
            String iso8601 = timestamp.toString();
            assertTrue(iso8601.endsWith("Z"), "Timestamp should be in UTC (end with Z)");
        }

        @Test
        @DisplayName("AC-3: submit button should be disabled when declaration not confirmed")
        void shouldDisableSubmitWhenDeclarationNotConfirmed() {
            // Given
            setupToStep3();

            // When - declaration not confirmed
            viewModel.setDeclarationConfirmed(false);

            // Then
            assertFalse(viewModel.canSubmit());
        }

        @Test
        @DisplayName("AC-3: submit button should be enabled when declaration is confirmed")
        void shouldEnableSubmitWhenDeclarationConfirmed() {
            // Given
            setupToStep3();

            // When
            viewModel.setDeclarationConfirmed(true);

            // Then
            assertTrue(viewModel.canSubmit());
        }

        @Test
        @DisplayName("AC-3: canSubmit requires both CALCULATED state and declaration")
        void canSubmitRequiresBothCalculatedStateAndDeclaration() {
            // Given - not in CALCULATED state
            viewModel.startSubmission(TaxYear.of(2025));
            viewModel.setDeclarationConfirmed(true);

            // Then - still can't submit
            assertFalse(viewModel.canSubmit());
        }

        @Test
        @DisplayName("should notify property changes when declaration changes")
        void shouldNotifyPropertyChangesWhenDeclarationChanges() {
            // Given
            boolean[] changed = {false};
            viewModel.declarationConfirmedProperty().addListener((obs, oldVal, newVal) -> {
                changed[0] = true;
            });

            // When
            viewModel.setDeclarationConfirmed(true);

            // Then
            assertTrue(changed[0]);
        }

        @Test
        @DisplayName("should reset declaration on cancel")
        void shouldResetDeclarationOnCancel() {
            // Given
            setupToStep3();
            viewModel.setDeclarationConfirmed(true);
            assertNotNull(viewModel.getDeclarationTimestamp());

            // When
            viewModel.cancel();

            // Then
            assertFalse(viewModel.isDeclarationConfirmed());
            assertNull(viewModel.getDeclarationTimestamp());
        }

        @Test
        @DisplayName("should reset declaration when starting new submission")
        void shouldResetDeclarationWhenStartingNewSubmission() {
            // Given
            viewModel.setDeclarationConfirmed(true);

            // When
            viewModel.startSubmission(TaxYear.of(2025));

            // Then
            assertFalse(viewModel.isDeclarationConfirmed());
            assertNull(viewModel.getDeclarationTimestamp());
        }

        @Test
        @DisplayName("AC-5: should return official HMRC declaration text")
        void shouldReturnOfficialHmrcDeclarationText() {
            // Given
            String expectedText = "I declare that the information I have given on this tax return " +
                "and any supplementary pages is correct and complete to the best of my knowledge and belief. " +
                "I understand that I may have to pay financial penalties and face prosecution if I give false information.";

            // Then
            assertEquals(expectedText, AnnualSubmissionViewModel.DECLARATION_TEXT);
        }
    }

    // === Helper Methods ===

    private void setupToStep3() {
        TaxYear taxYear = TaxYear.of(2025);
        viewModel.startSubmission(taxYear);
        viewModel.setTotalIncome(new BigDecimal("50000.00"));
        viewModel.setTotalExpenses(new BigDecimal("10000.00"));
        viewModel.setNetProfit(new BigDecimal("40000.00"));
        viewModel.executeNextStep(); // Step 2
        viewModel.setCalculationResult(createMockCalculationResult());
        viewModel.setCurrentState(AnnualSubmissionState.CALCULATED);
        viewModel.setLoading(false);
        viewModel.executeNextStep(); // Step 3
    }

    private void setupToStep4() {
        setupToStep3();
        viewModel.confirmAndSubmit();
    }

    private TaxCalculationResult createMockCalculationResult() {
        // Using common module's TaxCalculationResult.create() factory method
        // incomeTax=5000, NI Class 2=500, NI Class 4=1500 (total NI = 2000)
        return TaxCalculationResult.create(
            "calc-123",                     // calculationId
            new BigDecimal("50000.00"),     // totalIncome
            new BigDecimal("10000.00"),     // totalExpenses
            new BigDecimal("40000.00"),     // netProfit
            new BigDecimal("5000.00"),      // incomeTax (equivalent to taxDue)
            new BigDecimal("500.00"),       // nationalInsuranceClass2
            new BigDecimal("1500.00")       // nationalInsuranceClass4
        );
    }
}
