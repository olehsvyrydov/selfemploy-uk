package uk.selfemploy.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.selfemploy.common.domain.AnnualSubmissionSaga;
import uk.selfemploy.common.domain.AnnualSubmissionState;
import uk.selfemploy.common.domain.TaxCalculationResult;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.auth.TokenException;
import uk.selfemploy.core.auth.TokenProvider;
import uk.selfemploy.core.exception.ValidationException;
import uk.selfemploy.hmrc.client.SelfAssessmentCalculationClient;
import uk.selfemploy.hmrc.client.SelfAssessmentDeclarationClient;
import uk.selfemploy.hmrc.client.dto.*;
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
import static org.mockito.Mockito.*;

/**
 * TDD tests for AnnualSubmissionService.
 *
 * <p>Tests cover:
 * - State transitions (INITIATED → CALCULATING → CALCULATED → DECLARING → COMPLETED)
 * - Resume capability from each non-terminal state
 * - Error handling and compensation
 * - Validation
 */
@DisplayName("AnnualSubmissionService")
class AnnualSubmissionServiceTest {

    private AnnualSubmissionService service;
    private AnnualSubmissionSagaRepository repository;
    private SelfAssessmentCalculationClient calculationClient;
    private SelfAssessmentDeclarationClient declarationClient;
    private HmrcResilienceDecorator resilienceDecorator;
    private TokenProvider tokenProvider;

    private static final String TEST_NINO = "AA123456A";
    private static final TaxYear TEST_TAX_YEAR = TaxYear.of(2024);
    private static final String TEST_CALCULATION_ID = "calc-12345";
    private static final String TEST_CHARGE_REFERENCE = "XA123456789012";
    private static final String TEST_BEARER_TOKEN = "Bearer test-access-token";

