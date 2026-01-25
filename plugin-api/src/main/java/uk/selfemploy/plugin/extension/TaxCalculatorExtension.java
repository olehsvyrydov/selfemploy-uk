package uk.selfemploy.plugin.extension;

/**
 * Extension point for custom tax calculations.
 *
 * <p>Plugins implement this interface to provide alternative or specialized
 * tax calculators. This allows supporting different tax jurisdictions
 * (e.g., Scottish income tax), special tax reliefs, or industry-specific
 * calculations.</p>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li><b>Scottish Tax</b> - Different income tax bands and rates</li>
 *   <li><b>Special Reliefs</b> - Artist's averaging, farmers averaging</li>
 *   <li><b>Industry Rules</b> - Construction industry scheme, etc.</li>
 *   <li><b>Future Rates</b> - Calculate with proposed future tax rates</li>
 * </ul>
 *
 * <h2>Calculator Selection</h2>
 * <p>When calculating tax, the system checks each registered calculator:</p>
 * <ol>
 *   <li>Calculators are sorted by {@link #getPriority()} (highest first)</li>
 *   <li>The first calculator where {@link #appliesTo(TaxContext)} returns true is used</li>
 *   <li>If no plugin calculator applies, the built-in calculator is used</li>
 * </ol>
 *
 * <h2>Implementation Example</h2>
 * <pre>{@code
 * public class ScottishTaxCalculator implements TaxCalculatorExtension {
 *     @Override
 *     public String getCalculatorId() {
 *         return "scottish-income-tax";
 *     }
 *
 *     @Override
 *     public String getCalculatorName() {
 *         return "Scottish Income Tax";
 *     }
 *
 *     @Override
 *     public String getCalculatorDescription() {
 *         return "Calculates income tax using Scottish rates and bands.";
 *     }
 *
 *     @Override
 *     public boolean appliesTo(TaxContext context) {
 *         return context.region().usesScottishRates();
 *     }
 *
 *     @Override
 *     public TaxResult calculateTax(TaxContext context) {
 *         // Perform Scottish tax calculation
 *         return result;
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Tax calculations may be called from background threads. Implementations
 * must be thread-safe.</p>
 *
 * @see TaxContext
 * @see TaxResult
 * @see ExtensionPoint
 */
public interface TaxCalculatorExtension extends ExtensionPoint {

    /**
     * Returns the unique identifier for this calculator.
     *
     * <p>The ID must be unique across all tax calculators. It is recommended
     * to use a namespaced format like "plugin-name.calculator-id".</p>
     *
     * @return the calculator ID, never null or blank
     */
    String getCalculatorId();

    /**
     * Returns the display name for this calculator.
     *
     * <p>This name is shown in settings and tax summary screens when this
     * calculator is being used.</p>
     *
     * @return the calculator name, never null or blank
     */
    String getCalculatorName();

    /**
     * Returns a description of when this calculator should be used.
     *
     * <p>This description helps users understand what scenarios this
     * calculator handles and when it would apply.</p>
     *
     * @return the calculator description, never null (may be empty)
     */
    default String getCalculatorDescription() {
        return "";
    }

    /**
     * Returns whether this calculator applies to the given context.
     *
     * <p>Implementations should check the context properties (region, tax year,
     * additional data) to determine if this calculator should be used.</p>
     *
     * <p>Only the first applicable calculator (by priority) is used for each
     * calculation request.</p>
     *
     * @param context the tax calculation context
     * @return true if this calculator should handle the calculation
     */
    boolean appliesTo(TaxContext context);

    /**
     * Calculates tax for the given context.
     *
     * <p>This method performs the actual tax calculation, returning the
     * breakdown of income tax and National Insurance liability.</p>
     *
     * <p>This method may be called from a background thread. Implementations
     * must be thread-safe.</p>
     *
     * @param context the tax calculation context
     * @return the calculated tax result, never null
     * @throws TaxCalculationException if calculation fails
     */
    TaxResult calculateTax(TaxContext context);

    /**
     * Returns the priority of this calculator.
     *
     * <p>Higher values indicate higher priority. When multiple calculators
     * apply to a context, the one with the highest priority is used.</p>
     *
     * <p>Built-in calculators have priority 0. Plugin calculators should use
     * positive values to override built-in behavior when appropriate.</p>
     *
     * <p>Default value is 10.</p>
     *
     * @return the calculator priority
     */
    default int getPriority() {
        return 10;
    }
}
