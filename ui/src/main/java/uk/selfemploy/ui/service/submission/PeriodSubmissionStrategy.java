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
     * <p>Both expense columns are filed: what was spent, and the part of it that cannot be claimed.
     * HMRC subtracts the second from the first, so the deduction this payload asks for is the
     * claimable spend the rest of the app computes.</p>
     *
     * @param reviewData the quarterly review data
     * @return PeriodicUpdate with periodDates, periodIncome, and both expense columns
     */
    private PeriodicUpdate buildPeriodicUpdate(QuarterlyReviewData reviewData) {
        // Income as turnover
        PeriodicUpdate.PeriodIncome periodIncome =
                PeriodicUpdate.PeriodIncome.ofTurnover(reviewData.getTotalIncome());

        // Expenses mapped to SA103 categories using shared mapping logic
        MappedExpenses mapped = mapExpenses(reviewData.getSpendByCategory());

        PeriodicUpdate.PeriodExpenses periodExpenses = PeriodicUpdate.PeriodExpenses.builder()
                .costOfGoodsBought(mapped.costOfGoodsBought().spent())
                .cisPaymentsToSubcontractors(mapped.cisPaymentsToSubcontractors().spent())
                .staffCosts(mapped.staffCosts().spent())
                .travelCosts(mapped.travelCosts().spent())
                .premisesRunningCosts(mapped.premisesRunningCosts().spent())
                .maintenanceCosts(mapped.maintenanceCosts().spent())
                .adminCosts(mapped.adminCosts().spent())
                .advertisingCosts(mapped.advertisingCosts().spent())
                .businessEntertainmentCosts(mapped.businessEntertainmentCosts().spent())
                .interest(mapped.interest().spent())
                .financialCharges(mapped.financialCharges().spent())
                .badDebt(mapped.badDebt().spent())
                .professionalFees(mapped.professionalFees().spent())
                .depreciation(mapped.depreciation().spent())
                .other(mapped.other().spent())
                .build();

        return new PeriodicUpdate(
                reviewData.getPeriodStart(),
                reviewData.getPeriodEnd(),
                periodIncome,
                periodExpenses,
                disallowableExpenses(mapped)
        );
    }
}
