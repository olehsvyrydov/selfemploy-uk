package uk.selfemploy.ui.service.submission;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.dto.DisallowableExpenses;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.profit.CategorySpend;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

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
     * Gets both columns for a single expense category, defaulting to zero if not present.
     *
     * @param expenses the two-column breakdown for the period
     * @param category the category to look up
     * @return what was spent in the category and how much of it may be claimed
     */
    protected CategorySpend getCategorySpend(Map<ExpenseCategory, CategorySpend> expenses,
                                             ExpenseCategory category) {
        if (expenses == null) {
            return CategorySpend.ZERO;
        }
        CategorySpend spend = expenses.get(category);
        return spend != null ? spend : CategorySpend.ZERO;
    }

    /**
     * Sums both columns across several categories that share one SA103 box.
     *
     * <p>Used for aggregating related categories like Travel + Travel Mileage,
     * or Other Expenses + Home Office Simplified. The two columns are summed together so a box's
     * declared spend and its disallowable part are always derived from the same set of records.</p>
     *
     * @param expenses the two-column breakdown for the period
     * @param categories the categories to sum
     * @return the combined spend and claim for the box
     */
    protected CategorySpend sumCategorySpend(Map<ExpenseCategory, CategorySpend> expenses,
                                             ExpenseCategory... categories) {
        CategorySpend sum = CategorySpend.ZERO;
        for (ExpenseCategory category : categories) {
            CategorySpend spend = getCategorySpend(expenses, category);
            sum = sum.plus(spend.spent(), spend.claimable());
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
     * Maps UI expense categories to the SA103 boxes, keeping both columns per box.
     *
     * <p>This method extracts the common expense mapping logic used by both
     * {@link PeriodSubmissionStrategy} and {@link CumulativeSubmissionStrategy}.
     * The mapping includes category aggregation:</p>
     * <ul>
     *   <li>Travel + Travel Mileage → travelCosts</li>
     *   <li>Other Expenses + Home Office Simplified → other</li>
     * </ul>
     *
     * <p>Fed from {@link QuarterlyReviewData#getSpendByCategory()}, which is unfiltered: a category
     * HMRC disallows reaches this map with its spend intact and nothing claimable, which is what a
     * return has to report. Filtering it here would file that spend as £0.00.</p>
     *
     * @param expenses the two-column breakdown from QuarterlyReviewData
     * @return a MappedExpenses record with both columns of every SA103 box
     */
    protected MappedExpenses mapExpenses(Map<ExpenseCategory, CategorySpend> expenses) {
        return new MappedExpenses(
                getCategorySpend(expenses, ExpenseCategory.COST_OF_GOODS),
                getCategorySpend(expenses, ExpenseCategory.SUBCONTRACTOR_COSTS),
                getCategorySpend(expenses, ExpenseCategory.STAFF_COSTS),
                sumCategorySpend(expenses, ExpenseCategory.TRAVEL, ExpenseCategory.TRAVEL_MILEAGE),
                getCategorySpend(expenses, ExpenseCategory.PREMISES),
                getCategorySpend(expenses, ExpenseCategory.REPAIRS),
                getCategorySpend(expenses, ExpenseCategory.OFFICE_COSTS),
                getCategorySpend(expenses, ExpenseCategory.ADVERTISING),
                getCategorySpend(expenses, ExpenseCategory.BUSINESS_ENTERTAINMENT),
                getCategorySpend(expenses, ExpenseCategory.INTEREST),
                getCategorySpend(expenses, ExpenseCategory.FINANCIAL_CHARGES),
                getCategorySpend(expenses, ExpenseCategory.BAD_DEBTS),
                getCategorySpend(expenses, ExpenseCategory.PROFESSIONAL_FEES),
                getCategorySpend(expenses, ExpenseCategory.DEPRECIATION),
                sumCategorySpend(expenses, ExpenseCategory.OTHER_EXPENSES, ExpenseCategory.HOME_OFFICE_SIMPLIFIED)
        );
    }

    /**
     * The disallowable column of the payload, built from the same mapping as the spend column.
     *
     * <p>Each figure is {@link CategorySpend#disallowable()} for that box, so the two columns are
     * derived from one set of records and cannot disagree. A payload with a spend column and no
     * such block declares the whole spend as deductible.</p>
     *
     * @param mapped the two-column mapping from {@link #mapExpenses(Map)}
     * @return the periodDisallowableExpenses block
     */
    protected DisallowableExpenses disallowableExpenses(MappedExpenses mapped) {
        return DisallowableExpenses.builder()
                .costOfGoods(mapped.costOfGoodsBought().disallowable())
                .cisPaymentsToSubcontractors(mapped.cisPaymentsToSubcontractors().disallowable())
                .staffCosts(mapped.staffCosts().disallowable())
                .travelCosts(mapped.travelCosts().disallowable())
                .premisesRunningCosts(mapped.premisesRunningCosts().disallowable())
                .maintenanceCosts(mapped.maintenanceCosts().disallowable())
                .adminCosts(mapped.adminCosts().disallowable())
                .advertisingCosts(mapped.advertisingCosts().disallowable())
                .businessEntertainmentCosts(mapped.businessEntertainmentCosts().disallowable())
                .interest(mapped.interest().disallowable())
                .financialCharges(mapped.financialCharges().disallowable())
                .badDebt(mapped.badDebt().disallowable())
                .professionalFees(mapped.professionalFees().disallowable())
                .depreciation(mapped.depreciation().disallowable())
                .other(mapped.other().disallowable())
                .build();
    }

    /**
     * Intermediate record holding both SA103 columns per box.
     *
     * <p>This record is used to transfer mapped expense data between the base class
     * and strategy subclasses, avoiding code duplication in expense mapping logic. Each component
     * carries what was spent in that box and how much of it may be claimed, because the payload
     * declares both and deriving them separately is how they came to disagree.</p>
     *
     * @param costOfGoodsBought SA103F Box 17
     * @param cisPaymentsToSubcontractors SA103F Box 18
     * @param staffCosts SA103F Box 19
     * @param travelCosts SA103F Box 20 (Travel + Travel Mileage combined)
     * @param premisesRunningCosts SA103F Box 21
     * @param maintenanceCosts SA103F Box 22
     * @param adminCosts SA103F Box 23
     * @param advertisingCosts SA103F Box 24
     * @param businessEntertainmentCosts disallowable in full
     * @param interest SA103F Box 25
     * @param financialCharges SA103F Box 26
     * @param badDebt SA103F Box 27
     * @param professionalFees SA103F Box 28
     * @param depreciation SA103F Box 29 (disallowable in full)
     * @param other SA103F Box 30 (Other Expenses + Home Office Simplified combined)
     */
    public record MappedExpenses(
            CategorySpend costOfGoodsBought,
            CategorySpend cisPaymentsToSubcontractors,
            CategorySpend staffCosts,
            CategorySpend travelCosts,
            CategorySpend premisesRunningCosts,
            CategorySpend maintenanceCosts,
            CategorySpend adminCosts,
            CategorySpend advertisingCosts,
            CategorySpend businessEntertainmentCosts,
            CategorySpend interest,
            CategorySpend financialCharges,
            CategorySpend badDebt,
            CategorySpend professionalFees,
            CategorySpend depreciation,
            CategorySpend other
    ) {}
}
