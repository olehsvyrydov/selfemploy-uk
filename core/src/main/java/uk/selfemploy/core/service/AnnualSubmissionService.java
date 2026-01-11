package uk.selfemploy.core.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.common.domain.AnnualSubmissionSaga;
import uk.selfemploy.common.domain.AnnualSubmissionState;
import uk.selfemploy.common.domain.TaxCalculationResult;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.auth.TokenProvider;
import uk.selfemploy.core.exception.ValidationException;
import uk.selfemploy.hmrc.client.SelfAssessmentCalculationClient;
import uk.selfemploy.hmrc.client.SelfAssessmentDeclarationClient;
import uk.selfemploy.hmrc.client.dto.*;
import uk.selfemploy.hmrc.exception.HmrcApiException;
import uk.selfemploy.hmrc.resilience.HmrcResilienceDecorator;
import uk.selfemploy.persistence.repository.AnnualSubmissionSagaRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing Annual Self Assessment submissions using Saga pattern.
 *
 * <p>Implements a multi-step submission process with resume capability:
 * <pre>
 * INITIATED → CALCULATING → CALCULATED → DECLARING → COMPLETED
 * </pre>
 *
 * <p>Each state transition is persisted before making HMRC API calls,
 * enabling resume from the last successful state after failures.
 *
 * <p>Uses HmrcResilienceDecorator for retry and circuit breaker capabilities.
 */
