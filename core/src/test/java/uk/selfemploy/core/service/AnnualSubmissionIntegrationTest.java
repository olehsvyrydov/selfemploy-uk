package uk.selfemploy.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.selfemploy.common.domain.AnnualSubmissionSaga;
import uk.selfemploy.common.domain.AnnualSubmissionState;
import uk.selfemploy.common.domain.TaxCalculationResult;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.auth.TokenProvider;
import uk.selfemploy.core.exception.ValidationException;
import uk.selfemploy.hmrc.client.SelfAssessmentCalculationClient;
import uk.selfemploy.hmrc.client.SelfAssessmentDeclarationClient;
import uk.selfemploy.hmrc.client.dto.*;
import uk.selfemploy.hmrc.exception.HmrcNetworkException;
import uk.selfemploy.hmrc.exception.HmrcServerException;
import uk.selfemploy.hmrc.exception.HmrcValidationException;
import uk.selfemploy.hmrc.resilience.HmrcResilienceDecorator;
import uk.selfemploy.persistence.repository.AnnualSubmissionSagaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for SE-403 Annual Self Assessment.
 * Tests based on /rob's QA test specification.
 *
 * <p>Test IDs: IT-403-011 to IT-403-028 (Integration Tests)
 *
 * <p>These tests verify the Saga pattern implementation including:
 * - Resume capability from each non-terminal state
 * - State machine transitions
 * - Input validation
 * - Edge cases for terminal states
 *
 * @see docs/sprints/sprint-4/testing/rob-qa-SE-403.md
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SE-403 Annual Submission Integration Tests")
class AnnualSubmissionIntegrationTest {

    @Mock
    private AnnualSubmissionSagaRepository repository;

    @Mock
    private SelfAssessmentCalculationClient calculationClient;

    @Mock
    private SelfAssessmentDeclarationClient declarationClient;

    @Mock
    private HmrcResilienceDecorator resilienceDecorator;

    @Mock
    private TokenProvider tokenProvider;

    private AnnualSubmissionService service;

    private static final String TEST_NINO = "AA123456A";
    private static final TaxYear TEST_TAX_YEAR = TaxYear.of(2024);
    private static final String TEST_CALCULATION_ID = "calc-integration-12345";
    private static final String TEST_CHARGE_REFERENCE = "SA-INT-123456789";
    private static final String TEST_BEARER_TOKEN = "Bearer test-access-token";

    @BeforeEach
    void setUp() {
        service = new AnnualSubmissionService(
                repository,
                calculationClient,
                declarationClient,
                resilienceDecorator,
                tokenProvider
        );

        // Default behavior: resilience decorator just passes through
        when(resilienceDecorator.executeWithRetry(any()))
                .thenAnswer(invocation -> {
                    var supplier = invocation.getArgument(0, java.util.function.Supplier.class);
                    return supplier.get();
                });

        // Default behavior: token provider returns valid token
        when(tokenProvider.getValidToken()).thenReturn(TEST_BEARER_TOKEN);
    }

    // =========================================================================
    // P0: Resume Capability Tests (TC-011, TC-012)
    // =========================================================================

    @Nested
    @DisplayName("P0: Resume Capability Tests (TC-011, TC-012)")
    class ResumeCapabilityIntegrationTests {