    @BeforeEach
    void setUp() {
        repository = mock(AnnualSubmissionSagaRepository.class);
        calculationClient = mock(SelfAssessmentCalculationClient.class);
        declarationClient = mock(SelfAssessmentDeclarationClient.class);
        resilienceDecorator = mock(HmrcResilienceDecorator.class);
        tokenProvider = mock(TokenProvider.class);

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

    @Nested
    @DisplayName("startOrResume")
    class StartOrResumeTests {

        @Test
        @DisplayName("should create new saga when none exists")
        void shouldCreateNewSaga() {
            // Given
            when(repository.findByNinoAndTaxYear(TEST_NINO, TEST_TAX_YEAR.startYear()))
                .thenReturn(Optional.empty());

            // When
            AnnualSubmissionSaga saga = service.startOrResume(TEST_TAX_YEAR, TEST_NINO);

            // Then
            assertThat(saga).isNotNull();
            assertThat(saga.nino()).isEqualTo(TEST_NINO);
            assertThat(saga.taxYear()).isEqualTo(TEST_TAX_YEAR);
            assertThat(saga.state()).isEqualTo(AnnualSubmissionState.INITIATED);

            verify(repository).save(saga);
        }

        @Test
        @DisplayName("should resume existing saga in non-terminal state")
        void shouldResumeExistingSaga() {
            // Given
            AnnualSubmissionSaga existingSaga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                .withCalculating(TEST_CALCULATION_ID);

            when(repository.findByNinoAndTaxYear(TEST_NINO, TEST_TAX_YEAR.startYear()))
                .thenReturn(Optional.of(existingSaga));

            // When
            AnnualSubmissionSaga saga = service.startOrResume(TEST_TAX_YEAR, TEST_NINO);

            // Then
            assertThat(saga).isEqualTo(existingSaga);
            assertThat(saga.state()).isEqualTo(AnnualSubmissionState.CALCULATING);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception for completed saga")
        void shouldThrowExceptionForCompletedSaga() {
            // Given
            AnnualSubmissionSaga completedSaga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                .withCalculating(TEST_CALCULATION_ID)
                .withCalculated(createTestCalculationResult())
                .withDeclaring()
                .withCompleted(TEST_CHARGE_REFERENCE);

            when(repository.findByNinoAndTaxYear(TEST_NINO, TEST_TAX_YEAR.startYear()))
                .thenReturn(Optional.of(completedSaga));

            // When / Then
            assertThatThrownBy(() -> service.startOrResume(TEST_TAX_YEAR, TEST_NINO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already completed");
        }

        @Test
        @DisplayName("should validate inputs")
        void shouldValidateInputs() {
            assertThatThrownBy(() -> service.startOrResume(null, TEST_NINO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("taxYear");

            assertThatThrownBy(() -> service.startOrResume(TEST_TAX_YEAR, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("nino");

            assertThatThrownBy(() -> service.startOrResume(TEST_TAX_YEAR, ""))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("nino");
        }
    }

    @Nested
    @DisplayName("executeNextStep")
    class ExecuteNextStepTests {

        @Test
        @DisplayName("should trigger calculation from INITIATED state")
        void shouldTriggerCalculationFromInitiated() {
            // Given
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO);
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            TriggerCalculationResponse hmrcResponse = new TriggerCalculationResponse(TEST_CALCULATION_ID);
            when(calculationClient.triggerCalculation(
                eq(TEST_NINO),
                eq("2024-25"),
                anyString(),
                any(TriggerCalculationRequest.class)
            )).thenReturn(hmrcResponse);

            // When
            AnnualSubmissionSaga result = service.executeNextStep(saga.id());

            // Then
            assertThat(result.state()).isEqualTo(AnnualSubmissionState.CALCULATING);
            assertThat(result.calculationId()).isEqualTo(TEST_CALCULATION_ID);

            ArgumentCaptor<AnnualSubmissionSaga> captor = ArgumentCaptor.forClass(AnnualSubmissionSaga.class);
            verify(repository, times(2)).save(captor.capture());

            // First save: transition to CALCULATING
            assertThat(captor.getAllValues().get(0).state()).isEqualTo(AnnualSubmissionState.CALCULATING);
        }

        @Test
        @DisplayName("should retrieve calculation from CALCULATING state")
        void shouldRetrieveCalculationFromCalculating() {
            // Given
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                .withCalculating(TEST_CALCULATION_ID);
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            CalculationResponse hmrcResponse = createHmrcCalculationResponse();
            when(calculationClient.getCalculation(
                eq(TEST_NINO),
                eq("2024-25"),
                eq(TEST_CALCULATION_ID),
                anyString()
            )).thenReturn(hmrcResponse);

            // When
            AnnualSubmissionSaga result = service.executeNextStep(saga.id());

            // Then
            assertThat(result.state()).isEqualTo(AnnualSubmissionState.CALCULATED);
            assertThat(result.calculationResult()).isNotNull();
            assertThat(result.calculationResult().calculationId()).isEqualTo(TEST_CALCULATION_ID);
            assertThat(result.calculationResult().totalTaxLiability()).isEqualByComparingTo("15000.00");

            verify(repository, times(1)).save(any(AnnualSubmissionSaga.class));
        }

        @Test
        @DisplayName("should not execute from CALCULATED state without confirmation")
        void shouldNotExecuteFromCalculatedWithoutConfirmation() {
            // Given
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                .withCalculating(TEST_CALCULATION_ID)
                .withCalculated(createTestCalculationResult());
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            // When / Then
            assertThatThrownBy(() -> service.executeNextStep(saga.id()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User must confirm");
        }

        @Test
        @DisplayName("should transition to FAILED on HMRC validation error")
        void shouldTransitionToFailedOnValidationError() {
            // Given
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO);
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            HmrcValidationException hmrcError = new HmrcValidationException(
                "Invalid National Insurance Number", "INVALID_NINO", 400
            );
            when(calculationClient.triggerCalculation(any(), any(), any(), any()))
                .thenThrow(hmrcError);

            // When / Then
            assertThatThrownBy(() -> service.executeNextStep(saga.id()))
                .isInstanceOf(HmrcValidationException.class);

            ArgumentCaptor<AnnualSubmissionSaga> captor = ArgumentCaptor.forClass(AnnualSubmissionSaga.class);
            verify(repository, atLeastOnce()).save(captor.capture());

            AnnualSubmissionSaga savedSaga = captor.getValue();
            assertThat(savedSaga.state()).isEqualTo(AnnualSubmissionState.FAILED);
            assertThat(savedSaga.errorMessage()).contains("INVALID_NINO");
        }

        @Test
        @DisplayName("should persist state before API call for resume capability")
        void shouldPersistStateBeforeApiCall() {
            // Given
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO);
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            // Simulate HMRC API throwing exception
            when(calculationClient.triggerCalculation(any(), any(), any(), any()))
                .thenThrow(new HmrcServerException("Service unavailable", "SERVER_ERROR", 503));

            // When / Then
            assertThatThrownBy(() -> service.executeNextStep(saga.id()))
                .isInstanceOf(HmrcServerException.class);

            // Verify state was persisted BEFORE the API call
            ArgumentCaptor<AnnualSubmissionSaga> captor = ArgumentCaptor.forClass(AnnualSubmissionSaga.class);
            verify(repository, atLeastOnce()).save(captor.capture());

            // State should be FAILED due to server error
            assertThat(captor.getValue().state()).isEqualTo(AnnualSubmissionState.FAILED);
        }
    }

    @Nested
    @DisplayName("confirmDeclaration")
    class ConfirmDeclarationTests {

        @Test
        @DisplayName("should submit declaration from CALCULATED state")
        void shouldSubmitDeclarationFromCalculated() {
            // Given
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                .withCalculating(TEST_CALCULATION_ID)
                .withCalculated(createTestCalculationResult());
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            FinalDeclarationResponse hmrcResponse = new FinalDeclarationResponse(
                TEST_CHARGE_REFERENCE,
                LocalDateTime.now()
            );
            when(declarationClient.submitDeclaration(
                eq(TEST_NINO),
                eq("2024-25"),
                anyString(),
                any(FinalDeclarationRequest.class)
            )).thenReturn(hmrcResponse);

            // When
            AnnualSubmissionSaga result = service.confirmDeclaration(saga.id());

            // Then
            assertThat(result.state()).isEqualTo(AnnualSubmissionState.COMPLETED);
            assertThat(result.hmrcConfirmation()).isEqualTo(TEST_CHARGE_REFERENCE);

            ArgumentCaptor<AnnualSubmissionSaga> captor = ArgumentCaptor.forClass(AnnualSubmissionSaga.class);
            verify(repository, times(2)).save(captor.capture());

            // First save: transition to DECLARING
            assertThat(captor.getAllValues().get(0).state()).isEqualTo(AnnualSubmissionState.DECLARING);
            // Second save: transition to COMPLETED
            assertThat(captor.getAllValues().get(1).state()).isEqualTo(AnnualSubmissionState.COMPLETED);
        }

        @Test
        @DisplayName("should throw exception if not in CALCULATED state")
        void shouldThrowExceptionIfNotCalculated() {
            // Given
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO);
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            // When / Then
            assertThatThrownBy(() -> service.confirmDeclaration(saga.id()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CALCULATED");
        }

        @Test
        @DisplayName("should validate saga exists")
        void shouldValidateSagaExists() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.confirmDeclaration(nonExistentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Saga not found");
        }
    }

    @Nested
    @DisplayName("getCalculationResult")
    class GetCalculationResultTests {

        @Test
        @DisplayName("should return calculation result when available")
        void shouldReturnCalculationResult() {
            // Given
            TaxCalculationResult calculationResult = createTestCalculationResult();
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                .withCalculating(TEST_CALCULATION_ID)
                .withCalculated(calculationResult);
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            // When
            Optional<TaxCalculationResult> result = service.getCalculationResult(saga.id());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(calculationResult);
        }

        @Test
        @DisplayName("should return empty when calculation not yet complete")
        void shouldReturnEmptyWhenNotCalculated() {
            // Given
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO);
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            // When
            Optional<TaxCalculationResult> result = service.getCalculationResult(saga.id());

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getSagaState")
    class GetSagaStateTests {

        @Test
        @DisplayName("should return saga state when exists")
        void shouldReturnSagaState() {
            // Given
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO);
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            // When
            Optional<AnnualSubmissionSaga> result = service.getSagaState(saga.id());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(saga);
        }

        @Test
        @DisplayName("should return empty when saga not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When
            Optional<AnnualSubmissionSaga> result = service.getSagaState(nonExistentId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Resume Capability")
    class ResumeCapabilityTests {

        @Test
        @DisplayName("should resume from CALCULATING state after network failure")
        void shouldResumeFromCalculating() {
            // Given - Saga in CALCULATING state (calculation was triggered but result not yet retrieved)
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                .withCalculating(TEST_CALCULATION_ID);
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            CalculationResponse hmrcResponse = createHmrcCalculationResponse();
            when(calculationClient.getCalculation(
                eq(TEST_NINO),
                eq("2024-25"),
                eq(TEST_CALCULATION_ID),
                anyString()
            )).thenReturn(hmrcResponse);

            // When - Resume by calling executeNextStep
            AnnualSubmissionSaga result = service.executeNextStep(saga.id());

            // Then - Should continue from CALCULATING → CALCULATED
            assertThat(result.state()).isEqualTo(AnnualSubmissionState.CALCULATED);
            assertThat(result.calculationResult()).isNotNull();
        }

        @Test
        @DisplayName("should resume from DECLARING state after network failure")
        void shouldResumeFromDeclaring() {
            // Given - Saga in DECLARING state (declaration was being submitted)
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                .withCalculating(TEST_CALCULATION_ID)
                .withCalculated(createTestCalculationResult())
                .withDeclaring();
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            FinalDeclarationResponse hmrcResponse = new FinalDeclarationResponse(
                TEST_CHARGE_REFERENCE,
                LocalDateTime.now()
            );
            when(declarationClient.submitDeclaration(any(), any(), any(), any()))
                .thenReturn(hmrcResponse);

            // When - Resume by calling executeNextStep
            AnnualSubmissionSaga result = service.executeNextStep(saga.id());

            // Then - Should complete the declaration
            assertThat(result.state()).isEqualTo(AnnualSubmissionState.COMPLETED);
            assertThat(result.hmrcConfirmation()).isEqualTo(TEST_CHARGE_REFERENCE);
        }
    }

    @Nested
    @DisplayName("Token Integration (TD-001)")
    class TokenIntegrationTests {

        @Test
        @DisplayName("should use TokenProvider for HMRC API calls")
        void shouldUseTokenProviderForApiCalls() {
            // Given
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO);
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            TriggerCalculationResponse hmrcResponse = new TriggerCalculationResponse(TEST_CALCULATION_ID);
            when(calculationClient.triggerCalculation(
                eq(TEST_NINO),
                eq("2024-25"),
                eq(TEST_BEARER_TOKEN),
                any(TriggerCalculationRequest.class)
            )).thenReturn(hmrcResponse);

            // When
            service.executeNextStep(saga.id());

            // Then
            verify(tokenProvider).getValidToken();
            verify(calculationClient).triggerCalculation(
                eq(TEST_NINO),
                eq("2024-25"),
                eq(TEST_BEARER_TOKEN),
                any(TriggerCalculationRequest.class)
            );
        }

        @Test
        @DisplayName("should propagate TokenException when no token available")
        void shouldPropagateTokenExceptionWhenNoToken() {
            // Given
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO);
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));
            when(tokenProvider.getValidToken())
                .thenThrow(new TokenException(TokenException.TokenError.NO_TOKEN));

            // When / Then
            assertThatThrownBy(() -> service.executeNextStep(saga.id()))
                .isInstanceOf(TokenException.class)
                .satisfies(ex -> {
                    TokenException tokenEx = (TokenException) ex;
                    assertThat(tokenEx.getError()).isEqualTo(TokenException.TokenError.NO_TOKEN);
                    assertThat(tokenEx.requiresReauthentication()).isTrue();
                });

            // Verify HMRC API was NOT called
            verify(calculationClient, never()).triggerCalculation(any(), any(), any(), any());
        }

        @Test
        @DisplayName("should propagate TokenException when refresh fails")
        void shouldPropagateTokenExceptionWhenRefreshFails() {
            // Given
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO);
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));
            when(tokenProvider.getValidToken())
                .thenThrow(new TokenException(TokenException.TokenError.REFRESH_FAILED));

            // When / Then
            assertThatThrownBy(() -> service.executeNextStep(saga.id()))
                .isInstanceOf(TokenException.class)
                .satisfies(ex -> {
                    TokenException tokenEx = (TokenException) ex;
                    assertThat(tokenEx.getError()).isEqualTo(TokenException.TokenError.REFRESH_FAILED);
                    assertThat(tokenEx.getUserMessage()).contains("re-authenticate");
                });
        }

        @Test
        @DisplayName("should use token for all HMRC API calls in saga")
        void shouldUseTokenForAllApiCalls() {
            // Given - saga in CALCULATING state
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                .withCalculating(TEST_CALCULATION_ID);
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            CalculationResponse hmrcResponse = createHmrcCalculationResponse();
            when(calculationClient.getCalculation(any(), any(), any(), eq(TEST_BEARER_TOKEN)))
                .thenReturn(hmrcResponse);

            // When
            service.executeNextStep(saga.id());

            // Then
            verify(tokenProvider).getValidToken();
            verify(calculationClient).getCalculation(
                eq(TEST_NINO),
                eq("2024-25"),
                eq(TEST_CALCULATION_ID),
                eq(TEST_BEARER_TOKEN)
            );
        }

        @Test
        @DisplayName("should use token for declaration submission")
        void shouldUseTokenForDeclaration() {
            // Given
            AnnualSubmissionSaga saga = AnnualSubmissionSaga.create(TEST_TAX_YEAR, TEST_NINO)
                .withCalculating(TEST_CALCULATION_ID)
                .withCalculated(createTestCalculationResult());
            when(repository.findById(saga.id())).thenReturn(Optional.of(saga));

            FinalDeclarationResponse hmrcResponse = new FinalDeclarationResponse(
                TEST_CHARGE_REFERENCE,
                LocalDateTime.now()
            );
            when(declarationClient.submitDeclaration(any(), any(), eq(TEST_BEARER_TOKEN), any()))
                .thenReturn(hmrcResponse);

            // When
            service.confirmDeclaration(saga.id());

            // Then
            verify(tokenProvider).getValidToken();
            verify(declarationClient).submitDeclaration(
                eq(TEST_NINO),
                eq("2024-25"),
                eq(TEST_BEARER_TOKEN),
                any(FinalDeclarationRequest.class)
            );
        }
    }

    // Helper methods

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