@ApplicationScoped
public class AnnualSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(AnnualSubmissionService.class);

    private final AnnualSubmissionSagaRepository repository;
    private final SelfAssessmentCalculationClient calculationClient;
    private final SelfAssessmentDeclarationClient declarationClient;
    private final HmrcResilienceDecorator resilienceDecorator;
    private final TokenProvider tokenProvider;

    @Inject
    public AnnualSubmissionService(
            AnnualSubmissionSagaRepository repository,
            @RestClient SelfAssessmentCalculationClient calculationClient,
            @RestClient SelfAssessmentDeclarationClient declarationClient,
            HmrcResilienceDecorator resilienceDecorator,
            TokenProvider tokenProvider) {
        this.repository = repository;
        this.calculationClient = calculationClient;
        this.declarationClient = declarationClient;
        this.resilienceDecorator = resilienceDecorator;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Starts a new annual submission or resumes an existing one.
     *
     * <p>If a saga already exists for the given tax year and NINO:
     * - If completed, throws exception
     * - If failed or in-progress, returns existing saga for resume
     *
     * @param taxYear The tax year for the submission
     * @param nino National Insurance Number
     * @return The saga (new or existing)
     * @throws ValidationException if inputs are invalid
     * @throws IllegalStateException if submission already completed
     */
    public AnnualSubmissionSaga startOrResume(TaxYear taxYear, String nino) {
        validateInputs(taxYear, nino);

        log.info("Starting or resuming annual submission for NINO {}**** tax year {}",
                nino.substring(0, 2), taxYear.label());

        Optional<AnnualSubmissionSaga> existing = repository.findByNinoAndTaxYear(
                nino, taxYear.startYear());

        if (existing.isPresent()) {
            AnnualSubmissionSaga saga = existing.get();
            if (saga.state() == AnnualSubmissionState.COMPLETED) {
                throw new IllegalStateException(
                        "Annual submission already completed for tax year " + taxYear.label());
            }
            log.info("Resuming existing saga {} in state {}", saga.id(), saga.state());
            return saga;
        }

        // Create new saga
        AnnualSubmissionSaga newSaga = AnnualSubmissionSaga.create(taxYear, nino);
        repository.save(newSaga);

        log.info("Created new annual submission saga {} for tax year {}", newSaga.id(), taxYear.label());
        return newSaga;
    }

    /**
     * Executes the next step in the saga based on current state.
     *
     * <p>State-specific actions:
     * - INITIATED: Trigger tax calculation
     * - CALCULATING: Retrieve calculation result
     * - CALCULATED: Throw exception (user must explicitly confirm)
     * - DECLARING: Complete declaration (resume from network failure)
     * - COMPLETED/FAILED: No action
     *
     * @param sagaId The saga ID
     * @return Updated saga after step execution
     * @throws IllegalArgumentException if saga not found
     * @throws IllegalStateException if in CALCULATED state (requires confirmDeclaration)
     */
    public AnnualSubmissionSaga executeNextStep(UUID sagaId) {
        AnnualSubmissionSaga saga = getSagaOrThrow(sagaId);

        log.info("Executing next step for saga {} in state {}", sagaId, saga.state());

        try {
            return switch (saga.state()) {
                case INITIATED -> triggerCalculation(saga);
                case CALCULATING -> retrieveCalculation(saga);
                case CALCULATED -> throw new IllegalStateException(
                        "User must confirm declaration before proceeding. Call confirmDeclaration().");
                case DECLARING -> completeDeclaration(saga);
                case COMPLETED, FAILED -> {
                    log.warn("Saga {} is in terminal state {}, no action taken", sagaId, saga.state());
                    yield saga;
                }
            };
        } catch (HmrcApiException e) {
            log.error("HMRC API error during saga execution: {}", e.getMessage());
            // Fetch latest state from repository in case it was updated during the step
            AnnualSubmissionSaga latestSaga = repository.findById(sagaId).orElse(saga);
            String errorMessage = e.getErrorCode() != null ?
                    e.getErrorCode() + ": " + e.getMessage() : e.getMessage();
            AnnualSubmissionSaga failedSaga = latestSaga.withFailed(errorMessage);
            repository.save(failedSaga);
            throw e;
        }
    }

    /**
     * Confirms and submits the final declaration after user review.
     *
     * <p>Can only be called when saga is in CALCULATED state.
     * Transitions through DECLARING → COMPLETED.
     *
     * @param sagaId The saga ID
     * @return Completed saga with HMRC confirmation
     * @throws IllegalArgumentException if saga not found
     * @throws IllegalStateException if not in CALCULATED state
     */
    public AnnualSubmissionSaga confirmDeclaration(UUID sagaId) {
        AnnualSubmissionSaga saga = getSagaOrThrow(sagaId);

        if (saga.state() != AnnualSubmissionState.CALCULATED) {
            throw new IllegalStateException(
                    "Can only confirm declaration from CALCULATED state. Current state: " + saga.state());
        }

        log.info("User confirmed declaration for saga {}", sagaId);

        try {
            // Transition to DECLARING and persist
            AnnualSubmissionSaga declaringSaga = saga.withDeclaring();
            repository.save(declaringSaga);

            // Submit declaration
            return completeDeclaration(declaringSaga);

        } catch (HmrcApiException e) {
            log.error("HMRC API error during declaration: {}", e.getMessage());
            // Fetch latest state from repository in case it was updated during the step
            AnnualSubmissionSaga latestSaga = repository.findById(sagaId).orElse(saga);
            String errorMessage = e.getErrorCode() != null ?
                    e.getErrorCode() + ": " + e.getMessage() : e.getMessage();
            AnnualSubmissionSaga failedSaga = latestSaga.withFailed(errorMessage);
            repository.save(failedSaga);
            throw e;
        }
    }

    /**
     * Gets the calculation result if available.
     *
     * @param sagaId The saga ID
     * @return Optional containing result if saga is in CALCULATED or later state
     */
    public Optional<TaxCalculationResult> getCalculationResult(UUID sagaId) {
        return getSagaState(sagaId)
                .map(AnnualSubmissionSaga::calculationResult);
    }

    /**
     * Gets the current saga state.
     *
     * @param sagaId The saga ID
     * @return Optional containing the saga if found
     */
    public Optional<AnnualSubmissionSaga> getSagaState(UUID sagaId) {
        return repository.findById(sagaId);
    }

    // Private helper methods

    private AnnualSubmissionSaga triggerCalculation(AnnualSubmissionSaga saga) {
        log.info("Triggering tax calculation for saga {}", saga.id());

        // Get valid OAuth token before making API call
        String bearerToken = tokenProvider.getValidToken();
        log.debug("Retrieved valid OAuth token for HMRC API call");

        // Transition to CALCULATING and persist BEFORE API call
        AnnualSubmissionSaga calculatingSaga = saga.withCalculating("");
        repository.save(calculatingSaga);

        // Call HMRC API to trigger calculation
        TriggerCalculationResponse response = resilienceDecorator.executeWithRetry(() ->
                calculationClient.triggerCalculation(
                        saga.nino(),
                        formatTaxYear(saga.taxYear()),
                        bearerToken,
                        TriggerCalculationRequest.forAnnualSubmission()
                )
        );

        // Update with actual calculation ID
        AnnualSubmissionSaga updatedSaga = new AnnualSubmissionSaga(
                calculatingSaga.id(),
                calculatingSaga.nino(),
                calculatingSaga.taxYear(),
                AnnualSubmissionState.CALCULATING,
                response.calculationId(),
                null,
                null,
                null,
                calculatingSaga.createdAt(),
                java.time.Instant.now()
        );
        repository.save(updatedSaga);

        log.info("Calculation triggered for saga {}, calculationId: {}", saga.id(), response.calculationId());
        return updatedSaga;
    }

    private AnnualSubmissionSaga retrieveCalculation(AnnualSubmissionSaga saga) {
        log.info("Retrieving calculation for saga {}, calculationId: {}", saga.id(), saga.calculationId());

        // Get valid OAuth token before making API call
        String bearerToken = tokenProvider.getValidToken();
        log.debug("Retrieved valid OAuth token for HMRC API call");

        // Call HMRC API to get calculation
        CalculationResponse response = resilienceDecorator.executeWithRetry(() ->
                calculationClient.getCalculation(
                        saga.nino(),
                        formatTaxYear(saga.taxYear()),
                        saga.calculationId(),
                        bearerToken
                )
        );

        // Convert HMRC response to domain model
        TaxCalculationResult result = convertToCalculationResult(response);

        // Transition to CALCULATED
        AnnualSubmissionSaga calculatedSaga = saga.withCalculated(result);
        repository.save(calculatedSaga);

        log.info("Calculation retrieved for saga {}, total tax liability: {}",
                saga.id(), result.totalTaxLiability());
        return calculatedSaga;
    }

    private AnnualSubmissionSaga completeDeclaration(AnnualSubmissionSaga saga) {
        log.info("Submitting final declaration for saga {}", saga.id());

        // Get valid OAuth token before making API call
        String bearerToken = tokenProvider.getValidToken();
        log.debug("Retrieved valid OAuth token for HMRC API call");

        // Submit declaration to HMRC
        FinalDeclarationResponse response = resilienceDecorator.executeWithRetry(() ->
                declarationClient.submitDeclaration(
                        saga.nino(),
                        formatTaxYear(saga.taxYear()),
                        bearerToken,
                        new FinalDeclarationRequest(saga.calculationId())
                )
        );

        // Transition to COMPLETED
        AnnualSubmissionSaga completedSaga = saga.withCompleted(response.chargeReference());
        repository.save(completedSaga);

        log.info("Declaration completed for saga {}, charge reference: {}",
                saga.id(), response.chargeReference());
        return completedSaga;
    }

    private TaxCalculationResult convertToCalculationResult(CalculationResponse response) {
        BigDecimal incomeTax = response.incomeTax() != null ?
                response.incomeTax().totalIncomeTax() : BigDecimal.ZERO;

        BigDecimal niClass2 = response.nics() != null && response.nics().class2Nics() != null ?
                response.nics().class2Nics().amount() : BigDecimal.ZERO;

        BigDecimal niClass4 = response.nics() != null && response.nics().class4Nics() != null ?
                response.nics().class4Nics().totalClass4Nics() : BigDecimal.ZERO;

        return TaxCalculationResult.create(
                response.calculationId(),
                response.totalIncomeReceived(),
                response.totalAllowancesAndDeductions(),
                response.totalTaxableIncome(),
                incomeTax,
                niClass2,
                niClass4
        );
    }

    private String formatTaxYear(TaxYear taxYear) {
        int endYear = taxYear.startYear() + 1;
        return String.format("%d-%02d", taxYear.startYear(), endYear % 100);
    }

    private AnnualSubmissionSaga getSagaOrThrow(UUID sagaId) {
        return repository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga not found: " + sagaId));
    }

    private void validateInputs(TaxYear taxYear, String nino) {
        if (taxYear == null) {
            throw new ValidationException("taxYear", "taxYear: Tax year cannot be null");
        }
        if (nino == null || nino.isBlank()) {
            throw new ValidationException("nino", "nino: NINO cannot be null or empty");
        }
    }
}