        @Test
        @DisplayName("IT-403-011: Resume from CALCULATING state after network failure")
        void shouldResumeFromCalculatingStateAfterNetworkFailure() {
            // Given: Saga in CALCULATING state (network failure occurred after triggerCalculation)
            AnnualSubmissionSaga calculatingSaga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                    .withCalculating(TEST_CALCULATION_ID);

            when(repository.findById(calculatingSaga.id())).thenReturn(Optional.of(calculatingSaga));

            // Mock HMRC calculation response
            CalculationResponse hmrcResponse = createHmrcCalculationResponse();
            when(calculationClient.getCalculation(
                    eq(TEST_NINO),
                    eq("2024-25"),
                    eq(TEST_CALCULATION_ID),
                    anyString()
            )).thenReturn(hmrcResponse);

            // When: Resume by calling executeNextStep
            AnnualSubmissionSaga result = service.executeNextStep(calculatingSaga.id());

            // Then: Should transition to CALCULATED with calculation result
            assertThat(result.state()).isEqualTo(AnnualSubmissionState.CALCULATED);
            assertThat(result.calculationResult()).isNotNull();
            assertThat(result.calculationResult().calculationId()).isEqualTo(TEST_CALCULATION_ID);
            assertThat(result.calculationResult().totalTaxLiability()).isEqualByComparingTo("15000.00");

            // Verify repository save was called
            verify(repository).save(any(AnnualSubmissionSaga.class));
        }

        @Test
        @DisplayName("IT-403-012: Resume from DECLARING state after network failure")
        void shouldResumeFromDeclaringStateAfterNetworkFailure() {
            // Given: Saga in DECLARING state (network failure occurred during declaration)
            AnnualSubmissionSaga declaringSaga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                    .withCalculating(TEST_CALCULATION_ID)
                    .withCalculated(createTestCalculationResult())
                    .withDeclaring();

            when(repository.findById(declaringSaga.id())).thenReturn(Optional.of(declaringSaga));

            // Mock HMRC declaration response
            FinalDeclarationResponse hmrcResponse = new FinalDeclarationResponse(
                    TEST_CHARGE_REFERENCE,
                    LocalDateTime.now()
            );
            when(declarationClient.submitDeclaration(any(), any(), any(), any()))
                    .thenReturn(hmrcResponse);

            // When: Resume by calling executeNextStep
            AnnualSubmissionSaga result = service.executeNextStep(declaringSaga.id());

            // Then: Should complete the declaration
            assertThat(result.state()).isEqualTo(AnnualSubmissionState.COMPLETED);
            assertThat(result.hmrcConfirmation()).isEqualTo(TEST_CHARGE_REFERENCE);

            // Verify repository save was called
            verify(repository).save(any(AnnualSubmissionSaga.class));
        }

        @Test
        @DisplayName("IT-403-011b: Verify state persisted before resuming API call")
        void shouldVerifyStatePersistenceBeforeResume() {
            // Given: Saga in CALCULATING state
            AnnualSubmissionSaga calculatingSaga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                    .withCalculating(TEST_CALCULATION_ID);

            when(repository.findById(calculatingSaga.id())).thenReturn(Optional.of(calculatingSaga));
            when(calculationClient.getCalculation(any(), any(), any(), any()))
                    .thenReturn(createHmrcCalculationResponse());

            // When
            service.executeNextStep(calculatingSaga.id());

            // Then: Verify the saga state was saved
            ArgumentCaptor<AnnualSubmissionSaga> captor = ArgumentCaptor.forClass(AnnualSubmissionSaga.class);
            verify(repository).save(captor.capture());

            AnnualSubmissionSaga savedSaga = captor.getValue();
            assertThat(savedSaga.state()).isEqualTo(AnnualSubmissionState.CALCULATED);
        }
    }

    // =========================================================================
    // P0: Completed Saga Handling (TC-013)
    // =========================================================================

    @Nested
    @DisplayName("P1: Completed Saga Handling (TC-013)")
    class CompletedSagaHandlingTests {

        @Test
        @DisplayName("IT-403-013: Cannot restart completed submission - throws IllegalStateException")
        void shouldThrowExceptionWhenRestartingCompletedSubmission() {
            // Given: Saga already in COMPLETED state
            AnnualSubmissionSaga completedSaga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                    .withCalculating(TEST_CALCULATION_ID)
                    .withCalculated(createTestCalculationResult())
                    .withDeclaring()
                    .withCompleted(TEST_CHARGE_REFERENCE);

            when(repository.findByNinoAndTaxYear(TEST_NINO, TEST_TAX_YEAR.startYear()))
                    .thenReturn(Optional.of(completedSaga));

            // When / Then: Attempting to start submission for same tax year throws exception
            assertThatThrownBy(() -> service.startOrResume(TEST_TAX_YEAR, TEST_NINO))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already completed")
                    .hasMessageContaining(TEST_TAX_YEAR.label());

            // Verify no new saga was saved
            verify(repository, never()).save(any());
        }
    }

