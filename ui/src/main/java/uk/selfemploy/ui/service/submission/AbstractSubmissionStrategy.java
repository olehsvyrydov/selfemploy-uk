package uk.selfemploy.ui.service.submission;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.ui.viewmodel.CategorySummary;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Abstract base class for HMRC submission strategies.
 *
 * <p>Provides common functionality for expense category mapping and JSON serialization.
 * Subclasses implement the version-specific endpoint building and DTO construction.</p>
 *
 * <h3>Common functionality provided:</h3>
 * <ul>
 *   <li>Jackson ObjectMapper configuration for HMRC date formats</li>
 *   <li>Expense category amount extraction</li>
 *   <li>Category aggregation (e.g., Travel + Travel Mileage)</li>
 *   <li>Base URL construction</li>
 * </ul>
 *
 * @see PeriodSubmissionStrategy
 * @see CumulativeSubmissionStrategy
 */
public abstract class AbstractSubmissionStrategy implements SubmissionStrategy {

    protected final ObjectMapper objectMapper;

    /**
     * The first tax year this strategy supports (inclusive).
     * For example, 2017 means tax year 2017-18 onwards.
     */
    protected final int minTaxYear;

    /**
     * The last tax year this strategy supports (inclusive).
     * For example, 2024 means up to and including tax year 2024-25.
     * Use Integer.MAX_VALUE for "no upper limit".
     */
    protected final int maxTaxYear;

    /**
     * Creates a new strategy with the given tax year range.
     *
     * @param minTaxYear first supported tax year (start year, e.g., 2017 for 2017-18)
     * @param maxTaxYear last supported tax year (start year, e.g., 2024 for 2024-25)
     */
    protected AbstractSubmissionStrategy(int minTaxYear, int maxTaxYear) {
        this.minTaxYear = minTaxYear;
        this.maxTaxYear = maxTaxYear;
        this.objectMapper = createObjectMapper();
    }

    /**
     * Creates and configures the Jackson ObjectMapper for HMRC API serialization.
     *
     * @return configured ObjectMapper
     */
    protected ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Override
    public boolean supports(TaxYear taxYear) {
        if (taxYear == null) {
            // Subclasses can override this default behavior
            return isDefaultStrategy();
        }
        int startYear = taxYear.startYear();
        return startYear >= minTaxYear && startYear <= maxTaxYear;
    }

    /**
     * Returns whether this strategy should be used as the default when tax year is null.
     * Override in subclass to change default behavior.
     *
     * @return true if this is the default strategy
     */
    protected boolean isDefaultStrategy() {
        return false;
    }

    /**
     * Builds the base path for the Self-Employment API.
     *
     * @param baseUrl the HMRC API base URL
     * @param nino the National Insurance Number
     * @param businessId the HMRC business ID
     * @return the base path without endpoint-specific suffix
     */
    protected String buildBasePath(String baseUrl, String nino, String businessId) {
        return baseUrl + "/individuals/business/self-employment/" + nino + "/" + businessId;
    }

    // ==================== Expense Category Helpers ====================

    /**
     * Gets the amount for a single expense category, defaulting to zero if not present.
     *
     * @param expenses the expense category map
     * @param category the category to look up
     * @return the category amount or BigDecimal.ZERO
     */
    protected BigDecimal getCategoryAmount(Map<ExpenseCategory, CategorySummary> expenses, ExpenseCategory category) {
        if (expenses == null) {
            return BigDecimal.ZERO;
        }
        CategorySummary summary = expenses.get(category);
        return summary != null ? summary.amount() : BigDecimal.ZERO;
    }

    /**
     * Sums amounts for multiple expense categories.
     *
     * <p>Used for aggregating related categories like Travel + Travel Mileage,
     * or Other Expenses + Home Office Simplified.</p>
     *
     * @param expenses the expense category map
     * @param categories the categories to sum
     * @return the total amount
     */
    protected BigDecimal sumCategoryAmounts(Map<ExpenseCategory, CategorySummary> expenses, ExpenseCategory... categories) {
        BigDecimal sum = BigDecimal.ZERO;
        for (ExpenseCategory category : categories) {
            sum = sum.add(getCategoryAmount(expenses, category));
        }
        return sum;
    }

