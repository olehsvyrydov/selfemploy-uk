package uk.selfemploy.core.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.dto.PeriodicUpdate;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;
import uk.selfemploy.core.auth.TokenProvider;
import uk.selfemploy.core.exception.SubmissionException;
import uk.selfemploy.core.exception.ValidationException;
import uk.selfemploy.hmrc.client.MtdPeriodicUpdateClient;
import uk.selfemploy.hmrc.client.dto.HmrcSubmissionResponse;
import uk.selfemploy.hmrc.exception.HmrcApiException;
import uk.selfemploy.hmrc.exception.HmrcValidationException;
import uk.selfemploy.persistence.repository.ExpenseRepository;
import uk.selfemploy.persistence.repository.IncomeRepository;
import uk.selfemploy.persistence.repository.SubmissionRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for submitting quarterly MTD updates to HMRC.
 *
 * <p>Handles the preparation and submission of quarterly income and expense
 * summaries as required by Making Tax Digital.</p>
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/self-employment-business-api">
 *     HMRC Self-Employment Business API</a>
 */
@ApplicationScoped
public class QuarterlySubmissionService {

    private static final Logger log = LoggerFactory.getLogger(QuarterlySubmissionService.class);

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final SubmissionRepository submissionRepository;
    private final MtdPeriodicUpdateClient mtdClient;
    private final TokenProvider tokenProvider;

    @Inject
    public QuarterlySubmissionService(
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            SubmissionRepository submissionRepository,
            @RestClient MtdPeriodicUpdateClient mtdClient,
            TokenProvider tokenProvider) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.submissionRepository = submissionRepository;
        this.mtdClient = mtdClient;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Submits a quarterly update to HMRC.
     *
     * <p>Calculates cumulative totals from tax year start to quarter end,
     * builds the PeriodicUpdate, submits to HMRC, and saves the submission record.</p>
     *
     * @param businessId The business ID
     * @param nino       National Insurance Number
     * @param taxYear    The tax year
     * @param quarter    The quarter to submit
     * @return The submission record with HMRC reference
     * @throws ValidationException   if validation fails
     * @throws SubmissionException   if the submission fails
     */
    public Submission submitQuarter(UUID businessId, String nino, TaxYear taxYear, Quarter quarter) {
        // Validate inputs
        validateInputs(businessId, nino, taxYear, quarter);

        // Check for duplicate submission
        if (submissionRepository.existsQuarterlySubmission(businessId, taxYear, quarter)) {
            throw new SubmissionException(String.format(
                    "%s %s has already been submitted. Use amendment flow to update.",
                    quarter, taxYear.label()));
        }

        log.info("Preparing quarterly submission for {} {} business {}",
                quarter, taxYear.label(), businessId);

        // Calculate cumulative totals from tax year start to quarter end
        LocalDate periodStart = quarter.getStartDate(taxYear);
        LocalDate periodEnd = quarter.getEndDate(taxYear);

        // Get cumulative data from tax year start
        List<Income> incomes = incomeRepository.findByDateRange(
                businessId, taxYear.startDate(), periodEnd);
        List<Expense> expenses = expenseRepository.findByDateRange(
                businessId, taxYear.startDate(), periodEnd);

        // Calculate totals
        BigDecimal totalIncome = calculateTotalIncome(incomes);
        BigDecimal totalExpenses = calculateTotalExpenses(expenses);
        BigDecimal netProfit = totalIncome.subtract(totalExpenses);

        // Build PeriodicUpdate DTO
        PeriodicUpdate periodicUpdate = buildPeriodicUpdate(
                periodStart, periodEnd, incomes, expenses);

        // Get valid OAuth token before making API call
        String bearerToken = tokenProvider.getValidToken();
        log.debug("Retrieved valid OAuth token for HMRC API call");

        // Create pending submission
        Submission submission = Submission.createQuarterly(
                businessId, taxYear, quarter, totalIncome, totalExpenses);

        try {
            // Submit to HMRC
            log.info("Submitting {} {} to HMRC for NINO {}****{}",
                    quarter, taxYear.label(),
                    nino.substring(0, 2), nino.substring(nino.length() - 1));

            HmrcSubmissionResponse response = mtdClient.submitPeriodicUpdate(
                    nino, businessId.toString(), bearerToken, periodicUpdate);

            // Update submission with success
            Submission acceptedSubmission = submission.withAccepted(response.hmrcReference());
            submissionRepository.save(acceptedSubmission);

            log.info("Successfully submitted {} {} with HMRC reference: {}",
                    quarter, taxYear.label(), response.hmrcReference());

            return acceptedSubmission;

        } catch (HmrcValidationException e) {
            // HMRC validation error - save as rejected
            log.warn("HMRC validation error for {} {}: {} - {}",
                    quarter, taxYear.label(), e.getErrorCode(), e.getMessage());

            Submission rejectedSubmission = submission.withRejected(
                    String.format("%s: %s", e.getErrorCode(), e.getMessage()));
            submissionRepository.save(rejectedSubmission);

            throw new SubmissionException("HMRC validation failed: " + e.getMessage(), e);

        } catch (HmrcApiException e) {
            // Other HMRC error - save as rejected
            log.error("HMRC API error for {} {}: {}", quarter, taxYear.label(), e.getMessage());

            Submission rejectedSubmission = submission.withRejected(e.getMessage());
            submissionRepository.save(rejectedSubmission);

            throw new SubmissionException("HMRC submission failed: " + e.getMessage(), e, e.isRetryable());
        }
    }