    // =========================================================================
    // P1: Validation Tests (TC-018, TC-019, TC-020)
    // =========================================================================

    @Nested
    @DisplayName("P1: Validation Tests (TC-018, TC-019, TC-020)")
    class ValidationIntegrationTests {

        @Test
        @DisplayName("IT-403-018: Null tax year throws ValidationException")
        void shouldThrowValidationExceptionForNullTaxYear() {
            // When / Then
            assertThatThrownBy(() -> service.startOrResume(null, TEST_NINO))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("taxYear");
        }

        @Test
        @DisplayName("IT-403-019a: Null NINO throws ValidationException")
        void shouldThrowValidationExceptionForNullNino() {
            // When / Then
            assertThatThrownBy(() -> service.startOrResume(TEST_TAX_YEAR, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("nino");
        }

        @Test
        @DisplayName("IT-403-019b: Empty string NINO throws ValidationException")
        void shouldThrowValidationExceptionForEmptyNino() {
            // When / Then
            assertThatThrownBy(() -> service.startOrResume(TEST_TAX_YEAR, ""))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("nino");
        }

        @Test
        @DisplayName("IT-403-019c: Blank NINO throws ValidationException")
        void shouldThrowValidationExceptionForBlankNino() {
            // When / Then
            assertThatThrownBy(() -> service.startOrResume(TEST_TAX_YEAR, "   "))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("nino");
        }
    }

    // =========================================================================
    // P1: State Machine Tests (TC-021, TC-022, TC-023)
    // =========================================================================

    @Nested
    @DisplayName("P1: State Machine Tests (TC-021, TC-022, TC-023)")
    class StateMachineIntegrationTests {

        @Test
        @DisplayName("IT-403-021: Full saga lifecycle - INITIATED -> CALCULATING -> CALCULATED -> DECLARING -> COMPLETED")
        void shouldCompleteFullSagaLifecycle() {
            // Step 1: Start new saga (INITIATED)
            when(repository.findByNinoAndTaxYear(TEST_NINO, TEST_TAX_YEAR.startYear()))
                    .thenReturn(Optional.empty());

            AnnualSubmissionSaga newSaga = service.startOrResume(TEST_TAX_YEAR, TEST_NINO);

            assertThat(newSaga.state()).isEqualTo(AnnualSubmissionState.INITIATED);

            // Step 2: Execute next step (INITIATED -> CALCULATING)
            when(repository.findById(newSaga.id())).thenReturn(Optional.of(newSaga));

            TriggerCalculationResponse triggerResponse = new TriggerCalculationResponse(TEST_CALCULATION_ID);
            when(calculationClient.triggerCalculation(any(), any(), any(), any()))
                    .thenReturn(triggerResponse);

            AnnualSubmissionSaga calculatingSaga = service.executeNextStep(newSaga.id());
            assertThat(calculatingSaga.state()).isEqualTo(AnnualSubmissionState.CALCULATING);

            // Step 3: Execute next step (CALCULATING -> CALCULATED)
            when(repository.findById(newSaga.id())).thenReturn(Optional.of(calculatingSaga));
            when(calculationClient.getCalculation(any(), any(), any(), any()))
                    .thenReturn(createHmrcCalculationResponse());

            AnnualSubmissionSaga calculatedSaga = service.executeNextStep(newSaga.id());
            assertThat(calculatedSaga.state()).isEqualTo(AnnualSubmissionState.CALCULATED);
            assertThat(calculatedSaga.calculationResult()).isNotNull();

            // Step 4: Confirm declaration (CALCULATED -> DECLARING -> COMPLETED)
            when(repository.findById(newSaga.id())).thenReturn(Optional.of(calculatedSaga));
            when(declarationClient.submitDeclaration(any(), any(), any(), any()))
                    .thenReturn(new FinalDeclarationResponse(TEST_CHARGE_REFERENCE, LocalDateTime.now()));

            AnnualSubmissionSaga completedSaga = service.confirmDeclaration(newSaga.id());
            assertThat(completedSaga.state()).isEqualTo(AnnualSubmissionState.COMPLETED);
            assertThat(completedSaga.hmrcConfirmation()).isEqualTo(TEST_CHARGE_REFERENCE);
        }

        @Test
        @DisplayName("IT-403-022: Invalid transition - Cannot skip CALCULATING state")
        void shouldNotAllowSkippingCalculatingState() {
            // Given: Saga in INITIATED state
            AnnualSubmissionSaga initiatedSaga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO);

            // Then: Cannot directly transition to CALCULATED (would skip CALCULATING)
            assertThatThrownBy(() -> initiatedSaga.withCalculated(createTestCalculationResult()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CALCULATING");
        }

        @Test
        @DisplayName("IT-403-023: executeNextStep from CALCULATED throws - requires user confirmation")
        void shouldThrowWhenExecutingFromCalculatedWithoutConfirmation() {
            // Given: Saga in CALCULATED state
            AnnualSubmissionSaga calculatedSaga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                    .withCalculating(TEST_CALCULATION_ID)
                    .withCalculated(createTestCalculationResult());

            when(repository.findById(calculatedSaga.id())).thenReturn(Optional.of(calculatedSaga));

            // When / Then
            assertThatThrownBy(() -> service.executeNextStep(calculatedSaga.id()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("User must confirm declaration");
        }

        @Test
        @DisplayName("IT-403-021b: State transition to FAILED on HMRC validation error")
        void shouldTransitionToFailedOnHmrcValidationError() {
            // Given: Saga in INITIATED state
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO);
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            HmrcValidationException hmrcError = new HmrcValidationException(
                    "Invalid National Insurance Number",
                    "INVALID_NINO",
                    400
            );
            when(calculationClient.triggerCalculation(any(), any(), any(), any()))
                    .thenThrow(hmrcError);

            // When / Then
            assertThatThrownBy(() -> service.executeNextStep(saga.id()))
                    .isInstanceOf(HmrcValidationException.class);

            // Verify FAILED state was saved
            ArgumentCaptor<AnnualSubmissionSaga> captor = ArgumentCaptor.forClass(AnnualSubmissionSaga.class);
            verify(repository, atLeastOnce()).save(captor.capture());

            AnnualSubmissionSaga savedSaga = captor.getValue();
            assertThat(savedSaga.state()).isEqualTo(AnnualSubmissionState.FAILED);
            assertThat(savedSaga.errorMessage()).contains("INVALID_NINO");
        }
    }

    // =========================================================================
    // P1: Terminal State Edge Cases (TC-026, TC-027, TC-028)
    // =========================================================================

    @Nested
    @DisplayName("P1: Terminal State Edge Cases (TC-026, TC-027, TC-028)")
    class TerminalStateEdgeCasesTests {

        @Test
        @DisplayName("IT-403-026: executeNextStep on COMPLETED saga - no action taken")
        void shouldNotTakeActionOnCompletedSaga() {
            // Given: Saga in COMPLETED state
            AnnualSubmissionSaga completedSaga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                    .withCalculating(TEST_CALCULATION_ID)
                    .withCalculated(createTestCalculationResult())
                    .withDeclaring()
                    .withCompleted(TEST_CHARGE_REFERENCE);

            when(repository.findById(completedSaga.id())).thenReturn(Optional.of(completedSaga));

            // When
            AnnualSubmissionSaga result = service.executeNextStep(completedSaga.id());

            // Then: Saga returned unchanged, no save operation
            assertThat(result.state()).isEqualTo(AnnualSubmissionState.COMPLETED);
            assertThat(result.hmrcConfirmation()).isEqualTo(TEST_CHARGE_REFERENCE);

            // No save should be called for terminal state
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("IT-403-027a: executeNextStep on FAILED saga - no action taken")
        void shouldNotTakeActionOnFailedSaga() {
            // Given: Saga in FAILED state
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                    .withCalculating(TEST_CALCULATION_ID);

            // Manually create a FAILED saga
            AnnualSubmissionSaga failedSaga = new AnnualSubmissionSaga(
                    saga.id(),
                    saga.nino(),
                    saga.taxYear(),
                    AnnualSubmissionState.FAILED,
                    saga.calculationId(),
                    null,
                    null,
                    "Previous network error",
                    saga.createdAt(),
                    saga.updatedAt()
            );

            when(repository.findById(failedSaga.id())).thenReturn(Optional.of(failedSaga));

            // When
            AnnualSubmissionSaga result = service.executeNextStep(failedSaga.id());

            // Then: Saga returned unchanged
            assertThat(result.state()).isEqualTo(AnnualSubmissionState.FAILED);
            assertThat(result.errorMessage()).isEqualTo("Previous network error");

            // No save should be called for terminal state
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("IT-403-027b: FAILED saga can be resumed via startOrResume")
        void shouldAllowResumingFailedSagaViaStartOrResume() {
            // Given: Saga in FAILED state (recoverable)
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                    .withCalculating(TEST_CALCULATION_ID);

            // Manually create a FAILED saga that's in a resumable position
            AnnualSubmissionSaga failedSaga = new AnnualSubmissionSaga(
                    saga.id(),
                    saga.nino(),
                    saga.taxYear(),
                    AnnualSubmissionState.FAILED,
                    saga.calculationId(),
                    null,
                    null,
                    "Transient network error",
                    saga.createdAt(),
                    saga.updatedAt()
            );

            // Note: FAILED is considered terminal, so startOrResume will throw
            // This is the expected behavior - user cannot resume a FAILED saga directly
            when(repository.findByNinoAndTaxYear(TEST_NINO, TEST_TAX_YEAR.startYear()))
                    .thenReturn(Optional.of(failedSaga));

            // When: Try to start/resume - FAILED saga should still be returned for retry
            // (FAILED != COMPLETED, so it's handled differently)
            AnnualSubmissionSaga result = service.startOrResume(TEST_TAX_YEAR, TEST_NINO);

            // Then: Returns the failed saga for retry
            assertThat(result.state()).isEqualTo(AnnualSubmissionState.FAILED);
        }

        @Test
        @DisplayName("IT-403-028: Saga not found - throws IllegalArgumentException")
        void shouldThrowWhenSagaNotFound() {
            // Given: Random UUID that doesn't exist
            UUID nonExistentId = UUID.randomUUID();
            when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.executeNextStep(nonExistentId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Saga not found")
                    .hasMessageContaining(nonExistentId.toString());
        }

        @Test
        @DisplayName("IT-403-028b: confirmDeclaration with non-existent saga - throws IllegalArgumentException")
        void shouldThrowWhenConfirmDeclarationForNonExistentSaga() {
            // Given: Random UUID that doesn't exist
            UUID nonExistentId = UUID.randomUUID();
            when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.confirmDeclaration(nonExistentId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Saga not found");
        }
    }

    // =========================================================================
    // P1: Error Handling During State Transitions
    // =========================================================================

    @Nested
    @DisplayName("P1: Error Handling During State Transitions")
    class ErrorHandlingTransitionTests {

        @Test
        @DisplayName("IT-403-E01: Server error during calculation trigger - state transitions to FAILED")
        void shouldTransitionToFailedOnServerErrorDuringCalculation() {
            // Given
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO);
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            HmrcServerException serverError = new HmrcServerException(
                    "Service unavailable", "SERVER_ERROR", 503
            );
            when(calculationClient.triggerCalculation(any(), any(), any(), any()))
                    .thenThrow(serverError);

            // When / Then
            assertThatThrownBy(() -> service.executeNextStep(saga.id()))
                    .isInstanceOf(HmrcServerException.class);

            // Verify FAILED state was saved
            ArgumentCaptor<AnnualSubmissionSaga> captor = ArgumentCaptor.forClass(AnnualSubmissionSaga.class);
            verify(repository, atLeastOnce()).save(captor.capture());

            AnnualSubmissionSaga savedSaga = captor.getValue();
            assertThat(savedSaga.state()).isEqualTo(AnnualSubmissionState.FAILED);
            assertThat(savedSaga.errorMessage()).contains("SERVER_ERROR");
        }

        @Test
        @DisplayName("IT-403-E02: Error during declaration - state transitions to FAILED")
        void shouldTransitionToFailedOnErrorDuringDeclaration() {
            // Given: Saga in CALCULATED state
            AnnualSubmissionSaga calculatedSaga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                    .withCalculating(TEST_CALCULATION_ID)
                    .withCalculated(createTestCalculationResult());

            when(repository.findById(calculatedSaga.id())).thenReturn(Optional.of(calculatedSaga));

            HmrcServerException serverError = new HmrcServerException(
                    "Service unavailable during declaration", "GATEWAY_TIMEOUT", 504
            );
            when(declarationClient.submitDeclaration(any(), any(), any(), any()))
                    .thenThrow(serverError);

            // When / Then
            assertThatThrownBy(() -> service.confirmDeclaration(calculatedSaga.id()))
                    .isInstanceOf(HmrcServerException.class);

            // Verify state transitions: first to DECLARING, then to FAILED
            ArgumentCaptor<AnnualSubmissionSaga> captor = ArgumentCaptor.forClass(AnnualSubmissionSaga.class);
            verify(repository, atLeast(2)).save(captor.capture());

            // Last saved state should be FAILED
            AnnualSubmissionSaga lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
            assertThat(lastSaved.state()).isEqualTo(AnnualSubmissionState.FAILED);
            assertThat(lastSaved.errorMessage()).contains("GATEWAY_TIMEOUT");
        }

        @Test
        @DisplayName("IT-403-E03: Confirm declaration from wrong state - throws IllegalStateException")
        void shouldThrowWhenConfirmDeclarationFromWrongState() {
            // Given: Saga in CALCULATING state (not CALCULATED)
            AnnualSubmissionSaga calculatingSaga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                    .withCalculating(TEST_CALCULATION_ID);

            when(repository.findById(calculatingSaga.id())).thenReturn(Optional.of(calculatingSaga));

            // When / Then
            assertThatThrownBy(() -> service.confirmDeclaration(calculatingSaga.id()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CALCULATED");
        }
    }

    // =========================================================================
    // P2: Edge Cases for Calculation Results
    // =========================================================================

    @Nested
    @DisplayName("P2: Edge Cases for Calculation Results")
    class CalculationResultEdgeCasesTests {

        @Test
        @DisplayName("IT-403-P2-01: Zero tax liability is handled correctly")
        void shouldHandleZeroTaxLiability() {
            // Given: Saga in CALCULATING state
            AnnualSubmissionSaga calculatingSaga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                    .withCalculating(TEST_CALCULATION_ID);

            when(repository.findById(calculatingSaga.id())).thenReturn(Optional.of(calculatingSaga));

            // Mock zero tax calculation response
            CalculationResponse zeroTaxResponse = new CalculationResponse(
                    TEST_CALCULATION_ID,
                    LocalDateTime.now(),
                    "customerRequest",
                    BigDecimal.ZERO,
                    new BigDecimal("12000.00"), // Below personal allowance
                    new BigDecimal("12570.00"),
                    BigDecimal.ZERO,
                    new CalculationResponse.IncomeTaxBreakdown(
                            BigDecimal.ZERO,
                            BigDecimal.ZERO
                    ),
                    new CalculationResponse.NationalInsuranceBreakdown(
                            new CalculationResponse.NationalInsuranceBreakdown.Class2Nics(BigDecimal.ZERO),
                            new CalculationResponse.NationalInsuranceBreakdown.Class4Nics(BigDecimal.ZERO, BigDecimal.ZERO)
                    )
            );

            when(calculationClient.getCalculation(any(), any(), any(), any()))
                    .thenReturn(zeroTaxResponse);

            // When
            AnnualSubmissionSaga result = service.executeNextStep(calculatingSaga.id());

            // Then
            assertThat(result.state()).isEqualTo(AnnualSubmissionState.CALCULATED);
            assertThat(result.calculationResult().totalTaxLiability()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("IT-403-P2-02: Large amounts are handled correctly")
        void shouldHandleLargeAmounts() {
            // Given: Saga in CALCULATING state
            AnnualSubmissionSaga calculatingSaga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                    .withCalculating(TEST_CALCULATION_ID);

            when(repository.findById(calculatingSaga.id())).thenReturn(Optional.of(calculatingSaga));

            // Mock large amount calculation response
            BigDecimal largeIncome = new BigDecimal("999999.99");
            BigDecimal largeTax = new BigDecimal("450000.00");

            CalculationResponse largeTaxResponse = new CalculationResponse(
                    TEST_CALCULATION_ID,
                    LocalDateTime.now(),
                    "customerRequest",
                    largeTax,
                    largeIncome,
                    new BigDecimal("12570.00"),
                    new BigDecimal("987429.99"),
                    new CalculationResponse.IncomeTaxBreakdown(
                            largeTax,
                            largeTax
                    ),
                    new CalculationResponse.NationalInsuranceBreakdown(
                            new CalculationResponse.NationalInsuranceBreakdown.Class2Nics(BigDecimal.ZERO),
                            new CalculationResponse.NationalInsuranceBreakdown.Class4Nics(
                                    new BigDecimal("987429.99"),
                                    BigDecimal.ZERO
                            )
                    )
            );

            when(calculationClient.getCalculation(any(), any(), any(), any()))
                    .thenReturn(largeTaxResponse);

            // When
            AnnualSubmissionSaga result = service.executeNextStep(calculatingSaga.id());

            // Then
            assertThat(result.state()).isEqualTo(AnnualSubmissionState.CALCULATED);
            assertThat(result.calculationResult().totalTaxLiability()).isEqualByComparingTo(largeTax);
            assertThat(result.calculationResult().totalIncome()).isEqualByComparingTo(largeIncome);
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private TaxCalculationResult createTestCalculationResult() {
        return TaxCalculationResult.create(
                TEST_CALCULATION_ID,
                new BigDecimal("50000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("40000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("3000.00"),
                new BigDecimal("2000.00")
        );
    }

    private CalculationResponse createHmrcCalculationResponse() {
        return new CalculationResponse(
                TEST_CALCULATION_ID,
                LocalDateTime.now(),
                "customerRequest",
                new BigDecimal("15000.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("40000.00"),
                new CalculationResponse.IncomeTaxBreakdown(
                        new BigDecimal("10000.00"),
                        new BigDecimal("10000.00")
                ),
                new CalculationResponse.NationalInsuranceBreakdown(
                        new CalculationResponse.NationalInsuranceBreakdown.Class2Nics(
                                new BigDecimal("3000.00")
                        ),
                        new CalculationResponse.NationalInsuranceBreakdown.Class4Nics(
                                new BigDecimal("40000.00"),
                                new BigDecimal("2000.00")
                        )
                )
        );
    }
}
