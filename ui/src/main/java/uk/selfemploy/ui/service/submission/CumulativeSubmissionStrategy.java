package uk.selfemploy.ui.service.submission;

import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.dto.CumulativeSummary;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

/**
 * Submission strategy for tax years 2025-26 onwards.
 *
 * <p>Uses PUT /cumulative endpoint with taxYear as query parameter.
 * The request body does NOT include periodDates - uses flat structure.
 * Accept header: application/vnd.hmrc.5.0+json (same as period endpoint)</p>
 *
 * <h3>Request format:</h3>
 * <pre>
 * PUT /cumulative?taxYear=2025-26
 * {
 *   "periodIncome": { ... },
 *   "periodExpenses": { ... }
 * }
 * </pre>
 *
 * <p>Note: No periodDates wrapper - dates are determined from the taxYear query parameter.</p>
 *
 * @see PeriodSubmissionStrategy for tax years up to 2024-25
 */
public class CumulativeSubmissionStrategy extends AbstractSubmissionStrategy {

    /**
     * Tax year 2025-26 is the first year for the cumulative endpoint.
     */
    private static final int MIN_TAX_YEAR = 2025;

    /**
     * No upper limit - cumulative endpoint will be used for all future tax years
     * until HMRC introduces a new endpoint format.
     */
    private static final int MAX_TAX_YEAR = Integer.MAX_VALUE;

    public CumulativeSubmissionStrategy() {
        super(MIN_TAX_YEAR, MAX_TAX_YEAR);
    }

    @Override
    public String getHttpMethod() {
        return "PUT";
    }

    @Override
    public String buildEndpointUrl(String baseUrl, String nino, String businessId, TaxYear taxYear) {
        String basePath = buildBasePath(baseUrl, nino, businessId);
        String taxYearParam = taxYear != null ? taxYear.hmrcFormat() : "";
        return basePath + "/cumulative?taxYear=" + taxYearParam;
    }

    @Override
    public String serializeRequest(QuarterlyReviewData reviewData) throws Exception {
        validateReviewData(reviewData);
        CumulativeSummary summary = buildCumulativeSummary(reviewData);
        return objectMapper.writeValueAsString(summary);
    }

    @Override
    public String getDescription() {
        return "Cumulative endpoint (PUT /cumulative) for tax years 2025-26 onwards";
    }

    /**
     * Builds a CumulativeSummary DTO from the quarterly review data.
     *
     * <p>The CumulativeSummary does NOT include periodDates - the tax year is
     * provided as a query parameter instead. This is required by the HMRC
     * Self-Employment Business API v5.0 for tax years 2025-26 onwards.</p>
     *
     * <p>Uses the shared {@link #mapExpenses(Map)} method from the base class
     * to ensure consistent SA103 category mapping across all strategies.</p>
     *
     * @param reviewData the quarterly review data
     * @return CumulativeSummary with periodIncome and periodExpenses (no periodDates)
     */
    private CumulativeSummary buildCumulativeSummary(QuarterlyReviewData reviewData) {
        // Income as turnover
        CumulativeSummary.CumulativeIncome income =
                CumulativeSummary.CumulativeIncome.ofTurnover(reviewData.getTotalIncome());

        // Expenses mapped to SA103 categories using shared mapping logic
        MappedExpenses mapped = mapExpenses(reviewData.getExpensesByCategory());

        CumulativeSummary.CumulativeExpenses periodExpenses = CumulativeSummary.CumulativeExpenses.builder()
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

        return new CumulativeSummary(income, periodExpenses);
    }
}
