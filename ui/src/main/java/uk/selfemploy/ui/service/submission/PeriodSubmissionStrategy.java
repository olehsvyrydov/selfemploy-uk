package uk.selfemploy.ui.service.submission;

import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.dto.PeriodicUpdate;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

/**
 * Submission strategy for tax years up to 2024-25.
 *
 * <p>Uses POST /period endpoint with periodDates in the request body.
 * Accept header: application/vnd.hmrc.5.0+json</p>
 *
 * <h3>Request format:</h3>
 * <pre>
 * {
 *   "periodDates": {
 *     "periodStartDate": "2024-04-06",
 *     "periodEndDate": "2024-07-05"
 *   },
 *   "periodIncome": { ... },
 *   "periodExpenses": { ... }
 * }
 * </pre>
 *
 * @see CumulativeSubmissionStrategy for tax years 2025-26+
 */
public class PeriodSubmissionStrategy extends AbstractSubmissionStrategy {

    /**
     * Tax year 2024-25 is the last year for the period endpoint.
     */
    private static final int MAX_TAX_YEAR = 2024;

    /**
     * First supported tax year (HMRC MTD started with 2017-18).
     */
    private static final int MIN_TAX_YEAR = 2017;

    public PeriodSubmissionStrategy() {
        super(MIN_TAX_YEAR, MAX_TAX_YEAR);
    }

    @Override
    public String getHttpMethod() {
        return "POST";
    }

    @Override
    public String buildEndpointUrl(String baseUrl, String nino, String businessId, TaxYear taxYear) {
        return buildBasePath(baseUrl, nino, businessId) + "/period";
    }

    @Override
    public String serializeRequest(QuarterlyReviewData reviewData) throws Exception {
        validateReviewData(reviewData);
        PeriodicUpdate periodicUpdate = buildPeriodicUpdate(reviewData);
        return objectMapper.writeValueAsString(periodicUpdate);
    }

    @Override
    protected boolean isDefaultStrategy() {
        // Use period endpoint as default when tax year is unknown (safer for older tax years)
        return true;
    }

    @Override
    public String getDescription() {
        return "Period endpoint (POST /period) for tax years 2017-18 to 2024-25";
    }

    /**
     * Builds a PeriodicUpdate DTO from the quarterly review data.
     *
     * <p>The PeriodicUpdate includes periodDates wrapper as required by the
     * HMRC Self-Employment Business API v5.0 for tax years up to 2024-25.</p>
     *
     * <p>Uses the shared {@link #mapExpenses(Map)} method from the base class
     * to ensure consistent SA103 category mapping across all strategies.</p>
     *
     * @param reviewData the quarterly review data
     * @return PeriodicUpdate with periodDates, periodIncome, and periodExpenses
     */
    private PeriodicUpdate buildPeriodicUpdate(QuarterlyReviewData reviewData) {
        // Income as turnover
        PeriodicUpdate.PeriodIncome periodIncome =
                PeriodicUpdate.PeriodIncome.ofTurnover(reviewData.getTotalIncome());

        // Expenses mapped to SA103 categories using shared mapping logic
        MappedExpenses mapped = mapExpenses(reviewData.getExpensesByCategory());

        PeriodicUpdate.PeriodExpenses periodExpenses = PeriodicUpdate.PeriodExpenses.builder()
                .costOfGoodsBought(mapped.costOfGoodsBought())
                .cisPaymentsToSubcontractors(mapped.cisPaymentsToSubcontractors())
                .staffCosts(mapped.staffCosts())
                .travelCosts(mapped.travelCosts())
                .premisesRunningCosts(mapped.premisesRunningCosts())
                .maintenanceCosts(mapped.maintenanceCosts())
                .adminCosts(mapped.adminCosts())
                .advertisingCosts(mapped.advertisingCosts())
                .businessEntertainmentCosts(mapped.businessEntertainmentCosts())
                .interest(mapped.interest())
                .financialCharges(mapped.financialCharges())
                .badDebt(mapped.badDebt())
                .professionalFees(mapped.professionalFees())
                .depreciation(mapped.depreciation())
                .other(mapped.other())
                .build();

        return new PeriodicUpdate(
                reviewData.getPeriodStart(),
                reviewData.getPeriodEnd(),
                periodIncome,
                periodExpenses
        );
    }
}