    /**
     * Validates that review data is not null.
     *
     * @param reviewData the data to validate
     * @throws IllegalArgumentException if reviewData is null
     */
    protected void validateReviewData(QuarterlyReviewData reviewData) {
        if (reviewData == null) {
            throw new IllegalArgumentException("reviewData must not be null");
        }
    }

    // ==================== Expense Mapping ====================

    /**
     * Maps UI expense categories to HMRC SA103 expense fields.
     *
     * <p>This method extracts the common expense mapping logic used by both
     * {@link PeriodSubmissionStrategy} and {@link CumulativeSubmissionStrategy}.
     * The mapping includes category aggregation:</p>
     * <ul>
     *   <li>Travel + Travel Mileage → travelCosts</li>
     *   <li>Other Expenses + Home Office Simplified → other</li>
     * </ul>
     *
     * @param expenses the expense category map from QuarterlyReviewData
     * @return a MappedExpenses record with all HMRC SA103 fields
     */
    protected MappedExpenses mapExpenses(Map<ExpenseCategory, CategorySummary> expenses) {
        return new MappedExpenses(
                getCategoryAmount(expenses, ExpenseCategory.COST_OF_GOODS),
                getCategoryAmount(expenses, ExpenseCategory.SUBCONTRACTOR_COSTS),
                getCategoryAmount(expenses, ExpenseCategory.STAFF_COSTS),
                sumCategoryAmounts(expenses, ExpenseCategory.TRAVEL, ExpenseCategory.TRAVEL_MILEAGE),
                getCategoryAmount(expenses, ExpenseCategory.PREMISES),
                getCategoryAmount(expenses, ExpenseCategory.REPAIRS),
                getCategoryAmount(expenses, ExpenseCategory.OFFICE_COSTS),
                getCategoryAmount(expenses, ExpenseCategory.ADVERTISING),
                getCategoryAmount(expenses, ExpenseCategory.BUSINESS_ENTERTAINMENT),
                getCategoryAmount(expenses, ExpenseCategory.INTEREST),
                getCategoryAmount(expenses, ExpenseCategory.FINANCIAL_CHARGES),
                getCategoryAmount(expenses, ExpenseCategory.BAD_DEBTS),
                getCategoryAmount(expenses, ExpenseCategory.PROFESSIONAL_FEES),
                getCategoryAmount(expenses, ExpenseCategory.DEPRECIATION),
                sumCategoryAmounts(expenses, ExpenseCategory.OTHER_EXPENSES, ExpenseCategory.HOME_OFFICE_SIMPLIFIED)
        );
    }

    /**
     * Intermediate record holding mapped expense amounts for HMRC SA103 fields.
     *
     * <p>This record is used to transfer mapped expense data between the base class
     * and strategy subclasses, avoiding code duplication in expense mapping logic.</p>
     *
     * <p>Created per Rev's code review suggestion to extract shared mapping logic
     * from PeriodSubmissionStrategy and CumulativeSubmissionStrategy.</p>
     *
     * @param costOfGoodsBought SA103F Box 17
     * @param cisPaymentsToSubcontractors SA103F Box 18
     * @param staffCosts SA103F Box 19
     * @param travelCosts SA103F Box 20 (Travel + Travel Mileage combined)
     * @param premisesRunningCosts SA103F Box 21
     * @param maintenanceCosts SA103F Box 22
     * @param adminCosts SA103F Box 23
     * @param advertisingCosts SA103F Box 24
     * @param businessEntertainmentCosts Not allowable
     * @param interest SA103F Box 25
     * @param financialCharges SA103F Box 26
     * @param badDebt SA103F Box 27
     * @param professionalFees SA103F Box 28
     * @param depreciation SA103F Box 29 (not allowable)
     * @param other SA103F Box 30 (Other Expenses + Home Office Simplified combined)
     */
    public record MappedExpenses(
            BigDecimal costOfGoodsBought,
            BigDecimal cisPaymentsToSubcontractors,
            BigDecimal staffCosts,
            BigDecimal travelCosts,
            BigDecimal premisesRunningCosts,
            BigDecimal maintenanceCosts,
            BigDecimal adminCosts,
            BigDecimal advertisingCosts,
            BigDecimal businessEntertainmentCosts,
            BigDecimal interest,
            BigDecimal financialCharges,
            BigDecimal badDebt,
            BigDecimal professionalFees,
            BigDecimal depreciation,
            BigDecimal other
    ) {}
}