    private void validateInputs(UUID businessId, String nino, TaxYear taxYear, Quarter quarter) {
        if (businessId == null) {
            throw new ValidationException("businessId", "Business ID cannot be null");
        }
        if (nino == null || nino.isBlank()) {
            throw new ValidationException("nino", "NINO cannot be null or empty");
        }
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        if (quarter == null) {
            throw new ValidationException("quarter", "Quarter cannot be null");
        }
    }

    private BigDecimal calculateTotalIncome(List<Income> incomes) {
        return incomes.stream()
                .map(Income::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalExpenses(List<Expense> expenses) {
        return expenses.stream()
                .map(Expense::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PeriodicUpdate buildPeriodicUpdate(
            LocalDate periodStart,
            LocalDate periodEnd,
            List<Income> incomes,
            List<Expense> expenses) {

        // Calculate income
        BigDecimal turnover = calculateTotalIncome(incomes);
        PeriodicUpdate.PeriodIncome periodIncome = PeriodicUpdate.PeriodIncome.ofTurnover(turnover);

        // Calculate expenses by category
        Map<ExpenseCategory, BigDecimal> expensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::category,
                        Collectors.reducing(BigDecimal.ZERO, Expense::amount, BigDecimal::add)));

        PeriodicUpdate.PeriodExpenses periodExpenses = PeriodicUpdate.PeriodExpenses.builder()
                .costOfGoodsBought(expensesByCategory.getOrDefault(ExpenseCategory.COST_OF_GOODS, BigDecimal.ZERO))
                .cisPaymentsToSubcontractors(expensesByCategory.getOrDefault(ExpenseCategory.SUBCONTRACTOR_COSTS, BigDecimal.ZERO))
                .staffCosts(expensesByCategory.getOrDefault(ExpenseCategory.STAFF_COSTS, BigDecimal.ZERO))
                .travelCosts(sumCategories(expensesByCategory, ExpenseCategory.TRAVEL, ExpenseCategory.TRAVEL_MILEAGE))
                .premisesRunningCosts(expensesByCategory.getOrDefault(ExpenseCategory.PREMISES, BigDecimal.ZERO))
                .maintenanceCosts(expensesByCategory.getOrDefault(ExpenseCategory.REPAIRS, BigDecimal.ZERO))
                .adminCosts(expensesByCategory.getOrDefault(ExpenseCategory.OFFICE_COSTS, BigDecimal.ZERO))
                .advertisingCosts(expensesByCategory.getOrDefault(ExpenseCategory.ADVERTISING, BigDecimal.ZERO))
                .businessEntertainmentCosts(expensesByCategory.getOrDefault(ExpenseCategory.BUSINESS_ENTERTAINMENT, BigDecimal.ZERO))
                .interest(expensesByCategory.getOrDefault(ExpenseCategory.INTEREST, BigDecimal.ZERO))
                .financialCharges(expensesByCategory.getOrDefault(ExpenseCategory.FINANCIAL_CHARGES, BigDecimal.ZERO))
                .badDebt(expensesByCategory.getOrDefault(ExpenseCategory.BAD_DEBTS, BigDecimal.ZERO))
                .professionalFees(expensesByCategory.getOrDefault(ExpenseCategory.PROFESSIONAL_FEES, BigDecimal.ZERO))
                .depreciation(expensesByCategory.getOrDefault(ExpenseCategory.DEPRECIATION, BigDecimal.ZERO))
                .other(sumCategories(expensesByCategory, ExpenseCategory.OTHER_EXPENSES, ExpenseCategory.HOME_OFFICE_SIMPLIFIED))
                .build();

        return new PeriodicUpdate(periodStart, periodEnd, periodIncome, periodExpenses);
    }

    private BigDecimal sumCategories(Map<ExpenseCategory, BigDecimal> map, ExpenseCategory... categories) {
        BigDecimal sum = BigDecimal.ZERO;
        for (ExpenseCategory category : categories) {
            sum = sum.add(map.getOrDefault(category, BigDecimal.ZERO));
        }
        return sum;
    }
}
