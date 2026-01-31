package uk.selfemploy.ui.service.submission;

import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

/**
 * Strategy interface for HMRC submission serialization.
 *
 * <p>Different tax years require different API endpoints and DTO structures.
 * Implementations of this interface encapsulate the version-specific logic
 * for serializing submission data to HMRC's expected format.</p>
 *
 * <h3>Current strategies:</h3>
 * <ul>
 *   <li>{@link PeriodSubmissionStrategy} - Tax years up to 2024-25 (POST /period with periodDates)</li>
 *   <li>{@link CumulativeSubmissionStrategy} - Tax years 2025-26+ (PUT /cumulative with taxYear query param)</li>
 * </ul>
 *
 * @see SubmissionStrategyFactory
 */
public interface SubmissionStrategy {

    /**
     * Returns the HTTP method for this strategy (POST or PUT).
     *
     * @return "POST" or "PUT"
     */
    String getHttpMethod();

    /**
     * Builds the endpoint path for this strategy.
     *
     * @param baseUrl the HMRC API base URL
     * @param nino the National Insurance Number
     * @param businessId the HMRC business ID
     * @param taxYear the tax year (may be used in query parameter)
     * @return the full endpoint URL
     */
    String buildEndpointUrl(String baseUrl, String nino, String businessId, TaxYear taxYear);

    /**
     * Serializes the review data to JSON in the format expected by this strategy's endpoint.
     *
     * @param reviewData the quarterly review data
     * @return JSON string formatted for HMRC API
     * @throws Exception if serialization fails
     */
    String serializeRequest(QuarterlyReviewData reviewData) throws Exception;

    /**
     * Returns whether this strategy supports the given tax year.
     *
     * @param taxYear the tax year to check
     * @return true if this strategy handles the tax year
     */
    boolean supports(TaxYear taxYear);

    /**
     * Returns a description of this strategy for logging/debugging.
     *
     * @return strategy description
     */
    String getDescription();
}
