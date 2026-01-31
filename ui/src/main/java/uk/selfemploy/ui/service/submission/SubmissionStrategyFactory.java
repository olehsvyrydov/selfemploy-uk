package uk.selfemploy.ui.service.submission;

import uk.selfemploy.common.domain.TaxYear;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Factory for creating the appropriate submission strategy based on tax year.
 *
 * <p>This factory implements the Strategy pattern to select the correct HMRC
 * submission approach based on the tax year. Different tax years require
 * different API endpoints and DTO structures:</p>
 *
 * <ul>
 *   <li>Tax years 2017-18 to 2024-25: {@link PeriodSubmissionStrategy} (POST /period)</li>
 *   <li>Tax years 2025-26 onwards: {@link CumulativeSubmissionStrategy} (PUT /cumulative)</li>
 * </ul>
 *
 * <h3>Extensibility:</h3>
 * <p>To add support for a new tax year format or endpoint, simply create a new
 * strategy class extending {@link AbstractSubmissionStrategy} and register it
 * in this factory. No changes required to existing strategies or the service layer.</p>
 *
 * <h3>Example - Adding future strategy:</h3>
 * <pre>
 * // If HMRC introduces a new endpoint for 2030-31+:
 * public class FutureSubmissionStrategy extends AbstractSubmissionStrategy {
 *     public FutureSubmissionStrategy() {
 *         super(2030, Integer.MAX_VALUE);
 *     }
 *     // ... implementation
 * }
 *
 * // Register in factory:
 * strategies.add(new FutureSubmissionStrategy());
 * </pre>
 */
public class SubmissionStrategyFactory {

    private static final Logger LOG = Logger.getLogger(SubmissionStrategyFactory.class.getName());

    private final List<SubmissionStrategy> strategies;
    private final SubmissionStrategy defaultStrategy;

    /**
     * Creates a factory with default strategy registrations.
     */
    public SubmissionStrategyFactory() {
        this.strategies = new ArrayList<>();

        // Register strategies in order of preference (most specific first)
        // The factory will use the first strategy that supports the given tax year
        strategies.add(new CumulativeSubmissionStrategy());  // 2025-26+
        strategies.add(new PeriodSubmissionStrategy());      // 2017-18 to 2024-25

        // Default strategy when tax year is null or unknown
        this.defaultStrategy = new PeriodSubmissionStrategy();
    }

    /**
     * Creates a factory with custom strategies.
     *
     * <p>Useful for testing or for adding custom strategies without modifying this class.</p>
     *
     * @param strategies the list of strategies to use
     * @param defaultStrategy the strategy to use when no other strategy matches
     */
    public SubmissionStrategyFactory(List<SubmissionStrategy> strategies, SubmissionStrategy defaultStrategy) {
        this.strategies = new ArrayList<>(strategies);
        this.defaultStrategy = defaultStrategy;
    }

    /**
     * Gets the appropriate submission strategy for the given tax year.
     *
     * @param taxYear the tax year for the submission
     * @return the strategy to use for this tax year
     */
    public SubmissionStrategy getStrategy(TaxYear taxYear) {
        if (taxYear == null) {
            LOG.fine("Tax year is null, using default strategy: " + defaultStrategy.getDescription());
            return defaultStrategy;
        }

        for (SubmissionStrategy strategy : strategies) {
            if (strategy.supports(taxYear)) {
                LOG.fine("Selected strategy for tax year " + taxYear.label() + ": " + strategy.getDescription());
                return strategy;
            }
        }

        // Fallback to default if no strategy supports the tax year
        LOG.warning("No strategy found for tax year " + taxYear.label() + ", using default: " + defaultStrategy.getDescription());
        return defaultStrategy;
    }

    /**
     * Registers a new strategy.
     *
     * <p>The strategy will be added to the beginning of the list, giving it
     * priority over existing strategies for overlapping tax year ranges.</p>
     *
     * @param strategy the strategy to register
     */
    public void registerStrategy(SubmissionStrategy strategy) {
        strategies.add(0, strategy);
        LOG.info("Registered new submission strategy: " + strategy.getDescription());
    }

    /**
     * Returns all registered strategies.
     *
     * @return unmodifiable list of strategies
     */
    public List<SubmissionStrategy> getStrategies() {
        return List.copyOf(strategies);
    }

    /**
     * Returns the default strategy used when no other strategy matches.
     *
     * @return the default strategy
     */
    public SubmissionStrategy getDefaultStrategy() {
        return defaultStrategy;
    }
}
